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
 * Copyright 2026 SiteNetSoft - Replaced Apache StatusLine with framework-agnostic types
 * Copyright 2026 SiteNetSoft - OLINGO-1542: Preserve ODataError in server error responses
 */
package org.sitenetsoft.olinguito.client.api.communication;

import org.sitenetsoft.olinguito.commons.api.ex.ODataError;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;

import java.io.InputStream;
import java.io.Serial;

/**
 * Represents a server error in OData.
 */
public class ODataServerErrorException extends ODataRuntimeException {

  @Serial

  private static final long serialVersionUID = -6423014532618680135L;

  private final ODataError error;

  private final InputStream rawResponse;

  /**
   * Constructor.
   *
   * @param statusCode HTTP status code.
   * @param statusMessage HTTP reason phrase.
   */
  public ODataServerErrorException(final int statusCode, final String statusMessage) {
    this(statusCode, statusMessage, null, null);
  }

  /**
   * Constructor.
   *
   * @param statusCode HTTP status code.
   * @param statusMessage HTTP reason phrase.
   * @param rawResponse raw response of the request.
   */
  public ODataServerErrorException(final int statusCode, final String statusMessage,
      final InputStream rawResponse) {
    this(statusCode, statusMessage, null, rawResponse);
  }

  /**
   * Constructor.
   *
   * @param statusCode HTTP status code.
   * @param statusMessage HTTP reason phrase.
   * @param error OData error to be wrapped.
   * @param rawResponse raw response of the request.
   */
  public ODataServerErrorException(final int statusCode, final String statusMessage,
      final ODataError error, final InputStream rawResponse) {
    super(error == null
        ? "HTTP/" + statusCode + " " + statusMessage
        : (error.getCode() == null || error.getCode().isEmpty() ? "" : "(" + error.getCode() + ") ")
            + error.getMessage() + " [HTTP/" + statusCode + " " + statusMessage + "]");
    this.error = error;
    this.rawResponse = rawResponse;
  }

  /**
   * Gets OData error.
   *
   * @return OData error.
   */
  public ODataError getODataError() {
    return error;
  }

  /**
   * Return raw response from the request (can be null).
   *
   * @return raw response from the request (can be null).
   */
  public InputStream getRawResponse() {
    return rawResponse;
  }
}
