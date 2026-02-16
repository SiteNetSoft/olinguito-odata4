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
 * Copyright 2026 SiteNetSoft - Thread-safe EDM caches using ConcurrentHashMap
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSchema;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTerm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;

public abstract class AbstractEdm implements Edm {

  /** Sentinel key used in place of null for ConcurrentHashMap (which forbids null keys). */
  private static final FullQualifiedName NULL_CONTAINER_KEY =
      new FullQualifiedName("org.sitenetsoft.olinguito.internal", "__NULL_CONTAINER__");

  protected volatile Map<String, EdmSchema> schemas;
  protected volatile List<EdmSchema> schemaList;
  private volatile boolean isEntityDerivedFromES;
  private volatile boolean isComplexDerivedFromES;
  private volatile boolean isPreviousES;

  private final Map<FullQualifiedName, EdmEntityContainer> entityContainers =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmEnumType> enumTypes =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmTypeDefinition> typeDefinitions =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmEntityType> entityTypes =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmComplexType> complexTypes =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmAction> unboundActions =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, List<EdmFunction>> unboundFunctionsByName =
      new ConcurrentHashMap<>();

  private final Map<FunctionMapKey, EdmFunction> unboundFunctionsByKey =
      new ConcurrentHashMap<>();

  private final Map<ActionMapKey, EdmAction> boundActions =
      new ConcurrentHashMap<>();

  private final Map<FunctionMapKey, EdmFunction> boundFunctions =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmTerm> terms =
      new ConcurrentHashMap<>();

  private final Map<TargetQualifierMapKey, EdmAnnotations> annotationGroups =
      new ConcurrentHashMap<>();

  private volatile Map<String, String> aliasToNamespaceInfo = null;

  private final Map<FullQualifiedName, EdmEntityType> entityTypesWithAnnotations =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmEntityType> entityTypesDerivedFromES =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmComplexType> complexTypesWithAnnotations =
      new ConcurrentHashMap<>();

  private final Map<FullQualifiedName, EdmComplexType> complexTypesDerivedFromES =
      new ConcurrentHashMap<>();

  private final Map<String, List<CsdlAnnotation>> annotationMap =
      new ConcurrentHashMap<>();

  @Override
  public List<EdmSchema> getSchemas() {
    if (schemaList == null) {
      initSchemas();
    }
    return schemaList;
  }

  @Override
  public EdmSchema getSchema(final String namespace) {
    if (schemas == null) {
      initSchemas();
    }

    EdmSchema schema = schemas.get(namespace);
    if (schema == null) {
      schema = schemas.get(aliasToNamespaceInfo.get(namespace));
    }
    return schema;
  }

  private void initSchemas() {
    loadAliasToNamespaceInfo();
    Map<String, EdmSchema> localSchemas = createSchemas();
    // Build the list from the original ordered map before copying to ConcurrentHashMap
    // (ConcurrentHashMap does not preserve insertion order)
    List<EdmSchema> orderedValues = new ArrayList<>();
    ConcurrentHashMap<String, EdmSchema> safeSchemas = new ConcurrentHashMap<>();
    for (Map.Entry<String, EdmSchema> entry : localSchemas.entrySet()) {
      if (entry.getKey() != null && entry.getValue() != null) {
        safeSchemas.put(entry.getKey(), entry.getValue());
        orderedValues.add(entry.getValue());
      }
    }
    schemas = safeSchemas;
    schemaList = Collections.unmodifiableList(orderedValues);
  }

  private void loadAliasToNamespaceInfo() {
    Map<String, String> localAliasToNamespaceInfo = createAliasToNamespaceInfo();
    aliasToNamespaceInfo = new ConcurrentHashMap<>(localAliasToNamespaceInfo);
  }

  @Override
  public EdmEntityContainer getEntityContainer() {
    return getEntityContainer(null);
  }

  @Override
  public EdmEntityContainer getEntityContainer(final FullQualifiedName namespaceOrAliasFQN) {
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    final FullQualifiedName key = fqn != null ? fqn : NULL_CONTAINER_KEY;
    EdmEntityContainer container = entityContainers.get(key);
    if (container == null) {
      container = createEntityContainer(fqn);
      if (container != null) {
        EdmEntityContainer existing = entityContainers.putIfAbsent(key, container);
        if (existing != null) {
          container = existing;
        } else if (fqn == null) {
          entityContainers.putIfAbsent(new FullQualifiedName(container.getNamespace(), container.getName()), container);
        }
      }
    }
    return container;
  }

  @Override
  public EdmEnumType getEnumType(final FullQualifiedName namespaceOrAliasFQN) {
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      return null;
    }
    EdmEnumType enumType = enumTypes.get(fqn);
    if (enumType == null) {
      enumType = createEnumType(fqn);
      if (enumType != null) {
        EdmEnumType existing = enumTypes.putIfAbsent(fqn, enumType);
        if (existing != null) {
          enumType = existing;
        }
      }
    }
    return enumType;
  }

  @Override
  public EdmTypeDefinition getTypeDefinition(final FullQualifiedName namespaceOrAliasFQN) {
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      return null;
    }
    EdmTypeDefinition typeDefinition = typeDefinitions.get(fqn);
    if (typeDefinition == null) {
      typeDefinition = createTypeDefinition(fqn);
      if (typeDefinition != null) {
        EdmTypeDefinition existing = typeDefinitions.putIfAbsent(fqn, typeDefinition);
        if (existing != null) {
          typeDefinition = existing;
        }
      }
    }
    return typeDefinition;
  }

  @Override
  public EdmEntityType getEntityType(final FullQualifiedName namespaceOrAliasFQN) {
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      return null;
    }
    EdmEntityType entityType = entityTypes.get(fqn);
    if (entityType == null) {
      entityType = createEntityType(fqn);
      if (entityType != null) {
        EdmEntityType existing = entityTypes.putIfAbsent(fqn, entityType);
        if (existing != null) {
          entityType = existing;
        }
      }
    }
    return entityType;
  }

  @Override
  public EdmEntityType getEntityTypeWithAnnotations(final FullQualifiedName namespaceOrAliasFQN) {
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      setIsPreviousES(false);
      return null;
    }
    EdmEntityType entityType = entityTypesWithAnnotations.get(fqn);
    if (entityType == null) {
      entityType = createEntityType(fqn);
      if (entityType != null) {
        EdmEntityType existing = entityTypesWithAnnotations.putIfAbsent(fqn, entityType);
        if (existing != null) {
          entityType = existing;
        }
      }
    }
    setIsPreviousES(false);
    return entityType;
  }

  protected EdmEntityType getEntityTypeWithAnnotations(final FullQualifiedName namespaceOrAliasFQN,
      boolean isEntityDerivedFromES) {
    this.isEntityDerivedFromES = isEntityDerivedFromES;
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      this.isEntityDerivedFromES = false;
      return null;
    }
    if (!isPreviousES() && getEntityContainer() != null) {
       getEntityContainer().getEntitySetsWithAnnotations();
    }
    EdmEntityType entityType = entityTypesDerivedFromES.get(fqn);
    if (entityType == null) {
      entityType = createEntityType(fqn);
      if (entityType != null) {
        EdmEntityType existing = entityTypesDerivedFromES.putIfAbsent(fqn, entityType);
        if (existing != null) {
          entityType = existing;
        }
      }
    }
    this.isEntityDerivedFromES = false;
    return entityType;
  }

  protected EdmComplexType getComplexTypeWithAnnotations(final FullQualifiedName namespaceOrAliasFQN,
      boolean isComplexDerivedFromES) {
    this.isComplexDerivedFromES = isComplexDerivedFromES;
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      this.isComplexDerivedFromES = false;
      return null;
    }
    if (!isPreviousES() && getEntityContainer() != null) {
       getEntityContainer().getEntitySetsWithAnnotations();
    }
    EdmComplexType complexType = complexTypesDerivedFromES.get(fqn);
    if (complexType == null) {
      complexType = createComplexType(fqn);
      if (complexType != null) {
        EdmComplexType existing = complexTypesDerivedFromES.putIfAbsent(fqn, complexType);
        if (existing != null) {
          complexType = existing;
        }
      }
    }
    this.isComplexDerivedFromES = false;
    return complexType;
  }

  @Override
  public EdmComplexType getComplexType(final FullQualifiedName namespaceOrAliasFQN) {
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      return null;
    }
    EdmComplexType complexType = complexTypes.get(fqn);
    if (complexType == null) {
      complexType = createComplexType(fqn);
      if (complexType != null) {
        EdmComplexType existing = complexTypes.putIfAbsent(fqn, complexType);
        if (existing != null) {
          complexType = existing;
        }
      }
    }
    return complexType;
  }

  @Override
  public EdmComplexType getComplexTypeWithAnnotations(final FullQualifiedName namespaceOrAliasFQN) {
    final FullQualifiedName fqn = resolvePossibleAlias(namespaceOrAliasFQN);
    if (fqn == null) {
      setIsPreviousES(false);
      return null;
    }
    EdmComplexType complexType = complexTypesWithAnnotations.get(fqn);
    if (complexType == null) {
      complexType = createComplexType(fqn);
      if (complexType != null) {
        EdmComplexType existing = complexTypesWithAnnotations.putIfAbsent(fqn, complexType);
        if (existing != null) {
          complexType = existing;
        }
      }
    }
    setIsPreviousES(false);
    return complexType;
  }

  @Override
  public EdmAction getUnboundAction(final FullQualifiedName actionName) {
    final FullQualifiedName fqn = resolvePossibleAlias(actionName);
    if (fqn == null) {
      return null;
    }
    EdmAction action = unboundActions.get(fqn);
    if (action == null) {
      action = createUnboundAction(fqn);
      if (action != null) {
        EdmAction existing = unboundActions.putIfAbsent(actionName, action);
        if (existing != null) {
          action = existing;
        }
      }
    }

    return action;
  }

  @Override
  public EdmAction getBoundAction(final FullQualifiedName actionName,
      final FullQualifiedName bindingParameterTypeName, final Boolean isBindingParameterCollection) {

    final FullQualifiedName actionFqn = resolvePossibleAlias(actionName);
    final FullQualifiedName bindingParameterTypeFqn = resolvePossibleAlias(bindingParameterTypeName);
    final ActionMapKey key = new ActionMapKey(actionFqn, bindingParameterTypeFqn, isBindingParameterCollection);
    EdmAction action = boundActions.get(key);
    if (action == null) {
      action = createBoundAction(actionFqn, bindingParameterTypeFqn, isBindingParameterCollection);
      if (action != null) {
        EdmAction existing = boundActions.putIfAbsent(key, action);
        if (existing != null) {
          action = existing;
        }
      }
    }

    return action;
  }

  @Override
  public List<EdmFunction> getUnboundFunctions(final FullQualifiedName functionName) {
    final FullQualifiedName functionFqn = resolvePossibleAlias(functionName);
    if (functionFqn == null) {
      return null;
    }

    List<EdmFunction> functions = unboundFunctionsByName.get(functionFqn);
    if (functions == null) {
      functions = createUnboundFunctions(functionFqn);
      if (functions != null) {
        List<EdmFunction> existing = unboundFunctionsByName.putIfAbsent(functionFqn, functions);
        if (existing != null) {
          functions = existing;
        } else {
          for (EdmFunction unbound : functions) {
            final FunctionMapKey key = new FunctionMapKey(
                new FullQualifiedName(unbound.getNamespace(), unbound.getName()),
                unbound.getBindingParameterTypeFqn(),
                unbound.isBindingParameterTypeCollection(),
                unbound.getParameterNames());
            unboundFunctionsByKey.putIfAbsent(key, unbound);
          }
        }
      }
    }

    return functions;
  }

  @Override
  public EdmFunction getUnboundFunction(final FullQualifiedName functionName, final List<String> parameterNames) {
    final FullQualifiedName functionFqn = resolvePossibleAlias(functionName);

    final FunctionMapKey key = new FunctionMapKey(functionFqn, null, null, parameterNames);
    EdmFunction function = unboundFunctionsByKey.get(key);
    if (function == null) {
      function = createUnboundFunction(functionFqn, parameterNames);
      if (function != null) {
        EdmFunction existing = unboundFunctionsByKey.putIfAbsent(key, function);
        if (existing != null) {
          function = existing;
        }
      }
    }

    return function;
  }

  @Override
  public EdmFunction getBoundFunction(final FullQualifiedName functionName,
      final FullQualifiedName bindingParameterTypeName,
      final Boolean isBindingParameterCollection, final List<String> parameterNames) {

    final FullQualifiedName functionFqn = resolvePossibleAlias(functionName);
    final FullQualifiedName bindingParameterTypeFqn = resolvePossibleAlias(bindingParameterTypeName);
    final FunctionMapKey key =
        new FunctionMapKey(functionFqn, bindingParameterTypeFqn, isBindingParameterCollection, parameterNames);
    EdmFunction function = boundFunctions.get(key);
    if (function == null) {
      function = createBoundFunction(functionFqn, bindingParameterTypeFqn, isBindingParameterCollection,
          parameterNames);
      if (function != null) {
        EdmFunction existing = boundFunctions.putIfAbsent(key, function);
        if (existing != null) {
          function = existing;
        }
      }
    }

    return function;
  }

  @Override
  public EdmTerm getTerm(final FullQualifiedName termName) {
    final FullQualifiedName fqn = resolvePossibleAlias(termName);
    if (fqn == null) {
      return null;
    }
    EdmTerm term = terms.get(fqn);
    if (term == null) {
      term = createTerm(fqn);
      if (term != null) {
        EdmTerm existing = terms.putIfAbsent(fqn, term);
        if (existing != null) {
          term = existing;
        }
      }
    }
    return term;
  }

  @Override
  public EdmAnnotations getAnnotationGroup(final FullQualifiedName targetName, String qualifier) {
    final FullQualifiedName fqn = resolvePossibleAlias(targetName);
    TargetQualifierMapKey key = new TargetQualifierMapKey(fqn, qualifier);
    EdmAnnotations _annotations = annotationGroups.get(key);
    if (_annotations == null) {
      _annotations = createAnnotationGroup(fqn, qualifier);
      if (_annotations != null) {
        EdmAnnotations existing = annotationGroups.putIfAbsent(key, _annotations);
        if (existing != null) {
          _annotations = existing;
        }
      }
    }
    return _annotations;
  }

  private FullQualifiedName resolvePossibleAlias(final FullQualifiedName namespaceOrAliasFQN) {
    if (aliasToNamespaceInfo == null) {
      loadAliasToNamespaceInfo();
    }
    FullQualifiedName finalFQN = null;
    if (namespaceOrAliasFQN != null) {
      final String namespace = aliasToNamespaceInfo.get(namespaceOrAliasFQN.getNamespace());
      // If not contained in info it must be a namespace
      if (namespace == null) {
        finalFQN = namespaceOrAliasFQN;
      } else {
        finalFQN = new FullQualifiedName(namespace, namespaceOrAliasFQN.getName());
      }
    }
    return finalFQN;
  }

  protected abstract Map<String, EdmSchema> createSchemas();

  protected abstract Map<String, String> createAliasToNamespaceInfo();

  public void cacheAliasNamespaceInfo(final String alias, final String namespace) {
    aliasToNamespaceInfo.put(alias, namespace);
  }

  protected abstract EdmEntityContainer createEntityContainer(FullQualifiedName containerName);

  public void cacheEntityContainer(final FullQualifiedName containerFQN, final EdmEntityContainer container) {
    final FullQualifiedName key = containerFQN != null ? containerFQN : NULL_CONTAINER_KEY;
    entityContainers.put(key, container);
  }

  protected abstract EdmEnumType createEnumType(FullQualifiedName enumName);

  public void cacheEnumType(final FullQualifiedName enumName, final EdmEnumType enumType) {
    enumTypes.put(enumName, enumType);
  }

  protected abstract EdmTypeDefinition createTypeDefinition(FullQualifiedName typeDefinitionName);

  public void cacheTypeDefinition(final FullQualifiedName typeDefName, final EdmTypeDefinition typeDef) {
    typeDefinitions.put(typeDefName, typeDef);
  }

  protected abstract EdmEntityType createEntityType(FullQualifiedName entityTypeName);

  public void cacheEntityType(final FullQualifiedName entityTypeName, final EdmEntityType entityType) {
    entityTypes.put(entityTypeName, entityType);
  }

  protected abstract EdmComplexType createComplexType(FullQualifiedName complexTypeName);

  public void cacheComplexType(final FullQualifiedName compelxTypeName, final EdmComplexType complexType) {
    complexTypes.put(compelxTypeName, complexType);
  }

  protected abstract EdmAction createUnboundAction(FullQualifiedName actionName);

  protected abstract List<EdmFunction> createUnboundFunctions(FullQualifiedName functionName);

  protected abstract EdmFunction createUnboundFunction(FullQualifiedName functionName, List<String> parameterNames);

  protected abstract EdmAction createBoundAction(FullQualifiedName actionName,
      FullQualifiedName bindingParameterTypeName,
      Boolean isBindingParameterCollection);

  protected abstract EdmFunction createBoundFunction(FullQualifiedName functionName,
      FullQualifiedName bindingParameterTypeName, Boolean isBindingParameterCollection,
      List<String> parameterNames);

  public void cacheFunction(final FullQualifiedName functionName, final EdmFunction function) {
    final FunctionMapKey key = new FunctionMapKey(functionName,
        function.getBindingParameterTypeFqn(), function.isBindingParameterTypeCollection(),
        function.getParameterNames());

    if (function.isBound()) {
      boundFunctions.putIfAbsent(key, function);
    } else {
      unboundFunctionsByName.computeIfAbsent(functionName, k -> new ArrayList<>()).add(function);
      unboundFunctionsByKey.putIfAbsent(key, function);
    }
  }

  public void cacheAction(final FullQualifiedName actionName, final EdmAction action) {
    if (action.isBound()) {
      final ActionMapKey key = new ActionMapKey(actionName,
          action.getBindingParameterTypeFqn(), action.isBindingParameterTypeCollection());
      boundActions.put(key, action);
    } else {
      unboundActions.put(actionName, action);
    }
  }

  protected abstract EdmTerm createTerm(FullQualifiedName termName);

  public void cacheTerm(final FullQualifiedName termName, final EdmTerm term) {
    terms.put(termName, term);
  }

  protected abstract EdmAnnotations createAnnotationGroup(FullQualifiedName targetName, String qualifier);

  public void cacheAnnotationGroup(final FullQualifiedName targetName,
      final EdmAnnotations annotationsGroup) {
    TargetQualifierMapKey key = new TargetQualifierMapKey(targetName, annotationsGroup.getQualifier());
    annotationGroups.put(key, annotationsGroup);
  }

  @Override
  public EdmAction getBoundActionWithBindingType(FullQualifiedName bindingParameterTypeName,
      Boolean isBindingParameterCollection) {
    for (EdmSchema schema:getSchemas()) {
      for (EdmAction action: schema.getActions()) {
        if (action.isBound()) {
          EdmParameter bindingParameter = action.getParameter(action.getParameterNames().get(0));
          if (bindingParameter.getType().getFullQualifiedName().equals(bindingParameterTypeName)
              && bindingParameter.isCollection() == isBindingParameterCollection) {
            return action;
          }
        }
      }
    }
    return null;
  }

  @Override
  public List<EdmFunction> getBoundFunctionsWithBindingType(FullQualifiedName bindingParameterTypeName,
      Boolean isBindingParameterCollection){
    List<EdmFunction> functions = new ArrayList<EdmFunction>();
    for (EdmSchema schema:getSchemas()) {
      for (EdmFunction function: schema.getFunctions()) {
        if (function.isBound()) {
          EdmParameter bindingParameter = function.getParameter(function.getParameterNames().get(0));
          if (bindingParameter.getType().getFullQualifiedName().equals(bindingParameterTypeName)
              && bindingParameter.isCollection() == isBindingParameterCollection) {
            functions.add(function);
          }
        }
      }
    }
    return functions;
  }

  protected boolean isEntityDerivedFromES() {
    return isEntityDerivedFromES;
  }

  protected boolean isComplexDerivedFromES() {
    return isComplexDerivedFromES;
  }

  protected void setIsPreviousES(boolean isPreviousES) {
    this.isPreviousES = isPreviousES;
  }

  protected boolean isPreviousES() {
    return isPreviousES;
  }

  protected Map<String, List<CsdlAnnotation>> getAnnotationsMap() {
    return annotationMap;
  }
}
