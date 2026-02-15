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
 * Copyright 2026 SiteNetSoft - Bridge from ODataHttpRequest to Apache HttpUriRequest
 * Copyright 2026 SiteNetSoft - Implement transport-agnostic request manipulation methods
 * Copyright 2026 SiteNetSoft - Deprecated unwrap() methods in favor of transport-agnostic interface
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.ContentType;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Bridge implementation that wraps an Apache {@link HttpUriRequestBase} as an {@link ODataHttpRequest}.
 *
 * <p>In HC 5.x, all standard methods (GET, POST, PUT, DELETE, PATCH) extend
 * {@link HttpUriRequestBase}, which always supports an entity body. The
 * {@link #supportsEntity()} method uses method semantics (POST, PUT, PATCH, MERGE)
 * to determine whether setting an entity is appropriate.</p>
 */
public class ApacheHttpRequest implements ODataHttpRequest {

  private static final Set<String> ENTITY_METHODS = Set.of("POST", "PUT", "PATCH", "MERGE");

  final HttpUriRequestBase delegate;

  public ApacheHttpRequest(final HttpUriRequestBase delegate) {
    this.delegate = delegate;
  }

  @Override
  public URI getURI() {
    try {
      return delegate.getUri();
    } catch (java.net.URISyntaxException e) {
      throw new IllegalStateException("Invalid URI in request", e);
    }
  }

  @Override
  public void setURI(final URI uri) {
    delegate.setUri(uri);
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(delegate.getMethod());
  }

  @Override
  public void addHeader(final String name, final String value) {
    delegate.addHeader(name, value);
  }

  @Override
  public Map<String, String> getAllHeaders() {
    final Map<String, String> result = new LinkedHashMap<>();
    for (Header header : delegate.getHeaders()) {
      result.put(header.getName(), header.getValue());
    }
    return result;
  }

  @Override
  public boolean supportsEntity() {
    return ENTITY_METHODS.contains(delegate.getMethod());
  }

  @Override
  public void setEntity(final byte[] body, final boolean chunked) {
    if (!supportsEntity()) {
      throw new IllegalStateException("HTTP method " + delegate.getMethod() + " does not support a request body");
    }
    final ByteArrayEntity entity = new ByteArrayEntity(body, ContentType.DEFAULT_BINARY, chunked);
    delegate.setEntity(entity);
  }

  @Override
  public void setEntity(final InputStream body, final long contentLength, final boolean chunked) {
    if (!supportsEntity()) {
      throw new IllegalStateException("HTTP method " + delegate.getMethod() + " does not support a request body");
    }
    final InputStreamEntity entity = new InputStreamEntity(body, contentLength, ContentType.DEFAULT_BINARY);
    delegate.setEntity(entity);
  }

  @Override
  public void abort() {
    delegate.abort();
  }

  /**
   * Returns the underlying Apache {@link HttpUriRequestBase}.
   *
   * @return the wrapped HttpUriRequestBase instance
   * @deprecated Use the transport-agnostic {@link ODataHttpRequest} interface methods instead.
   */
  @Deprecated
  public HttpUriRequestBase unwrap() {
    return delegate;
  }

  /**
   * Extracts the Apache {@link HttpUriRequestBase} from an {@link ODataHttpRequest}.
   *
   * @param request the ODataHttpRequest (must be an ApacheHttpRequest)
   * @return the underlying HttpUriRequestBase
   * @throws IllegalArgumentException if request is not an ApacheHttpRequest
   * @deprecated Use the transport-agnostic {@link ODataHttpRequest} interface methods instead.
   */
  @Deprecated
  public static HttpUriRequestBase unwrap(final ODataHttpRequest request) {
    if (request instanceof ApacheHttpRequest apacheRequest) {
      return apacheRequest.delegate;
    }
    throw new IllegalArgumentException(
        "Expected ApacheHttpRequest but got " + (request == null ? "null" : request.getClass().getName()));
  }
}
