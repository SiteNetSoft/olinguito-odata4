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
 * Copyright 2026 SiteNetSoft - Bridge from ODataHttpResponse to Apache HttpResponse
 * Copyright 2026 SiteNetSoft - Deprecated unwrap() methods in favor of transport-agnostic interface
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;

/**
 * Bridge implementation that wraps an Apache {@link HttpResponse} as an {@link ODataHttpResponse}.
 */
public class ApacheHttpResponse implements ODataHttpResponse {

  private final ClassicHttpResponse delegate;

  public ApacheHttpResponse(final ClassicHttpResponse delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getStatusCode() {
    return delegate.getCode();
  }

  @Override
  public String getReasonPhrase() {
    return delegate.getReasonPhrase();
  }

  @Override
  public Map<String, Collection<String>> getHeaders() {
    final Map<String, Collection<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (Header header : delegate.getHeaders()) {
      headers.computeIfAbsent(header.getName(), k -> new HashSet<>()).add(header.getValue());
    }
    return headers;
  }

  @Override
  public Collection<String> getHeader(final String name) {
    final Header[] headers = delegate.getHeaders(name);
    if (headers == null || headers.length == 0) {
      return null;
    }
    final Collection<String> values = new HashSet<>();
    for (Header header : headers) {
      values.add(header.getValue());
    }
    return values;
  }

  @Override
  public InputStream getBody() {
    try {
      return delegate.getEntity() == null ? null : delegate.getEntity().getContent();
    } catch (IOException e) {
      throw new IllegalStateException("Error retrieving response body", e);
    }
  }

  @Override
  public String getContentType() {
    if (delegate.getEntity() == null || delegate.getEntity().getContentType() == null) {
      return null;
    }
    return delegate.getEntity().getContentType();
  }

  /**
   * Returns the underlying Apache {@link ClassicHttpResponse}.
   *
   * @return the wrapped ClassicHttpResponse instance
   * @deprecated Use the transport-agnostic {@link ODataHttpResponse} interface methods instead.
   */
  @Deprecated(forRemoval = true)
  public ClassicHttpResponse unwrap() {
    return delegate;
  }

  /**
   * Extracts the Apache {@link ClassicHttpResponse} from an {@link ODataHttpResponse}.
   *
   * @param response the ODataHttpResponse (must be an ApacheHttpResponse)
   * @return the underlying ClassicHttpResponse
   * @throws IllegalArgumentException if response is not an ApacheHttpResponse
   * @deprecated Use the transport-agnostic {@link ODataHttpResponse} interface methods instead.
   */
  @Deprecated(forRemoval = true)
  public static ClassicHttpResponse unwrap(final ODataHttpResponse response) {
    if (response instanceof ApacheHttpResponse apacheResponse) {
      return apacheResponse.delegate;
    }
    throw new IllegalArgumentException(
        "Expected ApacheHttpResponse but got " + (response == null ? "null" : response.getClass().getName()));
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
