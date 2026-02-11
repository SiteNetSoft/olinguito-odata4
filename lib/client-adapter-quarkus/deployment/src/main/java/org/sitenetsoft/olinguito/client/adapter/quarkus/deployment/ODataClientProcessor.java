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
 * Copyright 2026 SiteNetSoft - Named/multi-client support for Quarkus OData client extension
 */
package org.sitenetsoft.olinguito.client.adapter.quarkus.deployment;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.smallrye.common.annotation.Identifier;

import org.sitenetsoft.olinguito.client.adapter.quarkus.runtime.ODataClientNames;
import org.sitenetsoft.olinguito.client.adapter.quarkus.runtime.ODataClientRecorder;
import org.sitenetsoft.olinguito.client.adapter.quarkus.runtime.ODataClientsBuildTimeConfig;
import org.sitenetsoft.olinguito.client.adapter.quarkus.runtime.ODataClientsRuntimeConfig;
import org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;

/**
 * Quarkus deployment processor for the OData client extension.
 * Creates synthetic CDI beans for each configured OData client.
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
     * Creates the OData client build item with configuration for all named clients.
     */
    @BuildStep
    ODataClientBuildItem createBuildItem(ODataClientsBuildTimeConfig config) {
        Map<String, ODataClientBuildItem.ClientEntry> clients = new LinkedHashMap<>();
        for (var entry : config.clients().entrySet()) {
            clients.put(entry.getKey(), new ODataClientBuildItem.ClientEntry(
                    entry.getValue().enabled(),
                    entry.getValue().serviceRoot().orElse(null)));
        }
        // Ensure the default client always exists
        clients.putIfAbsent(ODataClientNames.DEFAULT,
                new ODataClientBuildItem.ClientEntry(true, null));
        return new ODataClientBuildItem(clients);
    }

    /**
     * Marks user-provided HttpClientFactory beans as unremovable so they are
     * available for lookup from the recorder.
     */
    @BuildStep
    void markHttpClientFactoryUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(UnremovableBeanBuildItem.beanTypes(HttpClientFactory.class));
    }

    /**
     * Creates synthetic CDI beans for each configured OData client.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBeans(
            ODataClientBuildItem buildItem,
            ODataClientRecorder recorder,
            ODataClientsRuntimeConfig runtimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        for (var entry : buildItem.getClients().entrySet()) {
            String clientName = entry.getKey();
            ODataClientBuildItem.ClientEntry clientEntry = entry.getValue();

            if (!clientEntry.enabled()) {
                continue;
            }

            boolean isDefault = ODataClientNames.isDefault(clientName);

            // ODataClient bean
            var clientConfigurator = SyntheticBeanBuildItem
                    .configure(ODataClient.class)
                    .scope(ApplicationScoped.class)
                    .setRuntimeInit()
                    .runtimeValue(recorder.createClient(clientName, runtimeConfig))
                    .unremovable();

            if (isDefault) {
                clientConfigurator.addQualifier().annotation(Default.class).done();
            }
            clientConfigurator.addQualifier().annotation(Identifier.class)
                    .addValue("value", clientName).done();

            syntheticBeans.produce(clientConfigurator.done());

            // EdmEnabledODataClient bean (only if service root is configured)
            String serviceRoot = clientEntry.serviceRoot();
            if (serviceRoot != null && !serviceRoot.isBlank()) {
                var edmConfigurator = SyntheticBeanBuildItem
                        .configure(EdmEnabledODataClient.class)
                        .scope(ApplicationScoped.class)
                        .setRuntimeInit()
                        .runtimeValue(recorder.createEdmEnabledClient(
                                clientName, serviceRoot, runtimeConfig))
                        .unremovable();

                if (isDefault) {
                    edmConfigurator.addQualifier().annotation(Default.class).done();
                }
                edmConfigurator.addQualifier().annotation(Identifier.class)
                        .addValue("value", clientName).done();

                syntheticBeans.produce(edmConfigurator.done());
            }
        }
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
}
