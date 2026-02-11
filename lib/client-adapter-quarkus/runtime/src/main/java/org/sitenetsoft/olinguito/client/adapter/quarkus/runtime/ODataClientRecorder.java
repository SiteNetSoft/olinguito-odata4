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

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Quarkus recorder for OData client extension.
 * Records runtime initialization steps that pass configuration to the CDI producer.
 */
@Recorder
public class ODataClientRecorder {

    /**
     * Initializes the OData client producer with the service root URL.
     *
     * @param serviceRoot the OData service root URL, or {@code null} if not configured
     */
    public void initializeProducer(String serviceRoot) {
        var container = Arc.container();
        if (container != null) {
            var instance = container.instance(ODataClientProducer.class);
            if (instance.isAvailable()) {
                instance.get().setServiceRoot(serviceRoot);
            }
        }
    }
}
