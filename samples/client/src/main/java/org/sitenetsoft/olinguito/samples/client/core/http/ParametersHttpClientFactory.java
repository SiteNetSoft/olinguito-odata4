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
package org.sitenetsoft.olinguito.samples.client.core.http;

import java.net.URI;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpClientFactory;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Shows how to customize the runtime behavior of HTTP client component.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/fundamentals.html#d5e299">More
 * information</a>.
 *
 * @see ParametersHttpUriRequestFactory for how to customize at request level
 */
public class ParametersHttpClientFactory extends DefaultHttpClientFactory {

  @Override
  protected HttpClientBuilder createBuilder(final HttpMethod method, final URI uri) {
    final int timeout = 1000;
    final RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(timeout)
        .setSocketTimeout(timeout)
        .build();

    return super.createBuilder(method, uri).setDefaultRequestConfig(config);
  }

}
