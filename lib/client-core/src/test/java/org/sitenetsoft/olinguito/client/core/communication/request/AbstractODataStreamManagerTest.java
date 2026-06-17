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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1587: configurable response timeout
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.junit.jupiter.api.Test;

/** OLINGO-1587: the no-arg getResponse() must honor the configured timeout, not a hardcoded 300s. */
class AbstractODataStreamManagerTest {

  /** Minimal concrete manager whose getResponse(timeout) simply waits on the wrapped future. */
  static final class TestStreamManager extends AbstractODataStreamManager<ODataResponse> {
    TestStreamManager(final Wrapper<Future<ODataHttpResponse>> futureWrap) {
      super(futureWrap);
    }

    @Override
    protected ODataResponse getResponse(final long timeout, final TimeUnit unit) {
      getHttpResponse(timeout, unit); // throws HttpClientException once the timeout elapses
      return null;
    }
  }

  @Test
  void getResponseHonorsConfiguredTimeout() {
    final Wrapper<Future<ODataHttpResponse>> futureWrap = new Wrapper<>();
    futureWrap.setWrapped(new CompletableFuture<>()); // never completes

    final TestStreamManager manager = new TestStreamManager(futureWrap);
    manager.setResponseTimeoutInSec(1);

    final long startNanos = System.nanoTime();
    assertThrows(HttpClientException.class, manager::getResponse);
    final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;

    // With the old hardcoded 300s default this would block for minutes.
    assertTrue(elapsedMillis < 30_000L,
        "getResponse() should time out near the configured 1s, took " + elapsedMillis + "ms");
  }
}
