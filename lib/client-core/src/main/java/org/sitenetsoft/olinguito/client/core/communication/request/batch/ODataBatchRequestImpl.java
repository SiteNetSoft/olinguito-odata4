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
 * Copyright 2026 SiteNetSoft - Implemented validateSingleRequest() method
 * Copyright 2026 SiteNetSoft - Port OLINGO-1587: apply configured response timeout to batch manager
 */
package org.sitenetsoft.olinguito.client.core.communication.request.batch;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.api.communication.header.ODataPreferences;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataBatchableRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.BatchManager;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchResponseItem;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataBatchResponse;
import org.sitenetsoft.olinguito.client.core.communication.response.AbstractODataResponse;
import org.sitenetsoft.olinguito.client.core.communication.response.batch.ODataBatchResponseManager;

public class ODataBatchRequestImpl
        extends AbstractODataBatchRequest<ODataBatchResponse, BatchManager>
        implements ODataBatchRequest {

  public ODataBatchRequestImpl(final ODataClient odataClient, final URI uri) {
    super(odataClient, uri);
    setAccept(odataClient.getConfiguration().getDefaultBatchAcceptFormat().toContentTypeString());
  }

  @Override
  protected BatchManager getPayloadManager() {
    if (payloadManager == null) {
      payloadManager = new BatchManagerImpl(this);
    }
    return (BatchManager) payloadManager;
  }

  @Override
  public ODataBatchRequest rawAppend(final byte[] toBeStreamed) throws IOException {
    getPayloadManager().getBodyStreamWriter().write(toBeStreamed);
    return this;
  }

  @Override
  public ODataBatchRequest rawAppend(final byte[] toBeStreamed, int off, int len) throws IOException {
    getPayloadManager().getBodyStreamWriter().write(toBeStreamed, off, len);
    return this;
  }

  @Override
  protected ODataHttpResponse doExecute() {
    if (odataClient.getConfiguration().isContinueOnError()) {
      setPrefer(new ODataPreferences().continueOnError());
    }

    return super.doExecute();
  }

  /**
   * Batch request payload management.
   */
  public class BatchManagerImpl extends AbstractBatchManager implements BatchManager {

    public BatchManagerImpl(final ODataBatchRequest req) {
      super(req, ODataBatchRequestImpl.this.futureWrapper,
              ODataBatchRequestImpl.this.odataClient.getConfiguration().isContinueOnError());
      setResponseTimeoutInSec(ODataBatchRequestImpl.this.odataClient.getConfiguration().getResponseTimeoutInSec());
    }

    @Override
    protected ODataBatchResponse getResponseInstance(final long timeout, final TimeUnit unit) {
      return new ODataBatchResponseImpl(odataClient, httpClient,
          getHttpResponse(timeout, unit));
    }

    @Override
    protected void validateSingleRequest(final ODataBatchableRequest request) {
      if (request == null) {
        throw new IllegalArgumentException("Batch request item cannot be null");
      }
      if (request.getURI() == null) {
        throw new IllegalArgumentException("Batch request item must have a valid URI");
      }
    }
  }

  protected class ODataBatchResponseImpl extends AbstractODataResponse implements ODataBatchResponse {

    protected ODataBatchResponseImpl(
            final ODataClient odataClient, final ODataHttpClient httpClient, final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    public Iterator<ODataBatchResponseItem> getBody() {
      return new ODataBatchResponseManager(this, expectedResItems, odataClient.getConfiguration().isContinueOnError());
    }

    @Override
    public void close() {
      for (ODataBatchResponseItem resItem : expectedResItems) {
        resItem.close();
      }
      super.close();
    }

  }
}
