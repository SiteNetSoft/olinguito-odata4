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
 * Copyright 2026 SiteNetSoft - Tier 7 Task 1: pinned case- and prefix-insensitive option names
 */
package org.sitenetsoft.olinguito.server.api.uri.queryoption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests the resolution of system query option names, which since OData 4.01 are
 * case-insensitive and may omit the "$" prefix ([OData-Protocol] 13.2.1 items 6 and 7).
 */
class SystemQueryOptionKindTest {

  @Test
  void dollarPrefixedLowerCaseIsUnchanged() {
    assertEquals(SystemQueryOptionKind.FILTER, SystemQueryOptionKind.get("$filter"));
    assertEquals(SystemQueryOptionKind.SCHEMAVERSION, SystemQueryOptionKind.get("$schemaversion"));
  }

  @Test
  void dollarIsOptional() {
    assertEquals(SystemQueryOptionKind.FILTER, SystemQueryOptionKind.get("filter"));
    assertEquals(SystemQueryOptionKind.SELECT, SystemQueryOptionKind.get("select"));
    assertEquals(SystemQueryOptionKind.TOP, SystemQueryOptionKind.get("top"));
  }

  @Test
  void namesAreCaseInsensitive() {
    assertEquals(SystemQueryOptionKind.FILTER, SystemQueryOptionKind.get("$FILTER"));
    assertEquals(SystemQueryOptionKind.FILTER, SystemQueryOptionKind.get("Filter"));
    assertEquals(SystemQueryOptionKind.ORDERBY, SystemQueryOptionKind.get("OrderBy"));
  }

  @Test
  void nonOptionsResolveToNull() {
    assertNull(SystemQueryOptionKind.get(null));
    assertNull(SystemQueryOptionKind.get(""));
    assertNull(SystemQueryOptionKind.get("$"));
    assertNull(SystemQueryOptionKind.get("notAnOption"));
    assertNull(SystemQueryOptionKind.get("$notAnOption"));
  }
}
