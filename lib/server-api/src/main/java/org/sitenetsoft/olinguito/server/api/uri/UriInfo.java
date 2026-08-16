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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 1: $schemaversion system query option
 */
package org.sitenetsoft.olinguito.server.api.uri;

import java.util.List;

import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SchemaVersionOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOption;

/**
 * <p>Object acting as general access to URI information extracted from the request URI.</p>
 * <p>Depending on the URI info kind different interfaces are used to provide access to that information.
 * Use method {@link #getKind()} to obtain URI info kind information and to perform an appropriate cast.</p>
 */
public interface UriInfo extends UriInfoService, UriInfoMetadata, UriInfoResource, UriInfoBatch,
UriInfoAll, UriInfoCrossjoin, UriInfoEntityId {

  /**
   * See {@link UriInfoKind} for more details which kinds are allowed.
   * @return the kind of this URI info object.
   */
  UriInfoKind getKind();

  /**
   * Convenience casting method.
   * @return this as a {@link UriInfoService} object
   */
  UriInfoService asUriInfoService();

  /**
   * Convenience casting method.
   * @return this as a {@link UriInfoAll} object
   */
  UriInfoAll asUriInfoAll();

  /**
   * Convenience casting method.
   * @return this as a {@link UriInfoBatch} object
   */
  UriInfoBatch asUriInfoBatch();

  /**
   * Convenience casting method.
   * @return this as a {@link UriInfoCrossjoin} object
   */
  UriInfoCrossjoin asUriInfoCrossjoin();

  /**
   * Convenience casting method.
   * @return this as a {@link UriInfoEntityId} object
   */
  UriInfoEntityId asUriInfoEntityId();

  /**
   * Convenience casting method.
   * @return this as a {@link UriInfoMetadata} object
   */
  UriInfoMetadata asUriInfoMetadata();

  /**
   * Convenience casting method.
   * @return this as a {@link UriInfoResource} object
   */
  UriInfoResource asUriInfoResource();

  /**
   * Gets a list of all system query options which were in the URI.
   * @return a list of all system query options used
   */
  List<SystemQueryOption> getSystemQueryOptions();

  /**
   * Gets a list of all alias definitions which were in the URI (including aliases not used anywhere).
   * @return a list of all alias definitions
   */
  List<AliasQueryOption> getAliases();

  /**
   * Gets the $schemaversion system query option, if it was used in the request. Per OData 4.01,
   * Part 1: Protocol, section 11.2.12, this option MAY be included in any request; it is therefore
   * declared once here on {@link UriInfo} rather than on each of the seven kind-specific
   * sub-interfaces (the per-kind {@code getFormatOption} pattern), since every consumer of URI
   * information holds a {@link UriInfo} reference regardless of kind.
   * @return the $schemaversion option, or <code>null</code> if it was not used in the URI
   */
  SchemaVersionOption getSchemaVersionOption();
}
