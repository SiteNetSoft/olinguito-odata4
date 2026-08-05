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
 * Copyright 2026 SiteNetSoft - OkHttp adapter for ODataHttpRequest
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * {@link ODataHttpRequest} implementation backed by OkHttp.
 * <p>
 * Accumulates headers, body, URI, and method; the final {@link Request} is
 * built lazily when {@link #build()} is called by the client wrapper.
 */
public class OkHttpRequestWrapper implements ODataHttpRequest {

  private static final Set<String> ENTITY_METHODS = Set.of("POST", "PUT", "PATCH", "MERGE");

  private URI uri;
  private final HttpMethod method;
  private final Map<String, String> headers = new LinkedHashMap<>();
  private byte[] bodyBytes;
  private InputStream bodyStream;
  private long contentLength = -1;
  private volatile Call activeCall;

  public OkHttpRequestWrapper(final HttpMethod method, final URI uri) {
    this.method = method;
    this.uri = uri;
  }

  @Override
  public URI getURI() {
    return uri;
  }

  @Override
  public void setURI(final URI uri) {
    this.uri = uri;
  }

  @Override
  public HttpMethod getMethod() {
    return method;
  }

  @Override
  public void addHeader(final String name, final String value) {
    headers.put(name, value);
  }

  @Override
  public Map<String, String> getAllHeaders() {
    return Collections.unmodifiableMap(headers);
  }

  @Override
  public boolean supportsEntity() {
    return ENTITY_METHODS.contains(method.name());
  }

  @Override
  public void setEntity(final byte[] body, final boolean chunked) {
    this.bodyBytes = body;
    this.bodyStream = null;
    this.contentLength = body.length;
  }

  @Override
  public void setEntity(final InputStream body, final long contentLength, final boolean chunked) {
    this.bodyStream = body;
    this.bodyBytes = null;
    this.contentLength = contentLength;
  }

  @Override
  public void abort() {
    final Call call = this.activeCall;
    if (call != null) {
      call.cancel();
    }
  }

  /**
   * Sets the active OkHttp {@link Call} so that {@link #abort()} can cancel it.
   *
   * @param call the in-flight call
   */
  void setActiveCall(final Call call) {
    this.activeCall = call;
  }

  /**
   * Builds the OkHttp {@link Request} from the accumulated state.
   *
   * @return a fully configured OkHttp request
   */
  Request build() {
    final Request.Builder builder = new Request.Builder().url(uri.toASCIIString());

    // Add headers
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder.addHeader(entry.getKey(), entry.getValue());
    }

    // Build request body
    final RequestBody requestBody = buildRequestBody();

    // OkHttp method — supports arbitrary method strings
    builder.method(method.name(), requestBody);

    return builder.build();
  }

  private RequestBody buildRequestBody() {
    if (!supportsEntity()) {
      return null;
    }

    final MediaType mediaType = resolveMediaType();

    if (bodyBytes != null) {
      return RequestBody.create(bodyBytes, mediaType);
    }

    if (bodyStream != null) {
      final long length = this.contentLength;
      final InputStream stream = this.bodyStream;
      return new RequestBody() {
        @Override
        public MediaType contentType() {
          return mediaType;
        }

        @Override
        public long contentLength() {
          return length >= 0 ? length : -1;
        }

        @Override
        public void writeTo(final BufferedSink sink) throws java.io.IOException {
          try (var source = Okio.source(stream)) {
            sink.writeAll(source);
          }
        }
      };
    }

    // Entity method with no body set yet — OkHttp requires a body for POST/PUT/PATCH
    return RequestBody.create(new byte[0], mediaType);
  }

  private MediaType resolveMediaType() {
    final String ct = headers.get("Content-Type");
    if (ct != null) {
      final MediaType parsed = MediaType.parse(ct);
      if (parsed != null) {
        return parsed;
      }
    }
    return null;
  }
}
