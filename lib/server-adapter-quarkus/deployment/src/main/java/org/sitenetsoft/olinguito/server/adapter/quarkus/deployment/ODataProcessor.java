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
package org.sitenetsoft.olinguito.server.adapter.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

import org.sitenetsoft.olinguito.server.adapter.quarkus.runtime.ODataConfig;
import org.sitenetsoft.olinguito.server.adapter.quarkus.runtime.ODataHandlerHolder;
import org.sitenetsoft.olinguito.server.adapter.quarkus.runtime.ODataRecorder;
import org.sitenetsoft.olinguito.server.adapter.quarkus.runtime.ODataServiceProducer;

/**
 * Quarkus deployment processor for the OData extension.
 * Contains build steps that configure the extension at build time.
 */
public class ODataProcessor {

    private static final String FEATURE = "olinguito-odata";

    /**
     * Registers the OData feature.
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Creates the OData build item with configuration.
     */
    @BuildStep
    ODataBuildItem createBuildItem(ODataConfig config) {
        return new ODataBuildItem(config.path(), config.enabled());
    }

    /**
     * Registers CDI beans for the OData extension.
     */
    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ODataHandlerHolder.class)
                .addBeanClass(ODataServiceProducer.class)
                .setUnremovable()
                .build());
    }

    /**
     * Registers classes for reflection (needed for native image).
     */
    @BuildStep
    void registerReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        // OData core classes that need reflection
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                // OData API classes
                "org.sitenetsoft.olinguito.server.api.ODataRequest",
                "org.sitenetsoft.olinguito.server.api.ODataResponse",
                "org.sitenetsoft.olinguito.server.api.OData",
                "org.sitenetsoft.olinguito.server.api.ServiceMetadata",

                // OData core implementation
                "org.sitenetsoft.olinguito.server.core.ODataImpl",
                "org.sitenetsoft.olinguito.server.core.ODataRequestHandlerImpl",
                "org.sitenetsoft.olinguito.server.core.ODataHandlerImpl",

                // Common API classes
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction",
                "org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton",

                // Processor interfaces
                "org.sitenetsoft.olinguito.server.api.processor.Processor",
                "org.sitenetsoft.olinguito.server.api.processor.EntityProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.EntityCollectionProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.PrimitiveProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.ComplexProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.ActionProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.FunctionProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.BatchProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.MediaEntityProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.ReferenceProcessor",
                "org.sitenetsoft.olinguito.server.api.processor.ReferenceCollectionProcessor"
        ).methods().fields().build());

        // Jackson classes for JSON serialization
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "com.fasterxml.jackson.core.JsonGenerator",
                "com.fasterxml.jackson.core.JsonParser",
                "com.fasterxml.jackson.databind.ObjectMapper"
        ).methods().build());
    }

    /**
     * Registers classes that need runtime initialization (for native image).
     */
    @BuildStep
    void registerRuntimeInit(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        // XML parser classes that need runtime initialization
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "com.fasterxml.aalto.async.AsyncByteScanner"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "com.fasterxml.aalto.in.ByteSourceBootstrapper"));
    }

    /**
     * Registers native image resources.
     */
    @BuildStep
    void registerResources(BuildProducer<NativeImageResourceBuildItem> resources) {
        // Include any resource files needed at runtime
        resources.produce(new NativeImageResourceBuildItem(
                "META-INF/services/org.sitenetsoft.olinguito.server.api.OData"));
    }

    /**
     * Registers the OData route at runtime.
     * Uses a single wildcard route that handles both the base path and sub-paths.
     * Routes are marked as blocking because OData processing involves blocking I/O.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerRoutes(
            ODataBuildItem odataBuildItem,
            ODataConfig config,
            ODataRecorder recorder,
            BuildProducer<RouteBuildItem> routes) {

        if (!odataBuildItem.isEnabled()) {
            return;
        }

        String basePath = odataBuildItem.getPath();
        // Normalize: remove trailing slash if present
        if (basePath.endsWith("/") && basePath.length() > 1) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        // Default split value (can be overridden at runtime via config)
        int split = 0;

        // Register wildcard route for all OData sub-paths (e.g., /odata/*)
        // Use blockingRoute() because OData processing involves blocking I/O
        String wildcardPath = basePath + "/*";
        routes.produce(RouteBuildItem.builder()
                .route(wildcardPath)
                .blockingRoute()
                .handler(recorder.createVertxHandler(basePath, split))
                .build());

        // Register exact path route for service document (e.g., /odata)
        // This is needed because /odata/* doesn't match /odata exactly
        routes.produce(RouteBuildItem.builder()
                .route(basePath)
                .blockingRoute()
                .handler(recorder.createVertxHandler(basePath, split))
                .build());
    }
}
