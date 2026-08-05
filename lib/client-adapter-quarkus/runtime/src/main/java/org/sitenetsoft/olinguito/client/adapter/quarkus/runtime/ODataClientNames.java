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

/**
 * Constants for OData client naming in multi-client configurations.
 */
public final class ODataClientNames {

    /**
     * The name used for the default (unnamed) OData client.
     */
    public static final String DEFAULT = "<default>";

    private ODataClientNames() {
        // utility class
    }

    /**
     * Returns {@code true} if the given name represents the default client.
     */
    public static boolean isDefault(String name) {
        return DEFAULT.equals(name);
    }
}
