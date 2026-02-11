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

import java.io.Closeable;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Transport-agnostic representation of an HTTP response.
 * <p>
 * Provides status code, reason phrase, headers, and body access without
 * depending on any specific HTTP client library.
 */
public interface ODataHttpResponse extends Closeable {

  /**
   * Returns the HTTP status code.
   *
   * @return status code (e.g. 200, 404)
   */
  int getStatusCode();

  /**
   * Returns the HTTP reason phrase.
   *
   * @return reason phrase (e.g. "OK", "Not Found"), or {@code null}
   */
  String getReasonPhrase();

  /**
   * Returns all response headers as a multi-valued map.
   * <p>
   * Header names are case-insensitive keys; each key maps to one or more values.
   *
   * @return unmodifiable header map
   */
  Map<String, Collection<String>> getHeaders();

  /**
   * Returns the values for a specific response header.
   *
   * @param name header name (case-insensitive)
   * @return header values, or {@code null} if not present
   */
  Collection<String> getHeader(String name);

  /**
   * Returns the response body as an input stream.
   *
   * @return body stream, or {@code null} if no body
   */
  InputStream getBody();

  /**
   * Returns the content type of the response body.
   *
   * @return content type string, or {@code null} if not available
   */
  String getContentType();
}
