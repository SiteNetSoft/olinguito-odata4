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
package org.sitenetsoft.olinguito.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 *
 */
class DecoderTest {

  @Test
  void asciiCharacters() {
    assertNull(Decoder.decode(null));

    String s = "azAZ019";
    assertEquals(s, Decoder.decode(s));

    s = "\"\\`{}|";
    assertEquals(s, Decoder.decode(s));
  }

  @Test
  void asciiControl() {
    assertEquals("\u0000\b\t\n\r", Decoder.decode("%00%08%09%0a%0d"));
  }

  @Test
  void asciiEncoded() {
    assertEquals("<>%&", Decoder.decode("%3c%3e%25%26"));
    assertEquals(":/?#[]@", Decoder.decode("%3a%2f%3f%23%5b%5d%40"));
    assertEquals(" !\"$'()*+,-.", Decoder.decode("%20%21%22%24%27%28%29%2A%2B%2C%2D%2E"));
  }

  @Test
  void unicodeCharacters() {
    assertEquals("€", Decoder.decode("%E2%82%AC"));
    assertEquals("\uFDFC", Decoder.decode("%EF%B7%BC"));
  }

  @Test
  void charactersOutsideBmp() {
    assertEquals(String.valueOf(Character.toChars(0x1F603)), Decoder.decode("%f0%9f%98%83"));
  }

  @Test
  void wrongCharacter() {
      assertThrows(IllegalArgumentException.class, () -> Decoder.decode("%20ä"));
  }

  @Test
  void wrongPercentNumber() {
      assertThrows(NumberFormatException.class, () -> Decoder.decode("%-3"));
  }

  @Test
  void wrongPercentPercent() {
      assertThrows(IllegalArgumentException.class, () -> Decoder.decode("%%a"));
  }

  @Test
  void unfinishedPercent() {
      assertThrows(IllegalArgumentException.class, () -> Decoder.decode("%a"));
  }

  @Test
  void nullByte() {
      assertThrows(IllegalArgumentException.class, () -> Decoder.decode("%\u0000ff"));
  }
}
