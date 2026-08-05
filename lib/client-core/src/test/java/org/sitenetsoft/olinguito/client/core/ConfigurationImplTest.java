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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1587: configurable response timeout
 */
package org.sitenetsoft.olinguito.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** OLINGO-1587: the streamed/batch response timeout is configurable (default 300s). */
class ConfigurationImplTest {

  @Test
  void responseTimeoutDefaultsTo300() {
    assertEquals(300, new ConfigurationImpl().getResponseTimeoutInSec());
  }

  @Test
  void responseTimeoutIsConfigurable() {
    final ConfigurationImpl configuration = new ConfigurationImpl();
    configuration.setResponseTimeoutInSec(42);
    assertEquals(42, configuration.getResponseTimeoutInSec());
  }

  @Test
  void responseTimeoutRejectsNonPositive() {
    final ConfigurationImpl configuration = new ConfigurationImpl();
    assertThrows(IllegalArgumentException.class, () -> configuration.setResponseTimeoutInSec(0));
    assertThrows(IllegalArgumentException.class, () -> configuration.setResponseTimeoutInSec(-1));
  }
}
