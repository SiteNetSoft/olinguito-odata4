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
 */
package org.sitenetsoft.olinguito.server.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
import org.sitenetsoft.olinguito.server.api.async.AsyncSupport;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.etag.CustomETagSupport;
import org.sitenetsoft.olinguito.server.api.etag.PreconditionException;
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
    ODataResponse response = new ODataResponse();
    final int responseHandle = debugger.startRuntimeMeasurement("ODataHandler", "process");
    try {
      processInternal(request, response);
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
    debugger.stopRuntimeMeasurement(responseHandle);
    return response;
  }

  private void processInternal(final ODataRequest request, final ODataResponse response)
      throws ODataApplicationException, ODataLibraryException {
    final int measurementHandle = debugger.startRuntimeMeasurement("ODataHandler", "processInternal");

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

    final int measurementDispatcher = debugger.startRuntimeMeasurement("ODataDispatcher", "dispatch");
    try {
      new ODataDispatcher(localUriInfo, this).dispatch(request, response);
    } finally {
      debugger.stopRuntimeMeasurement(measurementDispatcher);
      debugger.stopRuntimeMeasurement(measurementHandle);
    }
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
