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
 * Copyright 2026 SiteNetSoft - OkHttp adapter for ODataHttpClient
 * Copyright 2026 SiteNetSoft - Native async executeAsync() via OkHttp enqueue()
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * {@link ODataHttpClient} implementation backed by an OkHttp {@link OkHttpClient}.
 */
public class OkHttpClientWrapper implements ODataHttpClient {

  private final OkHttpClient client;

  public OkHttpClientWrapper(final OkHttpClient client) {
    this.client = client;
  }

  @Override
  public ODataHttpResponse execute(final ODataHttpRequest request) {
    if (!(request instanceof OkHttpRequestWrapper wrapper)) {
      throw new IllegalArgumentException(
          "Expected OkHttpRequestWrapper but got " + (request == null ? "null" : request.getClass().getName()));
    }

    final Request okRequest = wrapper.build();
    final Call call = client.newCall(okRequest);
    wrapper.setActiveCall(call);

    try {
      final Response response = call.execute();
      return new OkHttpResponseWrapper(response);
    } catch (IOException e) {
      throw new HttpClientException(request.getURI().toASCIIString(), e);
    }
  }

  @Override
  public CompletableFuture<ODataHttpResponse> executeAsync(final ODataHttpRequest request) {
    if (!(request instanceof OkHttpRequestWrapper wrapper)) {
      throw new IllegalArgumentException(
          "Expected OkHttpRequestWrapper but got " + (request == null ? "null" : request.getClass().getName()));
    }

    final Request okRequest = wrapper.build();
    final Call call = client.newCall(okRequest);
    wrapper.setActiveCall(call);

    final CompletableFuture<ODataHttpResponse> future = new CompletableFuture<>();
    call.enqueue(new Callback() {
      @Override
      public void onFailure(final Call call, final IOException e) {
        future.completeExceptionally(new HttpClientException(request.getURI().toASCIIString(), e));
      }

      @Override
      public void onResponse(final Call call, final Response response) {
        future.complete(new OkHttpResponseWrapper(response));
      }
    });
    return future;
  }

  /**
   * Returns the underlying OkHttp client.
   *
   * @return the wrapped OkHttpClient instance
   */
  public OkHttpClient unwrap() {
    return client;
  }

  @Override
  public void close() {
    // No-op: OkHttpClient instances are shared across requests via the factory.
    // The connection pool and dispatcher are managed by the shared client's lifecycle.
  }
}
