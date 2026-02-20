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
 * Copyright 2026 SiteNetSoft - Improved test assertions
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.deserializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.core.deserializer.AbstractODataDeserializerTest;
import org.junit.jupiter.api.Test;

class ODataDeserializerEntityCollectionTest extends AbstractODataDeserializerTest {

  @Test
  void esAllPrim() throws Exception {
    final EntityCollection entitySet = deserialize(getFileAsStream("ESAllPrim.json"), "ETAllPrim");
    assertNotNull(entitySet);
    assertEquals(3, entitySet.getEntities().size());

    // Check first entity
    Entity entity = entitySet.getEntities().get(0);
    List<Property> properties = entity.getProperties();
    assertNotNull(properties);
    assertEquals(16, properties.size());

    assertEquals(Short.valueOf((short) 32767), entity.getProperty("PropertyInt16").getValue());
    assertEquals("First Resource - positive values", entity.getProperty("PropertyString").getValue());
    assertTrue((Boolean) entity.getProperty("PropertyBoolean").getValue());
    assertEquals(Short.valueOf((short) 255), entity.getProperty("PropertyByte").getValue());
    assertEquals(Byte.valueOf((byte) 127), entity.getProperty("PropertySByte").getValue());
    assertEquals(Integer.valueOf(2147483647), entity.getProperty("PropertyInt32").getValue());
    assertEquals(Long.valueOf(9223372036854775807l), entity.getProperty("PropertyInt64").getValue());
    assertEquals(Float.valueOf(1.79E20f), entity.getProperty("PropertySingle").getValue());
    assertEquals(Double.valueOf(-1.79E19), entity.getProperty("PropertyDouble").getValue());
    assertEquals(new BigDecimal(34), entity.getProperty("PropertyDecimal").getValue());
    assertNotNull(entity.getProperty("PropertyBinary").getValue());
    assertNotNull(entity.getProperty("PropertyDate").getValue());
    assertNotNull(entity.getProperty("PropertyDateTimeOffset").getValue());
    assertNotNull(entity.getProperty("PropertyDuration").getValue());
    assertNotNull(entity.getProperty("PropertyGuid").getValue());
    assertNotNull(entity.getProperty("PropertyTimeOfDay").getValue());
  }

  @Test
  void eSCompCollComp() throws Exception {
    final EntityCollection entitySet = deserialize(getFileAsStream("ESCompCollComp.json"), "ETCompCollComp");
    assertNotNull(entitySet);
    assertEquals(2, entitySet.getEntities().size());

    // Since entity deserialization is called we do not check all entities here excplicitly
  }

  @Test
  void esAllPrimODataAnnotationsAreIgnored() throws Exception {
    deserialize(getFileAsStream("ESAllPrimWithODataAnnotations.json"), "ETAllPrim");
  }

  @Test
  void emptyETAllPrim() throws Exception {
    String entityCollectionString = "{\"value\" : []}";
    final EntityCollection entityCollection = deserialize(entityCollectionString, "ETAllPrim");
    assertNotNull(entityCollection.getEntities());
    assertTrue(entityCollection.getEntities().isEmpty());
  }

  @Test
  void esAllPrimCustomAnnotationsLeadToNotImplemented() throws Exception {
    expectException(getFileAsStream("ESAllPrimWithCustomAnnotations.json"), "ETAllPrim",
        DeserializerException.MessageKeys.NOT_IMPLEMENTED);
  }

  @Test
  void esAllPrimDoubleKeysLeadToException() throws Exception {
    expectException(getFileAsStream("ESAllPrimWithDoubleKey.json"), "ETAllPrim",
        DeserializerException.MessageKeys.DUPLICATE_PROPERTY);
  }

  @Test
  void wrongValueTagJsonValueNull() throws Exception {
    expectException("{\"value\" : null}", "ETAllPrim",
        DeserializerException.MessageKeys.VALUE_TAG_MUST_BE_AN_ARRAY);
  }

  @Test
  void wrongValueTagJsonValueNumber() throws Exception {
    expectException("{\"value\" : 1234}", "ETAllPrim",
        DeserializerException.MessageKeys.VALUE_TAG_MUST_BE_AN_ARRAY);
  }

  @Test
  void wrongValueTagJsonValueObject() throws Exception {
    expectException("{\"value\" : {}}", "ETAllPrim",
        DeserializerException.MessageKeys.VALUE_TAG_MUST_BE_AN_ARRAY);
  }

  @Test
  void valueTagMissing() throws Exception {
    expectException("{}", "ETAllPrim",
        DeserializerException.MessageKeys.VALUE_ARRAY_NOT_PRESENT);
  }

  @Test
  void wrongValueInValueArrayNumber() throws Exception {
    expectException("{\"value\" : [1234,123]}", "ETAllPrim",
        DeserializerException.MessageKeys.INVALID_ENTITY);
  }

  @Test
  void wrongValueInValueArrayNestedArray() throws Exception {
    expectException("{\"value\" : [[],[]]}", "ETAllPrim",
        DeserializerException.MessageKeys.INVALID_ENTITY);
  }

  @Test
  void invalidJsonSyntax() throws Exception {
    expectException("{\"value\" : }", "ETAllPrim",
        DeserializerException.MessageKeys.JSON_SYNTAX_EXCEPTION);
  }

  @Test
  void emptyInput() throws Exception {
    expectException("", "ETAllPrim", DeserializerException.MessageKeys.JSON_SYNTAX_EXCEPTION);
  }

  @Test
  void unknownContentInCollection() throws Exception {
    expectException("{\"value\":[],\"unknown\":null}", "ETAllPrim",
        DeserializerException.MessageKeys.UNKNOWN_CONTENT);
  }

  @Test
  void customAnnotationNotSupportedYet() throws Exception {
    expectException("{\"value\": [], \"@custom.annotation\": null}", "ETAllPrim",
        DeserializerException.MessageKeys.NOT_IMPLEMENTED);
  }

  private EntityCollection deserialize(final InputStream stream, final String entityTypeName)
      throws DeserializerException {
    return OData.newInstance().createDeserializer(ContentType.JSON, metadata)
        .entityCollection(stream, edm.getEntityType(new FullQualifiedName(NAMESPACE, entityTypeName)))
        .getEntityCollection();
  }

  private EntityCollection deserialize(final String input, final String entityTypeName)
      throws DeserializerException {
    return OData.newInstance().createDeserializer(ContentType.JSON, metadata)
        .entityCollection(new ByteArrayInputStream(input.getBytes()),
            edm.getEntityType(new FullQualifiedName(NAMESPACE, entityTypeName)))
        .getEntityCollection();
  }

  private void expectException(final InputStream stream, final String entityTypeName,
      final DeserializerException.MessageKeys messageKey) {
    try {
      deserialize(stream, entityTypeName);
      fail("Expected exception not thrown.");
    } catch (final DeserializerException e) {
      assertEquals(messageKey, e.getMessageKey());
    }
  }

  private void expectException(final String entityCollectionString, final String entityTypeName,
    final DeserializerException.MessageKeys messageKey) {
    expectException(new ByteArrayInputStream(entityCollectionString.getBytes()), entityTypeName, messageKey);
  }
}
