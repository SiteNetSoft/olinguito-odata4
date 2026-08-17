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
 * Copyright 2026 SiteNetSoft - OData 4.01: tests for Core.AlternateKeys support
 * Copyright 2026 SiteNetSoft - OData 4.01: tests for malformed and ambiguous alternate-key declarations
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAlternateKey;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAlternateKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;

class EdmAlternateKeysTest {

  private static final FullQualifiedName TYPE_NAME = new FullQualifiedName("ns", "ET");
  private static final FullQualifiedName COMPLEX_NAME = new FullQualifiedName("ns", "CT");
  private static final FullQualifiedName CONTAINER_NAME = new FullQualifiedName("ns", "container");

  /** A single PropertyRef record, optionally with an Alias. */
  private static CsdlRecord propertyRef(final String name, final String alias) {
    final List<CsdlPropertyValue> values = new ArrayList<>();
    values.add(new CsdlPropertyValue().setProperty("Name").setValue(new CsdlPropertyPath().setValue(name)));
    if (alias != null) {
      values.add(new CsdlPropertyValue().setProperty("Alias")
          .setValue(new CsdlConstantExpression(ConstantExpressionType.String, alias)));
    }
    return new CsdlRecord().setPropertyValues(values);
  }

  /** An AlternateKey record wrapping the given PropertyRef records. */
  private static CsdlRecord alternateKey(final CsdlRecord... refs) {
    return new CsdlRecord().setPropertyValues(List.of(new CsdlPropertyValue().setProperty("Key")
        .setValue(new CsdlCollection().setItems(List.<CsdlExpression> of(refs)))));
  }

  private static CsdlAnnotation alternateKeys(final String term, final CsdlRecord... groups) {
    return new CsdlAnnotation().setTerm(term)
        .setExpression(new CsdlCollection().setItems(List.<CsdlExpression> of(groups)));
  }

  private static CsdlEntityType entityType(final List<CsdlAnnotation> annotations) {
    final CsdlEntityType entityType = new CsdlEntityType()
        .setName(TYPE_NAME.getName())
        .setKey(List.of(new CsdlPropertyRef().setName("Id")))
        .setProperties(List.of(
            new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName()),
            new CsdlProperty().setName("Code").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new CsdlProperty().setName("Region").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new CsdlProperty().setName("Nr").setType(EdmPrimitiveTypeKind.Int16.getFullQualifiedName()),
            new CsdlProperty().setName("Address").setType(COMPLEX_NAME)));
    if (annotations != null) {
      entityType.setAnnotations(annotations);
    }
    return entityType;
  }

  private static List<CsdlAnnotation> typeLevelAnnotations(final String term) {
    return List.of(alternateKeys(term,
        alternateKey(propertyRef("Code", null)),
        alternateKey(propertyRef("Region", "R"), propertyRef("Nr", null)),
        alternateKey(propertyRef("Address/City", null)),
        new CsdlRecord().setPropertyValues(List.of(new CsdlPropertyValue().setProperty("NotKey")
            .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "x"))))));
  }

  private static CsdlEdmProvider provider(final CsdlEntityType entityType) throws Exception {
    final CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    when(provider.getEntityType(TYPE_NAME)).thenReturn(entityType);
    when(provider.getComplexType(COMPLEX_NAME)).thenReturn(new CsdlComplexType()
        .setName(COMPLEX_NAME.getName())
        .setProperties(List.of(
            new CsdlProperty().setName("City").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()))));
    return provider;
  }

  private static EdmEntityType edmEntityType(final CsdlEdmProvider provider, final CsdlEntityType entityType) {
    return new EdmEntityTypeImpl(new EdmProviderImpl(provider), TYPE_NAME, entityType);
  }

  private static EdmEntitySet edmEntitySet(final CsdlEdmProvider provider, final List<CsdlAnnotation> annotations)
      throws Exception {
    final EdmProviderImpl edm = new EdmProviderImpl(provider);
    final CsdlEntityContainerInfo containerInfo = new CsdlEntityContainerInfo().setContainerName(CONTAINER_NAME);
    when(provider.getEntityContainerInfo(CONTAINER_NAME)).thenReturn(containerInfo);
    final EdmEntityContainer container = new EdmEntityContainerImpl(edm, provider, containerInfo);
    final CsdlEntitySet entitySet = new CsdlEntitySet().setName("ES").setType(TYPE_NAME);
    if (annotations != null) {
      entitySet.setAnnotations(annotations);
    }
    when(provider.getEntitySet(CONTAINER_NAME, "ES")).thenReturn(entitySet);
    return new EdmEntitySetImpl(edm, container, entitySet);
  }

  @Test
  void typeLevelGroupsAreExposedInOrder() throws Exception {
    final CsdlEntityType csdlType = entityType(typeLevelAnnotations("Core.AlternateKeys"));
    final List<EdmAlternateKey> keys = edmEntityType(provider(csdlType), csdlType).getAlternateKeys();
    assertEquals(3, keys.size());
    assertEquals(1, keys.get(0).getPropertyRefs().size());
    assertEquals(2, keys.get(1).getPropertyRefs().size());
    assertEquals(1, keys.get(2).getPropertyRefs().size());
    assertEquals("Code", keys.get(0).getPropertyRefs().get(0).getName());
  }

  @Test
  void aliasDrivesUrlName() throws Exception {
    final CsdlEntityType csdlType = entityType(typeLevelAnnotations("Core.AlternateKeys"));
    final List<EdmAlternateKeyPropertyRef> refs =
        edmEntityType(provider(csdlType), csdlType).getAlternateKeys().get(1).getPropertyRefs();
    assertEquals("Region", refs.get(0).getName());
    assertEquals("R", refs.get(0).getAlias());
    assertEquals("R", refs.get(0).getUrlName());
    assertNotNull(refs.get(0).getProperty());
    assertEquals("Region", refs.get(0).getProperty().getName());
    assertEquals("Nr", refs.get(1).getName());
    assertNull(refs.get(1).getAlias());
    assertEquals("Nr", refs.get(1).getUrlName());
    assertNotNull(refs.get(1).getProperty());
    assertEquals("Nr", refs.get(1).getProperty().getName());
  }

  @Test
  void nestedPathHasNoResolvedProperty() throws Exception {
    final CsdlEntityType csdlType = entityType(typeLevelAnnotations("Core.AlternateKeys"));
    final EdmAlternateKeyPropertyRef ref =
        edmEntityType(provider(csdlType), csdlType).getAlternateKeys().get(2).getPropertyRefs().get(0);
    assertEquals("Address/City", ref.getName());
    assertEquals("Address/City", ref.getUrlName());
    assertNull(ref.getProperty());
  }

  @Test
  void setLevelGroupsAreSeparateFromType() throws Exception {
    final CsdlEntityType csdlType = entityType(typeLevelAnnotations("Core.AlternateKeys"));
    final CsdlEdmProvider provider = provider(csdlType);
    final EdmEntitySet entitySet =
        edmEntitySet(provider, List.of(alternateKeys("Core.AlternateKeys", alternateKey(propertyRef("Nr", null)))));
    final List<EdmAlternateKey> setKeys = entitySet.getAlternateKeys();
    assertEquals(1, setKeys.size());
    assertEquals(1, setKeys.get(0).getPropertyRefs().size());
    assertEquals("Nr", setKeys.get(0).getPropertyRefs().get(0).getName());
    assertNotNull(setKeys.get(0).getPropertyRefs().get(0).getProperty());
    assertEquals(3, entitySet.getEntityType().getAlternateKeys().size());
  }

  @Test
  void unannotatedTypeHasNoAlternateKeys() throws Exception {
    final CsdlEntityType csdlType = entityType(null);
    assertTrue(edmEntityType(provider(csdlType), csdlType).getAlternateKeys().isEmpty());
    assertTrue(edmEntitySet(provider(csdlType), null).getAlternateKeys().isEmpty());
  }

  @Test
  void fullyQualifiedTermNameAlsoMatches() throws Exception {
    final CsdlEntityType csdlType = entityType(typeLevelAnnotations("Org.OData.Core.V1.AlternateKeys"));
    assertEquals(3, edmEntityType(provider(csdlType), csdlType).getAlternateKeys().size());
  }

  /** A PropertyRef record built from raw property values, for the malformed / tolerance cases. */
  private static CsdlRecord rawPropertyRef(final CsdlPropertyValue... values) {
    return new CsdlRecord().setPropertyValues(List.of(values));
  }

  /** An AlternateKey record whose Key property carries the given (possibly malformed) expression. */
  private static CsdlRecord alternateKeyWithKey(final CsdlExpression key) {
    return new CsdlRecord()
        .setPropertyValues(List.of(new CsdlPropertyValue().setProperty("Key").setValue(key)));
  }

  /** Reads the alternate keys of an entity type annotated with the given annotations. */
  private static List<EdmAlternateKey> alternateKeysOf(final List<CsdlAnnotation> annotations) throws Exception {
    final CsdlEntityType csdlType = entityType(annotations);
    return edmEntityType(provider(csdlType), csdlType).getAlternateKeys();
  }

  /** The sentinel group that must survive next to a skipped one. */
  private static CsdlRecord validGroup() {
    return alternateKey(propertyRef("Code", null));
  }

  private static void assertOnlySentinelSurvives(final List<EdmAlternateKey> keys) {
    assertEquals(1, keys.size());
    assertEquals("Code", keys.get(0).getPropertyRefs().get(0).getName());
  }

  @Test
  void keyThatIsNotACollectionIsSkipped() throws Exception {
    assertOnlySentinelSurvives(alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKeyWithKey(new CsdlConstantExpression(ConstantExpressionType.String, "Code")),
        validGroup()))));
  }

  @Test
  void recordWithoutKeyPropertyIsSkipped() throws Exception {
    assertOnlySentinelSurvives(alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        new CsdlRecord().setPropertyValues(List.of(new CsdlPropertyValue().setProperty("NotKey")
            .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "x")))),
        validGroup()))));
  }

  @Test
  void emptyKeyCollectionIsSkipped() throws Exception {
    assertOnlySentinelSurvives(alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKeyWithKey(new CsdlCollection().setItems(List.of())),
        validGroup()))));
  }

  @Test
  void propertyRefWithoutNameInvalidatesTheWholeGroup() throws Exception {
    assertOnlySentinelSurvives(alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKey(propertyRef("Code", null), rawPropertyRef(new CsdlPropertyValue().setProperty("Alias")
            .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "A")))),
        validGroup()))));
  }

  @Test
  void propertyRefWithEmptyNameInvalidatesTheWholeGroup() throws Exception {
    assertOnlySentinelSurvives(alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKey(propertyRef("", null), propertyRef("Nr", null)),
        validGroup()))));
  }

  @Test
  void annotationWithoutCollectionExpressionYieldsNoGroups() throws Exception {
    assertTrue(alternateKeysOf(List.of(new CsdlAnnotation().setTerm("Core.AlternateKeys"))).isEmpty());
    assertTrue(alternateKeysOf(List.of(new CsdlAnnotation().setTerm("Core.AlternateKeys")
        .setExpression(new CsdlConstantExpression(ConstantExpressionType.String, "Code")))).isEmpty());
  }

  @Test
  void unrelatedTermIsIgnored() throws Exception {
    assertTrue(alternateKeysOf(List.of(alternateKeys("Core.Description", validGroup()))).isEmpty());
  }

  @Test
  void constantStringNameIsTolerated() throws Exception {
    final List<EdmAlternateKey> keys = alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKeyWithKey(new CsdlCollection().setItems(List.<CsdlExpression> of(
            rawPropertyRef(new CsdlPropertyValue().setProperty("Name")
                .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "Code")))))))));
    assertEquals(1, keys.size());
    final EdmAlternateKeyPropertyRef ref = keys.get(0).getPropertyRefs().get(0);
    assertEquals("Code", ref.getName());
    assertNotNull(ref.getProperty());
    assertEquals("Code", ref.getProperty().getName());
  }

  @Test
  void pathExpressionNameIsTolerated() throws Exception {
    final List<EdmAlternateKey> keys = alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKeyWithKey(new CsdlCollection().setItems(List.<CsdlExpression> of(
            rawPropertyRef(new CsdlPropertyValue().setProperty("Name")
                .setValue(new CsdlPath().setValue("Code")))))))));
    assertEquals(1, keys.size());
    final EdmAlternateKeyPropertyRef ref = keys.get(0).getPropertyRefs().get(0);
    assertEquals("Code", ref.getName());
    assertNotNull(ref.getProperty());
  }

  @Test
  void ambiguousUrlNamesInOneGroupSkipTheGroup() throws Exception {
    // two references sharing the same alias
    assertOnlySentinelSurvives(alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKey(propertyRef("Region", "X"), propertyRef("Nr", "X")),
        validGroup()))));
    // an alias shadowing another reference's declared property name
    assertOnlySentinelSurvives(alternateKeysOf(List.of(alternateKeys("Core.AlternateKeys",
        alternateKey(propertyRef("Region", "Nr"), propertyRef("Nr", null)),
        validGroup()))));
  }
}
