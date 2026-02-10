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

import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.api.http.WrappingHttpClientFactory;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Implementation for working behind an HTTP proxy (possibly requiring authentication); requires another concrete
 * {@link HttpClientFactory} implementation acting as real HTTP client factory.
 */
public class ProxyWrappingHttpClientFactory implements WrappingHttpClientFactory {

  private final URI proxy;

  private final String proxyUsername;

  private final String proxyPassword;

  private final DefaultHttpClientFactory wrapped;

  public ProxyWrappingHttpClientFactory(final URI proxy) {
    this(proxy, null, null, new DefaultHttpClientFactory());
  }

  public ProxyWrappingHttpClientFactory(final URI proxy, final String proxyUsername, final String proxyPassword) {
    this(proxy, proxyUsername, proxyPassword, new DefaultHttpClientFactory());
  }

  public ProxyWrappingHttpClientFactory(final URI proxy, final DefaultHttpClientFactory wrapped) {
    this(proxy, null, null, wrapped);
  }

  public ProxyWrappingHttpClientFactory(final URI proxy,
          final String proxyUsername, final String proxyPassword, final DefaultHttpClientFactory wrapped) {

    this.proxy = proxy;
    this.proxyUsername = proxyUsername;
    this.proxyPassword = proxyPassword;
    this.wrapped = wrapped;
  }

  @Override
  public DefaultHttpClientFactory getWrappedHttpClientFactory() {
    return this.wrapped;
  }

  @Override
  public HttpClient create(final HttpMethod method, final URI uri) {
    final HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
    final HttpClientBuilder builder = wrapped.createBuilder(method, uri).setProxy(proxyHost);

    if (proxyUsername != null && proxyPassword != null) {
      final CredentialsProvider provider = new BasicCredentialsProvider();
      provider.setCredentials(new AuthScope(proxyHost),
              new UsernamePasswordCredentials(proxyUsername, proxyPassword));
      builder.setDefaultCredentialsProvider(provider);
    }

    return builder.build();
  }

  @Override
  public void close(final HttpClient httpClient) {
    wrapped.close(httpClient);
  }

}
