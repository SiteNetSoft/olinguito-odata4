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
package org.sitenetsoft.olinguito.server.adapter.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataRequestHandler;
import org.sitenetsoft.olinguito.server.api.ODataResponse;

/**
 * Servlet-based adapter that maps HttpServletRequest/Response
 * to ODataRequest/ODataResponse and delegates to the core handler.
 *
 * This lives in server-adapter-servlet so that server-core
 * remains HTTP/servlet-agnostic.
 */
public final class ServletODataAdapter implements ODataServletHandler {

    /**
     * Attribute name used by Spring-based apps to store the request mapping.
     * Mirrors the old ODataHttpHandlerImpl behavior for compatibility.
     */
    private static final String REQUEST_MAPPING_ATTR = "requestMapping";

    private final ODataRequestHandler core;

    /**
     * Number of leading path segments that belong to the "service resolution"
     * part (see old ODataHttpHandlerImpl semantics).
     */
    private int split = 0;

    public ServletODataAdapter(ODataRequestHandler core) {
        this.core = core;
    }

    @Override
    public void setSplit(int split) {
        this.split = split;
        core.setSplit(split);
    }

    @Override
    public void process(HttpServletRequest req, HttpServletResponse resp) {
        ODataRequest odReq = new ODataRequest();

        try {
            // Body & protocol
            odReq.setBody(req.getInputStream());
            odReq.setProtocol(req.getProtocol());

            // HTTP method (GET, POST, etc., including X-HTTP-Method override)
            odReq.setMethod(extractMethod(req));

            // Headers
            copyHeaders(odReq, req);

            // URI information (base URI, OData path, query, service resolution)
            fillUriInformation(odReq, req, split);

            // Delegate to the core handler
            ODataResponse odResp = core.process(odReq);

            if (odResp == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // HTTP status
            int statusCode = odResp.getStatusCode();
            if (statusCode > 0) {
                resp.setStatus(statusCode);
            }

            // Response headers
            Map<String, List<String>> responseHeaders = odResp.getAllHeaders();
            if (responseHeaders != null) {
                for (Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
                    String name = header.getKey();
                    for (String value : header.getValue()) {
                        // addHeader to preserve multi-value headers
                        resp.addHeader(name, value);
                    }
                }
            }

            /*Map<String, List<String>> responseHeaders = odResp.getAllHeaders();
            if (responseHeaders != null) {
                for (Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
                    String name = header.getKey();
                    for (String value : header.getValue()) {
                        // addHeader to preserve multi-value headers
                        resp.addHeader(name, value);
                    }
                }
            }

            normalizeResponseContentType(resp);*/

            // 🔧 Last-chance normalization for JSON content type:
            // If it's plain application/json with no odata.metadata parameter,
            // default to OData JSON minimal (as in original Olingo).
            /*String ct = resp.getHeader(HttpHeader.CONTENT_TYPE);
            if (ct == null) {
                ct = resp.getContentType();
            }
            if (ct != null && "application/json".equalsIgnoreCase(ct.trim())) {
                resp.setHeader(HttpHeader.CONTENT_TYPE, "application/json;odata.metadata=minimal");
            }*/

            // Response body
            /*InputStream content = odResp.getContent();
            if (content != null) {
                // Last chance before commit
                normalizeResponseContentType(resp);

                try (InputStream in = content;
                     OutputStream out = resp.getOutputStream()) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
            }*/

            InputStream content = odResp.getContent();
            if (content != null) {
                try (InputStream in = content;
                     OutputStream out = resp.getOutputStream()) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
            }

            System.out.println("ODATA RESPONSE CT = " + odResp.getHeader(HttpHeader.CONTENT_TYPE));

        } catch (IOException e) {
            // If something goes wrong at servlet I/O level, surface as 500
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException ignored) {
                // nothing else we can do here
            }
        }
    }

    // ------------------------------------------------------------------------
    // Helper methods (ported from the old ODataHttpHandlerImpl, but
    // kept local and servlet-only so core remains HTTP-agnostic.
    // ------------------------------------------------------------------------

    /**
     * Determine the effective HTTP method, taking into account
     * X-HTTP-Method / X-HTTP-Method-Override for tunneling.
     * This version throws IllegalArgumentException on invalid methods.
     */
    private static HttpMethod extractMethod(final HttpServletRequest httpRequest) {
        HttpMethod httpRequestMethod;
        String requestMethodName = httpRequest.getMethod();

        try {
            httpRequestMethod = HttpMethod.valueOf(requestMethodName);
        } catch (IllegalArgumentException e) {
            // In the original code this became an ODataHandlerException.
            // Here we let it bubble as IllegalArgumentException.
            throw new IllegalArgumentException("HTTP method not allowed: " + requestMethodName, e);
        }

        if (httpRequestMethod != HttpMethod.POST) {
            return httpRequestMethod;
        }

        String xHttpMethod = httpRequest.getHeader(HttpHeader.X_HTTP_METHOD);
        String xHttpMethodOverride = httpRequest.getHeader(HttpHeader.X_HTTP_METHOD_OVERRIDE);

        if (xHttpMethod == null && xHttpMethodOverride == null) {
            return httpRequestMethod;
        } else if (xHttpMethod == null) {
            return HttpMethod.valueOf(xHttpMethodOverride);
        } else if (xHttpMethodOverride == null) {
            return HttpMethod.valueOf(xHttpMethod);
        } else {
            if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
                throw new IllegalArgumentException(
                        "Ambiguous X-HTTP-Methods: " + xHttpMethod + " vs " + xHttpMethodOverride
                );
            }
            return HttpMethod.valueOf(xHttpMethod);
        }
    }

    /**
     * Copy servlet headers into the ODataRequest, preserving multi-valued headers.
     */
    private static void copyHeaders(final ODataRequest odRequest, final HttpServletRequest req) {
        Enumeration<String> headerNames = req.getHeaderNames();
        if (headerNames == null) {
            return;
        }

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            @SuppressWarnings("unchecked")
            final List<String> headerValues = Collections.list(req.getHeaders(headerName));
            odRequest.addHeader(headerName, headerValues);
        }
    }

    /**
     * Fill URI-related information on the ODataRequest:
     *  - rawRequestUri
     *  - rawBaseUri
     *  - rawODataPath
     *  - rawServiceResolutionUri
     *  - rawQueryPath
     *
     * Matches the behavior of the original ODataHttpHandlerImpl.
     */
    private static void fillUriInformation(final ODataRequest odRequest,
                                           final HttpServletRequest httpRequest,
                                           final int split) {

        String rawRequestUri = httpRequest.getRequestURL().toString();

        String rawServiceResolutionUri = null;
        String rawODataPath;

        // Spring-style requestMapping support
        Object mappingAttr = httpRequest.getAttribute(REQUEST_MAPPING_ATTR);
        if (mappingAttr != null) {
            String requestMapping = mappingAttr.toString();
            rawServiceResolutionUri = requestMapping;
            int beginIndex = rawRequestUri.indexOf(requestMapping) + requestMapping.length();
            rawODataPath = rawRequestUri.substring(beginIndex);
        } else if (!"".equals(httpRequest.getServletPath())) {
            int beginIndex = rawRequestUri.indexOf(httpRequest.getServletPath())
                    + httpRequest.getServletPath().length();
            rawODataPath = rawRequestUri.substring(beginIndex);
        } else if (!"".equals(httpRequest.getContextPath())) {
            int beginIndex = rawRequestUri.indexOf(httpRequest.getContextPath())
                    + httpRequest.getContextPath().length();
            rawODataPath = rawRequestUri.substring(beginIndex);
        } else {
            rawODataPath = httpRequest.getRequestURI();
        }

        // Service resolution split handling
        if (split > 0) {
            rawServiceResolutionUri = rawODataPath;
            for (int i = 0; i < split; i++) {
                int index = rawODataPath.indexOf('/', 1);
                if (index == -1) {
                    rawODataPath = "";
                    break;
                } else {
                    rawODataPath = rawODataPath.substring(index);
                }
            }
            int end = rawServiceResolutionUri.length() - rawODataPath.length();
            rawServiceResolutionUri = rawServiceResolutionUri.substring(0, end);
        }

        String rawBaseUri = rawRequestUri.substring(0,
                rawRequestUri.length() - rawODataPath.length());

        odRequest.setRawQueryPath(httpRequest.getQueryString());
        odRequest.setRawRequestUri(
                rawRequestUri
                        + (httpRequest.getQueryString() == null ? "" : "?" + httpRequest.getQueryString())
        );
        odRequest.setRawODataPath(rawODataPath);
        odRequest.setRawBaseUri(rawBaseUri);
        odRequest.setRawServiceResolutionUri(rawServiceResolutionUri);
    }

    private static void normalizeResponseContentType(HttpServletResponse resp) {
        String ct = resp.getHeader(HttpHeader.CONTENT_TYPE);
        if (ct == null) {
            ct = resp.getContentType();
        }
        if (ct == null) {
            return;
        }

        String lower = ct.toLowerCase();

        // If already has odata.metadata, keep it
        if (lower.contains("odata.metadata=")) {
            return;
        }

        // If it's JSON (with or without charset), default to minimal
        // Examples to catch:
        // - application/json
        // - application/json; charset=UTF-8
        if (lower.startsWith("application/json")) {
            // keep charset if present
            /*String charsetPart = "";
            int charsetIdx = lower.indexOf("charset=");
            if (charsetIdx >= 0) {
                // keep original case/spacing from ct by slicing from the original string
                int semi = ct.indexOf(';', charsetIdx);
                charsetPart = semi >= 0 ?
                ct.substring(ct.indexOf(';', charsetIdx)) : ct.substring(ct.indexOf(';', charsetIdx));
            }*/

            // Build a stable value; simplest: minimal first, then charset if you want
            // Safer for tests expecting exact string: do NOT append charset unless your tests expect it.
            resp.setHeader(HttpHeader.CONTENT_TYPE, "application/json;odata.metadata=minimal");
        }
    }
}