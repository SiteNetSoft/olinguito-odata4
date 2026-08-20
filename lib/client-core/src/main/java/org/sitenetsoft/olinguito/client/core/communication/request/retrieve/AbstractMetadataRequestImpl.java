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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: format-aware metadata request base
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.net.URI;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataRequest;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

public abstract class AbstractMetadataRequestImpl<V> extends AbstractODataRetrieveRequest<V> {

  private final ContentType metadataFormat;

  public AbstractMetadataRequestImpl(final ODataClient odataClient, final URI query) {
    this(odataClient, query, ContentType.APPLICATION_XML);
  }

  /**
   * A metadata request is pinned to one representation: OData 4.01, Part 1: Protocol section 11.1.2
   * defines exactly two ([OData-CSDLXML] as application/xml and [OData-CSDLJSON] as application/json),
   * and the request knows which one it can deserialize, so Accept and Content-Type are fixed here and
   * the setters stay no-ops.
   *
   * @param odataClient the OData client.
   * @param query the request URI.
   * @param metadataFormat the metadata representation this request can deserialize.
   */
  protected AbstractMetadataRequestImpl(final ODataClient odataClient, final URI query,
      final ContentType metadataFormat) {
    super(odataClient, query);
    this.metadataFormat = metadataFormat;
    super.setAccept(metadataFormat.toContentTypeString());
    super.setContentType(metadataFormat.toContentTypeString());
  }

  @Override
  public ContentType getDefaultFormat() {
    return metadataFormat;
  }

  @Override
  public ODataRequest setAccept(final String value) {
    // do nothing: Accept is fixed to the metadata format this request can deserialize
    return this;
  }

  @Override
  public ODataRequest setContentType(final String value) {
    // do nothing: Content-Type is fixed to the metadata format this request can deserialize
    return this;
  }

}
