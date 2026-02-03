/*
 * Copyright 2026 SiteNetSoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

import org.sitenetsoft.olinguito.server.api.ODataRequestHandler;

import java.util.function.Consumer;

/**
 * Quarkus recorder for OData extension.
 * Records runtime initialization steps for route registration.
 */
@Recorder
public class ODataRecorder {

    /**
     * Creates a consumer that sets up OData routes on the Vert.x router.
     *
     * @param path  The OData base path
     * @param split Number of leading path segments for service resolution
     * @return Consumer to configure the router
     */
    public Consumer<Route> createRouteHandler(String path, int split) {
        return route -> {
            route.handler(createVertxHandler(path, split));
        };
    }

    /**
     * Creates the route customizer for integrating with Vert.x HTTP.
     *
     * @param path  The OData base path
     * @param split Number of leading path segments for service resolution
     * @return Handler for RoutingContext
     */
    public Handler<RoutingContext> createVertxHandler(String path, int split) {
        return ctx -> {
            try {
                // Lazy initialization on first request
                ODataRequestHandler handler = getOrCreateHandler();
                if (handler != null) {
                    new VertxODataHandler(handler, path, split).handle(ctx);
                } else {
                    ctx.response()
                            .setStatusCode(503)
                            .end("OData service not configured. Please provide a CsdlEdmProvider bean.");
                }
            } catch (Exception e) {
                // Ensure we always send a response
                if (!ctx.response().ended()) {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                    ctx.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "text/plain")
                            .end("OData handler error: " + errorMessage);
                }
            }
        };
    }

    /**
     * Creates the Vert.x route filter that intercepts OData requests.
     *
     * @param path          The base path for OData endpoints
     * @param runtimeConfig Runtime OData configuration
     * @return Handler for RoutingContext
     */
    public Handler<RoutingContext> createRouteFilter(String path, ODataRuntimeConfig runtimeConfig) {
        return ctx -> {
            String requestPath = ctx.request().path();
            String normalizedBasePath = normalizePath(path);

            // Check if the request path matches the OData base path
            boolean matchesPath = requestPath.startsWith(normalizedBasePath)
                    || (normalizedBasePath.isEmpty() && requestPath.equals("/"));
            if (matchesPath) {

                ODataRequestHandler handler = getOrCreateHandler();
                if (handler != null) {
                    new VertxODataHandler(handler, normalizedBasePath, runtimeConfig.split()).handle(ctx);
                } else {
                    ctx.next();
                }
            } else {
                ctx.next();
            }
        };
    }

    private ODataRequestHandler getOrCreateHandler() {
        // Look up the CDI-managed OData handler
        try {
            var container = Arc.container();
            if (container == null) {
                return null;
            }
            var instanceHandle = container.instance(ODataHandlerHolder.class);
            if (instanceHandle == null || !instanceHandle.isAvailable()) {
                return null;
            }
            ODataHandlerHolder holder = instanceHandle.get();
            return holder != null ? holder.getHandler() : null;
        } catch (Exception e) {
            // Log or handle the exception as needed
            return null;
        }
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
