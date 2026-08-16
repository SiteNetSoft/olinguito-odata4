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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 2: additive schema-version-carrying constructor
 */
package org.sitenetsoft.olinguito.server.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.core.edm.EdmProviderImpl;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.etag.ServiceMetadataETagSupport;

/**
 */
public class ServiceMetadataImpl implements ServiceMetadata {

  private final Edm edm;
  private final List<EdmxReference> references;
  private final ServiceMetadataETagSupport serviceMetadataETagSupport;
  private final String schemaVersion;

  public ServiceMetadataImpl(final CsdlEdmProvider edmProvider, final List<EdmxReference> references,
      final ServiceMetadataETagSupport serviceMetadataETagSupport) {
    this(edmProvider, references, serviceMetadataETagSupport, null);
  }

  /**
   * Creates service metadata that additionally carries a schema version, for
   * {@link ServiceMetadata#getSchemaVersion()} / the <code>$schemaversion</code> system query option
   * (OData 4.01, Part 1: Protocol, section 11.2.12). Not exposed via {@link org.sitenetsoft.olinguito
   * .server.api.OData#createServiceMetadata}: callers that want a versioned {@code ServiceMetadata}
   * construct this class directly.
   *
   * @param edmProvider a custom or default implementation for creating metadata
   * @param references list of edmx references
   * @param serviceMetadataETagSupport ETag support for the metadata document (may be {@code null})
   * @param schemaVersion the schema version this service's data model conforms to, or {@code null}
   * if this service is unversioned
   */
  public ServiceMetadataImpl(final CsdlEdmProvider edmProvider, final List<EdmxReference> references,
      final ServiceMetadataETagSupport serviceMetadataETagSupport, final String schemaVersion) {
    edm = new EdmProviderImpl(edmProvider);
    this.references = new ArrayList<>();
    this.references.addAll(references);
    this.serviceMetadataETagSupport = serviceMetadataETagSupport;
    this.schemaVersion = schemaVersion;
  }

  @Override
  public Edm getEdm() {
    return edm;
  }

  @Override
  public ODataServiceVersion getDataServiceVersion() {
    return ODataServiceVersion.V40;
  }

  @Override
  public List<EdmxReference> getReferences() {
    return Collections.unmodifiableList(references);
  }

  @Override
  public ServiceMetadataETagSupport getServiceMetadataETagSupport() {
    return serviceMetadataETagSupport;
  }

  @Override
  public String getSchemaVersion() {
    return schemaVersion;
  }
}
