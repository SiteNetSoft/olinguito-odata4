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
package org.sitenetsoft.olinguito.server.core.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.junit.jupiter.api.Test;

public class DebugTabBodyTest extends AbstractDebugTabTest {

  @Test
  public void nullResponseMustNotLeadToException() throws Exception {
    DebugTabBody tab = new DebugTabBody(null);

    assertEquals("null", createJson(tab));
    assertEquals("<pre class=\"code\">\nODataLibrary: No body.\n</pre>\n", createHtml(tab));
  }

  @Test
  public void json() throws Exception {
    ODataResponse response = new ODataResponse();
    response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.JSON_NO_METADATA.toContentTypeString());
    response.setContent(new ByteArrayInputStream("{\"property\": true}".getBytes(StandardCharsets.UTF_8)));
    assertEquals("\"{\\\"property\\\": true}\"", createJson(new DebugTabBody(response)));

    response.setContent(new ByteArrayInputStream("{\"property\": false}".getBytes(StandardCharsets.UTF_8)));
    assertEquals("<pre class=\"code json\">\n{\"property\": false}\n</pre>\n", createHtml(new DebugTabBody(response)));
  }

  @Test
  public void xml() throws Exception {
    ODataResponse response = new ODataResponse();
    response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_XML.toContentTypeString());
    response.setContent(new ByteArrayInputStream("<?xml version='1.1'?>\n<a xmlns=\"b\" />\n".getBytes(StandardCharsets.UTF_8)));
    assertEquals("\"<?xml version='1.1'?>\\n<a xmlns=\\\"b\\\" />\\n\"", createJson(new DebugTabBody(response)));

    response.setContent(new ByteArrayInputStream("<?xml version='1.1'?>\n<c xmlns=\"d\" />\n".getBytes(StandardCharsets.UTF_8)));
    assertEquals("<pre class=\"code xml\">\n&lt;?xml version='1.1'?&gt;\n&lt;c xmlns=\"d\" /&gt;\n\n</pre>\n",
        createHtml(new DebugTabBody(response)));
  }

  @Test
  public void text() throws Exception {
    ODataResponse response = new ODataResponse();
    response.setContent(new ByteArrayInputStream("testText\n12".getBytes(StandardCharsets.UTF_8)));
    assertEquals("\"testText\\n12\"", createJson(new DebugTabBody(response)));

    response.setContent(new ByteArrayInputStream("testText\n34".getBytes(StandardCharsets.UTF_8)));
    assertEquals("<pre class=\"code\">\ntestText\n34\n</pre>\n", createHtml(new DebugTabBody(response)));
  }

  @Test
  public void image() throws Exception {
    ODataResponse response = new ODataResponse();
    response.setHeader(HttpHeader.CONTENT_TYPE, "image/png");
    response.setContent(new ByteArrayInputStream(new byte[] { -1, -2, -3, -4 }));
    assertEquals("\"//79/A==\"", createJson(new DebugTabBody(response)));

    response.setContent(new ByteArrayInputStream(new byte[] { -5, -6, -7, -8 }));
    assertEquals("<img src=\"data:image/png;base64,+/r5+A==\" />\n", createHtml(new DebugTabBody(response)));
  }

  @Test
  public void streamError() throws Exception {
    ODataResponse response = new ODataResponse();
    InputStream input = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("test");
      }
    };
    response.setContent(input);
    assertEquals("\"Could not parse Body for Debug Output\"", createJson(new DebugTabBody(response)));
    assertEquals("<pre class=\"code\">\nCould not parse Body for Debug Output\n</pre>\n",
        createHtml(new DebugTabBody(response)));
  }
}
