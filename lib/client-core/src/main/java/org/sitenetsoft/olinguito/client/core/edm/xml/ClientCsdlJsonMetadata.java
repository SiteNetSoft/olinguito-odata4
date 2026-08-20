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
 * Copyright 2026 SiteNetSoft - Read CSDL JSON metadata in the client deserializer
 */
package org.sitenetsoft.olinguito.client.core.edm.xml;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.client.api.edm.xml.Reference;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;

/**
 * The {@link XMLMetadata} of a CSDL JSON metadata document, as read by
 * {@link ClientCsdlJsonMetadataParser}. A CSDL JSON document has no {@code edmx:Edmx} wrapper - the
 * document object itself carries {@code $Version}, {@code $Reference} and the schemas - so this holds
 * the three of them directly instead of delegating to an {@code Edmx} the way
 * {@code ClientCsdlXMLMetadata} does. Everything it answers is what the CSDL XML representation of the
 * same model answers.
 */
class ClientCsdlJsonMetadata extends CsdlAbstractEdmItem implements Serializable, XMLMetadata {

  @Serial
  private static final long serialVersionUID = -3110748325950531322L;

  private final List<CsdlSchema> schemas;
  private final List<Reference> references;
  private final String edmVersion;

  ClientCsdlJsonMetadata(final List<CsdlSchema> schemas, final List<Reference> references,
      final String edmVersion) {
    this.schemas = new ArrayList<>(schemas);
    this.references = new ArrayList<>(references);
    this.edmVersion = edmVersion;
  }

  @Override
  public List<CsdlSchema> getSchemas() {
    return this.schemas;
  }

  @Override
  public CsdlSchema getSchema(final int index) {
    return getSchemas().get(index);
  }

  @Override
  public CsdlSchema getSchema(final String key) {
    return getSchemaByNsOrAlias().get(key);
  }

  @Override
  public Map<String, CsdlSchema> getSchemaByNsOrAlias() {
    final Map<String, CsdlSchema> schemaByNsOrAlias = new HashMap<>();
    for (CsdlSchema schema : getSchemas()) {
      schemaByNsOrAlias.put(schema.getNamespace(), schema);
      if (schema.getAlias() != null && !schema.getAlias().isBlank()) {
        schemaByNsOrAlias.put(schema.getAlias(), schema);
      }
    }
    return schemaByNsOrAlias;
  }

  @Override
  public List<Reference> getReferences() {
    return this.references;
  }

  /**
   * Always {@code null}, exactly as {@code ClientCsdlXMLMetadata} answers when it was built without
   * them: the namespace lists it carries are the XML namespace declarations of the {@code Schema}
   * elements, and a CSDL JSON document has no XML namespaces at all.
   */
  @Override
  public List<List<String>> getSchemaNamespaces() {
    return null;
  }

  @Override
  public String getEdmVersion() {
    return this.edmVersion;
  }
}
