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
 * Copyright 2026 SiteNetSoft - Replaced Apache Commons with Java standard library
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON conformant $EntityContainer, flat $Extends,
 * structural container children (no $Kind) and served $Version
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON facet defaults ($Nullable polarity,
 * omitted $Type for Edm.String, numeric enum values and type-definition facets, $OnDelete,
 * single $ReferentialConstraint object)
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON bare-value constant expressions and
 * record @type control information
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: pinned $OpenType on entity and complex types
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: pinned null-safe numeric and section 14.3.7 enumeration
 * member constants
 */
package org.sitenetsoft.olinguito.server.core.serializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmMember;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSchema;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTerm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression.EdmExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAliasInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumMember;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDelete;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDeleteAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReferentialConstraint;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlAnnotationPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlApply;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCast;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElementReference;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNull;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlUrlRef;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceInclude;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceIncludeAnnotation;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.ServiceMetadataImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MetadataDocumentJsonSerializerTest {

  private static ODataSerializer serializer;
  
  @BeforeAll
  static void init() throws SerializerException {
    serializer = OData.newInstance().createSerializer(ContentType.APPLICATION_JSON);
  }
  
  @Test
  void writeMetadataWithEmptyMockedEdm() throws Exception {
    final Edm edm = mock(Edm.class);
    ServiceMetadata metadata = mock(ServiceMetadata.class);
    when(metadata.getEdm()).thenReturn(edm);

    assertEquals("{\"$Version\":\"4.0\"}",
        new String(serializer.metadataDocument(metadata).getContent().readAllBytes(), StandardCharsets.UTF_8));
  }
  
  @Test
  void writeEdmxWithLocalTestEdm() throws Exception {
    List<EdmxReference> edmxReferences = new ArrayList<>();
    EdmxReference reference = new EdmxReference(URI.create("http://example.com"));
    edmxReferences.add(reference);

    EdmxReference referenceWithInclude = new EdmxReference(
        URI.create("http://localhost/odata/odata/v4.0/referenceWithInclude"));
    EdmxReferenceInclude include = new EdmxReferenceInclude("Org.OData.Core.V1", "Core");
    referenceWithInclude.addInclude(include);
    edmxReferences.add(referenceWithInclude);

    EdmxReference referenceWithTwoIncludes = new EdmxReference(
        URI.create("http://localhost/odata/odata/v4.0/referenceWithTwoIncludes"));
    referenceWithTwoIncludes.addInclude(new EdmxReferenceInclude("Org.OData.Core.2", "Core2"));
    referenceWithTwoIncludes.addInclude(new EdmxReferenceInclude("Org.OData.Core.3", "Core3"));
    edmxReferences.add(referenceWithTwoIncludes);

    EdmxReference referenceWithIncludeAnnos = new EdmxReference(
        URI.create("http://localhost/odata/odata/v4.0/referenceWithIncludeAnnos"));
    referenceWithIncludeAnnos.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("TermNs.2", "Q.2", "TargetNS.2"));
    referenceWithIncludeAnnos.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("TermNs.3", "Q.3", "TargetNS.3"));
    edmxReferences.add(referenceWithIncludeAnnos);

    EdmxReference referenceWithAll = new EdmxReference(
        URI.create("http://localhost/odata/odata/v4.0/referenceWithAll"));
    referenceWithAll.addInclude(new EdmxReferenceInclude("ReferenceWithAll.1", "Core1"));
    referenceWithAll.addInclude(new EdmxReferenceInclude("ReferenceWithAll.2", "Core2"));
    referenceWithAll.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("ReferenceWithAllTermNs.4", "Q.4", "TargetNS.4"));
    referenceWithAll.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("ReferenceWithAllTermNs.5", "Q.5", "TargetNS.5"));
    edmxReferences.add(referenceWithAll);

    EdmxReference referenceWithAllAndNull = new EdmxReference(
        URI.create("http://localhost/odata/odata/v4.0/referenceWithAllAndNull"));
    referenceWithAllAndNull.addInclude(new EdmxReferenceInclude("referenceWithAllAndNull.1"));
    referenceWithAllAndNull.addInclude(new EdmxReferenceInclude("referenceWithAllAndNull.2", null));
    referenceWithAllAndNull.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("ReferenceWithAllTermNs.4"));
    referenceWithAllAndNull.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("ReferenceWithAllTermAndNullNs.5", "Q.5", null));
    referenceWithAllAndNull.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("ReferenceWithAllTermAndNullNs.6", null, "TargetNS"));
    referenceWithAllAndNull.addIncludeAnnotation(
        new EdmxReferenceIncludeAnnotation("ReferenceWithAllTermAndNullNs.7", null, null));
    edmxReferences.add(referenceWithAllAndNull);

    ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    final Edm edm = mock(Edm.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);
    when(serviceMetadata.getReferences()).thenReturn(edmxReferences);

    InputStream metadata = serializer.metadataDocument(serviceMetadata).getContent();
    assertNotNull(metadata);
    final String metadataString = new String(metadata.readAllBytes(), StandardCharsets.UTF_8);
    // edmx reference
    assertTrue(metadataString.contains(
        "\"$Reference\":{\"http://example.com\":{},"));
    assertTrue(metadataString.contains("\"http://localhost/odata/odata/v4.0/referenceWithInclude\":"
        + "{\"$Include\":[{\"$Namespace\":\"Org.OData.Core.V1\",\"$Alias\":\"Core\"}]}"));
    assertTrue(metadataString.contains(
        "\"http://localhost/odata/odata/v4.0/referenceWithTwoIncludes\":"
        + "{\"$Include\":["
        + "{\"$Namespace\":\"Org.OData.Core.2\",\"$Alias\":\"Core2\"},"
        + "{\"$Namespace\":\"Org.OData.Core.3\",\"$Alias\":\"Core3\"}]}"));
    assertTrue(metadataString.contains(
        "\"http://localhost/odata/odata/v4.0/referenceWithIncludeAnnos\":"
        + "{\"$IncludeAnnotations\":"
        + "[{\"$TermNamespace\":\"TermNs.2\",\"$Qualifier\":\"Q.2\","
        + "\"$TargetNamespace\":\"TargetNS.2\"},"
        + "{\"$TermNamespace\":\"TermNs.3\",\"$Qualifier\":\"Q.3\","
        + "\"$TargetNamespace\":\"TargetNS.3\"}]}"));
    assertTrue(metadataString.contains(
        "\"http://localhost/odata/odata/v4.0/referenceWithAll\":"
        + "{\"$Include\":[{\"$Namespace\":\"ReferenceWithAll.1\","
        + "\"$Alias\":\"Core1\"},"
        + "{\"$Namespace\":\"ReferenceWithAll.2\",\"$Alias\":\"Core2\"}],"
        + "\"$IncludeAnnotations\":"
        + "[{\"$TermNamespace\":\"ReferenceWithAllTermNs.4\",\"$Qualifier\":\"Q.4\","
        + "\"$TargetNamespace\":\"TargetNS.4\"},"
        + "{\"$TermNamespace\":\"ReferenceWithAllTermNs.5\",\"$Qualifier\":\"Q.5\","
        + "\"$TargetNamespace\":\"TargetNS.5\"}]}"));
    assertTrue(metadataString.contains(
        "\"http://localhost/odata/odata/v4.0/referenceWithAllAndNull\":"
        + "{\"$Include\":[{\"$Namespace\":\"referenceWithAllAndNull.1\"},"
        + "{\"$Namespace\":\"referenceWithAllAndNull.2\"}],\"$IncludeAnnotations\":"
        + "[{\"$TermNamespace\":\"ReferenceWithAllTermNs.4\"},"
        + "{\"$TermNamespace\":\"ReferenceWithAllTermAndNullNs.5\",\"$Qualifier\":\"Q.5\"},"
        + "{\"$TermNamespace\":\"ReferenceWithAllTermAndNullNs.6\","
        + "\"$TargetNamespace\":\"TargetNS\"},"
        + "{\"$TermNamespace\":\"ReferenceWithAllTermAndNullNs.7\"}]}"));
  }
  
  /** Test if annotations on EnumType Members are added as children of the Member element
   *  in compliance with OData v4.01, section 10
   */
  @Test
  void testAnnotationsNestedInEnumMembers() throws Exception {
    // Create mock schema
    EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));
    
    // create mock metadata
    ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);
    
    // add mock enums to schema
    EdmEnumType enumType = mock(EdmEnumType.class);
    when(schema.getEnumTypes()).thenReturn(Collections.singletonList(enumType));
    when(enumType.getName()).thenReturn("MyEnum");
    when(enumType.getKind()).thenReturn(EdmTypeKind.ENUM);
    EdmPrimitiveType int32Type = OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32);
    when(enumType.getUnderlyingType()).thenReturn(int32Type);
    
    // mock enum member values
    when(enumType.getMemberNames()).thenReturn(Collections.singletonList("MyMember"));
    EdmMember member = mock(EdmMember.class);
    when(enumType.getMember("MyMember")).thenReturn(member);
    when(member.getName()).thenReturn("MyMember");
    when(member.getValue()).thenReturn("0");
    
    EdmAnnotation annotation = mock(EdmAnnotation.class);
    when(member.getAnnotations()).thenReturn(Collections.singletonList(annotation));
    when(annotation.getQualifier()).thenReturn("Core.Description");
    EdmConstantExpression expression = mock(EdmConstantExpression.class);
    when(expression.isConstant()).thenReturn(true);
    when(expression.asConstant()).thenReturn(expression);
    when(expression.getExpressionType()).thenReturn(EdmExpressionType.String);
    when(expression.getExpressionName()).thenReturn("String");
    when(expression.getValueAsString()).thenReturn("MyDescription");
    when(annotation.getExpression()).thenReturn(expression);
    
    InputStream metadata = serializer.metadataDocument(serviceMetadata).getContent();
    assertNotNull(metadata);
    String metadataString = new String(metadata.readAllBytes(), StandardCharsets.UTF_8);


    assertTrue(metadataString.contains(
        "{\"$Version\":\"4.0\","
        + "\"MyNamespace\":{\"MyEnum\":"
        + "{\"$Kind\":\"EnumType\","
        + "\"$UnderlyingType\":\"Edm.Int32\",\"MyMember\":0,"
        + "\"MyMember#Core.Description\":\"MyDescription\"}}}"));

  }

  /**
   * Section 10.3: "Each member MUST specify an associated numeric value." A model that violates it can
   * no longer be serialized, and the failure is reported through this module's error contract -- an
   * unchecked NumberFormatException would escape {@code ODataJsonSerializer#metadataDocument}, which
   * catches only IOException.
   */
  @Test
  void nonNumericEnumMemberValueIsReportedAsSerializerException() {
    EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));

    ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);

    EdmEnumType enumType = mock(EdmEnumType.class);
    when(schema.getEnumTypes()).thenReturn(Collections.singletonList(enumType));
    when(enumType.getName()).thenReturn("MyEnum");
    when(enumType.getFullQualifiedName()).thenReturn(new FullQualifiedName("MyNamespace", "MyEnum"));
    when(enumType.getKind()).thenReturn(EdmTypeKind.ENUM);
    when(enumType.getUnderlyingType())
        .thenReturn(OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32));
    when(enumType.getMemberNames()).thenReturn(Collections.singletonList("MyMember"));
    EdmMember member = mock(EdmMember.class);
    when(enumType.getMember("MyMember")).thenReturn(member);
    when(member.getName()).thenReturn("MyMember");
    when(member.getValue()).thenReturn("notANumber");

    final SerializerException exception = assertThrows(SerializerException.class,
        () -> serializer.metadataDocument(serviceMetadata).getContent());
    assertEquals(SerializerException.MessageKeys.WRONG_PROPERTY_VALUE, exception.getMessageKey());
  }

  @Test
  void annotationWithUnresolvableTermFallsBackToRawName() throws Exception {
    EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));

    ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);

    EdmEnumType enumType = mock(EdmEnumType.class);
    when(schema.getEnumTypes()).thenReturn(Collections.singletonList(enumType));
    when(enumType.getName()).thenReturn("MyEnum");
    when(enumType.getKind()).thenReturn(EdmTypeKind.ENUM);
    EdmPrimitiveType int32Type = OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32);
    when(enumType.getUnderlyingType()).thenReturn(int32Type);
    when(enumType.getMemberNames()).thenReturn(Collections.singletonList("MyMember"));
    EdmMember member = mock(EdmMember.class);
    when(enumType.getMember("MyMember")).thenReturn(member);
    when(member.getName()).thenReturn("MyMember");
    when(member.getValue()).thenReturn("0");

    EdmAnnotation annotation = mock(EdmAnnotation.class);
    when(member.getAnnotations()).thenReturn(Collections.singletonList(annotation));
    // The term's vocabulary is not part of the served metadata, so getTerm() cannot resolve it,
    // but the raw term name is still available and must be emitted (OLINGO-1399).
    when(annotation.getTerm()).thenReturn(null);
    when(annotation.getTermName()).thenReturn("my.vocab.Unresolved");
    EdmConstantExpression expression = mock(EdmConstantExpression.class);
    when(expression.isConstant()).thenReturn(true);
    when(expression.asConstant()).thenReturn(expression);
    when(expression.getExpressionType()).thenReturn(EdmExpressionType.String);
    when(expression.getExpressionName()).thenReturn("String");
    when(expression.getValueAsString()).thenReturn("MyDescription");
    when(annotation.getExpression()).thenReturn(expression);

    InputStream metadata = serializer.metadataDocument(serviceMetadata).getContent();
    String metadataString = new String(metadata.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(metadataString.contains("\"MyMember@my.vocab.Unresolved\":\"MyDescription\""));
    // The member name must not be emitted with an empty term ("MyMember@").
    assertFalse(metadataString.contains("\"MyMember@\":"));
  }

  /** Writes simplest (empty) Schema. */
  @Test
  void writeMetadataWithEmptySchema() throws Exception {
    EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));
    ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);

    InputStream metadata = serializer.metadataDocument(serviceMetadata).getContent();
    assertNotNull(metadata);
    assertEquals("{\"$Version\":\"4.0\",\"MyNamespace\":{}}",
        new String(metadata.readAllBytes(), StandardCharsets.UTF_8));
  }
  
  @Test
  void testNullMetadata() throws Exception {
      assertThrows(SerializerException.class, () -> serializer.metadataDocument(null).getContent());
  }
  
  @Test
  void testNullEdm() throws Exception {
      assertThrows(SerializerException.class, () -> {
          ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
          when(serviceMetadata.getEdm()).thenReturn(null);
          serializer.metadataDocument(serviceMetadata).getContent();
      });
  }
  
  @Test
  void writeMetadataWithTypeDefinitions() throws Exception {
    EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));
    EdmTypeDefinition typeDefinition = mock(EdmTypeDefinition.class);
    when (schema.getTypeDefinitions()).thenReturn(Collections.singletonList(typeDefinition));
    when(typeDefinition.getMaxLength()).thenReturn(10);
    when(typeDefinition.getScale()).thenReturn(2);
    when(typeDefinition.getPrecision()).thenReturn(10);
    when(typeDefinition.getSrid()).thenReturn(SRID.valueOf("123"));
    when(typeDefinition.getName()).thenReturn("MyTypeDefinition");
    when(typeDefinition.getKind()).thenReturn(EdmTypeKind.DEFINITION);
    EdmPrimitiveType int32Type = OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32);
    when(typeDefinition.getUnderlyingType()).thenReturn(int32Type);
    
    EdmAnnotation annotation = mock(EdmAnnotation.class);
    when(typeDefinition.getAnnotations()).thenReturn(Collections.singletonList(annotation));
    EdmTerm term = mock(EdmTerm.class);
    when(term.getName()).thenReturn("Unit");
    when(term.getFullQualifiedName()).thenReturn(new FullQualifiedName("Measures", "Unit"));
    when(annotation.getTerm()).thenReturn(term);
    EdmConstantExpression expression = mock(EdmConstantExpression.class);
    when(expression.isConstant()).thenReturn(true);
    when(expression.asConstant()).thenReturn(expression);
    when(expression.getExpressionType()).thenReturn(EdmExpressionType.String);
    when(expression.getExpressionName()).thenReturn("String");
    when(expression.getValueAsString()).thenReturn("Centimeters");
    when(annotation.getExpression()).thenReturn(expression);
    
    ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);
    
    InputStream metadata = serializer.metadataDocument(serviceMetadata).getContent();
    assertNotNull(metadata);
    String metadataStr = new String(metadata.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals("{\"$Version\":\"4.0\","
        + "\"MyNamespace\":"
        + "{\"MyTypeDefinition\":{"
        + "\"$Kind\":\"TypeDefinition\","
        + "\"$UnderlyingType\":\"Edm.Int32\","
        + "\"$MaxLength\":10,\"$Precision\":10,"
        + "\"$Scale\":2,\"$SRID\":\"123\","
        + "\"@Measures.Unit\":\"Centimeters\"}}}",
        metadataStr);
  }
  
  @Test
  void aliasTest() throws Exception {
    String metadata = localMetadata();
    assertTrue(metadata.contains("\"ENString\":{\"$Kind\":\"EnumType\",\"$IsFlags\":true,"
        + "\"$UnderlyingType\":\"Edm.Int16\",\"String1\":1,"
        + "\"String1@Core.Description#Target\":\"Description of Enum Member\"}"));
    assertTrue(metadata.contains("\"ETAbstract\":{\"$Kind\":\"EntityType\",\"$Abstract\":true,"
        + "\"PropertyString\":{\"$Nullable\":true},\"NavPropertyETTwoKeyNavOne\":"
        + "{\"$Kind\":\"NavigationProperty\",\"$Type\":\"Alias.ETTwoKeyNavOne\","
        + "\"$Nullable\":true}},"
        + "\"ETAbstractBase\":{\"$Kind\":\"EntityType\",\"$BaseType\":\"Alias.ETAbstract\","
        + "\"$Key\":[\"PropertyInt16\"],\"PropertyInt16\":{\"$Type\":\"Edm.Int16\","
        + "\"@Core.Description#Target\":\"Description of Type\"},"
        + "\"@Core.Description#Target\":\"Description of Type\"}"));
    assertTrue(metadata.contains("\"CTTwoPrim\":{\"$Kind\":\"ComplexType\","
        + "\"$Abstract\":true,\"PropertyInt16\":"
        + "{\"$Type\":\"Edm.Int16\","
        + "\"@Core.Description#Target\":\"Description of Type\"},"
        + "\"PropertyString\":{\"$Nullable\":true}},"
        + "\"CTTwoPrimBase\":{\"$Kind\":\"ComplexType\",\"$BaseType\":\"Alias.CTTwoPrim\","
        + "\"@Core.Description#Target\":\"Description of Complex Type\"}"));
    assertTrue(metadata.contains("\"ET\":{\"$Kind\":\"EntityType\","
        + "\"$Key\":[{\"EntityInfoID\":\"Info/ID\"},\"name\"],"
        + "\"name\":{\"$Nullable\":true"
        + "},\"Info\":"
        + "{\"$Type\":\"Alias.CTEntityInfo\",\"$Nullable\":true},"
        + "\"NavPropertyETOne\":{\"$Kind\":\"NavigationProperty\","
        + "\"$Type\":\"Alias.ETOne\",\"$Nullable\":true},"
        + "\"NavProperty\":{\"$Kind\":\"NavigationProperty\","
        + "\"$Type\":\"Alias.ETAbstract\","
        + "\"$OnDelete\":\"Cascade\",\"$OnDelete@core.Term\":true}}"));
    assertTrue(metadata.contains("\"BAETTwoKeyNavRTETTwoKeyNavParam\":"
        + "[{\"$Kind\":\"Action\",\"$EntitySetPath\":\"BindingParam/NavPropertyETTwoKeyNavOne\","
        + "\"$IsBound\":true,\"$Parameter\":[{\"$Name\":\"BindingParam\",\"$Type\":\"Alias.ETTwoKeyNav\","
        + "\"$Nullable\":true},"
        + "{\"$Name\":\"PropertyComp\",\"$Type\":\"Alias.CTPrimComp\",\"$Nullable\":true}],"
        + "\"$ReturnType\":"
        + "{\"$Type\":\"Alias.ETTwoKeyNav\",\"$Collection\":true}},"
        + "{\"$Kind\":\"Action\","
        + "\"$EntitySetPath\":\"BindingParam/NavPropertyET\",\"$IsBound\":true,\"$Parameter\":"
        + "[{\"$Name\":\"BindingParam\",\"$Type\":\"Alias.ET\",\"$Nullable\":true}],"
        + "\"$ReturnType\":{\"$Type\":"
        + "\"Alias.ET\"}},{\"$Kind\":\"Action\","
        + "\"$Parameter\":[{\"$Name\":\"PropertyComp\",\"$Type\":\"Alias.CTPrimComp\","
        + "\"$Nullable\":true}],"
        + "\"$ReturnType\":{\"$Type\":\"Alias.ET\"}}]"));
    assertTrue(metadata.contains("\"UARTPrimParam\":[{\"$Kind\":\"Action\","
        + "\"$Parameter\":[{\"$Name\":\"ParameterInt16\","
        + "\"$Type\":\"Edm.Int16\",\"$Nullable\":true}],"
        + "\"$ReturnType\":{\"$Type\":\"Edm.String\",\"$Nullable\":true}}]"));
    assertTrue(metadata.contains("\"UFNRTInt16\":"
        + "[{\"$Kind\":\"Function\","
        + "\"$ReturnType\":{\"$Type\":\"Edm.Int16\",\"$Nullable\":true}}]"));
    assertTrue(metadata.contains("\"BFETTwoKeyNavRTETTwoKeyNavParam\":"
        + "[{\"$Kind\":\"Function\",\"$EntitySetPath\":"
        + "\"BindingParam/NavPropertyETTwoKeyNavOne\",\"$IsBound\":true,"
        + "\"$IsComposable\":true,\"$Parameter\":[{\"$Name\":\"BindingParam\","
        + "\"$Type\":\"Alias.ETTwoKeyNav\",\"$Nullable\":true},{\"$Name\":\"PropertyComp\","
        + "\"$Type\":\"Alias.CTPrimComp\",\"$Nullable\":true}],\"$ReturnType\":{\"$Type\":"
        + "\"Alias.ETTwoKeyNav\",\"$Collection\":true}},"
        + "{\"$Kind\":\"Function\","
        + "\"$EntitySetPath\":\"BindingParam/NavPropertyET\",\"$IsBound\":true,"
        + "\"$Parameter\":[{\"$Name\":\"BindingParam\",\"$Type\":\"Alias.ET\","
        + "\"$Nullable\":true}],"
        + "\"$ReturnType\":{\"$Type\":\"Alias.ET\"}}]"));
    assertTrue(metadata.contains("\"term\":{\"$Kind\":\"Term\",\"$Type\":\"Edm.String\","
        + "\"$Nullable\":true},"
        + "\"Term1\":{\"$Kind\":\"Term\",\"$Type\":\"Edm.String\",\"$Nullable\":true},"
        + "\"Term2\":{\"$Kind\":\"Term\",\"$Type\":\"Edm.String\","
        + "\"$DefaultValue\":\"default\",\"$MaxLength\":1,"
        + "\"$Precision\":2,\"$Scale\":3},"
        + "\"Term3\":{\"$Kind\":\"Term\",\"$Type\":\"Edm.String\","
        + "\"$AppliesTo\":\"Property EntitySet Schema\",\"$Nullable\":true},"
        + "\"Term4\":{\"$Kind\":\"Term\",\"$Type\":\"Edm.String\",\"$BaseTerm\":\"Alias.Term1\","
        + "\"$Nullable\":true}"));
    assertTrue(metadata.contains("\"ESTwoKeyNav\":{\"$Collection\":true,"
        + "\"$Type\":\"Alias.ETTwoKeyNav\",\"$NavigationPropertyBinding\":{"
        + "\"NavPropertyETTwoKeyNavOne/namespace.ETOne/NavPropertyET\":\"ES\","
        + "\"NavPropertyETOne\":\"ESOne\"}}"));
    assertTrue(metadata.contains("\"SIBinding\":{"
        + "\"$Type\":\"Alias.ET\",\"$NavigationPropertyBinding\":{\"NavPropertyETOne\":\"ESOne\"}}"));
    assertTrue(metadata.contains("\"AIRTPrimParam\":{"
        + "\"$Action\":\"Alias.UARTPrimParam\",\"$EntitySet\":\"ESTwoKeyNav\"}"));
    assertTrue(metadata.contains("\"FINRTInt16\":{"
        + "\"$Function\":\"Alias.UFNRTInt16\",\"$EntitySet\":\"ESTwoKeyNavOne\","
        + "\"$IncludeInServiceDocument\":true}"));
    assertTrue(metadata.contains("\"ETTwoKeyNavOne\":{\"$Kind\":\"EntityType\","
        + "\"$HasStream\":true,\"$BaseType\":\"Alias.ETOne\","
        + "\"PropertyString\":{\"$Nullable\":true},"
        + "\"NavPropertyETAbstract\":{\"$Kind\":\"NavigationProperty\","
        + "\"$Type\":\"Alias.ETAbstract\",\"$Collection\":true,"
        + "\"$Partner\":"
        + "\"NavPropertyETTwoKeyNavOne\",\"$ContainsTarget\":true,\"$ReferentialConstraint\":"
        + "{\"PropertyString\":\"PropertyString\","
        + "\"PropertyString@Core.Description#Target\":\"Description of Complex Type\","
        + "\"PropertyInt16\":\"PropertyInt16\"}}}"));
    assertTrue(metadata.contains("\"$Annotations\":{\"Alias.ETAbstract#Tablett\":"
        + "{\"@ns.term#T1\":\"qrvM3e7_\","
        + "\"@ns.term#T2\":true,"
        + "\"@ns.term#T3\":\"2012-02-29\","
        + "\"@ns.term#T4\":\"2012-02-29T01:02:03Z\","
        + "\"@ns.term#T5\":-12345678901234567234567890,"
        + "\"@ns.term#T6\":\"PT10S\","
        + "\"@ns.term#T7\":\"enumMember\","
        + "\"@ns.term#T8\":1.42,"
        + "\"@ns.term#T9\":\"aabbccdd-aabb-ccdd-eeff-aabbccddeeff\","
        + "\"@ns.term#T10\":42,\"@ns.term#T11\":\"ABCD\","
        + "\"@ns.term#T12\":\"00:00:00.999\","
        + "\"@ns.term#T13\":{\"$And\":[true,false],\"@ns.term\":true},"
        + "\"@ns.term#T14\":{\"$Or\":[true,false],\"@ns.term\":true}"));
    assertTrue(metadata.contains("\"@ns.term#T15\":{\"$Eq\":[true,false],\"@ns.term\":true},"
        + "\"@ns.term#T16\":{\"$Ne\":[true,false],\"@ns.term\":true},"
        + "\"@ns.term#T17\":{\"$Gt\":[true,false],\"@ns.term\":true},"
        + "\"@ns.term#T18\":{\"$Ge\":[true,false],\"@ns.term\":true},"
        + "\"@ns.term#T19\":{\"$Lt\":[true,false],\"@ns.term\":true},"
        + "\"@ns.term#T20\":{\"$Le\":[true,false],\"@ns.term\":true},"
        + "\"@ns.term#T21\":{\"$Path\":\"AnnoPathValue\"},"
        + "\"@ns.term#T22\":{\"$Apply\":[true],\"$Function\":\"odata.concat\",\"@ns.term\":true}"));
    assertTrue(metadata.contains("\"@ns.term#T23\":[true,false,\"String\"],"
        + "\"@ns.term#T24\":{\"$If\":[true,\"Then\",\"Else\"],\"@ns.term\":true},"
        + "\"@ns.term#T25\":{\"$LabeledElementReference\":\"LabeledElementReferenceValue\"},"
        + "\"@ns.term#T26\":{\"$Null\":null,\"@ns.term\":true},"
        + "\"@ns.term#T27\":{\"$NavigationPropertyPath\":\"NavigationPropertyPathValue\"},"
        + "\"@ns.term#T28\":{\"$Path\":\"PathValue\"},"
        + "\"@ns.term#T29\":{\"$PropertyPath\":\"PropertyPathValue\"}"));
    assertTrue(metadata.contains("\"@ns.term#T30\":{\"$Not\":true,\"@ns.term\":true}"));
    assertTrue(metadata.contains("\"@ns.term#T300\":{\"$Cast\":\"value\","
        + "\"$Type\":\"Edm.String\",\"$MaxLength\":1,\"$Precision\":2,"
        + "\"$Scale\":3,\"@ns.term\":true}"));
    assertTrue(metadata.contains("\"@ns.term#T31\":{\"$IsOf\":\"value\",\"$Type\":\"Edm.String\","
        + "\"$MaxLength\":1,\"$Precision\":2,\"$Scale\":3,\"@ns.term\":true}"));
    assertTrue(metadata.contains("\"@ns.term#T32\":{\"$LabeledElement\":\"value\","
        + "\"$Name\":\"NameAtt\",\"@ns.term\":true}"));
    assertTrue(metadata.contains("\"@ns.term#T33\":{\"@type\":\"#Alias.ETAbstract\","
        + "\"PropName\":\"value\",\"PropName@ns.term\":true,\"@ns.term\":true},"
        + "\"@ns.term#T34\":{\"$UrlRef\":\"URLRefValue\",\"@ns.term\":true}"));
  }

  /**
   * CSDL JSON section 7.2.1: "The value of $Nullable is one of the Boolean literals true or false.
   * Absence of the member means false." This is the exact inverse of the CSDL XML attribute, whose
   * absence means true - writing XML polarity into a JSON document silently turns every nullable
   * property into a non-nullable one for any conformant reader.
   */
  @Test
  void nullableIsWrittenWithJsonPolarity() throws Exception {
    final String metadata = localMetadata();
    assertTrue(metadata.contains("\"Info\":{\"$Type\":\"Alias.CTEntityInfo\",\"$Nullable\":true}"),
        "a nullable property must say so explicitly");
    assertTrue(metadata.contains("\"PropertyInt16\":{\"$Type\":\"Edm.Int16\","),
        "a non-nullable property omits the member entirely");
    assertFalse(metadata.contains("\"$Nullable\":false"),
        "false is the default and must never be written");
  }

  /**
   * Section 7.1: "Absence of the $Type member means the type is Edm.String. This member SHOULD be
   * omitted for string properties to reduce document size." The reader applies the same default, so
   * nothing is lost - Task 5's parser pins the reading side.
   */
  @Test
  void stringPropertiesOmitTheirType() throws Exception {
    final String metadata = localMetadata();
    assertTrue(metadata.contains("\"PropertyString\":{\"$Nullable\":true}"),
        "a nullable string property carries neither $Type nor $Kind");
    // The two negatives name every Edm.String structural property LocalProvider declares; they are
    // fixture-specific by construction and must be extended when the fixture grows a new one.
    assertFalse(metadata.contains("\"PropertyString\":{\"$Type\":\"Edm.String\""),
        "no structural property writes the default type");
    assertFalse(metadata.contains("\"name\":{\"$Type\":\"Edm.String\""),
        "no structural property writes the default type");
    assertTrue(metadata.contains("\"$ReturnType\":{\"$Type\":\"Edm.String\",\"$Nullable\":true}"),
        "return types keep $Type - section 12.8 states the default but no SHOULD-omit rule");
  }

  /**
   * Section 6.3 (entity types) and section 9.3 (complex types): "The value of $OpenType is one of the
   * Boolean literals true or false. Absence of the member means false." The member was never written,
   * so an open type was indistinguishable from a closed one on the CSDL JSON wire.
   */
  @Test
  void openTypesDeclareThemselves() throws Exception {
    final EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("ns");
    final Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));
    final ServiceMetadata metadata = mock(ServiceMetadata.class);
    when(metadata.getEdm()).thenReturn(edm);

    final EdmEntityType openEntity = mock(EdmEntityType.class);
    when(openEntity.getName()).thenReturn("ETOpen");
    when(openEntity.isOpenType()).thenReturn(true);
    final EdmEntityType closedEntity = mock(EdmEntityType.class);
    when(closedEntity.getName()).thenReturn("ETClosed");
    when(schema.getEntityTypes()).thenReturn(List.of(openEntity, closedEntity));

    final EdmComplexType openComplex = mock(EdmComplexType.class);
    when(openComplex.getName()).thenReturn("CTOpen");
    when(openComplex.isOpenType()).thenReturn(true);
    when(schema.getComplexTypes()).thenReturn(List.of(openComplex));

    final String document = new String(
        serializer.metadataDocument(metadata).getContent().readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(document.contains("\"ETOpen\":{\"$Kind\":\"EntityType\",\"$OpenType\":true}"),
        "an open entity type declares itself");
    assertTrue(document.contains("\"CTOpen\":{\"$Kind\":\"ComplexType\",\"$OpenType\":true}"),
        "an open complex type declares itself");
    assertTrue(document.contains("\"ETClosed\":{\"$Kind\":\"EntityType\"}"),
        "a closed type stays silent - false is the default");
    assertFalse(document.contains("$OpenType\":false"),
        "the default is never written");
  }

  /**
   * Section 14.3.7: "Enumeration member expressions are represented as a string containing the numeric
   * or symbolic enumeration value", the spec's Example 51 being {@code "@self.HasPattern": "Red,Striped"}.
   * The Csdl model carries the CSDL XML form - qualified members separated by spaces - which a
   * conformant third-party reader cannot interpret, so the writer emits the spec form.
   */
  @Test
  void enumMemberConstantsAreWrittenInTheSpecForm() throws Exception {
    assertEquals("\"Red,Striped\"",
        constantAnnotation(EdmExpressionType.EnumMember,
            "MyNamespace.Pattern/Red MyNamespace.Pattern/Striped"),
        "the members lose their type qualification and are joined with a comma");
  }

  /** Section 14.3.7: a single member and an already unqualified value are written unchanged. */
  @Test
  void enumMemberConstantsKeepASingleMember() throws Exception {
    assertEquals("\"Red\"", constantAnnotation(EdmExpressionType.EnumMember, "MyNamespace.Pattern/Red"));
    assertEquals("\"Red\"", constantAnnotation(EdmExpressionType.EnumMember, "Red"));
  }

  /**
   * OLINGO-1534: a constant whose value is null is written as a JSON null. {@code new BigDecimal(null)}
   * throws a NullPointerException, which the writer's NumberFormatException fallback does not catch.
   */
  @Test
  void nullNumericConstantsAreWrittenAsJsonNull() throws Exception {
    assertEquals("null", constantAnnotation(EdmExpressionType.Decimal, null));
    assertEquals("null", constantAnnotation(EdmExpressionType.Float, null));
  }

  /**
   * Serializes a schema carrying a single annotated enumeration member and returns the JSON text of the
   * annotation value, so a constant expression can be pinned on its own.
   */
  private String constantAnnotation(final EdmExpressionType type, final String value) throws Exception {
    EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));
    ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);

    EdmEnumType enumType = mock(EdmEnumType.class);
    when(schema.getEnumTypes()).thenReturn(Collections.singletonList(enumType));
    when(enumType.getName()).thenReturn("MyEnum");
    when(enumType.getKind()).thenReturn(EdmTypeKind.ENUM);
    when(enumType.getUnderlyingType())
        .thenReturn(OData.newInstance().createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32));
    when(enumType.getMemberNames()).thenReturn(Collections.singletonList("MyMember"));
    EdmMember member = mock(EdmMember.class);
    when(enumType.getMember("MyMember")).thenReturn(member);
    when(member.getName()).thenReturn("MyMember");
    when(member.getValue()).thenReturn("0");

    EdmAnnotation annotation = mock(EdmAnnotation.class);
    when(member.getAnnotations()).thenReturn(Collections.singletonList(annotation));
    when(annotation.getQualifier()).thenReturn("Pinned");
    EdmConstantExpression expression = mock(EdmConstantExpression.class);
    when(expression.isConstant()).thenReturn(true);
    when(expression.asConstant()).thenReturn(expression);
    when(expression.getExpressionType()).thenReturn(type);
    when(expression.getExpressionName()).thenReturn(type.name());
    when(expression.getValueAsString()).thenReturn(value);
    when(annotation.getExpression()).thenReturn(expression);

    final String document = new String(serializer.metadataDocument(serviceMetadata).getContent()
        .readAllBytes(), StandardCharsets.UTF_8);
    final String prefix = "\"MyMember#Pinned\":";
    final int start = document.indexOf(prefix) + prefix.length();
    assertTrue(start > prefix.length(), "the annotated member must be in " + document);
    final int end = document.indexOf('}', start);
    return document.substring(start, end);
  }

  /** Section 10.3: enumeration member values are JSON numbers, not strings. */
  @Test
  void enumMemberValuesAreNumbers() throws Exception {
    assertTrue(localMetadata().contains("\"String1\":1,"), "an enum member value is a number");
  }

  /**
   * Section 8.5: "The value of $ReferentialConstraint is an object with one member per referential
   * constraint." {@code NavPropertyETAbstract} declares two constraints, so a writer that opened one
   * object per constraint would emit the member name twice.
   */
  @Test
  void referentialConstraintsShareOneObject() throws Exception {
    final String metadata = localMetadata();
    assertEquals(1, countOccurrences(metadata, "\"$ReferentialConstraint\":"),
        "the constraints of one navigation property live in a single object");
    assertTrue(metadata.contains("\"$ReferentialConstraint\":{\"PropertyString\":\"PropertyString\","
        + "\"PropertyString@Core.Description#Target\":\"Description of Complex Type\","
        + "\"PropertyInt16\":\"PropertyInt16\"}"),
        "both constraints are members of the same object");
  }

  /**
   * Section 8.5 / Example 23: a referential constraint may carry annotations, and they are members of
   * the same object prefixed with the constraint's member name.
   */
  @Test
  void referentialConstraintAnnotationsArePrefixedWithTheConstraintName() throws Exception {
    assertTrue(localMetadata().contains(
        "\"PropertyString@Core.Description#Target\":\"Description of Complex Type\""),
        "the annotation of a referential constraint is written, prefixed with its member name");
  }

  /**
   * Section 8.2: "Nullable MUST NOT be specified for a collection-valued navigation property, a
   * collection is allowed to have zero items." The model defaults nullable to true, so the guard is
   * what keeps {@code NavPropertyETAbstractMany} -- collection-valued and left at that default -- from
   * emitting a forbidden member.
   */
  @Test
  void collectionValuedNavigationPropertiesNeverWriteNullable() throws Exception {
    final String metadata = localMetadata();
    assertTrue(metadata.contains("\"NavPropertyETAbstractMany\":{\"$Kind\":\"NavigationProperty\","
        + "\"$Type\":\"Alias.ETAbstract\",\"$Collection\":true}"),
        "a nullable collection navigation property is written without $Nullable");
  }

  /**
   * Section 12.8: "If the return type is a collection of entity types, the $Nullable member has no
   * meaning and MUST NOT be specified." Both {@code ETTwoKeyNav}-returning operations are declared
   * with the default nullable=true, so only the guard keeps the member out.
   */
  @Test
  void entityCollectionReturnTypesNeverWriteNullable() throws Exception {
    final String metadata = localMetadata();
    assertTrue(metadata.contains("\"$ReturnType\":{\"$Type\":\"Alias.ETTwoKeyNav\",\"$Collection\":true}"),
        "an entity-collection return type is written without $Nullable");
    // Scoped to entity collections on purpose: for a collection-valued *property* section 7.2.1 says
    // "$Nullable applies to items of the collection", so e.g. CTEntityInfo/ID (a collection of
    // Edm.Int16) legitimately keeps the member.
    assertFalse(metadata.contains("\"$Type\":\"Alias.ETTwoKeyNav\",\"$Collection\":true,\"$Nullable\""),
        "neither ETTwoKeyNav-returning operation writes $Nullable on its entity collection");
  }

  private static int countOccurrences(final String haystack, final String needle) {
    int count = 0;
    for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
      count++;
    }
    return count;
  }


  /**
   * OData 4.01, CSDL JSON section 4: a metadata document of a service MUST carry $EntityContainer,
   * and it is the one place in the whole format where the namespace-qualified name is required even
   * though the schema declares an alias. Its value must name the entity container "of that service"
   * (Section 4) -- not just any schema-owned container -- namespace-qualified the way the document
   * itself spells it.
   *
   * <p>{@code LocalProvider} declares an entity container in each of three schemas
   * ({@code namespace1.container1}, {@code namespace2.container2}, {@code namespace.container}), and
   * {@code getEntityContainerInfo(null)} identifies {@code namespace.container} (local name
   * {@code "container"}) as the service's default container. {@code MetadataDocumentJsonSerializer}
   * matches that local name against the schema-owned containers and finds it on schema
   * {@code namespace} (not schema1's same-shaped-but-differently-named {@code container1}), so the
   * asserted value is namespace-qualified from schema {@code namespace}: {@code namespace.container}.
   * Namespace {@code namespace} declares alias {@code Alias}, so the negative assertion (alias-
   * qualified form absent) is load-bearing: an implementation that alias-resolved here would emit
   * {@code "Alias.container"} instead, and the assertion would catch it.
   */
  @Test
  void documentCarriesNamespaceQualifiedEntityContainer() throws Exception {
    final String metadata = localMetadata();
    assertTrue(metadata.contains("\"$EntityContainer\":\"namespace.container\""),
        "the document object must name the service's container with its namespace, not its alias");
    assertFalse(metadata.contains("\"$EntityContainer\":\"Alias.container\""),
        "the alias-qualified form is explicitly not allowed for $EntityContainer");
  }

  /**
   * Section 4 makes $EntityContainer a MUST only "if the CSDL JSON document is the metadata document
   * of an OData service" -- an EDM with schemas but no entity container anywhere has none to name, so
   * the document must omit the member entirely rather than invent or dangle a reference.
   */
  @Test
  void documentOmitsEntityContainerWhenNoSchemaDeclaresOne() throws Exception {
    final EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    final Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));
    final ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);

    final InputStream metadata = serializer.metadataDocument(serviceMetadata).getContent();
    final String metadataString = new String(metadata.readAllBytes(), StandardCharsets.UTF_8);
    assertFalse(metadataString.contains("$EntityContainer"),
        "no schema and no default container means nothing to name");
  }

  /**
   * CSDL JSON section 13.1: $Extends is a member of the container object. The nested
   * {"Extending":{"$Kind":"EntityContainer","$Extends":...}} object Olinguito used to write does not
   * exist in the format at all.
   *
   * <p>{@code LocalProvider} chains two extended containers: schema {@code namespace}'s {@code
   * container} extends {@code namespace1.container1} (written aliased, as {@code Alias1.container1},
   * since {@code namespace1} declares alias {@code Alias1}), and schema {@code namespace1}'s {@code
   * container1} in turn extends {@code namespace2.container2} (written unaliased, since {@code
   * namespace2} declares none). Either flattened pair demonstrates the fix; this asserts the
   * {@code container1} one.
   */
  @Test
  void containerExtendsIsWrittenFlat() throws Exception {
    final String metadata = localMetadata();
    assertTrue(metadata.contains("\"container1\":{\"$Kind\":\"EntityContainer\","
        + "\"$Extends\":\"namespace2.container2\""),
        "$Extends must sit directly on the container object");
    assertFalse(metadata.contains("Extending"), "the nested Extending object must be gone");
  }

  /** CSDL JSON section 13.2: the entity set object MUST contain $Collection with the value true. */
  @Test
  void entitySetCarriesCollectionTrue() throws Exception {
    assertTrue(localMetadata().contains("\"ESOne\":{\"$Collection\":true,\"$Type\":"),
        "an entity set is a collection and must say so");
  }

  /**
   * CSDL JSON sections 13.2/13.3/13.5/13.6 list the members of each container child, and $Kind is in
   * none of the four lists - container children are told apart structurally ($Collection+$Type,
   * $Type, $Action, $Function). Schema-level elements keep the $Kind the spec does define for them.
   */
  @Test
  void containerChildrenCarryNoKindMember() throws Exception {
    final String metadata = localMetadata();
    assertFalse(metadata.contains("\"$Kind\":\"EntitySet\""), "an entity set has no $Kind");
    assertFalse(metadata.contains("\"$Kind\":\"Singleton\""), "a singleton has no $Kind");
    assertFalse(metadata.contains("\"$Kind\":\"ActionImport\""), "an action import has no $Kind");
    assertFalse(metadata.contains("\"$Kind\":\"FunctionImport\""), "a function import has no $Kind");
    assertTrue(metadata.contains("\"$Kind\":\"EntityContainer\""), "the container itself keeps $Kind");
    assertTrue(metadata.contains("\"$Kind\":\"EntityType\""), "schema-level elements keep $Kind");
  }

  /**
   * $Version reports the version the service serves. Every Olinguito service reports
   * ODataServiceVersion.V40 today (ServiceMetadataImpl#getDataServiceVersion), which matches the XML
   * serializer's Version="4.0" and the OData-Version response header.
   */
  @Test
  void versionIsTheServedVersion() throws Exception {
    assertTrue(localMetadata().startsWith("{\"$Version\":\"4.0\","), "the served version is 4.0");
  }

  /**
   * Sections 13.5/13.6: $EntitySet on an action/function import is "either the unqualified name of an
   * entity set in the same entity container or a path to an entity set in a different entity
   * container." {@code LocalProvider}'s main container ({@code namespace.container}) has both shapes:
   * {@code AIRTPrimParam} binds to {@code ESTwoKeyNav} in its own container (asserted unqualified by
   * {@code aliasTest}), and the fixture-added {@code AIRTPrimParamCrossContainer} binds to
   * {@code ESContainer2}, which is declared in the unrelated {@code namespace2.container2} -- a
   * different container than the one importing it, so the value must be the target container's
   * namespace-qualified name followed by a slash and the entity set's name.
   */
  @Test
  void actionImportEntitySetIsQualifiedPathAcrossContainers() throws Exception {
    assertTrue(localMetadata().contains(
        "\"AIRTPrimParamCrossContainer\":{\"$Action\":\"Alias.UARTPrimParam\","
        + "\"$EntitySet\":\"namespace2.container2/ESContainer2\"}"),
        "an entity set in a different container must be named by a container-qualified path");
  }

  /**
   * OData 4.01, CSDL JSON section 14.3: constants are bare JSON values. The $Binary/$Date/$Int/...
   * member names this writer used to emit are CSDL XML element names and do not exist in CSDL JSON at
   * all (citations digest, Gaps), so a conformant reader could not interpret them.
   */
  @Test
  void constantExpressionsAreBareJsonValues() throws Exception {
    final String metadata = localMetadata();
    for (String xmlOnlyMember : List.of("$Binary", "$Date", "$DateTimeOffset", "$Decimal", "$Duration",
        "$EnumMember", "$Float", "$Guid", "$Int", "$String", "$TimeOfDay")) {
      assertFalse(metadata.contains("\"" + xmlOnlyMember + "\":"),
          xmlOnlyMember + " is a CSDL XML element name and must never appear in CSDL JSON");
    }
    assertTrue(metadata.contains("\"@ns.term#T10\":42,"), "an integer constant is a JSON number");
    assertTrue(metadata.contains("\"@ns.term#T9\":\"aabbccdd-aabb-ccdd-eeff-aabbccddeeff\","),
        "a guid constant is a JSON string");
  }

  /** Section 14.4.12: a record is a bare object whose type rides on the @type control information. */
  @Test
  void recordExpressionsCarryAtType() throws Exception {
    final String metadata = localMetadata();
    assertTrue(metadata.contains("\"@ns.term#T33\":{\"@type\":\"#Alias.ETAbstract\","));
    assertFalse(metadata.contains("\"$Record\""), "there is no $Record wrapper in CSDL JSON");
  }

  /** Sections 14.3.5/14.3.8: the three special numeric values stay strings. */
  @Test
  void specialFloatingPointValuesStayStrings() throws Exception {
    for (String special : List.of("INF", "-INF", "NaN")) {
      final String written = writeSingleAnnotation(EdmExpressionType.Float, special);
      assertTrue(written.contains("\"@ns.term\":\"" + special + "\""), special + " is written as a string");
    }
    assertTrue(writeSingleAnnotation(EdmExpressionType.Float, "1.5").contains("\"@ns.term\":1.5"));
  }

  /**
   * Section 14.3.10 plus its IEEE754Compatible note: an integer that a JSON number cannot carry
   * exactly (|value| > 2^53 - 1) is written as a string, as in the spec's own Example 55.
   */
  @Test
  void integersBeyondTheSafeRangeAreWrittenAsStrings() throws Exception {
    assertTrue(writeSingleAnnotation(EdmExpressionType.Int, "9007199254740992")
        .contains("\"@ns.term\":\"9007199254740992\""));
    assertTrue(writeSingleAnnotation(EdmExpressionType.Int, "9007199254740991")
        .contains("\"@ns.term\":9007199254740991"));
  }

  /**
   * Serializes a schema carrying exactly one annotation whose value is the given constant, so a
   * single constant's wire form can be asserted without threading it through LocalProvider.
   */
  private String writeSingleAnnotation(final EdmExpressionType type, final String value) throws Exception {
    final EdmSchema schema = mock(EdmSchema.class);
    when(schema.getNamespace()).thenReturn("MyNamespace");
    final Edm edm = mock(Edm.class);
    when(edm.getSchemas()).thenReturn(List.of(schema));
    final ServiceMetadata serviceMetadata = mock(ServiceMetadata.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);

    final EdmAnnotation annotation = mock(EdmAnnotation.class);
    when(schema.getAnnotations()).thenReturn(Collections.singletonList(annotation));
    final EdmTerm term = mock(EdmTerm.class);
    when(term.getFullQualifiedName()).thenReturn(new FullQualifiedName("ns", "term"));
    when(annotation.getTerm()).thenReturn(term);
    final EdmConstantExpression expression = mock(EdmConstantExpression.class);
    when(expression.isConstant()).thenReturn(true);
    when(expression.asConstant()).thenReturn(expression);
    when(expression.getExpressionType()).thenReturn(type);
    when(expression.getExpressionName()).thenReturn(type.name());
    when(expression.getValueAsString()).thenReturn(value);
    when(annotation.getExpression()).thenReturn(expression);

    return new String(serializer.metadataDocument(serviceMetadata).getContent().readAllBytes(),
        StandardCharsets.UTF_8);
  }

  private String localMetadata() throws SerializerException, IOException {
    CsdlEdmProvider provider = new LocalProvider();
    ServiceMetadata serviceMetadata = new ServiceMetadataImpl(provider, Collections.emptyList(), null);
    InputStream metadataStream = serializer.metadataDocument(serviceMetadata).getContent();
    String metadata = new String(metadataStream.readAllBytes(), StandardCharsets.UTF_8);
    assertNotNull(metadata);
    return metadata;
  }
  
  static class LocalProvider implements CsdlEdmProvider {
    private final static String nameSpace = "namespace";
    private final static String nameSpace1 = "namespace1";
    private final static String nameSpace2 = "namespace2";

    private final FullQualifiedName nameETAbstract = new FullQualifiedName(nameSpace, "ETAbstract");
    private final FullQualifiedName nameETAbstractBase = new FullQualifiedName(nameSpace, "ETAbstractBase");
    private final FullQualifiedName nameET = new FullQualifiedName(nameSpace, "ET");
    private final FullQualifiedName nameETTwoKeyNav = new FullQualifiedName(nameSpace, "ETTwoKeyNav");
    private final FullQualifiedName nameETTwoKeyNavOne = new FullQualifiedName(nameSpace, "ETTwoKeyNavOne");
    private final FullQualifiedName nameETOne = new FullQualifiedName(nameSpace, "ETOne");

    private final FullQualifiedName nameInt16 = EdmPrimitiveTypeKind.Int16.getFullQualifiedName();
    private final FullQualifiedName nameDateTimeOffset = EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName();
    private final FullQualifiedName nameString = EdmPrimitiveTypeKind.String.getFullQualifiedName();
    private final FullQualifiedName nameUARTPrimParam = new FullQualifiedName(nameSpace, "UARTPrimParam");
    private final FullQualifiedName nameCTEntityInfo = new FullQualifiedName(nameSpace, "CTEntityInfo");
    private final CsdlProperty propertyInt16_NotNullable = new CsdlProperty()
    .setName("PropertyInt16")
    .setType(nameInt16)
    .setNullable(false);
    private final CsdlProperty propertyString = new CsdlProperty()
    .setName("PropertyString")
    .setType(nameString);
    private final CsdlProperty nameProperty = new CsdlProperty()
        .setName("name")
        .setType(nameString)
        .setNullable(true);
    private final CsdlProperty infoProperty = new CsdlProperty()
        .setName("Info")
        .setType(nameCTEntityInfo);
    private final CsdlProperty idProperty = new CsdlProperty()
        .setName("ID")
        .setType(nameInt16)
        .setCollection(true);
    private final CsdlProperty createdProperty = new CsdlProperty()
        .setName("Created")
        .setType(nameDateTimeOffset)
        .setPrecision(20)
        .setScale(2)
        .setDefaultValue("10-2-2017:20:30:40")
        .setMaxLength(30);
    private final CsdlNavigationProperty navProperty = new CsdlNavigationProperty()
        .setName("NavProperty")
        .setType(nameETAbstract)
        .setNullable(false)
        .setOnDelete(new CsdlOnDelete().setAction(CsdlOnDeleteAction.Cascade)
            .setAnnotations(Collections.singletonList(new CsdlAnnotation().setTerm("core.Term"))));
    
    private final FullQualifiedName nameCTTwoPrim = new FullQualifiedName(nameSpace, "CTTwoPrim");
    private final FullQualifiedName nameCTTwoPrimBase = new FullQualifiedName(nameSpace, "CTTwoPrimBase");
    private final FullQualifiedName nameCTPrimComp = new FullQualifiedName(nameSpace, "CTPrimComp");
    private final FullQualifiedName nameUFNRTInt16 = new FullQualifiedName(nameSpace, "UFNRTInt16");
    private final FullQualifiedName nameUFNRTInt161 = new FullQualifiedName("nameSpace2", "UFNRTInt161");
    private final FullQualifiedName nameContainer = new FullQualifiedName(nameSpace, "container");
    private final FullQualifiedName nameContainer1 = new FullQualifiedName(nameSpace1, "container1");
    private final FullQualifiedName nameContainer2 = new FullQualifiedName(nameSpace2, "container2");
    private final FullQualifiedName nameENString = new FullQualifiedName(nameSpace, "ENString");
    private final FullQualifiedName nameBAETTwoKeyNavRTETTwoKeyNavParam = 
        new FullQualifiedName(nameSpace, "BAETTwoKeyNavRTETTwoKeyNavParam");
    private final FullQualifiedName nameBFETTwoKeyNavRTETTwoKeyNavParam = 
        new FullQualifiedName(nameSpace, "BFETTwoKeyNavRTETTwoKeyNavParam");
    private final FullQualifiedName nameBAProp = 
        new FullQualifiedName(nameSpace, "BAProp");

    @Override
    public List<CsdlAliasInfo> getAliasInfos() {
      return Collections.singletonList(new CsdlAliasInfo().setAlias("Alias").setNamespace(nameSpace));
    }

    @Override
    public CsdlEnumType getEnumType(final FullQualifiedName enumTypeName) {
      
      if (nameENString.equals(enumTypeName)) {
        
        CsdlAnnotation memberAnnotation = new CsdlAnnotation()
            .setTerm("Core.Description")
            .setQualifier("Target")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.String, "Description of Enum Member"));
        
        return new CsdlEnumType()
        .setName(nameENString.getName())
        .setFlags(true)
        .setUnderlyingType(EdmPrimitiveTypeKind.Int16.getFullQualifiedName())
        .setMembers(Collections.singletonList(
            new CsdlEnumMember().setName("String1").setValue("1")
                .setAnnotations(Collections.singletonList(memberAnnotation))));
      }
      return null;
    }

    @Override
    public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {
      if (entityTypeName.equals(nameETAbstract)) {
        return new CsdlEntityType()
        .setName("ETAbstract")
        .setAbstract(true)
        .setNavigationProperties(Collections.singletonList(
                new CsdlNavigationProperty().setName("NavPropertyETTwoKeyNavOne").setType(nameETTwoKeyNavOne)))
        .setProperties(Collections.singletonList(propertyString));
      } else if (entityTypeName.equals(nameETAbstractBase)) {
        CsdlAnnotation annotation = new CsdlAnnotation()
            .setTerm("Core.Description")
            .setQualifier("Target")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.String, "Description of Type"));
        propertyInt16_NotNullable.setAnnotations(Collections.singletonList(annotation));
        
        return new CsdlEntityType()
        .setName("ETAbstractBase")
        .setBaseType(nameETAbstract)
        .setKey(Collections.singletonList(new CsdlPropertyRef().setName("PropertyInt16")))
        .setProperties(Collections.singletonList(propertyInt16_NotNullable))
        .setAnnotations(Collections.singletonList(annotation));
      } else if (entityTypeName.equals(nameET)) {
        return new CsdlEntityType()
            .setName("ET")
            .setKey(List.of(new CsdlPropertyRef().setAlias("EntityInfoID").setName("Info/ID"), 
                new CsdlPropertyRef().setName("name")))
            .setNavigationProperties(List.of(
                new CsdlNavigationProperty().setName("NavPropertyETOne").setType(nameETOne), navProperty))
            .setProperties(List.of(nameProperty, infoProperty));
      } else if (entityTypeName.equals(nameETTwoKeyNav)) {
        return new CsdlEntityType()
            .setName("ETTwoKeyNav")
            .setKey(List.of(new CsdlPropertyRef().setName("PropertyInt16"), 
                new CsdlPropertyRef().setName("PropertyString")))
            .setNavigationProperties(List.of(
                new CsdlNavigationProperty().setName("NavPropertyETTwoKeyNavOne").setType(nameETTwoKeyNavOne),
                new CsdlNavigationProperty().setName("NavPropertyETOne").setType(nameETOne),
                // Collection-valued and left at the model default nullable=true, so that the
                // section 8.2 "MUST NOT be specified" guard has something to suppress.
                new CsdlNavigationProperty().setName("NavPropertyETAbstractMany").setType(nameETAbstract)
                    .setCollection(true)))
            .setProperties(List.of(propertyInt16_NotNullable, propertyString));
      } else if (entityTypeName.equals(nameETOne)) {
        return new CsdlEntityType()
            .setName("ETOne")
            .setKey(Collections.singletonList(new CsdlPropertyRef().setName("PropertyInt16")))
            .setNavigationProperties(Collections.singletonList(
                    new CsdlNavigationProperty().setName("NavPropertyET").setType(nameET)))
            .setProperties(Collections.singletonList(propertyInt16_NotNullable));
      } else if (entityTypeName.equals(nameETTwoKeyNavOne)) {
        return new CsdlEntityType()
            .setName("ETTwoKeyNavOne")
            .setBaseType(nameETOne)
            .setHasStream(true)
            .setProperties(Collections.singletonList(propertyString))
            .setNavigationProperties(Collections.singletonList(
                    new CsdlNavigationProperty().setName("NavPropertyETAbstract")
                            .setCollection(true).setType(nameETAbstract)
                            .setContainsTarget(true).setPartner("NavPropertyETTwoKeyNavOne").setNullable(false)
                            .setReferentialConstraints(List.of(new CsdlReferentialConstraint()
                                    .setProperty("PropertyString").setReferencedProperty("PropertyString")
                                    .setAnnotations(Collections.singletonList(new CsdlAnnotation()
                                            .setTerm("Core.Description")
                                            .setQualifier("Target")
                                            .setExpression(new CsdlConstantExpression(
                                                    ConstantExpressionType.String,
                                                    "Description of Complex Type")))),
                                    // A second constraint on the same navigation property: section 8.5
                                    // requires both to be members of ONE $ReferentialConstraint object.
                                    new CsdlReferentialConstraint()
                                    .setProperty("PropertyInt16")
                                    .setReferencedProperty("PropertyInt16")))));
      }
      return null;
    }

    @Override
    public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) throws ODataException {
      if (complexTypeName.equals(nameCTTwoPrim)) {
        return new CsdlComplexType()
        .setName("CTTwoPrim")
        .setAbstract(true)
        .setProperties(List.of(propertyInt16_NotNullable, propertyString));

      }
      if (complexTypeName.equals(nameCTTwoPrimBase)) {
        CsdlAnnotation annotation = new CsdlAnnotation()
            .setTerm("Core.Description")
            .setQualifier("Target")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.String, "Description of Complex Type"));
        
        return new CsdlComplexType()
        .setName("CTTwoPrimBase")
        .setBaseType(nameCTTwoPrim)
        .setProperties(List.of(propertyInt16_NotNullable, propertyString))
        .setAnnotations(Collections.singletonList(annotation));
      }
      if (complexTypeName.equals(nameCTEntityInfo)) {
        return new CsdlComplexType()
            .setName("CTEntityInfo")
            .setProperties(List.of(idProperty, createdProperty));
      }
      if (complexTypeName.equals(nameCTPrimComp)) {
        return new CsdlComplexType()
        .setName("CTPrimComp")
        .setProperties(Collections.singletonList(propertyString));
      }
      return null;

    }

    @Override
    public List<CsdlAction> getActions(final FullQualifiedName actionName) {
      if (actionName.equals(nameUARTPrimParam)) {
        return Collections.singletonList(
            new CsdlAction().setName("UARTPrimParam")
            .setParameters(Collections.singletonList(
                new CsdlParameter().setName("ParameterInt16").setType(nameInt16)))
                .setReturnType(new CsdlReturnType().setType(nameString)));
      }
      if (actionName.equals(nameBAETTwoKeyNavRTETTwoKeyNavParam)) {
        return List.of(
            new CsdlAction().setName("BAETTwoKeyNavRTETTwoKeyNavParam")
            .setParameters(List.of(
                new CsdlParameter().setName("BindingParam").setType(nameETTwoKeyNav),
                new CsdlParameter().setName("PropertyComp").setType(nameCTPrimComp)))
            .setReturnType(new CsdlReturnType().setType(nameETTwoKeyNav).setCollection(true))
            .setEntitySetPath("BindingParam/NavPropertyETTwoKeyNavOne")
            .setBound(true),
            new CsdlAction().setName("BAETTwoKeyNavRTETTwoKeyNavParam")
            .setParameters(Collections.singletonList(
                    new CsdlParameter().setName("BindingParam").setType(nameET)))
            .setReturnType(new CsdlReturnType().setNullable(false).setType(nameET))
            .setBound(true)
            .setEntitySetPath("BindingParam/NavPropertyET"),
            new CsdlAction().setName("BAETTwoKeyNavRTETTwoKeyNavParam")
            .setParameters(Collections.singletonList(
                    new CsdlParameter().setName("PropertyComp").setType(nameCTPrimComp)))
            .setReturnType(new CsdlReturnType().setNullable(false).setType(nameET)));
      }
      if (actionName.equals(nameBAProp)) {
        return Collections.singletonList(new CsdlAction().setName("BAProp")
        .setParameters(List.of(
            new CsdlParameter().setName("BindingParam").setType(nameET),
            new CsdlParameter().setName("PropertyInt").setType(nameInt16).
            setPrecision(10).setScale(3).setMaxLength(10).setNullable(false).setCollection(true)))
        .setReturnType(new CsdlReturnType().setNullable(true).setType(nameInt16)
            .setPrecision(10).setScale(3).setMaxLength(10))
        .setBound(true)
        .setEntitySetPath("BindingParam/NavPropertyET"));
      }
      return null;
    }

    @Override
    public List<CsdlFunction> getFunctions(final FullQualifiedName functionName) {
      if (functionName.equals(nameUFNRTInt16)) {
        return Collections.singletonList(
            new CsdlFunction()
            .setName("UFNRTInt16")
            .setParameters(Collections.emptyList())
            .setReturnType(new CsdlReturnType().setType(nameInt16)));
      }
      if (functionName.equals(nameBFETTwoKeyNavRTETTwoKeyNavParam)) {
        return List.of(
            new CsdlFunction().setName("BFETTwoKeyNavRTETTwoKeyNavParam")
            .setParameters(List.of(
                new CsdlParameter().setName("BindingParam").setType(nameETTwoKeyNav),
                new CsdlParameter().setName("PropertyComp").setType(nameCTPrimComp)))
            .setReturnType(new CsdlReturnType().setType(nameETTwoKeyNav).setCollection(true))
            .setEntitySetPath("BindingParam/NavPropertyETTwoKeyNavOne")
            .setBound(true).setComposable(true),
            new CsdlFunction().setName("BFETTwoKeyNavRTETTwoKeyNavParam")
            .setParameters(Collections.singletonList(
                    new CsdlParameter().setName("BindingParam").setType(nameET)))
            .setReturnType(new CsdlReturnType().setNullable(false).setType(nameET))
            .setBound(true)
            .setEntitySetPath("BindingParam/NavPropertyET"));
      }
      return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
        throws ODataException {
        return switch (entitySetName) {
            case "ESAllPrim" -> new CsdlEntitySet()
                    .setName("ESAllPrim")
                    .setType(nameETAbstractBase);
            case "ESOne" -> new CsdlEntitySet()
                    .setName("ESOne")
                    .setType(nameETOne);
            case "ESTwoKeyNav" -> new CsdlEntitySet()
                    .setName("ESTwoKeyNav")
                    .setType(nameETTwoKeyNav)
                    .setNavigationPropertyBindings(List.of(new CsdlNavigationPropertyBinding()
                                    .setPath("NavPropertyETTwoKeyNavOne/namespace.ETOne/NavPropertyET")
                                    .setTarget("ES"),
                            new CsdlNavigationPropertyBinding()
                                    .setPath("NavPropertyETOne")
                                    .setTarget("ESOne")));
            case "ESTwoKeyNavOne" -> new CsdlEntitySet()
                    .setName("ESTwoKeyNavOne")
                    .setType(nameETTwoKeyNavOne)
                    .setIncludeInServiceDocument(true);
            case "ES" -> new CsdlEntitySet()
                    .setName("ES")
                    .setType(nameET)
                    .setIncludeInServiceDocument(false)
                    .setNavigationPropertyBindings(Collections.singletonList(new CsdlNavigationPropertyBinding()
                            .setPath("NavPropertyETOne")
                            .setTarget("ESOne")));
            case "ESContainer2" -> new CsdlEntitySet()
                    .setName("ESContainer2")
                    .setType(nameETOne);
            default -> null;
        };
    }

    @Override
    public CsdlSingleton getSingleton(final FullQualifiedName entityContainer, final String singletonName) {
      if (singletonName.equals("SI")) {
        return new CsdlSingleton()
        .setName("SI")
        .setType(nameETAbstractBase);
      }
      if (singletonName.equals("SIBinding")) {
        return new CsdlSingleton()
            .setName("SIBinding")
            .setType(nameET)
            .setNavigationPropertyBindings(Collections.singletonList(new CsdlNavigationPropertyBinding()
                .setPath("NavPropertyETOne")
                .setTarget("ESOne")));
      }
      return null;
    }

    @Override
    public CsdlActionImport getActionImport(final FullQualifiedName entityContainer, final String actionImportName) {
      if (entityContainer.equals(nameContainer)) {
        if (actionImportName.equals("AIRTPrimParam")) {
          return new CsdlActionImport()
          .setName("AIRTPrimParam")
          .setAction(nameUARTPrimParam)
          .setEntitySet("ESTwoKeyNav");
        }
        if (actionImportName.equals("AIRTPrimParamCrossContainer")) {
          // Tier 6 Wave 1, finding 3: $EntitySet must be a container-FQN-qualified path when the
          // returned entity set lives in a different entity container than the one importing it.
          return new CsdlActionImport()
          .setName("AIRTPrimParamCrossContainer")
          .setAction(nameUARTPrimParam)
          .setEntitySet(nameContainer2.getFullQualifiedNameAsString() + "/ESContainer2");
        }
      }
      return null;
    }

    @Override
    public CsdlFunctionImport getFunctionImport(final FullQualifiedName entityContainer,
        final String functionImportName) {
      if (null != entityContainer && entityContainer.equals(nameContainer)) {
        if (functionImportName.equals("FINRTInt16")) {
          return new CsdlFunctionImport()
          .setName("FINRTInt16")
          .setFunction(nameUFNRTInt16)
          .setIncludeInServiceDocument(true)
          .setEntitySet("ESTwoKeyNavOne");
        }
      } else {
        if (functionImportName.equals("FINRTInt161")) {
          return new CsdlFunctionImport()
          .setName("FINRTInt161")
          .setFunction(nameUFNRTInt161)
          .setIncludeInServiceDocument(true)
          .setEntitySet("ESTwoKeyNavOne");
        }
      }
      return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
      List<CsdlSchema> schemas = new ArrayList<>();
      CsdlSchema schema1 = new CsdlSchema();
      schema1.setNamespace(nameSpace1);
      schema1.setAlias("Alias1");
      schemas.add(schema1);
   // EntityContainer
      schema1.setEntityContainer(getEntityContainer1());
      
      CsdlSchema schema2 = new CsdlSchema();
      schema2.setNamespace(nameSpace2);
      schemas.add(schema2);
   // EntityContainer
      schema2.setEntityContainer(getEntityContainer2());
   
      
      CsdlSchema schema = new CsdlSchema();
      schema.setNamespace(nameSpace);
      schema.setAlias("Alias");
      schemas.add(schema);

      // EnumTypes
      schema.setEnumTypes(Collections.singletonList(getEnumType(nameENString)));

      // EntityTypes
      schema.setEntityTypes(List.of(
          getEntityType(nameETAbstract),
          getEntityType(nameETAbstractBase),
          getEntityType(nameET),
          getEntityType(nameETOne),
          getEntityType(nameETTwoKeyNav),
          getEntityType(nameETTwoKeyNavOne)));

      // ComplexTypes
      schema.setComplexTypes(List.of(
          getComplexType(nameCTTwoPrim),
          getComplexType(nameCTTwoPrimBase),
          getComplexType(nameCTEntityInfo),
          getComplexType(nameCTPrimComp)));

      // TypeDefinitions

      // Actions
      List<CsdlAction> actions1 = getActions(nameUARTPrimParam);
      List<CsdlAction> actions2 = getActions(nameBAETTwoKeyNavRTETTwoKeyNavParam);
      List<CsdlAction> actions3 = getActions(nameBAProp);
      List<CsdlAction> actions = new ArrayList<>();
      actions.addAll(actions1);
      actions.addAll(actions2);
      actions.addAll(actions3);
      
      schema.setActions(actions);

      // Functions
      List<CsdlFunction> function1 = getFunctions(nameUFNRTInt16);
      List<CsdlFunction> function2 = getFunctions(nameBFETTwoKeyNavRTETTwoKeyNavParam);
      List<CsdlFunction> functions = new ArrayList<>();
      functions.addAll(function1);
      functions.addAll(function2);
      
      schema.setFunctions(functions);

      // EntityContainer
      schema.setEntityContainer(getEntityContainer());

      // Terms
      schema.setTerms(List.of(
          getTerm(new FullQualifiedName("ns", "term")),
          getTerm(new FullQualifiedName("namespace", "Term1")),
          getTerm(new FullQualifiedName("ns", "Term2")),
          getTerm(new FullQualifiedName("ns", "Term3")),
          getTerm(new FullQualifiedName("ns", "Term4"))));

      // Annotationgroups
      schema.setAnnotationsGroup(List.of(
          getAnnotationsGroup(new FullQualifiedName("Alias", "ETAbstract"), "Tablett"),
          getAnnotationsGroup(new FullQualifiedName("Alias", "ET"), "T")));
      
      return schemas;
    }

    public CsdlEntityContainer getEntityContainer1() throws ODataException {
      CsdlEntityContainer container = new CsdlEntityContainer();
      container.setName("container1");

      // EntitySets
      container.setEntitySets(Collections.singletonList(getEntitySet(nameContainer1, "ESAllPrim")));
      container.setExtendsContainer(nameContainer2.getFullQualifiedNameAsString());

      return container;
    }
    
    public CsdlEntityContainer getEntityContainer2() throws ODataException {
      CsdlEntityContainer container = new CsdlEntityContainer();
      container.setName("container2");
      // Target of the cross-container action import below (Tier 6 Wave 1, finding 3): an entity set
      // that lives in a container other than the one importing it.
      container.setEntitySets(Collections.singletonList(getEntitySet(nameContainer2, "ESContainer2")));

      return container;
    }
    
    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName) {
      if (entityContainerName == null) {
        // Namespace-consistent with the container actually declared in schema "namespace" below, so the
        // document-level $EntityContainer test is load-bearing (namespace "namespace" carries alias
        // "Alias", so an implementation that alias-resolved would be caught).
        return new CsdlEntityContainerInfo().setContainerName(nameContainer);
      }
      return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
      CsdlEntityContainer container = new CsdlEntityContainer();
      container.setName("container");

      // EntitySets
      container.setEntitySets(List.of(getEntitySet(nameContainer, "ESAllPrim"),
          getEntitySet(nameContainer, "ES"),
          getEntitySet(nameContainer, "ESOne"),
          getEntitySet(nameContainer, "ESTwoKeyNav"),
          getEntitySet(nameContainer, "ESTwoKeyNavOne")));

      // Singletons
      container.setSingletons(List.of(getSingleton(nameContainer, "SI"),
          getSingleton(nameContainer, "SIBinding")));

      // ActionImports
      container.setActionImports(List.of(getActionImport(nameContainer, "AIRTPrimParam"),
          getActionImport(nameContainer, "AIRTPrimParamCrossContainer")));

      // FunctionImports
      container.setFunctionImports(List.of(getFunctionImport(nameContainer, "FINRTInt16"),
          getFunctionImport(null, "FINRTInt161")));
      
      container.setExtendsContainer(new FullQualifiedName(nameSpace1, "container1").getFullQualifiedNameAsString());

      return container;
    }

    @Override
    public CsdlTypeDefinition getTypeDefinition(final FullQualifiedName typeDefinitionName) {
      return null;
    }

    @Override
    public CsdlTerm getTerm(final FullQualifiedName termName) throws ODataException {
      if (new FullQualifiedName("ns", "term").equals(termName)) {
        return new CsdlTerm().setType("Edm.String").setName("term");

      } else if (new FullQualifiedName("namespace", "Term1").equals(termName)) {
        return new CsdlTerm().setType("Edm.String").setName("Term1");

      } else if (new FullQualifiedName("ns", "Term2").equals(termName)) {
        return new CsdlTerm().setType("Edm.String").setName("Term2")
            .setNullable(false).setDefaultValue("default").setMaxLength(1).setPrecision(2).setScale(3);

      } else if (new FullQualifiedName("ns", "Term3").equals(termName)) {
        return new CsdlTerm().setType("Edm.String").setName("Term3")
            .setAppliesTo(List.of("Property", "EntitySet", "Schema"));

      } else if (new FullQualifiedName("ns", "Term4").equals(termName)) {
        return new CsdlTerm().setType("Edm.String").setName("Term4").setBaseTerm("namespace.Term1");

      } else if (new FullQualifiedName("Core", "Description").equals(termName)) {
        return new CsdlTerm().setType("Edm.String").setName("Description");

      }
      return null;
    }

    @Override
    public CsdlAnnotations getAnnotationsGroup(final FullQualifiedName targetName, final String qualifier) {
      if (new FullQualifiedName("Alias", "ETAbstract").equals(targetName) && "Tablett".equals(qualifier)) {
        CsdlAnnotations annoGroup = new CsdlAnnotations();
        annoGroup.setTarget("Alias.ETAbstract").setQualifier("Tablett");

        List<CsdlAnnotation> innerAnnotations = Collections.singletonList(
            new CsdlAnnotation().setTerm("ns.term"));

        List<CsdlAnnotation> annotationsList = new ArrayList<>();
        annoGroup.setAnnotations(annotationsList);
        // Constant Annotations
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T1")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Binary).setValue("qrvM3e7_")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T2")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Bool, "true")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T3")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Date, "2012-02-29")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T4")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.DateTimeOffset, "2012-02-29T01:02:03Z")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T5")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Decimal, "-12345678901234567234567890")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T6")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Duration, "PT10S")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T7")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.EnumMember, "enumMember")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T8")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Float, "1.42")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T9")
            .setExpression(
                new CsdlConstantExpression(ConstantExpressionType.Guid, "aabbccdd-aabb-ccdd-eeff-aabbccddeeff")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T10")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Int, "42")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T11")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.String, "ABCD")));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T12")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.TimeOfDay, "00:00:00.999")));

        // logical expressions
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T13")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.And)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T14")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Or)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));
        
        // comparison
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T15")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Eq)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T16")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Ne)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T17")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Gt)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T18")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Ge)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T19")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Lt)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T20")
            .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Le)
            .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setRight(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"))
            .setAnnotations(innerAnnotations)));

        // Other
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T21")
            .setExpression(new CsdlAnnotationPath().setValue("AnnoPathValue")));

        List<CsdlExpression> parameters = new ArrayList<>();
        parameters.add(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T22")
            .setExpression(new CsdlApply().setFunction("odata.concat")
                .setParameters(parameters)
                .setAnnotations(innerAnnotations)));

        List<CsdlExpression> items = new ArrayList<>();
        items.add(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"));
        items.add(new CsdlConstantExpression(ConstantExpressionType.Bool, "false"));
        items.add(new CsdlConstantExpression(ConstantExpressionType.String, "String"));
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T23")
            .setExpression(new CsdlCollection().setItems(items)));

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T24")
            .setExpression(new CsdlIf()
            .setGuard(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
            .setThen(new CsdlConstantExpression(ConstantExpressionType.String, "Then"))
            .setElse(new CsdlConstantExpression(ConstantExpressionType.String, "Else"))
            .setAnnotations(innerAnnotations)));

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T25")
            .setExpression(new CsdlLabeledElementReference().setValue("LabeledElementReferenceValue")));

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T26")
            .setExpression(new CsdlNull().setAnnotations(innerAnnotations)));

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T27")
            .setExpression(new CsdlNavigationPropertyPath().setValue("NavigationPropertyPathValue")));

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T28")
            .setExpression(new CsdlPath().setValue("PathValue")));

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T29")
            .setExpression(new CsdlPropertyPath().setValue("PropertyPathValue")));
        
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T30")
        .setExpression(new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Not)
        .setLeft(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))
        .setAnnotations(innerAnnotations)));

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T300")
        .setExpression(new CsdlCast()
        .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "value"))
        .setMaxLength(1)
        .setPrecision(2)
        .setScale(3)
        .setType("Edm.String")
        .setAnnotations(innerAnnotations))); 
        
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T31")
        .setExpression(new CsdlIsOf()
        .setMaxLength(1)
        .setPrecision(2)
        .setScale(3)
        .setType("Edm.String")
        .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "value"))
        .setAnnotations(innerAnnotations))); 

    annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T32")
        .setExpression(new CsdlLabeledElement()
        .setName("NameAtt")
        .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "value"))
        .setAnnotations(innerAnnotations)));
        
        CsdlPropertyValue prop = new CsdlPropertyValue()
        .setProperty("PropName")
        .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "value"))
        .setAnnotations(innerAnnotations);
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T33")
            .setExpression(new CsdlRecord().setType("Alias.ETAbstract")
                .setPropertyValues(Collections.singletonList(prop))
                .setAnnotations(innerAnnotations))); 

        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T34")
            .setExpression(new CsdlUrlRef()
            .setValue(new CsdlConstantExpression(ConstantExpressionType.String, "URLRefValue"))
            .setAnnotations(innerAnnotations)));
        
        annotationsList.add(new CsdlAnnotation().setTerm("ns.term").setQualifier("T35")
            .setExpression(new CsdlNull()));

        return annoGroup;
      } else if (new FullQualifiedName("Alias", "ET").equals(targetName)) {
        CsdlAnnotations annoGroup = new CsdlAnnotations();
        annoGroup.setTarget("Alias.ET");
        return annoGroup;
      }
      return null;
    }
  }
}
