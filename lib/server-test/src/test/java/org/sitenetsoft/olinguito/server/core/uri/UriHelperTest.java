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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.uri;

import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.uri.UriHelper;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UriHelperTest {

  private static final OData odata = OData.newInstance();
  private static final Edm edm = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList()).getEdm();
  private static final EdmEntityContainer container = edm.getEntityContainer();
  private static final UriHelper helper = odata.createUriHelper();
  private final DataProvider data = new DataProvider(odata, edm);

  @Test
  void canonicalURL() throws Exception {
    final EdmEntitySet entitySet = container.getEntitySet("ESAllPrim");
    final Entity entity = data.readAll(entitySet).getEntities().get(0);
    Assertions.assertEquals("ESAllPrim(32767)", helper.buildCanonicalURL(entitySet, entity));
  }

  @Test
  void canonicalURLLong() throws Exception {
    final EdmEntitySet entitySet = container.getEntitySet("ESAllKey");
    final Entity entity = data.readAll(entitySet).getEntities().get(0);
    Assertions.assertEquals("ESAllKey("
        + "PropertyString='First',"
        + "PropertyBoolean=true,"
        + "PropertyByte=255,"
        + "PropertySByte=127,"
        + "PropertyInt16=32767,"
        + "PropertyInt32=2147483647,"
        + "PropertyInt64=9223372036854775807,"
        + "PropertyDecimal=34,"
        + "PropertyDate=2012-12-03,"
        + "PropertyDateTimeOffset=2012-12-03T07%3A16%3A23Z,"
        + "PropertyDuration=duration'PT6S',"
        + "PropertyGuid=01234567-89ab-cdef-0123-456789abcdef,"
        + "PropertyTimeOfDay=02%3A48%3A21)",
        helper.buildCanonicalURL(entitySet, entity));
  }

  @Test
  void canonicalURLWrong() throws Exception {
      assertThrows(SerializerException.class, () -> {
          final EdmEntitySet entitySet = container.getEntitySet("ESAllPrim");
          Entity entity = data.readAll(entitySet).getEntities().get(0);
          entity.getProperty("PropertyInt16").setValue(ValueType.PRIMITIVE, "wrong");
          helper.buildCanonicalURL(entitySet, entity);
      });
  }

  @Test
  void canonicalURLWithoutKeys() throws Exception {
      assertThrows(SerializerException.class, () -> {
          final EdmEntitySet entitySet = container.getEntitySet("ESAllPrim");
          Entity entity = data.readAll(entitySet).getEntities().get(0);
          List<Property> properties = entity.getProperties();
          properties.remove(0);
          helper.buildCanonicalURL(entitySet, entity);
      });
  }

  @Test
  void canonicalURLWithKeyHavingNullValue() throws Exception {
      assertThrows(SerializerException.class, () -> {
          final EdmEntitySet entitySet = container.getEntitySet("ESAllPrim");
          Entity entity = data.readAll(entitySet).getEntities().get(0);
          Property property = entity.getProperties().get(0);
          property.setValue(property.getValueType(), null);
          helper.buildCanonicalURL(entitySet, entity);
      });
  }
}
