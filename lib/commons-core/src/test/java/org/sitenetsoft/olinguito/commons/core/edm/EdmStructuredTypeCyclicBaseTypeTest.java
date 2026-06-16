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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1289: cyclic base-type detection
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.junit.jupiter.api.Test;

/** OLINGO-1289: a cyclic BaseType chain must raise an EdmException, not a StackOverflowError. */
class EdmStructuredTypeCyclicBaseTypeTest {

  private static final FullQualifiedName FQN_A = new FullQualifiedName("ns", "A");
  private static final FullQualifiedName FQN_B = new FullQualifiedName("ns", "B");

  @Test
  void selfCyclicComplexBaseType() {
    final Edm edm = mock(Edm.class);
    final EdmComplexTypeImpl a =
        new EdmComplexTypeImpl(edm, FQN_A, new CsdlComplexType().setName("A").setBaseType(FQN_A));
    lenient().when(edm.getComplexType(FQN_A)).thenReturn(a);

    assertThrows(EdmException.class, a::getPropertyNames);
  }

  @Test
  void mutualCyclicComplexBaseType() {
    final Edm edm = mock(Edm.class);
    final EdmComplexTypeImpl a =
        new EdmComplexTypeImpl(edm, FQN_A, new CsdlComplexType().setName("A").setBaseType(FQN_B));
    final EdmComplexTypeImpl b =
        new EdmComplexTypeImpl(edm, FQN_B, new CsdlComplexType().setName("B").setBaseType(FQN_A));
    lenient().when(edm.getComplexType(FQN_A)).thenReturn(a);
    lenient().when(edm.getComplexType(FQN_B)).thenReturn(b);

    assertThrows(EdmException.class, a::getPropertyNames);
  }

  @Test
  void mutualCyclicEntityBaseType() {
    final Edm edm = mock(Edm.class);
    final EdmEntityTypeImpl a =
        new EdmEntityTypeImpl(edm, FQN_A, new CsdlEntityType().setName("A").setBaseType(FQN_B));
    final EdmEntityTypeImpl b =
        new EdmEntityTypeImpl(edm, FQN_B, new CsdlEntityType().setName("B").setBaseType(FQN_A));
    lenient().when(edm.getEntityType(FQN_A)).thenReturn(a);
    lenient().when(edm.getEntityType(FQN_B)).thenReturn(b);

    assertThrows(EdmException.class, a::getBaseType);
  }

  @Test
  void nonCyclicComplexBaseTypeStillResolves() {
    final Edm edm = mock(Edm.class);
    final EdmComplexTypeImpl a =
        new EdmComplexTypeImpl(edm, FQN_A, new CsdlComplexType().setName("A").setBaseType(FQN_B));
    final EdmComplexTypeImpl b =
        new EdmComplexTypeImpl(edm, FQN_B, new CsdlComplexType().setName("B"));
    lenient().when(edm.getComplexType(FQN_A)).thenReturn(a);
    lenient().when(edm.getComplexType(FQN_B)).thenReturn(b);

    assertEquals(b, a.getBaseType());
    assertNotNull(a.getPropertyNames());
  }
}
