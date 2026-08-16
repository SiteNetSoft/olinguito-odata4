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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 7 fix round 1: Content-Type coverage across the
 * synchronous, async-wrapped, and batch-serialized dispatch paths
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.AsyncRequestWrapper;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.client.core.communication.request.AbstractODataBasicRequest;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link AbstractODataRetrieveRequest}'s {@code $query} POST rewrite, isolated
 * from any real HTTP transport: only request construction (and, for the batch test, in-memory
 * serialization) is exercised - never a real {@code execute()} against a server - so these
 * assertions can run without one.
 */
class QueryPostRequestTest {

  private static final String SERVICE_ROOT = "http://localhost/svc/";
  private static final String QUERY = "$top=1&$select=PropertyInt16,PropertyString";

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
    assertEquals(client.getConfiguration().getDefaultPubFormat().toContentTypeString(), request.getContentType());
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
    assertEquals(client.getConfiguration().getDefaultPubFormat().toContentTypeString(), request.getContentType());
  }

  @Test
  void queryPostWhenFlagIsOnAndUriHasAQueryString() throws Exception {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setUseQueryPostRequest(true);

    final URI uri = URI.create(SERVICE_ROOT + "ESAllPrim?" + QUERY);
    final ODataEntitySetRequest<ClientEntitySet> request =
        client.getRetrieveRequestFactory().getEntitySetRequest(uri);

    assertEquals(HttpMethod.POST, request.getMethod());
    assertEquals(URI.create(SERVICE_ROOT + "ESAllPrim/$query"), request.getURI());
    assertEquals(ContentType.TEXT_PLAIN.toContentTypeString(), request.getContentType());

    final InputStream payload = payloadOf(request);
    assertArrayEquals(QUERY.getBytes(StandardCharsets.UTF_8), payload.readAllBytes());
  }

  /**
   * Fix round 1, finding CRITICAL: {@code AsyncRequestWrapperImpl} has its own {@code doExecute()}
   * that never calls the wrapped request's {@code execute()}/{@code doExecute()} - it reads
   * {@code getMethod()}/{@code getURI()}/{@code getHeader(...)} straight off the wrapped request,
   * and its constructor additionally does a Content-Type self-reassignment
   * ({@code odataRequest.setContentType(odataRequest.getContentType())}) before any of our code
   * would have run under the old (fixed) implementation. Merely constructing the async wrapper -
   * never calling {@code execute()} on it, so no network/server involved - must already observe
   * {@code text/plain} on the wrapped request.
   */
  @Test
  void asyncWrapperObservesTextPlainContentTypeWithoutExecuting() {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setUseQueryPostRequest(true);

    final URI uri = URI.create(SERVICE_ROOT + "ESAllPrim?" + QUERY);
    final ODataEntitySetRequest<ClientEntitySet> request =
        client.getRetrieveRequestFactory().getEntitySetRequest(uri);

    final AsyncRequestWrapper<ODataRetrieveResponse<ClientEntitySet>> wrapper =
        client.getAsyncRequestFactory().getAsyncRequestWrapper(request);
    assertTrue(wrapper != null, "constructing the wrapper must not require a server");

    // The wrapper mutates (self-reassigns) the SAME wrapped request instance's headers in its
    // constructor rather than copying them elsewhere at construction time, so inspecting the
    // original reference we passed in is exactly what AsyncRequestWrapperImpl.doExecute() will
    // read from at actual-execution time.
    assertEquals(ContentType.TEXT_PLAIN.toContentTypeString(), request.getContentType());
  }

  /**
   * Fix round 1, finding IMPORTANT: batch composition ({@code AbstractODataBasicRequest#batch()} -&gt;
   * {@code AbstractODataRequest#toByteArray()}) only fills in the default Content-Type "if not yet
   * set" and never calls this class's {@code doExecute()} either. Mirrors the
   * {@code BatchItemTrailingCrlfTest} (OLINGO-1240) pattern: mock {@code ODataBatchRequest} and
   * capture the bytes handed to {@code rawAppend(byte[])}.
   */
  @Test
  void batchSerializationIncludesTextPlainContentTypeHeader() throws IOException {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setUseQueryPostRequest(true);

    final URI uri = URI.create(SERVICE_ROOT + "ESAllPrim?" + QUERY);
    final ODataEntitySetRequest<ClientEntitySet> request =
        client.getRetrieveRequestFactory().getEntitySetRequest(uri);

    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    final ODataBatchRequest batchRequest = mock(ODataBatchRequest.class);
    when(batchRequest.rawAppend(any(byte[].class))).thenAnswer(invocation -> {
      captured.write((byte[]) invocation.getArgument(0));
      return batchRequest;
    });

    request.batch(batchRequest);

    final String serialized = captured.toString(StandardCharsets.UTF_8);
    assertTrue(serialized.contains("POST"), "serialized item must contain the request line");
    assertTrue(
        serialized.contains("Content-Type: " + ContentType.TEXT_PLAIN.toContentTypeString()),
        "serialized item must declare a text/plain Content-Type; was:\n" + serialized);
    assertTrue(serialized.contains(QUERY), "serialized item must carry the query string as its body");
  }

  private static InputStream payloadOf(final ODataEntitySetRequest<ClientEntitySet> request) {
    return ((AbstractODataBasicRequest<?>) request).getPayload();
  }
}
