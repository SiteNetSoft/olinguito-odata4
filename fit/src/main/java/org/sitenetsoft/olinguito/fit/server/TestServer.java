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
package org.sitenetsoft.olinguito.fit.server;

/**
 * Interface for test servers used in integration tests.
 * Implementations include {@link TomcatTestServer} for servlet-based testing
 * and QuarkusTestServer for Quarkus extension testing.
 */
public interface TestServer {

    /**
     * Starts the test server.
     *
     * @throws Exception if the server fails to start
     */
    void start() throws Exception;

    /**
     * Stops the test server.
     *
     * @throws Exception if the server fails to stop
     */
    void stop() throws Exception;

    /**
     * Returns the port the server is listening on.
     *
     * @return the server port
     */
    int getPort();

    /**
     * Returns the base URL of the server (e.g., "http://localhost:9080").
     *
     * @return the base URL
     */
    String getBaseUrl();

    /**
     * Invalidates all sessions on the server.
     * Used for test isolation.
     */
    void invalidateAllSessions();

    /**
     * Checks if the server is currently running.
     *
     * @return true if the server is running, false otherwise
     */
    boolean isRunning();
}
