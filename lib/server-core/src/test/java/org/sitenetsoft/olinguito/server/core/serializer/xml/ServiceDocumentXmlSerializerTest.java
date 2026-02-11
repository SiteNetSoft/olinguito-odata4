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
 * Copyright 2026 SiteNetSoft - Replaced Apache Commons with Java standard library
 */
package org.sitenetsoft.olinguito.server.core.serializer.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.ServiceMetadataImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ServiceDocumentXmlSerializerTest {
  private static ODataSerializer serializer;

  @BeforeAll
  public static void init() throws SerializerException {
    serializer = OData.newInstance().createSerializer(ContentType.APPLICATION_ATOM_XML);
  }

  @Test
  public void writeServiceWithEmptyMockedEdm() throws Exception {
    final Edm edm = mock(Edm.class);
    EdmEntityContainer container = mock(EdmEntityContainer.class);
    when(container.getFullQualifiedName()).thenReturn(new FullQualifiedName("service", "test"));
    when(container.getEntitySets()).thenReturn(Collections.emptyList());
    when(container.getFunctionImports()).thenReturn(Collections.emptyList());
    when(container.getSingletons()).thenReturn(Collections.emptyList());
    when(edm.getEntityContainer()).thenReturn(container);
    ServiceMetadata metadata = mock(ServiceMetadata.class);
    when(metadata.getEdm()).thenReturn(edm);

    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<app:service xmlns:atom=\"http://www.w3.org/2005/Atom\" "
        + "xmlns:app=\"http://www.w3.org/2007/app\" "
        + "xmlns:metadata=\"http://docs.oasis-open.org/odata/ns/metadata\" "
        + "metadata:context=\"http://host/svc/$metadata\">"
        + "<app:workspace><atom:title>service.test</atom:title></app:workspace>"
        + "</app:service>",
        new String(serializer.serviceDocument(metadata, "http://host/svc")
            .getContent().readAllBytes(), StandardCharsets.UTF_8));
  }

  @Test
  public void writeServiceDocument() throws Exception {
    CsdlEdmProvider provider = new MetadataDocumentXmlSerializerTest.LocalProvider();
    ServiceMetadata serviceMetadata = new ServiceMetadataImpl(provider,
        Collections.emptyList(), null);
    InputStream metadataStream = serializer.serviceDocument(serviceMetadata, "http://host/svc").getContent();
    String metadata = new String(metadataStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<app:service xmlns:atom=\"http://www.w3.org/2005/Atom\" "
        + "xmlns:app=\"http://www.w3.org/2007/app\" "
        + "xmlns:metadata=\"http://docs.oasis-open.org/odata/ns/metadata\" "
        + "metadata:context=\"http://host/svc/$metadata\">"
        + "<app:workspace>"
        + "<atom:title>org.olingo.container</atom:title>"
        + "<app:collection href=\"ESAllPrim\" metadata:name=\"ESAllPrim\">"
        + "<atom:title>ESAllPrim</atom:title>"
        + "</app:collection>"
        + "<metadata:function-import href=\"FINRTInt16\" metadata:name=\"FINRTInt16\">"
        + "<atom:title>FINRTInt16</atom:title>"
        + "</metadata:function-import>"
        + "<metadata:function-import href=\"FINRTET\" metadata:name=\"FINRTET\">"
        + "<atom:title>FINRTET</atom:title>"
        + "</metadata:function-import>"
        + "<metadata:singleton href=\"SI\" metadata:name=\"SI\">"
        + "<atom:title>SI</atom:title>"
        + "</metadata:singleton>"
        + "</app:workspace>"
        + "</app:service>",
        metadata);
  }
}
