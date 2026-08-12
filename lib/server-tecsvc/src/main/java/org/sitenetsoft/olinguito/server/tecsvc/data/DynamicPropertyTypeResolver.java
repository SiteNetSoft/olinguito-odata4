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
 * Copyright 2026 SiteNetSoft - New file: shared dynamic-property EdmPrimitiveTypeKind resolution,
 * extracted from ExpressionVisitorImpl so the tecsvc GET-dispatch path (Task 3 of the OpenType
 * CRUD project) can reuse the same idiom instead of duplicating it
 */
package org.sitenetsoft.olinguito.server.tecsvc.data;

import java.math.BigDecimal;
import java.util.UUID;

import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;

/**
 * Resolves the {@link EdmPrimitiveTypeKind} of a dynamic (undeclared, open-type) {@link Property}.
 *
 * <p>Dynamic properties carry no EDM property definition, so their runtime type has to be
 * derived: first from the stored type-name string (set when the value came in with an explicit
 * {@code @type} annotation), falling back to inference from the Java value's class, and
 * ultimately to {@link EdmPrimitiveTypeKind#String} when neither is conclusive.</p>
 */
public final class DynamicPropertyTypeResolver {

  private DynamicPropertyTypeResolver() {
  }

  /**
   * Resolves the {@link EdmPrimitiveTypeKind} for the given dynamic property.
   *
   * @param property the dynamic property to resolve the kind for; its value may be {@code null}
   * @return the resolved kind, defaulting to {@link EdmPrimitiveTypeKind#String} when the stored
   *         type name is absent/unresolvable and the value's Java class is not one of the
   *         explicitly recognized scalar types
   */
  public static EdmPrimitiveTypeKind resolveKind(final Property property) {
    final String typeName = property.getType();
    if (typeName != null) {
      try {
        return EdmPrimitiveTypeKind.valueOfFQN(typeName);
      } catch (final IllegalArgumentException e) {
        // Unresolvable stored type string; fall through to value-based inference below.
      }
    }
    return kindFromValue(property.getValue());
  }

  /**
   * Infers an {@link EdmPrimitiveTypeKind} from a dynamic property's Java value class. This
   * mapping is deliberately partial - it only covers the Java types the tecsvc JSON
   * deserializer actually produces for untyped/inferred dynamic scalars (numbers, booleans,
   * strings, GUIDs). Types with no unambiguous single Java representation without an explicit
   * {@code @type} annotation - e.g. {@code byte[]} (could be Edm.Binary or Edm.Duration-as-bytes),
   * temporal values (Edm.Date/DateTimeOffset/TimeOfDay/Duration all round-trip through different
   * Java shapes) - are intentionally left unhandled here and fall through to the String default;
   * such values should arrive with an explicit stored type name instead (see the {@code typeName}
   * branch above).
   */
  private static EdmPrimitiveTypeKind kindFromValue(final Object value) {
    if (value instanceof Short) {
      return EdmPrimitiveTypeKind.Int16;
    } else if (value instanceof Integer) {
      return EdmPrimitiveTypeKind.Int32;
    } else if (value instanceof Long) {
      return EdmPrimitiveTypeKind.Int64;
    } else if (value instanceof Boolean) {
      return EdmPrimitiveTypeKind.Boolean;
    } else if (value instanceof Float) {
      return EdmPrimitiveTypeKind.Single;
    } else if (value instanceof Double) {
      return EdmPrimitiveTypeKind.Double;
    } else if (value instanceof BigDecimal) {
      return EdmPrimitiveTypeKind.Decimal;
    } else if (value instanceof UUID) {
      return EdmPrimitiveTypeKind.Guid;
    } else if (value instanceof Byte) {
      return EdmPrimitiveTypeKind.SByte;
    }
    return EdmPrimitiveTypeKind.String;
  }
}
