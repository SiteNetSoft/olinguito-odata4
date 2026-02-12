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
 * Copyright 2026 SiteNetSoft - Pluggable HTTP adapter support (apache/okhttp)
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
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * Quarkus recorder for OData client extension.
 * Creates and configures OData client instances at runtime.
 */
@Recorder
public class ODataClientRecorder {

    private static final String BASIC_AUTH_APACHE =
            "org.sitenetsoft.olinguito.client.core.http.BasicAuthHttpClientFactory";
    private static final String BASIC_AUTH_OKHTTP =
            "org.sitenetsoft.olinguito.client.adapter.okhttp.OkHttpBasicAuthClientFactory";
    private static final String PROXY_FACTORY =
            "org.sitenetsoft.olinguito.client.core.http.ProxyWrappingHttpClientFactory";

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

        ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig =
                runtimeConfig.clients().get(clientName);

        ODataClientFactory.Builder builder = ODataClientFactory.builder();
        selectAdapter(builder, clientConfig);

        ODataClient client = builder.build();
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

        ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig =
                runtimeConfig.clients().get(clientName);

        ODataClientFactory.Builder builder = ODataClientFactory.builder();
        selectAdapter(builder, clientConfig);

        EdmEnabledODataClient client = builder.buildEdmEnabled(serviceRoot);
        if (clientConfig != null) {
            applyConfiguration(client.getConfiguration(), clientConfig);
        }
        return new RuntimeValue<>(client);
    }

    private void selectAdapter(ODataClientFactory.Builder builder,
                               ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig) {
        String adapter = clientConfig != null ? clientConfig.httpAdapter() : "apache";
        if ("okhttp".equalsIgnoreCase(adapter)) {
            builder.withOkHttp();
        } else {
            builder.withApache();
        }
    }

    private void applyConfiguration(
            Configuration config,
            ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig) {

        HttpClientFactory factory = resolveHttpClientFactory(clientConfig);
        if (factory != null) {
            config.setHttpClientFactory(factory);
        }
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
        HttpClientFactory baseFactory = resolveBasicAuthFactory(clientConfig);

        // Priority 3: Proxy wrapping (Apache-only, requires ProxyWrappingHttpClientFactory)
        if (baseFactory != null
                && clientConfig.proxy().enabled()
                && clientConfig.proxy().uri().isPresent()) {
            return wrapWithProxy(baseFactory, clientConfig);
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

    private HttpClientFactory resolveBasicAuthFactory(
            ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig) {

        ODataClientsRuntimeConfig.BasicAuthConfig basicAuth = clientConfig.basicAuth();
        if (!basicAuth.enabled() || basicAuth.username().isEmpty() || basicAuth.password().isEmpty()) {
            return null;
        }

        String username = basicAuth.username().get();
        String password = basicAuth.password().get();

        // Select the auth factory class based on the configured adapter
        String factoryClass = "okhttp".equalsIgnoreCase(clientConfig.httpAdapter())
                ? BASIC_AUTH_OKHTTP
                : BASIC_AUTH_APACHE;

        try {
            return (HttpClientFactory) Class.forName(factoryClass)
                    .getDeclaredConstructor(String.class, String.class)
                    .newInstance(username, password);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to create basic auth factory: " + factoryClass, e);
        }
    }

    private HttpClientFactory wrapWithProxy(
            HttpClientFactory baseFactory,
            ODataClientsRuntimeConfig.ODataClientRuntimeConfig clientConfig) {

        // ProxyWrappingHttpClientFactory accepts DefaultHttpClientFactory (Apache-specific).
        // Proxy wrapping is only supported with the Apache adapter.
        try {
            URI proxyUri = URI.create(clientConfig.proxy().uri().get());
            Class<?> proxyClass = Class.forName(PROXY_FACTORY);
            Class<?> baseClass = baseFactory.getClass();

            if (clientConfig.proxy().username().isPresent()
                    && clientConfig.proxy().password().isPresent()) {
                return (HttpClientFactory) proxyClass
                        .getDeclaredConstructor(URI.class, String.class, String.class, baseClass)
                        .newInstance(proxyUri,
                                clientConfig.proxy().username().get(),
                                clientConfig.proxy().password().get(),
                                baseFactory);
            }
            return (HttpClientFactory) proxyClass
                    .getDeclaredConstructor(URI.class, baseClass)
                    .newInstance(proxyUri, baseFactory);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to create proxy factory. "
                    + "Proxy wrapping is only supported with the Apache HTTP adapter.", e);
        }
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
