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

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Runtime configuration for the OData client extension.
 */
@ConfigMapping(prefix = "quarkus.odata-client.runtime")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ODataClientRuntimeConfig {

    /**
     * Default publication format. Supported values: JSON, JSON_NO_METADATA,
     * JSON_FULL_METADATA, APPLICATION_XML, APPLICATION_ATOM_XML.
     */
    @WithDefault("JSON")
    String defaultPubFormat();

    /**
     * Whether to enable GZIP compression for requests.
     */
    @WithDefault("false")
    boolean gzipCompression();

    /**
     * Whether to use chunked transfer encoding.
     */
    @WithDefault("true")
    boolean chunked();

    /**
     * Whether to use key-as-segment URL convention.
     */
    @WithDefault("false")
    boolean keyAsSegment();

    /**
     * Whether to use fully qualified names for operations in URLs.
     */
    @WithDefault("true")
    boolean useUrlOperationFqn();

    /**
     * Whether to use X-HTTP-Method header for tunneling.
     */
    @WithDefault("false")
    boolean useXHttpMethod();

    /**
     * Whether to continue on error in batch requests.
     */
    @WithDefault("false")
    boolean continueOnError();

    /**
     * Whether to address derived types in URLs.
     */
    @WithDefault("true")
    boolean addressingDerivedTypes();

    /**
     * Basic authentication configuration.
     */
    BasicAuthConfig basicAuth();

    /**
     * OAuth2 configuration.
     */
    OAuth2Config oauth2();

    /**
     * HTTP proxy configuration.
     */
    ProxyConfig proxy();

    /**
     * Basic authentication configuration group.
     */
    interface BasicAuthConfig {

        /**
         * Whether basic authentication is enabled.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * The username for basic authentication.
         */
        Optional<String> username();

        /**
         * The password for basic authentication.
         */
        Optional<String> password();
    }

    /**
     * OAuth2 configuration group.
     * <p>
     * Note: {@code AbstractOAuth2HttpClientFactory} is abstract. Users must provide
     * their own {@code HttpClientFactory} CDI bean for OAuth2 support.
     */
    interface OAuth2Config {

        /**
         * Whether OAuth2 is enabled.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * The OAuth2 grant service URI.
         */
        Optional<String> grantServiceUri();

        /**
         * The OAuth2 token service URI.
         */
        Optional<String> tokenServiceUri();
    }

    /**
     * HTTP proxy configuration group.
     */
    interface ProxyConfig {

        /**
         * Whether the HTTP proxy is enabled.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * The proxy URI (e.g., {@code http://proxy.example.com:8080}).
         */
        Optional<String> uri();

        /**
         * The proxy username for authentication.
         */
        Optional<String> username();

        /**
         * The proxy password for authentication.
         */
        Optional<String> password();
    }
}
