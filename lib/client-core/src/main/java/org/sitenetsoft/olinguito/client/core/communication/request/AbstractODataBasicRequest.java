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
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import org.sitenetsoft.olinguito.client.api.ODataBatchConstants;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataBasicRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Basic request abstract implementation.
 *
 * @param <T> OData response type corresponding to the request implementation.
 */
public abstract class AbstractODataBasicRequest<T extends ODataResponse>
    extends AbstractODataRequest implements ODataBasicRequest<T> {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  private static final byte[] CRLF = {13, 10};

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param method request method.
   * @param uri OData request URI.
   */
  public AbstractODataBasicRequest(final ODataClient odataClient, final HttpMethod method, final URI uri) {
    super(odataClient, method, uri);
  }

  @Override
  public void setFormat(final ContentType contentType) {
    if (contentType != null) {
      final String formatString = contentType.toContentTypeString();
      setAccept(formatString);
      setContentType(formatString);
    }
  }

  @Override
  public final Future<T> asyncExecute() {
      //NOSONAR
      return odataClient.getConfiguration().getExecutor().submit(this::execute);
  }

  /**
   * Gets payload as an InputStream.
   *
   * @return InputStream for entire payload.
   */
  public abstract InputStream getPayload();

  /**
   * Serializes the full request into the given batch request.
   *
   * @param req destination batch request.
   */
  public void batch(final ODataBatchRequest req) {
    batch(req, null);
  }

  /**
   * Serializes the full request into the given batch request.
   * <p>
   * This method have to be used to serialize a changeset item with the specified contentId.
   *
   * @param req destination batch request.
   * @param contentId contentId of the changeset item.
   */
  public void batch(final ODataBatchRequest req, final String contentId) {
    try {
      req.rawAppend(toByteArray());
      if (contentId != null && !contentId.isBlank()) {
        req.rawAppend((ODataBatchConstants.CHANGESET_CONTENT_ID_NAME + ": " + contentId).getBytes(DEFAULT_CHARSET));
        req.rawAppend(CRLF);
      }
      req.rawAppend(CRLF);

      final InputStream payload = getPayload();
      if (payload != null) {
        req.rawAppend(getPayload().readAllBytes());
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
