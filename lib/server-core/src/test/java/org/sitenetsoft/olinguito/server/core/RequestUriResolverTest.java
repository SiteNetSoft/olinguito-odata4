/*
 * Copyright 2026 SiteNetSoft - Port OLINGO-1163/OLINGO-1422: rawODataPath hostname collision
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.junit.jupiter.api.Test;

/**
 * OLINGO-1163 / OLINGO-1422: the servlet/context path must not be matched inside the hostname
 * when computing the raw OData path.
 */
class RequestUriResolverTest {

  @Test
  void servletPathMatchingInsideHostnameDoesNotCorruptPath() {
    // servletPath "/odata" also occurs in host "odata.test.de"
    final ODataRequest req = new ODataRequest();
    RequestUriResolver.fillUriInformation(req,
        "http://odata.test.de/wf/odata/$metadata", "/wf/odata/$metadata", null,
        "/wf", "/odata", null, 0);

    assertEquals("/$metadata", req.getRawODataPath());
    assertEquals("http://odata.test.de/wf/odata", req.getRawBaseUri());
  }

  @Test
  void contextPathMatchingInsideHostnameDoesNotCorruptPath() {
    // contextPath "/wf" used (no servletPath); host contains no collision but exercise the branch
    final ODataRequest req = new ODataRequest();
    RequestUriResolver.fillUriInformation(req,
        "http://wf.example.com/wf/Entities", "/wf/Entities", null,
        "/wf", null, null, 0);

    assertEquals("/Entities", req.getRawODataPath());
    assertEquals("http://wf.example.com/wf", req.getRawBaseUri());
  }

  @Test
  void normalServletPathStillResolves() {
    final ODataRequest req = new ODataRequest();
    RequestUriResolver.fillUriInformation(req,
        "http://host.example.com/svc/Entities(1)", "/svc/Entities(1)", "$select=Name",
        "", "/svc", null, 0);

    assertEquals("/Entities(1)", req.getRawODataPath());
    assertEquals("http://host.example.com/svc", req.getRawBaseUri());
    assertEquals("$select=Name", req.getRawQueryPath());
  }

  @Test
  void requestMappingMatchingInsideHostnameDoesNotCorruptPath() {
    final ODataRequest req = new ODataRequest();
    RequestUriResolver.fillUriInformation(req,
        "http://odata.host.com/app/odata/Products", "/app/odata/Products", null,
        "/app", "/odata", "/app/odata", 0);

    assertEquals("/Products", req.getRawODataPath());
    assertEquals("/app/odata", req.getRawServiceResolutionUri());
  }
}
