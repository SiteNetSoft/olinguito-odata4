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
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.net.URI;

import org.apache.http.client.methods.HttpUriRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;

/**
 * Bridge implementation that wraps an Apache {@link HttpUriRequest} as an {@link ODataHttpRequest}.
 */
public class ApacheHttpRequest implements ODataHttpRequest {

  private final HttpUriRequest delegate;

  public ApacheHttpRequest(final HttpUriRequest delegate) {
    this.delegate = delegate;
  }

  @Override
  public URI getURI() {
    return delegate.getURI();
  }

  /**
   * Returns the underlying Apache {@link HttpUriRequest}.
   *
   * @return the wrapped HttpUriRequest instance
   */
  public HttpUriRequest unwrap() {
    return delegate;
  }

  /**
   * Extracts the Apache {@link HttpUriRequest} from an {@link ODataHttpRequest}.
   *
   * @param request the ODataHttpRequest (must be an ApacheHttpRequest)
   * @return the underlying HttpUriRequest
   * @throws IllegalArgumentException if request is not an ApacheHttpRequest
   */
  public static HttpUriRequest unwrap(final ODataHttpRequest request) {
    if (request instanceof ApacheHttpRequest apacheRequest) {
      return apacheRequest.delegate;
    }
    throw new IllegalArgumentException(
        "Expected ApacheHttpRequest but got " + (request == null ? "null" : request.getClass().getName()));
  }
}
