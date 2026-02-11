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

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that carries OData client configuration information
 * from build steps to runtime initialization. Supports multiple named clients.
 */
public final class ODataClientBuildItem extends SimpleBuildItem {

    private final Map<String, ClientEntry> clients;

    public ODataClientBuildItem(Map<String, ClientEntry> clients) {
        this.clients = clients;
    }

    public Map<String, ClientEntry> getClients() {
        return clients;
    }

    /**
     * Configuration entry for a single named OData client.
     */
    public record ClientEntry(boolean enabled, String serviceRoot) {
    }
}
