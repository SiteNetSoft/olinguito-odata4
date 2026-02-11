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
 * Copyright 2026 SiteNetSoft - New abstraction to decouple client-api from Apache HttpComponents
 */
package org.sitenetsoft.olinguito.client.api.http;

import java.net.URI;

/**
 * Transport-agnostic representation of an HTTP request.
 * <p>
 * Provides the minimal surface needed by API consumers.
 * Bridge implementations in {@code client-core} unwrap to the native request type.
 */
public interface ODataHttpRequest {

  /**
   * Returns the target URI of this request.
   *
   * @return request URI
   */
  URI getURI();
}
