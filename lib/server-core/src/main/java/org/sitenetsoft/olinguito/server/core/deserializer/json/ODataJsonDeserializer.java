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
 * Copyright 2026 SiteNetSoft - Replaced Apache Commons with Java standard library
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Fixed odata.bind validation to use endsWith (OLINGO-1620)
 * Copyright 2026 SiteNetSoft - Modernized instanceof to pattern matching
 * Copyright 2026 SiteNetSoft - Port OLINGO-1181: deep-insert navigation/binding links for complex types
 * Copyright 2026 SiteNetSoft - Fixed nullable collection parameters in actions (OLINGO-1633)
 * Copyright 2026 SiteNetSoft - Fixed Valuable.asCollection()
 * ClassCastException on entity collection parameters (OLINGO-1638)
 * Copyright 2026 SiteNetSoft - OLINGO-1354: USE_BIG_DECIMAL_FOR_FLOATS
 * in action parameter deserialization
 * Copyright 2026 SiteNetSoft - OLINGO-1590: Check EdmTypeKind before
 * assuming "Geo" prefix means geospatial type
 * Copyright 2026 SiteNetSoft - OLINGO-1236: accept "NaN"/"INF"/"-INF" string
 * values for Single/Double properties
 * Copyright 2026 SiteNetSoft - Add OpenType support (dynamic property deserialization)
 * Copyright 2026 SiteNetSoft - OpenType: support name@odata.type annotations and
 * primitive collections for dynamic properties
 * Copyright 2026 SiteNetSoft - OpenType: accept dynamic properties inside open complex values
 * Copyright 2026 SiteNetSoft - OpenType: resolve annotated element type for dynamic collection
 * properties instead of ignoring name@odata.type on arrays
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 1: implement ODataDeserializer.dynamicProperty
 * Copyright 2026 SiteNetSoft - OData 4.01: apply default values of omitted optional action parameters
 * Copyright 2026 SiteNetSoft - OData 4.01: read optional parameter defaults as URI literals
 * Copyright 2026 SiteNetSoft - OData 4.01: shared resolver for optional-parameter default values
 * Copyright 2026 SiteNetSoft - Keep geo values geospatial: value type, collection member
 * dimension and the GeoJSON CRS "type: name" requirement
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 2 Task 9: read the entity reference of an entity-typed
 * action parameter value in both format versions
 */
package org.sitenetsoft.olinguito.server.core.deserializer.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.IConstants;
import org.sitenetsoft.olinguito.commons.api.constants.Constantsv00;
import org.sitenetsoft.olinguito.commons.api.constants.Constantsv01;
import org.sitenetsoft.olinguito.commons.api.data.Annotation;
import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.DeletedEntity;
import org.sitenetsoft.olinguito.commons.api.data.DeletedEntity.Reason;
import org.sitenetsoft.olinguito.commons.api.data.Delta;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Link;
import org.sitenetsoft.olinguito.commons.api.data.Parameter;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmMapping;
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
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.GeospatialCollection;
import org.sitenetsoft.olinguito.commons.api.edm.geo.LineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiLineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiPoint;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiPolygon;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Polygon;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException.MessageKeys;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerResult;
import org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.deserializer.DeserializerResultImpl;
import org.sitenetsoft.olinguito.server.core.deserializer.helper.ExpandTreeBuilder;
import org.sitenetsoft.olinguito.server.core.deserializer.helper.ExpandTreeBuilderImpl;
import org.sitenetsoft.olinguito.server.core.serializer.utils.ContentTypeHelper;
import org.sitenetsoft.olinguito.server.core.uri.parser.OptionalParameterDefaults;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ODataJsonDeserializer implements ODataDeserializer {

  private static final Map<String, Class<? extends Geospatial>> jsonNameToGeoDataType;
  static {
    Map<String, Class<? extends Geospatial>> temp = new HashMap<>();
    temp.put(Constants.ELEM_POINT, Point.class);
    temp.put(Constants.ELEM_MULTIPOINT, MultiPoint.class);
    temp.put(Constants.ELEM_LINESTRING, LineString.class);
    temp.put("MultiLineString", MultiLineString.class);
    temp.put(Constants.ELEM_POLYGON, Polygon.class);
    temp.put("MultiPolygon", MultiPolygon.class);
    temp.put("GeometryCollection", GeospatialCollection.class);
    jsonNameToGeoDataType = Collections.unmodifiableMap(temp);
  }

  private static final String ODATA_ANNOTATION_MARKER = "@";
  private static final String ODATA_CONTROL_INFORMATION_PREFIX = "@odata.";
  private static final String REASON = "reason";
  private static final String ODATA_STREAM_PROPERTY_MEDIA_READ_LINK = "mediaReadLink";
  private static final String ODATA_STREAM_PROPERTY_MEDIA_EDIT_LINK = "mediaEditLink";
  private static final String ODATA_STREAM_PROPERTY_MEDIA_MIME_TYPE = "mediaMimeType";
  private static final String COLLECTION_TYPE_PREFIX = "Collection(";

  private final boolean isIEEE754Compatible;
  private ServiceMetadata serviceMetadata;
  private IConstants constants;
  private ODataJsonInstanceAnnotationDeserializer instanceAnnotDeserializer;

  public ODataJsonDeserializer(final ContentType contentType) {
    this(contentType, null, new Constantsv00());
  }

  public ODataJsonDeserializer(final ContentType contentType, final ServiceMetadata serviceMetadata) {
    isIEEE754Compatible = ContentTypeHelper.isODataIEEE754Compatible(contentType);
    this.serviceMetadata = serviceMetadata;
    this.constants = new Constantsv00();
    instanceAnnotDeserializer = new ODataJsonInstanceAnnotationDeserializer();
  }

  public ODataJsonDeserializer(ContentType contentType, ServiceMetadata serviceMetadata, IConstants constants) {
    isIEEE754Compatible = ContentTypeHelper.isODataIEEE754Compatible(contentType);
    this.serviceMetadata = serviceMetadata;
    this.constants = constants;
    instanceAnnotDeserializer = new ODataJsonInstanceAnnotationDeserializer();
  }

  public ODataJsonDeserializer(ContentType contentType, IConstants constants) {
    isIEEE754Compatible = ContentTypeHelper.isODataIEEE754Compatible(contentType);
    this.constants = constants;
    instanceAnnotDeserializer = new ODataJsonInstanceAnnotationDeserializer();
  }

  @Override
  public DeserializerResult entityCollection(final InputStream stream, final EdmEntityType edmEntityType)
      throws DeserializerException {
    try {
      return DeserializerResultImpl.with().entityCollection(
          consumeEntityCollectionNode(edmEntityType, parseJsonTree(stream), null))
          .build();
    } catch (final IOException e) {
      throw wrapParseException(e);
    }
  }

  private EntityCollection consumeEntityCollectionNode(final EdmEntityType edmEntityType, final ObjectNode tree,
      final ExpandTreeBuilder expandBuilder) throws DeserializerException {
    EntityCollection entitySet = new EntityCollection();

    // Consume entities
    JsonNode jsonNode = tree.get(Constants.VALUE);
    if (jsonNode != null) {
      entitySet.getEntities().addAll(consumeEntitySetArray(edmEntityType, jsonNode, expandBuilder));
      tree.remove(Constants.VALUE);
    } else {
      throw new DeserializerException("Could not find value array.",
          DeserializerException.MessageKeys.VALUE_ARRAY_NOT_PRESENT);
    }

    if (tree.isObject()) {
      removeAnnotations(tree);
    }
    assertJsonNodeIsEmpty(tree);

    return entitySet;
  }

  private List<Entity> consumeEntitySetArray(final EdmEntityType edmEntityType, final JsonNode jsonNode,
      final ExpandTreeBuilder expandBuilder) throws DeserializerException {
    if (jsonNode.isArray()) {
      List<Entity> entities = new ArrayList<>();
      for (JsonNode arrayElement : jsonNode) {
        if (arrayElement.isArray() || arrayElement.isValueNode()) {
          throw new DeserializerException("Nested Arrays and primitive values are not allowed for an entity value.",
              DeserializerException.MessageKeys.INVALID_ENTITY);
        }
        EdmEntityType derivedEdmEntityType = (EdmEntityType) getDerivedType(edmEntityType, arrayElement);
        entities.add(consumeEntityNode(derivedEdmEntityType, (ObjectNode) arrayElement, expandBuilder));
      }
      return entities;
    } else {
      throw new DeserializerException("The content of the value tag must be an Array but is not.",
          DeserializerException.MessageKeys.VALUE_TAG_MUST_BE_AN_ARRAY);
    }
  }

  @Override
  public DeserializerResult entity(final InputStream stream, final EdmEntityType edmEntityType)
      throws DeserializerException {
    try {
      final ObjectNode tree = parseJsonTree(stream);
      final ExpandTreeBuilder expandBuilder = ExpandTreeBuilderImpl.create();

      EdmEntityType derivedEdmEntityType = (EdmEntityType) getDerivedType(edmEntityType, tree);

      return DeserializerResultImpl.with().entity(consumeEntityNode(derivedEdmEntityType, tree, expandBuilder))
          .expandOption(expandBuilder.build())
          .build();
    } catch (final IOException e) {
      throw wrapParseException(e);
    }
  }

  private Entity consumeEntityNode(final EdmEntityType edmEntityType, final ObjectNode tree,
      final ExpandTreeBuilder expandBuilder) throws DeserializerException {
    Entity entity = new Entity();
    entity.setType(edmEntityType.getFullQualifiedName().getFullQualifiedNameAsString());
    
    // Check and consume @id for v4.01
    consumeId(tree, entity);

    // Check and consume all Properties
    consumeEntityProperties(edmEntityType, tree, entity);

    // Check and consume all expanded Navigation Properties
    consumeExpandedNavigationProperties(edmEntityType, tree, entity, expandBuilder);

    // consume delta json node fields for v4.01
    consumeDeltaJsonNodeFields(edmEntityType, tree, entity, expandBuilder);

    // consume dynamic properties for open types; this must run before
    // consumeRemainingJsonNodeFields, which strips @odata.* control information fields
    // (including name@odata.type annotations dynamic properties rely on)
    if (edmEntityType.isOpenType()) {
      consumeDynamicProperties(edmEntityType, tree, entity.getProperties());
    }

    // consume remaining json node fields
    consumeRemainingJsonNodeFields(edmEntityType, tree, entity);

    assertJsonNodeIsEmpty(tree);

    return entity;
  }

  /**
   * Consumes remaining fields on an open type as dynamic properties. Primitive scalar values are
   * accepted as-is (with their type inferred, or taken from a sibling <code>name@odata.type</code>
   * annotation, if present); arrays of primitive values are accepted as
   * {@link ValueType#COLLECTION_PRIMITIVE}. Object-valued fields (and arrays containing a non-primitive
   * element) are left untouched (dynamic complex values are unsupported) and fall through to
   * {@link #assertJsonNodeIsEmpty(JsonNode)} as unknown content.
   *
   * @param edmType edm structured type which is open
   * @param node json node which is consumed
   * @param properties the entity's property list to append dynamic properties to
   * @throws DeserializerException if an exception during consumation occurs
   */
  private void consumeDynamicProperties(final EdmStructuredType edmType, final ObjectNode node,
      final List<Property> properties) throws DeserializerException {
    final Map<String, String> dynamicTypes = new HashMap<>();
    final List<String> annotations = new ArrayList<>();
    final String typeSuffix = constants.getType();
    Iterator<Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      final Entry<String, JsonNode> field = fields.next();
      final String key = field.getKey();
      if (key.endsWith(typeSuffix) && key.length() > typeSuffix.length()) {
        String annotatedType = field.getValue().asText();
        if (annotatedType.startsWith(Constants.HASH)) {
          annotatedType = annotatedType.substring(1);
        }
        dynamicTypes.put(key.substring(0, key.length() - typeSuffix.length()), annotatedType);
        annotations.add(key);
      }
    }
    node.remove(annotations);

    final List<String> consumed = new ArrayList<>();
    fields = node.fields();
    while (fields.hasNext()) {
      final Entry<String, JsonNode> field = fields.next();
      final String name = field.getKey();
      if (name.contains(Constants.AT)) {
        continue;
      }
      final JsonNode value = field.getValue();
      if (value.isObject()) {
        continue;
      }
      if (value.isArray()) {
        final Property collectionProperty =
            createDynamicCollectionProperty(name, (ArrayNode) value, dynamicTypes.get(name));
        if (collectionProperty == null) {
          // an element in the array is not a primitive value; leave the field unconsumed
          continue;
        }
        properties.add(collectionProperty);
      } else {
        properties.add(createDynamicProperty(name, value, dynamicTypes.get(name)));
      }
      consumed.add(name);
    }
    node.remove(consumed);
  }

  private Property createDynamicProperty(final String name, final JsonNode value, final String annotatedType)
      throws DeserializerException {
    final Property property = new Property();
    property.setName(name);
    if (value.isNull()) {
      property.setValue(ValueType.PRIMITIVE, null);
      return property;
    }
    if (annotatedType == null) {
      property.setType(inferPrimitiveTypeName(value));
      property.setValue(ValueType.PRIMITIVE, inferPrimitiveValue(value));
      return property;
    }
    final EdmPrimitiveType type = resolveAnnotatedPrimitiveType(name, annotatedType);
    property.setType(type.getFullQualifiedName().getFullQualifiedNameAsString());
    property.setValue(ValueType.PRIMITIVE, parseAnnotatedPrimitiveValue(name, type, value));
    return property;
  }

  /**
   * Resolves a <code>name@odata.type</code> annotation value (already stripped of its leading
   * <code>#</code>) to an {@link EdmPrimitiveType}. Per the OData JSON format, a primitive type
   * annotation carries the bare type name (e.g. <code>DateTimeOffset</code>) without the
   * <code>Edm.</code> namespace prefix, so it is added back before resolution unless already present.
   *
   * @param name the dynamic property name (used for the exception parameter)
   * @param annotatedType the annotation value with its leading <code>#</code> already stripped
   * @throws DeserializerException wrapping {@link MessageKeys#UNKNOWN_CONTENT} if the type is not a
   *           known Edm primitive type
   */
  private EdmPrimitiveType resolveAnnotatedPrimitiveType(final String name, final String annotatedType)
      throws DeserializerException {
    final String fqnCandidate = annotatedType.contains(".")
        ? annotatedType : EdmPrimitiveType.EDM_NAMESPACE + "." + annotatedType;
    try {
      return EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.valueOfFQN(fqnCandidate));
    } catch (final IllegalArgumentException e) {
      throw new DeserializerException("Unknown annotated type: " + annotatedType + " for property: " + name, e,
          DeserializerException.MessageKeys.UNKNOWN_CONTENT, name);
    }
  }

  private Object parseAnnotatedPrimitiveValue(final String name, final EdmPrimitiveType type, final JsonNode value)
      throws DeserializerException {
    try {
      return type.valueOfString(value.asText(), true, null,
          Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, true, type.getDefaultType());
    } catch (final EdmPrimitiveTypeException e) {
      throw new DeserializerException(
          "Invalid value: " + value.asText() + " for property: " + name, e,
          DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
    }
  }

  /**
   * Builds a dynamic collection property from a JSON array. When a sibling
   * <code>name@odata.type</code> annotation (e.g. <code>#Collection(Guid)</code>) was present, every
   * element is parsed as that resolved element type via {@link EdmPrimitiveType#valueOfString}
   * (mirroring the annotated-scalar path in {@link #createDynamicProperty}); otherwise the element
   * type is inferred from the first non-null element (or defaults to <code>Edm.String</code> for an
   * empty/all-null array), as before.
   *
   * @param name the dynamic property name
   * @param value the JSON array
   * @param annotatedType the <code>Collection(...)</code>-wrapped annotated element type (with its
   *          leading <code>#</code> already stripped), or {@code null} if unannotated
   * @return the collection property, or {@code null} if unannotated and the array contains a
   *         non-primitive element (the field must then be left unconsumed so it is rejected as
   *         unknown content)
   * @throws DeserializerException if {@code annotatedType} names an unresolvable Edm type, or an
   *           element's value does not parse as that type
   */
  private Property createDynamicCollectionProperty(final String name, final ArrayNode value,
      final String annotatedType) throws DeserializerException {
    if (annotatedType != null) {
      return createAnnotatedDynamicCollectionProperty(name, value, annotatedType);
    }
    final List<Object> values = new ArrayList<>();
    String typeName = null;
    for (final JsonNode element : value) {
      if (element.isContainerNode()) {
        return null;
      }
      if (element.isNull()) {
        values.add(null);
        continue;
      }
      if (typeName == null) {
        typeName = inferPrimitiveTypeName(element);
      }
      values.add(inferPrimitiveValue(element));
    }
    final Property property = new Property();
    property.setName(name);
    property.setType(typeName == null
        ? EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString() : typeName);
    property.setValue(ValueType.COLLECTION_PRIMITIVE, values);
    return property;
  }

  /**
   * Builds a dynamic collection property whose element type is fixed by an
   * <code>name@odata.type</code> annotation (see {@link #createDynamicCollectionProperty}).
   */
  private Property createAnnotatedDynamicCollectionProperty(final String name, final ArrayNode value,
      final String annotatedType) throws DeserializerException {
    final String elementTypeName = annotatedType.startsWith(COLLECTION_TYPE_PREFIX) && annotatedType.endsWith(")")
        ? annotatedType.substring(COLLECTION_TYPE_PREFIX.length(), annotatedType.length() - 1)
        : annotatedType;
    final EdmPrimitiveType type = resolveAnnotatedPrimitiveType(name, elementTypeName);
    final List<Object> values = new ArrayList<>();
    for (final JsonNode element : value) {
      values.add(element.isNull() ? null : parseAnnotatedPrimitiveValue(name, type, element));
    }
    final Property property = new Property();
    property.setName(name);
    property.setType(type.getFullQualifiedName().getFullQualifiedNameAsString());
    property.setValue(ValueType.COLLECTION_PRIMITIVE, values);
    return property;
  }

  private String inferPrimitiveTypeName(final JsonNode value) {
    return (value.isShort() ? EdmPrimitiveTypeKind.Int16
        : value.isInt() ? EdmPrimitiveTypeKind.Int32
        : value.isLong() ? EdmPrimitiveTypeKind.Int64
        : value.isBoolean() ? EdmPrimitiveTypeKind.Boolean
        : value.isFloat() ? EdmPrimitiveTypeKind.Single
        : value.isDouble() ? EdmPrimitiveTypeKind.Double
        : value.isBigDecimal() ? EdmPrimitiveTypeKind.Decimal
        : EdmPrimitiveTypeKind.String).getFullQualifiedName().getFullQualifiedNameAsString();
  }

  private Object inferPrimitiveValue(final JsonNode value) {
    return value.isShort() ? value.shortValue()
        : value.isInt() ? value.intValue()
        : value.isLong() ? value.longValue()
        : value.isBoolean() ? value.booleanValue()
        : value.isFloat() ? value.floatValue()
        : value.isDouble() ? value.doubleValue()
        : value.isBigDecimal() ? value.decimalValue()
        : value.asText();
  }

  private void consumeDeltaJsonNodeFields(EdmEntityType edmEntityType, ObjectNode node,
      Entity entity, ExpandTreeBuilder expandBuilder) 
      throws DeserializerException {
    if (constants instanceof Constantsv01) {
      List<String> navigationPropertyNames = edmEntityType.getNavigationPropertyNames();
      for (String navigationPropertyName : navigationPropertyNames) {
        // read expanded navigation property for delta
        String delta = navigationPropertyName + Constants.AT + Constants.DELTAVALUE;
        JsonNode jsonNode = node.get(delta);
        EdmNavigationProperty edmNavigationProperty = edmEntityType.getNavigationProperty(navigationPropertyName);
        if (jsonNode != null && jsonNode.isArray() && edmNavigationProperty.isCollection()) {
          checkNotNullOrValidNull(jsonNode, edmNavigationProperty);
          Link link = new Link();
          link.setType(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE);
          link.setTitle(navigationPropertyName);
          Delta deltaValue = new Delta();
          for (JsonNode arrayElement : jsonNode) {
            String removed = Constants.AT + Constants.REMOVED;
            if (arrayElement.get(removed) != null) {
              //if @removed is present create a DeletedEntity Object
              JsonNode reasonNode = arrayElement.get(removed);
              DeletedEntity deletedEntity = new DeletedEntity();
              Reason reason = null;
              if (reasonNode.get(REASON) != null) {
                if(reasonNode.get(REASON).asText().equals(Reason.changed.name())){
                  reason = Reason.changed;
                }else if(reasonNode.get(REASON).asText().equals(Reason.deleted.name())){
                  reason = Reason.deleted;
                }
              }else{
                throw new DeserializerException("DeletedEntity reason is null.",
                    SerializerException.MessageKeys.MISSING_DELTA_PROPERTY, Constants.REASON);
              }
              deletedEntity.setReason(reason);
              try {
                deletedEntity.setId(new URI(arrayElement.get(constants.getId()).asText()));
              } catch (URISyntaxException e) {
                throw new DeserializerException("Could not set Id for deleted Entity", e,
                    DeserializerException.MessageKeys.UNKNOWN_CONTENT);
              }
              deltaValue.getDeletedEntities().add(deletedEntity);
            } else {
              //For @id and properties create normal entity
            	Entity inlineEntity = consumeEntityNode(edmNavigationProperty.getType(), 
              		  (ObjectNode) arrayElement, expandBuilder);
              deltaValue.getEntities().add(inlineEntity);
            }
          }
          link.setInlineEntitySet(deltaValue);
          entity.getNavigationLinks().add(link);
          node.remove(navigationPropertyName);
        }
      }
    }

  }

  private void consumeId(ObjectNode node, Entity entity) 
      throws DeserializerException {
    // [OData-JSON] 4.0 section 4.5: control information is ignored for requests, so an ordinary
    // 4.0 entity payload must not have its "@odata.id" promoted to the entity-id. The 4.01 format
    // does read "@id" here, as it always has. The entity-reference form of an action parameter
    // value is a different case and is handled by readEntityReference, called from createParameter.
    // The isTextual() guard matters: new URI(null) throws NullPointerException, not
    // URISyntaxException, so a non-string "@id" used to be a 500.
    if (node.get(constants.getId()) != null && constants instanceof Constantsv01
        && node.get(constants.getId()).isTextual()) {
      try {
        entity.setId(new URI(node.get(constants.getId()).textValue()));
        node.remove(constants.getId());
      } catch (URISyntaxException e) {
        throw new DeserializerException("Could not form Id", e,
            DeserializerException.MessageKeys.UNKNOWN_CONTENT);
      }
    }
  }

  /**
   * Reads the id control information off an entity-typed action parameter value, in both format
   * versions. [OData-JSON] section 18 allows such a value to be "just the entity reference", and
   * section 4.5.8 makes that reference the id ("@odata.id" in 4.0, "@id" in 4.01). Unlike an
   * ordinary entity payload, where the id is control information the service ignores
   * ([OData-JSON] 4.0 section 4.5), here the id IS the value - dropping it would leave the service
   * an empty object it cannot resolve. Returns null when the value carries no textual id.
   */
  private URI readEntityReference(final JsonNode node) throws DeserializerException {
    if (node == null || !node.isObject()) {
      return null;
    }
    final JsonNode id = node.get(constants.getId());
    if (id == null || !id.isTextual()) {
      return null;
    }
    try {
      return new URI(id.textValue());
    } catch (final URISyntaxException e) {
      throw new DeserializerException("Could not form Id", e,
          DeserializerException.MessageKeys.UNKNOWN_CONTENT);
    }
  }

  @Override
  public DeserializerResult actionParameters(final InputStream stream, final EdmAction edmAction)
      throws DeserializerException {
	  Map<String, Parameter> parameters = new HashMap<>();
	  ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	  byte[] inputContent = null;
    try {
    	stream.transferTo(byteArrayOutputStream);
    	// copy the content of input stream to reuse it
      	  inputContent = byteArrayOutputStream.toByteArray();
      	  if (inputContent.length > 0) {
      		InputStream inputStream1 = new ByteArrayInputStream(inputContent);
    	      ObjectNode tree = parseJsonTree(inputStream1);
    	      parameters = consumeParameters(edmAction, tree);
    	
    	      if (tree.isObject()) {
    	        removeAnnotations(tree);
    	      }
    	      assertJsonNodeIsEmpty(tree);
      	  }
      return DeserializerResultImpl.with().actionParameters(parameters).build();

    } catch (final IOException e) {
      throw wrapParseException(e);
    }
  }

  private ObjectNode parseJsonTree(final InputStream stream) throws IOException, DeserializerException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
    objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
    JsonParser parser = new JsonFactory(objectMapper).createParser(stream);
    final JsonNode tree = parser.getCodec().readTree(parser);
    if (tree == null || !tree.isObject()) {
      throw new DeserializerException("Invalid JSON syntax.",
          DeserializerException.MessageKeys.JSON_SYNTAX_EXCEPTION);
    }
    return (ObjectNode) tree;
  }

  private Map<String, Parameter> consumeParameters(final EdmAction edmAction, final ObjectNode node)
      throws DeserializerException {
    List<String> parameterNames = edmAction.getParameterNames();
    if (edmAction.isBound()) {
      // The binding parameter must not occur in the payload.
      parameterNames = parameterNames.subList(1, parameterNames.size());
    }
    Map<String, Parameter> parameters = new LinkedHashMap<>();
    for (final String paramName : parameterNames) {
      final EdmParameter edmParameter = edmAction.getParameter(paramName);

      switch (edmParameter.getType().getKind()) {
      case PRIMITIVE:
      case DEFINITION:
      case ENUM:
      case COMPLEX:
      case ENTITY:
        Parameter parameter = createParameter(node.get(paramName), paramName, edmParameter);
        parameters.put(paramName, parameter);
        node.remove(paramName);
        break;
      default:
        throw new DeserializerException(
            "Invalid type kind " + edmParameter.getType().getKind() + " for action parameter: " + paramName,
            DeserializerException.MessageKeys.INVALID_ACTION_PARAMETER_TYPE, paramName);
      }
    }
    return parameters;
  }

  private Parameter createParameter(final JsonNode node, final String paramName, final EdmParameter edmParameter)
      throws DeserializerException {
    if (node == null) {
      final Parameter defaultParameter = createDefaultParameter(paramName, edmParameter);
      if (defaultParameter != null) {
        return defaultParameter;
      }
    }
    Parameter parameter = new Parameter();
    parameter.setName(paramName);
    if (node == null || node.isNull()) {
      if (!edmParameter.isNullable()) {
        throw new DeserializerException("Non-nullable parameter not present or null: " + paramName,
            MessageKeys.INVALID_NULL_PARAMETER, paramName);
      }
      parameter.setValue(ValueType.PRIMITIVE, null);
    } else if (edmParameter.getType().getKind() == EdmTypeKind.ENTITY) {
      if (edmParameter.isCollection()) {
        // The references are read before consumeEntitySetArray, which strips the control
        // information off each element ([OData-JSON] section 13: every element is a representation
        // of an entity or of an entity reference).
        final List<URI> references = new ArrayList<>();
        if (node.isArray()) {
          for (final JsonNode element : node) {
            references.add(readEntityReference(element));
          }
        }
        final List<Entity> entities =
            consumeEntitySetArray((EdmEntityType) edmParameter.getType(), node, null);
        for (int i = 0; i < entities.size() && i < references.size(); i++) {
          if (entities.get(i).getId() == null) {
            entities.get(i).setId(references.get(i));
          }
        }
        parameter.setValue(ValueType.COLLECTION_ENTITY, entities);
      } else {
        final URI reference = readEntityReference(node);
        final Entity entity = consumeEntityNode((EdmEntityType) edmParameter.getType(), (ObjectNode) node, null);
        if (entity.getId() == null) {
          entity.setId(reference);
        }
        parameter.setValue(ValueType.ENTITY, entity);
      }
    } else {
      final Property property =
          consumePropertyNode(edmParameter.getName(), edmParameter.getType(), edmParameter.isCollection(),
              edmParameter.isNullable(), edmParameter.getMaxLength(),
              edmParameter.getPrecision(), edmParameter.getScale(), true, edmParameter.getMapping(), node);
      parameter.setValue(property.getValueType(), property.getValue());
      parameter.setType(property.getType());
    }
    return parameter;
  }

  /**
   * Creates a parameter from the default value of an omitted optional parameter (OData 4.01,
   * Part 1: Protocol, section 11.5.5.1). The default value of the Core.OptionalParameter annotation
   * is a URI literal (Core vocabulary: "using the same rules as the cast function in URLs"), so it
   * is read like a parameter value in a URL. The rule applies to omission only: an explicitly
   * passed null value stays null. Returns null if the parameter has no applicable default value, in
   * which case the regular handling of an omitted parameter applies.
   */
  private Parameter createDefaultParameter(final String paramName, final EdmParameter edmParameter)
      throws DeserializerException {
    final String defaultValue = OptionalParameterDefaults.defaultLiteral(edmParameter);
    final EdmType type = edmParameter.getType();
    if (defaultValue == null || edmParameter.isCollection() || !(type instanceof EdmPrimitiveType primitiveType)) {
      return null;
    }
    final Parameter parameter = new Parameter();
    parameter.setName(paramName);
    parameter.setType(type.getFullQualifiedName().getFullQualifiedNameAsString());
    try {
      final Object value = OptionalParameterDefaults.valueOfUriLiteral(edmParameter, primitiveType, defaultValue);
      parameter.setValue(type.getKind() == EdmTypeKind.ENUM ? ValueType.ENUM : ValueType.PRIMITIVE, value);
    } catch (final EdmPrimitiveTypeException e) {
      throw new DeserializerException("Invalid default value for parameter: " + paramName, e,
          MessageKeys.INVALID_VALUE_FOR_PROPERTY, paramName);
    }
    return parameter;
  }

  /** Reads a parameter value from a String. */
  public Parameter parameter(final String content, final EdmParameter parameter) throws DeserializerException {
    try {
      JsonParser parser = new JsonFactory(new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true)
          .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true))
              .createParser(content);
      JsonNode node = parser.getCodec().readTree(parser);
      if (node == null) {
        throw new DeserializerException("Invalid JSON syntax.",
            DeserializerException.MessageKeys.JSON_SYNTAX_EXCEPTION);
      }
      final Parameter result = createParameter(node, parameter.getName(), parameter);
      if (node.isObject()) {
        removeAnnotations((ObjectNode) node);
        assertJsonNodeIsEmpty(node);
      }
      return result;
    } catch (final IOException e) {
      throw wrapParseException(e);
    }
  }

  /**
   * Consumes all remaining fields of Json ObjectNode and tries to map found values
   * to according Entity fields and omits OData fields to be ignored (e.g., control information).
   *
   * @param edmEntityType edm entity type which for which the json node is consumed
   * @param node json node which is consumed
   * @param entity entity instance which is filled
   * @throws DeserializerException if an exception during consumation occurs
   */
  private void consumeRemainingJsonNodeFields(final EdmEntityType edmEntityType, final ObjectNode node,
      final Entity entity) throws DeserializerException {
    final List<String> toRemove = new ArrayList<>();
    Iterator<Entry<String, JsonNode>> fieldsIterator = node.properties().iterator();
    while (fieldsIterator.hasNext()) {
      Entry<String, JsonNode> field = fieldsIterator.next();

      if (field.getKey().endsWith(constants.getBind())) {
        Link bindingLink = consumeBindingLink(field.getKey(), field.getValue(), edmEntityType);
        entity.getNavigationBindings().add(bindingLink);
        toRemove.add(field.getKey());
      } else if (!field.getKey().contains(ODATA_CONTROL_INFORMATION_PREFIX) && 
    		  field.getKey().contains(ODATA_ANNOTATION_MARKER) &&
    		  field.getKey().substring(field.getKey().indexOf(ODATA_ANNOTATION_MARKER))
    		  .contains(".")) {
    	// Instance annotations start with @ sign followed by 
          // alias or namespace 
          // followed by a dot and then term name
    	  String[] keySplit = field.getKey().split(ODATA_ANNOTATION_MARKER);
    	  String termName = keySplit[1];
    	  Annotation annotation = instanceAnnotDeserializer.consumeInstanceAnnotation(termName, field.getValue());
    	  // If keySplit has a value at zeroth index then instance annotation is specified like 
    	  // propertyName@Term
    	  if (!keySplit[0].isEmpty()) {
    		  if (edmEntityType.getPropertyNames().contains(keySplit[0])) {
    			  entity.getProperty(keySplit[0]).getAnnotations().add(annotation);
    		  } else if (edmEntityType.getNavigationPropertyNames().contains(keySplit[0])) {
    			  Link link = entity.getNavigationLink(keySplit[0]);
    			  link.getAnnotations().add(annotation);
    		  }
    	  } else {
    		  entity.getAnnotations().add(annotation);
    	  }
    	  toRemove.add(field.getKey());
      } else if (isStreamPropertyNode(field.getKey())) {
        consumeStreamPropertyNode(entity, edmEntityType, field);
        toRemove.add(field.getKey());
      }
    }
    // remove here to avoid iterator issues.
    node.remove(toRemove);

    removeAnnotations(node);
  }

  /**
   * Process stream property instance annotation,
   * include
   * <ul>
   * <li>odata.mediaReadLink for 4.0 or mediaReadLink for 4.01</li>
   * <li>odata.mediaEditLink for 4.0 or mediaEditLink for 4.01</li>
   * <li>odata.mediaMimeType for 4.0 or mediaMimeType for 4.01</li>
   * </ul>
   *
   * @return true if jsonNodeKey present stream property annotation, false for otherwise
   */
  private boolean isStreamPropertyNode(String jsonNodeKey) {
    return jsonNodeKey.endsWith(ODATA_STREAM_PROPERTY_MEDIA_READ_LINK)
      || jsonNodeKey.endsWith(ODATA_STREAM_PROPERTY_MEDIA_EDIT_LINK)
      || jsonNodeKey.endsWith(ODATA_STREAM_PROPERTY_MEDIA_MIME_TYPE);
  }

  /**
   * Construct a empty {@code Property} and fill stream property annotation data into it
   *
   * @param entity entity instance which is filled
   * @param edmEntityType edm entity type which for which the json node is consumed
   * @param field Json field entry which current consuming
   *
   * @throws DeserializerException thrown by {@code instanceAnnotDeserializer} if consume
   * instance annotation failed
   */
  private void consumeStreamPropertyNode(final Entity entity,
                                         final EdmEntityType edmEntityType,
                                         final Entry<String, JsonNode> field) throws DeserializerException {
    String[] keySplit = field.getKey().split(ODATA_ANNOTATION_MARKER);
    String termName = keySplit[1];
    Annotation annotation = instanceAnnotDeserializer.consumeInstanceAnnotation(termName, field.getValue());
    String propertyName = keySplit[0];
    if(edmEntityType.getProperty(propertyName) == null) {
      return;
    }

    Property property = entity.getProperty(propertyName);
    if(property == null) {
      property = new Property();
      property.setName(propertyName);
      entity.addProperty(property);
    }
    property.getAnnotations().add(annotation);
  }

  private void consumeEntityProperties(final EdmEntityType edmEntityType, final ObjectNode node,
      final Entity entity) throws DeserializerException {
    List<String> propertyNames = edmEntityType.getPropertyNames();
    for (String propertyName : propertyNames) {
      JsonNode jsonNode = node.get(propertyName);
      if (jsonNode != null) {
        EdmProperty edmProperty = (EdmProperty) edmEntityType.getProperty(propertyName);
        if (jsonNode.isNull() && !edmProperty.isNullable()) {
          throw new DeserializerException("Property: " + propertyName + " must not be null.",
              DeserializerException.MessageKeys.INVALID_NULL_PROPERTY, propertyName);
        }
        Property property = consumePropertyNode(edmProperty.getName(), edmProperty.getType(),
            edmProperty.isCollection(), edmProperty.isNullable(), edmProperty.getMaxLength(),
            edmProperty.getPrecision(), edmProperty.getScale(), edmProperty.isUnicode(), edmProperty.getMapping(),
            jsonNode);
        entity.addProperty(property);
        node.remove(propertyName);
      }
    }
  }

  private void consumeExpandedNavigationProperties(final EdmEntityType edmEntityType, final ObjectNode node,
      final Entity entity, final ExpandTreeBuilder expandBuilder) throws DeserializerException {
    List<String> navigationPropertyNames = edmEntityType.getNavigationPropertyNames();
    for (String navigationPropertyName : navigationPropertyNames) {
      // read expanded navigation property
      JsonNode jsonNode = node.get(navigationPropertyName);
      if (jsonNode != null) {
        EdmNavigationProperty edmNavigationProperty = edmEntityType.getNavigationProperty(navigationPropertyName);
        checkNotNullOrValidNull(jsonNode, edmNavigationProperty);

        Link link = createLink(expandBuilder, navigationPropertyName, jsonNode, edmNavigationProperty);
        entity.getNavigationLinks().add(link);
        node.remove(navigationPropertyName);
      }
    }
  }

  /**
   * Check if jsonNode is not null or if null but nullable or collection navigationProperty
   *
   * @param jsonNode related json node
   * @param edmNavigationProperty related navigation property
   * @throws DeserializerException if jsonNode is not null or if null but nullable or collection navigationProperty
   */
  private void checkNotNullOrValidNull(final JsonNode jsonNode,
      final EdmNavigationProperty edmNavigationProperty) throws DeserializerException {
    boolean isNullable = edmNavigationProperty.isNullable();
    if ((jsonNode.isNull() && !isNullable) || (jsonNode.isNull() && edmNavigationProperty.isCollection())) {
      throw new DeserializerException("Property: " + edmNavigationProperty.getName() + " must not be null.",
          MessageKeys.INVALID_NULL_PROPERTY, edmNavigationProperty.getName());
    }
  }

  private Link createLink(final ExpandTreeBuilder expandBuilder, final String navigationPropertyName,
      final JsonNode jsonNode,
      final EdmNavigationProperty edmNavigationProperty) throws DeserializerException {
    Link link = new Link();
    link.setTitle(navigationPropertyName);
    final ExpandTreeBuilder childExpandBuilder = (expandBuilder != null) ? expandBuilder.expand(edmNavigationProperty)
        : null;
    EdmEntityType derivedEdmEntityType = (EdmEntityType) getDerivedType(
        edmNavigationProperty.getType(), jsonNode);
    if (jsonNode.isArray() && edmNavigationProperty.isCollection()) {
      link.setType(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE);
      EntityCollection inlineEntitySet = new EntityCollection();
      inlineEntitySet.getEntities().addAll(
          consumeEntitySetArray(derivedEdmEntityType, jsonNode, childExpandBuilder));
      link.setInlineEntitySet(inlineEntitySet);
    } else if (!jsonNode.isArray() && (!jsonNode.isValueNode() || jsonNode.isNull())
        && !edmNavigationProperty.isCollection()) {
      link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
      if (!jsonNode.isNull()) {
        Entity inlineEntity = consumeEntityNode(derivedEdmEntityType, (ObjectNode) jsonNode, childExpandBuilder);
        link.setInlineEntity(inlineEntity);
      }
    } else {
      throw new DeserializerException("Invalid value: " + jsonNode.getNodeType()
          + " for expanded navigation property: " + navigationPropertyName,
          MessageKeys.INVALID_VALUE_FOR_NAVIGATION_PROPERTY, navigationPropertyName);
    }
    return link;
  }
  
  private Link consumeBindingLink(final String key, final JsonNode jsonNode,
      final EdmStructuredType edmStructuredType)
      throws DeserializerException {
    String[] splitKey = key.split(ODATA_ANNOTATION_MARKER);
    String navigationPropertyName = splitKey[0];
    EdmNavigationProperty edmNavigationProperty = edmStructuredType.getNavigationProperty(navigationPropertyName);
    if (edmNavigationProperty == null) {
      throw new DeserializerException("Invalid navigationPropertyName: " + navigationPropertyName,
          DeserializerException.MessageKeys.NAVIGATION_PROPERTY_NOT_FOUND, navigationPropertyName);
    }
    Link bindingLink = new Link();
    bindingLink.setTitle(navigationPropertyName);

    if (edmNavigationProperty.isCollection()) {
      assertIsNullNode(key, jsonNode);
      if (!jsonNode.isArray()) {
        throw new DeserializerException("Binding annotation: " + key + " must be an array.",
            DeserializerException.MessageKeys.INVALID_ANNOTATION_TYPE, key);
      }
      List<String> bindingLinkStrings = new ArrayList<>();
      for (JsonNode arrayValue : jsonNode) {
        assertIsNullNode(key, arrayValue);
        if (!arrayValue.isTextual()) {
          throw new DeserializerException("Binding annotation: " + key + " must have string valued array.",
              DeserializerException.MessageKeys.INVALID_ANNOTATION_TYPE, key);
        }
        bindingLinkStrings.add(arrayValue.asText());
      }
      bindingLink.setType(Constants.ENTITY_COLLECTION_BINDING_LINK_TYPE);
      bindingLink.setBindingLinks(bindingLinkStrings);
    } else {
      if (!jsonNode.isValueNode()) {
        throw new DeserializerException("Binding annotation: " + key + " must be a string value.",
            DeserializerException.MessageKeys.INVALID_ANNOTATION_TYPE, key);
      }
      if (edmNavigationProperty.isNullable() && jsonNode.isNull()) {
        bindingLink.setBindingLink(null);
      } else {
        assertIsNullNode(key, jsonNode);
        bindingLink.setBindingLink(jsonNode.asText());        
      }
      bindingLink.setType(Constants.ENTITY_BINDING_LINK_TYPE);
    }
    return bindingLink;
  }

  private void assertIsNullNode(final String key, final JsonNode jsonNode) throws DeserializerException {
    if (jsonNode.isNull()) {
      throw new DeserializerException("Annotation: " + key + "must not have a null value.",
          DeserializerException.MessageKeys.INVALID_NULL_ANNOTATION, key);
    }
  }

  private Property consumePropertyNode(final String name, final EdmType type, final boolean isCollection,
      final boolean isNullable, final Integer maxLength, final Integer precision, final Integer scale,
      final boolean isUnicode, final EdmMapping mapping, final JsonNode jsonNode) throws DeserializerException {
    Property property = new Property();
    property.setName(name);
    property.setType(type.getFullQualifiedName().getFullQualifiedNameAsString());
    if (isCollection) {
      consumePropertyCollectionNode(name, type, isNullable, maxLength, precision, scale, isUnicode, mapping, jsonNode,
          property);
    } else {
      consumePropertySingleNode(name, type, isNullable, maxLength, precision, scale, isUnicode, mapping, jsonNode,
          property);
    }
    return property;
  }

  private void consumePropertySingleNode(final String name, final EdmType type,
      final boolean isNullable, final Integer maxLength, final Integer precision, final Integer scale,
      final boolean isUnicode, final EdmMapping mapping, final JsonNode jsonNode, final Property property)
      throws DeserializerException {
    switch (type.getKind()) {
    case PRIMITIVE:
    case DEFINITION:
    case ENUM:
      Object value = readPrimitiveValue(name, (EdmPrimitiveType) type,
          isNullable, maxLength, precision, scale, isUnicode, mapping, jsonNode);
      // A geo value's Java representation is a Geospatial object, not a literal, and the JSON writer
      // dispatches on ValueType (Property.isGeospatial()); tagging it PRIMITIVE makes the writer emit
      // the WKT literal valueToString produces instead of the GeoJSON object [OData-JSON] 7.1 requires.
      property.setValue(valueTypeFor(type, false), value);
      break;
    case COMPLEX:
      EdmType derivedType = getDerivedType((EdmComplexType) type,
          jsonNode);
      property.setType(derivedType.getFullQualifiedName()
          .getFullQualifiedNameAsString());

      value = readComplexNode(name, derivedType, isNullable, jsonNode);
      property.setValue(ValueType.COMPLEX, value);
      break;
    default:
      throw new DeserializerException("Invalid Type Kind for a property found: " + type.getKind(),
          DeserializerException.MessageKeys.INVALID_JSON_TYPE_FOR_PROPERTY, name);
    }
  }

  private Object readComplexNode(final String name, final EdmType type, final boolean isNullable,
      final JsonNode jsonNode)
      throws DeserializerException {
    // read and add all complex properties
    ComplexValue value = readComplexValue(name, type, isNullable, jsonNode);

    if (jsonNode.isObject()) {
      removeAnnotations((ObjectNode) jsonNode);
    }
    // Afterwards the node must be empty
    assertJsonNodeIsEmpty(jsonNode);

    return value;
  }

  private void consumePropertyCollectionNode(final String name, final EdmType type,
      final boolean isNullable, final Integer maxLength, final Integer precision, final Integer scale,
      final boolean isUnicode, final EdmMapping mapping, final JsonNode jsonNode, final Property property)
      throws DeserializerException {

    Iterator<JsonNode> iterator;
    List<Object> valueArray = new ArrayList<>();
    if (!jsonNode.isArray()) {
      iterator = List.of(jsonNode).iterator();
    } else {
      iterator = jsonNode.iterator();
    }
    switch (type.getKind()) {
    case PRIMITIVE:
    case DEFINITION:
    case ENUM:
      while (iterator.hasNext()) {
        JsonNode arrayElement = iterator.next();
        Object value = readPrimitiveValue(name, (EdmPrimitiveType) type,
            isNullable, maxLength, precision, scale, isUnicode, mapping, arrayElement);
        valueArray.add(value);
      }
      property.setValue(valueTypeFor(type, true), valueArray);
      break;
    case COMPLEX:
      while (iterator.hasNext()) {
        // read and add all complex properties
        Object value = readComplexNode(name, type, isNullable, iterator.next());
        valueArray.add(value);
      }
      property.setValue(ValueType.COLLECTION_COMPLEX, valueArray);
      break;
    default:
      throw new DeserializerException("Invalid Type Kind for a property found: " + type.getKind(),
          DeserializerException.MessageKeys.INVALID_JSON_TYPE_FOR_PROPERTY, name);
    }
  }

  private ComplexValue readComplexValue(final String name, final EdmType type,
      final boolean isNullable, final JsonNode jsonNode) throws DeserializerException {
    if (isValidNull(name, isNullable, jsonNode)) {
      return null;
    }
    if (jsonNode.isArray() || !jsonNode.isContainerNode()) {
      throw new DeserializerException(
          "Invalid value for property: " + name + " must not be an array or primitive value.",
          DeserializerException.MessageKeys.INVALID_JSON_TYPE_FOR_PROPERTY, name);
    }
    // Even if there are no properties defined we have to give back an empty list
    ComplexValue complexValue = new ComplexValue();
    EdmComplexType edmType = (EdmComplexType) type;
    
    //Check if the properties are from derived type
    edmType = (EdmComplexType) getDerivedType(edmType, jsonNode);
    
    // Check and consume all Properties
    for (String propertyName : edmType.getPropertyNames()) {
      JsonNode subNode = jsonNode.get(propertyName);
      if (subNode != null) {
        EdmProperty edmProperty = (EdmProperty) edmType.getProperty(propertyName);
        if (subNode.isNull() && !edmProperty.isNullable()) {
          throw new DeserializerException("Property: " + propertyName + " must not be null.",
              DeserializerException.MessageKeys.INVALID_NULL_PROPERTY, propertyName);
        }
        Property property = consumePropertyNode(edmProperty.getName(), edmProperty.getType(),
            edmProperty.isCollection(),
            edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(),
            edmProperty.isUnicode(), edmProperty.getMapping(),
            subNode);
        complexValue.getValue().add(property);
        if (jsonNode instanceof ObjectNode objNode) {
          objNode.remove(propertyName);
        }
      }
    }

    // OLINGO-1181: complex types may declare navigation properties. Consume expanded navigation
    // properties (nested entities) and navigation binding links (@odata.bind) so that deep insert
    // populates ComplexValue.getNavigationLinks() and ComplexValue.getNavigationBindings(), mirroring
    // the entity-level handling. Complex types without navigation properties make this a no-op.
    for (String navigationPropertyName : edmType.getNavigationPropertyNames()) {
      JsonNode subNode = jsonNode.get(navigationPropertyName);
      if (subNode != null) {
        EdmNavigationProperty edmNavigationProperty = edmType.getNavigationProperty(navigationPropertyName);
        checkNotNullOrValidNull(subNode, edmNavigationProperty);
        Link link = createLink(null, navigationPropertyName, subNode, edmNavigationProperty);
        complexValue.getNavigationLinks().add(link);
        if (jsonNode instanceof ObjectNode objNode) {
          objNode.remove(navigationPropertyName);
        }
      }
    }
    if (jsonNode instanceof ObjectNode objNode) {
      final List<String> toRemove = new ArrayList<>();
      Iterator<Entry<String, JsonNode>> fieldsIterator = objNode.properties().iterator();
      while (fieldsIterator.hasNext()) {
        Entry<String, JsonNode> field = fieldsIterator.next();
        if (field.getKey().endsWith(constants.getBind())) {
          Link bindingLink = consumeBindingLink(field.getKey(), field.getValue(), edmType);
          complexValue.getNavigationBindings().add(bindingLink);
          toRemove.add(field.getKey());
        }
      }
      objNode.remove(toRemove);
    }

    // consume dynamic properties for open complex types; this must run before the caller's
    // removeAnnotations/assertJsonNodeIsEmpty, which would otherwise strip name@odata.type
    // annotations dynamic properties rely on and reject the remaining fields as unknown content
    if (edmType.isOpenType() && jsonNode instanceof ObjectNode objNode) {
      consumeDynamicProperties(edmType, objNode, complexValue.getValue());
    }

    complexValue.setTypeName(edmType.getFullQualifiedName().getFullQualifiedNameAsString());
    return complexValue;
  }

  /**
   * Tells whether an EDM type is one of the sixteen geospatial primitive types. The type kind must be
   * checked as well because enumeration types whose name starts with "Geo" exist (OLINGO-1590).
   */
  private static boolean isGeoType(final EdmType type) {
    if (type.getKind() != EdmTypeKind.PRIMITIVE) {
      return false;
    }
    try {
      return EdmPrimitiveTypeKind.valueOf(type.getName()).isGeospatial();
    } catch (final IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * The {@link ValueType} that matches an EDM primitive-ish type: geospatial types are GEOSPATIAL,
   * enumeration types ENUM, everything else PRIMITIVE (and the collection counterpart of each).
   */
  private static ValueType valueTypeFor(final EdmType type, final boolean isCollection) {
    if (type.getKind() == EdmTypeKind.ENUM) {
      return isCollection ? ValueType.COLLECTION_ENUM : ValueType.ENUM;
    }
    if (isGeoType(type)) {
      return isCollection ? ValueType.COLLECTION_GEOSPATIAL : ValueType.GEOSPATIAL;
    }
    return isCollection ? ValueType.COLLECTION_PRIMITIVE : ValueType.PRIMITIVE;
  }

  private Object readPrimitiveValue(final String name, final EdmPrimitiveType type,
      final boolean isNullable, final Integer maxLength, final Integer precision, final Integer scale,
      final boolean isUnicode, final EdmMapping mapping, final JsonNode jsonNode) throws DeserializerException {
    if (isValidNull(name, isNullable, jsonNode)) {
      return null;
    }
    final boolean isGeoType = isGeoType(type);
    if (!isGeoType) {
      checkForValueNode(name, jsonNode);
    }
    checkJsonTypeBasedOnPrimitiveType(name, type, jsonNode);
    try {
      if (isGeoType) {
        return readPrimitiveGeoValue(name, type, (ObjectNode) jsonNode);
      }
      return type.valueOfString(jsonNode.asText(),
          isNullable, maxLength, precision, scale, isUnicode,
          getJavaClassForPrimitiveType(mapping, type));
    } catch (final EdmPrimitiveTypeException e) {
      throw new DeserializerException(
          "Invalid value: " + jsonNode.asText() + " for property: " + name, e,
          DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
    }
  }

  private boolean isValidNull(final String name, final boolean isNullable, final JsonNode jsonNode)
      throws DeserializerException {
    if (jsonNode.isNull()) {
      if (isNullable) {
        return true;
      } else {
        throw new DeserializerException("Property: " + name + " must not be null.",
            DeserializerException.MessageKeys.INVALID_NULL_PROPERTY, name);
      }
    }
    return false;
  }

  private Geospatial readPrimitiveGeoValue(final String name, final EdmPrimitiveType type, ObjectNode jsonNode)
      throws DeserializerException, EdmPrimitiveTypeException {
    return readPrimitiveGeoValue(name, type, null, jsonNode);
  }

  /**
   * Reads a geospatial JSON value following the GeoJSON specification defined in RFC 7946.
   * @param name property name
   * @param type EDM type of the value
   *             (<code>null</code> for recursive calls while parsing a GeometryCollection)
   * @param inheritedDimension the enclosing collection's dimension, used when <code>type</code> is
   *             <code>null</code>: [RFC7946] section 3.1.8 makes every element of a
   *             GeometryCollection a GeoJSON Geometry object of the same collection, so a member of
   *             an Edm.GeographyCollection is a geography value, not a geometry one
   */
  private Geospatial readPrimitiveGeoValue(final String name, final EdmPrimitiveType type,
      final Geospatial.Dimension inheritedDimension, ObjectNode jsonNode)
      throws DeserializerException, EdmPrimitiveTypeException {
    JsonNode typeNode = jsonNode.remove(Constants.ATTR_TYPE);
    if (typeNode != null && typeNode.isTextual()) {
      final Class<? extends Geospatial> geoDataType = jsonNameToGeoDataType.get(typeNode.asText());
      if (geoDataType != null && (type == null || geoDataType.equals(type.getDefaultType()))) {
        final JsonNode topNode = jsonNode.remove(
            geoDataType.equals(GeospatialCollection.class) ? Constants.JSON_GEOMETRIES : Constants.JSON_COORDINATES);

        final SRID srid = readCrs(name, jsonNode);

        assertJsonNodeIsEmpty(jsonNode);

        if (topNode != null && topNode.isArray()) {
          final Geospatial.Dimension dimension = type != null
              ? (type.getName().startsWith("Geometry")
                  ? Geospatial.Dimension.GEOMETRY : Geospatial.Dimension.GEOGRAPHY)
              : (inheritedDimension == null ? Geospatial.Dimension.GEOMETRY : inheritedDimension);
          if (geoDataType.equals(Point.class)) {
            return readGeoPointValue(name, dimension, topNode, srid);
          } else if (geoDataType.equals(MultiPoint.class)) {
            return new MultiPoint(dimension, srid, readGeoPointValues(name, dimension, 0, false, topNode));
          } else if (geoDataType.equals(LineString.class)) {
            // Although a line string with less than two points is not really one, the OData specification says:
            // "The coordinates member of a LineString can have zero or more positions".
            // Therefore the required minimal size of the points array currently is zero.
            return new LineString(dimension, srid, readGeoPointValues(name, dimension, 0, false, topNode));
          } else if (geoDataType.equals(MultiLineString.class)) {
            List<LineString> lines = new ArrayList<>();
            for (final JsonNode element : topNode) {
              // Line strings can be empty (see above).
              lines.add(new LineString(dimension, srid, readGeoPointValues(name, dimension, 0, false, element)));
            }
            return new MultiLineString(dimension, srid, lines);
          } else if (geoDataType.equals(Polygon.class)) {
            return readGeoPolygon(name, dimension, topNode, srid);
          } else if (geoDataType.equals(MultiPolygon.class)) {
            List<Polygon> polygons = new ArrayList<>();
            for (final JsonNode element : topNode) {
              polygons.add(readGeoPolygon(name, dimension, element, null));
            }
            return new MultiPolygon(dimension, srid, polygons);
          } else if (geoDataType.equals(GeospatialCollection.class)) {
            List<Geospatial> elements = new ArrayList<>();
            for (final JsonNode element : topNode) {
              if (element.isObject()) {
                elements.add(readPrimitiveGeoValue(name, null, dimension, (ObjectNode) element));
              } else {
                throw new DeserializerException("Invalid value '" + element + "' in property: " + name,
                    DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
              }
            }
            return new GeospatialCollection(dimension, srid, elements);
          }
        }
      }
    }
    throw new DeserializerException("Invalid value '" + jsonNode + "' for property: " + name,
        DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
  }

  /**
   * Reads the optional GeoJSON CRS object. [OData-JSON] section 7.1: "If the optional CRS object is
   * present, it MUST be of type name, where the value of the name member of the contained properties
   * object is an EPSG SRID legacy identifier, see [GeoJSON-2008]." The legacy identifier form is
   * <code>EPSG:nnnn</code>; anything else is a malformed value for the property, which is a 400
   * rather than the NullPointerException the previous unguarded reader raised.
   * @param name property name
   * @param jsonNode the geo value's JSON object, from which the CRS member is removed
   * @return the SRID, or <code>null</code> when no CRS object is present
   */
  private SRID readCrs(final String name, final ObjectNode jsonNode) throws DeserializerException {
    final JsonNode crs = jsonNode.remove(Constants.JSON_CRS);
    if (crs == null) {
      return null;
    }
    final JsonNode crsType = crs.get(Constants.ATTR_TYPE);
    final JsonNode properties = crs.get(Constants.PROPERTIES);
    final JsonNode crsName = properties == null ? null : properties.get(Constants.JSON_NAME);
    if (crsType == null || !Constants.JSON_NAME.equals(crsType.asText())
        || crsName == null || !crsName.isTextual()) {
      throw new DeserializerException("Invalid CRS '" + crs + "' in property: " + name,
          DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
    }
    final String identifier = crsName.asText();
    final int colon = identifier.lastIndexOf(':');
    if (colon < 0 || !identifier.regionMatches(true, 0, "EPSG:", 0, 5)) {
      throw new DeserializerException("Invalid CRS name '" + identifier + "' in property: " + name,
          DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
    }
    try {
      return SRID.valueOf(identifier.substring(colon + 1));
    } catch (final IllegalArgumentException e) {
      throw new DeserializerException("Invalid CRS name '" + identifier + "' in property: " + name, e,
          DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
    }
  }

  private Point readGeoPointValue(final String name, final Geospatial.Dimension dimension, JsonNode node, SRID srid)
      throws DeserializerException, EdmPrimitiveTypeException {
    if (node.isArray() && (node.size() ==2 || node.size() == 3)
        && node.get(0).isNumber() && node.get(1).isNumber() && (node.get(2) == null || node.get(2).isNumber())) {
      Point point = new Point(dimension, srid);
      point.setX(getDoubleValue(node.get(0).asText()));
      point.setY(getDoubleValue(node.get(1).asText()));
      if (node.get(2) != null) {
        point.setZ(getDoubleValue(node.get(2).asText()));
      }
      return point;
    }
    throw new DeserializerException("Invalid point value '" + node + "' in property: " + name,
        DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
  }

  private double getDoubleValue(final String value) throws EdmPrimitiveTypeException {
    final BigDecimal bigDecimalValue = new BigDecimal(value);
    final Double result = bigDecimalValue.doubleValue();
    // "Real" infinite values cannot occur, so we can throw an exception
    // if the conversion to a double results in an infinite value.
    // An exception is also thrown if the number cannot be stored in a double without loss.
    if (result.isInfinite() || BigDecimal.valueOf(result).compareTo(bigDecimalValue) != 0) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
    }
    return result;
  }

  private List<Point> readGeoPointValues(final String name, final Geospatial.Dimension dimension,
      final int minimalSize, final boolean closed, JsonNode node)
      throws DeserializerException, EdmPrimitiveTypeException {
    if (node.isArray()) {
      List<Point> points = new ArrayList<>();
      for (final JsonNode element : node) {
        points.add(readGeoPointValue(name, dimension, element, null));
      }
      if (points.size() >= minimalSize
          && (!closed || points.get(points.size() - 1).equals(points.get(0)))) {
          return points;
      }
    }
    throw new DeserializerException("Invalid point values '" + node + "' in property: " + name,
        DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
  }

  private Polygon readGeoPolygon(final String name, final Geospatial.Dimension dimension, JsonNode node, SRID srid)
      throws DeserializerException, EdmPrimitiveTypeException {
    // There could be a more strict verification that the lines describe boundaries and have the correct winding order.
    if (node.isArray() && (node.size() >= 1)) {
      List<LineString> interiors = new ArrayList<>();
      for (int i = 1; i < node.size(); i++) {
        interiors.add(new LineString(dimension, srid, readGeoPointValues(name, dimension, 4, true, node.get(i))));
      }
      return new Polygon(dimension, srid, interiors,
          new LineString(dimension, srid, readGeoPointValues(name, dimension, 4, true, node.get(0))));
    }
    throw new DeserializerException("Invalid polygon values '" + node + "' in property: " + name,
        DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
  }

  /**
   * Returns the primitive type's default class or the manually mapped class if present.
   * @param mapping
   * @param type
   * @return the java class to be used during deserialization
   */
  private Class<?> getJavaClassForPrimitiveType(final EdmMapping mapping, final EdmPrimitiveType type) {
    final EdmPrimitiveType edmPrimitiveType =
        type instanceof EdmEnumType enumType ? enumType.getUnderlyingType()
            : type instanceof EdmTypeDefinition typeDef ? typeDef.getUnderlyingType() : type;
    return mapping == null || mapping.getMappedJavaClass() == null ? edmPrimitiveType.getDefaultType() : mapping
        .getMappedJavaClass();
  }

  /**
   * Check if JsonNode is a value node (<code>jsonNode.isValueNode()</code>) and if not throw
   * an DeserializerException.
   * @param name name of property which is checked
   * @param jsonNode node which is checked
   * @throws DeserializerException is thrown if json node is not a value node
   */
  private void checkForValueNode(final String name, final JsonNode jsonNode) throws DeserializerException {
    if (!jsonNode.isValueNode()) {
      throw new DeserializerException("Invalid value for property: " + name + " must not be an object or array.",
          DeserializerException.MessageKeys.INVALID_JSON_TYPE_FOR_PROPERTY, name);
    }
  }

  private void removeAnnotations(final ObjectNode tree) throws DeserializerException {
    List<String> toRemove = new ArrayList<>();
    Iterator<Entry<String, JsonNode>> fieldsIterator = tree.properties().iterator();
    while (fieldsIterator.hasNext()) {
      Map.Entry<String, JsonNode> field = fieldsIterator.next();

      if (field.getKey().contains(ODATA_CONTROL_INFORMATION_PREFIX)) {
        // Control Information is ignored for requests as per specification chapter "4.5 Control Information"
        toRemove.add(field.getKey());
      } else if (field.getKey().contains(ODATA_ANNOTATION_MARKER)) {
        if(constants instanceof Constantsv01){
          toRemove.add(field.getKey());
        }else{
          throw new DeserializerException("Custom annotation with field name: " + field.getKey() + " not supported",
            DeserializerException.MessageKeys.NOT_IMPLEMENTED);
        }
      }
    }
    // remove here to avoid iterator issues.
    tree.remove(toRemove);
  }

  /**
   * Validates that node is empty (<code>node.size() == 0</code>).
   * @param node node to be checked
   * @throws DeserializerException if node is not empty
   */
  private void assertJsonNodeIsEmpty(final JsonNode node) throws DeserializerException {
    if (node.size() != 0) {
      final String unknownField = node.fieldNames().next();
      throw new DeserializerException("Tree should be empty but still has content left: " + unknownField,
          DeserializerException.MessageKeys.UNKNOWN_CONTENT, unknownField);
    }
  }

  private void checkJsonTypeBasedOnPrimitiveType(final String propertyName, final EdmPrimitiveType edmPrimitiveType,
      final JsonNode jsonNode) throws DeserializerException {
    boolean valid = true;
    if (edmPrimitiveType instanceof EdmTypeDefinition typeDef) {
      checkJsonTypeBasedOnPrimitiveType(propertyName,
          typeDef.getUnderlyingType(), jsonNode);
    } else if (edmPrimitiveType.getKind() == EdmTypeKind.ENUM) {
      // Enum values must be strings.
      valid = jsonNode.isTextual();
    } else {
      final String name = edmPrimitiveType.getName();
      EdmPrimitiveTypeKind primKind;
      try {
        primKind = EdmPrimitiveTypeKind.valueOf(name);
      } catch (final IllegalArgumentException e) {
        throw new DeserializerException("Unknown Primitive Type: " + name, e,
            DeserializerException.MessageKeys.UNKNOWN_PRIMITIVE_TYPE, name, propertyName);
      }
      valid = matchTextualCase(jsonNode, primKind)
          || matchNumberCase(jsonNode, primKind)
          || matchBooleanCase(jsonNode, primKind)
          || matchIEEENumberCase(jsonNode, primKind)
          || matchIEEESpecialValueCase(jsonNode, primKind)
          || jsonNode.isObject() && name.startsWith("Geo");
    }
    if (!valid) {
      throw new DeserializerException(
          "Invalid json type: " + jsonNode.getNodeType() + " for " + edmPrimitiveType + " property: " + propertyName,
          DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, propertyName);
    }
  }

  private boolean matchIEEENumberCase(final JsonNode node, final EdmPrimitiveTypeKind primKind) {
    return (isIEEE754Compatible ? node.isTextual() : node.isNumber())
        && (primKind == EdmPrimitiveTypeKind.Int64 || primKind == EdmPrimitiveTypeKind.Decimal);
  }

  private boolean matchIEEESpecialValueCase(final JsonNode node, final EdmPrimitiveTypeKind primKind) {
    // OData represents the IEEE 754 special values as the JSON strings "NaN", "INF" and "-INF"
    // (a bare numeric token is not valid JSON). EdmSingle/EdmDouble already parse these strings.
    return node.isTextual()
        && (primKind == EdmPrimitiveTypeKind.Single || primKind == EdmPrimitiveTypeKind.Double)
        && ("NaN".equals(node.textValue())
            || "INF".equals(node.textValue())
            || "-INF".equals(node.textValue()));
  }

  private boolean matchBooleanCase(final JsonNode node, final EdmPrimitiveTypeKind primKind) {
    return node.isBoolean() && primKind == EdmPrimitiveTypeKind.Boolean;
  }

  private boolean matchNumberCase(final JsonNode node, final EdmPrimitiveTypeKind primKind) {
    return node.isNumber() &&
        (primKind == EdmPrimitiveTypeKind.Int16
            || primKind == EdmPrimitiveTypeKind.Int32
            || primKind == EdmPrimitiveTypeKind.Byte
            || primKind == EdmPrimitiveTypeKind.SByte
            || primKind == EdmPrimitiveTypeKind.Single
            || primKind == EdmPrimitiveTypeKind.Double);
  }

  private boolean matchTextualCase(final JsonNode node, final EdmPrimitiveTypeKind primKind) {
    return node.isTextual() &&
        (primKind == EdmPrimitiveTypeKind.String
            || primKind == EdmPrimitiveTypeKind.Binary
            || primKind == EdmPrimitiveTypeKind.Date
            || primKind == EdmPrimitiveTypeKind.DateTimeOffset
            || primKind == EdmPrimitiveTypeKind.Duration
            || primKind == EdmPrimitiveTypeKind.Guid
            || primKind == EdmPrimitiveTypeKind.TimeOfDay);
  }

  @Override
  public DeserializerResult property(final InputStream stream, final EdmProperty edmProperty)
      throws DeserializerException {
    try {
      final ObjectNode tree = parseJsonTree(stream);

      final Property property;
      JsonNode jsonNode = tree.get(Constants.VALUE);
      if (jsonNode != null) {
        property = consumePropertyNode(edmProperty.getName(), edmProperty.getType(),
            edmProperty.isCollection(),
            edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(),
            edmProperty.isUnicode(), edmProperty.getMapping(),
            jsonNode);
        tree.remove(Constants.VALUE);
      } else {
        property = consumePropertyNode(edmProperty.getName(), edmProperty.getType(),
            edmProperty.isCollection(),
            edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(),
            edmProperty.isUnicode(), edmProperty.getMapping(),
            tree);
      }
      return DeserializerResultImpl.with().property(property).build();
    } catch (final IOException e) {
      throw wrapParseException(e);
    }
  }

  /**
   * Deserializes a single, schema-less dynamic (OpenType) property. Accepts the same
   * <code>{"value": ...}</code> payload shape as {@link #property(InputStream, EdmProperty)},
   * plus an optional <code>value@odata.type</code> sibling annotation (mirroring the
   * <code>name@odata.type</code> annotation honored for dynamic properties inside an open
   * entity/complex value by {@link #consumeDynamicProperties}) used to resolve the property's
   * type since no {@link EdmProperty} is available here to supply it. Delegates the actual
   * value inference/annotation handling to {@link #createDynamicProperty} and
   * {@link #createDynamicCollectionProperty}, naming the resulting {@link Property}
   * {@code propertyName}.
   */
  @Override
  public DeserializerResult dynamicProperty(final InputStream stream, final String propertyName)
      throws DeserializerException {
    try {
      final ObjectNode tree = parseJsonTree(stream);
      final JsonNode valueNode = tree.get(Constants.VALUE);
      if (valueNode == null) {
        throw new DeserializerException(
            "Could not find a 'value' member for dynamic property: " + propertyName,
            DeserializerException.MessageKeys.UNKNOWN_CONTENT, propertyName);
      }

      String annotatedType = null;
      final JsonNode annotatedTypeNode = tree.get(Constants.VALUE + constants.getType());
      if (annotatedTypeNode != null) {
        annotatedType = annotatedTypeNode.asText();
        if (annotatedType.startsWith(Constants.HASH)) {
          annotatedType = annotatedType.substring(1);
        }
      }

      final Property property;
      if (valueNode.isObject()) {
        throw new DeserializerException(
            "Invalid value for dynamic property: " + propertyName + " must not be an object.",
            DeserializerException.MessageKeys.UNKNOWN_CONTENT, propertyName);
      } else if (valueNode.isArray()) {
        property = createDynamicCollectionProperty(propertyName, (ArrayNode) valueNode, annotatedType);
        if (property == null) {
          throw new DeserializerException(
              "Invalid value for dynamic property: " + propertyName + " must not contain object elements.",
              DeserializerException.MessageKeys.UNKNOWN_CONTENT, propertyName);
        }
      } else {
        property = createDynamicProperty(propertyName, valueNode, annotatedType);
      }
      return DeserializerResultImpl.with().property(property).build();
    } catch (final IOException e) {
      throw wrapParseException(e);
    }
  }

  @Override
  public DeserializerResult entityReferences(final InputStream stream) throws DeserializerException {
    try {
      List<URI> parsedValues = new ArrayList<>();
      final ObjectNode tree = parseJsonTree(stream);
      final String key = constants.getId();
      JsonNode jsonNode = tree.get(Constants.VALUE);
      if (jsonNode != null) {
        if (jsonNode.isArray()) {
          ArrayNode arrayNode = (ArrayNode) jsonNode;
          Iterator<JsonNode> it = arrayNode.iterator();
          while (it.hasNext()) {
            final JsonNode next = it.next();
            if (next.has(key)) {
              parsedValues.add(new URI(next.get(key).asText()));
            }
          }
        } else {
          throw new DeserializerException("Value must be an array", DeserializerException.MessageKeys.UNKNOWN_CONTENT);
        }
        tree.remove(Constants.VALUE);
        return DeserializerResultImpl.with().entityReferences(parsedValues).build();
      }
      if (tree.get(key) != null) {
        parsedValues.add(new URI(tree.get(key).asText()));
      } else {
        throw new DeserializerException("Missing entity reference", DeserializerException.MessageKeys.UNKNOWN_CONTENT);
      }
      return DeserializerResultImpl.with().entityReferences(parsedValues).build();
    } catch (final IOException e) {
      throw wrapParseException(e);
    } catch (final URISyntaxException e) {
      throw new DeserializerException("failed to read @odata.id", e,
          DeserializerException.MessageKeys.UNKNOWN_CONTENT);
    }
  }

  private DeserializerException wrapParseException(final IOException e) {
    if (e instanceof JsonParseException) {
      return new DeserializerException("A JsonParseException occurred.", e,
          DeserializerException.MessageKeys.JSON_SYNTAX_EXCEPTION);
    } else if (e instanceof JsonMappingException) {
      return new DeserializerException("Duplicate json property detected.", e,
          DeserializerException.MessageKeys.DUPLICATE_PROPERTY);
    } else {
      return new DeserializerException("An IOException occurred.", e,
          DeserializerException.MessageKeys.IO_EXCEPTION);
    }
  }

  private EdmType getDerivedType(final EdmStructuredType edmType, final JsonNode jsonNode)
      throws DeserializerException {
    JsonNode odataTypeNode = jsonNode.get(constants.getType());
    if (odataTypeNode != null) {
      String odataType = odataTypeNode.asText();
      if (!odataType.isEmpty()) {
        odataType = odataType.substring(1);

        if (odataType.equalsIgnoreCase(edmType.getFullQualifiedName().getFullQualifiedNameAsString())) {
          return edmType;
        } else if (this.serviceMetadata == null) {
          throw new DeserializerException(
              "Failed to resolve Odata type " + odataType + " due to metadata is not available",
              DeserializerException.MessageKeys.UNKNOWN_CONTENT);
        }

        final EdmStructuredType currentEdmType = edmType.getKind() == EdmTypeKind.ENTITY ?
            serviceMetadata.getEdm().getEntityType(new FullQualifiedName(odataType)) :
            serviceMetadata.getEdm().getComplexType(new FullQualifiedName(odataType));
        if (!isAssignable(edmType, currentEdmType)) {
          throw new DeserializerException("Odata type " + odataType + " not allowed here",
              DeserializerException.MessageKeys.UNKNOWN_CONTENT);
        }

        return currentEdmType;
      }
    }
    return edmType;
  }

  private boolean isAssignable(final EdmStructuredType edmStructuredType,
      final EdmStructuredType edmStructuredTypeToAssign) {
    return edmStructuredTypeToAssign != null
        && (edmStructuredType.getFullQualifiedName().equals(edmStructuredTypeToAssign.getFullQualifiedName())
            || isAssignable(edmStructuredType, edmStructuredTypeToAssign.getBaseType()));
  }
}
