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
 * Copyright 2026 SiteNetSoft - Code quality improvements; replaced commons-codec Hex with HexFormat
 * Copyright 2026 SiteNetSoft - Removed HttpComponents dependency; pure Java URI construction
 */
package org.sitenetsoft.olinguito.client.core.uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.datatype.Duration;

import java.util.HexFormat;
import org.sitenetsoft.olinguito.commons.core.Encoder;
import org.sitenetsoft.olinguito.client.core.StringHelper;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.domain.ClientValue;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.api.http.WrappingHttpClientFactory;
import org.sitenetsoft.olinguito.client.api.uri.SegmentType;
import org.sitenetsoft.olinguito.client.core.http.BasicAuthHttpClientFactory;
import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmBinary;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmDecimal;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmDouble;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmDuration;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmInt64;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmSingle;

/**
 * URI utilities.
 */
public final class URIUtils {

  /**
   * Logger.
   */

  private static final Pattern ENUM_VALUE = Pattern.compile("([^']+\\.[^']+)?'[^']+'");
  private static final String URI_OPTIONS = "/$";

  private URIUtils() {
    // Empty private constructor for static utility classes
  }

  /**
   * Build URI starting from the given base and href.
   * <br/>
   * If href is absolute or base is null then base will be ignored.
   *
   * @param base URI prefix.
   * @param href URI suffix.
   * @return built URI.
   */
  public static URI getURI(final String base, final String href) {
    if (href == null) {
      throw new IllegalArgumentException("Null link provided");
    }

    URI uri = URI.create(href);

    if (!uri.isAbsolute() && base != null) {
      uri = URI.create(base + "/" + href);
    }

    return uri.normalize();
  }

  /**
   * Build URI starting from the given base and href.
   * <br/>
   * If href is absolute or base is null then base will be ignored.
   *
   * @param base URI prefix.
   * @param href URI suffix.
   * @return built URI.
   */
  public static URI getURI(final URI base, final URI href) {
    if (href == null) {
      throw new IllegalArgumentException("Null link provided");
    }
    return getURI(base, href.toASCIIString());
  }

  /**
   * Build URI starting from the given base and href.
   * <br/>
   * If href is absolute or base is null then base will be ignored.
   *
   * @param base URI prefix.
   * @param href URI suffix.
   * @return built URI.
   */
  public static URI getURI(final URI base, final String href) {
    if (href == null) {
      throw new IllegalArgumentException("Null link provided");
    }

    URI uri = URI.create(href);

    if (!uri.isAbsolute() && base != null) {
      uri = URI.create(base.toASCIIString() + "/" + href);
    }

    return uri.normalize();
  }

  private static String timestamp(final Timestamp timestamp)
      throws UnsupportedEncodingException, EdmPrimitiveTypeException {

    return Encoder.encode(EdmDateTimeOffset.getInstance().
        valueToString(timestamp, null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null));
  }

  private static String calendar(final Calendar calendar)
      throws UnsupportedEncodingException, EdmPrimitiveTypeException {

    return Encoder.encode(EdmDateTimeOffset.getInstance().
        valueToString(calendar, null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null));
  }

  private static String duration(final Duration duration)
      throws UnsupportedEncodingException, EdmPrimitiveTypeException {

    return EdmDuration.getInstance().toUriLiteral(Encoder.encode(EdmDuration.getInstance().
        valueToString(duration, null, null,
            Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null)));
  }

  private static String quoteString(final String string, final boolean singleQuoteEscape)
      throws UnsupportedEncodingException {

    return ENUM_VALUE.matcher(string).matches()
        ? string
        : singleQuoteEscape
            ? "'" + string + "'"
            : "\"" + string + "\"";
  }

  /**
   * Turns primitive values into their respective URI representation.
   *
   * @param obj primitive value
   * @return URI representation
   */
  public static String escape(final Object obj) {
    return escape(obj, true);
  }

  private static String escape(final Object obj, final boolean singleQuoteEscape) {
    String value;

    try {
      if (obj == null) {
        value = Constants.ATTR_NULL;
      } else if (obj instanceof Collection) {
        final StringBuilder buffer = new StringBuilder("[");
        for (@SuppressWarnings("unchecked")
        final Iterator<Object> itor = ((Collection<Object>) obj).iterator(); itor.hasNext();) {
          buffer.append(escape(itor.next(), false));
          if (itor.hasNext()) {
            buffer.append(',');
          }
        }
        buffer.append(']');

        value = buffer.toString();
      } else if (obj instanceof Map) {
        final StringBuilder buffer = new StringBuilder("{");
        for (@SuppressWarnings("unchecked")
        final Iterator<Map.Entry<String, Object>> itor =
            ((Map<String, Object>) obj).entrySet().iterator(); itor.hasNext();) {

          final Map.Entry<String, Object> entry = itor.next();
          buffer.append("\"").append(entry.getKey()).append("\"");
          buffer.append(':').append(escape(entry.getValue(), false));

          if (itor.hasNext()) {
            buffer.append(',');
          }
        }
        buffer.append('}');

        value = buffer.toString();
      } else if (obj instanceof ParameterAlias alias) {
        value = "@" + alias.getAlias();
      } else if (obj instanceof Boolean b) {
        value = Boolean.toString(b);
      } else if (obj instanceof UUID) {
        value = obj.toString();
      } else if (obj instanceof byte[] bytes) {
        value = EdmBinary.getInstance().toUriLiteral(HexFormat.of().formatHex(bytes));
      } else if (obj instanceof Timestamp ts) {
        value = timestamp(ts);
      } else if (obj instanceof Calendar cal) {
        value = calendar(cal);
      } else if (obj instanceof Duration dur) {
        value = duration(dur);
      } else if (obj instanceof BigDecimal) {
        value = EdmDecimal.getInstance().valueToString(obj, null, null,
            Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null);
      } else if (obj instanceof Double) {
        value = EdmDouble.getInstance().valueToString(obj, null, null,
            Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null);
      } else if (obj instanceof Float) {
        value = EdmSingle.getInstance().valueToString(obj, null, null,
            Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null);
      } else if (obj instanceof Long) {
        value = EdmInt64.getInstance().valueToString(obj, null, null,
            Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null);
      } else if (obj instanceof Geospatial geo) {
        value = Encoder.encode(EdmPrimitiveTypeFactory.getInstance(
            geo.getEdmPrimitiveTypeKind()).valueToString(geo, null, null,
            Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null));
      } else if (obj instanceof String s) {
        value = quoteString(s, singleQuoteEscape);
      } else {
        value = obj.toString();
      }
    } catch (final EdmPrimitiveTypeException | UnsupportedEncodingException e) {
      value = obj.toString();
    }

    return value;
  }

  public static boolean shouldUseRepeatableHttpBodyEntry(final ODataClient client) {
    // returns true for authentication request in case of http401 which needs retry so requires being repeatable.
    HttpClientFactory httpclientFactory = client.getConfiguration().getHttpClientFactory();
    if (httpclientFactory instanceof BasicAuthHttpClientFactory) {
      return true;
    } else if (httpclientFactory instanceof WrappingHttpClientFactory tmp) {
        return tmp.getWrappedHttpClientFactory() instanceof BasicAuthHttpClientFactory;
    }

    return false;
  }

  /**
   * Reads an input stream into a byte array, respecting the client's chunked encoding settings.
   *
   * @param client the OData client providing configuration
   * @param input the input stream to read
   * @return the input stream content as a byte array, or {@code null} if chunked encoding is used
   *         and the body should be streamed rather than buffered
   */
  public static byte[] readInputStreamBytes(final ODataClient client, final InputStream input) {
    boolean useChunked = client.getConfiguration().isUseChuncked();

    if (shouldUseRepeatableHttpBodyEntry(client) || !useChunked) {
      try {
        return input.readAllBytes();
      } catch (IOException e) {
        throw new ODataRuntimeException("While reading input for not chunked encoding", e);
      }
    }
    return null;
  }

  public static URI addValueSegment(final URI uri) {
    if (uri.getPath().endsWith(SegmentType.VALUE.getValue())) {
      return uri;
    }
    final String newPath = uri.getPath() + "/" + SegmentType.VALUE.getValue();
    try {
      return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
          newPath, uri.getQuery(), uri.getFragment());
    } catch (java.net.URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static URI buildFunctionInvokeURI(final URI uri, final Map<String, ClientValue> parameters) {
    final String rawQuery = uri.getRawQuery();
    String baseURI;
    String uriOption = "";
    String pathSegments = null;
    // Check if Query contains /$ and extract options like /$count, /$value and /$ref
    if (uri.toASCIIString().contains(URI_OPTIONS)) {
      uriOption = uri.toASCIIString().substring(uri.toASCIIString().indexOf(URI_OPTIONS),
          (rawQuery == null ? uri.toASCIIString().length() : uri.toASCIIString().indexOf(rawQuery) - 1));
    }
    if (rawQuery != null) {
      baseURI = StringHelper.substringBefore(uri.toASCIIString(), uriOption + "?" + rawQuery);
    } else if (!uriOption.isEmpty()) {
      baseURI = StringHelper.substringBefore(uri.toASCIIString(), uriOption);
    } else {
      baseURI = uri.toASCIIString();
    }
    if (baseURI.endsWith("()")) {
      baseURI = baseURI.substring(0, baseURI.length() - 2);
    } else {
      /*
       * If FunctionName is followed by a Navigation segment or Actions,
       * then get the substring till function name so that parameters can be appended to it.
       */
      int bracIndex = baseURI.indexOf("()");
      if (bracIndex != -1) {
        pathSegments = baseURI.substring(bracIndex + 2);
        baseURI = baseURI.substring(0, bracIndex);
      }
    }
    final StringBuilder inlineParams = new StringBuilder();
    for (Map.Entry<String, ClientValue> param : parameters.entrySet()) {
      inlineParams.append(param.getKey()).append("=");

      Object value = null;
      if (param.getValue().isPrimitive()) {
        value = param.getValue().asPrimitive().toValue();
      } else if (param.getValue().isComplex()) {
        value = param.getValue().asComplex().asJavaMap();
      } else if (param.getValue().isCollection()) {
        value = param.getValue().asCollection().asJavaCollection();
      } else if (param.getValue().isEnum()) {
        value = param.getValue().asEnum().toString();
      }

      inlineParams.append(URIUtils.escape(value)).append(',');
    }

    if (!inlineParams.isEmpty()) {
      inlineParams.deleteCharAt(inlineParams.length() - 1);
    }

    return URI.create(baseURI + "(" + Encoder.encode(inlineParams.toString()) + ")"
        + (pathSegments == null ? "" : pathSegments)
        + (!uriOption.isEmpty() ? "/" + Encoder.encode(uriOption.substring(1)) : "")
        + (rawQuery != null && !rawQuery.isBlank() ? "?" + rawQuery : ""));
  }
}
