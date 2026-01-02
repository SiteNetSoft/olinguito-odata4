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
package org.sitenetsoft.olinguito.server.core.edm.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.core.edm.EdmEntityContainerImpl;
import org.sitenetsoft.olinguito.commons.core.edm.EdmEntitySetImpl;
import org.sitenetsoft.olinguito.commons.core.edm.EdmProviderImpl;
import org.junit.Test;

public class EdmEntitySetImplTest {

  @Test
  public void entitySet() throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    EdmProviderImpl edm = new EdmProviderImpl(provider);

    final FullQualifiedName typeName = new FullQualifiedName("ns", "entityType");
    final CsdlEntityType entityTypeProvider = new CsdlEntityType()
        .setName(typeName.getName())
        .setKey(Arrays.asList(new CsdlPropertyRef().setName("Id")));
    when(provider.getEntityType(typeName)).thenReturn(entityTypeProvider);

    final FullQualifiedName containerName = new FullQualifiedName("ns", "container");
    final CsdlEntityContainerInfo containerInfo = new CsdlEntityContainerInfo().setContainerName(containerName);
    when(provider.getEntityContainerInfo(containerName)).thenReturn(containerInfo);
    final EdmEntityContainer entityContainer = new EdmEntityContainerImpl(edm, provider, containerInfo);

    final String entitySetName = "entitySet";
    final CsdlEntitySet entitySetProvider = new CsdlEntitySet()
        .setName(entitySetName)
        .setTitle("title")
        .setType(typeName)
        .setNavigationPropertyBindings(Arrays.asList(
            new CsdlNavigationPropertyBinding().setPath("path")
                .setTarget(containerName.getFullQualifiedNameAsString() + "/" + entitySetName)));
    when(provider.getEntitySet(containerName, entitySetName)).thenReturn(entitySetProvider);

    final EdmEntitySet entitySet = new EdmEntitySetImpl(edm, entityContainer, entitySetProvider);
    assertEquals(entitySetName, entityContainer.getEntitySet(entitySetName).getName());
    assertEquals(entitySetName, entitySet.getName());
    assertEquals("title", entitySet.getTitle());
    final EdmEntityType entityType = entitySet.getEntityType();
    assertEquals(typeName.getNamespace(), entityType.getNamespace());
    assertEquals(typeName.getName(), entityType.getName());
    assertEquals(entityContainer, entitySet.getEntityContainer());
    assertNull(entitySet.getRelatedBindingTarget(null));
    final EdmBindingTarget target = entitySet.getRelatedBindingTarget("path");
    assertEquals(entitySetName, target.getName());
    assertTrue(entitySet.isIncludeInServiceDocument());
  }

  @Test
  public void entitySetIncludeInServiceDocumentFalseAndInvalidType() throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    EdmProviderImpl edm = new EdmProviderImpl(provider);

    final FullQualifiedName containerName = new FullQualifiedName("ns", "container");
    final CsdlEntityContainerInfo containerInfo = new CsdlEntityContainerInfo().setContainerName(containerName);
    when(provider.getEntityContainerInfo(containerName)).thenReturn(containerInfo);
    final EdmEntityContainer entityContainer = new EdmEntityContainerImpl(edm, provider, containerInfo);

    final String entitySetName = "entitySet";
    final CsdlEntitySet entitySetProvider = new CsdlEntitySet()
        .setName(entitySetName)
        .setType("invalid.invalid")
        .setIncludeInServiceDocument(false);
    when(provider.getEntitySet(containerName, entitySetName)).thenReturn(entitySetProvider);

    final EdmEntitySet entitySet = new EdmEntitySetImpl(edm, entityContainer, entitySetProvider);
    assertFalse(entitySet.isIncludeInServiceDocument());

    try {
      entitySet.getEntityType();
      fail("Expected an EdmException");
    } catch (EdmException e) {
      assertEquals("Can´t find entity type: invalid.invalid for entity set or singleton: " + entitySetName, e
          .getMessage());
    }
  }
}
