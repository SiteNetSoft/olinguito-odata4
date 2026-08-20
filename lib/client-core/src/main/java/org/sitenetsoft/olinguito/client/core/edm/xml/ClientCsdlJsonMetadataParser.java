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
 * Copyright 2026 SiteNetSoft - Read CSDL JSON metadata in the client deserializer
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: kept the collection navigation property $Nullable prohibition
 */
package org.sitenetsoft.olinguito.client.core.edm.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.sitenetsoft.olinguito.client.api.edm.xml.Reference;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumMember;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDelete;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDeleteAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOperation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReferentialConstraint;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlStructuralType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlAnnotationPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlApply;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCast;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElementReference;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression
    .LogicalOrComparisonExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNull;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlUrlRef;

/**
 * Reads a CSDL JSON metadata document (OData CSDL JSON, OASIS) into the {@link XMLMetadata} graph the
 * CSDL XML deserializer of this client produces, so that {@code ODataReader#readMetadata} and all
 * {@code Edm} construction work the same for either representation of the same model.
 * <p>
 * The document object, its references and its schemas (sections 4 and 5), the structural model
 * (sections 6 to 11), the operations and the entity container (sections 12 and 13) and the annotations
 * with their expressions (section 14) are all read here. Two defaults differ from the CSDL XML ones and
 * are the easiest thing to get wrong: {@code $Nullable} is absent-means-<em>false</em> (section 7.2.1),
 * the opposite of the XML attribute, and an absent {@code $Type} on a structural property means
 * {@code Edm.String} (section 7.1).
 * <p>
 * Qualified names are resolved against the document-global aliases of sections 4.2 and 5.1: CSDL JSON
 * <em>requires</em> the alias once one is declared, and an alias declared by an {@code $Include} names a
 * schema that is not in this document at all, so neither has an entry in
 * {@link XMLMetadata#getSchemaByNsOrAlias()}. The alias is kept on the schema, so the client's own
 * namespace-or-alias lookup keeps working unchanged.
 * <p>
 * Besides the conformant shape, the shapes the pre-conformance Olinguito JSON writer produced are
 * accepted too, so this client reads the documents an older Olinguito service still serves. They are
 * tolerated on input only; nothing in this library writes them any more.
 */
public final class ClientCsdlJsonMetadataParser {

  private static final String DOLLAR = "$";
  private static final String AT = "@";
  private static final String VERSION = DOLLAR + "Version";
  private static final String REFERENCES = DOLLAR + "Reference";
  private static final String INCLUDE = DOLLAR + "Include";
  private static final String NAMESPACE = DOLLAR + "Namespace";
  private static final String ALIAS = DOLLAR + "Alias";
  private static final String INCLUDE_ANNOTATIONS = DOLLAR + "IncludeAnnotations";
  private static final String TERM_NAMESPACE = DOLLAR + "TermNamespace";
  private static final String TARGET_NAMESPACE = DOLLAR + "TargetNamespace";
  private static final String QUALIFIER = DOLLAR + "Qualifier";
  private static final String IS_FLAGS = DOLLAR + "IsFlags";
  private static final String UNDERLYING_TYPE = DOLLAR + "UnderlyingType";
  private static final String KIND = DOLLAR + "Kind";
  private static final String MAX_LENGTH = DOLLAR + "MaxLength";
  private static final String PRECISION = DOLLAR + "Precision";
  private static final String SCALE = DOLLAR + "Scale";
  private static final String SRID_MEMBER = DOLLAR + "SRID";
  private static final String COLLECTION = DOLLAR + "Collection";
  private static final String BASE_TYPE = DOLLAR + "BaseType";
  private static final String HAS_STREAM = DOLLAR + "HasStream";
  private static final String KEY = DOLLAR + "Key";
  private static final String ABSTRACT = DOLLAR + "Abstract";
  private static final String OPEN_TYPE = DOLLAR + "OpenType";
  private static final String TYPE = DOLLAR + "Type";
  private static final String NULLABLE = DOLLAR + "Nullable";
  private static final String UNICODE = DOLLAR + "Unicode";
  private static final String DEFAULT_VALUE = DOLLAR + "DefaultValue";
  private static final String PARTNER = DOLLAR + "Partner";
  private static final String CONTAINS_TARGET = DOLLAR + "ContainsTarget";
  private static final String REFERENTIAL_CONSTRAINT = DOLLAR + "ReferentialConstraint";
  private static final String ON_DELETE = DOLLAR + "OnDelete";
  private static final String ENTITY_CONTAINER = DOLLAR + "EntityContainer";
  private static final String ANNOTATIONS = DOLLAR + "Annotations";
  private static final String BASE_TERM = DOLLAR + "BaseTerm";
  private static final String APPLIES_TO = DOLLAR + "AppliesTo";
  private static final String IS_BOUND = DOLLAR + "IsBound";
  private static final String IS_COMPOSABLE = DOLLAR + "IsComposable";
  private static final String ENTITY_SET_PATH = DOLLAR + "EntitySetPath";
  private static final String RETURN_TYPE = DOLLAR + "ReturnType";
  private static final String PARAMETER = DOLLAR + "Parameter";
  private static final String NAME = DOLLAR + "Name";
  private static final String EXTENDS = DOLLAR + "Extends";
  private static final String INCLUDE_IN_SERVICE_DOCUMENT = DOLLAR + "IncludeInServiceDocument";
  private static final String NAVIGATION_PROPERTY_BINDING = DOLLAR + "NavigationPropertyBinding";
  private static final String ACTION = DOLLAR + "Action";
  private static final String FUNCTION = DOLLAR + "Function";
  private static final String ENTITY_SET = DOLLAR + "EntitySet";
  private static final String PATH = DOLLAR + "Path";
  private static final String ANNOTATION_PATH = DOLLAR + "AnnotationPath";
  private static final String NAVIGATION_PROPERTY_PATH = DOLLAR + "NavigationPropertyPath";
  private static final String PROPERTY_PATH = DOLLAR + "PropertyPath";
  private static final String NOT = DOLLAR + "Not";
  private static final String APPLY = DOLLAR + "Apply";
  private static final String CAST = DOLLAR + "Cast";
  private static final String IF = DOLLAR + "If";
  private static final String IS_OF = DOLLAR + "IsOf";
  private static final String LABELED_ELEMENT = DOLLAR + "LabeledElement";
  private static final String LABELED_ELEMENT_REFERENCE = DOLLAR + "LabeledElementReference";
  private static final String NULL = DOLLAR + "Null";
  private static final String URL_REF = DOLLAR + "UrlRef";
  private static final String LEGACY_ON_DELETE = "OnDelete";
  private static final String LEGACY_ON_DELETE_ACTION = "Action";
  /** The member the pre-conformance Olinguito writer nested the container's inheritance in. */
  private static final String LEGACY_EXTENDING = "Extending";

  private static final String KIND_ENTITY_TYPE = "EntityType";
  private static final String KIND_COMPLEX_TYPE = "ComplexType";
  private static final String KIND_ENUM_TYPE = "EnumType";
  private static final String KIND_TYPE_DEFINITION = "TypeDefinition";
  private static final String KIND_TERM = "Term";
  private static final String KIND_ENTITY_CONTAINER = "EntityContainer";
  private static final String KIND_NAVIGATION_PROPERTY = "NavigationProperty";
  private static final String KIND_ACTION = "Action";
  private static final String KIND_FUNCTION = "Function";
  private static final String KIND_ENTITY_SET = "EntitySet";

  private static final String EDM_STRING = "Edm.String";
  private static final String EDM_INT32 = "Edm.Int32";
  private static final String SCALE_VARIABLE = "variable";
  private static final String SCALE_FLOATING = "floating";
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  /**
   * Section 14.4.12: "The type of a record expression is represented as the @type control
   * information". These member names are JSON control information, never annotation terms.
   */
  private static final String AT_TYPE = "@type";
  private static final String ODATA_AT_TYPE = "@odata.type";
  private static final Set<String> CONTROL_INFORMATION = Set.of(AT_TYPE, ODATA_AT_TYPE);

  /**
   * Section 14.4.2: the operators whose value is an array of two operand expressions. $Has and $In are
   * in the specification's table but have no {@link LogicalOrComparisonExpressionType} constant in this
   * model, so they are reported rather than silently dropped.
   */
  private static final Set<String> BINARY_OPERATORS =
      Set.of(DOLLAR + "And", DOLLAR + "Or", DOLLAR + "Eq", DOLLAR + "Ne",
          DOLLAR + "Gt", DOLLAR + "Ge", DOLLAR + "Lt", DOLLAR + "Le");

  private static final Set<String> UNSUPPORTED_OPERATORS = Set.of(DOLLAR + "Has", DOLLAR + "In");

  /** Document-global aliases, mapped to the namespace they stand for. */
  private final Map<String, String> aliasToNamespace = new HashMap<>();

  /** The namespace-qualified name of the service's entity container, as declared by $EntityContainer. */
  private String serviceContainerFqn;

  /** The schema the entity container currently in the model was installed into, if any. */
  private CsdlSchema containerOwner;

  private ClientCsdlJsonMetadataParser() {
    // the entry point is parse(InputStream); one instance reads one document
  }

  /**
   * Reads one CSDL JSON metadata document.
   *
   * @param input the document
   * @return its metadata, the same graph the CSDL XML deserializer builds for the same model
   * @throws IllegalArgumentException if the stream is not a well-formed CSDL JSON document
   */
  public static XMLMetadata parse(final InputStream input) {
    try {
      return new ClientCsdlJsonMetadataParser().readDocument(new ObjectMapper().readTree(input));
    } catch (IOException | RuntimeException e) {
      throw new IllegalArgumentException("Could not parse as CSDL JSON document", e);
    }
  }

  // ------------------------------------------------------------------- section 4: the document object

  private XMLMetadata readDocument(final JsonNode tree) {
    // Section 4: "A CSDL JSON document consists of a single JSON object."
    final ObjectNode document = objectNode(tree, "");
    // "This document object MUST contain the member $Version", whose value "is a string containing
    // either 4.0 or 4.01".
    final String version = requireText(document, VERSION, "");
    if (!ODataServiceVersion.V40.toString().equals(version)
        && !ODataServiceVersion.V401.toString().equals(version)) {
      throw new CsdlJsonParseException(child("", VERSION), "only 4.0 and 4.01 are valid CSDL JSON versions");
    }
    // Section 4: the metadata document of a service MUST name its container here, with the
    // namespace-qualified name. A document that is not a service metadata document has no such member.
    if (document.hasNonNull(ENTITY_CONTAINER) && document.get(ENTITY_CONTAINER).isTextual()) {
      this.serviceContainerFqn = document.get(ENTITY_CONTAINER).asText();
    }
    final List<Reference> references = document.has(REFERENCES)
        ? readReferences(objectNode(document, REFERENCES, ""), child("", REFERENCES))
        : new ArrayList<>();
    collectAliases(document);
    final List<CsdlSchema> schemas = new ArrayList<>();
    final Iterator<Map.Entry<String, JsonNode>> members = document.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      if (member.getKey().startsWith(DOLLAR) || member.getKey().startsWith(AT)) {
        continue; // $Version, $EntityContainer, $Reference and document annotations
      }
      schemas.add(readSchema(member.getKey(), objectNode(member, ""), member.getKey()));
    }
    return new ClientCsdlJsonMetadata(schemas, references, version);
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
  private String resolveName(final String qualifiedName) {
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

  /**
   * Section 4.1: "The value of $Reference is an object that contains one member per referenced CSDL
   * document. The name of the pair is a URI for the referenced document."
   */
  private List<Reference> readReferences(final ObjectNode references, final String path) {
    final List<Reference> result = new ArrayList<>();
    final Iterator<Map.Entry<String, JsonNode>> members = references.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String uri = member.getKey();
      final String referencePath = child(path, uri);
      final ObjectNode node = objectNode(member, path);
      final ClientCsdlReference reference = new ClientCsdlReference();
      try {
        reference.setUri(URI.create(uri));
      } catch (IllegalArgumentException e) {
        throw new CsdlJsonParseException(referencePath, "the reference name is not a valid URI", e);
      }
      if (node.has(INCLUDE)) {
        // Section 4.2: "Array items are objects that MUST contain the member $Namespace and MAY
        // contain the member $Alias."
        final ArrayNode includes = arrayNode(node, INCLUDE, referencePath);
        for (int i = 0; i < includes.size(); i++) {
          final String includePath = item(child(referencePath, INCLUDE), i);
          final ObjectNode include = objectNode(includes.get(i), includePath);
          final ClientCsdlInclude clientInclude = new ClientCsdlInclude();
          clientInclude.setNamespace(requireText(include, NAMESPACE, includePath));
          clientInclude.setAlias(text(include, ALIAS, null));
          reference.getIncludes().add(clientInclude);
        }
      }
      if (node.has(INCLUDE_ANNOTATIONS)) {
        // Section 4.3: "Array items are objects that MUST contain the member $TermNamespace and MAY
        // contain the members $Qualifier and $TargetNamespace."
        final ArrayNode included = arrayNode(node, INCLUDE_ANNOTATIONS, referencePath);
        for (int i = 0; i < included.size(); i++) {
          final String annotationPath = item(child(referencePath, INCLUDE_ANNOTATIONS), i);
          final ObjectNode annotation = objectNode(included.get(i), annotationPath);
          final ClientCsdlIncludeAnnotations includeAnnotations = new ClientCsdlIncludeAnnotations();
          includeAnnotations.setTermNamespace(requireText(annotation, TERM_NAMESPACE, annotationPath));
          includeAnnotations.setQualifier(text(annotation, QUALIFIER, null));
          includeAnnotations.setTargetNamespace(text(annotation, TARGET_NAMESPACE, null));
          reference.getIncludeAnnotations().add(includeAnnotations);
        }
      }
      readAnnotations(node, reference, "", referencePath);
      result.add(reference);
    }
    return result;
  }

  // ------------------------------------------------------------------------------ section 5: schemas

  private CsdlSchema readSchema(final String namespace, final ObjectNode schemaNode, final String path) {
    final CsdlSchema schema = new CsdlSchema();
    schema.setNamespace(namespace);
    schema.setAlias(text(schemaNode, ALIAS, null));
    final Iterator<Map.Entry<String, JsonNode>> members = schemaNode.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (ANNOTATIONS.equals(name)) {
        // Section 5.2: externally targeted annotations, one member per annotation target.
        readAnnotationGroups(objectNode(member, path), schema, child(path, name));
        continue;
      }
      if (name.startsWith(DOLLAR) || name.startsWith(AT)) {
        continue; // $Alias and the annotations on the schema itself, read below in one pass
      }
      readSchemaMember(schema, name, member.getValue(), child(path, name));
    }
    readAnnotations(schemaNode, schema, "", path);
    return schema;
  }

  private void readSchemaMember(final CsdlSchema schema, final String name, final JsonNode value,
      final String path) {
    if (value.isArray()) {
      // Sections 12.2 and 12.4: an action or a function is a schema member whose value is the array of
      // its overloads.
      readOverloads(schema, name, (ArrayNode) value, path);
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
        readContainer(schema, name, node, path);
        break;
      default:
        throw new CsdlJsonParseException(child(path, KIND), "unknown schema member kind " + kind);
    }
  }

  // -------------------------------------------------------------- sections 6 to 11: the type system

  private CsdlEntityType readEntityType(final String name, final ObjectNode node, final String path) {
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
    readAnnotations(node, type, "", path);
    return type;
  }

  private CsdlComplexType readComplexType(final String name, final ObjectNode node, final String path) {
    final CsdlComplexType type = new CsdlComplexType();
    type.setName(name);
    if (node.has(BASE_TYPE)) {
      type.setBaseType(resolveName(requireText(node, BASE_TYPE, path)));
    }
    type.setAbstract(flag(node, ABSTRACT));
    type.setOpenType(flag(node, OPEN_TYPE));
    readStructuralTypeMembers(type, node, path);
    readAnnotations(node, type, "", path);
    return type;
  }

  private void readStructuralTypeMembers(final CsdlStructuralType type, final ObjectNode node,
      final String path) {
    final List<CsdlProperty> properties = new ArrayList<>();
    final List<CsdlNavigationProperty> navigationProperties = new ArrayList<>();
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (name.startsWith(DOLLAR) || name.contains(AT)) {
        continue; // the type's own members and its annotations
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

  private static List<CsdlPropertyRef> readKey(final ArrayNode key, final String path) {
    final List<CsdlPropertyRef> refs = new ArrayList<>();
    for (int i = 0; i < key.size(); i++) {
      final JsonNode entry = key.get(i);
      if (entry.isTextual()) {
        // Section 6.5: "Key properties without a key alias are represented as strings containing the
        // property name."
        refs.add(new CsdlPropertyRef().setName(entry.asText()));
      } else if (entry.isObject() && entry.size() == 1) {
        // "Key properties with a key alias are represented as objects with one member whose name is
        // the key alias and whose value is a string containing the path to the property."
        final Map.Entry<String, JsonNode> alias = entry.fields().next();
        refs.add(new CsdlPropertyRef().setAlias(alias.getKey()).setName(alias.getValue().asText()));
      } else {
        throw new CsdlJsonParseException(item(path, i),
            "a key item is either a property name or a single-member alias object");
      }
    }
    return refs;
  }

  private CsdlProperty readProperty(final String name, final ObjectNode node, final String path) {
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
    readAnnotations(node, property, "", path);
    return property;
  }

  private CsdlNavigationProperty readNavigationProperty(final String name, final ObjectNode node,
      final String path) {
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
      // "Annotations for $OnDelete are prefixed with $OnDelete."
      readAnnotations(node, property.getOnDelete(), ON_DELETE, path);
    }
    readAnnotations(node, property, "", path);
    return property;
  }

  /**
   * Section 8.4: "The value of $OnDelete is a string" with one of the values Cascade, None, SetNull or
   * SetDefault. The pre-conformance Olinguito writer emitted an "OnDelete" object with an "Action"
   * member instead, which is tolerated on input and never written.
   */
  private static String onDeleteAction(final ObjectNode node, final String path) {
    if (node.has(ON_DELETE)) {
      return requireText(node, ON_DELETE, path);
    }
    final JsonNode legacy = node.get(LEGACY_ON_DELETE);
    if (legacy != null && legacy.isObject() && legacy.get(LEGACY_ON_DELETE_ACTION) != null) {
      return legacy.get(LEGACY_ON_DELETE_ACTION).asText();
    }
    return null;
  }

  private List<CsdlReferentialConstraint> readReferentialConstraints(final ObjectNode node,
      final String path) {
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
      readAnnotations(value, constraint, member.getKey(), constraintPath);
      constraints.add(constraint);
    }
    return constraints;
  }

  private CsdlEnumType readEnumType(final String name, final ObjectNode node, final String path) {
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
      readAnnotations(node, enumMember, memberName, path);
      members.add(enumMember);
    }
    type.setMembers(members);
    readAnnotations(node, type, "", path);
    return type;
  }

  private CsdlTypeDefinition readTypeDefinition(final String name, final ObjectNode node,
      final String path) {
    final CsdlTypeDefinition definition = new CsdlTypeDefinition();
    definition.setName(name);
    // Section 11: the type definition object MUST contain $UnderlyingType.
    definition.setUnderlyingType(resolveName(requireText(node, UNDERLYING_TYPE, path)));
    readFacets(node, facets(definition), path);
    readAnnotations(node, definition, "", path);
    return definition;
  }

  private CsdlTerm readTerm(final String name, final ObjectNode node, final String path) {
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
    readAnnotations(node, term, "", path);
    return term;
  }

  private static List<String> readAppliesTo(final JsonNode node, final String path) {
    final List<String> appliesTo = new ArrayList<>();
    if (node.isArray()) {
      for (final JsonNode entry : node) {
        appliesTo.add(entry.asText());
      }
    } else if (node.isTextual()) {
      appliesTo.addAll(List.of(WHITESPACE.split(node.asText())));
    } else {
      throw new CsdlJsonParseException(path, "$AppliesTo is an array of symbolic values");
    }
    return appliesTo;
  }

  // ------------------------------------------------------------- section 12: actions and functions

  /**
   * Sections 12.2 and 12.4: an action or function is a schema member whose value is an array with one
   * object per overload. The overload's own $Kind discriminates the two, so a single member may not
   * mix them - but each item is validated on its own so the error names the offending item.
   */
  private void readOverloads(final CsdlSchema schema, final String name, final ArrayNode node,
      final String path) {
    for (int i = 0; i < node.size(); i++) {
      overloadKind(node, i, path); // validates every item before anything is added to the schema
    }
    for (int i = 0; i < node.size(); i++) {
      final String itemPath = item(path, i);
      final ObjectNode overload = objectNode(node.get(i), itemPath);
      if (KIND_ACTION.equals(overloadKind(node, i, path))) {
        final CsdlAction action = new CsdlAction();
        action.setName(name);
        readOperation(action, overload, itemPath);
        schema.getActions().add(action);
      } else {
        final CsdlFunction function = new CsdlFunction();
        function.setName(name);
        // Section 12.7: "If not explicitly indicated, it is not composable."
        function.setComposable(flag(overload, IS_COMPOSABLE));
        readOperation(function, overload, itemPath);
        if (function.getReturnType() == null) {
          // Section 12.4: a function overload "MUST contain the member $ReturnType" - unlike an action.
          throw new CsdlJsonParseException(child(itemPath, RETURN_TYPE),
              "a function overload must declare its $ReturnType");
        }
        schema.getFunctions().add(function);
      }
    }
  }

  private void readOperation(final CsdlOperation operation, final ObjectNode node, final String path) {
    // Section 12.5: "Absence of the member means false."
    operation.setBound(flag(node, IS_BOUND));
    // Section 12.6: stored as written; it is split into binding parameter and segments at resolution
    // time, exactly as the CSDL XML deserializer leaves it.
    operation.setEntitySetPath(text(node, ENTITY_SET_PATH, null));
    operation.setParameters(node.has(PARAMETER)
        ? readParameters(arrayNode(node, PARAMETER, path), child(path, PARAMETER))
        : new ArrayList<>());
    if (node.has(RETURN_TYPE)) {
      operation.setReturnType(readReturnType(objectNode(node, RETURN_TYPE, path), child(path, RETURN_TYPE)));
    }
    readAnnotations(node, operation, "", path);
  }

  /** Section 12.9: an ordered array of objects, each carrying its own $Name. */
  private List<CsdlParameter> readParameters(final ArrayNode node, final String path) {
    final List<CsdlParameter> parameters = new ArrayList<>();
    for (int i = 0; i < node.size(); i++) {
      final String itemPath = item(path, i);
      final ObjectNode entry = objectNode(node.get(i), itemPath);
      final CsdlParameter parameter = new CsdlParameter();
      parameter.setName(requireText(entry, NAME, itemPath));
      // "Absence of the $Type member means the type is Edm.String."
      parameter.setType(resolveName(text(entry, TYPE, EDM_STRING)));
      parameter.setCollection(flag(entry, COLLECTION));
      parameter.setNullable(flag(entry, NULLABLE));
      readFacets(entry, facets(parameter), itemPath);
      readAnnotations(entry, parameter, "", itemPath);
      parameters.add(parameter);
    }
    return parameters;
  }

  /** Section 12.8: $Type defaults to Edm.String and $Nullable to false, plus the section 7.2 facets. */
  private CsdlReturnType readReturnType(final ObjectNode node, final String path) {
    final CsdlReturnType returnType = new CsdlReturnType();
    returnType.setType(resolveName(text(node, TYPE, EDM_STRING)));
    returnType.setCollection(flag(node, COLLECTION));
    returnType.setNullable(flag(node, NULLABLE));
    readFacets(node, facets(returnType), path);
    readAnnotations(node, returnType, "", path);
    return returnType;
  }

  /**
   * Sections 12.2/12.4: "The action overload object MUST contain the member $Kind with a string value
   * of Action" and likewise Function.
   */
  private static String overloadKind(final ArrayNode node, final int index, final String path) {
    final String itemPath = item(path, index);
    final String kind = text(objectNode(node.get(index), itemPath), KIND, null);
    if (!KIND_ACTION.equals(kind) && !KIND_FUNCTION.equals(kind)) {
      throw new CsdlJsonParseException(child(itemPath, KIND),
          "an overload object must carry $Kind with the value Action or Function");
    }
    return kind;
  }

  // --------------------------------------------------------------- section 13: the entity container

  /**
   * Installs the entity container of a schema member whose $Kind is EntityContainer. Section 13 allows
   * exactly one entity container per metadata document; when a document declares more than one, the
   * document's $EntityContainer member says which one is the service's, and the others are dropped.
   * Without that member two containers cannot be told apart, which is an error.
   */
  private void readContainer(final CsdlSchema schema, final String name, final ObjectNode node,
      final String path) {
    final String qualifiedName = schema.getNamespace() + "." + name;
    final String service = resolveName(this.serviceContainerFqn);
    if (this.containerOwner != null) {
      if (service == null) {
        throw new CsdlJsonParseException(path,
            "a metadata document defines exactly one entity container; $EntityContainer names it");
      }
      if (!service.equals(qualifiedName)) {
        return; // not the service's container, and one is already in the model
      }
      this.containerOwner.setEntityContainer(null);
    }
    schema.setEntityContainer(readEntityContainer(name, node, path));
    this.containerOwner = schema;
  }

  private CsdlEntityContainer readEntityContainer(final String name, final ObjectNode node,
      final String path) {
    final CsdlEntityContainer container = new CsdlEntityContainer();
    container.setName(name);
    container.setEntitySets(new ArrayList<>());
    container.setSingletons(new ArrayList<>());
    container.setActionImports(new ArrayList<>());
    container.setFunctionImports(new ArrayList<>());
    if (node.has(EXTENDS)) {
      // Section 13.1: "the qualified name of the entity container to be extended".
      container.setExtendsContainer(resolveName(requireText(node, EXTENDS, path)));
    }
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String memberName = member.getKey();
      if (memberName.startsWith(DOLLAR) || memberName.contains(AT)) {
        continue; // $Kind, $Extends and annotations on the container or on one of its children
      }
      final String memberPath = child(path, memberName);
      readContainerMember(container, memberName, objectNode(member.getValue(), memberPath), memberPath);
    }
    readAnnotations(node, container, "", path);
    return container;
  }

  private void readContainerMember(final CsdlEntityContainer container, final String name,
      final ObjectNode node, final String path) {
    final String kind = text(node, KIND, null);
    if (node.has(ACTION)) {
      container.getActionImports().add(readActionImport(name, node, path));
    } else if (node.has(FUNCTION)) {
      container.getFunctionImports().add(readFunctionImport(name, node, path));
    } else if (node.has(COLLECTION) || KIND_ENTITY_SET.equals(kind)) {
      // Section 13.2: "The entity set object MUST contain the members $Collection and $Type." The
      // pre-conformance writer omitted $Collection and wrote "$Kind": "EntitySet" instead.
      container.getEntitySets().add(readEntitySet(name, node, path));
    } else if (node.has(TYPE)) {
      // Section 13.3: a singleton has a $Type and no $Collection.
      container.getSingletons().add(readSingleton(name, node, path));
    } else if (KIND_ENTITY_CONTAINER.equals(kind) || LEGACY_EXTENDING.equals(name)) {
      // The pre-conformance writer nested the container's inheritance in an "Extending" object.
      if (node.has(EXTENDS)) {
        container.setExtendsContainer(resolveName(requireText(node, EXTENDS, path)));
      }
    } else {
      throw new CsdlJsonParseException(path,
          "an entity container member is an entity set, a singleton, an action import or a function import");
    }
  }

  private CsdlEntitySet readEntitySet(final String name, final ObjectNode node, final String path) {
    final CsdlEntitySet entitySet = new CsdlEntitySet();
    entitySet.setName(name);
    entitySet.setType(resolveName(requireText(node, TYPE, path)));
    // Section 13.2: "Absence of the member means true."
    entitySet.setIncludeInServiceDocument(!node.has(INCLUDE_IN_SERVICE_DOCUMENT)
        || flag(node, INCLUDE_IN_SERVICE_DOCUMENT));
    entitySet.setNavigationPropertyBindings(readNavigationPropertyBindings(node, path));
    readAnnotations(node, entitySet, "", path);
    return entitySet;
  }

  private CsdlSingleton readSingleton(final String name, final ObjectNode node, final String path) {
    final CsdlSingleton singleton = new CsdlSingleton();
    singleton.setName(name);
    singleton.setType(resolveName(requireText(node, TYPE, path)));
    // Section 13.3 also allows $Nullable on a 4.01 singleton; CsdlSingleton has no place for it, and
    // the CSDL XML deserializer drops the equivalent attribute too.
    singleton.setNavigationPropertyBindings(readNavigationPropertyBindings(node, path));
    readAnnotations(node, singleton, "", path);
    return singleton;
  }

  private CsdlActionImport readActionImport(final String name, final ObjectNode node, final String path) {
    final CsdlActionImport actionImport = new CsdlActionImport();
    actionImport.setName(name);
    // Section 13.5: "The value of $Action is a string containing the qualified name of an unbound
    // action."
    actionImport.setAction(resolveName(requireText(node, ACTION, path)));
    if (node.has(ENTITY_SET)) {
      actionImport.setEntitySet(unqualified(requireText(node, ENTITY_SET, path)));
    }
    readAnnotations(node, actionImport, "", path);
    return actionImport;
  }

  private CsdlFunctionImport readFunctionImport(final String name, final ObjectNode node,
      final String path) {
    final CsdlFunctionImport functionImport = new CsdlFunctionImport();
    functionImport.setName(name);
    functionImport.setFunction(resolveName(requireText(node, FUNCTION, path)));
    if (node.has(ENTITY_SET)) {
      functionImport.setEntitySet(unqualified(requireText(node, ENTITY_SET, path)));
    }
    // Section 13.6: "If not explicitly indicated, it is not included." - the opposite of section 13.2.
    functionImport.setIncludeInServiceDocument(flag(node, INCLUDE_IN_SERVICE_DOCUMENT));
    readAnnotations(node, functionImport, "", path);
    return functionImport;
  }

  /**
   * Section 13.4.2: "an object ... whose name is the navigation property binding path and whose value
   * is a string containing the navigation property binding target". Both are paths, so neither is
   * alias-resolved.
   */
  private static List<CsdlNavigationPropertyBinding> readNavigationPropertyBindings(final ObjectNode node,
      final String path) {
    final List<CsdlNavigationPropertyBinding> bindings = new ArrayList<>();
    if (!node.has(NAVIGATION_PROPERTY_BINDING)) {
      return bindings;
    }
    final String bindingPath = child(path, NAVIGATION_PROPERTY_BINDING);
    final ObjectNode value = objectNode(node, NAVIGATION_PROPERTY_BINDING, path);
    final Iterator<Map.Entry<String, JsonNode>> members = value.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      if (member.getKey().contains(AT)) {
        continue; // "<path>@<term>" annotates the binding of that path
      }
      if (!member.getValue().isTextual()) {
        throw new CsdlJsonParseException(child(bindingPath, member.getKey()),
            "a navigation property binding maps a path to the name or path of its target");
      }
      bindings.add(new CsdlNavigationPropertyBinding()
          .setPath(member.getKey())
          .setTarget(member.getValue().asText()));
    }
    return bindings;
  }

  /**
   * Sections 13.5/13.6: $EntitySet is "either the unqualified name of an entity set in the same entity
   * container or a path to an entity set in a different entity container". The model keeps the
   * unqualified name; a target path keeps both of its segments, only its container half carries a dot.
   */
  private static String unqualified(final String target) {
    if (target.indexOf('/') >= 0) {
      return target;
    }
    return target.substring(target.lastIndexOf('.') + 1);
  }

  // ---------------------------------------------------------------------- section 14.2: annotations

  /**
   * Section 14.2: "An annotation is represented as a member whose name consists of an at (@)
   * character, followed by the qualified name of a term, optionally followed by a hash (#) and a
   * qualifier." When the annotation targets something nested in this object rather than the object
   * itself - an enumeration member, a record member, {@code $OnDelete}, the dependent property of a
   * referential constraint, or an annotation - the member name is prefixed with the name of that
   * thing; {@code memberPrefix} is that name, and the empty string means "this object".
   *
   * @param node the JSON object carrying the annotation members
   * @param target the model element the annotations belong to
   * @param memberPrefix the target prefix the member names must carry, "" for the object itself
   * @param path the JSON path of {@code node}, for error reporting
   */
  private void readAnnotations(final ObjectNode node, final CsdlAnnotatable target,
      final String memberPrefix, final String path) {
    if (node == null || target == null) {
      return;
    }
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (!isAnnotationOf(name, memberPrefix)) {
        continue;
      }
      final CsdlAnnotation annotation =
          readAnnotation(name.substring(memberPrefix.length() + 1), member.getValue(), child(path, name));
      target.getAnnotations().add(annotation);
      // "An annotation can itself be annotated": the member name of such an annotation is this
      // member's whole name followed by another @term.
      readAnnotations(node, annotation, name, path);
    }
  }

  /**
   * Whether {@code name} is {@code prefix} followed by exactly one {@code @term[#qualifier]}. A second
   * at sign means the member annotates the annotation named by everything before it, not this object.
   */
  private static boolean isAnnotationOf(final String name, final String prefix) {
    final int at = prefix.length();
    return name.length() > at + 1
        && name.startsWith(prefix)
        && name.charAt(at) == AT.charAt(0)
        && name.indexOf(AT, at + 1) < 0
        && !CONTROL_INFORMATION.contains(name.substring(at));
  }

  /**
   * @param member the annotation member name with its at sign and any target prefix stripped, so
   *        {@code term} or {@code term#qualifier}
   */
  private CsdlAnnotation readAnnotation(final String member, final JsonNode value, final String path) {
    final CsdlAnnotation annotation = new CsdlAnnotation();
    final int hash = member.indexOf('#');
    if (hash < 0) {
      // The term name is a qualified name, so a document alias stands for its namespace.
      annotation.setTerm(resolveName(member));
    } else {
      annotation.setTerm(resolveName(member.substring(0, hash)));
      // Section 14.2.1: "The qualifier is a simple identifier."
      annotation.setQualifier(member.substring(hash + 1));
    }
    annotation.setExpression(readExpression(value, path));
    return annotation;
  }

  /**
   * Section 5.2: {@code $Annotations} is "an object with one member per annotation target", the member
   * name being the target path optionally followed by a hash and a qualifier.
   */
  private void readAnnotationGroups(final ObjectNode node, final CsdlSchema schema, final String path) {
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      final String memberPath = child(path, name);
      final CsdlAnnotations group = new CsdlAnnotations();
      final int hash = name.indexOf('#');
      if (hash < 0) {
        group.setTarget(resolveTarget(name));
      } else {
        group.setTarget(resolveTarget(name.substring(0, hash)));
        group.setQualifier(name.substring(hash + 1));
      }
      readAnnotations(objectNode(member.getValue(), memberPath), group, "", memberPath);
      schema.getAnnotationGroups().add(group);
    }
  }

  /**
   * Section 14.2.2: a target path interleaves qualified names with the delimiters {@code / ( ) ,}, and
   * "All qualified names used in a target path MUST be in scope", so every one of them may be written
   * with a document alias. Only the qualified names are resolved; the simple identifiers between them
   * have no dot and pass through {@link #resolveName(String)} unchanged.
   */
  private String resolveTarget(final String target) {
    final StringBuilder resolved = new StringBuilder(target.length());
    int segment = 0;
    for (int i = 0; i < target.length(); i++) {
      final char c = target.charAt(i);
      if (c == '/' || c == '(' || c == ')' || c == ',') {
        resolved.append(resolveName(target.substring(segment, i))).append(c);
        segment = i + 1;
      }
    }
    return resolved.append(resolveName(target.substring(segment))).toString();
  }

  // ------------------------------------------------------- sections 14.3 and 14.4: the expressions

  /**
   * Sections 14.3 and 14.4. A CSDL JSON constant is a bare JSON value - there are no {@code $Binary},
   * {@code $Date}, {@code $Int}, ... members, those are CSDL XML element names - so the constant's
   * {@link ConstantExpressionType} is recovered from the JSON shape alone. What a string means is
   * governed by the declared type of the applied term, which a document that does not define the term
   * cannot tell us; this is a property of the format, not a limitation of this reader.
   */
  private CsdlExpression readExpression(final JsonNode value, final String path) {
    if (value == null || value.isNull()) {
      // Section 14.4.11: "Null expressions that do not contain annotations are represented as the
      // literal null."
      return new CsdlNull();
    }
    if (value.isBoolean()) {
      // Section 14.3.2: "Boolean expressions are represented as the literals true or false."
      return new CsdlConstantExpression(ConstantExpressionType.Bool, value.asText());
    }
    if (value.isIntegralNumber()) {
      // Section 14.3.10: an integer is a JSON number (or, above the safe range, a JSON string, which
      // is indistinguishable from any other string and therefore read as one).
      return new CsdlConstantExpression(ConstantExpressionType.Int, value.asText());
    }
    if (value.isNumber()) {
      // Sections 14.3.5 and 14.3.8: a decimal or floating-point value as a JSON number.
      return new CsdlConstantExpression(ConstantExpressionType.Float, value.asText());
    }
    if (value.isTextual()) {
      // Sections 14.3.1/.3/.4/.6/.7/.9/.11/.12 all render as a JSON string.
      return new CsdlConstantExpression(ConstantExpressionType.String, value.asText());
    }
    if (value.isArray()) {
      // Section 14.4.6: "Collection expressions are represented as arrays with one array item per item
      // expression"; there is no $Collection wrapper.
      final List<CsdlExpression> items = new ArrayList<>();
      for (int i = 0; i < value.size(); i++) {
        items.add(readExpression(value.get(i), item(path, i)));
      }
      return new CsdlCollection().setItems(items);
    }
    return readObjectExpression(objectNode(value, path), path);
  }

  private CsdlExpression readObjectExpression(final ObjectNode node, final String path) {
    if (node.has(PATH)) {
      // Section 14.4.1: the value is a path, never a qualified name, so it is never alias-resolved.
      return new CsdlPath().setValue(requireText(node, PATH, path));
    }
    if (node.has(ANNOTATION_PATH)) {
      return new CsdlAnnotationPath().setValue(requireText(node, ANNOTATION_PATH, path));
    }
    if (node.has(NAVIGATION_PROPERTY_PATH)) {
      return new CsdlNavigationPropertyPath().setValue(requireText(node, NAVIGATION_PROPERTY_PATH, path));
    }
    if (node.has(PROPERTY_PATH)) {
      return new CsdlPropertyPath().setValue(requireText(node, PROPERTY_PATH, path));
    }
    for (final String operator : UNSUPPORTED_OPERATORS) {
      if (node.has(operator)) {
        throw new CsdlJsonParseException(child(path, operator),
            "the " + operator + " operator has no LogicalOrComparisonExpressionType in this model");
      }
    }
    for (final String operator : BINARY_OPERATORS) {
      if (node.has(operator)) {
        return readBinaryOperator(node, operator, path);
      }
    }
    if (node.has(NOT)) {
      // Section 14.4.2: "Negation expressions are represented as an object with a single member $Not
      // whose value is an annotation expression."
      final CsdlLogicalOrComparisonExpression not =
          new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Not)
              .setLeft(readExpression(node.get(NOT), child(path, NOT)));
      readAnnotations(node, not, "", path);
      return not;
    }
    if (node.has(APPLY)) {
      return readApply(node, path);
    }
    if (node.has(CAST)) {
      final CsdlCast cast = new CsdlCast()
          .setValue(readExpression(node.get(CAST), child(path, CAST)))
          .setType(castType(node, path));
      readFacets(node, facets(cast), path);
      readAnnotations(node, cast, "", path);
      return cast;
    }
    if (node.has(IS_OF)) {
      final CsdlIsOf isOf = new CsdlIsOf()
          .setValue(readExpression(node.get(IS_OF), child(path, IS_OF)))
          .setType(castType(node, path));
      readFacets(node, facets(isOf), path);
      readAnnotations(node, isOf, "", path);
      return isOf;
    }
    if (node.has(IF)) {
      return readIf(node, path);
    }
    if (node.has(LABELED_ELEMENT)) {
      final CsdlLabeledElement labeled = new CsdlLabeledElement()
          .setValue(readExpression(node.get(LABELED_ELEMENT), child(path, LABELED_ELEMENT)))
          .setName(requireText(node, NAME, path));
      readAnnotations(node, labeled, "", path);
      return labeled;
    }
    if (node.has(LABELED_ELEMENT_REFERENCE)) {
      // Section 14.4.10: the value is a qualified name, so a document alias stands for its namespace.
      return new CsdlLabeledElementReference()
          .setValue(resolveName(requireText(node, LABELED_ELEMENT_REFERENCE, path)));
    }
    if (node.has(NULL)) {
      // Section 14.4.11: "Null expression containing annotations are represented as an object with a
      // member $Null whose value is the literal null."
      final CsdlNull nullExpression = new CsdlNull();
      readAnnotations(node, nullExpression, "", path);
      return nullExpression;
    }
    if (node.has(URL_REF)) {
      final CsdlUrlRef urlRef =
          new CsdlUrlRef().setValue(readExpression(node.get(URL_REF), child(path, URL_REF)));
      readAnnotations(node, urlRef, "", path);
      return urlRef;
    }
    final CsdlExpression legacy = readLegacyConstant(node, path);
    if (legacy != null) {
      return legacy;
    }
    rejectUnknownExpression(node, path);
    return readRecord(node, path);
  }

  /**
   * Legacy input tolerance: an object whose single non-annotation member is a CSDL XML element name is
   * the constant expression the pre-conformance Olinguito writer produced. Conformant CSDL JSON has no
   * such members, and nothing in this library writes them any more.
   */
  private static CsdlExpression readLegacyConstant(final ObjectNode node, final String path) {
    for (final ConstantExpressionType type : ConstantExpressionType.values()) {
      final String member = DOLLAR + type.name();
      if (node.has(member)) {
        final JsonNode value = node.get(member);
        if (value.isContainerNode()) {
          throw new CsdlJsonParseException(child(path, member), "a constant expression value is not an object");
        }
        return new CsdlConstantExpression(type, value.asText());
      }
    }
    return null;
  }

  /**
   * Sections 14.4.5 and 14.4.8: "a member $Type whose value is a string containing the qualified type
   * name, and optionally a member $Collection with a value of true". The Csdl model has no collection
   * flag on a cast, it keeps the same {@code Collection(...)} type expression the CSDL XML attribute
   * carries, which is what {@code EdmCastImpl} parses; dropping $Collection would turn a cast to a
   * collection into a cast to a scalar.
   */
  private String castType(final ObjectNode node, final String path) {
    final String type = resolveName(requireText(node, TYPE, path));
    return flag(node, COLLECTION) ? "Collection(" + type + ")" : type;
  }

  /**
   * Section 14.4.12: a record is "an object with one member per property value expression", and a
   * property name is a simple identifier - it never starts with a dollar. So an object whose only
   * members are $-prefixed ones is not a record; it is a dynamic expression this reader does not know,
   * either a form the Csdl model has no class for (such as $ModelElementPath) or a misspelling. It is
   * reported rather than silently turned into an empty record. The legacy record's $Type is the one
   * $-member that does belong to a record.
   */
  private static void rejectUnknownExpression(final ObjectNode node, final String path) {
    String unknown = null;
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final String name = members.next().getKey();
      if (!name.startsWith(DOLLAR)) {
        if (!name.contains(AT)) {
          return; // a property value expression, so this really is a record
        }
      } else if (unknown == null && !TYPE.equals(name)) {
        unknown = name;
      }
    }
    if (unknown != null) {
      throw new CsdlJsonParseException(child(path, unknown),
          "the member " + unknown + " is not a constant or dynamic expression this parser knows");
    }
  }

  /**
   * Section 14.4.2: "represented as an object with a single member whose value is an array with two
   * annotation expressions".
   */
  private CsdlExpression readBinaryOperator(final ObjectNode node, final String operator,
      final String path) {
    final String operatorPath = child(path, operator);
    final JsonNode operands = node.get(operator);
    if (!operands.isArray() || operands.size() != 2) {
      throw new CsdlJsonParseException(operatorPath, "a comparison or logical operator takes two operands");
    }
    final CsdlLogicalOrComparisonExpression expression = new CsdlLogicalOrComparisonExpression(
        LogicalOrComparisonExpressionType.valueOf(operator.substring(1)))
        .setLeft(readExpression(operands.get(0), item(operatorPath, 0)))
        .setRight(readExpression(operands.get(1), item(operatorPath, 1)));
    readAnnotations(node, expression, "", path);
    return expression;
  }

  /**
   * Section 14.4.4: "an object with a member $Apply whose value is an array of annotation expressions,
   * and a member $Function whose value is a string containing the qualified name of the client-side
   * function".
   */
  private CsdlExpression readApply(final ObjectNode node, final String path) {
    final String applyPath = child(path, APPLY);
    final JsonNode arguments = node.get(APPLY);
    if (!arguments.isArray()) {
      throw new CsdlJsonParseException(applyPath, "$Apply is an array of annotation expressions");
    }
    final List<CsdlExpression> parameters = new ArrayList<>();
    for (int i = 0; i < arguments.size(); i++) {
      parameters.add(readExpression(arguments.get(i), item(applyPath, i)));
    }
    // The client-side function name is a qualified name; odata.* is reserved by the specification.
    final CsdlApply apply = new CsdlApply()
        .setFunction(resolveName(requireText(node, FUNCTION, path)))
        .setParameters(parameters);
    readAnnotations(node, apply, "", path);
    return apply;
  }

  /** Section 14.4.7: "an object with a member $If whose value is an array of two or three ... ". */
  private CsdlExpression readIf(final ObjectNode node, final String path) {
    final String ifPath = child(path, IF);
    final JsonNode branches = node.get(IF);
    if (!branches.isArray() || branches.size() < 2 || branches.size() > 3) {
      throw new CsdlJsonParseException(ifPath, "$If is an array of two or three annotation expressions");
    }
    final CsdlIf conditional = new CsdlIf()
        .setGuard(readExpression(branches.get(0), item(ifPath, 0)))
        .setThen(readExpression(branches.get(1), item(ifPath, 1)));
    if (branches.size() == 3) {
      conditional.setElse(readExpression(branches.get(2), item(ifPath, 2)));
    }
    readAnnotations(node, conditional, "", path);
    return conditional;
  }

  /**
   * Section 14.4.12: "Record expressions are represented as objects with one member per property value
   * expression", with the type in the {@code @type} control information and "Annotations for record
   * members ... prefixed with the member name".
   */
  private CsdlExpression readRecord(final ObjectNode node, final String path) {
    final CsdlRecord record = new CsdlRecord();
    final String type = recordType(node);
    if (type != null) {
      record.setType(resolveName(type));
    }
    final List<CsdlPropertyValue> propertyValues = new ArrayList<>();
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (name.startsWith(DOLLAR) || name.contains(AT)) {
        continue; // the legacy $Type, the @type control information and the annotations
      }
      final String memberPath = child(path, name);
      final CsdlPropertyValue propertyValue = new CsdlPropertyValue()
          .setProperty(name)
          .setValue(readExpression(member.getValue(), memberPath));
      readAnnotations(node, propertyValue, name, path);
      propertyValues.add(propertyValue);
    }
    record.setPropertyValues(propertyValues);
    readAnnotations(node, record, "", path);
    return record;
  }

  /**
   * The record's type name. Section 14.4.12 carries it in the {@code @type} control information, whose
   * value may be a bare qualified name, the {@code #}-prefixed short form or - as in the specification's
   * own Example 86 - a URI with the qualified name as its fragment; everything after the last hash is
   * the name in all three. A {@code $Type} member is the pre-conformance Olinguito shape, read but
   * never written.
   */
  private static String recordType(final ObjectNode node) {
    JsonNode type = null;
    // The order is fixed: @type is the control information section 14.4.12 names, @odata.type is its
    // 4.0 spelling. Iterating a set would be salted per JVM and nondeterministic when both appear.
    for (final String member : new String[] {AT_TYPE, ODATA_AT_TYPE}) {
      if (type == null && node.hasNonNull(member) && node.get(member).isTextual()) {
        type = node.get(member);
      }
    }
    if (type == null && node.hasNonNull(TYPE) && node.get(TYPE).isTextual()) {
      type = node.get(TYPE);
    }
    return type == null ? null : type.asText().substring(type.asText().lastIndexOf('#') + 1);
  }

  // ------------------------------------------------------------------------ section 7.2: the facets

  /** The subset of a Csdl type that carries type facets; the Csdl types do not share an interface. */
  private interface FacetSink {
    void maxLength(Integer value);

    void precision(Integer value);

    void scale(Integer value);

    /** Only CsdlProperty models the symbolic scale; the others skip it, as the XML deserializer does. */
    default void scaleAsString(final String value) {
      // ignored - the target has no place to keep the symbolic value
    }

    /** Only properties and type definitions carry $Unicode. */
    default void unicode(final boolean value) {
      // ignored - the target has no Unicode facet
    }

    void srid(SRID value);
  }

  private static void readFacets(final ObjectNode node, final FacetSink sink, final String path) {
    if (node.has(MAX_LENGTH)) {
      if (node.get(MAX_LENGTH).isTextual()) {
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
        sink.scaleAsString(symbolic.toLowerCase(Locale.ROOT));
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

  private static FacetSink facets(final CsdlParameter parameter) {
    return new FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        parameter.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        parameter.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        parameter.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        parameter.setSrid(value);
      }
    };
  }

  private static FacetSink facets(final CsdlReturnType returnType) {
    return new FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        returnType.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        returnType.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        returnType.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        returnType.setSrid(value);
      }
    };
  }

  private static FacetSink facets(final CsdlCast cast) {
    return new FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        cast.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        cast.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        cast.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        cast.setSrid(value);
      }
    };
  }

  private static FacetSink facets(final CsdlIsOf isOf) {
    return new FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        isOf.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        isOf.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        isOf.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        isOf.setSrid(value);
      }
    };
  }

  // ------------------------------------------------------------------------------------- plumbing

  private static String requireText(final ObjectNode node, final String member, final String path) {
    final JsonNode value = node.get(member);
    if (value == null || !value.isTextual()) {
      throw new CsdlJsonParseException(child(path, member), "the member " + member + " must be a string");
    }
    return value.asText();
  }

  private static String text(final ObjectNode node, final String member, final String defaultValue) {
    final JsonNode value = node.get(member);
    return value != null && value.isTextual() ? value.asText() : defaultValue;
  }

  private static boolean flag(final ObjectNode node, final String member) {
    final JsonNode value = node.get(member);
    return value != null && value.asBoolean(false);
  }

  private static Integer integer(final ObjectNode node, final String member, final String path) {
    final JsonNode value = node.get(member);
    if (value == null || !value.canConvertToInt()) {
      throw new CsdlJsonParseException(child(path, member), "the member " + member + " must be an integer");
    }
    return value.intValue();
  }

  private static ObjectNode objectNode(final ObjectNode parent, final String member, final String path) {
    return objectNode(parent.get(member), child(path, member));
  }

  private static ObjectNode objectNode(final Map.Entry<String, JsonNode> member, final String path) {
    return objectNode(member.getValue(), child(path, member.getKey()));
  }

  private static ObjectNode objectNode(final JsonNode node, final String path) {
    if (node == null || !node.isObject()) {
      throw new CsdlJsonParseException(path, "a JSON object is expected here");
    }
    return (ObjectNode) node;
  }

  private static ArrayNode arrayNode(final ObjectNode parent, final String member, final String path) {
    final JsonNode node = parent.get(member);
    if (node == null || !node.isArray()) {
      throw new CsdlJsonParseException(child(path, member), "a JSON array is expected here");
    }
    return (ArrayNode) node;
  }

  private static String child(final String path, final String member) {
    return path.isEmpty() ? member : path + "/" + member;
  }

  private static String item(final String path, final int index) {
    return path + "[" + index + "]";
  }

  /**
   * A member of the document that does not hold what the format says it holds. It is unchecked so the
   * tree walk stays readable, and never escapes {@link #parse(InputStream)}, which reports every
   * failure as the {@code IllegalArgumentException} the CSDL XML entry point reports.
   */
  private static final class CsdlJsonParseException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 4232659128140925484L;

    CsdlJsonParseException(final String path, final String message) {
      super(path.isEmpty() ? message : path + ": " + message);
    }

    CsdlJsonParseException(final String path, final String message, final Throwable cause) {
      super(path.isEmpty() ? message : path + ": " + message, cause);
    }
  }
}
