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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.client.core.communication.request.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import org.sitenetsoft.olinguito.client.api.ODataBatchConstants;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.ODataClientBuilder;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataBatchableRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchLineIterator;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchRequest;
import org.sitenetsoft.olinguito.client.api.domain.ClientInvokeResult;
import org.sitenetsoft.olinguito.client.core.communication.request.invoke.ODataInvokeRequestImpl;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.junit.jupiter.api.Test;

class ODataBatchUtilitiesTest {
  
  @Test
  void testBatchRequest(){
    ODataBatchUtilities util = new ODataBatchUtilities();
    Map<String, Collection<String>> value = new HashMap<>();
    util.addHeaderLine("header:name", value );
    value.put("header:name", null);
    util.addHeaderLine("header:name", value );
  }
  
  @Test
  void testBatchConstants(){
    assertEquals("boundary", ODataBatchConstants.BOUNDARY);
    assertEquals("application/http", ODataBatchConstants.ITEM_CONTENT_TYPE );
    assertEquals("Content-Type: application/http", ODataBatchConstants.ITEM_CONTENT_TYPE_LINE );
    assertEquals("Content-Transfer-Encoding: binary", 
        ODataBatchConstants.ITEM_TRANSFER_ENCODING_LINE );
    assertEquals("Content-ID", ODataBatchConstants.CHANGESET_CONTENT_ID_NAME );
  }
  
 
  @Test
  void testChangeSet() throws URISyntaxException{
    ODataClient client = ODataClientBuilder.createClient();
    URI uri = new URI("test");
    final InputStream input = getClass().getResourceAsStream("batchResponse.batch");
    Reader reader = new InputStreamReader(input);
    ODataBatchLineIterator iterator = new ODataBatchLineIteratorImpl(new BufferedReader(reader));
    ODataBatchRequest req = new ODataBatchRequestImpl(client, uri);;
    ODataChangesetResponseItem expectedResItem = new ODataChangesetResponseItem(true);
    ODataChangesetImpl change = new ODataChangesetImpl(req , expectedResItem, new ODataBatchRequestContext());
    assertNotNull(change);
    ODataBatchableRequest request = new ODataInvokeRequestImpl<ClientInvokeResult>(
        client, ClientInvokeResult.class, HttpMethod.POST, uri);
    change.addRequest(request);
    assertNotNull(change.getBodyStreamWriter());
    change.close();
    change.closeItem();
    assertNotNull(change.getLastContentId());
    assertTrue(change.hasStreamedSomething());
    assertFalse(change.isOpen());
    change.streamRequestHeader(request);
    change.streamRequestHeader("1");
    
  }
  
  @Test
  void testChangeSetNeg() throws URISyntaxException{
      assertThrows(IllegalArgumentException.class, () -> {
          ODataClient client = ODataClientBuilder.createClient();
          URI uri = new URI("test");
          ODataBatchRequest req = new ODataBatchRequestImpl(client, uri);;
          ODataChangesetResponseItem expectedResItem = new ODataChangesetResponseItem(true);
          ODataChangesetImpl change = new ODataChangesetImpl(req , expectedResItem, new ODataBatchRequestContext());
          assertNotNull(change);
          ODataBatchableRequest request = new ODataInvokeRequestImpl<ClientInvokeResult>(
          client, ClientInvokeResult.class, HttpMethod.GET, uri);
          change.addRequest(request);
          assertNotNull(change.getBodyStreamWriter());
          change.close();
          change.closeItem();
      });
  }
  
  @Test
  void testChangeSetCloseNeg() throws URISyntaxException{
      assertThrows(IllegalStateException.class, () -> {
          ODataClient client = ODataClientBuilder.createClient();
          URI uri = new URI("test");
          ODataBatchRequest req = new ODataBatchRequestImpl(client, uri);;
          ODataChangesetResponseItem expectedResItem = new ODataChangesetResponseItem(true);
          ODataChangesetImpl change = new ODataChangesetImpl(req , expectedResItem, new ODataBatchRequestContext());
          assertNotNull(change);
          assertNotNull(change.getBodyStreamWriter());
          change.close();
          change.closeItem();
          ODataBatchableRequest request = new ODataInvokeRequestImpl<ClientInvokeResult>(
          client, ClientInvokeResult.class, HttpMethod.POST, uri);
          change.addRequest(request);
      });
  }
  
  @Test
  void testChangeSetResponse() throws URISyntaxException{
    ODataChangesetResponseItem expectedResItem = new ODataChangesetResponseItem(true);
    expectedResItem.setUnexpected();
    assertNotNull(expectedResItem);
    expectedResItem.close();
  }
}
