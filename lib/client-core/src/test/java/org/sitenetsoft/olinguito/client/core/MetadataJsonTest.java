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
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: pinned the collection navigation property $Nullable
 */
package org.sitenetsoft.olinguito.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.client.api.edm.xml.Reference;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDeleteAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlAnnotationPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlApply;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCast;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElementReference;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNull;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlUrlRef;
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
  // ------------------------------------------------------------------ the aliased fixture: helpers

  private static final String ALIASED_NS = "ns";

  private XMLMetadata aliased() {
    return parse(getClass().getResourceAsStream("demo-metadata-aliased.json"));
  }

  private CsdlSchema aliasedSchema() {
    return aliased().getSchema(ALIASED_NS);
  }

  private static XMLMetadata parse(final InputStream input) {
    return client.getDeserializer(ContentType.APPLICATION_JSON).toJSONMetadata(input);
  }

  private static XMLMetadata parse(final String document) {
    return parse(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)));
  }

  // ---------------------------------------------------------------- sections 4.2 and 5.1: aliases

  /**
   * Sections 4.2 and 5.1: once a schema declares an alias, "the alias MUST be used instead of the
   * namespace within qualified names throughout the document". Every alias-qualified name of the
   * fixture has to arrive at the Edm as the model element it names.
   */
  @Test
  void aliasQualifiedNamesResolveAtTheEdmLevel() {
    final Edm edm = client.getReader().readMetadata(aliased().getSchemaByNsOrAlias());
    final EdmEntityType product = edm.getEntityType(new FullQualifiedName(ALIASED_NS, "Product"));
    assertEquals(new FullQualifiedName(ALIASED_NS, "Base"), product.getBaseType().getFullQualifiedName());
    assertEquals(new FullQualifiedName(ALIASED_NS, "Category"),
        product.getNavigationProperty("Category").getType().getFullQualifiedName());

    final EdmEntityType category = edm.getEntityType(new FullQualifiedName(ALIASED_NS, "Category"));
    assertEquals(new FullQualifiedName(ALIASED_NS, "Info"),
        category.getStructuralProperty("Info").getType().getFullQualifiedName());
    assertEquals(new FullQualifiedName(ALIASED_NS, "Colour"),
        category.getStructuralProperty("Shade").getType().getFullQualifiedName());
    assertEquals(new FullQualifiedName(ALIASED_NS, "Weight"),
        category.getStructuralProperty("Mass").getType().getFullQualifiedName());
    assertEquals(new FullQualifiedName(ALIASED_NS, "Product"),
        category.getNavigationProperty("Products").getType().getFullQualifiedName());

    final EdmEntityContainer container = edm.getEntityContainer();
    assertEquals("Container", container.getName());
    assertEquals("Category", container.getEntitySet("Categories").getEntityType().getName());
  }

  /**
   * The alias stays on the schema, so a caller that looks a schema up by its alias - the way
   * {@code ClientCsdlEdmProvider} does through {@link XMLMetadata#getSchemaByNsOrAlias()} - still
   * finds it after the parser resolved the document's own alias-qualified names.
   */
  @Test
  void theAliasSurvivesForCallerLookups() {
    final XMLMetadata metadata = aliased();
    assertEquals("self", metadata.getSchema(ALIASED_NS).getAlias());
    assertSame(metadata.getSchema(ALIASED_NS), metadata.getSchema("self"));
    assertTrue(metadata.getSchemaByNsOrAlias().containsKey("self"));
    assertTrue(metadata.getSchemaByNsOrAlias().containsKey(ALIASED_NS));
  }

  /**
   * Section 4.2: an alias declared by an $Include names a schema that is not in this document, so the
   * client's own namespace-or-alias map cannot resolve it; the parser does. A prefix that is neither a
   * schema alias nor an include alias passes through untouched.
   */
  @Test
  void includeAliasesResolveAndUnknownPrefixesPassThrough() {
    final CsdlSchema schema = aliasedSchema();
    assertEquals("Org.OData.Core.V1.Description", schema.getAnnotations().get(0).getTerm());
    final CsdlAnnotations group = schema.getAnnotationGroup(ALIASED_NS + ".Category", null);
    assertNotNull(group.getAnnotation("Org.OData.Core.V1.Description"));
    // odata.concat has no alias mapping, so the client-side function name is left as written.
    assertEquals("odata.concat", ((CsdlApply) group
        .getAnnotation("Org.OData.Core.V1.Fn").getExpression()).getFunction());
  }

  /** Section 4: $Version, $Reference with $Include and $IncludeAnnotations, and the schema list. */
  @Test
  void documentLevelMembers() {
    final XMLMetadata metadata = aliased();
    assertEquals("4.01", metadata.getEdmVersion());
    assertEquals(1, metadata.getSchemas().size());
    assertSame(metadata.getSchema(0), metadata.getSchema(ALIASED_NS));
    // A CSDL JSON document has no XML namespace declarations, so there are none to report.
    assertNull(metadata.getSchemaNamespaces());

    final List<Reference> references = metadata.getReferences();
    assertEquals(1, references.size());
    final Reference reference = references.get(0);
    assertEquals("http://localhost/vocabularies/Org.OData.Core.V1.xml", reference.getUri().toASCIIString());
    assertEquals("Org.OData.Core.V1", reference.getIncludes().get(0).getNamespace());
    assertEquals("Core", reference.getIncludes().get(0).getAlias());
    assertEquals("Org.OData.Core.V1", reference.getIncludeAnnotations().get(0).getTermNamespace());
    assertEquals("Tablet", reference.getIncludeAnnotations().get(0).getQualifier());
    assertEquals(ALIASED_NS, reference.getIncludeAnnotations().get(0).getTargetNamespace());
  }

  // ------------------------------------------------------ sections 6 to 11: the rest of the model

  /** Sections 10 and 11: enumeration types with their members and type definitions with their facets. */
  @Test
  void enumTypeAndTypeDefinition() {
    final CsdlEnumType colour = aliasedSchema().getEnumType("Colour");
    assertEquals("Edm.Int16", colour.getUnderlyingType());
    assertTrue(colour.isFlags());
    assertEquals(2, colour.getMembers().size());
    assertEquals("Red", colour.getMembers().get(0).getName());
    assertEquals("1", colour.getMembers().get(0).getValue());
    // Section 10.3: "Annotations for enumeration members are prefixed with the member name."
    assertEquals("the warm one", ((CsdlConstantExpression) colour.getMembers().get(0)
        .getAnnotations().get(0).getExpression()).getValue());

    final CsdlTypeDefinition weight = aliasedSchema().getTypeDefinition("Weight");
    assertEquals("Edm.Decimal", weight.getUnderlyingType());
    assertEquals(Integer.valueOf(7), weight.getPrecision());
    assertEquals(Integer.valueOf(3), weight.getScale());
  }

  /** Section 14.1: a term with its type, its default and the model elements it applies to. */
  @Test
  void termIsRead() {
    final CsdlTerm rating = aliasedSchema().getTerm("Rating");
    assertEquals("Edm.Int32", rating.getType());
    assertTrue(rating.isNullable());
    assertEquals("3", rating.getDefaultValue());
    assertEquals(List.of("EntityType", "Property"), rating.getAppliesTo());
  }

  /** Section 7.2: the facets, including the $Unicode default and the symbolic $SRID. */
  @Test
  void facetsAreRead() {
    final CsdlEntityType category = aliasedSchema().getEntityType("Category");
    final CsdlProperty name = category.getProperty("Name");
    assertEquals(Integer.valueOf(40), name.getMaxLength());
    assertFalse(name.isUnicode());
    // "Absence of the member means true", so the property that does not say anything is unicode.
    assertTrue(category.getProperty("Tags").isUnicode());
    assertTrue(category.getProperty("Tags").isCollection());
    assertEquals("variable", category.getProperty("Where").getSrid().toString());
  }

  /** Sections 6.5, 8.4 and 8.5: the key, $OnDelete and the referential constraints. */
  @Test
  void keyOnDeleteAndReferentialConstraint() {
    assertEquals(List.of("Code"),
        aliasedSchema().getEntityType("Category").getKey().stream().map(k -> k.getName()).toList());
    final CsdlNavigationProperty products =
        aliasedSchema().getEntityType("Category").getNavigationProperty("Products");
    assertTrue(products.isCollection());
    assertEquals("Category", products.getPartner());
    assertEquals(CsdlOnDeleteAction.Cascade, products.getOnDelete().getAction());

    final CsdlNavigationProperty category =
        aliasedSchema().getEntityType("Product").getNavigationProperty("Category");
    assertEquals(1, category.getReferentialConstraints().size());
    assertEquals("CategoryCode", category.getReferentialConstraints().get(0).getProperty());
    assertEquals("Code", category.getReferentialConstraints().get(0).getReferencedProperty());
  }

  /**
   * Section 8.2: "Nullable MUST NOT be specified for a collection-valued navigation property, a
   * collection is allowed to have zero items." The section 7.2.1 false default therefore does not
   * apply to one; the model default stands, which is what the CSDL XML path produces for the same
   * model. A single-valued navigation property does take the false default. Mirrors
   * MetadataJsonParserTest#collectionNavigationPropertyKeepsTheModelNullableDefault on the server.
   */
  @Test
  void collectionNavigationPropertyKeepsTheModelNullableDefault() {
    final CsdlNavigationProperty products =
        aliasedSchema().getEntityType("Category").getNavigationProperty("Products");
    assertTrue(products.isCollection());
    assertTrue(products.isNullable(),
        "$Nullable is prohibited on a collection navigation property, so no default is applied");

    final CsdlSchema schema = parse("{\"$Version\":\"4.01\",\"ns\":{\"T\":{\"$Kind\":\"EntityType\","
        + "\"Single\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.T\"},"
        + "\"Many\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.T\",\"$Collection\":true}}}}")
        .getSchema("ns");
    assertFalse(schema.getEntityType("T").getNavigationProperty("Single").isNullable(),
        "absent $Nullable means false for a single-valued navigation property");
    assertTrue(schema.getEntityType("T").getNavigationProperty("Many").isNullable());
  }

  // --------------------------------------------------- sections 12 and 13: behaviour and container

  /** Sections 12.2/12.4: overloads are arrays; 12.7/12.8/12.9: composability, return type, parameters. */
  @Test
  void actionAndFunctionOverloads() {
    final CsdlAction discount = aliasedSchema().getActions("Discount").get(0);
    assertTrue(discount.isBound());
    assertEquals("product", discount.getEntitySetPath());
    assertEquals(2, discount.getParameters().size());
    assertEquals("product", discount.getParameters().get(0).getName());
    assertEquals(ALIASED_NS + ".Product", discount.getParameters().get(0).getType());
    assertFalse(discount.getParameters().get(0).isNullable());
    assertTrue(discount.getParameters().get(1).isNullable());
    assertEquals(Integer.valueOf(5), discount.getParameters().get(1).getPrecision());
    assertEquals(Integer.valueOf(2), discount.getParameters().get(1).getScale());
    assertEquals(ALIASED_NS + ".Product", discount.getReturnType().getType());

    // Section 12.5: "Absence of the member means false" - Reset is unbound and has no return type.
    final CsdlAction reset = aliasedSchema().getActions("Reset").get(0);
    assertFalse(reset.isBound());
    assertNull(reset.getReturnType());
    assertTrue(reset.getParameters().isEmpty());

    final CsdlFunction top = aliasedSchema().getFunctions("Top").get(0);
    assertTrue(top.isComposable());
    assertFalse(top.isBound());
    assertTrue(top.getReturnType().isCollection());
    assertTrue(top.getReturnType().isNullable());
    assertEquals(ALIASED_NS + ".Product", top.getReturnType().getType());
    assertEquals("count", top.getParameters().get(0).getName());
  }

  /** Section 13: the container with its entity sets, singleton, imports and the two opposite defaults. */
  @Test
  void entityContainerMembers() {
    final CsdlEntityContainer container = aliasedSchema().getEntityContainer();
    assertEquals("Container", container.getName());
    assertEquals(2, container.getEntitySets().size());
    // Section 13.2: "Absence of the member means true."
    assertTrue(container.getEntitySet("Categories").isIncludeInServiceDocument());
    assertFalse(container.getEntitySet("Products").isIncludeInServiceDocument());
    assertEquals(ALIASED_NS + ".Category", container.getEntitySet("Categories").getType());
    assertEquals("Products",
        container.getEntitySet("Categories").getNavigationPropertyBindings().get(0).getPath());
    assertEquals("Products",
        container.getEntitySet("Categories").getNavigationPropertyBindings().get(0).getTarget());

    assertEquals(ALIASED_NS + ".Product", container.getSingleton("Best").getType());
    assertEquals(ALIASED_NS + ".Reset", container.getActionImport("ResetAll").getAction());
    assertEquals(ALIASED_NS + ".Top", container.getFunctionImport("TopProducts").getFunction());
    assertEquals("Products", container.getFunctionImport("TopProducts").getEntitySet());
    assertTrue(container.getFunctionImport("TopProducts").isIncludeInServiceDocument());
  }

  /** Section 13.6: "If not explicitly indicated, it is not included" - the opposite of section 13.2. */
  @Test
  void functionImportDefaultsToNotIncludedInServiceDocument() {
    final CsdlEntityContainer container = parse("{\"$Version\":\"4.01\",\"$EntityContainer\":\"ns.C\","
        + "\"ns\":{\"F\":[{\"$Kind\":\"Function\",\"$ReturnType\":{}}],"
        + "\"C\":{\"$Kind\":\"EntityContainer\",\"FI\":{\"$Function\":\"ns.F\"}}}}")
        .getSchema(ALIASED_NS).getEntityContainer();
    assertFalse(container.getFunctionImport("FI").isIncludeInServiceDocument());
    // Section 12.8: an absent $Type on the return type means Edm.String.
    assertEquals("Edm.String",
        parse("{\"$Version\":\"4.01\",\"ns\":{\"F\":[{\"$Kind\":\"Function\",\"$ReturnType\":{}}]}}")
            .getSchema(ALIASED_NS).getFunctions("F").get(0).getReturnType().getType());
  }

  // ------------------------------------------------------------- section 14: annotation expressions

  private CsdlAnnotation categoryAnnotation(final String term) {
    final CsdlAnnotation annotation = aliasedSchema()
        .getAnnotationGroup(ALIASED_NS + ".Category", null)
        .getAnnotation("Org.OData.Core.V1." + term);
    assertNotNull(annotation, term);
    return annotation;
  }

  /** Sections 14.3: a constant is a bare JSON value, and its type comes from the JSON shape alone. */
  @Test
  void constantAnnotationValues() {
    assertEquals(ConstantExpressionType.String,
        ((CsdlConstantExpression) categoryAnnotation("Description").getExpression()).getType());
    // The term of the document's own schema is alias-resolved just like an included one.
    final CsdlAnnotation rating = aliasedSchema()
        .getAnnotationGroup(ALIASED_NS + ".Category", null).getAnnotation(ALIASED_NS + ".Rating");
    assertEquals(ConstantExpressionType.Int,
        ((CsdlConstantExpression) rating.getExpression()).getType());
    assertEquals("5", ((CsdlConstantExpression) rating.getExpression()).getValue());
    // Section 14.2.1: the qualifier is what follows the hash in the member name, and it is kept apart
    // from the term, which is why the term of the qualified annotation is the bare term name.
    final CsdlAnnotation tag = categoryAnnotation("Tag");
    assertEquals("Tablet", tag.getQualifier());
    assertEquals(ConstantExpressionType.Bool, ((CsdlConstantExpression) tag.getExpression()).getType());
  }

  /** Section 14.4: every dynamic expression the Csdl model has a class for. */
  @Test
  void dynamicAnnotationExpressions() {
    assertEquals("Name", ((CsdlPath) categoryAnnotation("Ref").getExpression()).getValue());
    assertEquals("Products",
        ((CsdlNavigationPropertyPath) categoryAnnotation("Nav").getExpression()).getValue());
    assertEquals("Code", ((CsdlPropertyPath) categoryAnnotation("Prop").getExpression()).getValue());
    assertEquals("Products/@Core.Description",
        ((CsdlAnnotationPath) categoryAnnotation("Ann").getExpression()).getValue());

    // Section 14.4.12: the record's type is the @type control information, alias-resolved.
    final CsdlRecord record = (CsdlRecord) categoryAnnotation("Rec").getExpression();
    assertEquals(ALIASED_NS + ".Info", record.getType());
    assertEquals(2, record.getPropertyValues().size());
    assertEquals("ID", record.getPropertyValues().get(0).getProperty());
    // "Annotations for record members are prefixed with the member name."
    assertEquals("the label", ((CsdlConstantExpression) record.getPropertyValues().get(1)
        .getAnnotations().get(0).getExpression()).getValue());

    // Section 14.4.6: a collection is a bare JSON array, one item per item expression.
    final CsdlCollection collection = (CsdlCollection) categoryAnnotation("Coll").getExpression();
    assertEquals(4, collection.getItems().size());
    assertEquals(ConstantExpressionType.Int,
        ((CsdlConstantExpression) collection.getItems().get(0)).getType());
    assertEquals(ConstantExpressionType.Float,
        ((CsdlConstantExpression) collection.getItems().get(1)).getType());
    assertInstanceOf(CsdlNull.class, collection.getItems().get(3));

    final CsdlIf conditional = (CsdlIf) categoryAnnotation("Cond").getExpression();
    assertInstanceOf(CsdlPath.class, conditional.getGuard());
    assertNotNull(conditional.getElse());

    final CsdlApply apply = (CsdlApply) categoryAnnotation("Fn").getExpression();
    assertEquals(1, apply.getParameters().size());

    // Sections 14.4.5/14.4.8: $Collection turns the cast type into a Collection(...) type expression.
    assertEquals("Collection(Edm.String)", ((CsdlCast) categoryAnnotation("Cast").getExpression()).getType());
    assertEquals("Edm.Int32", ((CsdlIsOf) categoryAnnotation("Check").getExpression()).getType());
    assertInstanceOf(CsdlUrlRef.class, categoryAnnotation("Url").getExpression());

    final CsdlLogicalOrComparisonExpression eq =
        (CsdlLogicalOrComparisonExpression) categoryAnnotation("Cmp").getExpression();
    assertEquals(CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType.Eq, eq.getType());
    assertInstanceOf(CsdlPath.class, eq.getLeft());
    assertInstanceOf(CsdlConstantExpression.class, eq.getRight());
    final CsdlLogicalOrComparisonExpression not =
        (CsdlLogicalOrComparisonExpression) categoryAnnotation("Neg").getExpression();
    assertEquals(CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType.Not, not.getType());

    assertEquals("L", ((CsdlLabeledElement) categoryAnnotation("Lbl").getExpression()).getName());
    // Section 14.4.10: the reference is a qualified name, so it is alias-resolved.
    assertEquals(ALIASED_NS + ".L",
        ((CsdlLabeledElementReference) categoryAnnotation("LblRef").getExpression()).getValue());

    // Section 14.4.11: a $Null object exists only to carry annotations of its own.
    final CsdlNull annotatedNull = (CsdlNull) categoryAnnotation("Nothing").getExpression();
    assertEquals("nothing",
        ((CsdlConstantExpression) annotatedNull.getAnnotations().get(0).getExpression()).getValue());
  }

  /** Section 5.2: the target path is alias-resolved and the hash carries the group's qualifier. */
  @Test
  void externalTargetPathsAreResolvedWithTheirQualifier() {
    final CsdlSchema schema = aliasedSchema();
    assertEquals(2, schema.getAnnotationGroups().size());
    assertNotNull(schema.getAnnotationGroup(ALIASED_NS + ".Category", null));
    final CsdlAnnotations name = schema.getAnnotationGroup(ALIASED_NS + ".Category/Name", "Tablet");
    assertNotNull(name);
    assertEquals("the name", ((CsdlConstantExpression) name
        .getAnnotation("Org.OData.Core.V1.Description").getExpression()).getValue());
  }

  /** Annotations land on the container and on the schema itself, not only on the type system. */
  @Test
  void annotationsOnTheSchemaAndTheContainer() {
    assertEquals("the schema itself", ((CsdlConstantExpression) aliasedSchema()
        .getAnnotations().get(0).getExpression()).getValue());
    assertEquals("the container", ((CsdlConstantExpression) aliasedSchema().getEntityContainer()
        .getAnnotations().get(0).getExpression()).getValue());
  }

  // ------------------------------------------------------------------------- input-only tolerances

  /**
   * The shapes the pre-conformance Olinguito JSON writer produced, which an older service still
   * serves. They are read here and never written.
   */
  @Test
  void legacyWriterShapesAreTolerated() {
    final CsdlSchema schema = parse("{\"$Version\":\"4.01\",\"$EntityContainer\":\"ns.C\",\"ns\":{"
        + "\"T\":{\"$Kind\":\"EntityType\",\"$Key\":[\"ID\"],\"ID\":{\"$Type\":\"Edm.Int32\"},"
        + "\"N\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.T\","
        + "\"OnDelete\":{\"Action\":\"SetNull\"}}},"
        + "\"C\":{\"$Kind\":\"EntityContainer\","
        + "\"Ts\":{\"$Kind\":\"EntitySet\",\"$Type\":\"ns.T\"},"
        + "\"Extending\":{\"$Extends\":\"ns.Other\"}},"
        + "\"$Annotations\":{\"ns.T\":{\"@a.Legacy\":{\"$Int\":\"7\"},"
        + "\"@a.Rec\":{\"$Type\":\"ns.T\",\"P\":1}}}}}").getSchema(ALIASED_NS);
    assertEquals(CsdlOnDeleteAction.SetNull,
        schema.getEntityType("T").getNavigationProperty("N").getOnDelete().getAction());
    // The entity set the pre-conformance writer wrote without $Collection is still an entity set.
    assertNotNull(schema.getEntityContainer().getEntitySet("Ts"));
    assertTrue(schema.getEntityContainer().getSingletons().isEmpty());
    assertEquals("ns.Other", schema.getEntityContainer().getExtendsContainer());
    final CsdlAnnotations group = schema.getAnnotationGroup("ns.T", null);
    assertEquals(ConstantExpressionType.Int,
        ((CsdlConstantExpression) group.getAnnotation("a.Legacy").getExpression()).getType());
    assertEquals("ns.T", ((CsdlRecord) group.getAnnotation("a.Rec").getExpression()).getType());
  }

  // ------------------------------------------------------------------------------- the error paths

  /** Every failure reaches the caller as the IllegalArgumentException the CSDL XML path reports. */
  @Test
  void malformedDocumentsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> parse("[]"), "not a JSON object");
    assertThrows(IllegalArgumentException.class, () -> parse("{\"ns\":{}}"), "no $Version");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"3.0\",\"ns\":{}}"), "unknown $Version");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"4.01\",\"ns\":{\"T\":{}}}"), "schema member without $Kind");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"4.01\",\"ns\":{\"T\":{\"$Kind\":\"EntityType\","
            + "\"N\":{\"$Kind\":\"NavigationProperty\"}}}}"), "navigation property without $Type");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"4.01\",\"ns\":{\"F\":[{\"$Kind\":\"Function\"}]}}"),
        "function overload without $ReturnType");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"4.01\",\"ns\":{\"P\":[{\"$Parameter\":[]}]}}"),
        "overload without $Kind");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"4.01\",\"ns\":{\"$Annotations\":{\"ns.T\":"
            + "{\"@a.T\":{\"$ModelElementPath\":\"x\"}}}}}"), "unknown dynamic expression member");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"4.01\",\"ns\":{\"$Annotations\":{\"ns.T\":"
            + "{\"@a.T\":{\"$Has\":[1,2]}}}}}"), "$Has has no model class");
    assertThrows(IllegalArgumentException.class,
        () -> parse("{\"$Version\":\"4.01\",\"ns\":{\"T\":{\"$Kind\":\"EntityType\","
            + "\"P\":{\"$MaxLength\":\"max\"}}}}"), "the symbolic $MaxLength");
  }

  /**
   * Section 13: a document that declares two entity containers without saying which one is the
   * service's cannot be read; with $EntityContainer the named one wins.
   */
  @Test
  void theServiceContainerIsTheOneEntityContainerNames() {
    final String twoContainers = "{\"$Version\":\"4.01\",%s\"ns\":{"
        + "\"A\":{\"$Kind\":\"EntityContainer\"},\"B\":{\"$Kind\":\"EntityContainer\"}}}";
    assertThrows(IllegalArgumentException.class, () -> parse(String.format(twoContainers, "")));
    assertEquals("B", parse(String.format(twoContainers, "\"$EntityContainer\":\"ns.B\","))
        .getSchema(ALIASED_NS).getEntityContainer().getName());
  }
}
