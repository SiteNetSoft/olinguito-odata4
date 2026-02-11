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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 */
package org.sitenetsoft.olinguito.client.core.communication.request.streamed;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.sitenetsoft.olinguito.client.api.ODataBatchConstants;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpRequest;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataPayloadManager;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataStreamedRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.core.communication.request.AbstractODataRequest;
import org.sitenetsoft.olinguito.client.core.communication.request.Wrapper;
import org.sitenetsoft.olinguito.client.core.uri.URIUtils;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Streamed OData request abstract class.
 *
 * @param <V> OData response type corresponding to the request implementation.
 * @param <T> OData request payload type corresponding to the request implementation.
 */
public abstract class AbstractODataStreamedRequest<V extends ODataResponse, T extends ODataPayloadManager<V>>
        extends AbstractODataRequest implements ODataStreamedRequest<V, T> {

  /**
   * OData payload stream manager.
   */
  protected ODataPayloadManager<V> payloadManager;
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  private static final byte[] CRLF = {13, 10};

  /**
   * Wrapper for actual streamed request's future. This holds information about the HTTP request / response currently
   * open.
   */
  protected final Wrapper<Future<HttpResponse>> futureWrapper = new Wrapper<>();

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param method OData request HTTP method.
   * @param uri OData request URI.
   */
  public AbstractODataStreamedRequest(final ODataClient odataClient,
          final HttpMethod method, final URI uri) {

    super(odataClient, method, uri);
    setAccept(ContentType.APPLICATION_OCTET_STREAM.toContentTypeString());
    setContentType(ContentType.APPLICATION_OCTET_STREAM.toContentTypeString());
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultMediaFormat();
  }

  /**
   * Gets OData request payload management object.
   *
   * @return OData request payload management object.
   */
  protected abstract T getPayloadManager();

  /**
   * {@inheritDoc }
   */
  @Override
  @SuppressWarnings("unchecked")
  public T payloadManager() {
    payloadManager = getPayloadManager();

    final HttpUriRequest apacheRequest = ApacheHttpRequest.unwrap(request);
    if (URIUtils.shouldUseRepeatableHttpBodyEntry(odataClient)) {
      futureWrapper.setWrapped(odataClient.getConfiguration().getExecutor().submit(() -> { //NOSONAR
        ((HttpEntityEnclosingRequestBase) apacheRequest).setEntity(
                URIUtils.buildInputStreamEntity(odataClient, payloadManager.getBody()));
        try {
          return doExecute();
        } finally {
          payloadManager.finalizeBody();
        }
      }));
    } else {
      ((HttpEntityEnclosingRequestBase) apacheRequest).setEntity(
              URIUtils.buildInputStreamEntity(odataClient, payloadManager.getBody()));

      futureWrapper.setWrapped(odataClient.getConfiguration().getExecutor().submit(() -> { //NOSONAR
        try {
          return doExecute();
        } finally {
          payloadManager.finalizeBody();
        }
      }));
    }

    // returns the stream manager object
    return (T) payloadManager;
  }

  /**
   * Writes (and consume) the request onto the given batch stream.
   * <p>
   * Please note that this method will consume the request (execution won't be possible anymore).
   *
   * @param req destination batch request.
   */
  public void batch(final ODataBatchRequest req) {
    batch(req, null);
  }

  /**
   * Writes (and consume) the request onto the given batch stream.
   * <p>
   * Please note that this method will consume the request (execution won't be possible anymore).
   *
   * @param req destination batch request.
   * @param contentId ContentId header value to be added to the serialization. Use this in case of changeset items.
   */
  public void batch(final ODataBatchRequest req, final String contentId) {
    final InputStream input = getPayloadManager().getBody();

    try {
      // finalize the body
      getPayloadManager().finalizeBody();

      req.rawAppend(toByteArray());
      if (contentId != null && !contentId.isBlank()) {
        req.rawAppend((ODataBatchConstants.CHANGESET_CONTENT_ID_NAME + ": " + contentId).getBytes(DEFAULT_CHARSET));
        req.rawAppend(CRLF);
      }
      req.rawAppend(CRLF);

      try {
        req.rawAppend(input.readAllBytes());
      } catch (Exception e) {
        LOG.debug("Invalid stream", e);
        req.rawAppend(new byte[0]);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        LOG.debug("Failed to close resource", e);
      }
    }
  }
}
