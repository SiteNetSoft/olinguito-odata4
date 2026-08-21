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
 * Copyright 2026 SiteNetSoft - Fixed logger class, deprecated API usages, and code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Fixed resource leak in initFromEnclosedPart
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 11: read the AsyncResult status of an unwrapped async result
 * Copyright 2026 SiteNetSoft - Drain response body on close to prevent connection leaks (OLINGO-1621/1622)
 */
package org.sitenetsoft.olinguito.client.core.communication.response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchLineIterator;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.api.http.NoContentException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.ConfigurationImpl;
import org.sitenetsoft.olinguito.client.core.communication.util.PipedInputStream;
import org.sitenetsoft.olinguito.client.core.communication.util.PipedOutputStream;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.ODataBatchController;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.ODataBatchLineIteratorImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.ODataBatchUtilities;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract representation of an OData response.
 */
public abstract class AbstractODataResponse implements ODataResponse {

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractODataResponse.class);

  protected final ODataClient odataClient;

  private static final byte[] CRLF = {13, 10};

  /**
   * HTTP client.
   */
  protected final ODataHttpClient httpClient;

  /**
   * HTTP response.
   */
  protected final ODataHttpResponse res;

  /**
   * Response headers.
   */
  protected final Map<String, Collection<String>> headers =
      new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  /**
   * Response code.
   */
  protected int statusCode = -1;

  /**
   * Response message.
   */
  protected String statusMessage = null;

  /**
   * Response body/payload.
   */
  protected InputStream payload = null;

  /**
   * Initialization check.
   */
  protected boolean hasBeenInitialized = false;

  /**
   * Batch info (if to be batched).
   */
  protected ODataBatchController batchInfo = null;

  private byte[] inputContent = null;

  public AbstractODataResponse(
      final ODataClient odataClient, final ODataHttpClient httpclient, final ODataHttpResponse res) {

    this.odataClient = odataClient;
    this.httpClient = httpclient;
    this.res = res;
    if (res != null) {
      initFromHttpResponse(res);
    }
  }

  @Override
  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  @Override
  public Collection<String> getHeader(final String name) {
    return headers.get(name);
  }

  @Override
  public String getETag() {
    final Collection<String> etag = getHeader(HttpHeader.ETAG);
    return etag == null || etag.isEmpty() ? null : etag.iterator().next();
  }

  @Override
  public String getContentType() {
    final Collection<String> contentTypes = getHeader(HttpHeader.CONTENT_TYPE);
    return contentTypes == null || contentTypes.isEmpty() ? null : contentTypes.iterator().next();
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public String getStatusMessage() {
    return statusMessage;
  }

  @Override
  public final ODataResponse initFromHttpResponse(final ODataHttpResponse res) {
    try {
      this.payload = res.getBody();
      this.inputContent = null;
    } catch (final IllegalStateException e) {
      try {
        res.close();
      } catch (IOException ioe) {
        LOG.warn("Error closing response", ioe);
      }
      LOG.error("Error retrieving payload", e);
      throw new ODataRuntimeException(e);
    }
    final Map<String, Collection<String>> responseHeaders = res.getHeaders();
    for (Map.Entry<String, Collection<String>> entry : responseHeaders.entrySet()) {
      final Collection<String> headerValues;
      if (headers.containsKey(entry.getKey())) {
        headerValues = headers.get(entry.getKey());
      } else {
        headerValues = new HashSet<>();
        headers.put(entry.getKey(), headerValues);
      }
      headerValues.addAll(entry.getValue());
    }

    statusCode = res.getStatusCode();
    statusMessage = res.getReasonPhrase();

    hasBeenInitialized = true;
    return this;
  }

  /**
   * Initializes this response from the unwrapped result of a completed asynchronous operation, as
   * returned by the status monitor when the monitor request did not accept <code>application/http</code>
   * ([OData-Protocol] 11.6). The monitor response's own status is always <code>200 OK</code>; the final
   * status code of the asynchronous operation is carried by the <code>AsyncResult</code> header
   * ([OData-Protocol] 8.3.1) and, when present and parsable, becomes this response's status.
   *
   * @param res the status-monitor HTTP response
   * @return this response, initialized
   */
  public ODataResponse initFromAsyncResult(final ODataHttpResponse res) {
    initFromHttpResponse(res);

    // the status-monitor response is closed by the caller as soon as it has been read, so the result
    // body has to be buffered here - unlike initFromEnclosedPart, initFromHttpResponse keeps the live
    // stream of the monitor response as the payload.
    if (this.payload != null) {
      try {
        this.payload = new ByteArrayInputStream(this.payload.readAllBytes());
      } catch (final IOException e) {
        LOG.error("Error buffering the asynchronous result payload", e);
        throw new ODataRuntimeException(e);
      }
    }

    final Collection<String> asyncResult = getHeader(HttpHeader.ASYNC_RESULT);
    if (asyncResult != null && !asyncResult.isEmpty()) {
      final String value = asyncResult.iterator().next();
      try {
        this.statusCode = Integer.parseInt(value.trim());
        final HttpStatusCode status = HttpStatusCode.fromStatusCode(this.statusCode);
        this.statusMessage = status == null ? this.statusMessage : status.getInfo();
      } catch (final NumberFormatException e) {
        LOG.warn("Unparsable AsyncResult header value '{}'", value, e);
      }
    }
    return this;
  }

  @Override
  public ODataResponse initFromBatch(
      final Map.Entry<Integer, String> responseLine,
      final Map<String, Collection<String>> headers,
      final ODataBatchLineIterator batchLineIterator,
      final String boundary) {

    if (hasBeenInitialized) {
      throw new IllegalStateException("Request already initialized");
    }

    this.batchInfo = new ODataBatchController(batchLineIterator, boundary);

    this.statusCode = responseLine.getKey();
    this.statusMessage = responseLine.getValue();
    this.headers.putAll(headers);

    this.hasBeenInitialized = true;
    return this;
  }

  @Override
  public ODataResponse initFromEnclosedPart(final InputStream part) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(part, StandardCharsets.UTF_8))) {
      if (hasBeenInitialized) {
        throw new IllegalStateException("Request already initialized");
      }

      final ODataBatchLineIteratorImpl batchLineIterator = new ODataBatchLineIteratorImpl(reader);

      final Map.Entry<Integer, String> partResponseLine = ODataBatchUtilities.readResponseLine(batchLineIterator);
      LOG.debug("Retrieved async item response {}", partResponseLine);

      this.statusCode = partResponseLine.getKey();
      this.statusMessage = partResponseLine.getValue();

      final Map<String, Collection<String>> partHeaders = ODataBatchUtilities.readHeaders(batchLineIterator);
      LOG.debug("Retrieved async item headers {}", partHeaders);

      this.headers.putAll(partHeaders);

      final ByteArrayOutputStream bos = new ByteArrayOutputStream();

      while (batchLineIterator.hasNext()) {
        bos.write(batchLineIterator.nextLine().getBytes(StandardCharsets.UTF_8));
        bos.write(CRLF);
      }

      this.payload = new ByteArrayInputStream(bos.toByteArray());

      this.hasBeenInitialized = true;
      return this;
    } catch (IOException e) {
      LOG.error("Error streaming payload response", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
    if (payload != null) {
      try {
        payload.transferTo(OutputStream.nullOutputStream());
      } catch (IOException e) {
        LOG.debug("Error draining response body", e);
      } finally {
        try {
          payload.close();
        } catch (IOException e) {
          LOG.debug("Error closing payload", e);
        }
      }
    }
    if (res != null) {
      try {
        res.close();
      } catch (IOException e) {
        LOG.debug("Unable to close response: {}", res, e);
      }
    }
    odataClient.getConfiguration().getHttpClientFactory().close(httpClient);

    if (batchInfo != null) {
      batchInfo.setValidBatch(false);
    }
  }

  @Override
  public InputStream getRawResponse() {

    InputStream inputStream;
    if (HttpStatusCode.NO_CONTENT.getStatusCode() == getStatusCode()) {
      throw new NoContentException();
    }

    if (payload == null && batchInfo != null && batchInfo.isValidBatch()) {
      // get input stream till the end of item
      payload = new PipedInputStream(null);

      try {
        final PipedOutputStream os = new PipedOutputStream((PipedInputStream) payload,
                ConfigurationImpl.DEFAULT_BUFFER_SIZE);

        new Thread(() -> {
          try {
            ODataBatchUtilities.readBatchPart(batchInfo, os, true);
          } catch (Exception e) {
            LOG.error("Error streaming batch item payload", e);
          } finally {
            try {
              os.close();
            } catch (IOException e) {
              LOG.debug("Failed to close resource", e);
            }
          }
        }).start();
      } catch (Exception e) {
        LOG.error("Error streaming payload response", e);
        throw new IllegalStateException(e);
      }
    } else if (payload != null) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try {
        payload.transferTo(byteArrayOutputStream);
       if(inputContent == null){
         inputContent  = byteArrayOutputStream.toByteArray();
       }
        inputStream = new ByteArrayInputStream(inputContent);
        return inputStream;
      } catch (IOException e) {
        if (res != null) {
          try {
            res.close();
          } catch (IOException ioe) {
            LOG.warn("Error closing response", ioe);
          }
        }
        LOG.error("Error retrieving payload", e);
        throw new ODataRuntimeException(e);
      }
    }
    return payload;
  }
}
