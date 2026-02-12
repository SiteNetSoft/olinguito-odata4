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
 * Copyright 2026 SiteNetSoft - OkHttp HttpClientFactory implementation
 * Copyright 2026 SiteNetSoft - Shared client instance, HTTP/2, retry/timeout configuration
 * Copyright 2026 SiteNetSoft - Certificate pinning, event listener, async support
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * {@link HttpClientFactory} implementation that creates OkHttp-backed clients.
 * <p>
 * By default, a single shared {@link OkHttpClient} instance is created lazily and
 * reused across all requests (OkHttp best practice for connection pool and thread
 * pool sharing). Per-request customizations (e.g. different timeouts) are applied
 * via {@link OkHttpClient#newBuilder()}, which shares the connection pool.
 * <p>
 * Subclasses can override {@link #configureBuilder(OkHttpClient.Builder)} to
 * customize the shared client (e.g. adding interceptors for authentication).
 */
public class OkHttpClientFactory implements HttpClientFactory {

  private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 30;
  private static final int DEFAULT_READ_TIMEOUT_SECONDS = 30;
  private static final int DEFAULT_WRITE_TIMEOUT_SECONDS = 30;
  private static final int DEFAULT_CALL_TIMEOUT_SECONDS = 0; // no limit

  private volatile OkHttpClient sharedClient;
  private boolean http2Enabled = true;
  private boolean retryOnConnectionFailure = true;
  private int connectTimeoutSeconds = DEFAULT_CONNECT_TIMEOUT_SECONDS;
  private int readTimeoutSeconds = DEFAULT_READ_TIMEOUT_SECONDS;
  private int writeTimeoutSeconds = DEFAULT_WRITE_TIMEOUT_SECONDS;
  private int callTimeoutSeconds = DEFAULT_CALL_TIMEOUT_SECONDS;
  private int maxIdleConnections = 5;
  private int keepAliveDurationSeconds = 300;
  private final Map<String, List<String>> certificatePins = new LinkedHashMap<>();
  private EventListener.Factory eventListenerFactory;

  /**
   * Enables or disables HTTP/2 protocol support.
   * When enabled (default), OkHttp negotiates HTTP/2 for HTTPS connections
   * and falls back to HTTP/1.1 for plain HTTP.
   *
   * @param enabled {@code true} to enable HTTP/2
   * @return this factory for chaining
   */
  public OkHttpClientFactory setHttp2Enabled(final boolean enabled) {
    this.http2Enabled = enabled;
    resetSharedClient();
    return this;
  }

  /**
   * Enables or disables automatic retry on connection failure.
   *
   * @param retry {@code true} to retry on connection failure (default)
   * @return this factory for chaining
   */
  public OkHttpClientFactory setRetryOnConnectionFailure(final boolean retry) {
    this.retryOnConnectionFailure = retry;
    resetSharedClient();
    return this;
  }

  /**
   * Sets the connect timeout in seconds.
   *
   * @param seconds connect timeout (0 for no timeout)
   * @return this factory for chaining
   */
  public OkHttpClientFactory setConnectTimeout(final int seconds) {
    this.connectTimeoutSeconds = seconds;
    resetSharedClient();
    return this;
  }

  /**
   * Sets the read timeout in seconds.
   *
   * @param seconds read timeout (0 for no timeout)
   * @return this factory for chaining
   */
  public OkHttpClientFactory setReadTimeout(final int seconds) {
    this.readTimeoutSeconds = seconds;
    resetSharedClient();
    return this;
  }

  /**
   * Sets the write timeout in seconds.
   *
   * @param seconds write timeout (0 for no timeout)
   * @return this factory for chaining
   */
  public OkHttpClientFactory setWriteTimeout(final int seconds) {
    this.writeTimeoutSeconds = seconds;
    resetSharedClient();
    return this;
  }

  /**
   * Sets the overall call timeout in seconds (covers DNS, connect, write, read).
   *
   * @param seconds call timeout (0 for no timeout, the default)
   * @return this factory for chaining
   */
  public OkHttpClientFactory setCallTimeout(final int seconds) {
    this.callTimeoutSeconds = seconds;
    resetSharedClient();
    return this;
  }

  /**
   * Configures the connection pool. Set {@code maxIdleConnections} to 0 to disable
   * connection reuse (useful for testing).
   *
   * @param maxIdle maximum number of idle connections to keep in the pool
   * @param keepAliveSeconds how long idle connections are kept alive
   * @return this factory for chaining
   */
  public OkHttpClientFactory setConnectionPool(
      final int maxIdle, final int keepAliveSeconds) {
    this.maxIdleConnections = maxIdle;
    this.keepAliveDurationSeconds = keepAliveSeconds;
    resetSharedClient();
    return this;
  }

  /**
   * Adds a certificate pin for a hostname. Pins are SHA-256 hashes of the
   * certificate's Subject Public Key Info (SPKI).
   * <p>
   * Example:
   * <pre>
   *   factory.addCertificatePin("api.example.com",
   *       "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
   * </pre>
   *
   * @param hostname the hostname pattern (supports wildcards like {@code *.example.com})
   * @param pins     one or more {@code sha256/...} pin hashes
   * @return this factory for chaining
   */
  public OkHttpClientFactory addCertificatePin(final String hostname, final String... pins) {
    this.certificatePins.put(hostname, List.of(pins));
    resetSharedClient();
    return this;
  }

  /**
   * Removes all certificate pins.
   *
   * @return this factory for chaining
   */
  public OkHttpClientFactory clearCertificatePins() {
    this.certificatePins.clear();
    resetSharedClient();
    return this;
  }

  /**
   * Sets a custom {@link EventListener.Factory} for monitoring HTTP metrics.
   * Use {@link OkHttpEventLogger#FACTORY} for built-in SLF4J debug logging.
   *
   * @param factory the event listener factory, or {@code null} to disable
   * @return this factory for chaining
   */
  public OkHttpClientFactory setEventListenerFactory(final EventListener.Factory factory) {
    this.eventListenerFactory = factory;
    resetSharedClient();
    return this;
  }

  /**
   * Returns the shared {@link OkHttpClient} instance, creating it on first access.
   * The instance is reused across all requests from this factory.
   *
   * @return the shared OkHttpClient
   */
  protected OkHttpClient getSharedClient() {
    OkHttpClient client = this.sharedClient;
    if (client == null) {
      synchronized (this) {
        client = this.sharedClient;
        if (client == null) {
          final OkHttpClient.Builder builder = new OkHttpClient.Builder();
          configureBuilder(builder);
          client = builder.build();
          this.sharedClient = client;
        }
      }
    }
    return client;
  }

  /**
   * Configures the {@link OkHttpClient.Builder} for the shared client instance.
   * Subclasses may override to add interceptors, SSL configuration, etc.
   *
   * @param builder the builder to configure
   */
  protected void configureBuilder(final OkHttpClient.Builder builder) {
    builder
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
        .connectionPool(new ConnectionPool(
            maxIdleConnections, keepAliveDurationSeconds, TimeUnit.SECONDS))
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(retryOnConnectionFailure);

    if (http2Enabled) {
      builder.protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));
    } else {
      builder.protocols(List.of(Protocol.HTTP_1_1));
    }

    // When connection pooling is disabled, add Connection: close header
    // to prevent HTTP keep-alive reuse.
    if (maxIdleConnections == 0) {
      builder.addNetworkInterceptor(chain -> chain.proceed(
          chain.request().newBuilder()
              .header("Connection", "close")
              .build()));
    }

    // Certificate pinning
    if (!certificatePins.isEmpty()) {
      final CertificatePinner.Builder pinnerBuilder = new CertificatePinner.Builder();
      for (Map.Entry<String, List<String>> entry : certificatePins.entrySet()) {
        pinnerBuilder.add(entry.getKey(), entry.getValue().toArray(new String[0]));
      }
      builder.certificatePinner(pinnerBuilder.build());
    }

    // Event listener for metrics/logging
    if (eventListenerFactory != null) {
      builder.eventListenerFactory(eventListenerFactory);
    }
  }

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    return new OkHttpClientWrapper(getSharedClient());
  }

  @Override
  public void close(final ODataHttpClient httpClient) {
    // Don't close the shared client when individual wrappers are closed.
    // The shared client's resources are released when this factory is discarded.
    try {
      httpClient.close();
    } catch (IOException e) {
      // silently close
    }
  }

  private void resetSharedClient() {
    synchronized (this) {
      this.sharedClient = null;
    }
  }
}
