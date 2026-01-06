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
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;

public class EdmTypeImpl extends AbstractEdmNamed implements EdmType {

  protected final FullQualifiedName typeName;
  protected final EdmTypeKind kind;

  public EdmTypeImpl(final Edm edm, final FullQualifiedName typeName, final EdmTypeKind kind,
      final CsdlAnnotatable annotatable) {
    super(edm, typeName.getName(), annotatable);
    this.typeName = typeName;
    this.kind = kind;
  }

  @Override
  public FullQualifiedName getFullQualifiedName() {
    return typeName;
  }

  @Override
  public String getNamespace() {
    return typeName.getNamespace();
  }

  @Override
  public EdmTypeKind getKind() {
    return kind;
  }
}
