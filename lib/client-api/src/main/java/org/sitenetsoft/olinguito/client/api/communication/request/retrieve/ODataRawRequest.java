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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1507: allow suppressing the auto-injected Accept header
 */
package org.sitenetsoft.olinguito.client.api.communication.request.retrieve;

import org.sitenetsoft.olinguito.client.api.communication.request.ODataRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRawResponse;

/**
 * This interface represents a generic OData request.
 */
public interface ODataRawRequest extends ODataRequest {

  /**
   * Sets accepted format.
   *
   * @param format format.
   */
  void setFormat(final String format);

  /**
   * Suppresses the {@code Accept} header so the request is sent without one.
   *
   * <p>By default a request without an explicit {@code Accept} header has one injected from the
   * default format. Call this to send a truly raw request (for example to let the server choose
   * the representation, or to talk to a server that behaves differently when {@code Accept} is
   * present). A subsequent {@link #setAccept} or {@link #setFormat} re-enables the header.</p>
   */
  void removeAccept();

  /**
   * Executes the query.
   *
   * @return query response.
   */
  ODataRawResponse execute();
}
