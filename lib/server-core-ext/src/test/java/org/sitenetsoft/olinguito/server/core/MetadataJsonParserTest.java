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
 * Copyright 2026 SiteNetSoft - Pinned the include-alias resolution of qualified names
 * Copyright 2026 SiteNetSoft - Added the operation, import and entity container parser tests
 * Copyright 2026 SiteNetSoft - Added the annotation and annotation expression parser tests
 * Copyright 2026 SiteNetSoft - Pinned the collection cast type and the unknown expression member
 * Copyright 2026 SiteNetSoft - Pinned the collection navigation property $Nullable prohibition
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDeleteAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlApply;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCast;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNull;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlUrlRef;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataJsonParserTest {

  private static final FullQualifiedName CATEGORY = new FullQualifiedName("ns", "Category");
  private static final FullQualifiedName PRODUCT = new FullQualifiedName("ns", "Product");

  /**
   * A document whose qualified names use an alias declared by an $Include (Core, A), an alias declared
   * by a schema of the document itself (self) and a prefix that is neither (Foo).
   */
  private static final String ALIASED_CSDL = "{\"$Version\":\"4.01\",\"$Reference\":{"
      + "\"http://docs.oasis-open.org/odata/odata/v4.0/errata03/csd01/complete/vocabularies/"
      + "Org.OData.Core.V1.xml\":{\"$Include\":[{\"$Namespace\":\"Org.OData.Core.V1\","
      + "\"$Alias\":\"Core\"}]},"
      + "\"http://localhost/a.xml\":{\"$Include\":[{\"$Namespace\":\"org.sitenetsoft.olinguito.a\","
      + "\"$Alias\":\"A\"}]}},"
      + "\"ns\":{\"$Alias\":\"self\",\"ET\":{\"$Kind\":\"EntityType\",\"$Key\":[\"ID\"],"
      + "\"ID\":{\"$Type\":\"Edm.Int32\"},"
      + "\"Description\":{\"$Type\":\"Core.Tag\"},"
      + "\"Extended\":{\"$Type\":\"A.ExtendedInfo\"},"
      + "\"Own\":{\"$Type\":\"self.Info\"},"
      + "\"Unknown\":{\"$Type\":\"Foo.Bar\"}},"
      + "\"Info\":{\"$Kind\":\"ComplexType\"}}}";

  /** Resolves the http://localhost/ references of {@link #ALIASED_CSDL} from the test classpath. */
  private final ReferenceResolver localResolver = new ReferenceResolver() {
    @Override
    public InputStream resolveReference(final URI uri, final String base) {
      final String str = uri.toASCIIString();
      if (str.startsWith("http://localhost/")) {
        return getClass().getClassLoader().getResourceAsStream(str.substring("http://localhost/".length()));
      }
      return null;
    }
  };

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
    assertTrue(provider.getEntityType(PRODUCT).getNavigationProperty("Category").isNullable());
  }

  /**
   * Section 8.2: "Nullable MUST NOT be specified for a collection-valued navigation property, a
   * collection is allowed to have zero items." The section 7.2.1 false default therefore does not
   * apply to one; the model keeps its own default, which is what the CSDL XML parser produces for the
   * same model. A single-valued navigation property does take the false default.
   */
  @Test
  void collectionNavigationPropertyKeepsTheModelNullableDefault() throws Exception {
    assertTrue(provider.getEntityType(CATEGORY).getNavigationProperty("Products").isCollection());
    assertTrue(provider.getEntityType(CATEGORY).getNavigationProperty("Products").isNullable(),
        "$Nullable is prohibited on a collection navigation property, so no default is applied");

    final String document = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"$Key\":[\"ID\"],\"ID\":{\"$Type\":\"Edm.Int32\"},"
        + "\"Single\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.ET\"},"
        + "\"Many\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.ET\",\"$Collection\":true}}}}";
    final SchemaBasedEdmProvider parsed =
        new MetadataJsonParser().buildEdmProvider(new StringReader(document));
    final CsdlEntityType type = parsed.getEntityType(new FullQualifiedName("ns", "ET"));
    assertFalse(type.getNavigationProperty("Single").isNullable(),
        "absent $Nullable means false for a single-valued navigation property");
    assertTrue(type.getNavigationProperty("Many").isNullable());
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

  /**
   * Section 4.2: "If an included schema specifies an alias, the alias MUST be used in qualified names
   * throughout the document to identify model elements of the included schema." The model keeps
   * namespace-qualified names, so both include aliases must be mapped back.
   */
  @Test
  void includeAliasResolvesToTheIncludedNamespace() throws Exception {
    final CsdlEntityType type = aliasedProvider().getEntityType(new FullQualifiedName("ns", "ET"));
    assertEquals("Org.OData.Core.V1.Tag", type.getProperty("Description").getType());
    assertEquals("org.sitenetsoft.olinguito.a.ExtendedInfo", type.getProperty("Extended").getType());
    assertEquals("ns.Info", type.getProperty("Own").getType());
  }

  /** A prefix that is neither an include alias nor a schema alias is a namespace, and passes through. */
  @Test
  void unknownPrefixPassesThroughUnchanged() throws Exception {
    assertEquals("Foo.Bar",
        aliasedProvider().getEntityType(new FullQualifiedName("ns", "ET")).getProperty("Unknown").getType());
  }

  /**
   * Two layers look like they could resolve an include alias: this parser rewrites alias-qualified names
   * while reading, and {@link ReferenceLoader} additionally registers every referenced provider under its
   * alias. The parser is the authoritative - in fact the only - one: provider lookups match a schema by
   * its namespace ({@code SchemaBasedEdmProvider#getSchemaDirectly}), so the loader's alias key never
   * resolves anything, and an alias-qualified name that reached the model would be unresolvable.
   */
  @Test
  void parseTimeAliasResolutionIsAuthoritative() throws Exception {
    final SchemaBasedEdmProvider aliased = aliasedProvider();
    assertNotNull(aliased.getSchema("org.sitenetsoft.olinguito.a", true), "the reference was loaded");
    assertNull(aliased.getSchema("A", true),
        "the loader's alias registration does not make a schema findable by alias");
    assertEquals("org.sitenetsoft.olinguito.a.ExtendedInfo",
        aliased.getEntityType(new FullQualifiedName("ns", "ET")).getProperty("Extended").getType(),
        "the parser resolves the alias, so no alias-qualified name ever reaches the provider lookup");
  }

  private SchemaBasedEdmProvider aliasedProvider() throws Exception {
    return new MetadataJsonParser()
        .referenceResolver(this.localResolver)
        .buildEdmProvider(new StringReader(ALIASED_CSDL));
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

  @Test
  void entityContainerFromTheConformantFixture() throws Exception {
    final CsdlEntityContainer container = provider.getEntityContainer();
    assertEquals("Container", container.getName());
    assertEquals("ns.BaseContainer", container.getExtendsContainer());

    final CsdlEntitySet categories = container.getEntitySet("Categories");
    assertEquals("ns.Category", categories.getType());
    assertTrue(categories.isIncludeInServiceDocument(), "absence of the member means true for an entity set");
    assertEquals("Products", categories.getNavigationPropertyBindings().get(0).getPath());
    assertEquals("Products", categories.getNavigationPropertyBindings().get(0).getTarget());
    assertFalse(container.getEntitySet("Products").isIncludeInServiceDocument());

    final CsdlSingleton best = container.getSingleton("Best");
    assertEquals("ns.Product", best.getType());
  }

  @Test
  void actionAndFunctionOverloadsAreArrays() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{"
        + "\"Reset\":[{\"$Kind\":\"Action\"},"
        + "{\"$Kind\":\"Action\",\"$IsBound\":true,\"$EntitySetPath\":\"b/Nav\","
        + "\"$Parameter\":[{\"$Name\":\"b\",\"$Type\":\"ns.ET\"},{\"$Name\":\"n\"}],"
        + "\"$ReturnType\":{\"$Type\":\"ns.ET\",\"$Collection\":true}}],"
        + "\"Top\":[{\"$Kind\":\"Function\",\"$IsComposable\":true,"
        + "\"$Parameter\":[{\"$Name\":\"Year\",\"$Type\":\"Edm.Decimal\",\"$Precision\":4,\"$Scale\":0}],"
        + "\"$ReturnType\":{\"$Type\":\"Edm.Int32\",\"$Nullable\":true}}]}}";
    final SchemaBasedEdmProvider parsed =
        new MetadataJsonParser().buildEdmProvider(new StringReader(csdl));

    final List<CsdlAction> actions = parsed.getActions(new FullQualifiedName("ns", "Reset"));
    assertEquals(2, actions.size());
    assertFalse(actions.get(0).isBound(), "absence of $IsBound means false");
    assertNull(actions.get(0).getReturnType(), "$ReturnType is optional for an action");
    assertTrue(actions.get(1).isBound());
    assertEquals("b/Nav", actions.get(1).getEntitySetPath());
    assertEquals(2, actions.get(1).getParameters().size());
    assertEquals("Edm.String", actions.get(1).getParameters().get(1).getType());
    assertTrue(actions.get(1).getReturnType().isCollection());

    final CsdlFunction function = parsed.getFunctions(new FullQualifiedName("ns", "Top")).get(0);
    assertTrue(function.isComposable());
    assertEquals(Integer.valueOf(4), function.getParameters().get(0).getPrecision());
    assertTrue(function.getReturnType().isNullable());
  }

  /** Section 12.4: "It MUST contain the member $ReturnType" - for functions only. */
  @Test
  void functionWithoutReturnTypeIsRejected() {
    final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader(
            "{\"$Version\":\"4.01\",\"ns\":{\"F\":[{\"$Kind\":\"Function\"}]}}")));
    assertEquals("ns/F[0]/$ReturnType", thrown.getJsonPath());
  }

  /**
   * The brief pinned the raw {@code self.} prefix here, but that predates the document-global alias
   * resolution Task 5 had to add (and which the brief's own $Extends expectation relies on): a
   * qualified name only resolves against the provider once the alias is mapped back to its namespace.
   */
  @Test
  void actionAndFunctionImports() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"$EntityContainer\":\"ns.C\",\"ns\":{\"$Alias\":\"self\","
        + "\"C\":{\"$Kind\":\"EntityContainer\","
        + "\"Reset\":{\"$Action\":\"self.Reset\"},"
        + "\"Top\":{\"$Function\":\"self.Top\",\"$EntitySet\":\"Products\","
        + "\"$IncludeInServiceDocument\":true}}}}";
    final CsdlEntityContainer container =
        new MetadataJsonParser().buildEdmProvider(new StringReader(csdl)).getEntityContainer();
    assertEquals("ns.Reset", container.getActionImports().get(0).getAction());
    assertEquals("Reset", container.getActionImports().get(0).getName());
    assertNull(container.getActionImports().get(0).getEntitySet());
    assertEquals("ns.Top", container.getFunctionImports().get(0).getFunction());
    assertEquals("Products", container.getFunctionImports().get(0).getEntitySet());
    assertTrue(container.getFunctionImports().get(0).isIncludeInServiceDocument());
  }

  /** A cross-container $EntitySet target path keeps both of its segments. */
  @Test
  void entitySetTargetPathIsNotStripped() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"$EntityContainer\":\"ns.C\",\"ns\":{"
        + "\"C\":{\"$Kind\":\"EntityContainer\","
        + "\"Top\":{\"$Function\":\"ns.Top\",\"$EntitySet\":\"other.Container/Products\"}}}}";
    assertEquals("other.Container/Products",
        new MetadataJsonParser().buildEdmProvider(new StringReader(csdl))
            .getEntityContainer().getFunctionImports().get(0).getEntitySet());
  }

  /** Section 13.6: a function import that says nothing is NOT in the service document (13.2 is the
   * other way round for entity sets). */
  @Test
  void functionImportDefaultsToNotIncludedInServiceDocument() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"$EntityContainer\":\"ns.C\",\"ns\":{"
        + "\"C\":{\"$Kind\":\"EntityContainer\",\"Top\":{\"$Function\":\"ns.Top\"}}}}";
    assertFalse(new MetadataJsonParser().buildEdmProvider(new StringReader(csdl))
        .getEntityContainer().getFunctionImports().get(0).isIncludeInServiceDocument());
  }

  /**
   * Legacy tolerance: the shapes the old Olinguito JSON writer produced are accepted on input and
   * normalized. They are never written - Tasks 1-3 removed them from the writer.
   */
  @Test
  void legacyWriterShapesAreTolerated() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{"
        + "\"C\":{\"$Kind\":\"EntityContainer\","
        + "\"Extending\":{\"$Kind\":\"EntityContainer\",\"$Extends\":\"ns.Base\"},"
        + "\"ES\":{\"$Kind\":\"EntitySet\",\"$Type\":\"ns.ET\"},"
        + "\"S\":{\"$Kind\":\"Singleton\",\"$Type\":\"ns.ET\"},"
        + "\"AI\":{\"$Kind\":\"ActionImport\",\"$Action\":\"ns.A\",\"$EntitySet\":\"Alias.ES\"}}}}";
    final CsdlEntityContainer container =
        new MetadataJsonParser().buildEdmProvider(new StringReader(csdl)).getEntityContainer();
    assertEquals("C", container.getName(), "the container is found without a top-level $EntityContainer");
    assertEquals("ns.Base", container.getExtendsContainer(), "the nested Extending object is normalized");
    assertEquals("ns.ET", container.getEntitySet("ES").getType(), "an entity set without $Collection");
    assertEquals("ns.ET", container.getSingleton("S").getType());
    assertEquals("ES", container.getActionImports().get(0).getEntitySet(), "the alias prefix is stripped");
  }

  @Test
  void twoContainersWithoutEntityContainerMemberAreRejected() {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"C1\":{\"$Kind\":\"EntityContainer\"},"
        + "\"C2\":{\"$Kind\":\"EntityContainer\"}}}";
    assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader(csdl)));
  }

  /** $EntityContainer picks the service container out of a document that declares more than one. */
  @Test
  void entityContainerMemberSelectsTheServiceContainer() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"$EntityContainer\":\"ns.C2\",\"ns\":{"
        + "\"C1\":{\"$Kind\":\"EntityContainer\"},\"C2\":{\"$Kind\":\"EntityContainer\"}}}";
    assertEquals("C2", new MetadataJsonParser().buildEdmProvider(new StringReader(csdl))
        .getEntityContainer().getName());
  }

  /** An entity container member that is none of the four child kinds is an error, with its path. */
  @Test
  void unknownEntityContainerMemberIsRejected() {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"C\":{\"$Kind\":\"EntityContainer\","
        + "\"Odd\":{\"$Whatever\":true}}}}";
    final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader(csdl)));
    assertEquals("ns/C/Odd", thrown.getJsonPath());
  }

  /** A parameter object MUST carry $Name; the error names the failing array item. */
  @Test
  void parameterWithoutNameIsRejected() {
    final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader(
            "{\"$Version\":\"4.01\",\"ns\":{\"A\":[{\"$Kind\":\"Action\",\"$Parameter\":[{}]}]}}")));
    assertEquals("ns/A[0]/$Parameter[0]/$Name", thrown.getJsonPath());
  }

  /** An overload array item MUST carry $Kind with Action or Function. */
  @Test
  void overloadWithoutKindIsRejected() {
    final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().buildEdmProvider(new StringReader(
            "{\"$Version\":\"4.01\",\"ns\":{\"A\":[{\"$IsBound\":true}]}}")));
    assertEquals("ns/A[0]/$Kind", thrown.getJsonPath());
  }

  /** Legacy tolerance: the pre-conformance writer's "OnDelete" object beside the conformant string. */
  @Test
  void legacyOnDeleteObjectIsTolerated() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"$Key\":[\"ID\"],\"ID\":{\"$Type\":\"Edm.Int32\"},"
        + "\"Nav\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.ET\","
        + "\"OnDelete\":{\"Action\":\"Cascade\"}}}}}";
    final CsdlEntityType type = new MetadataJsonParser().buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET"));
    assertEquals(CsdlOnDeleteAction.Cascade,
        type.getNavigationProperty("Nav").getOnDelete().getAction());
  }

  @Test
  void annotationsAreSkippedUnlessRequested() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@Core.Description\":\"d\"}}}";
    assertTrue(new MetadataJsonParser().buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations().isEmpty());
    assertEquals(1, new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations().size());
  }

  @Test
  void constantAnnotationValuesAreBareJson() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@UI.ReadOnly\":true,"
        + "\"@UI.Thumbnail\":\"T0RhdGE\","
        + "\"@UI.Width\":3.14,"
        + "\"@A.Very.Long.Int\":42,"
        + "\"@UI.DisplayName\":null,"
        + "\"@UI.Tags\":[\"a\",\"b\"],"
        + "\"@Core.Description#Tablet\":\"d\"}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals(ConstantExpressionType.Bool, expression(annotations, "UI.ReadOnly").asConstant().getType());
    assertEquals(ConstantExpressionType.String, expression(annotations, "UI.Thumbnail").asConstant().getType());
    assertEquals(ConstantExpressionType.Float, expression(annotations, "UI.Width").asConstant().getType());
    assertEquals(ConstantExpressionType.Int, expression(annotations, "A.Very.Long.Int").asConstant().getType());
    assertTrue(expression(annotations, "UI.DisplayName") instanceof CsdlNull);
    assertEquals(2, ((CsdlCollection) expression(annotations, "UI.Tags")).getItems().size());
    assertEquals("Tablet", annotation(annotations, "Core.Description").getQualifier());
  }

  @Test
  void dynamicExpressionsAreRead() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@ns.T1\":{\"$Path\":\"FirstName\"},"
        + "\"@ns.T2\":{\"$And\":[true,false]},"
        + "\"@ns.T3\":{\"$Not\":true},"
        + "\"@ns.T4\":{\"$Apply\":[\"a\",\"b\"],\"$Function\":\"odata.concat\"},"
        + "\"@ns.T5\":{\"$If\":[true,\"t\",\"e\"]},"
        + "\"@ns.T6\":{\"$LabeledElement\":{\"$Path\":\"F\"},\"$Name\":\"L\"},"
        + "\"@ns.T7\":{\"$Cast\":\"v\",\"$Type\":\"Edm.String\",\"$MaxLength\":3},"
        + "\"@ns.T8\":{\"GivenName\":{\"$Path\":\"FirstName\"},\"@type\":\"ns.Manager\"},"
        + "\"@ns.T9\":{\"$UrlRef\":\"http://example.org\"}}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals("FirstName", ((CsdlPath) expression(annotations, "ns.T1")).getValue());
    assertEquals(CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType.And,
        ((CsdlLogicalOrComparisonExpression) expression(annotations, "ns.T2")).getType());
    assertEquals(CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType.Not,
        ((CsdlLogicalOrComparisonExpression) expression(annotations, "ns.T3")).getType());
    assertEquals("odata.concat", ((CsdlApply) expression(annotations, "ns.T4")).getFunction());
    assertNotNull(((CsdlIf) expression(annotations, "ns.T5")).getElse());
    assertEquals("L", ((CsdlLabeledElement) expression(annotations, "ns.T6")).getName());
    assertEquals(Integer.valueOf(3), ((CsdlCast) expression(annotations, "ns.T7")).getMaxLength());
    final CsdlRecord record = (CsdlRecord) expression(annotations, "ns.T8");
    assertEquals("ns.Manager", record.getType());
    assertEquals("GivenName", record.getPropertyValues().get(0).getProperty());
    assertNotNull(((CsdlUrlRef) expression(annotations, "ns.T9")).getValue());
  }

  /** Section 5.2: $Annotations is an object with one member per external annotation target. */
  @Test
  void externalTargetingAnnotationGroups() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"$Annotations\":{"
        + "\"ns.ET#Tablet\":{\"@Core.Description\":\"d\"}},"
        + "\"ET\":{\"$Kind\":\"EntityType\"}}}";
    final CsdlAnnotations group = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getAnnotationsGroup(new FullQualifiedName("ns", "ET"), "Tablet");
    assertNotNull(group);
    assertEquals(1, group.getAnnotations().size());
  }

  /** Section 10.3 / 8.6 / 8.5: annotations on an enum member, on $OnDelete and on a constraint are
   * prefixed with the thing they annotate. */
  @Test
  void prefixedAnnotationsLandOnTheirTarget() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{"
        + "\"E\":{\"$Kind\":\"EnumType\",\"Red\":1,\"Red@Core.Description\":\"the red one\"},"
        + "\"ET\":{\"$Kind\":\"EntityType\",\"Dep\":{\"$Type\":\"Edm.Int32\"},"
        + "\"Nav\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.ET\","
        + "\"$OnDelete\":\"Cascade\",\"$OnDelete@Core.Description\":\"why\","
        + "\"$ReferentialConstraint\":{\"Dep\":\"Principal\","
        + "\"Dep@Core.Description\":\"constraint\"}}}}}";
    final SchemaBasedEdmProvider parsed =
        new MetadataJsonParser().parseAnnotations(true).buildEdmProvider(new StringReader(csdl));
    assertEquals(1, parsed.getEnumType(new FullQualifiedName("ns", "E"))
        .getMember("Red").getAnnotations().size());
    final CsdlNavigationProperty nav =
        parsed.getEntityType(new FullQualifiedName("ns", "ET")).getNavigationProperty("Nav");
    assertEquals(1, nav.getOnDelete().getAnnotations().size());
    assertEquals(1, nav.getReferentialConstraints().get(0).getAnnotations().size());
    assertEquals("Principal", nav.getReferentialConstraints().get(0).getReferencedProperty());
  }

  /** Section 14.2: "An annotation can itself be annotated" - the member name gains a second @term. */
  @Test
  void annotationsOnAnnotationsLandOnTheAnnotation() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@Measures.ISOCurrency\":\"USD\","
        + "\"@Measures.ISOCurrency@Core.Description\":\"The parent company's currency\"}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals(1, annotations.size());
    assertEquals("Core.Description", annotations.get(0).getAnnotations().get(0).getTerm());
  }

  /** Section 14.2: the term name is a qualified name, so a document alias is resolved. */
  @Test
  void annotationTermNamesAreAliasResolved() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"$Alias\":\"self\","
        + "\"ET\":{\"$Kind\":\"EntityType\",\"@self.Tag\":\"x\"},"
        + "\"Tag\":{\"$Kind\":\"Term\",\"$Type\":\"Edm.String\"}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals("ns.Tag", annotations.get(0).getTerm());
  }

  /** Section 14.4.1: a path value is a path, never an alias-qualified name, so it is left verbatim. */
  @Test
  void pathExpressionValuesAreNotAliasResolved() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"$Alias\":\"self\","
        + "\"ET\":{\"$Kind\":\"EntityType\",\"@ns.T\":{\"$Path\":\"self.Thing/Name\"}}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals("self.Thing/Name", ((CsdlPath) annotations.get(0).getExpression()).getValue());
  }

  /** Section 14.4.11: a $Null object carries the annotations a bare null cannot. */
  @Test
  void annotatedNullExpression() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@UI.Address\":{\"$Null\":null,\"@self.Reason\":\"Private\"}}}}";
    final CsdlExpression value = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations().get(0).getExpression();
    assertTrue(value instanceof CsdlNull);
    assertEquals(1, ((CsdlNull) value).getAnnotations().size());
  }

  /** Section 14.4.12: the record's @type is the JSON control information, possibly a URI#fragment. */
  @Test
  void recordTypeComesFromTheTypeControlInformation() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"$Alias\":\"self\","
        + "\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@person.Employee\":{\"@type\":\"https://example.org/vocabs/person#self.Manager\","
        + "\"@Core.Description\":\"Annotation on record\","
        + "\"GivenName\":{\"$Path\":\"FirstName\"},"
        + "\"GivenName@Core.Description\":\"Annotation on record member\"},"
        + "\"@person.Short\":{\"@type\":\"#self.Manager\",\"X\":1},"
        + "\"@person.Legacy\":{\"$Type\":\"self.Manager\",\"X\":1}}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    final CsdlRecord record = (CsdlRecord) expression(annotations, "person.Employee");
    assertEquals("ns.Manager", record.getType());
    assertEquals(1, record.getAnnotations().size());
    assertEquals(1, record.getPropertyValues().size());
    assertEquals(1, record.getPropertyValues().get(0).getAnnotations().size());
    assertEquals("ns.Manager", ((CsdlRecord) expression(annotations, "person.Short")).getType());
    assertEquals("ns.Manager", ((CsdlRecord) expression(annotations, "person.Legacy")).getType());
  }

  /**
   * Legacy tolerance: the pre-conformance writer emitted the CSDL XML element names as members. They
   * are read back with their declared constant type; nothing writes them any more.
   */
  @Test
  void legacyTypedConstantMembersAreTolerated() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@ns.B\":{\"$Binary\":\"T0RhdGE\"},"
        + "\"@ns.D\":{\"$Date\":\"2012-02-29\"},"
        + "\"@ns.I\":{\"$Int\":\"42\"},"
        + "\"@ns.E\":{\"$EnumMember\":\"Red\"}}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals(ConstantExpressionType.Binary, expression(annotations, "ns.B").asConstant().getType());
    assertEquals("2012-02-29", expression(annotations, "ns.D").asConstant().getValue());
    assertEquals(ConstantExpressionType.Int, expression(annotations, "ns.I").asConstant().getType());
    assertEquals(ConstantExpressionType.EnumMember, expression(annotations, "ns.E").asConstant().getType());
  }

  /**
   * Section 14.4.2 lists $Has and $In among the comparison operators, but this codebase's
   * LogicalOrComparisonExpressionType has no constant for either. A recorded limitation: the parser
   * reports it rather than dropping the expression.
   */
  @Test
  void hasAndInComparisonsAreRejected() {
    final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
        () -> new MetadataJsonParser().parseAnnotations(true).buildEdmProvider(new StringReader(
            "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
                + "\"@ns.T\":{\"$Has\":[{\"$Path\":\"P\"},\"Red\"]}}}}")));
    assertEquals("ns/ET/@ns.T/$Has", thrown.getJsonPath());
  }

  /** Annotations on the schema, on properties, on operations and on container children all land. */
  @Test
  void annotationsLandOnEveryModelElement() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"$EntityContainer\":\"ns.C\",\"ns\":{"
        + "\"@Core.Description\":\"schema\","
        + "\"ET\":{\"$Kind\":\"EntityType\",\"P\":{\"@Core.Description\":\"property\"},"
        + "\"Nav\":{\"$Kind\":\"NavigationProperty\",\"$Type\":\"ns.ET\","
        + "\"@Core.Description\":\"nav\"}},"
        + "\"TD\":{\"$Kind\":\"TypeDefinition\",\"$UnderlyingType\":\"Edm.String\","
        + "\"@Core.Description\":\"td\"},"
        + "\"E\":{\"$Kind\":\"EnumType\",\"@Core.Description\":\"enum\"},"
        + "\"T\":{\"$Kind\":\"Term\",\"$Type\":\"Edm.String\",\"@Core.Description\":\"term\"},"
        + "\"CT\":{\"$Kind\":\"ComplexType\",\"@Core.Description\":\"complex\"},"
        + "\"F\":[{\"$Kind\":\"Function\",\"@Core.Description\":\"function\","
        + "\"$Parameter\":[{\"$Name\":\"p\",\"@Core.Description\":\"parameter\"}],"
        + "\"$ReturnType\":{\"$Type\":\"Edm.String\",\"@Core.Description\":\"return\"}}],"
        + "\"C\":{\"$Kind\":\"EntityContainer\",\"@Core.Description\":\"container\","
        + "\"ES\":{\"$Collection\":true,\"$Type\":\"ns.ET\",\"@Core.Description\":\"set\"},"
        + "\"SI\":{\"$Type\":\"ns.ET\",\"@Core.Description\":\"singleton\"},"
        + "\"FI\":{\"$Function\":\"ns.F\",\"@Core.Description\":\"import\"}}}}";
    final SchemaBasedEdmProvider parsed =
        new MetadataJsonParser().parseAnnotations(true).buildEdmProvider(new StringReader(csdl));
    assertEquals("schema", term(parsed.getSchema("ns").getAnnotations()));
    final CsdlEntityType entityType = parsed.getEntityType(new FullQualifiedName("ns", "ET"));
    assertEquals("property", term(entityType.getProperty("P").getAnnotations()));
    assertEquals("nav", term(entityType.getNavigationProperty("Nav").getAnnotations()));
    assertEquals("td", term(parsed.getTypeDefinition(new FullQualifiedName("ns", "TD")).getAnnotations()));
    assertEquals("enum", term(parsed.getEnumType(new FullQualifiedName("ns", "E")).getAnnotations()));
    assertEquals("term", term(parsed.getTerm(new FullQualifiedName("ns", "T")).getAnnotations()));
    assertEquals("complex", term(parsed.getComplexType(new FullQualifiedName("ns", "CT")).getAnnotations()));
    final CsdlFunction function = parsed.getFunctions(new FullQualifiedName("ns", "F")).get(0);
    assertEquals("function", term(function.getAnnotations()));
    assertEquals("parameter", term(function.getParameters().get(0).getAnnotations()));
    assertEquals("return", term(function.getReturnType().getAnnotations()));
    final CsdlEntityContainer container = parsed.getEntityContainer();
    assertEquals("container", term(container.getAnnotations()));
    assertEquals("set", term(container.getEntitySet("ES").getAnnotations()));
    assertEquals("singleton", term(container.getSingleton("SI").getAnnotations()));
    assertEquals("import", term(container.getFunctionImport("FI").getAnnotations()));
  }

  /**
   * Sections 14.4.5 and 14.4.8: $Collection beside $Type means the cast targets a collection. The Csdl
   * model keeps that in the type expression itself, the way the CSDL XML Type attribute carries it, so
   * dropping the member would silently turn the cast into a cast to a scalar.
   */
  @Test
  void collectionCastAndIsOfKeepTheirCollectionType() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"$Alias\":\"self\","
        + "\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@ns.T1\":{\"$Cast\":{\"$Path\":\"P\"},\"$Type\":\"self.CT\",\"$Collection\":true},"
        + "\"@ns.T2\":{\"$Cast\":{\"$Path\":\"P\"},\"$Type\":\"self.CT\"},"
        + "\"@ns.T3\":{\"$IsOf\":{\"$Path\":\"P\"},\"$Type\":\"Edm.String\",\"$Collection\":true},"
        + "\"@ns.T4\":{\"$IsOf\":{\"$Path\":\"P\"},\"$Type\":\"Edm.String\"}}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals("Collection(ns.CT)", ((CsdlCast) expression(annotations, "ns.T1")).getType());
    assertEquals("ns.CT", ((CsdlCast) expression(annotations, "ns.T2")).getType());
    assertEquals("Collection(Edm.String)", ((CsdlIsOf) expression(annotations, "ns.T3")).getType());
    assertEquals("Edm.String", ((CsdlIsOf) expression(annotations, "ns.T4")).getType());
  }

  /**
   * Section 14.4.12: a record's members are property names, which are simple identifiers. An object
   * whose only members are $-prefixed is an expression this parser does not know - a form with no Csdl
   * class, such as $ModelElementPath, or a misspelling - and is reported rather than read as an empty
   * record.
   */
  @Test
  void unknownDollarExpressionMemberIsRejected() {
    for (final String member : List.of("$ModelElementPath", "$Pathh")) {
      final CsdlJsonParseException thrown = assertThrows(CsdlJsonParseException.class,
          () -> new MetadataJsonParser().parseAnnotations(true).buildEdmProvider(new StringReader(
              "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
                  + "\"@ns.T\":{\"" + member + "\":\"x\"}}}}")), member);
      assertEquals("ns/ET/@ns.T/" + member, thrown.getJsonPath(), member);
    }
  }

  /** The rejection above must not touch a genuine record, with or without its @type. */
  @Test
  void recordsWithRealMembersStillParse() throws Exception {
    final String csdl = "{\"$Version\":\"4.01\",\"ns\":{\"ET\":{\"$Kind\":\"EntityType\","
        + "\"@ns.T1\":{\"@type\":\"#ns.Manager\",\"GivenName\":{\"$Path\":\"F\"}},"
        + "\"@ns.T2\":{\"GivenName\":{\"$Path\":\"F\"}},"
        + "\"@ns.T3\":{\"@type\":\"#ns.Manager\"},"
        + "\"@ns.T4\":{\"$Type\":\"ns.Manager\"}}}}";
    final List<CsdlAnnotation> annotations = new MetadataJsonParser().parseAnnotations(true)
        .buildEdmProvider(new StringReader(csdl))
        .getEntityType(new FullQualifiedName("ns", "ET")).getAnnotations();
    assertEquals(1, ((CsdlRecord) expression(annotations, "ns.T1")).getPropertyValues().size());
    assertEquals(1, ((CsdlRecord) expression(annotations, "ns.T2")).getPropertyValues().size());
    assertEquals("ns.Manager", ((CsdlRecord) expression(annotations, "ns.T3")).getType());
    assertEquals("ns.Manager", ((CsdlRecord) expression(annotations, "ns.T4")).getType());
  }

  private static String term(final List<CsdlAnnotation> annotations) {
    assertEquals(1, annotations.size());
    return annotations.get(0).getExpression().asConstant().getValue();
  }

  private static CsdlAnnotation annotation(final List<CsdlAnnotation> annotations, final String term) {
    for (final CsdlAnnotation annotation : annotations) {
      if (term.equals(annotation.getTerm())) {
        return annotation;
      }
    }
    throw new AssertionError("no annotation for the term " + term);
  }

  private static CsdlExpression expression(final List<CsdlAnnotation> annotations, final String term) {
    return annotation(annotations, term).getExpression();
  }
}
