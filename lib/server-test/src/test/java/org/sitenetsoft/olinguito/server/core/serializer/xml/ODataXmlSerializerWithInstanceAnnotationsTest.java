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
 * Copyright 2026 SiteNetSoft - Added XML instance annotation serialization tests (OLINGO-1581)
 */
package org.sitenetsoft.olinguito.server.core.serializer.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.sitenetsoft.olinguito.commons.api.data.Annotation;
import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL.Suffix;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.EntitySerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.tecsvc.MetadataETagSupport;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Test;

/**
 * Tests for XML instance annotation serialization (OLINGO-1581).
 */
class ODataXmlSerializerWithInstanceAnnotationsTest {

  private static final OData odata = OData.newInstance();
  private static final ServiceMetadata metadata = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList(), new MetadataETagSupport("W/\"metadataETag\""));
  private static final EdmEntityContainer entityContainer = metadata.getEdm().getEntityContainer();

  private final DataProvider data = new DataProvider(odata, metadata.getEdm());
  private final ODataSerializer serializer = new ODataXmlSerializer();

  @Test
  void entityWithPrimitiveInstanceAnnotation() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation = new Annotation();
    annotation.setTerm("com.contoso.display.highlight");
    annotation.setType("Boolean");
    annotation.setValue(ValueType.PRIMITIVE, true);
    entity.getAnnotations().add(annotation);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("m:annotation"),
        "Should contain m:annotation element");
    assertTrue(resultString.contains("com.contoso.display.highlight"),
        "Should contain term 'com.contoso.display.highlight'");
    assertTrue(resultString.contains("#Boolean"),
        "Should contain type '#Boolean'");
    assertTrue(resultString.contains(">true</"),
        "Should contain value 'true'");
    // Verify annotation appears after category element
    int categoryIndex = resultString.indexOf("a:category");
    int annotationIndex = resultString.indexOf("m:annotation");
    assertTrue(annotationIndex > categoryIndex,
        "Annotation should appear after category element");
  }

  @Test
  void entityWithCollectionPrimitiveInstanceAnnotation() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation = new Annotation();
    annotation.setTerm("com.contoso.PersonalInfo.PhoneNumbers");
    annotation.setType("String");
    annotation.setValue(ValueType.COLLECTION_PRIMITIVE,
        List.of("(203)555-1718", "(203)555-1719"));
    entity.getAnnotations().add(annotation);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("com.contoso.PersonalInfo.PhoneNumbers"),
        "Should contain annotation term");
    assertTrue(resultString.contains("#Collection(String)"),
        "Should contain Collection type");
    assertTrue(resultString.contains("m:element"),
        "Should contain m:element elements");
    assertTrue(resultString.contains("(203)555-1718"),
        "Should contain first phone number");
    assertTrue(resultString.contains("(203)555-1719"),
        "Should contain second phone number");
  }

  @Test
  void entityWithComplexInstanceAnnotation() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation = new Annotation();
    annotation.setTerm("com.contoso.display.style");
    annotation.setType("com.contoso.display.styleType");
    List<Property> properties = new ArrayList<>();
    properties.add(new Property("Boolean", "title", ValueType.PRIMITIVE, true));
    properties.add(new Property("Int16", "Order", ValueType.PRIMITIVE, (short) 1));
    ComplexValue complexValue = new ComplexValue();
    complexValue.setTypeName("com.contoso.display.styleType");
    complexValue.getValue().addAll(properties);
    annotation.setValue(ValueType.COMPLEX, complexValue);
    entity.getAnnotations().add(annotation);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("com.contoso.display.style"),
        "Should contain annotation term");
    assertTrue(resultString.contains("#com.contoso.display.styleType"),
        "Should contain complex type");
    assertTrue(resultString.contains("d:title"),
        "Should contain d:title property");
    assertTrue(resultString.contains("d:Order"),
        "Should contain d:Order property");
  }

  @Test
  void entityWithCollectionComplexInstanceAnnotation() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation = new Annotation();
    annotation.setTerm("com.contoso.display.items");
    annotation.setType("com.contoso.display.itemType");

    List<ComplexValue> complexValues = new ArrayList<>();

    ComplexValue cv1 = new ComplexValue();
    cv1.setTypeName("com.contoso.display.itemType");
    cv1.getValue().add(new Property("String", "name", ValueType.PRIMITIVE, "Item1"));
    complexValues.add(cv1);

    ComplexValue cv2 = new ComplexValue();
    cv2.setTypeName("com.contoso.display.itemType");
    cv2.getValue().add(new Property("String", "name", ValueType.PRIMITIVE, "Item2"));
    complexValues.add(cv2);

    annotation.setValue(ValueType.COLLECTION_COMPLEX, complexValues);
    entity.getAnnotations().add(annotation);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("com.contoso.display.items"),
        "Should contain annotation term");
    assertTrue(resultString.contains("#Collection(com.contoso.display.itemType)"),
        "Should contain Collection type");
    assertTrue(resultString.contains("Item1"),
        "Should contain Item1");
    assertTrue(resultString.contains("Item2"),
        "Should contain Item2");
  }

  @Test
  void entityPropertyWithInstanceAnnotation() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation = new Annotation();
    annotation.setTerm("com.contoso.display.style");
    annotation.setType("com.contoso.display.styleType");
    List<Property> properties = new ArrayList<>();
    properties.add(new Property("Boolean", "title", ValueType.PRIMITIVE, true));
    properties.add(new Property("Int16", "Order", ValueType.PRIMITIVE, (short) 1));
    ComplexValue complexValue = new ComplexValue();
    complexValue.setTypeName("com.contoso.display.styleType");
    complexValue.getValue().addAll(properties);
    annotation.setValue(ValueType.COMPLEX, complexValue);

    Property property = entity.getProperty("PropertyString");
    property.getAnnotations().add(annotation);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("com.contoso.display.style"),
        "Should contain annotation term inside property");
    // Annotation should be within the d:PropertyString element
    int propStart = resultString.indexOf("<d:PropertyString");
    int propEnd = resultString.indexOf("</d:PropertyString>");
    int annotIndex = resultString.indexOf("m:annotation", propStart);
    assertTrue(annotIndex > propStart && annotIndex < propEnd,
        "Annotation should be inside the PropertyString element");
  }

  @Test
  void entityWithComplexPropertyAnnotation() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation = new Annotation();
    annotation.setTerm("com.contoso.display.highlight");
    annotation.setType("Boolean");
    annotation.setValue(ValueType.PRIMITIVE, true);

    Property property = entity.getProperty("PropertyComp");
    property.getAnnotations().add(annotation);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("com.contoso.display.highlight"),
        "Should contain annotation on complex property");
    int propStart = resultString.indexOf("<d:PropertyComp");
    int propEnd = resultString.indexOf("</d:PropertyComp>");
    int annotIndex = resultString.indexOf("m:annotation", propStart);
    assertTrue(annotIndex > propStart && annotIndex < propEnd,
        "Annotation should be inside the PropertyComp element");
  }

  @Test
  void entityWithMultipleAnnotations() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation1 = new Annotation();
    annotation1.setTerm("com.contoso.display.highlight");
    annotation1.setType("Boolean");
    annotation1.setValue(ValueType.PRIMITIVE, true);
    entity.getAnnotations().add(annotation1);

    Annotation annotation2 = new Annotation();
    annotation2.setTerm("com.contoso.display.order");
    annotation2.setType("Int32");
    annotation2.setValue(ValueType.PRIMITIVE, 42);
    entity.getAnnotations().add(annotation2);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("com.contoso.display.highlight"),
        "Should contain first annotation");
    assertTrue(resultString.contains("com.contoso.display.order"),
        "Should contain second annotation");
    assertTrue(resultString.contains(">42</"),
        "Should contain value 42");
  }

  @Test
  void entityWithNoAnnotationsDoesNotBreak() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("a:entry"),
        "Should contain entry element");
    assertFalse(resultString.contains("m:annotation"),
        "Should not contain m:annotation when no annotations exist");
  }

  @Test
  void entityWithNestedComplexAnnotation() throws Exception {
    final EdmEntitySet edmEntitySet = entityContainer.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(edmEntitySet).getEntities().get(0);

    Annotation annotation = new Annotation();
    annotation.setTerm("com.contoso.display.style");
    annotation.setType("com.contoso.display.styleType");

    List<Property> properties = new ArrayList<>();
    properties.add(new Property("Boolean", "title", ValueType.PRIMITIVE, true));

    List<ComplexValue> orderValues = new ArrayList<>();
    ComplexValue orderValue = new ComplexValue();
    orderValue.setTypeName("com.contoso.display.orderDetails");
    orderValue.getValue().add(new Property("String", "name", ValueType.PRIMITIVE, "Cars"));
    orderValue.getValue().add(new Property("String", "brand", ValueType.PRIMITIVE, "BMW"));
    orderValues.add(orderValue);
    properties.add(new Property("Order", "Order", ValueType.COLLECTION_COMPLEX, orderValues));

    ComplexValue complexValue = new ComplexValue();
    complexValue.setTypeName("com.contoso.display.styleType");
    complexValue.getValue().addAll(properties);
    annotation.setValue(ValueType.COMPLEX, complexValue);
    entity.getAnnotations().add(annotation);

    InputStream result = serializer.entity(metadata, edmEntitySet.getEntityType(), entity,
        EntitySerializerOptions.with()
            .contextURL(ContextURL.with().entitySet(edmEntitySet).suffix(Suffix.ENTITY).build())
            .build()).getContent();
    final String resultString = new String(result.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(resultString.contains("com.contoso.display.style"),
        "Should contain annotation term");
    assertTrue(resultString.contains("d:title"),
        "Should contain nested d:title");
    assertTrue(resultString.contains("d:Order"),
        "Should contain nested d:Order");
    assertTrue(resultString.contains("Cars"),
        "Should contain nested name value 'Cars'");
    assertTrue(resultString.contains("BMW"),
        "Should contain nested brand value 'BMW'");
  }
}
