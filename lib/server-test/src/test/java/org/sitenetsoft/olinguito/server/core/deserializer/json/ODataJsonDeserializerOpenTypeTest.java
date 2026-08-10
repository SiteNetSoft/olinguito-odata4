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
 */
package org.sitenetsoft.olinguito.server.core.deserializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Property;
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
  void dynamicArrayValueNotYetSupportedOnOpenType() {
    // Array-valued dynamic properties are Task 3's job to support; until then they must be left
    // unconsumed and fall through to UNKNOWN_CONTENT, same as object-valued dynamic properties.
    // Task 3 is expected to update/replace this test once arrays become supported.
    final String payload = "{\"PropertyInt16\":1,\"PropertyString\":\"abc\",\"Tags\":[\"a\",\"b\"]}";
    final DeserializerException e = assertThrows(DeserializerException.class,
        () -> deserialize(payload, "ETOpen"));
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
