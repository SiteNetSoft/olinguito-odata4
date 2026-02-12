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
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.net.URI;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataServiceDocumentRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.data.ServiceDocument;
import org.sitenetsoft.olinguito.client.api.domain.ClientServiceDocument;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * This class implements an OData service document request.
 */
public class ODataServiceDocumentRequestImpl extends AbstractODataRetrieveRequest<ClientServiceDocument>
        implements ODataServiceDocumentRequest {

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param uri request URI.
   */
  ODataServiceDocumentRequestImpl(final ODataClient odataClient, final URI uri) {
    super(odataClient, uri);
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultFormat();
  }

  @Override
  public ODataRetrieveResponse<ClientServiceDocument> execute() {
    return new ODataServiceResponseImpl(odataClient, httpClient, doExecute());
  }

  /**
   * Response class about an ODataServiceDocumentRequest.
   */
  protected class ODataServiceResponseImpl extends AbstractODataRetrieveResponse {

    private ClientServiceDocument serviceDocument = null;

    private ODataServiceResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    public ClientServiceDocument getBody() {
      if (serviceDocument == null) {
        try {
          final ResWrap<ServiceDocument> resource = odataClient.
                  getDeserializer(ContentType.parse(getContentType())).toServiceDocument(getRawResponse());

          serviceDocument = odataClient.getBinder().getODataServiceDocument(resource.getPayload());
        } catch (final ODataDeserializerException e) {
          throw new IllegalArgumentException(e);
        } finally {
          this.close();
        }
      }
      return serviceDocument;
    }
  }
}
