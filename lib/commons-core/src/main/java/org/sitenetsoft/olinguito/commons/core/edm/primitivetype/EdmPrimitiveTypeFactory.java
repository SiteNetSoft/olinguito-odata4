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
 * Copyright 2026 SiteNetSoft - Converted switch statements to switch expressions
 */
package org.sitenetsoft.olinguito.commons.core.edm.primitivetype;

import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;

public final class EdmPrimitiveTypeFactory {

  /**
   * Returns an instance for the provided {@link EdmPrimitiveTypeKind} in the form of {@link EdmPrimitiveType}.
   *
   * @param kind EdmPrimitiveTypeKind
   * @return {@link EdmPrimitiveType} instance
   */
  public static EdmPrimitiveType getInstance(final EdmPrimitiveTypeKind kind) {
    return switch (kind) {
      case Binary -> EdmBinary.getInstance();
      case Boolean -> EdmBoolean.getInstance();
      case Byte -> EdmByte.getInstance();
      case SByte -> EdmSByte.getInstance();
      case Date -> EdmDate.getInstance();
      case DateTimeOffset -> EdmDateTimeOffset.getInstance();
      case TimeOfDay -> EdmTimeOfDay.getInstance();
      case Duration -> EdmDuration.getInstance();
      case Decimal -> EdmDecimal.getInstance();
      case Single -> EdmSingle.getInstance();
      case Double -> EdmDouble.getInstance();
      case Guid -> EdmGuid.getInstance();
      case Int16 -> EdmInt16.getInstance();
      case Int32 -> EdmInt32.getInstance();
      case Int64 -> EdmInt64.getInstance();
      case String -> EdmString.getInstance();
      case Stream -> EdmStream.getInstance();
      case Geography -> EdmGeography.getInstance();
      case GeographyPoint -> EdmGeographyPoint.getInstance();
      case GeographyLineString -> EdmGeographyLineString.getInstance();
      case GeographyPolygon -> EdmGeographyPolygon.getInstance();
      case GeographyMultiPoint -> EdmGeographyMultiPoint.getInstance();
      case GeographyMultiLineString -> EdmGeographyMultiLineString.getInstance();
      case GeographyMultiPolygon -> EdmGeographyMultiPolygon.getInstance();
      case GeographyCollection -> EdmGeographyCollection.getInstance();
      case Geometry -> EdmGeometry.getInstance();
      case GeometryPoint -> EdmGeometryPoint.getInstance();
      case GeometryLineString -> EdmGeometryLineString.getInstance();
      case GeometryPolygon -> EdmGeometryPolygon.getInstance();
      case GeometryMultiPoint -> EdmGeometryMultiPoint.getInstance();
      case GeometryMultiLineString -> EdmGeometryMultiLineString.getInstance();
      case GeometryMultiPolygon -> EdmGeometryMultiPolygon.getInstance();
      case GeometryCollection -> EdmGeometryCollection.getInstance();
    };
  }

  private EdmPrimitiveTypeFactory() {
    // empty constructor for static utility class
  }

}
