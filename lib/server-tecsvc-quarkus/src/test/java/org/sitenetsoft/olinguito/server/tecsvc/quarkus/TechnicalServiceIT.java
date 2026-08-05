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
package org.sitenetsoft.olinguito.server.tecsvc.quarkus;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Native-mode integration tests for the Technical OData Service.
 * <p>
 * This test class extends {@link TechnicalServiceTest} and runs the same tests
 * against the native executable when built with the native profile.
 * <p>
 * To run native tests:
 * <pre>
 * mvn verify -Pnative -pl lib/server-tecsvc-quarkus
 * </pre>
 * <p>
 * Prerequisites:
 * <ul>
 *   <li>GraalVM with native-image installed</li>
 *   <li>GRAALVM_HOME environment variable set</li>
 * </ul>
 */
@QuarkusIntegrationTest
class TechnicalServiceIT extends TechnicalServiceTest {
    // Inherits all tests from TechnicalServiceTest
    // Tests will run against the native executable
}
