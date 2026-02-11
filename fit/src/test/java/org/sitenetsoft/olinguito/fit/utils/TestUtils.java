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
 * Copyright 2026 SiteNetSoft - Random string generation replacing commons-lang3 RandomStringUtils
 */
package org.sitenetsoft.olinguito.fit.utils;

import java.security.SecureRandom;

/**
 * Test utility methods replacing Apache Commons Lang RandomStringUtils.
 */
public final class TestUtils {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String ALPHA_LOWER = "abcdefghijklmnopqrstuvwxyz";
  private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";

  private TestUtils() {
    // utility class
  }

  /**
   * Generates a random string of the given length from the specified character set.
   *
   * @param length the length of the random string
   * @param chars  the characters to choose from
   * @return a random string
   */
  public static String randomString(int length, String chars) {
    final StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
  }

  /**
   * Generates a random alphabetic (lowercase) string of the given length.
   */
  public static String randomAlphabetic(int length) {
    return randomString(length, ALPHA_LOWER);
  }

  /**
   * Generates a random alphanumeric string of the given length.
   */
  public static String randomAlphanumeric(int length) {
    return randomString(length, ALPHANUMERIC);
  }
}
