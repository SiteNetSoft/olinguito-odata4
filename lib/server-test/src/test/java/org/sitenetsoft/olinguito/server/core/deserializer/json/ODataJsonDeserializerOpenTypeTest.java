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
 * Copyright 2026 SiteNetSoft - Add OpenType support (dynamic property deserialization)
 * Copyright 2026 SiteNetSoft - OpenType: support name@odata.type annotations and
 * primitive collections for dynamic properties
 * Copyright 2026 SiteNetSoft - OpenType: dynamic properties inside open complex values
 * Copyright 2026 SiteNetSoft - OpenType: annotated dynamic collection element type tests
 * Copyright 2026 SiteNetSoft - Client/server symmetry: feed the exact payload the client
 * writer now produces for a dynamic Guid collection under minimal metadata back into the
 * server deserializer and confirm the element type survives the round trip
 */
package org.sitenetsoft.olinguito.server.core.deserializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer;
import org.sitenetsoft.olinguito.server.core.deserializer.AbstractODataDeserializerTest;
import org.junit.jupiter.api.Test;

class ODataJsonDeserializerOpenTypeTest extends AbstractODataDeserializerTest {

  private static final OData odata = OData.newInstance();

  @Test
  void dynamicPrimitivesAcceptedOnOpenType() throws Exception {
    final String payload = "{\"PropertyInt16\":1,\"PropertyString\":\"abc\","
        + "\"Custom\":\"hello\",\"CustomInt\":42,\"CustomBool\":true,\"CustomNull\":null}";
    final Entity entity = deserialize(payload, "ETOpen");
    assertEquals(6, entity.getProperties().size());
    final Property dynamic = entity.getProperty("Custom");
    assertNotNull(dynamic);
    assertEquals("hello", dynamic.getValue());
    assertEquals("Edm.String", dynamic.getType());
    assertEquals("Edm.Int32", entity.getProperty("CustomInt").getType());
    assertEquals("Edm.Boolean", entity.getProperty("CustomBool").getType());
    assertTrue(entity.getProperty("CustomNull").isNull());
  }

  @Test
  void dynamicIntegerOutOfInt32RangeInfersInt64() throws Exception {
    final String payload = "{\"PropertyInt16\":1,\"PropertyString\":\"abc\",\"CustomBigInt\":9999999999}";
    final Entity entity = deserialize(payload, "ETOpen");
    assertEquals("Edm.Int64", entity.getProperty("CustomBigInt").getType());
  }

  @Test
  void annotatedDynamicPropertyParsedAsDeclaredType() throws Exception {
    final String payload = "{\"PropertyInt16\":1,"
        + "\"When@odata.type\":\"#DateTimeOffset\",\"When\":\"2026-08-09T12:00:00Z\"}";
    final Entity entity = deserialize(payload, "ETOpen");
    final Property when = entity.getProperty("When");
    assertEquals("Edm.DateTimeOffset", when.getType());
    // EdmDateTimeOffset.getDefaultType() is java.sql.Timestamp in this codebase.
    assertTrue(when.getValue() instanceof Timestamp);
  }

  @Test
  void dynamicCollectionOfPrimitives() throws Exception {
    final String payload = "{\"PropertyInt16\":1,\"Tags\":[\"a\",\"b\"],\"Empty\":[]}";
    final Entity entity = deserialize(payload, "ETOpen");
    final Property tags = entity.getProperty("Tags");
    assertEquals(ValueType.COLLECTION_PRIMITIVE, tags.getValueType());
    assertEquals(List.of("a", "b"), tags.asCollection());
    assertEquals("Collection(Edm.String)", "Collection(" + entity.getProperty("Empty").getType() + ")");
  }

  @Test
  void annotatedDynamicCollectionParsedAsDeclaredElementType() throws Exception {
    // "Refs@odata.type":"#Collection(Guid)" must resolve every element as Edm.Guid rather than the
    // unannotated inference path's Edm.String default.
    final String payload = "{\"PropertyInt16\":1,"
        + "\"Refs@odata.type\":\"#Collection(Guid)\","
        + "\"Refs\":[\"01234567-89ab-cdef-0123-456789abcdef\",\"11234567-89ab-cdef-0123-456789abcdef\"]}";
    final Entity entity = deserialize(payload, "ETOpen");
    final Property refs = entity.getProperty("Refs");
    assertEquals(ValueType.COLLECTION_PRIMITIVE, refs.getValueType());
    assertEquals("Edm.Guid", refs.getType());
    assertEquals(
        List.of(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"),
            UUID.fromString("11234567-89ab-cdef-0123-456789abcdef")),
        refs.asCollection());
  }

  @Test
  void clientWrittenDynamicGuidCollectionPayloadSurvivesServerDeserialization() throws Exception {
    // Client/server symmetry: this is the exact payload JsonSerializer#valuable (client-core)
    // now produces for a dynamic Collection(Edm.Guid) property under minimal metadata - see
    // OpenTypeClientTest#dynamicGuidCollectionWrittenUnderMinimalMetadataCarriesCollectionAnnotation
    // in lib/client-core - proving the new client "name@odata.type":"#Collection(X)" annotation
    // round-trips through the server's existing dynamic-collection-annotation support rather
    // than silently defaulting to Collection(Edm.String).
    final String payload = "{\"PropertyInt16\":100,"
        + "\"Refs@odata.type\":\"#Collection(Guid)\","
        + "\"Refs\":[\"01234567-89ab-cdef-0123-456789abcdef\"]}";
    final Entity entity = deserialize(payload, "ETOpen");
    final Property refs = entity.getProperty("Refs");
    assertEquals(ValueType.COLLECTION_PRIMITIVE, refs.getValueType());
    assertEquals("Edm.Guid", refs.getType());
    assertEquals(List.of(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")), refs.asCollection());
  }

  @Test
  void annotatedDynamicCollectionWithUnknownElementTypeRejected() {
    final String payload = "{\"PropertyInt16\":1,"
        + "\"Refs@odata.type\":\"#Collection(No.Such.Type)\",\"Refs\":[\"v\"]}";
    assertThrows(DeserializerException.class, () -> deserialize(payload, "ETOpen"));
  }

  @Test
  void jsonObjectInDynamicSlotRejected() {
    final String payload = "{\"PropertyInt16\":1,\"Nested\":{\"a\":1}}";
    final DeserializerException e = assertThrows(DeserializerException.class,
        () -> deserialize(payload, "ETOpen"));
    assertEquals(DeserializerException.MessageKeys.UNKNOWN_CONTENT, e.getMessageKey());
  }

  @Test
  void unknownAnnotatedTypeRejected() {
    final String payload = "{\"PropertyInt16\":1,\"X@odata.type\":\"#No.Such.Type\",\"X\":\"v\"}";
    assertThrows(DeserializerException.class, () -> deserialize(payload, "ETOpen"));
  }

  @Test
  void dynamicPropertyInsideOpenComplexValue() throws Exception {
    final String payload = "{\"PropertyInt16\":1,"
        + "\"PropertyComp\":{\"CompString\":\"s\",\"CompDynamic\":5}}";
    final Entity entity = deserialize(payload, "ETOpen");
    final ComplexValue comp = entity.getProperty("PropertyComp").asComplex();
    assertEquals(2, comp.getValue().size());
    assertEquals("CompDynamic", comp.getValue().get(1).getName());
  }

  @Test
  void dynamicPropertyInsideClosedComplexValueRejected() {
    // ETCompAllPrim.PropertyComp is CTAllPrim, a closed complex type: an unrecognized member
    // inside it must still be rejected, mirroring unknownPropertyStillRejectedOnClosedType.
    final String payload = "{\"PropertyInt16\":1,\"PropertyComp\":{\"CompDynamic\":5}}";
    final DeserializerException e = assertThrows(DeserializerException.class,
        () -> deserialize(payload, "ETCompAllPrim"));
    assertEquals(DeserializerException.MessageKeys.UNKNOWN_CONTENT, e.getMessageKey());
  }

  @Test
  void unknownPropertyStillRejectedOnClosedType() {
    final String payload = "{\"PropertyInt16\":1,\"PropertyString\":\"abc\",\"Custom\":\"x\"}";
    final DeserializerException e = assertThrows(DeserializerException.class,
        () -> deserialize(payload, "ETTwoPrim"));
    assertEquals(DeserializerException.MessageKeys.UNKNOWN_CONTENT, e.getMessageKey());
  }

  private Entity deserialize(final String entityString, final String entityTypeName) throws DeserializerException {
    final EdmEntityType entityType = edm.getEntityType(new FullQualifiedName("olingo.odata.test1", entityTypeName));
    final ODataDeserializer deserializer = odata.createDeserializer(ContentType.JSON);
    return deserializer.entity(new ByteArrayInputStream(entityString.getBytes(StandardCharsets.UTF_8)), entityType)
        .getEntity();
  }
}
