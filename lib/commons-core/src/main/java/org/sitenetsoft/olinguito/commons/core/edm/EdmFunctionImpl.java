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
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;

public class EdmFunctionImpl extends AbstractEdmOperation implements EdmFunction {

  private final CsdlFunction function;

  public EdmFunctionImpl(final Edm edm, final FullQualifiedName name, final CsdlFunction function) {
    super(edm, name, function, EdmTypeKind.FUNCTION);
    this.function = function;
  }

  @Override
  public boolean isComposable() {
    return function.isComposable();
  }

  @Override
  public EdmReturnType getReturnType() {
    final EdmReturnType returnType = super.getReturnType();
    if (returnType == null) {
      throw new EdmException("ReturnType for a function must not be null");
    }
    return returnType;
  }

}
