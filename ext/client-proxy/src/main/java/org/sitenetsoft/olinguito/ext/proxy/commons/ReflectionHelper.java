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
 * Copyright 2026 SiteNetSoft - Replacement for commons-lang3 reflection builders
 */
package org.sitenetsoft.olinguito.ext.proxy.commons;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Reflection-based equals, hashCode, and toString utilities replacing
 * {@code EqualsBuilder.reflectionEquals}, {@code HashCodeBuilder.reflectionHashCode},
 * and {@code ReflectionToStringBuilder.toString} from commons-lang3.
 */
public final class ReflectionHelper {

  private ReflectionHelper() {
    // utility class
  }

  /**
   * Reflection-based equals. Compares all non-static, non-transient fields
   * of two objects, excluding the specified field names.
   */
  public static boolean reflectionEquals(Object lhs, Object rhs, String... excludeFields) {
    if (lhs == rhs) {
      return true;
    }
    if (lhs == null || rhs == null) {
      return false;
    }
    if (lhs.getClass() != rhs.getClass()) {
      return false;
    }

    final Set<String> excluded = Set.of(excludeFields);
    for (Field field : getAllFields(lhs.getClass())) {
      if (excluded.contains(field.getName())) {
        continue;
      }
      field.setAccessible(true);
      try {
        if (!Objects.equals(field.get(lhs), field.get(rhs))) {
          return false;
        }
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Cannot access field: " + field.getName(), e);
      }
    }
    return true;
  }

  /**
   * Reflection-based hashCode. Computes a hash from all non-static, non-transient fields,
   * excluding the specified field names.
   */
  public static int reflectionHashCode(Object obj, String... excludeFields) {
    if (obj == null) {
      throw new IllegalArgumentException("Object must not be null");
    }

    final Set<String> excluded = Set.of(excludeFields);
    int result = 17;
    for (Field field : getAllFields(obj.getClass())) {
      if (excluded.contains(field.getName())) {
        continue;
      }
      field.setAccessible(true);
      try {
        result = 31 * result + Objects.hashCode(field.get(obj));
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Cannot access field: " + field.getName(), e);
      }
    }
    return result;
  }

  /**
   * Reflection-based toString. Produces a string containing all non-static, non-transient
   * field names and values.
   */
  public static String reflectionToString(Object obj) {
    if (obj == null) {
      return "null";
    }

    final StringJoiner joiner = new StringJoiner(", ",
        obj.getClass().getSimpleName() + "[", "]");
    for (Field field : getAllFields(obj.getClass())) {
      field.setAccessible(true);
      try {
        joiner.add(field.getName() + "=" + field.get(obj));
      } catch (IllegalAccessException e) {
        joiner.add(field.getName() + "=<inaccessible>");
      }
    }
    return joiner.toString();
  }

  /**
   * Collects all non-static, non-transient fields from the class hierarchy.
   */
  private static List<Field> getAllFields(Class<?> clazz) {
    final List<Field> fields = new ArrayList<>();
    for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field field : c.getDeclaredFields()) {
        final int modifiers = field.getModifiers();
        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
          fields.add(field);
        }
      }
    }
    return fields;
  }
}
