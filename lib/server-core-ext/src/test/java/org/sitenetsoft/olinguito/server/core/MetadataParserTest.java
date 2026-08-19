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
 * Copyright 2026 SiteNetSoft - Fixed resource leaks, replaced wildcard import
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Pinned the 4.01 metadata version gate and the Nullable defaults
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataParserTest {
  final String NS = "Microsoft.OData.SampleService.Models.TripPin";
  final FullQualifiedName NSF = new FullQualifiedName(NS);
  final FullQualifiedName EC = new FullQualifiedName(NS, "DefaultContainer");

  CsdlEdmProvider provider = null;

  ReferenceResolver testReferenceResolver = new ReferenceResolver() {
    @Override
    public InputStream resolveReference(URI uri, String xmlBase) {
      String str = uri.toASCIIString();
      if (str.startsWith("http://localhost/")) {
        return getClass().getClassLoader().getResourceAsStream(str.substring(17));
      }
      return null;
    }
  };

  @BeforeEach
  void setUp() throws Exception {
    MetadataParser parser = new MetadataParser();
    InputStream in = getClass().getClassLoader().getResourceAsStream("trippin.xml");
    assertNotNull(in);
    provider = (CsdlEdmProvider) parser.buildEdmProvider(new InputStreamReader(in, StandardCharsets.UTF_8));
  }

  @Test
  void testAction() throws ODataException {
    // test action
    List<CsdlAction> actions = provider.getActions(new FullQualifiedName(NS, "ResetDataSource"));
    assertNotNull(actions);
    assertEquals(1, actions.size());
  }

  @Test
  void testFunction() throws ODataException {
    // test function
    List<CsdlFunction> functions = provider
        .getFunctions(new FullQualifiedName(NS, "GetFavoriteAirline"));
    assertNotNull(functions);
    assertEquals(1, functions.size());
    assertEquals("GetFavoriteAirline", functions.get(0).getName());
    assertTrue(functions.get(0).isBound());
    assertTrue(functions.get(0).isComposable());
    assertEquals(
        "person/Trips/PlanItems/Microsoft.OData.SampleService.Models.TripPin.Flight/Airline",
        functions.get(0).getEntitySetPath());

    List<CsdlParameter> parameters = functions.get(0).getParameters();
    assertNotNull(parameters);
    assertEquals(1, parameters.size());
    assertEquals("person", parameters.get(0).getName());
    assertEquals("Microsoft.OData.SampleService.Models.TripPin.Person",parameters.get(0).getType());
    assertFalse(parameters.get(0).isNullable());

    assertNotNull(functions.get(0).getReturnType());
    assertEquals("Microsoft.OData.SampleService.Models.TripPin.Airline",
        functions.get(0).getReturnType().getType());
    assertFalse(functions.get(0).getReturnType().isNullable());
  }

  @Test
  void testEnumType() throws ODataException {
    // test enum type
    CsdlEnumType enumType = provider.getEnumType(new FullQualifiedName(NS, "PersonGender"));
    assertNotNull(enumType);
    assertEquals("Male", enumType.getMembers().get(0).getName());
    assertEquals("Female", enumType.getMembers().get(1).getName());
    assertEquals("Unknown", enumType.getMembers().get(2).getName());
    assertEquals("0", enumType.getMembers().get(0).getValue());
    assertEquals("1", enumType.getMembers().get(1).getValue());
    assertEquals("2", enumType.getMembers().get(2).getValue());
  }

  @Test
  void testEntityType() throws ODataException {
    // test Entity Type
    CsdlEntityType et = provider.getEntityType(new FullQualifiedName(NS, "Photo"));
    assertNotNull(et);
    assertNotNull(et.getKey());
    assertEquals("Id", et.getKey().get(0).getName());
    assertTrue(et.hasStream());
    assertEquals("Id", et.getProperties().get(0).getName());
    assertEquals("Edm.Int64", et.getProperties().get(0).getType());
    assertEquals("Name", et.getProperties().get(1).getName());
    assertEquals("Edm.String", et.getProperties().get(1).getType());
  }

  @Test
  void testComplexType() throws ODataException {
    // Test Complex Type
    CsdlComplexType ct = provider.getComplexType(new FullQualifiedName(NS, "City"));
    assertNotNull(ct);
    assertEquals(3, ct.getProperties().size());
    CsdlProperty p = ct.getProperties().get(0);
    assertEquals("CountryRegion", p.getName());
    assertEquals("Edm.String", p.getType());
    assertFalse(p.isNullable());

    ct = provider.getComplexType(new FullQualifiedName(NS, "Location"));
    assertNotNull(ct);

    ct = provider.getComplexType(new FullQualifiedName(NS, "EventLocation"));
    assertNotNull(ct);
  }

  @Test
  void testEntitySet() throws Exception {
    CsdlEntitySet es = provider.getEntitySet(EC, "People");
    assertNotNull(es);
    assertEquals("Microsoft.OData.SampleService.Models.TripPin.Person",es.getType());

    List<CsdlNavigationPropertyBinding> bindings = es.getNavigationPropertyBindings();
    assertNotNull(bindings);
    assertEquals(6, bindings.size());
    assertEquals("Microsoft.OData.SampleService.Models.TripPin.Flight/From", bindings.get(2)
        .getPath());
    assertEquals("Airports", bindings.get(2).getTarget());
  }

  @Test
  void testFunctionImport() throws Exception {
    CsdlFunctionImport fi = provider.getFunctionImport(EC, "GetNearestAirport");
    assertNotNull(fi);
    assertEquals("Microsoft.OData.SampleService.Models.TripPin.GetNearestAirport", fi.getFunction());
    assertEquals("Airports", fi.getEntitySet());
    assertTrue(fi.isIncludeInServiceDocument());
  }

  @Test
  void testActionImport() throws Exception {
    CsdlActionImport ai = provider.getActionImport(EC, "ResetDataSource");
    assertNotNull(ai);
    assertEquals("Microsoft.OData.SampleService.Models.TripPin.ResetDataSource", ai.getAction());
    assertNull(ai.getEntitySet());
  }

  @Test
  void testSingleton() throws Exception {
    CsdlSingleton single = this.provider.getSingleton(EC, "Me");
    assertNotNull(single);

    assertEquals("Microsoft.OData.SampleService.Models.TripPin.Person",single.getType());

    List<CsdlNavigationPropertyBinding> bindings = single.getNavigationPropertyBindings();
    assertNotNull(bindings);
    assertEquals(6, bindings.size());
    assertEquals("Microsoft.OData.SampleService.Models.TripPin.Flight/From", bindings.get(2).getPath());
    assertEquals("Airports", bindings.get(2).getTarget());
  }
  
  @Test
  void testParsingWithNoFormat() throws Exception {
    MetadataParser parser = new MetadataParser();
    try (var reader = new java.io.InputStreamReader(
            java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/test/resources/skip-annotation.xml")),
            StandardCharsets.UTF_8)) {
      provider = (CsdlEdmProvider) parser.buildEdmProvider(reader);
    }
  }

  @Test
  void testReferenceLoad() throws Exception {
    MetadataParser parser = new MetadataParser();
    parser.recursivelyLoadReferences(false);
    parser.referenceResolver(this.testReferenceResolver);
    try (var reader = new java.io.InputStreamReader(
            java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/test/resources/test.xml")),
            StandardCharsets.UTF_8)) {
      provider = (CsdlEdmProvider) parser.buildEdmProvider(reader);
    }
  }

  @Test
  void testReferenceLoadRecursively() throws Exception {
    MetadataParser parser = new MetadataParser();
    parser.recursivelyLoadReferences(true);
    parser.referenceResolver(testReferenceResolver);
    try (var reader = new java.io.InputStreamReader(
            java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/test/resources/test.xml")),
            StandardCharsets.UTF_8)) {
      SchemaBasedEdmProvider providerTest = parser.buildEdmProvider(reader);

      Assertions.assertNotNull(providerTest.getSchema("Microsoft.OData.SampleService.Models.TripPin", false));

      Assertions.assertNull(providerTest.getSchema("org.sitenetsoft.olinguito.a", false));
      Assertions.assertNull(providerTest.getSchema("org.sitenetsoft.olinguito.b", false));

      Assertions.assertNotNull(providerTest.getSchema("org.sitenetsoft.olinguito.a", true));
      Assertions.assertNotNull(providerTest.getSchema("org.sitenetsoft.olinguito.b", true));
    }
  }

  @Test
  void testCircleReferenceShouldNotStackOverflow() throws Exception {
    MetadataParser parser = new MetadataParser();
    parser.recursivelyLoadReferences(true);
    parser.referenceResolver(testReferenceResolver);
    try (var reader = new java.io.InputStreamReader(
            java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/test/resources/test.xml")),
            StandardCharsets.UTF_8)) {
      SchemaBasedEdmProvider providerTest = parser.buildEdmProvider(reader);

      Assertions.assertNull(providerTest.getSchema("Not Found", true));
    }
  }

  @Test
  void testLoadCoreVocabulary() throws Exception {
    MetadataParser parser = new MetadataParser();
    parser.implicitlyLoadCoreVocabularies(true);
    parser.referenceResolver(testReferenceResolver);
    try (var reader = new java.io.InputStreamReader(
            java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/test/resources/test.xml")),
            StandardCharsets.UTF_8)) {
      SchemaBasedEdmProvider provider = parser.buildEdmProvider(reader);

      Assertions.assertNotNull(provider.getVocabularySchema("Org.OData.Core.V1"));
      Assertions.assertNotNull(provider.getSchema("Org.OData.Core.V1"));
    }
  }

  private static final String MINIMAL_CSDL_PREFIX =
      "<edmx:Edmx Version=\"%s\" xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\">"
      + "<edmx:DataServices>"
      + "<Schema Namespace=\"ns\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\">"
      + "<EntityType Name=\"ET\"><Key><PropertyRef Name=\"ID\"/></Key>"
      + "<Property Name=\"ID\" Type=\"Edm.Int32\" Nullable=\"false\"/>"
      + "<NavigationProperty Name=\"Nav\" Type=\"ns.ET\"/></EntityType>"
      + "<Function Name=\"F\"><ReturnType Type=\"Edm.String\"/></Function>"
      + "<Action Name=\"A\"><Parameter Name=\"P\" Type=\"Edm.String\"/></Action>"
      + "<Term Name=\"T\" Type=\"Edm.String\"/>"
      + "</Schema></edmx:DataServices></edmx:Edmx>";

  private SchemaBasedEdmProvider parse(final String version) throws Exception {
    return new MetadataParser().buildEdmProvider(new StringReader(String.format(MINIMAL_CSDL_PREFIX, version)));
  }

  /** A 4.01 metadata document is not an error in a 4.01 library. */
  @Test
  void version401IsAccepted() throws Exception {
    assertNotNull(parse("4.01").getEntityType(new FullQualifiedName("ns", "ET")));
  }

  /** 4.0 keeps working exactly as before - this is the closed pin for the widened gate. */
  @Test
  void version40IsStillAccepted() throws Exception {
    assertNotNull(parse("4.0").getEntityType(new FullQualifiedName("ns", "ET")));
  }

  /** Anything else is still rejected with the existing error taxonomy. */
  @Test
  void unknownVersionIsStillRejected() {
    final XMLStreamException thrown = Assertions.assertThrows(XMLStreamException.class, () -> parse("3.0"));
    Assertions.assertTrue(thrown.getMessage().contains("supported"));
  }

  /**
   * CSDL XML defaults the Nullable attribute of a return type, a parameter and a term to true, and
   * the Csdl* model classes agree (CsdlReturnType/CsdlParameter/CsdlTerm all initialise
   * nullable = true). The parser used to overwrite that default with false whenever the attribute was
   * absent - the PR#11 defect, which the navigation-property and property readers already avoid.
   */
  @Test
  void absentNullableDefaultsToTrue() throws Exception {
    final SchemaBasedEdmProvider parsed = parse("4.0");
    assertTrue(parsed.getFunctions(new FullQualifiedName("ns", "F")).get(0).getReturnType().isNullable());
    assertTrue(parsed.getActions(new FullQualifiedName("ns", "A")).get(0).getParameters().get(0).isNullable());
    assertTrue(parsed.getTerm(new FullQualifiedName("ns", "T")).isNullable());
    assertTrue(parsed.getEntityType(new FullQualifiedName("ns", "ET"))
        .getNavigationProperties().get(0).isNullable());
    assertFalse(parsed.getEntityType(new FullQualifiedName("ns", "ET"))
        .getProperties().get(0).isNullable());
  }
}
