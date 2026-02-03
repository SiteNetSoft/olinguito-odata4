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
 */
package org.sitenetsoft.olinguito.server.tecsvc.quarkus;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceInclude;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataContent;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataRequestHandler;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.tecsvc.ETagSupport;
import org.sitenetsoft.olinguito.server.tecsvc.MetadataETagSupport;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.tecsvc.processor.TechnicalActionProcessor;
import org.sitenetsoft.olinguito.server.tecsvc.processor.TechnicalBatchProcessor;
import org.sitenetsoft.olinguito.server.tecsvc.processor.TechnicalEntityProcessor;
import org.sitenetsoft.olinguito.server.tecsvc.processor.TechnicalPrimitiveComplexProcessor;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vert.x route handler for the Technical OData Service.
 * Handles session management and processes OData requests.
 */
public class TechnicalODataHandler implements Handler<RoutingContext> {

    private static final Logger LOG = LoggerFactory.getLogger(TechnicalODataHandler.class);

    private static final String METADATA_ETAG = "W/\"" + UUID.randomUUID() + "\"";

    private final SessionManager sessionManager;
    private final String basePath;
    private final int split;

    // Cached OData and ServiceMetadata (they are stateless)
    private final OData odata;
    private final ServiceMetadata serviceMetadata;

    public TechnicalODataHandler(SessionManager sessionManager, String basePath, int split) {
        this.sessionManager = sessionManager;
        this.basePath = normalizePath(basePath);
        this.split = split;

        // Initialize OData and ServiceMetadata once
        this.odata = OData.newInstance();
        EdmxReference reference = new EdmxReference(URI.create("../v4.0/cs02/vocabularies/Org.OData.Core.V1.xml"));
        reference.addInclude(new EdmxReferenceInclude("Org.OData.Core.V1", "Core"));
        this.serviceMetadata = odata.createServiceMetadata(
                new EdmTechProvider(),
                Collections.singletonList(reference),
                new MetadataETagSupport(METADATA_ETAG));
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest vertxRequest = ctx.request();

        // Handle requests with body
        if (hasBody(vertxRequest)) {
            ctx.request().bodyHandler(body -> processRequest(ctx, body));
        } else {
            processRequest(ctx, null);
        }
    }

    private void processRequest(RoutingContext ctx, Buffer body) {
        try {
            // Get or create session
            String sessionId = getOrCreateSessionId(ctx);

            // Get or create DataProvider for this session
            DataProvider dataProvider = sessionManager.getOrCreate(sessionId, odata, serviceMetadata.getEdm());

            // Create a new handler with processors for this request
            ODataRequestHandler handler = odata.createHandler(serviceMetadata);
            handler.setSplit(split);

            // Register processors
            handler.register(new TechnicalEntityProcessor(dataProvider, serviceMetadata));
            handler.register(new TechnicalPrimitiveComplexProcessor(dataProvider, serviceMetadata));
            handler.register(new TechnicalActionProcessor(dataProvider, serviceMetadata));
            handler.register(new TechnicalBatchProcessor(dataProvider));
            handler.register(new ETagSupport());

            // Build OData request
            ODataRequest odRequest = buildODataRequest(ctx, body);

            // Process the request
            ODataResponse odResponse = handler.process(odRequest);

            if (odResponse == null) {
                ctx.response().setStatusCode(500).end("Internal Server Error: null response");
                return;
            }

            // Set session cookie if new
            setSessionCookie(ctx, sessionId);

            // Write response
            writeResponse(ctx.response(), odResponse);
        } catch (Exception e) {
            LOG.error("Error processing OData request", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            if (!ctx.response().ended()) {
                ctx.response()
                        .setStatusCode(500)
                        .putHeader("Content-Type", "text/plain")
                        .end("Internal Server Error: " + errorMessage);
            }
        }
    }

    private String getOrCreateSessionId(RoutingContext ctx) {
        Cookie cookie = ctx.request().getCookie(SessionManager.SESSION_COOKIE_NAME);
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
            return cookie.getValue();
        }
        return sessionManager.generateSessionId();
    }

    private void setSessionCookie(RoutingContext ctx, String sessionId) {
        Cookie existingCookie = ctx.request().getCookie(SessionManager.SESSION_COOKIE_NAME);
        if (existingCookie == null) {
            ctx.response().addCookie(Cookie.cookie(SessionManager.SESSION_COOKIE_NAME, sessionId)
                    .setPath("/"));
        }
    }

    private ODataRequest buildODataRequest(RoutingContext ctx, Buffer body) {
        HttpServerRequest vertxRequest = ctx.request();
        ODataRequest odRequest = new ODataRequest();

        // Set HTTP method
        HttpMethod httpMethod = extractMethod(vertxRequest);
        odRequest.setMethod(httpMethod);

        // Set protocol
        odRequest.setProtocol(vertxRequest.version().name());

        // Copy headers
        copyHeaders(odRequest, vertxRequest);

        // Set body
        if (body != null && body.length() > 0) {
            odRequest.setBody(new ByteArrayInputStream(body.getBytes()));
        }

        // Fill URI information
        fillUriInformation(odRequest, ctx);

        return odRequest;
    }

    private HttpMethod extractMethod(HttpServerRequest request) {
        HttpMethod httpMethod;
        String methodName = request.method().name();

        try {
            httpMethod = HttpMethod.valueOf(methodName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("HTTP method not allowed: " + methodName, e);
        }

        // Handle method tunneling for POST requests
        if (httpMethod != HttpMethod.POST) {
            return httpMethod;
        }

        String xHttpMethod = request.getHeader(HttpHeader.X_HTTP_METHOD);
        String xHttpMethodOverride = request.getHeader(HttpHeader.X_HTTP_METHOD_OVERRIDE);

        if (xHttpMethod == null && xHttpMethodOverride == null) {
            return httpMethod;
        } else if (xHttpMethod == null) {
            return HttpMethod.valueOf(xHttpMethodOverride);
        } else if (xHttpMethodOverride == null) {
            return HttpMethod.valueOf(xHttpMethod);
        } else {
            if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
                throw new IllegalArgumentException(
                        "Ambiguous X-HTTP-Methods: " + xHttpMethod + " vs " + xHttpMethodOverride);
            }
            return HttpMethod.valueOf(xHttpMethod);
        }
    }

    private void copyHeaders(ODataRequest odRequest, HttpServerRequest vertxRequest) {
        for (Map.Entry<String, String> header : vertxRequest.headers()) {
            odRequest.addHeader(header.getKey(), header.getValue());
        }
    }

    private void fillUriInformation(ODataRequest odRequest, RoutingContext ctx) {
        HttpServerRequest request = ctx.request();

        // Build raw request URI
        String scheme = request.isSSL() ? "https" : "http";
        String host = request.getHeader("Host");
        if (host == null) {
            host = request.authority() != null ? request.authority().toString() : "localhost";
        }

        String path = request.path();
        String query = request.query();

        String rawRequestUri = scheme + "://" + host + path;
        if (query != null && !query.isEmpty()) {
            rawRequestUri += "?" + query;
        }

        // Calculate OData path (path after the base path)
        String rawODataPath = path;
        if (basePath != null && !basePath.isEmpty() && path.startsWith(basePath)) {
            rawODataPath = path.substring(basePath.length());
            if (!rawODataPath.startsWith("/") && !rawODataPath.isEmpty()) {
                rawODataPath = "/" + rawODataPath;
            }
        }

        // Handle service resolution (split)
        String rawServiceResolutionUri = null;
        if (split > 0) {
            rawServiceResolutionUri = rawODataPath;
            String tempPath = rawODataPath;
            for (int i = 0; i < split; i++) {
                int index = tempPath.indexOf('/', 1);
                if (index == -1) {
                    tempPath = "";
                    break;
                } else {
                    tempPath = tempPath.substring(index);
                }
            }
            int end = rawServiceResolutionUri.length() - tempPath.length();
            rawServiceResolutionUri = rawServiceResolutionUri.substring(0, end);
            rawODataPath = tempPath;
        }

        // Calculate base URI
        String rawBaseUri = scheme + "://" + host + basePath;
        if (rawServiceResolutionUri != null) {
            rawBaseUri += rawServiceResolutionUri;
        }

        odRequest.setRawRequestUri(rawRequestUri);
        odRequest.setRawBaseUri(rawBaseUri);
        odRequest.setRawODataPath(rawODataPath);
        odRequest.setRawQueryPath(query);
        odRequest.setRawServiceResolutionUri(rawServiceResolutionUri);
    }

    private void writeResponse(HttpServerResponse response, ODataResponse odResponse) {
        try {
            // Set status code
            response.setStatusCode(odResponse.getStatusCode());

            // Copy headers
            Map<String, List<String>> headers = odResponse.getAllHeaders();
            if (headers != null) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String name = entry.getKey();
                    for (String value : entry.getValue()) {
                        if (HttpHeader.CONTENT_TYPE.equalsIgnoreCase(name)) {
                            response.putHeader(name, value);
                        } else {
                            response.headers().add(name, value);
                        }
                    }
                }
            }

            // Write body
            InputStream content = odResponse.getContent();
            ODataContent odataContent = odResponse.getODataContent();

            if (content != null) {
                writeInputStream(response, content);
            } else if (odataContent != null) {
                writeODataContent(response, odataContent);
            } else {
                response.end();
            }
        } catch (Exception e) {
            LOG.error("Error writing response", e);
            if (!response.ended()) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                response.setStatusCode(500)
                        .putHeader("Content-Type", "text/plain")
                        .end("Error writing response: " + errorMessage);
            }
        }
    }

    private void writeInputStream(HttpServerResponse response, InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        response.end(Buffer.buffer(baos.toByteArray()));
    }

    private void writeODataContent(HttpServerResponse response, ODataContent content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        content.write(baos);
        response.end(Buffer.buffer(baos.toByteArray()));
    }

    private boolean hasBody(HttpServerRequest request) {
        String method = request.method().name();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String normalized = path;
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
