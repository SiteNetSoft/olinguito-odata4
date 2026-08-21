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
 * Copyright 2026 SiteNetSoft - Byte-for-byte parity tests for streamed primitive/complex
 * collection serialization
 */
package org.sitenetsoft.olinguito.server.core.serializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.PropertyIterator;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.ComplexSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerResult;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CountOption;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.CountOptionImpl;
import org.sitenetsoft.olinguito.server.tecsvc.MetadataETagSupport;
import org.sitenetsoft.olinguito.server.tecsvc.provider.ComplexTypeProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;

class PropertyCollectionStreamedTest {

  private static final OData odata = OData.newInstance();
  private static final ServiceMetadata metadata = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList(), new MetadataETagSupport("W/\"metadataETag\""));
  private static final EdmEntityContainer entityContainer = metadata.getEdm().getEntityContainer();
  private static final EdmEntitySet esCollAllPrim = entityContainer.getEntitySet("ESCollAllPrim");
  private static final EdmEntitySet esMixPrimCollComp = entityContainer.getEntitySet("ESMixPrimCollComp");
  private static final ContextURL collectionContextUrl = ContextURL.with().entitySet(esCollAllPrim)
      .keyPath("1").navOrPropertyPath("CollPropertyString").build();
  private static final ContextURL complexCollectionContextUrl = ContextURL.with().entitySet(esMixPrimCollComp)
      .keyPath("32767").navOrPropertyPath("CollPropertyComp").build();

  private final ODataSerializer serializer = new ODataJsonSerializer(ContentType.JSON);

  /** Identical helper to PropertyStreamedDefaultsTest.iteratorOver — copied deliberately so each
   *  test class stands alone. */
  private static PropertyIterator iterator(final String name, final ValueType valueType, final List<?> values) {
    final Iterator<?> delegate = values.iterator();
    final PropertyIterator propertyIterator = new PropertyIterator() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public Property next() {
        final Object value = delegate.next();
        return new Property(null, name, elementValueType(valueType), value);
      }
    };
    propertyIterator.setName(name);
    propertyIterator.setValueType(valueType);
    return propertyIterator;
  }

  private static ValueType elementValueType(final ValueType collectionValueType) {
    switch (collectionValueType) {
    case COLLECTION_PRIMITIVE: return ValueType.PRIMITIVE;
    case COLLECTION_ENUM: return ValueType.ENUM;
    case COLLECTION_GEOSPATIAL: return ValueType.GEOSPATIAL;
    case COLLECTION_COMPLEX: return ValueType.COMPLEX;
    default: throw new IllegalArgumentException(collectionValueType.name());
    }
  }

  private static ComplexValue complexValue(final String propertyString, final short propertyInt16) {
    final ComplexValue value = new ComplexValue();
    value.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, propertyInt16));
    value.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, propertyString));
    return value;
  }

  /** A CTBase-typed element (CTBase derives from CTTwoPrim), so the @odata.type emission condition
   *  in writeComplexCollectionElement (derivedType != type) is actually exercised. */
  private static ComplexValue derivedComplexValue(final String propertyString, final short propertyInt16,
      final String additionalPropString) {
    final ComplexValue value = complexValue(propertyString, propertyInt16);
    value.setTypeName("olingo.odata.test1.CTBase");
    value.getValue().add(new Property(null, "AdditionalPropString", ValueType.PRIMITIVE, additionalPropString));
    return value;
  }

  private static String streamed(final SerializerStreamResult result) throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    result.getODataContent().write(out);
    return out.toString(StandardCharsets.UTF_8);
  }

  private static String buffered(final SerializerResult result) throws Exception {
    try (InputStream in = result.getContent()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void primitiveCollectionStreamedMatchesBuffered() throws Exception {
    final List<String> values = Arrays.asList("Employee1@company.example", "Employee2@company.example", null);
    final EdmPrimitiveType type = odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String);
    final Property property = new Property(null, "CollPropertyString", ValueType.COLLECTION_PRIMITIVE, values);

    final String bufferedJson = buffered(serializer.primitiveCollection(metadata, type, property,
        PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).build()));
    final String streamedJson = streamed(serializer.primitiveCollectionStreamed(metadata, type,
        iterator("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, values),
        PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).build()));

    assertEquals(bufferedJson, streamedJson);
  }

  @Test
  void primitiveCollectionStreamedWritesTheCountBeforeTheValue() throws Exception {
    // [OData-JSON] 4.4: "If present and the streaming=true content-type parameter is set, it MUST
    // come before the value name/value pair."
    final CountOption count = new CountOptionImpl().setValue(true);
    final PropertyIterator properties =
        iterator("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, List.of("a", "b"));
    properties.setCount(2);

    final String json = streamed(serializer.primitiveCollectionStreamed(metadata,
        odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String), properties,
        PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).count(count).build()));

    assertTrue(json.indexOf("\"@odata.count\":2") < json.indexOf("\"value\""), json);
    assertTrue(json.indexOf("\"@odata.context\"") < json.indexOf("\"@odata.count\""), json);
  }

  @Test
  void primitiveCollectionStreamedWithCountMatchesBuffered() throws Exception {
    final CountOption count = new CountOptionImpl().setValue(true);
    final List<String> values = List.of("a", "b");
    final EdmPrimitiveType type = odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String);
    final Property property = new Property(null, "CollPropertyString", ValueType.COLLECTION_PRIMITIVE, values);
    property.setCount(values.size());
    final PropertyIterator properties = iterator("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, values);
    properties.setCount(values.size());

    assertEquals(
        buffered(serializer.primitiveCollection(metadata, type, property,
            PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).count(count).build())),
        streamed(serializer.primitiveCollectionStreamed(metadata, type, properties,
            PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).count(count).build())));
  }

  @Test
  void emptyPrimitiveCollectionStreamsAnEmptyArray() throws Exception {
    assertEquals("{\"@odata.context\":\"../$metadata#ESCollAllPrim(1)/CollPropertyString\","
        + "\"@odata.metadataEtag\":\"W/\\\"metadataETag\\\"\",\"value\":[]}",
        streamed(serializer.primitiveCollectionStreamed(metadata,
            odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String),
            iterator("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, List.of()),
            PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).build())));
  }

  @Test
  void complexCollectionStreamedMatchesBuffered() throws Exception {
    final List<ComplexValue> values = List.of(complexValue("Comp1", (short) 1), complexValue("Comp2", (short) 2));
    final EdmComplexType type = metadata.getEdm().getComplexType(ComplexTypeProvider.nameCTTwoPrim);
    final Property property = new Property(null, "CollPropertyComp", ValueType.COLLECTION_COMPLEX, values);

    assertEquals(
        buffered(serializer.complexCollection(metadata, type, property,
            ComplexSerializerOptions.with().contextURL(complexCollectionContextUrl).build())),
        streamed(serializer.complexCollectionStreamed(metadata, type,
            iterator("CollPropertyComp", ValueType.COLLECTION_COMPLEX, values),
            ComplexSerializerOptions.with().contextURL(complexCollectionContextUrl).build())));
  }

  @Test
  void complexCollectionStreamedWithDerivedTypeMatchesBuffered() throws Exception {
    // at least one element carries a derived type name, so the "@odata.type" emission condition
    // (derivedType != type) is actually exercised on both the buffered and the streamed side
    final List<ComplexValue> values = List.of(complexValue("Comp1", (short) 1),
        derivedComplexValue("Comp2", (short) 2, "ADD"));
    final EdmComplexType type = metadata.getEdm().getComplexType(ComplexTypeProvider.nameCTTwoPrim);
    final Property property = new Property(null, "CollPropertyComp", ValueType.COLLECTION_COMPLEX, values);

    assertEquals(
        buffered(serializer.complexCollection(metadata, type, property,
            ComplexSerializerOptions.with().contextURL(complexCollectionContextUrl).build())),
        streamed(serializer.complexCollectionStreamed(metadata, type,
            iterator("CollPropertyComp", ValueType.COLLECTION_COMPLEX, values),
            ComplexSerializerOptions.with().contextURL(complexCollectionContextUrl).build())));
  }

  @Test
  void streamedCollectionAtFullMetadataMatchesBuffered() throws Exception {
    // pins that the @odata.type line the full-metadata buffered writer emits is emitted by the
    // streamed writer too, in the same position
    final List<String> values = List.of("a");
    final EdmPrimitiveType type = odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String);
    final ODataSerializer full = odata.createSerializer(ContentType.JSON_FULL_METADATA);

    assertEquals(
        buffered(full.primitiveCollection(metadata, type,
            new Property(null, "CollPropertyString", ValueType.COLLECTION_PRIMITIVE, values),
            PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).build())),
        streamed(full.primitiveCollectionStreamed(metadata, type,
            iterator("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, values),
            PrimitiveSerializerOptions.with().contextURL(collectionContextUrl).build())));
  }

  @Test
  void xmlSerializerReportsStreamedCollectionsAsNotImplemented() throws Exception {
    // the XML serializer inherits the Task 1 defaults deliberately (see the deviation recorded in
    // the design spec); this pins that it reports rather than half-implements
    final ODataSerializer xml = odata.createSerializer(ContentType.APPLICATION_XML);
    final SerializerException e = assertThrows(SerializerException.class,
        () -> xml.primitiveCollectionStreamed(metadata,
            odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String),
            iterator("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, List.of("a")), null));
    assertEquals(SerializerException.MessageKeys.NOT_IMPLEMENTED, e.getMessageKey());
  }
}
