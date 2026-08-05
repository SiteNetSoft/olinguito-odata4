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
 * Copyright 2026 SiteNetSoft - Replaced Apache StatusLine/Header with framework-agnostic types
 */
package org.sitenetsoft.olinguito.client.api.communication;

import org.sitenetsoft.olinguito.commons.api.ex.ODataError;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;

import java.io.InputStream;
import java.io.Serial;
import java.util.Collection;
import java.util.Map;

/**
 * Represents a client error in OData.
 *
 * @see ODataError
 */
public class ODataClientErrorException extends ODataRuntimeException {

  @Serial

  private static final long serialVersionUID = -2551523202755268162L;

  private final int statusCode;

  private final String statusMessage;

  private final ODataError error;

  private final InputStream rawResponse;

  private Map<String, Collection<String>> headerInfo;

  /**
   * Constructor.
   *
   * @param statusCode HTTP status code.
   * @param statusMessage HTTP reason phrase.
   */
  public ODataClientErrorException(final int statusCode, final String statusMessage) {
    this(statusCode, statusMessage, null, null);
  }

  /**
   * Constructor.
   *
   * @param statusCode HTTP status code.
   * @param statusMessage HTTP reason phrase.
   * @param rawResponse raw response of the request.
   */
  public ODataClientErrorException(final int statusCode, final String statusMessage,
      final InputStream rawResponse) {
    this(statusCode, statusMessage, null, rawResponse);
  }

  /**
   * Constructor.
   *
   * @param statusCode HTTP status code.
   * @param statusMessage HTTP reason phrase.
   * @param error OData error to be wrapped.
   */
  public ODataClientErrorException(final int statusCode, final String statusMessage,
      final ODataError error) {
    this(statusCode, statusMessage, error, null);
  }

  /**
   * Constructor.
   *
   * @param statusCode HTTP status code.
   * @param statusMessage HTTP reason phrase.
   * @param error OData error to be wrapped.
   * @param rawResponse raw response of the request.
   */
  public ODataClientErrorException(final int statusCode, final String statusMessage,
      final ODataError error, final InputStream rawResponse) {
    super(error == null
        ? "HTTP/" + statusCode + " " + statusMessage
        : (error.getCode() == null || error.getCode().isEmpty() ? "" : "(" + error.getCode() + ") ")
            + error.getMessage() + " [HTTP/" + statusCode + " " + statusMessage + "]");

    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
    this.error = error;
    this.rawResponse = rawResponse;
  }

  /**
   * Gets the HTTP status code.
   *
   * @return HTTP status code.
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Gets the HTTP reason phrase.
   *
   * @return reason phrase.
   */
  public String getStatusMessage() {
    return statusMessage;
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
   * Sets response headers.
   *
   * @param headerInfo response headers as a multi-valued map.
   */
  public void setHeaderInfo(final Map<String, Collection<String>> headerInfo) {
    this.headerInfo = headerInfo;
  }

  /**
   * Returns response headers.
   *
   * @return response headers as a multi-valued map.
   */
  public Map<String, Collection<String>> getHeaderInfo() {
    return headerInfo;
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
