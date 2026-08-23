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
 * Copyright 2026 SiteNetSoft - Tier 8: end-to-end evidence for the OData 4.01 Intermediate clauses
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
 * End-to-end evidence for the [OData-Protocol] section 13.2.2 clauses Tier 8 closes, and for the
 * section 13.1.2 item 4 gap that level inherits. The parser and evaluator carry their own unit
 * tests; these exercise the same clauses over real HTTP, because this codebase's recurring failure
 * shape is a construct that parses cleanly and is then dropped before it reaches a response.
 */
public class Conformance401IntermediateITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + "/";
  private static final String APPLICATION_JSON = "application/json";

  /**
   * 13.1.2 item 4. A cast combined with any other clause used to answer 500: the collection-narrowing
   * special case recognised only a top-level Binary whose left operand carried the type filter, so the
   * cast member was evaluated against entities that are not of the cast type.
   */
  @Test
  public void castInFilterCombinedWithAnotherClause() throws Exception {
    // ESBase(222) is "TEST B" / "TEST C 0815", so this matches nothing and must simply be empty.
    assertEquals(0, entityCount("ESTwoPrim?$filter=olingo.odata.test1.ETBase/AdditionalPropertyString_5"
        + " eq 'TEST A 0815' and PropertyInt16 eq 222"));

    // ESBase(111) satisfies both clauses.
    assertEquals(1, entityCount("ESTwoPrim?$filter=olingo.odata.test1.ETBase/AdditionalPropertyString_5"
        + " eq 'TEST A 0815' and PropertyInt16 eq 111"));
  }

  /** The single-clause shape the special case already handled keeps its answers. */
  @Test
  public void castInFilterAlone() throws Exception {
    assertEquals(1, entityCount("ESTwoPrim?$filter=olingo.odata.test1.ETBase/AdditionalPropertyString_5"
        + " eq 'TEST A 0815'"));
    assertEquals(0, entityCount("ESTwoPrim?$filter=olingo.odata.test1.ETBase/AdditionalPropertyString_5"
        + " eq 'nope'"));
  }

  private int entityCount(final String pathAndQuery) throws IOException {
    final String content = body(pathAndQuery);
    final int start = content.indexOf("\"value\":[");
    assertTrue("expected a collection response: " + content, start >= 0);
    // Every tecsvc entity in these sets carries PropertyInt16, so counting it counts entities.
    return content.substring(start).split("\"PropertyInt16\"", -1).length - 1;
  }

  private String body(final String pathAndQuery) throws IOException {
    final HttpURLConnection connection = connect(pathAndQuery);
    assertEquals(pathAndQuery, HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    return read(connection);
  }

  private HttpURLConnection connect(final String pathAndQuery) throws IOException {
    final URL url = new URL(SERVICE_URI + pathAndQuery.replace(" ", "%20").replace("'", "%27"));
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, APPLICATION_JSON);
    connection.connect();
    return connection;
  }

  private static String read(final HttpURLConnection connection) throws IOException {
    final InputStream stream = connection.getResponseCode() >= 400
        ? connection.getErrorStream() : connection.getInputStream();
    return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
  }

  @Override
  protected ODataClient getClient() {
    return null;
  }
}
