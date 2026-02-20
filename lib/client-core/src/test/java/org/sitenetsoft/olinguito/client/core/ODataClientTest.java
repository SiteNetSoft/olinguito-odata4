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
 * Copyright 2026 SiteNetSoft - Improved test assertions
 */
package org.sitenetsoft.olinguito.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.uri.SearchFactory;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.junit.jupiter.api.Test;

public class ODataClientTest {

  @Test
  public void before() {
    ODataClient client = ODataClientFactory.getClient();
    assertNotNull(client);
    assertEquals(ODataServiceVersion.V40, client.getServiceVersion());
  }
  
  @Test
  public void clientImplTest() {
    ODataClientImpl client = (ODataClientImpl) ODataClientFactory.getClient();
    assertNotNull(client);
    assertNotNull(client.newPreferences());
    assertNotNull(client.getAsyncRequestFactory());
    assertNotNull(client.getRetrieveRequestFactory());
    assertNotNull(client.getCUDRequestFactory());
    assertNotNull(client.getInvokeRequestFactory());
    assertNotNull(client.getBatchRequestFactory());
    assertEquals(ODataServiceVersion.V40, client.getServiceVersion());
  }
  
  @Test
  public void clientFactoryTest() {
    assertNotNull(ODataClientFactory.getClient());
    assertNotNull(ODataClientFactory.getEdmEnabledClient(null));
    assertNotNull(ODataClientFactory.getEdmEnabledClient(null, null));
    assertNotNull(ODataClientFactory.getEdmEnabledClient(null, null, null));
    assertNotNull(ODataClientFactory.getEdmEnabledClient(null, null, null, null));
  }
  
  @Test
  public void searchTest() {
    ODataClient client = ODataClientFactory.getClient();
    assertNotNull(client);
    SearchFactory searchFactory = client.getSearchFactory();
    assertNotNull(searchFactory);
    LiteralSearch literal = (LiteralSearch) searchFactory.literal("test");
    assertNotNull(literal);
    assertEquals("test", literal.build());
    AndSearch and = (AndSearch) searchFactory.and(literal, literal);
    assertNotNull(and);
    assertEquals("(test AND test)", and.build());
    OrSearch or = (OrSearch) searchFactory.or(literal, literal);
    assertNotNull(or);
    assertEquals("(test OR test)", or.build());
    NotSearch not = (NotSearch) searchFactory.not(literal);
    assertNotNull(not);
    assertEquals("NOT (test)", not.build());
  }
  
  @Test
  public void configurationTest() {
    ODataClient client = ODataClientFactory.getClient();
    assertNotNull(client);
    ConfigurationImpl config = (ConfigurationImpl) client.getConfiguration();
    assertNotNull(config);
    assertNotNull(config.getDefaultBatchAcceptFormat());
    assertNotNull(config.getDefaultFormat());
    assertNotNull(config.getDefaultMediaFormat());
    assertNotNull(config.getDefaultPubFormat());
    assertNotNull(config.getDefaultValueFormat());
    assertNotNull(config.getExecutor());
    assertNotNull(config.getHttpClientFactory());
    assertNotNull(config.getHttpUriRequestFactory());
    config.setAddressingDerivedTypes(true);
    assertTrue(config.isAddressingDerivedTypes());
    config.setContinueOnError(true);
    assertTrue(config.isContinueOnError());
    config.setGzipCompression(true);
    assertTrue(config.isGzipCompression());
    config.setKeyAsSegment(true);
    assertTrue(config.isKeyAsSegment());
    config.setUseChuncked(true);
    assertTrue(config.isUseChuncked());
    config.setUseUrlOperationFQN(true);
    assertTrue(config.isUseUrlOperationFQN());
    config.setUseXHTTPMethod(true);
    assertTrue(config.isUseXHTTPMethod());
    config.setDefaultBatchAcceptFormat(ContentType.APPLICATION_ATOM_SVC);
    assertEquals(ContentType.APPLICATION_ATOM_SVC, config.getDefaultBatchAcceptFormat());
    config.setDefaultMediaFormat(ContentType.APPLICATION_ATOM_SVC);
    assertEquals(ContentType.APPLICATION_ATOM_SVC, config.getDefaultMediaFormat());
    config.setDefaultPubFormat(ContentType.APPLICATION_ATOM_SVC);
    assertEquals(ContentType.APPLICATION_ATOM_SVC, config.getDefaultPubFormat());
    config.setDefaultValueFormat(ContentType.APPLICATION_ATOM_SVC);
    assertEquals(ContentType.APPLICATION_ATOM_SVC, config.getDefaultValueFormat());
    config.setExecutor(null);
    assertNull(config.getExecutor());
    config.setHttpClientFactory(null);
    assertNull(config.getHttpClientFactory());
    config.setHttpUriRequestFactory(null);
    assertNull(config.getHttpUriRequestFactory());
    config.setProperty("key", "value");
    assertEquals("value", config.getProperty("key", "value"));
  }
}
