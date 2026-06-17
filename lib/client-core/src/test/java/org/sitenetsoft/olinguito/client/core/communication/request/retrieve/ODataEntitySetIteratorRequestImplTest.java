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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1504: stream entity-set iterator without buffering
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.junit.jupiter.api.Test;

/** OLINGO-1504: the iterator response must hand back the live stream, not a buffered copy. */
class ODataEntitySetIteratorRequestImplTest {

  @Test
  void getRawResponseReturnsLiveStreamWithoutBuffering() {
    final ODataClient client = ODataClientFactory.getClient();
    final ODataHttpClient httpClient = mock(ODataHttpClient.class);
    final ODataHttpResponse res = mock(ODataHttpResponse.class);
    final InputStream live = new ByteArrayInputStream("{\"value\":[]}".getBytes(StandardCharsets.UTF_8));
    lenient().when(res.getBody()).thenReturn(live);
    lenient().when(res.getHeaders()).thenReturn(Collections.emptyMap());
    lenient().when(res.getStatusCode()).thenReturn(200);
    lenient().when(res.getReasonPhrase()).thenReturn("OK");

    final ODataEntitySetIteratorRequestImpl<ClientEntitySet, ClientEntity> request =
        new ODataEntitySetIteratorRequestImpl<>(client, URI.create("http://host/svc/ES"));
    final ODataEntitySetIteratorRequestImpl<ClientEntitySet, ClientEntity>.ODataEntitySetIteratorResponseImpl
        response = request.new ODataEntitySetIteratorResponseImpl(client, httpClient, res);

    // The base AbstractODataResponse would buffer into a new ByteArrayInputStream; the override
    // must return the exact live payload stream instead.
    assertSame(live, response.getRawResponse());
  }
}
