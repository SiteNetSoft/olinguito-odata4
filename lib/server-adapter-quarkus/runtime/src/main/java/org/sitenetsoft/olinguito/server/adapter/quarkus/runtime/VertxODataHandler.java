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
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.server.api.ODataContent;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataRequestHandler;
import org.sitenetsoft.olinguito.server.api.ODataResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Vert.x route handler that bridges between Vert.x RoutingContext
 * and the OData request/response model.
 */
public class VertxODataHandler implements Handler<RoutingContext> {

    private final ODataRequestHandler odataHandler;
    private final String basePath;
    private final int split;

    public VertxODataHandler(ODataRequestHandler odataHandler, String basePath, int split) {
        this.odataHandler = odataHandler;
        this.basePath = normalizePath(basePath);
        this.split = split;
        this.odataHandler.setSplit(split);
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest vertxRequest = ctx.request();

        // For requests with body, we need to wait for the body to be fully received
        if (hasBody(vertxRequest)) {
            ctx.request().bodyHandler(body -> processRequest(ctx, body));
        } else {
            processRequest(ctx, null);
        }
    }

    private void processRequest(RoutingContext ctx, Buffer body) {
        try {
            ODataRequest odRequest = buildODataRequest(ctx, body);
            ODataResponse odResponse = odataHandler.process(odRequest);

            if (odResponse == null) {
                ctx.response().setStatusCode(500).end("Internal Server Error");
                return;
            }

            writeResponse(ctx.response(), odResponse);
        } catch (Exception e) {
            ctx.fail(500, e);
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
            VertxODataResponseWriter.writeInputStream(response, content);
        } else if (odataContent != null) {
            VertxODataResponseWriter.writeODataContent(response, odataContent);
        } else {
            response.end();
        }
    }

    private boolean hasBody(HttpServerRequest request) {
        String method = request.method().name();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // Ensure path starts with / and doesn't end with /
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
