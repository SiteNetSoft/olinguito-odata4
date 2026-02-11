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

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

/**
 * Build-time configuration for the OData client extension.
 * Supports multiple named clients via map-based configuration.
 */
@ConfigMapping(prefix = "quarkus.odata-client")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ODataClientsBuildTimeConfig {

    /**
     * Named OData clients. The default (unnamed) client uses the {@code <default>} key.
     */
    @WithParentName
    @WithUnnamedKey("<default>")
    @ConfigDocMapKey("client-name")
    Map<String, ODataClientBuildTimeConfig> clients();

    /**
     * Build-time configuration for a single OData client.
     */
    interface ODataClientBuildTimeConfig {

        /**
         * Whether this OData client is enabled.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * The OData service root URL. When set, an {@code EdmEnabledODataClient}
         * bean will be produced in addition to the standard {@code ODataClient}.
         */
        Optional<String> serviceRoot();
    }
}
