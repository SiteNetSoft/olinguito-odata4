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
 * Copyright 2026 SiteNetSoft - Bridge from ODataHttpClient to Apache HttpClient
 * Copyright 2026 SiteNetSoft - Implement transport-agnostic execute() method
 * Copyright 2026 SiteNetSoft - Deprecated unwrap() methods in favor of transport-agnostic interface
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;

/**
 * Bridge implementation that wraps an Apache {@link HttpClient} as an {@link ODataHttpClient}.
 */
public class ApacheHttpClient implements ODataHttpClient {

  private final HttpClient delegate;

  public ApacheHttpClient(final HttpClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public ODataHttpResponse execute(final ODataHttpRequest request) {
    final HttpUriRequest apacheRequest = ApacheHttpRequest.unwrap(request);
    try {
      final HttpResponse response = delegate.execute(apacheRequest);
      return new ApacheHttpResponse(response);
    } catch (IOException e) {
      throw new HttpClientException(request.getURI().toASCIIString(), e);
    }
  }

  /**
   * Returns the underlying Apache {@link HttpClient}.
   *
   * @return the wrapped HttpClient instance
   * @deprecated Use the transport-agnostic {@link ODataHttpClient#execute(ODataHttpRequest)} method instead.
   */
  @Deprecated
  public HttpClient unwrap() {
    return delegate;
  }

  /**
   * Extracts the Apache {@link HttpClient} from an {@link ODataHttpClient}.
   *
   * @param client the ODataHttpClient (must be an ApacheHttpClient)
   * @return the underlying HttpClient
   * @throws IllegalArgumentException if client is not an ApacheHttpClient
   * @deprecated Use the transport-agnostic {@link ODataHttpClient#execute(ODataHttpRequest)} method instead.
   */
  @Deprecated
  public static HttpClient unwrap(final ODataHttpClient client) {
    if (client instanceof ApacheHttpClient apacheClient) {
      return apacheClient.delegate;
    }
    throw new IllegalArgumentException(
        "Expected ApacheHttpClient but got " + (client == null ? "null" : client.getClass().getName()));
  }

  @Override
  public void close() throws IOException {
    if (delegate instanceof Closeable closeable) {
      closeable.close();
    }
  }
}
