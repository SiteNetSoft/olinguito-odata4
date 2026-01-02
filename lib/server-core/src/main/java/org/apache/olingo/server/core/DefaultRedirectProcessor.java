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
 */
package org.sitenetsoft.olinguito.server.core;

import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;

public class DefaultRedirectProcessor implements RedirectProcessor {

  @Override
  public void init(final OData odata, final ServiceMetadata serviceMetadata) {
    // No init needed
  }

  @Override
  public void redirect(final ODataRequest request, final ODataResponse response) {
    response.setStatusCode(HttpStatusCode.TEMPORARY_REDIRECT.getStatusCode());

    String location;

    String rawUri = request.getRawRequestUri();
    String rawQueryPath = request.getRawQueryPath();
    if (rawQueryPath == null) {
      location = request.getRawRequestUri() + "/";
    } else {
      location = rawUri.substring(0, rawUri.indexOf(rawQueryPath) - 1) + "/?" + rawQueryPath;
    }

    response.setHeader(HttpHeader.LOCATION, location);
  }
}
