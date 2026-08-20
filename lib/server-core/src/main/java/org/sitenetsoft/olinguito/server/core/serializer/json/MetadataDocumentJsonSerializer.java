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
 * Copyright 2026 SiteNetSoft - Removed unnecessary boxing and modernized length checks
 * Copyright 2026 SiteNetSoft - OLINGO-1534: Null-safe field writing for constant expressions in logical/comparison
 * Copyright 2026 SiteNetSoft - OLINGO-1399: fall back to raw term name for unresolvable annotation terms
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON conformant $EntityContainer, flat $Extends,
 * structural container children (no $Kind) and served $Version
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON facet defaults ($Nullable polarity,
 * omitted $Type for Edm.String, numeric enum values and type-definition facets, $OnDelete,
 * single $ReferentialConstraint object)
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON bare-value constant expressions and
 * record @type control information
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: write $OpenType for open entity and complex types
 */
package org.sitenetsoft.olinguito.server.core.serializer.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.EdmMember;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.EdmOperation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmReferentialConstraint;
import org.sitenetsoft.olinguito.commons.api.edm.EdmReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSchema;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTerm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.TargetType;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmApply;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmCast;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmDynamicExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmIf;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLabeledElementReference;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLogicalOrComparisonExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmNot;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmNull;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPath;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmRecord;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmUrlRef;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceInclude;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceIncludeAnnotation;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.Kind;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;

import com.fasterxml.jackson.core.JsonGenerator;

public class MetadataDocumentJsonSerializer {
  
  private final ServiceMetadata serviceMetadata;
  private final Map<String, String> namespaceToAlias = new HashMap<>();
  private static final String DOLLAR = "$";
  private static final String VERSION = DOLLAR + "Version";
  private static final String REFERENCES = DOLLAR + "Reference";
  private static final String INCLUDE = DOLLAR + "Include";
  private static final String NAMESPACE = DOLLAR + "Namespace";
  private static final String ALIAS = DOLLAR + "Alias";
  private static final String INCLUDE_ANNOTATIONS = DOLLAR + "IncludeAnnotations";
  private static final String TERM_NAMESPACE = DOLLAR + "TermNamespace";
  private static final String TARGET_NAMESPACE = DOLLAR + "TargetNamespace";
  private static final String QUALIFIER = DOLLAR + "Qualifier";
  private static final String IS_FLAGS = DOLLAR + "IsFlags";
  private static final String UNDERLYING_TYPE = DOLLAR + "UnderlyingType";
  private static final String KIND = DOLLAR + "Kind";
  private static final String MAX_LENGTH = DOLLAR + "MaxLength";
  private static final String PRECISION = DOLLAR + "Precision";
  private static final String SCALE = DOLLAR + "Scale";
  private static final String SRID = DOLLAR + "SRID";
  private static final String COLLECTION = DOLLAR + "Collection";
  private static final String BASE_TYPE = DOLLAR + "BaseType";
  private static final String HAS_STREAM = DOLLAR + "HasStream";
  private static final String KEY = DOLLAR + "Key";
  private static final String ABSTRACT = DOLLAR + "Abstract";
  private static final String OPEN_TYPE = DOLLAR + "OpenType";
  private static final String TYPE = DOLLAR + "Type";
  private static final String NULLABLE = DOLLAR + "Nullable";
  private static final String UNICODE = DOLLAR + "Unicode";
  private static final String DEFAULT_VALUE = DOLLAR + "DefaultValue";
  private static final String PARTNER = DOLLAR + "Partner";
  private static final String CONTAINS_TARGET = DOLLAR + "ContainsTarget";
  private static final String REFERENTIAL_CONSTRAINT = DOLLAR + "ReferentialConstraint";
  private static final String ISBOUND = DOLLAR + "IsBound";
  private static final String ENTITY_SET_PATH = DOLLAR + "EntitySetPath";
  private static final String PARAMETER = DOLLAR + "Parameter";
  private static final String RETURN_TYPE = DOLLAR + "ReturnType";
  private static final String ISCOMPOSABLE = DOLLAR + "IsComposable";
  private static final String PARAMETER_NAME = DOLLAR + "Name";
  private static final String BASE_TERM = DOLLAR + "BaseTerm";
  private static final String APPLIES_TO = DOLLAR + "AppliesTo";
  private static final String NAVIGATION_PROPERTY_BINDING = DOLLAR + "NavigationPropertyBinding";
  private static final String EXTENDS = DOLLAR + "Extends";
  private static final String INCLUDE_IN_SERV_DOC = DOLLAR + "IncludeInServiceDocument";
  private static final String ANNOTATION = DOLLAR + "Annotations";
  private static final String ANNOTATION_PATH = DOLLAR + "Path";
  private static final String NAME = DOLLAR + "Name";
  private static final String ENTITY_CONTAINER_MEMBER = DOLLAR + "EntityContainer";
  private static final String ON_DELETE_MEMBER = DOLLAR + "OnDelete";
  private static final String TYPE_DEFINITION_KIND = "TypeDefinition";
  private static final String EDM_STRING = "Edm.String";
  private static final String AT_TYPE = "@type";
  private static final long MAX_SAFE_INTEGER = 9007199254740991L;
  private static final String INF = "INF";
  private static final String NEGATIVE_INF = "-INF";
  private static final String NAN = "NaN";

  public MetadataDocumentJsonSerializer(final ServiceMetadata serviceMetadata) throws SerializerException {
    if (serviceMetadata == null || serviceMetadata.getEdm() == null) {
      throw new SerializerException("Service Metadata and EDM must not be null for a service.",
          SerializerException.MessageKeys.NULL_METADATA_OR_EDM);
    }
    this.serviceMetadata = serviceMetadata;
  }
  
  public void writeMetadataDocument(final JsonGenerator json) throws SerializerException, IOException {
    json.writeStartObject();
    // OData 4.01, CSDL JSON section 4: "The value of $Version is a string containing either 4.0 or
    // 4.01." The version written is the one the service serves; every other value (and a service
    // that reports none) is written as 4.0, which is the version the handler advertises in the
    // OData-Version response header and the XML serializer writes as Version="4.0".
    final ODataServiceVersion dataServiceVersion = serviceMetadata.getDataServiceVersion();
    json.writeStringField(VERSION,
        ODataServiceVersion.V401 == dataServiceVersion ? ODataServiceVersion.V401.toString()
            : ODataServiceVersion.V40.toString());
    // Section 4: a metadata document of an OData service MUST name its entity container, and this is
    // the only reference in the document that MUST NOT use the alias. The container that is actually
    // serialized into the schema tree gets its namespace from the schema that declares it
    // (EdmSchema#getEntityContainer()); Edm#getEntityContainer() resolves the *default* container via
    // the provider's container-info lookup, which is not guaranteed to agree with that namespace (and
    // does not, for a provider whose default-container lookup answers with a different namespace than
    // the schema the container is actually declared in) -- naming that container here would dangle. So
    // this walks the schemas for the one that owns the served container and names it from there,
    // falling back to Edm#getEntityContainer() only when no schema carries one.
    final String entityContainerName = documentEntityContainerName();
    if (entityContainerName != null) {
      json.writeStringField(ENTITY_CONTAINER_MEMBER, entityContainerName);
    }
    if (!serviceMetadata.getReferences().isEmpty()) {
      appendReference(json);
    }
    appendDataServices(json);
    json.writeEndObject();
  }

  /**
   * Section 4: the document-level $EntityContainer value must name the entity container "of that
   * service" -- {@code Edm#getEntityContainer()} identifies which container that is (via the
   * provider's default-container lookup) -- but namespace-qualified the way the document itself
   * spells it, i.e. from the schema that declares a same-named container, never from whatever
   * namespace the provider's lookup happens to report. Order of resolution:
   * <ol>
   * <li>Ask {@code Edm#getEntityContainer()} for the service's container; take its local name.</li>
   * <li>Find the schema whose {@code getEntityContainer()} is non-null and whose container's name
   * equals that local name; use that schema's namespace.</li>
   * <li>If no schema-owned container matches by name, fall back to the first schema-owned
   * container found while walking the schemas.</li>
   * <li>If no schema carries a container at all, fall back to the {@code Edm#getEntityContainer()}
   * FQN as-is; if there is no container at all, there is nothing to name.</li>
   * </ol>
   *
   * @return the namespace-qualified name of the service's entity container as the document spells
   *         it, or {@code null} when the EDM has no entity container at all
   */
  private String documentEntityContainerName() {
    final Edm edm = serviceMetadata.getEdm();
    final EdmEntityContainer defaultContainer = edm.getEntityContainer();
    final String defaultLocalName = defaultContainer == null ? null : defaultContainer.getName();
    String firstSchemaOwnedName = null;
    for (EdmSchema schema : edm.getSchemas()) {
      final EdmEntityContainer schemaContainer = schema.getEntityContainer();
      if (schemaContainer == null) {
        continue;
      }
      final String candidateName = schema.getNamespace() + "." + schemaContainer.getName();
      if (defaultLocalName != null && defaultLocalName.equals(schemaContainer.getName())) {
        return candidateName;
      }
      if (firstSchemaOwnedName == null) {
        firstSchemaOwnedName = candidateName;
      }
    }
    if (firstSchemaOwnedName != null) {
      return firstSchemaOwnedName;
    }
    return defaultContainer == null ? null
        : defaultContainer.getNamespace() + "." + defaultContainer.getName();
  }

  private void appendDataServices(JsonGenerator json) throws SerializerException, IOException {
    for (EdmSchema schema : serviceMetadata.getEdm().getSchemas()) {
      appendSchema(json, schema);
    }
  }

  private void appendSchema(JsonGenerator json, EdmSchema schema) 
      throws SerializerException, IOException {
    json.writeFieldName(schema.getNamespace());
    json.writeStartObject();
    if (schema.getAlias() != null) {
      json.writeStringField(ALIAS, schema.getAlias());
      namespaceToAlias.put(schema.getNamespace(), schema.getAlias());
    }
    // EnumTypes
    appendEnumTypes(json, schema.getEnumTypes());
    
    // TypeDefinitions
    appendTypeDefinitions(json, schema.getTypeDefinitions());
    
    // EntityTypes
    appendEntityTypes(json, schema.getEntityTypes());
    
    // ComplexTypes
    appendComplexTypes(json, schema.getComplexTypes());
    
    // Actions
    appendActions(json, schema.getActions());
    
    // Functions
    appendFunctions(json, schema.getFunctions());
    
    //Terms
    appendTerms(json, schema.getTerms());
    
    // EntityContainer
    appendEntityContainer(json, schema.getEntityContainer());
 
    // AnnotationGroups
    appendAnnotationGroups(json, schema.getAnnotationGroups());

    appendAnnotations(json, schema, null);
    
    json.writeEndObject();
  }

  private void appendAnnotationGroups(final JsonGenerator json, 
      final List<EdmAnnotations> annotationGroups) throws SerializerException, IOException {
    if (!annotationGroups.isEmpty()) {
      json.writeObjectFieldStart(ANNOTATION);
    }
    for (EdmAnnotations annotationGroup : annotationGroups) {
      appendAnnotationGroup(json, annotationGroup);
    }
    if (!annotationGroups.isEmpty()) {
      json.writeEndObject();
    }
  }

  private void appendAnnotationGroup(final JsonGenerator json, 
      final EdmAnnotations annotationGroup) throws SerializerException, IOException {
    String targetPath = annotationGroup.getTargetPath();
    if (annotationGroup.getQualifier() != null) {
      json.writeObjectFieldStart(targetPath + "#" + annotationGroup.getQualifier());
    } else {
      json.writeObjectFieldStart(targetPath);
    }
    appendAnnotations(json, annotationGroup, null);
    json.writeEndObject();
  }

  private void appendEntityContainer(final JsonGenerator json, 
      final EdmEntityContainer container) throws SerializerException, IOException {
    if (container != null) {
      json.writeObjectFieldStart(container.getName());
      json.writeStringField(KIND, Kind.EntityContainer.name());
      FullQualifiedName parentContainerName = container.getParentContainerName();
      if (parentContainerName != null) {
        String parentContainerNameString;
        if (namespaceToAlias.get(parentContainerName.getNamespace()) != null) {
          parentContainerNameString =
              namespaceToAlias.get(parentContainerName.getNamespace()) + "." + parentContainerName.getName();
        } else {
          parentContainerNameString = parentContainerName.getFullQualifiedNameAsString();
        }
        // Section 13.1: "The value of $Extends is the qualified name of the entity container to be
        // extended." It is a member of the container object; there is no Extending object in CSDL JSON.
        json.writeStringField(EXTENDS, parentContainerNameString);
      }

      // EntitySets
      appendEntitySets(json, container.getEntitySets());

      // ActionImports
      appendActionImports(json, container.getActionImports());

      // FunctionImports
      appendFunctionImports(json, container.getFunctionImports());

       
      // Singletons
      appendSingletons(json, container.getSingletons());

      // Annotations
      appendAnnotations(json, container, null);

      json.writeEndObject();
    }
    
  }

  private void appendSingletons(final JsonGenerator json, 
      final List<EdmSingleton> singletons) throws SerializerException, IOException {
    for (EdmSingleton singleton : singletons) {
      json.writeObjectFieldStart(singleton.getName());
      // Section 13.3: the singleton object's only defined members are $Type, $Nullable and
      // $NavigationPropertyBinding (plus annotations); $Kind is not one of them.
      json.writeStringField(TYPE, getAliasedFullQualifiedName(singleton.getEntityType()));
      
      appendNavigationPropertyBindings(json, singleton);
      appendAnnotations(json, singleton, null);
      json.writeEndObject();
    }
  }

  private void appendFunctionImports(final JsonGenerator json, final List<EdmFunctionImport> functionImports)
      throws SerializerException, IOException {
    for (EdmFunctionImport functionImport : functionImports) {
      json.writeObjectFieldStart(functionImport.getName());

      // Section 13.6: the function import object MUST contain $Function; $Kind is not defined for it.
      String functionFQNString;
      FullQualifiedName functionFqn = functionImport.getFunctionFqn();
      if (namespaceToAlias.get(functionFqn.getNamespace()) != null) {
        functionFQNString = namespaceToAlias.get(functionFqn.getNamespace()) + "." + functionFqn.getName();
      } else {
        functionFQNString = functionFqn.getFullQualifiedNameAsString();
      }
      json.writeStringField(DOLLAR + Kind.Function.name(), functionFQNString);

      EdmEntitySet returnedEntitySet = functionImport.getReturnedEntitySet();
      if (returnedEntitySet != null) {
        // Section 13.6: "either the unqualified name of an entity set in the same entity container or
        // a path to an entity set in a different entity container."
        json.writeStringField(DOLLAR + Kind.EntitySet.name(),
            entitySetReferenceValue(functionImport.getEntityContainer(), returnedEntitySet));
      }
      // Default is false and we do not write the default
      if (functionImport.isIncludeInServiceDocument()) {
        json.writeBooleanField(INCLUDE_IN_SERV_DOC, functionImport.isIncludeInServiceDocument());
      }
      appendAnnotations(json, functionImport, null);
      json.writeEndObject();
    }
  }

  private void appendActionImports(final JsonGenerator json,
      final List<EdmActionImport> actionImports)
          throws SerializerException, IOException {
    for (EdmActionImport actionImport : actionImports) {
      json.writeObjectFieldStart(actionImport.getName());
      // Section 13.5: the action import object MUST contain $Action; $Kind is not defined for it.
      json.writeStringField(DOLLAR + Kind.Action.name(), getAliasedFullQualifiedName(actionImport.getUnboundAction()));
      EdmEntitySet returnedEntitySet = actionImport.getReturnedEntitySet();
      if (returnedEntitySet != null) {
        // Section 13.5: "either the unqualified name of an entity set in the same entity container or
        // a path to an entity set in a different entity container."
        json.writeStringField(DOLLAR + Kind.EntitySet.name(),
            entitySetReferenceValue(actionImport.getEntityContainer(), returnedEntitySet));
      }
      appendAnnotations(json, actionImport, null);
      json.writeEndObject();
    }

  }

  /**
   * Sections 13.5/13.6: the value of $EntitySet is "either the unqualified name of an entity set in the
   * same entity container or a path to an entity set in a different entity container." A target path to
   * a model element in a different container is namespace-qualified-container-name/element-name (see
   * the general target-path syntax, section 14.5.2 / Example 42:
   * {@code MySchema.MyEntityContainer/MyEntitySet/...}), never alias- or container-namespace-of-the-
   * import-prefixed as the pre-conformance code wrote it.
   */
  private String entitySetReferenceValue(final EdmEntityContainer importContainer,
      final EdmEntitySet returnedEntitySet) {
    EdmEntityContainer targetContainer = returnedEntitySet.getEntityContainer();
    if (targetContainer != null
        && !targetContainer.getFullQualifiedName().equals(importContainer.getFullQualifiedName())) {
      return targetContainer.getFullQualifiedName().getFullQualifiedNameAsString() + "/" + returnedEntitySet.getName();
    }
    return returnedEntitySet.getName();
  }

  private void appendEntitySets(final JsonGenerator json, 
      final List<EdmEntitySet> entitySets) throws SerializerException, IOException {
    for (EdmEntitySet entitySet : entitySets) {
      json.writeObjectFieldStart(entitySet.getName());
      // Section 13.2: "The entity set object MUST contain the members $Collection and $Type." $Kind is
      // not one of the members section 13.2 defines for an entity set, so it is not written.
      json.writeBooleanField(COLLECTION, true);
      json.writeStringField(TYPE, getAliasedFullQualifiedName(entitySet.getEntityType()));
      if (!entitySet.isIncludeInServiceDocument()) {
        json.writeBooleanField(INCLUDE_IN_SERV_DOC, entitySet.isIncludeInServiceDocument());
      }

      appendNavigationPropertyBindings(json, entitySet);
      appendAnnotations(json, entitySet, null);
      json.writeEndObject();
    }
  }

  private void appendNavigationPropertyBindings(final JsonGenerator json, 
      final EdmBindingTarget bindingTarget) throws SerializerException, IOException {
    if (bindingTarget.getNavigationPropertyBindings() != null && 
        !bindingTarget.getNavigationPropertyBindings().isEmpty()) {
      json.writeObjectFieldStart(NAVIGATION_PROPERTY_BINDING);
      for (EdmNavigationPropertyBinding binding : bindingTarget.getNavigationPropertyBindings()) {
        json.writeStringField(binding.getPath(), binding.getTarget());
      }
      json.writeEndObject();
    }
  }

  private void appendTerms(final JsonGenerator json, final List<EdmTerm> terms) 
      throws SerializerException, IOException {
    for (EdmTerm term : terms) {
      json.writeObjectFieldStart(term.getName());
      json.writeStringField(KIND, Kind.Term.name());

      json.writeStringField(TYPE, getAliasedFullQualifiedName(term.getType()));

      if (term.getBaseTerm() != null) {
        json.writeStringField(BASE_TERM, getAliasedFullQualifiedName(term.getBaseTerm().getFullQualifiedName()));
      }

      if (term.getAppliesTo() != null && !term.getAppliesTo().isEmpty()) {
        String appliesToString = "";
        boolean first = true;
        for (TargetType target : term.getAppliesTo()) {
          if (first) {
            first = false;
            appliesToString = target.toString();
          } else {
            appliesToString = appliesToString + " " + target.toString();
          }
        }
        json.writeStringField(APPLIES_TO, appliesToString);
      }

      // Facets
      // Section 7.2.1: absence of $Nullable means false in CSDL JSON (the XML attribute defaults the
      // other way), so the member is written when the value is nullable and omitted otherwise.
      if (term.isNullable()) {
        json.writeBooleanField(NULLABLE, true);
      }

      if (term.getDefaultValue() != null) {
        json.writeStringField(DEFAULT_VALUE, term.getDefaultValue());
      }

      if (term.getMaxLength() != null) {
        json.writeNumberField(MAX_LENGTH, term.getMaxLength());
      }

      if (term.getPrecision() != null) {
        json.writeNumberField(PRECISION, term.getPrecision());
      }

      if (term.getScale() != null) {
        json.writeNumberField(SCALE, term.getScale());
      }
      
      appendAnnotations(json, term, null);
      json.writeEndObject();
    }
    
  }

  private void appendFunctions(final JsonGenerator json, 
      final List<EdmFunction> functions) throws SerializerException, IOException {
    Map<String, List<EdmFunction>> functionsMap = new HashMap<>();
    for (EdmFunction function : functions) {
      if (functionsMap.containsKey(function.getName())) {
        List<EdmFunction> actionsWithSpecificActionName = functionsMap.get(function.getName());
        actionsWithSpecificActionName.add(function);
        functionsMap.put(function.getName(), actionsWithSpecificActionName);
      } else {
        List<EdmFunction> functionList = new ArrayList<>();
        functionList.add(function);
        functionsMap.put(function.getName(), functionList);
      }
    }
    
    for (Entry<String, List<EdmFunction>> functionsMapEntry : functionsMap.entrySet()) {
      json.writeArrayFieldStart(functionsMapEntry.getKey());
      List<EdmFunction> functionEntry = functionsMapEntry.getValue();
      for (EdmFunction function : functionEntry) {
        json.writeStartObject();
        json.writeStringField(KIND, Kind.Function.name());
        if (function.getEntitySetPath() != null) {
          json.writeStringField(ENTITY_SET_PATH, function.getEntitySetPath());
        }
        if (function.isBound()) {
          json.writeBooleanField(ISBOUND, function.isBound());
        }

        if (function.isComposable()) {
          json.writeBooleanField(ISCOMPOSABLE, function.isComposable());
        }
        
        appendOperationParameters(json, function);

        appendOperationReturnType(json, function);

        appendAnnotations(json, function, null);

        json.writeEndObject();
      }
      json.writeEndArray();
    }
  }

  private void appendActions(final JsonGenerator json, 
      final List<EdmAction> actions) throws SerializerException, IOException {
    Map<String, List<EdmAction>> actionsMap = new HashMap<>();
    for (EdmAction action : actions) {
      if (actionsMap.containsKey(action.getName())) {
        List<EdmAction> actionsWithSpecificActionName = actionsMap.get(action.getName());
        actionsWithSpecificActionName.add(action);
        actionsMap.put(action.getName(), actionsWithSpecificActionName);
      } else {
        List<EdmAction> actionList = new ArrayList<>();
        actionList.add(action);
        actionsMap.put(action.getName(), actionList);
      }
    }
    for (Entry<String, List<EdmAction>> actionsMapEntry : actionsMap.entrySet()) {
      json.writeArrayFieldStart(actionsMapEntry.getKey());
      List<EdmAction> actionEntry = actionsMapEntry.getValue();
      for (EdmAction action : actionEntry) {
        json.writeStartObject();
        json.writeStringField(KIND, Kind.Action.name());
        if (action.getEntitySetPath() != null) {
          json.writeStringField(ENTITY_SET_PATH, action.getEntitySetPath());
        }
        // Section 12.5: "Absence of the member means false", so the default is omitted.
        if (action.isBound()) {
          json.writeBooleanField(ISBOUND, true);
        }

        appendOperationParameters(json, action);

        appendOperationReturnType(json, action);

        appendAnnotations(json, action, null);

        json.writeEndObject();
      }
      json.writeEndArray();
    }
  }

  private void appendOperationReturnType(final JsonGenerator json, 
      final EdmOperation operation) throws SerializerException, IOException {
    EdmReturnType returnType = operation.getReturnType();
    if (returnType != null) {
      json.writeObjectFieldStart(RETURN_TYPE);
      String returnTypeFqnString;
      if (EdmTypeKind.PRIMITIVE.equals(returnType.getType().getKind())) {
        returnTypeFqnString = getFullQualifiedName(returnType.getType());
      } else {
        returnTypeFqnString = getAliasedFullQualifiedName(returnType.getType());
      }
      json.writeStringField(TYPE, returnTypeFqnString);
      if (returnType.isCollection()) {
        json.writeBooleanField(COLLECTION, returnType.isCollection());
      }
      
      appendReturnTypeFacets(json, returnType);
      json.writeEndObject();
    }
  }

  private void appendReturnTypeFacets(final JsonGenerator json, 
      final EdmReturnType returnType) throws SerializerException, IOException {
    // Section 12.8/7.2.1: absence of $Nullable means false in CSDL JSON, the inverse of the XML
    // attribute, so the member is only written when the return type is nullable. Section 12.8 also
    // says "If the return type is a collection of entity types, the $Nullable member has no meaning
    // and MUST NOT be specified", so that one case is suppressed even when the model reports true.
    final boolean entityCollection = returnType.isCollection()
        && EdmTypeKind.ENTITY == returnType.getType().getKind();
    if (returnType.isNullable() && !entityCollection) {
      json.writeBooleanField(NULLABLE, true);
    }
    if (returnType.getMaxLength() != null) {
      json.writeNumberField(MAX_LENGTH, returnType.getMaxLength());
    }
    if (returnType.getPrecision() != null) {
      json.writeNumberField(PRECISION, returnType.getPrecision());
    }
    if (returnType.getScale() != null) {
      json.writeNumberField(SCALE, returnType.getScale());
    }
  }

  private void appendOperationParameters(final JsonGenerator json, 
      final EdmOperation operation) throws SerializerException, IOException {
    if (!operation.getParameterNames().isEmpty()) {
      json.writeArrayFieldStart(PARAMETER);
    }
    for (String parameterName : operation.getParameterNames()) {
      EdmParameter parameter = operation.getParameter(parameterName);
      json.writeStartObject();
      json.writeStringField(PARAMETER_NAME, parameterName);
      String typeFqnString;
      if (EdmTypeKind.PRIMITIVE.equals(parameter.getType().getKind())) {
        typeFqnString = getFullQualifiedName(parameter.getType());
      } else {
        typeFqnString = getAliasedFullQualifiedName(parameter.getType());
      }
      json.writeStringField(TYPE, typeFqnString);
      if (parameter.isCollection()) {
        json.writeBooleanField(COLLECTION, parameter.isCollection());
      }
      
      appendParameterFacets(json, parameter);

      appendAnnotations(json, parameter, null);
      json.writeEndObject();
    }
    if (!operation.getParameterNames().isEmpty()) {
      json.writeEndArray();
    }
  }

  private void appendParameterFacets(final JsonGenerator json, 
      final EdmParameter parameter) throws SerializerException, IOException {
    // Section 12.9/7.2.1: absence of $Nullable means false in CSDL JSON, the inverse of the XML
    // attribute, so the member is only written when the parameter is nullable.
    if (parameter.isNullable()) {
      json.writeBooleanField(NULLABLE, true);
    }
    if (parameter.getMaxLength() != null) {
      json.writeNumberField(MAX_LENGTH, parameter.getMaxLength());
    }
    if (parameter.getPrecision() != null) {
      json.writeNumberField(PRECISION, parameter.getPrecision());
    }
    if (parameter.getScale() != null) {
      json.writeNumberField(SCALE, parameter.getScale());
    }
  }

  private void appendComplexTypes(final JsonGenerator json, 
      final List<EdmComplexType> complexTypes) throws SerializerException, IOException {
    for (EdmComplexType complexType : complexTypes) {
      json.writeObjectFieldStart(complexType.getName());

      json.writeStringField(KIND, Kind.ComplexType.name());
      if (complexType.getBaseType() != null) {
        json.writeStringField(BASE_TYPE, getAliasedFullQualifiedName(complexType.getBaseType()));
      }

      if (complexType.isAbstract()) {
        json.writeBooleanField(ABSTRACT, complexType.isAbstract());
      }

      // Section 9.3: "The value of $OpenType is one of the Boolean literals true or false.
      // Absence of the member means false." - so an open type has to say so, and a closed one stays
      // silent. The member was never written before, which closed every open type on the JSON wire.
      if (complexType.isOpenType()) {
        json.writeBooleanField(OPEN_TYPE, true);
      }

      appendProperties(json, complexType);

      appendNavigationProperties(json, complexType);

      appendAnnotations(json, complexType, null);

      json.writeEndObject();
    }
  }

  private void appendEntityTypes(JsonGenerator json, 
      List<EdmEntityType> entityTypes) throws SerializerException, IOException {
    for (EdmEntityType entityType : entityTypes) {
      json.writeObjectFieldStart(entityType.getName());
      json.writeStringField(KIND, Kind.EntityType.name());
      if (entityType.hasStream()) {
        json.writeBooleanField(HAS_STREAM, entityType.hasStream());
      }

      if (entityType.getBaseType() != null) {
        json.writeStringField(BASE_TYPE, getAliasedFullQualifiedName(entityType.getBaseType()));
      }

      if (entityType.isAbstract()) {
        json.writeBooleanField(ABSTRACT, entityType.isAbstract());
      }

      // Section 6.3: $OpenType defaults to false, so an open entity type has to declare itself.
      if (entityType.isOpenType()) {
        json.writeBooleanField(OPEN_TYPE, true);
      }

      appendKey(json, entityType);

      appendProperties(json, entityType);

      appendNavigationProperties(json, entityType);

      appendAnnotations(json, entityType, null);

      json.writeEndObject();
    }
  }

  private void appendNavigationProperties(final JsonGenerator json, 
      final EdmStructuredType type) throws SerializerException, IOException {
    List<String> navigationPropertyNames = new ArrayList<>(type.getNavigationPropertyNames());
    if (type.getBaseType() != null) {
      navigationPropertyNames.removeAll(type.getBaseType().getNavigationPropertyNames());
    }
    for (String navigationPropertyName : navigationPropertyNames) {
      EdmNavigationProperty navigationProperty = type.getNavigationProperty(navigationPropertyName);
      json.writeObjectFieldStart(navigationPropertyName);
      json.writeStringField(KIND, Kind.NavigationProperty.name());
      
      json.writeStringField(TYPE, getAliasedFullQualifiedName(navigationProperty.getType()));
      if (navigationProperty.isCollection()) {
        json.writeBooleanField(COLLECTION, navigationProperty.isCollection());
      }
      
      // Section 8.2/7.2.1: absence of $Nullable means false in CSDL JSON (the XML attribute defaults
      // the other way), so the member is written when the value is nullable and omitted otherwise.
      // Section 8.2 also forbids it outright on collections: "Nullable MUST NOT be specified for a
      // collection-valued navigation property, a collection is allowed to have zero items." The model
      // defaults nullable to true, so without this guard every collection navigation property that
      // leaves the default would emit a MUST-NOT member.
      if (!navigationProperty.isCollection() && navigationProperty.isNullable()) {
        json.writeBooleanField(NULLABLE, true);
      }

      if (navigationProperty.getPartner() != null) {
        EdmNavigationProperty partner = navigationProperty.getPartner();
        json.writeStringField(PARTNER, partner.getName());
      }

      if (navigationProperty.containsTarget()) {
        json.writeBooleanField(CONTAINS_TARGET, navigationProperty.containsTarget());
      }

      final List<EdmReferentialConstraint> constraints = navigationProperty.getReferentialConstraints();
      if (constraints != null && !constraints.isEmpty()) {
        // Section 8.5: "The value of $ReferentialConstraint is an object with one member per
        // referential constraint." One object per constraint would repeat the member name.
        json.writeObjectFieldStart(REFERENTIAL_CONSTRAINT);
        for (EdmReferentialConstraint constraint : constraints) {
          json.writeStringField(constraint.getPropertyName(), constraint.getReferencedPropertyName());
          // Section 8.5 / Example 23: annotations of a referential constraint are members of the same
          // object, prefixed with the constraint's member name. The constraint itself is the
          // annotatable -- passing each annotation instead would write its *nested* annotations.
          appendAnnotations(json, constraint, constraint.getPropertyName());
        }
        json.writeEndObject();
      }
      
      if (navigationProperty.getOnDelete() != null) {
        // Section 8.6: "The value of $OnDelete is a string with one of the values Cascade, None,
        // SetNull, or SetDefault." / "Annotations for $OnDelete are prefixed with $OnDelete."
        json.writeStringField(ON_DELETE_MEMBER, navigationProperty.getOnDelete().getAction());
        appendAnnotations(json, navigationProperty.getOnDelete(), ON_DELETE_MEMBER);
      }

      appendAnnotations(json, navigationProperty, null);

      json.writeEndObject();
    }
  }

  private void appendProperties(final JsonGenerator json, 
      final EdmStructuredType type) throws SerializerException, IOException {
    List<String> propertyNames = new ArrayList<>(type.getPropertyNames());
    if (type.getBaseType() != null) {
      propertyNames.removeAll(type.getBaseType().getPropertyNames());
    }
    for (String propertyName : propertyNames) {
      EdmProperty property = type.getStructuralProperty(propertyName);
      json.writeObjectFieldStart(propertyName);
      // Section 7.1: "Absence of the $Type member means the type is Edm.String. This member SHOULD be
      // omitted for string properties to reduce document size." For a collection-valued property the
      // rule applies to the item type, which is what getType() returns here.
      final String fqnString = property.isPrimitive()
          ? getFullQualifiedName(property.getType()) : getAliasedFullQualifiedName(property.getType());
      if (!EDM_STRING.equals(fqnString)) {
        json.writeStringField(TYPE, fqnString);
      }
      if (property.isCollection()) {
        json.writeBooleanField(COLLECTION, property.isCollection());
      }

      // Facets
      // Section 7.2.1: absence of $Nullable means false in CSDL JSON (the XML attribute defaults the
      // other way), so the member is written when the value is nullable and omitted otherwise.
      if (property.isNullable()) {
        json.writeBooleanField(NULLABLE, true);
      }

      // Section 7.2.5 defaults $Unicode the other way round -- "Absence of the member means true" --
      // so, unlike $Nullable above, this member is the one written when the value is false.
      if (!property.isUnicode()) {
        json.writeBooleanField(UNICODE, property.isUnicode());
      }

      if (property.getDefaultValue() != null) {
        json.writeStringField(DEFAULT_VALUE, property.getDefaultValue());
      }

      if (property.getMaxLength() != null) {
        json.writeNumberField(MAX_LENGTH, property.getMaxLength());
      }

      if (property.getPrecision() != null) {
        json.writeNumberField(PRECISION, property.getPrecision());
      }

      if (property.getScale() != null) {
        json.writeNumberField(SCALE, property.getScale());
      }
      
      if (property.getSrid() != null) {
          json.writeStringField(SRID, "" + property.getSrid());
      }

      appendAnnotations(json, property, null);
      json.writeEndObject();
    }
  }

  private void appendKey(final JsonGenerator json, 
      final EdmEntityType entityType) throws SerializerException, IOException {
    List<EdmKeyPropertyRef> keyPropertyRefs = entityType.getKeyPropertyRefs();
    if (keyPropertyRefs != null && !keyPropertyRefs.isEmpty()) {
      // Resolve Base Type key as it is shown in derived type
      EdmEntityType baseType = entityType.getBaseType();
      if (baseType != null && baseType.getKeyPropertyRefs() != null && !(baseType.getKeyPropertyRefs().isEmpty())) {
        return;
      }
      json.writeArrayFieldStart(KEY);
      for (EdmKeyPropertyRef keyRef : keyPropertyRefs) {
        
        if (keyRef.getAlias() != null) {
          json.writeStartObject();
          json.writeStringField(keyRef.getAlias(), keyRef.getName());
          json.writeEndObject();
        } else {
          json.writeString(keyRef.getName());
        }
      }
      json.writeEndArray();
    }
  }

  private String getAliasedFullQualifiedName(final EdmType type) {
    FullQualifiedName fqn = type.getFullQualifiedName();
    return getAliasedFullQualifiedName(fqn);
  }
  
  private void appendTypeDefinitions(final JsonGenerator json, 
      final List<EdmTypeDefinition> typeDefinitions) throws SerializerException, IOException {
    for (EdmTypeDefinition definition : typeDefinitions) {
      json.writeObjectFieldStart(definition.getName());
      // Section 11: "The type definition object MUST contain the member $Kind with a string value of
      // TypeDefinition" -- EdmTypeKind.DEFINITION.name() leaked a Java enum name into the wire format.
      json.writeStringField(KIND, TYPE_DEFINITION_KIND);
      json.writeStringField(UNDERLYING_TYPE, getFullQualifiedName(definition.getUnderlyingType()));
      
      // Facets, section 7.2: $MaxLength, $Precision and $Scale are numbers; $SRID stays a string
      // ("a string containing a number or the symbolic value variable").
      if (definition.getMaxLength() != null) {
        json.writeNumberField(MAX_LENGTH, definition.getMaxLength());
      }

      if (definition.getPrecision() != null) {
        json.writeNumberField(PRECISION, definition.getPrecision());
      }

      if (definition.getScale() != null) {
        json.writeNumberField(SCALE, definition.getScale());
      }
      
      if (definition.getSrid() != null) {
        json.writeStringField(SRID, "" + definition.getSrid());
      }

      appendAnnotations(json, definition, null);
      json.writeEndObject();
    }
  }

  private void appendEnumTypes(JsonGenerator json, List<EdmEnumType> enumTypes) 
      throws SerializerException, IOException {
    for (EdmEnumType enumType : enumTypes) {
      json.writeObjectFieldStart(enumType.getName());
      json.writeStringField(KIND, Kind.EnumType.name());
      // Section 10.2: absence of $IsFlags means false, and defaults are omitted (section 2.2).
      if (enumType.isFlags()) {
        json.writeBooleanField(IS_FLAGS, true);
      }
      json.writeStringField(UNDERLYING_TYPE, getFullQualifiedName(enumType.getUnderlyingType()));

      for (String memberName : enumType.getMemberNames()) {

        EdmMember member = enumType.getMember(memberName);
        if (member.getValue() != null) {
          // Section 10.3: "Each member MUST specify an associated numeric value." A value that is not
          // numeric is a broken model; failing is preferable to writing a string that no conformant
          // reader can use.
          json.writeNumberField(memberName, enumMemberValue(enumType, memberName, member.getValue()));
        }

        appendAnnotations(json, member, memberName);
      }
      json.writeEndObject();
    }
  }
  
  /**
   * Reads an enumeration member value as the numeric value section 10.3 requires it to be, reporting a
   * non-numeric one through this module's error contract instead of an unchecked
   * {@link NumberFormatException} that would escape the serializer uncaught.
   */
  private long enumMemberValue(final EdmEnumType enumType, final String memberName, final String value)
      throws SerializerException {
    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException e) {
      throw new SerializerException("The value '" + value + "' of enumeration member '" + memberName
          + "' of type '" + enumType.getFullQualifiedName().getFullQualifiedNameAsString()
          + "' is not numeric.", e, SerializerException.MessageKeys.WRONG_PROPERTY_VALUE,
          memberName, value);
    }
  }

  private void appendAnnotations(JsonGenerator json, 
      final EdmAnnotatable annotatable, String memberName) throws SerializerException, IOException {
    List<EdmAnnotation> annotations = annotatable.getAnnotations();
    if (annotations != null && !annotations.isEmpty()) {
      for (EdmAnnotation annotation : annotations) {
        String termName = memberName != null ? memberName : "";
        if (annotation.getTerm() != null) {
          termName += "@" + getAliasedFullQualifiedName(annotation.getTerm().getFullQualifiedName());
        } else if (annotation.getTermName() != null) {
          // The term's vocabulary is not part of the served metadata, so it cannot be resolved
          // to an EdmTerm; fall back to the raw term name so the member name stays valid.
          termName += "@" + annotation.getTermName();
        }
        if (annotation.getQualifier() != null) {
          termName += "#" + annotation.getQualifier();
        } 
        if (annotation.getExpression() == null && !termName.isEmpty()) {
          json.writeBooleanField(termName, true);
        } else {
          appendExpression(json, annotation.getExpression(), termName);
        }
        appendAnnotations(json, annotation, termName);
      }
    }
  }
  
  private void appendExpression(final JsonGenerator json,
      final EdmExpression expression, String termName) throws SerializerException, IOException {
    if (expression == null) {
      return;
    }
    if (expression.isConstant()) {
      appendConstantExpression(json, expression.asConstant(), termName);
    } else if (expression.isDynamic()) {
      appendDynamicExpression(json, expression.asDynamic(), termName);
    } else {
      throw new IllegalArgumentException("Unkown expressiontype in metadata");
    }
  }
  
  private void appendDynamicExpression(JsonGenerator json, 
      EdmDynamicExpression dynExp, String termName) throws SerializerException, IOException {
    if (termName != null) {
	  json.writeFieldName(termName);
    }
    switch (dynExp.getExpressionType()) {
    // Logical
    case And:
      appendLogicalOrComparisonExpression(json, dynExp.asAnd());
      break;
    case Or:
      appendLogicalOrComparisonExpression(json, dynExp.asOr());
      break;
    case Not:
      appendNotExpression(json, dynExp.asNot());
      break;
    // Comparison
    case Eq:
      appendLogicalOrComparisonExpression(json, dynExp.asEq());
      break;
    case Ne:
      appendLogicalOrComparisonExpression(json, dynExp.asNe());
      break;
    case Gt:
      appendLogicalOrComparisonExpression(json, dynExp.asGt());
      break;
    case Ge:
      appendLogicalOrComparisonExpression(json, dynExp.asGe());
      break;
    case Lt:
      appendLogicalOrComparisonExpression(json, dynExp.asLt());
      break;
    case Le:
      appendLogicalOrComparisonExpression(json, dynExp.asLe());
      break;
    case AnnotationPath:
      json.writeStartObject();
      json.writeStringField(ANNOTATION_PATH, dynExp.asAnnotationPath().getValue());
      json.writeEndObject();
      break;
    case Apply:
      EdmApply asApply = dynExp.asApply();
      json.writeStartObject();
      json.writeArrayFieldStart(DOLLAR + asApply.getExpressionName());
      for (EdmExpression parameter : asApply.getParameters()) {
        appendExpression(json, parameter, null);
      }
      json.writeEndArray();
      json.writeStringField(DOLLAR + Kind.Function.name(), asApply.getFunction());
      
      appendAnnotations(json, asApply, null);
      json.writeEndObject();
      break;
    case Cast:
      EdmCast asCast = dynExp.asCast();
      json.writeStartObject();
      appendExpression(json, asCast.getValue(), DOLLAR + asCast.getExpressionName());
      json.writeStringField(TYPE, getAliasedFullQualifiedName(asCast.getType()));
     
      if (asCast.getMaxLength() != null) {
        json.writeNumberField(MAX_LENGTH, asCast.getMaxLength());
      }

      if (asCast.getPrecision() != null) {
        json.writeNumberField(PRECISION, asCast.getPrecision());
      }

      if (asCast.getScale() != null) {
        json.writeNumberField(SCALE, asCast.getScale());
      }
      appendAnnotations(json, asCast, null);
      json.writeEndObject();
      break;
    case Collection:
      json.writeStartArray();
      for (EdmExpression item : dynExp.asCollection().getItems()) {
        appendExpression(json, item, null);
      }
      json.writeEndArray();
      break;
    case If:
      EdmIf asIf = dynExp.asIf();
      json.writeStartObject();
      json.writeArrayFieldStart(DOLLAR + asIf.getExpressionName());
      appendExpression(json, asIf.getGuard(), null);
      appendExpression(json, asIf.getThen(), null);
      appendExpression(json, asIf.getElse(), null);
      json.writeEndArray();
      appendAnnotations(json, asIf, null);
      json.writeEndObject();
      break;
    case IsOf:
      EdmIsOf asIsOf = dynExp.asIsOf();
      json.writeStartObject();
      appendExpression(json, asIsOf.getValue(), DOLLAR + asIsOf.getExpressionName());
      
      json.writeStringField(TYPE, getAliasedFullQualifiedName(asIsOf.getType()));

      if (asIsOf.getMaxLength() != null) {
        json.writeNumberField(MAX_LENGTH, asIsOf.getMaxLength());
      }

      if (asIsOf.getPrecision() != null) {
        json.writeNumberField(PRECISION, asIsOf.getPrecision());
      }

      if (asIsOf.getScale() != null) {
        json.writeNumberField(SCALE, asIsOf.getScale());
      }
      appendAnnotations(json, asIsOf, null);
      json.writeEndObject();
      break;
    case LabeledElement:
      EdmLabeledElement asLabeledElement = dynExp.asLabeledElement();
      json.writeStartObject();
      appendExpression(json, asLabeledElement.getValue(), DOLLAR + asLabeledElement.getExpressionName());
      json.writeStringField(NAME, asLabeledElement.getName());
      appendAnnotations(json, asLabeledElement, null);
      json.writeEndObject();
      break;
    case LabeledElementReference:
      EdmLabeledElementReference asLabeledElementReference = dynExp.asLabeledElementReference();
      json.writeStartObject();
      json.writeStringField(DOLLAR + asLabeledElementReference.getExpressionName(), 
          asLabeledElementReference.getValue());
      json.writeEndObject();
      break;
    case Null:
      EdmNull asNull = dynExp.asNull();
      json.writeStartObject();
      json.writeStringField(DOLLAR + asNull.getExpressionName(), null);
      appendAnnotations(json, dynExp.asNull(), null);
      json.writeEndObject();
      break;
    case NavigationPropertyPath:
      EdmNavigationPropertyPath asNavigationPropertyPath = dynExp.asNavigationPropertyPath();
      json.writeStartObject();
      json.writeStringField(DOLLAR + asNavigationPropertyPath.getExpressionName(), 
          asNavigationPropertyPath.getValue());
      json.writeEndObject();
      break;
    case Path:
      EdmPath asPath = dynExp.asPath();
      json.writeStartObject();
      json.writeStringField(DOLLAR + asPath.getExpressionName(), asPath.getValue());
      json.writeEndObject();
      break;
    case PropertyPath:
      EdmPropertyPath asPropertyPath = dynExp.asPropertyPath();
      json.writeStartObject();
      json.writeStringField(DOLLAR + asPropertyPath.getExpressionName(), asPropertyPath.getValue());
      json.writeEndObject();
      break;
    case Record:
      EdmRecord asRecord = dynExp.asRecord();
      json.writeStartObject();
      try {
        EdmStructuredType structuredType = asRecord.getType();
        if (structuredType != null) {
          // Section 14.4.12: "The type of a record expression is represented as the @type control
          // information" - there is no $Type member (and no $Record wrapper) on a record.
          json.writeStringField(AT_TYPE, "#" + getAliasedFullQualifiedName(structuredType));
        }
      } catch (EdmException e) {
        FullQualifiedName type = asRecord.getTypeFQN();
        if (type != null) {
          json.writeStringField(AT_TYPE, "#" + getAliasedFullQualifiedName(type));
        }
      }
      for (EdmPropertyValue propValue : asRecord.getPropertyValues()) {
        appendExpression(json, propValue.getValue(), propValue.getProperty());
        appendAnnotations(json, propValue, propValue.getProperty());
      }
      appendAnnotations(json, asRecord, null);
      json.writeEndObject();
      break;
    case UrlRef:
      EdmUrlRef asUrlRef = dynExp.asUrlRef();
      json.writeStartObject();
      appendExpression(json, asUrlRef.getValue(), DOLLAR + asUrlRef.getExpressionName());
      appendAnnotations(json, asUrlRef, null);
      json.writeEndObject();
      break;
    default:
      throw new IllegalArgumentException("Unkown ExpressionType for dynamic expression: " + dynExp.getExpressionType());
    }
  }

  private void appendNotExpression(final JsonGenerator json, final EdmNot exp) 
      throws SerializerException, IOException {
    json.writeStartObject();
    appendExpression(json, exp.getLeftExpression(), DOLLAR + exp.getExpressionName());
    appendAnnotations(json, exp, null);
    json.writeEndObject();
  }

  private void appendLogicalOrComparisonExpression(final JsonGenerator json, 
      final EdmLogicalOrComparisonExpression exp) throws SerializerException, IOException {
    json.writeStartObject();
    json.writeArrayFieldStart(DOLLAR + exp.getExpressionName());
    appendExpression(json, exp.getLeftExpression(), null);
    appendExpression(json, exp.getRightExpression(), null);
    json.writeEndArray();
    appendAnnotations(json, exp, null);
    json.writeEndObject();
  }

  private void appendConstantExpression(final JsonGenerator json,
      final EdmConstantExpression constExp, final String termName) throws SerializerException, IOException {
    if (termName != null && !termName.isEmpty()) {
      json.writeFieldName(termName);
    }
    final String value = constExp.getValueAsString();
    switch (constExp.getExpressionType()) {
    case Bool:
      // Section 14.3.2: "Boolean expressions are represented as the literals true or false."
      json.writeBoolean(Boolean.parseBoolean(value));
      break;
    case Int:
      writeIntegerConstant(json, value);
      break;
    case Decimal:
    case Float:
      writeNumericConstant(json, value);
      break;
    case Binary:
    case Date:
    case DateTimeOffset:
    case Duration:
    case EnumMember:
    case Guid:
    case String:
    case TimeOfDay:
      // Sections 14.3.1/.3/.4/.6/.7/.9/.11/.12: all of these are plain JSON strings. CSDL JSON has no
      // $Binary/$Date/$Int/... members at all - those are CSDL XML element names.
      json.writeString(value);
      break;
    default:
      throw new IllegalArgumentException("Unknown ExpressionType "
          + "for constant expression: " + constExp.getExpressionType());
    }
  }

  private void writeIntegerConstant(final JsonGenerator json, final String value) throws IOException {
    // Section 14.3.10: an integer is a JSON number unless IEEE754Compatible=true was requested, which
    // this service does not offer. An integer outside the IEEE-754 exactly-representable range would
    // lose precision as a JSON number, so it goes out as a string - the spec's own Example 55.
    try {
      final long parsed = Long.parseLong(value);
      if (parsed > MAX_SAFE_INTEGER || parsed < -MAX_SAFE_INTEGER) {
        json.writeString(value);
      } else {
        json.writeNumber(parsed);
      }
    } catch (NumberFormatException e) {
      json.writeString(value);
    }
  }

  private void writeNumericConstant(final JsonGenerator json, final String value) throws IOException {
    // Sections 14.3.5/14.3.8: "The special values INF, -INF, or NaN are represented as strings."
    if (INF.equals(value) || NEGATIVE_INF.equals(value) || NAN.equals(value)) {
      json.writeString(value);
    } else {
      try {
        json.writeNumber(new BigDecimal(value));
      } catch (NumberFormatException e) {
        // A value the model claims is numeric but is not: write it verbatim rather than corrupt it.
        json.writeString(value);
      }
    }
  }

  private String getAliasedFullQualifiedName(final FullQualifiedName fqn) {
    final String name;
    if (namespaceToAlias.get(fqn.getNamespace()) != null) {
      name = namespaceToAlias.get(fqn.getNamespace()) + "." + fqn.getName();
    } else {
      name = fqn.getFullQualifiedNameAsString();
    }

    return name;
  }
  
  private String getFullQualifiedName(final EdmType type) {
    return type.getFullQualifiedName().getFullQualifiedNameAsString();
  }

  private void appendReference(JsonGenerator json) throws SerializerException, IOException {
    json.writeObjectFieldStart(REFERENCES);
    for (final EdmxReference reference : serviceMetadata.getReferences()) {
      json.writeObjectFieldStart(reference.getUri().toASCIIString());

      List<EdmxReferenceInclude> includes = reference.getIncludes();
      if (!includes.isEmpty()) {
        appendIncludes(json, includes);
      }

      List<EdmxReferenceIncludeAnnotation> includeAnnotations = reference.getIncludeAnnotations();
      if (!includeAnnotations.isEmpty()) {
        appendIncludeAnnotations(json, includeAnnotations);
      }
      json.writeEndObject();
    }
    json.writeEndObject();
  }

  private void appendIncludeAnnotations(JsonGenerator json, 
      List<EdmxReferenceIncludeAnnotation> includeAnnotations) throws SerializerException, IOException {
    json.writeArrayFieldStart(INCLUDE_ANNOTATIONS);
    for (EdmxReferenceIncludeAnnotation includeAnnotation : includeAnnotations) {
      json.writeStartObject();
      json.writeStringField(TERM_NAMESPACE, includeAnnotation.getTermNamespace());
      if (includeAnnotation.getQualifier() != null) {
        json.writeStringField(QUALIFIER, includeAnnotation.getQualifier());
      }
      if (includeAnnotation.getTargetNamespace() != null) {
        json.writeStringField(TARGET_NAMESPACE, includeAnnotation.getTargetNamespace());
      }
      json.writeEndObject();
    }
    json.writeEndArray();
  }

  private void appendIncludes(JsonGenerator json, 
      List<EdmxReferenceInclude> includes) throws SerializerException, IOException {
   json.writeArrayFieldStart(INCLUDE);
   for (EdmxReferenceInclude include : includes) {
     json.writeStartObject();
     json.writeStringField(NAMESPACE, include.getNamespace());
     if (include.getAlias() != null) {
       namespaceToAlias.put(include.getNamespace(), include.getAlias());
       // Reference Aliases are ignored for now since they are not V2 compatible
       json.writeStringField(ALIAS, include.getAlias());
     }
     json.writeEndObject();
   }
   json.writeEndArray();
  }
}
