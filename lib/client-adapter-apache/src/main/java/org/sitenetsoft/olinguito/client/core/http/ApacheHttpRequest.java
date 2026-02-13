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
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Bridge implementation that wraps an Apache {@link HttpUriRequest} as an {@link ODataHttpRequest}.
 */
public class ApacheHttpRequest implements ODataHttpRequest {

  final HttpUriRequest delegate;

  public ApacheHttpRequest(final HttpUriRequest delegate) {
    this.delegate = delegate;
  }

  @Override
  public URI getURI() {
    return delegate.getURI();
  }

  @Override
  public void setURI(final URI uri) {
    if (delegate instanceof HttpRequestBase requestBase) {
      requestBase.setURI(uri);
    } else {
      throw new UnsupportedOperationException("Cannot set URI on " + delegate.getClass().getName());
    }
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
    for (Header header : delegate.getAllHeaders()) {
      result.put(header.getName(), header.getValue());
    }
    return result;
  }

  @Override
  public boolean supportsEntity() {
    return delegate instanceof HttpEntityEnclosingRequestBase;
  }

  @Override
  public void setEntity(final byte[] body, final boolean chunked) {
    if (delegate instanceof HttpEntityEnclosingRequestBase entityReq) {
      final ByteArrayEntity entity = new ByteArrayEntity(body);
      entity.setChunked(chunked);
      entityReq.setEntity(entity);
    } else {
      throw new IllegalStateException("HTTP method " + delegate.getMethod() + " does not support a request body");
    }
  }

  @Override
  public void setEntity(final InputStream body, final long contentLength, final boolean chunked) {
    if (delegate instanceof HttpEntityEnclosingRequestBase entityReq) {
      final InputStreamEntity entity = new InputStreamEntity(body, contentLength);
      entity.setChunked(chunked);
      entityReq.setEntity(entity);
    } else {
      throw new IllegalStateException("HTTP method " + delegate.getMethod() + " does not support a request body");
    }
  }

  @Override
  public void abort() {
    delegate.abort();
  }

  /**
   * Returns the underlying Apache {@link HttpUriRequest}.
   *
   * @return the wrapped HttpUriRequest instance
   * @deprecated Use the transport-agnostic {@link ODataHttpRequest} interface methods instead.
   */
  @Deprecated
  public HttpUriRequest unwrap() {
    return delegate;
  }

  /**
   * Extracts the Apache {@link HttpUriRequest} from an {@link ODataHttpRequest}.
   *
   * @param request the ODataHttpRequest (must be an ApacheHttpRequest)
   * @return the underlying HttpUriRequest
   * @throws IllegalArgumentException if request is not an ApacheHttpRequest
   * @deprecated Use the transport-agnostic {@link ODataHttpRequest} interface methods instead.
   */
  @Deprecated
  public static HttpUriRequest unwrap(final ODataHttpRequest request) {
    if (request instanceof ApacheHttpRequest apacheRequest) {
      return apacheRequest.delegate;
    }
    throw new IllegalArgumentException(
        "Expected ApacheHttpRequest but got " + (request == null ? "null" : request.getClass().getName()));
  }
}
