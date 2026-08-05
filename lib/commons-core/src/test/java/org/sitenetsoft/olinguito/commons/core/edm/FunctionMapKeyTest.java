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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.junit.jupiter.api.Test;

class FunctionMapKeyTest {

  private final FullQualifiedName fqn = new FullQualifiedName("namespace", "name");

  private final FullQualifiedName fqnType = new FullQualifiedName("namespace2", "name2");

  @Test
  void testEqualsPositive() {
    FunctionMapKey key = new FunctionMapKey(fqn, null, null, null);
    FunctionMapKey someKey = new FunctionMapKey(fqn, null, null, null);
    assertEquals(key, someKey);

    key = new FunctionMapKey(fqn, null, true, null);
    someKey = new FunctionMapKey(fqn, null, true, null);
    assertEquals(key, someKey);

    key = new FunctionMapKey(fqn, fqnType, true, null);
    someKey = new FunctionMapKey(fqn, fqnType, true, null);
    assertEquals(key, someKey);

    key = new FunctionMapKey(fqn, fqnType, false, null);
    someKey = new FunctionMapKey(fqn, fqnType, false, null);
    assertEquals(key, someKey);

    key = new FunctionMapKey(fqn, fqnType, false, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, fqnType, false, new ArrayList<>());
    assertEquals(key, someKey);

    List<String> keyList = new ArrayList<>();
    keyList.add("Employee");
    List<String> someKeyList = new ArrayList<>();
    someKeyList.add("Employee");
    key = new FunctionMapKey(fqn, fqnType, false, keyList);
    someKey = new FunctionMapKey(fqn, fqnType, false, someKeyList);
    assertEquals(key, someKey);

    key = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    assertEquals(key, someKey);

    keyList = new ArrayList<>();
    keyList.add("Employee");
    someKeyList = new ArrayList<>();
    someKeyList.add("Employee");
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertEquals(key, someKey);
  }

  @Test
  void testEqualsNegative() {
    FunctionMapKey key = new FunctionMapKey(fqn, null, null, null);
    FunctionMapKey someKey = new FunctionMapKey(fqn, null, true, null);
    assertNotSame(key, someKey);

    key = new FunctionMapKey(fqn, null, true, null);
    someKey = new FunctionMapKey(fqn, null, false, null);
    assertNotSame(key, someKey);

    key = new FunctionMapKey(fqn, fqnType, true, null);
    someKey = new FunctionMapKey(fqn, null, true, null);
    assertNotSame(key, someKey);

    key = new FunctionMapKey(fqn, null, false, null);
    someKey = new FunctionMapKey(fqn, fqnType, true, null);
    assertNotSame(key, someKey);

    key = new FunctionMapKey(fqn, fqnType, false, null);
    someKey = new FunctionMapKey(fqn, fqnType, false, new ArrayList<>());
    assertNotSame(key, someKey);

    List<String> keyList = new ArrayList<>();
    keyList.add("Employee");
    List<String> someKeyList = new ArrayList<>();
    someKeyList.add("Employee2");
    key = new FunctionMapKey(fqn, fqnType, false, keyList);
    someKey = new FunctionMapKey(fqn, fqnType, false, someKeyList);
    assertNotSame(key, someKey);

    key = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    assertNotSame(key, someKey);

    keyList = new ArrayList<>();
    keyList.add("Employee");
    someKeyList = new ArrayList<>();
    someKeyList.add("Employee2");
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertNotSame(key, someKey);

    key = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, null, null, null);
    assertNotSame(key, someKey);

    keyList = new ArrayList<>();
    keyList.add("Employee");
    someKeyList = new ArrayList<>();
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertNotSame(key, someKey);

    keyList = new ArrayList<>();
    keyList.add("Employee");
    someKeyList = new ArrayList<>();
    someKeyList.add("EmpLoYeE");
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertNotSame(key, someKey);
  }

  @Test
  void testHashCodePositive() {
    FunctionMapKey key = new FunctionMapKey(fqn, null, null, null);
    FunctionMapKey someKey = new FunctionMapKey(fqn, null, null, null);
    assertEquals(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, null, true, null);
    someKey = new FunctionMapKey(fqn, null, true, null);
    assertEquals(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, fqnType, true, null);
    someKey = new FunctionMapKey(fqn, fqnType, true, null);
    assertEquals(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, fqnType, false, null);
    someKey = new FunctionMapKey(fqn, fqnType, false, null);
    assertEquals(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, fqnType, false, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, fqnType, false, new ArrayList<>());
    assertEquals(key.hashCode(), someKey.hashCode());

    List<String> keyList = new ArrayList<>();
    keyList.add("Employee");
    List<String> someKeyList = new ArrayList<>();
    someKeyList.add("Employee");
    key = new FunctionMapKey(fqn, fqnType, false, keyList);
    someKey = new FunctionMapKey(fqn, fqnType, false, someKeyList);
    assertEquals(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    assertEquals(key.hashCode(), someKey.hashCode());

    keyList = new ArrayList<>();
    keyList.add("Employee");
    someKeyList = new ArrayList<>();
    someKeyList.add("Employee");
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertEquals(key.hashCode(), someKey.hashCode());

    keyList = new ArrayList<>();
    keyList.add("Employee");
    keyList.add("employee");
    someKeyList = new ArrayList<>();
    someKeyList.add("Employee");
    someKeyList.add("employee");
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertEquals(key.hashCode(), someKey.hashCode());

    keyList = new ArrayList<>();
    keyList.add("Employee");
    keyList.add("Employee2");
    someKeyList = new ArrayList<>();
    someKeyList.add("Employee2");
    someKeyList.add("Employee");
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertEquals(key.hashCode(), someKey.hashCode());
  }

  @Test
  void testHashCodeNegative() {
    FunctionMapKey key = new FunctionMapKey(fqn, null, null, null);
    FunctionMapKey someKey = new FunctionMapKey(fqn, null, true, null);
    assertNotSame(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, null, true, null);
    someKey = new FunctionMapKey(fqn, null, false, null);
    assertNotSame(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, fqnType, true, null);
    someKey = new FunctionMapKey(fqn, null, true, null);
    assertNotSame(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, null, false, null);
    someKey = new FunctionMapKey(fqn, fqnType, true, null);
    assertNotSame(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, fqnType, false, null);
    someKey = new FunctionMapKey(fqn, fqnType, false, new ArrayList<>());
    assertNotSame(key.hashCode(), someKey.hashCode());

    List<String> keyList = new ArrayList<>();
    keyList.add("Employee");
    List<String> someKeyList = new ArrayList<>();
    someKeyList.add("Employee2");
    key = new FunctionMapKey(fqn, fqnType, false, keyList);
    someKey = new FunctionMapKey(fqn, fqnType, false, someKeyList);
    assertNotSame(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    assertNotSame(key.hashCode(), someKey.hashCode());

    keyList = new ArrayList<>();
    keyList.add("Employee");
    someKeyList = new ArrayList<>();
    someKeyList.add("Employee2");
    key = new FunctionMapKey(fqn, null, null, keyList);
    someKey = new FunctionMapKey(fqn, null, null, someKeyList);
    assertNotSame(key.hashCode(), someKey.hashCode());

    key = new FunctionMapKey(fqn, null, null, new ArrayList<>());
    someKey = new FunctionMapKey(fqn, null, null, null);
    assertNotSame(key.hashCode(), someKey.hashCode());
  }

}
