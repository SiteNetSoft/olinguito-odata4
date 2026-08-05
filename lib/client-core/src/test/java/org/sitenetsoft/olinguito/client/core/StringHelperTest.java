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
 * Copyright 2026 SiteNetSoft - Tests for StringHelper utility class
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.client.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringHelperTest {

  // --- substringBefore ---

  @Test
  void substringBeforeNullInput() {
    assertNull(StringHelper.substringBefore(null, "/"));
  }

  @Test
  void substringBeforeNullSeparator() {
    assertEquals("abc", StringHelper.substringBefore("abc", null));
  }

  @Test
  void substringBeforeNotFound() {
    assertEquals("abc", StringHelper.substringBefore("abc", "/"));
  }

  @Test
  void substringBeforeFound() {
    assertEquals("abc", StringHelper.substringBefore("abc/def", "/"));
  }

  @Test
  void substringBeforeFirstOccurrence() {
    assertEquals("abc", StringHelper.substringBefore("abc/def/ghi", "/"));
  }

  @Test
  void substringBeforeEmptyResult() {
    assertEquals("", StringHelper.substringBefore("/abc", "/"));
  }

  @Test
  void substringBeforeMultiCharSeparator() {
    assertEquals("http://example.com", StringHelper.substringBefore(
        "http://example.com/$metadata#Customers", "/$metadata"));
  }

  // --- substringAfter ---

  @Test
  void substringAfterNullInput() {
    assertNull(StringHelper.substringAfter(null, "/"));
  }

  @Test
  void substringAfterNullSeparator() {
    assertEquals("", StringHelper.substringAfter("abc", null));
  }

  @Test
  void substringAfterNotFound() {
    assertEquals("", StringHelper.substringAfter("abc", "/"));
  }

  @Test
  void substringAfterFound() {
    assertEquals("def", StringHelper.substringAfter("abc/def", "/"));
  }

  @Test
  void substringAfterFirstOccurrence() {
    assertEquals("def/ghi", StringHelper.substringAfter("abc/def/ghi", "/"));
  }

  @Test
  void substringAfterEmptyResult() {
    assertEquals("", StringHelper.substringAfter("abc/", "/"));
  }

  @Test
  void substringAfterMultiCharSeparator() {
    assertEquals("#Customers", StringHelper.substringAfter(
        "http://example.com/$metadata#Customers", "/$metadata"));
  }

  // --- substringBeforeLast ---

  @Test
  void substringBeforeLastNullInput() {
    assertNull(StringHelper.substringBeforeLast(null, "/"));
  }

  @Test
  void substringBeforeLastNullSeparator() {
    assertEquals("abc", StringHelper.substringBeforeLast("abc", null));
  }

  @Test
  void substringBeforeLastNotFound() {
    assertEquals("abc", StringHelper.substringBeforeLast("abc", "/"));
  }

  @Test
  void substringBeforeLastFound() {
    assertEquals("abc", StringHelper.substringBeforeLast("abc/def", "/"));
  }

  @Test
  void substringBeforeLastMultipleOccurrences() {
    assertEquals("abc/def", StringHelper.substringBeforeLast("abc/def/ghi", "/"));
  }

  @Test
  void substringBeforeLastDottedName() {
    assertEquals("org.example", StringHelper.substringBeforeLast("org.example.Term", "."));
  }

  // --- substringAfterLast ---

  @Test
  void substringAfterLastNullInput() {
    assertNull(StringHelper.substringAfterLast(null, "/"));
  }

  @Test
  void substringAfterLastNullSeparator() {
    assertEquals("", StringHelper.substringAfterLast("abc", null));
  }

  @Test
  void substringAfterLastNotFound() {
    assertEquals("", StringHelper.substringAfterLast("abc", "/"));
  }

  @Test
  void substringAfterLastFound() {
    assertEquals("def", StringHelper.substringAfterLast("abc/def", "/"));
  }

  @Test
  void substringAfterLastMultipleOccurrences() {
    assertEquals("ghi", StringHelper.substringAfterLast("abc/def/ghi", "/"));
  }

  @Test
  void substringAfterLastEmptyResult() {
    assertEquals("", StringHelper.substringAfterLast("abc/", "/"));
  }

  @Test
  void substringAfterLastContextUrl() {
    assertEquals("4326", StringHelper.substringAfterLast(
        "http://www.opengis.net/def/crs/EPSG/0/4326", "/"));
  }

  // --- edge cases ---

  @Test
  void emptyStringInput() {
    assertEquals("", StringHelper.substringBefore("", "/"));
    assertEquals("", StringHelper.substringAfter("", "/"));
    assertEquals("", StringHelper.substringBeforeLast("", "/"));
    assertEquals("", StringHelper.substringAfterLast("", "/"));
  }

  @Test
  void emptySeparator() {
    assertEquals("", StringHelper.substringBefore("abc", ""));
    assertEquals("abc", StringHelper.substringAfter("abc", ""));
    assertEquals("abc", StringHelper.substringBeforeLast("abc", ""));
    assertEquals("", StringHelper.substringAfterLast("abc", ""));
  }
}
