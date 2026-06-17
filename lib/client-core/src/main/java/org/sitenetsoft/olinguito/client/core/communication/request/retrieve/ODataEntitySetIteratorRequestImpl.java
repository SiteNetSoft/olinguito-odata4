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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1504: stream entity-set iterator response without buffering
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.NoContentException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntitySetIteratorRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySetIterator;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * This class implements an OData EntitySet query request.
 */
public class ODataEntitySetIteratorRequestImpl<ES extends ClientEntitySet, E extends ClientEntity>
        extends AbstractODataRetrieveRequest<ClientEntitySetIterator<ES, E>>
        implements ODataEntitySetIteratorRequest<ES, E> {

  private ClientEntitySetIterator<ES, E> entitySetIterator = null;

  /**
   * Private constructor.
   *
   * @param odataClient client instance getting this request
   * @param query query to be executed.
   */
  public ODataEntitySetIteratorRequestImpl(final ODataClient odataClient, final URI query) {
    super(odataClient, query);
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultPubFormat();
  }

  @Override
  public ODataRetrieveResponse<ClientEntitySetIterator<ES, E>> execute() {
    return new ODataEntitySetIteratorResponseImpl(odataClient, httpClient, doExecute());
  }

  /**
   * Response class about an ODataEntitySetIteratorRequest.
   */
  protected class ODataEntitySetIteratorResponseImpl extends AbstractODataRetrieveResponse {

    ODataEntitySetIteratorResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    public ClientEntitySetIterator<ES, E> getBody() {
      if (entitySetIterator == null) {
        entitySetIterator = new ClientEntitySetIterator<>(
                odataClient, getRawResponse(), Objects.requireNonNull(ContentType.parse(getContentType())));
      }
      return entitySetIterator;
    }

    /**
     * Returns the live response stream directly instead of buffering the whole feed into memory,
     * so a large non-paginated collection does not cause an OutOfMemoryError (OLINGO-1504). The
     * iterator returned by {@link #getBody()} consumes the stream incrementally and closes it.
     */
    @Override
    public InputStream getRawResponse() {
      if (HttpStatusCode.NO_CONTENT.getStatusCode() == getStatusCode()) {
        throw new NoContentException();
      }
      return payload;
    }
  }
}
