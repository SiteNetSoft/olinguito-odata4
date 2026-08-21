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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 3: end-to-end test proving
 * ESCollAllPrim/CollPropertyString and ESMixPrimCollComp/CollPropertyComp are served through the
 * streamed serializer path, byte-equal to the buffered path, and that the gate stays narrow
 * Copyright 2026 SiteNetSoft - Pin that non-JSON response formats keep the buffered path
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.server.api.serializer.ComplexSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerResult;
import org.sitenetsoft.olinguito.server.api.uri.UriParameter;
import org.sitenetsoft.olinguito.server.core.ODataHandlerImpl;
import org.sitenetsoft.olinguito.server.core.debug.ServerCoreDebugger;
import org.sitenetsoft.olinguito.server.tecsvc.MetadataETagSupport;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;

/**
 * Drives the real {@link org.sitenetsoft.olinguito.server.core.ODataHandlerImpl} with a real
 * {@link TechnicalPrimitiveComplexProcessor} - the honest test of the wiring, following the idiom
 * {@code ODataHandlerImplTest} uses elsewhere in this codebase - rather than calling the processor's
 * package-private {@code readProperty} directly, since no existing tecsvc test drives the processor
 * that way.
 */
class StreamedCollectionPropertyTest {

  private static final String BASE_URI = "http://localhost/odata/";

  private final OData odata = OData.newInstance();
  private final ServiceMetadata metadata = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList(), new MetadataETagSupport("W/\"metadataETag\""));
  private final Edm edm = metadata.getEdm();
  private final EdmEntityContainer entityContainer = edm.getEntityContainer();
  private final DataProvider dataProvider = new DataProvider(odata, edm);

  private ODataResponse dispatch(final String path) {
    return dispatch(path, null);
  }

  private ODataResponse dispatch(final String path, final String queryPath) {
    final ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.GET);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath(path);
    request.setRawQueryPath(queryPath);
    request.addHeader(HttpHeader.CONTENT_TYPE, Collections.singletonList(ContentType.JSON.toContentTypeString()));

    final ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));
    handler.register(new TechnicalPrimitiveComplexProcessor(dataProvider, metadata));
    return handler.process(request);
  }

  private static byte[] contentOf(final ODataResponse response) throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    response.getODataContent().write(out);
    return out.toByteArray();
  }

  private static UriParameter key(final String value) {
    final UriParameter parameter = Mockito.mock(UriParameter.class);
    Mockito.when(parameter.getName()).thenReturn("PropertyInt16");
    Mockito.when(parameter.getText()).thenReturn(value);
    return parameter;
  }

  @Test
  void collAllPrimStringIsStreamedAndByteEqualToBuffered() throws Exception {
    final ODataResponse response = dispatch("ESCollAllPrim(1)/CollPropertyString");
    assertNotNull(response.getODataContent());
    assertNull(response.getContent());

    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESCollAllPrim");
    final EdmProperty edmProperty = (EdmProperty) edmEntitySet.getEntityType().getProperty("CollPropertyString");
    final Entity entity = dataProvider.read(edmEntitySet, List.of(key("1")));
    final Property property = entity.getProperty("CollPropertyString");
    final String keyPredicate = odata.createUriHelper().buildKeyPredicate(edmEntitySet.getEntityType(), entity);
    final ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet)
        .keyPath(keyPredicate).navOrPropertyPath("CollPropertyString").build();

    final ODataSerializer serializer = odata.createSerializer(ContentType.JSON);
    final SerializerResult buffered = serializer.primitiveCollection(
        metadata, (EdmPrimitiveType) edmProperty.getType(), property,
        PrimitiveSerializerOptions.with().contextURL(contextURL)
            .nullable(edmProperty.isNullable())
            .maxLength(edmProperty.getMaxLength())
            .precision(edmProperty.getPrecision())
            .scale(edmProperty.getScale())
            .unicode(edmProperty.isUnicode())
            .build());

    assertArrayEquals(buffered.getContent().readAllBytes(), contentOf(response));
  }

  @Test
  void collAllPrimStringCountIsNotStreamed() throws Exception {
    final ODataResponse response = dispatch("ESCollAllPrim(1)/CollPropertyString/$count");
    assertNull(response.getODataContent());
    assertNotNull(response.getContent());
  }

  @Test
  void mixPrimCollCompCompIsStreamedAndByteEqualToBuffered() throws Exception {
    final ODataResponse response = dispatch("ESMixPrimCollComp(7)/CollPropertyComp");
    assertNotNull(response.getODataContent());
    assertNull(response.getContent());

    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    final EdmProperty edmProperty = (EdmProperty) edmEntitySet.getEntityType().getProperty("CollPropertyComp");
    final Entity entity = dataProvider.read(edmEntitySet, List.of(key("7")));
    final Property property = entity.getProperty("CollPropertyComp");
    final String keyPredicate = odata.createUriHelper().buildKeyPredicate(edmEntitySet.getEntityType(), entity);
    final ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet)
        .keyPath(keyPredicate).navOrPropertyPath("CollPropertyComp").build();

    final ODataSerializer serializer = odata.createSerializer(ContentType.JSON);
    final SerializerResult buffered = serializer.complexCollection(
        metadata, (EdmComplexType) edmProperty.getType(), property,
        ComplexSerializerOptions.with().contextURL(contextURL).build());

    assertArrayEquals(buffered.getContent().readAllBytes(), contentOf(response));
  }

  @Test
  void collAllPrimStringIsNotStreamedAsXml() throws Exception {
    // regression: the streaming gate ignored the response content type, so the XML serializer -
    // which does not implement the streamed entry points - answered 501 for a URL that served XML
    // before (still pinned by ODataXmlSerializerTest#primitiveCollectionProperty).
    final ODataResponse response = dispatch("ESCollAllPrim(1)/CollPropertyString", "$format=xml");

    assertEquals(200, response.getStatusCode());
    assertNull(response.getODataContent());
    assertNotNull(response.getContent());
    final String body = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(body.contains("Employee1@company.example"), body);
  }

  @Test
  void mixPrimCollCompCompIsNotStreamedAsXml() throws Exception {
    final ODataResponse response = dispatch("ESMixPrimCollComp(7)/CollPropertyComp", "$format=xml");

    assertEquals(200, response.getStatusCode());
    assertNull(response.getODataContent());
    assertNotNull(response.getContent());
    final String body = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(body.contains("PropertyString"), body);
  }

  @Test
  void collAllPrimInt16IsNotStreamed() throws Exception {
    final ODataResponse response = dispatch("ESCollAllPrim(1)/CollPropertyInt16");
    assertNull(response.getODataContent());
    assertNotNull(response.getContent());
  }
}
