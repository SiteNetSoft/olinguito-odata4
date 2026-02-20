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
 * Copyright 2026 SiteNetSoft - Replaced bare RuntimeException with ODataRuntimeException
 */
package org.sitenetsoft.olinguito.client.core.metadatavalidator;

import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;

public class EdmTypeValidator {
  
  private final Map<String, String> aliasNamespaceMap;
  private final Map<FullQualifiedName, EdmEntityContainer> edmContainersMap;
  private final Map<FullQualifiedName, EdmEntityType> edmEntityTypesMap;
  private final Map<FullQualifiedName, EdmComplexType> edmComplexTypesMap;
  private final Map<FullQualifiedName, EdmFunction> edmFunctionsMap;

  /**
   * Constructs an EdmTypeValidator with the given EDM metadata maps.
   *
   * @param aliasNamespaceMap mapping of namespace aliases to full namespaces
   * @param edmContainersMap mapping of qualified names to entity containers
   * @param edmEntityTypesMap mapping of qualified names to entity types
   * @param edmComplexTypesMap mapping of qualified names to complex types
   * @param edmFunctionsMap mapping of qualified names to functions
   */
  public EdmTypeValidator(Map<String, String> aliasNamespaceMap,
      Map<FullQualifiedName, EdmEntityContainer> edmContainersMap,
      Map<FullQualifiedName, EdmEntityType> edmEntityTypesMap,
      Map<FullQualifiedName, EdmComplexType> edmComplexTypesMap,
      Map<FullQualifiedName, EdmFunction> edmFunctionsMap) {
    this.aliasNamespaceMap = aliasNamespaceMap;
    this.edmContainersMap = edmContainersMap;
    this.edmEntityTypesMap = edmEntityTypesMap;
    this.edmComplexTypesMap = edmComplexTypesMap;
    this.edmFunctionsMap = edmFunctionsMap;
  }
  
  /**
   * Validates Edm
   */
  public void validateEdm() {
    validateEdmEntityTypes();
    validateEdmEntitySet();
    validateEdmFunctionImport();
  }
  
  /**
   * This method validates Edm Entity types.
   * Looks for correct namespace aliases and correct base types
   */
  private void validateEdmEntityTypes() {
    for (Map.Entry<FullQualifiedName, EdmEntityType> entityTypes : edmEntityTypesMap.entrySet()) {
      if (entityTypes.getValue() != null && entityTypes.getKey() != null) {
        EdmEntityType entityType = entityTypes.getValue();
        if (entityType.getBaseType() != null) {
          FullQualifiedName baseTypeFQName = entityType.getBaseType().getFullQualifiedName();
          EdmEntityType baseEntityType = edmEntityTypesMap.get(baseTypeFQName);
          
          if (baseEntityType != null && baseEntityType.getKeyPredicateNames().isEmpty()) {
            throw new ODataRuntimeException("Missing key for EntityType " + baseEntityType.getName());
          }
        } else if (entityType.getKeyPredicateNames().isEmpty()) {
          throw new ODataRuntimeException("Missing key for EntityType " + entityType.getName());
        }
      }
    }

  }
  
  /**
   * This method validates Edm entity sets.
   * It checks if entity sets are part of correct container and 
   * entity types defined for entity sets are correct and
   * validates navigation property bindings 
   */
  private void validateEdmEntitySet() {
    for (Map.Entry<FullQualifiedName, EdmEntityContainer> container : edmContainersMap.entrySet()) {
      for (EdmEntitySet entitySet : container.getValue().getEntitySets()) {
        validateNavigationBindingPaths(entitySet);
      }
    }
  }

  /**
   * This method checks if the target entity of the navigation binding path is defined.
   * It checks if the type of navigation property of the source entity and target entity is the same.
   *
   * @param entitySet the entity set whose navigation bindings to validate
   */
  private void validateNavigationBindingPaths(EdmEntitySet entitySet) {
    List<EdmNavigationPropertyBinding> navigationPropertyBindings = entitySet.getNavigationPropertyBindings();
    if (!navigationPropertyBindings.isEmpty()) {
      for (EdmNavigationPropertyBinding navigationPropertyBinding : navigationPropertyBindings) {
        String navBindingPath = navigationPropertyBinding.getPath();
        EdmBindingTarget edmBindingTarget = entitySet.getRelatedBindingTarget(navBindingPath);
        EdmEntityType sourceEntityType = edmEntityTypesMap.get(entitySet.getEntityType().getFullQualifiedName());
        
        if (edmBindingTarget instanceof EdmSingleton) {
          throw new ODataRuntimeException("Validations of Singletons are not supported: " + edmBindingTarget.getName());
        }
        
        EdmEntityType targetEntityType = edmBindingTarget.getEntityType();
        EdmNavigationProperty navProperty;
        if (navBindingPath.contains("/")) {
          navProperty = findLastQualifiedNameHavingNavigationProperty(navBindingPath, sourceEntityType);
        } else {
          navProperty = (EdmNavigationProperty) sourceEntityType.getProperty(navBindingPath);
        }
        FullQualifiedName navFQName = fetchCorrectNamespaceFromAlias(navProperty.getType().getFullQualifiedName());
        validateReferentialConstraint(sourceEntityType, targetEntityType, navProperty);
        String targetType = targetEntityType.getFullQualifiedName().getFullQualifiedNameAsString();
        if (!(navFQName.getFullQualifiedNameAsString().equals(targetType)) 
            && !(navProperty.getType().compatibleTo(targetEntityType))) {
          throw new ODataRuntimeException("Navigation Property Type " +  
            navProperty.getType().getFullQualifiedName() +" does not match "
                + "the binding target type " + targetType);
        }
      }
    }
  }

  /**
   * Validates referential constraints between source and target entity types.
   *
   * @param sourceEntityType the source entity type
   * @param targetEntityType the target entity type
   * @param navProperty the navigation property containing the referential constraints
   */
  private void validateReferentialConstraint(EdmEntityType sourceEntityType, EdmEntityType targetEntityType,
      EdmNavigationProperty navProperty) {
    if (!navProperty.getReferentialConstraints().isEmpty()) {
      String propertyName = navProperty.getReferentialConstraints().get(0).getPropertyName();
      if (sourceEntityType.getProperty(propertyName) == null) {
        throw new ODataRuntimeException("Property name "+ propertyName + " not part of the source entity.");
      }
      String referencedPropertyName = navProperty.getReferentialConstraints().get(0).getReferencedPropertyName();
      if (targetEntityType.getProperty(referencedPropertyName) == null) {
        throw new ODataRuntimeException("Property name " + referencedPropertyName + " not part of the target entity.");
      }
    }
  }
  
  /**
   * This looks for the last fully qualified identifier to fetch the navigation property
   * e.g. if navigation property path is Microsoft.Exchange.Services.OData.Model.ItemAttachment/Item
   * then it fetches the entity ItemAttachment and fetches the navigation property Item
   * if navigation property path is EntityType/ComplexType/OData.Model.DerivedComplexType/Item
   * then it fetches the complex type DerivedComplexType and fetches the navigation property Item
   * @param navBindingPath the navigation binding path containing qualified type segments
   * @param sourceEntityType the source entity type to start resolution from
   * @return the resolved navigation property
   */
  private EdmNavigationProperty findLastQualifiedNameHavingNavigationProperty(String navBindingPath, 
      EdmEntityType sourceEntityType) {
    String[] paths = navBindingPath.split("/");
    String lastFullQualifiedName = "";
    for (String path : paths) {
      if (path.contains(".")) {
        lastFullQualifiedName = path;
      }
    }
    String strNavProperty = paths[paths.length - 1];
    String remainingPath = navBindingPath.substring(navBindingPath.indexOf(lastFullQualifiedName) 
        + lastFullQualifiedName.length() + (lastFullQualifiedName.isEmpty() ? 0 : 1),
        navBindingPath.lastIndexOf(strNavProperty));
    if (!remainingPath.isEmpty()) {
      remainingPath = remainingPath.substring(0, remainingPath.length() - 1);
    }
    EdmNavigationProperty navProperty;
    EdmEntityType sourceEntityTypeHavingNavProp = lastFullQualifiedName.isEmpty() ? sourceEntityType :
      (edmEntityTypesMap.containsKey(new FullQualifiedName(lastFullQualifiedName)) ? 
        edmEntityTypesMap.get(new FullQualifiedName(lastFullQualifiedName)) : 
          edmEntityTypesMap.get(fetchCorrectNamespaceFromAlias(new FullQualifiedName(lastFullQualifiedName))));
    if (sourceEntityTypeHavingNavProp == null) {
      EdmComplexType sourceComplexTypeHavingNavProp = 
          edmComplexTypesMap.containsKey(new FullQualifiedName(lastFullQualifiedName)) ?
          edmComplexTypesMap.get(new FullQualifiedName(lastFullQualifiedName)) : 
            edmComplexTypesMap.get(fetchCorrectNamespaceFromAlias(new FullQualifiedName(lastFullQualifiedName)));
      if (sourceComplexTypeHavingNavProp == null) {
        throw new ODataRuntimeException("The fully Qualified type " + lastFullQualifiedName + 
            " mentioned in navigation binding path not found ");
      }
      navProperty = !remainingPath.isEmpty() ? fetchNavigationProperty(remainingPath, strNavProperty,
          sourceComplexTypeHavingNavProp) : sourceComplexTypeHavingNavProp.getNavigationProperty(strNavProperty);
    } else {
      navProperty = !remainingPath.isEmpty() ? fetchNavigationProperty(remainingPath, strNavProperty,
          sourceEntityTypeHavingNavProp) : sourceEntityTypeHavingNavProp.getNavigationProperty(strNavProperty);
    }
    return navProperty;
  }
  
  /**
   * Fetch the correct navigation property from the remaining path
   * @param remainingPath the remaining path segments to traverse
   * @param strNavProperty the navigation property name at the end of the path
   * @param sourceTypeHavingNavProp the structured type to start traversal from
   * @return the resolved navigation property
   */
  private EdmNavigationProperty fetchNavigationProperty(String remainingPath,
      String strNavProperty, EdmStructuredType sourceTypeHavingNavProp) {
    String[] paths = remainingPath.split("/");
    for (String path : paths) {
      FullQualifiedName fqName = null;
      if (sourceTypeHavingNavProp instanceof EdmComplexType) {
        fqName = sourceTypeHavingNavProp.getProperty(path).getType().getFullQualifiedName();
      } else if (sourceTypeHavingNavProp instanceof EdmEntityType) {
        fqName = sourceTypeHavingNavProp.getProperty(path).getType().getFullQualifiedName();
      }
      if (fqName != null) {
        String namespace = aliasNamespaceMap.get(fqName.getNamespace());
        fqName = namespace != null ? new FullQualifiedName(namespace, fqName.getName()) : fqName;
      }
      
      sourceTypeHavingNavProp = edmEntityTypesMap.containsKey(fqName) ? 
          edmEntityTypesMap.get(fqName) : 
            edmComplexTypesMap.get(fqName);
    }
    return sourceTypeHavingNavProp.getNavigationProperty(strNavProperty);
  }

  /**
   * This validates the namespace to alias mapping.
   *
   * @param fqName the fully qualified name that may use an alias as namespace
   * @return the resolved fully qualified name with the actual namespace
   */
  private FullQualifiedName fetchCorrectNamespaceFromAlias(FullQualifiedName fqName) {
    if (aliasNamespaceMap.containsKey(fqName.getNamespace())) {
      String namespace = aliasNamespaceMap.get(fqName.getNamespace());
      fqName = new FullQualifiedName(namespace, fqName.getName());
    }
    return fqName;
  }

  /**
   * These methods validate edm function imports.
   * It checks if function imports are part of correct container and
   * functions defined for function imports are correct
   */
  private void validateEdmFunctionImport() {
    for (Map.Entry<FullQualifiedName, EdmEntityContainer> container : edmContainersMap.entrySet()) {
      for (EdmFunctionImport functionImport : container.getValue().getFunctionImports()) {
        FullQualifiedName fqFunction = functionImport.getFunctionFqn();
        if (!(edmFunctionsMap.containsKey(fqFunction))) {
          validateEdmFunctionsWithAlias(fqFunction);
        }
      }
    }
  }

  /**
   * Validates that a function referenced by alias exists in the EDM functions map.
   *
   * @param aliasName the fully qualified name using an alias as namespace
   * @return the resolved fully qualified name with the actual namespace
   */
  private FullQualifiedName validateEdmFunctionsWithAlias(FullQualifiedName aliasName) {
    String namespace = aliasNamespaceMap.get(aliasName.getNamespace());
    FullQualifiedName fqName = new FullQualifiedName(namespace, aliasName.getName());
    if (!edmFunctionsMap.containsKey(fqName)) {
      throw new ODataRuntimeException("Invalid Function " + aliasName);
    }
    return fqName;
  }
}
