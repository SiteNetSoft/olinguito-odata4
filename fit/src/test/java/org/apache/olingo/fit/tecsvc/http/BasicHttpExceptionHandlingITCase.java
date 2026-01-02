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
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;

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

  @Override
  protected ODataClient getClient() {
    return null;
  }

}
