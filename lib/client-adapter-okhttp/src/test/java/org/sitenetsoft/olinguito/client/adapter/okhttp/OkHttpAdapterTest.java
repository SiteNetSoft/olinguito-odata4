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
 * Copyright 2026 SiteNetSoft - Unit tests for OkHttp adapter
 * Copyright 2026 SiteNetSoft - Replaced wildcard import with explicit imports
 * Copyright 2026 SiteNetSoft - Updated for OkHttp 5.3.2 API changes
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

class OkHttpAdapterTest {

  private MockWebServer server;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    server.close();
  }

  @Test
  void testGetRequest() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body("{\"value\":42}")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/test").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, uri);
      request.addHeader("Accept", "application/json");

      final ODataHttpResponse response = client.execute(request);

      assertEquals(200, response.getStatusCode());
      assertNotNull(response.getContentType());
      assertTrue(response.getContentType().startsWith("application/json"));

      try (InputStream body = response.getBody()) {
        assertNotNull(body);
        final String bodyStr = new String(body.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("{\"value\":42}", bodyStr);
      }

      response.close();
    }

    final RecordedRequest recorded = server.takeRequest();
    assertEquals("GET", recorded.getMethod());
    assertEquals("/test", recorded.getUrl().encodedPath());
    assertEquals("application/json", recorded.getHeaders().get("Accept"));
  }

  @Test
  void testPostRequestWithByteBody() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(201)
        .addHeader("Location", "/entities/1")
        .body("")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/entities").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.POST, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.POST, uri);
      request.addHeader("Content-Type", "application/json");

      final byte[] body = "{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8);
      request.setEntity(body, false);

      final ODataHttpResponse response = client.execute(request);

      assertEquals(201, response.getStatusCode());
      response.close();
    }

    final RecordedRequest recorded = server.takeRequest();
    assertEquals("POST", recorded.getMethod());
    assertEquals("{\"name\":\"test\"}", recorded.getBody().utf8());
  }

  @Test
  void testPostRequestWithStreamBody() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .body("ok")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/stream").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.POST, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.POST, uri);
      request.addHeader("Content-Type", "text/plain");

      final InputStream bodyStream = new ByteArrayInputStream("streaming body".getBytes(StandardCharsets.UTF_8));
      request.setEntity(bodyStream, -1, true);

      final ODataHttpResponse response = client.execute(request);

      assertEquals(200, response.getStatusCode());
      response.close();
    }

    final RecordedRequest recorded = server.takeRequest();
    assertEquals("POST", recorded.getMethod());
    assertEquals("streaming body", recorded.getBody().utf8());
  }

  @Test
  void testPutRequest() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(204)
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/entities/1").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.PUT, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.PUT, uri);
      request.addHeader("Content-Type", "application/json");
      request.setEntity("{\"name\":\"updated\"}".getBytes(StandardCharsets.UTF_8), false);

      final ODataHttpResponse response = client.execute(request);

      assertEquals(204, response.getStatusCode());
      response.close();
    }

    final RecordedRequest recorded = server.takeRequest();
    assertEquals("PUT", recorded.getMethod());
  }

  @Test
  void testPatchRequest() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .body("{}")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/entities/1").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.PATCH, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.PATCH, uri);
      request.addHeader("Content-Type", "application/json");
      request.setEntity("{\"field\":\"value\"}".getBytes(StandardCharsets.UTF_8), false);

      final ODataHttpResponse response = client.execute(request);
      assertEquals(200, response.getStatusCode());
      response.close();
    }

    assertEquals("PATCH", server.takeRequest().getMethod());
  }

  @Test
  void testDeleteRequest() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(204)
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/entities/1").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.DELETE, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.DELETE, uri);

      final ODataHttpResponse response = client.execute(request);
      assertEquals(204, response.getStatusCode());
      response.close();
    }

    assertEquals("DELETE", server.takeRequest().getMethod());
  }

  @Test
  void testMergeRequest() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .body("{}")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/entities/1").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.MERGE, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.MERGE, uri);
      request.addHeader("Content-Type", "application/json");
      request.setEntity("{}".getBytes(StandardCharsets.UTF_8), false);

      final ODataHttpResponse response = client.execute(request);
      assertEquals(200, response.getStatusCode());
      response.close();
    }

    assertEquals("MERGE", server.takeRequest().getMethod());
  }

  @Test
  void testResponseHeaders() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .addHeader("X-Custom", "value1")
        .addHeader("X-Custom", "value2")
        .addHeader("ETag", "W/\"abc\"")
        .body("")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/test").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, uri);
      final ODataHttpResponse response = client.execute(request);

      final Map<String, Collection<String>> allHeaders = response.getHeaders();
      assertNotNull(allHeaders);

      // Multi-valued header
      final Collection<String> customValues = response.getHeader("X-Custom");
      assertNotNull(customValues);
      assertEquals(2, customValues.size());
      assertTrue(customValues.contains("value1"));
      assertTrue(customValues.contains("value2"));

      // Single-valued header
      final Collection<String> etagValues = response.getHeader("ETag");
      assertNotNull(etagValues);
      assertTrue(etagValues.contains("W/\"abc\""));

      // Non-existent header
      assertNull(response.getHeader("X-Missing"));

      response.close();
    }
  }

  @Test
  void testRequestWrapperProperties() {
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = URI.create("http://example.com/odata");

    final ODataHttpRequest request = requestFactory.create(HttpMethod.POST, uri);

    assertEquals(uri, request.getURI());
    assertEquals(HttpMethod.POST, request.getMethod());
    assertTrue(request.supportsEntity());

    // Change URI
    final URI newUri = URI.create("http://example.com/odata/entities");
    request.setURI(newUri);
    assertEquals(newUri, request.getURI());

    // Headers
    request.addHeader("Accept", "application/json");
    request.addHeader("X-Test", "abc");
    final Map<String, String> headers = request.getAllHeaders();
    assertEquals("application/json", headers.get("Accept"));
    assertEquals("abc", headers.get("X-Test"));
  }

  @Test
  void testGetRequestDoesNotSupportEntity() {
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, URI.create("http://example.com"));
    assertFalse(request.supportsEntity());
  }

  @Test
  void testDeleteRequestDoesNotSupportEntity() {
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final ODataHttpRequest request = requestFactory.create(HttpMethod.DELETE, URI.create("http://example.com"));
    assertFalse(request.supportsEntity());
  }

  @Test
  void testConnectionRefusedThrowsHttpClientException() {
    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    // Use a port that is not listening
    final URI uri = URI.create("http://localhost:1/test");

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, uri);
      assertThrows(HttpClientException.class, () -> client.execute(request));
    } catch (IOException e) {
      fail("close() should not throw", e);
    }
  }

  @Test
  void testBasicAuthFactory() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .body("authenticated")
        .build());

    final OkHttpBasicAuthClientFactory factory = new OkHttpBasicAuthClientFactory("user", "pass");
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/secure").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, uri);
      final ODataHttpResponse response = client.execute(request);

      assertEquals(200, response.getStatusCode());
      response.close();
    }

    final RecordedRequest recorded = server.takeRequest();
    final String authHeader = recorded.getHeaders().get("Authorization");
    assertNotNull(authHeader);
    assertTrue(authHeader.startsWith("Basic "));
  }

  @Test
  void testExecuteWithWrongRequestTypeThrows() {
    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final URI uri = URI.create("http://example.com");

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      // Create a mock request that isn't OkHttpRequestWrapper
      final ODataHttpRequest badRequest = new ODataHttpRequest() {
        @Override
        public URI getURI() {
          return uri;
        }
      };
      assertThrows(IllegalArgumentException.class, () -> client.execute(badRequest));
    } catch (IOException e) {
      fail("close() should not throw", e);
    }
  }

  @Test
  void testAsyncGetRequest() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body("{\"async\":true}")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/async").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, uri);

      final CompletableFuture<ODataHttpResponse> future = client.executeAsync(request);
      final ODataHttpResponse response = future.get();

      assertEquals(200, response.getStatusCode());
      try (InputStream body = response.getBody()) {
        assertEquals("{\"async\":true}", new String(body.readAllBytes(), StandardCharsets.UTF_8));
      }
      response.close();
    }
  }

  @Test
  void testAsyncPostRequest() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(201)
        .body("")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/async-create").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.POST, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.POST, uri);
      request.addHeader("Content-Type", "application/json");
      request.setEntity("{\"data\":1}".getBytes(StandardCharsets.UTF_8), false);

      final ODataHttpResponse response = client.executeAsync(request).get();
      assertEquals(201, response.getStatusCode());
      response.close();
    }
  }

  @Test
  void testAsyncConnectionRefusedCompletesExceptionally() {
    final OkHttpClientFactory factory = new OkHttpClientFactory();
    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = URI.create("http://localhost:1/fail");

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, uri);
      final CompletableFuture<ODataHttpResponse> future = client.executeAsync(request);

      final ExecutionException ex = assertThrows(ExecutionException.class, future::get);
      assertInstanceOf(org.sitenetsoft.olinguito.client.api.http.HttpClientException.class, ex.getCause());
    } catch (IOException e) {
      fail("close() should not throw", e);
    }
  }

  @Test
  void testCertificatePinningConfiguration() {
    final OkHttpClientFactory factory = new OkHttpClientFactory();
    factory.addCertificatePin("example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

    // Verify factory creates clients without error (pin validation happens at connect time)
    final ODataHttpClient client = factory.create(HttpMethod.GET, URI.create("http://example.com"));
    assertNotNull(client);
  }

  @Test
  void testEventListenerConfiguration() throws Exception {
    server.enqueue(new MockResponse.Builder()
        .code(200)
        .body("ok")
        .build());

    final OkHttpClientFactory factory = new OkHttpClientFactory();
    factory.setEventListenerFactory(OkHttpEventLogger.FACTORY);

    final OkHttpUriRequestFactory requestFactory = new OkHttpUriRequestFactory();
    final URI uri = server.url("/metrics").uri();

    try (ODataHttpClient client = factory.create(HttpMethod.GET, uri)) {
      final ODataHttpRequest request = requestFactory.create(HttpMethod.GET, uri);
      final ODataHttpResponse response = client.execute(request);
      assertEquals(200, response.getStatusCode());
      response.close();
    }
  }
}
