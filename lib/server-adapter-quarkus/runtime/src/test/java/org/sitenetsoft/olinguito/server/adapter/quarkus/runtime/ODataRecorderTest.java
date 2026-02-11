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

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ODataRecorder}.
 */
class ODataRecorderTest {

    private ODataRecorder recorder;
    private ODataServicesRuntimeConfig runtimeConfig;

    @BeforeEach
    void setUp() {
        recorder = new ODataRecorder();
        runtimeConfig = mock(ODataServicesRuntimeConfig.class);
    }

    @Test
    void testCreateVertxHandlerForDefaultService() {
        Handler<RoutingContext> handler = recorder.createVertxHandler(
                ODataServiceNames.DEFAULT, "/odata", runtimeConfig);

        assertNotNull(handler, "Vert.x handler should not be null");
    }

    @Test
    void testCreateVertxHandlerForNamedService() {
        Handler<RoutingContext> handler = recorder.createVertxHandler(
                "catalog", "/catalog", runtimeConfig);

        assertNotNull(handler, "Vert.x handler for named service should not be null");
    }

    @Test
    void testCreateRouteHandlerForDefaultService() {
        Consumer<io.vertx.ext.web.Route> routeHandler = recorder.createRouteHandler(
                ODataServiceNames.DEFAULT, "/odata", runtimeConfig);

        assertNotNull(routeHandler, "Route handler should not be null");
    }

    @Test
    void testCreateRouteHandlerForNamedService() {
        Consumer<io.vertx.ext.web.Route> routeHandler = recorder.createRouteHandler(
                "catalog", "/catalog", runtimeConfig);

        assertNotNull(routeHandler, "Route handler for named service should not be null");
    }
}
