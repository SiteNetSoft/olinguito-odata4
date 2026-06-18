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
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataRawRequest;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.client.core.ODataClientImpl;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.junit.jupiter.api.Test;

class RetrieveRequestTest {

  @Test
  void testEdmMetadata() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    EdmMetadataRequestImpl edmMetadata = (EdmMetadataRequestImpl) factory
        .getMetadataRequest("metadata");
    assertNotNull(edmMetadata);
    assertNotNull(edmMetadata.addCustomHeader("name", "value"));
    assertNotNull(edmMetadata.getDefaultFormat());
    assertNotNull(edmMetadata.setAccept(ContentType.VALUE_ODATA_METADATA_FULL));
    assertNotNull(edmMetadata.setContentType(ContentType.VALUE_ODATA_METADATA_FULL));
  }
  
  @Test
  void testXmlMetadata() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    XMLMetadataRequestImpl metadata = (XMLMetadataRequestImpl) factory
        .getXMLMetadataRequest("test");
    assertNotNull(metadata);
  }
  
  @Test
  void testDeltaRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataDeltaRequestImpl delta = (ODataDeltaRequestImpl) factory
        .getDeltaRequest(uri);
    assertNotNull(delta);
    assertNotNull(delta.getDefaultFormat());
   }
  
  @Test
  void testEntityRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataEntityRequestImpl req = (ODataEntityRequestImpl) factory
        .getEntityRequest(uri);
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }
  
  @Test
  void testEntitySetIteratorRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataEntitySetIteratorRequestImpl req = (ODataEntitySetIteratorRequestImpl) factory
        .getEntitySetIteratorRequest(uri);
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }
  
  @Test
  void testEntitySetRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataEntitySetRequestImpl req = (ODataEntitySetRequestImpl) factory
        .getEntitySetRequest(uri);
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }
  
  @Test
  void testMediaRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost","8080","","","$value");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataMediaRequestImpl req = (ODataMediaRequestImpl) factory
        .getMediaEntityRequest(uri);
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }
  
  @Test
  void testPropertyRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataPropertyRequestImpl req = (ODataPropertyRequestImpl) factory
        .getPropertyRequest(uri);
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }
  
  @Test
  void testRawRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataRawRequestImpl req = (ODataRawRequestImpl) factory
        .getRawRequest(uri);
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }

  @Test
  void testRawRequestRemoveAcceptSuppressesHeader() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("http://localhost:8080/svc");
    ODataRawRequest req = client.getRetrieveRequestFactory().getRawRequest(uri);

    // By default an Accept header is injected when none was set explicitly.
    assertTrue(new String(req.toByteArray(), StandardCharsets.UTF_8).contains("Accept:"));

    // After removeAccept() the request carries no Accept header.
    req.removeAccept();
    assertFalse(new String(req.toByteArray(), StandardCharsets.UTF_8).contains("Accept:"));

    // An explicit Accept re-enables the header.
    req.setAccept(ContentType.JSON.toContentTypeString());
    assertTrue(new String(req.toByteArray(), StandardCharsets.UTF_8).contains("Accept:"));
   }

  @Test
  void testServiceDocumentRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataServiceDocumentRequestImpl req = (ODataServiceDocumentRequestImpl) factory
        .getServiceDocumentRequest("doc");
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }
  
  @Test
  void testValueRequest() throws URISyntaxException {

    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    RetrieveRequestFactoryImpl factory = (RetrieveRequestFactoryImpl) client
        .getRetrieveRequestFactory();
    assertNotNull(factory);
    ODataValueRequestImpl req = (ODataValueRequestImpl) factory
        .getValueRequest(uri);
    assertNotNull(req);
    assertNotNull(req.getDefaultFormat());
   }
  
}
