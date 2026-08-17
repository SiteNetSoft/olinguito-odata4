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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 3: second tecsvc deployment serving the OData
 * 4.01 key-as-segment URL convention (Part 2: URL Conventions, section 4.3.6)
 */
package org.sitenetsoft.olinguito.server.tecsvc;

import java.io.Serial;

import org.sitenetsoft.olinguito.server.api.ODataHandler;

/**
 * Serves the very same technical service as {@link TechnicalServlet}, but with the OData 4.01
 * key-as-segment URL convention switched on for the whole service, so that entity keys may be given
 * as their own path segments (<tt>ESAllPrim/32767</tt>) in addition to the parenthesized form.
 */
public class TechnicalKeyAsSegmentServlet extends TechnicalServlet {

  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  protected void configureHandler(final ODataHandler handler) {
    handler.setKeyAsSegment(true);
  }
}
