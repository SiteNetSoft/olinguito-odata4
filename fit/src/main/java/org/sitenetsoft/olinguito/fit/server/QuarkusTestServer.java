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
package org.sitenetsoft.olinguito.fit.server;

import jakarta.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test server implementation that uses embedded Quarkus.
 * This server starts a Quarkus application with the server-tecsvc-quarkus module.
 *
 * <p>The Quarkus dependencies are loaded dynamically to avoid compilation issues
 * when only the Tomcat test profile is active.</p>
 */
public class QuarkusTestServer implements TestServer {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusTestServer.class);

    private static final String SESSION_MANAGER_CLASS =
        "org.sitenetsoft.olinguito.server.tecsvc.quarkus.SessionManager";

    private final int port;
    private final Object sessionManager;  // SessionManager loaded via reflection
    private volatile boolean running = false;

    private QuarkusTestServer(int port, Object sessionManager) {
        this.port = port;
        this.sessionManager = sessionManager;
    }

    @Override
    public void start() throws Exception {
        // Quarkus is started via the test framework
        running = true;
        LOG.info("QuarkusTestServer marked as running on port {}", port);
    }

    @Override
    public void stop() {
        // Quarkus is stopped via the test framework
        running = false;
        LOG.info("QuarkusTestServer stopped");
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Override
    public void invalidateAllSessions() {
        if (sessionManager != null) {
            try {
                Method invalidateAll = sessionManager.getClass().getMethod("invalidateAll");
                invalidateAll.invoke(sessionManager);
            } catch (Exception e) {
                LOG.warn("Failed to invalidate sessions: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Builder for QuarkusTestServer.
     */
    public static class Builder implements TestServerBuilder<Builder> {

        private int port = 9080;
        private Object sessionManager;
        private final List<ServletMapping> servletMappings = new ArrayList<>();
        private final Map<String, String> staticContent = new HashMap<>();

        private Builder(int port) {
            this.port = port;
        }

        public static Builder create(int port) {
            return new Builder(port);
        }

        @Override
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder sessionManager(Object sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }

        @Override
        public Builder addServlet(Class<? extends HttpServlet> servletClass, String path) {
            servletMappings.add(new ServletMapping(servletClass, path));
            LOG.info("Servlet mapping registered (will be ignored in Quarkus mode): {} -> {}",
                    servletClass.getName(), path);
            return this;
        }

        @Override
        public Builder addServlet(HttpServlet servlet, String path) {
            LOG.info("Servlet instance registered (will be ignored in Quarkus mode): {} -> {}",
                    servlet.getClass().getName(), path);
            return this;
        }

        @Override
        public Builder addStaticContent(String uri, String resource) {
            staticContent.put(uri, resource);
            LOG.info("Static content registered (will be ignored in Quarkus mode): {} -> {}", uri, resource);
            return this;
        }

        @Override
        public TestServer build() throws Exception {
            if (sessionManager == null) {
                // Try to create a SessionManager via reflection
                try {
                    Class<?> sessionManagerClass = Class.forName(SESSION_MANAGER_CLASS);
                    sessionManager = sessionManagerClass.getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    LOG.warn("SessionManager class not found - session invalidation will be a no-op");
                } catch (Exception e) {
                    LOG.warn("Failed to create SessionManager: {}", e.getMessage());
                }
            }
            QuarkusTestServer server = new QuarkusTestServer(port, sessionManager);
            server.start();
            return server;
        }
    }

    private static class ServletMapping {
        final Class<? extends HttpServlet> servletClass;
        final String path;

        ServletMapping(Class<? extends HttpServlet> servletClass, String path) {
            this.servletClass = servletClass;
            this.path = path;
        }
    }
}
