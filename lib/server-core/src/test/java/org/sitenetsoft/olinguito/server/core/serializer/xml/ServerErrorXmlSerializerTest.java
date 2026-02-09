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
 */
package org.sitenetsoft.olinguito.server.core.serializer.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.sitenetsoft.olinguito.commons.api.ex.ODataErrorDetail;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.junit.jupiter.api.Test;

public class ServerErrorXmlSerializerTest {

  final ODataSerializer ser;

  public ServerErrorXmlSerializerTest() throws SerializerException {
    ser = OData.newInstance().createSerializer(ContentType.APPLICATION_XML);
  }

  @Test
  public void basicODataErrorWithCode() throws Exception {
    ODataServerError error = new ODataServerError();
    error.setCode("Code").setMessage("ErrorMessage");
    InputStream stream = ser.error(error).getContent();
    String jsonString = IOUtils.toString(stream, StandardCharsets.UTF_8);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<error xmlns=\"http://docs.oasis-open.org/odata/ns/metadata\">"
        + "<code>Code</code>"
        + "<message>ErrorMessage</message>"
        + "</error>",
        jsonString);
  }

  @Test
  public void nullErrorResultsInException() throws Exception {
      assertThrows(SerializerException.class, () -> ser.error(null));
  }

  @Test
  public void singleDetailNothingSet() throws Exception {
    ODataServerError error = new ODataServerError()
    .setCode("code")
    .setMessage("err message")
    .setTarget("target")
    .setDetails(Collections.singletonList(
        new ODataErrorDetail()
        .setCode("detail code")
        .setMessage("detail message")));

    InputStream stream = ser.error(error).getContent();
    String jsonString = IOUtils.toString(stream, StandardCharsets.UTF_8);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<error xmlns=\"http://docs.oasis-open.org/odata/ns/metadata\">"
        + "<code>code</code>"
        + "<message>err message</message>"
        + "<target>target</target>"
        + "<details>"
        + "<code>detail code</code>"
        + "<message>detail message</message>"
        + "</details>"
        + "</error>",
        jsonString);
  }
}
