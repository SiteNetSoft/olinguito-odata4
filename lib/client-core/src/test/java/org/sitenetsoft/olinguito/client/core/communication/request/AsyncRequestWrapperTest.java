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
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - OLINGO-1476: Tests for relative Location URIs
 * Copyright 2026 SiteNetSoft - OLINGO-1475: Tests for chunked-encoding configuration on async payloads
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.sitenetsoft.olinguito.client.api.Configuration;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataBatchableRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.AsyncResponseWrapper;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientInvokeResult;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.api.http.HttpUriRequestFactory;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.client.core.communication.request.AsyncRequestWrapperImpl.AsyncResponseWrapperImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.ODataBatchRequestImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.invoke.ODataInvokeRequestImpl;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpClient;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpRequest;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.junit.jupiter.api.Test;

class AsyncRequestWrapperTest {

  @Test
  void testBatchReq() throws URISyntaxException {

    ODataClient client = ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    AsyncBatchRequestWrapperImpl req = new AsyncBatchRequestWrapperImpl(client,
        client.getBatchRequestFactory().getBatchRequest("root"));
    assertNotNull(req.addChangeset());
    ODataBatchableRequest request = new ODataInvokeRequestImpl<>(
            client, ClientInvokeResult.class, HttpMethod.GET, uri);
    req.addRetrieve(request);
    req.addOutsideUpdate(request);
    assertNotNull(client.getAsyncRequestFactory().getAsyncRequestWrapper(request));
    ODataBatchRequestImpl batchRequest = new ODataBatchRequestImpl(client, uri);
    assertNotNull(client.getAsyncRequestFactory().getAsyncBatchRequestWrapper(batchRequest));
    assertNotNull(req.wait(10));
  }

  @Test
  void testReq() throws URISyntaxException {

    ODataClient client = ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    AsyncRequestWrapperImpl<ODataResponse> req = new AsyncRequestWrapperImpl<>(client,
        client.getBatchRequestFactory().getBatchRequest("root"));
    assertNotNull(req);
    new ODataInvokeRequestImpl<>(
            client, ClientInvokeResult.class, HttpMethod.GET, uri);
    req.checkRequest(client, null);
    assertNotNull(req.callback(uri));
    req.extendHeader("header", "value");
    AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl res = req.new AsyncResponseWrapperImpl();
    res.forceNextMonitorCheck(uri);
  }

  private AsyncRequestWrapperImpl<ODataResponse> createAsyncRequestWrapperImplWithRetryAfter(int retryAfter)
      throws IOException, URISyntaxException {

    HttpClient httpClient = mock(HttpClient.class);
    ODataClient oDataClient = mock(ODataClient.class);
    Configuration configuration = mock(Configuration.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    HttpUriRequestFactory httpUriRequestFactory = mock(HttpUriRequestFactory.class);
    HttpUriRequestBase httpUriRequest = mock(HttpUriRequestBase.class);
    when(httpUriRequest.getMethod()).thenReturn("GET");

    when(oDataClient.getConfiguration()).thenReturn(configuration);
    when(configuration.getHttpClientFactory()).thenReturn(httpClientFactory);
    when(configuration.getHttpUriRequestFactory()).thenReturn(httpUriRequestFactory);
    when(httpClientFactory.create(any(), any())).thenReturn(new ApacheHttpClient(httpClient));
    when(httpUriRequestFactory.create(any(), any())).thenReturn(new ApacheHttpRequest(httpUriRequest));

    ClassicHttpResponse firstResponse = new BasicClassicHttpResponse(202);
    firstResponse.addHeader(HttpHeader.LOCATION, "http://localhost/monitor");
    firstResponse.addHeader(HttpHeader.RETRY_AFTER, String.valueOf(retryAfter));
    when(httpClient.executeOpen(any(), any(ClassicHttpRequest.class), any())).thenReturn(firstResponse);

    AbstractODataRequest oDataRequest = mock(AbstractODataRequest.class);
    ODataResponse oDataResponse = mock(ODataResponse.class);
    when(oDataRequest.getResponseTemplate()).thenReturn(oDataResponse);
    when(oDataRequest.getURI()).thenReturn(new URI("http://localhost/path"));
    when(oDataResponse.initFromHttpResponse(any(ODataHttpResponse.class))).thenReturn(null);

    return new AsyncRequestWrapperImpl<>(oDataClient, oDataRequest);
  }

  @Test
  void testTooBigRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(Integer.MAX_VALUE);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(AsyncResponseWrapperImpl.MAX_RETRY_AFTER, wrappedResponseImpl.retryAfter);
  }

  @Test
  void testZeroRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(0);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(0, wrappedResponseImpl.retryAfter);
  }

  @Test
  void testNegativeRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(-1);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(AsyncResponseWrapperImpl.DEFAULT_RETRY_AFTER, wrappedResponseImpl.retryAfter);
  }

  @Test
  void testRetryAfter() throws IOException, URISyntaxException {

    int retryAfter = 7;
    assertNotEquals(retryAfter, AsyncResponseWrapperImpl.DEFAULT_RETRY_AFTER);
    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(retryAfter);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(retryAfter, wrappedResponseImpl.retryAfter);
  }

  private AbstractODataBasicRequest<?> mockBasicRequest(final ODataHttpRequest httpRequest, final boolean useChunked)
      throws URISyntaxException {
    final ODataClient client = mock(ODataClient.class);
    final Configuration configuration = mock(Configuration.class);
    final HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    final HttpUriRequestFactory httpUriRequestFactory = mock(HttpUriRequestFactory.class);

    when(client.getConfiguration()).thenReturn(configuration);
    when(configuration.getHttpClientFactory()).thenReturn(httpClientFactory);
    when(configuration.getHttpUriRequestFactory()).thenReturn(httpUriRequestFactory);
    when(configuration.isGzipCompression()).thenReturn(false);
    when(configuration.isUseChuncked()).thenReturn(useChunked);
    when(httpClientFactory.create(any(), any())).thenReturn(mock(ODataHttpClient.class));
    when(httpUriRequestFactory.create(any(), any())).thenReturn(httpRequest);
    when(httpRequest.supportsEntity()).thenReturn(true);

    @SuppressWarnings("unchecked")
    final AbstractODataBasicRequest<ODataResponse> odataRequest = mock(AbstractODataBasicRequest.class);
    when(odataRequest.getMethod()).thenReturn(HttpMethod.POST);
    when(odataRequest.getURI()).thenReturn(new URI("http://server/path"));
    when(odataRequest.getHeaderNames()).thenReturn(Collections.emptyList());
    when(odataRequest.getPayload()).thenReturn(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

    new AsyncRequestWrapperImpl<>(client, odataRequest);
    return odataRequest;
  }

  @Test
  void asyncRequestBuffersPayloadWhenChunkingDisabled() throws URISyntaxException {
    // OLINGO-1475: when chunked encoding is disabled, the async request must buffer the payload
    // (byte[] entity, not chunked) instead of always streaming it chunked.
    final ODataHttpRequest httpRequest = mock(ODataHttpRequest.class);
    mockBasicRequest(httpRequest, false);

    verify(httpRequest).setEntity(any(byte[].class), eq(false));
    verify(httpRequest, never()).setEntity(any(InputStream.class), anyLong(), anyBoolean());
  }

  @Test
  void asyncRequestStreamsChunkedWhenChunkingEnabled() throws URISyntaxException {
    // OLINGO-1475: when chunked encoding is enabled, the async request streams the payload chunked.
    final ODataHttpRequest httpRequest = mock(ODataHttpRequest.class);
    mockBasicRequest(httpRequest, true);

    verify(httpRequest).setEntity(any(InputStream.class), eq(-1L), eq(true));
    verify(httpRequest, never()).setEntity(any(byte[].class), anyBoolean());
  }

  @Test
  void testWrapper() {

    Wrapper<String> wrap = new Wrapper<>();
    wrap.setWrapped("test");
    assertEquals("test", wrap.getWrapped());
  }

  @Test
  void testException() {

    AsyncRequestException ex = new AsyncRequestException("Exception");
    assertEquals("Exception", ex.getMessage());
  }

  private AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl
      createAsyncRequestWrapperImplWithLocation(String target, String location)
      throws IOException, URISyntaxException {

    HttpClient httpClient = mock(HttpClient.class);
    ODataClient oDataClient = mock(ODataClient.class);
    Configuration configuration = mock(Configuration.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    HttpUriRequestFactory httpUriRequestFactory = mock(HttpUriRequestFactory.class);
    HttpUriRequestBase httpUriRequest = mock(HttpUriRequestBase.class);
    when(httpUriRequest.getMethod()).thenReturn("GET");

    when(oDataClient.getConfiguration()).thenReturn(configuration);
    when(configuration.getHttpClientFactory()).thenReturn(httpClientFactory);
    when(configuration.getHttpUriRequestFactory()).thenReturn(httpUriRequestFactory);
    when(httpClientFactory.create(any(), any())).thenReturn(new ApacheHttpClient(httpClient));
    when(httpUriRequestFactory.create(any(), any())).thenReturn(new ApacheHttpRequest(httpUriRequest));

    ClassicHttpResponse firstResponse = new BasicClassicHttpResponse(202);
    firstResponse.addHeader(HttpHeader.LOCATION, location);
    when(httpClient.executeOpen(any(), any(ClassicHttpRequest.class), any())).thenReturn(firstResponse);

    ODataResponse oDataResponse = mock(ODataResponse.class);
    when(oDataResponse.initFromHttpResponse(any(ODataHttpResponse.class))).thenReturn(null);

    AbstractODataRequest oDataRequest = mock(AbstractODataRequest.class);
    when(oDataRequest.getURI()).thenReturn(new URI(target));
    when(oDataRequest.getResponseTemplate()).thenReturn(oDataResponse);

    AsyncRequestWrapperImpl<ODataResponse> req = new AsyncRequestWrapperImpl<>(oDataClient, oDataRequest);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    return (AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl) wrappedResponse;
  }

  @Test
  void testLocationWithInvalidScheme() throws IOException, URISyntaxException {
      assertThrows(AsyncRequestException.class, () -> {
          String target = "https://server/path";
          String location = "http://server/path";
          createAsyncRequestWrapperImplWithLocation(target, location);
      });
  }

  @Test
  void testLocationWithInvalidHost() throws IOException, URISyntaxException {
      assertThrows(AsyncRequestException.class, () -> {
          String target = "http://server/path";
          String location = "http://something.else/path";
          createAsyncRequestWrapperImplWithLocation(target, location);
      });
  }

  @Test
  void testLocationWithInvalidPort() throws IOException, URISyntaxException {
      assertThrows(AsyncRequestException.class, () -> {
          String target = "http://server/path";
          String location = "http://server:8080/path";
          createAsyncRequestWrapperImplWithLocation(target, location);
      });
  }

  @Test
  void testLocationWithDifferentPaths() throws IOException, URISyntaxException {
    String target = "http://server/path";
    String location = "http://server/monitor";
    AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl wrapper =
        createAsyncRequestWrapperImplWithLocation(target, location);
    assertEquals(new URI(location), wrapper.location);
  }

  @Test
  void testLocationWithRelativeUrl() throws IOException, URISyntaxException {
    String target = "http://server/service/path";
    String location = "/monitor/123";
    AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl wrapper =
        createAsyncRequestWrapperImplWithLocation(target, location);
    assertEquals(new URI("http://server/monitor/123"), wrapper.location);
  }

  @Test
  void testLocationWithRelativePathTraversal() throws IOException, URISyntaxException {
    String target = "http://server:8080/a/b/c/path";
    String location = "../../_async('EQ1XW')";
    AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl wrapper =
        createAsyncRequestWrapperImplWithLocation(target, location);
    assertEquals(new URI("http://server:8080/a/_async('EQ1XW')"), wrapper.location);
  }

  @Test
  void testLocationWithRelativeSibling() throws IOException, URISyntaxException {
    String target = "http://server/service/entities";
    String location = "monitor/status";
    AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl wrapper =
        createAsyncRequestWrapperImplWithLocation(target, location);
    assertEquals(new URI("http://server/service/monitor/status"), wrapper.location);
  }

}
