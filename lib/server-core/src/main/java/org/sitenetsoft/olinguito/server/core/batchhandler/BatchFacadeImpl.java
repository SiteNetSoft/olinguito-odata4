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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 2: thread the outer $schemaversion through to
 * BatchPartHandler for batch-part inheritance
 */
package org.sitenetsoft.olinguito.server.core.batchhandler;

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
import org.sitenetsoft.olinguito.server.core.deserializer.batch.BatchParserCommon;

public class BatchFacadeImpl implements BatchFacade {
  private final BatchPartHandler partHandler;

  /**
   * Creates a new BatchFacade.
   * @param oDataHandler   handler
   * @param batchProcessor batch processor
   * @param isStrict       mode switch (currently not used)
   */
  public BatchFacadeImpl(final ODataHandler oDataHandler, final BatchProcessor batchProcessor,
                         final boolean isStrict) {
    this(oDataHandler, batchProcessor, isStrict, null);
  }

  /**
   * Creates a new BatchFacade that threads the outer <code>$batch</code> request's
   * <code>$schemaversion</code> down to the individual parts (OData 4.01, Part 1: Protocol,
   * section 11.2.12: parts without their own <code>$schemaversion</code> inherit the outer value).
   *
   * @param oDataHandler   handler
   * @param batchProcessor batch processor
   * @param isStrict       mode switch (currently not used)
   * @param outerSchemaVersion the outer request's <code>$schemaversion</code> value, or
   * {@code null} if the outer request carried none
   */
  public BatchFacadeImpl(final ODataHandler oDataHandler, final BatchProcessor batchProcessor,
                         final boolean isStrict, final String outerSchemaVersion) {
    partHandler = new BatchPartHandler(oDataHandler, batchProcessor, this, outerSchemaVersion);
  }

  @Override
  public ODataResponse handleODataRequest(final ODataRequest request)
      throws ODataApplicationException, ODataLibraryException {
    return partHandler.handleODataRequest(request);
  }

  @Override
  public ODataResponsePart handleBatchRequest(final BatchRequestPart request)
      throws ODataApplicationException, ODataLibraryException {
    return partHandler.handleBatchRequest(request);
  }

  @Override
  public String extractBoundaryFromContentType(final String contentType) throws BatchDeserializerException {
    return BatchParserCommon.getBoundary(contentType, 0);
  }
}
