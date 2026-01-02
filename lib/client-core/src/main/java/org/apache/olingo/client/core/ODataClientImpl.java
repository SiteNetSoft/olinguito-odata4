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
package org.sitenetsoft.olinguito.client.core;

import org.sitenetsoft.olinguito.client.api.Configuration;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.header.ODataHeaders;
import org.sitenetsoft.olinguito.client.api.communication.header.ODataPreferences;
import org.sitenetsoft.olinguito.client.api.communication.request.AsyncRequestFactory;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.BatchRequestFactory;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.CUDRequestFactory;
import org.sitenetsoft.olinguito.client.api.communication.request.invoke.InvokeRequestFactory;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.RetrieveRequestFactory;
import org.sitenetsoft.olinguito.client.api.domain.ClientObjectFactory;
import org.sitenetsoft.olinguito.client.api.serialization.ClientODataDeserializer;
import org.sitenetsoft.olinguito.client.api.serialization.ODataBinder;
import org.sitenetsoft.olinguito.client.api.serialization.ODataMetadataValidation;
import org.sitenetsoft.olinguito.client.api.serialization.ODataReader;
import org.sitenetsoft.olinguito.client.api.serialization.ODataSerializer;
import org.sitenetsoft.olinguito.client.api.serialization.ODataWriter;
import org.sitenetsoft.olinguito.client.api.uri.FilterFactory;
import org.sitenetsoft.olinguito.client.api.uri.SearchFactory;
import org.sitenetsoft.olinguito.client.api.uri.URIBuilder;
import org.sitenetsoft.olinguito.client.core.communication.header.ODataHeadersImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.AsyncRequestFactoryImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.BatchRequestFactoryImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.cud.CUDRequestFactoryImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.invoke.InvokeRequestFactoryImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.retrieve.RetrieveRequestFactoryImpl;
import org.sitenetsoft.olinguito.client.core.domain.ClientObjectFactoryImpl;
import org.sitenetsoft.olinguito.client.core.serialization.AtomSerializer;
import org.sitenetsoft.olinguito.client.core.serialization.ClientODataDeserializerImpl;
import org.sitenetsoft.olinguito.client.core.serialization.JsonSerializer;
import org.sitenetsoft.olinguito.client.core.serialization.ODataBinderImpl;
import org.sitenetsoft.olinguito.client.core.serialization.ODataMetadataValidationImpl;
import org.sitenetsoft.olinguito.client.core.serialization.ODataReaderImpl;
import org.sitenetsoft.olinguito.client.core.serialization.ODataWriterImpl;
import org.sitenetsoft.olinguito.client.core.uri.FilterFactoryImpl;
import org.sitenetsoft.olinguito.client.core.uri.URIBuilderImpl;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

public class ODataClientImpl implements ODataClient {

  private final FilterFactory filterFactory = new FilterFactoryImpl();

  private final SearchFactory searchFactory = new SearchFactoryImpl();

  private final ODataReader reader = new ODataReaderImpl(this);

  private final ODataBinder binder = new ODataBinderImpl(this);

  private final ClientObjectFactory objectFactory = new ClientObjectFactoryImpl();

  private final AsyncRequestFactory asyncReqFact = new AsyncRequestFactoryImpl(this);

  private final RetrieveRequestFactory retrieveReqFact = new RetrieveRequestFactoryImpl(this);

  private final CUDRequestFactory cudReqFact = new CUDRequestFactoryImpl(this);

  private final InvokeRequestFactory invokeReqFact = new InvokeRequestFactoryImpl(this);

  private final BatchRequestFactory batchReqFact = new BatchRequestFactoryImpl(this);

  protected final Configuration configuration = new ConfigurationImpl();

  private final ODataWriter writer = new ODataWriterImpl(this);
  
  private final ODataMetadataValidation metadataValidation = new ODataMetadataValidationImpl();

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public ODataPreferences newPreferences() {
    return new ODataPreferences();
  }

  @Override
  public ODataWriter getWriter() {
    return writer;
  }

  @Override
  public ODataServiceVersion getServiceVersion() {
    return ODataServiceVersion.V40;
  }

  @Override
  public ODataHeaders newVersionHeaders() {
    final ODataHeadersImpl odataHeaders = new ODataHeadersImpl();
    odataHeaders.setHeader(HttpHeader.ODATA_MAX_VERSION, ODataServiceVersion.V40.toString());
    odataHeaders.setHeader(HttpHeader.ODATA_VERSION, ODataServiceVersion.V40.toString());
    return odataHeaders;
  }

  @Override
  public URIBuilder newURIBuilder(final String serviceRoot) {
    return new URIBuilderImpl(getConfiguration(), serviceRoot);
  }

  @Override
  public FilterFactory getFilterFactory() {
    return filterFactory;
  }

  @Override
  public SearchFactory getSearchFactory() {
    return searchFactory;
  }

  @Override
  public ClientODataDeserializer getDeserializer(final ContentType contentType) {
    return new ClientODataDeserializerImpl(false, contentType);
  }

  @Override
  public ODataSerializer getSerializer(final ContentType contentType) {
    return contentType.isCompatible(ContentType.APPLICATION_ATOM_SVC)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML)
        || contentType.isCompatible(ContentType.APPLICATION_XML) ?
        new AtomSerializer() : new JsonSerializer(false, contentType);
  }

  @Override
  public ODataReader getReader() {
    return reader;
  }

  @Override
  public ODataBinder getBinder() {
    return binder;
  }

  @Override
  public ClientObjectFactory getObjectFactory() {
    return objectFactory;
  }

  @Override
  public AsyncRequestFactory getAsyncRequestFactory() {
    return asyncReqFact;
  }

  @Override
  public RetrieveRequestFactory getRetrieveRequestFactory() {
    return retrieveReqFact;
  }

  @Override
  public CUDRequestFactory getCUDRequestFactory() {
    return cudReqFact;
  }

  @Override
  public InvokeRequestFactory getInvokeRequestFactory() {
    return invokeReqFact;
  }

  @Override
  public BatchRequestFactory getBatchRequestFactory() {
    return batchReqFact;
  }

  @Override
  public ODataMetadataValidation metadataValidation() {
    return metadataValidation;
  }
}
