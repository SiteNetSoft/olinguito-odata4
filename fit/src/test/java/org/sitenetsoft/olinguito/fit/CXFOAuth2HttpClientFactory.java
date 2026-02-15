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
 * Copyright 2026 SiteNetSoft - Migrate from deprecated DefaultHttpClient to HttpClientBuilder;
 * replaced commons-codec Base64 with java.util.Base64
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Removed commons-lang3 dependency
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 */
package org.sitenetsoft.olinguito.fit;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.MediaType;

import java.util.Base64;
import org.sitenetsoft.olinguito.fit.utils.StringHelper;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.grants.refresh.RefreshTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.core.http.AbstractOAuth2HttpClientFactory;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpClient;
import org.sitenetsoft.olinguito.client.core.http.OAuth2Exception;
import org.sitenetsoft.olinguito.fit.rest.OAuth2Provider;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class CXFOAuth2HttpClientFactory extends AbstractOAuth2HttpClientFactory {

  private static final Consumer OAUTH2_CONSUMER =
          new Consumer(OAuth2Provider.CLIENT_ID, OAuth2Provider.CLIENT_SECRET);

  private ClientAccessToken accessToken;

  public CXFOAuth2HttpClientFactory(final URI oauth2GrantServiceURI, final URI oauth2TokenServiceURI) {
    super(oauth2GrantServiceURI, oauth2TokenServiceURI);
  }

  private WebClient getAccessTokenService() {
    final JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setAddress(oauth2TokenServiceURI.toASCIIString());
    bean.setUsername("odatajclient");
    bean.setPassword("odatajclient");
    return bean.createWebClient().
        type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.APPLICATION_JSON_TYPE);
  }

  @Override
  protected boolean isInited() throws OAuth2Exception {
    return accessToken != null;
  }

  @Override
  protected void init() throws OAuth2Exception {
    final URI authURI = OAuthClientUtils.getAuthorizationURI(
        oauth2GrantServiceURI.toASCIIString(),
        OAuth2Provider.CLIENT_ID,
        OAuth2Provider.REDIRECT_URI,
        null,
        "foo bar");

    // Disable automatic redirects handling
    final RequestConfig config = RequestConfig.custom()
            .setRedirectsEnabled(false)
            .build();
    final var httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(config)
            .build();

    JsonNode oAuthAuthorizationData;
    String authenticityCookie;
    try {
      // 1. Need to (basic) authenticate against the OAuth2 service
      final HttpGet method = new HttpGet(authURI);
      method.addHeader("Authorization", "Basic "
          + Base64.getEncoder().encodeToString("odatajclient:odatajclient".getBytes()));
      final ClassicHttpResponse response = httpClient.execute(method);

      // 2. Pull out OAuth2 authorization data and "authenticity" cookie (CXF specific)
      oAuthAuthorizationData = new XmlMapper().readTree(EntityUtils.toString(response.getEntity()));

      final Header setCookieHeader = response.getFirstHeader("Set-Cookie");
      if (setCookieHeader == null) {
        throw new IllegalStateException("OAuth flow is broken");
      }
      authenticityCookie = setCookieHeader.getValue();
    } catch (Exception e) {
      throw new OAuth2Exception(e);
    }

    String code;
    try {
      // 3. Submit the HTTP form for allowing access to the application
      final URI location = new URIBuilder(oAuthAuthorizationData.get("replyTo").asText()).
          addParameter("session_authenticity_token", oAuthAuthorizationData.get("authenticityToken").asText()).
          addParameter("client_id", oAuthAuthorizationData.get("clientId").asText()).
          addParameter("redirect_uri", oAuthAuthorizationData.get("redirectUri").asText()).
          addParameter("oauthDecision", "allow").
          addParameter("scope", "foo bar").
          build();
      final HttpGet method = new HttpGet(location);
      method.addHeader("Authorization", "Basic "
          + Base64.getEncoder().encodeToString("odatajclient:odatajclient".getBytes()));
      method.addHeader("Cookie", authenticityCookie);

      final ClassicHttpResponse response = httpClient.execute(method);

      final Header locationHeader = response.getFirstHeader("Location");
      if (response.getCode() != 303 || locationHeader == null) {
        throw new IllegalStateException("OAuth flow is broken");
      }

      // 4. Get the authorization code value out of this last redirect
      code = StringHelper.substringAfterLast(locationHeader.getValue(), "=");

      EntityUtils.consume(response.getEntity());
    } catch (Exception e) {
      throw new OAuth2Exception(e);
    }

    // 5. Obtain the access token
    try {
      accessToken = OAuthClientUtils.getAccessToken(
          getAccessTokenService(), OAUTH2_CONSUMER, new AuthorizationCodeGrant(code));
    } catch (OAuthServiceException e) {
      throw new OAuth2Exception(e);
    }

    if (accessToken == null) {
      throw new OAuth2Exception("No OAuth2 access token");
    }
  }

  @Override
  protected void accessToken(final HttpClient client) throws OAuth2Exception {
    // Token header is added via the builder interceptor — no action needed on already-built client
  }

  @Override
  protected void refreshToken(final HttpClient client) throws OAuth2Exception {
    final String refreshToken = accessToken.getRefreshToken();
    if (refreshToken == null) {
      throw new OAuth2Exception("No OAuth2 refresh token");
    }

    // refresh the token
    try {
      accessToken = OAuthClientUtils.getAccessToken(
          getAccessTokenService(), OAUTH2_CONSUMER, new RefreshTokenGrant(refreshToken));
    } catch (OAuthServiceException e) {
      throw new OAuth2Exception(e);
    }
  }

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    if (!isInited()) {
      init();
    }

    final AtomicReference<HttpClient> clientRef = new AtomicReference<>();

    final HttpClientBuilder builder = createWrappedBuilder(method, uri);

    // Add the OAuth2 Authorization header
    builder.addRequestInterceptorFirst((request, entity, context) -> {
      request.removeHeaders(HttpHeaders.AUTHORIZATION);
      request.addHeader(HttpHeaders.AUTHORIZATION, OAuthClientUtils.createAuthorizationHeader(accessToken));
    });

    // Track current request for retry
    builder.addRequestInterceptorLast((request, entity, context) -> {
      if (request instanceof HttpUriRequestBase) {
        currentRequest = (HttpUriRequestBase) request;
      } else {
        currentRequest = null;
      }
    });

    // Handle 401 by refreshing the token
    builder.addResponseInterceptorLast((response, entity, context) -> {
      if (response.getCode() == HttpStatus.SC_UNAUTHORIZED) {
        refreshToken(clientRef.get());
        if (currentRequest != null) {
          clientRef.get().execute(currentRequest);
        }
      }
    });

    final HttpClient httpClient = builder.build();
    clientRef.set(httpClient);
    return new ApacheHttpClient(httpClient);
  }

}
