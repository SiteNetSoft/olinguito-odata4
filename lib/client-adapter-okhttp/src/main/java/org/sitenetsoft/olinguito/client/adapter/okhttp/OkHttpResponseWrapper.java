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
 * Copyright 2026 SiteNetSoft - OkHttp adapter for ODataHttpResponse
 * Copyright 2026 SiteNetSoft - Wrap body stream to close OkHttp response on stream close
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * {@link ODataHttpResponse} implementation backed by an OkHttp {@link Response}.
 * <p>
 * The body stream is wrapped so that closing it also closes the underlying
 * OkHttp response, ensuring the connection is properly returned to the pool.
 */
public class OkHttpResponseWrapper implements ODataHttpResponse {

  private final Response response;

  public OkHttpResponseWrapper(final Response response) {
    this.response = response;
  }

  @Override
  public int getStatusCode() {
    return response.code();
  }

  @Override
  public String getReasonPhrase() {
    final String message = response.message();
    return message.isEmpty() ? null : message;
  }

  @Override
  public Map<String, Collection<String>> getHeaders() {
    final Map<String, Collection<String>> result =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (String name : response.headers().names()) {
      result.put(name, new ArrayList<>(response.headers().values(name)));
    }
    return result;
  }

  @Override
  public Collection<String> getHeader(final String name) {
    final List<String> values = response.headers().values(name);
    return values.isEmpty() ? null : new ArrayList<>(values);
  }

  @Override
  public InputStream getBody() {
    final ResponseBody body = response.body();
    if (body == null) {
      return null;
    }
    // Wrap the stream so that closing it also closes the OkHttp response,
    // ensuring any unread data is discarded and the connection is returned
    // to the pool cleanly.
    return new FilterInputStream(body.byteStream()) {
      @Override
      public void close() throws IOException {
        try {
          super.close();
        } finally {
          response.close();
        }
      }
    };
  }

  @Override
  public String getContentType() {
    final ResponseBody body = response.body();
    if (body == null || body.contentType() == null) {
      return null;
    }
    return body.contentType().toString();
  }

  @Override
  public void close() throws IOException {
    response.close();
  }
}
