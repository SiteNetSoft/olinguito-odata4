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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 9: tests for the asynchronous status monitor
 * resource ([OData-Protocol] sections 11.6 and 8.3.1)
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 9 fix round 1: pin the monitor branch ahead of
 * version validation, and pin the multi-valued result-header copy
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

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
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;

/**
 * [OData-Protocol] section 11.6: a GET to the status monitor answers 202 Accepted with a
 * <code>Location</code> header while the invocation runs and 200 OK once it has completed, either
 * as an <code>application/http</code> message or as the result itself with the
 * <code>AsyncResult</code> header section 8.3.1 requires; a DELETE requests cancellation and gets
 * 405 Method Not Allowed from a service that does not support it.
 */
class AsyncStatusMonitorTest {

  private static final String BASE_URI = "http://localhost/odata";

  /** A monitor URL the OData URI parser cannot parse, exactly as section 11.6 permits. */
  private static final String MONITOR_PATH = "status/1";
  private static final String MONITOR_URI = BASE_URI + "/" + MONITOR_PATH;

  /** An {@link AsyncSupport} that owns one monitor resource whose state the test dictates. */
  private static class MonitorAsyncSupport implements AsyncSupport {
    private AsyncResult result = AsyncResult.notFound();
    private Integer retryAfter;
    private boolean cancelSupported;
    private int cancelCount;

    @Override
    public boolean isStatusMonitorRequest(final ODataRequest request) {
      return MONITOR_PATH.equals(request.getRawODataPath());
    }

    @Override
    public String submit(final ODataRequest request, final AsyncInvocation invocation) {
      return MONITOR_URI;
    }

    @Override
    public AsyncResult resolve(final ODataRequest request) {
      return result;
    }

    @Override
    public boolean cancel(final ODataRequest request) {
      cancelCount++;
      return cancelSupported;
    }

    @Override
    public Integer getRetryAfter() {
      return retryAfter;
    }
  }

  @Test
  void aRunningMonitorAnswers202WithLocation() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.result = AsyncResult.running();

    final ODataResponse response = process(monitor(HttpMethod.GET), async);

    assertEquals(HttpStatusCode.ACCEPTED.getStatusCode(), response.getStatusCode());
    assertEquals(MONITOR_URI, response.getHeader(HttpHeader.LOCATION));
    assertNull(response.getHeader(HttpHeader.RETRY_AFTER));
  }

  @Test
  void aRunningMonitorRepeatsRetryAfterWhenTheServiceSuppliesIt() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.result = AsyncResult.running();
    async.retryAfter = 3;

    final ODataResponse response = process(monitor(HttpMethod.GET), async);

    assertEquals(HttpStatusCode.ACCEPTED.getStatusCode(), response.getStatusCode());
    assertEquals("3", response.getHeader(HttpHeader.RETRY_AFTER));
  }

  @Test
  void anUnknownMonitorAnswers404() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.result = AsyncResult.notFound();

    final ODataResponse response = process(monitor(HttpMethod.GET), async);

    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.getStatusCode());
    assertNull(response.getHeader(HttpHeader.LOCATION));
    assertNull(response.getHeader(HttpHeader.ASYNC_RESULT));
  }

  @Test
  void aCompletedMonitorWrapsTheResultForA40ClientAcceptingApplicationHttp() throws Exception {
    final MonitorAsyncSupport async = completed();
    final ODataRequest request = monitor(HttpMethod.GET);
    request.addHeader(HttpHeader.ODATA_MAX_VERSION, Collections.singletonList("4.0"));
    request.addHeader(HttpHeader.ACCEPT, Collections.singletonList("application/http"));

    final ODataResponse response = process(request, async);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertEquals(ContentType.APPLICATION_HTTP.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
    final String body = content(response);
    assertTrue(body.startsWith("HTTP/1.1 200 OK\r\n"), body);
    assertNull(response.getHeader(HttpHeader.ASYNC_RESULT));
  }

  @Test
  void aCompletedMonitorWrapsTheResultForA40ClientWithNoAcceptHeader() throws Exception {
    final MonitorAsyncSupport async = completed();
    final ODataRequest request = monitor(HttpMethod.GET);
    request.addHeader(HttpHeader.ODATA_MAX_VERSION, Collections.singletonList("4.0"));

    final ODataResponse response = process(request, async);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertEquals(ContentType.APPLICATION_HTTP.toContentTypeString(),
        response.getHeader(HttpHeader.CONTENT_TYPE));
    final String body = content(response);
    assertTrue(body.startsWith("HTTP/1.1 200 OK\r\n"), body);
    assertNull(response.getHeader(HttpHeader.ASYNC_RESULT));
  }

  @Test
  void aCompletedMonitorUnwrapsTheResultAndSetsAsyncResultOtherwise() throws Exception {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    final ODataResponse result = result(HttpStatusCode.CREATED, "{\"value\":\"created\"}");
    result.setHeader(HttpHeader.LOCATION, BASE_URI + "/ESTwoPrim(1)");
    async.result = AsyncResult.completed(result);

    final ODataRequest request = monitor(HttpMethod.GET);
    request.addHeader(HttpHeader.ACCEPT, Collections.singletonList(ContentType.JSON.toContentTypeString()));

    final ODataResponse response = process(request, async);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertEquals("201", response.getHeader(HttpHeader.ASYNC_RESULT));
    assertEquals(ContentType.JSON.toContentTypeString(), response.getHeader(HttpHeader.CONTENT_TYPE));
    assertEquals(BASE_URI + "/ESTwoPrim(1)", response.getHeader(HttpHeader.LOCATION));
    final String body = content(response);
    assertEquals("{\"value\":\"created\"}", body);
    assertFalse(body.startsWith("HTTP/1.1"));
  }

  @Test
  void aRequestWithNeitherAcceptNorMaxVersionIsUnwrapped() throws Exception {
    final ODataResponse response = process(monitor(HttpMethod.GET), completed());

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertEquals("200", response.getHeader(HttpHeader.ASYNC_RESULT));
    assertEquals(ContentType.JSON.toContentTypeString(), response.getHeader(HttpHeader.CONTENT_TYPE));
  }

  @Test
  void asyncResultCarriesTheFinalStatusCodeNotTheMonitorStatus() throws Exception {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.result = AsyncResult.completed(
        result(HttpStatusCode.NOT_FOUND, "{\"error\":{\"code\":null,\"message\":\"gone\"}}"));

    final ODataResponse response = process(monitor(HttpMethod.GET), async);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertEquals("404", response.getHeader(HttpHeader.ASYNC_RESULT));
    assertTrue(content(response).contains("\"error\""));
  }

  @Test
  void deleteCancelsWhenTheServiceSupportsIt() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.cancelSupported = true;

    final ODataResponse response = process(monitor(HttpMethod.DELETE), async);

    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());
    assertEquals(1, async.cancelCount);
    assertNull(response.getHeader(HttpHeader.ALLOW));
  }

  @Test
  void deleteAnswers405WhenTheServiceDoesNotSupportCancellation() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();

    final ODataResponse response = process(monitor(HttpMethod.DELETE), async);

    assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatusCode());
    assertEquals(HttpMethod.GET.name(), response.getHeader(HttpHeader.ALLOW));
  }

  @Test
  void aMonitorRequestIsNeverParsedAsAnODataUri() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.result = AsyncResult.running();

    final ODataResponse response = process(monitor(HttpMethod.GET), async);

    // a plain URI-parser answer would be a 4xx error document, never a 202 with a Location
    assertEquals(HttpStatusCode.ACCEPTED.getStatusCode(), response.getStatusCode());
    assertEquals(MONITOR_URI, response.getHeader(HttpHeader.LOCATION));
  }

  @Test
  void aMonitorRequestIsNeverVersionValidated() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.result = AsyncResult.running();
    final ODataRequest request = monitor(HttpMethod.GET);
    // a version this service knows nothing about: an OData request carrying it is rejected with
    // 400, but a status monitor is not an OData request - section 11.6 requires only that its URL
    // "MUST differ from any other resource URL". This pins the branch ahead of version validation.
    request.addHeader(HttpHeader.ODATA_VERSION, Collections.singletonList("4.02"));

    final ODataResponse response = process(request, async);

    assertEquals(HttpStatusCode.ACCEPTED.getStatusCode(), response.getStatusCode());
    assertEquals(MONITOR_URI, response.getHeader(HttpHeader.LOCATION));
  }

  @Test
  void aBogusODataVersionOnAnOrdinaryRequestIsStillRejected() {
    final ODataRequest request = new ODataRequest();
    request.setMethod(HttpMethod.GET);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath("ESAllPrim");
    request.setRawRequestUri(BASE_URI + "/ESAllPrim");
    request.addHeader(HttpHeader.ODATA_VERSION, Collections.singletonList("4.02"));

    // the counterpart of the test above: version validation is alive and well for everything that
    // is not a status monitor, so the monitor's exemption is the branch placement, not a hole.
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(),
        process(request, new MonitorAsyncSupport()).getStatusCode());
  }

  @Test
  void everyValueOfAMultiValuedResultHeaderSurvivesOntoTheUnwrappedResponse() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    final ODataResponse result = result(HttpStatusCode.OK, "{\"value\":\"done\"}");
    result.addHeader(HttpHeader.PREFERENCE_APPLIED, "respond-async");
    result.addHeader(HttpHeader.PREFERENCE_APPLIED, "return=representation");
    async.result = AsyncResult.completed(result);

    final ODataResponse response = process(monitor(HttpMethod.GET), async);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    // section 11.6: "any other headers ... represent the result of the completed asynchronous
    // operation" - all of them, not just the first value of each name.
    assertEquals(Arrays.asList("respond-async", "return=representation"),
        response.getHeaders(HttpHeader.PREFERENCE_APPLIED));
  }

  @Test
  void withoutAnAsyncSupportAMonitorUrlIsJustAnUnknownResource() {
    final ODataResponse response = process(monitor(HttpMethod.GET), null);

    assertTrue(response.getStatusCode() >= 400, "status " + response.getStatusCode());
    assertNull(response.getHeader(HttpHeader.ASYNC_RESULT));
  }

  private static MonitorAsyncSupport completed() {
    final MonitorAsyncSupport async = new MonitorAsyncSupport();
    async.result = AsyncResult.completed(result(HttpStatusCode.OK, "{\"value\":\"done\"}"));
    return async;
  }

  private static ODataResponse result(final HttpStatusCode status, final String body) {
    final ODataResponse result = new ODataResponse();
    result.setStatusCode(status.getStatusCode());
    result.setHeader(HttpHeader.CONTENT_TYPE, ContentType.JSON.toContentTypeString());
    result.setContent(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    return result;
  }

  private static ODataRequest monitor(final HttpMethod method) {
    final ODataRequest request = new ODataRequest();
    request.setMethod(method);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath(MONITOR_PATH);
    request.setRawRequestUri(MONITOR_URI);
    return request;
  }

  private static ODataResponse process(final ODataRequest request, final AsyncSupport asyncSupport) {
    final OData odata = OData.newInstance();
    final ServiceMetadata metadata =
        odata.createServiceMetadata(new EdmTechProvider(), Collections.emptyList());
    final ODataHandlerImpl handler = new ODataHandlerImpl(odata, metadata, new ServerCoreDebugger(odata));
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
