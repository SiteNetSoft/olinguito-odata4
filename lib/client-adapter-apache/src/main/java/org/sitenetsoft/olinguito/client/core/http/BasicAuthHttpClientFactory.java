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
package org.sitenetsoft.olinguito.client.core.http;

import java.net.URI;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Implementation for working with Basic Authentication.
 */
public class BasicAuthHttpClientFactory extends DefaultHttpClientFactory {

  private final String username;

  private final String password;

  public BasicAuthHttpClientFactory(final String username, final String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  protected HttpClientBuilder createBuilder(final HttpMethod method, final URI uri) {
    final BasicCredentialsProvider provider = new BasicCredentialsProvider();
    provider.setCredentials(
            new AuthScope(new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort())),
            new UsernamePasswordCredentials(username, password.toCharArray()));
    return super.createBuilder(method, uri).setDefaultCredentialsProvider(provider);
  }
}
