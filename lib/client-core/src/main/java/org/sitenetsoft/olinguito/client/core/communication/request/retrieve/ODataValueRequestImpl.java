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
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataValueRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientPrimitiveValue;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * This class implements an OData entity property value query request.
 */
public class ODataValueRequestImpl extends AbstractODataRetrieveRequest<ClientPrimitiveValue>
        implements ODataValueRequest {

  /**
   * Private constructor.
   *
   * @param odataClient client instance getting this request
   * @param query query to be executed.
   */
  ODataValueRequestImpl(final ODataClient odataClient, final URI query) {
    super(odataClient, query);
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultValueFormat();
  }

  @Override
  public ODataRetrieveResponse<ClientPrimitiveValue> execute() {
    return new ODataValueResponseImpl(odataClient, httpClient, doExecute());
  }

  /**
   * Response class about an ODataDeleteReODataValueRequestquest.
   */
  protected class ODataValueResponseImpl extends AbstractODataRetrieveResponse {

    private ClientPrimitiveValue value = null;

    private ODataValueResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    public ClientPrimitiveValue getBody() {
      if (value == null) {
        final ContentType contentType = ContentType.parse(getContentType());

        try {
            assert contentType != null;
            value = odataClient.getObjectFactory().newPrimitiveValueBuilder().
                  setType(contentType.isCompatible(ContentType.TEXT_PLAIN)
                          ? EdmPrimitiveTypeKind.String : EdmPrimitiveTypeKind.Stream).
                  setValue(new String(getRawResponse().readAllBytes(), StandardCharsets.UTF_8)).build();
        } catch (Exception e) {
          throw new HttpClientException(e);
        } finally {
          this.close();
        }
      }
      return value;
    }
  }
}
