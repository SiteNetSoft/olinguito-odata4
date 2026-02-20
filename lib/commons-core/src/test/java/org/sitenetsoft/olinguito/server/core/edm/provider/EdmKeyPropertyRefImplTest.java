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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.edm.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmElement;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.core.edm.EdmKeyPropertyRefImpl;
import org.junit.jupiter.api.Test;

class EdmKeyPropertyRefImplTest {

  @Test
  void noAlias() {
    CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("Id");
    EdmEntityType etMock = mock(EdmEntityType.class);
    EdmProperty keyPropertyMock = mock(EdmProperty.class);
    when(etMock.getStructuralProperty("Id")).thenReturn(keyPropertyMock);
    EdmKeyPropertyRef ref = new EdmKeyPropertyRefImpl(etMock, providerRef);
    assertEquals("Id", ref.getName());
    assertNull(ref.getAlias());

    EdmProperty property = ref.getProperty();
    assertNotNull(property);
    assertTrue(property == keyPropertyMock);
    assertTrue(property == ref.getProperty());
  }

  @Test
  void aliasForPropertyInComplexPropertyOneLevel() {
    CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("comp/Id").setAlias("alias");
    EdmEntityType etMock = mock(EdmEntityType.class);
    EdmProperty keyPropertyMock = mock(EdmProperty.class);
    EdmProperty compMock = mock(EdmProperty.class);
    EdmComplexType compTypeMock = mock(EdmComplexType.class);
    when(compTypeMock.getStructuralProperty("Id")).thenReturn(keyPropertyMock);
    when(compMock.getType()).thenReturn(compTypeMock);
    when(etMock.getStructuralProperty("comp")).thenReturn(compMock);
    EdmKeyPropertyRef ref = new EdmKeyPropertyRefImpl(etMock, providerRef);
    assertEquals("alias", ref.getAlias());

    EdmProperty property = ref.getProperty();
    assertNotNull(property);
    assertTrue(property == keyPropertyMock);
  }

  @Test
  void aliasForPropertyInComplexPropertyButWrongPath() {
      assertThrows(EdmException.class, () -> {
          CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("comp/wrong").setAlias("alias");
          EdmEntityType etMock = mock(EdmEntityType.class);
          EdmProperty keyPropertyMock = mock(EdmProperty.class);
          EdmElement compMock = mock(EdmProperty.class);
          EdmComplexType compTypeMock = mock(EdmComplexType.class);
          when(compTypeMock.getProperty("Id")).thenReturn(keyPropertyMock);
          when(compMock.getType()).thenReturn(compTypeMock);
          when(etMock.getProperty("comp")).thenReturn(compMock);
          new EdmKeyPropertyRefImpl(etMock, providerRef).getProperty();
      });
  }

  @Test
  void aliasForPropertyInComplexPropertyButWrongPath2() {
      assertThrows(EdmException.class, () -> {
          CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("wrong/Id").setAlias("alias");
          EdmEntityType etMock = mock(EdmEntityType.class);
          EdmProperty keyPropertyMock = mock(EdmProperty.class);
          EdmElement compMock = mock(EdmProperty.class);
          EdmComplexType compTypeMock = mock(EdmComplexType.class);
          when(compTypeMock.getProperty("Id")).thenReturn(keyPropertyMock);
          when(compMock.getType()).thenReturn(compTypeMock);
          when(etMock.getProperty("comp")).thenReturn(compMock);
          new EdmKeyPropertyRefImpl(etMock, providerRef).getProperty();
      });
  }

  @Test
  void aliasForPropertyInComplexPropertyTwoLevels() {
    CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("comp/comp2/Id").setAlias("alias");
    EdmEntityType etMock = mock(EdmEntityType.class);
    EdmProperty keyPropertyMock = mock(EdmProperty.class);
    EdmProperty compMock = mock(EdmProperty.class);
    EdmComplexType compTypeMock = mock(EdmComplexType.class);
    EdmProperty comp2Mock = mock(EdmProperty.class);
    EdmComplexType comp2TypeMock = mock(EdmComplexType.class);
    when(comp2TypeMock.getStructuralProperty("Id")).thenReturn(keyPropertyMock);
    when(comp2Mock.getType()).thenReturn(comp2TypeMock);
    when(compTypeMock.getStructuralProperty("comp2")).thenReturn(comp2Mock);
    when(compMock.getType()).thenReturn(compTypeMock);
    when(etMock.getStructuralProperty("comp")).thenReturn(compMock);
    EdmKeyPropertyRef ref = new EdmKeyPropertyRefImpl(etMock, providerRef);

    EdmProperty property = ref.getProperty();
    assertNotNull(property);
    assertTrue(property == keyPropertyMock);
  }

  @Test
  void oneKeyNoAliasButInvalidProperty() {
      assertThrows(EdmException.class, () -> {
          CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("Id");
          EdmKeyPropertyRef ref = new EdmKeyPropertyRefImpl(mock(EdmEntityType.class), providerRef);
          ref.getProperty();
      });
  }

  @Test
  void aliasButNoPath() {
      assertThrows(EdmException.class, () -> {
          CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("Id").setAlias("alias");
          EdmKeyPropertyRef ref = new EdmKeyPropertyRefImpl(mock(EdmEntityType.class), providerRef);
          ref.getProperty();
      });
  }

  @Test
  void aliasButEmptyPath() {
      assertThrows(EdmException.class, () -> {
          CsdlPropertyRef providerRef = new CsdlPropertyRef().setName("").setAlias("alias");
          EdmKeyPropertyRef ref = new EdmKeyPropertyRefImpl(mock(EdmEntityType.class), providerRef);
          ref.getProperty();
      });
  }
}
