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
 *
 * Copyright 2026 SiteNetSoft - Named/multi-service support for Quarkus OData server extension
 */
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataRequestHandler;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.processor.Processor;

/**
 * Quarkus recorder for OData server extension.
 * Creates per-service Vert.x route handlers with lazy, qualifier-aware CDI lookups.
 */
@Recorder
public class ODataRecorder {

    private static final Map<String, ODataRequestHandler> HANDLERS = new ConcurrentHashMap<>();

    private final RuntimeValue<ODataServicesRuntimeConfig> runtimeConfig;

    /**
     * Constructs the recorder with runtime configuration injected by Quarkus.
     *
     * @param runtimeConfig runtime configuration for all services
     */
    public ODataRecorder(RuntimeValue<ODataServicesRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Creates a consumer that sets up an OData route on the Vert.x router.
     *
     * @param serviceName   the service name ({@code "<default>"} or a named key)
     * @param basePath      the OData base path
     * @return consumer to configure the route
     */
    public Consumer<Route> createRouteHandler(
            String serviceName,
            String basePath) {
        return route -> route.handler(createVertxHandler(serviceName, basePath));
    }

    /**
     * Creates a Vert.x route handler for the given OData service.
     *
     * @param serviceName   the service name ({@code "<default>"} or a named key)
     * @param basePath      the OData base path
     * @return handler for RoutingContext
     */
    public Handler<RoutingContext> createVertxHandler(
            String serviceName,
            String basePath) {
        return ctx -> {
            try {
                ODataRequestHandler handler = getHandler(serviceName);
                if (handler != null) {
                    int split = getSplit(serviceName);
                    new VertxODataHandler(handler, basePath, split).handle(ctx);
                } else {
                    ctx.response()
                            .setStatusCode(503)
                            .end("OData service not configured. "
                                    + "Please provide a CsdlEdmProvider bean"
                                    + (ODataServiceNames.isDefault(serviceName)
                                            ? "."
                                            : " qualified with @Identifier(\"" + serviceName + "\")."));
                }
            } catch (Exception e) {
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

    private int getSplit(String serviceName) {
        var serviceConfig = runtimeConfig.getValue().services().get(serviceName);
        return serviceConfig != null ? serviceConfig.split() : 0;
    }

    private static ODataRequestHandler getHandler(String serviceName) {
        return HANDLERS.computeIfAbsent(serviceName, ODataRecorder::initializeHandler);
    }

    private static ODataRequestHandler initializeHandler(String serviceName) {
        var container = Arc.container();
        if (container == null) {
            return null;
        }

        boolean isDefault = ODataServiceNames.isDefault(serviceName);

        // Look up CsdlEdmProvider — exactly one per service
        var edmProviderInstance = isDefault
                ? container.select(CsdlEdmProvider.class)
                : container.select(CsdlEdmProvider.class, Identifier.Literal.of(serviceName));
        if (!edmProviderInstance.isResolvable()) {
            return null;
        }

        OData odata = OData.newInstance();

        // Collect EdmxReference beans
        List<EdmxReference> references = new ArrayList<>();
        var refInstances = isDefault
                ? container.select(EdmxReference.class)
                : container.select(EdmxReference.class, Identifier.Literal.of(serviceName));
        if (!refInstances.isUnsatisfied()) {
            refInstances.forEach(references::add);
        }

        ServiceMetadata serviceMetadata = odata.createServiceMetadata(
                edmProviderInstance.get(),
                references.isEmpty() ? Collections.emptyList() : references);

        ODataRequestHandler requestHandler = odata.createHandler(serviceMetadata);

        // Register all discovered Processor beans
        var processorInstances = isDefault
                ? container.select(Processor.class)
                : container.select(Processor.class, Identifier.Literal.of(serviceName));
        if (!processorInstances.isUnsatisfied()) {
            processorInstances.forEach(requestHandler::register);
        }

        return requestHandler;
    }
}
