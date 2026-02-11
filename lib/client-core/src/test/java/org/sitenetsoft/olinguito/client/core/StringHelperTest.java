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
 */
package org.sitenetsoft.olinguito.client.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringHelperTest {

  // --- substringBefore ---

  @Test
  public void substringBeforeNullInput() {
    assertNull(StringHelper.substringBefore(null, "/"));
  }

  @Test
  public void substringBeforeNullSeparator() {
    assertEquals("abc", StringHelper.substringBefore("abc", null));
  }

  @Test
  public void substringBeforeNotFound() {
    assertEquals("abc", StringHelper.substringBefore("abc", "/"));
  }

  @Test
  public void substringBeforeFound() {
    assertEquals("abc", StringHelper.substringBefore("abc/def", "/"));
  }

  @Test
  public void substringBeforeFirstOccurrence() {
    assertEquals("abc", StringHelper.substringBefore("abc/def/ghi", "/"));
  }

  @Test
  public void substringBeforeEmptyResult() {
    assertEquals("", StringHelper.substringBefore("/abc", "/"));
  }

  @Test
  public void substringBeforeMultiCharSeparator() {
    assertEquals("http://example.com", StringHelper.substringBefore(
        "http://example.com/$metadata#Customers", "/$metadata"));
  }

  // --- substringAfter ---

  @Test
  public void substringAfterNullInput() {
    assertNull(StringHelper.substringAfter(null, "/"));
  }

  @Test
  public void substringAfterNullSeparator() {
    assertEquals("", StringHelper.substringAfter("abc", null));
  }

  @Test
  public void substringAfterNotFound() {
    assertEquals("", StringHelper.substringAfter("abc", "/"));
  }

  @Test
  public void substringAfterFound() {
    assertEquals("def", StringHelper.substringAfter("abc/def", "/"));
  }

  @Test
  public void substringAfterFirstOccurrence() {
    assertEquals("def/ghi", StringHelper.substringAfter("abc/def/ghi", "/"));
  }

  @Test
  public void substringAfterEmptyResult() {
    assertEquals("", StringHelper.substringAfter("abc/", "/"));
  }

  @Test
  public void substringAfterMultiCharSeparator() {
    assertEquals("#Customers", StringHelper.substringAfter(
        "http://example.com/$metadata#Customers", "/$metadata"));
  }

  // --- substringBeforeLast ---

  @Test
  public void substringBeforeLastNullInput() {
    assertNull(StringHelper.substringBeforeLast(null, "/"));
  }

  @Test
  public void substringBeforeLastNullSeparator() {
    assertEquals("abc", StringHelper.substringBeforeLast("abc", null));
  }

  @Test
  public void substringBeforeLastNotFound() {
    assertEquals("abc", StringHelper.substringBeforeLast("abc", "/"));
  }

  @Test
  public void substringBeforeLastFound() {
    assertEquals("abc", StringHelper.substringBeforeLast("abc/def", "/"));
  }

  @Test
  public void substringBeforeLastMultipleOccurrences() {
    assertEquals("abc/def", StringHelper.substringBeforeLast("abc/def/ghi", "/"));
  }

  @Test
  public void substringBeforeLastDottedName() {
    assertEquals("org.example", StringHelper.substringBeforeLast("org.example.Term", "."));
  }

  // --- substringAfterLast ---

  @Test
  public void substringAfterLastNullInput() {
    assertNull(StringHelper.substringAfterLast(null, "/"));
  }

  @Test
  public void substringAfterLastNullSeparator() {
    assertEquals("", StringHelper.substringAfterLast("abc", null));
  }

  @Test
  public void substringAfterLastNotFound() {
    assertEquals("", StringHelper.substringAfterLast("abc", "/"));
  }

  @Test
  public void substringAfterLastFound() {
    assertEquals("def", StringHelper.substringAfterLast("abc/def", "/"));
  }

  @Test
  public void substringAfterLastMultipleOccurrences() {
    assertEquals("ghi", StringHelper.substringAfterLast("abc/def/ghi", "/"));
  }

  @Test
  public void substringAfterLastEmptyResult() {
    assertEquals("", StringHelper.substringAfterLast("abc/", "/"));
  }

  @Test
  public void substringAfterLastContextUrl() {
    assertEquals("4326", StringHelper.substringAfterLast(
        "http://www.opengis.net/def/crs/EPSG/0/4326", "/"));
  }

  // --- edge cases ---

  @Test
  public void emptyStringInput() {
    assertEquals("", StringHelper.substringBefore("", "/"));
    assertEquals("", StringHelper.substringAfter("", "/"));
    assertEquals("", StringHelper.substringBeforeLast("", "/"));
    assertEquals("", StringHelper.substringAfterLast("", "/"));
  }

  @Test
  public void emptySeparator() {
    assertEquals("", StringHelper.substringBefore("abc", ""));
    assertEquals("abc", StringHelper.substringAfter("abc", ""));
    assertEquals("abc", StringHelper.substringBeforeLast("abc", ""));
    assertEquals("", StringHelper.substringAfterLast("abc", ""));
  }
}
