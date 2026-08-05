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

/**
 * Constants for OData service naming in multi-service configurations.
 */
public final class ODataServiceNames {

    /**
     * The name used for the default (unnamed) OData service.
     */
    public static final String DEFAULT = "<default>";

    private ODataServiceNames() {
        // utility class
    }

    /**
     * Returns {@code true} if the given name represents the default service.
     */
    public static boolean isDefault(String name) {
        return DEFAULT.equals(name);
    }
}
