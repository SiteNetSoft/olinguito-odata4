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
package org.sitenetsoft.olinguito.client.adapter.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

import org.sitenetsoft.olinguito.client.adapter.quarkus.runtime.ODataClientConfig;
import org.sitenetsoft.olinguito.client.adapter.quarkus.runtime.ODataClientProducer;
import org.sitenetsoft.olinguito.client.adapter.quarkus.runtime.ODataClientRecorder;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;

/**
 * Quarkus deployment processor for the OData client extension.
 * Contains build steps that configure the extension at build time.
 */
public class ODataClientProcessor {

    private static final String FEATURE = "olinguito-odata-client";

    /**
     * Registers the OData client feature.
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Creates the OData client build item with configuration.
     */
    @BuildStep
    ODataClientBuildItem createBuildItem(ODataClientConfig config) {
        return new ODataClientBuildItem(
                config.enabled(),
                config.serviceRoot().orElse(null));
    }

    /**
     * Registers CDI beans for the OData client extension.
     */
    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ODataClientProducer.class)
                .setUnremovable()
                .build());
    }

    /**
     * Marks user-provided HttpClientFactory beans as unremovable so they are
     * available for injection into the ODataClientProducer.
     */
    @BuildStep
    void markHttpClientFactoryUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(UnremovableBeanBuildItem.beanTypes(HttpClientFactory.class));
    }

    /**
     * Registers classes for reflection (needed for native image).
     */
    @BuildStep
    void registerReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        // OData client API and core classes
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                // Client API
                "org.sitenetsoft.olinguito.client.api.ODataClient",
                "org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient",
                "org.sitenetsoft.olinguito.client.api.Configuration",

                // Client core implementation
                "org.sitenetsoft.olinguito.client.core.ODataClientImpl",
                "org.sitenetsoft.olinguito.client.core.EdmEnabledODataClientImpl",
                "org.sitenetsoft.olinguito.client.core.ODataClientFactory",
                "org.sitenetsoft.olinguito.client.core.ConfigurationImpl",

                // HTTP factories
                "org.sitenetsoft.olinguito.client.core.http.DefaultHttpClientFactory",
                "org.sitenetsoft.olinguito.client.core.http.BasicAuthHttpClientFactory",
                "org.sitenetsoft.olinguito.client.core.http.ProxyWrappingHttpClientFactory",
                "org.sitenetsoft.olinguito.client.core.http.AbstractHttpClientFactory",

                // Serialization
                "org.sitenetsoft.olinguito.client.core.serialization.ODataDeserializerImpl",
                "org.sitenetsoft.olinguito.client.core.serialization.JsonDeserializer",

                // Common API classes
                "org.sitenetsoft.olinguito.commons.api.edm.Edm",
                "org.sitenetsoft.olinguito.commons.api.format.ContentType"
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
        // AbstractHttpClientFactory has a static block that loads client.properties
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "org.sitenetsoft.olinguito.client.core.http.AbstractHttpClientFactory"));

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
        // client.properties is loaded by AbstractHttpClientFactory's static initializer
        resources.produce(new NativeImageResourceBuildItem("client.properties"));
    }

    /**
     * Initializes the OData client producer at runtime.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeClient(
            ODataClientBuildItem buildItem,
            ODataClientRecorder recorder) {

        if (!buildItem.isEnabled()) {
            return;
        }

        recorder.initializeProducer(buildItem.getServiceRoot());
    }
}
