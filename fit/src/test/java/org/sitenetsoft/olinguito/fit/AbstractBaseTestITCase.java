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
 *
 * Copyright 2026 SiteNetSoft - Added VFS reset in test teardown for test isolation
 * Copyright 2026 SiteNetSoft - Removed servlet types from test infrastructure
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 3: register the key-as-segment tecsvc endpoint
 */
package org.sitenetsoft.olinguito.fit;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.fit.server.TestServer;
import org.sitenetsoft.olinguito.fit.server.TestServerFactory;
import org.sitenetsoft.olinguito.fit.server.TomcatTestServer;
import org.sitenetsoft.olinguito.fit.utils.FSManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBaseTestITCase {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseTestITCase.class);

  protected static final int DEFAULT_PORT = 9080;

  protected abstract ODataClient getClient();

  private static TestServer server;

  @BeforeClass
  public static void init() throws Exception {
    if (TestServerFactory.isQuarkus()) {
      initQuarkusServer();
    } else {
      initTomcatServer();
    }
  }

  private static void initTomcatServer() throws Exception {
    LOG.info("Initializing Tomcat test server");
    server = TomcatTestServer.init(DEFAULT_PORT)
        .addServlet("org.sitenetsoft.olinguito.server.tecsvc.TechnicalServlet",
            "/odata-server-tecsvc/odata.svc/*")
        .addServlet("org.sitenetsoft.olinguito.server.tecsvc.TechnicalKeyAsSegmentServlet",
            "/odata-server-tecsvc/odata-kas.svc/*")
        .addServlet("org.sitenetsoft.olinguito.server.tecsvc.async.TechnicalStatusMonitorServlet",
            "/odata-server-tecsvc/status/*")
        .addAuthServlet("org.sitenetsoft.olinguito.server.tecsvc.TechnicalServlet",
            "/odata-server-tecsvc/auth", "/*")
        .addClasspathContent(
            "/odata-server-tecsvc/v4.0/cs02/vocabularies/Org.OData.Core.V1.xml",
            "org-odata-core-v1.xml")
        .addWebApp(false)
        .start();
  }

  private static void initQuarkusServer() throws Exception {
    LOG.info("Initializing Quarkus test server");
    // TODO: Quarkus testing is not yet fully implemented.
    // For now, fall back to Tomcat server.
    LOG.warn("Quarkus test server not yet implemented - falling back to Tomcat");
    initTomcatServer();
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    if (server != null) {
      server.invalidateAllSessions();
    }
    FSManager.reset();
  }

  /**
   * Returns the base URL for the OData service.
   *
   * @return the service URL
   */
  protected static String getServiceUrl() {
    if (server != null) {
      return server.getBaseUrl() + "/odata-server-tecsvc/odata.svc";
    }
    return "http://localhost:" + DEFAULT_PORT + "/odata-server-tecsvc/odata.svc";
  }

  /**
   * Returns the test server instance.
   *
   * @return the test server
   */
  protected static TestServer getServer() {
    return server;
  }
}
