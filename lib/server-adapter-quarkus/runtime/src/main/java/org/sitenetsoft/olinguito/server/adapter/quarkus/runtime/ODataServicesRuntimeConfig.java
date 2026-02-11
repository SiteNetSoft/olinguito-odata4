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

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

/**
 * Runtime configuration for the OData server extension.
 * Supports multiple named services via map-based configuration.
 */
@ConfigMapping(prefix = "quarkus.odata")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ODataServicesRuntimeConfig {

    /**
     * Named OData services. The default (unnamed) service uses the {@code <default>} key.
     */
    @WithParentName
    @WithUnnamedKey("<default>")
    @ConfigDocMapKey("service-name")
    Map<String, ODataServiceRuntimeConfig> services();

    /**
     * Runtime configuration for a single OData service.
     */
    interface ODataServiceRuntimeConfig {

        /**
         * Number of leading path segments that belong to the service resolution URI.
         * This is used for multi-tenant scenarios where the service root varies.
         */
        @WithDefault("0")
        int split();

        /**
         * Whether to enable debug mode for OData processing.
         */
        @WithDefault("false")
        boolean debug();
    }
}
