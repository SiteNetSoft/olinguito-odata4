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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating test servers based on configuration.
 * The server type is determined by the {@code test.server.type} system property.
 */
public final class TestServerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerFactory.class);

    /**
     * System property to select the test server type.
     * Valid values: "tomcat" (default), "quarkus"
     */
    public static final String SERVER_TYPE_PROPERTY = "test.server.type";

    /**
     * Server type constant for Tomcat.
     */
    public static final String SERVER_TYPE_TOMCAT = "tomcat";

    /**
     * Server type constant for Quarkus.
     */
    public static final String SERVER_TYPE_QUARKUS = "quarkus";

    private TestServerFactory() {
        // Utility class
    }

    /**
     * Creates a test server builder based on the configured server type.
     *
     * @param port the port to use
     * @return a builder for the appropriate server type
     */
    public static TestServerBuilder<?> create(int port) {
        String serverType = System.getProperty(SERVER_TYPE_PROPERTY, SERVER_TYPE_TOMCAT);
        LOG.info("Creating test server of type: {}", serverType);

        return switch (serverType.toLowerCase()) {
            case SERVER_TYPE_QUARKUS -> createQuarkusBuilder(port);
            default -> TomcatTestServer.init(port);
        };
    }

    private static TestServerBuilder<?> createQuarkusBuilder(int port) {
        try {
            // Load QuarkusTestServer dynamically to avoid compile-time dependency
            Class<?> builderClass = Class.forName(
                "org.sitenetsoft.olinguito.fit.server.QuarkusTestServer$Builder");
            return (TestServerBuilder<?>) builderClass
                .getMethod("create", int.class)
                .invoke(null, port);
        } catch (ClassNotFoundException e) {
            LOG.warn("QuarkusTestServer not available, falling back to Tomcat");
            return TomcatTestServer.init(port);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create QuarkusTestServer builder", e);
        }
    }

    /**
     * Returns the configured server type.
     *
     * @return the server type
     */
    public static String getServerType() {
        return System.getProperty(SERVER_TYPE_PROPERTY, SERVER_TYPE_TOMCAT);
    }

    /**
     * Checks if the configured server type is Quarkus.
     *
     * @return true if Quarkus is configured
     */
    public static boolean isQuarkus() {
        return SERVER_TYPE_QUARKUS.equalsIgnoreCase(getServerType());
    }

    /**
     * Checks if the configured server type is Tomcat.
     *
     * @return true if Tomcat is configured
     */
    public static boolean isTomcat() {
        return SERVER_TYPE_TOMCAT.equalsIgnoreCase(getServerType());
    }
}
