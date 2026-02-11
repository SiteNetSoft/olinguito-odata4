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
 * Copyright 2026 SiteNetSoft - Fixed deprecated API usages and code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced deprecated DecompressingHttpClient
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.BatchRequestFactory;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.CUDRequestFactory;
import org.sitenetsoft.olinguito.client.api.communication.request.invoke.InvokeRequestFactory;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.header.ODataHeaders;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpClient;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpRequest;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpResponse;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Abstract representation of an OData request. Get instance by using factories.
 *
 * @see CUDRequestFactory
 * @see BatchRequestFactory
 * @see InvokeRequestFactory
 */
public abstract class AbstractODataRequest extends AbstractRequest implements ODataRequest {

  private static final byte[] CRLF = {13, 10};
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  protected final ODataClient odataClient;

  /**
   * OData request method.
   */
  protected final HttpMethod method;

  /**
   * OData request header.
   */
  protected final ODataHeaders odataHeaders;

  /**
   * Target URI.
   */
  protected URI uri;

  /**
   * HTTP client.
   */
  protected ODataHttpClient httpClient;

  /**
   * HTTP request.
   */
  protected ODataHttpRequest request;

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param method HTTP request method. If configured X-HTTP-METHOD header will be used.
   * @param uri OData request URI.
   */
  protected AbstractODataRequest(final ODataClient odataClient, final HttpMethod method, final URI uri) {
    super();

    this.odataClient = odataClient;
    this.method = method;

    // initialize default headers
    this.odataHeaders = odataClient.newVersionHeaders();

    // target uri
    this.uri = uri;
    this.httpClient = getHttpClient(method, uri);
    this.request = odataClient.getConfiguration().getHttpUriRequestFactory().create(this.method, uri);
  }

  public abstract ContentType getDefaultFormat();

  @Override
  public URI getURI() {
    return uri;
  }

  @Override
  public ODataHttpRequest getHttpRequest() {
    return request;
  }

  @Override
  public void setURI(final URI uri) {
    this.uri = uri;
    this.httpClient = getHttpClient(method, uri);
    this.request = odataClient.getConfiguration().getHttpUriRequestFactory().create(this.method, this.uri);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return odataHeaders.getHeaderNames();
  }

  @Override
  public String getHeader(final String name) {
    return odataHeaders.getHeader(name);
  }

  @Override
  public ODataRequest setAccept(final String value) {
    odataHeaders.setHeader(HttpHeader.ACCEPT, value);
    return this;
  }

  @Override
  public ODataRequest setIfMatch(final String value) {
    odataHeaders.setHeader(HttpHeader.IF_MATCH, value);
    return this;
  }

  @Override
  public ODataRequest setIfNoneMatch(final String value) {
    odataHeaders.setHeader(HttpHeader.IF_NONE_MATCH, value);
    return this;
  }

  @Override
  public ODataRequest setPrefer(final String value) {
    odataHeaders.setHeader(HttpHeader.PREFER, value);
    return this;
  }

  @Override
  public ODataRequest setXHTTPMethod(final String value) {
    odataHeaders.setHeader(HttpHeader.X_HTTP_METHOD, value);
    return this;
  }

  @Override
  public ODataRequest setContentType(final String value) {
    odataHeaders.setHeader(HttpHeader.CONTENT_TYPE, value);
    return this;
  }

  @Override
  public ODataRequest addCustomHeader(final String name, final String value) {
    odataHeaders.setHeader(name, value);
    return this;
  }

  @Override
  public String getAccept() {
    final String acceptHead = odataHeaders.getHeader(HttpHeader.ACCEPT);
    return (acceptHead == null || acceptHead.isBlank()) ? getDefaultFormat().toContentTypeString() : acceptHead;
  }

  @Override
  public String getIfMatch() {
    return odataHeaders.getHeader(HttpHeader.IF_MATCH);
  }

  @Override
  public String getIfNoneMatch() {
    return odataHeaders.getHeader(HttpHeader.IF_NONE_MATCH);
  }

  @Override
  public String getPrefer() {
    return odataHeaders.getHeader(HttpHeader.PREFER);
  }

  @Override
  public String getContentType() {
    final String contentTypeHead = odataHeaders.getHeader(HttpHeader.CONTENT_TYPE);
    return (contentTypeHead == null || contentTypeHead.isBlank())
        ? getDefaultFormat().toContentTypeString() : contentTypeHead;
  }

  @Override
  public HttpMethod getMethod() {
    return method;
  }

  /**
   * Gets request headers.
   *
   * @return request headers.
   */
  public ODataHeaders getHeader() {
    return odataHeaders;
  }

  @Override
  public byte[] toByteArray() {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {

        baos.write((getMethod().toString() + ' ' + uri.toString() + ' ' + "HTTP/1.1").getBytes(DEFAULT_CHARSET));

      baos.write(CRLF);

      // Set Content-Type and Accept headers with default values, if not yet set
      String contentTypeHeader = odataHeaders.getHeader(HttpHeader.CONTENT_TYPE);
      if (contentTypeHeader == null || contentTypeHeader.isBlank()) {
        setContentType(getContentType());
      }
      String acceptHeader = odataHeaders.getHeader(HttpHeader.ACCEPT);
      if (acceptHeader == null || acceptHeader.isBlank()) {
        setAccept(getAccept());
      }

      for (String name : getHeaderNames()) {
        final String value = getHeader(name);

        if (value != null && !value.isBlank()) {
          baos.write((name + ": " + value).getBytes(DEFAULT_CHARSET));
          baos.write(CRLF);
        }
      }

      return baos.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      try {
        baos.close();
      } catch (IOException e) {
        LOG.debug("Failed to close resource", e);
      }
    }
  }

  @Override
  public InputStream rawExecute() {
    final HttpUriRequest apacheRequest = ApacheHttpRequest.unwrap(request);
    HttpEntity httpEntity = null;
    try {
      httpEntity = doExecute().getEntity();
      return httpEntity == null ? null : httpEntity.getContent();
    } catch (IOException e) {
      EntityUtils.consumeQuietly(httpEntity);
      throw new HttpClientException(e);
    } catch (RuntimeException e) {
      apacheRequest.abort();
      EntityUtils.consumeQuietly(httpEntity);
      throw new HttpClientException(e);
    }
  }

  /**
   * Builds the request and execute it.
   *
   * @return Apache HttpResponse object.
   */
  protected HttpResponse doExecute() {
    final HttpUriRequest apacheRequest = ApacheHttpRequest.unwrap(request);
    final HttpClient apacheClient = ApacheHttpClient.unwrap(httpClient);

    checkRequest(odataClient, request);

    // Set Content-Type and Accept headers with default values, if not yet set
    String contentTypeHeader = odataHeaders.getHeader(HttpHeader.CONTENT_TYPE);
    if (contentTypeHeader == null || contentTypeHeader.isBlank()) {
      setContentType(getContentType());
    }
    String acceptHeader = odataHeaders.getHeader(HttpHeader.ACCEPT);
    if (acceptHeader == null || acceptHeader.isBlank()) {
      setAccept(getAccept());
    }

    // Add header for KeyAsSegment management
    if (odataClient.getConfiguration().isKeyAsSegment()) {
      addCustomHeader("DataServiceUrlConventions", odataClient.newPreferences().keyAsSegment());
    }

    // Add all available headers
    for (String key : getHeaderNames()) {
      apacheRequest.addHeader(key, odataHeaders.getHeader(key));
    }

    if (LOG.isDebugEnabled()) {
      for (Header header : apacheRequest.getAllHeaders()) {
          LOG.debug("HTTP header being sent: {}", header);
      }
    }

    HttpResponse response;
    try {
      response = apacheClient.execute(apacheRequest);
    } catch (IOException e) {
      throw new HttpClientException(apacheRequest.getURI().toASCIIString(), e);
    } catch (RuntimeException e) {
      apacheRequest.abort();
      throw new HttpClientException(apacheRequest.getURI().toASCIIString(), e);
    }

    final ODataHttpResponse wrappedResponse = new ApacheHttpResponse(response);
    try {
      checkResponse(odataClient, wrappedResponse, getAccept());
    } catch (ODataRuntimeException e) {
      try {
        wrappedResponse.close();
      } catch (IOException ioe) {
        LOG.warn("Unable to close response: {}", response, ioe);
      }
      odataClient.getConfiguration().getHttpClientFactory().close(httpClient);
      throw e;
    }

    return response;
  }

    /**
   * Gets an empty response that can be initialized by a stream.
   * <br/>
   * This method has to be used to build response items about a batch request.
   *
   * @param <V> ODataResponse type.
   * @return empty OData response instance.
   */
  @SuppressWarnings("unchecked")
  public <V extends ODataResponse> V getResponseTemplate() {
    for (Class<?> clazz : this.getClass().getDeclaredClasses()) {
      if (ODataResponse.class.isAssignableFrom(clazz)) {
        try {
          final Constructor<?> constructor = clazz.getDeclaredConstructor(
              this.getClass(), ODataClient.class, ODataHttpClient.class, ODataHttpResponse.class);
          constructor.setAccessible(true);
          return (V) constructor.newInstance(this, odataClient, httpClient, null);
        } catch (Exception e) {
          LOG.error("Error retrieving response class template instance", e);
        }
      }
    }

    throw new IllegalStateException("No response class template has been found");
  }

  private ODataHttpClient getHttpClient(final HttpMethod method, final URI uri) {
    ODataHttpClient client = odataClient.getConfiguration().getHttpClientFactory().create(method, uri);
    if (odataClient.getConfiguration().isGzipCompression()) {
      HttpClient apacheClient = ApacheHttpClient.unwrap(client);
      return new ApacheHttpClient(new ContentCompressingHttpClient(apacheClient));
    }
    return client;
  }
}
