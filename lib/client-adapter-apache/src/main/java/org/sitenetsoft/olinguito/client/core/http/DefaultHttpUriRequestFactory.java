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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 * Copyright 2026 SiteNetSoft - Return ODataHttpRequest wrapping Apache HttpUriRequest
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.net.URI;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.sitenetsoft.olinguito.client.api.http.HttpUriRequestFactory;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Default implementation returning default HttpUriRequest implementations.
 */
public class DefaultHttpUriRequestFactory implements HttpUriRequestFactory {

  @Override
  public ODataHttpRequest create(final HttpMethod method, final URI uri) {
    final HttpUriRequestBase request = switch (method) {
        case POST -> new HttpPost(uri);
        case PUT -> new HttpPut(uri);
        case PATCH -> new HttpPatch(uri);
        case MERGE -> new HttpMerge(uri);
        case DELETE -> new HttpDelete(uri);
        default -> new HttpGet(uri);
    };
    return new ApacheHttpRequest(request);
  }
}
