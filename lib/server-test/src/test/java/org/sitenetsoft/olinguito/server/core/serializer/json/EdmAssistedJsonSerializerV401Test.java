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
 * Copyright 2026 SiteNetSoft - Fixed deprecated API usages and code quality warnings
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.serializer.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.sitenetsoft.olinguito.commons.api.data.AbstractEntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Link;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.EdmAssistedSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.EdmAssistedSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.tecsvc.MetadataETagSupport;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EdmAssistedJsonSerializerV401Test {
  private static final OData oData = OData.newInstance();
  private static final ServiceMetadata metadata = oData.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList(), null);
  private static final EdmEntityContainer entityContainer = metadata.getEdm().getEntityContainer();
  private final EdmAssistedSerializer serializer;
  private final EdmAssistedSerializer serializerMin;
  private final EdmAssistedSerializer serializerNone;

  public EdmAssistedJsonSerializerV401Test() throws SerializerException {
	  List<String> versions = new ArrayList<>();
	  versions.add("4.01");
	  versions.add("4");
    serializer = oData.createEdmAssistedSerializer(ContentType.JSON_FULL_METADATA, versions);
    serializerMin = oData.createEdmAssistedSerializer(ContentType.JSON, versions);
    serializerNone = oData.createEdmAssistedSerializer(ContentType.JSON_NO_METADATA, versions);
  }

  @Test
  void entityCollectionSimple() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1.25F));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1)\","
        + "\"value\":[{\"@id\":null,\"Property1@type\":\"#Single\",\"Property1\":1.25}]}",
        serialize(serializer, metadata, null, entityCollection, null));
  }

  @Test
  void entityCollectionWithEdm() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESTwoPrim");
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "test"))
        .addProperty(new Property(null, "AdditionalProperty", ValueType.PRIMITIVE, (byte) 42));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#ESTwoPrim\",\"value\":[{\"@id\":null,"
        + "\"PropertyInt16\":1,\"PropertyString\":\"test\","
        + "\"AdditionalProperty@type\":\"#SByte\",\"AdditionalProperty\":42}]}",
        serialize(serializer, metadata, entitySet, entityCollection, null));
  }

  @Test
  void entityCollection() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property0", ValueType.PRIMITIVE, null))
        .addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1));
    Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    date.clear();
    date.set(2000, Calendar.FEBRUARY, 29);
    entity.addProperty(new Property("Edm.Date", "Property2", ValueType.PRIMITIVE, date))
        .addProperty(new Property("Edm.DateTimeOffset", "Property3", ValueType.PRIMITIVE, date))
        .addProperty(new Property(null, "Property4", ValueType.COLLECTION_PRIMITIVE,
            Arrays.asList(true, false, null)));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    entityCollection.setCount(2);
    entityCollection.setNext(URI.create("nextLink"));
    Assertions.assertEquals(
        "{\"@context\":\"$metadata#EntitySet(Property0,Property1,Property2,Property3,Property4)\","
            + "\"@count\":2,"
            + "\"value\":[{\"@id\":null,"
            + "\"Property0\":null,"
            + "\"Property1@type\":\"#Int32\",\"Property1\":1,"
            + "\"Property2@type\":\"#Date\",\"Property2\":\"2000-02-29\","
            + "\"Property3@type\":\"#DateTimeOffset\",\"Property3\":\"2000-02-29T00:00:00Z\","
            + "\"Property4@type\":\"#Collection(Boolean)\",\"Property4\":[true,false,null]}],"
            + "\"@nextLink\":\"nextLink\"}",
        serialize(serializer, metadata, null, entityCollection, null));
  }

  @Test
  void entityCollectionIEEE754Compatible() throws Exception {
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, Long.MIN_VALUE))
        .addProperty(new Property(null, "Property2", ValueType.PRIMITIVE, BigDecimal.valueOf(Long.MAX_VALUE, 10)))
        .addProperty(new Property("Edm.Byte", "Property3", ValueType.PRIMITIVE, 20)));
    entityCollection.setCount(3);
    Assertions.assertEquals(
        "{\"@odata.context\":\"$metadata#EntitySet(Property1,Property2,Property3)\","
            + "\"@odata.count\":\"3\","
            + "\"value\":[{\"@odata.id\":null,"
            + "\"Property1@odata.type\":\"#Int64\",\"Property1\":\"-9223372036854775808\","
            + "\"Property2@odata.type\":\"#Decimal\",\"Property2\":\"922337203.6854775807\","
            + "\"Property3@odata.type\":\"#Byte\",\"Property3\":20}]}",
        serialize(
            oData.createEdmAssistedSerializer(
                ContentType.create(ContentType.JSON_FULL_METADATA, ContentType.PARAMETER_IEEE754_COMPATIBLE, "true")),
            metadata, null, entityCollection, null));
    
    List<String> versions = new ArrayList<>();
    versions.add("4.01");
    Assertions.assertEquals(
            "{\"@context\":\"$metadata#EntitySet(Property1,Property2,Property3)\","
                + "\"@count\":\"3\","
                + "\"value\":[{\"@id\":null,"
                + "\"Property1@type\":\"#Int64\",\"Property1\":\"-9223372036854775808\","
                + "\"Property2@type\":\"#Decimal\",\"Property2\":\"922337203.6854775807\","
                + "\"Property3@type\":\"#Byte\",\"Property3\":20}]}",
            serialize(
                oData.createEdmAssistedSerializer(
                    ContentType.create(ContentType.JSON_FULL_METADATA, 
                    		ContentType.PARAMETER_IEEE754_COMPATIBLE, "true"), versions), 
                metadata, null, entityCollection, null));
  }

  @Test
  void entityCollectionWithComplexProperty() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1L));
    ComplexValue complexValue = new ComplexValue();
    complexValue.getValue().add(new Property(null, "Inner1", ValueType.PRIMITIVE,
        BigDecimal.TEN.scaleByPowerOfTen(-5)));
    Calendar time = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    time.clear();
    time.set(Calendar.HOUR_OF_DAY, 13);
    time.set(Calendar.SECOND, 59);
    time.set(Calendar.MILLISECOND, 999);
    complexValue.getValue().add(new Property("Edm.TimeOfDay", "Inner2", ValueType.PRIMITIVE, time));
    entity.addProperty(new Property("Namespace.ComplexType", "Property2", ValueType.COMPLEX, complexValue));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1,Property2)\","
        + "\"value\":[{\"@id\":null,"
        + "\"Property1@type\":\"#Int64\",\"Property1\":1,"
        + "\"Property2\":{\"@type\":\"#Namespace.ComplexType\","
        + "\"Inner1@type\":\"#Decimal\",\"Inner1\":0.00010,"
        + "\"Inner2@type\":\"#TimeOfDay\",\"Inner2\":\"13:00:59.999\"}}]}",
        serialize(serializer, metadata, null, entityCollection, null));
  }

  @Test
  void entityCollectionWithComplexCollection() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    ComplexValue complexValue1 = new ComplexValue();
    complexValue1.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 1));
    complexValue1.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "one"));
    ComplexValue complexValue2 = new ComplexValue();
    complexValue2.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 2));
    complexValue2.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "two"));
    ComplexValue complexValue3 = new ComplexValue();
    complexValue3.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 3));
    complexValue3.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "three"));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "CollPropertyComp", ValueType.COLLECTION_COMPLEX,
            List.of(complexValue1, complexValue2, complexValue3))));
    Assertions.assertEquals("{\"@context\":\"$metadata#ESMixPrimCollComp(CollPropertyComp)\","
        + "\"value\":[{\"@id\":null,"
        + "\"CollPropertyComp\":["
        + "{\"PropertyInt16\":1,\"PropertyString\":\"one\"},"
        + "{\"PropertyInt16\":2,\"PropertyString\":\"two\"},"
        + "{\"PropertyInt16\":3,\"PropertyString\":\"three\"}]}]}",
        serialize(serializer, metadata, entitySet, entityCollection, "CollPropertyComp"));
  }

  @Test
  void entityCollectionWithEmptyCollection() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "CollPropertyString", ValueType.COLLECTION_PRIMITIVE,
            Collections.emptyList())));
    Assertions.assertEquals(
        "{\"@context\":\"$metadata#ESMixPrimCollComp(CollPropertyString)\","
            + "\"value\":[{\"@id\":null,\"CollPropertyString\":[]}]}",
        serialize(serializer, metadata, entitySet, entityCollection, "CollPropertyString"));
  }

  @Test
  void expand() throws Exception {
    final Entity relatedEntity1 = new Entity().addProperty(new Property(null, "Related1", ValueType.PRIMITIVE, 1.5));
    final Entity relatedEntity2 = new Entity().addProperty(new Property(null, "Related1", ValueType.PRIMITIVE, 2.75));
    EntityCollection target = new EntityCollection();
    target.getEntities().add(relatedEntity1);
    target.getEntities().add(relatedEntity2);
    Link link = new Link();
    link.setTitle("NavigationProperty");
    link.setInlineEntitySet(target);
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, (short) 1));
    entity.getNavigationLinks().add(link);
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1,NavigationProperty(Related1))\","
        + "\"value\":[{\"@id\":null,"
        + "\"Property1@type\":\"#Int16\",\"Property1\":1,"
        + "\"NavigationProperty\":["
        + "{\"@id\":null,\"Related1@type\":\"#Double\",\"Related1\":1.5},"
        + "{\"@id\":null,\"Related1@type\":\"#Double\",\"Related1\":2.75}]}]}",
        serialize(serializer, metadata, null, entityCollection, "Property1,NavigationProperty(Related1)"));
  }

  @Test
  void expandWithEdm() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESTwoPrim");
    Entity entity = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 42))
        .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "test"));
    final Entity target = new Entity()
        .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 2))
        .addProperty(new Property(null, "PropertyByte", ValueType.PRIMITIVE, 3L));
    Link link = new Link();
    link.setTitle("NavPropertyETAllPrimOne");
    link.setInlineEntity(target);
    entity.getNavigationLinks().add(link);
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#ESTwoPrim\",\"value\":[{\"@id\":null,"
        + "\"PropertyInt16\":42,\"PropertyString\":\"test\","
        + "\"NavPropertyETAllPrimOne\":{\"@id\":null,\"PropertyInt16\":2,\"PropertyByte\":3}}]}",
        serialize(serializer, metadata, entitySet, entityCollection, null));
  }

  @Test
  void metadata() throws Exception {
    final ServiceMetadata metadata = oData.createServiceMetadata(null, Collections.emptyList(),
        new MetadataETagSupport("W/\"42\""));
    Entity entity = new Entity();
    entity.setType("Namespace.EntityType");
    entity.setId(URI.create("ID"));
    entity.setETag("W/\"1000\"");
    Link link = new Link();
    link.setHref("editLink");
    entity.setEditLink(link);
    entity.setMediaContentSource(URI.create("media"));
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE,
        UUID.fromString("12345678-ABCD-1234-CDEF-123456789012")));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1)\","
        + "\"@metadataEtag\":\"W/\\\"42\\\"\",\"value\":[{"
        + "\"@etag\":\"W/\\\"1000\\\"\","
        + "\"@type\":\"#Namespace.EntityType\","
        + "\"@id\":\"ID\","
        + "\"Property1@type\":\"#Guid\",\"Property1\":\"12345678-abcd-1234-cdef-123456789012\","
        + "\"@editLink\":\"editLink\","
        + "\"@mediaReadLink\":\"editLink/$value\"}]}",
        serialize(serializer, metadata, null, entityCollection, null));

    Assertions.assertEquals("{\"value\":[{\"Property1\":\"12345678-abcd-1234-cdef-123456789012\"}]}",
        serialize(oData.createEdmAssistedSerializer(ContentType.JSON_NO_METADATA), metadata,
            null, entityCollection, null));
  }

  @Test
  void enumType() throws Exception {
      assertThrows(SerializerException.class, () -> {
          EntityCollection entityCollection = new EntityCollection();
          entityCollection.getEntities().add(
          new Entity().addProperty(new Property(null, "Property1", ValueType.ENUM, 42)));
          serializer.entityCollection(metadata, null, entityCollection, null);
      });
  }

  @Test
  void collectionEnumType() throws Exception {
      assertThrows(SerializerException.class, () -> {
          EntityCollection entityCollection = new EntityCollection();
          entityCollection.getEntities().add(
          new Entity().addProperty(new Property(null, "Property1", ValueType.COLLECTION_ENUM, List.of(42))));
          serializer.entityCollection(metadata, null, entityCollection, null);
      });
  }

  @Test
  void geoType() throws Exception {
      assertThrows(SerializerException.class, () -> {
          EntityCollection entityCollection = new EntityCollection();
          entityCollection.getEntities().add(
          new Entity().addProperty(new Property(null, "Property1", ValueType.GEOSPATIAL, 1)));
          serializer.entityCollection(metadata, null, entityCollection, null);
      });
  }

  @Test
  void unsupportedType() throws Exception {
      assertThrows(SerializerException.class, () -> {
          EntityCollection entityCollection = new EntityCollection();
          entityCollection.getEntities().add(
          new Entity().addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, TimeZone.getDefault())));
          serializer.entityCollection(metadata, null, entityCollection, null);
      });
  }

  @Test
  void wrongValueForType() throws Exception {
      assertThrows(SerializerException.class, () -> {
          EntityCollection entityCollection = new EntityCollection();
          entityCollection.getEntities().add(
          new Entity().addProperty(new Property("Edm.SByte", "Property1", ValueType.PRIMITIVE, "-1")));
          serializer.entityCollection(metadata, null, entityCollection, null);
      });
  }

  @Test
  void wrongValueForPropertyFacet() throws Exception {
      assertThrows(SerializerException.class, () -> {
          EntityCollection entityCollection = new EntityCollection();
          entityCollection.getEntities().add(
          new Entity().addProperty(
          new Property(null, "PropertyDecimal", ValueType.PRIMITIVE, BigDecimal.ONE.scaleByPowerOfTen(-11))));
          serializer.entityCollection(metadata,
              entityContainer.getEntitySet("ESAllPrim").getEntityType(), entityCollection,
              null);
      });
  }

  @Test
  void wrongValueForPropertyFacetInComplexProperty() throws Exception {
      assertThrows(SerializerException.class, () -> {
          ComplexValue innerComplexValue = new ComplexValue();
          innerComplexValue.getValue().add(new Property(null, "PropertyDecimal", ValueType.PRIMITIVE,
          BigDecimal.ONE.scaleByPowerOfTen(-6)));
          ComplexValue complexValue = new ComplexValue();
          complexValue.getValue().add(new Property(null, "PropertyComp", ValueType.COMPLEX,
          innerComplexValue));
          EntityCollection entityCollection = new EntityCollection();
          entityCollection.getEntities().add(
          new Entity().addProperty(
          new Property(null, "CollPropertyComp", ValueType.COLLECTION_COMPLEX,
          Collections.singletonList(complexValue))));
          serializer.entityCollection(metadata,
              entityContainer.getEntitySet("ESKeyNav").getEntityType(), entityCollection,
              null);
      });
  }

  private String serialize(final EdmAssistedSerializer serializer, final ServiceMetadata metadata,
      final EdmEntitySet edmEntitySet, final AbstractEntityCollection entityCollection, final String selectList)
      throws SerializerException, IOException {
    ContextURL.Builder contextURLBuilder = ContextURL.with();
    if (edmEntitySet == null) {
      contextURLBuilder.entitySetOrSingletonOrType("EntitySet");
    } else {
      contextURLBuilder.entitySet(edmEntitySet);
    }
    if (selectList == null && entityCollection != null) {
      if (edmEntitySet == null) {
        StringBuilder names = new StringBuilder();
        for (final Property property :
          entityCollection.iterator().next().getProperties()) {
          names.append(!names.isEmpty() ? ',' : "").append(property.getName());
        }
        contextURLBuilder.selectList(names.toString());
      }
    } else {
      contextURLBuilder.selectList(selectList);
    }
    return new String(
        serializer.entityCollection(metadata,
            edmEntitySet == null ? null : edmEntitySet.getEntityType(),
            entityCollection,
            EdmAssistedSerializerOptions.with().contextURL(contextURLBuilder.build()).build())
            .getContent().readAllBytes(), StandardCharsets.UTF_8);
  }
  
  @Test
  void entityCollectionSimpleMetadataMin() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1.25F));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1)\","
         + "\"value\":[{\"Property1\":1.25}]}",
        serialize(serializerMin, metadata, null, entityCollection, null));
  }
  
  @Test
  void entityCollectionSimpleMetadataNone() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1.25F));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"value\":[{\"Property1\":1.25}]}",
        serialize(serializerNone, metadata, null, entityCollection, null));
  }
  
  @Test
  void entityCollectionMetadataMin() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property0", ValueType.PRIMITIVE, null))
        .addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1));
    Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    date.clear();
    date.set(2000, Calendar.FEBRUARY, 29);
    entity.addProperty(new Property("Edm.Date", "Property2", ValueType.PRIMITIVE, date))
        .addProperty(new Property("Edm.DateTimeOffset", "Property3", ValueType.PRIMITIVE, date))
        .addProperty(new Property(null, "Property4", ValueType.COLLECTION_PRIMITIVE,
            Arrays.asList(true, false, null)));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    entityCollection.setCount(2);
    entityCollection.setNext(URI.create("nextLink"));
    Assertions.assertEquals(
        "{\"@context\":\"$metadata#EntitySet(Property0,Property1,Property2,Property3,Property4)\","
             + "\"@count\":2,"
             + "\"value\":[{"
             + "\"Property0\":null,"
             + "\"Property1\":1,"
             + "\"Property2\":\"2000-02-29\","
             + "\"Property3\":\"2000-02-29T00:00:00Z\","
             + "\"Property4\":[true,false,null]}],"
             + "\"@nextLink\":\"nextLink\"}",
        serialize(serializerMin, metadata, null, entityCollection, null));
  }
  
  @Test
  void entityCollectionMetadataNone() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property0", ValueType.PRIMITIVE, null))
        .addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1));
    Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    date.clear();
    date.set(2000, Calendar.FEBRUARY, 29);
    entity.addProperty(new Property("Edm.Date", "Property2", ValueType.PRIMITIVE, date))
        .addProperty(new Property("Edm.DateTimeOffset", "Property3", ValueType.PRIMITIVE, date))
        .addProperty(new Property(null, "Property4", ValueType.COLLECTION_PRIMITIVE,
            Arrays.asList(true, false, null)));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    entityCollection.setCount(2);
    entityCollection.setNext(URI.create("nextLink"));
    Assertions.assertEquals(
        "{"
             + "\"@count\":2,"
             + "\"value\":[{"
             + "\"Property0\":null,"
             + "\"Property1\":1,"
             + "\"Property2\":\"2000-02-29\","
             + "\"Property3\":\"2000-02-29T00:00:00Z\","
             + "\"Property4\":[true,false,null]}],"
             + "\"@nextLink\":\"nextLink\"}",
        serialize(serializerNone, metadata, null, entityCollection, null));
  }
  
  @Test
  void entityCollectionWithComplexPropertyMetadataMin() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1L));
    ComplexValue complexValue = new ComplexValue();
    complexValue.getValue().add(new Property(null, "Inner1", ValueType.PRIMITIVE,
        BigDecimal.TEN.scaleByPowerOfTen(-5)));
    Calendar time = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    time.clear();
    time.set(Calendar.HOUR_OF_DAY, 13);
    time.set(Calendar.SECOND, 59);
    time.set(Calendar.MILLISECOND, 999);
    complexValue.getValue().add(new Property("Edm.TimeOfDay", "Inner2", ValueType.PRIMITIVE, time));
    entity.addProperty(new Property("Namespace.ComplexType", "Property2", ValueType.COMPLEX, complexValue));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1,Property2)\","
         + "\"value\":[{"
         + "\"Property1\":1,"
         + "\"Property2\":{"
         + "\"Inner1\":0.00010,"
         + "\"Inner2\":\"13:00:59.999\"}}]}",
        serialize(serializerMin, metadata, null, entityCollection, null));
  }
  
  @Test
  void entityCollectionWithComplexPropertyMetadataNone() throws Exception {
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, 1L));
    ComplexValue complexValue = new ComplexValue();
    complexValue.getValue().add(new Property(null, "Inner1", ValueType.PRIMITIVE,
        BigDecimal.TEN.scaleByPowerOfTen(-5)));
    Calendar time = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    time.clear();
    time.set(Calendar.HOUR_OF_DAY, 13);
    time.set(Calendar.SECOND, 59);
    time.set(Calendar.MILLISECOND, 999);
    complexValue.getValue().add(new Property("Edm.TimeOfDay", "Inner2", ValueType.PRIMITIVE, time));
    entity.addProperty(new Property("Namespace.ComplexType", "Property2", ValueType.COMPLEX, complexValue));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{"
         + "\"value\":[{"
         + "\"Property1\":1,"
         + "\"Property2\":{"
         + "\"Inner1\":0.00010,"
         + "\"Inner2\":\"13:00:59.999\"}}]}",
        serialize(serializerNone, metadata, null, entityCollection, null));
  }

  @Test
  void entityCollectionWithComplexCollectionMin() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    ComplexValue complexValue1 = new ComplexValue();
    complexValue1.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 1));
    complexValue1.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "one"));
    ComplexValue complexValue2 = new ComplexValue();
    complexValue2.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 2));
    complexValue2.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "two"));
    ComplexValue complexValue3 = new ComplexValue();
    complexValue3.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 3));
    complexValue3.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "three"));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "CollPropertyComp", ValueType.COLLECTION_COMPLEX,
            List.of(complexValue1, complexValue2, complexValue3))));
    Assertions.assertEquals("{\"@context\":\"$metadata#ESMixPrimCollComp(CollPropertyComp)\","
         + "\"value\":[{"
         + "\"CollPropertyComp\":["
         + "{\"PropertyInt16\":1,\"PropertyString\":\"one\"},"
         + "{\"PropertyInt16\":2,\"PropertyString\":\"two\"},"
         + "{\"PropertyInt16\":3,\"PropertyString\":\"three\"}]}]}",
        serialize(serializerMin, metadata, entitySet, entityCollection, "CollPropertyComp"));
  }
  
  @Test
  void entityCollectionWithComplexCollectionNone() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    ComplexValue complexValue1 = new ComplexValue();
    complexValue1.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 1));
    complexValue1.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "one"));
    ComplexValue complexValue2 = new ComplexValue();
    complexValue2.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 2));
    complexValue2.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "two"));
    ComplexValue complexValue3 = new ComplexValue();
    complexValue3.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, 3));
    complexValue3.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, "three"));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "CollPropertyComp", ValueType.COLLECTION_COMPLEX,
            List.of(complexValue1, complexValue2, complexValue3))));
    Assertions.assertEquals("{"
         + "\"value\":[{"
         + "\"CollPropertyComp\":["
         + "{\"PropertyInt16\":1,\"PropertyString\":\"one\"},"
         + "{\"PropertyInt16\":2,\"PropertyString\":\"two\"},"
         + "{\"PropertyInt16\":3,\"PropertyString\":\"three\"}]}]}",
        serialize(serializerNone, metadata, entitySet, entityCollection, "CollPropertyComp"));
  }

  @Test
  void entityCollectionWithEmptyCollectionMin() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "CollPropertyString", ValueType.COLLECTION_PRIMITIVE,
            Collections.emptyList())));
    Assertions.assertEquals(
        "{\"@context\":\"$metadata#ESMixPrimCollComp(CollPropertyString)\","
             + "\"value\":[{\"CollPropertyString\":[]}]}",
        serialize(serializerMin, metadata, entitySet, entityCollection, "CollPropertyString"));
  }
  
  @Test
  void entityCollectionWithEmptyCollectionNone() throws Exception {
    final EdmEntitySet entitySet = entityContainer.getEntitySet("ESMixPrimCollComp");
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "CollPropertyString", ValueType.COLLECTION_PRIMITIVE,
            Collections.emptyList())));
    Assertions.assertEquals(
        "{"
             + "\"value\":[{\"CollPropertyString\":[]}]}",
        serialize(serializerNone, metadata, entitySet, entityCollection, "CollPropertyString"));
  }

  @Test
  void expandMetadataMin() throws Exception {
    final Entity relatedEntity1 = new Entity().addProperty(new Property(null, "Related1", ValueType.PRIMITIVE, 1.5));
    final Entity relatedEntity2 = new Entity().addProperty(new Property(null, "Related1", ValueType.PRIMITIVE, 2.75));
    EntityCollection target = new EntityCollection();
    target.getEntities().add(relatedEntity1);
    target.getEntities().add(relatedEntity2);
    Link link = new Link();
    link.setTitle("NavigationProperty");
    link.setInlineEntitySet(target);
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, (short) 1));
    entity.getNavigationLinks().add(link);
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1,NavigationProperty(Related1))\","
         + "\"value\":[{"
         + "\"Property1\":1,"
         + "\"NavigationProperty\":["
         + "{\"Related1\":1.5},"
         + "{\"Related1\":2.75}]}]}",
        serialize(serializerMin, metadata, null, entityCollection, "Property1,NavigationProperty(Related1)"));
  }

  @Test
  void expandMetadataNone() throws Exception {
    final Entity relatedEntity1 = new Entity().addProperty(new Property(null, "Related1", ValueType.PRIMITIVE, 1.5));
    final Entity relatedEntity2 = new Entity().addProperty(new Property(null, "Related1", ValueType.PRIMITIVE, 2.75));
    EntityCollection target = new EntityCollection();
    target.getEntities().add(relatedEntity1);
    target.getEntities().add(relatedEntity2);
    Link link = new Link();
    link.setTitle("NavigationProperty");
    link.setInlineEntitySet(target);
    Entity entity = new Entity();
    entity.setId(null);
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, (short) 1));
    entity.getNavigationLinks().add(link);
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{"
        + "\"value\":[{"
        + "\"Property1\":1,"
        + "\"NavigationProperty\":["
        + "{\"Related1\":1.5},"
        + "{\"Related1\":2.75}]}]}",
        serialize(serializerNone, metadata, null, entityCollection, "Property1,NavigationProperty(Related1)"));
  }

  @Test
  void metadataMin() throws Exception {
    final ServiceMetadata metadata = oData.createServiceMetadata(null, Collections.emptyList(),
        new MetadataETagSupport("W/\"42\""));
    Entity entity = new Entity();
    entity.setType("Namespace.EntityType");
    entity.setId(URI.create("ID"));
    entity.setETag("W/\"1000\"");
    Link link = new Link();
    link.setHref("editLink");
    entity.setEditLink(link);
    entity.setMediaContentSource(URI.create("media"));
    entity.addProperty(new Property(null, "Property1", ValueType.PRIMITIVE,
        UUID.fromString("12345678-ABCD-1234-CDEF-123456789012")));
    EntityCollection entityCollection = new EntityCollection();
    entityCollection.getEntities().add(entity);
    Assertions.assertEquals("{\"@context\":\"$metadata#EntitySet(Property1)\","
        + "\"@metadataEtag\":\"W/\\\"42\\\"\",\"value\":[{"
        + "\"@etag\":\"W/\\\"1000\\\"\","
        + "\"Property1\":\"12345678-abcd-1234-cdef-123456789012\","
        + "\"@editLink\":\"editLink\","
        + "\"@mediaReadLink\":\"editLink/$value\"}]}",
        serialize(serializerMin, metadata, null, entityCollection, null));
  }
  
  @Test
  void entityCollectionWithBigDecimalProperty() throws Exception {
    EntityCollection entityCollection = new EntityCollection();
    BigDecimal b = new BigDecimal("1.666666666666666666666666666666667");
    entityCollection.getEntities().add(new Entity()
        .addProperty(new Property(null, "Property1", ValueType.PRIMITIVE, b)));
    Assertions.assertTrue(
        serialize(serializerMin, metadata, null, entityCollection, null)
        .contains("1.666666666666666666666666666666667"));
  }
}
