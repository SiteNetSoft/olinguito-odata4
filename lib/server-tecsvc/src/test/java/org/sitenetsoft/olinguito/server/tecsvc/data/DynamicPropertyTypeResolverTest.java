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
 * Copyright 2026 SiteNetSoft - New file: OpenType CRUD Task 3 unit coverage for
 * DynamicPropertyTypeResolver
 */
package org.sitenetsoft.olinguito.server.tecsvc.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;

import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.junit.jupiter.api.Test;

class DynamicPropertyTypeResolverTest {

  @Test
  void resolvesFromExplicitStoredTypeName() {
    final Property property = new Property("Edm.Int32", "Dynamic", ValueType.PRIMITIVE, 42L);
    assertEquals(EdmPrimitiveTypeKind.Int32, DynamicPropertyTypeResolver.resolveKind(property));
  }

  @Test
  void fallsBackToValueInferenceWhenStoredTypeNameIsUnresolvable() {
    final Property property = new Property("not.a.real.Type", "Dynamic", ValueType.PRIMITIVE, "text");
    assertEquals(EdmPrimitiveTypeKind.String, DynamicPropertyTypeResolver.resolveKind(property));
  }

  @Test
  void inferLong() {
    assertEquals(EdmPrimitiveTypeKind.Int64,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, 42L)));
  }

  @Test
  void inferShort() {
    assertEquals(EdmPrimitiveTypeKind.Int16,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, (short) 1)));
  }

  @Test
  void inferInteger() {
    assertEquals(EdmPrimitiveTypeKind.Int32,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, 1)));
  }

  @Test
  void inferBoolean() {
    assertEquals(EdmPrimitiveTypeKind.Boolean,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, true)));
  }

  @Test
  void inferFloat() {
    assertEquals(EdmPrimitiveTypeKind.Single,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, 1.5f)));
  }

  @Test
  void inferDouble() {
    assertEquals(EdmPrimitiveTypeKind.Double,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, 1.5d)));
  }

  @Test
  void inferBigDecimal() {
    assertEquals(EdmPrimitiveTypeKind.Decimal,
        DynamicPropertyTypeResolver.resolveKind(
            new Property(null, "Dynamic", ValueType.PRIMITIVE, new BigDecimal("1.5"))));
  }

  @Test
  void inferUuid() {
    assertEquals(EdmPrimitiveTypeKind.Guid,
        DynamicPropertyTypeResolver.resolveKind(
            new Property(null, "Dynamic", ValueType.PRIMITIVE, UUID.randomUUID())));
  }

  @Test
  void inferByte() {
    assertEquals(EdmPrimitiveTypeKind.SByte,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, (byte) 1)));
  }

  @Test
  void inferStringDefault() {
    assertEquals(EdmPrimitiveTypeKind.String,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, "dynamic")));
  }

  @Test
  void nullValueFallsBackToStringDefault() {
    assertEquals(EdmPrimitiveTypeKind.String,
        DynamicPropertyTypeResolver.resolveKind(new Property(null, "Dynamic", ValueType.PRIMITIVE, null)));
  }
}
