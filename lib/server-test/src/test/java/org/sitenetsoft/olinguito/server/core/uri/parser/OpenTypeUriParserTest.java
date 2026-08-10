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
 * Copyright 2026 SiteNetSoft - Add regression tests for dynamic-member NPE/bypass-narrowness fixes
 * Copyright 2026 SiteNetSoft - Add regression tests for IN-candidate and add/sub non-dynamic-operand checks
 * Copyright 2026 SiteNetSoft - Add $select dynamic-property parsing tests
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
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Binary;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Expression;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Member;
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
    final Expression expression = uriInfo.getFilterOption().getExpression();
    assertNotNull(expression);
    // Walk down to the left operand's member path and confirm it actually resolved to a
    // dynamic-property resource, not merely that some non-null expression was produced.
    final Expression left = ((Binary) expression).getLeftOperand();
    assertLastPartIsDynamicProperty((Member) left, "DynamicInt");
  }

  private static void assertLastPartIsDynamicProperty(final Member member, final String expectedName) {
    final List<UriResource> parts = member.getResourcePath().getUriResourceParts();
    final UriResource last = parts.get(parts.size() - 1);
    assertEquals(UriResourceKind.dynamicProperty, last.getKind());
    assertEquals(expectedName, ((UriResourceDynamicProperty) last).getPropertyName());
  }

  @Test
  void filterAddChainOnDynamicPropertyRejectedNotNpe() {
    // (DynamicInt add 5) is compatible (dynamic bypass); chaining a further "add true" can no
    // longer see the dynamic origin (the intermediate is a Binary, not a Member) and must be
    // rejected with a proper semantic exception rather than an NPE while building the message.
    assertThrows(UriParserSemanticException.class, () -> new Parser(edm, odata)
        .parseUri("ESOpen", "$filter=DynamicInt add 5 add true", null, null));
  }

  @Test
  void filterInOnComposedDynamicArithmeticRejectedNotNpe() {
    // The IN operand (DynamicInt add 5) has an unknown (null) type that is not itself a
    // dynamic member; it cannot be proven compatible with the typed candidate list and must be
    // rejected with a proper semantic exception rather than an NPE.
    assertThrows(UriParserSemanticException.class, () -> new Parser(edm, odata)
        .parseUri("ESOpen", "$filter=(DynamicInt add 5) in (1,2)", null, null));
  }

  @Test
  void filterRelationDynamicVersusComplexOperandRejected() {
    // The dynamic left operand is skipped, but the known non-dynamic right operand (a complex
    // property) must still be validated and rejected as an invalid relational operand.
    assertThrows(UriParserSemanticException.class, () -> new Parser(edm, odata)
        .parseUri("ESOpen", "$filter=DynamicInt gt PropertyComp", null, null));
  }

  @Test
  void filterEqualityDynamicVersusComplexOperandRejected() {
    // Pins the equality-site fix: the dynamic left operand is skipped, but the known
    // non-dynamic right operand (a complex property) must still be rejected as non-primitive.
    assertThrows(UriParserSemanticException.class, () -> new Parser(edm, odata)
        .parseUri("ESOpen", "$filter=DynamicInt eq PropertyComp", null, null));
  }

  @Test
  void filterInDynamicLeftVersusComplexCandidateRejected() {
    // The LEFT operand's dynamic-compatibility rule leaves its own kind unconstrained, but that
    // does NOT exempt the IN candidate list from its own primitive-ness check: a complex
    // candidate must still be rejected even though the left operand is a genuine dynamic member.
    assertThrows(UriParserSemanticException.class, () -> new Parser(edm, odata)
        .parseUri("ESOpen", "$filter=DynamicInt in (PropertyComp)", null, null));
  }

  @Test
  void filterAddDynamicVersusComplexOperandRejected() {
    // The dynamic left operand is skipped, but the known non-dynamic right operand (a complex
    // property) must still be validated and rejected as an invalid add operand.
    assertThrows(UriParserSemanticException.class, () -> new Parser(edm, odata)
        .parseUri("ESOpen", "$filter=DynamicInt add PropertyComp", null, null));
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

  @Test
  void selectDynamicNameParsesOnOpenType() throws Exception {
    final UriInfo uriInfo = new Parser(edm, odata).parseUri("ESOpen", "$select=DynamicInt", null, null);
    assertEquals(1, uriInfo.getSelectOption().getSelectItems().size());
    final List<UriResource> parts =
        uriInfo.getSelectOption().getSelectItems().get(0).getResourcePath().getUriResourceParts();
    final UriResource last = parts.get(parts.size() - 1);
    assertEquals(UriResourceKind.dynamicProperty, last.getKind());
    assertEquals("DynamicInt", ((UriResourceDynamicProperty) last).getPropertyName());
  }

  @Test
  void selectUnknownNameStillRejectedOnClosedType() {
    assertThrows(UriParserSemanticException.class,
        () -> new Parser(edm, odata).parseUri("ESTwoPrim", "$select=Unknown", null, null));
  }
}
