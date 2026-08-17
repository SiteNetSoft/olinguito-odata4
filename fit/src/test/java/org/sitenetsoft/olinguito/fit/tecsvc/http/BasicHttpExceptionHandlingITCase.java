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
 * Copyright 2026 SiteNetSoft - OData 4.01: malformed optional-parameter default values are rejected with 400
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.fit.AbstractBaseTestITCase;
import org.sitenetsoft.olinguito.fit.tecsvc.TecSvcConst;
import org.junit.Test;

public class BasicHttpExceptionHandlingITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + '/';

  @Test
  public void ambiguousXHTTPMethod() throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(SERVICE_URI).openConnection();
    connection.setRequestMethod(HttpMethod.POST.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, ContentType.APPLICATION_JSON.toContentTypeString());
    connection.setRequestProperty(HttpHeader.X_HTTP_METHOD, "value");
    connection.setRequestProperty(HttpHeader.X_HTTP_METHOD_OVERRIDE, "differentValue");
    connection.connect();

    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
  }

  /**
   * A malformed <code>Core.OptionalParameter</code> <code>DefaultValue</code> is bad client-visible
   * model input for this call, so the URL path must answer 400 rather than 500.
   */
  @Test
  public void functionWithMalformedOptionalParameterDefaultIsBadRequest() throws Exception {
    final HttpURLConnection connection =
        getConnection(HttpMethod.GET, "FICRTStringOptionalBadDefault(ParameterString='x')", null);

    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());

    // Control: the very same call shape against the function whose default value is well-formed is
    // answered normally, so the 400 above is about the default value and not about the URL itself.
    final HttpURLConnection valid =
        getConnection(HttpMethod.GET, "FICRTStringOptionalParam(ParameterString='x')", null);

    assertEquals(HttpStatusCode.OK.getStatusCode(), valid.getResponseCode());
  }

  /** The same malformed default reached through an action body is a 400 as well. */
  @Test
  public void actionWithMalformedOptionalParameterDefaultIsBadRequest() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "AIRTStringOptionalBadDefault", "{}");

    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
  }

  /**
   * Opens a raw connection against the tecsvc service, optionally writing a JSON request body.
   *
   * @param method HTTP method to use.
   * @param pathAndQuery resource path relative to the service root.
   * @param body request body to write; when {@code null}, no body is written.
   * @return the connected {@link HttpURLConnection}.
   */
  private HttpURLConnection getConnection(final HttpMethod method, final String pathAndQuery, final String body)
      throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(SERVICE_URI + pathAndQuery).openConnection();
    connection.setRequestMethod(method.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, ContentType.APPLICATION_JSON.toContentTypeString());
    if (body != null) {
      connection.setRequestProperty(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.toContentTypeString());
      connection.setDoOutput(true);
      final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
      writer.write(body);
      writer.close();
    }
    connection.connect();
    return connection;
  }

  @Override
  protected ODataClient getClient() {
    return null;
  }

}
