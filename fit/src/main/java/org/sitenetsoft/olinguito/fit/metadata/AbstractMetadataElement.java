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
 * Copyright 2026 SiteNetSoft - Removed commons-lang3 dependency
 */
package org.sitenetsoft.olinguito.fit.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;

public class AbstractMetadataElement {

  @Override
  public String toString() {
    final StringJoiner joiner = new StringJoiner(", ",
        getClass().getSimpleName() + "[", "]");
    for (Class<?> c = getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field field : c.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
          continue;
        }
        field.setAccessible(true);
        try {
          joiner.add(field.getName() + "=" + field.get(this));
        } catch (IllegalAccessException e) {
          joiner.add(field.getName() + "=<inaccessible>");
        }
      }
    }
    return joiner.toString();
  }
}
