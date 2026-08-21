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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 8: tests for the 202 Accepted answer to the
 * respond-async preference ([OData-Protocol] sections 8.2.8.8 and 11.6)
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.async.AsyncInvocation;
import org.sitenetsoft.olinguito.server.api.async.AsyncResult;
import org.sitenetsoft.olinguito.server.api.async.AsyncSupport;
import org.sitenetsoft.olinguito.server.core.debug.ServerCoreDebugger;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.processor.DefaultProcessor;
import org.sitenetsoft.olinguito.server.api.processor.EntityProcessor;
import org.sitenetsoft.olinguito.server.api.processor.Processor;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.tecsvc.processor.TechnicalPrimitiveComplexProcessor;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;

/**
 * [OData-Protocol] section 8.2.8.8: a service that applies <code>respond-async</code> "MUST include
 * a Preference-Applied response header containing the respond-async preference"; section 11.6: a
 * 202 Accepted "MUST include a Location header pointing to a status monitor resource ... in
 * addition to an optional Retry-After header", and a service "MUST NOT reply to a Data Service
 * Request with 202 Accepted if the request has not included the respond-async preference".
 */
class AsyncRespondAsyncTest {

  private static final String BASE_URI = "http://localhost/odata";
  private static final String STATUS_URL = "http://host/svc/status/1";

  /**
   * A primitive-property path is used throughout: tecsvc's {@code TechnicalEntityProcessor} carries
   * its own pre-existing {@code respond-async} implementation, which would mask what is tested here.
   */
  private static final String PATH = "ESAllPrim(32767)/PropertyString";

  /** Records what the handler submits without ever running it on the calling thread. */
  private static class RecordingAsyncSupport implements AsyncSupport {
    private AsyncInvocation invocation;
    private ODataRequest submittedRequest;
    private int submitCount;
    private Integer retryAfter;

    @Override
    public boolean isStatusMonitorRequest(final ODataRequest request) {
      return false;
    }

    @Override
    public String submit(final ODataRequest request, final AsyncInvocation asyncInvocation) {
      submitCount++;
      submittedRequest = request;
      invocation = asyncInvocation;
      return STATUS_URL;
    }

    @Override
    public AsyncResult resolve(final ODataRequest request) {
      return AsyncResult.notFound();
    }

    @Override
    public Integer getRetryAfter() {
      return retryAfter;
    }
  }

  @Test
  void withoutAnAsyncSupportThePreferenceIsIgnored() throws Exception {
    final ODataResponse plain = process(get(PATH, null), null);
    final ODataResponse preferred = process(get(PATH, "respond-async"), null);

    assertEquals(HttpStatusCode.OK.getStatusCode(), plain.getStatusCode());
    assertEquals(plain.getStatusCode(), preferred.getStatusCode());
    // wholesale, so that ANY header difference fails: with no AsyncSupport registered a service is
    // byte-identical to one that never heard of the preference - no Location, no
    // Preference-Applied, no Retry-After, and nothing else either.
    assertEquals(plain.getAllHeaders(), preferred.getAllHeaders());
    assertEquals(content(plain), content(preferred));
  }

  @Test
  void withAnAsyncSupportThePreferenceProduces202() {
    final RecordingAsyncSupport async = new RecordingAsyncSupport();
    final ODataResponse response = process(get(PATH, "respond-async"), async);

    assertEquals(HttpStatusCode.ACCEPTED.getStatusCode(), response.getStatusCode());
    assertEquals(STATUS_URL, response.getHeader(HttpHeader.LOCATION));
    assertEquals("respond-async", response.getHeader(HttpHeader.PREFERENCE_APPLIED));
    assertNull(response.getContent());
    assertEquals(1, async.submitCount);
  }

  @Test
  void retryAfterIsWrittenOnlyWhenTheServiceSuppliesIt() {
    final RecordingAsyncSupport none = new RecordingAsyncSupport();
    assertNull(process(get(PATH, "respond-async"), none).getHeader(HttpHeader.RETRY_AFTER));

    final RecordingAsyncSupport seven = new RecordingAsyncSupport();
    seven.retryAfter = 7;
    assertEquals("7", process(get(PATH, "respond-async"), seven).getHeader(HttpHeader.RETRY_AFTER));
  }

  @Test
  void theSubmittedInvocationProducesTheSynchronousResponse() throws Exception {
    final ODataResponse synchronous = process(get(PATH, null), null);

    final RecordingAsyncSupport async = new RecordingAsyncSupport();
    process(get(PATH, "respond-async"), async);
    assertNotNull(async.invocation);

    final ODataResponse asynchronous = async.invocation.invoke();
    assertEquals(synchronous.getStatusCode(), asynchronous.getStatusCode());
    assertEquals(synchronous.getHeader(HttpHeader.CONTENT_TYPE), asynchronous.getHeader(HttpHeader.CONTENT_TYPE));
    assertEquals(content(synchronous), content(asynchronous));
  }

  @Test
  void aFailingInvocationProducesTheSameErrorPayloadAsASynchronousFailure() throws Exception {
    final String missing = "ESAllPrim(4711)/PropertyString";
    final ODataResponse synchronous = process(get(missing, null), null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), synchronous.getStatusCode());

    final RecordingAsyncSupport async = new RecordingAsyncSupport();
    final ODataResponse response = process(get(missing, "respond-async"), async);
    assertEquals(HttpStatusCode.ACCEPTED.getStatusCode(), response.getStatusCode());

    final ODataResponse asynchronous = async.invocation.invoke();
    final String synchronousBody = content(synchronous);
    final String asynchronousBody = content(asynchronous);
    assertEquals(synchronous.getStatusCode(), asynchronous.getStatusCode());
    assertEquals(synchronousBody, asynchronousBody);
    assertTrue(asynchronousBody.contains("\"error\""), asynchronousBody);
  }

  @Test
  void aRequestWithoutThePreferenceIsNeverSubmitted() {
    final RecordingAsyncSupport async = new RecordingAsyncSupport();
    final ODataResponse response = process(get(PATH, null), async);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertEquals(0, async.submitCount);
    assertNull(response.getHeader(HttpHeader.PREFERENCE_APPLIED));
  }

  @Test
  void theInvocationSeesTheRequestBodyAfterTheOriginalStreamIsConsumed() throws Exception {
    final String body = "{\"PropertyInt16\":4711,\"PropertyString\":\"Test\"}";
    final ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.POST);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath("ESTwoPrim");
    request.setRawRequestUri(BASE_URI + "/ESTwoPrim");
    request.addHeader(HttpHeader.CONTENT_TYPE, Collections.singletonList(ContentType.JSON.toContentTypeString()));
    request.addHeader(HttpHeader.PREFER, Collections.singletonList("respond-async"));
    request.setBody(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

    final RecordingAsyncSupport async = new RecordingAsyncSupport();
    final EchoingCreateProcessor creator = new EchoingCreateProcessor();
    final ODataResponse response = process(request, async, creator);
    assertEquals(HttpStatusCode.ACCEPTED.getStatusCode(), response.getStatusCode());

    // the container recycles the original request: drain and close its stream before replaying
    request.getBody().readAllBytes();
    request.getBody().close();
    request.setBody(null);

    assertNotNull(async.submittedRequest);
    final ODataResponse asynchronous = async.invocation.invoke();
    assertEquals(HttpStatusCode.CREATED.getStatusCode(), asynchronous.getStatusCode());
    assertEquals(body, content(asynchronous));
  }

  /** Answers a create request by echoing back whatever body the invocation could still read. */
  private static class EchoingCreateProcessor extends DefaultProcessor implements EntityProcessor {
    @Override
    public void readEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
        final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
      throw new ODataApplicationException("not used", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    @Override
    public void createEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
        final ContentType requestFormat, final ContentType responseFormat)
        throws ODataApplicationException, ODataLibraryException {
      try {
        response.setContent(new ByteArrayInputStream(request.getBody().readAllBytes()));
      } catch (final IOException e) {
        throw new ODataApplicationException("body unreadable", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
            Locale.ROOT, e);
      }
      response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.JSON.toContentTypeString());
    }

    @Override
    public void updateEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
        final ContentType requestFormat, final ContentType responseFormat)
        throws ODataApplicationException, ODataLibraryException {
      throw new ODataApplicationException("not used", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    @Override
    public void deleteEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
        throws ODataApplicationException, ODataLibraryException {
      throw new ODataApplicationException("not used", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
  }

  private ODataRequest get(final String path, final String prefer) {
    final ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.GET);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath(path);
    request.setRawRequestUri(BASE_URI + "/" + path);
    request.addHeader(HttpHeader.CONTENT_TYPE, Collections.singletonList(ContentType.JSON.toContentTypeString()));
    if (prefer != null) {
      request.addHeader(HttpHeader.PREFER, Collections.singletonList(prefer));
    }
    return request;
  }

  private ODataResponse process(final ODataRequest request, final AsyncSupport asyncSupport) {
    return process(request, asyncSupport, null);
  }

  private ODataResponse process(final ODataRequest request, final AsyncSupport asyncSupport,
      final Processor extraProcessor) {
    final OData odata = OData.newInstance();
    final ServiceMetadata metadata =
        odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final DataProvider dataProvider = new DataProvider(odata, metadata.getEdm());
    final ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));
    handler.register(new TechnicalPrimitiveComplexProcessor(dataProvider, metadata));
    if (extraProcessor != null) {
      handler.register(extraProcessor);
    }
    if (asyncSupport != null) {
      handler.register(asyncSupport);
    }
    final ODataResponse response = handler.process(request);
    assertNotNull(response);
    return response;
  }

  private static String content(final ODataResponse response) throws IOException {
    final InputStream content = response.getContent();
    return content == null ? "" : new String(content.readAllBytes(), StandardCharsets.UTF_8);
  }
}
