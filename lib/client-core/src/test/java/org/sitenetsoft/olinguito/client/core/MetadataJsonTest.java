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
package org.sitenetsoft.olinguito.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * The CSDL JSON half of {@link MetadataTest}: the same demo model, read from its CSDL JSON
 * representation, has to produce the object graph the CSDL XML reader produces.
 */
class MetadataJsonTest extends AbstractTest {

  private static final String NS = "ODataDemo";

  private XMLMetadata json() {
    return client.getDeserializer(ContentType.APPLICATION_JSON)
        .toJSONMetadata(getClass().getResourceAsStream("demo-metadata.json"));
  }

  private XMLMetadata xml() {
    return client.getDeserializer(ContentType.APPLICATION_XML)
        .toMetadata(getClass().getResourceAsStream("demo-metadata.xml"));
  }

  /** The JSON reader produces the same XMLMetadata graph shape the XML reader produces. */
  @Test
  void jsonAndXmlProduceTheSameSchema() {
    final CsdlSchema fromJson = json().getSchema(NS);
    final CsdlSchema fromXml = xml().getSchema(NS);
    assertNotNull(fromJson);
    assertEquals(fromXml.getNamespace(), fromJson.getNamespace());
    assertEquals(fromXml.getEntityTypes().size(), fromJson.getEntityTypes().size());
    assertEquals(fromXml.getComplexTypes().size(), fromJson.getComplexTypes().size());
    assertEquals(fromXml.getEntityContainer().getEntitySets().size(),
        fromJson.getEntityContainer().getEntitySets().size());
    assertEquals(fromXml.getAnnotationGroups().size(), fromJson.getAnnotationGroups().size());
  }

  /** The Edm built from the JSON document is equivalent to the one built from the XML document. */
  @Test
  void edmFromJsonMatchesEdmFromXml() {
    final Edm fromJson = client.getReader().readMetadata(json().getSchemaByNsOrAlias());
    final Edm fromXml = client.getReader().readMetadata(xml().getSchemaByNsOrAlias());

    final FullQualifiedName product = new FullQualifiedName(NS, "Product");
    assertEquals(fromXml.getEntityType(product).getPropertyNames(),
        fromJson.getEntityType(product).getPropertyNames());
    assertEquals(fromXml.getEntityType(product).getNavigationPropertyNames(),
        fromJson.getEntityType(product).getNavigationPropertyNames());
    for (String name : fromXml.getEntityType(product).getPropertyNames()) {
      assertEquals(fromXml.getEntityType(product).getStructuralProperty(name).isNullable(),
          fromJson.getEntityType(product).getStructuralProperty(name).isNullable(), name);
      assertEquals(fromXml.getEntityType(product).getStructuralProperty(name).getType(),
          fromJson.getEntityType(product).getStructuralProperty(name).getType(), name);
    }
    assertTrue(fromJson.getEntityType(new FullQualifiedName(NS, "Category")).isOpenType());
    assertTrue(fromJson.getEntityType(new FullQualifiedName(NS, "Advertisement")).hasStream());
    assertEquals(fromXml.getEntityContainer().getEntitySet("Products").getEntityType().getName(),
        fromJson.getEntityContainer().getEntitySet("Products").getEntityType().getName());
  }

  /** Section 5.2 external targeting survives, including the annotation values MetadataTest#demo pins. */
  @Test
  void externalTargetingAnnotations() {
    final CsdlAnnotations annots = json().getSchema(NS)
        .getAnnotationGroup("ODataDemo.DemoService/Suppliers", null);
    assertNotNull(annots);
    assertEquals("http://www.odata.org/",
        annots.getAnnotation("Org.OData.Publication.V1.PrivacyPolicyUrl").getExpression()
            .asConstant().getValue());
  }

  @Test
  void malformedJsonMetadataIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> client.getDeserializer(ContentType.APPLICATION_JSON)
        .toJSONMetadata(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8))));
  }
}
