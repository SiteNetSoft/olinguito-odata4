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
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Default implementation returning HttpClients with default parameters.
 */
public class DefaultHttpClientFactory extends AbstractHttpClientFactory {

  protected HttpClientBuilder createBuilder(final HttpMethod method, final URI uri) {
    return HttpClientBuilder.create().setUserAgent(USER_AGENT);
  }

  @Override
  public CloseableHttpClient create(final HttpMethod method, final URI uri) {
    return createBuilder(method, uri).build();
  }

  @Override
  public void close(final HttpClient httpClient) {
    if (httpClient instanceof Closeable closeable) {
      try {
        closeable.close();
      } catch (IOException e) {
        // silently close
      }
    }
  }

}
