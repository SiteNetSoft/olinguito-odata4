/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 7: unit coverage for $query POST retrieve
 * requests (OData 4.01 URL Conventions section 4.17)
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.client.core.communication.request.AbstractODataBasicRequest;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link AbstractODataRetrieveRequest}'s {@code $query} POST rewrite, isolated
 * from any real HTTP transport: only request construction is exercised (never {@code execute()}),
 * so these assertions can run without a server.
 */
class QueryPostRequestTest {

  private static final String SERVICE_ROOT = "http://localhost/svc/";

  @Test
  void plainGetWhenFlagIsOff() {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setUseQueryPostRequest(false);

    final URI uri = URI.create(SERVICE_ROOT + "ESAllPrim?$top=1");
    final ODataEntitySetRequest<ClientEntitySet> request =
        client.getRetrieveRequestFactory().getEntitySetRequest(uri);

    assertEquals(HttpMethod.GET, request.getMethod());
    assertEquals(uri, request.getURI());
    assertNull(payloadOf(request));
  }

  @Test
  void plainGetWhenFlagIsOnButUriHasNoQueryString() {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setUseQueryPostRequest(true);

    final URI uri = URI.create(SERVICE_ROOT + "ESAllPrim");
    final ODataEntitySetRequest<ClientEntitySet> request =
        client.getRetrieveRequestFactory().getEntitySetRequest(uri);

    assertEquals(HttpMethod.GET, request.getMethod());
    assertEquals(uri, request.getURI());
    assertNull(payloadOf(request));
  }

  @Test
  void queryPostWhenFlagIsOnAndUriHasAQueryString() throws Exception {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setUseQueryPostRequest(true);

    final String query = "$top=1&$select=PropertyInt16,PropertyString";
    final URI uri = URI.create(SERVICE_ROOT + "ESAllPrim?" + query);
    final ODataEntitySetRequest<ClientEntitySet> request =
        client.getRetrieveRequestFactory().getEntitySetRequest(uri);

    assertEquals(HttpMethod.POST, request.getMethod());
    assertEquals(URI.create(SERVICE_ROOT + "ESAllPrim/$query"), request.getURI());

    final InputStream payload = payloadOf(request);
    assertArrayEquals(query.getBytes(StandardCharsets.UTF_8), payload.readAllBytes());
  }

  private static InputStream payloadOf(final ODataEntitySetRequest<ClientEntitySet> request) {
    return ((AbstractODataBasicRequest<?>) request).getPayload();
  }
}
