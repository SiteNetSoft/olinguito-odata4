/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
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
    private ODataRuntimeConfig runtimeConfig;

    @BeforeEach
    void setUp() {
        recorder = new ODataRecorder();
        runtimeConfig = mock(ODataRuntimeConfig.class);
    }

    @Test
    void testCreateRouteHandler() {
        Consumer<Route> routeHandler = recorder.createRouteHandler("/odata", 0);

        assertNotNull(routeHandler, "Route handler should not be null");
    }

    @Test
    void testCreateVertxHandler() {
        Handler<RoutingContext> handler = recorder.createVertxHandler("/odata", 0);

        assertNotNull(handler, "Vert.x handler should not be null");
    }

    @Test
    void testCreateRouteFilter() {
        Handler<RoutingContext> filter = recorder.createRouteFilter("/odata", runtimeConfig);

        assertNotNull(filter, "Route filter should not be null");
    }

    @Test
    void testCreateRouteFilterWithEmptyPath() {
        Handler<RoutingContext> filter = recorder.createRouteFilter("", runtimeConfig);

        assertNotNull(filter, "Route filter with empty path should not be null");
    }

    @Test
    void testCreateRouteFilterWithNullPath() {
        Handler<RoutingContext> filter = recorder.createRouteFilter(null, runtimeConfig);

        assertNotNull(filter, "Route filter with null path should not be null");
    }

    @Test
    void testCreateRouteFilterWithTrailingSlash() {
        Handler<RoutingContext> filter = recorder.createRouteFilter("/odata/", runtimeConfig);

        assertNotNull(filter, "Route filter with trailing slash should not be null");
    }

    @Test
    void testCreateRouteFilterWithoutLeadingSlash() {
        Handler<RoutingContext> filter = recorder.createRouteFilter("odata", runtimeConfig);

        assertNotNull(filter, "Route filter without leading slash should not be null");
    }
}
