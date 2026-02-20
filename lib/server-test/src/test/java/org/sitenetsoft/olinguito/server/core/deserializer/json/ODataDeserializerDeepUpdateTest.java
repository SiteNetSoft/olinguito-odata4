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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.deserializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;

import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.data.DeletedEntity.Reason;
import org.sitenetsoft.olinguito.commons.api.data.Delta;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Link;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerResult;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption;
import org.sitenetsoft.olinguito.server.core.deserializer.AbstractODataDeserializerTest;
import org.junit.jupiter.api.Test;

class ODataDeserializerDeepUpdateTest extends AbstractODataDeserializerTest {

  @Test
  void unbalancedESAllPrim() throws Exception {
    final DeserializerResult result = deserializeWithResult("UnbalancedESAllPrimFeedUpdate.json");
    ExpandOption root = result.getExpandTree();
    assertEquals(1, root.getExpandItems().size());

    ExpandItem etTwoPrimManyLevel = root.getExpandItems().get(0);
    assertEquals("NavPropertyETTwoPrimMany", etTwoPrimManyLevel.getResourcePath().getUriResourceParts().get(0)
        .getSegmentValue());
    assertEquals(1, etTwoPrimManyLevel.getExpandOption().getExpandItems().size());

    ExpandItem etAllPrimOneLevel = etTwoPrimManyLevel.getExpandOption().getExpandItems().get(0);
    assertEquals("NavPropertyETAllPrimOne", etAllPrimOneLevel.getResourcePath().getUriResourceParts().get(0)
        .getSegmentValue());
    assertEquals(1, etAllPrimOneLevel.getExpandOption().getExpandItems().size());

    ExpandItem etTwoPrimOneLevel = etAllPrimOneLevel.getExpandOption().getExpandItems().get(0);
    assertEquals("NavPropertyETTwoPrimOne", etTwoPrimOneLevel.getResourcePath().getUriResourceParts().get(0)
        .getSegmentValue());
    assertNull(etTwoPrimOneLevel.getExpandOption());
  }

  @Test
  void esAllPrimExpandedToOne() throws Exception {
    final Entity entity = deserialize("EntityESAllPrimExpandedNavPropertyETTwoPrimOneUpdate.json");

    Link navigationLink = entity.getNavigationLink("NavPropertyETTwoPrimOne");
    assertNotNull(navigationLink);

    assertEquals("NavPropertyETTwoPrimOne", navigationLink.getTitle());
    assertEquals(Constants.ENTITY_NAVIGATION_LINK_TYPE, navigationLink.getType());
    assertNotNull(navigationLink.getInlineEntity());
    assertNull(navigationLink.getInlineEntitySet());
  }

  @Test
  void esAllPrimExpandedToOneWithODataAnnotations() throws Exception {
    deserialize("EntityESAllPrimExpandedNavPropertyETTwoPrimOneWithODataAnnotationsUpdate.json");
  }

  @Test
  void esAllPrimExpandedToMany() throws Exception {
    final Entity entity = deserialize("EntityESAllPrimExpandedNavPropertyETTwoPrimManyUpdate.json");

    Link navigationLink = entity.getNavigationLink("NavPropertyETTwoPrimMany");
    assertNotNull(navigationLink);

    assertEquals("NavPropertyETTwoPrimMany", navigationLink.getTitle());
    assertEquals(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE, navigationLink.getType());
    assertNull(navigationLink.getInlineEntity());
    assertNotNull(navigationLink.getInlineEntitySet());
    assertEquals(2, navigationLink.getInlineEntitySet().getEntities().size());
    assertNull(navigationLink.getInlineEntitySet().getEntities().get(0).getId());
    assertNotNull(navigationLink.getInlineEntitySet().getEntities().get(1).getId());
  }
  
  @Test
  void esAllPrimExpandedToManyError() throws Exception {
    try{
      deserialize("EntityESAllPrimExpandedNavPropertyETTwoPrimManyError.json");
      fail("Expected exception not thrown.");
    } catch (final DeserializerException e) {
      assertEquals(SerializerException.MessageKeys.MISSING_DELTA_PROPERTY, e.getMessageKey());
    }
  }
  
  @Test
  void esAllPrimExpandedToManyDelta() throws Exception {
    final Entity entity = deserialize("EntityESAllPrimExpandedNavPropertyETTwoPrimManyDelta.json");

    Link navigationLink = entity.getNavigationLink("NavPropertyETTwoPrimMany");
    assertNotNull(navigationLink);

    assertEquals("NavPropertyETTwoPrimMany", navigationLink.getTitle());
    assertEquals(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE, navigationLink.getType());
    assertNull(navigationLink.getInlineEntity());
    assertNotNull(navigationLink.getInlineEntitySet());
    assertEquals(3, navigationLink.getInlineEntitySet().getEntities().size());
    assertEquals("ESAllPrim(5)",navigationLink.getInlineEntitySet().getEntities().get(0).getId().toString());
    assertEquals("ESAllPrim(6)",navigationLink.getInlineEntitySet().getEntities().get(1).getId().toString());
    assertNull(navigationLink.getInlineEntitySet().getEntities().get(2).getId());
    Delta delta = ((Delta)navigationLink.getInlineEntitySet());
    assertEquals(2, delta.getDeletedEntities().size());
    assertNotNull(delta.getDeletedEntities().get(0).getId());
    assertNotNull(delta.getDeletedEntities().get(1).getId());
    assertEquals(Reason.deleted, delta.getDeletedEntities().get(0).getReason());
    assertEquals(Reason.changed, delta.getDeletedEntities().get(1).getReason());
  }

  @Test
  void esAllPrimExpandedToManyWithODataAnnotations() throws Exception {
    deserialize("EntityESAllPrimExpandedNavPropertyETTwoPrimManyWithODataAnnotationsUpdate.json");
  }


  @Test
  void expandedToOneInvalidNullValue() throws Exception {
    ODataJsonDeserializerEntityTest.expectException(
        "{\"PropertyInt16\":32767,"
            + "\"NavPropertyETTwoPrimOne\":null"
            + "}",
        "ETAllPrim",
        DeserializerException.MessageKeys.INVALID_NULL_PROPERTY);
  }

  @Test
  void expandedToOneValidNullValue() throws Exception {
    final Entity entity = ODataJsonDeserializerEntityTest.deserialize(
        "{\"PropertyInt16\":32767,"
            + "\"NavPropertyETAllPrimOne\":null"
            + "}",
        "ETTwoPrim");

    assertEquals(1, entity.getNavigationLinks().size());
    final Link link = entity.getNavigationLinks().get(0);

    assertEquals("NavPropertyETAllPrimOne", link.getTitle());
    assertNull(link.getInlineEntity());
    assertNull(link.getInlineEntitySet());
  }

  @Test
  void expandedToOneInvalidStringValue() throws Exception {
    ODataJsonDeserializerEntityTest.expectException(
        "{\"PropertyInt16\":32767,"
            + "\"NavPropertyETTwoPrimOne\":\"First Resource - positive values\""
            + "}",
        "ETAllPrim",
        DeserializerException.MessageKeys.INVALID_VALUE_FOR_NAVIGATION_PROPERTY);
  }

  @Test
  void expandedToManyInvalidNullValue() throws Exception {
    ODataJsonDeserializerEntityTest.expectException(
        "{\"PropertyInt16\":32767,"
            + "\"NavPropertyETTwoPrimMany\":null"
            + "}",
        "ETAllPrim",
        DeserializerException.MessageKeys.INVALID_NULL_PROPERTY);
  }

  @Test
  void expandedToManyInvalidStringValue() throws Exception {
    ODataJsonDeserializerEntityTest.expectException(
        "{\"PropertyInt16\":32767,"
            + "\"NavPropertyETTwoPrimMany\":\"First Resource - positive values\""
            + "}",
        "ETAllPrim",
        DeserializerException.MessageKeys.INVALID_VALUE_FOR_NAVIGATION_PROPERTY);
  }

  private Entity deserialize(final String resourceName) throws IOException, DeserializerException {
    return deserializeWithResult(resourceName).getEntity();
  }

  private DeserializerResult deserializeWithResult(final String resourceName) throws IOException,
      DeserializerException {
    InputStream stream = getFileAsStream(resourceName);
    final EdmEntityType entityType = edm.getEntityType(new FullQualifiedName(NAMESPACE, "ETAllPrim"));
    return ODataJsonDeserializerEntityTest.deserializeWithResultv401(stream ,
        entityType, ContentType.JSON);
  }
}
