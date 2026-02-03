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
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.sitenetsoft.olinguito.server.api.OData;

/**
 * CDI producer for OData-related beans.
 */
@ApplicationScoped
public class ODataServiceProducer {

    /**
     * Produces an OData instance.
     * Note: OData instances should not be shared across threads,
     * so we produce a new one each time. For request-scoped usage,
     * consider using @RequestScoped instead.
     *
     * @return a new OData instance
     */
    @Produces
    @ApplicationScoped
    public OData produceOData() {
        return OData.newInstance();
    }
}
