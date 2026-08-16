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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 2: inherit the outer batch request's
 * $schemaversion into parts that don't carry their own (OData 4.01, Part 1: Protocol,
 * section 11.2.12)
 */
package org.sitenetsoft.olinguito.server.core.batchhandler;

import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.core.Encoder;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataHandler;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.batch.BatchFacade;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchDeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchRequestPart;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.ODataResponsePart;
import org.sitenetsoft.olinguito.server.api.processor.BatchProcessor;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOptionKind;
import org.sitenetsoft.olinguito.server.core.batchhandler.referenceRewriting.BatchReferenceRewriter;

public class BatchPartHandler {

  private static final String SCHEMAVERSION_OPTION = SystemQueryOptionKind.SCHEMAVERSION.toString();

  private final ODataHandler oDataHandler;
  private final BatchProcessor batchProcessor;
  private final BatchFacade batchFacade;
  private final BatchReferenceRewriter rewriter;
  private final String outerSchemaVersion;

  public BatchPartHandler(final ODataHandler oDataHandler, final BatchProcessor processor,
                          final BatchFacade batchFacade) {
    this(oDataHandler, processor, batchFacade, null);
  }

  /**
   * @param oDataHandler handler used to process each batch part as an independent request
   * @param processor the batch processor
   * @param batchFacade the facade handed to the batch processor
   * @param outerSchemaVersion the <code>$schemaversion</code> value carried by the outer
   * <code>$batch</code> request, or {@code null} if the outer request carried none. Per OData 4.01,
   * Part 1: Protocol, section 11.2.12, parts that don't specify their own
   * <code>$schemaversion</code> inherit this value; parts with their own option keep it.
   */
  public BatchPartHandler(final ODataHandler oDataHandler, final BatchProcessor processor,
                          final BatchFacade batchFacade, final String outerSchemaVersion) {
    this.oDataHandler = oDataHandler;
    batchProcessor = processor;
    this.batchFacade = batchFacade;
    this.outerSchemaVersion = outerSchemaVersion;
    rewriter = new BatchReferenceRewriter();
  }

  public ODataResponse handleODataRequest(final ODataRequest request) throws BatchDeserializerException {
    return handle(request, true);
  }

  public ODataResponsePart handleBatchRequest(final BatchRequestPart request)
      throws ODataApplicationException, ODataLibraryException {
    if (request.isChangeSet()) {
      return handleChangeSet(request);
    } else {
      final ODataResponse response = handle(request.getRequests().get(0), false);

      return new ODataResponsePart(response, false);
    }
  }

  public ODataResponse handle(final ODataRequest request, final boolean isChangeSet)
      throws BatchDeserializerException {
    ODataResponse response;

    inheritSchemaVersion(request);

    if (isChangeSet) {
      rewriter.replaceReference(request);

      response = oDataHandler.process(request);

      rewriter.addMapping(request, response);
    } else {
      response = oDataHandler.process(request);
    }

    // Add content id to response
    final String contentId = request.getHeader(HttpHeader.CONTENT_ID);
    if (contentId != null) {
      response.setHeader(HttpHeader.CONTENT_ID, contentId);
    }

    return response;
  }

  private ODataResponsePart handleChangeSet(final BatchRequestPart request) throws ODataApplicationException,
  ODataLibraryException {
    return batchProcessor.processChangeSet(batchFacade, request.getRequests());
  }

  /**
   * Appends the outer request's <code>$schemaversion</code> to this part's raw query path, unless
   * the outer request carried none or this part already specifies its own <code>$schemaversion</code>
   * (per OData 4.01, Part 1: Protocol, section 11.2.12, a part's own option takes precedence).
   *
   * @param request the batch part request, mutated in place
   */
  private void inheritSchemaVersion(final ODataRequest request) {
    if (outerSchemaVersion == null || hasSchemaVersionOption(request.getRawQueryPath())) {
      return;
    }
    final String appendedOption = SCHEMAVERSION_OPTION + "=" + Encoder.encode(outerSchemaVersion);
    final String rawQueryPath = request.getRawQueryPath();
    request.setRawQueryPath(
        rawQueryPath == null || rawQueryPath.isEmpty() ? appendedOption : rawQueryPath + "&" + appendedOption);
  }

  /**
   * Checks whether a raw query path already contains a <code>$schemaversion</code> option, matching
   * the option name (the segment before '=') rather than doing a substring search.
   *
   * @param rawQueryPath the raw query path to check, may be {@code null} or empty
   * @return {@code true} if a <code>$schemaversion</code> option is already present
   */
  private static boolean hasSchemaVersionOption(final String rawQueryPath) {
    if (rawQueryPath == null || rawQueryPath.isEmpty()) {
      return false;
    }
    for (final String optionSegment : rawQueryPath.split("&")) {
      final int equalsIndex = optionSegment.indexOf('=');
      final String optionName = equalsIndex == -1 ? optionSegment : optionSegment.substring(0, equalsIndex);
      if (SCHEMAVERSION_OPTION.equals(optionName)) {
        return true;
      }
    }
    return false;
  }

}
