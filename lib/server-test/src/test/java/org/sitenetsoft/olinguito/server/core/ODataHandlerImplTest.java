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
 * Copyright 2026 SiteNetSoft - Improved test assertions
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - OLINGO-1314: Updated version error tests for sanitized messages
 * Copyright 2026 SiteNetSoft - OLINGO-1372: Tests for error responses respecting Accept header
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 2: replaced the dynamic-property 501 pin with
 * routing assertions now that ODataDispatcher dispatches dynamicProperty segments
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 2 fix: pin ETag precondition enforcement for
 * dynamic-property PUT/PATCH/DELETE
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 6: tests for OData 4.01 POST /$query
 * (URL Conventions section 4.17)
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 6 fix round 1: adapter-realistic rawRequestUri
 * rebuild tests (merged query, not just a stripped /$query suffix)
 */
package org.sitenetsoft.olinguito.server.core;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.batch.BatchFacade;
import org.sitenetsoft.olinguito.server.api.etag.CustomETagSupport;
import org.sitenetsoft.olinguito.server.api.processor.ActionComplexCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionComplexProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionEntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionEntityProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionPrimitiveCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionPrimitiveProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionVoidProcessor;
import org.sitenetsoft.olinguito.server.api.processor.BatchProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ComplexCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ComplexProcessor;
import org.sitenetsoft.olinguito.server.api.processor.CountComplexCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.CountEntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.CountPrimitiveCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.EntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.EntityProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ErrorProcessor;
import org.sitenetsoft.olinguito.server.api.processor.MediaEntityProcessor;
import org.sitenetsoft.olinguito.server.api.processor.MetadataProcessor;
import org.sitenetsoft.olinguito.server.api.processor.PrimitiveCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.PrimitiveProcessor;
import org.sitenetsoft.olinguito.server.api.processor.PrimitiveValueProcessor;
import org.sitenetsoft.olinguito.server.api.processor.Processor;
import org.sitenetsoft.olinguito.server.api.processor.ReferenceCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ReferenceProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ServiceDocumentProcessor;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.core.debug.ServerCoreDebugger;
import org.sitenetsoft.olinguito.server.tecsvc.processor.TechnicalActionProcessor;
import org.sitenetsoft.olinguito.server.tecsvc.provider.ContainerProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.VerificationModeFactory;

class ODataHandlerImplTest {

  private static final String BASE_URI = "http://localhost/odata";

  @Test
  void serviceDocumentNonDefault() throws Exception {
    final ServiceDocumentProcessor processor = mock(ServiceDocumentProcessor.class);
    doThrow(new ODataApplicationException("msg", 100, Locale.ENGLISH)).when(processor)
        .readServiceDocument(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
            any(ContentType.class));
    final ODataResponse response = dispatch(HttpMethod.GET, "/", processor);
    assertEquals(HttpStatusCode.CONTINUE.getStatusCode(), response.getStatusCode());

    verify(processor).readServiceDocument(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    // We support HEAD now too
    final ServiceDocumentProcessor processor2 = mock(ServiceDocumentProcessor.class);
    doThrow(new ODataApplicationException("msg", 100, Locale.ENGLISH)).when(processor2)
    .readServiceDocument(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));
    final ODataResponse response2 = dispatch(HttpMethod.HEAD, "/", processor2);
    assertEquals(HttpStatusCode.CONTINUE.getStatusCode(), response2.getStatusCode());

    verify(processor2).readServiceDocument(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, "/", processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, "/", processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, "/", processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, "/", processor);
  }

  @Test
  void serviceDocumentDefault() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "/", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    String ct = response.getHeader(HttpHeader.CONTENT_TYPE);
    assertThat(ct, containsString("application/json"));
    assertThat(ct, containsString("odata.metadata=minimal"));

    assertNotNull(response.getContent());
    String doc = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);

    assertThat(doc, containsString("\"@odata.context\":\"$metadata\""));
    assertThat(doc, containsString("\"value\":"));
    
    final ODataResponse response2 = dispatch(HttpMethod.HEAD, "/", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response2.getStatusCode());
    assertNull(response2.getHeader(HttpHeader.CONTENT_TYPE));
    assertNull(response2.getContent());
  }

  @Test
  void serviceDocumentRedirect() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "", null);
    assertEquals(HttpStatusCode.TEMPORARY_REDIRECT.getStatusCode(), response.getStatusCode());
    assertEquals(BASE_URI + "/", response.getHeader(HttpHeader.LOCATION));
    
    final ODataResponse responseHead = dispatch(HttpMethod.HEAD, "", null);
    assertEquals(HttpStatusCode.TEMPORARY_REDIRECT.getStatusCode(), responseHead.getStatusCode());
    assertEquals(BASE_URI + "/", responseHead.getHeader(HttpHeader.LOCATION));
  }

  @Test
  void metadataNonDefault() throws Exception {
    final MetadataProcessor processor = mock(MetadataProcessor.class);
    doThrow(new ODataApplicationException("msg", 100, Locale.ENGLISH)).when(processor)
    .readMetadata(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", processor);
    assertEquals(HttpStatusCode.CONTINUE.getStatusCode(), response.getStatusCode());

    verify(processor).readMetadata(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
    
    // We support HEAD now too
    final MetadataProcessor processor2 = mock(MetadataProcessor.class);
    doThrow(new ODataApplicationException("msg", 100, Locale.ENGLISH)).when(processor2)
    .readMetadata(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));
    final ODataResponse response2 = dispatch(HttpMethod.HEAD, "$metadata", processor2);
    assertEquals(HttpStatusCode.CONTINUE.getStatusCode(), response2.getStatusCode());

    verify(processor2).readMetadata(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, "$metadata", processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, "$metadata", processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, "$metadata", processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, "$metadata", processor);
  }

  @Test
  void metadataDefault() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertEquals(ContentType.APPLICATION_XML.toContentTypeString(), response.getHeader(HttpHeader.CONTENT_TYPE));

    assertNotNull(response.getContent());
    assertThat(new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8),
        containsString("<edmx:Edmx Version=\"4.0\""));
    
    final ODataResponse response2 = dispatch(HttpMethod.HEAD, "$metadata", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response2.getStatusCode());
    assertNull(response2.getHeader(HttpHeader.CONTENT_TYPE));
    assertNull(response2.getContent());
  }

  @Test
  void maxVersionNone() {
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", null);
    assertEquals(ODataServiceVersion.V40.toString(), response.getHeader(HttpHeader.ODATA_VERSION));
  }

  @Test
  void maxVersionSupported() {
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", null,
        HttpHeader.ODATA_MAX_VERSION, ODataServiceVersion.V40.toString(), null);
    assertEquals(ODataServiceVersion.V40.toString(), response.getHeader(HttpHeader.ODATA_VERSION));
  }

  @Test
  void maxVersionNotSupported() {
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", null,
        HttpHeader.ODATA_MAX_VERSION, ODataServiceVersion.V30.toString(), null);

    assertEquals(ODataServiceVersion.V40.toString(), response.getHeader(HttpHeader.ODATA_VERSION));
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatusCode());
  }

  @Test
  void contentNegotiationSupported() {
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", "$format=xml", null, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
  }

  @Test
  void contentNegotiationNotSupported() {
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", "$format=not/Supported", null, null, null);
    assertEquals(HttpStatusCode.NOT_ACCEPTABLE.getStatusCode(), response.getStatusCode());
  }

  @Test
  void contentNegotiationNotSupported2() {
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", "$format=notSupported", null, null, null);
    assertEquals(HttpStatusCode.NOT_ACCEPTABLE.getStatusCode(), response.getStatusCode());
  }

  @Test
  void unregisteredProcessor() {
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrim", null);
    assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());
  }

  @Test
  void uriParserExceptionResultsInRightResponseNotFound() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "NotFound", null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
  }

  @Test
  void uriParserExceptionResultsInRightResponseBadRequest() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrim('122')", null);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatusCode());
  }

  @Test
  void uriParserExceptionWithFormatQueryJson() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrims", "$format=json", "", "", null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertEquals("application/json;odata.metadata=minimal",
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void uriParserExceptionWithFormatQueryJsonAndMore() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrims", "$format=json&$top=3", "", "", null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertEquals("application/json;odata.metadata=minimal",
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void uriParserExceptionWithFormatJsonAcceptAtom() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrims", "$format=json",
        HttpHeader.ACCEPT, ContentType.APPLICATION_ATOM_XML.toContentTypeString(), null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertEquals("application/json;odata.metadata=minimal",
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void uriParserExceptionWithFormatQueryAtom() throws Exception {
    // OLINGO-1372: $format=atom must be honored in error responses
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrims", "$format=atom", "", "", null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertEquals(ContentType.APPLICATION_ATOM_XML.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void uriParserExceptionWithFormatQueryAtomAndTop() throws Exception {
    // OLINGO-1372: $format=atom must be honored even with other query params
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrims", "$format=atom&$top=19", "", "", null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertEquals(ContentType.APPLICATION_ATOM_XML.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void uriParserExceptionWithFormatAtomAcceptJson() throws Exception {
    // OLINGO-1372: $format takes precedence over Accept header
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrims", "$format=atom",
        HttpHeader.ACCEPT, ContentType.APPLICATION_JSON.toContentTypeString(), null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertEquals(ContentType.APPLICATION_ATOM_XML.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void uriParserExceptionWithFormatQueryInvali() throws Exception {
    final ODataResponse response = dispatch(HttpMethod.GET, "ESAllPrims", "$format=somenotvalid", "", "", null);
    assertEquals(HttpStatusCode.NOT_ACCEPTABLE.getStatusCode(), response.getStatusCode());
    assertEquals("application/json;odata.metadata=minimal",
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void applicationExceptionInProcessorMessage() throws Exception {
    final String ODATA_ERRORCODE = "425";
    final String ORIGINAL_MESSAGE = "original message";
    final String LOCALIZED_MESSAGE = "localized message";
    MetadataProcessor processor = mock(MetadataProcessor.class);

    ODataApplicationException oDataApplicationException =
        new ODataApplicationException(ORIGINAL_MESSAGE, 425, Locale.ENGLISH, ODATA_ERRORCODE) {
          @Serial
          private static final long serialVersionUID = 1L;

          @Override
          public String getLocalizedMessage() {
            return LOCALIZED_MESSAGE;
          }
        };

    doThrow(oDataApplicationException).when(processor).readMetadata(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", processor);
    InputStream contentStream = response.getContent();
    String responseContent = new String(contentStream.readAllBytes(), StandardCharsets.UTF_8);
    // does the response contain the localized message and the status code?
    boolean isMessage = responseContent.contains(LOCALIZED_MESSAGE) && responseContent.contains(ODATA_ERRORCODE);
    // test if message is localized
    assertTrue(isMessage);
    // test if the original is hold
    assertEquals(ORIGINAL_MESSAGE, oDataApplicationException.getMessage());
  }

  @Test
  void applicationExceptionInProcessor() throws Exception {
    MetadataProcessor processor = mock(MetadataProcessor.class);
    doThrow(new ODataApplicationException("msg", 425, Locale.ENGLISH)).when(processor).readMetadata(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
    final ODataResponse response = dispatch(HttpMethod.GET, "$metadata", processor);
    assertEquals(425, response.getStatusCode());
  }

  @Test
  void uriParserExceptionResultsInRightResponseEdmCause() throws Exception {
    final OData odata = OData.newInstance();
    final ServiceMetadata serviceMetadata = odata.createServiceMetadata(
        new CsdlAbstractEdmProvider() {
          @Override
          public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
              throws ODataException {
            throw new ODataException("msg");
          }
        },
        Collections.emptyList());

    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.GET);
    request.setRawODataPath("EdmException");

    final ODataResponse response =
        new ODataHandlerImpl(odata, serviceMetadata, new ServerCoreDebugger(odata)).process(request);
    assertNotNull(response);
    assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }
  
  @Test
  void handlerExtTest() throws Exception {
    final OData odata = OData.newInstance();
    final ServiceMetadata serviceMetadata = odata.createServiceMetadata(
        new CsdlAbstractEdmProvider() {
          @Override
          public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
              throws ODataException {
            throw new ODataException("msg");
          }
        },
        Collections.emptyList());

    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.GET);
    request.setRawODataPath("EdmException");
    ODataHandlerImpl handler =  new ODataHandlerImpl(odata, serviceMetadata, new ServerCoreDebugger(odata));
    Processor extension =  new TechnicalActionProcessor(null, serviceMetadata);
    handler.register(extension);
    assertNull(handler.getLastThrownException());
    assertNull(handler.getUriInfo());
  }

  @Test
  void dispatchBatch() throws Exception {
    final String uri = "$batch";
    final BatchProcessor processor = mock(BatchProcessor.class);

    dispatch(HttpMethod.POST, uri, null, HttpHeader.CONTENT_TYPE, ContentType.MULTIPART_MIXED.toContentTypeString(),
        processor);
    verify(processor).processBatch(any(BatchFacade.class), any(ODataRequest.class), any(ODataResponse.class));

    dispatchMethodNotAllowed(HttpMethod.GET, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchEntitySet() throws Exception {
    final String uri = "ESAllPrim";
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchEntitySetCount() throws Exception {
    final String uri = "ESAllPrim/$count";
    final CountEntityCollectionProcessor processor = mock(CountEntityCollectionProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).countEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchCountWithNavigation() throws Exception {
    final CountEntityCollectionProcessor processor = mock(CountEntityCollectionProcessor.class);
    String uri = "ESAllPrim(0)/NavPropertyETTwoPrimMany/$count";
    dispatch(HttpMethod.GET, uri, processor);

    verify(processor).countEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchFunction() throws Exception {
    EntityProcessor entityProcessor = mock(EntityProcessor.class);
    dispatch(HttpMethod.GET, "FICRTETKeyNav()", entityProcessor);
    verify(entityProcessor).readEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    EntityCollectionProcessor entityCollectionProcessor = mock(EntityCollectionProcessor.class);
    dispatch(HttpMethod.GET, "FICRTCollESTwoKeyNavParam(ParameterInt16=123)", entityCollectionProcessor);
    verify(entityCollectionProcessor).readEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    final String entityCountUri = "FICRTCollESTwoKeyNavParam(ParameterInt16=123)/$count";
    final CountEntityCollectionProcessor entityCountProcessor = mock(CountEntityCollectionProcessor.class);
    dispatch(HttpMethod.GET, entityCountUri, entityCountProcessor);
    verify(entityCountProcessor).countEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    dispatchMethodNotAllowed(HttpMethod.POST, entityCountUri, entityCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, entityCountUri, entityCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.PUT, entityCountUri, entityCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, entityCountUri, entityCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, entityCountUri, entityCountProcessor);

    PrimitiveProcessor primitiveProcessor = mock(PrimitiveProcessor.class);
    dispatch(HttpMethod.GET, "FICRTString()", primitiveProcessor);
    verify(primitiveProcessor).readPrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    // FINRTInt16 is not composable so /$value is not allowed
    final String valueUri = "FINRTInt16()/$value";
    final PrimitiveValueProcessor primitiveValueProcessor = mock(PrimitiveValueProcessor.class);
    dispatchMethodWithError(HttpMethod.GET, valueUri, primitiveValueProcessor, HttpStatusCode.BAD_REQUEST);
    dispatchMethodWithError(HttpMethod.POST, valueUri, primitiveValueProcessor, HttpStatusCode.BAD_REQUEST);
    dispatchMethodWithError(HttpMethod.PATCH, valueUri, primitiveValueProcessor, HttpStatusCode.BAD_REQUEST);
    dispatchMethodWithError(HttpMethod.PUT, valueUri, primitiveValueProcessor, HttpStatusCode.BAD_REQUEST);
    dispatchMethodWithError(HttpMethod.DELETE, valueUri, primitiveValueProcessor, HttpStatusCode.BAD_REQUEST);
    dispatchMethodWithError(HttpMethod.HEAD, valueUri, primitiveValueProcessor, HttpStatusCode.BAD_REQUEST);

    final String primitiveCollectionUri = "FICRTCollString()";
    PrimitiveCollectionProcessor primitiveCollectionProcessor = mock(PrimitiveCollectionProcessor.class);
    dispatch(HttpMethod.GET, primitiveCollectionUri, primitiveCollectionProcessor);
    verify(primitiveCollectionProcessor).readPrimitiveCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
    dispatchMethodNotAllowed(HttpMethod.POST, primitiveCollectionUri, primitiveCollectionProcessor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, primitiveCollectionUri, primitiveCollectionProcessor);
    dispatchMethodNotAllowed(HttpMethod.PUT, primitiveCollectionUri, primitiveCollectionProcessor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, primitiveCollectionUri, primitiveCollectionProcessor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, primitiveCollectionUri, primitiveCollectionProcessor);

    final String primitiveCountUri = "FICRTCollString()/$count";
    final CountPrimitiveCollectionProcessor primitiveCountProcessor = mock(CountPrimitiveCollectionProcessor.class);
    dispatch(HttpMethod.GET, primitiveCountUri, primitiveCountProcessor);
    verify(primitiveCountProcessor).countPrimitiveCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    dispatchMethodNotAllowed(HttpMethod.POST, primitiveCountUri, primitiveCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, primitiveCountUri, primitiveCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.PUT, primitiveCountUri, primitiveCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, primitiveCountUri, primitiveCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, primitiveCountUri, primitiveCountProcessor);

    ComplexProcessor complexProcessor = mock(ComplexProcessor.class);
    dispatch(HttpMethod.GET, "FICRTCTTwoPrim()", complexProcessor);
    verify(complexProcessor).readComplex(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    ComplexCollectionProcessor complexCollectionProcessor = mock(ComplexCollectionProcessor.class);
    dispatch(HttpMethod.GET, "FICRTCollCTTwoPrim()", complexCollectionProcessor);
    verify(complexCollectionProcessor).readComplexCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    final String complexCountUri = "FICRTCollCTTwoPrim()/$count";
    final CountComplexCollectionProcessor complexCountProcessor = mock(CountComplexCollectionProcessor.class);
    dispatch(HttpMethod.GET, complexCountUri, complexCountProcessor);
    verify(complexCountProcessor).countComplexCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    dispatchMethodNotAllowed(HttpMethod.POST, complexCountUri, complexCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, complexCountUri, complexCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.PUT, complexCountUri, complexCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, complexCountUri, complexCountProcessor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, complexCountUri, complexCountProcessor);

    final String mediaUri = "FICRTESMedia(ParameterInt16=1)/$value";
    final MediaEntityProcessor mediaProcessor = mock(MediaEntityProcessor.class);
    dispatch(HttpMethod.GET, mediaUri, mediaProcessor);
    verify(mediaProcessor).readMediaEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
    dispatchMethodNotAllowed(HttpMethod.POST, mediaUri, mediaProcessor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, mediaUri, mediaProcessor);
    dispatchMethodNotAllowed(HttpMethod.PUT, mediaUri, mediaProcessor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, mediaUri, mediaProcessor);

    dispatch(HttpMethod.HEAD, mediaUri, mediaProcessor);
  }

  @Test
  void dispatchAction() throws Exception {
    final ActionPrimitiveProcessor primitiveProcessor = mock(ActionPrimitiveProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRT_STRING, primitiveProcessor);
    verify(primitiveProcessor).processActionPrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));
    dispatchMethodNotAllowed(HttpMethod.GET, ContainerProvider.AIRT_STRING, primitiveProcessor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, ContainerProvider.AIRT_STRING, primitiveProcessor);
    dispatchMethodNotAllowed(HttpMethod.PUT, ContainerProvider.AIRT_STRING, primitiveProcessor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, ContainerProvider.AIRT_STRING, primitiveProcessor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, ContainerProvider.AIRT_STRING, primitiveProcessor);

    ActionPrimitiveCollectionProcessor primitiveCollectionProcessor = mock(ActionPrimitiveCollectionProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRT_COLL_STRING_TWO_PARAM, primitiveCollectionProcessor);
    verify(primitiveCollectionProcessor).processActionPrimitiveCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    ActionComplexProcessor complexProcessor = mock(ActionComplexProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRTCT_TWO_PRIM_PARAM, complexProcessor);
    verify(complexProcessor).processActionComplex(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    ActionComplexCollectionProcessor complexCollectionProcessor = mock(ActionComplexCollectionProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRT_COLL_CT_TWO_PRIM_PARAM, complexCollectionProcessor);
    verify(complexCollectionProcessor).processActionComplexCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    ActionEntityProcessor entityProcessor = mock(ActionEntityProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRTET_TWO_KEY_TWO_PRIM_PARAM, entityProcessor);
    verify(entityProcessor).processActionEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    ActionEntityCollectionProcessor entityCollectionProcessor = mock(ActionEntityCollectionProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRT_COLL_ET_KEY_NAV_PARAM, entityCollectionProcessor);
    verify(entityCollectionProcessor).processActionEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    ActionEntityProcessor entityProcessorEs = mock(ActionEntityProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRTES_ALL_PRIM_PARAM, entityProcessorEs);
    verify(entityProcessorEs).processActionEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    ActionEntityCollectionProcessor entityCollectionProcessorEs = mock(ActionEntityCollectionProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRT_COLL_ES_ALL_PRIM_PARAM, entityCollectionProcessorEs);
    verify(entityCollectionProcessorEs).processActionEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    final ActionVoidProcessor voidProcessor = mock(ActionVoidProcessor.class);
    dispatch(HttpMethod.POST, ContainerProvider.AIRT, voidProcessor);
    verify(voidProcessor).processActionVoid(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
    dispatchMethodNotAllowed(HttpMethod.GET, ContainerProvider.AIRT, voidProcessor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, ContainerProvider.AIRT, voidProcessor);
    dispatchMethodNotAllowed(HttpMethod.PUT, ContainerProvider.AIRT, voidProcessor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, ContainerProvider.AIRT, voidProcessor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, ContainerProvider.AIRT, voidProcessor);
  }

  @Test
  void dispatchEntity() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PATCH, uri, processor);
    verify(processor).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, times(2)).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deleteEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatch(HttpMethod.POST, "ESAllPrim", processor);
    verify(processor).createEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }


  @Test
  void dispatchSingleton() throws Exception {
    final String uri = "SI";
    final EntityProcessor processor = mock(EntityProcessor.class);
    
    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PATCH, uri, processor);
    verify(processor).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, times(2)).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
  }
  
  @Test
  void dispatchSingletonMedia() throws Exception {
    final String uri = "SIMedia/$value";
    final MediaEntityProcessor processor = mock(MediaEntityProcessor.class);
    
    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readMediaEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor).updateMediaEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
  }
  
  @Test
  void dispatchSingletonNavigation() throws Exception {
    final String uri = "SINav/NavPropertyETTwoKeyNavOne";
    final String sigletonNavUri = "ESTwoKeyNav(PropertyInt16=1,PropertyString='1')/NavPropertySINav";
    final String sigletonManyNavUri = "SINav/NavPropertyETTwoKeyNavMany";
    final EntityProcessor processor = mock(EntityProcessor.class);
    final EntityCollectionProcessor collectionProcessor = mock(EntityCollectionProcessor.class);
    
    dispatch(HttpMethod.GET, sigletonNavUri, processor);
    verify(processor).readEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PATCH, sigletonNavUri, processor);
    verify(processor).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, sigletonNavUri, processor);
    verify(processor, times(2)).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, sigletonNavUri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, sigletonNavUri, processor);
    
    dispatch(HttpMethod.GET, uri, processor);
    verify(processor, times(2)).readEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PATCH, uri, processor);
    verify(processor,  times(3)).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, times(4)).updateEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));
    
    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deleteEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    
    
    dispatch(HttpMethod.GET, sigletonManyNavUri, collectionProcessor);
    verify(collectionProcessor).readEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
    
    dispatchMethodNotAllowed(HttpMethod.PATCH, sigletonManyNavUri, processor);
    
    dispatch(HttpMethod.PUT, sigletonManyNavUri, processor);
    
    dispatch(HttpMethod.POST, sigletonManyNavUri, processor);
    verify(processor).createEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));


    dispatchMethodNotAllowed(HttpMethod.DELETE, sigletonManyNavUri, processor);
  }
  
  @Test
  void dispatchMedia() throws Exception {
    final String uri = "ESMedia(1)/$value";
    final MediaEntityProcessor processor = mock(MediaEntityProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readMediaEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.POST, "ESMedia", processor);
    verify(processor).createMediaEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor).updateMediaEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deleteMediaEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);

    dispatch(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchValueOnNoMedia() throws Exception {
    final String uri = "ESAllPrim(1)/$value";
    final MediaEntityProcessor processor = mock(MediaEntityProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verifyNoInteractions(processor);

    dispatch(HttpMethod.POST, uri, processor);
    verifyNoInteractions(processor);

    dispatch(HttpMethod.PUT, uri, processor);
    verifyNoInteractions(processor);

    dispatch(HttpMethod.DELETE, uri, processor);
    verifyNoInteractions(processor);
    
    dispatch(HttpMethod.HEAD, uri, processor);
    verifyNoInteractions(processor);
  }

  @Test
  void dispatchMediaWithNavigation() throws Exception {
    /*
     * In Java we decided that any kind of navigation will be accepted. This means that a $value on a media resource
     * must be dispatched as well
     */
    final String uri = "ESKeyNav(1)/NavPropertyETMediaOne/$value";
    final MediaEntityProcessor processor = mock(MediaEntityProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readMediaEntity(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, "ESKeyNav(1)/NavPropertyETMediaOne", processor);

    dispatchMethodNotAllowed(HttpMethod.POST, "ESKeyNav(1)/NavPropertyETMediaOne/$value", processor);

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor).updateMediaEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class), any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deleteMediaEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);

    dispatch(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchMediaDeleteIndirect() throws Exception {
    final MediaEntityProcessor processor = mock(MediaEntityProcessor.class);
    dispatch(HttpMethod.DELETE, "ESMedia(1)", processor);

    verify(processor).deleteEntity(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    dispatchMethodNotAllowed(HttpMethod.HEAD, "ESMedia(1)", processor);
  }

  @Test
  void dispatchPrimitiveProperty() throws Exception {
    final String uri = "ESAllPrim(0)/PropertyString";
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readPrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PATCH, uri, processor);
    verify(processor).updatePrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, times(2)).updatePrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deletePrimitive(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dynamicPropertyGetRoutesToPrimitiveProcessor() throws Exception {
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
    dispatch(HttpMethod.GET, "ESOpen(1)/DynamicString", processor);
    verify(processor).readPrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
  }

  @Test
  void dynamicPropertyPutRoutesToUpdatePrimitive() throws Exception {
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
    dispatch(HttpMethod.PUT, "ESOpen(1)/DynamicString", processor);
    verify(processor).updatePrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));
  }

  @Test
  void dynamicPropertyPatchRoutesToUpdatePrimitive() throws Exception {
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
    dispatch(HttpMethod.PATCH, "ESOpen(1)/DynamicString", processor);
    verify(processor).updatePrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));
  }

  @Test
  void dynamicPropertyDeleteRoutesToDeletePrimitive() throws Exception {
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
    dispatch(HttpMethod.DELETE, "ESOpen(1)/DynamicString", processor);
    verify(processor).deletePrimitive(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
  }

  @Test
  void dynamicPropertyPostAndHeadAreRejected() throws Exception {
    // A dynamic property behaves like a scalar primitive property: POST is not a valid method on a
    // single value and HEAD is not supported (mirrors dispatchPrimitiveProperty's POST/HEAD checks).
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
    final String uri = "ESOpen(1)/DynamicString";
    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void closedTypeUnknownSegmentStill404s() throws Exception {
    // A closed (non-open) type must still reject an unknown path segment at parse time
    // (UriParserSemanticException/PROPERTY_NOT_IN_TYPE), regardless of dynamic-property dispatch
    // now being wired up for open types.
    final ODataResponse response = dispatch(HttpMethod.GET, "ESTwoPrim(1)/Nope", null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
  }

  @Test
  void dynamicPropertyPutEnforcesEtagPreconditionWhenSupportEnabled() throws Exception {
    // PreconditionsValidator must keep resolving the entity set's binding target across a
    // dynamicProperty segment (like it already does for primitiveProperty/complexProperty), so a
    // PUT on ESOpen(1)/DynamicString without If-Match must be rejected with Precondition Required
    // when the registered CustomETagSupport reports the entity set needs an ETag, and the mocked
    // processor must never be invoked in that case.
    final String uri = "ESOpen(1)/DynamicString";
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
    final CustomETagSupport eTagSupport = mock(CustomETagSupport.class);
    when(eTagSupport.hasETag(any(EdmBindingTarget.class))).thenReturn(true);

    final ODataResponse withoutIfMatch =
        dispatchWithETagSupport(HttpMethod.PUT, uri, processor, eTagSupport, null);
    // The bug this pins: without a resolved binding target for the dynamicProperty segment,
    // PreconditionsValidator#mustValidatePreconditions silently returns false, so the write is
    // never rejected and reaches the processor - assert both symptoms so a regression is
    // unambiguous either way.
    verifyNoInteractions(processor);
    assertEquals(HttpStatusCode.PRECONDITION_REQUIRED.getStatusCode(), withoutIfMatch.getStatusCode());

    dispatchWithETagSupport(HttpMethod.PUT, uri, processor, eTagSupport, "*");
    verify(processor).updatePrimitive(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));
  }

  private ODataResponse dispatchWithETagSupport(final HttpMethod method, final String path,
      final Processor processor, final CustomETagSupport eTagSupport, final String ifMatchValue) {
    ODataRequest request = new ODataRequest();
    request.setMethod(method);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath(path);
    request.addHeader(HttpHeader.CONTENT_TYPE, Collections.singletonList(ContentType.JSON.toContentTypeString()));
    if (ifMatchValue != null) {
      request.addHeader(HttpHeader.IF_MATCH, Collections.singletonList(ifMatchValue));
    }

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(
        new EdmTechProvider(), Collections.emptyList());

    ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));
    handler.register(processor);
    handler.register(eTagSupport);

    final ODataResponse response = handler.process(request);
    assertNotNull(response);
    return response;
  }

  @Test
  void dispatchPrimitivePropertyValue() throws Exception {
    final String uri = "ESAllPrim(0)/PropertyString/$value";
    final PrimitiveValueProcessor processor = mock(PrimitiveValueProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readPrimitiveValue(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, null, HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString(),
        processor);
    verify(processor).updatePrimitiveValue(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deletePrimitiveValue(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchPrimitiveCollectionProperty() throws Exception {
    final String uri = "ESMixPrimCollComp(7)/CollPropertyString";
    final PrimitiveCollectionProcessor processor = mock(PrimitiveCollectionProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readPrimitiveCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, VerificationModeFactory.times(1)).updatePrimitiveCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.POST, uri, processor);
    verify(processor, VerificationModeFactory.times(2)).updatePrimitiveCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deletePrimitiveCollection(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchPrimitiveCollectionPropertyCount() throws Exception {
    final String uri = "ESMixPrimCollComp(7)/CollPropertyString/$count";
    final CountPrimitiveCollectionProcessor processor = mock(CountPrimitiveCollectionProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).countPrimitiveCollection(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchComplexProperty() throws Exception {
    final String uri = "ESMixPrimCollComp(7)/PropertyComp";
    final ComplexProcessor processor = mock(ComplexProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readComplex(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PATCH, uri, processor);
    verify(processor).updateComplex(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, times(2)).updateComplex(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deleteComplex(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchComplexCollectionProperty() throws Exception {
    final String uri = "ESMixPrimCollComp(7)/CollPropertyComp";
    final ComplexCollectionProcessor processor = mock(ComplexCollectionProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readComplexCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, VerificationModeFactory.times(1)).updateComplexCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class),
        any(ContentType.class));

    dispatch(HttpMethod.POST, uri, processor);
    verify(processor, VerificationModeFactory.times(2)).updateComplexCollection(any(ODataRequest.class),
        any(ODataResponse.class), any(UriInfo.class), any(ContentType.class), any(ContentType.class));

    dispatch(HttpMethod.DELETE, uri, processor);
    verify(processor).deleteComplexCollection(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchComplexCollectionPropertyCount() throws Exception {
    final String uri = "ESMixPrimCollComp(7)/CollPropertyComp/$count";
    final CountComplexCollectionProcessor processor = mock(CountComplexCollectionProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).countComplexCollection(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.DELETE, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);
  }

  @Test
  void dispatchReference() throws Exception {
    final String uri = "ESAllPrim(0)/NavPropertyETTwoPrimOne/$ref";
    final String uriMany = "ESAllPrim(0)/NavPropertyETTwoPrimMany/$ref";
    final String singletonUri = "SINav/NavPropertyETKeyNavOne/$ref";
    final String singletonUriMany = "SINav/NavPropertyETTwoKeyNavMany/$ref";
    final String singleUri = "SINav/$ref";
    final ReferenceProcessor processor = mock(ReferenceProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.PATCH, uri, processor);
    verify(processor).updateReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, uri, processor);
    verify(processor, times(2)).updateReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, uri, processor);

    dispatch(HttpMethod.POST, uriMany, processor);
    verify(processor).createReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.DELETE, uriMany, "$id=ESTwoPrim(1)", null, null, processor);
    verify(processor).deleteReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor);    
    
    //singleton URIs
    
    dispatch(HttpMethod.GET, singletonUri, processor);
    verify(processor, times(2)).readReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.PATCH, singletonUri, processor);
    verify(processor, times(3)).updateReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, singletonUri, processor);
    verify(processor, times(4)).updateReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, singletonUri, processor); 
    
    dispatch(HttpMethod.GET, singleUri, processor);
    verify(processor, times(3)).readReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.PATCH, singleUri, processor);
    verify(processor, times(5)).updateReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.PUT, singleUri, processor);
    verify(processor, times(6)).updateReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.POST, singleUri, processor); 
    
    dispatch(HttpMethod.POST, singletonUriMany, processor);
    verify(processor, times(2)).createReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatch(HttpMethod.DELETE, singletonUriMany, "$id=ESTwoPrim(1)", null, null, processor);
    verify(processor, times(2)).deleteReference(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class));
    
    dispatchMethodNotAllowed(HttpMethod.HEAD, singletonUriMany, processor);
  }

  @Test
  void dispatchReferenceCollection() throws Exception {
    final String uri = "ESAllPrim(0)/NavPropertyETTwoPrimMany/$ref";
    final String singletonUri = "SINav/NavPropertyETTwoKeyNavMany/$ref";
    final ReferenceCollectionProcessor processor = mock(ReferenceCollectionProcessor.class);

    dispatch(HttpMethod.GET, uri, processor);
    verify(processor).readReferenceCollection(any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.PATCH, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, uri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, uri, processor); 
    
    //singleton ref
    dispatch(HttpMethod.GET, singletonUri, processor);
    verify(processor, times(2)).readReferenceCollection(any(ODataRequest.class), 
        any(ODataResponse.class), any(UriInfo.class),
        any(ContentType.class));

    dispatchMethodNotAllowed(HttpMethod.PATCH, singletonUri, processor);
    dispatchMethodNotAllowed(HttpMethod.PUT, singletonUri, processor);
    dispatchMethodNotAllowed(HttpMethod.HEAD, singletonUri, processor);
  }

  @Test
  void noRequestContentType() throws Exception {
    EntityProcessor processor = mock(EntityProcessor.class);
    final ODataResponse response = dispatch(HttpMethod.POST, "ESAllPrim", null,
        HttpHeader.CONTENT_TYPE, null, processor);
    assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
  }

  @Test
  void illegalRequestContentType() throws Exception {
    EntityProcessor processor = mock(EntityProcessor.class);
    final ODataResponse response = dispatch(HttpMethod.POST, "ESAllPrim", null,
        HttpHeader.CONTENT_TYPE, "*/*", processor);
    verifyNoInteractions(processor);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatusCode());
  }

  @Test
  void unsupportedRequestContentType() throws Exception {
    EntityProcessor processor = mock(EntityProcessor.class);
    ErrorProcessor errorProcessor = mock(ErrorProcessor.class);
    dispatch(HttpMethod.POST, "ESAllPrim", null, HttpHeader.CONTENT_TYPE, "some/unsupported", errorProcessor);
    verifyNoInteractions(processor);
    verify(errorProcessor).processError(any(ODataRequest.class), any(ODataResponse.class),
        any(ODataServerError.class),
        any(ContentType.class));
  }

  private ODataResponse dispatch(final HttpMethod method, final String path, final String query,
      final String headerName, final String headerValue, final Processor processor) {
    ODataRequest request = new ODataRequest();
    request.setMethod(method);
    request.setRawBaseUri(BASE_URI);
    if (path.isEmpty()) {
      request.setRawRequestUri(BASE_URI);
    }
    request.setRawODataPath(path);
    request.setRawQueryPath(query);

    if (headerName != null) {
      request.addHeader(headerName, Collections.singletonList(headerValue));
    }

    if (headerName != HttpHeader.CONTENT_TYPE) {
      request.addHeader(HttpHeader.CONTENT_TYPE, Collections.singletonList(
          ContentType.JSON.toContentTypeString()));
    }

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(
        new EdmTechProvider(), Collections.emptyList());

    ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));

    if (processor != null) {
      handler.register(processor);
    }

    final ODataResponse response = handler.process(request);
    assertNotNull(response);
    return response;
  }

  @Test
  void dispatchEmptyContentWithoutContentType() {
    final String path = "ESAllPrim";
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);
    
    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.POST);
    request.setRawBaseUri(BASE_URI);
    request.setRawRequestUri(BASE_URI);
    request.setRawODataPath(path);
    request.setBody(new ByteArrayInputStream(new byte[0]));

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(
        new EdmTechProvider(), Collections.emptyList());

    ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));

    if (processor != null) {
      handler.register(processor);
    }

    final ODataResponse response = handler.process(request);
    assertNotNull(response);
  }
  
  private ODataResponse dispatch(final HttpMethod method, final String path, final Processor processor) {
    return dispatch(method, path, null, null, null, processor);
  }

  private void dispatchMethodNotAllowed(final HttpMethod method, final String path, final Processor processor) {
    final ODataResponse response = dispatch(method, path, processor);
    assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatusCode());
    assertNotNull(response.getContent());
  }

  private void dispatchMethodWithError(final HttpMethod method, final String path, final Processor processor,
      final HttpStatusCode statusCode) {
    final ODataResponse response = dispatch(method, path, processor);
    assertEquals(statusCode.getStatusCode(), response.getStatusCode());
    assertNotNull(response.getContent());
  }
  
  @Test
  void validateInvalidOdataVersion1() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> header = new HashMap<>();
    header.put(HttpHeader.ODATA_VERSION, "3.0");
    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, header, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
    assertEquals(400, response.getStatusCode());
    assertNotNull(response.getContent());
    String doc = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(doc.contains("OData version '4.0' is not supported."));
    assertFalse(doc.contains("3.0"));
  }

  @Test
  void validateInvalidOdataVersion2() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> header = new HashMap<>();
    header.put(HttpHeader.ODATA_VERSION, "5.0");

    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, header, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
    assertEquals(400, response.getStatusCode());
    assertNotNull(response.getContent());
    String doc = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(doc.contains("OData version '4.0' is not supported."));
    assertFalse(doc.contains("5.0"));
  }
  
  @Test
  void validateInvalidOdataMaxVersion1() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> header = new HashMap<>();
    header.put(HttpHeader.ODATA_MAX_VERSION, "3.0");

    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, header, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
    assertEquals(400, response.getStatusCode());
    assertNotNull(response.getContent());
    String doc = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(doc.contains("OData version '4.0' is not supported."));
    assertFalse(doc.contains("3.0"));
  }
  
  @Test
  void validateValidOdataMaxVersion2() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> header = new HashMap<>();
    header.put(HttpHeader.ODATA_MAX_VERSION, "5.0");
    
    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, header, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
  }
  
  @Test
  void validateValidOdataVersionAndMaxVersion1() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeader.ODATA_VERSION, "4.0");
    headers.put(HttpHeader.ODATA_MAX_VERSION, "5.0");
    
    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, headers, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
  }
  
  @Test
  void validateInvalidOdataVersionAndMaxVersion2() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeader.ODATA_VERSION, "3.0");
    headers.put(HttpHeader.ODATA_MAX_VERSION, "4.0");

    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, headers, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
    assertEquals(400, response.getStatusCode());
    assertNotNull(response.getContent());
    String doc = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(doc.contains("OData version '4.0' is not supported."));
    assertFalse(doc.contains("3.0"));
  }
  
  @Test
  void validateInvalidOdataVersionAndMaxVersion3() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeader.ODATA_VERSION, "5.0");
    headers.put(HttpHeader.ODATA_MAX_VERSION, "5.0");

    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, headers, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
    assertEquals(400, response.getStatusCode());
    assertNotNull(response.getContent());
    String doc = new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(doc.contains("OData version '4.0' is not supported."));
    assertFalse(doc.contains("5.0"));
  }
  
  @Test
  void validateValidOdataVersionAndMaxVersion2() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeader.ODATA_VERSION, "4.0");
    headers.put(HttpHeader.ODATA_MAX_VERSION, "4.01");
    
    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, headers, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
  }
  
  @Test
  void validateValidOdataVersionAndMaxVersion3() throws Exception {
    final String uri = "ESAllPrim(0)";
    final EntityProcessor processor = mock(EntityProcessor.class);

    final Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeader.ODATA_VERSION, "4.0");
    headers.put(HttpHeader.ODATA_MAX_VERSION, "4.0");
    
    final ODataResponse response = dispatchToValidateHeaders
        (HttpMethod.GET, uri, null, headers, processor);
    assertEquals("4.0", response.getHeader(HttpHeader.ODATA_VERSION));
  }
  
  // OLINGO-1372: Error responses must respect Accept header

  @Test
  void errorResponseRespectsAcceptXml() {
    // Request an invalid path with Accept: application/xml
    // This triggers a URI parser error (uriInfo stays null) and the error response
    // should be serialized as XML, not JSON.
    final ODataResponse response = dispatch(HttpMethod.GET, "nonExistentEntity", null,
        HttpHeader.ACCEPT, ContentType.APPLICATION_XML.toContentTypeString(), null);
    assertNotNull(response.getContent());
    assertEquals(ContentType.APPLICATION_XML.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void errorResponseRespectsAcceptXmlWithQueryString() {
    // When uriInfo is null and a query string is present (but no $format),
    // the Accept header should still be honored.
    final ODataResponse response = dispatch(HttpMethod.GET, "nonExistentEntity", "$top=10",
        HttpHeader.ACCEPT, ContentType.APPLICATION_XML.toContentTypeString(), null);
    assertNotNull(response.getContent());
    assertEquals(ContentType.APPLICATION_XML.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void errorResponseRespectsFormatOptionXml() {
    // When uriInfo is null and $format=xml is specified, the error response
    // should be serialized as XML.
    final ODataResponse response = dispatch(HttpMethod.GET, "nonExistentEntity", "$format=xml",
        null, null, null);
    assertNotNull(response.getContent());
    assertEquals(ContentType.APPLICATION_XML.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void errorResponseDefaultsToJsonWithoutAccept() {
    // Without Accept header or $format, errors should default to JSON.
    final ODataResponse response = dispatch(HttpMethod.GET, "nonExistentEntity", null,
        null, null, null);
    assertNotNull(response.getContent());
    assertThat(response.getHeader(HttpHeader.CONTENT_TYPE), containsString("application/json"));
  }

  private ODataResponse dispatchToValidateHeaders(final HttpMethod method, final String path, final String query,
      final Map<String, String> headers, final Processor processor) throws ODataHandlerException {
    ODataRequest request = new ODataRequest();
    request.setMethod(method);
    request.setRawBaseUri(BASE_URI);
    for (Entry<String, String> header : headers.entrySet()) {
      request.addHeader(header.getKey(), header.getValue());
    }
    if (path.isEmpty()) {
      request.setRawRequestUri(BASE_URI);
    }
    request.setRawODataPath(path);
    request.setRawQueryPath(query);

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(
        new EdmTechProvider(), Collections.emptyList());

    ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));

    if (processor != null) {
      handler.register(processor);
    }

    final ODataResponse response = handler.process(request);
    return response;
  }

  // OData 4.01 URL Conventions section 4.17: a POST to a resource path ending in /$query carries
  // query options in a text/plain body; those options are merged with any query options already
  // present in the URL and the request is processed as the equivalent GET.

  @Test
  void queryPostDispatchesAsGet() throws Exception {
    final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
    dispatchQueryPost("ESAllPrim(32767)/PropertyString/$query",
        null, "$format=json", ContentType.TEXT_PLAIN.toContentTypeString(), processor);
    verify(processor).readPrimitive(any(), any(), any(), any());
  }

  @Test
  void queryPostMergesUrlAndBodyOptions() throws Exception {
    // URL carries $format, body carries $select - both must reach the parser.
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);
    dispatchQueryPost("ESAllPrim/$query", "$format=json", "$select=PropertyString",
        ContentType.TEXT_PLAIN.toContentTypeString(), processor);
    verify(processor).readEntityCollection(any(), any(), any(), any());
  }

  @Test
  void queryPostDuplicateOptionAcrossUrlAndBodyIsBadRequest() throws Exception {
    // Duplicates across URL and body fall out as the parser's standing DOUBLE_SYSTEM_QUERY_OPTION
    // 400 - merging never silently drops or prefers one source over the other.
    final ODataResponse response = dispatchQueryPost("ESAllPrim/$query", "$select=PropertyInt16",
        "$select=PropertyString", ContentType.TEXT_PLAIN.toContentTypeString(), null);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatusCode());
  }

  @Test
  void queryGetIsMethodNotAllowed() throws Exception {
    // GET on a /$query path violates "Requests to paths ending in /$query MUST use the POST verb."
    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.GET);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath("ESAllPrim/$query");
    final ODataResponse response = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata))
        .process(request);
    assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatusCode());
  }

  @Test
  void queryPostWrongContentTypeIsUnsupportedMediaType() throws Exception {
    final ODataResponse response = dispatchQueryPost("ESAllPrim/$query", null, "$top=1",
        ContentType.APPLICATION_JSON.toContentTypeString(), null);
    assertEquals(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatusCode());
  }

  @Test
  void queryPostMissingContentTypeIsUnsupportedMediaType() throws Exception {
    // Content-Type absent entirely (not merely wrong) must also be rejected with 415.
    final ODataResponse response = dispatchQueryPost("ESAllPrim/$query", null, "$top=1", null, null);
    assertEquals(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatusCode());
  }

  @Test
  void queryPostEmptyBodyBehavesAsPlainGet() throws Exception {
    // An empty text/plain body is legal: /$query with nothing to add is just the equivalent GET.
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);
    dispatchQueryPost("ESAllPrim/$query", null, "", ContentType.TEXT_PLAIN.toContentTypeString(), processor);
    verify(processor).readEntityCollection(any(), any(), any(), any());
  }

  @Test
  void queryPostCaseVarianceNotStripped() throws Exception {
    // Segment matching is exact: "$Query" (capital Q) is not "$query" and must not be rewritten.
    // The request is left untouched - still POST, still an unrecognized path segment - and hits
    // the parser's pre-existing 400 SYNTAX error exactly as it would have before this feature.
    final ODataResponse response = dispatchQueryPost("ESAllPrim/$Query", null, "$top=1",
        ContentType.TEXT_PLAIN.toContentTypeString(), null);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatusCode());
  }

  @Test
  void batchQueryPostIsUnaffectedByRewrite() throws Exception {
    // $batch/$query rewrites to a GET on $batch; the batch dispatcher's own POST-only check then
    // rejects it with the ordinary method-not-allowed error (405), observed and documented here.
    final BatchProcessor processor = mock(BatchProcessor.class);
    final ODataResponse response = dispatchQueryPost("$batch/$query", null, "",
        ContentType.TEXT_PLAIN.toContentTypeString(), processor);
    verifyNoInteractions(processor);
    assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatusCode());
  }

  @Test
  void queryPostOnServiceRootPathDoesNotError() throws Exception {
    // A /$query path with no preceding resource segment (the service-root case, using this
    // codebase's "/" convention for the root resource path) must strip down to "" cleanly rather
    // than throwing - it lands on the existing empty-path redirect branch.
    final ODataResponse response = dispatchQueryPost("/$query", null, "",
        ContentType.TEXT_PLAIN.toContentTypeString(), null);
    assertEquals(HttpStatusCode.TEMPORARY_REDIRECT.getStatusCode(), response.getStatusCode());
  }

  @Test
  void queryPostRebuildsRawRequestUriWithMergedQuery() throws Exception {
    // Adapter-realistic shape (RequestUriResolver / ODataNettyHandlerImpl both populate
    // rawRequestUri as "<path>?<queryString>"): rawRequestUri must end up carrying the FULL merged
    // query (URL $format + body $select), not just have /$query stripped, since downstream
    // context-URL/next-link/delta-link generation reads rawRequestUri directly rather than
    // recomputing it from rawODataPath/rawQueryPath.
    final String path = "ESAllPrim/$query";
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);
    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.POST);
    request.setRawBaseUri(BASE_URI);
    request.setRawRequestUri(BASE_URI + "/" + path + "?$format=json");
    request.setRawODataPath(path);
    request.setRawQueryPath("$format=json");
    request.setBody(new ByteArrayInputStream("$select=PropertyString".getBytes(StandardCharsets.UTF_8)));
    request.addHeader(HttpHeader.CONTENT_TYPE,
        Collections.singletonList(ContentType.TEXT_PLAIN.toContentTypeString()));

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));
    handler.register(processor);

    handler.process(request);

    verify(processor).readEntityCollection(any(), any(), any(), any());
    assertEquals("ESAllPrim", request.getRawODataPath());
    assertEquals("$format=json&$select=PropertyString", request.getRawQueryPath());
    assertEquals(BASE_URI + "/ESAllPrim?$format=json&$select=PropertyString", request.getRawRequestUri());
  }

  @Test
  void queryPostRebuildsRawRequestUriWithBodyOnlyQuery() throws Exception {
    // No URL query string at all: rawRequestUri must still gain the body's merged options
    // (?$select=...), not merely lose the /$query suffix - otherwise a next-link built off this
    // request would silently drop every option the client sent in the body.
    final String path = "ESAllPrim/$query";
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);
    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.POST);
    request.setRawBaseUri(BASE_URI);
    request.setRawRequestUri(BASE_URI + "/" + path);
    request.setRawODataPath(path);
    request.setBody(new ByteArrayInputStream("$select=PropertyString".getBytes(StandardCharsets.UTF_8)));
    request.addHeader(HttpHeader.CONTENT_TYPE,
        Collections.singletonList(ContentType.TEXT_PLAIN.toContentTypeString()));

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));
    handler.register(processor);

    handler.process(request);

    verify(processor).readEntityCollection(any(), any(), any(), any());
    assertEquals("ESAllPrim", request.getRawODataPath());
    assertEquals("$select=PropertyString", request.getRawQueryPath());
    assertEquals(BASE_URI + "/ESAllPrim?$select=PropertyString", request.getRawRequestUri());
  }

  @Test
  void nonQueryRequestLeavesRawRequestUriUntouched() throws Exception {
    // Pin: a request whose path does NOT end in /$query must have rawRequestUri left completely
    // alone by handleQueryPathIfPresent's early no-op return.
    final String path = "ESAllPrim";
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);
    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.GET);
    request.setRawBaseUri(BASE_URI);
    request.setRawRequestUri(BASE_URI + "/" + path + "?$top=1");
    request.setRawODataPath(path);
    request.setRawQueryPath("$top=1");

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));
    handler.register(processor);

    handler.process(request);

    verify(processor).readEntityCollection(any(), any(), any(), any());
    assertEquals(BASE_URI + "/ESAllPrim?$top=1", request.getRawRequestUri());
  }

  private ODataResponse dispatchQueryPost(final String path, final String urlQuery, final String body,
      final String contentType, final Processor processor) {
    ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.POST);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath(path);
    request.setRawQueryPath(urlQuery);
    request.setBody(new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)));

    if (contentType != null) {
      request.addHeader(HttpHeader.CONTENT_TYPE, Collections.singletonList(contentType));
    }

    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(
        new EdmTechProvider(), Collections.emptyList());

    ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));

    if (processor != null) {
      handler.register(processor);
    }

    final ODataResponse response = handler.process(request);
    assertNotNull(response);
    return response;
  }

  // Tier 5 Wave 2 Task 2: $schemaversion validated against ServiceMetadata.getSchemaVersion()
  // (OData 4.01, Part 1: Protocol, section 11.2.12).

  @Test
  void schemaVersionMismatchIsNotFound() throws Exception {
    final ServiceMetadata metadata = versionedMetadata("1.2.3");
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);

    final ODataResponse response =
        dispatchWithMetadata(HttpMethod.GET, "ESAllPrim", "$schemaversion=9.9.9", metadata, processor);

    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertNotNull(response.getContent());
    verifyNoInteractions(processor);
  }

  @Test
  void schemaVersionStarAndExactMatchPass() throws Exception {
    final ServiceMetadata metadata = versionedMetadata("1.2.3");

    final EntityCollectionProcessor starProcessor = mock(EntityCollectionProcessor.class);
    dispatchWithMetadata(HttpMethod.GET, "ESAllPrim", "$schemaversion=*", metadata, starProcessor);
    verify(starProcessor).readEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));

    final EntityCollectionProcessor exactProcessor = mock(EntityCollectionProcessor.class);
    dispatchWithMetadata(HttpMethod.GET, "ESAllPrim", "$schemaversion=1.2.3", metadata, exactProcessor);
    verify(exactProcessor).readEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
  }

  @Test
  void schemaVersionIgnoredWhenServiceUnversioned() throws Exception {
    // Recorded decision: a service with no schema version has no version to check the option
    // against, so $schemaversion is accepted but has no validating effect.
    final OData odata = OData.newInstance();
    final ServiceMetadata metadata = odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);

    dispatchWithMetadata(HttpMethod.GET, "ESAllPrim", "$schemaversion=9.9.9", metadata, processor);

    verify(processor).readEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
  }

  @Test
  void schemaVersionAbsentUnchanged() throws Exception {
    final ServiceMetadata metadata = versionedMetadata("1.2.3");
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);

    dispatchWithMetadata(HttpMethod.GET, "ESAllPrim", null, metadata, processor);

    verify(processor).readEntityCollection(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
  }

  @Test
  void schemaVersionMismatchOnMetadataPathIsNotFound() throws Exception {
    // $schemaversion is validated on the $metadata path too, not just on resource paths.
    final ServiceMetadata metadata = versionedMetadata("1.2.3");
    final MetadataProcessor processor = mock(MetadataProcessor.class);

    final ODataResponse response =
        dispatchWithMetadata(HttpMethod.GET, "$metadata", "$schemaversion=9.9.9", metadata, processor);

    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    verifyNoInteractions(processor);
  }

  @Test
  void schemaVersionMatchOnMetadataPathPasses() throws Exception {
    final ServiceMetadata metadata = versionedMetadata("1.2.3");
    final MetadataProcessor processor = mock(MetadataProcessor.class);

    dispatchWithMetadata(HttpMethod.GET, "$metadata", "$schemaversion=1.2.3", metadata, processor);

    verify(processor).readMetadata(
        any(ODataRequest.class), any(ODataResponse.class), any(UriInfo.class), any(ContentType.class));
  }

  // Note: a literal "mismatched $schemaversion + disallowed HTTP method" ordering test (404 vs.
  // 405) is not exercisable without also touching UriValidator: pre-existing
  // UriValidator.validateNonReadQueryOptions() rejects ANY system query option (schemaversion
  // included) on non-GET methods outside a narrow allow-list ($id for DELETE on references,
  // $select/$expand for PUT/PATCH/POST) with 400 Bad Request, before ODataHandlerImpl even runs
  // the schema-version check. That pre-existing restriction is out of this task's scope (only
  // ODataHandlerImpl/BatchHandler/ServiceMetadata were to change). The two tests below pin the
  // same "check runs before dispatch" ordering using a GET request and an unregistered processor
  // (dispatch-level 501 Not Implemented) instead, which is reachable through UriValidator.

  @Test
  void schemaVersionMismatchPrecedesUnregisteredProcessorNotImplemented() throws Exception {
    // Pin: the $schemaversion check sits between UriValidator.validate(...) and
    // ODataDispatcher.dispatch(...) in ODataHandlerImpl.processInternal; processor selection
    // (yielding 501 for an unregistered processor) happens inside dispatch(...). So a mismatched
    // $schemaversion request against a resource with no registered processor surfaces the
    // schema-version 404, not the dispatcher's 501 - the request never reaches dispatch().
    final ServiceMetadata metadata = versionedMetadata("1.2.3");

    final ODataResponse response =
        dispatchWithMetadata(HttpMethod.GET, "ESAllPrim", "$schemaversion=9.9.9", metadata, null);

    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
  }

  @Test
  void schemaVersionMatchStillYieldsNotImplementedForUnregisteredProcessor() throws Exception {
    // Counterpart to the pin above: once $schemaversion matches, the request reaches dispatch()
    // as before, so an unregistered processor is still reported as 501 there.
    final ServiceMetadata metadata = versionedMetadata("1.2.3");

    final ODataResponse response =
        dispatchWithMetadata(HttpMethod.GET, "ESAllPrim", "$schemaversion=1.2.3", metadata, null);

    assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());
  }

  private ServiceMetadata versionedMetadata(final String schemaVersion) {
    return new ServiceMetadataImpl(new EdmTechProvider(), Collections.emptyList(), null, schemaVersion);
  }

  private ODataResponse dispatchWithMetadata(final HttpMethod method, final String path, final String query,
      final ServiceMetadata metadata, final Processor processor) {
    ODataRequest request = new ODataRequest();
    request.setMethod(method);
    request.setRawBaseUri(BASE_URI);
    if (path.isEmpty()) {
      request.setRawRequestUri(BASE_URI);
    }
    request.setRawODataPath(path);
    request.setRawQueryPath(query);
    request.addHeader(HttpHeader.CONTENT_TYPE, Collections.singletonList(ContentType.JSON.toContentTypeString()));

    final OData odata = OData.newInstance();
    final ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));

    if (processor != null) {
      handler.register(processor);
    }

    final ODataResponse response = handler.process(request);
    assertNotNull(response);
    return response;
  }
}
