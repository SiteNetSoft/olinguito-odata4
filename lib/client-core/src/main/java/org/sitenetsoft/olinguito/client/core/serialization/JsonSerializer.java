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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - OLINGO-1626/1627: Infer primitive type
 * instead of defaulting to String
 * Copyright 2026 SiteNetSoft - Always emit name@odata.type for non-JSON-native primitive
 * values, even under minimal metadata, so a schema-agnostic reader (e.g. the server's open-type
 * dynamic-property inference) can recover the correct EDM type instead of defaulting to String
 */
package org.sitenetsoft.olinguito.client.core.serialization;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.serialization.ODataSerializer;
import org.sitenetsoft.olinguito.client.api.serialization.ODataSerializerException;
import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.data.Annotation;
import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Link;
import org.sitenetsoft.olinguito.commons.api.data.Linked;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.Valuable;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.core.edm.EdmTypeInfo;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class JsonSerializer implements ODataSerializer {

  private static final List<EdmPrimitiveTypeKind> NUMBER_TYPES = List.of(
    EdmPrimitiveTypeKind.Byte, EdmPrimitiveTypeKind.SByte,
    EdmPrimitiveTypeKind.Single, EdmPrimitiveTypeKind.Double,
    EdmPrimitiveTypeKind.Int16, EdmPrimitiveTypeKind.Int32, EdmPrimitiveTypeKind.Int64,
    EdmPrimitiveTypeKind.Decimal);

  /**
   * Primitive kinds whose EDM type is unambiguously recoverable from their bare JSON
   * representation alone (a JSON string, boolean, or number) - mirrors the server's
   * {@code ODataJsonSerializer.JSON_NATIVE_DYNAMIC_KINDS} allow-list for dynamic (open-type)
   * properties, plus {@code Byte}/{@code SByte} (also plain JSON numbers). Anything outside this
   * set needs a {@code name@odata.type} annotation to round-trip correctly, since the client has
   * no EDM to fall back on and cannot tell a declared property from a dynamic one.
   */
  private static final Set<EdmPrimitiveTypeKind> JSON_NATIVE_KINDS = Set.of(
    EdmPrimitiveTypeKind.String, EdmPrimitiveTypeKind.Boolean,
    EdmPrimitiveTypeKind.Int16, EdmPrimitiveTypeKind.Int32, EdmPrimitiveTypeKind.Int64,
    EdmPrimitiveTypeKind.Double, EdmPrimitiveTypeKind.Decimal, EdmPrimitiveTypeKind.Single,
    EdmPrimitiveTypeKind.Byte, EdmPrimitiveTypeKind.SByte);

  private final JsonGeoValueSerializer geoSerializer = new JsonGeoValueSerializer();

  protected boolean serverMode;
  protected ContentType contentType;
  protected final boolean isIEEE754Compatible;
  protected final boolean isODataMetadataNone;
  protected final boolean isODataMetadataFull;

  public JsonSerializer(final boolean serverMode, final ContentType contentType) {
    this.serverMode = serverMode;
    this.contentType = contentType;
    this.isIEEE754Compatible = isIEEE754Compatible();
    this.isODataMetadataNone = isODataMetadataNone();
    this.isODataMetadataFull = isODataMetadataFull();
  }

  @Override
  public <T> void write(final Writer writer, final T obj) throws ODataSerializerException {
    try (final JsonGenerator json = new JsonFactory().createGenerator(writer)) {
      if (obj instanceof EntityCollection entityCollection) {
        new JsonEntitySetSerializer(serverMode, contentType).doSerialize(entityCollection, json);
      } else if (obj instanceof Entity entity) {
        new JsonEntitySerializer(serverMode, contentType).doSerialize(entity, json);
      } else if (obj instanceof Property property) {
        new JsonPropertySerializer(serverMode, contentType).doSerialize(property, json);
      } else if (obj instanceof Link link) {
        link(link, json);
      }
      json.flush();
    } catch (final IOException | EdmPrimitiveTypeException e) {
      throw new ODataSerializerException(e);
    }
  }

  private void reference(final ResWrap<URI> container, final JsonGenerator json) throws IOException {
    json.writeStartObject();

    if (!isODataMetadataNone) {
      json.writeStringField(Constants.JSON_CONTEXT, container.getContextURL().toASCIIString());
    }
    json.writeStringField(Constants.JSON_ID, container.getPayload().toASCIIString());

    json.writeEndObject();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> void write(final Writer writer, final ResWrap<T> container) throws ODataSerializerException {
    final T obj = container == null ? null : container.getPayload();
    try (final JsonGenerator json = new JsonFactory().createGenerator(writer)) {
      if (obj instanceof EntityCollection) {
        new JsonEntitySetSerializer(serverMode, contentType).doContainerSerialize(
            (ResWrap<EntityCollection>) container, json);
      } else if (obj instanceof Entity) {
        new JsonEntitySerializer(serverMode, contentType).doContainerSerialize((ResWrap<Entity>) container, json);
      } else if (obj instanceof Property) {
        new JsonPropertySerializer(serverMode, contentType).doContainerSerialize((ResWrap<Property>) container, json);
      } else if (obj instanceof Link link) {
        link(link, json);
      } else if (obj instanceof URI) {
        reference((ResWrap<URI>) container, json);
      }
      json.flush();
    } catch (final IOException | EdmPrimitiveTypeException e) {
      throw new ODataSerializerException(e);
    }
  }

  protected void link(final Link link, final JsonGenerator jgen) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField(Constants.JSON_URL, link.getHref());
    jgen.writeEndObject();
  }

  protected void links(final Linked linked, final JsonGenerator jgen)
      throws IOException, EdmPrimitiveTypeException {

    if (serverMode) {
      serverLinks(linked, jgen);
    } else {
      clientLinks(linked, jgen);
    }
  }

  protected void clientLinks(final Linked linked, final JsonGenerator jgen)
      throws IOException, EdmPrimitiveTypeException {

    final Map<String, List<String>> entitySetLinks = new HashMap<>();
    for (Link link : linked.getNavigationLinks()) {
      for (Annotation annotation : link.getAnnotations()) {
        valuable(jgen, annotation, link.getTitle() + "@" + annotation.getTerm());
      }

      if (isEntitySetNavigation(link)) {
        final List<String> uris;
        if (entitySetLinks.containsKey(link.getTitle())) {
          uris = entitySetLinks.get(link.getTitle());
        } else {
          uris = new ArrayList<>();
          entitySetLinks.put(link.getTitle(), uris);
        }
        if (link.getHref() != null && !link.getHref().isEmpty()) {
          uris.add(link.getHref());
        }
      } else {
        if (link.getHref() != null && !link.getHref().isEmpty()) {
          jgen.writeStringField(link.getTitle() + Constants.JSON_BIND_LINK_SUFFIX, link.getHref());
        }
      }

      if (link.getInlineEntity() != null) {
        jgen.writeFieldName(link.getTitle());
        new JsonEntitySerializer(serverMode, contentType).doSerialize(link.getInlineEntity(), jgen);
      } else if (link.getInlineEntitySet() != null) {
        jgen.writeArrayFieldStart(link.getTitle());
        final JsonEntitySerializer entitySerializer = new JsonEntitySerializer(serverMode, contentType);
        for (Entity subEntry : link.getInlineEntitySet().getEntities()) {
          entitySerializer.doSerialize(subEntry, jgen);
        }
        jgen.writeEndArray();
      }
    }
    for (Map.Entry<String, List<String>> entitySetLink : entitySetLinks.entrySet()) {
      if (!entitySetLink.getValue().isEmpty()) {
        jgen.writeArrayFieldStart(entitySetLink.getKey() + Constants.JSON_BIND_LINK_SUFFIX);
        for (String uri : entitySetLink.getValue()) {
          jgen.writeString(uri);
        }
        jgen.writeEndArray();
      }
    }
  }

  private boolean isEntitySetNavigation(final Link link) {
    return Constants.ENTITY_SET_NAVIGATION_LINK_TYPE.equals(link.getType());
  }

  protected void serverLinks(final Linked linked, final JsonGenerator jgen)
      throws IOException, EdmPrimitiveTypeException {
    if (linked instanceof Entity entity && isODataMetadataFull) {
      for (Link link : entity.getMediaEditLinks()) {
        if (link.getHref() != null && !link.getHref().isEmpty()) {
          jgen.writeStringField(link.getTitle() + Constants.JSON_MEDIA_EDIT_LINK, link.getHref());
        }
      }
    }

    if (isODataMetadataFull) {
      for (Link link : linked.getAssociationLinks()) {
        if (link.getHref() != null && !link.getHref().isEmpty()) {
          jgen.writeStringField(link.getTitle() + Constants.JSON_ASSOCIATION_LINK, link.getHref());
        }
      }
    }

    for (Link link : linked.getNavigationLinks()) {
      for (Annotation annotation : link.getAnnotations()) {
        valuable(jgen, annotation, link.getTitle() + "@" + annotation.getTerm());
      }

      if (link.getHref() != null && !link.getHref().isEmpty() && isODataMetadataFull) {
        jgen.writeStringField(link.getTitle() + Constants.JSON_NAVIGATION_LINK, link.getHref());
      }

      if (link.getInlineEntity() != null) {
        jgen.writeFieldName(link.getTitle());
        new JsonEntitySerializer(serverMode, contentType).doSerialize(link.getInlineEntity(), jgen);
      } else if (link.getInlineEntitySet() != null) {
        jgen.writeArrayFieldStart(link.getTitle());
        JsonEntitySerializer entitySerializer = new JsonEntitySerializer(serverMode, contentType);
        for (Entity subEntry : link.getInlineEntitySet().getEntities()) {
          entitySerializer.doSerialize(subEntry, jgen);
        }
        jgen.writeEndArray();
      }
    }
  }

  private void collection(final JsonGenerator jgen, final EdmTypeInfo typeInfo,
      final ValueType valueType, final List<?> value)
          throws IOException, EdmPrimitiveTypeException {

    EdmTypeInfo itemTypeInfo = typeInfo == null ?
        null :
        new EdmTypeInfo.Builder().setTypeExpression(typeInfo.getFullQualifiedName().toString()).build();

    jgen.writeStartArray();

    for (Object item : value) {
      switch (valueType) {
      case COLLECTION_PRIMITIVE:
        primitiveValue(jgen, itemTypeInfo, item);
        break;

      case COLLECTION_GEOSPATIAL:
        jgen.writeStartObject();
        geoSerializer.serialize(jgen, (Geospatial) item);
        jgen.writeEndObject();
        break;

      case COLLECTION_ENUM:
        jgen.writeString(item.toString());
        break;

      case COLLECTION_COMPLEX:
        final ComplexValue complexItem2 = (ComplexValue) item;
        itemTypeInfo = complexItem2.getTypeName() == null ? 
            itemTypeInfo : new EdmTypeInfo.Builder().setTypeExpression(complexItem2.getTypeName()).build();
        complexValue(jgen, itemTypeInfo, complexItem2.getValue(), complexItem2);
        break;

      default:
      }
    }

    jgen.writeEndArray();
  }

  protected void primitiveValue(final JsonGenerator jgen, final EdmTypeInfo typeInfo, final Object value)
      throws IOException, EdmPrimitiveTypeException {

    final EdmPrimitiveTypeKind kind = typeInfo == null ?
        EdmTypeInfo.determineTypeKind(value) :
        typeInfo.getPrimitiveTypeKind();

    if (value == null) {
      jgen.writeNull();
    } else if (kind == EdmPrimitiveTypeKind.Boolean) {
      jgen.writeBoolean((Boolean) value);
    } else if (kind == null) {
      if (serverMode) {
        throw new EdmPrimitiveTypeException("The primitive type could not be determined.");
      } else {
        jgen.writeString(value.toString()); // This might not be valid OData.
      }
    } else {
      Integer precision = Constants.DEFAULT_PRECISION;
      Integer scale = Constants.DEFAULT_SCALE;
      if(kind == EdmPrimitiveTypeKind.Decimal && value instanceof BigDecimal bigDecimal){
          precision = bigDecimal.precision();
          scale = bigDecimal.scale();
          if (precision == 0) {
            precision = null;
          } else if (scale > precision) {
            precision = scale;
          }
      }
      final String serialized = EdmPrimitiveTypeFactory.getInstance(kind)
          .valueToString(value, null, null, precision, scale, null);

      if (isIEEE754Compatible && (kind == EdmPrimitiveTypeKind.Int64 || kind == EdmPrimitiveTypeKind.Decimal)
          || !NUMBER_TYPES.contains(kind)) {
        jgen.writeString(serialized);
      } else {
        jgen.writeNumber(serialized);
      }
    }
  }

  private void complexValue(final JsonGenerator jgen, final EdmTypeInfo typeInfo,
      final List<Property> value, final Linked linked)
      throws IOException, EdmPrimitiveTypeException {
    jgen.writeStartObject();

    if (typeInfo != null && isODataMetadataFull) {
      jgen.writeStringField(Constants.JSON_TYPE, typeInfo.external());
    }

    for (Property property : value) {
      valuable(jgen, property, property.getName());
    }
    if (linked != null) {
      links(linked, jgen);
    }

    jgen.writeEndObject();
  }

  private void value(final JsonGenerator jgen, final String type, final Valuable value)
      throws IOException, EdmPrimitiveTypeException {
    final EdmTypeInfo typeInfo = type == null ? null : new EdmTypeInfo.Builder().setTypeExpression(type).build();

    if (value.isNull()) {
      jgen.writeNull();
    } else if (value.isCollection()) {
      collection(jgen, typeInfo, value.getValueType(), value.asCollection());
    } else if (value.isPrimitive()) {
      primitiveValue(jgen, typeInfo, value.asPrimitive());
    } else if (value.isEnum()) {
      jgen.writeString(value.asEnum().toString());
    } else if (value.isGeospatial()) {
      jgen.writeStartObject();
      geoSerializer.serialize(jgen, value.asGeospatial());
      jgen.writeEndObject();
    } else if (value.isComplex()) {
      complexValue(jgen, typeInfo, value.asComplex().getValue(), value.asComplex());
    }
  }

  protected void valuable(final JsonGenerator jgen, final Valuable valuable, final String name)
      throws IOException, EdmPrimitiveTypeException {

    if (!Constants.VALUE.equals(name) && !(valuable instanceof Annotation)
        && !(valuable.isComplex() && !valuable.isCollection())) {

      String type = valuable.getType();
      if (!valuable.isCollection() && (type == null || type.isEmpty()) && valuable.isPrimitive()) {
        final EdmPrimitiveTypeKind kind = EdmTypeInfo.determineTypeKind(valuable.getValue());
        type = (kind != null ? kind : EdmPrimitiveTypeKind.String).getFullQualifiedName().toString();
      }
      if (type != null && !type.isEmpty() && !isODataMetadataNone) {
        if (isODataMetadataFull) {
          // Pre-existing, unchanged behavior: every property gets its type annotated.
          jgen.writeStringField(
              name + Constants.JSON_TYPE,
              new EdmTypeInfo.Builder().setTypeExpression(type).build().external());
        } else {
          // New: under minimal (and unspecified) metadata, still annotate a scalar primitive
          // whose kind can't be recovered from its bare JSON form alone - the client has no EDM,
          // so it can't tell a declared property from a dynamic (open-type) one, and the OData
          // JSON spec requires this control information for the latter. Uses the same bare,
          // "#"-prefixed short-name convention as the server's dynamic-property writer
          // (ODataJsonSerializer#writeDynamicProperty) rather than the unprefixed form used
          // above for full metadata, so a round-tripping server or client recognizes it.
          final EdmPrimitiveTypeKind scalarKind = scalarPrimitiveKind(valuable, type);
          if (scalarKind != null && !JSON_NATIVE_KINDS.contains(scalarKind)) {
            jgen.writeStringField(name + Constants.JSON_TYPE, "#" + scalarKind.name());
          }
        }
      }
    }

    for (Annotation annotation : valuable.getAnnotations()) {
      valuable(jgen, annotation, name + "@" + annotation.getTerm());
    }

    jgen.writeFieldName(name);
    value(jgen, valuable.getType(), valuable);
  }

  /**
   * The {@link EdmPrimitiveTypeKind} of a scalar (non-collection) primitive value, or
   * {@code null} for anything else (collections, complex, enum, geospatial) - used to decide
   * whether a {@code name@odata.type} annotation is required under minimal metadata.
   */
  private EdmPrimitiveTypeKind scalarPrimitiveKind(final Valuable valuable, final String type) {
    if (valuable.isCollection() || !valuable.isPrimitive()) {
      return null;
    }
    return new EdmTypeInfo.Builder().setTypeExpression(type).build().getPrimitiveTypeKind();
  }

  private boolean isIEEE754Compatible() {
    final String parameter = contentType.getParameters().get(ContentType.PARAMETER_IEEE754_COMPATIBLE);
    return "true".equalsIgnoreCase(parameter);
  }

  private boolean isODataMetadataNone() {
    return contentType.isCompatible(ContentType.APPLICATION_JSON)
        && ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(
            contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA));
  }
  
  private boolean isODataMetadataFull() {
    return contentType.isCompatible(ContentType.APPLICATION_JSON)
        && ContentType.VALUE_ODATA_METADATA_FULL.equalsIgnoreCase(
            contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA));
  }
}
