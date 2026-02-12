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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataRawRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRawResponse;
import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.client.core.communication.request.AbstractODataRequest;
import org.sitenetsoft.olinguito.client.core.communication.response.AbstractODataResponse;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * This class implements a generic OData request.
 */
public class ODataRawRequestImpl extends AbstractODataRequest implements ODataRawRequest {

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param uri request URI.
   */
  ODataRawRequestImpl(final ODataClient odataClient, final URI uri) {
    super(odataClient, HttpMethod.GET, uri);
  }

  @Override
  public ContentType getDefaultFormat() {
    return odataClient.getConfiguration().getDefaultPubFormat();
  }

  @Override
  public void setFormat(final String format) {
    setAccept(format);
    setContentType(format);
  }

  @Override
  public ODataRawResponse execute() {
    return new ODataRawResponseImpl(odataClient, httpClient, doExecute());
  }

  private class ODataRawResponseImpl extends AbstractODataResponse implements ODataRawResponse {

    private byte[] obj = null;

    private ODataRawResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    public <T> ResWrap<T> getBodyAs(final Class<T> reference) {
      if (obj == null) {
        try {
          this.obj = getRawResponse().readAllBytes();
        } catch (IOException e) {
          throw new IllegalArgumentException(e);
        } finally {
          this.close();
        }
      }

      try {
        return odataClient.getReader().read(new ByteArrayInputStream(obj), getContentType(), reference);
      } catch (final ODataDeserializerException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }
}
