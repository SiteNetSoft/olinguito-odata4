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
 * Copyright 2026 SiteNetSoft - OData 4.01: single resolver for operation-parameter URI literals and
 * optional-parameter default values
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import org.sitenetsoft.olinguito.commons.api.edm.EdmMapping;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;

/**
 * Single place where an operation parameter's URI literal is turned into a typed value, and where
 * the default value of an omitted optional parameter (OData 4.01, Part 1: Protocol, section
 * 11.5.4.1.1; Core vocabulary term <code>Core.OptionalParameter</code>) is read. The Core vocabulary
 * states the default value uses "the same rules as the cast function in URLs", so it is a URI
 * literal and goes through exactly the same conversion as a value supplied in the request.
 */
public final class OptionalParameterDefaults {

  private OptionalParameterDefaults() {
    // utility class
  }

  /**
   * @param parameter the operation parameter
   * @return the parameter's default value as URI literal, or <code>null</code> when the parameter is
   *         not optional or declares no default value
   */
  public static String defaultLiteral(final EdmParameter parameter) {
    return parameter.isOptional() ? parameter.getOptionalDefaultValue() : null;
  }

  /**
   * @param parameter the operation parameter
   * @return whether the parameter is a non-collection primitive, type definition or enumeration and
   *         therefore carries a URI literal rather than a JSON payload
   */
  public static boolean isPrimitiveLike(final EdmParameter parameter) {
    final EdmType type = parameter.getType();
    final EdmTypeKind kind = type == null ? null : type.getKind();
    return !parameter.isCollection()
        && (kind == EdmTypeKind.PRIMITIVE || kind == EdmTypeKind.DEFINITION || kind == EdmTypeKind.ENUM);
  }

  /**
   * @param parameter the operation parameter
   * @param primitiveType the parameter's type
   * @return the Java class a value of this parameter is materialized as, honoring an
   *         {@link EdmMapping} when the provider declared one
   */
  public static Class<?> targetClass(final EdmParameter parameter, final EdmPrimitiveType primitiveType) {
    final EdmMapping mapping = parameter.getMapping();
    return mapping == null || mapping.getMappedJavaClass() == null
        ? primitiveType.getDefaultType() : mapping.getMappedJavaClass();
  }

  /**
   * Converts a URI literal into the parameter's typed value, applying the parameter's facets.
   * @param parameter the operation parameter
   * @param primitiveType the parameter's type
   * @param uriLiteral the URI literal to convert; may be <code>null</code>
   * @return the typed value
   * @throws EdmPrimitiveTypeException if the literal is not valid for the parameter
   */
  public static Object valueOfUriLiteral(final EdmParameter parameter, final EdmPrimitiveType primitiveType,
      final String uriLiteral) throws EdmPrimitiveTypeException {
    return primitiveType.valueOfString(primitiveType.fromUriLiteral(uriLiteral),
        parameter.isNullable(), parameter.getMaxLength(), parameter.getPrecision(), parameter.getScale(),
        true, targetClass(parameter, primitiveType));
  }
}
