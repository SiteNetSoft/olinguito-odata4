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
 * Copyright 2026 SiteNetSoft - Added the asynchronous-processing service provider interface
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataHandler;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.OlingoExtension;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.async.AsyncInvocation;
import org.sitenetsoft.olinguito.server.api.async.AsyncResult;
import org.sitenetsoft.olinguito.server.api.async.AsyncSupport;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;

class AsyncSupportRegistrationTest {

  @Test
  void anAsyncSupportExtensionIsAccepted() {
    final OData odata = OData.newInstance();
    final ServiceMetadata serviceMetadata =
        odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final ODataHandler handler = odata.createRawHandler(serviceMetadata);
    handler.register(new RecordingAsyncSupport());
  }

  @Test
  void anUnknownExtensionIsStillRejected() {
    final OData odata = OData.newInstance();
    final ServiceMetadata serviceMetadata =
        odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final ODataHandler handler = odata.createRawHandler(serviceMetadata);
    assertThrows(ODataRuntimeException.class, () -> handler.register(new OlingoExtension() { }));
  }

  @Test
  void asyncResultStates() {
    assertEquals(AsyncResult.State.RUNNING, AsyncResult.running().getState());
    assertNull(AsyncResult.running().getResponse());
    assertEquals(AsyncResult.State.NOT_FOUND, AsyncResult.notFound().getState());
    final ODataResponse response = new ODataResponse();
    assertSame(response, AsyncResult.completed(response).getResponse());
    assertEquals(AsyncResult.State.COMPLETED, AsyncResult.completed(response).getState());
    assertThrows(IllegalArgumentException.class, () -> AsyncResult.completed(null));
  }

  @Test
  void cancelAndRetryAfterDefaultToUnsupportedAndAbsent() {
    final AsyncSupport minimal = new AsyncSupport() {
      @Override
      public boolean isStatusMonitorRequest(final ODataRequest request) {
        return false;
      }

      @Override
      public String submit(final ODataRequest request, final AsyncInvocation invocation) {
        return "loc";
      }

      @Override
      public AsyncResult resolve(final ODataRequest request) {
        return AsyncResult.notFound();
      }
    };
    assertFalse(minimal.cancel(new ODataRequest()));
    assertNull(minimal.getRetryAfter());
  }

  /**
   * Test double reused verbatim (as a self-contained copy) by later Tier 6 Wave 3 tasks: records
   * the requests it was handed and the invocation it was given, with settable {@link AsyncResult}
   * answers.
   */
  static class RecordingAsyncSupport implements AsyncSupport {

    final List<ODataRequest> statusMonitorChecks = new ArrayList<>();
    final List<ODataRequest> submittedRequests = new ArrayList<>();
    AsyncInvocation submittedInvocation;
    String submitReturnValue = "https://example.org/status-monitor/1";
    AsyncResult resolveReturnValue = AsyncResult.running();

    @Override
    public boolean isStatusMonitorRequest(final ODataRequest request) {
      statusMonitorChecks.add(request);
      return false;
    }

    @Override
    public String submit(final ODataRequest request, final AsyncInvocation invocation) {
      submittedRequests.add(request);
      submittedInvocation = invocation;
      return submitReturnValue;
    }

    @Override
    public AsyncResult resolve(final ODataRequest request) {
      return resolveReturnValue;
    }
  }
}
