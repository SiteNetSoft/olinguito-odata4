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
 * Copyright 2026 SiteNetSoft - OLINGO-918: Cover the ABNF geo URI literal grammar
 * Copyright 2026 SiteNetSoft - OLINGO-918: Cover case-variant SRID/Collection keywords
 */
package org.sitenetsoft.olinguito.commons.core.edm.primitivetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.stream.Stream;

import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.GeospatialCollection;
import org.sitenetsoft.olinguito.commons.api.edm.geo.LineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiLineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiPoint;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiPolygon;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Polygon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EdmGeoTest extends PrimitiveTypeBaseTest {

  @Test
  void point() throws EdmPrimitiveTypeException {
    final String input = "geometry'SRID=0;Point(142.1 64.1)'";

    expectContentErrorInValueOfString(EdmGeographyPoint.getInstance(), input);

    final Point point = EdmGeometryPoint.getInstance().valueOfString(input, null, null, null, null, null, Point.class);
    assertNotNull(point);
    assertEquals("0", point.getSrid().toString());
    assertEquals(142.1, point.getX(), 0);
    assertEquals(64.1, point.getY(), 0);

    assertEquals(input, EdmGeometryPoint.getInstance().valueToString(point, null, null, null, null, null));
  }

  @Test
  void multiPoint() throws EdmPrimitiveTypeException {
    final String input = "geography'SRID=0;MultiPoint((142.1 64.1),(1.0 2.0))'";

    expectContentErrorInValueOfString(EdmGeometryMultiPoint.getInstance(), input);

    MultiPoint multipoint = EdmGeographyMultiPoint.getInstance().
        valueOfString(input, null, null, null, null, null, MultiPoint.class);
    assertNotNull(multipoint);
    assertEquals("0", multipoint.getSrid().toString());
    assertEquals(142.1, multipoint.iterator().next().getX(), 0);
    assertEquals(64.1, multipoint.iterator().next().getY(), 0);

    assertEquals(input, EdmGeographyMultiPoint.getInstance().valueToString(multipoint, null, null, null, null, null));

    multipoint = EdmGeographyMultiPoint.getInstance().
        valueOfString("geography'SRID=0;MultiPoint()'", null, null, null, null, null, MultiPoint.class);
    assertFalse(multipoint.iterator().hasNext());
  }

  @Test
  void lineString() throws EdmPrimitiveTypeException {
    final String input = "geography'SRID=0;LineString(142.1 64.1,3.14 2.78)'";

    expectContentErrorInValueOfString(EdmGeographyPoint.getInstance(), input);
    expectContentErrorInValueOfString(EdmGeometryLineString.getInstance(), input);

    final LineString lineString = EdmGeographyLineString.getInstance().
        valueOfString(input, null, null, null, null, null, LineString.class);
    assertNotNull(lineString);
    assertEquals("0", lineString.getSrid().toString());
    final Iterator<Point> itor = lineString.iterator();
    assertEquals(142.1, itor.next().getX(), 0);
    assertEquals(2.78, itor.next().getY(), 0);

    assertEquals(input, EdmGeographyLineString.getInstance().valueToString(lineString, null, null, null, null, null));
  }

  @Test
  void multiLineString() throws EdmPrimitiveTypeException {
    final String input = "geography'SRID=0;MultiLineString((142.1 64.1,3.14 2.78),(142.1 64.7,3.14 2.78))'";

    expectContentErrorInValueOfString(EdmGeographyPoint.getInstance(), input);
    expectContentErrorInValueOfString(EdmGeometryLineString.getInstance(), input);

    final MultiLineString multiLineString = EdmGeographyMultiLineString.getInstance().
        valueOfString(input, null, null, null, null, null, MultiLineString.class);
    assertNotNull(multiLineString);
    assertEquals("0", multiLineString.getSrid().toString());
    final Iterator<LineString> itor = multiLineString.iterator();
    assertEquals(142.1, itor.next().iterator().next().getX(), 0);
    assertEquals(64.7, itor.next().iterator().next().getY(), 0);

    assertEquals(input, EdmGeographyMultiLineString.getInstance().
        valueToString(multiLineString, null, null, null, null, null));
  }

  @Test
  void polygon() throws EdmPrimitiveTypeException {
    final String input = "geography'SRID=0;Polygon((1.0 1.0,1.0 1.0),(1.0 1.0,2.0 2.0,3.0 3.0,1.0 1.0))'";

    expectContentErrorInValueOfString(EdmGeometryPolygon.getInstance(), input);

    final Polygon polygon = EdmGeographyPolygon.getInstance().
        valueOfString(input, null, null, null, null, null, Polygon.class);
    assertNotNull(polygon);
    assertEquals("0", polygon.getSrid().toString());
    Iterator<Point> itor = polygon.getInterior(0).iterator();
    assertEquals(1, itor.next().getX(), 0);
    assertEquals(1, itor.next().getY(), 0);
    itor = polygon.getExterior().iterator();
    itor.next();
    assertEquals(2, itor.next().getX(), 0);
    assertEquals(3, itor.next().getY(), 0);

    assertEquals(input, EdmGeographyPolygon.getInstance().valueToString(polygon, null, null, null, null, null));
  }
  
  @Test
  void polygonWithoutHoles() throws EdmPrimitiveTypeException {
    final String input = "geography'SRID=0;Polygon((1.0 1.0,2.0 2.0,3.0 3.0,1.0 1.0))'";

    final Polygon polygon = EdmGeographyPolygon.getInstance().
        valueOfString(input, null, null, null, null, null, Polygon.class);
    assertNotNull(polygon);
    assertEquals("0", polygon.getSrid().toString());
    assertEquals(0, polygon.getNumberOfInteriorRings());
    Iterator<Point> itor = polygon.getExterior().iterator();
    assertEquals(1, itor.next().getX(), 0);
    assertEquals(2, itor.next().getX(), 0);
    assertEquals(3, itor.next().getX(), 0);
    assertEquals(1, itor.next().getX(), 0);

    assertEquals(input, EdmGeographyPolygon.getInstance().valueToString(polygon, null, null, null, null, null));
  }

  @Test
  void polygonMultipleHoles() throws EdmPrimitiveTypeException {
    final String input = "geography'SRID=4326;Polygon((1.0 1.0,1.0 1.0),(2.0 2.0,2.0 2.0)"
      + ",(1.0 1.0,2.0 2.0,3.0 3.0,1.0 1.0))'";

    expectContentErrorInValueOfString(EdmGeometryPolygon.getInstance(), input);

    final Polygon polygon = EdmGeographyPolygon.getInstance().
        valueOfString(input, null, null, null, null, null, Polygon.class);
    assertNotNull(polygon);
    assertEquals("4326", polygon.getSrid().toString());
    Iterator<Point> itor = polygon.getInterior(0).iterator();
    assertEquals(1, itor.next().getX(), 0);
    assertEquals(1, itor.next().getY(), 0);
    itor = polygon.getInterior(1).iterator();
    assertEquals(2, itor.next().getX(), 0);
    assertEquals(2, itor.next().getY(), 0);
    itor = polygon.getExterior().iterator();
    itor.next();
    assertEquals(2, itor.next().getX(), 0);
    assertEquals(3, itor.next().getY(), 0);

    assertEquals(input, EdmGeographyPolygon.getInstance().valueToString(polygon, null, null, null, null, null));
  }

  @Test
  void multiPolygon() throws EdmPrimitiveTypeException {
    final String input = "geometry'SRID=0;MultiPolygon("
        + "((1.0 1.0,1.0 1.0),(1.0 1.0,2.0 2.0,3.0 3.0,1.0 1.0)),"
        + "((1.0 1.0,1.0 1.0),(1.0 1.0,2.0 2.0,3.0 3.0,1.0 1.0))"
        + ")'";

    expectContentErrorInValueOfString(EdmGeographyPolygon.getInstance(), input);

    final MultiPolygon multiPolygon = EdmGeometryMultiPolygon.getInstance().
        valueOfString(input, null, null, null, null, null, MultiPolygon.class);
    assertNotNull(multiPolygon);
    assertEquals("0", multiPolygon.getSrid().toString());
    final Iterator<Polygon> itor = multiPolygon.iterator();
    assertEquals(1, itor.next().getInterior(0).iterator().next().getX(), 0);
    assertEquals(1, itor.next().getInterior(0).iterator().next().getX(), 0);

    assertEquals(input, EdmGeometryMultiPolygon.getInstance().
        valueToString(multiPolygon, null, null, null, null, null));

    EdmGeographyMultiPolygon.getInstance().valueOfString(
        "geography'SRID=0;MultiPolygon(((1 1,1 1),(1 1,2 2,3 3,1 1)))'",
        null, null, null, null, null, MultiPolygon.class);
  }

  @Test
  void collection() throws EdmPrimitiveTypeException {
    final String input = "geometry'SRID=0;Collection(LineString(142.1 64.1,3.14 2.78))'";

    final GeospatialCollection collection = EdmGeometryCollection.getInstance().
        valueOfString(input, null, null, null, null, null, GeospatialCollection.class);
    assertNotNull(collection);
    assertEquals("0", collection.getSrid().toString());

    final Geospatial item = collection.iterator().next();
    assertNotNull(item);
    assertTrue(item instanceof LineString);

    assertEquals(input, EdmGeometryCollection.getInstance().
        valueToString(collection, null, null, null, null, null));
  }

  /**
   * OData 4.01 ABNF: geographyPoint = geographyPrefix SQUOTE fullPointLiteral SQUOTE. The URL form
   * is what valueToString already produces, so toUriLiteral must leave it alone rather than
   * double-wrapping it.
   */
  @Test
  void uriLiteralOfAnAlreadyPrefixedValueIsIdempotent() throws EdmPrimitiveTypeException {
    final String urlForm = "geometry'SRID=0;Point(142.1 64.1)'";
    assertEquals(urlForm, EdmGeometryPoint.getInstance().toUriLiteral(urlForm));
    assertEquals(urlForm, EdmGeometryPoint.getInstance().fromUriLiteral(urlForm));
  }

  /**
   * OData 4.01 ABNF: fullPointLiteral = sridLiteral pointLiteral - the bare, unquoted, SRID-prefixed
   * form is the one legal in payloads and in CSDL DefaultValue. It must be accepted on input and
   * wrapped on output.
   */
  @Test
  void bareFullLiteralIsWrappedAndAccepted() throws EdmPrimitiveTypeException {
    assertEquals("geometry'SRID=0;Point(142.1 64.1)'",
        EdmGeometryPoint.getInstance().toUriLiteral("SRID=0;Point(142.1 64.1)"));
    assertEquals("geometry'SRID=0;Point(142.1 64.1)'",
        EdmGeometryPoint.getInstance().fromUriLiteral("SRID=0;Point(142.1 64.1)"));
    assertEquals("geography'SRID=4326;LineString(1.0 2.0,3.0 4.0)'",
        EdmGeographyLineString.getInstance().toUriLiteral("SRID=4326;LineString(1.0 2.0,3.0 4.0)"));
  }

  /**
   * OData 4.01 CSDL section 7.2.6: "If no value is specified, the facet defaults to 0 for Geometry
   * types or 4326 for Geography types." A literal without the sridLiteral prefix therefore takes the
   * type's default rather than being rejected.
   */
  @Test
  void absentSridPrefixTakesTheFacetDefault() throws EdmPrimitiveTypeException {
    assertEquals("geometry'SRID=0;Point(1.0 2.0)'",
        EdmGeometryPoint.getInstance().toUriLiteral("Point(1.0 2.0)"));
    assertEquals("geography'SRID=4326;Point(1.0 2.0)'",
        EdmGeographyPoint.getInstance().toUriLiteral("Point(1.0 2.0)"));
    assertEquals("geography'SRID=4326;Point(1.0 2.0)'",
        EdmGeographyPoint.getInstance().fromUriLiteral("Point(1.0 2.0)"));
  }

  /** A URL literal must round-trip through fromUriLiteral into a value and back out again. */
  @Test
  void uriLiteralRoundTripsThroughTheValue() throws EdmPrimitiveTypeException {
    final String urlForm = "geography'SRID=4326;Point(142.1 64.1)'";
    final EdmGeographyPoint instance = EdmGeographyPoint.getInstance();
    final Point point = instance.valueOfString(instance.fromUriLiteral(urlForm),
        null, null, null, null, null, Point.class);
    assertEquals(urlForm, instance.toUriLiteral(instance.valueToString(point, null, null, null, null, null)));
  }

  /** A literal that is not a geo literal at all is rejected, not passed through. */
  @Test
  void malformedUriLiteralIsRejected() {
    expectErrorInFromUriLiteral(EdmGeometryPoint.getInstance(), "test");
    expectErrorInFromUriLiteral(EdmGeometryPoint.getInstance(), "geometry'SRID=0;Point(1.0 2.0)");
    expectErrorInFromUriLiteral(EdmGeometryPoint.getInstance(), "geography'SRID=0;Point(1.0 2.0)'");
    expectErrorInFromUriLiteral(EdmGeometryPoint.getInstance(), "SRID=0;Point(1.0)");
  }

  /**
   * OData 4.01 ABNF positionLiteral: "doubleValue SP doubleValue [ SP doubleValue ] [ SP doubleValue ]
   * ; longitude, latitude, altitude/elevation (optional), linear referencing measure (optional)".
   * The third element is kept on Point.z; the fourth is parsed and dropped, because Point carries no
   * M coordinate (recorded deviation 3).
   */
  @Test
  void positionAcceptsAltitudeAndDropsTheMeasure() throws EdmPrimitiveTypeException {
    final EdmGeometryPoint instance = EdmGeometryPoint.getInstance();
    final Point withZ = instance.valueOfString("geometry'SRID=0;Point(1.0 2.0 42.0)'",
        null, null, null, null, null, Point.class);
    assertEquals(42.0, withZ.getZ(), 0);
    assertEquals("geometry'SRID=0;Point(1.0 2.0 42.0)'",
        instance.valueToString(withZ, null, null, null, null, null));

    final Point withM = instance.valueOfString("geometry'SRID=0;Point(1.0 2.0 42.0 7.0)'",
        null, null, null, null, null, Point.class);
    assertEquals(42.0, withM.getZ(), 0);
    assertEquals("geometry'SRID=0;Point(1.0 2.0 42.0)'",
        instance.valueToString(withM, null, null, null, null, null));
  }

  /**
   * RFC 5234 section 2.3: ABNF quoted-string literals are case-insensitive, so "geography",
   * "SRID" and "Point" all match regardless of case. UriTokenizer already reads every geo keyword
   * with nextConstantIgnoreCase, so the literal grammar must not be stricter than the tokenizer.
   */
  @Test
  void uriLiteralIsMatchedCaseInsensitively() throws EdmPrimitiveTypeException {
    assertEquals("geography'SRID=0;POINT(1 2)'",
        EdmGeographyPoint.getInstance().fromUriLiteral("geography'SRID=0;POINT(1 2)'"));
    assertEquals("geometry'srid=0;Point(1 2)'",
        EdmGeometryPoint.getInstance().fromUriLiteral("geometry'srid=0;Point(1 2)'"));
    assertEquals("geometry'srid=0;Point(1 2)'",
        EdmGeometryPoint.getInstance().fromUriLiteral("srid=0;Point(1 2)"));
    assertEquals("geography'SRID=4326;Point(1 2)'",
        EdmGeographyPoint.getInstance().fromUriLiteral("Geography'SRID=4326;Point(1 2)'"));
    assertEquals("geometry'SRID=0;MULTIPOINT((1 2))'",
        EdmGeometryMultiPoint.getInstance().fromUriLiteral("GEOMETRY'SRID=0;MULTIPOINT((1 2))'"));
  }

  /**
   * Whatever fromUriLiteral accepts, valueOfString on the same type must be able to read: anything
   * else fails late and, through VisitorOperand.tryCast, makes a $filter evaluate to null instead
   * of reporting an error. The case-variant sridLiteral and "Collection" keywords are the cases
   * that used to slip through the case-insensitive literal grammar into a case-sensitive parser.
   */
  @Test
  void everyAcceptedCaseVariantIsAlsoParseable() throws EdmPrimitiveTypeException {
    assertCaseVariantParses(EdmGeometryPoint.getInstance(), "srid=0;Point(1 2)");
    assertCaseVariantParses(EdmGeometryPoint.getInstance(), "Srid=0;Point(1 2)");
    assertCaseVariantParses(EdmGeometryPoint.getInstance(), "geometry'srid=0;Point(1 2)'");
    assertCaseVariantParses(EdmGeometryPoint.getInstance(), "GEOMETRY'SRID=0;POINT(1 2)'");
    assertCaseVariantParses(EdmGeographyPoint.getInstance(), "Geography'sRiD=4326;Point(1 2)'");
    assertCaseVariantParses(EdmGeographyCollection.getInstance(),
        "geography'srid=4326;collection(Point(1 2))'");
  }

  private static void assertCaseVariantParses(final EdmPrimitiveType instance, final String literal)
      throws EdmPrimitiveTypeException {

    final Geospatial value = instance.valueOfString(instance.fromUriLiteral(literal),
        null, null, null, null, null, Geospatial.class);
    assertNotNull(value);
  }

  /**
   * [OData-ABNF] spells nanInfinity as the case-sensitive rules "NaN" / "-INF" / "INF", and so does
   * UriTokenizer (nextConstant). The literal grammar must not accept a spelling EdmDouble rejects.
   */
  @Test
  void nanAndInfinityStayCaseSensitive() throws EdmPrimitiveTypeException {
    assertCaseVariantParses(EdmGeometryPoint.getInstance(), "geometry'SRID=0;Point(NaN -INF)'");
    assertThrows(EdmPrimitiveTypeException.class,
        () -> EdmGeometryPoint.getInstance().fromUriLiteral("geometry'SRID=0;Point(nan 2)'"));
    assertThrows(EdmPrimitiveTypeException.class,
        () -> EdmGeometryPoint.getInstance().fromUriLiteral("geometry'SRID=0;Point(1 inf)'"));
  }

  /** An already-prefixed literal, or an empty one, must never be wrapped a second time. */
  @Test
  void toUriLiteralNeverDoubleWraps() {
    assertEquals("geography'SRID=0;Point(1.0 2.0)'",
        EdmGeometryPoint.getInstance().toUriLiteral("geography'SRID=0;Point(1.0 2.0)'"));
    assertEquals("Geometry'SRID=0;Point(1.0 2.0)'",
        EdmGeometryPoint.getInstance().toUriLiteral("Geometry'SRID=0;Point(1.0 2.0)'"));
    assertEquals("", EdmGeometryPoint.getInstance().toUriLiteral(""));
  }

  private static Stream<Arguments> geoLiteralsOfEveryShape() {
    return Stream.of(
        Arguments.of(EdmGeometryPoint.getInstance(), "geometry'SRID=0;Point(142.1 64.1)'"),
        Arguments.of(EdmGeometryLineString.getInstance(),
            "geometry'SRID=0;LineString(142.1 64.1,3.14 2.78)'"),
        Arguments.of(EdmGeometryPolygon.getInstance(),
            "geometry'SRID=0;Polygon((1.0 1.0,2.0 2.0,3.0 3.0,1.0 1.0))'"),
        Arguments.of(EdmGeometryMultiPoint.getInstance(),
            "geometry'SRID=0;MultiPoint((142.1 64.1),(1.0 2.0))'"),
        Arguments.of(EdmGeometryMultiLineString.getInstance(),
            "geometry'SRID=0;MultiLineString((142.1 64.1,3.14 2.78),(142.1 64.7,3.14 2.78))'"),
        Arguments.of(EdmGeometryMultiPolygon.getInstance(),
            "geometry'SRID=0;MultiPolygon(((1.0 1.0,1.0 1.0),(1.0 1.0,2.0 2.0,3.0 3.0,1.0 1.0)))'"),
        Arguments.of(EdmGeographyCollection.getInstance(),
            "geography'SRID=4326;Collection(LineString(142.1 64.1,3.14 2.78))'"));
  }

  /**
   * Every one of the seven [OData-ABNF] geo literal shapes must survive both round trips: through
   * toUriLiteral/fromUriLiteral, and through the value itself.
   */
  @ParameterizedTest
  @MethodSource("geoLiteralsOfEveryShape")
  void everyShapeRoundTrips(final EdmPrimitiveType instance, final String literal)
      throws EdmPrimitiveTypeException {

    assertEquals(literal, instance.fromUriLiteral(instance.toUriLiteral(literal)));

    final Geospatial value = instance.valueOfString(instance.fromUriLiteral(literal),
        null, null, null, null, null, Geospatial.class);
    assertNotNull(value);
    assertEquals(literal,
        instance.toUriLiteral(instance.valueToString(value, null, null, null, null, null)));
  }
}
