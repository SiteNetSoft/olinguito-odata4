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
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 */
package org.sitenetsoft.olinguito.samples.client.core.http;

import java.net.URI;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.core.http.AbstractHttpClientFactory;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpClient;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Shows how to customize the way how the underlying network socket are managed by the HTTP component; the specific
 * sample is about how to trust self-signed SSL certificates and also empowers connection management.
 * <br/>
 * HTTP connections make use of a java.net.Socket object internally to handle transmission of data across the wire.
 * However they rely on the SchemeSocketFactory interface to create, initialize and connect sockets. This enables the
 * users of HttpClient to provide application specific socket initialization code at runtime. PlainSocketFactory is the
 * default factory for creating and initializing plain (unencrypted) sockets.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/connmgmt.html#d5e512">More
 * information</a>.
 */
public class SocketFactoryHttpClientFactory extends AbstractHttpClientFactory {

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    try {
      final TrustStrategy acceptTrustStrategy = (certificate, authType) -> true;

      final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
              new SSLContextBuilder().loadTrustMaterial(null, acceptTrustStrategy).build(),
              NoopHostnameVerifier.INSTANCE);

      final HttpClientConnectionManager connectionManager =
              PoolingHttpClientConnectionManagerBuilder.create()
                      .setSSLSocketFactory(sslSocketFactory)
                      .build();

      return new ApacheHttpClient(HttpClientBuilder.create()
              .setUserAgent(USER_AGENT)
              .setConnectionManager(connectionManager)
              .build());
    } catch (Exception e) {
      throw new ODataRuntimeException(e);
    }
  }

  @Override
  public void close(final ODataHttpClient httpClient) {
    try {
      httpClient.close();
    } catch (java.io.IOException e) {
      // silently close
    }
  }
}
