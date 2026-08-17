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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 3: serve a second route with the OData 4.01
 * key-as-segment URL convention (Part 2: URL Conventions, section 4.3.6)
 */
package org.sitenetsoft.olinguito.server.tecsvc.quarkus;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class that sets up the Technical OData Service routes
 * for Quarkus testing.
 */
@ApplicationScoped
public class TechnicalServiceConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TechnicalServiceConfiguration.class);

    public static final String DEFAULT_BASE_PATH = "/odata-server-tecsvc/odata.svc";
    /** The same technical service, served with the key-as-segment URL convention switched on. */
    public static final String KEY_AS_SEGMENT_BASE_PATH = "/odata-server-tecsvc/odata-kas.svc";
    public static final int DEFAULT_SPLIT = 0;

    @Inject
    SessionManager sessionManager;

    @Inject
    Router router;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Configuring Technical OData Service routes");
        configureRoutes(DEFAULT_BASE_PATH, DEFAULT_SPLIT);
        configureRoutes(KEY_AS_SEGMENT_BASE_PATH, DEFAULT_SPLIT, true);
    }

    /**
     * Configures the OData routes.
     *
     * @param basePath the base path for OData requests
     * @param split the split value for service resolution
     */
    public void configureRoutes(String basePath, int split) {
        configureRoutes(basePath, split, false);
    }

    /**
     * Configures the OData routes.
     *
     * @param basePath the base path for OData requests
     * @param split the split value for service resolution
     * @param keyAsSegment whether entity keys may be given as their own path segments
     */
    public void configureRoutes(String basePath, int split, boolean keyAsSegment) {
        TechnicalODataHandler handler = new TechnicalODataHandler(sessionManager, basePath, split, keyAsSegment);

        // Route for all HTTP methods on the OData path
        String routePath = basePath + "/*";
        router.route(routePath).handler(handler);

        // Also route the base path itself (for service document)
        router.route(basePath).handler(handler);

        LOG.info("Registered OData routes at: {} (split={}, keyAsSegment={})", basePath, split, keyAsSegment);
    }

    /**
     * Returns the session manager for test access.
     *
     * @return the session manager
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
