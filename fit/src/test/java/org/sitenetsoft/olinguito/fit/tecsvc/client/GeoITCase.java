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
 * Copyright 2026 SiteNetSoft - Added the end-to-end geospatial fit tests over ESGeo
 */
package org.sitenetsoft.olinguito.fit.tecsvc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.communication.ODataServerErrorException;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataDeleteRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntityRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataDeleteResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityCreateResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientCollectionValue;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.api.domain.ClientProperty;
import org.sitenetsoft.olinguito.client.api.domain.ClientValue;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial.Dimension;
import org.sitenetsoft.olinguito.commons.api.edm.geo.GeospatialCollection;
import org.sitenetsoft.olinguito.commons.api.edm.geo.LineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Polygon;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;

/**
 * End-to-end proof of the Tier 6 geospatial work against the technical reference service: the
 * {@code ESGeo} entity set round trips through the real client, {@code $filter} evaluates each of the
 * three functions [OData-URL] section 5.1.1.11 defines, and the two limits the reference service draws
 * - the 501 for an overload outside that section and the 400 for comparing a geo value to anything but
 * {@code null} ([OData-URL] section 5.1.1.1) - are pinned rather than left to be rediscovered.
 *
 * <p>Two deliberate departures from the fit suite's usual shape, both forced by real behaviour:
 *
 * <ul>
 * <li><b>JSON only.</b> {@code ODataXmlSerializer} refuses a geospatial property, so {@code ESGeo} has
 * no Atom representation at all; this case therefore extends the JSON-only
 * {@link AbstractTecSvcITCase} rather than the format-parameterised {@code AbstractParamTecSvcITCase},
 * and {@link #esGeoInXmlIsNotSupported()} pins the refusal.</li>
 * <li><b>{@code odata.metadata=full}.</b> A geo value goes on the wire as a bare GeoJSON object
 * ([OData-JSON] section 7.1), which carries no dimension: {@code {"type":"Point", ...}} is written
 * identically for {@code Edm.GeometryPoint} and {@code Edm.GeographyPoint}. Only the
 * {@code name@odata.type} annotation that full metadata adds tells a client which of the two it holds,
 * so the assertions on dimension and SRID below need it. At minimal metadata the client falls back to
 * guessing {@code Edm.Geography*}, which is why this case asks for full metadata instead of asserting
 * a guess.</li>
 * </ul>
 */
public class GeoITCase extends AbstractTecSvcITCase {

  private static final String ES_GEO = "ESGeo";
  private static final String ES_TWO_PRIM = "ESTwoPrim";
  private static final String PROPERTY_INT16 = "PropertyInt16";
  private static final String PROPERTY_STRING = "PropertyString";
  private static final String PROPERTY_GEOMETRY_POINT = "PropertyGeometryPoint";
  private static final String PROPERTY_GEOMETRY_LINE = "PropertyGeometryLineString";
  private static final String PROPERTY_GEOMETRY_POLYGON = "PropertyGeometryPolygon";
  private static final String PROPERTY_GEOGRAPHY_POINT = "PropertyGeographyPoint";
  private static final String PROPERTY_GEOGRAPHY_LINE = "PropertyGeographyLineString";
  private static final String PROPERTY_GEOGRAPHY_COLLECTION = "PropertyGeographyCollection";
  private static final String COLL_PROPERTY_GEOMETRY_POINT = "CollPropertyGeometryPoint";

  /** The distance in metres of entity 1's geography point (1.5 2.5) from (0 0) is 324158.xx. */
  private static final String GEOGRAPHY_DISTANCE_FROM_ORIGIN =
      "geo.distance(" + PROPERTY_GEOGRAPHY_POINT + ",geography'SRID=4326;Point(0 0)')";

  /** The length in metres of entity 1's geography line string (0 0, 1 1, 2 2) is 314474.xx. */
  private static final String GEOGRAPHY_LINE_LENGTH = "geo.length(" + PROPERTY_GEOGRAPHY_LINE + ')';

  @Override
  protected ContentType getContentType() {
    return ContentType.JSON_FULL_METADATA;
  }

  /**
   * [OData-JSON] section 7.1: a geo value on the wire is a GeoJSON geometry object, so reading
   * {@code ESGeo} must hand back {@link Geospatial} values - and the right dimension and SRID for each -
   * rather than the WKT strings a geo value would degrade into if it were tagged as an ordinary
   * primitive anywhere along the way.
   */
  @Test
  public void readGeoEntity() {
    final ClientEntity entity = readEntity(1);

    final ClientProperty geometryPoint = entity.getProperty(PROPERTY_GEOMETRY_POINT);
    assertEquals("Edm.GeometryPoint", geometryPoint.getPrimitiveValue().getTypeName());
    final Point point = geoValue(geometryPoint, Point.class);
    assertEquals(1.5, point.getX(), 0);
    assertEquals(2.5, point.getY(), 0);
    assertEquals(Dimension.GEOMETRY, point.getDimension());
    assertEquals("0", point.getSrid().toString());

    // The same coordinates in the other dimension: the CSDL omits the SRID facet, so the default
    // 4326 for a geography type applies ([OData-CSDL] section 7.2.6).
    final Point geography = geoValue(entity.getProperty(PROPERTY_GEOGRAPHY_POINT), Point.class);
    assertEquals(Dimension.GEOGRAPHY, geography.getDimension());
    assertEquals("4326", geography.getSrid().toString());
    assertEquals(1.5, geography.getX(), 0);
    assertEquals(2.5, geography.getY(), 0);

    // (0 0) -> (3 4), the 3-4-5 line geo.length is measured over below.
    final LineString line = geoValue(entity.getProperty(PROPERTY_GEOMETRY_LINE), LineString.class);
    assertEquals(List.of(0.0, 0.0, 3.0, 4.0), coordinates(line));

    // The axis-aligned square (0 0)-(4 4), first position repeated last as [OData-ABNF] requires.
    final Polygon polygon = geoValue(entity.getProperty(PROPERTY_GEOMETRY_POLYGON), Polygon.class);
    assertEquals(List.of(0.0, 0.0, 4.0, 0.0, 4.0, 4.0, 0.0, 4.0, 0.0, 0.0), coordinates(polygon.getExterior()));
    assertEquals(Dimension.GEOMETRY, polygon.getDimension());

    // Edm.GeographyCollection and Edm.GeometryCollection are both written as a GeoJSON
    // "GeometryCollection" (GeoJSON has no "GeographyCollection"), so the declared type - not the
    // GeoJSON type name - is what says which one this is.
    final ClientProperty geoCollection = entity.getProperty(PROPERTY_GEOGRAPHY_COLLECTION);
    assertEquals("Edm.GeographyCollection", geoCollection.getPrimitiveValue().getTypeName());
    final GeospatialCollection collection = geoValue(geoCollection, GeospatialCollection.class);
    final Iterator<Geospatial> members = collection.iterator();
    final Point member = (Point) members.next();
    assertEquals(1.0, member.getX(), 0);
    assertEquals(1.0, member.getY(), 0);
    Assert.assertFalse(members.hasNext());

    // A collection-valued geo property is an array of GeoJSON objects, so its members have to come
    // back as geospatial values too, not as complex values with "type"/"coordinates" fields.
    final ClientCollectionValue<ClientValue> points =
        entity.getProperty(COLL_PROPERTY_GEOMETRY_POINT).getCollectionValue();
    assertEquals(2, points.size());
    assertEquals(List.of(0.0, 0.0, 1.0, 1.0), collectionCoordinates(points));
    for (final ClientValue value : points) {
      assertEquals(Dimension.GEOMETRY, ((Point) value.asPrimitive().toValue()).getDimension());
    }
  }

  /**
   * Entity 3 has every geo property null. A null geo value is written as JSON {@code null}, but an
   * empty collection-valued geo property is written as {@code []} and must not read back as null.
   */
  @Test
  public void readGeoEntityWithNullValues() {
    final ClientEntity entity = readEntity(3);
    assertNull(entity.getProperty(PROPERTY_GEOMETRY_POINT).getPrimitiveValue().toValue());
    assertNull(entity.getProperty(PROPERTY_GEOGRAPHY_POINT).getPrimitiveValue().toValue());
    assertNull(entity.getProperty(PROPERTY_GEOMETRY_POLYGON).getPrimitiveValue().toValue());

    final ClientProperty collection = entity.getProperty(COLL_PROPERTY_GEOMETRY_POINT);
    assertNotNull(collection.getCollectionValue());
    assertEquals(0, collection.getCollectionValue().size());
  }

  /** A geo property is projectable: $select over it keeps the GeoJSON value for every entity. */
  @Test
  public void selectGeoProperty() {
    final ODataClient client = getClient();
    final ODataEntitySetRequest<ClientEntitySet> request = client.getRetrieveRequestFactory()
        .getEntitySetRequest(client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_GEO)
            .select(PROPERTY_INT16 + ',' + PROPERTY_GEOMETRY_POINT).build());
    setCookieHeader(request);
    final ODataRetrieveResponse<ClientEntitySet> response = request.execute();
    saveCookieHeader(response);

    final List<ClientEntity> entities = response.getBody().getEntities();
    assertEquals(3, entities.size());
    assertEquals(1.5, geoValue(entities.get(0).getProperty(PROPERTY_GEOMETRY_POINT), Point.class).getX(), 0);
    assertEquals(11.5, geoValue(entities.get(1).getProperty(PROPERTY_GEOMETRY_POINT), Point.class).getX(), 0);
    assertEquals(12.5, geoValue(entities.get(1).getProperty(PROPERTY_GEOMETRY_POINT), Point.class).getY(), 0);
    assertNull(entities.get(2).getProperty(PROPERTY_GEOMETRY_POINT).getPrimitiveValue().toValue());
    // Only the two selected properties are written.
    assertNull(entities.get(0).getProperty(PROPERTY_GEOGRAPHY_POINT));
  }

  /**
   * The full CRUD round trip: a POSTed geo value comes back from the create response, is still a geo
   * value when read again from the stored entity, and the entity then deletes cleanly. The read-back
   * is the part that matters - it is what proves the server stored the value as a geospatial one and
   * not as a primitive that a later write would re-serialise as a WKT string.
   */
  @Test
  public void createReadAndDeleteGeoEntity() {
    final ODataClient client = getClient();
    final ClientEntity newEntity = getFactory().newEntity(new FullQualifiedName(SERVICE_NAMESPACE, "ETGeo"));
    newEntity.getProperties().add(getFactory().newPrimitiveProperty(PROPERTY_GEOMETRY_POINT,
        getFactory().newPrimitiveValueBuilder().setType(EdmPrimitiveTypeKind.GeometryPoint)
            .setValue(point(Dimension.GEOMETRY, 7, 8)).build()));
    newEntity.getProperties().add(getFactory().newPrimitiveProperty(PROPERTY_GEOGRAPHY_POINT,
        getFactory().newPrimitiveValueBuilder().setType(EdmPrimitiveTypeKind.GeographyPoint)
            .setValue(point(Dimension.GEOGRAPHY, 9, 10)).build()));
    final ClientCollectionValue<ClientValue> points = getFactory()
        .newCollectionValue("Collection(Edm.GeometryPoint)");
    points.add(getFactory().newPrimitiveValueBuilder().setType(EdmPrimitiveTypeKind.GeometryPoint)
        .setValue(point(Dimension.GEOMETRY, 1, 2)).build());
    newEntity.getProperties().add(getFactory().newCollectionProperty(COLL_PROPERTY_GEOMETRY_POINT, points));

    final ODataEntityCreateResponse<ClientEntity> createResponse = client.getCUDRequestFactory()
        .getEntityCreateRequest(
            client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_GEO).build(), newEntity).execute();
    assertEquals(HttpStatusCode.CREATED.getStatusCode(), createResponse.getStatusCode());

    // The technical service keeps its data per session, so the read-back and the delete have to carry
    // the session the create ran in; see BasicITCase#deleteEntity for the same idiom.
    final String cookie = createResponse.getHeader(HttpHeader.SET_COOKIE).iterator().next();
    final ClientEntity created = createResponse.getBody();
    // Three entities are seeded, so the generated key is 4.
    assertEquals(4, ((Number) created.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
    assertCreatedValues(created);
    assertEquals(SERVICE_URI + ES_GEO + "(4)",
        createResponse.getHeader(HttpHeader.LOCATION).iterator().next());

    final URI uri = client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_GEO)
        .appendKeySegment(4).build();
    final ODataEntityRequest<ClientEntity> readRequest =
        client.getRetrieveRequestFactory().getEntityRequest(uri);
    readRequest.addCustomHeader(HttpHeader.COOKIE, cookie);
    assertCreatedValues(readRequest.execute().getBody());

    final ODataDeleteRequest deleteRequest = client.getCUDRequestFactory().getDeleteRequest(uri);
    deleteRequest.addCustomHeader(HttpHeader.COOKIE, cookie);
    final ODataDeleteResponse deleteResponse = deleteRequest.execute();
    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), deleteResponse.getStatusCode());

    final ODataEntityRequest<ClientEntity> goneRequest =
        client.getRetrieveRequestFactory().getEntityRequest(uri);
    goneRequest.addCustomHeader(HttpHeader.COOKIE, cookie);
    try {
      goneRequest.execute();
      Assert.fail("Expected the deleted entity to be gone.");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getStatusCode());
    }
  }

  /**
   * [OData-URL] section 5.1.1.11.1 {@code geo.distance}. A geometry pair is planar and exact, so the
   * distance from entity 1's own point is 0 and entity 2's point is sqrt(200) away. A geography pair is
   * a great circle in metres, so entity 1's point is bracketed rather than compared exactly.
   */
  @Test
  public void filterByGeoDistance() {
    final String fromEntityOnesPoint =
        "geo.distance(" + PROPERTY_GEOMETRY_POINT + ",geometry'SRID=0;Point(1.5 2.5)')";
    assertKeys(filter(fromEntityOnesPoint + " lt 0.5"), 1);
    assertKeys(filter(fromEntityOnesPoint + " gt 14 and " + fromEntityOnesPoint + " lt 14.2"), 2);

    // 324158.xx m from (0 0) for entity 1; entity 2's point is roughly 1880 km away.
    assertKeys(filter(GEOGRAPHY_DISTANCE_FROM_ORIGIN + " gt 324000 and "
        + GEOGRAPHY_DISTANCE_FROM_ORIGIN + " lt 324200"), 1);
    assertKeys(filter(GEOGRAPHY_DISTANCE_FROM_ORIGIN + " gt 324000"), 1, 2);
  }

  /**
   * [OData-URL] section 5.1.1.11.3 {@code geo.length}. Both seeded geometry line strings are 3-4-5
   * triangles, so their planar length is exactly 5 for entity 1 and entity 2 alike; the geography line
   * string is a great-circle length in metres and is bracketed.
   */
  @Test
  public void filterByGeoLength() {
    assertKeys(filter("geo.length(" + PROPERTY_GEOMETRY_LINE + ") eq 5"), 1, 2);
    assertKeys(filter("geo.length(" + PROPERTY_GEOMETRY_LINE + ") gt 5"));

    // 314474.xx m for entity 1's (0 0, 1 1, 2 2); entity 2's line is shorter, the meridians having
    // converged by latitude 10.
    assertKeys(filter(GEOGRAPHY_LINE_LENGTH + " gt 314000 and " + GEOGRAPHY_LINE_LENGTH + " lt 315000"), 1);
  }

  /**
   * [OData-URL] section 5.1.1.11.2 {@code geo.intersects}: true when the point lies within the interior
   * or on the boundary of the polygon. Each entity's own point is inside its own square, and entity 1's
   * point (1.5 2.5) is outside entity 2's square (10 10)-(14 14).
   */
  @Test
  public void filterByGeoIntersects() {
    assertKeys(filter("geo.intersects(" + PROPERTY_GEOMETRY_POINT + ',' + PROPERTY_GEOMETRY_POLYGON + ')'), 1, 2);
    assertKeys(filter("geo.intersects(" + PROPERTY_GEOMETRY_POINT
        + ",geometry'SRID=0;Polygon((10 10,14 10,14 14,10 14,10 10))')"), 2);
    assertKeys(filter("geo.intersects(" + PROPERTY_GEOMETRY_POINT
        + ",geometry'SRID=0;Polygon((0 0,4 0,4 4,0 4,0 0))')"), 1);
    // On the boundary counts as intersecting: (0 0) is a vertex of entity 1's own square.
    assertKeys(filter("geo.intersects(geometry'SRID=0;Point(0 0)'," + PROPERTY_GEOMETRY_POLYGON + ')'), 1);
    assertKeys(filter("geo.intersects(geometry'SRID=0;Point(100 100)'," + PROPERTY_GEOMETRY_POLYGON + ')'));
  }

  /**
   * The Tier 6 design's recorded deviation 1: the reference service implements only the overloads
   * [OData-URL] section 5.1.1.11 defines, and a geography operand paired with a geometry one is not one
   * of them. The parser type-checks each parameter against both dimensions independently, so this call
   * parses and then answers 501 from the evaluator, naming the function it could not evaluate.
   */
  @Test
  public void mixedDimensionGeoDistanceIsNotImplemented() {
    try {
      filter("geo.distance(" + PROPERTY_GEOMETRY_POINT + ",geography'SRID=4326;Point(0 0)') lt 1");
      Assert.fail("Expected a 501 for a mixed-dimension geo.distance.");
    } catch (final ODataServerErrorException e) {
      // 501 is a server-status error, so the client raises ODataServerErrorException, which carries the
      // status only inside its message; the OData error payload carries the detail.
      assertTrue("Expected a 501, got: " + e.getMessage(),
          e.getMessage().contains("HTTP/" + HttpStatusCode.NOT_IMPLEMENTED.getStatusCode()));
      assertTrue("Expected the message to name the function, got: " + e.getODataError().getMessage(),
          e.getODataError().getMessage().contains("geo.distance"));
    }
  }

  /**
   * [OData-URL] section 5.1.1.1: "Edm.Binary, Edm.Stream, and the Edm.Geo types can only be compared to
   * the null value using the eq and ne operators."
   */
  @Test
  public void geoValuesAreOnlyComparableToNull() {
    assertKeys(filter(PROPERTY_GEOMETRY_POINT + " eq null"), 3);
    assertKeys(filter(PROPERTY_GEOMETRY_POINT + " ne null"), 1, 2);

    try {
      filter(PROPERTY_GEOMETRY_POINT + " eq geometry'SRID=0;Point(1.5 2.5)'");
      Assert.fail("Expected a 400 for comparing a geo value to a non-null operand.");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
      assertTrue("Expected the message to name the property, got: " + e.getODataError().getMessage(),
          e.getODataError().getMessage().contains(PROPERTY_GEOMETRY_POINT));
    }
  }

  /**
   * The server's Atom writer has no geo support - {@code ODataXmlSerializer} refuses a geospatial
   * property - so {@code ESGeo} is a JSON-only entity set in this release. Pinned here rather than left
   * to surprise someone: the refusal is reported as a 400 naming the property it could not write.
   */
  @Test
  public void esGeoInXmlIsNotSupported() {
    final ODataClient xmlClient = ODataClientFactory.getClient();
    xmlClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_XML);
    try {
      xmlClient.getRetrieveRequestFactory().getEntitySetRequest(
          xmlClient.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_GEO).build()).execute();
      Assert.fail("Expected the XML serializer to refuse a geospatial property.");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
      assertTrue("Expected the message to name the property, got: " + e.getODataError().getMessage(),
          e.getODataError().getMessage().contains(PROPERTY_GEOGRAPHY_POINT));
    }
  }

  /**
   * At {@code odata.metadata=minimal} the payload declares no type at all, so a client can still tell
   * that a collection member is a GeoJSON geometry - the members must not decay into complex values with
   * {@code "type"}/{@code "coordinates"} fields - but it cannot tell which dimension it is and falls
   * back to the same {@code Edm.Geography*} guess the single-valued path makes. Pinned so the fallback
   * and its limit are both known.
   */
  @Test
  public void readGeoCollectionAtMinimalMetadata() {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setDefaultPubFormat(ContentType.JSON);
    final ClientEntity entity = client.getRetrieveRequestFactory().getEntityRequest(
        client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_GEO).appendKeySegment(1).build())
        .execute().getBody();

    final ClientCollectionValue<ClientValue> points =
        entity.getProperty(COLL_PROPERTY_GEOMETRY_POINT).getCollectionValue();
    assertEquals(2, points.size());
    assertEquals(List.of(0.0, 0.0, 1.0, 1.0), collectionCoordinates(points));
    for (final ClientValue value : points) {
      assertEquals(Dimension.GEOGRAPHY, ((Point) value.asPrimitive().toValue()).getDimension());
    }
  }

  /** Closed pin: a request that touches no geo value at all is unaffected by everything above. */
  @Test
  public void nonGeoRequestsAreUnaffected() {
    final ODataClient client = getClient();
    final ODataEntitySetRequest<ClientEntitySet> request = client.getRetrieveRequestFactory()
        .getEntitySetRequest(client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_TWO_PRIM)
            .filter("PropertyInt16 gt 0").build());
    final ODataRetrieveResponse<ClientEntitySet> response = request.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    // ESTwoPrim seeds 32766, -365, -32766 and 32767 (DataCreator#createESTwoPrim).
    final List<ClientEntity> entities = response.getBody().getEntities();
    assertEquals(2, entities.size());
    assertEquals(32766, ((Number) entities.get(0).getProperty(PROPERTY_INT16)
        .getPrimitiveValue().toValue()).intValue());
    assertEquals("Test String1", entities.get(0).getProperty(PROPERTY_STRING).getPrimitiveValue().toValue());
    assertEquals(32767, ((Number) entities.get(1).getProperty(PROPERTY_INT16)
        .getPrimitiveValue().toValue()).intValue());
    assertEquals("Test String4", entities.get(1).getProperty(PROPERTY_STRING).getPrimitiveValue().toValue());
  }

  private void assertCreatedValues(final ClientEntity entity) {
    final Point geometry = geoValue(entity.getProperty(PROPERTY_GEOMETRY_POINT), Point.class);
    assertEquals(7.0, geometry.getX(), 0);
    assertEquals(8.0, geometry.getY(), 0);
    assertEquals(Dimension.GEOMETRY, geometry.getDimension());

    final Point geography = geoValue(entity.getProperty(PROPERTY_GEOGRAPHY_POINT), Point.class);
    assertEquals(9.0, geography.getX(), 0);
    assertEquals(10.0, geography.getY(), 0);
    assertEquals(Dimension.GEOGRAPHY, geography.getDimension());

    final ClientCollectionValue<ClientValue> points =
        entity.getProperty(COLL_PROPERTY_GEOMETRY_POINT).getCollectionValue();
    assertEquals(1, points.size());
    assertEquals(List.of(1.0, 2.0), collectionCoordinates(points));

    // Everything not written stays null, and the empty geo values are still readable.
    assertNull(entity.getProperty(PROPERTY_GEOMETRY_POLYGON).getPrimitiveValue().toValue());
  }

  private ClientEntity readEntity(final int key) {
    final ODataClient client = getClient();
    final ODataEntityRequest<ClientEntity> request = client.getRetrieveRequestFactory().getEntityRequest(
        client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_GEO).appendKeySegment(key).build());
    setCookieHeader(request);
    final ODataRetrieveResponse<ClientEntity> response = request.execute();
    saveCookieHeader(response);
    return response.getBody();
  }

  private ClientEntitySet filter(final String filter) {
    final ODataClient client = getClient();
    final ODataEntitySetRequest<ClientEntitySet> request = client.getRetrieveRequestFactory()
        .getEntitySetRequest(client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_GEO)
            .filter(filter).select(PROPERTY_INT16).build());
    setCookieHeader(request);
    final ODataRetrieveResponse<ClientEntitySet> response = request.execute();
    saveCookieHeader(response);
    return response.getBody();
  }

  private void assertKeys(final ClientEntitySet entitySet, final int... expectedKeys) {
    final List<Integer> keys = new ArrayList<>();
    for (final ClientEntity entity : entitySet.getEntities()) {
      keys.add(((Number) entity.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
    }
    final List<Integer> expected = new ArrayList<>();
    for (final int key : expectedKeys) {
      expected.add(key);
    }
    assertEquals(expected, keys);
  }

  private <T extends Geospatial> T geoValue(final ClientProperty property, final Class<T> expected) {
    final Object value = property.getPrimitiveValue().toValue();
    assertTrue("Expected a " + expected.getSimpleName() + " for " + property.getName()
        + ", got " + (value == null ? "null" : value.getClass().getName()),
        expected.isInstance(value));
    return expected.cast(value);
  }

  private List<Double> coordinates(final Iterable<Point> positions) {
    final List<Double> flattened = new ArrayList<>();
    for (final Point position : positions) {
      flattened.add(position.getX());
      flattened.add(position.getY());
    }
    return flattened;
  }

  private List<Double> collectionCoordinates(final ClientCollectionValue<ClientValue> collection) {
    final List<Double> flattened = new ArrayList<>();
    for (final ClientValue value : collection) {
      final Point point = (Point) value.asPrimitive().toValue();
      flattened.add(point.getX());
      flattened.add(point.getY());
    }
    return flattened;
  }

  private static Point point(final Dimension dimension, final double x, final double y) {
    final Point point = new Point(dimension, null);
    point.setX(x);
    point.setY(y);
    return point;
  }
}
