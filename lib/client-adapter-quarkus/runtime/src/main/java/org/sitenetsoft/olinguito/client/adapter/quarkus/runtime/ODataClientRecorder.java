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
package org.sitenetsoft.olinguito.client.adapter.quarkus.runtime;

import java.net.URI;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import jakarta.enterprise.inject.Default;

import org.sitenetsoft.olinguito.client.api.Configuration;
import org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.client.core.http.BasicAuthHttpClientFactory;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpClientFactory;
import org.sitenetsoft.olinguito.client.core.http.ProxyWrappingHttpClientFactory;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * Quarkus recorder for OData client extension.
 * Creates and configures OData client instances at runtime.
 */
@Recorder
public class ODataClientRecorder {

    /**
     * Creates a configured {@link ODataClient} for the given client name.
     *
     * @param clientName    the client name (e.g., {@code "<default>"} or a named key)
     * @param runtimeConfig the runtime configuration containing all client configs
     * @return a runtime value wrapping the configured client
     */
    public RuntimeValue<ODataClient> createClient(
            String clientName,
            ODataClientsRuntimeConfig runtimeConfig) {

        ODataClient client = ODataClientFactory.getClient();
        ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig =
                runtimeConfig.clients().get(clientName);
        if (clientConfig != null) {
            applyConfiguration(client.getConfiguration(), clientConfig);
        }
        return new RuntimeValue<>(client);
    }

    /**
     * Creates a configured {@link EdmEnabledODataClient} for the given client name.
     *
     * @param clientName    the client name
     * @param serviceRoot   the OData service root URL
     * @param runtimeConfig the runtime configuration containing all client configs
     * @return a runtime value wrapping the configured EDM-enabled client
     */
    public RuntimeValue<EdmEnabledODataClient> createEdmEnabledClient(
            String clientName,
            String serviceRoot,
            ODataClientsRuntimeConfig runtimeConfig) {

        EdmEnabledODataClient client = ODataClientFactory.getEdmEnabledClient(serviceRoot);
        ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig =
                runtimeConfig.clients().get(clientName);
        if (clientConfig != null) {
            applyConfiguration(client.getConfiguration(), clientConfig);
        }
        return new RuntimeValue<>(client);
    }

    private void applyConfiguration(
            Configuration config,
            ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig) {

        config.setHttpClientFactory(resolveHttpClientFactory(clientConfig));
        config.setDefaultPubFormat(resolveContentType(clientConfig.defaultPubFormat()));
        config.setGzipCompression(clientConfig.gzipCompression());
        config.setUseChuncked(clientConfig.chunked());
        config.setKeyAsSegment(clientConfig.keyAsSegment());
        config.setUseUrlOperationFQN(clientConfig.useUrlOperationFqn());
        config.setUseXHTTPMethod(clientConfig.useXHttpMethod());
        config.setContinueOnError(clientConfig.continueOnError());
        config.setAddressingDerivedTypes(clientConfig.addressingDerivedTypes());
    }

    private HttpClientFactory resolveHttpClientFactory(
            ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig) {

        // Priority 1: User-provided CDI bean (unqualified)
        HttpClientFactory userFactory = resolveUserHttpClientFactory();
        if (userFactory != null) {
            return userFactory;
        }

        // Priority 2: Basic auth from config
        DefaultHttpClientFactory baseFactory = resolveBaseFactory(clientConfig);

        // Priority 3: Proxy wrapping
        if (clientConfig.proxy().enabled() && clientConfig.proxy().uri().isPresent()) {
            URI proxyUri = URI.create(clientConfig.proxy().uri().get());
            if (clientConfig.proxy().username().isPresent() && clientConfig.proxy().password().isPresent()) {
                return new ProxyWrappingHttpClientFactory(
                        proxyUri,
                        clientConfig.proxy().username().get(),
                        clientConfig.proxy().password().get(),
                        baseFactory);
            }
            return new ProxyWrappingHttpClientFactory(proxyUri, baseFactory);
        }

        return baseFactory;
    }

    private HttpClientFactory resolveUserHttpClientFactory() {
        var container = Arc.container();
        if (container != null) {
            var instance = container.select(HttpClientFactory.class, Default.Literal.INSTANCE);
            if (instance.isResolvable()) {
                return instance.get();
            }
        }
        return null;
    }

    private DefaultHttpClientFactory resolveBaseFactory(
            ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig) {

        ODataClientsRuntimeConfig.BasicAuthConfig basicAuth = clientConfig.basicAuth();
        if (basicAuth.enabled() && basicAuth.username().isPresent() && basicAuth.password().isPresent()) {
            return new BasicAuthHttpClientFactory(basicAuth.username().get(), basicAuth.password().get());
        }
        return new DefaultHttpClientFactory();
    }

    static ContentType resolveContentType(String format) {
        return switch (format.toUpperCase()) {
            case "JSON" -> ContentType.JSON;
            case "JSON_NO_METADATA" -> ContentType.JSON_NO_METADATA;
            case "JSON_FULL_METADATA" -> ContentType.JSON_FULL_METADATA;
            case "APPLICATION_XML" -> ContentType.APPLICATION_XML;
            case "APPLICATION_ATOM_XML" -> ContentType.APPLICATION_ATOM_XML;
            default -> ContentType.JSON;
        };
    }
}
