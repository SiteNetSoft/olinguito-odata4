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
 * Copyright 2026 SiteNetSoft - Fixed deprecated API usages and code quality warnings
 */
package org.sitenetsoft.olinguito.server.core.serializer.json;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.etag.ServiceMetadataETagSupport;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Test;

public class ServiceDocumentTest {

  private static final String serviceRoot = "http://localhost:8080/odata.svc";
  private static final ServiceMetadata metadata = OData.newInstance().createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList(),
      new ServiceMetadataETagSupport() {
        @Override
        public String getServiceDocumentETag() {
          return "W/\"serviceDocumentETag\"";
        }
        @Override
        public String getMetadataETag() {
          return "W/\"metadataETag\"";
        }
      });

  @Test
  public void writeServiceDocumentJson() throws Exception {
    OData server = OData.newInstance();
    assertNotNull(server);

    ODataSerializer serializer = server.createSerializer(ContentType.JSON);
    assertNotNull(serializer);

    InputStream result = serializer.serviceDocument(metadata, serviceRoot).getContent();
    assertNotNull(result);
    final String jsonString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(jsonString.contains(
        metadata.getServiceMetadataETagSupport().getMetadataETag().replace("\"", "\\\"")));

    assertTrue(jsonString.contains("ESAllPrim"));
    assertTrue(jsonString.contains("All PropertyTypes EntitySet"));
    assertTrue(jsonString.contains("ESCollAllPrim"));
    assertTrue(jsonString.contains("ESKeyNavCont"));
    assertFalse(jsonString.contains("ESInvisible"));

    assertTrue(jsonString.contains("FINRTInt16"));
    assertTrue(jsonString.contains("Simple FunctionImport"));
    assertTrue(jsonString.contains("FINRTCollETMixPrimCollCompTwoParam"));
    assertTrue(jsonString.contains("FICRTCollESKeyNavContParam"));
    assertFalse(jsonString.contains("FINInvisibleRTInt16"));
    assertTrue(jsonString.contains("FunctionImport"));

    assertTrue(jsonString.contains("SI"));
    assertTrue(jsonString.contains("Simple Singleton"));
    assertTrue(jsonString.contains("SINav"));
    assertTrue(jsonString.contains("SIMedia"));
    assertTrue(jsonString.contains("Singleton"));
  }

  @Test
  public void serviceDocumentNoMetadata() throws Exception {
    final String result = new String(
        OData.newInstance().createSerializer(ContentType.JSON_NO_METADATA)
            .serviceDocument(metadata, serviceRoot).getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertFalse(result.contains("odata.context"));
    assertFalse(result.contains("odata.metadata"));
    assertTrue(result.contains("ESAllPrim"));
  }
}
