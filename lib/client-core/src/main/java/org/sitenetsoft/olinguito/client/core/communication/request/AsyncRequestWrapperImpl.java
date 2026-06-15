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
 * Copyright 2026 SiteNetSoft - Code quality improvements and fixed interrupt handling
 * Copyright 2026 SiteNetSoft - Replaced deprecated DecompressingHttpClient
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Refactored to use transport-agnostic HTTP interfaces
 * Copyright 2026 SiteNetSoft - Fixed connection leak: close async monitor responses to release HC 5.x connections
 * Copyright 2026 SiteNetSoft - OLINGO-1476: Resolve relative Location URIs against original request URI
 * Copyright 2026 SiteNetSoft - OLINGO-1475: Honor chunked-encoding configuration for async request payloads
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.communication.header.ODataPreferences;
import org.sitenetsoft.olinguito.client.api.communication.request.AsyncRequestWrapper;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataDeleteRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.AsyncResponseWrapper;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataDeleteResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.http.CompressingODataHttpClient;
import org.sitenetsoft.olinguito.client.core.uri.URIUtils;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;

public class AsyncRequestWrapperImpl<R extends ODataResponse> extends AbstractRequest
    implements AsyncRequestWrapper<R> {

  protected static final int MAX_RETRY = 5;

  protected final ODataClient odataClient;

  /**
   * Request to be wrapped.
   */
  protected final ODataRequest odataRequest;

  /**
   * HTTP client.
   */
  protected final ODataHttpClient httpClient;

  /**
   * HTTP request.
   */
  protected final ODataHttpRequest request;

  /**
   * Target URI.
   */
  protected final URI uri;

  protected AsyncRequestWrapperImpl(final ODataClient odataClient, final ODataRequest odataRequest) {
    this.odataRequest = odataRequest;
    this.odataRequest.setAccept(this.odataRequest.getAccept());
    this.odataRequest.setContentType(this.odataRequest.getContentType());

    extendHeader(HttpHeader.PREFER, new ODataPreferences().respondAsync());

    this.odataClient = odataClient;
    final HttpMethod method = odataRequest.getMethod();

    // target uri
    this.uri = odataRequest.getURI();
    Objects.requireNonNull(this.uri, "Target URI can't be null");

    ODataHttpClient _httpClient = odataClient.getConfiguration().getHttpClientFactory().create(method, this.uri);
    if (odataClient.getConfiguration().isGzipCompression()) {
      _httpClient = new CompressingODataHttpClient(_httpClient);
    }
    this.httpClient = _httpClient;

    this.request = odataClient.getConfiguration().getHttpUriRequestFactory().create(method, this.uri);

    if (this.request.supportsEntity() && odataRequest instanceof AbstractODataBasicRequest<?> br) {
      // OLINGO-1475: async requests must honor the chunked-encoding configuration instead of
      // always streaming chunked. Mirror the synchronous path (AbstractODataRequest.setRequestEntity):
      // buffer the payload when chunking is disabled (or the request must be repeatable), otherwise
      // stream it with chunked transfer encoding.
      final InputStream payload = br.getPayload();
      final byte[] bytes = URIUtils.readInputStreamBytes(odataClient, payload);
      if (bytes != null) {
        final boolean useChunked = odataClient.getConfiguration().isUseChuncked();
        this.request.setEntity(bytes, useChunked && bytes.length >= 0);
      } else {
        this.request.setEntity(payload, -1, true);
      }
    }
  }

  @Override
  public final AsyncRequestWrapper<R> wait(final int waitInSeconds) {
    extendHeader(HttpHeader.PREFER, new ODataPreferences().wait(waitInSeconds));
    return this;
  }

  @Override
  public final AsyncRequestWrapper<R> callback(URI url) {
    extendHeader(HttpHeader.PREFER, new ODataPreferences().callback(url.toASCIIString()));
    return this;
  }

  protected final void extendHeader(final String headerName, final String headerValue) {
    final StringBuilder extended = new StringBuilder();
    if (this.odataRequest.getHeaderNames().contains(headerName)) {
      extended.append(this.odataRequest.getHeader(headerName)).append(", ");
    }

    this.odataRequest.addCustomHeader(headerName, extended.append(headerValue).toString());
  }

  @Override
  public AsyncResponseWrapper<R> execute() {
    return new AsyncResponseWrapperImpl(doExecute());
  }

  protected ODataHttpResponse doExecute() {
    // Add all available headers
    for (String key : odataRequest.getHeaderNames()) {
      final String value = odataRequest.getHeader(key);
      request.addHeader(key, value);
      LOG.debug("HTTP header being sent {}: {}", key, value);
    }

    return executeHttpRequest(httpClient, this.request);
  }

  private URI checkLocation(URI uri) {
    if (!uri.isAbsolute()) {
      uri = this.uri.resolve(uri);
    }
    if (!this.uri.getScheme().equals(uri.getScheme())) {
      throw new AsyncRequestException("Unexpected scheme in the Location header");
    }
    if (!this.uri.getHost().equals(uri.getHost())) {
      throw new AsyncRequestException("Unexpected host name in the Location header");
    }
    if (this.uri.getPort() != uri.getPort()) {
      throw new AsyncRequestException("Unexpected port in the Location header");
    }
    return uri;
  }

  public class AsyncResponseWrapperImpl implements AsyncResponseWrapper<R> {

    static final int DEFAULT_RETRY_AFTER = 5;
    static final int MAX_RETRY_AFTER = 10;

    protected URI location = null;

    protected R response = null;

    protected int retryAfter = DEFAULT_RETRY_AFTER;

    protected boolean preferenceApplied = false;

    public AsyncResponseWrapperImpl() {}

    /**
     * Constructor.
     *
     * @param res HTTP response.
     */
    @SuppressWarnings("unchecked")
    public AsyncResponseWrapperImpl(final ODataHttpResponse res) {
      if (res.getStatusCode() == 202) {
        retrieveMonitorDetails(res);
      } else {
        response = (R) ((AbstractODataRequest) odataRequest).getResponseTemplate().initFromHttpResponse(res);
      }
    }

    @Override
    public boolean isPreferenceApplied() {
      return preferenceApplied;
    }

    @Override
    public boolean isDone() {
      if (response == null) {
        // check to the monitor URL
        final ODataHttpResponse res = checkMonitor(location);

        if (res.getStatusCode() == 202) {
          retrieveMonitorDetails(res);
        } else {
          response = instantiateResponse(res);
        }
      }

      return response != null;
    }

    @Override
    public R getODataResponse() {
      ODataHttpResponse res = null;
      for (int i = 0; response == null && i < MAX_RETRY; i++) {
        res = checkMonitor(location);

        if (res.getStatusCode() == HttpStatusCode.ACCEPTED.getStatusCode()) {

          final Collection<String> retryHeaders = res.getHeader(HttpHeader.RETRY_AFTER);
          if (retryHeaders != null && !retryHeaders.isEmpty()) {
            this.retryAfter = parseReplyAfter(retryHeaders.iterator().next());
          }

          // Close the 202 response to release the connection back to the pool
          try {
            res.close();
          } catch (IOException ioe) {
            LOG.warn("Error closing async monitor response", ioe);
          }

          try {
            // wait for retry-after
            Thread.sleep((long) retryAfter * 1000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }

        } else {
          location = null;
          return instantiateResponse(res);
        }
      }

      if (response == null) {
        throw new ODataClientErrorException(
            res != null ? res.getStatusCode() : 0,
            res != null ? res.getReasonPhrase() : "Unknown");
      }

      return response;
    }

    URI createLocation(String string) {
      return checkLocation(URI.create(string));
    }

    int parseReplyAfter(String value) {
      if (value == null || value.isEmpty()) {
        return DEFAULT_RETRY_AFTER;
      }
      try {
        int n = Integer.parseInt(value);
        if (n < 0) {
          return DEFAULT_RETRY_AFTER;
        }
        return Math.min(n, MAX_RETRY_AFTER);
      } catch (NumberFormatException e) {
        return DEFAULT_RETRY_AFTER;
      }
    }

    @Override
    public ODataDeleteResponse delete() {
      final ODataDeleteRequest deleteRequest = odataClient.getCUDRequestFactory().getDeleteRequest(location);
      return deleteRequest.execute();
    }

    @Override
    public AsyncResponseWrapper<ODataDeleteResponse> asyncDelete() {
      return odataClient.getAsyncRequestFactory().<ODataDeleteResponse> getAsyncRequestWrapper(
          odataClient.getCUDRequestFactory().getDeleteRequest(location)).execute();
    }

    @Override
    public AsyncResponseWrapper<R> forceNextMonitorCheck(final URI uri) {
      this.location = uri;
      this.response = null;
      return this;
    }

    @SuppressWarnings("unchecked")
    private R instantiateResponse(final ODataHttpResponse res) {
      R odataResponse;
      try {
        odataResponse = (R) ((AbstractODataRequest) odataRequest).getResponseTemplate().initFromEnclosedPart(res
            .getBody());
      } catch (Exception e) {
        LOG.error("Error instantiating odata response", e);
        odataResponse = null;
      } finally {
        try {
          res.close();
        } catch (IOException ioe) {
          LOG.warn("Error closing response", ioe);
        }
      }
      return odataResponse;
    }

    private void retrieveMonitorDetails(final ODataHttpResponse res) {
      Collection<String> locationHeaders = res.getHeader(HttpHeader.LOCATION);
      if (locationHeaders != null && !locationHeaders.isEmpty()) {
        this.location = createLocation(locationHeaders.iterator().next());
      } else {
        throw new AsyncRequestException(
            "Invalid async request response. Monitor URL not found in Location header");
      }

      Collection<String> retryHeaders = res.getHeader(HttpHeader.RETRY_AFTER);
      if (retryHeaders != null && !retryHeaders.isEmpty()) {
        this.retryAfter = parseReplyAfter(retryHeaders.iterator().next());
      }

      Collection<String> prefHeaders = res.getHeader(HttpHeader.PREFERENCE_APPLIED);
      if (prefHeaders != null) {
        for (String value : prefHeaders) {
          if (value.equalsIgnoreCase(new ODataPreferences().respondAsync())) {
            preferenceApplied = true;
          }
        }
      }
      // Close the response to release the underlying connection back to the pool
      try {
        res.close();
      } catch (IOException ioe) {
        LOG.warn("Error closing async monitor response", ioe);
      }
    }
  }

  protected final ODataHttpResponse checkMonitor(final URI location) {
    if (location == null) {
      throw new AsyncRequestException("Invalid async request response. Missing monitor URL");
    }

    final ODataHttpRequest monitor = odataClient.getConfiguration().getHttpUriRequestFactory().create(HttpMethod.GET,
        location);

    return executeHttpRequest(httpClient, monitor);
  }

  protected final ODataHttpResponse executeHttpRequest(final ODataHttpClient client, final ODataHttpRequest req) {
    ODataHttpResponse response;
    try {
      response = client.execute(req);
    } catch (RuntimeException e) {
      req.abort();
      throw new HttpClientException(e);
    }

    checkResponse(odataClient, response, odataRequest.getAccept());

    return response;
  }
}
