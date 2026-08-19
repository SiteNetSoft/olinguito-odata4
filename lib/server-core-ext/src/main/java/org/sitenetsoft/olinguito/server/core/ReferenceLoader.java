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
 * Copyright 2026 SiteNetSoft - Extracted the shared CSDL reference/vocabulary loader
 */
package org.sitenetsoft.olinguito.server.core;

import java.io.InputStream;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceInclude;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;

/**
 * Loads the referenced and core-vocabulary schemas of a CSDL document. This logic is shared by every
 * CSDL parser (XML and JSON), which supply their own document parsing through {@link ProviderFactory}.
 */
class ReferenceLoader {

  /** Builds a provider from a referenced CSDL document, in the format of the calling parser. */
  interface ProviderFactory {
    SchemaBasedEdmProvider build(InputStream csdl, ReferenceResolver resolver, boolean loadCore,
        boolean useLocal, boolean loadReferenceSchemas, String namespace) throws ODataException;
  }

  private final Map<String, SchemaBasedEdmProvider> globalReferenceMap;
  private final ProviderFactory factory;

  ReferenceLoader(final Map<String, SchemaBasedEdmProvider> globalReferenceMap, final ProviderFactory factory) {
    this.globalReferenceMap = globalReferenceMap;
    this.factory = factory;
  }

  /** Loads the three vocabularies that are implicitly available even when not referenced. */
  void loadCoreVocabularies(SchemaBasedEdmProvider provider) throws ODataException {
    loadCoreVocabulary(provider, "Org.OData.Core.V1");
    loadCoreVocabulary(provider, "Org.OData.Capabilities.V1");
    loadCoreVocabulary(provider, "Org.OData.Measures.V1");
  }

  void loadCoreVocabulary(SchemaBasedEdmProvider provider, String namespace) throws ODataException {
    if ("Org.OData.Core.V1".equalsIgnoreCase(namespace)) {
      loadLocalVocabularySchema(provider, "Org.OData.Core.V1", "Org.OData.Core.V1.xml");
    } else if ("Org.OData.Capabilities.V1".equalsIgnoreCase(namespace)) {
      loadLocalVocabularySchema(provider, "Org.OData.Capabilities.V1", "Org.OData.Capabilities.V1.xml");
    } else if ("Org.OData.Measures.V1".equalsIgnoreCase(namespace)) {
      loadLocalVocabularySchema(provider, "Org.OData.Measures.V1", "Org.OData.Measures.V1.xml");
    } else {
      throw new ODataException("Unknown namespace to load vocabulary");
    }
  }

  /** Records a provider under its namespace so sibling documents reuse it instead of re-fetching it. */
  void rememberProvider(String namespace, SchemaBasedEdmProvider provider) {
    if (namespace != null && !namespace.isEmpty() && !this.globalReferenceMap.containsKey(namespace)) {
      this.globalReferenceMap.put(namespace, provider);
    }
  }

  void loadReferenceSchemas(SchemaBasedEdmProvider provider, String base, ReferenceResolver resolver,
      boolean loadCore, boolean useLocal, boolean recursivelyLoadReferences) throws ODataException {

    for (EdmxReference reference : provider.getReferences()) {
      try {
        SchemaBasedEdmProvider refProvider = null;

        for (EdmxReferenceInclude include : reference.getIncludes()) {

          // check if the schema is already loaded before in current provider.
          if (provider.getSchemaDirectly(include.getNamespace()) != null) {
            continue;
          }

          if (isCoreVocabulary(include.getNamespace()) && useLocal) {
            loadCoreVocabulary(provider, include.getNamespace());
            continue;
          }

          // check if the schema is already loaded before in parent providers
          refProvider = this.globalReferenceMap.get(include.getNamespace());

          if (refProvider == null) {
            InputStream is = resolver.resolveReference(reference.getUri(), base);
            if (is == null) {
              throw new EdmException("Failed to load Reference " + reference.getUri() + " loading failed");
            } else {
              // do not implicitly load core vocabularies any more. But if the
              // references loading the core vocabularies try to use local if we can
              refProvider = this.factory.build(is, resolver, false, useLocal,
                  recursivelyLoadReferences, include.getNamespace());
            }
          }

          if (refProvider != null) {
            CsdlSchema refSchema = refProvider.getSchema(include.getNamespace(), false);
            provider.addReferenceSchema(include.getNamespace(), refProvider);
            if (include.getAlias() != null) {
              refSchema.setAlias(include.getAlias());
              provider.addReferenceSchema(include.getAlias(), refProvider);
            }
          }
        }
      } catch (ODataException e) {
        throw new EdmException("Failed to load Reference " + reference.getUri() + " parsing failed");
      }
    }
  }

  static boolean isCoreVocabulary(String namespace) {
    return "Org.OData.Core.V1".equalsIgnoreCase(namespace)
        || "Org.OData.Capabilities.V1".equalsIgnoreCase(namespace)
        || "Org.OData.Measures.V1".equalsIgnoreCase(namespace);
  }

  static String fixBase(String base) {
    if (base.endsWith("/")) {
      return base;
    }
    return base + "/";
  }

  private void loadLocalVocabularySchema(SchemaBasedEdmProvider provider, String namespace, String resource)
      throws ODataException {
    CsdlSchema schema = provider.getVocabularySchema(namespace);
    if (schema == null) {
      InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource);
      if (is != null) {
        SchemaBasedEdmProvider childProvider = this.factory.build(is, null, false, false, true, "");
        provider.addVocabularySchema(namespace, childProvider);
      } else {
        throw new ODataException("failed to load " + resource + " core vocabulary");
      }
    }
  }
}
