/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Code quality: try-with-resources, narrowed exception catch
 */
package org.sitenetsoft.olinguito.client.core.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpClientFactory implements HttpClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractHttpClientFactory.class);

  protected static final String USER_AGENT;

  static {
    final StringBuilder userAgent = new StringBuilder("Apache-Olingo");

    try (InputStream input =
             AbstractHttpClientFactory.class.getResourceAsStream("/client.properties")) {
      if (input != null) {
        final Properties prop = new Properties();
        prop.load(input);
        userAgent.append('/').append(prop.getProperty("version"));
      }
    } catch (IOException e) {
      LOG.warn("Could not get Apache Olingo version", e);
    }

    USER_AGENT = userAgent.toString();
  }
}
