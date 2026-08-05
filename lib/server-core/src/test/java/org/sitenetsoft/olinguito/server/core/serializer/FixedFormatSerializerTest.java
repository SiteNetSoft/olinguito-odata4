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
 * Copyright 2026 SiteNetSoft - Replaced wildcard import with explicit imports
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.commons.api.data.EntityMediaObject;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.serializer.FixedFormatSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveValueSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;
import org.junit.jupiter.api.Test;

class FixedFormatSerializerTest {

  private final FixedFormatSerializer serializer;

  public FixedFormatSerializerTest() throws SerializerException {
    serializer = OData.newInstance().createFixedFormatSerializer();
  }

  @Test
  void binary() throws Exception {
    assertEquals("ABC",
        new String(serializer.binary(new byte[] { 0x41, 0x42, 0x43 }).readAllBytes(), StandardCharsets.UTF_8));
  }

  @Test
  void count() throws Exception {
    assertEquals("42", new String(serializer.count(42).readAllBytes(), StandardCharsets.UTF_8));
  }

  @Test
  void primitiveValue() throws Exception {
    final EdmPrimitiveType type = OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32);
    assertEquals("42", new String(serializer.primitiveValue(type, 42,
        PrimitiveValueSerializerOptions.with().nullable(true).build()).readAllBytes(), StandardCharsets.UTF_8));
  }
  
  @Test
  void binaryIntoStreamed() throws Exception {
	  EntityMediaObject mediaObject = new EntityMediaObject();
	  mediaObject.setBytes(new byte[] { 0x41, 0x42, 0x43 });
	  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    new FixedFormatSerializerImpl().binaryIntoStreamed(mediaObject, outputStream);
    assertEquals(mediaObject.getBytes().length, outputStream.toByteArray().length);
  }
  
  @Test
  void mediaEntityStreamed() throws Exception {
	  EntityMediaObject mediaObject = new EntityMediaObject();
	  mediaObject.setBytes(new byte[] { 0x41, 0x42, 0x43 });
	  SerializerStreamResult result = serializer.mediaEntityStreamed(mediaObject);
	  assertNotNull(result.getODataContent());
  }
}
