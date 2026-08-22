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
 * Copyright 2026 SiteNetSoft - Tier 7: end-to-end evidence for the OData 4.01 Minimal clauses
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
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
 * End-to-end evidence for the [OData-Protocol] section 13.2.1 clauses Tier 7 closes. The parser
 * and evaluator carry their own unit tests; these exercise the same clauses over real HTTP,
 * because the recurring failure shape in this codebase is a construct that parses cleanly and is
 * then dropped before it reaches a response.
 */
public class Conformance401ITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + "/";
  private static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_XML = "application/xml";

  /** Item 13: the service reports its capabilities through the Capabilities vocabulary. */
  @Test
  public void metadataReportsCapabilities() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET, "$metadata", APPLICATION_XML);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    final String body = readResponse(connection);
    assertTrue("the schema must report batch support",
        body.contains("<Annotation Term=\"Capabilities.BatchSupported\"><Bool>true</Bool></Annotation>"));
    assertTrue("the schema must report asynchronous request support",
        body.contains(
            "<Annotation Term=\"Capabilities.AsynchronousRequestsSupported\"><Bool>true</Bool></Annotation>"));
  }

  /** Item 13, JSON representation of the same annotations. */
  @Test
  public void jsonMetadataReportsCapabilities() throws Exception {
    final HttpURLConnection connection =
        getConnection(HttpMethod.GET, "$metadata?$format=json", APPLICATION_JSON);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    final String body = readResponse(connection);
    assertTrue("the JSON metadata document must carry the same annotations",
        body.contains("\"@Capabilities.BatchSupported\":true"));
    assertTrue("the JSON metadata document must carry the same annotations",
        body.contains("\"@Capabilities.AsynchronousRequestsSupported\":true"));
  }

  /** Items 6 and 7: system query option names take either spelling and any casing. */
  @Test
  public void systemQueryOptionsWithoutDollarAndInAnyCase() throws Exception {
    assertStatus(HttpStatusCode.OK, "ESAllPrim?$filter=PropertyInt16 gt 0");
    assertStatus(HttpStatusCode.OK, "ESAllPrim?filter=PropertyInt16 gt 0");
    assertStatus(HttpStatusCode.OK, "ESAllPrim?$FILTER=PropertyInt16 gt 0");
    assertStatus(HttpStatusCode.OK, "ESAllPrim?Top=1");
    assertStatus(HttpStatusCode.OK, "ESAllPrim?SELECT=PropertyString");

    // The same option twice, in any spelling mix, is still an error ([OData-URL] 5.1).
    assertStatus(HttpStatusCode.BAD_REQUEST, "ESAllPrim?$top=1&TOP=2");
  }

  /** Item 7: operator and canonical function names are case-insensitive. */
  @Test
  public void caseInsensitiveOperatorsAndFunctions() throws Exception {
    assertStatus(HttpStatusCode.OK, "ESAllPrim?$filter=PropertyInt16 GT 0");
    assertStatus(HttpStatusCode.OK, "ESAllPrim?$filter=CONTAINS(PropertyString,'First')");
    assertStatus(HttpStatusCode.OK, "ESAllPrim?$orderby=PropertyInt16 DESC");
  }

  /** Items 9a and 9b: a quoted string stands for a primitive value, prefixes are optional. */
  @Test
  public void quotedKeysAndUnprefixedLiterals() throws Exception {
    assertStatus(HttpStatusCode.OK, "ESAllPrim(32767)");
    assertStatus(HttpStatusCode.OK, "ESAllPrim('32767')");
    // A string that is not a literal of the key type is still rejected.
    assertStatus(HttpStatusCode.BAD_REQUEST, "ESAllPrim('abc')");
    // The enum type prefix is optional, and the prefixed spelling still works.
    assertStatus(HttpStatusCode.OK,
        "ESMixEnumDefCollComp?$filter=PropertyEnumString eq 'String1'");
    assertStatus(HttpStatusCode.OK,
        "ESMixEnumDefCollComp?$filter=PropertyEnumString eq olingo.odata.test1.ENString'String1'");
  }

  /** Item 9c: a parameter-less function import may omit its parentheses. */
  @Test
  public void parameterlessFunctionImportWithoutParentheses() throws Exception {
    assertStatus(HttpStatusCode.OK, "FINRTInt16()");
    assertStatus(HttpStatusCode.OK, "FINRTInt16");
  }

  /** Items 9j and 9k: divby and negative substring indexes are evaluated, not 501'd. */
  @Test
  public void divByAndNegativeSubstringAreEvaluated() throws Exception {
    assertStatus(HttpStatusCode.OK, "ESAllPrim?$filter=5 divby 2 eq 2.5");
    assertStatus(HttpStatusCode.OK, "ESAllPrim?$filter=substring('abcdef',-3) eq 'def'");
  }

  /** Item 4: preferences take either the 4.01 or the 4.0 spelling. */
  @Test
  public void bothPreferenceSpellings() throws Exception {
    assertPreferAccepted("maxpagesize=1");
    assertPreferAccepted("odata.maxpagesize=1");
  }

  private void assertStatus(final HttpStatusCode expected, final String pathAndQuery) throws IOException {
    final HttpURLConnection connection = getConnection(HttpMethod.GET,
        pathAndQuery.replace(" ", "%20").replace("'", "%27"), APPLICATION_JSON);
    assertEquals(pathAndQuery, expected.getStatusCode(), connection.getResponseCode());
  }

  private void assertPreferAccepted(final String prefer) throws IOException {
    // ESServerSidePaging is one of the entity sets tecsvc actually pages, so the preference is
    // applied and echoed; against a non-paging set nothing would be applied and nothing echoed.
    final URL url = new URL(SERVICE_URI + "ESServerSidePaging");
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, APPLICATION_JSON);
    connection.setRequestProperty(HttpHeader.PREFER, prefer);
    connection.connect();
    assertEquals(prefer, HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the applied preference must be echoed: " + prefer,
        connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED).contains("maxpagesize=1"));
  }

  private HttpURLConnection getConnection(final HttpMethod method, final String pathAndQuery,
      final String accept) throws IOException {
    final URL url = new URL(SERVICE_URI + pathAndQuery);
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(method.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, accept);
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
