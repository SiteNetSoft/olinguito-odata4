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
package org.sitenetsoft.olinguito.server.tecsvc.quarkus;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages session state for OData requests in Quarkus.
 * Maps session IDs (from cookies) to DataProvider instances.
 */
@ApplicationScoped
public class SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    public static final String SESSION_COOKIE_NAME = "ODATA_SESSION_ID";

    private final ConcurrentHashMap<String, DataProvider> sessions = new ConcurrentHashMap<>();

    /**
     * Gets or creates a DataProvider for the given session ID.
     *
     * @param sessionId the session ID (from cookie)
     * @param odata the OData instance
     * @param edm the EDM
     * @return the DataProvider for this session
     */
    public DataProvider getOrCreate(String sessionId, OData odata, Edm edm) {
        return sessions.computeIfAbsent(sessionId, id -> {
            LOG.info("Created new DataProvider for session: {}", id);
            return new DataProvider(odata, edm);
        });
    }

    /**
     * Gets a DataProvider for the given session ID, or null if not found.
     *
     * @param sessionId the session ID
     * @return the DataProvider, or null
     */
    public DataProvider get(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Generates a new unique session ID.
     *
     * @return a new session ID
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Invalidates a specific session.
     *
     * @param sessionId the session ID to invalidate
     */
    public void invalidate(String sessionId) {
        DataProvider removed = sessions.remove(sessionId);
        if (removed != null) {
            LOG.info("Invalidated session: {}", sessionId);
        }
    }

    /**
     * Invalidates all sessions.
     */
    public void invalidateAll() {
        int count = sessions.size();
        sessions.clear();
        LOG.info("Invalidated all {} sessions", count);
    }

    /**
     * Returns the number of active sessions.
     *
     * @return the session count
     */
    public int getSessionCount() {
        return sessions.size();
    }
}
