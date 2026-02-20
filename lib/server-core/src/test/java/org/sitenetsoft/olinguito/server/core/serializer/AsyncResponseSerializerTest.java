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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import java.io.ByteArrayInputStream;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.junit.jupiter.api.Test;

class AsyncResponseSerializerTest {
  private static final String CRLF = "\r\n";

  @Test
  void simpleResponse() throws Exception {
    ODataResponse response = new ODataResponse();
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.toContentTypeString());
    response.setHeader(HttpHeader.CONTENT_LENGTH, String.valueOf(200));

    response.setContent(new ByteArrayInputStream(("Walter Winter" + CRLF).getBytes(StandardCharsets.UTF_8)));

    AsyncResponseSerializer serializer = new AsyncResponseSerializer();
    InputStream in = serializer.serialize(response);
    String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals("HTTP/1.1 200 OK" + CRLF
        + "Content-Type: application/json" + CRLF
        + "Content-Length: 200" + CRLF + CRLF
        + "Walter Winter" + CRLF, result);
  }

  @Test
  void biggerResponse() throws Exception {
    ODataResponse response = new ODataResponse();
    response.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.toContentTypeString());
    response.setHeader(HttpHeader.CONTENT_LENGTH, String.valueOf(0));

    String testData = testData();
    response.setContent(new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8)));

    AsyncResponseSerializer serializer = new AsyncResponseSerializer();
    InputStream in = serializer.serialize(response);
    String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals("HTTP/1.1 202 Accepted" + CRLF
        + "Content-Type: application/json" + CRLF
        + "Content-Length: 0" + CRLF + CRLF
        + testData, result);
  }

  private String testData() {
    StringBuilder result = new StringBuilder();
    Random r = new Random();
    for (int i = 0; i < 20000; i++) {
      result.append((char) (r.nextInt(26) + 'a'));
    }

    return result.toString();
  }
}
