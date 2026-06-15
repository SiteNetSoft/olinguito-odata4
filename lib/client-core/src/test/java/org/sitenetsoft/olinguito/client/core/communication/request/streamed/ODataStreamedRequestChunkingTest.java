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
 * Copyright 2026 SiteNetSoft - OLINGO-1468: Test that streamed requests do not block when chunking is disabled
 */
package org.sitenetsoft.olinguito.client.core.communication.request.streamed;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

class ODataStreamedRequestChunkingTest {

  /**
   * OLINGO-1468: when chunked encoding is disabled, a streamed request buffers its body. The body
   * of a streamed request is written by the caller only after {@code payloadManager()} returns, so
   * buffering it (readAllBytes) on the calling thread blocks forever. {@code payloadManager()} must
   * defer the buffering to the executor and return promptly.
   */
  @Test
  void payloadManagerDoesNotBlockWhenChunkingDisabled() throws InterruptedException {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setUseChuncked(false);

    // A payload stream whose first read blocks until released, simulating a body that is only
    // produced after payloadManager() has returned.
    final CountDownLatch release = new CountDownLatch(1);
    final InputStream blockingStream = new InputStream() {
      private boolean eof = false;

      @Override
      public int read() {
        if (eof) {
          return -1;
        }
        try {
          release.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        eof = true;
        return -1;
      }
    };

    final ODataStreamUpdateRequestImpl request = new ODataStreamUpdateRequestImpl(
        client, HttpMethod.PUT, URI.create("http://localhost:1/svc/entity/$value"), blockingStream);

    final AtomicBoolean returned = new AtomicBoolean(false);
    final Thread worker = new Thread(() -> {
      request.payloadManager();
      returned.set(true);
    });
    worker.setDaemon(true);

    try {
      worker.start();
      worker.join(5000);
      assertTrue(returned.get(), "payloadManager() blocked on the calling thread when chunking was disabled");
    } finally {
      // Let the (executor-side) buffering read finish so no thread is left blocked.
      release.countDown();
      worker.join(5000);
    }
  }
}
