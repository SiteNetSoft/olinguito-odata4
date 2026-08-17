/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Extracted referential-constraint key computation for key-as-segment URLs
 * Copyright 2026 SiteNetSoft - Port OLINGO-1334: propagate lambda-variable scope into function parameter parsing
 * Copyright 2026 SiteNetSoft - OData 4.01: map ambiguous optional-parameter overloads to a 400 response
 * Copyright 2026 SiteNetSoft - OData 4.01: opened key-value helpers to the key-as-segment parser
 * Copyright 2026 SiteNetSoft - OData 4.01: resolve alternate keys in parenthesized key predicates
 * Copyright 2026 SiteNetSoft - OData 4.01: dedupe alternate-key declarations, reject null key values
 * Copyright 2026 SiteNetSoft - OData 4.01: malformed optional-parameter default values are rejected with 400
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAlternateKey;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAlternateKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAmbiguousOverloadException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.UriParameter;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceLambdaVariable;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePartTyped;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Expression;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Literal;
import org.sitenetsoft.olinguito.server.core.ODataImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriParameterImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceTypedImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceWithKeysImpl;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriTokenizer.TokenKind;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.AliasQueryOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.expression.LiteralImpl;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

public class ParserHelper {

  private static final OData odata = new ODataImpl();

  protected static final Map<TokenKind, EdmPrimitiveTypeKind> tokenToPrimitiveType;
  static {
    /* Enum and null are not present in the map. These have to be handled differently. */
    Map<TokenKind, EdmPrimitiveTypeKind> temp = new EnumMap<>(TokenKind.class);
    temp.put(TokenKind.BooleanValue, EdmPrimitiveTypeKind.Boolean);
    temp.put(TokenKind.StringValue, EdmPrimitiveTypeKind.String);
    // Very large integer values are of type Edm.Decimal but this is handled elsewhere.
    temp.put(TokenKind.IntegerValue, EdmPrimitiveTypeKind.Int64);
    temp.put(TokenKind.GuidValue, EdmPrimitiveTypeKind.Guid);
    temp.put(TokenKind.DateValue, EdmPrimitiveTypeKind.Date);
    temp.put(TokenKind.DateTimeOffsetValue, EdmPrimitiveTypeKind.DateTimeOffset);
    temp.put(TokenKind.TimeOfDayValue, EdmPrimitiveTypeKind.TimeOfDay);
    temp.put(TokenKind.DecimalValue, EdmPrimitiveTypeKind.Decimal);
    temp.put(TokenKind.DoubleValue, EdmPrimitiveTypeKind.Double);
    temp.put(TokenKind.DurationValue, EdmPrimitiveTypeKind.Duration);
    temp.put(TokenKind.BinaryValue, EdmPrimitiveTypeKind.Binary);

    temp.put(TokenKind.GeographyPoint, EdmPrimitiveTypeKind.GeographyPoint);
    temp.put(TokenKind.GeometryPoint, EdmPrimitiveTypeKind.GeometryPoint);
    temp.put(TokenKind.GeographyLineString, EdmPrimitiveTypeKind.GeographyLineString);
    temp.put(TokenKind.GeometryLineString, EdmPrimitiveTypeKind.GeometryLineString);
    temp.put(TokenKind.GeographyPolygon, EdmPrimitiveTypeKind.GeographyPolygon);
    temp.put(TokenKind.GeometryPolygon, EdmPrimitiveTypeKind.GeometryPolygon);
    temp.put(TokenKind.GeographyMultiPoint, EdmPrimitiveTypeKind.GeographyMultiPoint);
    temp.put(TokenKind.GeometryMultiPoint, EdmPrimitiveTypeKind.GeometryMultiPoint);
    temp.put(TokenKind.GeographyMultiLineString, EdmPrimitiveTypeKind.GeographyMultiLineString);
    temp.put(TokenKind.GeometryMultiLineString, EdmPrimitiveTypeKind.GeometryMultiLineString);
    temp.put(TokenKind.GeographyMultiPolygon, EdmPrimitiveTypeKind.GeographyMultiPolygon);
    temp.put(TokenKind.GeometryMultiPolygon, EdmPrimitiveTypeKind.GeometryMultiPolygon);
    temp.put(TokenKind.GeographyCollection, EdmPrimitiveTypeKind.GeographyCollection);
    temp.put(TokenKind.GeometryCollection, EdmPrimitiveTypeKind.GeometryCollection);

    tokenToPrimitiveType = Collections.unmodifiableMap(temp);
  }

  protected static void requireNext(UriTokenizer tokenizer, final TokenKind required) throws UriParserException {
    if (!tokenizer.next(required)) {
      throw new UriParserSyntaxException("Expected token '" + required.toString() + "' not found.",
          UriParserSyntaxException.MessageKeys.SYNTAX);
    }
  }

  protected static void requireTokenEnd(UriTokenizer tokenizer) throws UriParserException {
    requireNext(tokenizer, TokenKind.EOF);
  }

  protected static boolean bws(UriTokenizer tokenizer) {
    return tokenizer.nextWhitespace();
  }
  
  protected static TokenKind next(UriTokenizer tokenizer, final TokenKind... kinds) {
    for (final TokenKind kind : kinds) {
      if (tokenizer.next(kind)) {
        return kind;
      }
    }
    return null;
  }

  protected static TokenKind nextPrimitiveValue(UriTokenizer tokenizer) {
    return next(tokenizer,
        TokenKind.NULL,
        TokenKind.BooleanValue,
        TokenKind.StringValue,

        // The order of the next seven expressions is important in order to avoid
        // finding partly parsed tokens (counter-intuitive as it may be, even a GUID may start with digits ...).
        TokenKind.GuidValue,
        TokenKind.DoubleValue,
        TokenKind.DecimalValue,
        TokenKind.DateTimeOffsetValue,
        TokenKind.DateValue,
        TokenKind.TimeOfDayValue,
        TokenKind.IntegerValue,

        TokenKind.DurationValue,
        TokenKind.BinaryValue,
        TokenKind.EnumValue,

        // Geography and geometry literals are defined to be primitive,
        // although they contain several parts with their own meaning.
        TokenKind.GeographyPoint,
        TokenKind.GeometryPoint,
        TokenKind.GeographyLineString,
        TokenKind.GeometryLineString,
        TokenKind.GeographyPolygon,
        TokenKind.GeometryPolygon,
        TokenKind.GeographyMultiPoint,
        TokenKind.GeometryMultiPoint,
        TokenKind.GeographyMultiLineString,
        TokenKind.GeometryMultiLineString,
        TokenKind.GeographyMultiPolygon,
        TokenKind.GeometryMultiPolygon,
        TokenKind.GeographyCollection,
        TokenKind.GeometryCollection);
  }

  protected static List<UriParameter> parseFunctionParameters(UriTokenizer tokenizer,
      final Edm edm, final EdmType referringType, final boolean withComplex,
      final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    return parseFunctionParameters(tokenizer, edm, referringType, withComplex, aliases, null);
  }

  // OLINGO-1334: Lambda bound variables used inside function parameters (e.g.
  // .../any(bf:namespace.Func(p=bf/Prop) ne 2)) must be resolvable by the sub-parser that
  // parses the parameter expression, so the enclosing lambda-variable scope is threaded through.
  protected static List<UriParameter> parseFunctionParameters(UriTokenizer tokenizer,
      final Edm edm, final EdmType referringType, final boolean withComplex,
      final Map<String, AliasQueryOption> aliases,
      final Deque<UriResourceLambdaVariable> lambdaVariables)
      throws UriParserException, UriValidationException {
    List<UriParameter> parameters = new ArrayList<>();
    Set<String> parameterNames = new HashSet<>();
    ParserHelper.requireNext(tokenizer, TokenKind.OPEN);
    if (tokenizer.next(TokenKind.CLOSE)) {
      return parameters;
    }
    do {
      ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
      final String name = tokenizer.getText();
      if (parameterNames.contains(name)) {
        throw new UriParserSemanticException("Duplicated function parameter " + name,
            UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE, name);
      }
      parameterNames.add(name);
      ParserHelper.requireNext(tokenizer, TokenKind.EQ);
      if (tokenizer.next(TokenKind.COMMA) || tokenizer.next(TokenKind.CLOSE) || tokenizer.next(TokenKind.EOF)) {
        throw new UriParserSyntaxException("Parameter value expected.", UriParserSyntaxException.MessageKeys.SYNTAX);
      }
      UriParameterImpl parameter = new UriParameterImpl().setName(name);
      if (tokenizer.next(TokenKind.ParameterAliasName)) {
        final String aliasName = tokenizer.getText();
        parameter.setAlias(aliasName)
            .setExpression(aliases.containsKey(aliasName) ? aliases.get(aliasName).getValue() : null);
      } else if (tokenizer.next(TokenKind.jsonArrayOrObject)) {
        if (withComplex) {
          parameter.setText(tokenizer.getText());
        } else {
          throw new UriParserSemanticException("A JSON array or object is not allowed as parameter value.",
              UriParserSemanticException.MessageKeys.COMPLEX_PARAMETER_IN_RESOURCE_PATH, tokenizer.getText());
        }
      } else if (withComplex) {
        final ExpressionParser expressionParser = new ExpressionParser(edm, odata);
        if (lambdaVariables != null) {
          expressionParser.setLambdaVariables(lambdaVariables);
        }
        final Expression expression = expressionParser.parse(tokenizer, referringType, null, aliases);
        parameter.setText(expression instanceof Literal literal ?
            "null".equals(literal.getText()) ? null : literal.getText() :
            null)
            .setExpression(expression instanceof Literal ? null : expression);
      } else if (nextPrimitiveValue(tokenizer) == null) {
        throw new UriParserSemanticException("Wrong parameter value.",
            UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE, "");
      } else {
        final String literalValue = tokenizer.getText();
        parameter.setText("null".equals(literalValue) ? null : literalValue);
      }
      parameters.add(parameter);
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    return parameters;
  }

  protected static void validateFunctionParameters(final EdmFunction function, final List<UriParameter> parameters,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    for (final UriParameter parameter : parameters) {
      final String parameterName = parameter.getName();
      final EdmParameter edmParameter = function.getParameter(parameterName);
      final boolean isNullable = edmParameter.isNullable();
      if (parameter.getText() == null && parameter.getExpression() == null && !isNullable) {
        if (parameter.getAlias() == null) {
          // No alias, value is explicitly null.
          throw new UriValidationException("Missing non-nullable parameter " + parameterName,
              UriValidationException.MessageKeys.MISSING_PARAMETER, parameterName);
        } else {
          final String valueForAlias = aliases.containsKey(parameter.getAlias()) ?
              parseAliasValue(parameter.getAlias(),
                  edmParameter.getType(), edmParameter.isNullable(), edmParameter.isCollection(),
                  edm, referringType, aliases).getText() :
              null;
          // Alias value is missing or explicitly null.
          if (valueForAlias == null) {
            throw new UriValidationException("Missing alias for " + parameterName,
                UriValidationException.MessageKeys.MISSING_ALIAS, parameter.getAlias());
          }
        }
      }
    }
  }

  protected static AliasQueryOption parseAliasValue(final String name, final EdmType type, final boolean isNullable,
      final boolean isCollection, final Edm edm, final EdmType referringType,
      final Map<String, AliasQueryOption> aliases) throws UriParserException, UriValidationException {
    final EdmTypeKind kind = type == null ? null : type.getKind();
    final AliasQueryOption alias = aliases.get(name);
    if (alias != null && alias.getText() != null) {
      UriTokenizer aliasTokenizer = new UriTokenizer(alias.getText());
      if (kind == null
          || !((isCollection || kind == EdmTypeKind.COMPLEX || kind == EdmTypeKind.ENTITY ?
          aliasTokenizer.next(TokenKind.jsonArrayOrObject) :
          nextPrimitiveTypeValue(aliasTokenizer, (EdmPrimitiveType) type, isNullable))
          && aliasTokenizer.next(TokenKind.EOF))) {
        // The alias value is not an allowed literal value, so parse it again as expression.
        aliasTokenizer = new UriTokenizer(alias.getText());
        // Don't pass on the current alias to avoid circular references.
        Map<String, AliasQueryOption> aliasesInner = new HashMap<>(aliases);
        aliasesInner.remove(name);
        final Expression expression = new ExpressionParser(edm, odata)
            .parse(aliasTokenizer, referringType, null, aliasesInner);
        final EdmType expressionType = ExpressionParser.getType(expression);
        if (aliasTokenizer.next(TokenKind.EOF)
            && (expressionType == null || type == null || expressionType.equals(type))) {
          ((AliasQueryOptionImpl) alias).setAliasValue(expression);
        } else {
          throw new UriParserSemanticException("Illegal value for alias '" + alias.getName() + "'.",
              UriParserSemanticException.MessageKeys.UNKNOWN_PART, alias.getText());
        }
      }
    }
    return alias;
  }

  protected static List<UriParameter> parseNavigationKeyPredicate(UriTokenizer tokenizer,
      final EdmNavigationProperty navigationProperty,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    if (tokenizer.next(TokenKind.OPEN)) {
      if (navigationProperty.isCollection()) {
        return parseKeyPredicate(tokenizer, navigationProperty.getType(), navigationProperty.getPartner(),
            edm, referringType, aliases);
      } else {
        throw new UriParserSemanticException("A key is not allowed on non-collection navigation properties.",
            UriParserSemanticException.MessageKeys.KEY_NOT_ALLOWED);
      }
    }
    return null;
  }

  protected static List<UriParameter> parseKeyPredicate(UriTokenizer tokenizer, final EdmEntityType edmEntityType,
      final EdmNavigationProperty partner,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    return parseKeyPredicate(tokenizer, edmEntityType, partner, edm, referringType, aliases, null);
  }

  /**
   * Parses a parenthesized key predicate.
   * @param bindingTarget the binding target the entity type is addressed through, may be <code>null</code>;
   *                      it contributes the alternate keys declared on the entity set itself
   */
  protected static List<UriParameter> parseKeyPredicate(UriTokenizer tokenizer, final EdmEntityType edmEntityType,
      final EdmNavigationProperty partner,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases,
      final EdmBindingTarget bindingTarget)
      throws UriParserException, UriValidationException {
    final List<EdmKeyPropertyRef> keyPropertyRefs = edmEntityType.getKeyPropertyRefs();
    if (tokenizer.next(TokenKind.CLOSE)) {
      throw new UriParserSemanticException(
          "Expected " + keyPropertyRefs.size() + " key predicates but got none.",
          UriParserSemanticException.MessageKeys.WRONG_NUMBER_OF_KEY_PROPERTIES,
          Integer.toString(keyPropertyRefs.size()), "0");
    }
    List<UriParameter> keys = new ArrayList<>();
    final Map<String, String> referencedNames = referencedKeyNames(edmEntityType, partner);

    if (partner != null) {
      for (final EdmKeyPropertyRef candidate : keyPropertyRefs) {
    	  final UriParameter simpleKey = simpleKey(tokenizer, candidate, edm, referringType, aliases);
          if (simpleKey != null) {
            keys.add(simpleKey);
          }
      }
    }

    if (keyPropertyRefs.size() - referencedNames.size() == 1) {
      for (final EdmKeyPropertyRef candidate : keyPropertyRefs) {
       if (referencedNames.get(candidate.getName()) == null) {
          final UriParameter simpleKey = simpleKey(tokenizer, candidate, edm, referringType, aliases);
          if (simpleKey != null) {
            keys.add(simpleKey);
          }
          break;
        }
      }
    }
    if (keys.isEmpty()) {
      // An alternate key (URL Conventions 4.3.5) always addresses the entity completely, so it is only
      // tried when no key value is determined by a referential constraint of the navigation property
      // leading here; in that case the primary key is the only key that can be completed from the URI.
      if (referencedNames.isEmpty()) {
        final List<EdmAlternateKey> candidates = alternateKeyCandidates(edmEntityType, bindingTarget);
        if (!candidates.isEmpty()) {
          // UriTokenizer has a single save slot, so it is only claimed when an attempt really follows.
          tokenizer.saveState();
          final List<UriParameter> alternateKeys = alternateKey(tokenizer, candidates, edm, referringType, aliases);
          if (alternateKeys != null) {
            return alternateKeys;
          }
          tokenizer.returnToSavedState();
        }
      }
      if (tokenizer.next(TokenKind.ODataIdentifier)) {
        keys.addAll(compoundKey(tokenizer, edmEntityType, edm, referringType, aliases));
      } else {
        throw new UriParserSemanticException("The key value is not valid.",
            UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE, "");
      }
    }

    if (keys.size() < keyPropertyRefs.size() && partner != null) {
      // Fill missing keys from referential constraints.
      for (final String name : edmEntityType.getKeyPredicateNames()) {
        boolean found = false;
        for (final UriParameter key : keys) {
          if (name.equals(key.getName())) {
            found = true;
            break;
          }
        }
        if (!found && referencedNames.get(name) != null) {
          keys.add(0, new UriParameterImpl().setName(name).setReferencedProperty(referencedNames.get(name)));
        }
      }
    }

    // Check that all key predicates are filled from the URI.
    if (keys.size() < keyPropertyRefs.size()) {
      throw new UriParserSemanticException(
          "Expected " + keyPropertyRefs.size() + " key predicates but found " + keys.size() + ".",
          UriParserSemanticException.MessageKeys.WRONG_NUMBER_OF_KEY_PROPERTIES,
          Integer.toString(keyPropertyRefs.size()), Integer.toString(keys.size()));
    } else {
      return keys;
    }
  }

  /**
   * Determines the key properties of an entity type whose values are determined by referential constraints
   * of the navigation property that leads to it, so that they must not be given in the URI.
   * @param edmEntityType the entity type whose key is addressed
   * @param partner the partner of the navigation property leading to the entity type, may be <code>null</code>
   * @return a map from key-property name to the name of the referencing property, empty if there are none
   */
  protected static Map<String, String> referencedKeyNames(final EdmEntityType edmEntityType,
      final EdmNavigationProperty partner) {
    final Map<String, String> referencedNames = new HashMap<>();
    if (partner != null) {
      for (final String name : edmEntityType.getKeyPredicateNames()) {
        final String referencedName = partner.getReferencingPropertyName(name);
        if (referencedName != null) {
          referencedNames.put(name, referencedName);
        }
      }
    }
    return referencedNames;
  }

  private static UriParameter simpleKey(UriTokenizer tokenizer, final EdmKeyPropertyRef edmKeyPropertyRef,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    final EdmProperty edmProperty = edmKeyPropertyRef == null ? null : edmKeyPropertyRef.getProperty();
    if (nextPrimitiveTypeValue(tokenizer,
        edmProperty == null ? null : (EdmPrimitiveType) edmProperty.getType(),
        edmProperty == null ? false : edmProperty.isNullable())) {
      final String literalValue = tokenizer.getText();
      ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
      return createUriParameter(edmProperty, edmKeyPropertyRef.getName(), literalValue, edm, referringType, aliases);
    } else {
      return null;
    }
  }

  /**
   * Tries to read the key predicate as an alternate key (OData 4.01 URL Conventions 4.3.5).
   * The parsed set of URL names must be exactly the set of one declared alternate key; otherwise
   * <code>null</code> is returned so that the caller can fall back to the primary-key parsing
   * (which then produces the errors it always produced). The tokenizer may have been advanced.
   * @param candidates the addressable alternate keys, never empty
   * @return the key predicates in the declaration order of the matched alternate key, or <code>null</code>
   */
  private static List<UriParameter> alternateKey(UriTokenizer tokenizer, final List<EdmAlternateKey> candidates,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    final List<EdmAlternateKey> viable = new ArrayList<>(candidates);
    final Map<String, UriParameter> parsed = new LinkedHashMap<>();
    boolean hasComma;
    do {
      if (!tokenizer.next(TokenKind.ODataIdentifier)) {
        return null;
      }
      final String urlName = tokenizer.getText();
      if (parsed.containsKey(urlName)) {
        throw new UriValidationException("Duplicated key property " + urlName,
            UriValidationException.MessageKeys.DOUBLE_KEY_PROPERTY, urlName);
      }
      viable.removeIf(group -> propertyRef(group, urlName) == null);
      final EdmProperty edmProperty = uniqueProperty(viable, urlName);
      if (edmProperty == null || !tokenizer.next(TokenKind.EQ)) {
        return null;
      }
      if (tokenizer.next(TokenKind.COMMA) || tokenizer.next(TokenKind.CLOSE) || tokenizer.next(TokenKind.EOF)) {
        return null;
      }
      // A null value can never identify an entity, so it is rejected even for a nullable property.
      if (!nextPrimitiveTypeValue(tokenizer, (EdmPrimitiveType) edmProperty.getType(), false)) {
        throw new UriParserSemanticException(urlName + " has not a valid key value.",
            UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE, urlName);
      }
      parsed.put(urlName,
          createUriParameter(edmProperty, urlName, tokenizer.getText(), edm, referringType, aliases));
      hasComma = tokenizer.next(TokenKind.COMMA);
    } while (hasComma);
    if (!tokenizer.next(TokenKind.CLOSE)) {
      return null;
    }
    final EdmAlternateKey matched = matchAlternateKey(candidates, parsed.keySet());
    if (matched == null) {
      return null;
    }
    final List<UriParameter> keys = new ArrayList<>();
    for (final EdmAlternateKeyPropertyRef ref : matched.getPropertyRefs()) {
      keys.add(((UriParameterImpl) parsed.get(ref.getUrlName()))
          .setAlternateKeyPropertyName(ref.getProperty().getName()));
    }
    return keys;
  }

  /**
   * Collects the alternate keys that can be addressed: those declared on the entity type and, if the
   * entity type is addressed through an entity set, those declared on that entity set. Groups with an
   * unresolvable property reference (for example a path into a complex property) are not addressable,
   * and a group that consists of exactly the primary-key properties is left to the primary-key parsing.
   */
  private static List<EdmAlternateKey> alternateKeyCandidates(final EdmEntityType edmEntityType,
      final EdmBindingTarget bindingTarget) {
    final List<EdmAlternateKey> declared = new ArrayList<>(edmEntityType.getAlternateKeys());
    if (bindingTarget instanceof EdmEntitySet entitySet) {
      declared.addAll(entitySet.getAlternateKeys());
    }
    final Set<String> primaryKeyNames = new HashSet<>(edmEntityType.getKeyPredicateNames());
    final List<EdmAlternateKey> candidates = new ArrayList<>();
    final Set<Map<String, String>> seen = new HashSet<>();
    for (final EdmAlternateKey alternateKey : declared) {
      // The same group may be declared on the entity type and on the entity set; identical declarations
      // must count as one, otherwise a correct URL would find more than one match and be rejected.
      if (isResolvable(alternateKey) && !urlNames(alternateKey).equals(primaryKeyNames)
          && seen.add(signature(alternateKey))) {
        candidates.add(alternateKey);
      }
    }
    return candidates;
  }

  /** @return the mapping from URL name to addressed property name that identifies a group's declaration */
  private static Map<String, String> signature(final EdmAlternateKey alternateKey) {
    final Map<String, String> signature = new HashMap<>();
    for (final EdmAlternateKeyPropertyRef ref : alternateKey.getPropertyRefs()) {
      signature.put(ref.getUrlName(), ref.getProperty().getName());
    }
    return signature;
  }

  private static boolean isResolvable(final EdmAlternateKey alternateKey) {
    for (final EdmAlternateKeyPropertyRef ref : alternateKey.getPropertyRefs()) {
      if (ref.getProperty() == null) {
        return false;
      }
    }
    return true;
  }

  private static Set<String> urlNames(final EdmAlternateKey alternateKey) {
    final Set<String> names = new HashSet<>();
    for (final EdmAlternateKeyPropertyRef ref : alternateKey.getPropertyRefs()) {
      names.add(ref.getUrlName());
    }
    return names;
  }

  private static EdmAlternateKeyPropertyRef propertyRef(final EdmAlternateKey alternateKey, final String urlName) {
    for (final EdmAlternateKeyPropertyRef ref : alternateKey.getPropertyRefs()) {
      if (ref.getUrlName().equals(urlName)) {
        return ref;
      }
    }
    return null;
  }

  /**
   * @return the property the URL name addresses if all remaining candidate groups agree on it,
   *         <code>null</code> if there is no candidate left or they disagree
   */
  private static EdmProperty uniqueProperty(final List<EdmAlternateKey> viable, final String urlName) {
    EdmProperty edmProperty = null;
    for (final EdmAlternateKey alternateKey : viable) {
      final EdmProperty candidate = propertyRef(alternateKey, urlName).getProperty();
      if (edmProperty == null) {
        edmProperty = candidate;
      } else if (!edmProperty.getName().equals(candidate.getName())) {
        return null;
      }
    }
    return edmProperty;
  }

  /** @return the single alternate key with exactly the given URL names, <code>null</code> if there is not one */
  private static EdmAlternateKey matchAlternateKey(final List<EdmAlternateKey> candidates,
      final Set<String> parsedNames) {
    EdmAlternateKey matched = null;
    for (final EdmAlternateKey alternateKey : candidates) {
      if (urlNames(alternateKey).equals(parsedNames)) {
        if (matched != null) {
          return null;
        }
        matched = alternateKey;
      }
    }
    return matched;
  }

  private static List<UriParameter> compoundKey(UriTokenizer tokenizer, final EdmEntityType edmEntityType,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {

    List<UriParameter> parameters = new ArrayList<>();
    List<String> parameterNames = new ArrayList<>();

    // To validate that each key predicate is exactly specified once, we use a list to pick from.
    List<String> remainingKeyNames = new ArrayList<String>(edmEntityType.getKeyPredicateNames());

    // At least one key predicate is mandatory.  Try to fetch all.
    boolean hasComma = false;
    do {
      final String keyPredicateName = tokenizer.getText();
      if (parameterNames.contains(keyPredicateName)) {
        throw new UriValidationException("Duplicated key property " + keyPredicateName,
            UriValidationException.MessageKeys.DOUBLE_KEY_PROPERTY, keyPredicateName);
      }
      if (remainingKeyNames.isEmpty()) {
        throw new UriParserSemanticException("Too many key properties.",
            UriParserSemanticException.MessageKeys.WRONG_NUMBER_OF_KEY_PROPERTIES,
            Integer.toString(parameters.size()), Integer.toString(parameters.size() + 1));
      }
      if (!remainingKeyNames.remove(keyPredicateName)) {
        throw new UriValidationException("Unknown key property " + keyPredicateName,
            UriValidationException.MessageKeys.INVALID_KEY_PROPERTY, keyPredicateName);
      }
      parameters.add(keyValuePair(tokenizer, keyPredicateName, edmEntityType, edm, referringType, aliases));
      parameterNames.add(keyPredicateName);
      hasComma = tokenizer.next(TokenKind.COMMA);
      if (hasComma) {
        ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
      }
    } while (hasComma);
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);

    return parameters;
  }

  private static UriParameter keyValuePair(UriTokenizer tokenizer,
      final String keyPredicateName, final EdmEntityType edmEntityType,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    final EdmKeyPropertyRef keyPropertyRef = edmEntityType.getKeyPropertyRef(keyPredicateName);
    final EdmProperty edmProperty = keyPropertyRef == null ? null : keyPropertyRef.getProperty();
    if (edmProperty == null) {
      throw new UriValidationException(keyPredicateName + " is not a valid key property name.",
          UriValidationException.MessageKeys.INVALID_KEY_PROPERTY, keyPredicateName);
    }
    ParserHelper.requireNext(tokenizer, TokenKind.EQ);
    if (tokenizer.next(TokenKind.COMMA) || tokenizer.next(TokenKind.CLOSE) || tokenizer.next(TokenKind.EOF)) {
      throw new UriParserSyntaxException("Key value expected.", UriParserSyntaxException.MessageKeys.SYNTAX);
    }
    if (nextPrimitiveTypeValue(tokenizer, (EdmPrimitiveType) edmProperty.getType(), edmProperty.isNullable())) {
      return createUriParameter(edmProperty, keyPredicateName, tokenizer.getText(), edm, referringType, aliases);
    } else {
      throw new UriParserSemanticException(keyPredicateName + " has not a valid key value.",
          UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE, keyPredicateName);
    }
  }

  protected static UriParameter createUriParameter(final EdmProperty edmProperty, final String parameterName,
      final String literalValue, final Edm edm, final EdmType referringType,
      final Map<String, AliasQueryOption> aliases) throws UriParserException, UriValidationException {
    final AliasQueryOption alias = literalValue.startsWith("@") ?
        getKeyAlias(literalValue, edmProperty, edm, referringType, aliases) :
        null;
    final String value = alias == null ? literalValue : alias.getText();
    final EdmPrimitiveType primitiveType = (EdmPrimitiveType) edmProperty.getType();
    try {
      if (!(primitiveType.validate(primitiveType.fromUriLiteral(value), edmProperty.isNullable(),
          edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(), edmProperty.isUnicode()))) {
        throw new UriValidationException("Invalid key property",
            UriValidationException.MessageKeys.INVALID_KEY_PROPERTY, parameterName);
      }
    } catch (final EdmPrimitiveTypeException e) {
      throw new UriValidationException("Invalid key property", e,
          UriValidationException.MessageKeys.INVALID_KEY_PROPERTY, parameterName);
    }

    return new UriParameterImpl()
        .setName(parameterName)
        .setText("null".equals(literalValue) ? null : literalValue)
        .setAlias(alias == null ? null : literalValue)
        .setExpression(alias == null ? null :
            alias.getValue() == null ? new LiteralImpl(value, primitiveType) : alias.getValue());
  }

  private static AliasQueryOption getKeyAlias(final String name, final EdmProperty edmProperty,
      final Edm edm, final EdmType referringType, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    if (aliases.containsKey(name)) {
      return parseAliasValue(name,
          edmProperty.getType(), edmProperty.isNullable(), edmProperty.isCollection(),
          edm, referringType, aliases);
    } else {
      throw new UriValidationException("Alias '" + name + "' for key value not found.",
          UriValidationException.MessageKeys.MISSING_ALIAS, name);
    }
  }

  protected static boolean nextPrimitiveTypeValue(UriTokenizer tokenizer,
      final EdmPrimitiveType primitiveType, final boolean nullable) {
    final EdmPrimitiveType type = primitiveType instanceof EdmTypeDefinition typeDef ?
        typeDef.getUnderlyingType() :
        primitiveType;
    if (tokenizer.next(TokenKind.ParameterAliasName)) {
      return true;
    } else if (nullable && tokenizer.next(TokenKind.NULL)) {
      return true;

    // Special handling for frequently-used types and types with more than one token kind.
    } else if (odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Boolean).equals(type)) {
      return tokenizer.next(TokenKind.BooleanValue);
    } else if (odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String).equals(type)) {
      return tokenizer.next(TokenKind.StringValue);
    } else if (odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.SByte).equals(type)
        || odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Byte).equals(type)
        || odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int16).equals(type)
        || odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32).equals(type)
        || odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int64).equals(type)) {
      return tokenizer.next(TokenKind.IntegerValue);
    } else if (odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Guid).equals(type)) {
      return tokenizer.next(TokenKind.GuidValue);
    } else if (odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Decimal).equals(type)) {
      // The order is important.
      // A decimal value should not be parsed as integer and let the tokenizer stop at the decimal point.
      return tokenizer.next(TokenKind.DecimalValue)
          || tokenizer.next(TokenKind.IntegerValue);
    } else if (odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Double).equals(type)
        || odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Single).equals(type)) {
      // The order is important.
      // A floating-point value should not be parsed as decimal and let the tokenizer stop at 'E'.
      // A decimal value should not be parsed as integer and let the tokenizer stop at the decimal point.
      return tokenizer.next(TokenKind.DoubleValue)
          || tokenizer.next(TokenKind.DecimalValue)
          || tokenizer.next(TokenKind.IntegerValue);
    } else if (type.getKind() == EdmTypeKind.ENUM) {
      return tokenizer.next(TokenKind.EnumValue);
    } else {
      // Check the types that have not been checked already above.
      for (final Entry<TokenKind, EdmPrimitiveTypeKind> entry : tokenToPrimitiveType.entrySet()) {
        final EdmPrimitiveTypeKind kind = entry.getValue();
        if ((kind == EdmPrimitiveTypeKind.Date || kind == EdmPrimitiveTypeKind.DateTimeOffset
            || kind == EdmPrimitiveTypeKind.TimeOfDay || kind == EdmPrimitiveTypeKind.Duration
            || kind == EdmPrimitiveTypeKind.Binary
            || kind.isGeospatial())
            && odata.createPrimitiveTypeInstance(kind).equals(type)) {
          return tokenizer.next(entry.getKey());
        }
      }
      return false;
    }
  }

  protected static List<String> getParameterNames(final List<UriParameter> parameters) {
    List<String> names = new ArrayList<>();
    for (final UriParameter parameter : parameters) {
      names.add(parameter.getName());
    }
    return names;
  }

  protected static EdmStructuredType parseTypeCast(UriTokenizer tokenizer, final Edm edm,
      final EdmStructuredType referencedType) throws UriParserException {
    if (tokenizer.next(TokenKind.QualifiedName)) {
      final FullQualifiedName qualifiedName = new FullQualifiedName(tokenizer.getText());
      final EdmStructuredType type = referencedType.getKind() == EdmTypeKind.ENTITY ?
          edm.getEntityType(qualifiedName) :
          edm.getComplexType(qualifiedName);
      if (type == null) {
        throw new UriParserSemanticException("Type '" + qualifiedName + "' not found.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, qualifiedName.getFullQualifiedNameAsString());
      } else {
        if (!type.compatibleTo(referencedType)) {
          throw new UriParserSemanticException("The type cast '" + qualifiedName + "' is not compatible.",
              UriParserSemanticException.MessageKeys.INCOMPATIBLE_TYPE_FILTER, type.getName());
        }
      }
      return type;
    }
    return null;
  }

  protected static EdmType getTypeInformation(final UriResourcePartTyped resourcePart) {
    EdmType type = null;
    if (resourcePart instanceof UriResourceWithKeysImpl lastPartWithKeys) {
      if (lastPartWithKeys.getTypeFilterOnEntry() != null) {
        type = lastPartWithKeys.getTypeFilterOnEntry();
      } else if (lastPartWithKeys.getTypeFilterOnCollection() != null) {
        type = lastPartWithKeys.getTypeFilterOnCollection();
      } else {
        type = lastPartWithKeys.getType();
      }

    } else if (resourcePart instanceof UriResourceTypedImpl lastPartTyped) {
      type = lastPartTyped.getTypeFilter() == null ?
          lastPartTyped.getType() :
          lastPartTyped.getTypeFilter();
    } else {
      type = resourcePart.getType();
    }

    return type;
  }

  protected static int parseNonNegativeInteger(final String optionName, final String optionValue,
      final boolean zeroAllowed) throws UriParserException {
    int value;
    try {
      value = Integer.parseInt(optionValue);
    } catch (final NumberFormatException e) {
      throw new UriParserSyntaxException("Illegal value of '" + optionName + "' option!", e,
          UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
          optionName, optionValue);
    }
    if (value > 0 || value == 0 && zeroAllowed) {
      return value;
    } else {
      throw new UriParserSyntaxException("Illegal value of '" + optionName + "' option!",
          UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
          optionName, optionValue);
    }
  }
  
  protected static void validateFunctionParameterFacets(final EdmFunction function, 
      final List<UriParameter> parameters, Edm edm, Map<String, AliasQueryOption> aliases) 
          throws UriParserException, UriValidationException {
    final Set<String> supplied = new HashSet<>();
    for (UriParameter parameter : parameters) {
      supplied.add(parameter.getName());
      EdmParameter edmParameter = function.getParameter(parameter.getName());
      final EdmType type = edmParameter.getType();
      if (OptionalParameterDefaults.isPrimitiveLike(edmParameter)) {
        final EdmPrimitiveType primitiveType = (EdmPrimitiveType) type;
        String text = null;
        try {
          text = parameter.getAlias() == null ?
              parameter.getText() :
                aliases.containsKey(parameter.getAlias()) ?
                    parseAliasValue(parameter.getAlias(),
                        edmParameter.getType(), edmParameter.isNullable(), edmParameter.isCollection(),
                        edm, type, aliases).getText() : null;
          OptionalParameterDefaults.valueOfUriLiteral(edmParameter, primitiveType, text);
        } catch (final EdmPrimitiveTypeException e) {
          throw new UriValidationException(
              "Invalid value '" + text + "' for parameter " + parameter.getName(), e,
              UriValidationException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, parameter.getName());
        }
      }
    }

    // OData 4.01, Part 1: Protocol section 11.5.4.1.1 -- an optional parameter omitted from the URL
    // takes its Core.OptionalParameter DefaultValue. A default that is not a valid URI literal is
    // rejected here, as bad input to this call (400), rather than surfacing later as a 500.
    for (final String name : function.getParameterNames()) {
      if (supplied.contains(name)) {
        continue;
      }
      final EdmParameter edmParameter = function.getParameter(name);
      final String defaultLiteral = OptionalParameterDefaults.defaultLiteral(edmParameter);
      if (defaultLiteral == null || !OptionalParameterDefaults.isPrimitiveLike(edmParameter)) {
        continue;
      }
      try {
        OptionalParameterDefaults.valueOfUriLiteral(edmParameter,
            (EdmPrimitiveType) edmParameter.getType(), defaultLiteral);
      } catch (final EdmPrimitiveTypeException e) {
        throw new UriValidationException(
            "Invalid default value '" + defaultLiteral + "' for parameter " + name, e,
            UriValidationException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
      }
    }
  }

  /**
   * Resolves a bound function overload, mapping an ambiguous match (OData 4.01, Protocol
   * section 11.5.4.2) to a semantic URI-parser error instead of letting it surface as a 500.
   * Any other {@link org.sitenetsoft.olinguito.commons.api.edm.EdmException} indicates a broken
   * model and is deliberately left to propagate as a server error.
   */
  protected static EdmFunction getBoundFunction(final Edm edm, final FullQualifiedName functionName,
      final FullQualifiedName bindingParameterTypeName, final boolean isBindingParameterCollection,
      final List<String> parameterNames) throws UriParserSemanticException {
    try {
      return edm.getBoundFunction(functionName, bindingParameterTypeName, isBindingParameterCollection,
          parameterNames);
    } catch (final EdmAmbiguousOverloadException e) {
      throw ambiguousFunction(e, functionName.getFullQualifiedNameAsString(), parameterNames);
    }
  }

  /** Resolves an unbound function overload, see {@link #getBoundFunction}. */
  protected static EdmFunction getUnboundFunction(final Edm edm, final FullQualifiedName functionName,
      final List<String> parameterNames) throws UriParserSemanticException {
    try {
      return edm.getUnboundFunction(functionName, parameterNames);
    } catch (final EdmAmbiguousOverloadException e) {
      throw ambiguousFunction(e, functionName.getFullQualifiedNameAsString(), parameterNames);
    }
  }

  /** Resolves a function-import overload, see {@link #getBoundFunction}. */
  protected static EdmFunction getUnboundFunction(final EdmFunctionImport functionImport,
      final List<String> parameterNames) throws UriParserSemanticException {
    try {
      return functionImport.getUnboundFunction(parameterNames);
    } catch (final EdmAmbiguousOverloadException e) {
      throw ambiguousFunction(e, functionImport.getName(), parameterNames);
    }
  }

  private static UriParserSemanticException ambiguousFunction(final EdmAmbiguousOverloadException cause,
      final String functionName, final List<String> parameterNames) {
    return new UriParserSemanticException(
        "Ambiguous function '" + functionName + "' with parameters " + parameterNames + ".", cause,
        UriParserSemanticException.MessageKeys.FUNCTION_AMBIGUOUS, functionName, String.valueOf(parameterNames));
  }
}
