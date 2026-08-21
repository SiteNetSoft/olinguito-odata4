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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 10: coverage for the AsyncSupport implementation
 * of the technical service ([OData-Protocol] section 11.6)
 */
package org.sitenetsoft.olinguito.server.tecsvc.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.async.AsyncResult;

class TechnicalAsyncServiceTest {

  private static final String BASE_URI = "http://host/service/odata.svc";

  private final TechnicalAsyncService service = TechnicalAsyncService.getInstance();

  @Test
  void submitReturnsAMonitorLocationUnderTheServiceRoot() {
    final String first = service.submit(request(null), ODataResponse::new);
    final String second = service.submit(request(null), ODataResponse::new);

    assertTrue(first.matches("http://host/service/status/\\d+"), first);
    assertTrue(second.matches("http://host/service/status/\\d+"), second);
    assertNotEquals(first, second);
  }

  @Test
  void aMonitorRequestIsRecognizedByItsUrl() {
    final String location = service.submit(request(null), ODataResponse::new);

    assertTrue(service.isStatusMonitorRequest(monitorRequest(location)));
    assertFalse(service.isStatusMonitorRequest(monitorRequest(BASE_URI + "/ESAllPrim(1)")));
    // A substring test would misread an entity set whose name merely starts with "status".
    assertFalse(service.isStatusMonitorRequest(monitorRequest(BASE_URI + "/ESStatusReport")));
  }

  @Test
  void resolveReportsRunningThenCompletedThenNotFound() throws Exception {
    final CountDownLatch release = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(1);
    final ODataResponse invocationResponse = new ODataResponse();

    final String location = service.submit(request(null), () -> {
      try {
        release.await(10, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      done.countDown();
      return invocationResponse;
    });
    final ODataRequest monitor = monitorRequest(location);

    assertEquals(AsyncResult.State.RUNNING, service.resolve(monitor).getState());

    release.countDown();
    assertTrue(done.await(10, TimeUnit.SECONDS));
    final AsyncResult completed = awaitCompletion(monitor);
    assertEquals(AsyncResult.State.COMPLETED, completed.getState());
    assertSame(invocationResponse, completed.getResponse());

    // The result is retrieved exactly once: the monitor resource is gone afterwards.
    assertEquals(AsyncResult.State.NOT_FOUND, service.resolve(monitor).getState());
  }

  @Test
  void anUnknownMonitorLocationIsNotFound() {
    assertEquals(AsyncResult.State.NOT_FOUND,
        service.resolve(monitorRequest("http://host/service/status/does-not-exist")).getState());
  }

  @Test
  void theTecSleepPreferenceStillDelaysProcessing() throws Exception {
    final CountDownLatch invoked = new CountDownLatch(1);
    final String location = service.submit(
        request("respond-async, " + TechnicalAsyncService.TEC_ASYNC_SLEEP + "=1"),
        () -> {
          invoked.countDown();
          return new ODataResponse();
        });

    assertFalse(invoked.await(300, TimeUnit.MILLISECONDS), "the invocation must not run immediately");
    assertEquals(AsyncResult.State.RUNNING, service.resolve(monitorRequest(location)).getState());
    assertTrue(invoked.await(10, TimeUnit.SECONDS));
  }

  @Test
  void cancelIsNotSupported() {
    assertFalse(service.cancel(monitorRequest("http://host/service/status/1")));
  }

  private AsyncResult awaitCompletion(final ODataRequest monitor) throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      final AsyncResult result = service.resolve(monitor);
      if (result.getState() != AsyncResult.State.RUNNING) {
        return result;
      }
      TimeUnit.MILLISECONDS.sleep(50);
    }
    throw new AssertionError("The invocation did not complete in time.");
  }

  private static ODataRequest request(final String preferHeader) {
    final ODataRequest request = new ODataRequest();
    request.setRawBaseUri(BASE_URI);
    request.setRawRequestUri(BASE_URI + "/ESAllPrim");
    if (preferHeader != null) {
      request.addHeader(HttpHeader.PREFER, java.util.List.of(preferHeader));
    }
    return request;
  }

  private static ODataRequest monitorRequest(final String uri) {
    final ODataRequest request = new ODataRequest();
    request.setRawRequestUri(uri);
    return request;
  }
}
