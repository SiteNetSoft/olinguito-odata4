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
 * Copyright 2026 SiteNetSoft - Restored debugger tests using servlet-free String API
 */
package org.sitenetsoft.olinguito.server.core.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.debug.DebugInformation;
import org.sitenetsoft.olinguito.server.api.debug.DebugSupport;
import org.sitenetsoft.olinguito.server.api.debug.DefaultDebugSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerCoreDebuggerTest {

  private final OData odata = OData.newInstance();
  private ServerCoreDebugger debugger;

  @BeforeEach
  public void setupDebugger() {
    debugger = new ServerCoreDebugger(odata);
    DebugSupport processor = mock(DebugSupport.class);
    when(processor.isUserAuthorized()).thenReturn(true);
    when(processor.createDebugResponse(any(), any(DebugInformation.class)))
        .thenThrow(new ODataRuntimeException("Test"));
    debugger.setDebugSupportProcessor(processor);
  }

  @Test
  public void standardIsDebugModeIsFalse() {
    assertFalse(debugger.isDebugMode());
  }

  @Test
  public void resolveDebugModeNoDebugSupportProcessor() {
    ServerCoreDebugger localDebugger = new ServerCoreDebugger(odata);
    localDebugger.resolveDebugMode(DebugSupport.ODATA_DEBUG_JSON);
    assertFalse(localDebugger.isDebugMode());
  }

  @Test
  public void resolveDebugModeNullParameter() {
    debugger.resolveDebugMode((String) null);
    assertFalse(debugger.isDebugMode());
  }

  @Test
  public void resolveDebugModeJsonNotAuthorized() {
    DebugSupport debugSupportMock = mock(DebugSupport.class);
    when(debugSupportMock.isUserAuthorized()).thenReturn(false);

    ServerCoreDebugger localDebugger = new ServerCoreDebugger(odata);
    localDebugger.setDebugSupportProcessor(debugSupportMock);

    localDebugger.resolveDebugMode(DebugSupport.ODATA_DEBUG_JSON);
    assertFalse(localDebugger.isDebugMode());
  }

  @Test
  public void failResponse() throws Exception {
    debugger.resolveDebugMode(DebugSupport.ODATA_DEBUG_JSON);
    ODataResponse debugResponse = debugger.createDebugResponse(null, null, null, null, null);
    assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), debugResponse.getStatusCode());
    assertEquals("ODataLibrary: Could not assemble debug response.",
        new String(debugResponse.getContent().readAllBytes(), StandardCharsets.UTF_8));
  }

  @Test
  public void noDebugModeCreateDebugResponseCallMustDoNothing() {
    ODataResponse odResponse = new ODataResponse();
    ODataResponse debugResponse = debugger.createDebugResponse(null, odResponse, null, null, null);

    assertEquals(odResponse, debugResponse);
  }

  @Test
  public void runtimeMeasurement() throws Exception {
    ServerCoreDebugger defaultDebugger = new ServerCoreDebugger(odata);
    defaultDebugger.setDebugSupportProcessor(new DefaultDebugSupport());
    defaultDebugger.resolveDebugMode(DebugSupport.ODATA_DEBUG_JSON);

    final int handle = defaultDebugger.startRuntimeMeasurement("someClass", "someMethod");
    defaultDebugger.stopRuntimeMeasurement(handle);
    assertEquals(0, handle);

    assertThat(new String(defaultDebugger.createDebugResponse(null, null, null, null, null)
            .getContent().readAllBytes(), StandardCharsets.UTF_8),
        allOf(containsString("\"runtime\""), containsString("\"someClass\""), containsString("\"someMethod\""),
            containsString("]}}")));

    defaultDebugger.resolveDebugMode(DebugSupport.ODATA_DEBUG_HTML);
    assertThat(new String(defaultDebugger.createDebugResponse(null, null, null, null, null)
            .getContent().readAllBytes(), StandardCharsets.UTF_8),
        allOf(containsString(">Runtime<"), containsString(">someClass<"), containsString(">someMethod("),
            containsString("</html>")));
  }

}
