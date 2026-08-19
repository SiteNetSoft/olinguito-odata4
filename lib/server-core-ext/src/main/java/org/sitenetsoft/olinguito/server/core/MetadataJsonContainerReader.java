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
 * Copyright 2026 SiteNetSoft - Added the CSDL JSON operation and entity container reader
 */
package org.sitenetsoft.olinguito.server.core;

import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.AT;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.COLLECTION;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.DOLLAR;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.EDM_STRING;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.KIND;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.NULLABLE;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.TYPE;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.arrayNode;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.child;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.flag;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.objectNode;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.requireText;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOperation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;

/**
 * Reads the halves of a CSDL JSON document that describe behaviour rather than structure: the action
 * and function overload arrays (OData CSDL JSON section 12) and the entity container with its entity
 * sets, singletons, action imports and function imports (section 13).
 * <p>
 * It is a collaborator of {@link MetadataJsonParser}, which keeps ownership of the document walk and
 * hands over the schema members it does not read itself. Everything shared - alias resolution, the
 * facet reader, the error paths and the JSON accessors - comes from the parser.
 * <p>
 * Besides the conformant shape this reader also accepts the shapes the pre-conformance Olinguito JSON
 * writer produced. Those are tolerated on input only; nothing here is ever written back.
 */
final class MetadataJsonContainerReader {

  private static final String IS_BOUND = DOLLAR + "IsBound";
  private static final String IS_COMPOSABLE = DOLLAR + "IsComposable";
  private static final String ENTITY_SET_PATH = DOLLAR + "EntitySetPath";
  private static final String RETURN_TYPE = DOLLAR + "ReturnType";
  private static final String PARAMETER = DOLLAR + "Parameter";
  private static final String NAME = DOLLAR + "Name";
  private static final String EXTENDS = DOLLAR + "Extends";
  private static final String INCLUDE_IN_SERVICE_DOCUMENT = DOLLAR + "IncludeInServiceDocument";
  private static final String NAVIGATION_PROPERTY_BINDING = DOLLAR + "NavigationPropertyBinding";
  private static final String ACTION = DOLLAR + "Action";
  private static final String FUNCTION = DOLLAR + "Function";
  private static final String ENTITY_SET = DOLLAR + "EntitySet";

  private static final String KIND_ACTION = "Action";
  private static final String KIND_FUNCTION = "Function";
  private static final String KIND_ENTITY_SET = "EntitySet";
  private static final String KIND_ENTITY_CONTAINER = "EntityContainer";
  /** The member the pre-conformance writer used to nest the container's inheritance in. */
  private static final String LEGACY_EXTENDING = "Extending";

  private final MetadataJsonParser parser;

  /** The schema the entity container currently in the model was installed into, if any. */
  private CsdlSchema containerOwner;

  MetadataJsonContainerReader(final MetadataJsonParser parser) {
    this.parser = parser;
  }

  /** Forgets the container of the previous document; called once per document walk. */
  void reset() {
    this.containerOwner = null;
  }

  // ---------------------------------------------------------------- section 12: actions, functions

  /**
   * Sections 12.2 and 12.4: an action or function is a schema member whose value is an array with one
   * object per overload. The overload's own $Kind discriminates the two, so a single member may not
   * mix them - but each item is validated on its own so the error names the offending item.
   */
  void readOverloads(final CsdlSchema schema, final String name, final ArrayNode node, final String path)
      throws CsdlJsonParseException {
    for (int i = 0; i < node.size(); i++) {
      overloadKind(node, i, path); // validates every item before anything is added to the schema
    }
    schema.getActions().addAll(readActionOverloads(name, node, path));
    schema.getFunctions().addAll(readFunctionOverloads(name, node, path));
  }

  List<CsdlAction> readActionOverloads(final String name, final ArrayNode node, final String path)
      throws CsdlJsonParseException {
    final List<CsdlAction> actions = new ArrayList<>();
    for (int i = 0; i < node.size(); i++) {
      if (!KIND_ACTION.equals(overloadKind(node, i, path))) {
        continue;
      }
      final String itemPath = item(path, i);
      final CsdlAction action = new CsdlAction();
      action.setName(name);
      readOperation(action, objectNode(node.get(i), itemPath), itemPath);
      actions.add(action);
    }
    return actions;
  }

  List<CsdlFunction> readFunctionOverloads(final String name, final ArrayNode node, final String path)
      throws CsdlJsonParseException {
    final List<CsdlFunction> functions = new ArrayList<>();
    for (int i = 0; i < node.size(); i++) {
      if (!KIND_FUNCTION.equals(overloadKind(node, i, path))) {
        continue;
      }
      final String itemPath = item(path, i);
      final ObjectNode overload = objectNode(node.get(i), itemPath);
      final CsdlFunction function = new CsdlFunction();
      function.setName(name);
      // Section 12.7: "If not explicitly indicated, it is not composable."
      function.setComposable(flag(overload, IS_COMPOSABLE));
      readOperation(function, overload, itemPath);
      if (function.getReturnType() == null) {
        // Section 12.4: a function overload "MUST contain the member $ReturnType" - unlike an action.
        throw new CsdlJsonParseException(child(itemPath, RETURN_TYPE),
            "a function overload must declare its $ReturnType");
      }
      functions.add(function);
    }
    return functions;
  }

  void readOperation(final CsdlOperation operation, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    // Section 12.5: "Absence of the member means false."
    operation.setBound(flag(node, IS_BOUND));
    // Section 12.6: stored as written; it is split into binding parameter and segments at resolution
    // time, exactly as the CSDL XML parser leaves it.
    operation.setEntitySetPath(text(node, ENTITY_SET_PATH, null));
    operation.setParameters(node.has(PARAMETER)
        ? readParameters(arrayNode(node, PARAMETER, path), child(path, PARAMETER))
        : new ArrayList<>());
    if (node.has(RETURN_TYPE)) {
      operation.setReturnType(readReturnType(objectNode(node, RETURN_TYPE, path), child(path, RETURN_TYPE)));
    }
  }

  /** Section 12.9: an ordered array of objects, each carrying its own $Name. */
  List<CsdlParameter> readParameters(final ArrayNode node, final String path) throws CsdlJsonParseException {
    final List<CsdlParameter> parameters = new ArrayList<>();
    for (int i = 0; i < node.size(); i++) {
      final String itemPath = item(path, i);
      final ObjectNode item = objectNode(node.get(i), itemPath);
      final CsdlParameter parameter = new CsdlParameter();
      parameter.setName(requireText(item, NAME, itemPath));
      // "Absence of the $Type member means the type is Edm.String."
      parameter.setType(this.parser.resolveName(text(item, TYPE, EDM_STRING)));
      parameter.setCollection(flag(item, COLLECTION));
      parameter.setNullable(flag(item, NULLABLE));
      this.parser.readFacets(item, facets(parameter), itemPath);
      parameters.add(parameter);
    }
    return parameters;
  }

  /** Section 12.8: $Type defaults to Edm.String and $Nullable to false, plus the section 7.2 facets. */
  CsdlReturnType readReturnType(final ObjectNode node, final String path) throws CsdlJsonParseException {
    final CsdlReturnType returnType = new CsdlReturnType();
    returnType.setType(this.parser.resolveName(text(node, TYPE, EDM_STRING)));
    returnType.setCollection(flag(node, COLLECTION));
    returnType.setNullable(flag(node, NULLABLE));
    this.parser.readFacets(node, facets(returnType), path);
    return returnType;
  }

  // ---------------------------------------------------------------------- section 13: the container

  /**
   * Installs the entity container of a schema member whose $Kind is EntityContainer. Section 13 allows
   * exactly one entity container per metadata document; when a document declares more than one, the
   * document's $EntityContainer member says which one is the service's, and the others are dropped.
   * Without that member two containers cannot be told apart, which is an error.
   */
  void readContainer(final CsdlSchema schema, final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final String qualifiedName = schema.getNamespace() + "." + name;
    final String service = this.parser.resolveName(this.parser.getServiceContainerFqn());
    if (this.containerOwner != null) {
      if (service == null) {
        throw new CsdlJsonParseException(path,
            "a metadata document defines exactly one entity container; $EntityContainer names it");
      }
      if (!service.equals(qualifiedName)) {
        return; // not the service's container, and one is already in the model
      }
      this.containerOwner.setEntityContainer(null);
    }
    schema.setEntityContainer(readEntityContainer(name, node, path));
    this.containerOwner = schema;
  }

  CsdlEntityContainer readEntityContainer(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlEntityContainer container = new CsdlEntityContainer();
    container.setName(name);
    container.setEntitySets(new ArrayList<>());
    container.setSingletons(new ArrayList<>());
    container.setActionImports(new ArrayList<>());
    container.setFunctionImports(new ArrayList<>());
    if (node.has(EXTENDS)) {
      // Section 13.1: "the qualified name of the entity container to be extended".
      container.setExtendsContainer(this.parser.resolveName(requireText(node, EXTENDS, path)));
    }
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String memberName = member.getKey();
      if (memberName.startsWith(DOLLAR) || memberName.contains(AT)) {
        continue; // $Kind, $Extends and annotations on the container or on one of its children
      }
      final String memberPath = child(path, memberName);
      final ObjectNode value = objectNode(member.getValue(), memberPath);
      readContainerMember(container, memberName, value, memberPath);
    }
    return container;
  }

  private void readContainerMember(final CsdlEntityContainer container, final String name,
      final ObjectNode node, final String path) throws CsdlJsonParseException {
    final String kind = text(node, KIND, null);
    if (node.has(ACTION)) {
      container.getActionImports().add(readActionImport(name, node, path));
    } else if (node.has(FUNCTION)) {
      container.getFunctionImports().add(readFunctionImport(name, node, path));
    } else if (node.has(COLLECTION) || KIND_ENTITY_SET.equals(kind)) {
      // Section 13.2: "The entity set object MUST contain the members $Collection and $Type." The
      // pre-conformance writer omitted $Collection and wrote "$Kind": "EntitySet" instead.
      container.getEntitySets().add(readEntitySet(name, node, path));
    } else if (node.has(TYPE)) {
      // Section 13.3: a singleton has a $Type and no $Collection.
      container.getSingletons().add(readSingleton(name, node, path));
    } else if (KIND_ENTITY_CONTAINER.equals(kind) || LEGACY_EXTENDING.equals(name)) {
      // The pre-conformance writer nested the container's inheritance in an "Extending" object.
      if (node.has(EXTENDS)) {
        container.setExtendsContainer(this.parser.resolveName(requireText(node, EXTENDS, path)));
      }
    } else {
      throw new CsdlJsonParseException(path,
          "an entity container member is an entity set, a singleton, an action import or a function import");
    }
  }

  CsdlEntitySet readEntitySet(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlEntitySet entitySet = new CsdlEntitySet();
    entitySet.setName(name);
    entitySet.setType(this.parser.resolveName(requireText(node, TYPE, path)));
    // Section 13.2: "Absence of the member means true."
    entitySet.setIncludeInServiceDocument(!node.has(INCLUDE_IN_SERVICE_DOCUMENT)
        || flag(node, INCLUDE_IN_SERVICE_DOCUMENT));
    entitySet.setNavigationPropertyBindings(readNavigationPropertyBindings(node, path));
    return entitySet;
  }

  CsdlSingleton readSingleton(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlSingleton singleton = new CsdlSingleton();
    singleton.setName(name);
    singleton.setType(this.parser.resolveName(requireText(node, TYPE, path)));
    // Section 13.3 also allows $Nullable on a 4.01 singleton; CsdlSingleton has no place for it, and
    // the CSDL XML parser drops the equivalent attribute too.
    singleton.setNavigationPropertyBindings(readNavigationPropertyBindings(node, path));
    return singleton;
  }

  CsdlActionImport readActionImport(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlActionImport actionImport = new CsdlActionImport();
    actionImport.setName(name);
    // Section 13.5: "The value of $Action is a string containing the qualified name of an unbound
    // action."
    actionImport.setAction(this.parser.resolveName(requireText(node, ACTION, path)));
    if (node.has(ENTITY_SET)) {
      actionImport.setEntitySet(unqualified(requireText(node, ENTITY_SET, path)));
    }
    return actionImport;
  }

  CsdlFunctionImport readFunctionImport(final String name, final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final CsdlFunctionImport functionImport = new CsdlFunctionImport();
    functionImport.setName(name);
    functionImport.setFunction(this.parser.resolveName(requireText(node, FUNCTION, path)));
    if (node.has(ENTITY_SET)) {
      functionImport.setEntitySet(unqualified(requireText(node, ENTITY_SET, path)));
    }
    // Section 13.6: "If not explicitly indicated, it is not included." - the opposite of section 13.2.
    functionImport.setIncludeInServiceDocument(flag(node, INCLUDE_IN_SERVICE_DOCUMENT));
    return functionImport;
  }

  /**
   * Section 13.4.2: "an object ... whose name is the navigation property binding path and whose value
   * is a string containing the navigation property binding target". Both are paths, so neither is
   * alias-resolved.
   */
  List<CsdlNavigationPropertyBinding> readNavigationPropertyBindings(final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    final List<CsdlNavigationPropertyBinding> bindings = new ArrayList<>();
    if (!node.has(NAVIGATION_PROPERTY_BINDING)) {
      return bindings;
    }
    final String bindingPath = child(path, NAVIGATION_PROPERTY_BINDING);
    final ObjectNode value = objectNode(node, NAVIGATION_PROPERTY_BINDING, path);
    final Iterator<Map.Entry<String, JsonNode>> members = value.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      if (member.getKey().contains(AT)) {
        continue; // "<path>@<term>" annotates the binding of that path
      }
      if (!member.getValue().isTextual()) {
        throw new CsdlJsonParseException(child(bindingPath, member.getKey()),
            "a navigation property binding maps a path to the name or path of its target");
      }
      bindings.add(new CsdlNavigationPropertyBinding()
          .setPath(member.getKey())
          .setTarget(member.getValue().asText()));
    }
    return bindings;
  }

  /**
   * Sections 13.5/13.6: $EntitySet is "either the unqualified name of an entity set in the same entity
   * container or a path to an entity set in a different entity container". The model keeps the
   * unqualified name, so a schema-alias prefix - which the pre-conformance writer emitted - is
   * dropped. A target path keeps both of its segments; only its container half carries the dot.
   */
  static String unqualified(final String target) {
    if (target.indexOf('/') >= 0) {
      return target;
    }
    return target.substring(target.lastIndexOf('.') + 1);
  }

  // ------------------------------------------------------------------------------------- plumbing

  /**
   * Sections 12.2/12.4: "The action overload object MUST contain the member $Kind with a string value
   * of Action" and likewise Function.
   */
  private static String overloadKind(final ArrayNode node, final int index, final String path)
      throws CsdlJsonParseException {
    final String itemPath = item(path, index);
    final String kind = text(objectNode(node.get(index), itemPath), KIND, null);
    if (!KIND_ACTION.equals(kind) && !KIND_FUNCTION.equals(kind)) {
      throw new CsdlJsonParseException(child(itemPath, KIND),
          "an overload object must carry $Kind with the value Action or Function");
    }
    return kind;
  }

  private static String item(final String path, final int index) {
    return path + "[" + index + "]";
  }

  private static MetadataJsonParser.FacetSink facets(final CsdlParameter parameter) {
    return new MetadataJsonParser.FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        parameter.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        parameter.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        parameter.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        parameter.setSrid(value);
      }
    };
  }

  private static MetadataJsonParser.FacetSink facets(final CsdlReturnType returnType) {
    return new MetadataJsonParser.FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        returnType.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        returnType.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        returnType.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        returnType.setSrid(value);
      }
    };
  }
}
