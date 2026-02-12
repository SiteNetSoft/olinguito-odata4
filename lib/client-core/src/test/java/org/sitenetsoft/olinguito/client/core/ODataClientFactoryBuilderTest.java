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
 * Copyright 2026 SiteNetSoft - Tests for ODataClientFactory.builder() fluent API
 */
package org.sitenetsoft.olinguito.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.Configuration;
import org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.adapter.okhttp.OkHttpClientFactory;
import org.sitenetsoft.olinguito.client.adapter.okhttp.OkHttpUriRequestFactory;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpClientFactory;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpUriRequestFactory;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

class ODataClientFactoryBuilderTest {

  @Test
  void buildDefault() {
    ODataClient client = ODataClientFactory.builder().build();
    assertNotNull(client);
    assertInstanceOf(ODataClientImpl.class, client);
  }

  @Test
  void buildWithApache() {
    ODataClient client = ODataClientFactory.builder()
        .withApache()
        .build();

    Configuration config = client.getConfiguration();
    assertInstanceOf(DefaultHttpClientFactory.class,
        config.getHttpClientFactory());
    assertInstanceOf(DefaultHttpUriRequestFactory.class,
        config.getHttpUriRequestFactory());
  }

  @Test
  void buildWithOkHttp() {
    ODataClient client = ODataClientFactory.builder()
        .withOkHttp()
        .build();

    Configuration config = client.getConfiguration();
    assertInstanceOf(OkHttpClientFactory.class,
        config.getHttpClientFactory());
    assertInstanceOf(OkHttpUriRequestFactory.class,
        config.getHttpUriRequestFactory());
  }

  @Test
  void buildWithGzipCompression() {
    ODataClient client = ODataClientFactory.builder()
        .gzipCompression(true)
        .build();

    assertTrue(client.getConfiguration().isGzipCompression());
  }

  @Test
  void buildWithDefaultPubFormat() {
    ODataClient client = ODataClientFactory.builder()
        .defaultPubFormat(ContentType.APPLICATION_XML)
        .build();

    assertEquals(ContentType.APPLICATION_XML,
        client.getConfiguration().getDefaultPubFormat());
  }

  @Test
  void buildWithMultipleOptions() {
    ODataClient client = ODataClientFactory.builder()
        .withOkHttp()
        .gzipCompression(true)
        .keyAsSegment(true)
        .useXHttpMethod(true)
        .continueOnError(true)
        .useChunked(false)
        .defaultPubFormat(ContentType.JSON)
        .build();

    Configuration config = client.getConfiguration();
    assertInstanceOf(OkHttpClientFactory.class,
        config.getHttpClientFactory());
    assertTrue(config.isGzipCompression());
    assertTrue(config.isKeyAsSegment());
    assertTrue(config.isUseXHTTPMethod());
    assertTrue(config.isContinueOnError());
    assertEquals(false, config.isUseChuncked());
    assertEquals(ContentType.JSON, config.getDefaultPubFormat());
  }

  @Test
  void buildEdmEnabled() {
    EdmEnabledODataClient client = ODataClientFactory.builder()
        .withOkHttp()
        .gzipCompression(true)
        .buildEdmEnabled("http://example.com/odata");

    assertNotNull(client);
    assertInstanceOf(EdmEnabledODataClientImpl.class, client);
    assertInstanceOf(OkHttpClientFactory.class,
        client.getConfiguration().getHttpClientFactory());
    assertTrue(client.getConfiguration().isGzipCompression());
    assertEquals("http://example.com/odata", client.getServiceRoot());
  }

  @Test
  void buildWithCustomFactories() {
    OkHttpClientFactory myFactory = new OkHttpClientFactory();
    OkHttpUriRequestFactory myReqFactory = new OkHttpUriRequestFactory();

    ODataClient client = ODataClientFactory.builder()
        .httpClientFactory(myFactory)
        .httpUriRequestFactory(myReqFactory)
        .build();

    Configuration config = client.getConfiguration();
    assertEquals(myFactory, config.getHttpClientFactory());
    assertEquals(myReqFactory, config.getHttpUriRequestFactory());
  }

  @Test
  void buildWithDefaultFormats() {
    ODataClient client = ODataClientFactory.builder()
        .defaultValueFormat(ContentType.TEXT_PLAIN)
        .defaultBatchAcceptFormat(ContentType.MULTIPART_MIXED)
        .build();

    Configuration config = client.getConfiguration();
    assertEquals(ContentType.TEXT_PLAIN,
        config.getDefaultValueFormat());
    assertEquals(ContentType.MULTIPART_MIXED,
        config.getDefaultBatchAcceptFormat());
  }
}
