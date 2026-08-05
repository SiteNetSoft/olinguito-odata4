/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Migrate from deprecated DefaultHttpClient to HttpClientBuilder
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 */
package org.sitenetsoft.olinguito.samples.client.core.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpClientFactory;

/**
 * Shows how to install a custom exception recovery mechanism.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/fundamentals.html#d5e281">More
 * information</a>.
 */
public class RequestRetryHttpClientFactory extends DefaultHttpClientFactory {

  @Override
  protected HttpClientBuilder createBuilder(final HttpMethod method, final URI uri) {
    final HttpRequestRetryStrategy myRetryStrategy = new HttpRequestRetryStrategy() {

      @Override
      public boolean retryRequest(final HttpRequest request, final IOException exception,
              final int execCount, final HttpContext context) {
        if (execCount >= 5) {
          // Do not retry if over max retry count
          return false;
        }
        if (exception instanceof InterruptedIOException) {
          // Timeout
          return false;
        }
        if (exception instanceof UnknownHostException) {
          // Unknown host
          return false;
        }
        if (exception instanceof ConnectException) {
          // Connection refused
          return false;
        }
        if (exception instanceof SSLException) {
          // SSL handshake exception
          return false;
        }
        // Retry if the request is considered idempotent (not POST)
        return !"POST".equalsIgnoreCase(request.getMethod());
      }

      @Override
      public boolean retryRequest(final HttpResponse response, final int execCount,
              final HttpContext context) {
        return false;
      }

      @Override
      public TimeValue getRetryInterval(final HttpRequest request, final IOException exception,
              final int execCount, final HttpContext context) {
        return TimeValue.ofSeconds(1);
      }

      @Override
      public TimeValue getRetryInterval(final HttpResponse response, final int execCount,
              final HttpContext context) {
        return TimeValue.ofSeconds(1);
      }

    };

    return super.createBuilder(method, uri).setRetryStrategy(myRetryStrategy);
  }

}
