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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1 Task 10: CSDL JSON metadata round trip
 * against the technical service (OData 4.01, Part 3: CSDL JSON)
 */
package org.sitenetsoft.olinguito.fit.tecsvc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.EdmMetadataRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.JSONMetadataRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.EdmOperation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSchema;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;

/**
 * Proves the CSDL JSON metadata round trip end to end: the technical service writes its
 * {@code $metadata} as CSDL JSON, the client reads it, and the {@link Edm} that comes out is
 * equivalent to the one built from the CSDL XML representation of the very same model.
 */
public class MetadataJsonITCase extends AbstractTecSvcITCase {

  private static final String CORE_NAMESPACE = "Org.OData.Core.V1";

  @Override
  protected ContentType getContentType() {
    return ContentType.JSON;
  }

  /** The CSDL JSON document the technical service serves is readable by the client. */
  @Test
  public void readJsonMetadataDocument() {
    final JSONMetadataRequest request = getClient().getRetrieveRequestFactory()
        .getJSONMetadataRequest(SERVICE_URI);
    assertNotNull(request);
    setCookieHeader(request);

    final ODataRetrieveResponse<XMLMetadata> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final XMLMetadata metadata = response.getBody();
    assertNotNull(metadata);
    assertNotNull(metadata.getSchema(SERVICE_NAMESPACE));
    assertEquals(SERVICE_NAMESPACE, metadata.getSchema(SERVICE_NAMESPACE).getNamespace());
    assertEquals("Namespace1_Alias", metadata.getSchema(SERVICE_NAMESPACE).getAlias());
    assertNotNull(metadata.getSchema(SERVICE_NAMESPACE).getEntityContainer());
    assertEquals("Container", metadata.getSchema(SERVICE_NAMESPACE).getEntityContainer().getName());
    // The document declares its reference to the Core vocabulary even though the JSON request does
    // not follow it (see the guide's "Known limitations").
    assertFalse(metadata.getReferences().isEmpty());
  }

  /**
   * Closed pin for [OData-Protocol] section 11.1.2: a request that expresses no format preference
   * still gets - and still asks for - the XML representation.
   */
  @Test
  public void defaultMetadataFormatIsStillXml() {
    final ODataClient client = getClient();
    assertEquals(ContentType.APPLICATION_XML, client.getConfiguration().getMetadataFormat());

    final Edm edm = client.getRetrieveRequestFactory().getMetadataRequest(SERVICE_URI).execute().getBody();
    assertNotNull(edm);
    assertNotNull(edm.getEntityContainer());
    // The XML path follows $Reference, so the Core vocabulary schema is there.
    assertNotNull(edm.getSchema(CORE_NAMESPACE));
  }

  /** The configured metadata format drives the convenience {@code Edm} request. */
  @Test
  public void configuredJsonFormatYieldsAUsableEdm() {
    final Edm edm = jsonEdm();
    assertNotNull(edm);

    final EdmSchema schema = edm.getSchema(SERVICE_NAMESPACE);
    assertNotNull(schema);
    assertEquals("Namespace1_Alias", schema.getAlias());
    assertFalse(schema.getEntityTypes().isEmpty());
    assertFalse(schema.getComplexTypes().isEmpty());
    assertFalse(schema.getEnumTypes().isEmpty());
    assertFalse(schema.getTypeDefinitions().isEmpty());
    assertFalse(schema.getActions().isEmpty());
    assertFalse(schema.getFunctions().isEmpty());

    final EdmEntityContainer container = edm.getEntityContainer(
        new FullQualifiedName(SERVICE_NAMESPACE, "Container"));
    assertNotNull(container);
    assertNotNull(container.getEntitySet("ESAllPrim"));
    assertEquals(SERVICE_NAMESPACE, container.getEntitySet("ESAllPrim").getEntityType().getNamespace());
    assertNotNull(container.getSingleton("SIMedia"));
    assertNotNull(container.getActionImport("AIRTString"));
    assertNotNull(container.getFunctionImport("FICRTCollCTTwoPrim"));
  }

  /** Every entity type, with its properties, facets and navigation properties, survives the format. */
  @Test
  public void entityTypesAreEquivalent() {
    final List<String> fromXml = entityTypes(xmlEdm());
    final List<String> fromJson = entityTypes(jsonEdm());

    assertFalse(fromXml.isEmpty());
    assertEquals(fromXml.size(), fromJson.size());
    for (int i = 0; i < fromXml.size(); i++) {
      assertEquals(fromXml.get(i), fromJson.get(i));
    }
  }

  /** Every complex type, with its properties, facets and navigation properties, survives the format. */
  @Test
  public void complexTypesAreEquivalent() {
    final List<String> fromXml = complexTypes(xmlEdm());
    final List<String> fromJson = complexTypes(jsonEdm());

    assertFalse(fromXml.isEmpty());
    assertEquals(fromXml.size(), fromJson.size());
    for (int i = 0; i < fromXml.size(); i++) {
      assertEquals(fromXml.get(i), fromJson.get(i));
    }
  }

  /** Enum types keep their underlying type, their flags marker and every member name and value. */
  @Test
  public void enumTypesAndTypeDefinitionsAreEquivalent() {
    final Edm xml = xmlEdm();
    final Edm json = jsonEdm();

    final List<String> xmlEnums = enumTypes(xml);
    assertFalse(xmlEnums.isEmpty());
    assertEquals(xmlEnums, enumTypes(json));

    final List<String> xmlTypeDefinitions = typeDefinitions(xml);
    assertFalse(xmlTypeDefinitions.isEmpty());
    assertEquals(xmlTypeDefinitions, typeDefinitions(json));
  }

  /** Actions and functions keep their overloads, binding, parameters and return types. */
  @Test
  public void actionsAndFunctionsAreEquivalent() {
    final List<String> xmlOperations = operations(xmlEdm());
    final List<String> jsonOperations = operations(jsonEdm());

    assertFalse(xmlOperations.isEmpty());
    assertEquals(xmlOperations.size(), jsonOperations.size());
    for (int i = 0; i < xmlOperations.size(); i++) {
      assertEquals(xmlOperations.get(i), jsonOperations.get(i));
    }
  }

  /** The entity container - sets, singletons, imports and navigation property bindings - is equivalent. */
  @Test
  public void entityContainerIsEquivalent() {
    final Edm xml = xmlEdm();
    final Edm json = jsonEdm();

    assertEquals(xml.getEntityContainer().getFullQualifiedName(),
        json.getEntityContainer().getFullQualifiedName());

    final List<String> xmlContainer = container(xml);
    final List<String> jsonContainer = container(json);
    assertFalse(xmlContainer.isEmpty());
    assertEquals(xmlContainer.size(), jsonContainer.size());
    for (int i = 0; i < xmlContainer.size(); i++) {
      assertEquals(xmlContainer.get(i), jsonContainer.get(i));
    }
  }

  /**
   * The known limitation, pinned so it cannot regress silently: the JSON request does not follow
   * {@code $Reference}, so the referenced vocabulary schema is absent from the JSON-built model even
   * though the XML-built model has it.
   */
  @Test
  public void jsonMetadataDoesNotFollowReferences() {
    assertNotNull(xmlEdm().getSchema(CORE_NAMESPACE));

    final XMLMetadata metadata = getClient().getRetrieveRequestFactory()
        .getJSONMetadataRequest(SERVICE_URI).execute().getBody();
    assertFalse(metadata.getReferences().isEmpty());
    assertFalse(metadata.getSchemas().isEmpty());
    for (final CsdlSchema schema : metadata.getSchemas()) {
      assertEquals(SERVICE_NAMESPACE, schema.getNamespace());
    }
  }

  private Edm xmlEdm() {
    final ODataClient client = getClient();
    final EdmMetadataRequest request = client.getRetrieveRequestFactory().getMetadataRequest(SERVICE_URI);
    setCookieHeader(request);
    final ODataRetrieveResponse<Edm> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    return response.getBody();
  }

  private Edm jsonEdm() {
    final ODataClient client = getClient();
    client.getConfiguration().setMetadataFormat(ContentType.APPLICATION_JSON);
    final EdmMetadataRequest request = client.getRetrieveRequestFactory().getMetadataRequest(SERVICE_URI);
    setCookieHeader(request);
    final ODataRetrieveResponse<Edm> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    return response.getBody();
  }

  private static List<String> entityTypes(final Edm edm) {
    final List<String> signatures = new ArrayList<String>();
    for (final EdmEntityType type : edm.getSchema(SERVICE_NAMESPACE).getEntityTypes()) {
      signatures.add(structuredType(type)
          + " key=" + type.getKeyPredicateNames()
          + " hasStream=" + type.hasStream());
    }
    Collections.sort(signatures);
    return signatures;
  }

  private static List<String> complexTypes(final Edm edm) {
    final List<String> signatures = new ArrayList<String>();
    for (final EdmComplexType type : edm.getSchema(SERVICE_NAMESPACE).getComplexTypes()) {
      signatures.add(structuredType(type));
    }
    Collections.sort(signatures);
    return signatures;
  }

  private static String structuredType(final EdmStructuredType type) {
    final StringBuilder builder = new StringBuilder(type.getFullQualifiedName().getFullQualifiedNameAsString());
    builder.append(" abstract=").append(type.isAbstract());
    builder.append(" open=").append(type.isOpenType());
    builder.append(" base=").append(type.getBaseType() == null ? "-"
        : type.getBaseType().getFullQualifiedName().getFullQualifiedNameAsString());
    final List<String> properties = new ArrayList<String>();
    for (final String name : type.getPropertyNames()) {
      properties.add(property(name, type.getStructuralProperty(name)));
    }
    Collections.sort(properties);
    builder.append(" properties=").append(properties);
    final List<String> navigationProperties = new ArrayList<String>();
    for (final String name : type.getNavigationPropertyNames()) {
      navigationProperties.add(navigationProperty(name, type.getNavigationProperty(name)));
    }
    Collections.sort(navigationProperties);
    builder.append(" navigationProperties=").append(navigationProperties);
    return builder.toString();
  }

  private static String property(final String name, final EdmProperty property) {
    return name + ":" + property.getType().getFullQualifiedName().getFullQualifiedNameAsString()
        + (property.isCollection() ? "[]" : "")
        + " nullable=" + property.isNullable()
        + " maxLength=" + property.getMaxLength()
        + " precision=" + property.getPrecision()
        + " scale=" + property.getScale()
        + " unicode=" + property.isUnicode()
        + " defaultValue=" + property.getDefaultValue();
  }

  private static String navigationProperty(final String name, final EdmNavigationProperty property) {
    return name + ":" + property.getType().getFullQualifiedName().getFullQualifiedNameAsString()
        + (property.isCollection() ? "[]" : "")
        + " nullable=" + property.isNullable()
        + " containsTarget=" + property.containsTarget()
        + " partner=" + (property.getPartner() == null ? "-" : property.getPartner().getName())
        + " onDelete=" + (property.getOnDelete() == null ? "-" : property.getOnDelete().getAction())
        + " constraints=" + property.getReferentialConstraints().size();
  }

  private static List<String> enumTypes(final Edm edm) {
    final List<String> signatures = new ArrayList<String>();
    for (final EdmEnumType type : edm.getSchema(SERVICE_NAMESPACE).getEnumTypes()) {
      final StringBuilder builder = new StringBuilder(
          type.getFullQualifiedName().getFullQualifiedNameAsString());
      builder.append(" underlying=")
          .append(type.getUnderlyingType().getFullQualifiedName().getFullQualifiedNameAsString());
      builder.append(" isFlags=").append(type.isFlags());
      for (final String member : type.getMemberNames()) {
        builder.append(' ').append(member).append('=').append(type.getMember(member).getValue());
      }
      signatures.add(builder.toString());
    }
    Collections.sort(signatures);
    return signatures;
  }

  private static List<String> typeDefinitions(final Edm edm) {
    final List<String> signatures = new ArrayList<String>();
    for (final EdmTypeDefinition definition : edm.getSchema(SERVICE_NAMESPACE).getTypeDefinitions()) {
      signatures.add(definition.getFullQualifiedName().getFullQualifiedNameAsString()
          + " underlying="
          + definition.getUnderlyingType().getFullQualifiedName().getFullQualifiedNameAsString()
          + " maxLength=" + definition.getMaxLength()
          + " precision=" + definition.getPrecision()
          + " scale=" + definition.getScale()
          + " unicode=" + definition.isUnicode());
    }
    Collections.sort(signatures);
    return signatures;
  }

  private static List<String> operations(final Edm edm) {
    final List<String> signatures = new ArrayList<String>();
    final EdmSchema schema = edm.getSchema(SERVICE_NAMESPACE);
    for (final EdmOperation operation : schema.getActions()) {
      signatures.add("Action " + operation(operation));
    }
    for (final EdmOperation operation : schema.getFunctions()) {
      signatures.add("Function " + operation(operation));
    }
    Collections.sort(signatures);
    return signatures;
  }

  private static String operation(final EdmOperation operation) {
    final StringBuilder builder = new StringBuilder(
        operation.getFullQualifiedName().getFullQualifiedNameAsString());
    builder.append(" bound=").append(operation.isBound());
    builder.append(" entitySetPath=").append(operation.getEntitySetPath());
    for (final String name : operation.getParameterNames()) {
      final EdmParameter parameter = operation.getParameter(name);
      builder.append(" (").append(name).append(':')
          .append(parameter.getType().getFullQualifiedName().getFullQualifiedNameAsString())
          .append(parameter.isCollection() ? "[]" : "")
          .append(" nullable=").append(parameter.isNullable())
          .append(" maxLength=").append(parameter.getMaxLength())
          .append(" precision=").append(parameter.getPrecision())
          .append(" scale=").append(parameter.getScale())
          .append(')');
    }
    final EdmReturnType returnType = operation.getReturnType();
    if (returnType == null) {
      builder.append(" returns=-");
    } else {
      builder.append(" returns=")
          .append(returnType.getType().getFullQualifiedName().getFullQualifiedNameAsString())
          .append(returnType.isCollection() ? "[]" : "");
      // [OData-CSDLJSON] section 12.8: "If the return type is a collection of entity types, the
      // $Nullable member has no meaning and MUST NOT be specified." The value is therefore not on the
      // CSDL JSON wire at all and cannot be compared; every other return type carries it.
      if (!(returnType.isCollection() && EdmTypeKind.ENTITY == returnType.getType().getKind())) {
        builder.append(" nullable=").append(returnType.isNullable());
      }
    }
    return builder.toString();
  }

  private static List<String> container(final Edm edm) {
    final List<String> signatures = new ArrayList<String>();
    final EdmEntityContainer container = edm.getEntityContainer(
        new FullQualifiedName(SERVICE_NAMESPACE, "Container"));
    for (final EdmEntitySet entitySet : container.getEntitySets()) {
      signatures.add("EntitySet " + entitySet.getName()
          + " type=" + entityTypeName(entitySet)
          + " inServiceDocument=" + entitySet.isIncludeInServiceDocument()
          + " bindings=" + bindings(entitySet.getNavigationPropertyBindings()));
    }
    for (final EdmSingleton singleton : container.getSingletons()) {
      signatures.add("Singleton " + singleton.getName()
          + " type=" + entityTypeName(singleton)
          + " bindings=" + bindings(singleton.getNavigationPropertyBindings()));
    }
    for (final EdmActionImport actionImport : container.getActionImports()) {
      signatures.add("ActionImport " + actionImport.getName()
          + " action=" + actionImport.getUnboundAction().getFullQualifiedName()
              .getFullQualifiedNameAsString());
    }
    for (final EdmFunctionImport functionImport : container.getFunctionImports()) {
      // getFunctionFqn() hands back the name as the document spelled it, and the CSDL JSON reader
      // resolves $Alias at parse time while the CSDL XML one does not, so the resolved overloads are
      // compared instead of the raw name.
      final List<String> overloads = new ArrayList<String>();
      for (final EdmFunction function : functionImport.getUnboundFunctions()) {
        overloads.add(function.getFullQualifiedName().getFullQualifiedNameAsString()
            + function.getParameterNames());
      }
      Collections.sort(overloads);
      signatures.add("FunctionImport " + functionImport.getName()
          + " function=" + overloads
          + " inServiceDocument=" + functionImport.isIncludeInServiceDocument());
    }
    Collections.sort(signatures);
    return signatures;
  }

  /**
   * The technical service declares the entity set {@code ESPeople} but never declares its entity type
   * {@code ETPeople} in the schema, so neither representation can resolve it. The marker is compared
   * instead of the message, because the CSDL JSON reader resolves {@code $Alias} at parse time and the
   * CSDL XML one does not, so the two unresolvable names are spelled differently.
   */
  private static String entityTypeName(final EdmBindingTarget bindingTarget) {
    try {
      return bindingTarget.getEntityType().getFullQualifiedName().getFullQualifiedNameAsString();
    } catch (final EdmException e) {
      return "<undeclared entity type>";
    }
  }

  /**
   * [OData-CSDLJSON] section 13.4.2 represents the navigation property bindings as one object keyed by
   * binding path, so a model that declares the same path twice - as the technical service does for
   * {@code ESKeyNav} - can only carry it once in CSDL JSON. The duplicate is dropped on both sides.
   */
  private static List<String> bindings(final List<EdmNavigationPropertyBinding> navigationPropertyBindings) {
    final Set<String> bindings = new TreeSet<String>();
    for (final EdmNavigationPropertyBinding binding : navigationPropertyBindings) {
      bindings.add(binding.getPath() + "->" + binding.getTarget());
    }
    return new ArrayList<String>(bindings);
  }
}
