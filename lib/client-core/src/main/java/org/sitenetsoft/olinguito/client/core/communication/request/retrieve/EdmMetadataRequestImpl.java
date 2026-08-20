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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: honour the configured metadata document format
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: keep the delegate's pinned Accept/Content-Type
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.net.URI;
import java.util.Collection;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.EdmMetadataRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataRetrieveRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;

/**
 * This class implements a metadata query request.
 */
class EdmMetadataRequestImpl extends AbstractMetadataRequestImpl<Edm> implements EdmMetadataRequest {

  private final String serviceRoot;

  private EdmMetadataResponseImpl privateResponse;

  EdmMetadataRequestImpl(final ODataClient odataClient, final String serviceRoot, final URI uri) {
    super(odataClient, uri);
    this.serviceRoot = serviceRoot;
  }

  /**
   * Builds the request that actually fetches the metadata document, in the representation the client
   * is configured for, with this request's headers copied onto it. Package-private so that tests can
   * inspect the delegate without executing it.
   *
   * @return the configured, not yet executed, metadata request.
   */
  ODataRetrieveRequest<XMLMetadata> createMetadataRequest() {
    // OData 4.01, Part 1: Protocol section 11.1.2 - the CSDL JSON representation only when the caller
    // asked for it; anything that is not application/json compatible (including an unknown format) is
    // the XML representation, which is what a request expressing no format preference must get.
    final ContentType metadataFormat = odataClient.getConfiguration().getMetadataFormat();
    final ODataRetrieveRequest<XMLMetadata> request =
        metadataFormat != null && ContentType.APPLICATION_JSON.isCompatible(metadataFormat)
            ? odataClient.getRetrieveRequestFactory().getJSONMetadataRequest(serviceRoot)
            : odataClient.getRetrieveRequestFactory().getXMLMetadataRequest(serviceRoot);
    if (getPrefer() != null) {
      request.setPrefer(getPrefer());
    }
    if (getIfMatch() != null) {
      request.setIfMatch(getIfMatch());
    }
    if (getIfNoneMatch() != null) {
      request.setIfNoneMatch(getIfNoneMatch());
    }
    if (getHeader() != null) {
      for (String key : getHeaderNames()) {
        // Accept and Content-Type are pinned by the delegate to the representation it can deserialize;
        // copying this request's own (always application/xml) values would silently override them.
        if (HttpHeader.ACCEPT.equalsIgnoreCase(key) || HttpHeader.CONTENT_TYPE.equalsIgnoreCase(key)) {
          continue;
        }
        request.addCustomHeader(key, odataHeaders.getHeader(key));
      }
    }
    return request;
  }

  private EdmMetadataResponseImpl getPrivateResponse() {
    if (privateResponse == null) {
      final ODataRetrieveResponse<XMLMetadata> xmlMetadataResponse = createMetadataRequest().execute();

      privateResponse = new EdmMetadataResponseImpl(odataClient, httpClient, xmlMetadataResponse);
    }
    return privateResponse;
  }

  @Override
  public XMLMetadata getXMLMetadata() {
    return getPrivateResponse().getXMLMetadata();
  }

  @Override
  public ODataRetrieveResponse<Edm> execute() {
    return getPrivateResponse();
  }

  private class EdmMetadataResponseImpl extends AbstractODataRetrieveResponse {

    private final ODataRetrieveResponse<XMLMetadata> xmlMetadataResponse;

    private XMLMetadata metadata = null;

    private EdmMetadataResponseImpl(final ODataClient odataClient, final ODataHttpClient httpClient,
        final ODataRetrieveResponse<XMLMetadata> xmlMetadataResponse) {

      super(odataClient, httpClient, null);
      this.xmlMetadataResponse = xmlMetadataResponse;
    }

    @Override
    public void close() {
      super.close();
      xmlMetadataResponse.close();
    }

    @Override
    public int getStatusCode() {
      return xmlMetadataResponse.getStatusCode();
    }

    @Override
    public String getStatusMessage() {
      return xmlMetadataResponse.getStatusMessage();
    }

    @Override
    public Collection<String> getHeaderNames() {
      return xmlMetadataResponse.getHeaderNames();
    }

    @Override
    public Collection<String> getHeader(final String name) {
      return xmlMetadataResponse.getHeader(name);
    }

    public XMLMetadata getXMLMetadata() {
      if (metadata == null) {
        try {
          metadata = xmlMetadataResponse.getBody();
        } finally {
          this.close();
        }
      }
      return metadata;
    }

    @Override
    public Edm getBody() {
      return odataClient.getReader().readMetadata(getXMLMetadata().getSchemaByNsOrAlias());
    }
  }
}
