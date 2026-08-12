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
 * Copyright 2026 SiteNetSoft - Add ODataDeserializer.dynamicProperty tests (OpenType CRUD Task 1)
 */
package org.sitenetsoft.olinguito.server.core.deserializer.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer;
import org.sitenetsoft.olinguito.server.core.deserializer.AbstractODataDeserializerTest;
import org.junit.jupiter.api.Test;

class ODataJsonDeserializerDynamicPropertyTest extends AbstractODataDeserializerTest {

  private static final OData odata = OData.newInstance();

  @Test
  void bareStringValue() throws Exception {
    final Property p = deserializeDynamic("{\"value\":\"hello\"}", "Custom");
    assertEquals("Custom", p.getName());
    assertEquals("hello", p.getValue());
    assertEquals("Edm.String", p.getType());
  }

  @Test
  void annotatedGuidValue() throws Exception {
    final Property p = deserializeDynamic(
        "{\"value@odata.type\":\"#Guid\",\"value\":\"01234567-89ab-cdef-0123-456789abcdef\"}", "Ref");
    assertEquals("Edm.Guid", p.getType());
    assertEquals(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"), p.getValue());
  }

  @Test
  void nullValue() throws Exception {
    final Property p = deserializeDynamic("{\"value\":null}", "Gone");
    assertTrue(p.isNull());
  }

  @Test
  void collectionValue() throws Exception {
    final Property p = deserializeDynamic("{\"value\":[\"a\",\"b\"]}", "Tags");
    assertEquals(ValueType.COLLECTION_PRIMITIVE, p.getValueType());
    assertEquals(List.of("a", "b"), p.asCollection());
  }

  @Test
  void annotatedCollectionValue() throws Exception {
    final Property p = deserializeDynamic(
        "{\"value@odata.type\":\"#Collection(Guid)\",\"value\":[\"01234567-89ab-cdef-0123-456789abcdef\"]}", "Refs");
    // Mirrors the entity-level annotated-collection convention (see
    // ODataJsonDeserializerOpenTypeTest#annotatedDynamicCollectionParsedAsDeclaredElementType):
    // Property.getType() stores the bare element type, not "Collection(...)"-wrapped.
    assertEquals("Edm.Guid", p.getType());
    assertEquals(List.of(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")), p.asCollection());
  }

  @Test
  void objectValueRejected() {
    assertThrows(DeserializerException.class, () -> deserializeDynamic("{\"value\":{\"a\":1}}", "Nested"));
  }

  @Test
  void unknownAnnotatedTypeRejected() {
    assertThrows(DeserializerException.class,
        () -> deserializeDynamic("{\"value@odata.type\":\"#No.Such\",\"value\":\"x\"}", "X"));
  }

  @Test
  void garbagePayloadRejected() {
    assertThrows(DeserializerException.class, () -> deserializeDynamic("not json", "X"));
  }

  @Test
  void defaultMethodThrowsNotImplemented() throws Exception {
    final ODataDeserializer xml = odata.createDeserializer(ContentType.APPLICATION_XML);
    assertThrows(DeserializerException.class,
        () -> xml.dynamicProperty(new ByteArrayInputStream("<x/>".getBytes(UTF_8)), "X"));
  }

  @Test
  void emptyPayloadRejected() {
    assertThrows(DeserializerException.class, () -> deserializeDynamic("{}", "X"));
  }

  @Test
  void missingValueMemberRejected() {
    assertThrows(DeserializerException.class, () -> deserializeDynamic("{\"foo\":\"bar\"}", "X"));
  }

  @Test
  void whitespacePayloadRejected() {
    assertThrows(DeserializerException.class, () -> deserializeDynamic("   ", "X"));
  }

  @Test
  void collectionOfObjectsRejected() {
    assertThrows(DeserializerException.class, () -> deserializeDynamic("{\"value\":[{\"a\":1}]}", "Nested"));
  }

  @Test
  void emptyCollectionInfersString() throws Exception {
    final Property p = deserializeDynamic("{\"value\":[]}", "Empty");
    assertEquals("Edm.String", p.getType());
    assertEquals(List.of(), p.asCollection());
  }

  @Test
  void integerValueInfersInt32() throws Exception {
    final Property p = deserializeDynamic("{\"value\":42}", "Num");
    assertEquals("Edm.Int32", p.getType());
    assertEquals(42, p.getValue());
  }

  private Property deserializeDynamic(final String payload, final String propertyName) throws DeserializerException {
    final ODataDeserializer deserializer = odata.createDeserializer(ContentType.JSON);
    return deserializer.dynamicProperty(new ByteArrayInputStream(payload.getBytes(UTF_8)), propertyName)
        .getProperty();
  }
}
