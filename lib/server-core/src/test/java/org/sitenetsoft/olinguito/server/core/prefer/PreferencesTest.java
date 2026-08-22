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
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Added tests for the omit-values preference (OData 4.01, Protocol Section 8.2.8.6)
 */
package org.sitenetsoft.olinguito.server.core.prefer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.server.api.prefer.Preferences;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences.Preference;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences.Return;
import org.junit.jupiter.api.Test;

class PreferencesTest {

  @Test
  void empty() {
    final Preferences preferences = new PreferencesImpl(null);
    assertFalse(preferences.hasAllowEntityReferences());
    assertNull(preferences.getCallback());
    assertFalse(preferences.hasContinueOnError());
    assertNull(preferences.getMaxPageSize());
    assertFalse(preferences.hasTrackChanges());
    assertNull(preferences.getOmitValues());
    assertNull(preferences.getReturn());
    assertFalse(preferences.hasRespondAsync());
    assertNull(preferences.getWait());
  }

  @Test
  void all() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton(
        "odata.allow-entityreferences, odata.callback;url=\"callbackURI\","
            + "odata.continue-on-error, odata.include-annotations=\"*\", odata.maxpagesize=42,"
            + "odata.track-changes, omit-values=nulls, return=representation, respond-async, wait=12345"));
    assertTrue(preferences.hasAllowEntityReferences());
    assertEquals(URI.create("callbackURI"), preferences.getCallback());
    assertNotNull(preferences.getPreference("odata.callback"));
    assertNull(preferences.getPreference("odata.callback").getValue());
    assertEquals("callbackURI", preferences.getPreference("odata.callback").getParameters().get("url"));
    assertTrue(preferences.hasContinueOnError());
    assertEquals("*", preferences.getPreference("odata.Include-Annotations").getValue());
    assertEquals(Integer.valueOf(42), preferences.getMaxPageSize());
    assertEquals("42", preferences.getPreference("odata.MaxPageSize").getValue());
    assertTrue(preferences.hasTrackChanges());
    assertEquals(Preferences.OmitValues.NULLS, preferences.getOmitValues());
    assertEquals(Return.REPRESENTATION, preferences.getReturn());
    assertTrue(preferences.hasRespondAsync());
    assertEquals(Integer.valueOf(12345), preferences.getWait());
  }

  @Test
  void bothSpellingsOfEveryPreference() {
    // [OData-Protocol] 8.2.8: the bare name is the OData 4.01 spelling and the "odata."-prefixed
    // one is the 4.0 spelling a 4.01 service still accepts; 13.2.1 item 4 requires both.
    final Preferences bare = new PreferencesImpl(Collections.singleton(
        "allow-entityreferences, callback;url=\"callbackURI\", continue-on-error,"
            + " include-annotations=\"*\", maxpagesize=42, track-changes"));
    assertTrue(bare.hasAllowEntityReferences());
    assertEquals(URI.create("callbackURI"), bare.getCallback());
    assertTrue(bare.hasContinueOnError());
    assertEquals(Integer.valueOf(42), bare.getMaxPageSize());
    assertTrue(bare.hasTrackChanges());

    // The 4.0 spelling keeps working; "all" above already pins the rest of that form.
    final Preferences prefixed = new PreferencesImpl(Collections.singleton(
        "odata.allow-entityreferences, odata.continue-on-error, odata.maxpagesize=42"));
    assertTrue(prefixed.hasAllowEntityReferences());
    assertTrue(prefixed.hasContinueOnError());
    assertEquals(Integer.valueOf(42), prefixed.getMaxPageSize());
  }

  @Test
  void bareSpellingWinsWhenBothAreSent() {
    // 8.2.8.2 (callback) and 8.2.8.4 (include-annotations) state the tie-break explicitly:
    // the value of the unprefixed preference is used. Applied uniformly.
    final Preferences preferences = new PreferencesImpl(Collections.singleton(
        "maxpagesize=1, odata.maxpagesize=2"));
    assertEquals(Integer.valueOf(1), preferences.getMaxPageSize());
  }

  @Test
  void omitValuesDefaults() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton("omit-values=defaults"));
    assertEquals(Preferences.OmitValues.DEFAULTS, preferences.getOmitValues());
  }

  @Test
  void omitValuesCaseInsensitive() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton("omit-values=NuLLs"));
    assertEquals(Preferences.OmitValues.NULLS, preferences.getOmitValues());
  }

  @Test
  void omitValuesUnknownValue() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton("omit-values=weird"));
    assertNull(preferences.getOmitValues());
  }

  @Test
  void omitValuesNotSet() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton("wait=1"));
    assertNull(preferences.getOmitValues());
  }

  @Test
  void caseSensitivity() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton(
        "OData.Callback;URL=\"callbackURI\", return=REPRESENTATION, Wait=42"));
    assertEquals(URI.create("callbackURI"), preferences.getCallback());
    assertNull(preferences.getReturn());
    assertEquals(Integer.valueOf(42), preferences.getWait());
  }

  @Test
  void multipleValues() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton(
        ",return=minimal, ,, return=representation, wait=1, wait=2, wait=3,"));
    assertEquals(Return.MINIMAL, preferences.getReturn());
    assertEquals(Integer.valueOf(1), preferences.getWait());
  }

  @Test
  void multipleValuesDifferentHeaders() {
    final Preferences preferences = new PreferencesImpl(Arrays.asList(
        null, "",
        "return=representation, wait=1",
        "return=minimal, wait=2",
        "wait=3"));
    assertEquals(Return.REPRESENTATION, preferences.getReturn());
    assertEquals(Integer.valueOf(1), preferences.getWait());
  }

  @Test
  void multipleParameters() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton(
        "preference=a;;b=c; d = e; f;; ; g; h=\"i\";, wait=42"));
    final Preference preference = preferences.getPreference("preference");
    assertEquals("a", preference.getValue());
    final Map<String, String> parameters = preference.getParameters();
    assertEquals(5, parameters.size());
    assertEquals("c", parameters.get("b"));
    assertEquals("e", parameters.get("d"));
    assertTrue(parameters.containsKey("f"));
    assertNull(parameters.get("f"));
    assertTrue(parameters.containsKey("g"));
    assertNull(parameters.get("g"));
    assertEquals("i", parameters.get("h"));
    assertEquals(Integer.valueOf(42), preferences.getWait());
  }

  @Test
  void quotedValue() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton(
        "strangePreference=\"x\\\\y,\\\"abc\\\"z\", wait=42"));
    assertEquals("x\\y,\"abc\"z", preferences.getPreference("strangePreference").getValue());
    assertEquals(Integer.valueOf(42), preferences.getWait());
  }

  @Test
  void specialCharacters() {
    final Preferences preferences = new PreferencesImpl(Collections.singleton(
        "!#$%&'*+-.^_`|~ = \"!#$%&'()*+,-./:;<=>?@[]^_`{|}~¡\u00FF\", wait=42"));
    assertEquals("!#$%&'()*+,-./:;<=>?@[]^_`{|}~¡\u00FF",
        preferences.getPreference("!#$%&'*+-.^_`|~").getValue());
    assertEquals(Integer.valueOf(42), preferences.getWait());
  }

  @Test
  void wrongContent() {
    final Preferences preferences = new PreferencesImpl(List.of(
        "odata.callback;url=\":\"",
        "odata.maxpagesize=12345678901234567890",
        "return=something",
        "wait=-1"));
    assertNull(preferences.getCallback());
    assertEquals(":", preferences.getPreference("odata.callback").getParameters().get("url"));
    assertNull(preferences.getMaxPageSize());
    assertEquals("12345678901234567890", preferences.getPreference("odata.maxpagesize").getValue());
    assertNull(preferences.getReturn());
    assertEquals("something", preferences.getPreference("return").getValue());
    assertNull(preferences.getWait());
    assertEquals("-1", preferences.getPreference("wait").getValue());
  }

  @Test
  void wrongFormat() {
    final Preferences preferences = new PreferencesImpl(List.of(
        "return=, wait=1",
        "return=;, wait=2",
        "return=representation=, wait=3",
        "return=\"representation\"respond-async, wait=4",
        "respond-async[], wait=5",
        "odata.callback;=, wait=6",
        "odata.callback;url=, wait=7",
        "odata.callback;[], wait=8",
        "odata.callback;url=\"url\"parameter, wait=9",
        "wait=10"));
    assertEquals(Integer.valueOf(10), preferences.getWait());
  }
}
