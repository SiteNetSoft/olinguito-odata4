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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1289: detect cyclic base-type chains
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;

public class EdmComplexTypeImpl extends AbstractEdmStructuredType implements EdmComplexType {

  public EdmComplexTypeImpl(final Edm edm, final FullQualifiedName name, final CsdlComplexType complexType) {
    super(edm, name, EdmTypeKind.COMPLEX, complexType);
  }

  @Override
  protected EdmStructuredType buildBaseType(final FullQualifiedName baseTypeName) {
    EdmComplexType baseType = null;
    if (baseTypeName != null) {
      baseType = edm.getComplexType(baseTypeName);
      if (baseType == null) {
        throw new EdmException("Can't find base type with name: " + baseTypeName + " for complex type: "
            + getName());
      }
    }
    return baseType;
  }

  @Override
  public EdmComplexType getBaseType() {
    checkBaseType();
    return (EdmComplexType) baseType;
  }

  @Override
  protected void checkBaseType() {
    if (baseTypeName != null && baseType == null) {
      checkForCyclicBaseType();
      baseType = buildBaseType(baseTypeName);
    }
  }
}
