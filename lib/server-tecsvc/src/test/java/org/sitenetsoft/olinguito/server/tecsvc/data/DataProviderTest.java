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
 * Copyright 2026 SiteNetSoft - OData 4.01: default values of omitted optional function parameters
 * Copyright 2026 SiteNetSoft - OData 4.01: resolve entities through alternate keys
 * Copyright 2026 SiteNetSoft - OData 4.01: bound actions through alternate keys
 * Copyright 2026 SiteNetSoft - Empty key lists address no entity; first entity selected explicitly
 * Copyright 2026 SiteNetSoft - OData 4.01: referential-constraint key predicates from the source entity
 * Copyright 2026 SiteNetSoft - OData 4.01: malformed optional-parameter default values are rejected with 400
 * Copyright 2026 SiteNetSoft - OData 4.01: ETGeo/ESGeo geospatial reference model
 * Copyright 2026 SiteNetSoft - OData 4.01: entity-typed action parameters and entity references
 */
package org.sitenetsoft.olinguito.server.tecsvc.data;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Parameter;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.UriParameter;
import org.sitenetsoft.olinguito.server.core.uri.UriParameterImpl;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.SchemaProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DataProviderTest {

  private final OData oData = OData.newInstance();
  private final Edm edm =
      oData.createServiceMetadata(new EdmTechProvider(), Collections.emptyList())
      .getEdm();
  private final EdmEntityContainer entityContainer = edm.getEntityContainer();

  private final EdmEntitySet esAllPrim = entityContainer.getEntitySet("ESAllPrim");
  private final EdmEntitySet esAllKey = entityContainer.getEntitySet("ESAllKey");
  private final EdmEntitySet esCompAllPrim = entityContainer.getEntitySet("ESCompAllPrim");
  private final EdmEntitySet esCollAllPrim = entityContainer.getEntitySet("ESCollAllPrim");
  private final EdmEntitySet esMixPrimCollComp = entityContainer.getEntitySet("ESMixPrimCollComp");
  private final EdmEntitySet esMedia = entityContainer.getEntitySet("ESMedia");
  private final EdmEntitySet esGeo = entityContainer.getEntitySet("ESGeo");

  @Test
  void esAllPrimEntity() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity entity = dataProvider.readAll(esAllPrim).getEntities().get(2);
    Assertions.assertEquals(16, entity.getProperties().size());

    Assertions.assertEquals(entity,
        dataProvider.read(esAllPrim, List.of(mockParameter("PropertyInt16", "-0"))));
  }

  @Test
  void esAllKeyEntity() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity entity = dataProvider.readAll(esAllKey).getEntities().get(0);
    Assertions.assertEquals(13, entity.getProperties().size());

    Assertions.assertEquals(entity, dataProvider.read(esAllKey, List.of(
        mockParameter("PropertyBoolean", "true"),
        mockParameter("PropertyByte", "255"),
        mockParameter("PropertyDate", "2012-12-03"),
        mockParameter("PropertyDateTimeOffset", "2012-12-03T07:16:23Z"),
        mockParameter("PropertyDecimal", "34"),
        mockParameter("PropertyDuration", "duration'PT6S'"),
        mockParameter("PropertyGuid", "01234567-89AB-CDEF-0123-456789ABCDEF"),
        mockParameter("PropertyInt16", "32767"),
        mockParameter("PropertyInt32", "2147483647"),
        mockParameter("PropertyInt64", "9223372036854775807"),
        mockParameter("PropertySByte", "127"),
        mockParameter("PropertyString", "'First'"),
        mockParameter("PropertyTimeOfDay", "02:48:21"))));
    
  }

  @Test
  void esAllPrim() throws Exception {
    final DataProvider data = new DataProvider(oData, edm);
    EntityCollection outSet = data.readAll(esAllPrim);

    Assertions.assertEquals(4, outSet.getEntities().size());

    Entity first = outSet.getEntities().get(0);
    Assertions.assertEquals(16, first.getProperties().size());
    Assertions.assertEquals(2, first.getNavigationLinks().size());
    final EntityCollection target = first.getNavigationLink("NavPropertyETTwoPrimMany").getInlineEntitySet();
    Assertions.assertNotNull(target);
    Assertions.assertEquals(1, target.getEntities().size());
    Assertions.assertEquals(data.readAll(entityContainer.getEntitySet("ESTwoPrim")).getEntities().get(1),
        target.getEntities().get(0));

    Assertions.assertEquals(16, outSet.getEntities().get(1).getProperties().size());
    Assertions.assertEquals(16, outSet.getEntities().get(2).getProperties().size());
  }

  @Test
  void esCollAllPrim() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    EntityCollection outSet = dataProvider.readAll(esCollAllPrim);

    Assertions.assertEquals(3, outSet.getEntities().size());
    Assertions.assertEquals(17, outSet.getEntities().get(0).getProperties().size());
    Property list = outSet.getEntities().get(0).getProperties().get(1);
    Assertions.assertTrue(list.isCollection());
    Assertions.assertEquals(3, list.asCollection().size());
    Assertions.assertEquals(17, outSet.getEntities().get(1).getProperties().size());
    Assertions.assertEquals(17, outSet.getEntities().get(2).getProperties().size());
  }

  @Test
  void esCompAllPrim() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    
    EntityCollection outSet = dataProvider.readAll(esCompAllPrim);

    Assertions.assertEquals(4, outSet.getEntities().size());
    Assertions.assertEquals(2, outSet.getEntities().get(0).getProperties().size());
    Property complex = outSet.getEntities().get(0).getProperties().get(1);
    Assertions.assertTrue(complex.isComplex());
    Assertions.assertEquals(16, complex.asComplex().getValue().size());
    Assertions.assertEquals(2, outSet.getEntities().get(1).getProperties().size());
    Assertions.assertEquals(2, outSet.getEntities().get(2).getProperties().size());
  }

  @Test
  void esMixPrimCollComp() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    
    EntityCollection outSet = dataProvider.readAll(esMixPrimCollComp);

    Assertions.assertEquals(3, outSet.getEntities().size());
    Assertions.assertEquals(4, outSet.getEntities().get(0).getProperties().size());
    Property complex = outSet.getEntities().get(0).getProperties().get(2);
    Assertions.assertTrue(complex.isComplex());
    Assertions.assertEquals(2, complex.asComplex().getValue().size());
    Property complexCollection = outSet.getEntities().get(0).getProperties().get(3);
    Assertions.assertTrue(complexCollection.isCollection());
    List<?> linkedComplexValues = complexCollection.asCollection();
    Assertions.assertEquals(3, linkedComplexValues.size());
    ComplexValue linkedComplexValue = (ComplexValue) linkedComplexValues.get(0);
    Assertions.assertEquals(2, linkedComplexValue.getValue().size());
    Property lcProp = linkedComplexValue.getValue().get(0);
    Assertions.assertFalse(lcProp.isCollection());
    Assertions.assertEquals((short) 123, lcProp.getValue());
    //
    Assertions.assertEquals(4, outSet.getEntities().get(1).getProperties().size());
    Assertions.assertEquals(4, outSet.getEntities().get(2).getProperties().size());
  }

  @Test
  void esMedia() throws Exception {
    DataProvider dataProvider = new DataProvider(oData, edm);

    Entity entity = dataProvider.read(esMedia, List.of(mockParameter("PropertyInt16", "3")));
    Assertions.assertNotNull(dataProvider.readMedia(entity));
    dataProvider.delete(esMedia, entity);
    Assertions.assertEquals(3, dataProvider.readAll(esMedia).getEntities().size());
    entity = dataProvider.create(esMedia);
    Assertions.assertEquals((short) 3, entity.getProperty("PropertyInt16").getValue());
    dataProvider.setMedia(entity, new byte[] { 1, 2, 3, 4 }, "x/y");
    Assertions.assertArrayEquals(new byte[] { 1, 2, 3, 4 }, dataProvider.readMedia(entity));
    Assertions.assertEquals("x/y", entity.getMediaContentType());
  }

  @Test
  void esGeoEntity() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity entity = dataProvider.read(esGeo, List.of(mockParameter("PropertyInt16", "1")));
    Assertions.assertEquals(16, entity.getProperties().size());

    final Property point = entity.getProperty("PropertyGeometryPoint");
    // The value type must be GEOSPATIAL, not PRIMITIVE: the JSON writer dispatches on it, and a
    // PRIMITIVE tag makes it emit the WKT literal instead of the GeoJSON object.
    Assertions.assertEquals(ValueType.GEOSPATIAL, point.getValueType());
    Assertions.assertEquals(1.5, ((Point) point.getValue()).getX(), 0);
    Assertions.assertEquals(Geospatial.Dimension.GEOMETRY, ((Point) point.getValue()).getDimension());

    final Property geographyPoint = entity.getProperty("PropertyGeographyPoint");
    Assertions.assertEquals(Geospatial.Dimension.GEOGRAPHY,
        ((Point) geographyPoint.getValue()).getDimension());
    // [OData-CSDL] section 7.2.6: "the facet defaults to 0 for Geometry types or 4326 for Geography types".
    Assertions.assertEquals("4326", ((Point) geographyPoint.getValue()).getSrid().toString());
    Assertions.assertEquals("0", ((Point) point.getValue()).getSrid().toString());

    final Property collection = entity.getProperty("CollPropertyGeometryPoint");
    Assertions.assertEquals(ValueType.COLLECTION_GEOSPATIAL, collection.getValueType());
    Assertions.assertEquals(2, collection.asCollection().size());
  }

  /** Every geo property of the third seeded entity is null, and that is a legal entity. */
  @Test
  void esGeoNullEntity() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity entity = dataProvider.read(esGeo, List.of(mockParameter("PropertyInt16", "3")));
    Assertions.assertNull(entity.getProperty("PropertyGeometryPoint").getValue());
  }

  /** CRUD: create allocates a fresh Int16 key and the new entity is readable and deletable. */
  @Test
  void esGeoCrud() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertEquals(3, dataProvider.readAll(esGeo).getEntities().size());
    final Entity created = dataProvider.create(esGeo);
    Assertions.assertEquals(4, dataProvider.readAll(esGeo).getEntities().size());
    Assertions.assertNotNull(created.getProperty("PropertyInt16").getValue());
    // The create path must tag the geo properties GEOSPATIAL too, not just the seed path:
    // updatePropertyValue reuses the stored value type, so a PRIMITIVE tag here would make every
    // subsequent write of this entity serialise as a WKT literal instead of a GeoJSON object.
    Assertions.assertEquals(ValueType.GEOSPATIAL,
        created.getProperty("PropertyGeometryPoint").getValueType());
    Assertions.assertEquals(ValueType.GEOSPATIAL,
        created.getProperty("PropertyGeographyPolygon").getValueType());
    Assertions.assertEquals(ValueType.COLLECTION_GEOSPATIAL,
        created.getProperty("CollPropertyGeometryPoint").getValueType());
    dataProvider.delete(esGeo, created);
    Assertions.assertEquals(3, dataProvider.readAll(esGeo).getEntities().size());
  }

  /**
   * [OData-CSDL] section 4.1 lists the primitive types a key property may have and no Geo type is on
   * it, so ETGeo is keyed by an Edm.Int16. Pinning the model here is what keeps the restriction
   * honest, since the library does not validate key types (see the plan's Dropped section).
   */
  @Test
  void esGeoKeyIsNotAGeoType() {
    final EdmEntityType type = esGeo.getEntityType();
    Assertions.assertEquals(List.of("PropertyInt16"), type.getKeyPredicateNames());
    Assertions.assertEquals(EdmPrimitiveTypeKind.Int16.getFullQualifiedName(),
        type.getProperty("PropertyInt16").getType().getFullQualifiedName());
  }

  @Test
  void functionWithOmittedOptionalParameter() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmFunction function = edm.getUnboundFunction(
        new FullQualifiedName(SchemaProvider.NAMESPACE, "UFCRTStringOptionalParam"), List.of("ParameterString"));
    Assertions.assertNotNull(function);
    // The Core.OptionalParameter default value is a URI literal, so the quotes must not end up in
    // the value (OData 4.01, Part 1: Protocol, section 11.5.4.1.1).
    final Property property = dataProvider.readFunctionPrimitiveComplex(function,
        List.of(mockParameter("ParameterString", "'base'")), null);
    Assertions.assertEquals("base-default", property.getValue());
  }

  @Test
  void functionWithSpecifiedOptionalParameter() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmFunction function = edm.getUnboundFunction(
        new FullQualifiedName(SchemaProvider.NAMESPACE, "UFCRTStringOptionalParam"),
        List.of("ParameterString", "ParameterSuffix"));
    Assertions.assertNotNull(function);
    final Property property = dataProvider.readFunctionPrimitiveComplex(function,
        List.of(mockParameter("ParameterString", "'base'"), mockParameter("ParameterSuffix", "'-explicit'")), null);
    Assertions.assertEquals("base-explicit", property.getValue());
  }

  @Test
  void functionWithOmittedOptionalParameterWithoutDefault() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmFunction function = edm.getUnboundFunction(
        new FullQualifiedName(SchemaProvider.NAMESPACE, "UFCRTStringOptionalNoDefault"),
        List.of("ParameterString"));
    Assertions.assertNotNull(function);
    final Property property = dataProvider.readFunctionPrimitiveComplex(function,
        List.of(mockParameter("ParameterString", "'base'")), null);
    Assertions.assertEquals("base", property.getValue());
  }

  @Test
  void functionWithMalformedOptionalParameterDefaultIsBadRequest() {
    // The URI parser already rejects such a call; if a caller bypasses the parser, the malformed
    // model value is still bad input to the call (400), not a server error.
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmFunction function = edm.getUnboundFunction(
        new FullQualifiedName(SchemaProvider.NAMESPACE, "UFCRTStringOptionalBadDefault"),
        List.of("ParameterString"));
    Assertions.assertNotNull(function);
    final DataProvider.DataProviderException exception =
        Assertions.assertThrows(DataProvider.DataProviderException.class,
            () -> dataProvider.readFunctionPrimitiveComplex(function,
                List.of(mockParameter("ParameterString", "'x'")), null));
    Assertions.assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), exception.getStatusCode());
  }

  @Test
  void readByAlternateKeyResolvesEntity() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity entity = dataProvider.read(esAllPrim,
        List.of(alternateKeyParameter("PropertyString", "'Employee1@company.example'", "PropertyString")));
    Assertions.assertNotNull(entity);
    Assertions.assertEquals((short) 10, entity.getProperty("PropertyInt16").getValue());
  }

  @Test
  void readByAliasedMultiPartAlternateKey() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity entity = dataProvider.read(esAllPrim, List.of(
        alternateKeyParameter("StringPart", "'First Resource - positive values'", "PropertyString"),
        alternateKeyParameter("PropertyGuid", "01234567-89ab-cdef-0123-456789abcdef", "PropertyGuid")));
    Assertions.assertNotNull(entity);
    Assertions.assertEquals(Short.MAX_VALUE, entity.getProperty("PropertyInt16").getValue());
  }

  @Test
  void readByPrimaryKeyUnchanged() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity entity = dataProvider.read(esAllPrim, List.of(mockParameter("PropertyInt16", "10")));
    Assertions.assertNotNull(entity);
    Assertions.assertEquals("Employee1@company.example", entity.getProperty("PropertyString").getValue());
  }

  @Test
  void readResolvesReferencedKeyPropertyFromTheSourceEntity() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmEntitySet esKeyNav = entityContainer.getEntitySet("ESKeyNav");
    final Entity source = dataProvider.read(esKeyNav, List.of(mockParameter("PropertyInt16", "1")));
    Assertions.assertNotNull(source);

    final EdmEntitySet esTwoKeyNav = entityContainer.getEntitySet("ESTwoKeyNav");
    final Entity target = dataProvider.read(esTwoKeyNav.getEntityType(), dataProvider.readAll(esTwoKeyNav),
        List.of(referencedParameter("PropertyInt16", "PropertyInt16"), mockParameter("PropertyString", "'1'")),
        source);

    Assertions.assertNotNull(target);
    Assertions.assertEquals((short) 1, target.getProperty("PropertyInt16").getValue());
    Assertions.assertEquals("1", target.getProperty("PropertyString").getValue());
  }

  @Test
  void readWithReferencedKeyPropertyPicksTheRightSourceValue() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmEntitySet esKeyNav = entityContainer.getEntitySet("ESKeyNav");
    // ESKeyNav(3).PropertyInt16 == 3, so the same PropertyString must now select ESTwoKeyNav(3,'1').
    final Entity source = dataProvider.read(esKeyNav, List.of(mockParameter("PropertyInt16", "3")));
    Assertions.assertNotNull(source);

    final EdmEntitySet esTwoKeyNav = entityContainer.getEntitySet("ESTwoKeyNav");
    final Entity target = dataProvider.read(esTwoKeyNav.getEntityType(), dataProvider.readAll(esTwoKeyNav),
        List.of(referencedParameter("PropertyInt16", "PropertyInt16"), mockParameter("PropertyString", "'1'")),
        source);

    Assertions.assertNotNull(target);
    Assertions.assertEquals((short) 3, target.getProperty("PropertyInt16").getValue());
  }

  @Test
  void readWithReferencedKeyPropertyAndNoSourceEntityMatchesNothing() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmEntitySet esTwoKeyNav = entityContainer.getEntitySet("ESTwoKeyNav");
    Assertions.assertNull(dataProvider.read(esTwoKeyNav.getEntityType(), dataProvider.readAll(esTwoKeyNav),
        List.of(referencedParameter("PropertyInt16", "PropertyInt16"), mockParameter("PropertyString", "'1'"))));
  }

  @Test
  void unknownAlternateKeyValueReturnsNull() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertNull(dataProvider.read(esAllPrim,
        List.of(alternateKeyParameter("PropertyString", "'nope'", "PropertyString"))));
  }

  @Test
  void readWithoutKeysMatchesNothing() throws Exception {
    // An empty key list addresses no entity. The "first entity of the collection" choice that
    // collection-bound operations rely on is made explicitly by the processor through readFirst.
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertNull(dataProvider.read(esAllPrim, Collections.<UriParameter> emptyList()));
  }

  @Test
  void readFirstReturnsTheFirstEntityOfTheEntitySet() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertEquals(dataProvider.readAll(esAllPrim).getEntities().get(0),
        dataProvider.readFirst(esAllPrim));
  }

  @Test
  void readDataFromEntityWithoutKeysMatchesNothing() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertNull(dataProvider.readDataFromEntity(
        edm.getEntityType(new FullQualifiedName(SchemaProvider.NAMESPACE, "ETCont")),
        Collections.<UriParameter> emptyList()));
  }

  @Test
  void readFirstByEntityTypeReturnsTheFirstContainmentEntity() throws Exception {
    // Containment collections are keyed by entity-type name in the data map (DataCreator line 118).
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertNotNull(dataProvider.readFirst(
        edm.getEntityType(new FullQualifiedName(SchemaProvider.NAMESPACE, "ETCont"))));
  }

  @Test
  void boundActionThroughAlternateKey() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmEntitySet esKeyNav = entityContainer.getEntitySet("ESKeyNav");
    // ETKeyNav declares the type-level alternate key {PropertyString}
    final EntityActionResult result = dataProvider.processBoundActionEntity("BA_RTETTwoKeyNav",
        Collections.<String, Parameter> emptyMap(),
        List.of(alternateKeyParameter("PropertyString", "'I am String Property 2'", "PropertyString")),
        esKeyNav);
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getEntity());
  }

  /**
   * [OData-JSON] section 18: "Entity typed parameter values MAY include a subset of the properties,
   * or just the entity reference, as appropriate to the action." A value carrying every declared
   * property is echoed back, and the collection parameter is counted.
   */
  @Test
  void echoActionAcceptsACompleteEntityParameter() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmAction action = edm.getUnboundAction(
        new FullQualifiedName(SchemaProvider.NAMESPACE, "UARTETTwoPrimEchoParam"));
    Assertions.assertNotNull(action);

    final Entity parameterValue = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 7))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "echo me"));
    final Parameter parameter = new Parameter();
    parameter.setName("ParameterETTwoPrim");
    parameter.setValue(ValueType.ENTITY, parameterValue);
    final Parameter collection = new Parameter();
    collection.setName("CollParameterETTwoPrim");
    collection.setValue(ValueType.COLLECTION_ENTITY, List.of(parameterValue, parameterValue));

    final Entity result = dataProvider.processActionEntity("UARTETTwoPrimEchoParam",
        Map.of("ParameterETTwoPrim", parameter, "CollParameterETTwoPrim", collection)).getEntity();
    Assertions.assertEquals((short) 7, result.getProperty("PropertyInt16").getValue());
    Assertions.assertEquals("echo me (2)", result.getProperty("PropertyString").getValue());
  }

  /**
   * [OData-JSON] section 18: a subset of the properties is legal too. The design's decision is that
   * the value is used as it stands - the absent property is not defaulted from anywhere, so the echo
   * simply has nothing to echo for it.
   */
  @Test
  void echoActionAcceptsAnEntityParameterWithASubsetOfProperties() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity parameterValue = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 7));
    final Parameter parameter = new Parameter();
    parameter.setName("ParameterETTwoPrim");
    parameter.setValue(ValueType.ENTITY, parameterValue);

    final Entity result = dataProvider.processActionEntity("UARTETTwoPrimEchoParam",
        Map.of("ParameterETTwoPrim", parameter)).getEntity();
    Assertions.assertEquals((short) 7, result.getProperty("PropertyInt16").getValue());
    Assertions.assertEquals(" (0)", result.getProperty("PropertyString").getValue());
  }

  /**
   * [OData-JSON] section 4.5.8: "The id control information contains the entity-id ... By convention
   * the entity-id is identical to the canonical URL of the entity." A parameter value that is "just
   * the entity reference" is resolved against the service's own entity set.
   */
  @Test
  void echoActionResolvesAnIdOnlyEntityParameter() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity reference = new Entity();
    reference.setId(URI.create("ESTwoPrim(32767)"));
    final Parameter parameter = new Parameter();
    parameter.setName("ParameterETTwoPrim");
    parameter.setValue(ValueType.ENTITY, reference);

    final Entity result = dataProvider.processActionEntity("UARTETTwoPrimEchoParam",
        Map.of("ParameterETTwoPrim", parameter)).getEntity();
    Assertions.assertEquals((short) 32767, result.getProperty("PropertyInt16").getValue());
    Assertions.assertEquals("Test String4 (0)", result.getProperty("PropertyString").getValue());
  }

  /**
   * [OData-JSON] section 4.5.8: "By convention the entity-id is identical to the canonical URL of
   * the entity." A client may therefore send the absolute canonical URL where this service stores
   * the relative form, and both must address the same entity.
   */
  @Test
  void echoActionResolvesAnAbsoluteEntityReference() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity reference = new Entity();
    reference.setId(URI.create("http://localhost:9080/odata-server-tecsvc/odata.svc/ESTwoPrim(32767)"));
    final Parameter parameter = new Parameter();
    parameter.setName("ParameterETTwoPrim");
    parameter.setValue(ValueType.ENTITY, reference);

    final Entity result = dataProvider.processActionEntity("UARTETTwoPrimEchoParam",
        Map.of("ParameterETTwoPrim", parameter)).getEntity();
    Assertions.assertEquals((short) 32767, result.getProperty("PropertyInt16").getValue());
    Assertions.assertEquals("Test String4 (0)", result.getProperty("PropertyString").getValue());
  }

  /**
   * The reference must match the canonical id whole, or end with it on a path-segment boundary: a
   * bare suffix test would let "(32767)" address ESTwoPrim(32767).
   */
  @Test
  void echoActionRefusesAPartialSuffixOfAnEntityReference() {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    for (final String bogus : List.of("(32767)", "Prim(32767)", "ESTwoPrim(32767)/extra")) {
      final Entity reference = new Entity();
      reference.setId(URI.create(bogus));
      final Parameter parameter = new Parameter();
      parameter.setName("ParameterETTwoPrim");
      parameter.setValue(ValueType.ENTITY, reference);

      Assertions.assertThrows(DataProvider.DataProviderException.class,
          () -> dataProvider.processActionEntity("UARTETTwoPrimEchoParam",
              Map.of("ParameterETTwoPrim", parameter)),
          "'" + bogus + "' must not address ESTwoPrim(32767)");
    }
  }

  /**
   * Recorded decision: an id sent alongside properties is ignored, and the properties given are the
   * value. [OData-JSON] section 18 lets a value be a subset of the properties OR the entity
   * reference; a value carrying properties is read as the former, so no lookup happens and the
   * unresolvable-reference 400 does not apply to it.
   */
  @Test
  void echoActionIgnoresAnIdSentAlongsideProperties() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity value = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 7))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "echo me"));
    value.setId(URI.create("ESTwoPrim(4711)"));
    final Parameter parameter = new Parameter();
    parameter.setName("ParameterETTwoPrim");
    parameter.setValue(ValueType.ENTITY, value);

    final Entity result = dataProvider.processActionEntity("UARTETTwoPrimEchoParam",
        Map.of("ParameterETTwoPrim", parameter)).getEntity();
    Assertions.assertEquals((short) 7, result.getProperty("PropertyInt16").getValue());
    Assertions.assertEquals("echo me (0)", result.getProperty("PropertyString").getValue());
  }

  /** Design deviation 9: an unresolvable entity reference is a 400, not a null-valued echo. */
  @Test
  void echoActionRefusesAnUnresolvableReference() {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final Entity reference = new Entity();
    reference.setId(URI.create("ESTwoPrim(4711)"));
    final Parameter parameter = new Parameter();
    parameter.setName("ParameterETTwoPrim");
    parameter.setValue(ValueType.ENTITY, reference);

    final DataProvider.DataProviderException e = Assertions.assertThrows(
        DataProvider.DataProviderException.class,
        () -> dataProvider.processActionEntity("UARTETTwoPrimEchoParam",
            Map.of("ParameterETTwoPrim", parameter)));
    Assertions.assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
    Assertions.assertTrue(e.getMessage().contains("ESTwoPrim(4711)"));
  }

  private static UriParameter mockParameter(final String name, final String text) {
    UriParameter parameter = Mockito.mock(UriParameter.class);
    Mockito.when(parameter.getName()).thenReturn(name);
    Mockito.when(parameter.getText()).thenReturn(text);
    return parameter;
  }

  private static UriParameter referencedParameter(final String name, final String referencedProperty) {
    return new UriParameterImpl().setName(name).setReferencedProperty(referencedProperty);
  }

  private static UriParameter alternateKeyParameter(final String name, final String text,
      final String alternateKeyPropertyName) {
    return new UriParameterImpl().setName(name).setText(text)
        .setAlternateKeyPropertyName(alternateKeyPropertyName);
  }
}
