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
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.client.core;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.communication.ODataServerErrorException;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.client.core.communication.header.ODataErrorResponseChecker;
import org.sitenetsoft.olinguito.commons.api.ex.ODataError;
import org.sitenetsoft.olinguito.commons.api.ex.ODataErrorDetail;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorTest extends AbstractTest {

  private ODataError error(final String name, final ContentType contentType) throws ODataDeserializerException {
    final ODataError error = client.getDeserializer(contentType).toError(
            getClass().getResourceAsStream(name + "." + getSuffix(contentType)));
    assertNotNull(error);
    return error;
  }

  private ODataError simple(final ContentType contentType) throws ODataDeserializerException {
    final ODataError error = error("error", contentType);
    assertEquals("501", error.getCode());
    assertEquals("Unsupported functionality", error.getMessage());
    assertEquals("query", error.getTarget());

    // verify details
    final ODataErrorDetail detail = error.getDetails().get(0);
    assertEquals("301", detail.getCode(), "Code should be correct");
    assertEquals("$search", detail.getTarget(), "Target should be correct");
    assertEquals("$search query option not supported", detail.getMessage(), "Message should be correct");
    return error;
  }

  @Test
  void jsonSimple() throws Exception {
    final ODataError error = simple(ContentType.JSON);

    // verify inner error dictionary
    final Map<String, String> innerErr = error.getInnerError();
    assertEquals(2, innerErr.size(), "innerError dictionary size should be correct");
    assertEquals("{\"key1\":\"for debug deployment only\"}", innerErr.get("context"),
        "innerError['context'] should be correct");
    assertEquals("[\"callmethod1 etc\",\"callmethod2 etc\"]", innerErr.get("trace"),
        "innerError['trace'] should be correct");
  }

  @Test
  void atomSimple() throws Exception {
    simple(ContentType.APPLICATION_ATOM_XML);
  }

  @Test
  void test1OLINGO1102() throws Exception {
    ODataClient odataClient = ODataClientFactory.getClient();
    InputStream entity = getClass().getResourceAsStream("500error." + getSuffix(ContentType.JSON));

    ODataClientErrorException exp = (ODataClientErrorException) ODataErrorResponseChecker.
        checkResponse(odataClient, 500, "Internal Server Error", entity, ContentType.JSON);
    assertTrue(exp.getMessage().contains("(500)"));
    assertTrue(exp.getMessage().contains("Internal Server Error"));
    ODataError error = exp.getODataError();
    assertTrue(error.getMessage().startsWith("Internal Server Error"));
    assertEquals(500, Integer.parseInt(error.getCode()));
    assertEquals(2, error.getInnerError().size());
    assertEquals("\"Method does not support entities of specific type\"", error.getInnerError().get("message"));
    assertEquals("\"FaultException\"", error.getInnerError().get("type"));
    assertNull(error.getDetails());

  }

  @Test
  void test2OLINGO1102() throws Exception {
    ODataClient odataClient = ODataClientFactory.getClient();
    InputStream entity = getClass().getResourceAsStream("500error1." + getSuffix(ContentType.JSON));

    ODataServerErrorException exp = (ODataServerErrorException) ODataErrorResponseChecker.
        checkResponse(odataClient, 500, "Internal Server Error", entity, ContentType.JSON);
    assertTrue(exp.getMessage().contains("Internal Server Error"));
  }

  @Test
  void testWithNull() throws Exception {
    ODataClient odataClient = ODataClientFactory.getClient();

    ODataRuntimeException exp = ODataErrorResponseChecker.
        checkResponse(odataClient, 500, "Internal Server Error", null, ContentType.JSON);
    assertTrue(exp.getMessage().contains("Internal Server Error"));
  }

  @Test
  void testExpTextMsg403() throws Exception {
    ODataClient odataClient = ODataClientFactory.getClient();
    InputStream entity = new ByteArrayInputStream("CSRF Validation Exception".getBytes());

    ODataClientErrorException exp = (ODataClientErrorException) ODataErrorResponseChecker.
        checkResponse(odataClient, 403, "Forbidden", entity, ContentType.TEXT_PLAIN);
    assertEquals(403, exp.getStatusCode());
    ODataError error = exp.getODataError();
    assertEquals("CSRF Validation Exception", error.getMessage());
    assertEquals("403", error.getCode());
    assertEquals("Forbidden", error.getTarget());
    assertNull(exp.getHeaderInfo());
  }
}
