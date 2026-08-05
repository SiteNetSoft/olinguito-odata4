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
 * Copyright 2026 SiteNetSoft - OkHttp EventListener for connection metrics logging
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * {@link EventListener} implementation that logs OkHttp connection lifecycle
 * events via SLF4J at DEBUG level.
 * <p>
 * Enable by calling {@link OkHttpClientFactory#setEventListenerFactory(EventListener.Factory)}
 * with {@code OkHttpEventLogger.FACTORY}.
 * <p>
 * Logged events include DNS resolution, TCP connect, TLS handshake,
 * request/response timing, and connection release.
 */
public class OkHttpEventLogger extends EventListener {

  private static final Logger LOG = LoggerFactory.getLogger(OkHttpEventLogger.class);

  /**
   * Factory that creates a new {@link OkHttpEventLogger} per call.
   */
  public static final Factory FACTORY = call -> new OkHttpEventLogger();

  private long callStartNanos;

  @Override
  public void callStart(final Call call) {
    callStartNanos = System.nanoTime();
    if (LOG.isDebugEnabled()) {
      final Request request = call.request();
      LOG.debug("[OkHttp] >> {} {}", request.method(), request.url());
    }
  }

  @Override
  public void dnsStart(final Call call, final String domainName) {
    LOG.debug("[OkHttp] DNS lookup: {}", domainName);
  }

  @Override
  public void dnsEnd(final Call call, final String domainName,
                     final List<InetAddress> inetAddressList) {
    LOG.debug("[OkHttp] DNS resolved: {} -> {} ({} ms)", domainName, inetAddressList,
        elapsedMs());
  }

  @Override
  public void connectStart(final Call call, final InetSocketAddress inetSocketAddress,
                           final Proxy proxy) {
    LOG.debug("[OkHttp] Connecting to {} via {}", inetSocketAddress, proxy.type());
  }

  @Override
  public void connectEnd(final Call call, final InetSocketAddress inetSocketAddress,
                         final Proxy proxy, final Protocol protocol) {
    LOG.debug("[OkHttp] Connected to {} [{}] ({} ms)", inetSocketAddress, protocol,
        elapsedMs());
  }

  @Override
  public void connectFailed(final Call call, final InetSocketAddress inetSocketAddress,
                            final Proxy proxy, final Protocol protocol,
                            final IOException ioe) {
    LOG.debug("[OkHttp] Connection failed to {}: {} ({} ms)", inetSocketAddress,
        ioe.getMessage(), elapsedMs());
  }

  @Override
  public void secureConnectStart(final Call call) {
    LOG.debug("[OkHttp] TLS handshake starting ({} ms)", elapsedMs());
  }

  @Override
  public void secureConnectEnd(final Call call, final Handshake handshake) {
    if (handshake != null) {
      LOG.debug("[OkHttp] TLS handshake complete: {} ({} ms)",
          handshake.tlsVersion(), elapsedMs());
    }
  }

  @Override
  public void connectionAcquired(final Call call, final Connection connection) {
    LOG.debug("[OkHttp] Connection acquired [{}] ({} ms)",
        connection.protocol(), elapsedMs());
  }

  @Override
  public void connectionReleased(final Call call, final Connection connection) {
    LOG.debug("[OkHttp] Connection released ({} ms)", elapsedMs());
  }

  @Override
  public void requestHeadersStart(final Call call) {
    LOG.debug("[OkHttp] Sending request headers ({} ms)", elapsedMs());
  }

  @Override
  public void requestBodyEnd(final Call call, final long byteCount) {
    LOG.debug("[OkHttp] Sent {} bytes ({} ms)", byteCount, elapsedMs());
  }

  @Override
  public void responseHeadersEnd(final Call call, final Response response) {
    LOG.debug("[OkHttp] << {} {} ({} ms)", response.code(), response.message(),
        elapsedMs());
  }

  @Override
  public void responseBodyEnd(final Call call, final long byteCount) {
    LOG.debug("[OkHttp] Received {} bytes ({} ms)", byteCount, elapsedMs());
  }

  @Override
  public void callEnd(final Call call) {
    LOG.debug("[OkHttp] Call complete ({} ms total)", elapsedMs());
  }

  @Override
  public void callFailed(final Call call, final IOException ioe) {
    LOG.debug("[OkHttp] Call failed: {} ({} ms total)", ioe.getMessage(), elapsedMs());
  }

  private long elapsedMs() {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - callStartNanos);
  }
}
