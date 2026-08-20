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
 * Copyright 2026 SiteNetSoft - Added a CSDL JSON metadata parser for the structural model
 * Copyright 2026 SiteNetSoft - Delegated the operations and the entity container to a collaborator
 * Copyright 2026 SiteNetSoft - Delegated the annotations and their expressions to a collaborator
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: kept the collection navigation property $Nullable prohibition
 */
package org.sitenetsoft.olinguito.server.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumMember;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDelete;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDeleteAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReferentialConstraint;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlStructuralType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceInclude;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceIncludeAnnotation;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;

/**
 * Converts a CSDL JSON document (OData CSDL JSON, OASIS) into the same {@link SchemaBasedEdmProvider}
 * the CSDL XML {@link MetadataParser} produces. This class covers the document object, references,
 * schemas and the structural model; operations, the entity container, annotations and annotation
 * expressions are read by the members added on top of it.
 */
public class MetadataJsonParser {

  static final String DOLLAR = "$";
  static final String AT = "@";
  static final String VERSION = DOLLAR + "Version";
  static final String REFERENCES = DOLLAR + "Reference";
  static final String INCLUDE = DOLLAR + "Include";
  static final String NAMESPACE = DOLLAR + "Namespace";
  static final String ALIAS = DOLLAR + "Alias";
  static final String INCLUDE_ANNOTATIONS = DOLLAR + "IncludeAnnotations";
  static final String TERM_NAMESPACE = DOLLAR + "TermNamespace";
  static final String TARGET_NAMESPACE = DOLLAR + "TargetNamespace";
  static final String QUALIFIER = DOLLAR + "Qualifier";
  static final String IS_FLAGS = DOLLAR + "IsFlags";
  static final String UNDERLYING_TYPE = DOLLAR + "UnderlyingType";
  static final String KIND = DOLLAR + "Kind";
  static final String MAX_LENGTH = DOLLAR + "MaxLength";
  static final String PRECISION = DOLLAR + "Precision";
  static final String SCALE = DOLLAR + "Scale";
  static final String SRID_MEMBER = DOLLAR + "SRID";
  static final String COLLECTION = DOLLAR + "Collection";
  static final String BASE_TYPE = DOLLAR + "BaseType";
  static final String HAS_STREAM = DOLLAR + "HasStream";
  static final String KEY = DOLLAR + "Key";
  static final String ABSTRACT = DOLLAR + "Abstract";
  static final String OPEN_TYPE = DOLLAR + "OpenType";
  static final String TYPE = DOLLAR + "Type";
  static final String NULLABLE = DOLLAR + "Nullable";
  static final String UNICODE = DOLLAR + "Unicode";
  static final String DEFAULT_VALUE = DOLLAR + "DefaultValue";
  static final String PARTNER = DOLLAR + "Partner";
  static final String CONTAINS_TARGET = DOLLAR + "ContainsTarget";
  static final String REFERENTIAL_CONSTRAINT = DOLLAR + "ReferentialConstraint";
  static final String ON_DELETE = DOLLAR + "OnDelete";
  static final String ENTITY_CONTAINER = DOLLAR + "EntityContainer";
  static final String ANNOTATIONS = DOLLAR + "Annotations";
  static final String BASE_TERM = DOLLAR + "BaseTerm";
  static final String APPLIES_TO = DOLLAR + "AppliesTo";
  static final String LEGACY_ON_DELETE = "OnDelete";
  static final String LEGACY_ON_DELETE_ACTION = "Action";

  static final String KIND_ENTITY_TYPE = "EntityType";
  static final String KIND_COMPLEX_TYPE = "ComplexType";
  static final String KIND_ENUM_TYPE = "EnumType";
  static final String KIND_TYPE_DEFINITION = "TypeDefinition";
  static final String KIND_TERM = "Term";
  static final String KIND_ENTITY_CONTAINER = "EntityContainer";
  static final String KIND_NAVIGATION_PROPERTY = "NavigationProperty";

  static final String EDM_STRING = "Edm.String";
  static final String EDM_INT32 = "Edm.Int32";
  static final String SCALE_VARIABLE = "variable";
  static final String SCALE_FLOATING = "floating";
  private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");

  private boolean parseAnnotations = false;
  private ReferenceResolver referenceResolver = MetadataParser.defaultReferenceResolver();
  private boolean useLocalCoreVocabularies = true;
  private boolean implicitlyLoadCoreVocabularies = false;
  private boolean recursivelyLoadReferences = false;
  private final Map<String, SchemaBasedEdmProvider> globalReferenceMap;
  private final ReferenceLoader referenceLoader;

  /** The namespace-qualified name of the service's entity container, as declared by $EntityContainer. */
  private String serviceContainerFqn;

  /** Document-global aliases, mapped to the namespace they stand for. */
  private final Map<String, String> aliasToNamespace = new HashMap<>();

  /** Reads the operation overloads and the entity container; see {@link MetadataJsonContainerReader}. */
  private final MetadataJsonContainerReader containerReader = new MetadataJsonContainerReader(this);

  /** Reads annotations and their expressions; see {@link MetadataJsonAnnotationReader}. */
  private final MetadataJsonAnnotationReader annotationReader = new MetadataJsonAnnotationReader(this);

  public MetadataJsonParser() {
    this(new HashMap<>());
  }

  MetadataJsonParser(final Map<String, SchemaBasedEdmProvider> globalReferenceMap) {
    this.globalReferenceMap = globalReferenceMap;
    this.referenceLoader = new ReferenceLoader(globalReferenceMap, this::buildEdmProviderFromStream);
  }

  /**
   * Avoid reading the annotations in the $metadata
   * @param parse
   * @return this parser
   */
  public MetadataJsonParser parseAnnotations(final boolean parse) {
    this.parseAnnotations = parse;
    return this;
  }

  /**
   * Externalize the reference loading, such that they can be loaded from local caches
   * @param resolver
   * @return this parser
   */
  public MetadataJsonParser referenceResolver(final ReferenceResolver resolver) {
    this.referenceResolver = resolver;
    return this;
  }

  /**
   * Load the core libraries from local classpath
   * @param load true for yes; false otherwise
   * @return this parser
   */
  public MetadataJsonParser useLocalCoreVocabularies(final boolean load) {
    this.useLocalCoreVocabularies = load;
    return this;
  }

  /**
   * Load the references of the referenced documents too
   * @param load true for yes; false otherwise
   * @return this parser
   */
  public MetadataJsonParser recursivelyLoadReferences(final boolean load) {
    this.recursivelyLoadReferences = load;
    return this;
  }

  /**
   * Load the core vocabularies, irrespective of if they are defined in the $metadata
   * @param load
   * @return this parser
   */
  public MetadataJsonParser implicitlyLoadCoreVocabularies(final boolean load) {
    this.implicitlyLoadCoreVocabularies = load;
    return this;
  }

  /** @return the annotation reader, shared with {@link MetadataJsonContainerReader}. */
  MetadataJsonAnnotationReader annotations() {
    return this.annotationReader;
  }

  /** @return whether annotations are read; consulted by the annotation reader. */
  boolean isParseAnnotations() {
    return this.parseAnnotations;
  }

  /** @return the namespace-qualified container name declared by $Version's sibling $EntityContainer. */
  String getServiceContainerFqn() {
    return this.serviceContainerFqn;
  }

  public ServiceMetadata buildServiceMetadata(final Reader csdl) throws CsdlJsonParseException {
    final SchemaBasedEdmProvider provider = buildEdmProvider(csdl);
    return new ServiceMetadataImpl(provider, provider.getReferences(), null);
  }

  public SchemaBasedEdmProvider buildEdmProvider(final Reader csdl) throws CsdlJsonParseException {
    return addToEdmProvider(new SchemaBasedEdmProvider(), csdl);
  }

  public SchemaBasedEdmProvider addToEdmProvider(final SchemaBasedEdmProvider existing, final Reader csdl)
      throws CsdlJsonParseException {
    readInto(existing, readTree(csdl));
    return loadReferences(existing, this.referenceResolver, this.implicitlyLoadCoreVocabularies,
        this.useLocalCoreVocabularies, true, null);
  }

  protected SchemaBasedEdmProvider buildEdmProvider(final InputStream csdl, final ReferenceResolver resolver,
      final boolean loadCore, final boolean useLocal, final boolean loadReferenceSchemas, final String namespace)
      throws CsdlJsonParseException {
    final byte[] bytes = readAll(csdl);
    if (isXml(bytes)) {
      // The core vocabularies shipped with this library, and most published reference documents, are
      // CSDL XML. A CSDL JSON document referencing a CSDL XML document is not forbidden by either
      // format, so hand the whole document to the XML parser.
      try {
        return xmlParser().buildEdmProvider(new ByteArrayInputStream(bytes), resolver, loadCore, useLocal,
            loadReferenceSchemas, namespace);
      } catch (XMLStreamException e) {
        throw new CsdlJsonParseException("", "failed to read the referenced CSDL XML document", e);
      }
    }
    final SchemaBasedEdmProvider provider = new SchemaBasedEdmProvider();
    readInto(provider, readTree(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)));
    return loadReferences(provider, resolver, loadCore, useLocal, loadReferenceSchemas, namespace);
  }

  private SchemaBasedEdmProvider buildEdmProviderFromStream(final InputStream csdl, final ReferenceResolver resolver,
      final boolean loadCore, final boolean useLocal, final boolean loadReferenceSchemas, final String namespace)
      throws ODataException {
    return buildEdmProvider(csdl, resolver, loadCore, useLocal, loadReferenceSchemas, namespace);
  }

  private MetadataParser xmlParser() {
    return new MetadataParser(this.globalReferenceMap)
        .parseAnnotations(this.parseAnnotations)
        .referenceResolver(this.referenceResolver)
        .useLocalCoreVocabularies(this.useLocalCoreVocabularies)
        .implicitlyLoadCoreVocabularies(this.implicitlyLoadCoreVocabularies)
        .recursivelyLoadReferences(this.recursivelyLoadReferences);
  }

  private void readInto(final SchemaBasedEdmProvider provider, final JsonNode tree) throws CsdlJsonParseException {
    if (tree == null || !tree.isObject()) {
      throw new CsdlJsonParseException("", "a CSDL JSON document is a single JSON object");
    }
    readDocument((ObjectNode) tree, provider, "");
  }

  private SchemaBasedEdmProvider loadReferences(final SchemaBasedEdmProvider provider,
      final ReferenceResolver resolver, final boolean loadCore, final boolean useLocal,
      final boolean loadReferenceSchemas, final String namespace) throws CsdlJsonParseException {
    try {
      if (loadCore) {
        this.referenceLoader.loadCoreVocabularies(provider);
      }
      this.referenceLoader.rememberProvider(namespace, provider);
      if (resolver != null && loadReferenceSchemas) {
        // CSDL JSON has no xml:base analogue, so references resolve against the resolver's own base.
        this.referenceLoader.loadReferenceSchemas(provider, null, resolver, loadCore, useLocal,
            this.recursivelyLoadReferences);
      }
    } catch (ODataException e) {
      throw new CsdlJsonParseException("", e.getMessage(), e);
    }
    return provider;
  }

  public void loadCoreVocabulary(final SchemaBasedEdmProvider provider, final String namespace)
      throws CsdlJsonParseException {
    try {
      this.referenceLoader.loadCoreVocabulary(provider, namespace);
    } catch (ODataException e) {
      throw new CsdlJsonParseException("", e.getMessage(), e);
    }
  }

  private JsonNode readTree(final Reader csdl) throws CsdlJsonParseException {
    try {
      return new ObjectMapper().readTree(csdl);
    } catch (IOException e) {
      throw new CsdlJsonParseException("", "the document is not well-formed JSON", e);
    }
  }

  private static byte[] readAll(final InputStream csdl) throws CsdlJsonParseException {
    try {
      return csdl.readAllBytes();
    } catch (IOException e) {
      throw new CsdlJsonParseException("", "failed to read the CSDL document", e);
    }
  }

  private static boolean isXml(final byte[] bytes) {
    for (final byte b : bytes) {
      if (b == ' ' || b == '\t' || b == '\r' || b == '\n' || b == (byte) 0xEF || b == (byte) 0xBB
          || b == (byte) 0xBF) {
        continue;
      }
      return b == '<';
    }
    return false;
  }

  private void readDocument(final ObjectNode document, final SchemaBasedEdmProvider provider, final String path)
      throws CsdlJsonParseException {
    // Section 4: "This document object MUST contain the member $Version" whose value "is a string
    // containing either 4.0 or 4.01".
    final JsonNode version = document.get(VERSION);
    if (version == null || !version.isTextual()) {
      throw new CsdlJsonParseException(child(path, VERSION), "the document object must carry $Version");
    }
    if (!ODataServiceVersion.V40.toString().equals(version.asText())
        && !ODataServiceVersion.V401.toString().equals(version.asText())) {
      throw new CsdlJsonParseException(child(path, VERSION), "only 4.0 and 4.01 are valid CSDL JSON versions");
    }
    // Section 4: the metadata document of a service MUST name its container here, with the
    // namespace-qualified name. A document that is not a service metadata document has no such member.
    if (document.hasNonNull(ENTITY_CONTAINER) && document.get(ENTITY_CONTAINER).isTextual()) {
      this.serviceContainerFqn = document.get(ENTITY_CONTAINER).asText();
    }
    if (document.has(REFERENCES)) {
      readReferences(objectNode(document, REFERENCES, path), provider, child(path, REFERENCES));
    }
    collectAliases(document);
    this.containerReader.reset();
    final Iterator<Map.Entry<String, JsonNode>> members = document.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      if (member.getKey().startsWith(DOLLAR) || member.getKey().startsWith(AT)) {
        continue; // $Version, $EntityContainer, $Reference and document annotations
      }
      provider.addSchema(readSchema(member.getKey(), objectNode(member, path), child(path, member.getKey())));
    }
  }

  /**
   * Sections 4.2 and 5.1: aliases are document-global, and "the alias MUST be used instead of the
   * namespace within qualified names throughout the document". The model keeps namespace-qualified
   * names, so every alias declared by this document or by one of its includes is mapped back.
   */
  private void collectAliases(final ObjectNode document) {
    this.aliasToNamespace.clear();
    final JsonNode references = document.get(REFERENCES);
    if (references != null && references.isObject()) {
      for (final JsonNode reference : references) {
        final JsonNode includes = reference.get(INCLUDE);
        if (includes == null || !includes.isArray()) {
          continue;
        }
        for (final JsonNode include : includes) {
          if (include.hasNonNull(ALIAS) && include.hasNonNull(NAMESPACE)) {
            this.aliasToNamespace.put(include.get(ALIAS).asText(), include.get(NAMESPACE).asText());
          }
        }
      }
    }
    final Iterator<Map.Entry<String, JsonNode>> members = document.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      if (member.getKey().startsWith(DOLLAR) || member.getKey().startsWith(AT)
          || !member.getValue().isObject()) {
        continue;
      }
      final JsonNode alias = member.getValue().get(ALIAS);
      if (alias != null && alias.isTextual()) {
        this.aliasToNamespace.put(alias.asText(), member.getKey());
      }
    }
  }

  /** Replaces an alias-qualified name with its namespace-qualified form; other names pass through. */
  String resolveName(final String qualifiedName) {
    if (qualifiedName == null) {
      return null;
    }
    final int lastDot = qualifiedName.lastIndexOf('.');
    if (lastDot < 0) {
      return qualifiedName;
    }
    final String namespace = this.aliasToNamespace.get(qualifiedName.substring(0, lastDot));
    return namespace == null ? qualifiedName : namespace + qualifiedName.substring(lastDot);
  }

  private void readReferences(final ObjectNode references, final SchemaBasedEdmProvider provider, final String path)
      throws CsdlJsonParseException {
    final Iterator<Map.Entry<String, JsonNode>> members = references.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      // Section 4.1: "The name of the pair is a URI for the referenced document."
      final String uri = member.getKey();
      final String referencePath = child(path, uri);
      final ObjectNode node = objectNode(member, path);
      final EdmxReference reference;
      try {
        reference = new EdmxReference(URI.create(uri));
      } catch (IllegalArgumentException e) {
        throw new CsdlJsonParseException(referencePath, "the reference name is not a valid URI", e);
      }
      if (node.has(INCLUDE)) {
        final ArrayNode includes = arrayNode(node, INCLUDE, referencePath);
        for (int i = 0; i < includes.size(); i++) {
          final String includePath = referencePath + "/" + INCLUDE + "[" + i + "]";
          final ObjectNode include = objectNode(includes.get(i), includePath);
          reference.addInclude(new EdmxReferenceInclude(requireText(include, NAMESPACE, includePath),
              text(include, ALIAS, null)));
        }
      }
      if (node.has(INCLUDE_ANNOTATIONS)) {
        final ArrayNode included = arrayNode(node, INCLUDE_ANNOTATIONS, referencePath);
        for (int i = 0; i < included.size(); i++) {
          final String annotationPath = referencePath + "/" + INCLUDE_ANNOTATIONS + "[" + i + "]";
          final ObjectNode annotation = objectNode(included.get(i), annotationPath);
          final EdmxReferenceIncludeAnnotation includeAnnotation = new EdmxReferenceIncludeAnnotation(
              requireText(annotation, TERM_NAMESPACE, annotationPath));
          includeAnnotation.setQualifier(text(annotation, QUALIFIER, null));
          includeAnnotation.setTargetNamespace(text(annotation, TARGET_NAMESPACE, null));
          reference.addIncludeAnnotation(includeAnnotation);
        }
      }
      provider.addReference(reference);
    }
  }

  private CsdlSchema readSchema(final String namespace, final ObjectNode schemaNode, final String path)
      throws CsdlJsonParseException {
    final CsdlSchema schema = new CsdlSchema();
    schema.setNamespace(namespace);
    schema.setAlias(text(schemaNode, ALIAS, null));
    final Iterator<Map.Entry<String, JsonNode>> members = schemaNode.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (ALIAS.equals(name)) {
        continue;
      }
      if (ANNOTATIONS.equals(name)) {
        // Section 5.2: externally targeted annotations, one member per annotation target.
        this.annotationReader.readAnnotationGroups(objectNode(member, path), schema, child(path, name));
        continue;
      }
      if (name.startsWith(AT)) {
        continue; // annotations on the schema itself, read below in one pass
      }
      if (name.startsWith(DOLLAR)) {
        continue;
      }
      readSchemaMember(schema, name, member.getValue(), child(path, name));
    }
    this.annotationReader.readAnnotations(schemaNode, schema, "", path);
    return schema;
  }

  private void readSchemaMember(final CsdlSchema schema, final String name, final JsonNode value, final String path)
      throws CsdlJsonParseException {
    if (value.isArray()) {
      // Sections 12.2 and 12.4: an action or a function is a schema member whose value is the array of
      // its overloads.
      this.containerReader.readOverloads(schema, name, (ArrayNode) value, path);
      return;
    }
    final ObjectNode node = objectNode(value, path);
    final String kind = text(node, KIND, null);
    if (kind == null) {
      throw new CsdlJsonParseException(child(path, KIND), "a schema member must carry $Kind");
    }
    switch (kind) {
      case KIND_ENTITY_TYPE:
        schema.getEntityTypes().add(readEntityType(name, node, path));
        break;
      case KIND_COMPLEX_TYPE:
        schema.getComplexTypes().add(readComplexType(name, node, path));
        break;
      case KIND_ENUM_TYPE:
        schema.getEnumTypes().add(readEnumType(name, node, path));
        break;
      case KIND_TYPE_DEFINITION:
        schema.getTypeDefinitions().add(readTypeDefinition(name, node, path));
        break;
      case KIND_TERM:
        schema.getTerms().add(readTerm(name, node, path));
        break;
      case KIND_ENTITY_CONTAINER:
        this.containerReader.readContainer(schema, name, node, path);
        break;
      default:
        throw new CsdlJsonParseException(child(path, KIND), "unknown schema member kind " + kind);
    }
  }

  private CsdlEntityType readEntityType(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlEntityType type = new CsdlEntityType();
    type.setName(name);
    if (node.has(BASE_TYPE)) {
      type.setBaseType(resolveName(requireText(node, BASE_TYPE, path)));
    }
    type.setAbstract(flag(node, ABSTRACT));
    type.setOpenType(flag(node, OPEN_TYPE));
    type.setHasStream(flag(node, HAS_STREAM));
    if (node.has(KEY)) {
      type.setKey(readKey(arrayNode(node, KEY, path), child(path, KEY)));
    }
    readStructuralTypeMembers(type, node, path);
    this.annotationReader.readAnnotations(node, type, "", path);
    return type;
  }

  private CsdlComplexType readComplexType(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlComplexType type = new CsdlComplexType();
    type.setName(name);
    if (node.has(BASE_TYPE)) {
      type.setBaseType(resolveName(requireText(node, BASE_TYPE, path)));
    }
    type.setAbstract(flag(node, ABSTRACT));
    type.setOpenType(flag(node, OPEN_TYPE));
    readStructuralTypeMembers(type, node, path);
    this.annotationReader.readAnnotations(node, type, "", path);
    return type;
  }

  private void readStructuralTypeMembers(final CsdlStructuralType type, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final List<CsdlProperty> properties = new ArrayList<>();
    final List<CsdlNavigationProperty> navigationProperties = new ArrayList<>();
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (name.startsWith(DOLLAR) || name.contains(AT)) {
        continue; // type members and annotations
      }
      final String memberPath = child(path, name);
      final ObjectNode value = objectNode(member.getValue(), memberPath);
      if (KIND_NAVIGATION_PROPERTY.equals(text(value, KIND, null))) {
        navigationProperties.add(readNavigationProperty(name, value, memberPath));
      } else {
        // Section 7: "$Kind ... SHOULD be omitted to reduce document size", so anything that is not a
        // navigation property is a structural property.
        properties.add(readProperty(name, value, memberPath));
      }
    }
    type.setProperties(properties);
    type.setNavigationProperties(navigationProperties);
  }

  private List<CsdlPropertyRef> readKey(final ArrayNode key, final String path) throws CsdlJsonParseException {
    final List<CsdlPropertyRef> refs = new ArrayList<>();
    for (int i = 0; i < key.size(); i++) {
      final JsonNode item = key.get(i);
      if (item.isTextual()) {
        // Section 6.5: "Key properties without a key alias are represented as strings containing the
        // property name."
        refs.add(new CsdlPropertyRef().setName(item.asText()));
      } else if (item.isObject() && item.size() == 1) {
        // "Key properties with a key alias are represented as objects with one member whose name is
        // the key alias and whose value is a string containing the path to the property."
        final Map.Entry<String, JsonNode> alias = item.fields().next();
        refs.add(new CsdlPropertyRef().setAlias(alias.getKey()).setName(alias.getValue().asText()));
      } else {
        throw new CsdlJsonParseException(path + "[" + i + "]",
            "a key item is either a property name or a single-member alias object");
      }
    }
    return refs;
  }

  private CsdlProperty readProperty(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlProperty property = new CsdlProperty();
    property.setName(name);
    // Section 7.1: "Absence of the $Type member means the type is Edm.String."; for a collection the
    // $Type is the item type and $Collection is true.
    property.setType(resolveName(text(node, TYPE, EDM_STRING)));
    property.setCollection(flag(node, COLLECTION));
    // Section 7.2.1: "Absence of the member means false" - the opposite of the CSDL XML default.
    property.setNullable(flag(node, NULLABLE));
    if (node.hasNonNull(DEFAULT_VALUE)) {
      property.setDefaultValue(node.get(DEFAULT_VALUE).asText());
    }
    readFacets(node, facets(property), path);
    this.annotationReader.readAnnotations(node, property, "", path);
    return property;
  }

  private CsdlNavigationProperty readNavigationProperty(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlNavigationProperty property = new CsdlNavigationProperty();
    property.setName(name);
    // Section 8.1: a navigation property MUST specify $Type; there is no Edm.String fallback.
    property.setType(resolveName(requireText(node, TYPE, path)));
    final boolean collection = flag(node, COLLECTION);
    property.setCollection(collection);
    // Section 8.2: $Nullable "MUST NOT be specified for a collection-valued navigation property"; the
    // section 7.2.1 default of false therefore does not apply to one - leaving the model default keeps
    // a collection navigation property identical to the one the CSDL XML parser builds.
    if (!collection) {
      property.setNullable(flag(node, NULLABLE));
    }
    property.setPartner(text(node, PARTNER, null));
    property.setContainsTarget(flag(node, CONTAINS_TARGET));
    property.setReferentialConstraints(readReferentialConstraints(node, path));
    final String onDelete = onDeleteAction(node, path);
    if (onDelete != null) {
      try {
        property.setOnDelete(new CsdlOnDelete().setAction(CsdlOnDeleteAction.valueOf(onDelete)));
      } catch (IllegalArgumentException e) {
        throw new CsdlJsonParseException(child(path, ON_DELETE),
            "$OnDelete is one of Cascade, None, SetNull or SetDefault", e);
      }
      // Section 8.4: "Annotations for $OnDelete are prefixed with $OnDelete."
      this.annotationReader.readAnnotations(node, property.getOnDelete(), ON_DELETE, path);
    }
    this.annotationReader.readAnnotations(node, property, "", path);
    return property;
  }

  /**
   * Section 8.4: "The value of $OnDelete is a string". The pre-conformance Olinguito writer emitted an
   * "OnDelete" object with an "Action" member instead, which is tolerated on input and never written.
   */
  private static String onDeleteAction(final ObjectNode node, final String path) throws CsdlJsonParseException {
    if (node.has(ON_DELETE)) {
      return requireText(node, ON_DELETE, path);
    }
    final JsonNode legacy = node.get(LEGACY_ON_DELETE);
    if (legacy != null && legacy.isObject() && legacy.get(LEGACY_ON_DELETE_ACTION) != null) {
      return legacy.get(LEGACY_ON_DELETE_ACTION).asText();
    }
    return null;
  }

  private List<CsdlReferentialConstraint> readReferentialConstraints(final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final List<CsdlReferentialConstraint> constraints = new ArrayList<>();
    if (!node.has(REFERENTIAL_CONSTRAINT)) {
      return constraints;
    }
    final String constraintPath = child(path, REFERENTIAL_CONSTRAINT);
    final ObjectNode value = objectNode(node, REFERENTIAL_CONSTRAINT, path);
    final Iterator<Map.Entry<String, JsonNode>> members = value.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      if (member.getKey().contains(AT)) {
        continue; // "<dependent>@<term>" annotates the constraint of that dependent property
      }
      if (!member.getValue().isTextual()) {
        throw new CsdlJsonParseException(child(constraintPath, member.getKey()),
            "a referential constraint maps the dependent property to the path of the principal property");
      }
      final CsdlReferentialConstraint constraint = new CsdlReferentialConstraint()
          .setProperty(member.getKey())
          .setReferencedProperty(member.getValue().asText());
      // Section 8.5: "Annotations ... are prefixed with the name of the dependent property."
      this.annotationReader.readAnnotations(value, constraint, member.getKey(), constraintPath);
      constraints.add(constraint);
    }
    return constraints;
  }

  private CsdlEnumType readEnumType(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlEnumType type = new CsdlEnumType();
    type.setName(name);
    // Section 10.1: "If not explicitly specified, Edm.Int32 is used as the underlying type."
    type.setUnderlyingType(resolveName(text(node, UNDERLYING_TYPE, EDM_INT32)));
    type.setFlags(flag(node, IS_FLAGS));
    final List<CsdlEnumMember> members = new ArrayList<>();
    final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final String memberName = field.getKey();
      if (memberName.startsWith(DOLLAR) || memberName.contains(AT)) {
        continue; // "<member>@<term>" annotates that enumeration member
      }
      if (!field.getValue().isNumber()) {
        throw new CsdlJsonParseException(child(path, memberName),
            "an enumeration member value is a number valid for the underlying type");
      }
      final CsdlEnumMember enumMember =
          new CsdlEnumMember().setName(memberName).setValue(field.getValue().asText());
      // Section 10.3: "Annotations for enumeration members are prefixed with the member name."
      this.annotationReader.readAnnotations(node, enumMember, memberName, path);
      members.add(enumMember);
    }
    type.setMembers(members);
    this.annotationReader.readAnnotations(node, type, "", path);
    return type;
  }

  private CsdlTypeDefinition readTypeDefinition(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlTypeDefinition definition = new CsdlTypeDefinition();
    definition.setName(name);
    // Section 11: the type definition object MUST contain $UnderlyingType.
    definition.setUnderlyingType(resolveName(requireText(node, UNDERLYING_TYPE, path)));
    readFacets(node, facets(definition), path);
    this.annotationReader.readAnnotations(node, definition, "", path);
    return definition;
  }

  private CsdlTerm readTerm(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlTerm term = new CsdlTerm();
    term.setName(name);
    final String type = resolveName(text(node, TYPE, EDM_STRING));
    term.setType(flag(node, COLLECTION) ? "Collection(" + type + ")" : type);
    if (node.has(BASE_TERM)) {
      term.setBaseTerm(resolveName(requireText(node, BASE_TERM, path)));
    }
    term.setNullable(flag(node, NULLABLE));
    if (node.hasNonNull(DEFAULT_VALUE)) {
      term.setDefaultValue(node.get(DEFAULT_VALUE).asText());
    }
    if (node.has(APPLIES_TO)) {
      term.setAppliesTo(readAppliesTo(node.get(APPLIES_TO), child(path, APPLIES_TO)));
    }
    readFacets(node, facets(term), path);
    this.annotationReader.readAnnotations(node, term, "", path);
    return term;
  }

  private List<String> readAppliesTo(final JsonNode node, final String path) throws CsdlJsonParseException {
    final List<String> appliesTo = new ArrayList<>();
    if (node.isArray()) {
      for (final JsonNode item : node) {
        appliesTo.add(item.asText());
      }
    } else if (node.isTextual()) {
      appliesTo.addAll(List.of(WHITESPACE.split(node.asText())));
    } else {
      throw new CsdlJsonParseException(path, "$AppliesTo is an array of symbolic values");
    }
    return appliesTo;
  }

  /** The subset of a Csdl type that carries type facets; the Csdl types do not share an interface. */
  interface FacetSink {
    void maxLength(Integer value);

    void precision(Integer value);

    void scale(Integer value);

    /** Only CsdlProperty models the symbolic scale; the others skip it, as the XML parser does. */
    default void scaleAsString(final String value) {
      // ignored - the target has no place to keep the symbolic value
    }

    /** Only properties and type definitions carry $Unicode. */
    default void unicode(final boolean value) {
      // ignored - the target has no Unicode facet
    }

    void srid(SRID value);
  }

  private static FacetSink facets(final CsdlProperty property) {
    return new FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        property.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        property.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        property.setScale(value);
      }

      @Override
      public void scaleAsString(final String value) {
        property.setScaleAsString(value);
      }

      @Override
      public void unicode(final boolean value) {
        property.setUnicode(value);
      }

      @Override
      public void srid(final SRID value) {
        property.setSrid(value);
      }
    };
  }

  private static FacetSink facets(final CsdlTypeDefinition definition) {
    return new FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        definition.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        definition.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        definition.setScale(value);
      }

      @Override
      public void unicode(final boolean value) {
        definition.setUnicode(value);
      }

      @Override
      public void srid(final SRID value) {
        definition.setSrid(value);
      }
    };
  }

  private static FacetSink facets(final CsdlTerm term) {
    return new FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        term.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        term.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        term.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        term.setSrid(value);
      }
    };
  }

  void readFacets(final ObjectNode node, final FacetSink sink, final String path)
      throws CsdlJsonParseException {
    if (node.has(MAX_LENGTH)) {
      final JsonNode maxLength = node.get(MAX_LENGTH);
      if (maxLength.isTextual()) {
        // Section 7.2.2 on the CSDL XML symbolic value max.
        throw new CsdlJsonParseException(child(path, MAX_LENGTH),
            "This symbolic value is not allowed in CDSL JSON documents at all.");
      }
      sink.maxLength(integer(node, MAX_LENGTH, path));
    }
    if (node.has(PRECISION)) {
      sink.precision(integer(node, PRECISION, path));
    }
    if (node.has(SCALE)) {
      final JsonNode scale = node.get(SCALE);
      if (scale.isTextual()) {
        // Section 7.2.4: "a number or a string with one of the symbolic values floating or variable";
        // "Services SHOULD use lower-case values; clients SHOULD accept values in a case-insensitive
        // manner."
        final String symbolic = scale.asText();
        if (!SCALE_FLOATING.equalsIgnoreCase(symbolic) && !SCALE_VARIABLE.equalsIgnoreCase(symbolic)) {
          throw new CsdlJsonParseException(child(path, SCALE),
              "$Scale is a number or one of the symbolic values floating or variable");
        }
        sink.scaleAsString(symbolic.toLowerCase(java.util.Locale.ROOT));
      } else {
        sink.scale(integer(node, SCALE, path));
      }
    }
    // Section 7.2.5: "Absence of the member means true."
    sink.unicode(!node.has(UNICODE) || flag(node, UNICODE));
    if (node.has(SRID_MEMBER)) {
      // Section 7.2.6: "The value of $SRID is a string containing a number or the symbolic value
      // variable."
      sink.srid(SRID.valueOf(requireText(node, SRID_MEMBER, path)));
    }
  }

  static String requireText(final ObjectNode node, final String member, final String path)
      throws CsdlJsonParseException {
    final JsonNode value = node.get(member);
    if (value == null || !value.isTextual()) {
      throw new CsdlJsonParseException(child(path, member), "the member " + member + " must be a string");
    }
    return value.asText();
  }

  static String text(final ObjectNode node, final String member, final String defaultValue) {
    final JsonNode value = node.get(member);
    return value != null && value.isTextual() ? value.asText() : defaultValue;
  }

  static boolean flag(final ObjectNode node, final String member) {
    final JsonNode value = node.get(member);
    return value != null && value.asBoolean(false);
  }

  static Integer integer(final ObjectNode node, final String member, final String path)
      throws CsdlJsonParseException {
    final JsonNode value = node.get(member);
    if (value == null || !value.canConvertToInt()) {
      throw new CsdlJsonParseException(child(path, member), "the member " + member + " must be an integer");
    }
    return value.intValue();
  }

  static ObjectNode objectNode(final ObjectNode parent, final String member, final String path)
      throws CsdlJsonParseException {
    return objectNode(parent.get(member), child(path, member));
  }

  static ObjectNode objectNode(final Map.Entry<String, JsonNode> member, final String path)
      throws CsdlJsonParseException {
    return objectNode(member.getValue(), child(path, member.getKey()));
  }

  static ObjectNode objectNode(final JsonNode node, final String path) throws CsdlJsonParseException {
    if (node == null || !node.isObject()) {
      throw new CsdlJsonParseException(path, "a JSON object is expected here");
    }
    return (ObjectNode) node;
  }

  static ArrayNode arrayNode(final ObjectNode parent, final String member, final String path)
      throws CsdlJsonParseException {
    final JsonNode node = parent.get(member);
    if (node == null || !node.isArray()) {
      throw new CsdlJsonParseException(child(path, member), "a JSON array is expected here");
    }
    return (ArrayNode) node;
  }

  static String child(final String path, final String member) {
    return path.isEmpty() ? member : path + "/" + member;
  }
}
