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
 * Copyright 2026 SiteNetSoft - New file: pin tests proving the client is schema-agnostic
 * with respect to OData open-type dynamic properties (read and write)
 * Copyright 2026 SiteNetSoft - Add write/read pins for non-JSON-native dynamic primitive
 * types (Guid, DateTimeOffset), which need an explicit name@odata.type annotation to
 * round-trip since the client has no EDM to fall back on
 * Copyright 2026 SiteNetSoft - Add write pin for a dynamic Byte property: neither the client's
 * nor the server's bare-JSON-number inference can ever produce Byte, so it must be annotated too
 */
package org.sitenetsoft.olinguito.client.core.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientObjectFactory;
import org.sitenetsoft.olinguito.client.api.domain.ClientProperty;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmInt32;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmString;
import org.junit.jupiter.api.Test;

/**
 * Pins the client's ability to round-trip OData open-type "dynamic" properties - properties
 * that are not declared in {@code $metadata} - without any code changes. The client's JSON
 * deserializer ({@link JsonDeserializer#populate}) and serializer
 * ({@code JsonEntitySerializer#doSerialize}) both operate purely off the wire payload / the
 * {@code ClientEntity}'s own property list, never consulting an EDM, so undeclared properties
 * were already expected to just work. These tests exist to prove that expectation, not to add
 * new behavior; see {@code docs/site/guides/open-types-guide.md} for the feature writeup.
 */
class OpenTypeClientTest {

  private final ODataClient client = ODataClientFactory.getClient();

  @Test
  void deserializesUndeclaredPropertiesWithInferredTypes() throws Exception {
    final String json = "{"
        + "\"@odata.context\":\"$metadata#ESOpen/$entity\","
        + "\"PropertyInt16\":32767,"
        + "\"Custom\":\"hello\","
        + "\"CustomInt\":42"
        + "}";
    final InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

    final ClientEntity entity = client.getBinder().getODataEntity(
        client.getDeserializer(ContentType.JSON).toEntity(input));
    assertNotNull(entity);

    final ClientProperty custom = entity.getProperty("Custom");
    assertNotNull(custom);
    assertTrue(custom.hasPrimitiveValue());
    assertEquals("hello", custom.getPrimitiveValue().toValue());
    assertEquals(EdmString.getInstance(), custom.getPrimitiveValue().getType());

    final ClientProperty customInt = entity.getProperty("CustomInt");
    assertNotNull(customInt);
    assertTrue(customInt.hasPrimitiveValue());
    assertEquals(42, customInt.getPrimitiveValue().toValue());
    assertEquals(EdmInt32.getInstance(), customInt.getPrimitiveValue().getType());
  }

  @Test
  void serializesAnUndeclaredPropertyAddedThroughTheObjectFactory() throws Exception {
    final ClientObjectFactory factory = client.getObjectFactory();
    final ClientEntity entity = factory.newEntity(new FullQualifiedName("Olinguito.OData", "ETOpen"));
    entity.getProperties().add(factory.newPrimitiveProperty("PropertyInt16",
        factory.newPrimitiveValueBuilder().buildInt16((short) 100)));
    entity.getProperties().add(factory.newPrimitiveProperty("PropertyString",
        factory.newPrimitiveValueBuilder().buildString("created open type entity")));
    // Undeclared (dynamic) property: no EDM is consulted anywhere in the write path, so this
    // is legal to add regardless of what $metadata says about ETOpen.
    entity.getProperties().add(factory.newPrimitiveProperty("Brand",
        factory.newPrimitiveValueBuilder().buildString("new")));

    final InputStream written = client.getWriter().writeEntity(entity, ContentType.JSON);
    final String json = new String(written.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(json.contains("\"Brand\":\"new\""), () -> "expected dynamic property in JSON output: " + json);
  }

  @Test
  void serializesADynamicGuidPropertyWithATypeAnnotation() throws Exception {
    final ClientObjectFactory factory = client.getObjectFactory();
    final ClientEntity entity = factory.newEntity(new FullQualifiedName("Olinguito.OData", "ETOpen"));
    entity.getProperties().add(factory.newPrimitiveProperty("PropertyInt16",
        factory.newPrimitiveValueBuilder().buildInt16((short) 100)));
    // Undeclared (dynamic) Guid property: its EDM kind is not recoverable from a bare JSON
    // string, so the type annotation is mandatory for a schema-agnostic reader (e.g. the
    // server's open-type dynamic-property inference) to avoid silently mistyping it as String.
    entity.getProperties().add(factory.newPrimitiveProperty("Ref",
        factory.newPrimitiveValueBuilder().buildGuid(
            UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"))));

    final InputStream written = client.getWriter().writeEntity(entity, ContentType.JSON);
    final String json = new String(written.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(json.contains("\"Ref@odata.type\":\"#Guid\""),
        () -> "expected a #Guid type annotation for the dynamic Guid property: " + json);
  }

  @Test
  void serializesADynamicBytePropertyWithATypeAnnotation() throws Exception {
    final ClientObjectFactory factory = client.getObjectFactory();
    final ClientEntity entity = factory.newEntity(new FullQualifiedName("Olinguito.OData", "ETOpen"));
    entity.getProperties().add(factory.newPrimitiveProperty("PropertyInt16",
        factory.newPrimitiveValueBuilder().buildInt16((short) 100)));
    // Undeclared (dynamic) Byte property. Byte is JSON-number-shaped like Int16/Int32/Int64, but
    // unlike those, neither the client's nor the server's inference (both only ever distinguish
    // Int16/Int32/Int64/Single/Double/Decimal from a bare JSON number) can produce Byte from an
    // unannotated value - so it needs the annotation just as much as a non-numeric type like Guid.
    entity.getProperties().add(factory.newPrimitiveProperty("Age",
        factory.newPrimitiveValueBuilder().setType(EdmPrimitiveTypeKind.Byte).setValue((short) 5).build()));

    final InputStream written = client.getWriter().writeEntity(entity, ContentType.JSON);
    final String json = new String(written.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(json.contains("\"Age@odata.type\":\"#Byte\""),
        () -> "expected a #Byte type annotation for the dynamic Byte property: " + json);
  }

  @Test
  void deserializesAnAnnotatedDynamicDateTimeOffsetProperty() throws Exception {
    final String json = "{"
        + "\"@odata.context\":\"$metadata#ESOpen/$entity\","
        + "\"PropertyInt16\":1,"
        + "\"When@odata.type\":\"#DateTimeOffset\","
        + "\"When\":\"2026-01-01T00:00:00Z\""
        + "}";
    final InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

    final ClientEntity entity = client.getBinder().getODataEntity(
        client.getDeserializer(ContentType.JSON).toEntity(input));
    assertNotNull(entity);

    final ClientProperty when = entity.getProperty("When");
    assertNotNull(when);
    assertTrue(when.hasPrimitiveValue());
    assertEquals(EdmDateTimeOffset.getInstance(), when.getPrimitiveValue().getType());
  }
}
