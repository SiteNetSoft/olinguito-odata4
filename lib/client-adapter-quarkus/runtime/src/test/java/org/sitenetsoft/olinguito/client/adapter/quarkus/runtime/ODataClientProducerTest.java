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

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link ODataClientProducer}.
 * Verifies that the underlying ODataClientFactory works correctly.
 */
class ODataClientProducerTest {

    @Test
    void testODataClientFactoryCreatesClient() {
        ODataClient client = ODataClientFactory.getClient();
        assertNotNull(client, "ODataClient should not be null");
    }

    @Test
    void testODataClientHasConfiguration() {
        ODataClient client = ODataClientFactory.getClient();
        assertNotNull(client.getConfiguration(), "Configuration should not be null");
    }
}
