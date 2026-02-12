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
 * Copyright 2026 SiteNetSoft - OkHttp Basic Auth client factory
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;

/**
 * {@link OkHttpClientFactory} that adds HTTP Basic Authentication via an OkHttp interceptor.
 */
public class OkHttpBasicAuthClientFactory extends OkHttpClientFactory {

  private final String username;
  private final String password;

  public OkHttpBasicAuthClientFactory(final String username, final String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  protected void configureBuilder(final OkHttpClient.Builder builder) {
    super.configureBuilder(builder);
    final String credential = Credentials.basic(username, password);
    builder.addInterceptor(chain -> chain.proceed(
        chain.request().newBuilder()
            .header("Authorization", credential)
            .build()));
  }
}
