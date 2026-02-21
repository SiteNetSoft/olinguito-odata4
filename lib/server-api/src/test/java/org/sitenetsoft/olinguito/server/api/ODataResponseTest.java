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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Enhanced addHeader test for multi-value verification
 */
package org.sitenetsoft.olinguito.server.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ODataResponseTest {

  @Test
  void testResponse() {
    ODataResponse  r = new ODataResponse ();
    assertNotNull(r);
    r.addHeader("header", "value1");
    r.addHeader("header", "value2");
    List<String> header = r.getAllHeaders().get("header");
    assertNotNull(header);
    assertEquals(2, header.size());
    assertEquals("value1", header.get(0));
    assertEquals("value2", header.get(1));
    List<String> list = new ArrayList<>();
    r.addHeader("headerList", list );
    assertNotNull(r.getAllHeaders());
  }
  
  @Test
  void testError() {
    ODataServerError  r = new ODataServerError ();
    assertNotNull(r);
    assertNull(r.getLocale());
    Map<String, String> map = new HashMap<>();
    r.setInnerError(map);
    assertNotNull(r.getInnerError());
  }
}
