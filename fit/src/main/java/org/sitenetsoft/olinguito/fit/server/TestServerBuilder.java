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
import java.io.IOException;

/**
 * Builder interface for constructing test servers.
 * Provides a fluent API for configuring servlets and static content.
 *
 * @param <T> the builder type for method chaining
 */
public interface TestServerBuilder<T extends TestServerBuilder<T>> {

    /**
     * Sets the port for the server.
     *
     * @param port the port number
     * @return this builder
     */
    T port(int port);

    /**
     * Adds a servlet to the server.
     *
     * @param servletClass the servlet class
     * @param path the URL path mapping
     * @return this builder
     * @throws Exception if the servlet cannot be added
     */
    T addServlet(Class<? extends HttpServlet> servletClass, String path) throws Exception;

    /**
     * Adds a servlet instance to the server.
     *
     * @param servlet the servlet instance
     * @param path the URL path mapping
     * @return this builder
     * @throws IOException if the servlet cannot be added
     */
    T addServlet(HttpServlet servlet, String path) throws IOException;

    /**
     * Adds static content mapping.
     *
     * @param uri the URI to map
     * @param resource the resource name/path
     * @return this builder
     * @throws IOException if the mapping cannot be added
     */
    T addStaticContent(String uri, String resource) throws IOException;

    /**
     * Builds and returns the configured test server.
     *
     * @return the test server
     * @throws Exception if the server cannot be built
     */
    TestServer build() throws Exception;
}
