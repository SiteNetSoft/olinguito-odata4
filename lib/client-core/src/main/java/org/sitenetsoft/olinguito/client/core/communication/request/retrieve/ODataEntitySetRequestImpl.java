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
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.net.URI;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * This class implements an OData EntitySet query request.
 *
 * @param <ES> concrete ODataEntitySet implementation
 */
public class ODataEntitySetRequestImpl<ES extends ClientEntitySet>
    extends AbstractODataRetrieveRequest<ES> implements ODataEntitySetRequest<ES> {

  private ES entitySet = null;

  /**
   * Private constructor.
   *
   * @param odataClient client instance getting this request
   * @param query query to be executed.
   */
  public ODataEntitySetRequestImpl(final ODataClient odataClient, final URI query) {
    super(odataClient, query);
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultPubFormat();
  }

  @Override
  public ODataRetrieveResponse<ES> execute() {
    return new ODataEntitySetResponseImpl(odataClient, httpClient, doExecute());
  }

  /**
   * Response class about an ODataEntitySetRequest.
   */
  protected class ODataEntitySetResponseImpl extends AbstractODataRetrieveResponse {

    private ODataEntitySetResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
        final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ES getBody() {
      if (entitySet == null) {
        try {
          final ResWrap<EntityCollection> resource =
              odataClient.getDeserializer(ContentType.parse(getContentType())).
                  toEntitySet(getRawResponse());

          entitySet = (ES) odataClient.getBinder().getODataEntitySet(resource);
        } catch (final ODataDeserializerException e) {
          throw new IllegalArgumentException(e);
        } finally {
          this.close();
        }
      }
      return entitySet;
    }
  }
}
