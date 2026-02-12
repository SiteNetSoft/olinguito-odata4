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
 * Copyright 2026 SiteNetSoft - Non-deprecated replacement for DecompressingHttpClient; added Closeable support
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * Non-deprecated replacement for {@code org.apache.http.impl.client.DecompressingHttpClient}.
 * Wraps an existing {@link HttpClient} to add transparent gzip/deflate
 * content compression and decompression via {@link RequestAcceptEncoding}
 * and {@link ResponseContentEncoding} interceptors.
 */
public final class ContentCompressingHttpClient implements HttpClient, java.io.Closeable {

  private final HttpClient delegate;
  private final RequestAcceptEncoding requestInterceptor = new RequestAcceptEncoding();
  private final ResponseContentEncoding responseInterceptor = new ResponseContentEncoding();

  public ContentCompressingHttpClient(final HttpClient delegate) {
    this.delegate = delegate;
  }

  @Deprecated
  @Override
  public org.apache.http.params.HttpParams getParams() {
    return delegate.getParams();
  }

  @Deprecated
  @Override
  public org.apache.http.conn.ClientConnectionManager getConnectionManager() {
    return delegate.getConnectionManager();
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request) throws IOException {
    return execute(extractHost(request), request, (HttpContext) null);
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request, final HttpContext context) throws IOException {
    return execute(extractHost(request), request, context);
  }

  @Override
  public HttpResponse execute(final HttpHost target, final HttpRequest request) throws IOException {
    return execute(target, request, (HttpContext) null);
  }

  @Override
  public HttpResponse execute(final HttpHost target, final HttpRequest request,
      final HttpContext context) throws IOException {
    try {
      final HttpContext localContext = context != null ? context : new BasicHttpContext();
      requestInterceptor.process(request, localContext);
      final HttpResponse response = delegate.execute(target, request, localContext);
      try {
        responseInterceptor.process(response, localContext);
        if (Boolean.TRUE.equals(localContext.getAttribute(ResponseContentEncoding.UNCOMPRESSED))) {
          response.removeHeaders("Content-Length");
          response.removeHeaders("Content-Encoding");
          response.removeHeaders("Content-MD5");
        }
        return response;
      } catch (final HttpException ex) {
        EntityUtils.consume(response.getEntity());
        throw ex;
      }
    } catch (final HttpException ex) {
      throw new ClientProtocolException(ex);
    }
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
      final ResponseHandler<? extends T> handler) throws IOException {
    return execute(extractHost(request), request, handler, null);
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
      final ResponseHandler<? extends T> handler, final HttpContext context) throws IOException {
    return execute(extractHost(request), request, handler, context);
  }

  @Override
  public <T> T execute(final HttpHost target, final HttpRequest request,
      final ResponseHandler<? extends T> handler) throws IOException {
    return execute(target, request, handler, null);
  }

  @Override
  public <T> T execute(final HttpHost target, final HttpRequest request,
      final ResponseHandler<? extends T> handler, final HttpContext context) throws IOException {
    final HttpResponse response = execute(target, request, context);
    try {
      return handler.handleResponse(response);
    } finally {
      EntityUtils.consume(response.getEntity());
    }
  }

  @Override
  public void close() throws IOException {
    if (delegate instanceof java.io.Closeable closeable) {
      closeable.close();
    }
  }

  private static HttpHost extractHost(final HttpUriRequest request) {
    final URI uri = request.getURI();
    return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
  }
}
