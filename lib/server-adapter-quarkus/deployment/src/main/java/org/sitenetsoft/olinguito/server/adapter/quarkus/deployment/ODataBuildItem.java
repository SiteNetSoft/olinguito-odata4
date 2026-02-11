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
package org.sitenetsoft.olinguito.server.adapter.quarkus.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that carries OData service configuration information
 * from build steps to runtime initialization. Supports multiple named services.
 */
public final class ODataBuildItem extends SimpleBuildItem {

    private final Map<String, ServiceEntry> services;

    public ODataBuildItem(Map<String, ServiceEntry> services) {
        this.services = services;
    }

    public Map<String, ServiceEntry> getServices() {
        return services;
    }

    /**
     * Configuration entry for a single named OData service.
     */
    public record ServiceEntry(boolean enabled, String path) {
    }
}
