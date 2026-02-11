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
 */
package org.sitenetsoft.olinguito.client.core.communication.request.cud;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpRequest;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpResponse;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityUpdateResponse;
import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.client.api.serialization.ODataSerializerException;
import org.sitenetsoft.olinguito.client.core.communication.request.AbstractODataBasicRequest;
import org.sitenetsoft.olinguito.client.core.communication.response.AbstractODataResponse;
import org.sitenetsoft.olinguito.client.core.uri.URIUtils;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * This class implements an OData update request.
 *
 * @param <E> concrete ODataEntity implementation
 */
public class ODataEntityUpdateRequestImpl<E extends ClientEntity>
        extends AbstractODataBasicRequest<ODataEntityUpdateResponse<E>>
        implements ODataEntityUpdateRequest<E> {

  /**
   * Changes to be applied.
   */
  private final E changes;

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param method request method.
   * @param uri URI of the entity to be updated.
   * @param changes changes to be applied.
   */
  public ODataEntityUpdateRequestImpl(final ODataClient odataClient,
          final HttpMethod method, final URI uri, final E changes) {

    super(odataClient, method, uri);
    this.changes = changes;
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultPubFormat();
  }

  @Override
  public InputStream getPayload() {
    try {
      return odataClient.getWriter().writeEntity(changes, ContentType.parse(getContentType()));
    } catch (final ODataSerializerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public ODataEntityUpdateResponse<E> execute() {
    final InputStream input = getPayload();
    ((HttpEntityEnclosingRequestBase) ApacheHttpRequest.unwrap(request)).setEntity(
        URIUtils.buildInputStreamEntity(odataClient, input));

    try {
      final HttpResponse httpResponse = doExecute();
      final ODataEntityUpdateResponseImpl response =
              new ODataEntityUpdateResponseImpl(odataClient, httpClient, new ApacheHttpResponse(httpResponse));
      if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
        response.close();
      }
      return response;
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        LOG.debug("Failed to close resource", e);
      }
    }
  }

  /**
   * Response class about an ODataEntityUpdateRequest.
   */
  private class ODataEntityUpdateResponseImpl extends AbstractODataResponse implements ODataEntityUpdateResponse<E> {

    /**
     * Changes.
     */
    private E entity = null;

    private ODataEntityUpdateResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    @SuppressWarnings("unchecked")
    public E getBody() {
      if (entity == null) {
        try {
          final ResWrap<Entity> resource = odataClient.getDeserializer(ContentType.parse(getAccept())).
                  toEntity(getRawResponse());

          entity = (E) odataClient.getBinder().getODataEntity(resource);
        } catch (final ODataDeserializerException e) {
          throw new IllegalArgumentException(e);
        } finally {
          this.close();
        }
      }
      return entity;
    }
  }
}
