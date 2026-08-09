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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1240: batch POST item trailing CRLF regression test
 */
package org.sitenetsoft.olinguito.client.core.communication.request.batch;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityCreateRequest;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.core.AbstractTest;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.junit.jupiter.api.Test;

class BatchItemTrailingCrlfTest extends AbstractTest {

  @Test
  void postItemBodyEndsWithCrlf() throws IOException {
    final ClientEntity entity = client.getObjectFactory()
        .newEntity(new FullQualifiedName("Namespace", "EntityType"));
    entity.getProperties().add(client.getObjectFactory().newPrimitiveProperty("Name",
        client.getObjectFactory().newPrimitiveValueBuilder().buildString("Value")));

    final ODataEntityCreateRequest<ClientEntity> createRequest = client.getCUDRequestFactory()
        .getEntityCreateRequest(URI.create("http://host/service/Entities"), entity);

    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    final ODataBatchRequest batchRequest = mock(ODataBatchRequest.class);
    when(batchRequest.rawAppend(any(byte[].class))).thenAnswer(invocation -> {
      captured.write((byte[]) invocation.getArgument(0));
      return batchRequest;
    });

    createRequest.batch(batchRequest);

    final String serialized = captured.toString(StandardCharsets.UTF_8);
    assertTrue(serialized.contains("POST"), "serialized item must contain the request line");
    assertTrue(serialized.contains("\"Name\""), "serialized item must contain the JSON payload");
    assertTrue(serialized.endsWith("\r\n"),
        "OLINGO-1240: item body must end with CRLF before the boundary delimiter");
  }
}
