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
 * Copyright 2026 SiteNetSoft - Added the CSDL JSON metadata parser tests
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDeleteAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataJsonParserTest {

  private static final FullQualifiedName CATEGORY = new FullQualifiedName("ns", "Category");
  private static final FullQualifiedName PRODUCT = new FullQualifiedName("ns", "Product");

  private SchemaBasedEdmProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("csdl-json-conformant.json")) {
      assertNotNull(in);
      provider = new MetadataJsonParser()
          .buildEdmProvider(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
  }

  @Test
  void schemaAliasAndNamespace() throws Exception {
    assertEquals(1, provider.getSchemas().size());
    assertEquals("ns", provider.getSchemas().get(0).getNamespace());
    assertEquals("self", provider.getSchemas().get(0).getAlias());
  }

  @Test
  void referenceWithIncludeAndIncludeAnnotations() {
    assertEquals(1, provider.getReferences().size());
    final EdmxReference reference = provider.getReferences().get(0);
    assertEquals("http://localhost/vocabularies/Org.OData.Core.V1.xml", reference.getUri().toASCIIString());
    assertEquals("Org.OData.Core.V1", reference.getIncludes().get(0).getNamespace());
    assertEquals("Core", reference.getIncludes().get(0).getAlias());
    assertEquals("Tablet", reference.getIncludeAnnotations().get(0).getQualifier());
    assertEquals("ns", reference.getIncludeAnnotations().get(0).getTargetNamespace());
  }

  /**
   * CSDL JSON section 7.2.1: absence of $Nullable means false. The XML attribute defaults to true, so
   * this is the one default a JSON parser must not borrow from the XML parser.
   */
  @Test
  void nullableDefaultsToFalse() throws Exception {
    final CsdlEntityType category = provider.getEntityType(CATEGORY);
    assertFalse(category.getProperty("Code").isNullable(), "absent $Nullable means false");
    assertTrue(category.getProperty("Name").isNullable());
    assertFalse(category.getNavigationProperty("Products").isNullable());
    assertTrue(provider.getEntityType(PRODUCT).getNavigationProperty("Category").isNullable());
  }

  /**
   * Section 7.1: "Absence of the $Type member means the type is Edm.String. This member SHOULD be
   * omitted for string properties to reduce document size." This is the reading half of the omission
   * Task 2 implements in the writer, for both the single-valued and the collection-valued case.
   */
  @Test
  void absentTypeMeansEdmString() throws Exception {
    assertEquals("Edm.String", provider.getEntityType(CATEGORY).getProperty("Name").getType());
    assertEquals("Edm.String", provider.getEntityType(CATEGORY).getProperty("Tags").getType());
    assertTrue(provider.getEntityType(CATEGORY).getProperty("Tags").isCollection());
  }

  /** Section 6.5: a key property reached through a complex property is an object with its alias. */
  @Test
  void keyWithAndWithoutAlias() throws Exception {
    final List<CsdlPropertyRef> key = provider.getEntityType(CATEGORY).getKey();
    assertEquals(2, key.size());
    assertEquals("CategoryInfoID", key.get(0).getAlias());
    assertEquals("Info/ID", key.get(0).getName());
    assertNull(key.get(1).getAlias());
    assertEquals("Code", key.get(1).getName());
  }

  @Test
  void entityTypeFlagsAndFacets() throws Exception {
    assertTrue(provider.getEntityType(PRODUCT).hasStream());
    assertFalse(provider.getEntityType(CATEGORY).hasStream());
    assertEquals(Integer.valueOf(40), provider.getEntityType(CATEGORY).getProperty("Name").getMaxLength());
    assertTrue(provider.getComplexType(new FullQualifiedName("ns", "Info")).isOpenType());
  }

  @Test
  void enumTypeAndTypeDefinition() throws Exception {
    final CsdlEnumType colour = provider.getEnumType(new FullQualifiedName("ns", "Colour"));
    assertTrue(colour.isFlags());
    assertEquals("Edm.Int16", colour.getUnderlyingType());
    assertEquals("1", colour.getMember("Red").getValue());
    assertEquals("2", colour.getMember("Blue").getValue());

    final CsdlTypeDefinition weight = provider.getTypeDefinition(new FullQualifiedName("ns", "Weight"));
    assertEquals("Edm.Decimal", weight.getUnderlyingType());
    assertEquals(Integer.valueOf(7), weight.getPrecision());
    assertEquals(Integer.valueOf(3), weight.getScale());
  }

  @Test
  void navigationPropertyPartnerConstraintAndOnDelete() throws Exception {
    final CsdlNavigationProperty products = provider.getEntityType(CATEGORY).getNavigationProperty("Products");
    assertTrue(products.isCollection());
    assertEquals("ns.Product", products.getType());
    assertEquals("Category", products.getPartner());
    assertEquals(CsdlOnDeleteAction.Cascade, products.getOnDelete().getAction());

    final CsdlNavigationProperty category = provider.getEntityType(PRODUCT).getNavigationProperty("Category");
    assertEquals(1, category.getReferentialConstraints().size());
    assertEquals("CategoryCode", category.getReferentialConstraints().get(0).getProperty());
    assertEquals("Code", category.getReferentialConstraints().get(0).getReferencedProperty());
  }

  /** A navigation property MUST carry $Type (section 8.1) - there is no Edm.String fallback. */
  @Test
  void navigationPropertyWithoutTypeIsRejected() {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"Nav\":{\"$Kind\":\"NavigationProperty\"}}}}";
    final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader(csdl)));
    assertEquals("ns/ET/Nav/$Type", thrown.getJsonPath());
  }

  @Test
  void unknownVersionIsRejected() {
    final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader("{\"$Version\":\"3.0\"}")));
    assertEquals("$Version", thrown.getJsonPath());
  }

  @Test
  void missingVersionIsRejected() {
    assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader("{\"ns\":{}}")));
  }

  @Test
  void bothVersionsAreAccepted() throws Exception {
    assertNotNull(new MetadataJsonParser().buildEdmProvider(new StringReader("{\"$Version\":\"4.0\"}")));
    assertNotNull(new MetadataJsonParser().buildEdmProvider(new StringReader("{\"$Version\":\"4.01\"}")));
  }
}
