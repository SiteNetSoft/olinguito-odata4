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
 * Copyright 2026 SiteNetSoft - New abstraction to decouple client-api from Apache HttpComponents
 * Copyright 2026 SiteNetSoft - Added execute() method for transport-agnostic HTTP execution
 * Copyright 2026 SiteNetSoft - Added executeAsync() for non-blocking HTTP execution
 */
package org.sitenetsoft.olinguito.client.api.http;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * Transport-agnostic handle for an HTTP client instance.
 * <p>
 * Implementations wrap a concrete HTTP client library (e.g. Apache HttpComponents, OkHttp).
 * The core client-api never accesses the underlying library directly;
 * only {@code client-core} bridge implementations unwrap to the native type.
 */
public interface ODataHttpClient extends Closeable {

  /**
   * Executes the given HTTP request and returns the response.
   *
   * @param request the request to execute
   * @return the HTTP response
   * @throws HttpClientException if an I/O or protocol error occurs
   */
  default ODataHttpResponse execute(ODataHttpRequest request) {
    throw new UnsupportedOperationException("execute() not implemented by " + getClass().getName());
  }

  /**
   * Executes the given HTTP request asynchronously.
   * <p>
   * The default implementation delegates to {@link #execute(ODataHttpRequest)} on the
   * common fork-join pool. Adapters with native async support (e.g. OkHttp) override
   * this to use the library's own async mechanism.
   *
   * @param request the request to execute
   * @return a future that completes with the HTTP response
   */
  default CompletableFuture<ODataHttpResponse> executeAsync(ODataHttpRequest request) {
    return CompletableFuture.supplyAsync(() -> execute(request));
  }
}
