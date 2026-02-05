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
 * Copyright 2026 SiteNetSoft - Fixed deprecated API usages and code quality warnings
 */
package org.sitenetsoft.olinguito.server.core.deserializer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.deserializer.FixedFormatDeserializer;
import org.junit.Test;
import org.mockito.Mockito;

public class FixedFormatDeserializerTest {

  private static final OData oData = OData.newInstance();
  private final FixedFormatDeserializer deserializer = oData.createFixedFormatDeserializer();

  @Test
  public void binary() throws Exception {
    assertArrayEquals(new byte[] { 0x41, 0x42, 0x43 },
        deserializer.binary(IOUtils.toInputStream("ABC", StandardCharsets.UTF_8)));
  }

  @Test
  public void binaryLong() throws Exception {
    assertEquals(4 * 3 * 26,
        deserializer.binary(IOUtils.toInputStream(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ",
                StandardCharsets.UTF_8)).length);
  }

  @Test
  public void primitiveValue() throws Exception {
    EdmProperty property = Mockito.mock(EdmProperty.class);
    Mockito.when(property.getType()).thenReturn(oData.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int64));
    Mockito.when(property.isPrimitive()).thenReturn(true);
    assertEquals(42L, deserializer.primitiveValue(IOUtils.toInputStream("42", StandardCharsets.UTF_8), property));
  }

  @Test
  public void primitiveValueLong() throws Exception {
    EdmProperty property = Mockito.mock(EdmProperty.class);
    Mockito.when(property.getType()).thenReturn(oData.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String));
    Mockito.when(property.isPrimitive()).thenReturn(true);
    Mockito.when(property.isUnicode()).thenReturn(true);
    Mockito.when(property.getMaxLength()).thenReturn(61);
    final String value = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ\n"
        + "ÄÖÜ€﷼\n"
        + String.valueOf(Character.toChars(0x1F603));
    assertEquals(value, deserializer.primitiveValue(IOUtils.toInputStream(value, StandardCharsets.UTF_8), property));
  }
}
