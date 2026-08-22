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
 * Copyright 2026 SiteNetSoft - Added OMIT_VALUES for the OData 4.01 omit-values preference
 * Copyright 2026 SiteNetSoft - Tier 7 Wave 2: added the unprefixed OData 4.01 preference names
 */
package org.sitenetsoft.olinguito.commons.api.format;

/**
 * Names of preferences defined in the OData standard.
 */
public enum PreferenceName {

  // Each OData preference that was renamed in 4.01 has both spellings: the constant without the
  // _PREF suffix is the OData 4.0 "odata."-prefixed name, the one with it is the 4.01 name.
  // [OData-Protocol] 8.2.8 keeps the prefixed form for 4.0 clients; 13.2.1 item 4 requires both.
  ALLOW_ENTITY_REFERENCES("odata.allow-entityreferences"),
  ALLOW_ENTITY_REFERENCES_PREF("allow-entityreferences"),
  CALLBACK("odata.callback"),
  CALLBACK_PREF("callback"),
  CONTINUE_ON_ERROR("odata.continue-on-error"),
  CONTINUE_ON_ERROR_PREF("continue-on-error"),
  INCLUDE_ANNOTATIONS("odata.include-annotations"),
  INCLUDE_ANNOTATIONS_PREF("include-annotations"),
  MAX_PAGE_SIZE("odata.maxpagesize"),
  MAX_PAGE_SIZE_PREF("maxpagesize"),
  OMIT_VALUES("omit-values"),
  TRACK_CHANGES("odata.track-changes"),
  TRACK_CHANGES_PREF("track-changes"),
  RETURN("return"),
  RESPOND_ASYNC("respond-async"),
  WAIT("wait"),
  RETURN_CONTENT("return-content"),
  RETURN_NO_CONTENT("return-no-content"),
  KEY_AS_SEGMENT("KeyAsSegment");

  private final String preferenceName;

  PreferenceName(final String preferenceName) {
    this.preferenceName = preferenceName;
  }

  public String getName() {
    return preferenceName;
  }

  @Override
  public String toString() {
    return getName();
  }
}
