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
 * Copyright 2026 SiteNetSoft - Add OpenType support (serialize dynamic properties in JSON)
 * Copyright 2026 SiteNetSoft - Add $select-of-dynamic-property serializer tests
 * Copyright 2026 SiteNetSoft - Pin $select of a nested dynamic property under a declared complex
 * Copyright 2026 SiteNetSoft - Server must not emit @odata.type under odata.metadata=none
 * Copyright 2026 SiteNetSoft - omit-values: dynamic properties are never omitted (OData 4.01,
 * Protocol Section 8.2.8.6)
 */
package org.sitenetsoft.olinguito.server.core.serializer.json;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.EntitySerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.core.uri.parser.Parser;
import org.sitenetsoft.olinguito.server.tecsvc.MetadataETagSupport;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.SchemaProvider;
import org.junit.jupiter.api.Test;

/**
 * Tests JSON serialization of dynamic (undeclared) properties of open types: both directly on an open
 * entity/complex type, and nested inside an open complex value embedded in an entity.
 */
class ODataJsonSerializerOpenTypeTest {

  private static final OData odata = OData.newInstance();
  private static final ServiceMetadata metadata = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList(), new MetadataETagSupport("W/\"metadataETag\""));

  @Test
  void dynamicPropertiesSerializedMinimalMetadata() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"))
        .addProperty(new Property("Edm.String", "Custom", ValueType.PRIMITIVE, "hello"))
        .addProperty(new Property("Edm.Int64", "CustomInt", ValueType.PRIMITIVE, 42L));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
    assertTrue(json.contains("\"Custom\":\"hello\""));
    assertTrue(json.contains("\"CustomInt\":42"));
    assertFalse(json.contains("Custom@odata.type"));
  }

  @Test
  void nonNativeDynamicValueAnnotatedEvenInMinimal() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property("Edm.Guid", "Ref", ValueType.PRIMITIVE,
            UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
    assertTrue(json.contains("\"Ref@odata.type\":\"#Guid\""));
  }

  @Test
  void closedTypeDropsUndeclaredSilentlyAsToday() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"))
        .addProperty(new Property("Edm.String", "Sneaky", ValueType.PRIMITIVE, "x"));
    final String json = serializeEntity(entity, "ETTwoPrim", ContentType.JSON);
    assertFalse(json.contains("Sneaky"));
  }

  @Test
  void nonNativeDynamicValueNotAnnotatedUnderMetadataNone() throws Exception {
    // odata.metadata=none must never emit @odata.type, even for a non-JSON-native dynamic value:
    // the client has no way to consume type-control annotations under "none" metadata anyway, and
    // the OData JSON spec explicitly excludes them from that mode.
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property("Edm.Guid", "Ref", ValueType.PRIMITIVE,
            UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON_NO_METADATA);
    assertFalse(json.contains("@odata.type"));
    assertTrue(json.contains("\"Ref\":\"01234567-89ab-cdef-0123-456789abcdef\""));
  }

  @Test
  void dynamicPropertyAnnotatedInFullMetadata() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property("Edm.String", "Custom", ValueType.PRIMITIVE, "hello"));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON_FULL_METADATA);
    assertTrue(json.contains("\"Custom@odata.type\":\"#String\""));
  }

  @Test
  void nullTypeDynamicPropertySerializesUsingInferredType() throws Exception {
    // tecsvc's DataCreator seeds ETOpen's dynamic properties with a null type string (Task 1); the
    // serializer must infer a sane type from the Java value rather than fail or mistype it.
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "DynamicString", ValueType.PRIMITIVE, "dynamic"))
        .addProperty(new Property(null, "DynamicInt", ValueType.PRIMITIVE, 42L));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
    assertTrue(json.contains("\"DynamicString\":\"dynamic\""));
    assertTrue(json.contains("\"DynamicInt\":42"));
    // Long infers to Edm.Int64, which is JSON-native for dynamic properties: no annotation expected.
    assertFalse(json.contains("DynamicInt@odata.type"));
  }

  @Test
  void nullTypeDynamicPropertyWithNoValueFallsBackToString() throws Exception {
    // Neither an annotated type nor a value to infer from: must fall back to Edm.String rather than
    // throw, per the task's defensive-handling requirement.
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "DynamicNull", ValueType.PRIMITIVE, null));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
    assertTrue(json.contains("\"DynamicNull\":null"));
  }

  @Test
  void dynamicCollectionPropertySerializedWithoutAnnotationForNativeElementType() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property("Edm.String", "Tags", ValueType.COLLECTION_PRIMITIVE, List.of("a", "b")));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
    assertTrue(json.contains("\"Tags\":[\"a\",\"b\"]"));
    assertFalse(json.contains("Tags@odata.type"));
  }

  @Test
  void dynamicCollectionPropertyAnnotatedForNonNativeElementType() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property("Edm.Guid", "Refs", ValueType.COLLECTION_PRIMITIVE,
            List.of(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"))));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
    assertTrue(json.contains("\"Refs@odata.type\":\"#Collection(Guid)\""));
  }

  @Test
  void dynamicPropertiesSerializedInsideNestedOpenComplexValue() throws Exception {
    // Exercises the writeComplexValue loop (nested complex values), as opposed to the entity-level
    // writeProperties loop the tests above exercise.
    final ComplexValue complexValue = new ComplexValue();
    complexValue.setTypeName(SchemaProvider.NAMESPACE + ".CTOpen");
    complexValue.getValue().add(new Property(null, "CompString", ValueType.PRIMITIVE, "s"));
    complexValue.getValue().add(new Property("Edm.Int64", "CompDynamic", ValueType.PRIMITIVE, 5L));

    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"))
        .addProperty(new Property(SchemaProvider.NAMESPACE + ".CTOpen", "PropertyComp",
            ValueType.COMPLEX, complexValue));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
    assertTrue(json.contains("\"CompDynamic\":5"));
  }

  @Test
  void selectedDynamicPropertyIncludedAndNonSelectedDeclaredExcluded() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"))
        .addProperty(new Property("Edm.String", "Custom", ValueType.PRIMITIVE, "hello"));
    final SelectOption select = parseSelect("ESOpen", "$select=Custom");
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON, select);
    assertTrue(json.contains("\"Custom\":\"hello\""));
    assertFalse(json.contains("PropertyString"));
  }

  @Test
  void selectedAbsentDynamicPropertyOmittedWithoutError() throws Exception {
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"));
    final SelectOption select = parseSelect("ESOpen", "$select=Custom");
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON, select);
    assertFalse(json.contains("Custom"));
    assertFalse(json.contains("PropertyString"));
  }

  @Test
  void selectOfNestedDynamicPropertyUnderDeclaredComplexEmitsOnlyThatMember() throws Exception {
    // Extends selectedDynamicPropertyIncludedAndNonSelectedDeclaredExcluded to a nested complex
    // value: $select=PropertyComp/CompDynamic must write PropertyComp with only its CompDynamic
    // dynamic member (CompString, though present in the data, is not selected and must be
    // omitted), and no top-level declared properties (unselected).
    final ComplexValue complexValue = new ComplexValue();
    complexValue.setTypeName(SchemaProvider.NAMESPACE + ".CTOpen");
    complexValue.getValue().add(new Property(null, "CompString", ValueType.PRIMITIVE, "s"));
    complexValue.getValue().add(new Property("Edm.Int64", "CompDynamic", ValueType.PRIMITIVE, 5L));

    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"))
        .addProperty(new Property(SchemaProvider.NAMESPACE + ".CTOpen", "PropertyComp",
            ValueType.COMPLEX, complexValue));
    final SelectOption select = parseSelect("ESOpen", "$select=PropertyComp/CompDynamic");
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON, select);
    assertTrue(json.contains("\"CompDynamic\":5"));
    assertFalse(json.contains("CompString"));
    assertFalse(json.contains("PropertyString"));
  }

  private SelectOption parseSelect(final String entitySetName, final String selectQuery) throws Exception {
    return new Parser(metadata.getEdm(), odata).parseUri(entitySetName, selectQuery, null, null).getSelectOption();
  }

  private String serializeEntity(final Entity entity, final String entityTypeName, final ContentType contentType)
      throws Exception {
    return serializeEntity(entity, entityTypeName, contentType, null);
  }

  private String serializeEntity(final Entity entity, final String entityTypeName, final ContentType contentType,
      final SelectOption select) throws Exception {
    return serializeEntity(entity, entityTypeName, contentType, select, false);
  }

  private String serializeEntity(final Entity entity, final String entityTypeName, final ContentType contentType,
      final SelectOption select, final boolean omitNulls) throws Exception {
    final EdmEntityType edmEntityType = metadata.getEdm().getEntityType(
        new FullQualifiedName(SchemaProvider.NAMESPACE, entityTypeName));
    entity.setType(edmEntityType.getFullQualifiedName().getFullQualifiedNameAsString());
    final ODataSerializer serializer = new ODataJsonSerializer(contentType);
    final byte[] bytes = serializer.entity(metadata, edmEntityType, entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().type(edmEntityType).build())
            .select(select)
            .omitNulls(omitNulls)
            .build()).getContent().readAllBytes();
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Test
  void omitValuesNullsNeverOmitsDynamicPropertiesButOmitsDeclaredNulls() throws Exception {
    // Behavior matrix: dynamic (open-type) properties are written by a separate code path
    // (writeDynamicProperties/writeDynamicProperty) that the omit-values=nulls preference
    // intentionally does not touch, per the design's spec-silent decision. A null dynamic property
    // must still be written, while a null DECLARED property on the same open-type entity is omitted.
    final Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, null))
        .addProperty(new Property(null, "DynamicNull", ValueType.PRIMITIVE, null));
    final String json = serializeEntity(entity, "ETOpen", ContentType.JSON, null, true);
    assertTrue(json.contains("\"DynamicNull\":null"));
    assertFalse(json.contains("PropertyString"));
  }
}
