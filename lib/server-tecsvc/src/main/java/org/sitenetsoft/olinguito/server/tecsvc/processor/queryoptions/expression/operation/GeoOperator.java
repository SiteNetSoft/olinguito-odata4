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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.geo.ComposedGeospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial.Dimension;
import org.sitenetsoft.olinguito.commons.api.edm.geo.LineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Polygon;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.TypedOperand;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.VisitorOperand;

/**
 * Evaluates the three geo functions of [OData-URL] section 5.1.1.11 for the technical reference
 * service.
 *
 * <p>This is a reference implementation, not a geodesy library, and the limitation is the reference
 * service's rather than the library's:
 * <ul>
 *   <li><b>Geometry</b> values are treated as planar Cartesian coordinates, so a distance or length
 *       comes out in whatever linear unit the SRID uses. That is literally what section 5.1.1.11.1
 *       asks for ("in the coordinate reference system signified by the two points' SRIDs").</li>
 *   <li><b>Geography</b> values are measured with the haversine formula on a sphere of the WGS-84
 *       mean radius (6 371 008.8 m), so a distance or length comes out in <b>metres</b>. EPSG:4326's
 *       own units are degrees, in which a shortest distance is not meaningful; substituting metres is
 *       this service's decision, recorded as deviation 1 of the Tier 6 design.</li>
 *   <li><b>geo.intersects</b> is implemented for the Point x Polygon overloads only - the only ones
 *       section 5.1.1.11.2 defines - by even-odd ray casting in the plane. There is no CRS
 *       re-projection: a geography polygon is tested in degree space.</li>
 * </ul>
 * Anything else answers 501 with a message naming the function and the operand types.
 */
public class GeoOperator {

  /** WGS-84 mean radius (IUGG R1), in metres. */
  private static final double WGS84_MEAN_RADIUS = 6_371_008.8;

  /** Tolerance for "on the boundary" tests, in coordinate units. */
  private static final double EPSILON = 1.0e-12;

  private static final OData ODATA = OData.newInstance();
  private static final EdmPrimitiveType PRIM_DOUBLE =
      ODATA.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Double);
  private static final EdmPrimitiveType PRIM_BOOLEAN =
      ODATA.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Boolean);

  private final List<VisitorOperand> parameters;

  public GeoOperator(final List<VisitorOperand> parameters) {
    this.parameters = parameters;
  }

  /** [OData-URL] section 5.1.1.11.1 geo.distance. */
  public VisitorOperand distance() throws ODataApplicationException {
    final Point first = geoValue("geo.distance", 0, Point.class);
    final Point second = geoValue("geo.distance", 1, Point.class);
    if (first == null || second == null) {
      return new TypedOperand(null, PRIM_DOUBLE);
    }
    requireSameDimension("geo.distance", first, second);
    return new TypedOperand(
        first.getDimension() == Dimension.GEOGRAPHY ? haversine(first, second) : planar(first, second),
        PRIM_DOUBLE);
  }

  /** [OData-URL] section 5.1.1.11.3 geo.length. */
  public VisitorOperand length() throws ODataApplicationException {
    final LineString line = geoValue("geo.length", 0, LineString.class);
    if (line == null) {
      return new TypedOperand(null, PRIM_DOUBLE);
    }
    final List<Point> points = pointsOf(line);
    double total = 0;
    for (int i = 1; i < points.size(); i++) {
      total += line.getDimension() == Dimension.GEOGRAPHY
          ? haversine(points.get(i - 1), points.get(i))
          : planar(points.get(i - 1), points.get(i));
    }
    return new TypedOperand(total, PRIM_DOUBLE);
  }

  /** [OData-URL] section 5.1.1.11.2 geo.intersects, Point x Polygon only. */
  public VisitorOperand intersects() throws ODataApplicationException {
    final Point point = geoValue("geo.intersects", 0, Point.class);
    final Polygon polygon = geoValue("geo.intersects", 1, Polygon.class);
    if (point == null || polygon == null) {
      return new TypedOperand(null, PRIM_BOOLEAN);
    }
    requireSameDimension("geo.intersects", point, polygon);
    return new TypedOperand(pointInPolygon(point, polygon), PRIM_BOOLEAN);
  }

  private double planar(final Point from, final Point to) {
    final double dx = to.getX() - from.getX();
    final double dy = to.getY() - from.getY();
    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Great-circle distance in metres. x is longitude and y is latitude, in decimal degrees
   * ([OData-ABNF]'s positionLiteral comment and [GeoJSON] section 3.1.1 both fix that order).
   */
  private double haversine(final Point from, final Point to) {
    final double lat1 = Math.toRadians(from.getY());
    final double lat2 = Math.toRadians(to.getY());
    final double deltaLat = lat2 - lat1;
    final double deltaLon = Math.toRadians(to.getX() - from.getX());
    final double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
    return 2 * WGS84_MEAN_RADIUS * Math.asin(Math.min(1.0, Math.sqrt(a)));
  }

  /**
   * "true if the specified point lies within the interior or on the boundary of the specified
   * polygon" - inside the exterior ring, and not strictly inside any interior ring (a point on a
   * hole's own edge is on the polygon's boundary and therefore inside).
   */
  private boolean pointInPolygon(final Point point, final Polygon polygon) {
    if (!inRing(point, polygon.getExterior())) {
      return false;
    }
    for (int i = 0; i < polygon.getNumberOfInteriorRings(); i++) {
      final ComposedGeospatial<Point> hole = polygon.getInterior(i);
      if (inRing(point, hole) && !onRing(point, hole)) {
        return false;
      }
    }
    return true;
  }

  /** Even-odd ray casting, with an explicit boundary test first so edges and vertices count as in. */
  private boolean inRing(final Point point, final ComposedGeospatial<Point> ring) {
    final List<Point> pts = pointsOf(ring);
    if (pts.size() < 3) {
      return false;
    }
    if (onRing(point, ring)) {
      return true;
    }
    boolean inside = false;
    for (int i = 0, j = pts.size() - 1; i < pts.size(); j = i++) {
      final double xi = pts.get(i).getX();
      final double yi = pts.get(i).getY();
      final double xj = pts.get(j).getX();
      final double yj = pts.get(j).getY();
      if (yi > point.getY() != yj > point.getY()
          && point.getX() < (xj - xi) * (point.getY() - yi) / (yj - yi) + xi) {
        inside = !inside;
      }
    }
    return inside;
  }

  private boolean onRing(final Point point, final ComposedGeospatial<Point> ring) {
    final List<Point> pts = pointsOf(ring);
    for (int i = 0, j = pts.size() - 1; i < pts.size(); j = i++) {
      if (onSegment(point, pts.get(j), pts.get(i))) {
        return true;
      }
    }
    return false;
  }

  private boolean onSegment(final Point point, final Point a, final Point b) {
    final double cross = (b.getX() - a.getX()) * (point.getY() - a.getY())
        - (b.getY() - a.getY()) * (point.getX() - a.getX());
    if (Math.abs(cross) > EPSILON) {
      return false;
    }
    return Math.min(a.getX(), b.getX()) - EPSILON <= point.getX()
        && point.getX() <= Math.max(a.getX(), b.getX()) + EPSILON
        && Math.min(a.getY(), b.getY()) - EPSILON <= point.getY()
        && point.getY() <= Math.max(a.getY(), b.getY()) + EPSILON;
  }

  private List<Point> pointsOf(final ComposedGeospatial<Point> ring) {
    final List<Point> points = new ArrayList<>();
    for (final Point point : ring) {
      points.add(point);
    }
    return points;
  }

  private void requireSameDimension(final String function, final Geospatial first, final Geospatial second)
      throws ODataApplicationException {
    if (first.getDimension() != second.getDimension()) {
      throw notImplemented(function,
          first.getDimension() + " and " + second.getDimension() + " operands cannot be mixed");
    }
  }

  private <T extends Geospatial> T geoValue(final String function, final int index, final Class<T> expected)
      throws ODataApplicationException {
    final VisitorOperand operand = parameters.get(index);
    final Object value = operand.asTypedOperand().getValue();
    if (value == null) {
      return null;
    }
    if (!expected.isInstance(value)) {
      throw notImplemented(function, "parameter " + (index + 1) + " is a "
          + value.getClass().getSimpleName() + ", expected a " + expected.getSimpleName());
    }
    return expected.cast(value);
  }

  private ODataApplicationException notImplemented(final String function, final String detail) {
    return new ODataApplicationException(
        "The reference service does not implement this overload of " + function + ": " + detail + ".",
        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
  }
}
