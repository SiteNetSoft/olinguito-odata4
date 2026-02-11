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

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpResponse;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataPropertyUpdateRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataPropertyUpdateResponse;
import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.domain.ClientProperty;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.client.api.serialization.ODataSerializerException;
import org.sitenetsoft.olinguito.client.core.communication.request.AbstractODataBasicRequest;
import org.sitenetsoft.olinguito.client.core.communication.response.AbstractODataResponse;
import org.sitenetsoft.olinguito.client.core.uri.URIUtils;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * This class implements an OData update entity property request.
 */
public class ODataPropertyUpdateRequestImpl extends AbstractODataBasicRequest<ODataPropertyUpdateResponse>
        implements ODataPropertyUpdateRequest {

  /**
   * Value to be created.
   */
  private final ClientProperty property;

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param method request method.
   * @param targetURI entity set or entity or entity property URI.
   * @param property value to be created.
   */
  ODataPropertyUpdateRequestImpl(final ODataClient odataClient,
          final HttpMethod method, final URI targetURI, final ClientProperty property) {

    super(odataClient, method, targetURI);
    // set request body
    this.property = property;
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultFormat();
  }

  @Override
  public ODataPropertyUpdateResponse execute() {
    final InputStream input = getPayload();
    ((HttpEntityEnclosingRequestBase) ApacheHttpRequest.unwrap(request)).setEntity(
        URIUtils.buildInputStreamEntity(odataClient, input));

    try {
      return new ODataPropertyUpdateResponseImpl(odataClient, httpClient, new ApacheHttpResponse(doExecute()));
    } finally {
      try {
        input.close();
      } catch (IOException ignored) { }
    }
  }

  @Override
  public InputStream getPayload() {
    try {
      return odataClient.getWriter().writeProperty(property, ContentType.parse(getContentType()));
    } catch (final ODataSerializerException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Response class about an ODataPropertyUpdateRequest.
   */
  private class ODataPropertyUpdateResponseImpl extends AbstractODataResponse implements ODataPropertyUpdateResponse {

    private ClientProperty resProperty = null;

    private ODataPropertyUpdateResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    public ClientProperty getBody() {
      if (resProperty == null) {
        try {
          final ResWrap<Property> resource = odataClient.getDeserializer(ContentType.parse(getAccept())).
                  toProperty(getRawResponse());

          resProperty = odataClient.getBinder().getODataProperty(resource);
        } catch (final ODataDeserializerException e) {
          throw new IllegalArgumentException(e);
        } finally {
          this.close();
        }
      }
      return resProperty;
    }
  }
}
