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
 * Copyright 2026 SiteNetSoft - Transport-agnostic GZIP/deflate compression decorator
 * Copyright 2026 SiteNetSoft - Removed Apache-specific special-case; fully transport-agnostic
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.io.IOException;

import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;

/**
 * Transport-agnostic decorator that adds {@code Accept-Encoding: gzip, deflate}
 * to outgoing requests, enabling transparent response decompression.
 * <p>
 * Works with any {@link ODataHttpClient} implementation:
 * <ul>
 *   <li>Apache HttpClient handles decompression via its built-in
 *       {@code ResponseContentEncoding} interceptor when using
 *       {@code ContentCompressingHttpClient}.</li>
 *   <li>OkHttp handles gzip decompression transparently by default
 *       when the {@code Accept-Encoding} header is present.</li>
 * </ul>
 */
public class CompressingODataHttpClient implements ODataHttpClient {

  private final ODataHttpClient delegate;

  public CompressingODataHttpClient(final ODataHttpClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public ODataHttpResponse execute(final ODataHttpRequest request) {
    request.addHeader("Accept-Encoding", "gzip, deflate");
    return delegate.execute(request);
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
