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
package org.sitenetsoft.olinguito.client.adapter.quarkus.runtime;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

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
 * CDI producer for OData client beans.
 */
@ApplicationScoped
public class ODataClientProducer {

    @Inject
    ODataClientRuntimeConfig runtimeConfig;

    @Inject
    Instance<HttpClientFactory> httpClientFactoryInstance;

    private String serviceRoot;

    /**
     * Called by the recorder at RUNTIME_INIT to set the service root URL.
     */
    public void setServiceRoot(String serviceRoot) {
        this.serviceRoot = serviceRoot;
    }

    @Produces
    @ApplicationScoped
    public ODataClient produceODataClient() {
        ODataClient client = ODataClientFactory.getClient();
        applyConfiguration(client.getConfiguration());
        return client;
    }

    @Produces
    @ApplicationScoped
    public EdmEnabledODataClient produceEdmEnabledODataClient() {
        if (serviceRoot == null || serviceRoot.isBlank()) {
            throw new IllegalStateException(
                    "Cannot create EdmEnabledODataClient: 'quarkus.odata-client.service-root' is not configured");
        }
        EdmEnabledODataClient client = ODataClientFactory.getEdmEnabledClient(serviceRoot);
        applyConfiguration(client.getConfiguration());
        return client;
    }

    private void applyConfiguration(Configuration config) {
        // Apply HTTP client factory (user CDI bean takes priority)
        config.setHttpClientFactory(resolveHttpClientFactory());

        // Apply format configuration
        config.setDefaultPubFormat(resolveContentType(runtimeConfig.defaultPubFormat()));
        config.setGzipCompression(runtimeConfig.gzipCompression());
        config.setUseChuncked(runtimeConfig.chunked());
        config.setKeyAsSegment(runtimeConfig.keyAsSegment());
        config.setUseUrlOperationFQN(runtimeConfig.useUrlOperationFqn());
        config.setUseXHTTPMethod(runtimeConfig.useXHttpMethod());
        config.setContinueOnError(runtimeConfig.continueOnError());
        config.setAddressingDerivedTypes(runtimeConfig.addressingDerivedTypes());
    }

    private HttpClientFactory resolveHttpClientFactory() {
        // Priority 1: User-provided CDI bean
        if (httpClientFactoryInstance.isResolvable()) {
            return httpClientFactoryInstance.get();
        }

        // Priority 2: Basic auth from config
        DefaultHttpClientFactory baseFactory = resolveBaseFactory();

        // Priority 3: Proxy wrapping
        if (runtimeConfig.proxy().enabled() && runtimeConfig.proxy().uri().isPresent()) {
            URI proxyUri = URI.create(runtimeConfig.proxy().uri().get());
            if (runtimeConfig.proxy().username().isPresent() && runtimeConfig.proxy().password().isPresent()) {
                return new ProxyWrappingHttpClientFactory(
                        proxyUri,
                        runtimeConfig.proxy().username().get(),
                        runtimeConfig.proxy().password().get(),
                        baseFactory);
            }
            return new ProxyWrappingHttpClientFactory(proxyUri, baseFactory);
        }

        return baseFactory;
    }

    private DefaultHttpClientFactory resolveBaseFactory() {
        ODataClientRuntimeConfig.BasicAuthConfig basicAuth = runtimeConfig.basicAuth();
        if (basicAuth.enabled() && basicAuth.username().isPresent() && basicAuth.password().isPresent()) {
            return new BasicAuthHttpClientFactory(basicAuth.username().get(), basicAuth.password().get());
        }
        return new DefaultHttpClientFactory();
    }

    private static ContentType resolveContentType(String format) {
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
