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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 2: additive schema-version source
 * (OData 4.01, Part 1: Protocol, section 11.2.12; Core.SchemaVersion)
 */
package org.sitenetsoft.olinguito.server.api;

import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.server.api.etag.ServiceMetadataETagSupport;

/**
 * Metadata of an OData service like the Entity Data Model.
 */
public interface ServiceMetadata {
  /**
   * Gets the entity data model.
   * @return entity data model of this service
   */
  Edm getEdm();

  /**
   * Get the data-service version.
   * @return data service version of this service
   */
  ODataServiceVersion getDataServiceVersion();

  /**
   * Gets the list of references defined for this service.
   * @return list of defined emdx references of this service
   */
  List<EdmxReference> getReferences();

  /**
   * Gets the helper for ETag support of the metadata document (may be NULL).
   * @return metadata ETag support
   */
  ServiceMetadataETagSupport getServiceMetadataETagSupport();

  /**
   * Gets the schema version this service's data model conforms to, as defined by the
   * <code>Core.SchemaVersion</code> vocabulary term (OData 4.01, Part 1: Protocol, section 11.2.12,
   * the <code>$schemaversion</code> system query option). A service that does not publish a schema
   * version returns <code>null</code> (the default); in that case <code>$schemaversion</code> is
   * accepted on any request but has no validating effect (no version source to check against).
   * @return the schema version string, or <code>null</code> if this service is unversioned
   */
  default String getSchemaVersion() {
    return null;
  }
}
