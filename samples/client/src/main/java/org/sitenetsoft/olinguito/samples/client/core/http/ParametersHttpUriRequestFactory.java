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
 * Copyright 2026 SiteNetSoft - Migrate from deprecated HttpParams to RequestConfig
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 */
package org.sitenetsoft.olinguito.samples.client.core.http;

import java.net.URI;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpRequest;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpUriRequestFactory;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Shows how to customize the runtime behavior of an HTTP request.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/fundamentals.html#d5e299">More
 * information</a>.
 *
 * @see ParametersHttpClientFactory for how to customize at whole client level
 */
public class ParametersHttpUriRequestFactory extends DefaultHttpUriRequestFactory {

  @Override
  public ODataHttpRequest create(final HttpMethod method, final URI uri) {
    final ODataHttpRequest odataRequest = super.create(method, uri);
    final HttpUriRequest request = ApacheHttpRequest.unwrap(odataRequest);

    final int timeout = 1000;
    final RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(timeout)
        .setSocketTimeout(timeout)
        .build();

    if (request instanceof HttpRequestBase) {
      ((HttpRequestBase) request).setConfig(config);
    }

    return odataRequest;
  }

}
