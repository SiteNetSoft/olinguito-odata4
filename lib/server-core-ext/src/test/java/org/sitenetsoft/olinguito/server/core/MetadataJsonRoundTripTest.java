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
 * Copyright 2026 SiteNetSoft - Added the CSDL JSON writer/parser round trip
 * Copyright 2026 SiteNetSoft - Asserted the fixture's annotations survive the round trip
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The CSDL XML to CSDL JSON to model round trip. The JSON fixture is generated from the very model the
 * CSDL XML parser produced, so the corrected writer and the new reader are pinned against each other
 * and no hand-maintained JSON twin of trippin.xml can drift out of date.
 */
class MetadataJsonRoundTripTest {

  private static final String NS = "Microsoft.OData.SampleService.Models.TripPin";

  private static SchemaBasedEdmProvider fromXml;
  private static SchemaBasedEdmProvider fromJson;

  @BeforeAll
  static void parseBothRepresentations() throws Exception {
    try (InputStream in = MetadataJsonRoundTripTest.class.getClassLoader().getResourceAsStream("trippin.xml")) {
      assertNotNull(in);
      fromXml = new MetadataParser().parseAnnotations(true).implicitlyLoadCoreVocabularies(true)
          .buildEdmProvider(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    fromJson = new MetadataJsonParser().parseAnnotations(true).buildEdmProvider(new StringReader(write(fromXml)));
  }

  /** Serializes a provider with the production CSDL JSON writer. */
  private static String write(final SchemaBasedEdmProvider provider) throws Exception {
    final ServiceMetadata metadata = new ServiceMetadataImpl(provider, provider.getReferences(), null);
    return new String(OData.newInstance().createSerializer(ContentType.APPLICATION_JSON)
        .metadataDocument(metadata).getContent().readAllBytes(), StandardCharsets.UTF_8);
  }

  @Test
  void entityTypesSurviveTheRoundTrip() throws Exception {
    final CsdlEntityType xml = fromXml.getEntityType(new FullQualifiedName(NS, "Person"));
    final CsdlEntityType json = fromJson.getEntityType(new FullQualifiedName(NS, "Person"));
    assertEquals(xml.getName(), json.getName());
    assertEquals(xml.getKey().size(), json.getKey().size());
    assertEquals(xml.getProperties().size(), json.getProperties().size());
    for (CsdlProperty property : xml.getProperties()) {
      final CsdlProperty other = json.getProperty(property.getName());
      assertNotNull(other, property.getName());
      assertEquals(property.getType(), other.getType(), property.getName());
      assertEquals(property.isNullable(), other.isNullable(), property.getName() + " nullable");
      assertEquals(property.isCollection(), other.isCollection(), property.getName());
      assertEquals(property.getMaxLength(), other.getMaxLength(), property.getName());
    }
    for (CsdlNavigationProperty nav : xml.getNavigationProperties()) {
      final CsdlNavigationProperty other = json.getNavigationProperty(nav.getName());
      assertNotNull(other, nav.getName());
      assertEquals(nav.getType(), other.getType(), nav.getName());
      assertEquals(nav.isCollection(), other.isCollection(), nav.getName());
      if (!nav.isCollection()) {
        // Section 8.2 forbids $Nullable on a collection-valued navigation property, so the writer
        // omits it and the CSDL JSON default (false) is what comes back; the CSDL XML default is
        // true. A format difference, pinned here rather than papered over in either parser.
        assertEquals(nav.isNullable(), other.isNullable(), nav.getName() + " nullable");
      }
    }
  }

  @Test
  void enumsTypeDefinitionsAndOperationsSurviveTheRoundTrip() throws Exception {
    assertEquals(fromXml.getEnumType(new FullQualifiedName(NS, "PersonGender")).getMembers().size(),
        fromJson.getEnumType(new FullQualifiedName(NS, "PersonGender")).getMembers().size());
    assertEquals("0", fromJson.getEnumType(new FullQualifiedName(NS, "PersonGender"))
        .getMember("Male").getValue());
    assertEquals(fromXml.getActions(new FullQualifiedName(NS, "ResetDataSource")).size(),
        fromJson.getActions(new FullQualifiedName(NS, "ResetDataSource")).size());
    assertEquals(fromXml.getFunctions(new FullQualifiedName(NS, "GetNearestAirport")).size(),
        fromJson.getFunctions(new FullQualifiedName(NS, "GetNearestAirport")).size());
  }

  @Test
  void containerSurvivesTheRoundTrip() throws Exception {
    final CsdlEntityContainer xml = fromXml.getEntityContainer();
    final CsdlEntityContainer json = fromJson.getEntityContainer();
    assertEquals(xml.getName(), json.getName());
    assertEquals(xml.getEntitySets().size(), json.getEntitySets().size());
    assertEquals(xml.getSingletons().size(), json.getSingletons().size());
    assertEquals(xml.getActionImports().size(), json.getActionImports().size());
    assertEquals(xml.getFunctionImports().size(), json.getFunctionImports().size());
    for (CsdlEntitySet set : xml.getEntitySets()) {
      assertEquals(set.getType(), json.getEntitySet(set.getName()).getType(), set.getName());
      assertEquals(set.getNavigationPropertyBindings().size(),
          json.getEntitySet(set.getName()).getNavigationPropertyBindings().size(), set.getName());
    }
  }

  /**
   * The annotations of the fixture itself survive writer and reader. trippin.xml carries 37 of them -
   * EnumMember and Bool constants on properties, Records and Collections on entity sets - and this is
   * what exercises the whole chain end to end: the writer alias-qualifies every term name against the
   * implicitly loaded vocabularies, and the reader has to map those aliases back through the document's
   * own $Alias members before the terms compare equal.
   */
  @Test
  void annotationsSurviveTheRoundTrip() throws Exception {
    final CsdlEntityType xml = fromXml.getEntityType(new FullQualifiedName(NS, "Person"));
    final CsdlEntityType json = fromJson.getEntityType(new FullQualifiedName(NS, "Person"));
    assertEquals(terms(xml), terms(json), "Person");
    for (CsdlProperty property : xml.getProperties()) {
      assertEquals(terms(property), terms(json.getProperty(property.getName())), property.getName());
    }
    for (CsdlNavigationProperty nav : xml.getNavigationProperties()) {
      assertEquals(terms(nav), terms(json.getNavigationProperty(nav.getName())), nav.getName());
    }
    assertEquals(List.of("Org.OData.Core.V1.Permissions"),
        terms(json.getProperty("UserName")), "the fixture must carry the annotations being compared");

    final CsdlEntityContainer container = fromXml.getEntityContainer();
    for (CsdlEntitySet set : container.getEntitySets()) {
      assertEquals(terms(set), terms(fromJson.getEntityContainer().getEntitySet(set.getName())),
          set.getName());
    }
  }

  /** The nested shapes - a Collection of paths and a Record of Records - survive with their contents. */
  @Test
  void nestedAnnotationExpressionsSurviveTheRoundTrip() throws Exception {
    final CsdlEntitySet people = fromJson.getEntityContainer().getEntitySet("People");

    final CsdlCollection concurrency = (CsdlCollection) annotation(people,
        "Org.OData.Core.V1.OptimisticConcurrency").getExpression();
    assertEquals(1, concurrency.getItems().size());
    assertEquals("Concurrency", ((CsdlPropertyPath) concurrency.getItems().get(0)).getValue());

    final CsdlRecord restrictions = (CsdlRecord) annotation(people,
        "Org.OData.Capabilities.V1.NavigationRestrictions").getExpression();
    assertEquals(2, restrictions.getPropertyValues().size());
    assertEquals("Navigability", restrictions.getPropertyValues().get(0).getProperty());
    assertEquals("RestrictedProperties", restrictions.getPropertyValues().get(1).getProperty());
    final CsdlCollection restricted =
        (CsdlCollection) restrictions.getPropertyValues().get(1).getValue();
    assertEquals(1, restricted.getItems().size());
    final CsdlRecord entry = (CsdlRecord) restricted.getItems().get(0);
    assertEquals(2, entry.getPropertyValues().size());
    assertEquals("NavigationProperty", entry.getPropertyValues().get(0).getProperty());
  }

  private static CsdlAnnotation annotation(final CsdlAnnotatable annotatable, final String term) {
    for (final CsdlAnnotation annotation : annotatable.getAnnotations()) {
      if (term.equals(annotation.getTerm())) {
        return annotation;
      }
    }
    throw new AssertionError("no annotation for the term " + term);
  }

  /** The annotations of one model element as "term" or "term#qualifier", in document order. */
  private static List<String> terms(final CsdlAnnotatable annotatable) {
    assertNotNull(annotatable);
    final List<String> terms = new ArrayList<>();
    for (final CsdlAnnotation annotation : annotatable.getAnnotations()) {
      terms.add(annotation.getQualifier() == null ? annotation.getTerm()
          : annotation.getTerm() + "#" + annotation.getQualifier());
    }
    return terms;
  }

  /**
   * Constant annotation values survive the round trip exactly. CSDL JSON carries no per-value type
   * marker (section 14.3 renders Binary, Date, Guid and String identically, as JSON strings), so the
   * <em>value</em> is compared exactly while the type tag is only compared for the shapes JSON does
   * distinguish - boolean, integral number, non-integral number, string. This is the format's
   * asymmetry, not a parser defect, and normalizing it here is the only place the plan permits it.
   */
  @Test
  void constantAnnotationValuesSurviveTheRoundTrip() throws Exception {
    final String csdl = "<edmx:Edmx Version=\"4.01\" xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\">"
        + "<edmx:DataServices>"
        + "<Schema Namespace=\"ns\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\">"
        + "<Term Name=\"T\" Type=\"Edm.String\"/>"
        + "<EntityType Name=\"ET\"/>"
        + "<Annotation Term=\"ns.T\" Binary=\"qrvM3e7_\"/>"
        + "<Annotation Term=\"ns.T\" Qualifier=\"Q2\" Date=\"2012-02-29\"/>"
        + "<Annotation Term=\"ns.T\" Qualifier=\"Q3\" Guid=\"aabbccdd-aabb-ccdd-eeff-aabbccddeeff\"/>"
        + "<Annotation Term=\"ns.T\" Qualifier=\"Q4\" Int=\"42\"/>"
        + "<Annotation Term=\"ns.T\" Qualifier=\"Q5\" Float=\"1.42\"/>"
        + "<Annotation Term=\"ns.T\" Qualifier=\"Q6\" Bool=\"true\"/>"
        + "<Annotation Term=\"ns.T\" Qualifier=\"Q7\" String=\"ABCD\"/>"
        + "</Schema></edmx:DataServices></edmx:Edmx>";
    final SchemaBasedEdmProvider xmlProvider =
        new MetadataParser().parseAnnotations(true).buildEdmProvider(new StringReader(csdl));
    final SchemaBasedEdmProvider jsonProvider =
        new MetadataJsonParser().parseAnnotations(true).buildEdmProvider(new StringReader(write(xmlProvider)));

    final List<CsdlAnnotation> before = xmlProvider.getSchemas().get(0).getAnnotations();
    final List<CsdlAnnotation> after = jsonProvider.getSchemas().get(0).getAnnotations();
    assertEquals(7, before.size());
    assertEquals(before.size(), after.size());
    for (int i = 0; i < before.size(); i++) {
      final CsdlConstantExpression source = before.get(i).getExpression().asConstant();
      final CsdlConstantExpression target = after.get(i).getExpression().asConstant();
      assertEquals(before.get(i).getTerm(), after.get(i).getTerm(), "term " + i);
      assertEquals(before.get(i).getQualifier(), after.get(i).getQualifier(), "qualifier " + i);
      assertEquals(source.getValue(), target.getValue(), "value of annotation " + i);
      assertEquals(jsonShape(source.getType()), target.getType(), "shape of annotation " + i);
    }
  }

  /**
   * The ConstantExpressionType a conformant CSDL JSON document can still convey: everything the format
   * writes as a JSON string comes back as String, numbers come back as Int or Float, booleans as Bool.
   */
  private static ConstantExpressionType jsonShape(final ConstantExpressionType type) {
    switch (type) {
    case Bool:
    case Int:
    case Float:
      return type;
    case Decimal:
      return ConstantExpressionType.Float;
    default:
      return ConstantExpressionType.String;
    }
  }

  /**
   * The corrected writer emits a document a conformant reader accepts: $EntityContainer is present and
   * namespace-qualified, $Nullable is only ever written as true, no Extending object survives, no
   * container child carries $Kind, no string property carries $Type, and no CSDL XML element name
   * leaks into a constant expression.
   */
  @Test
  void theWrittenDocumentIsConformant() throws Exception {
    // re-serialize so the assertion is on the exact bytes, not on the parsed model
    final String json = write(fromXml);
    assertTrue(json.contains("\"$EntityContainer\":\"" + NS + ".DefaultContainer\""));
    assertFalse(json.contains("\"$Nullable\":false"));
    assertFalse(json.contains("Extending"));
    assertTrue(json.contains("\"$Collection\":true"));
    assertFalse(json.contains("\"$Kind\":\"EntitySet\""));
    assertFalse(json.contains("\"$Kind\":\"Singleton\""));
    assertFalse(json.contains("\"$Type\":\"Edm.String\",\"$Nullable\""));
    for (String xmlOnlyMember : List.of("$Binary", "$Date", "$Int", "$Guid", "$Float", "$TimeOfDay")) {
      assertFalse(json.contains("\"" + xmlOnlyMember + "\":"), xmlOnlyMember + " is CSDL XML only");
    }
  }
}
