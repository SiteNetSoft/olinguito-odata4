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
 * Copyright 2026 SiteNetSoft - New test for open-type dynamic property path segments
 * Copyright 2026 SiteNetSoft - Add filter/orderby/expand dynamic-property parsing tests
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceDynamicProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceKind;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Test;

/** Tests resolution of dynamic (open-type) path segments by the resource-path parser. */
class OpenTypeUriParserTest {

  private static final OData odata = OData.newInstance();
  private static final Edm edm = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList()).getEdm();

  @Test
  void dynamicPropertyPathSegmentResolvesOnOpenType() throws Exception {
    final UriInfo uriInfo = new Parser(edm, odata).parseUri("ESOpen(1)/Anything", null, null, null);
    final List<UriResource> parts = uriInfo.getUriResourceParts();
    final UriResource last = parts.get(parts.size() - 1);
    assertEquals(UriResourceKind.dynamicProperty, last.getKind());
    assertEquals("Anything", ((UriResourceDynamicProperty) last).getPropertyName());
  }

  @Test
  void unknownSegmentStillRejectedOnClosedType() {
    assertThrows(UriParserSemanticException.class,
        () -> new Parser(edm, odata).parseUri("ESTwoPrim(1)/Anything", null, null, null));
  }

  @Test
  void filterOnDynamicPropertyParsesOnOpenType() throws Exception {
    final UriInfo uriInfo = new Parser(edm, odata)
        .parseUri("ESOpen", "$filter=DynamicInt gt 5", null, null);
    assertNotNull(uriInfo.getFilterOption().getExpression());
  }

  @Test
  void orderByDynamicPropertyParsesOnOpenType() throws Exception {
    assertNotNull(new Parser(edm, odata)
        .parseUri("ESOpen", "$orderby=DynamicString desc", null, null).getOrderByOption());
  }

  @Test
  void filterOnUnknownStillRejectedOnClosedType() {
    assertThrows(UriParserException.class,
        () -> new Parser(edm, odata).parseUri("ESTwoPrim", "$filter=Nope eq 1", null, null));
  }

  @Test
  void expandOnDynamicNameStillRejected() {
    assertThrows(UriParserException.class,
        () -> new Parser(edm, odata).parseUri("ESOpen", "$expand=DynamicInt", null, null));
  }
}
