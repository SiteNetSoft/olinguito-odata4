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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON metadata request
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.net.URI;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.JSONMetadataRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * This class implements a CSDL JSON metadata request: the [OData-CSDLJSON] representation of the
 * metadata document (OData 4.01, Part 1: Protocol section 11.1.2).
 * <br/>
 * Unlike {@link XMLMetadataRequestImpl}, referenced documents ({@code $Reference}) are not followed;
 * the document is read exactly as served.
 */
public class JSONMetadataRequestImpl
    extends AbstractMetadataRequestImpl<XMLMetadata>
    implements JSONMetadataRequest {

  JSONMetadataRequestImpl(final ODataClient odataClient, final URI uri) {
    super(odataClient, uri, ContentType.APPLICATION_JSON);
  }

  @Override
  public ODataRetrieveResponse<XMLMetadata> execute() {
    return new AbstractODataRetrieveResponse(odataClient, httpClient, doExecute()) {

      private XMLMetadata metadata = null;

      @Override
      public XMLMetadata getBody() {
        if (metadata == null) {
          try {
            metadata = odataClient.getDeserializer(ContentType.APPLICATION_JSON).toJSONMetadata(getRawResponse());
          } finally {
            this.close();
          }
        }
        return metadata;
      }
    };
  }
}
