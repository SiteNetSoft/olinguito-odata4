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
 * Copyright 2026 SiteNetSoft - OData 4.01: geo.distance, geo.length and geo.intersects evaluation
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operation;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial.Dimension;
import org.sitenetsoft.olinguito.commons.api.edm.geo.LineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Polygon;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.TypedOperand;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.VisitorOperand;

class GeoOperatorTest {

  private static final EdmPrimitiveType GEOMETRY_POINT =
      OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.GeometryPoint);
  private static final EdmPrimitiveType GEOGRAPHY_POINT =
      OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.GeographyPoint);
  private static final EdmPrimitiveType GEOMETRY_LINE =
      OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.GeometryLineString);
  private static final EdmPrimitiveType GEOGRAPHY_LINE =
      OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.GeographyLineString);
  private static final EdmPrimitiveType GEOMETRY_POLYGON =
      OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.GeometryPolygon);

  /** [OData-URL] 5.1.1.11.1 on a geometry pair: planar Cartesian, in the SRID's own linear units. */
  @Test
  void geometryDistanceIsPlanar() throws Exception {
    final VisitorOperand result = new GeoOperator(List.of(
        operand(GEOMETRY_POINT, point(Dimension.GEOMETRY, 0, 0)),
        operand(GEOMETRY_POINT, point(Dimension.GEOMETRY, 3, 4)))).distance();
    Assertions.assertEquals(5.0, (Double) result.getValue(), 1.0e-9);
  }

  /** [OData-URL] 5.1.1.11.1 on a geography pair: haversine on the WGS-84 mean sphere, in metres. */
  @Test
  void geographyDistanceIsHaversine() throws Exception {
    final VisitorOperand result = new GeoOperator(List.of(
        operand(GEOGRAPHY_POINT, point(Dimension.GEOGRAPHY, 0, 0)),
        operand(GEOGRAPHY_POINT, point(Dimension.GEOGRAPHY, 0, 1)))).distance();
    // One degree of latitude on a sphere of radius 6371008.8 m is R * PI / 180 = 111195.08 m.
    Assertions.assertEquals(111195.08, (Double) result.getValue(), 0.01);
  }

  /** [OData-URL] 5.1.1.11.3: the total length of the line string. */
  @Test
  void geometryLengthIsTheSumOfTheSegments() throws Exception {
    final VisitorOperand result = new GeoOperator(List.of(
        operand(GEOMETRY_LINE, new LineString(Dimension.GEOMETRY, null, List.of(
            point(Dimension.GEOMETRY, 0, 0),
            point(Dimension.GEOMETRY, 3, 4),
            point(Dimension.GEOMETRY, 3, 5)))))).length();
    Assertions.assertEquals(6.0, (Double) result.getValue(), 1.0e-9);
  }

  /** The seeded ESGeo geometry line string (0 0, 3 4) is a 3-4-5 triangle, so its length is 5. */
  @Test
  void seededGeometryLineStringIsFiveUnitsLong() throws Exception {
    final VisitorOperand result = new GeoOperator(List.of(
        operand(GEOMETRY_LINE, new LineString(Dimension.GEOMETRY, null, List.of(
            point(Dimension.GEOMETRY, 0, 0),
            point(Dimension.GEOMETRY, 3, 4)))))).length();
    Assertions.assertEquals(5.0, (Double) result.getValue(), 1.0e-9);
  }

  /** A geography line string is measured in metres, segment by segment. */
  @Test
  void geographyLengthIsInMetres() throws Exception {
    final VisitorOperand result = new GeoOperator(List.of(
        operand(GEOGRAPHY_LINE, new LineString(Dimension.GEOGRAPHY, null, List.of(
            point(Dimension.GEOGRAPHY, 0, 0),
            point(Dimension.GEOGRAPHY, 0, 1),
            point(Dimension.GEOGRAPHY, 0, 2)))))).length();
    Assertions.assertEquals(2 * 111195.08, (Double) result.getValue(), 0.02);
  }

  /** An empty or single-point line string has length zero, not an error. */
  @Test
  void degenerateLengthIsZero() throws Exception {
    final VisitorOperand result = new GeoOperator(List.of(
        operand(GEOMETRY_LINE, new LineString(Dimension.GEOMETRY, null,
            List.of(point(Dimension.GEOMETRY, 1, 1)))))).length();
    Assertions.assertEquals(0.0, (Double) result.getValue(), 0);
  }

  /**
   * [OData-URL] 5.1.1.11.2: "returns true if the specified point lies within the interior or on the
   * boundary of the specified polygon".
   */
  @Test
  void intersectsCoversInteriorBoundaryAndOutside() throws Exception {
    final Polygon square = new Polygon(Dimension.GEOMETRY, null, null,
        new LineString(Dimension.GEOMETRY, null, List.of(
            point(Dimension.GEOMETRY, 0, 0), point(Dimension.GEOMETRY, 4, 0),
            point(Dimension.GEOMETRY, 4, 4), point(Dimension.GEOMETRY, 0, 4),
            point(Dimension.GEOMETRY, 0, 0))));
    Assertions.assertEquals(Boolean.TRUE, intersects(point(Dimension.GEOMETRY, 2, 2), square));
    Assertions.assertEquals(Boolean.TRUE, intersects(point(Dimension.GEOMETRY, 0, 2), square));
    Assertions.assertEquals(Boolean.TRUE, intersects(point(Dimension.GEOMETRY, 0, 0), square));
    Assertions.assertEquals(Boolean.FALSE, intersects(point(Dimension.GEOMETRY, 5, 2), square));
    Assertions.assertEquals(Boolean.FALSE, intersects(point(Dimension.GEOMETRY, -0.5, 2), square));
  }

  /**
   * The seeded ESGeo case: entity 1's point (1.5 2.5) is inside its own square (0 0)-(4 4) and
   * outside entity 2's square (10 10)-(14 14).
   */
  @Test
  void seededPointIsInsideItsOwnSquareOnly() throws Exception {
    Assertions.assertEquals(Boolean.TRUE,
        intersects(point(Dimension.GEOMETRY, 1.5, 2.5), seededSquare(0)));
    Assertions.assertEquals(Boolean.FALSE,
        intersects(point(Dimension.GEOMETRY, 1.5, 2.5), seededSquare(10)));
    Assertions.assertEquals(Boolean.TRUE,
        intersects(point(Dimension.GEOMETRY, 11.5, 12.5), seededSquare(10)));
    Assertions.assertEquals(Boolean.FALSE,
        intersects(point(Dimension.GEOMETRY, 11.5, 12.5), seededSquare(0)));
  }

  /** A point inside an interior ring (a hole) is outside the polygon. */
  @Test
  void intersectsExcludesHoles() throws Exception {
    final Polygon withHole = new Polygon(Dimension.GEOMETRY, null,
        List.of(new LineString(Dimension.GEOMETRY, null, List.of(
            point(Dimension.GEOMETRY, 1, 1), point(Dimension.GEOMETRY, 3, 1),
            point(Dimension.GEOMETRY, 3, 3), point(Dimension.GEOMETRY, 1, 3),
            point(Dimension.GEOMETRY, 1, 1)))),
        new LineString(Dimension.GEOMETRY, null, List.of(
            point(Dimension.GEOMETRY, 0, 0), point(Dimension.GEOMETRY, 4, 0),
            point(Dimension.GEOMETRY, 4, 4), point(Dimension.GEOMETRY, 0, 4),
            point(Dimension.GEOMETRY, 0, 0))));
    Assertions.assertEquals(Boolean.FALSE, intersects(point(Dimension.GEOMETRY, 2, 2), withHole));
    Assertions.assertEquals(Boolean.TRUE, intersects(point(Dimension.GEOMETRY, 0.5, 0.5), withHole));
    // The hole's own boundary is still part of the polygon's boundary.
    Assertions.assertEquals(Boolean.TRUE, intersects(point(Dimension.GEOMETRY, 1, 2), withHole));
  }

  /** A null operand yields a null result rather than an exception, matching the other operators. */
  @Test
  void nullOperandsYieldNull() throws Exception {
    Assertions.assertNull(new GeoOperator(List.of(
        operand(GEOMETRY_POINT, null), operand(GEOMETRY_POINT, point(Dimension.GEOMETRY, 1, 1))))
        .distance().getValue());
    Assertions.assertNull(new GeoOperator(List.of(operand(GEOMETRY_LINE, null))).length().getValue());
    Assertions.assertNull(new GeoOperator(List.of(
        operand(GEOMETRY_POINT, point(Dimension.GEOMETRY, 1, 1)), operand(GEOMETRY_POLYGON, null)))
        .intersects().getValue());
  }

  /**
   * The design spec's recorded deviation 1: tecsvc implements only the overloads the spec defines.
   * Mixing a geography point with a geometry point is not one of them.
   */
  @Test
  void mixedDimensionsAreNotImplemented() {
    final ODataApplicationException e = Assertions.assertThrows(ODataApplicationException.class,
        () -> new GeoOperator(List.of(
            operand(GEOGRAPHY_POINT, point(Dimension.GEOGRAPHY, 0, 0)),
            operand(GEOMETRY_POINT, point(Dimension.GEOMETRY, 0, 1)))).distance());
    Assertions.assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
    Assertions.assertTrue(e.getMessage().contains("geo.distance"));
  }

  /**
   * [OData-URL] 5.1.1.11.2 defines Point x Polygon only; a point against anything else is out of
   * spec and answers 501 rather than guessing.
   */
  @Test
  void nonPolygonSecondOperandIsNotImplemented() {
    final ODataApplicationException e = Assertions.assertThrows(ODataApplicationException.class,
        () -> new GeoOperator(List.of(
            operand(GEOMETRY_POINT, point(Dimension.GEOMETRY, 1, 1)),
            operand(GEOMETRY_LINE, new LineString(Dimension.GEOMETRY, null, List.of(
                point(Dimension.GEOMETRY, 0, 0), point(Dimension.GEOMETRY, 4, 4))))))
            .intersects());
    Assertions.assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
    Assertions.assertTrue(e.getMessage().contains("geo.intersects"));
  }

  private static Polygon seededSquare(final double offset) {
    return new Polygon(Dimension.GEOMETRY, null, null,
        new LineString(Dimension.GEOMETRY, null, List.of(
            point(Dimension.GEOMETRY, offset, offset),
            point(Dimension.GEOMETRY, 4 + offset, offset),
            point(Dimension.GEOMETRY, 4 + offset, 4 + offset),
            point(Dimension.GEOMETRY, offset, 4 + offset),
            point(Dimension.GEOMETRY, offset, offset))));
  }

  private Object intersects(final Point p, final Polygon polygon) throws ODataApplicationException {
    return new GeoOperator(List.of(operand(GEOMETRY_POINT, p), operand(GEOMETRY_POLYGON, polygon)))
        .intersects().getValue();
  }

  private VisitorOperand operand(final EdmPrimitiveType type, final Object value) {
    return new TypedOperand(value, type);
  }

  private static Point point(final Dimension dimension, final double x, final double y) {
    final Point point = new Point(dimension, null);
    point.setX(x);
    point.setY(y);
    return point;
  }
}
