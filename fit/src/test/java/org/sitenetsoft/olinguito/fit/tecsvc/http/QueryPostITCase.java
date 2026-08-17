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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 7: fit-level round trips for /$query POST
 * requests through real Tomcat (OData 4.01 URL Conventions section 4.17)
 * Copyright 2026 SiteNetSoft - Pinned the /$query x omit-values=nulls cross-feature seam
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 follow-up Task 7: pinned the three-way /$query x
 * omit-values=nulls x matchesPattern composition
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * Raw-HTTP round trips for the OData 4.01 <tt>/$query</tt> resource path (URL Conventions section
 * 4.17), served by {@code ODataHandlerImpl.handleQueryPathIfPresent}. These tests talk directly to
 * a real Tomcat instance via {@link HttpURLConnection}, deliberately bypassing the OData client, so
 * that the server-side rewrite (POST body merged with any URL query options, then dispatched as the
 * equivalent GET) is exercised end to end exactly as a non-Olinguito client would see it.
 */
public class QueryPostITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + "/";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String APPLICATION_JSON = "application/json";

  @Test
  public void queryPostEquivalentToGet() throws Exception {
    final String get = readResponse(getConnection(HttpMethod.GET,
        "ESAllPrim?$select=PropertyInt16,PropertyString&$orderby=PropertyInt16", null, null));
    final String post = readResponse(getConnection(HttpMethod.POST,
        "ESAllPrim/$query", "$select=PropertyInt16,PropertyString&$orderby=PropertyInt16", TEXT_PLAIN));
    assertEquals(get, post);
  }

  @Test
  public void queryPostMergesWithUrlOptions() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESAllPrim/$query?$top=1",
        "$select=PropertyInt16,PropertyString", TEXT_PLAIN);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());

    final String body = readResponse(connection);
    assertTrue("selected property must be present", body.contains("\"PropertyString\":"));
    assertFalse("non-selected property must be absent", body.contains("\"PropertyBoolean\":"));
    assertEquals("$top=1 from the URL must still apply", 1, countOccurrences(body, "\"PropertyString\":"));
  }

  @Test
  public void queryPostDuplicateOptionRejected() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESAllPrim/$query?$top=1",
        "$top=2", TEXT_PLAIN);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
  }

  @Test
  public void queryGetRejected() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET, "ESAllPrim/$query", null, null);
    assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode(), connection.getResponseCode());
  }

  @Test
  public void queryPostWrongContentTypeRejected() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESAllPrim/$query",
        "$top=1", APPLICATION_JSON);
    assertEquals(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), connection.getResponseCode());
  }

  @Test
  public void queryPostEmptyBodyBehavesLikePlainGet() throws Exception {
    final String get = readResponse(getConnection(HttpMethod.GET, "ESAllPrim", null, null));
    final String post = readResponse(getConnection(HttpMethod.POST, "ESAllPrim/$query", "", TEXT_PLAIN));
    assertEquals(get, post);
  }

  /**
   * MANDATORY regression test protecting the exact seam Task 6's fix round closed: the next link
   * generated for a paginated <tt>/$query</tt> POST response is built from the server's rewritten
   * <tt>rawRequestUri</tt>. If that field were only stripped of its <tt>/$query</tt> suffix instead
   * of being fully rebuilt with the merged query, the next link would either keep a stale
   * <tt>/$query</tt> segment or silently drop the body-supplied query options - and would then fail
   * to be a followable page-2 link.
   */
  @Test
  public void queryPostPaginatedNextLinkIsFollowable() throws Exception {
    final HttpURLConnection firstPage = getConnection(HttpMethod.POST, "ESServerSidePaging/$query",
        "$format=json", TEXT_PLAIN);
    assertEquals(HttpStatusCode.OK.getStatusCode(), firstPage.getResponseCode());
    final String firstBody = readResponse(firstPage);

    final String nextLink = extractNextLink(firstBody);
    assertFalse("next link must not resurrect the /$query segment", nextLink.contains("/$query"));
    assertTrue("next link must carry the merged query options", nextLink.contains("$format=json"));
    assertEquals(SERVICE_URI + "ESServerSidePaging?$format=json&%24skiptoken=1%2A10", nextLink);

    final HttpURLConnection secondPage = (HttpURLConnection) new URL(nextLink).openConnection();
    secondPage.setRequestMethod(HttpMethod.GET.name());
    secondPage.setRequestProperty(HttpHeader.ACCEPT, APPLICATION_JSON);
    secondPage.connect();
    assertEquals(HttpStatusCode.OK.getStatusCode(), secondPage.getResponseCode());
    final String secondBody = readResponse(secondPage);
    assertTrue("second page must start at the entity right after the first page",
        secondBody.contains("\"PropertyInt16\":11,"));
  }

  /**
   * MANDATORY regression test pinning the <tt>/$query</tt> x <tt>omit-values=nulls</tt>
   * cross-feature seam: {@code ODataHandlerImpl.handleQueryPathIfPresent} forces the HTTP method
   * to GET before dispatching (see {@link #getConnection}'s doc and the class-level comment
   * above), and tecsvc's Prefer-header gate for the omit-values preference
   * ({@code TechnicalEntityProcessor#readEntity}) only ever runs for a GET-dispatched request
   * (see {@code PreferHeaderForGetAndDeleteITCase#omitValuesNulls_GetEntity}, which pins the same
   * behavior for a plain GET on this exact entity). This test proves the two features compose: a
   * <tt>/$query</tt> POST with an empty <tt>text/plain</tt> body carries no additional query
   * options, so once rewritten it is equivalent to a plain GET on the same entity, and
   * <tt>ESTwoPrim(-32766)</tt>'s null-seeded <tt>PropertyString</tt>
   * (DataCreator#createESTwoPrim) must still be omitted from the response.
   */
  @Test
  public void queryPostOmitValuesNullsComposesWithGetOnlyGate() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESTwoPrim(-32766)/$query",
        "", TEXT_PLAIN, "omit-values=nulls");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("omit-values=nulls", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));

    final String body = readResponse(connection);
    assertFalse("omitted property must not merely be null-valued", body.contains("\"PropertyString\":null"));
    assertFalse("omitted property's field name must be absent entirely", body.contains("PropertyString"));
    assertTrue("non-null key property must remain present", body.contains("\"PropertyInt16\":-32766"));
  }

  /**
   * Three-way composition pin: <tt>/$query</tt> POST (URL Conventions section 4.17) x
   * <tt>Prefer: omit-values=nulls</tt> (Protocol section 8.2.8.6) x the 4.01 <tt>matchesPattern</tt>
   * filter function, with a pattern built entirely from regex metacharacters that are also URL
   * metacharacters, so the body's percent-encoding is genuinely stressed:
   * <tt>^Test+String?[0-9]+$</tt> sent as <tt>%5ETest%2BString%3F%5B0-9%5D%2B%24</tt>.
   * <tt>matchesPattern</tt> over a null input is null, so <tt>eq null</tt> selects exactly
   * ESTwoPrim(-32766) - the one entity whose PropertyString is null and therefore the one entity for
   * which omit-values=nulls has anything to omit.
   */
  @Test
  public void queryPostOmitValuesAndMatchesPatternComposeOnNullMatch() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESTwoPrim/$query",
        "$filter=matchesPattern(PropertyString,'%5ETest%2BString%3F%5B0-9%5D%2B%24') eq null",
        TEXT_PLAIN, "omit-values=nulls");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("omit-values=nulls", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));

    final String body = readResponse(connection);
    assertEquals("exactly one entity has a null PropertyString", 1, countOccurrences(body, "\"PropertyInt16\":"));
    assertTrue("the null-valued entity must be the one selected", body.contains("\"PropertyInt16\":-32766"));
    assertFalse("the null property must be omitted, not written as null", body.contains("PropertyString"));
  }

  /**
   * The same percent-encoded metacharacters must reach the filter parser intact: with the literal
   * space instead of <tt>+</tt> the pattern <tt>^Test String[0-9]+$</tt> matches the three entities
   * whose PropertyString is "Test String1", "Test String2" and "Test String4". The filter is closed
   * with an explicit <tt>eq true</tt> - deliberately, not for symmetry with the previous test: a bare
   * <tt>matchesPattern(...)</tt> is evaluated by tecsvc's {@code FilterHandler} against every entity
   * in the set, including ESTwoPrim(-32766) whose PropertyString is null; <tt>matchesPattern</tt>
   * over a null input is null, not a boolean, and {@code FilterHandler} 400s on any non-boolean
   * per-entity result rather than treating it as non-matching (observed: an unguarded
   * <tt>matchesPattern</tt> filter over this set always fails with 400 - see
   * {@link #queryPostOmitValuesEchoedOnEmptyMatchesPatternResult} for the same reason). Wrapping in
   * <tt>eq true</tt> gives every entity, including the null one, a genuine boolean comparison result.
   */
  @Test
  public void queryPostMatchesPatternMetacharactersSurviveBodyEncoding() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESTwoPrim/$query",
        "$filter=matchesPattern(PropertyString,'%5ETest%20String%5B0-9%5D%2B%24') eq true",
        TEXT_PLAIN, "omit-values=nulls");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("omit-values=nulls", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));

    final String body = readResponse(connection);
    assertEquals("three seeded entities match the pattern", 3, countOccurrences(body, "\"PropertyInt16\":"));
    assertTrue("non-null strings are untouched by omit-values", body.contains("\"PropertyString\":\"Test String1\""));
    assertFalse("the null-valued entity must not match", body.contains("\"PropertyInt16\":-32766"));
  }

  /**
   * An empty result still honors the preference: the Preference-Applied header is driven by the
   * request, not by whether any property happened to be omitted. Also closed with <tt>eq true</tt>
   * for the same reason as {@link #queryPostMatchesPatternMetacharactersSurviveBodyEncoding}: a bare
   * <tt>matchesPattern(...)</tt> filter would 400 once tecsvc's {@code FilterHandler} reaches
   * ESTwoPrim(-32766)'s null PropertyString, regardless of the pattern.
   */
  @Test
  public void queryPostOmitValuesEchoedOnEmptyMatchesPatternResult() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESTwoPrim/$query",
        "$filter=matchesPattern(PropertyString,'%5EZZZ%5B0-9%5D%2B%24') eq true", TEXT_PLAIN, "omit-values=nulls");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("omit-values=nulls", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));
    assertTrue("the result must be empty", readResponse(connection).contains("\"value\":[]"));
  }

  private static int countOccurrences(final String haystack, final String needle) {
    int count = 0;
    int index = 0;
    while ((index = haystack.indexOf(needle, index)) != -1) {
      count++;
      index += needle.length();
    }
    return count;
  }

  private static String extractNextLink(final String jsonBody) {
    final String marker = "\"@odata.nextLink\":\"";
    final int start = jsonBody.indexOf(marker);
    assertTrue("response must carry an @odata.nextLink", start >= 0);
    final int valueStart = start + marker.length();
    final int valueEnd = jsonBody.indexOf('"', valueStart);
    return jsonBody.substring(valueStart, valueEnd);
  }

  /**
   * Opens a raw connection against the tecsvc service, optionally writing a request body.
   *
   * @param method HTTP method to use.
   * @param pathAndQuery resource path (and, optionally, a URL query string), relative to the
   * service root.
   * @param body request body to write; when {@code null}, no body is written and the connection is
   * left as a plain request (matching a GET with no payload).
   * @param contentType {@code Content-Type} header value to send along with a non-null body.
   * @return the connected {@link HttpURLConnection}.
   */
  private HttpURLConnection getConnection(final HttpMethod method, final String pathAndQuery,
      final String body, final String contentType) throws IOException {
    return getConnection(method, pathAndQuery, body, contentType, null);
  }

  /**
   * Opens a raw connection against the tecsvc service, optionally writing a request body and a
   * <tt>Prefer</tt> header.
   *
   * @param method HTTP method to use.
   * @param pathAndQuery resource path (and, optionally, a URL query string), relative to the
   * service root.
   * @param body request body to write; when {@code null}, no body is written and the connection is
   * left as a plain request (matching a GET with no payload).
   * @param contentType {@code Content-Type} header value to send along with a non-null body.
   * @param preferHeader value of the {@code Prefer} request header to send; when {@code null}, no
   * {@code Prefer} header is sent.
   * @return the connected {@link HttpURLConnection}.
   */
  private HttpURLConnection getConnection(final HttpMethod method, final String pathAndQuery,
      final String body, final String contentType, final String preferHeader) throws IOException {
    final URL url = new URL(SERVICE_URI + pathAndQuery);
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(method.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, APPLICATION_JSON);
    if (preferHeader != null) {
      connection.setRequestProperty(HttpHeader.PREFER, preferHeader);
    }
    if (body != null) {
      connection.setRequestProperty(HttpHeader.CONTENT_TYPE, contentType);
      connection.setDoOutput(true);
      final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
      writer.write(body);
      writer.close();
    }
    connection.connect();
    return connection;
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
