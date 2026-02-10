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
 * Copyright 2026 SiteNetSoft - Migrated XMLUnit 1.6 to 2.11.0
 */
package org.sitenetsoft.olinguito.client.core;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

public abstract class AbstractTest {

  protected static final ODataClient client = ODataClientFactory.getClient();

  protected String getSuffix(final ContentType contentType) {
    return contentType.isCompatible(ContentType.APPLICATION_ATOM_SVC)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML)
        || contentType.isCompatible(ContentType.APPLICATION_XML) ? "xml": "json";
  }
}
