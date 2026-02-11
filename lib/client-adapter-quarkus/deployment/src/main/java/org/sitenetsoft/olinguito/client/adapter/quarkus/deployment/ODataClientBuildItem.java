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

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that carries OData client configuration information
 * from build steps to runtime initialization.
 */
public final class ODataClientBuildItem extends SimpleBuildItem {

    private final boolean enabled;
    private final String serviceRoot;

    public ODataClientBuildItem(boolean enabled, String serviceRoot) {
        this.enabled = enabled;
        this.serviceRoot = serviceRoot;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServiceRoot() {
        return serviceRoot;
    }
}
