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
 * Copyright 2026 SiteNetSoft - Added transport-agnostic request manipulation methods
 */
package org.sitenetsoft.olinguito.client.api.http;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Transport-agnostic representation of an HTTP request.
 * <p>
 * Provides the surface needed by API consumers to build and manipulate HTTP requests
 * without depending on any specific HTTP client library.
 * Bridge implementations in {@code client-core} unwrap to the native request type.
 */
public interface ODataHttpRequest {

  /**
   * Returns the target URI of this request.
   *
   * @return request URI
   */
  URI getURI();

  /**
   * Sets the target URI of this request.
   *
   * @param uri the new target URI
   */
  default void setURI(URI uri) {
    throw new UnsupportedOperationException("setURI() not implemented by " + getClass().getName());
  }

  /**
   * Returns the HTTP method of this request.
   *
   * @return the HTTP method
   */
  default HttpMethod getMethod() {
    throw new UnsupportedOperationException("getMethod() not implemented by " + getClass().getName());
  }

  /**
   * Adds a header to this request.
   *
   * @param name  header name
   * @param value header value
   */
  default void addHeader(String name, String value) {
    throw new UnsupportedOperationException("addHeader() not implemented by " + getClass().getName());
  }

  /**
   * Returns all headers set on this request as a name-to-value map.
   * If multiple values exist for a header, only the last value is returned.
   *
   * @return unmodifiable map of header names to values
   */
  default Map<String, String> getAllHeaders() {
    throw new UnsupportedOperationException("getAllHeaders() not implemented by " + getClass().getName());
  }

  /**
   * Returns whether this request's HTTP method supports a request body
   * (e.g. POST, PUT, PATCH, MERGE do; GET, DELETE, HEAD do not).
   *
   * @return {@code true} if the method supports a body entity
   */
  default boolean supportsEntity() {
    return false;
  }

  /**
   * Sets the request entity body from raw bytes (repeatable, known length).
   *
   * @param body    the body bytes
   * @param chunked whether to use chunked transfer encoding
   */
  default void setEntity(byte[] body, boolean chunked) {
    throw new UnsupportedOperationException("setEntity(byte[]) not implemented by " + getClass().getName());
  }

  /**
   * Sets the request entity body from a stream (potentially non-repeatable).
   *
   * @param body          the body stream
   * @param contentLength content length, or -1 if unknown
   * @param chunked       whether to use chunked transfer encoding
   */
  default void setEntity(InputStream body, long contentLength, boolean chunked) {
    throw new UnsupportedOperationException("setEntity(InputStream) not implemented by " + getClass().getName());
  }

  /**
   * Aborts this request. After calling this method, the request should not be reused.
   */
  default void abort() {
    throw new UnsupportedOperationException("abort() not implemented by " + getClass().getName());
  }
}
