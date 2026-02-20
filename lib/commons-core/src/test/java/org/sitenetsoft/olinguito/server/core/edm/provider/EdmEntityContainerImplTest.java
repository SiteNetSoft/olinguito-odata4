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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;
import org.sitenetsoft.olinguito.commons.core.edm.EdmEntityContainerImpl;
import org.sitenetsoft.olinguito.commons.core.edm.EdmProviderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EdmEntityContainerImplTest {

  EdmEntityContainer container;

  @BeforeEach
  void setup() {
    CsdlEdmProvider provider = new CustomProvider();
    EdmProviderImpl edm = new EdmProviderImpl(provider);
    CsdlEntityContainerInfo entityContainerInfo =
        new CsdlEntityContainerInfo().setContainerName(new FullQualifiedName("space", "name"));
    container = new EdmEntityContainerImpl(edm, provider, entityContainerInfo);
  }

  @Test
  void getAllEntitySetInitial() {
    List<EdmEntitySet> entitySets = container.getEntitySets();
    assertNotNull(entitySets);
    assertEquals(2, entitySets.size());
  }

  @Test
  void getAllEntitySetsAfterOneWasAlreadyLoaded() {
    container.getEntitySet("entitySetName");
    List<EdmEntitySet> entitySets = container.getEntitySets();
    assertNotNull(entitySets);
    assertEquals(2, entitySets.size());
  }

  @Test
  void getAllSingletonsInitial() {
    List<EdmSingleton> singletons = container.getSingletons();
    assertNotNull(singletons);
    assertEquals(2, singletons.size());
  }

  @Test
  void getAllSingletonsAfterOneWasAlreadyLoaded() {
    container.getSingleton("singletonName");
    List<EdmSingleton> singletons = container.getSingletons();
    assertNotNull(singletons);
    assertEquals(2, singletons.size());
  }

  @Test
  void getAllActionImportsInitial() {
    List<EdmActionImport> actionImports = container.getActionImports();
    assertNotNull(actionImports);
    assertEquals(2, actionImports.size());
  }

  @Test
  void getAllActionImportsAfterOneWasAlreadyLoaded() {
    container.getActionImport("actionImportName");
    List<EdmActionImport> actionImports = container.getActionImports();
    assertNotNull(actionImports);
    assertEquals(2, actionImports.size());
  }

  @Test
  void getAllFunctionImportsInitial() {
    List<EdmFunctionImport> functionImports = container.getFunctionImports();
    assertNotNull(functionImports);
    assertEquals(2, functionImports.size());
  }

  @Test
  void getAllFunctionImportsAfterOneWasAlreadyLoaded() {
    container.getFunctionImport("functionImportName");
    List<EdmFunctionImport> functionImports = container.getFunctionImports();
    assertNotNull(functionImports);
    assertEquals(2, functionImports.size());
  }

  @Test
  void checkEdmExceptionConversion() throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    FullQualifiedName containerName = new FullQualifiedName("space", "name");
    when(provider.getEntitySet(containerName, null)).thenThrow(new ODataException("msg"));
    when(provider.getSingleton(containerName, null)).thenThrow(new ODataException("msg"));
    when(provider.getFunctionImport(containerName, null)).thenThrow(new ODataException("msg"));
    when(provider.getActionImport(containerName, null)).thenThrow(new ODataException("msg"));
    EdmProviderImpl edm = new EdmProviderImpl(provider);
    CsdlEntityContainerInfo entityContainerInfo =
        new CsdlEntityContainerInfo().setContainerName(containerName);
    EdmEntityContainer container = new EdmEntityContainerImpl(edm, provider, entityContainerInfo);
    assertThrows(EdmException.class, () -> container.getEntitySet(null));
    assertThrows(EdmException.class, () -> container.getSingleton(null));
    assertThrows(EdmException.class, () -> container.getActionImport(null));
    assertThrows(EdmException.class, () -> container.getFunctionImport(null));
  }

  @Test
  void simpleContainerGetter() {
    assertEquals("name", container.getName());
    assertEquals("space", container.getNamespace());
    assertEquals(new FullQualifiedName("space.name"), container.getFullQualifiedName());
  }

  @Test
  void getExistingFunctionImport() {
    EdmFunctionImport functionImport = container.getFunctionImport("functionImportName");
    assertNotNull(functionImport);
    assertEquals("functionImportName", functionImport.getName());
    // Caching
    assertTrue(functionImport == container.getFunctionImport("functionImportName"));
  }

  @Test
  void getNonExistingFunctionImport() {
    assertNull(container.getFunctionImport(null));
  }

  @Test
  void getExistingActionImport() {
    EdmActionImport actionImport = container.getActionImport("actionImportName");
    assertNotNull(actionImport);
    assertEquals("actionImportName", actionImport.getName());
    // Caching
    assertTrue(actionImport == container.getActionImport("actionImportName"));
  }

  @Test
  void getNonExistingActionImport() {
    assertNull(container.getActionImport(null));
  }

  @Test
  void getExistingSingleton() {
    EdmSingleton singleton = container.getSingleton("singletonName");
    assertNotNull(singleton);
    assertEquals("singletonName", singleton.getName());
    // Caching
    assertTrue(singleton == container.getSingleton("singletonName"));
  }

  @Test
  void getNonExistingSingleton() {
    assertNull(container.getSingleton(null));
  }

  @Test
  void getExistingEntitySet() {
    EdmEntitySet entitySet = container.getEntitySet("entitySetName");
    assertNotNull(entitySet);
    assertEquals("entitySetName", entitySet.getName());
    // Caching
    assertTrue(entitySet == container.getEntitySet("entitySetName"));
  }

  @Test
  void getNonExistingEntitySet() {
    assertNull(container.getEntitySet(null));
  }

  private class CustomProvider extends CsdlAbstractEdmProvider {
    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
        throws ODataException {
      if (entitySetName != null) {
        return new CsdlEntitySet().setName("entitySetName");
      }
      return null;
    }

    @Override
    public CsdlSingleton getSingleton(final FullQualifiedName entityContainer, final String singletonName)
        throws ODataException {
      if (singletonName != null) {
        return new CsdlSingleton().setName("singletonName");
      }
      return null;
    }

    @Override
    public CsdlActionImport getActionImport(final FullQualifiedName entityContainer, final String actionImportName)
        throws ODataException {
      if (actionImportName != null) {
        return new CsdlActionImport().setName("actionImportName");
      }
      return null;
    }

    @Override
    public CsdlFunctionImport getFunctionImport(final FullQualifiedName entityContainer,
        final String functionImportName)
        throws ODataException {
      if (functionImportName != null) {
        return new CsdlFunctionImport().setName("functionImportName");
      }
      return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
      CsdlEntityContainer container = new CsdlEntityContainer();
      List<CsdlEntitySet> entitySets = new ArrayList<>();
      entitySets.add(new CsdlEntitySet().setName("entitySetName"));
      entitySets.add(new CsdlEntitySet().setName("entitySetName2"));
      container.setEntitySets(entitySets);

      List<CsdlSingleton> singletons = new ArrayList<>();
      singletons.add(new CsdlSingleton().setName("singletonName"));
      singletons.add(new CsdlSingleton().setName("singletonName2"));
      container.setSingletons(singletons);

      List<CsdlActionImport> actionImports = new ArrayList<>();
      actionImports.add(new CsdlActionImport().setName("actionImportName"));
      actionImports.add(new CsdlActionImport().setName("actionImportName2"));
      container.setActionImports(actionImports);

      List<CsdlFunctionImport> functionImports = new ArrayList<>();
      functionImports.add(new CsdlFunctionImport().setName("functionImportName"));
      functionImports.add(new CsdlFunctionImport().setName("functionImportName2"));
      container.setFunctionImports(functionImports);

      return container;
    }
  }
}
