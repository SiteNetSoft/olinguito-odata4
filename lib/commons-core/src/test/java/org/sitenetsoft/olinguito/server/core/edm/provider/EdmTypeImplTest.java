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
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.edm.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.core.edm.EdmTypeImpl;
import org.junit.jupiter.api.Test;

class EdmTypeImplTest {

  @Test
  void getterTest() {
    EdmType type = new EdmTypeImplTester(new FullQualifiedName("namespace", "name"), EdmTypeKind.PRIMITIVE);
    assertEquals("name", type.getName());
    assertEquals("namespace", type.getNamespace());
    assertEquals(EdmTypeKind.PRIMITIVE, type.getKind());
    EdmAnnotatable an = (EdmAnnotatable) type;
    assertNotNull(an.getAnnotations().get(0));
  }

  private class EdmTypeImplTester extends EdmTypeImpl {
    public EdmTypeImplTester(final FullQualifiedName name, final EdmTypeKind kind) {
      super(null, name, kind, new AnnoTester());
    }
  }

  private class AnnoTester implements CsdlAnnotatable {
    @Override
    public List<CsdlAnnotation> getAnnotations() {
      CsdlAnnotation annotation = new CsdlAnnotation();
      annotation.setTerm("NS.SimpleTerm");
      return List.of(annotation);
    }
  }
}
