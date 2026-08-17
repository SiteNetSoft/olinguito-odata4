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
 * Copyright 2026 SiteNetSoft - Resolve dynamic path segments on open types
 * Copyright 2026 SiteNetSoft - OData 4.01: map ambiguous optional-parameter overloads to a 400 response
 * Copyright 2026 SiteNetSoft - OData 4.01: key-as-segment URL convention (URL Conventions section 4.3.6)
 * Copyright 2026 SiteNetSoft - Kept fallback semantics for the model flag on single-valued navigation
 * Copyright 2026 SiteNetSoft - OData 4.01: referential-constraint omission and default namespaces in
 * key-as-segment URLs
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSchema;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.server.api.uri.UriParameter;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePartTyped;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.core.uri.UriParameterImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceActionImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceComplexPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceCountImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceDynamicPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceEntitySetImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceFunctionImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceNavigationPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourcePrimitivePropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceRefImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceSingletonImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceTypedImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceValueImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceWithKeysImpl;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriTokenizer.TokenKind;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

public class ResourcePathParser {

  /** Term marking a schema whose names may be used unqualified (URL Conventions 4.3.6). */
  private static final String DEFAULT_NAMESPACE_TERM = "Org.OData.Core.V1.DefaultNamespace";
  /** The same term written with the usual alias of the core vocabulary. */
  private static final String DEFAULT_NAMESPACE_TERM_ALIAS = "Core.DefaultNamespace";

  private final Edm edm;
  private final EdmEntityContainer edmEntityContainer;
  private final Map<String, AliasQueryOption> aliases;
  private final boolean keyAsSegment;
  private UriTokenizer tokenizer;

  /** Resource whose multi-part key is currently being collected from consecutive path segments. */
  private UriResourceWithKeysImpl pendingKeyResource;
  /** Entity type whose key is addressed with segments. */
  private EdmEntityType pendingKeyType;
  /** Names of all key predicates expected for {@link #pendingKeyResource}, in metadata order. */
  private List<String> pendingKeyNames;
  /** Key values collected so far for {@link #pendingKeyResource}. */
  private List<UriParameter> pendingKeyPredicates;
  /** Key properties of {@link #pendingKeyType} that are determined by referential constraints. */
  private Map<String, String> pendingReferencedKeyNames;

  public ResourcePathParser(final Edm edm, final Map<String, AliasQueryOption> aliases) {
    this(edm, aliases, false);
  }

  public ResourcePathParser(final Edm edm, final Map<String, AliasQueryOption> aliases,
      final boolean keyAsSegment) {
    this.edm = edm;
    this.aliases = aliases;
    this.keyAsSegment = keyAsSegment;
    edmEntityContainer = edm.getEntityContainer();
  }

  public UriResource parsePathSegment(final String pathSegment, UriResource previous)
      throws UriParserException, UriValidationException {
    if (pendingKeyResource != null) {
      // A multi-part key is being collected: this segment is the next key value
      // (URL Conventions 4.3.6: one segment per key value, in metadata key order).
      consumeKeySegment(pathSegment);
      return null;
    }

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
      // Precedence rules of URL Conventions 4.3.6 for a segment following an entity collection:
      // 1. a defined OData segment (starting with '$'), 2. a qualified bound operation or type name,
      // 3. an unqualified bound operation or type name in a default namespace, 4. a key value.
      if (tokenizer.next(TokenKind.REF)) {
        return ref(previous);
      } else if (tokenizer.next(TokenKind.VALUE)) {
        return value(previous);
      } else if (tokenizer.next(TokenKind.COUNT)) {
        return count(previous);
      } else if (tokenizer.next(TokenKind.QualifiedName)) {
        return boundOperationOrTypeCast(previous);
      } else if (!pathSegment.startsWith("$") && isKeyAsSegmentTarget(previous)) {
        final String defaultNamespace = resolveDefaultNamespace(pathSegment, previous);
        if (defaultNamespace != null) {
          // Re-parse the segment with the namespace prefixed: from here on it is an ordinary qualified name.
          tokenizer = new UriTokenizer(defaultNamespace + '.' + pathSegment);
          ParserHelper.requireNext(tokenizer, TokenKind.QualifiedName);
          return boundOperationOrTypeCast(previous);
        }
        startKeySegments(pathSegment, (UriResourceWithKeysImpl) previous);
        return null;
      } else if (tokenizer.next(TokenKind.ODataIdentifier)) {
        if (isKeyAsSegmentFallbackTarget(previous) && !isStructuralMemberName(previous, tokenizer.getText())) {
          startKeySegments(pathSegment, (UriResourceWithKeysImpl) previous);
          return null;
        }
        return navigationOrProperty(previous);
      } else if (!pathSegment.startsWith("$") && isKeyAsSegmentFallbackTarget(previous)) {
        startKeySegments(pathSegment, (UriResourceWithKeysImpl) previous);
        return null;
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

  /**
   * Determines whether a segment following the given resource has to be resolved as a key value
   * (URL Conventions 4.3.6). This is the case for an entity collection that has not been addressed
   * with a key yet, if the convention is enabled for the whole service or the (non-standard)
   * model flag opts the entity set or navigation property in.
   * @param previous the preceding resource-path segment
   * @return whether key-as-segment resolution applies
   */
  private boolean isKeyAsSegmentTarget(final UriResource previous) {
    if (previous instanceof UriResourceEntitySetImpl entitySet) {
      return entitySet.getKeyPredicates().isEmpty()
          && (keyAsSegment || entitySet.getEntitySet().isKeyAsSegmentAllowed());
    } else if (previous instanceof UriResourceNavigationPropertyImpl navigation) {
      final EdmNavigationProperty navigationProperty = navigation.getProperty();
      return navigation.getKeyPredicates().isEmpty() && navigation.isCollection()
          && (keyAsSegment || navigationProperty.isKeyAsSegmentAllowed());
    }
    return false;
  }

  /**
   * Determines whether a segment following the given resource may be resolved as a key value only if it
   * does not name a property or navigation property. This applies to the (non-standard) model flag on a
   * single-valued navigation property, where the previous behaviour of this fork was to try the key
   * interpretation only after property resolution had failed.
   * @param previous the preceding resource-path segment
   * @return whether key-as-segment resolution applies as a fallback
   */
  private boolean isKeyAsSegmentFallbackTarget(final UriResource previous) {
    return previous instanceof UriResourceNavigationPropertyImpl navigation
        && !navigation.isCollection()
        && navigation.getKeyPredicates().isEmpty()
        && navigation.getProperty().isKeyAsSegmentAllowed();
  }

  /**
   * Determines whether the given name is a structural or navigation property of the type addressed by the
   * given resource (an open type accepts every name as a dynamic property).
   * @param previous the preceding resource-path segment
   * @param name the name to look up
   * @return whether the name is a member of the addressed type
   */
  private boolean isStructuralMemberName(final UriResource previous, final String name) {
    if (previous instanceof UriResourcePartTyped previousTyped
        && previousTyped.getType() instanceof EdmStructuredType previousStructuredType) {
      final EdmType typeFilter = getPreviousTypeFilter(previousTyped);
      final EdmStructuredType type = typeFilter instanceof EdmStructuredType filteredType ?
          filteredType :
          previousStructuredType;
      return type.getStructuralProperty(name) != null
          || type.getNavigationProperty(name) != null
          || type.isOpenType();
    }
    return false;
  }

  /**
   * Determines whether a segment is an unqualified bound-operation or type name defined in a default
   * namespace (URL Conventions 4.3.6 rule 3). A schema is a default namespace if it is annotated with
   * the term <code>Org.OData.Core.V1.DefaultNamespace</code>; the annotation is matched by its raw term
   * name (also accepting the usual alias <code>Core.DefaultNamespace</code>) because that term does not
   * have to be part of the vocabularies served by this library.
   * @param pathSegment the current path segment
   * @param previous the preceding resource-path segment
   * @return the namespace the name belongs to, or {@code null} if the segment is not such a name
   */
  private String resolveDefaultNamespace(final String pathSegment, final UriResource previous) {
    if (!(previous instanceof UriResourcePartTyped previousTyped)) {
      return null;
    }
    final UriTokenizer nameTokenizer = new UriTokenizer(pathSegment);
    if (!nameTokenizer.next(TokenKind.ODataIdentifier)) {
      return null;
    }
    final String name = nameTokenizer.getText();
    if (!nameTokenizer.next(TokenKind.EOF) && !nameTokenizer.next(TokenKind.OPEN)) {
      return null;
    }
    final EdmType previousTypeFilter = getPreviousTypeFilter(previousTyped);
    final EdmType previousType = previousTypeFilter == null ? previousTyped.getType() : previousTypeFilter;
    for (final EdmSchema schema : edm.getSchemas()) {
      if (isDefaultNamespace(schema)
          && isBoundOperationOrTypeName(new FullQualifiedName(schema.getNamespace(), name),
              previousType, previousTyped.isCollection())) {
        return schema.getNamespace();
      }
    }
    return null;
  }

  /** Determines whether the given schema is annotated as a default namespace. */
  private boolean isDefaultNamespace(final EdmSchema schema) {
    for (final EdmAnnotation annotation : schema.getAnnotations()) {
      final String term = annotation.getTermName();
      if (DEFAULT_NAMESPACE_TERM.equals(term) || DEFAULT_NAMESPACE_TERM_ALIAS.equals(term)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines whether the given name is the name of a bound action, of an entity type, or of a bound
   * function that can follow the given previous type, tried in the same order as for qualified names.
   * (The previous segment is an entity collection, so only entity types can be meant.)
   */
  private boolean isBoundOperationOrTypeName(final FullQualifiedName name, final EdmType previousType,
      final boolean isCollection) {
    if (edm.getBoundAction(name, previousType.getFullQualifiedName(), isCollection) != null) {
      return true;
    }
    if (edm.getEntityType(name) != null) {
      return true;
    }
    for (final EdmFunction function :
        edm.getBoundFunctionsWithBindingType(previousType.getFullQualifiedName(), isCollection)) {
      if (name.equals(function.getFullQualifiedName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Starts to resolve path segments as the key of the given resource. The first key value is the
   * current segment; if the entity type has a multi-part key, the following segments provide the
   * remaining key values, one segment per key value in metadata key order.
   * @param pathSegment the segment holding the first key value
   * @param resource the entity collection to be addressed
   */
  private void startKeySegments(final String pathSegment, final UriResourceWithKeysImpl resource)
      throws UriParserException, UriValidationException {
    pendingKeyResource = resource;
    pendingKeyType = keySegmentEntityType(resource);
    final EdmNavigationProperty partner = resource instanceof UriResourceNavigationPropertyImpl navigation ?
        navigation.getProperty().getPartner() :
        null;
    pendingReferencedKeyNames = ParserHelper.referencedKeyNames(pendingKeyType, partner);
    // The key-predicate names are the only source of the metadata key order (and carry key aliases).
    // Key values that are determined by referential constraints are not given in the URI.
    pendingKeyNames = new ArrayList<>();
    for (final String name : pendingKeyType.getKeyPredicateNames()) {
      if (!pendingReferencedKeyNames.containsKey(name)) {
        pendingKeyNames.add(name);
      }
    }
    pendingKeyPredicates = new ArrayList<>();
    if (pendingKeyNames.isEmpty()) {
      // All key values are determined by referential constraints, so this segment cannot be a key value.
      clearKeySegments();
      throw new UriParserSemanticException("There are too many key properties.",
          UriParserSemanticException.MessageKeys.WRONG_NUMBER_OF_KEY_PROPERTIES, "0", "1");
    }
    consumeKeySegment(pathSegment);
  }

  /** Consumes one path segment as the next key value of the resource currently being addressed. */
  private void consumeKeySegment(final String pathSegment) throws UriParserException, UriValidationException {
    if (pathSegment.startsWith("$")) {
      // Defined OData segments never are key values (precedence rule 1), so the key stays incomplete.
      throw new UriParserSemanticException("A key value must not start with '$'.",
          UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE, pathSegment);
    }
    pendingKeyPredicates.add(keySegmentParameter(pendingKeyNames.get(pendingKeyPredicates.size()), pathSegment));
    if (pendingKeyPredicates.size() == pendingKeyNames.size()) {
      pendingKeyResource.setKeyPredicates(allKeyPredicates());
      clearKeySegments();
    }
  }

  /**
   * Combines the key values given as segments with the key values determined by referential constraints,
   * in metadata key order.
   */
  private List<UriParameter> allKeyPredicates() {
    if (pendingReferencedKeyNames.isEmpty()) {
      return pendingKeyPredicates;
    }
    final List<UriParameter> keys = new ArrayList<>();
    int given = 0;
    for (final String name : pendingKeyType.getKeyPredicateNames()) {
      final String referencedName = pendingReferencedKeyNames.get(name);
      if (referencedName == null) {
        keys.add(pendingKeyPredicates.get(given++));
      } else {
        keys.add(new UriParameterImpl().setName(name).setReferencedProperty(referencedName));
      }
    }
    return keys;
  }

  /**
   * Determines the entity type whose key is addressed with segments, taking a preceding type filter
   * on the collection into account.
   */
  private EdmEntityType keySegmentEntityType(final UriResourceWithKeysImpl resource) {
    final EdmType typeFilter = getPreviousTypeFilter(resource);
    return typeFilter instanceof EdmEntityType filterEntityType ?
        filterEntityType :
        (EdmEntityType) resource.getType();
  }

  /**
   * Converts one path segment into a key predicate. The segment text is already percent-decoded;
   * single quotes in it are part of the key value (URL Conventions 4.3.6), therefore the text is
   * converted into its URI-literal representation before the usual key-value validation is applied.
   */
  private UriParameter keySegmentParameter(final String keyPredicateName, final String pathSegment)
      throws UriParserException, UriValidationException {
    final EdmProperty property = pendingKeyType.getKeyPropertyRef(keyPredicateName).getProperty();
    final EdmPrimitiveType type = (EdmPrimitiveType) property.getType();
    final String literal = type.toUriLiteral(pathSegment);
    final UriTokenizer keyTokenizer = new UriTokenizer(literal);
    if (!ParserHelper.nextPrimitiveTypeValue(keyTokenizer, type, property.isNullable())
        || !keyTokenizer.next(TokenKind.EOF)) {
      throw new UriParserSemanticException(keyPredicateName + " has not a valid key value.",
          UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE, keyPredicateName);
    }
    return ParserHelper.createUriParameter(property, keyPredicateName, literal, edm, null, aliases);
  }

  /**
   * Requires that no multi-part key is left half-way addressed at the end of the resource path.
   * @throws UriParserSemanticException if key values are missing
   */
  public void requireCompleteKeySegments() throws UriParserSemanticException {
    if (pendingKeyResource != null) {
      final int expected = pendingKeyNames.size();
      final int given = pendingKeyPredicates.size();
      clearKeySegments();
      throw new UriParserSemanticException("There are too few key properties.",
          UriParserSemanticException.MessageKeys.WRONG_NUMBER_OF_KEY_PROPERTIES,
          String.valueOf(expected), String.valueOf(given));
    }
  }

  private void clearKeySegments() {
    pendingKeyResource = null;
    pendingKeyType = null;
    pendingKeyNames = null;
    pendingKeyPredicates = null;
    pendingReferencedKeyNames = null;
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
      if (structType.isOpenType()) {
        ParserHelper.requireTokenEnd(tokenizer);
        return new UriResourceDynamicPropertyImpl(name);
      }
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
      function = ParserHelper.getUnboundFunction(edmFunctionImport, names);
      if (function == null) {
        throw new UriParserSemanticException(
            "Function of function import '" + edmFunctionImport.getName() + "' "
                + "with parameters " + names.toString() + " not found.",
            UriParserSemanticException.MessageKeys.FUNCTION_NOT_FOUND, edmFunctionImport.getName(), names.toString());
      }
    } else {
      function = ParserHelper.getBoundFunction(edm, boundFunctionName,
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
}
