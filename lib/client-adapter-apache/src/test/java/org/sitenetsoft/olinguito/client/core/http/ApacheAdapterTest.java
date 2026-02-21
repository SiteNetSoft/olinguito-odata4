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
 * Copyright 2026 SiteNetSoft - Unit tests for Apache HttpComponents adapter
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 * Copyright 2026 SiteNetSoft - Replaced wildcard imports with explicit imports
 */
package org.sitenetsoft.olinguito.client.core.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

class ApacheAdapterTest {

  private static final URI TEST_URI = URI.create("http://localhost:9080/odata");

  // --- ApacheHttpClient tests ---

  @Test
  void testExecuteGetRequest() throws Exception {
    final HttpClient httpClient = mock(HttpClient.class);
    final ClassicHttpResponse httpResponse = mockResponse(200, "OK", "application/json", "{\"value\":42}");
    when(httpClient.executeOpen(any(), any(HttpUriRequestBase.class), any())).thenReturn(httpResponse);

    try (ODataHttpClient client = new ApacheHttpClient(httpClient)) {
      final ODataHttpRequest request = new ApacheHttpRequest(new HttpGet(TEST_URI));
      final ODataHttpResponse response = client.execute(request);

      assertEquals(200, response.getStatusCode());
      assertEquals("OK", response.getReasonPhrase());
      assertNotNull(response.getBody());
    }
  }

  @Test
  void testExecutePostRequest() throws Exception {
    final HttpClient httpClient = mock(HttpClient.class);
    final ClassicHttpResponse httpResponse = mockResponse(201, "Created", null, null);
    when(httpClient.executeOpen(any(), any(HttpUriRequestBase.class), any())).thenReturn(httpResponse);

    try (ODataHttpClient client = new ApacheHttpClient(httpClient)) {
      final ODataHttpRequest request = new ApacheHttpRequest(new HttpPost(TEST_URI));
      request.setEntity("{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8), false);

      final ODataHttpResponse response = client.execute(request);
      assertEquals(201, response.getStatusCode());
    }
  }

  @Test
  void testExecuteIOExceptionThrowsHttpClientException() throws Exception {
    final HttpClient httpClient = mock(HttpClient.class);
    when(httpClient.executeOpen(any(), any(HttpUriRequestBase.class), any()))
        .thenThrow(new IOException("Connection refused"));

    try (ODataHttpClient client = new ApacheHttpClient(httpClient)) {
      final ODataHttpRequest request = new ApacheHttpRequest(new HttpGet(TEST_URI));
      assertThrows(HttpClientException.class, () -> client.execute(request));
    }
  }

  @Test
  void testExecuteWithWrongRequestTypeThrows() throws Exception {
    final HttpClient httpClient = mock(HttpClient.class);

    try (ODataHttpClient client = new ApacheHttpClient(httpClient)) {
      final ODataHttpRequest badRequest = new ODataHttpRequest() {
        @Override
        public URI getURI() {
          return TEST_URI;
        }
      };
      assertThrows(IllegalArgumentException.class, () -> client.execute(badRequest));
    }
  }

  @Test
  void testCloseWithCloseableHttpClient() throws Exception {
    final java.io.Closeable closeable = mock(java.io.Closeable.class, withSettings()
        .extraInterfaces(HttpClient.class));
    final HttpClient httpClient = (HttpClient) closeable;

    final ApacheHttpClient client = new ApacheHttpClient(httpClient);
    client.close();

    verify(closeable).close();
  }

  @Test
  @SuppressWarnings("removal")
  void testUnwrapReturnsDelegate() {
    final HttpClient httpClient = mock(HttpClient.class);
    final ApacheHttpClient client = new ApacheHttpClient(httpClient);
    assertSame(httpClient, client.unwrap());
  }

  @Test
  @SuppressWarnings("removal")
  void testStaticUnwrapReturnsDelegate() {
    final HttpClient httpClient = mock(HttpClient.class);
    final ODataHttpClient client = new ApacheHttpClient(httpClient);
    assertSame(httpClient, ApacheHttpClient.unwrap(client));
  }

  @Test
  @SuppressWarnings("removal")
  void testStaticUnwrapWithWrongTypeThrows() {
    final ODataHttpClient wrongClient = mock(ODataHttpClient.class);
    assertThrows(IllegalArgumentException.class, () -> ApacheHttpClient.unwrap(wrongClient));
  }

  // --- ApacheHttpRequest tests ---

  @Test
  void testRequestGetURI() {
    final HttpGet get = new HttpGet(TEST_URI);
    final ODataHttpRequest request = new ApacheHttpRequest(get);
    assertEquals(TEST_URI, request.getURI());
  }

  @Test
  void testRequestSetURI() {
    final HttpGet get = new HttpGet(TEST_URI);
    final ODataHttpRequest request = new ApacheHttpRequest(get);
    final URI newUri = URI.create("http://example.com/new");
    request.setURI(newUri);
    assertEquals(newUri, request.getURI());
  }

  @Test
  void testRequestGetMethod() {
    assertEquals(HttpMethod.GET, new ApacheHttpRequest(new HttpGet(TEST_URI)).getMethod());
    assertEquals(HttpMethod.POST, new ApacheHttpRequest(new HttpPost(TEST_URI)).getMethod());
    assertEquals(HttpMethod.PUT, new ApacheHttpRequest(new HttpPut(TEST_URI)).getMethod());
  }

  @Test
  void testRequestAddAndGetHeaders() {
    final HttpGet get = new HttpGet(TEST_URI);
    final ODataHttpRequest request = new ApacheHttpRequest(get);
    request.addHeader("Accept", "application/json");
    request.addHeader("X-Custom", "value");

    final Map<String, String> headers = request.getAllHeaders();
    assertEquals("application/json", headers.get("Accept"));
    assertEquals("value", headers.get("X-Custom"));
  }

  @Test
  void testGetSupportsEntity() {
    assertFalse(new ApacheHttpRequest(new HttpGet(TEST_URI)).supportsEntity());
  }

  @Test
  void testPostSupportsEntity() {
    assertTrue(new ApacheHttpRequest(new HttpPost(TEST_URI)).supportsEntity());
  }

  @Test
  void testPutSupportsEntity() {
    assertTrue(new ApacheHttpRequest(new HttpPut(TEST_URI)).supportsEntity());
  }

  @Test
  void testSetEntityBytes() {
    final HttpPost post = new HttpPost(TEST_URI);
    final ODataHttpRequest request = new ApacheHttpRequest(post);
    request.setEntity("body".getBytes(StandardCharsets.UTF_8), false);
    assertNotNull(post.getEntity());
  }

  @Test
  void testSetEntityStream() {
    final HttpPost post = new HttpPost(TEST_URI);
    final ODataHttpRequest request = new ApacheHttpRequest(post);
    request.setEntity(new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8)), 6, true);
    assertNotNull(post.getEntity());
  }

  @Test
  void testSetEntityOnGetThrows() {
    final ODataHttpRequest request = new ApacheHttpRequest(new HttpGet(TEST_URI));
    assertThrows(IllegalStateException.class, () ->
        request.setEntity("body".getBytes(StandardCharsets.UTF_8), false));
  }

  @Test
  void testSetEntityStreamOnGetThrows() {
    final ODataHttpRequest request = new ApacheHttpRequest(new HttpGet(TEST_URI));
    assertThrows(IllegalStateException.class, () ->
        request.setEntity(new ByteArrayInputStream(new byte[0]), 0, false));
  }

  @Test
  void testAbort() {
    final HttpGet get = new HttpGet(TEST_URI);
    final ODataHttpRequest request = new ApacheHttpRequest(get);
    request.abort();
    assertTrue(get.isCancelled());
  }

  @Test
  @SuppressWarnings("removal")
  void testRequestUnwrap() {
    final HttpGet get = new HttpGet(TEST_URI);
    final ApacheHttpRequest request = new ApacheHttpRequest(get);
    assertSame(get, request.unwrap());
  }

  // --- ApacheHttpResponse tests ---

  @Test
  void testResponseStatusCode() {
    final ClassicHttpResponse httpResponse = mockResponse(200, "OK", null, null);
    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    assertEquals(200, response.getStatusCode());
  }

  @Test
  void testResponseReasonPhrase() {
    final ClassicHttpResponse httpResponse = mockResponse(404, "Not Found", null, null);
    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    assertEquals("Not Found", response.getReasonPhrase());
  }

  @Test
  void testResponseHeaders() {
    final ClassicHttpResponse httpResponse = mock(ClassicHttpResponse.class);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getReasonPhrase()).thenReturn("OK");
    when(httpResponse.getHeaders()).thenReturn(new Header[]{
        new BasicHeader("Content-Type", "application/json"),
        new BasicHeader("ETag", "W/\"abc\""),
        new BasicHeader("X-Multi", "val1"),
        new BasicHeader("X-Multi", "val2")
    });

    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    final Map<String, Collection<String>> headers = response.getHeaders();

    assertNotNull(headers);
    assertTrue(headers.containsKey("Content-Type"));
    assertTrue(headers.containsKey("ETag"));
  }

  @Test
  void testResponseGetHeader() {
    final ClassicHttpResponse httpResponse = mock(ClassicHttpResponse.class);
    when(httpResponse.getHeaders("ETag")).thenReturn(new Header[]{
        new BasicHeader("ETag", "W/\"abc\"")
    });

    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    final Collection<String> values = response.getHeader("ETag");
    assertNotNull(values);
    assertEquals(1, values.size());
    assertTrue(values.contains("W/\"abc\""));
  }

  @Test
  void testResponseGetHeaderMissing() {
    final ClassicHttpResponse httpResponse = mock(ClassicHttpResponse.class);
    when(httpResponse.getHeaders("X-Missing")).thenReturn(new Header[0]);

    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    assertNull(response.getHeader("X-Missing"));
  }

  @Test
  void testResponseBody() throws Exception {
    final byte[] content = "{\"value\":42}".getBytes(StandardCharsets.UTF_8);
    final ClassicHttpResponse httpResponse = mockResponse(200, "OK", "application/json",
        new String(content, StandardCharsets.UTF_8));

    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    try (InputStream body = response.getBody()) {
      assertNotNull(body);
      assertEquals("{\"value\":42}", new String(body.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Test
  void testResponseBodyNull() {
    final ClassicHttpResponse httpResponse = mock(ClassicHttpResponse.class);
    when(httpResponse.getCode()).thenReturn(204);
    when(httpResponse.getReasonPhrase()).thenReturn("No Content");
    when(httpResponse.getEntity()).thenReturn(null);

    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    assertNull(response.getBody());
  }

  @Test
  void testResponseContentType() {
    final ClassicHttpResponse httpResponse = mockResponse(200, "OK", "application/json;odata.metadata=minimal", "{}");
    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    assertEquals("application/json;odata.metadata=minimal", response.getContentType());
  }

  @Test
  void testResponseContentTypeNull() {
    final ClassicHttpResponse httpResponse = mock(ClassicHttpResponse.class);
    when(httpResponse.getCode()).thenReturn(204);
    when(httpResponse.getReasonPhrase()).thenReturn("No Content");
    when(httpResponse.getEntity()).thenReturn(null);

    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    assertNull(response.getContentType());
  }

  @Test
  void testResponseCloseWithCloseable() throws Exception {
    final ClassicHttpResponse httpResponse = mock(ClassicHttpResponse.class);
    when(httpResponse.getCode()).thenReturn(200);

    final ODataHttpResponse response = new ApacheHttpResponse(httpResponse);
    response.close();

    verify(httpResponse).close();
  }

  @Test
  @SuppressWarnings("removal")
  void testResponseUnwrap() {
    final ClassicHttpResponse httpResponse = mockResponse(200, "OK", null, null);
    final ApacheHttpResponse response = new ApacheHttpResponse(httpResponse);
    assertSame(httpResponse, response.unwrap());
  }

  @Test
  @SuppressWarnings("removal")
  void testResponseStaticUnwrapWithWrongTypeThrows() {
    final ODataHttpResponse wrongResponse = mock(ODataHttpResponse.class);
    assertThrows(IllegalArgumentException.class, () -> ApacheHttpResponse.unwrap(wrongResponse));
  }

  // --- DefaultHttpClientFactory tests ---

  @Test
  void testDefaultFactoryCreatesClient() {
    final DefaultHttpClientFactory factory = new DefaultHttpClientFactory();
    final ODataHttpClient client = factory.create(HttpMethod.GET, TEST_URI);
    assertNotNull(client);
    assertInstanceOf(ApacheHttpClient.class, client);
  }

  @Test
  void testDefaultFactoryClose() {
    final DefaultHttpClientFactory factory = new DefaultHttpClientFactory();
    final ODataHttpClient client = factory.create(HttpMethod.GET, TEST_URI);
    assertDoesNotThrow(() -> factory.close(client));
  }

  // --- DefaultHttpUriRequestFactory tests ---

  @Test
  void testRequestFactoryGet() {
    final DefaultHttpUriRequestFactory factory = new DefaultHttpUriRequestFactory();
    final ODataHttpRequest request = factory.create(HttpMethod.GET, TEST_URI);
    assertNotNull(request);
    assertInstanceOf(ApacheHttpRequest.class, request);
    assertEquals(HttpMethod.GET, request.getMethod());
  }

  @Test
  void testRequestFactoryPost() {
    final DefaultHttpUriRequestFactory factory = new DefaultHttpUriRequestFactory();
    final ODataHttpRequest request = factory.create(HttpMethod.POST, TEST_URI);
    assertEquals(HttpMethod.POST, request.getMethod());
    assertTrue(request.supportsEntity());
  }

  @Test
  void testRequestFactoryPut() {
    final DefaultHttpUriRequestFactory factory = new DefaultHttpUriRequestFactory();
    final ODataHttpRequest request = factory.create(HttpMethod.PUT, TEST_URI);
    assertEquals(HttpMethod.PUT, request.getMethod());
    assertTrue(request.supportsEntity());
  }

  @Test
  void testRequestFactoryPatch() {
    final DefaultHttpUriRequestFactory factory = new DefaultHttpUriRequestFactory();
    final ODataHttpRequest request = factory.create(HttpMethod.PATCH, TEST_URI);
    assertEquals(HttpMethod.PATCH, request.getMethod());
    assertTrue(request.supportsEntity());
  }

  @Test
  void testRequestFactoryDelete() {
    final DefaultHttpUriRequestFactory factory = new DefaultHttpUriRequestFactory();
    final ODataHttpRequest request = factory.create(HttpMethod.DELETE, TEST_URI);
    assertEquals(HttpMethod.DELETE, request.getMethod());
    assertFalse(request.supportsEntity());
  }

  @Test
  void testRequestFactoryMerge() {
    final DefaultHttpUriRequestFactory factory = new DefaultHttpUriRequestFactory();
    final ODataHttpRequest request = factory.create(HttpMethod.MERGE, TEST_URI);
    assertEquals(HttpMethod.MERGE, request.getMethod());
    assertTrue(request.supportsEntity());
  }

  // --- Helper methods ---

  private ClassicHttpResponse mockResponse(int statusCode, String reason, String contentType, String body) {
    final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
    when(response.getCode()).thenReturn(statusCode);
    when(response.getReasonPhrase()).thenReturn(reason);

    if (body != null) {
      final HttpEntity entity = mock(HttpEntity.class);
      try {
        when(entity.getContent()).thenReturn(
            new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (contentType != null) {
        when(entity.getContentType()).thenReturn(contentType);
      }
      when(response.getEntity()).thenReturn(entity);
    }

    when(response.getHeaders()).thenReturn(new Header[0]);
    return response;
  }
}
