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
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Replaced deprecated AndroidHttpClient with OkHttp adapter
 */
package org.sitenetsoft.olinguito.client.core.android.http;

import java.util.concurrent.TimeUnit;

import org.sitenetsoft.olinguito.client.adapter.okhttp.OkHttpClientFactory;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * Android-optimized {@link OkHttpClientFactory} with settings tuned for mobile devices.
 * <p>
 * Replaces the deprecated {@code android.net.http.AndroidHttpClient} with OkHttp,
 * the de-facto standard HTTP library on Android. Configures connection pooling
 * and timeouts appropriate for mobile network conditions.
 */
public class AndroidHttpClientFactory extends OkHttpClientFactory {

  private static final int MAX_IDLE_CONNECTIONS = 5;
  private static final int KEEP_ALIVE_DURATION_SECONDS = 30;

  public AndroidHttpClientFactory() {
    setConnectTimeout(15);
    setReadTimeout(20);
    setWriteTimeout(20);
    setRetryOnConnectionFailure(true);
  }

  @Override
  protected void configureBuilder(final OkHttpClient.Builder builder) {
    super.configureBuilder(builder);
    builder.connectionPool(
        new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION_SECONDS, TimeUnit.SECONDS));
  }
}
