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

import org.apache.http.client.methods.HttpPost;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpResponse;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityCreateRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityCreateResponse;
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
 * This class implements an OData create request.
 *
 * @param <E> concrete ODataEntity implementation
 */
public class ODataEntityCreateRequestImpl<E extends ClientEntity>
        extends AbstractODataBasicRequest<ODataEntityCreateResponse<E>>
        implements ODataEntityCreateRequest<E> {

  /**
   * Entity to be created.
   */
  private final E entity;

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param targetURI entity set URI.
   * @param entity entity to be created.
   */
  ODataEntityCreateRequestImpl(final ODataClient odataClient, final URI targetURI, final E entity) {
    super(odataClient, HttpMethod.POST, targetURI);
    this.entity = entity;
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultPubFormat();
  }

  @Override
  public InputStream getPayload() {
    try {
      return odataClient.getWriter().writeEntity(entity, ContentType.parse(getContentType()));
    } catch (final ODataSerializerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public ODataEntityCreateResponse<E> execute() {
    final InputStream input = getPayload();
    ((HttpPost) ApacheHttpRequest.unwrap(request)).setEntity(
        URIUtils.buildInputStreamEntity(odataClient, input));

    try {
      return new ODataEntityCreateResponseImpl(odataClient, httpClient, new ApacheHttpResponse(doExecute()));
    } finally {
      try {
        input.close();
      } catch (IOException ignored) { }
    }
  }

  /**
   * Response class about an ODataEntityCreateRequest.
   */
  private class ODataEntityCreateResponseImpl extends AbstractODataResponse implements ODataEntityCreateResponse<E> {

    private E resEntity = null;

    private ODataEntityCreateResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    @SuppressWarnings("unchecked")
    public E getBody() {
      if (resEntity == null) {
        try {
          final ResWrap<Entity> resource = odataClient.getDeserializer(ContentType.parse(getAccept())).
                  toEntity(getRawResponse());

          resEntity = (E) odataClient.getBinder().getODataEntity(resource);
        } catch (final ODataDeserializerException e) {
          throw new IllegalArgumentException(e);
        } finally {
          this.close();
        }
      }
      return resEntity;
    }
  }
}
