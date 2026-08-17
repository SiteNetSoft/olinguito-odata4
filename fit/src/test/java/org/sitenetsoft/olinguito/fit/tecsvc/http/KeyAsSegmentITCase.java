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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 3: fit-level round trips against the
 * key-as-segment tecsvc endpoint (OData 4.01, Part 2: URL Conventions, section 4.3.6)
 * Copyright 2026 SiteNetSoft - OData 4.01: referential-constraint key predicates from the source entity
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.fit.AbstractBaseTestITCase;
import org.sitenetsoft.olinguito.fit.tecsvc.TecSvcConst;
import org.junit.Test;

/**
 * Raw-HTTP round trips against a real Tomcat instance for OData 4.01 key-as-segment URLs
 * (Part 2: URL Conventions, section 4.3.6). tecsvc publishes the very same technical service twice:
 * at {@link TecSvcConst#BASE_URI} with the default (parenthesized-key) convention, and at
 * {@link TecSvcConst#KEY_AS_SEGMENT_BASE_URI} with the key-as-segment convention switched on for
 * the whole service. These tests pin the segment-key parsing, the precedence rules that keep
 * {@code $}-segments and qualified operation names winning over a key interpretation, and that the
 * default endpoint is left untouched.
 */
public class KeyAsSegmentITCase extends AbstractBaseTestITCase {

  private static final String KAS_URI = TecSvcConst.KEY_AS_SEGMENT_BASE_URI + "/";
  private static final String DEFAULT_URI = TecSvcConst.BASE_URI + "/";
  private static final String APPLICATION_JSON = "application/json";

  /** A key segment is read as the entity key: ESAllPrim is seeded with the key 32767. */
  @Test
  public void singleKeySegment() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESAllPrim/32767");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the addressed entity must be served",
        readResponse(connection).contains("\"PropertyInt16\":32767"));
  }

  /**
   * A string key part is taken from the raw segment, without the single quotes the parenthesized
   * convention requires: ESTwoKeyNav is seeded with (PropertyInt16=1, PropertyString='1').
   */
  @Test
  public void stringKeySegmentWithoutQuotes() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESTwoKeyNav/1/1");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    final String body = readResponse(connection);
    assertTrue("the integer key part must be read from the first segment", body.contains("\"PropertyInt16\":1"));
    assertTrue("the string key part must be read unquoted from the second segment",
        body.contains("\"PropertyString\":\"1\""));
  }

  /** Multi-part keys are consumed in metadata order (PropertyInt16, then PropertyString). */
  @Test
  public void multiPartKeySegments() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESTwoKeyNav/1/2");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    final String body = readResponse(connection);
    assertTrue(body.contains("\"PropertyInt16\":1"));
    assertTrue("the second segment must fill the second key property", body.contains("\"PropertyString\":\"2\""));
  }

  /** A partially supplied compound key is not a valid entity address. */
  @Test
  public void multiPartIncompleteIsBadRequest() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESTwoKeyNav/1");
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    assertTrue("the response must be an OData error body", readResponse(connection).contains("\"error\""));
  }

  /**
   * A key property covered by a referential constraint of the navigation property is omitted from the
   * URL (URL Conventions section 4.3.6 MUST) and its value is taken from the source entity's
   * referencing property. ESKeyNav(1) has PropertyInt16 == 1, so the single segment '1' completes the
   * key of ESTwoKeyNav to (PropertyInt16 = 1, PropertyString = '1'). Both URL conventions must resolve
   * the very same entity.
   */
  @Test
  public void referentialConstraintKeyIsOmitted() throws Exception {
    final HttpURLConnection keyAsSegment = get(KAS_URI + "ESKeyNav/1/NavPropertyETTwoKeyNavMany/1");
    assertEquals(HttpStatusCode.OK.getStatusCode(), keyAsSegment.getResponseCode());
    final String body = readResponse(keyAsSegment);
    assertTrue("the referenced key property must come from the source entity",
        body.contains("\"PropertyInt16\":1"));
    assertTrue("the segment-supplied key property must be applied",
        body.contains("\"PropertyString\":\"1\""));

    final HttpURLConnection parenthesized = get(DEFAULT_URI + "ESKeyNav(1)/NavPropertyETTwoKeyNavMany('1')");
    assertEquals("omitting a constrained key must behave the same in both URL conventions",
        parenthesized.getResponseCode(), keyAsSegment.getResponseCode());
    assertEquals("both conventions must serve the very same entity",
        withoutContextUrl(readResponse(parenthesized)), withoutContextUrl(body));
  }

  /** An unmatchable segment for the free key part is a plain 404, not a 400. */
  @Test
  public void referentialConstraintKeyWithUnknownRemainderIsNotFound() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESKeyNav/1/NavPropertyETTwoKeyNavMany/9");
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), connection.getResponseCode());
  }

  /**
   * The constrained key property is not merely optional: it has no segment at all, so a second
   * segment is one key value too many and the URI does not parse.
   */
  @Test
  public void referentialConstraintKeyTakesExactlyOneSegment() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESKeyNav/1/NavPropertyETTwoKeyNavMany/1/1");
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    assertTrue("the URI parser, not the data layer, must reject the surplus segment",
        readResponse(connection).contains("The URI is malformed."));
  }

  /** A {@code $}-prefixed segment keeps its own meaning and is never read as a key. */
  @Test
  public void dollarSegmentStillWins() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESAllPrim/$count", "text/plain");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("ESAllPrim is seeded with four entities", "4", readResponse(connection).trim());
  }

  /** A segment carrying a namespace-qualified operation name is a bound operation, not a key. */
  @Test
  public void qualifiedBoundOperationStillWins() throws Exception {
    final HttpURLConnection connection =
        get(KAS_URI + "ESTwoKeyNav/olingo.odata.test1.BFCESTwoKeyNavRTTwoKeyNav()");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the bound function must be invoked", readResponse(connection).contains("\"PropertyInt16\":1"));
  }

  /** A segment that cannot be converted to the key property's type is a bad request, not a 404. */
  @Test
  public void keySegmentTypeMismatchIsBadRequest() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESAllPrim/abc");
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    assertTrue("the response must be an OData error body", readResponse(connection).contains("\"error\""));
  }

  /** Switching key-as-segment on does not withdraw the parenthesized convention. */
  @Test
  public void parenthesizedKeyStillWorksOnKeyAsSegmentEndpoint() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESAllPrim(32767)");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue(readResponse(connection).contains("\"PropertyInt16\":32767"));
  }

  /**
   * The key-as-segment endpoint must serve exactly what the parenthesized address serves on the
   * default endpoint; the only difference is the relative context URL, which has to climb one more
   * path level because the key occupies a segment of its own.
   */
  @Test
  public void keyAsSegmentServesTheSameEntityAsTheParenthesizedAddress() throws Exception {
    final HttpURLConnection keyAsSegment = get(KAS_URI + "ESAllPrim/32767");
    assertEquals(HttpStatusCode.OK.getStatusCode(), keyAsSegment.getResponseCode());
    final String keyAsSegmentBody = readResponse(keyAsSegment);
    assertTrue("the context URL must be relative to the deeper path",
        keyAsSegmentBody.contains("\"@odata.context\":\"../$metadata#ESAllPrim/$entity\""));

    final HttpURLConnection parenthesized = get(DEFAULT_URI + "ESAllPrim(32767)");
    assertEquals(HttpStatusCode.OK.getStatusCode(), parenthesized.getResponseCode());
    assertEquals("apart from the context URL both URL shapes must serve the very same entity",
        readResponse(parenthesized),
        keyAsSegmentBody.replace("\"@odata.context\":\"../$metadata", "\"@odata.context\":\"$metadata"));
  }

  /** Without the flag a key segment is still just an unknown property/navigation name. */
  @Test
  public void defaultEndpointRejectsKeySegment() throws Exception {
    final HttpURLConnection connection = get(DEFAULT_URI + "ESAllPrim/32767");
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    assertTrue("the response must be an OData error body", readResponse(connection).contains("\"error\""));
  }

  /**
   * The per-entity-set model flag (the fork's non-standard {@code keyAsSegmentAllowed} CSDL flag, set
   * on tecsvc's {@code ESKeyAsSegmentInt}) keeps working on the default endpoint even though the
   * service-wide flag is off there. tecsvc seeds no data for that entity set, so the request gets as
   * far as the data provider and fails there rather than in the URI parser.
   */
  @Test
  public void defaultEndpointHonoursModelFlag() throws Exception {
    final HttpURLConnection connection = get(DEFAULT_URI + "ESKeyAsSegmentInt/1");
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), connection.getResponseCode());
    assertTrue("the response must be an OData error body", readResponse(connection).contains("\"error\""));
  }

  /**
   * Key-as-segment addresses are writable, not read-only conveniences. The update is tunnelled
   * through POST because {@link HttpURLConnection} refuses to send PATCH; tecsvc keeps the modified
   * data in the HTTP session, so the verifying read has to carry the same session cookie.
   */
  @Test
  public void writeThroughKeySegment() throws Exception {
    final HttpURLConnection patch = connect(HttpMethod.POST, KAS_URI + "ESAllPrim/32767", APPLICATION_JSON,
        "{\"PropertyString\":\"key as segment\"}", null, HttpMethod.PATCH);
    assertEquals(HttpStatusCode.OK.getStatusCode(), patch.getResponseCode());
    assertTrue("tecsvc answers an update with the updated entity",
        readResponse(patch).contains("\"PropertyString\":\"key as segment\""));
    final String session = patch.getHeaderField("Set-Cookie");

    final HttpURLConnection connection =
        connect(HttpMethod.GET, KAS_URI + "ESAllPrim/32767", APPLICATION_JSON, null, session, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the patched value must be visible through the same session",
        readResponse(connection).contains("\"PropertyString\":\"key as segment\""));
  }

  private HttpURLConnection get(final String uri) throws IOException {
    return get(uri, APPLICATION_JSON);
  }

  private HttpURLConnection get(final String uri, final String accept) throws IOException {
    return connect(HttpMethod.GET, uri, accept, null, null, null);
  }

  private HttpURLConnection connect(final HttpMethod method, final String uri, final String accept,
      final String body, final String cookie, final HttpMethod override) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
    connection.setRequestMethod(method.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, accept);
    if (override != null) {
      connection.setRequestProperty(HttpHeader.X_HTTP_METHOD_OVERRIDE, override.toString());
    }
    if (cookie != null) {
      connection.setRequestProperty("Cookie", cookie);
    }
    if (body != null) {
      connection.setRequestProperty(HttpHeader.CONTENT_TYPE, APPLICATION_JSON);
      connection.setDoOutput(true);
      final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
      writer.write(body);
      writer.close();
    }
    connection.connect();
    return connection;
  }

  /**
   * Strips the context URL from a response body. It is served relative to the request URL, so the
   * two URL conventions legitimately differ in how far it climbs up to $metadata; everything else
   * about the entity must be identical.
   */
  private static String withoutContextUrl(final String body) {
    return body.replaceFirst("\"@odata\\.context\":\"[^\"]*\",", "");
  }

  private static String readResponse(final HttpURLConnection connection) throws IOException {
    final InputStream stream = connection.getResponseCode() >= 400
        ? connection.getErrorStream() : connection.getInputStream();
    return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
  }

  @Override
  protected ODataClient getClient() {
    return null;
  }
}
