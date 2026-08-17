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
 * Copyright 2026 SiteNetSoft - OData 4.01: key-as-segment URL convention service flag
 */
package org.sitenetsoft.olinguito.server.api;

import org.sitenetsoft.olinguito.server.api.processor.Processor;

/**
 * <p>Handles requests as OData requests.</p>
 *
 * <p>This includes URI parsing, content negotiation, dispatching the request
 * to a specific custom processor implementation for handling data and
 * creating the serialized content for the response object.</p>
 */
public interface ODataHandler {

  /**
   * <p>Processes an OData request.</p>
   * <p>This includes URI parsing, content negotiation, dispatching the request
   * to a specific custom processor implementation for handling data and
   * creating the serialized content for the response object.</p>
   * @param request the OData request
   * @return OData response
   */
  ODataResponse process(final ODataRequest request);

  /**
   * <p>Registers additional custom processor implementations for handling OData requests.</p>
   * <p>If request processing requires a processor that is not registered then a
   * "not implemented" exception will happen.</p>
   */
  void register(Processor processor);

  /**
   * <p>Registers additional extensions for handling OData requests.</p>
   * <p>This method is used for registration of all possible extensions
   * and provide the extensibility for further extensions and
   * different ODataHandler implementations/extensions.</p>
   */
  void register(OlingoExtension extension);

  /**
   * Enables or disables the OData 4.01 key-as-segment URL convention (URL Conventions section 4.3.6)
   * for every entity collection served by this handler. Off by default (support is a MAY).
   * When enabled, a segment following an entity collection is resolved with the specification's
   * precedence rules: a {@code $}-segment, a qualified bound operation or type name, an unqualified
   * bound operation or type name in a default namespace, else a key value; multi-part keys use one
   * segment per key value in metadata key order. The canonical parenthesized form stays supported.
   * Individual entity sets and navigation properties may additionally opt in through the
   * (non-standard) {@code keyAsSegmentAllowed} model flag regardless of this setting.
   * @param enabled whether the convention is enabled service-wide
   */
  default void setKeyAsSegment(final boolean enabled) {
    throw new UnsupportedOperationException("key-as-segment configuration is not supported by this handler");
  }
}
