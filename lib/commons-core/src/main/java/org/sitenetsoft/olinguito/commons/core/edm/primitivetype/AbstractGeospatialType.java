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
 * Copyright 2026 SiteNetSoft - Use Locale.ROOT for case conversion
 * Copyright 2026 SiteNetSoft - Cached compiled regex patterns for split()
 * Copyright 2026 SiteNetSoft - OLINGO-1440: Fix polygon parsing without interior rings
 * Copyright 2026 SiteNetSoft - OLINGO-918: Implement the ABNF geo URI literal grammar
 */
package org.sitenetsoft.olinguito.commons.core.edm.primitivetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.geo.ComposedGeospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial.Dimension;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial.Type;
import org.sitenetsoft.olinguito.commons.api.edm.geo.GeospatialCollection;
import org.sitenetsoft.olinguito.commons.api.edm.geo.LineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiLineString;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiPoint;
import org.sitenetsoft.olinguito.commons.api.edm.geo.MultiPolygon;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Polygon;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;

public abstract class AbstractGeospatialType<T extends Geospatial> extends SingletonPrimitiveType {

  private static final Pattern PATTERN =
      Pattern.compile("([a-z]+)'SRID=([0-9]+);([a-zA-Z]+)\\((.*)\\)'");

  private static final Pattern COLLECTION_PATTERN =
      Pattern.compile("([a-z]+)'SRID=([0-9]+);Collection\\(([a-zA-Z]+)\\((.*)\\)\\)'");

  /**
   * [OData-ABNF] doubleValue, restricted to the forms that can appear inside a positionLiteral.
   */
  private static final String DOUBLE_VALUE =
      "(?:-?INF|NaN|[+-]?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)";

  /**
   * [OData-ABNF] positionLiteral = doubleValue SP doubleValue [ SP doubleValue ] [ SP doubleValue ]
   * - longitude, latitude, optional altitude/elevation, optional linear referencing measure.
   */
  private static final String POSITION_LITERAL =
      DOUBLE_VALUE + " " + DOUBLE_VALUE + "(?: " + DOUBLE_VALUE + "){0,2}";

  /** [OData-ABNF] pointData = OPEN positionLiteral CLOSE. */
  private static final String POINT_DATA = "\\(" + POSITION_LITERAL + "\\)";

  /** [OData-ABNF] lineStringData = OPEN positionLiteral 1*( COMMA positionLiteral ) CLOSE. */
  private static final String LINE_STRING_DATA =
      "\\(" + POSITION_LITERAL + "(?:," + POSITION_LITERAL + ")+\\)";

  /** [OData-ABNF] ringLiteral = OPEN positionLiteral *( COMMA positionLiteral ) CLOSE. */
  private static final String RING_LITERAL =
      "\\(" + POSITION_LITERAL + "(?:," + POSITION_LITERAL + ")*\\)";

  /** [OData-ABNF] polygonData = OPEN ringLiteral *( COMMA ringLiteral ) CLOSE. */
  private static final String POLYGON_DATA = "\\(" + RING_LITERAL + "(?:," + RING_LITERAL + ")*\\)";

  /**
   * [OData-ABNF] the six non-recursive geoLiteral alternatives: pointLiteral, lineStringLiteral,
   * multiPointLiteral, multiLineStringLiteral, polygonLiteral and multiPolygonLiteral. The three
   * multi* rules bracket their whole body, so an empty body is legal for them and only for them.
   */
  private static final String SIMPLE_GEO_LITERAL = "(?:"
      + "Point" + POINT_DATA
      + "|LineString" + LINE_STRING_DATA
      + "|MultiPoint\\((?:" + POINT_DATA + "(?:," + POINT_DATA + ")*)?\\)"
      + "|MultiLineString\\((?:" + LINE_STRING_DATA + "(?:," + LINE_STRING_DATA + ")*)?\\)"
      + "|Polygon" + POLYGON_DATA
      + "|MultiPolygon\\((?:" + POLYGON_DATA + "(?:," + POLYGON_DATA + ")*)?\\)"
      + ")";

  /**
   * [OData-ABNF] collectionLiteral = "Collection(" geoLiteral *( COMMA geoLiteral ) CLOSE, where
   * geoLiteral may itself be a collectionLiteral. Java regular expressions are not recursive, so the
   * nesting is unrolled to the two levels this implementation is able to parse.
   */
  private static final String INNER_COLLECTION_LITERAL =
      "Collection\\(" + SIMPLE_GEO_LITERAL + "(?:," + SIMPLE_GEO_LITERAL + ")*\\)";

  private static final String GEO_LITERAL =
      "(?:" + SIMPLE_GEO_LITERAL + "|" + INNER_COLLECTION_LITERAL + ")";

  private static final String COLLECTION_LITERAL =
      "Collection\\(" + GEO_LITERAL + "(?:," + GEO_LITERAL + ")*\\)";

  /**
   * [OData-ABNF] geographyPoint = geographyPrefix SQUOTE fullPointLiteral SQUOTE and its siblings,
   * with fullXLiteral = sridLiteral xLiteral and sridLiteral = "SRID" EQ 1*5DIGIT SEMI.
   */
  private static final Pattern URI_LITERAL_PATTERN = Pattern.compile(
      "(?:geography|geometry)'SRID=[0-9]{1,5};(?:" + GEO_LITERAL + "|" + COLLECTION_LITERAL + ")'");

  private static final Pattern POLYGON_SEPARATOR = Pattern.compile("\\),\\(");
  private static final Pattern MULTI_POLYGON_SEPARATOR = Pattern.compile("\\)\\),\\(\\(");

  private final Class<T> reference;

  protected final Dimension dimension;

  protected final Type type;

  protected AbstractGeospatialType(final Class<T> reference, final Dimension dimension, final Type type) {
    this.reference = reference;
    this.dimension = dimension;
    this.type = type;
  }

  @Override
  public Class<?> getDefaultType() {
    return reference;
  }

  /**
   * The URL-literal prefix of this type's dimension: [OData-ABNF] geographyPrefix = "geography" /
   * geometryPrefix = "geometry", each followed by SQUOTE.
   */
  private String uriPrefix() {
    return dimension.name().toLowerCase(Locale.ROOT) + '\'';
  }

  /**
   * Prepends the mandatory sridLiteral when the body has none. [OData-ABNF] never brackets
   * sridLiteral inside a full*Literal rule, so the prefix is required; [OData-CSDL] section 7.2.6
   * supplies the value when the caller did not: "the facet defaults to 0 for Geometry types or 4326
   * for Geography types".
   */
  private String withSridPrefix(final String body) {
    return body.regionMatches(true, 0, "SRID=", 0, 5)
        ? body
        : "SRID=" + (dimension == Dimension.GEOMETRY ? "0" : "4326") + ';' + body;
  }

  /**
   * Renders a geo literal in the URL form [OData-ABNF] defines for it,
   * <code>geographyPrefix SQUOTE fullPointLiteral SQUOTE</code> and its siblings. Both input shapes
   * are accepted: the already-wrapped form (which is what {@code valueToString} produces on this
   * type) is returned unchanged, and the bare full*Literal form used in payloads and CSDL
   * DefaultValue is wrapped, acquiring the SRID facet default if it carries no sridLiteral.
   */
  @Override
  public String toUriLiteral(final String literal) {
    if (literal == null) {
      return null;
    }
    final String prefix = uriPrefix();
    if (literal.startsWith(prefix) && literal.endsWith("'")) {
      return literal;
    }
    return prefix + withSridPrefix(literal) + '\'';
  }

  /**
   * Normalises a geo URL literal into the string {@code valueOfString} consumes on this type, which
   * is the prefixed, single-quoted form (both PATTERNs above require it). The bare full*Literal form
   * is accepted and wrapped; anything that is not a geo literal at all is rejected here rather than
   * silently handed on, which is what the inherited identity implementation used to do.
   */
  @Override
  public String fromUriLiteral(final String literal) throws EdmPrimitiveTypeException {
    if (literal == null) {
      return null;
    }
    final String prefix = uriPrefix();
    final String body;
    if (literal.startsWith(prefix) && literal.endsWith("'") && literal.length() > prefix.length()) {
      body = literal.substring(prefix.length(), literal.length() - 1);
    } else if (!literal.startsWith("geography'") && !literal.startsWith("geometry'")) {
      body = withSridPrefix(literal);
    } else {
      throw new EdmPrimitiveTypeException("The literal '" + literal + "' has illegal content.");
    }
    final String normalized = prefix + body + '\'';
    if (!URI_LITERAL_PATTERN.matcher(normalized).matches()) {
      throw new EdmPrimitiveTypeException("The literal '" + literal + "' has illegal content.");
    }
    return normalized;
  }

  private Matcher getMatcher(final Pattern pattern, final String value) throws EdmPrimitiveTypeException {
    final Matcher matcher = pattern.matcher(value);
    if (!matcher.matches()) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
    }

    Geospatial.Dimension _dimension = null;
    Geospatial.Type _type = null;
    try {
      _dimension = Geospatial.Dimension.valueOf(matcher.group(1).toUpperCase(Locale.ROOT));
      _type = Geospatial.Type.valueOf(matcher.group(3).toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.", e);
    }
    if (_dimension != this.dimension || (!pattern.equals(COLLECTION_PATTERN) && _type != this.type)) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
    }

    return matcher;
  }

  private Point newPoint(final SRID srid, final String point, final Boolean isNullable,
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode)
          throws EdmPrimitiveTypeException {

    final List<String> pointCoo = split(point, ' ');
    // [OData-ABNF] positionLiteral = doubleValue SP doubleValue [ SP doubleValue ] [ SP doubleValue ]
    // - longitude, latitude, optional altitude/elevation, optional linear referencing measure.
    if (pointCoo == null || pointCoo.size() < 2 || pointCoo.size() > 4) {
      throw new EdmPrimitiveTypeException("The literal '" + point + "' has illegal content.");
    }

    final Point result = new Point(this.dimension, srid);
    result.setX(EdmDouble.getInstance().valueOfString(pointCoo.get(0),
        isNullable, maxLength, precision, scale, isUnicode, Double.class));
    result.setY(EdmDouble.getInstance().valueOfString(pointCoo.get(1),
        isNullable, maxLength, precision, scale, isUnicode, Double.class));
    if (pointCoo.size() > 2) {
      result.setZ(EdmDouble.getInstance().valueOfString(pointCoo.get(2),
          isNullable, maxLength, precision, scale, isUnicode, Double.class));
    }
    // A fourth element is validated by being parsed and is then dropped: Point carries x, y and z
    // only, and [GeoJSON] section 3.1.1 SHOULD NOTs positions beyond three elements, so it is never
    // emitted again.
    if (pointCoo.size() > 3) {
      EdmDouble.getInstance().valueOfString(pointCoo.get(3),
          isNullable, maxLength, precision, scale, isUnicode, Double.class);
    }

    return result;
  }

  protected Point stringToPoint(final String value, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    final Matcher matcher = getMatcher(PATTERN, value);

    return newPoint(SRID.valueOf(matcher.group(2)), matcher.group(4),
        isNullable, maxLength, precision, scale, isUnicode);
  }

  protected MultiPoint stringToMultiPoint(final String value, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    final Matcher matcher = getMatcher(PATTERN, value);

    final List<Point> points = new ArrayList<>();
    for (final String pointCoo : split(matcher.group(4), ',')) {
      points.add(newPoint(null, pointCoo.substring(1, pointCoo.length() - 1),
          isNullable, maxLength, precision, scale, isUnicode));
    }

    return new MultiPoint(dimension, SRID.valueOf(matcher.group(2)), points);
  }

  private LineString newLineString(final SRID srid, final String lineString, final Boolean isNullable,
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode)
          throws EdmPrimitiveTypeException {

    final List<Point> points = new ArrayList<>();
    for (final String pointCoo : split(lineString, ',')) {
      points.add(newPoint(null, pointCoo, isNullable, maxLength, precision, scale, isUnicode));
    }

    return new LineString(this.dimension, srid, points);
  }

  protected LineString stringToLineString(final String value, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    final Matcher matcher = getMatcher(PATTERN, value);

    return newLineString(SRID.valueOf(matcher.group(2)), matcher.group(4),
        isNullable, maxLength, precision, scale, isUnicode);
  }

  protected MultiLineString stringToMultiLineString(final String value, final Boolean isNullable,
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode)
          throws EdmPrimitiveTypeException {

    final Matcher matcher = getMatcher(PATTERN, value);

    final List<LineString> lineStrings = new ArrayList<>();
    for (String coo : matcher.group(4).contains("),(")
        ? POLYGON_SEPARATOR.split(matcher.group(4)) : new String[] { matcher.group(4) }) {

      String lineString = coo;
      if (lineString.charAt(0) == '(') {
        lineString = lineString.substring(1);
      }
      if (lineString.endsWith(")")) {
        lineString = lineString.substring(0, lineString.length() - 1);
      }

      lineStrings.add(newLineString(null, lineString, isNullable, maxLength, precision, scale, isUnicode));
    }

    return new MultiLineString(this.dimension, SRID.valueOf(matcher.group(2)), lineStrings);
  }

  private Polygon newPolygon(final SRID srid, final String polygon, final Boolean isNullable,
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode)
          throws EdmPrimitiveTypeException {

    final String[] first = POLYGON_SEPARATOR.split(polygon);

    final List<LineString> interiorRings = new ArrayList<>();
    for (int i = 0; i < first.length -1; i++) {
    	List<Point> interior = new ArrayList<>();
	    for (final String pointCoo : split(first[i].substring(i==0?1:0, first[i].length()), ',')) {
	      interior.add(newPoint(null, pointCoo, isNullable, maxLength, precision, scale, isUnicode));
	    }
	    interiorRings.add(new LineString(dimension, srid, interior));
    }
    final List<Point> exterior = new ArrayList<>();
    String exteriorRing = first[first.length - 1];
    int startIdx = (first.length == 1 && !exteriorRing.isEmpty() && exteriorRing.charAt(0) == '(') ? 1 : 0;
    for (final String pointCoo : split(exteriorRing.substring(startIdx, exteriorRing.length() - 1), ',')) {
      exterior.add(newPoint(null, pointCoo, isNullable, maxLength, precision, scale, isUnicode));
    }

    return new Polygon(dimension, srid, interiorRings, new LineString(dimension, srid, exterior));
  }

  protected Polygon stringToPolygon(final String value, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    final Matcher matcher = getMatcher(PATTERN, value);

    return newPolygon(SRID.valueOf(matcher.group(2)), matcher.group(4),
        isNullable, maxLength, precision, scale, isUnicode);
  }

  protected MultiPolygon stringToMultiPolygon(final String value, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    final Matcher matcher = getMatcher(PATTERN, value);

    final List<Polygon> polygons = new ArrayList<>();
    for (String coo : matcher.group(4).contains(")),((") ?
        MULTI_POLYGON_SEPARATOR.split(matcher.group(4)) :
        new String[] { matcher.group(4) }) {

      String polygon = coo;
      if (polygon.startsWith("((")) {
        polygon = polygon.substring(1);
      }
      if (polygon.endsWith("))")) {
        polygon = polygon.substring(0, polygon.length() - 1);
      }
      if (polygon.charAt(0) != '(') {
        polygon = "(" + polygon;
      }
      if (!polygon.endsWith(")")) {
        polygon += ")";
      }

      polygons.add(newPolygon(null, polygon, isNullable, maxLength, precision, scale, isUnicode));
    }

    return new MultiPolygon(dimension, SRID.valueOf(matcher.group(2)), polygons);
  }

  protected GeospatialCollection stringToCollection(final String value, final Boolean isNullable,
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode)
          throws EdmPrimitiveTypeException {

    final Matcher matcher = getMatcher(COLLECTION_PATTERN, value);

    Geospatial item = null;
    switch (Geospatial.Type.valueOf(matcher.group(3).toUpperCase(Locale.ROOT))) {
    case POINT:
      item = newPoint(SRID.valueOf(matcher.group(2)), matcher.group(4),
          isNullable, maxLength, precision, scale, isUnicode);
      break;

    case MULTIPOINT:
      final List<Point> points = new ArrayList<>();
      for (final String pointCoo : split(matcher.group(4), ',')) {
        points.add(newPoint(null, pointCoo.substring(1, pointCoo.length() - 1),
            isNullable, maxLength, precision, scale, isUnicode));
      }

      item = new MultiPoint(dimension, SRID.valueOf(matcher.group(2)), points);
      break;

    case LINESTRING:
      item = newLineString(SRID.valueOf(matcher.group(2)), matcher.group(4),
          isNullable, maxLength, precision, scale, isUnicode);
      break;

    case MULTILINESTRING:
      final List<LineString> lineStrings = new ArrayList<>();
      for (final String coo : split(matcher.group(4), ',')) {
        lineStrings.add(newLineString(null, coo.substring(1, coo.length() - 1),
            isNullable, maxLength, precision, scale, isUnicode));
      }

      item = new MultiLineString(this.dimension, SRID.valueOf(matcher.group(2)), lineStrings);
      break;

    case POLYGON:
      item = newPolygon(SRID.valueOf(matcher.group(2)), matcher.group(4),
          isNullable, maxLength, precision, scale, isUnicode);
      break;

    case MULTIPOLYGON:
      final List<Polygon> polygons = new ArrayList<>();
      for (final String coo : split(matcher.group(4), ',')) {
        polygons.add(newPolygon(null, coo.substring(1, coo.length() - 1),
            isNullable, maxLength, precision, scale, isUnicode));
      }

      item = new MultiPolygon(dimension, SRID.valueOf(matcher.group(2)), polygons);
      break;

    default:
    }

    return new GeospatialCollection(dimension, SRID.valueOf(matcher.group(2)),
        Collections.singletonList(item));
  }

  private StringBuilder toStringBuilder(final SRID srid) {
    return new StringBuilder(dimension.name().toLowerCase(Locale.ROOT)).append('\'').
        append("SRID=").append(srid).append(';');
  }

  private String point(final Point point, final Boolean isNullable,
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode)
          throws EdmPrimitiveTypeException {

    final StringBuilder result = new StringBuilder()
        .append(EdmDouble.getInstance().valueToString(point.getX(),
            isNullable, maxLength, precision, scale, isUnicode))
        .append(' ')
        .append(EdmDouble.getInstance().valueToString(point.getY(),
            isNullable, maxLength, precision, scale, isUnicode));
    // Same rule the GeoJSON writer applies: an altitude is written only when it is set.
    if (point.getZ() != 0) {
      result.append(' ').append(EdmDouble.getInstance().valueToString(point.getZ(),
          isNullable, maxLength, precision, scale, isUnicode));
    }
    return result.toString();
  }

  protected String toString(final Point point, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (dimension != point.getDimension()) {
      throw new EdmPrimitiveTypeException("The value '" + point + "' is not valid.");
    }

    return toStringBuilder(point.getSrid()).
        append(reference.getSimpleName()).
        append('(').
        append(point(point, isNullable, maxLength, precision, scale, isUnicode)).
        append(")'").
        toString();
  }

  protected String toString(final MultiPoint multiPoint, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (dimension != multiPoint.getDimension()) {
      throw new EdmPrimitiveTypeException("The value '" + multiPoint + "' is not valid.");
    }

    final StringBuilder result = toStringBuilder(multiPoint.getSrid()).
        append(reference.getSimpleName()).
        append('(');

    for (final Iterator<Point> itor = multiPoint.iterator(); itor.hasNext();) {
      result.append('(').
      append(point(itor.next(), isNullable, maxLength, precision, scale, isUnicode)).
      append(')');
      if (itor.hasNext()) {
        result.append(',');
      }
    }

    return result.append(")'").toString();
  }

  private StringBuilder appendPoints(final ComposedGeospatial<Point> points, final Boolean isNullable, 
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode, 
      final StringBuilder result) throws EdmPrimitiveTypeException {
	for (final Iterator<Point> itor = points.iterator(); itor.hasNext();) {
      result.append(point(itor.next(), isNullable, maxLength, precision, scale, isUnicode));
      if (itor.hasNext()) {
        result.append(',');
      }
    }
	return result;
}

  protected String toString(final LineString lineString, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (dimension != lineString.getDimension()) {
      throw new EdmPrimitiveTypeException("The value '" + lineString + "' is not valid.");
    }

    StringBuilder builder = toStringBuilder(lineString.getSrid()).
        append(reference.getSimpleName()).
        append('(');
    return appendPoints(lineString, isNullable, maxLength, precision, scale, isUnicode, builder).
        append(")'").toString();
  }

  protected String toString(final MultiLineString multiLineString, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (dimension != multiLineString.getDimension()) {
      throw new EdmPrimitiveTypeException("The value '" + multiLineString + "' is not valid.");
    }

    final StringBuilder result = toStringBuilder(multiLineString.getSrid()).
        append(reference.getSimpleName()).
        append('(');

    for (final Iterator<LineString> itor = multiLineString.iterator(); itor.hasNext();) {
      result.append('(');
      appendPoints(itor.next(), isNullable, maxLength, precision, scale, isUnicode, result).
      append(')');
      if (itor.hasNext()) {
        result.append(',');
      }
    }

    return result.append(")'").toString();
  }

  private String polygon(final Polygon polygon, final Boolean isNullable,
      final Integer maxLength, final Integer precision, final Integer scale, final Boolean isUnicode)
          throws EdmPrimitiveTypeException {

    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < polygon.getNumberOfInteriorRings(); i++) {
	    result.append('(');
	    appendPoints(polygon.getInterior(i), isNullable, maxLength, precision, scale, isUnicode, result);
	    result.append("),");
    }
    
    result.append('(');
    appendPoints(polygon.getExterior(), isNullable, maxLength, precision, scale, isUnicode, result);

    return result.append(')').toString();
  }

  protected String toString(final Polygon polygon, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (dimension != polygon.getDimension()) {
      throw new EdmPrimitiveTypeException("The value '" + polygon + "' is not valid.");
    }

    return toStringBuilder(polygon.getSrid()).
        append(reference.getSimpleName()).
        append('(').
        append(polygon(polygon, isNullable, maxLength, precision, scale, isUnicode)).
        append(")'").toString();
  }

  protected String toString(final MultiPolygon multiPolygon, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (dimension != multiPolygon.getDimension()) {
      throw new EdmPrimitiveTypeException("The value '" + multiPolygon + "' is not valid.");
    }

    final StringBuilder result = toStringBuilder(multiPolygon.getSrid()).
        append(reference.getSimpleName()).
        append('(');

    for (final Iterator<Polygon> itor = multiPolygon.iterator(); itor.hasNext();) {
      result.append('(').
      append(polygon(itor.next(), isNullable, maxLength, precision, scale, isUnicode)).
      append(')');
      if (itor.hasNext()) {
        result.append(',');
      }
    }

    return result.append(")'").toString();
  }

  protected String toString(final GeospatialCollection collection, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (dimension != collection.getDimension()) {
      throw new EdmPrimitiveTypeException("The value '" + collection + "' is not valid.");
    }

    StringBuilder result = toStringBuilder(collection.getSrid()).append("Collection(");

    if (collection.iterator().hasNext()) {
      final Geospatial item = collection.iterator().next();
      result.append(item.getClass().getSimpleName()).append('(');

      switch (item.getEdmPrimitiveTypeKind()) {
      case GeographyPoint:
      case GeometryPoint:
        result.append(point((Point) item, isNullable, maxLength, precision, scale, isUnicode));
        break;

      case GeographyMultiPoint:
      case GeometryMultiPoint:
        for (final Iterator<Point> itor = ((MultiPoint) item).iterator(); itor.hasNext();) {
          result.append('(').
          append(point(itor.next(), isNullable, maxLength, precision, scale, isUnicode)).
          append(')');
          if (itor.hasNext()) {
            result.append(',');
          }
        }
        break;

      case GeographyLineString:
      case GeometryLineString:
        appendPoints((LineString) item, isNullable, maxLength, precision, scale, isUnicode, result);
        break;

      case GeographyMultiLineString:
      case GeometryMultiLineString:
        for (final Iterator<LineString> itor = ((MultiLineString) item).iterator(); itor.hasNext();) {
          result.append('(');
          appendPoints(itor.next(), isNullable, maxLength, precision, scale, isUnicode, result).
          append(')');
          if (itor.hasNext()) {
            result.append(',');
          }
        }
        break;

      case GeographyPolygon:
      case GeometryPolygon:
        result.append(polygon((Polygon) item, isNullable, maxLength, precision, scale, isUnicode));
        break;

      case GeographyMultiPolygon:
      case GeometryMultiPolygon:
        for (final Iterator<Polygon> itor = ((MultiPolygon) item).iterator(); itor.hasNext();) {
          result.append('(').
          append(polygon(itor.next(), isNullable, maxLength, precision, scale, isUnicode)).
          append(')');
          if (itor.hasNext()) {
            result.append(',');
          }
        }
        break;

      default:
      }

      result.append(')');
    }

    return result.append(")'").toString();
  }

  private List<String> split(final String input, final char separator) {
    if (input == null) {
      return null;
    }
    List<String> list = new ArrayList<>();
    int start = 0;
    int end;
    while ((end = input.indexOf(separator, start)) >= 0) {
      list.add(input.substring(start, end));
      start = end + 1;
    }
    if (start < input.length()) {
      list.add(input.substring(start));
    }
    return list;
  }
}
