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
 * Copyright 2026 SiteNetSoft - Modernized instanceof to pattern matching
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.EdmKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmString;
import org.sitenetsoft.olinguito.server.api.uri.UriParameter;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePartTyped;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceActionImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceComplexPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceCountImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceEntitySetImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceFunctionImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceNavigationPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourcePrimitivePropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceRefImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceSingletonImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceTypedImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceValueImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceWithKeysImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriParameterImpl;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriTokenizer.TokenKind;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

public class ResourcePathParser {

  private final Edm edm;
  private final EdmEntityContainer edmEntityContainer;
  private final Map<String, AliasQueryOption> aliases;
  private UriTokenizer tokenizer;

  public ResourcePathParser(final Edm edm, final Map<String, AliasQueryOption> aliases) {
    this.edm = edm;
    this.aliases = aliases;
    edmEntityContainer = edm.getEntityContainer();
  }

  public UriResource parsePathSegment(final String pathSegment, UriResource previous)
      throws UriParserException, UriValidationException {
    tokenizer = new UriTokenizer(pathSegment);

    // The order is important.
    // A qualified name should not be parsed as identifier and let the tokenizer halt at '.'.

    if (previous == null) {
      if (tokenizer.next(TokenKind.QualifiedName)) {
        throw new UriParserSemanticException("The initial segment must not be namespace-qualified.",
            UriParserSemanticException.MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT,
            new FullQualifiedName(tokenizer.getText()).getNamespace());
      } else if (tokenizer.next(TokenKind.ODataIdentifier)) {
        return leadingResourcePathSegment();
      }

    } else {
      try {
        if (tokenizer.next(TokenKind.REF)) {
          return ref(previous);
        } else if (tokenizer.next(TokenKind.VALUE)) {
          return value(previous);
        } else if (tokenizer.next(TokenKind.COUNT)) {
          return count(previous);
        } else if (tokenizer.next(TokenKind.QualifiedName)) {
          return boundOperationOrTypeCast(previous);
        } else if (tokenizer.next(TokenKind.ODataIdentifier)) {
          return navigationOrProperty(previous);
        } else if (canParseKeyFromSegment(pathSegment, previous)) {
          /*
           * This should only be reached if the path segment is an integer. If it can be parsed as a key segment,
           * null is returned so that this portion of the path is skipped.
           */
          return null;
        }
      } catch (UriParserException e) {
        /*
         * This exception will be caught if the path segment is a string. It will first try to be parsed as a navigation
         * or property. That will fail if the path segment is not part of the schema. If it can be parsed as a key
         * segment, null is returned so that this portion of the path is skipped. If it can not be parsed as a key
         * segment, then the original exception is re-thrown.
         */
        if (canParseKeyFromSegment(pathSegment,previous)) {
          return null;
        }
        throw e;
      }
    }

    throw new UriParserSyntaxException("Unexpected start of resource-path segment.",
        UriParserSyntaxException.MessageKeys.SYNTAX);
  }

  public EdmEntityType parseDollarEntityTypeCast(final String pathSegment) throws UriParserException {
    tokenizer = new UriTokenizer(pathSegment);
    ParserHelper.requireNext(tokenizer, TokenKind.QualifiedName);
    final String name = tokenizer.getText();
    ParserHelper.requireTokenEnd(tokenizer);
    final EdmEntityType type = edm.getEntityType(new FullQualifiedName(name));
    if (type == null) {
      throw new UriParserSemanticException("Type '" + name + "' not found.",
          UriParserSemanticException.MessageKeys.UNKNOWN_TYPE, name);
    }
    return type;
  }

  public List<String> parseCrossjoinSegment(final String pathSegment) throws UriParserException {
    tokenizer = new UriTokenizer(pathSegment);
    ParserHelper.requireNext(tokenizer, TokenKind.CROSSJOIN);
    ParserHelper.requireNext(tokenizer, TokenKind.OPEN);
    // At least one entity-set name is mandatory. Try to fetch all.
    List<String> entitySetNames = new ArrayList<>();
    do {
      ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
      final String name = tokenizer.getText();
      final EdmEntitySet edmEntitySet = edmEntityContainer.getEntitySet(name);
      if (edmEntitySet == null) {
        throw new UriParserSemanticException("Expected Entity Set Name.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, name);
      } else {
        entitySetNames.add(name);
      }
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    ParserHelper.requireTokenEnd(tokenizer);
    return entitySetNames;
  }

  /*
   * This logic is to handle our use case of supporting keys as segments.
   * If all the existing parsing fails, then we check if the previous segment is an entity set, or navigation.
   * If it is, and it has the flag set allow keys as segments, then we check if the entity set already has
   * the key parameter set. If it does, then we return the original parse exception (ie /entitySet('key')/un-parseable).
   * If there is no key set, then we pass the current segment back to the previous segment as the key.
   * With this logic, /entitySet('key') and /entitySet/key should be equivalent.
   */
  private boolean canParseKeyFromSegment(final String pathSegment, UriResource previous) {
    if (previous instanceof UriResourceEntitySetImpl entitySet) {
      if (entitySet.getEntitySet().isKeyAsSegmentAllowed()) {
        return didSetKeySegmentAsKey(pathSegment, entitySet, entitySet.getEntitySet().getEntityType());
      }
    } else if (previous instanceof UriResourceNavigationPropertyImpl navProp) {
      EdmNavigationProperty edmNavProperty = navProp.getProperty();
      if (edmNavProperty.isKeyAsSegmentAllowed()) {
        return didSetKeySegmentAsKey(pathSegment, navProp, edmNavProperty.getType());
      }
    }
    return false;
  }

  private UriResource ref(final UriResource previous) throws UriParserException {
    ParserHelper.requireTokenEnd(tokenizer);
    requireTyped(previous, "$ref");
    if (previous instanceof UriResourcePartTyped prevTyped
        && prevTyped.getType() instanceof EdmEntityType) {
      return new UriResourceRefImpl();
    } else {
      throw new UriParserSemanticException("$ref is only allowed on entity types.",
          UriParserSemanticException.MessageKeys.ONLY_FOR_ENTITY_TYPES, "$ref");
    }
  }

  private UriResource value(final UriResource previous) throws UriParserException {
    ParserHelper.requireTokenEnd(tokenizer);
    requireTyped(previous, "$value");
    if (previous instanceof UriResourcePartTyped prevTyped && !prevTyped.isCollection()) {
      requireMediaResourceInCaseOfEntity(previous);
      return new UriResourceValueImpl();
    } else {
      throw new UriParserSemanticException("$value is only allowed on typed path segments.",
          UriParserSemanticException.MessageKeys.ONLY_FOR_TYPED_PARTS, "$value");
    }
  }

  private void requireMediaResourceInCaseOfEntity(UriResource resource) throws UriParserSemanticException {
    // If the resource is an entity or navigatio
    if (resource instanceof UriResourceEntitySet resEntitySet && !resEntitySet.getEntityType().hasStream()
        || resource instanceof UriResourceNavigation resNav
        && resNav.getType() instanceof EdmEntityType navEntityType && !navEntityType.hasStream()) {
      throw new UriParserSemanticException("$value on entity is only allowed on media resources.",
          UriParserSemanticException.MessageKeys.NOT_A_MEDIA_RESOURCE, resource.getSegmentValue());
    }

    // Functions can also deliver an entity. In this case we have to check if the returned entity is a media resource
    if (resource instanceof UriResourceFunction resFunc) {
      EdmType returnType = resFunc.getFunction().getReturnType().getType();
      //Collection check is above so not needed here
      if (returnType instanceof EdmEntityType edmEntityType && !edmEntityType.hasStream()) {
        throw new UriParserSemanticException("$value on returned entity is only allowed on media resources.",
            UriParserSemanticException.MessageKeys.NOT_A_MEDIA_RESOURCE, resource.getSegmentValue());
      }
    }
  }

  private UriResource count(final UriResource previous) throws UriParserException {
    ParserHelper.requireTokenEnd(tokenizer);
    requireTyped(previous, "$count");
    if (previous instanceof UriResourcePartTyped prevTyped && prevTyped.isCollection()) {
      return new UriResourceCountImpl();
    } else {
      throw new UriParserSemanticException("$count is only allowed on collections.",
          UriParserSemanticException.MessageKeys.ONLY_FOR_COLLECTIONS, "$count");
    }
  }

  private UriResource leadingResourcePathSegment() throws UriParserException, UriValidationException {
    final String oDataIdentifier = tokenizer.getText();

    final EdmEntitySet edmEntitySet = edmEntityContainer.getEntitySet(oDataIdentifier);
    if (edmEntitySet != null) {
      final UriResourceEntitySetImpl entitySetResource = new UriResourceEntitySetImpl(edmEntitySet);

      if (tokenizer.next(TokenKind.OPEN)) {
        final List<UriParameter> keyPredicates =
            ParserHelper.parseKeyPredicate(tokenizer, entitySetResource.getEntityType(), null, edm, null, aliases);
        entitySetResource.setKeyPredicates(keyPredicates);
      }

      ParserHelper.requireTokenEnd(tokenizer);
      return entitySetResource;
    }

    final EdmSingleton edmSingleton = edmEntityContainer.getSingleton(oDataIdentifier);
    if (edmSingleton != null) {
      ParserHelper.requireTokenEnd(tokenizer);
      return new UriResourceSingletonImpl(edmSingleton);
    }

    final EdmActionImport edmActionImport = edmEntityContainer.getActionImport(oDataIdentifier);
    if (edmActionImport != null) {
      ParserHelper.requireTokenEnd(tokenizer);
      return new UriResourceActionImpl(edmActionImport);
    }

    final EdmFunctionImport edmFunctionImport = edmEntityContainer.getFunctionImport(oDataIdentifier);
    if (edmFunctionImport != null) {
      return functionCall(edmFunctionImport, null, null, false);
    }

    if (tokenizer.next(TokenKind.OPEN) || tokenizer.next(TokenKind.EOF)) {
      throw new UriParserSemanticException("Unexpected start of resource-path segment.",
          UriParserSemanticException.MessageKeys.RESOURCE_NOT_FOUND, oDataIdentifier);
    } else {
      throw new UriParserSyntaxException("Unexpected start of resource-path segment.",
          UriParserSyntaxException.MessageKeys.SYNTAX);
    }
  }

  private UriResource navigationOrProperty(final UriResource previous)
      throws UriParserException, UriValidationException {
    final String name = tokenizer.getText();

    UriResourcePartTyped previousTyped = null;
    EdmStructuredType structType = null;
    requireTyped(previous, name);
    if (previous instanceof UriResourcePartTyped prevTyped
        && prevTyped.getType() instanceof EdmStructuredType prevStructType) {
      previousTyped = prevTyped;
      final EdmType previousTypeFilter = getPreviousTypeFilter(previousTyped);
      structType = previousTypeFilter instanceof EdmStructuredType filterStructType ? filterStructType : prevStructType;
    } else {
      throw new UriParserSemanticException(
          "Cannot parse '" + name + "'; previous path segment is not a structural type.",
          UriParserSemanticException.MessageKeys.RESOURCE_PART_MUST_BE_PRECEDED_BY_STRUCTURAL_TYPE, name);
    }

    if (previousTyped.isCollection()) {
      throw new UriParserSemanticException("Property '" + name + "' is not allowed after collection.",
          UriParserSemanticException.MessageKeys.PROPERTY_AFTER_COLLECTION, name);
    }

    final EdmProperty property = structType.getStructuralProperty(name);
    if (property != null) {
      return property.isPrimitive()
          || property.getType().getKind() == EdmTypeKind.ENUM
          || property.getType().getKind() == EdmTypeKind.DEFINITION ?
          new UriResourcePrimitivePropertyImpl(property) :
          new UriResourceComplexPropertyImpl(property);
    }
    final EdmNavigationProperty navigationProperty = structType.getNavigationProperty(name);
    if (navigationProperty == null) {
      throw new UriParserSemanticException("Property '" + name + "' not found in type '"
          + structType.getFullQualifiedName().getFullQualifiedNameAsString() + "'",
          UriParserSemanticException.MessageKeys.PROPERTY_NOT_IN_TYPE,
          structType.getFullQualifiedName().getFullQualifiedNameAsString(), name);
    }
    List<UriParameter> keyPredicate =
        ParserHelper.parseNavigationKeyPredicate(tokenizer, navigationProperty, edm, null, aliases);
    ParserHelper.requireTokenEnd(tokenizer);
    return new UriResourceNavigationPropertyImpl(navigationProperty)
        .setKeyPredicates(keyPredicate);
  }

  private UriResource boundOperationOrTypeCast(UriResource previous)
      throws UriParserException, UriValidationException {
    final FullQualifiedName name = new FullQualifiedName(tokenizer.getText());
    requireTyped(previous, name.getFullQualifiedNameAsString());
    final UriResourcePartTyped previousTyped = (UriResourcePartTyped) previous;
    final EdmType previousTypeFilter = getPreviousTypeFilter(previousTyped);
    final EdmType previousType = previousTypeFilter == null ? previousTyped.getType() : previousTypeFilter;

    // We check for bound actions first because they cannot be followed by anything.
    final EdmAction boundAction =
        edm.getBoundAction(name, previousType.getFullQualifiedName(), previousTyped.isCollection());
    if (boundAction != null) {
      ParserHelper.requireTokenEnd(tokenizer);
      return new UriResourceActionImpl(boundAction);
    }

    // Type casts can be syntactically indistinguishable from bound function calls in the case of additional keys.
    // But normally they are shorter, so they come next.
    final EdmStructuredType type = previousTyped.getType() instanceof EdmEntityType ?
        edm.getEntityType(name) :
        edm.getComplexType(name);
    if (type != null) {
      return typeCast(name, type, previousTyped);
    }
    if (tokenizer.next(TokenKind.EOF)) {
      throw new UriParserSemanticException("Type '" + name.getFullQualifiedNameAsString() + "' not found.",
          UriParserSemanticException.MessageKeys.UNKNOWN_TYPE, name.getFullQualifiedNameAsString());
    }

    // Now a bound function call is the only remaining option.
    return functionCall(null, name, previousType.getFullQualifiedName(), previousTyped.isCollection());
  }

  private void requireTyped(final UriResource previous, final String forWhat) throws UriParserException {
    if (!(previous instanceof UriResourcePartTyped)) {
      throw new UriParserSemanticException("Path segment before '" + forWhat + "' is not typed.",
          UriParserSemanticException.MessageKeys.PREVIOUS_PART_NOT_TYPED, forWhat);
    }
  }

  private UriResource typeCast(final FullQualifiedName name, final EdmStructuredType type,
      final UriResourcePartTyped previousTyped) throws UriParserException, UriValidationException {
    if (type.compatibleTo(previousTyped.getType())) {
      EdmType previousTypeFilter = null;
      if (previousTyped instanceof UriResourceWithKeysImpl withKeysImpl) {
        if (previousTyped.isCollection()) {
          previousTypeFilter = withKeysImpl.getTypeFilterOnCollection();
          if (previousTypeFilter != null) {
            throw new UriParserSemanticException("Type filters are not chainable.",
                UriParserSemanticException.MessageKeys.TYPE_FILTER_NOT_CHAINABLE,
                previousTypeFilter.getName(), type.getName());
          }
          withKeysImpl.setCollectionTypeFilter(type);
        } else {
          previousTypeFilter = withKeysImpl.getTypeFilterOnEntry();
          if (previousTypeFilter != null) {
            throw new UriParserSemanticException("Type filters are not chainable.",
                UriParserSemanticException.MessageKeys.TYPE_FILTER_NOT_CHAINABLE,
                previousTypeFilter.getName(), type.getName());
          }
          withKeysImpl.setEntryTypeFilter(type);
        }
        if (tokenizer.next(TokenKind.OPEN)) {
          final List<UriParameter> keys =
              ParserHelper.parseKeyPredicate(tokenizer, (EdmEntityType) type, null, edm, null, aliases);
          if (previousTyped.isCollection()) {
            withKeysImpl.setKeyPredicates(keys);
          } else {
            throw new UriParserSemanticException("Key not allowed here.",
                UriParserSemanticException.MessageKeys.KEY_NOT_ALLOWED);
          }
        }
      } else if (previousTyped instanceof UriResourceTypedImpl typedImpl) {
        previousTypeFilter = typedImpl.getTypeFilter();
        if (previousTypeFilter != null) {
          throw new UriParserSemanticException("Type filters are not chainable.",
              UriParserSemanticException.MessageKeys.TYPE_FILTER_NOT_CHAINABLE,
              previousTypeFilter.getName(), type.getName());
        }
        typedImpl.setTypeFilter(type);
      }
      ParserHelper.requireTokenEnd(tokenizer);
      return null;
    } else {
      throw new UriParserSemanticException(
          "Type filter not compatible to previous path segment: " + name.getFullQualifiedNameAsString(),
          UriParserSemanticException.MessageKeys.INCOMPATIBLE_TYPE_FILTER, name.getFullQualifiedNameAsString());
    }
  }

  private EdmType getPreviousTypeFilter(final UriResourcePartTyped previousTyped) {
    if (previousTyped instanceof UriResourceWithKeysImpl withKeysImpl) {
      return withKeysImpl.getTypeFilterOnEntry() == null ?
          withKeysImpl.getTypeFilterOnCollection() :
          withKeysImpl.getTypeFilterOnEntry();
    } else if (previousTyped instanceof UriResourceTypedImpl typedImpl) {
      return typedImpl.getTypeFilter();
    } else {
      return null;
    }
  }

  private UriResource functionCall(final EdmFunctionImport edmFunctionImport,
      final FullQualifiedName boundFunctionName, final FullQualifiedName bindingParameterTypeName,
      final boolean isBindingParameterCollection) throws UriParserException, UriValidationException {
    final List<UriParameter> parameters = ParserHelper.parseFunctionParameters(tokenizer, edm, null, false, aliases);
    final List<String> names = ParserHelper.getParameterNames(parameters);
    EdmFunction function = null;
    if (edmFunctionImport != null) {
      function = edmFunctionImport.getUnboundFunction(names);
      if (function == null) {
        throw new UriParserSemanticException(
            "Function of function import '" + edmFunctionImport.getName() + "' "
                + "with parameters " + names.toString() + " not found.",
            UriParserSemanticException.MessageKeys.FUNCTION_NOT_FOUND, edmFunctionImport.getName(), names.toString());
      }
    } else {
      function = edm.getBoundFunction(boundFunctionName,
          bindingParameterTypeName, isBindingParameterCollection, names);
      if (function == null) {
        throw new UriParserSemanticException(
            "Function " + boundFunctionName + " not found.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, boundFunctionName.getFullQualifiedNameAsString());
      }
    }
    ParserHelper.validateFunctionParameters(function, parameters, edm, null, aliases);
    ParserHelper.validateFunctionParameterFacets(function, parameters, edm, aliases);
    UriResourceFunctionImpl resource = new UriResourceFunctionImpl(edmFunctionImport, function, parameters);
    if (tokenizer.next(TokenKind.OPEN)) {
      if (function.getReturnType() != null
          && function.getReturnType().getType().getKind() == EdmTypeKind.ENTITY
          && function.getReturnType().isCollection()) {
        resource.setKeyPredicates(
            ParserHelper.parseKeyPredicate(tokenizer,
                (EdmEntityType) function.getReturnType().getType(), null, edm, null, aliases));
      } else {
        throw new UriParserSemanticException("A key is not allowed.",
            UriParserSemanticException.MessageKeys.KEY_NOT_ALLOWED);
      }
    }
    ParserHelper.requireTokenEnd(tokenizer);
    return resource;
  }

  private boolean didSetKeySegmentAsKey(
    final String pathSegment,
    UriResourceWithKeysImpl resourceWithKeys,
    EdmEntityType entityType) {
    List<EdmKeyPropertyRef> keys = entityType.getKeyPropertyRefs();
    if (keys != null && keys.size() == 1) {
      // Currently only supports types with a single key.
      EdmKeyPropertyRef propertyRef = keys.get(0);
      String keyName = propertyRef.getName();
      List<UriParameter> parameterList = new ArrayList<>(resourceWithKeys.getKeyPredicates());
      if (parameterList.stream().noneMatch(u -> keyName.equals(u.getName()))) {
        String value = pathSegment;
        if (propertyRef.getProperty().getType() instanceof EdmString) {
          value = "'" + value + "'";
        }
        parameterList.add(new UriParameterImpl().setName(keyName).setText(value));
        resourceWithKeys.setKeyPredicates(parameterList);
        return true;
      }
    }

    return false;
  }
}
