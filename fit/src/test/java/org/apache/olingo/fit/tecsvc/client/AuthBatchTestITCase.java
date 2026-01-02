/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sitenetsoft.olinguito.fit.tecsvc.client;

import java.net.URI;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.BatchManager;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataChangeset;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.UpdateType;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntityRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataBatchResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.http.HttpClientException;
import org.sitenetsoft.olinguito.client.api.uri.URIBuilder;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class AuthBatchTestITCase extends AbstractParamTecSvcITCase{
  private final static ContentType ACCEPT = ContentType.APPLICATION_OCTET_STREAM;
  
  @Test
  public void authorized() throws EdmPrimitiveTypeException {
    final ODataClient authclient = getBasicAuthClient(USERNAME, PASSWORD);
    batchRequest(authclient, AUTH_URI);
  }

  @Test(expected = HttpClientException.class)
  public void unauthorized() throws EdmPrimitiveTypeException {
    final ODataClient unauthclient = getBasicAuthClient("not_auth", "not_auth");
    batchRequest(unauthclient, AUTH_URI);
  }
  
  private void batchRequest(final ODataClient client, final String baseURL) throws EdmPrimitiveTypeException {
    // create your request
    final ODataBatchRequest request = client.getBatchRequestFactory().getBatchRequest(baseURL);
    request.setAccept(ACCEPT.toContentTypeString());
    request.addCustomHeader("User-Agent", "Apache Olingo OData Client");
    request.addCustomHeader(HttpHeader.ACCEPT_CHARSET, "UTF-8");

    final BatchManager streamManager = request.payloadManager();

    // -------------------------------------------
    // Add retrieve item
    // -------------------------------------------
    // prepare URI
    URIBuilder targetURI = client.newURIBuilder(baseURL);
    targetURI.appendEntitySetSegment("ESAllPrim").appendKeySegment(32767);

    // create new request
    ODataEntityRequest<ClientEntity> queryReq = client.getRetrieveRequestFactory().getEntityRequest(targetURI.build());
  
    streamManager.addRequest(queryReq);
    // -------------------------------------------

    // -------------------------------------------
    // Add changeset item
    // -------------------------------------------
    final ODataChangeset changeset = streamManager.addChangeset();

    // Update Customer into the changeset
    targetURI = client.newURIBuilder(baseURL).appendEntitySetSegment("ESAllPrim").appendKeySegment(32767);
    final URI editLink = targetURI.build();

    final ClientEntity patch = client.getObjectFactory().newEntity(
        new FullQualifiedName(SERVICE_NAMESPACE, "ETAllPrim"));
    patch.setEditLink(editLink);

    patch.getProperties().add(client.getObjectFactory().newPrimitiveProperty(
        "PropertyString",
        client.getObjectFactory().newPrimitiveValueBuilder().buildString("Test")));

    final ODataEntityUpdateRequest<ClientEntity> changeReq =
        client.getCUDRequestFactory().getEntityUpdateRequest(UpdateType.PATCH, patch);

    changeset.addRequest(changeReq);
    // -------------------------------------------

    final ODataBatchResponse response = streamManager.getResponse();
    assertEquals(200, response.getStatusCode());
  }
  
}
