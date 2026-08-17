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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 3: base URI of the key-as-segment tecsvc endpoint
 */
package org.sitenetsoft.olinguito.fit.tecsvc;

public class TecSvcConst {

  private static final int DEFAULT_PORT = 9080;

  private static String getBaseUrl() {
    String port = System.getProperty("tomcat.servlet.port", String.valueOf(DEFAULT_PORT));
    return "http://localhost:" + port;
  }

  public static String getServiceUri() {
    return getBaseUrl() + "/odata-server-tecsvc/odata.svc";
  }

  public static String getAuthUri() {
    return getBaseUrl() + "/odata-server-tecsvc/auth";
  }

  // Legacy constants for backward compatibility
  public final static String BASE_URI = "http://localhost:9080/odata-server-tecsvc/odata.svc";
  public final static String AUTH_URI = "http://localhost:9080/odata-server-tecsvc/auth";
  /** The same technical service, served with the OData 4.01 key-as-segment convention switched on. */
  public final static String KEY_AS_SEGMENT_BASE_URI =
      "http://localhost:9080/odata-server-tecsvc/odata-kas.svc";

}
