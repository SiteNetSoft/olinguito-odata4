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
 * Copyright 2026 SiteNetSoft - Removed unnecessary boxing and modernized length checks
 * Copyright 2026 SiteNetSoft - Added the omit-values preference (OData 4.01, Protocol Section 8.2.8.6)
 * Copyright 2026 SiteNetSoft - Tier 7 Wave 2: accept both spellings of every OData preference
 */
package org.sitenetsoft.olinguito.server.core.prefer;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.format.PreferenceName;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences;

/**
 * Provides access methods to the preferences set in the Prefer HTTP request
 * header as described in <a href="https://www.ietf.org/rfc/rfc7240.txt">RFC 7240</a>.
 * Preferences defined in the OData standard can be accessed with named methods.
 */
public class PreferencesImpl implements Preferences {

  //parameter name for odata.callback
  private static final String URL = "url";

  private final Map<String, Preference> preferences;

  public PreferencesImpl(final Collection<String> preferHeaders) {
    preferences = PreferParser.parse(preferHeaders);
  }

  @Override
  public Preference getPreference(final String name) {
    return preferences.get(name.toLowerCase(Locale.ROOT));
  }

  @Override
  public boolean hasAllowEntityReferences() {
    return resolve(PreferenceName.ALLOW_ENTITY_REFERENCES_PREF, PreferenceName.ALLOW_ENTITY_REFERENCES) != null;
  }

  @Override
  public URI getCallback() {
    final Preference callback = resolve(PreferenceName.CALLBACK_PREF, PreferenceName.CALLBACK);
    if (callback != null
        && callback.getParameters() != null
        && callback.getParameters().get(URL) != null) {
      try {
        return URI.create(callback.getParameters().get(URL));
      } catch (final IllegalArgumentException e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public boolean hasContinueOnError() {
    return resolve(PreferenceName.CONTINUE_ON_ERROR_PREF, PreferenceName.CONTINUE_ON_ERROR) != null;
  }

  @Override
  public Integer getMaxPageSize() {
    return getNonNegativeIntegerPreference(
        resolve(PreferenceName.MAX_PAGE_SIZE_PREF, PreferenceName.MAX_PAGE_SIZE));
  }

  @Override
  public boolean hasTrackChanges() {
    return resolve(PreferenceName.TRACK_CHANGES_PREF, PreferenceName.TRACK_CHANGES) != null;
  }

  @Override
  public Return getReturn() {
    if (preferences.containsKey(PreferenceName.RETURN.getName())) {
      final String value = preferences.get(PreferenceName.RETURN.getName()).getValue();
      if (Return.REPRESENTATION.toString().toLowerCase(Locale.ROOT).equals(value)) {
        return Return.REPRESENTATION;
      } else if (Return.MINIMAL.toString().toLowerCase(Locale.ROOT).equals(value)) {
        return Return.MINIMAL;
      }
    }
    return null;
  }

  @Override
  public boolean hasRespondAsync() {
    return preferences.containsKey(PreferenceName.RESPOND_ASYNC.getName());
  }

  @Override
  public OmitValues getOmitValues() {
    if (preferences.containsKey(PreferenceName.OMIT_VALUES.getName())) {
      final String value = preferences.get(PreferenceName.OMIT_VALUES.getName()).getValue();
      if (value != null) {
        if (OmitValues.NULLS.toString().equalsIgnoreCase(value)) {
          return OmitValues.NULLS;
        } else if (OmitValues.DEFAULTS.toString().equalsIgnoreCase(value)) {
          return OmitValues.DEFAULTS;
        }
      }
    }
    return null;
  }

  @Override
  public Integer getWait() {
    return getNonNegativeIntegerPreference(preferences.get(PreferenceName.WAIT.getName()));
  }

  /**
   * Resolves a preference that has both an OData 4.01 spelling and the OData 4.0
   * "odata."-prefixed one ([OData-Protocol] 8.2.8, required by 13.2.1 item 4).
   * Where the spec states a tie-break for a preference sent in both spellings
   * (8.2.8.2 callback, 8.2.8.4 include-annotations) the unprefixed 4.01 name wins;
   * that rule is applied uniformly here.
   * @param bare the OData 4.01 name
   * @param prefixed the OData 4.0 "odata."-prefixed name
   * @return the preference found, or <code>null</code> if neither spelling was sent
   */
  private Preference resolve(final PreferenceName bare, final PreferenceName prefixed) {
    final Preference preference = preferences.get(bare.getName());
    return preference == null ? preferences.get(prefixed.getName()) : preference;
  }

  private Integer getNonNegativeIntegerPreference(final Preference preference) {
    if (preference != null && preference.getValue() != null) {
      try {
        final int result = Integer.parseInt(preference.getValue());
        return result < 0 ? null : result;
      } catch (final NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
}
