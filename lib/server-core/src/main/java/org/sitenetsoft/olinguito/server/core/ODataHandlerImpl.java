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
 * Copyright 2026 SiteNetSoft - OLINGO-1558: Thread-safe URI parsing
 * Copyright 2026 SiteNetSoft - OLINGO-1314: Don't echo raw header values in error messages (XSS)
 * Copyright 2026 SiteNetSoft - OLINGO-1372: Fix error responses ignoring Accept header
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 6: OData 4.01 POST /$query interception
 * (URL Conventions section 4.17) - rewrite a /$query POST request into the equivalent GET
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 6 fix round 1: rebuild rawRequestUri (not just
 * strip it) so context-URL/next-link/delta-link generation sees the merged query, not a stale one
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 2: validate $schemaversion against the service's
 * schema version (OData 4.01, Part 1: Protocol, section 11.2.12) before dispatch
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 1 fix round 1: implement setKeyAsSegment
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 follow-up Task 7: documented the /$query
 * trailing-slash edge (exact-suffix match is deliberate, not an oversight)
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 7: accept AsyncSupport registration
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 8: answer 202 Accepted for the respond-async
 * preference ([OData-Protocol] sections 8.2.8.8 and 11.6) via the registered AsyncSupport
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 9: serve the asynchronous status monitor
 * resource in both result shapes of [OData-Protocol] section 11.6, including the AsyncResult
 * response header of section 8.3.1
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 10: an asynchronously produced response carries
 * the OData-Version header, and a wildcard Accept media range also asks for the wrapped
 * application/http result shape ([OData-Protocol] section 11.6)
 */
package org.sitenetsoft.olinguito.server.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataHandler;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.OlingoExtension;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.async.AsyncResult;
import org.sitenetsoft.olinguito.server.api.async.AsyncSupport;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.etag.CustomETagSupport;
import org.sitenetsoft.olinguito.server.api.etag.PreconditionException;
import org.sitenetsoft.olinguito.server.api.prefer.PreferencesApplied;
import org.sitenetsoft.olinguito.server.api.processor.DefaultProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ErrorProcessor;
import org.sitenetsoft.olinguito.server.api.processor.Processor;
import org.sitenetsoft.olinguito.server.api.serializer.CustomContentTypeSupport;
import org.sitenetsoft.olinguito.server.api.serializer.RepresentationType;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FormatOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SchemaVersionOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOptionKind;
import org.sitenetsoft.olinguito.server.core.debug.ServerCoreDebugger;
import org.sitenetsoft.olinguito.server.core.uri.parser.Parser;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserException;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSemanticException;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSyntaxException;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.FormatOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidator;

/**
 * This class is not thread-safe.
 * Whenever used create a new instance per thread and not reuse the created instance.
 */
public class ODataHandlerImpl implements ODataHandler {

  /** OData 4.01 URL Conventions, section 4.17: the path suffix identifying a $query request. */
  private static final String QUERY_PATH_SEGMENT = "/$query";

  private final OData odata;
  private final ServiceMetadata serviceMetadata;
  private final List<Processor> processors = new LinkedList<>();
  private final ServerCoreDebugger debugger;

  private CustomContentTypeSupport customContentTypeSupport;
  private CustomETagSupport customETagSupport;
  private AsyncSupport asyncSupport;

  private boolean keyAsSegment;
  private UriInfo uriInfo;
  private Exception lastThrownException;

  public ODataHandlerImpl(final OData odata, final ServiceMetadata serviceMetadata, final ServerCoreDebugger debugger) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
    this.debugger = debugger;

    register(new DefaultRedirectProcessor());
    register(new DefaultProcessor());
  }

  public ODataResponse process(final ODataRequest request) {
    final ODataResponse response = new ODataResponse();
    final int responseHandle = debugger.startRuntimeMeasurement("ODataHandler", "process");
    processCatching(request, response, () -> processInternal(request, response));
    debugger.stopRuntimeMeasurement(responseHandle);
    return response;
  }

  /**
   * Runs <code>body</code>, converting any failure into an error response exactly as a synchronous
   * request's failure is converted. Asynchronous invocations run through this too, so an
   * asynchronous failure and a synchronous one produce the same payload.
   *
   * @param request the request being processed, used to negotiate the error representation
   * @param response the response the outcome is written to, returned for convenience
   * @param body the processing attempt to run
   * @return the same <code>response</code> instance that was passed in
   */
  ODataResponse processCatching(final ODataRequest request, final ODataResponse response,
      final ThrowingProcess body) {
    try {
      body.run();
    } catch (final UriValidationException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (final UriParserSemanticException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (final UriParserSyntaxException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (final UriParserException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (AcceptHeaderContentNegotiatorException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (ContentNegotiatorException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (SerializerException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (DeserializerException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (PreconditionException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (ODataHandlerException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e, null);
      handleException(request, response, serverError, e);
    } catch (ODataApplicationException e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e);
      handleException(request, response, serverError, e);
    } catch (Exception e) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e);
      handleException(request, response, serverError, e);
    }
    return response;
  }

  /** The body of a request-processing attempt, run by {@link #processCatching}. */
  interface ThrowingProcess {
    void run() throws ODataApplicationException, ODataLibraryException;
  }

  private void processInternal(final ODataRequest request, final ODataResponse response)
      throws ODataApplicationException, ODataLibraryException {
    final int measurementHandle = debugger.startRuntimeMeasurement("ODataHandler", "processInternal");

    // [OData-Protocol] section 11.6 requires only that "the status monitor resource URL MUST differ
    // from any other resource URL", so a service may mint one the OData URI parser cannot parse.
    // The monitor therefore has to be recognized before version validation and before parsing.
    if (asyncSupport != null && asyncSupport.isStatusMonitorRequest(request)) {
      handleStatusMonitor(request, response);
      debugger.stopRuntimeMeasurement(measurementHandle);
      return;
    }

    response.setHeader(HttpHeader.ODATA_VERSION, ODataServiceVersion.V40.toString());
    
    try {
      validateODataVersion(request);
    } catch (final ODataHandlerException e) {
      debugger.stopRuntimeMeasurement(measurementHandle);
      throw e;
    }

    final int measurementUriParser = debugger.startRuntimeMeasurement("Parser", "parseUri");
    final UriInfo localUriInfo;
    try {
      handleQueryPathIfPresent(request);
      localUriInfo = new Parser(serviceMetadata.getEdm(), odata)
          .setKeyAsSegment(keyAsSegment)
          .parseUri(request.getRawODataPath(), request.getRawQueryPath(), null, request.getRawBaseUri());
      this.uriInfo = localUriInfo;
    } catch (final ODataLibraryException e) {
      debugger.stopRuntimeMeasurement(measurementUriParser);
      debugger.stopRuntimeMeasurement(measurementHandle);
      throw e;
    }
    debugger.stopRuntimeMeasurement(measurementUriParser);

    final int measurementUriValidator = debugger.startRuntimeMeasurement("UriValidator", "validate");
    final HttpMethod method = request.getMethod();
    try {
      new UriValidator().validate(localUriInfo, method);
    } catch (final UriValidationException e) {
      debugger.stopRuntimeMeasurement(measurementUriValidator);
      debugger.stopRuntimeMeasurement(measurementHandle);
      throw e;
    }
    debugger.stopRuntimeMeasurement(measurementUriValidator);

    try {
      validateSchemaVersion(localUriInfo);
    } catch (final ODataApplicationException e) {
      debugger.stopRuntimeMeasurement(measurementHandle);
      throw e;
    }

    // [OData-Protocol] section 8.2.8.8: processing a request asynchronously is a MAY, so this only
    // happens when the service registered an AsyncSupport; without one the preference is ignored
    // and not echoed, which section 11.6 requires (it forbids a 202 without the preference, never
    // the other way round).
    if (asyncSupport != null
        && odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).hasRespondAsync()) {
      submitAsynchronously(request, localUriInfo, response);
      debugger.stopRuntimeMeasurement(measurementHandle);
      return;
    }

    final int measurementDispatcher = debugger.startRuntimeMeasurement("ODataDispatcher", "dispatch");
    try {
      new ODataDispatcher(localUriInfo, this).dispatch(request, response);
    } finally {
      debugger.stopRuntimeMeasurement(measurementDispatcher);
      debugger.stopRuntimeMeasurement(measurementHandle);
    }
  }

  /**
   * Serves a status monitor resource ([OData-Protocol] section 11.6).
   *
   * <p>A GET answers 202 Accepted with a <code>Location</code> header pointing at the monitor
   * itself while the invocation runs, 404 Not Found for a monitor that does not (or no longer)
   * exist, and 200 OK carrying the result once it has completed. A DELETE requests cancellation:
   * 204 No Content when the service cancelled, and 405 Method Not Allowed when it does not support
   * cancellation, because "if a delete request is not supported by the service, the service returns
   * 405 Method Not Allowed".</p>
   *
   * @param request the monitor request
   * @param response the response to write
   * @throws ODataLibraryException if the completed result cannot be serialized
   */
  private void handleStatusMonitor(final ODataRequest request, final ODataResponse response)
      throws ODataLibraryException {
    response.setHeader(HttpHeader.ODATA_VERSION, ODataServiceVersion.V40.toString());
    if (request.getMethod() == HttpMethod.DELETE) {
      if (asyncSupport.cancel(request)) {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      } else {
        response.setStatusCode(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode());
        response.setHeader(HttpHeader.ALLOW, HttpMethod.GET.name());
      }
      return;
    }

    final AsyncResult result = asyncSupport.resolve(request);
    switch (result.getState()) {
    case NOT_FOUND:
      response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
      break;
    case RUNNING:
      response.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());
      response.setHeader(HttpHeader.LOCATION, request.getRawRequestUri());
      final Integer retryAfter = asyncSupport.getRetryAfter();
      if (retryAfter != null) {
        response.setHeader(HttpHeader.RETRY_AFTER, retryAfter.toString());
      }
      break;
    case COMPLETED:
      writeAsynchronousResult(request, response, result.getResponse());
      break;
    default:
      throw new ODataRuntimeException("Unknown asynchronous result state " + result.getState());
    }
  }

  /**
   * Writes the completed result in whichever of section 11.6's two shapes the request asks for: the
   * RFC 7230 HTTP message wrapped in <code>application/http</code>, or the result itself with an
   * <code>AsyncResult</code> header carrying its status code (section 8.3.1).
   *
   * <p>In the unwrapped shape the result's own headers replace the monitor response's, because
   * "any other headers, along with the response body, represent the result of the completed
   * asynchronous operation".</p>
   *
   * @param request the monitor request, which decides the shape
   * @param response the monitor response to write
   * @param result the completed invocation's response
   * @throws ODataLibraryException if the wrapped HTTP message cannot be serialized
   */
  private void writeAsynchronousResult(final ODataRequest request, final ODataResponse response,
      final ODataResponse result) throws ODataLibraryException {
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    if (wantsHttpMessage(request)) {
      response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_HTTP.toContentTypeString());
      response.setContent(odata.createFixedFormatSerializer().asyncResponse(result));
      return;
    }
    for (final Map.Entry<String, List<String>> header : result.getAllHeaders().entrySet()) {
      final List<String> values = header.getValue();
      if (values == null || values.isEmpty()) {
        continue;
      }
      response.setHeader(header.getKey(), values.get(0));
      for (final String value : values.subList(1, values.size())) {
        response.addHeader(header.getKey(), value);
      }
    }
    response.setHeader(HttpHeader.ASYNC_RESULT, Integer.toString(result.getStatusCode()));
    if (result.getContent() != null) {
      response.setContent(result.getContent());
    } else if (result.getODataContent() != null) {
      response.setODataContent(result.getODataContent());
    }
  }

  /**
   * Whether the monitor request asks for the wrapped HTTP-message shape. Section 11.6 names one
   * case: the request "includes an OData-MaxVersion header with a value of 4.0 and no Accept
   * header, or an Accept header that includes application/http".
   *
   * <p>An Accept header "includes application/http" either by naming it or through a media range
   * that covers it — <code>application/*</code>, <code>*&#47;*</code> or the bare <code>*</code> a
   * JDK <code>HttpURLConnection</code> sends by default. The test stays a plain look at the media
   * ranges rather than a full negotiation: a monitor request is not an OData request, so an Accept
   * header the OData content negotiator dislikes must not make it fail, and <code>q</code> values
   * are not honoured.</p>
   *
   * <p>A request carrying neither header is not named by either sentence of section 11.6; it is
   * answered with the unwrapped shape (a recorded decision), following section 13.2.1's Minimal
   * conformance MUST for a 4.01 service to "return the AsyncResult result header in the final
   * response to an asynchronous request".</p>
   *
   * @param request the monitor request
   * @return whether the result must be wrapped as an <code>application/http</code> message
   */
  private static boolean wantsHttpMessage(final ODataRequest request) {
    final String accept = request.getHeader(HttpHeader.ACCEPT);
    if (accept != null) {
      return acceptCoversHttpMessage(accept);
    }
    final String maxVersion = request.getHeader(HttpHeader.ODATA_MAX_VERSION);
    return maxVersion != null && ODataServiceVersion.V40.toString().equals(maxVersion.trim());
  }

  /** Whether any media range of this Accept header covers <code>application/http</code>. */
  private static boolean acceptCoversHttpMessage(final String accept) {
    for (final String part : accept.split(",")) {
      final int parameterStart = part.indexOf(';');
      final String range = (parameterStart < 0 ? part : part.substring(0, parameterStart))
          .trim().toLowerCase(Locale.ROOT);
      if (ContentType.APPLICATION_HTTP.toContentTypeString().equals(range)
          || "application/*".equals(range) || "*/*".equals(range) || "*".equals(range)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Hands the request to the registered {@link AsyncSupport} and answers 202 Accepted.
   *
   * <p>[OData-Protocol] section 11.6: a 202 response "MUST include a Location header pointing to a
   * status monitor resource ... in addition to an optional Retry-After header"; section 8.2.8.8:
   * the service "MUST include a Preference-Applied response header containing the respond-async
   * preference".</p>
   *
   * <p>The submitted closure captures the already-parsed <code>localUriInfo</code> and never reads
   * the handler's mutable {@link #uriInfo} field, so a later request on the same handler cannot
   * corrupt an in-flight asynchronous dispatch. It dispatches directly rather than re-entering
   * {@link #process(ODataRequest)}, which would see the preference again and submit forever.</p>
   *
   * @param request the incoming request
   * @param localUriInfo the parsed URI info of that request
   * @param response the response the 202 is written to
   * @throws ODataLibraryException if the request body cannot be buffered for replay
   */
  private void submitAsynchronously(final ODataRequest request, final UriInfo localUriInfo,
      final ODataResponse response) throws ODataLibraryException {
    final ODataRequest detached = detach(request);
    final String location = asyncSupport.submit(detached, () -> {
      final ODataResponse asyncResponse = new ODataResponse();
      // The deferred result is a complete OData response in its own right, so it carries the
      // OData-Version header a synchronously dispatched response would have carried.
      asyncResponse.setHeader(HttpHeader.ODATA_VERSION, ODataServiceVersion.V40.toString());
      return processCatching(detached, asyncResponse,
          () -> new ODataDispatcher(localUriInfo, this).dispatch(detached, asyncResponse));
    });
    response.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());
    response.setHeader(HttpHeader.LOCATION, location);
    response.setHeader(HttpHeader.PREFERENCE_APPLIED,
        PreferencesApplied.with().respondAsync().build().toValueString());
    final Integer retryAfter = asyncSupport.getRetryAfter();
    if (retryAfter != null) {
      response.setHeader(HttpHeader.RETRY_AFTER, retryAfter.toString());
    }
  }

  /**
   * Copies a request so it can be replayed on another thread after the container has recycled the
   * original - the body is read into memory, because an <code>InputStream</code> handed over by a
   * servlet container is not valid once the response has been sent.
   *
   * @param request the request to copy
   * @return a detached copy carrying an in-memory body
   * @throws ODataLibraryException if the body cannot be read
   */
  private static ODataRequest detach(final ODataRequest request) throws ODataLibraryException {
    final ODataRequest copy = new ODataRequest();
    copy.setMethod(request.getMethod());
    copy.setProtocol(request.getProtocol());
    copy.setRawBaseUri(request.getRawBaseUri());
    copy.setRawODataPath(request.getRawODataPath());
    copy.setRawQueryPath(request.getRawQueryPath());
    copy.setRawRequestUri(request.getRawRequestUri());
    copy.setRawServiceResolutionUri(request.getRawServiceResolutionUri());
    for (final Map.Entry<String, List<String>> header : request.getAllHeaders().entrySet()) {
      copy.addHeader(header.getKey(), header.getValue());
    }
    if (request.getBody() != null) {
      try {
        copy.setBody(new ByteArrayInputStream(request.getBody().readAllBytes()));
      } catch (final IOException e) {
        throw new DeserializerException("Could not buffer the request body for asynchronous processing.",
            e, DeserializerException.MessageKeys.IO_EXCEPTION);
      }
    }
    return copy;
  }

  /**
   * OData 4.01, Part 1: Protocol, section 11.2.12: a request MAY carry the <code>$schemaversion</code>
   * system query option. If the service publishes a schema version (see
   * {@link ServiceMetadata#getSchemaVersion()}) and the requested version is neither <code>*</code>
   * (meaning "current version", which always matches) nor equal to the service's version, the
   * requested version does not exist and the request MUST be rejected with 404 Not Found. A service
   * that does not publish a schema version has no version to check against, so the option is
   * accepted but has no effect (recorded decision: no version source = accept-and-ignore).
   *
   * @param localUriInfo the parsed URI info of the current request
   * @throws ODataApplicationException with a 404 status if the requested schema version does not
   * exist on this service
   */
  private void validateSchemaVersion(final UriInfo localUriInfo) throws ODataApplicationException {
    final SchemaVersionOption schemaVersionOption = localUriInfo.getSchemaVersionOption();
    if (schemaVersionOption == null) {
      return;
    }
    final String serviceSchemaVersion = serviceMetadata.getSchemaVersion();
    if (serviceSchemaVersion == null) {
      return;
    }
    final String requestedSchemaVersion = schemaVersionOption.getSchemaVersion();
    if (!"*".equals(requestedSchemaVersion) && !serviceSchemaVersion.equals(requestedSchemaVersion)) {
      throw new ODataApplicationException("Schema version '" + requestedSchemaVersion + "' does not exist.",
          HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
    }
  }

  public void handleException(final ODataRequest request, final ODataResponse response,
      final ODataServerError serverError, final Exception exception) {
    final int measurementHandle = debugger.startRuntimeMeasurement("ODataHandler", "handleException");
    lastThrownException = exception;
    ErrorProcessor exceptionProcessor;
    try {
      exceptionProcessor = selectProcessor(ErrorProcessor.class);
    } catch (ODataHandlerException e) {
      // This cannot happen since there is always an ExceptionProcessor registered.
      exceptionProcessor = new DefaultProcessor();
    }
    ContentType requestedContentType;
    try {
      final FormatOption formatOption = getFormatOption(request, uriInfo);
      requestedContentType = ContentNegotiator.doContentNegotiation(formatOption, request,
          getCustomContentTypeSupport(), RepresentationType.ERROR);
    } catch (final AcceptHeaderContentNegotiatorException e) {
      requestedContentType = ContentType.JSON;
    } catch (final ContentNegotiatorException e) {
      requestedContentType = ContentType.JSON;
    }
    final int measurementError = debugger.startRuntimeMeasurement("ErrorProcessor", "processError");
    exceptionProcessor.processError(request, response, serverError, requestedContentType);
    debugger.stopRuntimeMeasurement(measurementError);
    debugger.stopRuntimeMeasurement(measurementHandle);
  }

  /**
   * Extract format option from either <code>uriInfo</code> (if not <code>NULL</code>)
   * or query from <code>request</code> (if not <code>NULL</code>).
   * If both options are <code>NULL</code>, <code>NULL</code> is returned.
   *
   * @param request request which is checked
   * @param uriInfo uriInfo which is checked
   * @return the evaluated format option or <code>NULL</code>.
   */
  private FormatOption getFormatOption(final ODataRequest request, final UriInfo uriInfo) {
    if(uriInfo == null) {
      String query = request.getRawQueryPath();
      if(query == null) {
        return null;
      }

      final String formatPrefix = SystemQueryOptionKind.FORMAT.toString() + "=";
      int index = query.indexOf(formatPrefix);
      if(index == -1) {
        return null;
      }
      int valueStart = index + formatPrefix.length();
      int endIndex = query.indexOf('&', valueStart);
      if(endIndex == -1) {
        endIndex = query.length();
      }
      String format = query.substring(valueStart, endIndex);
      if (format.isEmpty()) {
        return null;
      }
      return new FormatOptionImpl().setFormat(format);
    }
    return uriInfo.getFormatOption();
  }

  /**
   * OData 4.01 URL Conventions, section 4.17: a request whose resource path ends in
   * <code>/$query</code> MUST use the POST verb and MUST carry a <code>text/plain</code> body
   * of percent-encoded query options. Those options are processed together with any query
   * options already present in the request URL. This method rewrites such a request in place
   * into the equivalent GET request (segment stripped, body merged into the raw query path,
   * method forced to GET) before the URI is handed to the {@link Parser}.
   *
   * @param request the incoming request, mutated in place when its path ends in
   * <code>/$query</code>
   * @throws ODataHandlerException if the method is not POST or the Content-Type is not
   * <code>text/plain</code>-compatible, or the body cannot be read
   */
  private void handleQueryPathIfPresent(final ODataRequest request) throws ODataHandlerException {
    final String rawODataPath = request.getRawODataPath();
    // OData 4.01 URL Conventions section 4.17 names the segment "/$query" and this match is on the
    // path's exact suffix, deliberately: a trailing slash ("ESAllPrim/$query/") is NOT a $query
    // request. It is left untouched here and the URI parser then rejects the literal "$query"
    // segment with a 400, which is the intended outcome - silently accepting the variant would make
    // the rewritten path ambiguous with a real resource segment named "$query".
    if (rawODataPath == null || !rawODataPath.endsWith(QUERY_PATH_SEGMENT)) {
      return;
    }

    final HttpMethod method = request.getMethod();
    if (method != HttpMethod.POST) {
      throw new ODataHandlerException("HTTP method " + method + " is not allowed for a $query path.",
          ODataHandlerException.MessageKeys.HTTP_METHOD_NOT_ALLOWED, method == null ? "null" : method.toString());
    }

    final String contentTypeHeader = request.getHeader(HttpHeader.CONTENT_TYPE);
    final ContentType contentType = contentTypeHeader == null ? null : ContentType.parse(contentTypeHeader);
    if (contentType == null || !ContentType.TEXT_PLAIN.isCompatible(contentType)) {
      throw new ODataHandlerException("The $query request body must have content type text/plain.",
          ODataHandlerException.MessageKeys.INVALID_QUERY_BODY_CONTENT_TYPE);
    }

    final String body = readQueryBody(request).trim();

    final String strippedODataPath = rawODataPath.substring(0, rawODataPath.length() - QUERY_PATH_SEGMENT.length());
    request.setRawODataPath(strippedODataPath);

    if (!body.isEmpty()) {
      final String rawQueryPath = request.getRawQueryPath();
      request.setRawQueryPath(
          rawQueryPath == null || rawQueryPath.isEmpty() ? body : rawQueryPath + "&" + body);
    }
    final String mergedQuery = request.getRawQueryPath() == null ? "" : request.getRawQueryPath();

    rebuildRawRequestUri(request, rawODataPath, strippedODataPath, mergedQuery);

    request.setMethod(HttpMethod.GET);
  }

  /**
   * Real adapters (servlet, netty) populate <code>rawRequestUri</code> as
   * <code>&lt;scheme-authority-and-path&gt; + ("?" + queryString)</code> - i.e. the full request
   * path (base URI + OData path), with any query string appended verbatim. Since the query part of
   * a <code>/$query</code> request changes (the body is merged in, replacing whatever query string
   * was there before the rewrite), the whole field has to be rebuilt rather than merely having its
   * trailing <code>/$query</code> segment stripped, otherwise context-URL / next-link / delta-link
   * generation downstream (which reads this field directly) would either keep a stale
   * <code>/$query</code> segment or silently drop the merged query options.
   *
   * @param request the request being rewritten; its <code>rawODataPath</code> has already been
   * updated to <code>strippedODataPath</code> and its <code>rawQueryPath</code> to the final merged
   * value by the time this is called
   * @param originalODataPath the OData path as it was before stripping, i.e. still ending in
   * <code>/$query</code>
   * @param strippedODataPath the OData path with the trailing <code>/$query</code> segment removed
   * @param mergedQuery the final merged query string (URL query options plus body options),
   * possibly empty but never <code>null</code>
   */
  private static void rebuildRawRequestUri(final ODataRequest request, final String originalODataPath,
      final String strippedODataPath, final String mergedQuery) {
    final String rawRequestUri = request.getRawRequestUri();
    if (rawRequestUri == null) {
      return;
    }

    final int queryIndex = rawRequestUri.indexOf('?');
    final String pathPart = queryIndex == -1 ? rawRequestUri : rawRequestUri.substring(0, queryIndex);

    final String newPathPart;
    if (pathPart.endsWith(originalODataPath)) {
      // The common/adapter-populated case: rawRequestUri's path portion is base URI + rawODataPath,
      // so replacing the original OData path suffix with the stripped one keeps everything else
      // (scheme, authority, any path prefix) untouched.
      newPathPart = pathPart.substring(0, pathPart.length() - originalODataPath.length()) + strippedODataPath;
    } else if (pathPart.endsWith(QUERY_PATH_SEGMENT)) {
      // Path portion ends in /$query but isn't literally base+rawODataPath (e.g. differing
      // percent-encoding) - fall back to stripping just the segment.
      newPathPart = pathPart.substring(0, pathPart.length() - QUERY_PATH_SEGMENT.length());
    } else {
      // Path portion doesn't reflect the /$query segment at all; leave it as-is rather than guess.
      newPathPart = pathPart;
    }

    request.setRawRequestUri(mergedQuery.isEmpty() ? newPathPart : newPathPart + "?" + mergedQuery);
  }

  private static String readQueryBody(final ODataRequest request) throws ODataHandlerException {
    final InputStream body = request.getBody();
    if (body == null) {
      return "";
    }
    try {
      return new String(body.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new ODataHandlerException("Could not read the $query request body.", e,
          ODataHandlerException.MessageKeys.INVALID_PAYLOAD);
    }
  }

  private void validateODataVersion(final ODataRequest request) throws ODataHandlerException {
    final String odataVersion = request.getHeader(HttpHeader.ODATA_VERSION);
   if (odataVersion != null && !ODataServiceVersion.isValidODataVersion(odataVersion)) {
      throw new ODataHandlerException("ODataVersion not supported.",
          ODataHandlerException.MessageKeys.ODATA_VERSION_NOT_SUPPORTED,
          ODataServiceVersion.V40.toString());
    }

    final String maxVersion = request.getHeader(HttpHeader.ODATA_MAX_VERSION);
    if (maxVersion != null && !ODataServiceVersion.isValidMaxODataVersion(maxVersion)) {
        throw new ODataHandlerException("ODataVersion not supported.",
            ODataHandlerException.MessageKeys.ODATA_VERSION_NOT_SUPPORTED,
            ODataServiceVersion.V40.toString());
      }
  }

  <T extends Processor> T selectProcessor(final Class<T> cls) throws ODataHandlerException {
    for (final Processor processor : processors) {
      if (cls.isAssignableFrom(processor.getClass())) {
        processor.init(odata, serviceMetadata);
        return cls.cast(processor);
      }
    }
    throw new ODataHandlerException("Processor: " + cls.getSimpleName() + " not registered.",
        ODataHandlerException.MessageKeys.PROCESSOR_NOT_IMPLEMENTED, cls.getSimpleName());
  }

  public void register(final Processor processor) {
    processors.add(0, processor);
  }

  @Override
  public void register(OlingoExtension extension) {
    if(extension instanceof CustomContentTypeSupport customContentType) {
      this.customContentTypeSupport = customContentType;
    } else if(extension instanceof CustomETagSupport customETag) {
      this.customETagSupport = customETag;
    } else if (extension instanceof AsyncSupport asyncSupportExtension) {
      this.asyncSupport = asyncSupportExtension;
    } else {
      throw new ODataRuntimeException("Got not supported exception with class name " +
          extension.getClass().getSimpleName());
    }
  }

  @Override
  public void setKeyAsSegment(final boolean enabled) {
    this.keyAsSegment = enabled;
  }

  public CustomContentTypeSupport getCustomContentTypeSupport() {
    return customContentTypeSupport;
  }

  public CustomETagSupport getCustomETagSupport() {
    return customETagSupport;
  }

  AsyncSupport getAsyncSupport() {
    return asyncSupport;
  }

  public Exception getLastThrownException() {
    return lastThrownException;
  }

  public UriInfo getUriInfo() {
    return uriInfo;
  }
}
