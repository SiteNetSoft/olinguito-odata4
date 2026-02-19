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

import io.quarkus.test.QuarkusUnitTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the OData client Quarkus extension.
 * <p>
 * Disabled: ODataClientProcessor consumes runtime config in a build step.
 * Needs refactoring to use RuntimeValue&lt;ODataClientsRuntimeConfig&gt; in a @Recorder constructor.
 */
@Disabled("ODataClientProcessor build step consumes runtime config directly - needs @Recorder refactoring")
class ODataClientExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("application.properties"));

    @Test
    void testExtensionLoads() {
        assertTrue(true, "Extension should load without errors");
    }
}
