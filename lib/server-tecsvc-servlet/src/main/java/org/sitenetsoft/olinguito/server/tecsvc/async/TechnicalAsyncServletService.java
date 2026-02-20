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
 * Copyright 2026 SiteNetSoft - Servlet-dependent async methods extracted from TechnicalAsyncService
 */
package org.sitenetsoft.olinguito.server.tecsvc.async;

import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

/**
 * Servlet-dependent async support methods, extracted from {@link TechnicalAsyncService}.
 * Delegates to {@link TechnicalAsyncService#getInstance()} for async runner access.
 */
public class TechnicalAsyncServletService {
  private static final int COPY_BUFFER_SIZE = 8192;

  private final TechnicalAsyncService asyncService;

  public TechnicalAsyncServletService() {
    this.asyncService = TechnicalAsyncService.getInstance();
  }

  public boolean isStatusMonitorResource(HttpServletRequest request) {
    return request.getRequestURL() != null
        && request.getRequestURL().toString().contains(TechnicalAsyncService.STATUS_MONITOR_TOKEN);
  }

  public void handle(HttpServletRequest request, HttpServletResponse response) throws SerializerException, IOException {
    String location = getAsyncLocation(request);
    TechnicalAsyncService.AsyncRunner runner = asyncService.getRunner(location);

    if (runner == null) {
      response.setStatus(HttpStatusCode.NOT_FOUND.getStatusCode());
    } else {
      if (runner.isFinished()) {
        ODataResponse wrapResult = runner.getDispatched().getProcessResponse();
        wrapToAsyncHttpResponse(wrapResult, response);
        asyncService.removeRunner(location);
      } else {
        response.setStatus(HttpStatusCode.ACCEPTED.getStatusCode());
        response.setHeader(HttpHeader.LOCATION, location);
      }
    }
  }

  public void listQueue(HttpServletResponse response) {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><header/><body><h1>Queued requests</h1><ul>");
    for (Map.Entry<String, TechnicalAsyncService.AsyncRunner> entry : asyncService.getRunners().entrySet()) {
      AsyncProcessor<?> asyncProcessor = entry.getValue().getDispatched();
      sb.append("<li><b>ID: </b>").append(entry.getKey()).append("<br/>")
          .append("<b>Location: </b><a href=\"")
          .append(asyncProcessor.getLocation()).append("\">")
          .append(asyncProcessor.getLocation()).append("</a><br/>")
          .append("<b>Processor: </b>").append(asyncProcessor.getProcessorClass().getSimpleName()).append("<br/>")
          .append("<b>Finished: </b>").append(entry.getValue().isFinished()).append("<br/>")
          .append("</li>");
    }
    sb.append("</ul></body></html>");

    writeToResponse(response, sb.toString());
  }

  private static void writeToResponse(HttpServletResponse response, InputStream input) throws IOException {
    copy(input, response.getOutputStream());
  }

  private void writeToResponse(HttpServletResponse response, String content) {
    writeToResponse(response, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static void writeToResponse(HttpServletResponse response, byte[] content) {
    OutputStream output = null;
    try {
      output = response.getOutputStream();
      output.write(content);
    } catch (IOException e) {
      throw new ODataRuntimeException(e);
    } finally {
      closeStream(output);
    }
  }

  static void wrapToAsyncHttpResponse(final ODataResponse odResponse, final HttpServletResponse response)
      throws SerializerException, IOException {
    OData odata = OData.newInstance();
    InputStream odResponseStream = odata.createFixedFormatSerializer().asyncResponse(odResponse);

    response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_HTTP.toContentTypeString());
    response.setHeader(HttpHeader.CONTENT_ENCODING, "binary");
    response.setStatus(HttpStatusCode.OK.getStatusCode());

    writeToResponse(response, odResponseStream);
  }

  private static void copy(final InputStream input, final OutputStream output) {
    if (output == null || input == null) {
      return;
    }

    try (ReadableByteChannel ic = Channels.newChannel(input);
         WritableByteChannel oc = Channels.newChannel(output)) {
      ByteBuffer inBuffer = ByteBuffer.allocate(COPY_BUFFER_SIZE);
      while (ic.read(inBuffer) > 0) {
        inBuffer.flip();
        oc.write(inBuffer);
        inBuffer.rewind();
      }
    } catch (IOException e) {
      throw new ODataRuntimeException("Error on reading request content");
    }
  }

  private static void closeStream(final Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String getAsyncLocation(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }
}
