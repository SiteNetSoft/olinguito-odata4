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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1329: depend on the ODataHandler interface, not the impl
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 2: accept the outer request's $schemaversion so
 * it can be inherited by parts that don't specify their own
 */
package org.sitenetsoft.olinguito.server.core.batchhandler;

import java.util.List;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataHandler;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.batch.BatchFacade;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchDeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchDeserializerException.MessageKeys;
import org.sitenetsoft.olinguito.server.api.processor.BatchProcessor;
import org.sitenetsoft.olinguito.server.core.ODataHandlerException;
import org.sitenetsoft.olinguito.server.core.deserializer.batch.BatchParserCommon;

public class BatchHandler {
  private final BatchProcessor batchProcessor;
  private final ODataHandler oDataHandler;
  private final String outerSchemaVersion;
  private static final String RETURN_MINIMAL = "return=minimal";
  private static final String RETURN_REPRESENTATION = "return=representation";

  public BatchHandler(final ODataHandler oDataHandler, final BatchProcessor batchProcessor) {
    this(oDataHandler, batchProcessor, null);
  }

  /**
   * @param oDataHandler handler used to dispatch each batch part
   * @param batchProcessor the batch processor
   * @param outerSchemaVersion the <code>$schemaversion</code> value carried by the outer
   * <code>$batch</code> request, or {@code null} if the outer request carried none. Threaded down
   * to {@link BatchPartHandler} so parts without their own <code>$schemaversion</code> inherit it
   * (OData 4.01, Part 1: Protocol, section 11.2.12).
   */
  public BatchHandler(final ODataHandler oDataHandler, final BatchProcessor batchProcessor,
      final String outerSchemaVersion) {
    this.batchProcessor = batchProcessor;
    this.oDataHandler = oDataHandler;
    this.outerSchemaVersion = outerSchemaVersion;
  }

  public void process(final ODataRequest request, final ODataResponse response, final boolean isStrict)
      throws ODataApplicationException, ODataLibraryException {
    validateRequest(request);
    validatePreferHeader(request);

    final BatchFacade operation = new BatchFacadeImpl(oDataHandler, batchProcessor, isStrict, outerSchemaVersion);
    batchProcessor.processBatch(operation, request, response);
  }
  
  /** Checks if Prefer header is set with return=minimal or 
   * return=representation for batch requests
   * @param request
   * @throws ODataHandlerException
   */
  private void validatePreferHeader(final ODataRequest request) throws ODataHandlerException {
    final List<String> returnPreference = request.getHeaders(HttpHeader.PREFER);
    if (null != returnPreference) {
      for (String preference : returnPreference) {
        if (preference.equals(RETURN_MINIMAL) || preference.equals(RETURN_REPRESENTATION)) {
          throw new ODataHandlerException("Prefer Header not supported: " + preference,
              ODataHandlerException.MessageKeys.INVALID_PREFER_HEADER, preference);
        } 
      }
    }
  }

  private void validateRequest(final ODataRequest request) throws BatchDeserializerException {
    validateHttpMethod(request);
    validateContentType(request);
  }

  private void validateContentType(final ODataRequest request) throws BatchDeserializerException {
    // This method does validation.
    BatchParserCommon.parseContentType(request.getHeader(HttpHeader.CONTENT_TYPE), ContentType.MULTIPART_MIXED, 0);
  }

  private void validateHttpMethod(final ODataRequest request) throws BatchDeserializerException {
    if (request.getMethod() != HttpMethod.POST) {
      throw new BatchDeserializerException("Invalid HTTP method", MessageKeys.INVALID_METHOD, "0");
    }
  }
}
