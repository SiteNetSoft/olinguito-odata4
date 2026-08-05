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

import io.quarkus.runtime.RuntimeValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.ODataClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ODataClientRecorder}.
 * Note: Arc container is not available in unit tests, so user-provided
 * HttpClientFactory resolution is skipped gracefully.
 */
class ODataClientRecorderTest {

    private ODataClientRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ODataClientRecorder();
    }

    @Test
    void testCreateClientReturnsNonNull() {
        ODataClientsRuntimeConfig runtimeConfig = mock(ODataClientsRuntimeConfig.class);
        when(runtimeConfig.clients()).thenReturn(Map.of());

        RuntimeValue<ODataClient> result = recorder.createClient("<default>", runtimeConfig);
        assertNotNull(result, "RuntimeValue should not be null");
        assertNotNull(result.getValue(), "ODataClient should not be null");
    }

    @Test
    void testCreateClientWithMissingConfigEntry() {
        ODataClientsRuntimeConfig runtimeConfig = mock(ODataClientsRuntimeConfig.class);
        when(runtimeConfig.clients()).thenReturn(Map.of());

        // A client name not in the config map should not throw
        assertDoesNotThrow(() -> recorder.createClient("nonexistent", runtimeConfig));
    }

    @Test
    void testCreateClientWithEmptyConfigMap() {
        ODataClientsRuntimeConfig runtimeConfig = mock(ODataClientsRuntimeConfig.class);
        when(runtimeConfig.clients()).thenReturn(Map.of());

        RuntimeValue<ODataClient> result = recorder.createClient("<default>", runtimeConfig);
        assertNotNull(result.getValue(), "ODataClient should be created even with empty config");
    }
}
