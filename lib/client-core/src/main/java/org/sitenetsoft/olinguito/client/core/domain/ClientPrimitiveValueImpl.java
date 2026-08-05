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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced manual hashCode with Objects.hash()
 * Copyright 2026 SiteNetSoft - Modernized equals/hashCode with Objects utility methods
 */
package org.sitenetsoft.olinguito.client.core.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import org.sitenetsoft.olinguito.client.api.domain.AbstractClientValue;
import org.sitenetsoft.olinguito.client.api.domain.ClientEnumValue;
import org.sitenetsoft.olinguito.client.api.domain.ClientPrimitiveValue;
import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;

public class ClientPrimitiveValueImpl extends AbstractClientValue implements ClientPrimitiveValue {

  public static class BuilderImpl implements Builder {

    private final ClientPrimitiveValueImpl instance;

    public BuilderImpl() {
      instance = new ClientPrimitiveValueImpl();
    }

    @Override
    public BuilderImpl setType(final EdmType type) {
      EdmPrimitiveTypeKind primitiveTypeKind = null;
      if (type != null) {
        if (type.getKind() != EdmTypeKind.PRIMITIVE) {
          throw new IllegalArgumentException("Provided type %s is not primitive".formatted(type));
        }
        primitiveTypeKind = EdmPrimitiveTypeKind.valueOf(type.getName());
      }
      return setType(primitiveTypeKind);
    }

    @Override
    public BuilderImpl setType(final EdmPrimitiveTypeKind type) {
      if (type == EdmPrimitiveTypeKind.Stream) {
        throw new IllegalArgumentException(
                "Cannot build a primitive value for %s".formatted(EdmPrimitiveTypeKind.Stream));
      }
      if (type == EdmPrimitiveTypeKind.Geography || type == EdmPrimitiveTypeKind.Geometry) {
        throw new IllegalArgumentException(
                type + "is not an instantiable type. "
                        + "An entity can declare a property to be of type Geometry. "
                        + "An instance of an entity MUST NOT have a value of type Geometry. "
                        + "Each value MUST be of some subtype.");
      }

      instance.typeKind = type == null ? EdmPrimitiveTypeKind.String : type;
      instance.type = EdmPrimitiveTypeFactory.getInstance(instance.typeKind);

      return this;
    }

    @Override
    public BuilderImpl setValue(final Object value) {
      instance.value = value;
      return this;
    }

    @Override
    public ClientPrimitiveValue build() {
      if (instance.type == null) {
        setType(EdmPrimitiveTypeKind.String);
      }
      return instance;
    }

    @Override
    public ClientPrimitiveValue buildBoolean(final Boolean value) {
      return setType(EdmPrimitiveTypeKind.Boolean).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildInt16(final Short value) {
      return setType(EdmPrimitiveTypeKind.Int16).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildInt32(final Integer value) {
      return setType(EdmPrimitiveTypeKind.Int32).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildInt64(final Long value) {
      return setType(EdmPrimitiveTypeKind.Int64).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildSingle(final Float value) {
      return setType(EdmPrimitiveTypeKind.Single).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildDouble(final Double value) {
      return setType(EdmPrimitiveTypeKind.Double).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildString(final String value) {
      return setType(EdmPrimitiveTypeKind.String).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildGuid(final UUID value) {
      return setType(EdmPrimitiveTypeKind.Guid).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildBinary(final byte[] value) {
      return setType(EdmPrimitiveTypeKind.Binary).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildDecimal(BigDecimal value) {
      return setType(EdmPrimitiveTypeKind.Decimal).setValue(value).build();
    }

    @Override
    public ClientPrimitiveValue buildDuration(BigDecimal value) {
      return setType(EdmPrimitiveTypeKind.Duration).setValue(value).build();
    }

  }

  /**
   * Type kind.
   */
  private EdmPrimitiveTypeKind typeKind;

  /**
   * Type.
   */
  private EdmPrimitiveType type;

  /**
   * Actual value.
   */
  private Object value;

  protected ClientPrimitiveValueImpl() {
    super(null);
  }

  @Override
  public String getTypeName() {
    return typeKind.getFullQualifiedName().toString();
  }

  @Override
  public EdmPrimitiveTypeKind getTypeKind() {
    return typeKind;
  }

  @Override
  public EdmPrimitiveType getType() {
    return type;
  }

  @Override
  public Object toValue() {
    return value;
  }

  @Override
  public <T> T toCastValue(final Class<T> reference) throws EdmPrimitiveTypeException {
    if (value == null) {
      return null;
    } else if (typeKind.isGeospatial()) {
      return reference.cast(value);
    } else {
      // Facets use defaults since EDM property metadata is not available on ClientPrimitiveValue
      return type.valueOfString(type.valueToString(value,
                      null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null),
              null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null, reference);
    }
  }

  @Override
  public String toString() {
    if (value == null) {
      return "";
    } else if (typeKind.isGeospatial()) {
      return value.toString();
    } else {
      try {
        Integer precision = Constants.DEFAULT_PRECISION;
        Integer scale = Constants.DEFAULT_SCALE;
        if (typeKind.equals(EdmPrimitiveTypeKind.Decimal) && value instanceof BigDecimal bigDecimal) {
            precision = bigDecimal.precision();
            scale = bigDecimal.scale();
            if (precision == 0) {
              precision = null;
            } else if (scale > precision) {
              precision = scale;
            }
        }
        return type.valueToString(value, null, null, precision, scale, null);
      } catch (EdmPrimitiveTypeException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public ClientEnumValue asEnum() {
    return null;
  }

  @Override
  public boolean isComplex() {
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), type, typeKind, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof ClientPrimitiveValueImpl other)) {
      return false;
    }
    return Objects.equals(type, other.type)
        && typeKind == other.typeKind
        && Objects.equals(value, other.value);
  }

}
