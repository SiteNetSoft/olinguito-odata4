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
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

public class AsyncRequestWrapperTest {

  @Test
  public void testBatchReq() throws URISyntaxException {

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
  public void testReq() throws URISyntaxException {

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
  public void testTooBigRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(Integer.MAX_VALUE);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(AsyncResponseWrapperImpl.MAX_RETRY_AFTER, wrappedResponseImpl.retryAfter);
  }

  @Test
  public void testZeroRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(0);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(0, wrappedResponseImpl.retryAfter);
  }

  @Test
  public void testNegativeRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(-1);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(AsyncResponseWrapperImpl.DEFAULT_RETRY_AFTER, wrappedResponseImpl.retryAfter);
  }

  @Test
  public void testRetryAfter() throws IOException, URISyntaxException {

    int retryAfter = 7;
    assertNotEquals(retryAfter, AsyncResponseWrapperImpl.DEFAULT_RETRY_AFTER);
    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(retryAfter);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    AsyncResponseWrapperImpl wrappedResponseImpl = (AsyncResponseWrapperImpl) wrappedResponse;
    assertEquals(retryAfter, wrappedResponseImpl.retryAfter);
  }

  @Test
  public void testWrapper() {

    Wrapper<String> wrap = new Wrapper<>();
    wrap.setWrapped("test");
    assertEquals("test", wrap.getWrapped());
  }

  @Test
  public void testException() {

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
  public void testLocationWithInvalidScheme() throws IOException, URISyntaxException {
      assertThrows(AsyncRequestException.class, () -> {
          String target = "https://server/path";
          String location = "http://server/path";
          createAsyncRequestWrapperImplWithLocation(target, location);
      });
  }

  @Test
  public void testLocationWithInvalidHost() throws IOException, URISyntaxException {
      assertThrows(AsyncRequestException.class, () -> {
          String target = "http://server/path";
          String location = "http://something.else/path";
          createAsyncRequestWrapperImplWithLocation(target, location);
      });
  }

  @Test
  public void testLocationWithInvalidPort() throws IOException, URISyntaxException {
      assertThrows(AsyncRequestException.class, () -> {
          String target = "http://server/path";
          String location = "http://server:8080/path";
          createAsyncRequestWrapperImplWithLocation(target, location);
      });
  }

  @Test
  public void testLocationWithDifferentPaths() throws IOException, URISyntaxException {
    String target = "http://server/path";
    String location = "http://server/monitor";
    AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl wrapper =
        createAsyncRequestWrapperImplWithLocation(target, location);
    assertEquals(new URI(location), wrapper.location);
  }

}
