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
 * Copyright 2026 SiteNetSoft - Migrate from deprecated DefaultHttpClient to HttpClientBuilder
 * Copyright 2026 SiteNetSoft - Return ODataHttpClient wrapping Apache HttpClient
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 * Copyright 2026 SiteNetSoft - Fixed deprecated HC 5.x execute() calls
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.WrappingHttpClientFactory;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

public abstract class AbstractOAuth2HttpClientFactory
        extends AbstractHttpClientFactory implements WrappingHttpClientFactory {

  protected final DefaultHttpClientFactory wrapped;

  protected final URI oauth2GrantServiceURI;

  protected final URI oauth2TokenServiceURI;

  protected HttpUriRequestBase currentRequest;

  public AbstractOAuth2HttpClientFactory(final URI oauth2GrantServiceURI, final URI oauth2TokenServiceURI) {
    this(new DefaultHttpClientFactory(), oauth2GrantServiceURI, oauth2TokenServiceURI);
  }

  public AbstractOAuth2HttpClientFactory(final DefaultHttpClientFactory wrapped,
          final URI oauth2GrantServiceURI, final URI oauth2TokenServiceURI) {

    super();
    this.wrapped = wrapped;
    this.oauth2GrantServiceURI = oauth2GrantServiceURI;
    this.oauth2TokenServiceURI = oauth2TokenServiceURI;
  }

  @Override
  public HttpClientFactory getWrappedHttpClientFactory() {
    return wrapped;
  }

  protected HttpClientBuilder createWrappedBuilder(final HttpMethod method, final URI uri) {
    return wrapped.createBuilder(method, uri);
  }

  protected abstract boolean isInited() throws OAuth2Exception;

  protected abstract void init() throws OAuth2Exception;

  protected abstract void accessToken(HttpClient client) throws OAuth2Exception;

  protected abstract void refreshToken(HttpClient client) throws OAuth2Exception;

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    if (!isInited()) {
      init();
    }

    final AtomicReference<HttpClient> clientRef = new AtomicReference<>();

    final HttpClientBuilder builder = wrapped.createBuilder(method, uri);
    builder.addRequestInterceptorLast((HttpRequest request, EntityDetails entity, HttpContext context) -> {
      if (request instanceof HttpUriRequestBase uriRequest) {
        currentRequest = uriRequest;
      } else {
        currentRequest = null;
      }
    });
    builder.addResponseInterceptorLast((HttpResponse response, EntityDetails entity, HttpContext context) -> {
      if (response.getCode() == HttpStatus.SC_UNAUTHORIZED) {
        refreshToken(clientRef.get());
        if (currentRequest != null) {
          clientRef.get().execute(currentRequest, r -> null);
        }
      }
    });

    final HttpClient httpClient = builder.build();
    clientRef.set(httpClient);
    accessToken(httpClient);
    return new ApacheHttpClient(httpClient);
  }

  @Override
  public void close(final ODataHttpClient httpClient) {
    wrapped.close(httpClient);
  }

}
