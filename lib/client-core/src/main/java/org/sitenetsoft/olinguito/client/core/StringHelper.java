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
 * Copyright 2026 SiteNetSoft - String utility methods replacing commons-lang3 StringUtils
 */
package org.sitenetsoft.olinguito.client.core;

/**
 * Null-safe string utility methods that replace Apache Commons Lang StringUtils
 * substring operations with no direct JDK equivalent.
 */
public final class StringHelper {

  private StringHelper() {
    // utility class
  }

  /**
   * Gets the substring before the first occurrence of a separator.
   * Returns the original string if the separator is not found.
   * Returns {@code null} if the input string is {@code null}.
   *
   * @param str the string to get a substring from, may be null
   * @param separator the string to search for, may be null
   * @return the substring before the first occurrence of the separator,
   *         or the original string if not found, or {@code null} if null input
   */
  public static String substringBefore(final String str, final String separator) {
    if (str == null || separator == null) {
      return str;
    }
    final int pos = str.indexOf(separator);
    if (pos == -1) {
      return str;
    }
    return str.substring(0, pos);
  }

  /**
   * Gets the substring after the first occurrence of a separator.
   * Returns an empty string if the separator is not found.
   * Returns {@code null} if the input string is {@code null}.
   *
   * @param str the string to get a substring from, may be null
   * @param separator the string to search for, may be null
   * @return the substring after the first occurrence of the separator,
   *         or an empty string if not found, or {@code null} if null input
   */
  public static String substringAfter(final String str, final String separator) {
    if (str == null) {
      return null;
    }
    if (separator == null) {
      return "";
    }
    final int pos = str.indexOf(separator);
    if (pos == -1) {
      return "";
    }
    return str.substring(pos + separator.length());
  }

  /**
   * Gets the substring before the last occurrence of a separator.
   * Returns the original string if the separator is not found.
   * Returns {@code null} if the input string is {@code null}.
   *
   * @param str the string to get a substring from, may be null
   * @param separator the string to search for, may be null
   * @return the substring before the last occurrence of the separator,
   *         or the original string if not found, or {@code null} if null input
   */
  public static String substringBeforeLast(final String str, final String separator) {
    if (str == null || separator == null) {
      return str;
    }
    final int pos = str.lastIndexOf(separator);
    if (pos == -1) {
      return str;
    }
    return str.substring(0, pos);
  }

  /**
   * Gets the substring after the last occurrence of a separator.
   * Returns an empty string if the separator is not found.
   * Returns {@code null} if the input string is {@code null}.
   *
   * @param str the string to get a substring from, may be null
   * @param separator the string to search for, may be null
   * @return the substring after the last occurrence of the separator,
   *         or an empty string if not found, or {@code null} if null input
   */
  public static String substringAfterLast(final String str, final String separator) {
    if (str == null) {
      return null;
    }
    if (separator == null) {
      return "";
    }
    final int pos = str.lastIndexOf(separator);
    if (pos == -1) {
      return "";
    }
    return str.substring(pos + separator.length());
  }
}
