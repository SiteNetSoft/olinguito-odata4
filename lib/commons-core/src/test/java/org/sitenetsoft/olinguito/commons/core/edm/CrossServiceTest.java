package org.sitenetsoft.olinguito.commons.core.edm;
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
 
 import java.util.Collections;
 import java.util.List;
 
 import org.sitenetsoft.olinguito.commons.api.edm.Edm;
 import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
 import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
 import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmProvider;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAliasInfo;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
 import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
 import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
 import org.junit.jupiter.api.Assertions;
 import org.junit.jupiter.api.Test;
 
 public class CrossServiceTest {
 
   private final CsdlEdmProvider provider1 = new CsdlProvider("One");
   private final CsdlEdmProvider provider2 = new CsdlProvider("Two");
   private final Edm edm1 = new EdmProviderImpl(provider1);
   private final Edm edm2 = new EdmProviderImpl(provider2);
 
   private class CsdlProvider extends CsdlAbstractEdmProvider {
 
     private static final String NAMESPACE_PREFIX = "Namespace.";
     private static final String ALIAS_PREFIX = "Alias";
     private static final String CONTAINER_PREFIX = "Container";
     private static final String ENTITY_SET_PREFIX = "EntitySet";
     private static final String ENTITY_TYPE_PREFIX = "EntityType";
     private final String name;
     private final String namespace;
     private final String other;
     private final CsdlEntitySet entitySet;
     private final CsdlEntityType entityType;
 
     private CsdlProvider(final String name) {
       this.name = name;
    namespace = NAMESPACE_PREFIX + name;
       other = name.equals("One") ? "Two" : "One";
       entitySet = new CsdlEntitySet().setName(ENTITY_SET_PREFIX + name)
           .setType(new FullQualifiedName(namespace, ENTITY_TYPE_PREFIX + name));
       entityType = new CsdlEntityType().setName(ENTITY_TYPE_PREFIX + name)
           .setKey(Collections.singletonList(new CsdlPropertyRef().setName("ID" + name)))
           .setProperties(Collections.singletonList(
               new CsdlProperty().setName("ID" + name).setNullable(false).setType("Edm.Guid")))
           .setNavigationProperties(Collections.singletonList(
               new CsdlNavigationProperty().setName("Navigation" + other)
                   .setType(new FullQualifiedName(ALIAS_PREFIX + other, ENTITY_TYPE_PREFIX + other))));
     }
 
     @Override
     public List<CsdlAliasInfo> getAliasInfos() throws ODataException {
       return List.of(
           new CsdlAliasInfo().setNamespace(NAMESPACE_PREFIX + name).setAlias(ALIAS_PREFIX + name),
           new CsdlAliasInfo().setNamespace(NAMESPACE_PREFIX + other).setAlias(ALIAS_PREFIX + other));
     }
 
     @Override
     public List<CsdlSchema> getSchemas() throws ODataException {
       return Collections.singletonList(
           new CsdlSchema().setNamespace(namespace).setAlias(ALIAS_PREFIX + name)
               .setEntityContainer(getEntityContainer())
               .setEntityTypes(Collections.singletonList(entityType)));
     }
 
     @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName)
        throws ODataException {
      if (entityContainerName == null) {
        return new CsdlEntityContainerInfo().setContainerName(
            new FullQualifiedName(namespace, CONTAINER_PREFIX + name));
      } else if (namespace.equals(entityContainerName.getNamespace())) {
        if ((CONTAINER_PREFIX + name).equals(entityContainerName.getName())) {
          return new CsdlEntityContainerInfo().setContainerName(entityContainerName);
        }
        return null;
      } else if ((NAMESPACE_PREFIX + other).equals(entityContainerName.getNamespace())) {
        final CsdlEdmProvider otherProvider = this == provider1 ? provider2 : provider1;
        return otherProvider.getEntityContainerInfo(entityContainerName);
      }
      return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
      return new CsdlEntityContainer().setName(CONTAINER_PREFIX + name)
          .setEntitySets(Collections.singletonList(entitySet));
    }

    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
        throws ODataException {
      if (namespace.equals(entityContainer.getNamespace())) {
        if ((CONTAINER_PREFIX + name).equals(entityContainer.getName())
            && (ENTITY_SET_PREFIX + name).equals(entitySetName)) {
          return entitySet;
        }
        return null;
      } else if ((NAMESPACE_PREFIX + other).equals(entityContainer.getNamespace())) {
        final CsdlEdmProvider otherProvider = this == provider1 ? provider2 : provider1;
        return otherProvider.getEntitySet(entityContainer, entitySetName);
      }
      return null;
    }

    @Override
    public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {
      if (namespace.equals(entityTypeName.getNamespace())) {
        if ((ENTITY_TYPE_PREFIX + name).equals(entityTypeName.getName())) {
          return entityType;
        }
        return null;
      } else if ((NAMESPACE_PREFIX + other).equals(entityTypeName.getNamespace())) {
        // Requesting a type in a foreign namespace delegates the request to another CSDL provider.
        final CsdlEdmProvider otherProvider = this == provider1 ? provider2 : provider1;
        return otherProvider.getEntityType(entityTypeName);
      }
      return null;
    }
  }

  @Test
  void entityType() throws Exception {
    final FullQualifiedName typeName = new FullQualifiedName("Namespace.One", "EntityTypeOne");
    final EdmEntityType entityType = edm1.getEntityType(typeName);
    Assertions.assertNotNull(entityType);
    Assertions.assertNotNull(entityType.getNavigationProperty("NavigationTwo"));

    // We get an entity type in a foreign namespace if it is used in our namespace.
    final EdmEntityType targetType = entityType.getNavigationProperty("NavigationTwo").getType();
    Assertions.assertNotNull(targetType);
    final FullQualifiedName targetName = new FullQualifiedName("Namespace.Two", "EntityTypeTwo");
    Assertions.assertEquals(targetName, targetType.getFullQualifiedName());

    // Directly accessing the foreign type is also possible.
    Assertions.assertNotNull(edm1.getEntityType(targetName));
    Assertions.assertEquals(targetType, edm1.getEntityType(targetName));

    // However, the schema contains only elements from the own namespace.
    final List<EdmEntityType> entityTypes = edm1.getSchema("Namespace.One").getEntityTypes();
    Assertions.assertNotNull(entityTypes);
    Assertions.assertEquals(1, entityTypes.size());
    Assertions.assertEquals(typeName, entityTypes.get(0).getFullQualifiedName());

    // The foreign service has the foreign type available, both directly and in its schema.
    Assertions.assertNotNull(edm2.getEntityType(targetName));
    Assertions.assertEquals(targetName, edm2.getEntityType(targetName).getFullQualifiedName());
    Assertions.assertEquals(targetName,
        edm2.getSchema("AliasTwo").getEntityTypes().get(0).getFullQualifiedName());

    // Alias access is also supported.
    Assertions.assertNotNull(edm1.getEntityType(new FullQualifiedName("AliasOne", "EntityTypeOne")));

    // A wrong name leads to null result.
    Assertions.assertNull(edm1.getEntityType(new FullQualifiedName("AliasOne", "EntityTypeWrong")));
    Assertions.assertNull(edm1.getEntityType(new FullQualifiedName("AliasTwo", "EntityTypeWrong")));
    Assertions.assertNull(edm1.getEntityType(new FullQualifiedName("AliasWrong", "EntityTypeOne")));
  }

  @Test
  void entityContainer() throws Exception {
    Assertions.assertNotNull(edm1.getEntityContainer());
    Assertions.assertNotNull(edm1.getEntityContainer(new FullQualifiedName("Namespace.One", "ContainerOne")));
    Assertions.assertEquals(edm1.getEntityContainer(),
        edm1.getEntityContainer(new FullQualifiedName("Namespace.One", "ContainerOne")));
    Assertions.assertEquals(edm1.getEntityContainer(),
        edm1.getEntityContainer(new FullQualifiedName("AliasOne", "ContainerOne")));
    Assertions.assertNotNull(edm1.getEntityContainer(new FullQualifiedName("AliasTwo", "ContainerTwo")));

    // A wrong name leads to null result.
    Assertions.assertNull(edm1.getEntityContainer(new FullQualifiedName("AliasTwo", "ContainerOne")));
    Assertions.assertNull(edm1.getEntityContainer(new FullQualifiedName("AliasWrong", "ContainerOne")));
  }

  @Test
  void entitySet() throws Exception {
    final EdmEntitySet entitySet = edm1.getEntityContainer(new FullQualifiedName("AliasTwo", "ContainerTwo"))
        .getEntitySet("EntitySetTwo");
    Assertions.assertNotNull(entitySet);
    Assertions.assertEquals("EntitySetTwo", entitySet.getName());
    Assertions.assertNotNull(entitySet.getEntityType());
    Assertions.assertEquals("EntityTypeTwo", entitySet.getEntityType().getName());

    // A wrong name leads to null result.
    Assertions.assertNull(edm1.getEntityContainer().getEntitySet("EntitySetTwo"));
    Assertions.assertNull(
        edm1.getEntityContainer(new FullQualifiedName("AliasTwo", "ContainerTwo")).getEntitySet("EntitySetOne"));
  }

  @Test
  void schema() throws Exception {
    Assertions.assertNotNull(edm1.getSchemas());
    Assertions.assertEquals(1, edm1.getSchemas().size());
    Assertions.assertEquals("AliasOne", edm1.getSchemas().get(0).getAlias());
    Assertions.assertNotNull(edm1.getSchemas().get(0).getEntityTypes());
    Assertions.assertEquals(1, edm1.getSchemas().get(0).getEntityTypes().size());
    Assertions.assertEquals(new FullQualifiedName("Namespace.One", "EntityTypeOne"),
        edm1.getSchemas().get(0).getEntityTypes().get(0).getFullQualifiedName());
  }

  @Test
  void entitySets() throws Exception {
    Assertions.assertNotNull(edm1.getEntityContainer());
    Assertions.assertNotNull(edm1.getEntityContainer().getEntitySets());
    Assertions.assertEquals(1, edm1.getEntityContainer().getEntitySets().size());
    Assertions.assertEquals("EntitySetOne", edm1.getEntityContainer().getEntitySets().get(0).getName());
  }
}
