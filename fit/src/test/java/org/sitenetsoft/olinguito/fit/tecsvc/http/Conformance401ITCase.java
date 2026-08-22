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
