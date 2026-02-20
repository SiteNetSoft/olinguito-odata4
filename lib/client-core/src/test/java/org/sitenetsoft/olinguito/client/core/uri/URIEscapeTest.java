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
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.client.core.uri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Geospatial;
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.core.Encoder;
import org.sitenetsoft.olinguito.commons.core.edm.EdmEnumTypeImpl;
import org.junit.jupiter.api.Test;

class URIEscapeTest {

  @Test
  void _null() {
    assertEquals("null", URIUtils.escape( null));
  }

  @Test
  void _boolean() {
    assertEquals("true", URIUtils.escape( Boolean.TRUE));
  }

  @Test
  void _enum() throws UnsupportedEncodingException {
    final EdmEnumType pattern =
        new EdmEnumTypeImpl(null, new FullQualifiedName("Sales", "Pattern"), new CsdlEnumType());

    assertEquals("Sales.Pattern'Yellow'", URIUtils.escape( pattern.toUriLiteral("Yellow")));
  }

  @Test
  void datetimeoffset() throws UnsupportedEncodingException {
    final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
    calendar.clear();
    calendar.set(2014, 6, 11, 12, 30, 04);

    assertEquals(Encoder.encode("2014-07-11T12:30:04+01:00"),
        URIUtils.escape( calendar));
  }

  @Test
  void geospatial() throws UnsupportedEncodingException {
    final Point point = new Point(Geospatial.Dimension.GEOGRAPHY, null);
    point.setX(142.1);
    point.setY(64.1);

    assertEquals(Encoder.encode("geography'SRID=4326;Point(142.1 64.1)'"),
        URIUtils.escape( point));
  }

  @Test
  void collection() {
    assertEquals("[\"red\",\"green\"]",
        URIUtils.escape( List.of(new String[] { "red", "green" })));
  }

  @Test
  void complex() {
    assertEquals("{\"Name\":\"Value\"}",
        URIUtils.escape( Collections.singletonMap("Name", "Value")));
  }
}
