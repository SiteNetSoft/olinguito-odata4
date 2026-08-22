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
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 2 fix: pin that dynamic-property segments
 * preserve the resolved binding target for ETag precondition enforcement
 */
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.etag.CustomETagSupport;
import org.sitenetsoft.olinguito.server.api.etag.PreconditionException;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceValue;
import org.sitenetsoft.olinguito.server.core.etag.PreconditionsValidator;
import org.sitenetsoft.olinguito.server.core.uri.parser.Parser;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserException;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class PreconditionsValidatorTest {

  private static final OData odata = OData.newInstance();
  private static final Edm edm = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList()).getEdm();

  // -------------- POSITIVE TESTS --------------------------------------------------------------------------------

  @Test
  void simpleEntity() throws Exception {
    assertTrue(mustValidate("ESAllPrim(1)", "ESAllPrim"));
  }

  @Test
  void simpleEntityValue() throws Exception {
    assertTrue(mustValidate("ESMedia(1)/$value", "ESMedia"));
  }

  @Test
  void property() throws Exception {
    assertTrue(mustValidate("ESAllPrim(1)/PropertyInt16", "ESAllPrim"));
    assertTrue(mustValidate("ESMixPrimCollComp(0)/PropertyComp", "ESMixPrimCollComp"));
    assertTrue(mustValidate("ESMixPrimCollComp(0)/PropertyComp/PropertyString", "ESMixPrimCollComp"));
  }

  @Test
  void propertyValue() throws Exception {
    assertTrue(mustValidate("ESAllPrim(1)/PropertyInt16/$value", "ESAllPrim"));
    assertTrue(mustValidate("ESMixPrimCollComp(0)/PropertyComp/PropertyString/$value", "ESMixPrimCollComp"));
  }

  @Test
  void dynamicProperty() throws Exception {
    // A dynamic (open-type) property is still a property OF the addressed entity: the binding
    // target resolved from ESOpen(1) must survive the dynamicProperty segment exactly like it
    // survives primitiveProperty/complexProperty, so ETag preconditions are still enforced.
    assertTrue(mustValidate("ESOpen(1)/DynamicString", "ESOpen"));
  }

  @Test
  void EntityAndToOneNavigation() throws Exception {
    assertTrue(mustValidate("ESAllPrim(1)/NavPropertyETTwoPrimOne", "ESTwoPrim"));
  }

  @Test
  void EntityAndToManyNavigationWithKey() throws Exception {
    assertTrue(mustValidate("ESAllPrim(1)/NavPropertyETTwoPrimMany(1)", "ESTwoPrim"));
  }

  @Test
  void EntityAndToOneNavigationValue() throws Exception {
    assertTrue(mustValidate("ESKeyNav(1)/NavPropertyETMediaOne/$value", "ESMedia"));
  }

  @Test
  void navigationOnProperty() throws Exception {
    assertTrue(mustValidate("ESAllPrim(1)/NavPropertyETTwoPrimOne/PropertyInt16", "ESTwoPrim"));
  }

  @Test
  void navigationOnFunction() throws Exception {
    assertTrue(mustValidate("FICRTESTwoKeyNav()/NavPropertySINav", "SINav"));
  }

  @Test
  void boundActionOnEsKeyNav() throws Exception {
    assertTrue(mustValidate("ESKeyNav(1)/Namespace1_Alias.BA_RTETTwoKeyNav", "ESKeyNav"));
  }

  @Test
  void boundActionOnEsKeyNavWithNavigation() throws Exception {
    assertTrue(
        mustValidate("ESKeyNav(1)/NavPropertyETKeyNavOne/Namespace1_Alias.BA_RTETTwoKeyNav", "ESKeyNav"));
  }

  @Test
  void singleton() throws Exception {
    assertTrue(mustValidate("SI", "SI"));
  }

  @Test
  void singletonWithNavigation() throws Exception {
    assertTrue(mustValidate("SINav/NavPropertyETKeyNavOne", "ESKeyNav"));
  }

  @Test
  void singletonWithNavigationValue() throws Exception {
    assertTrue(mustValidate("SINav/NavPropertyETKeyNavOne/NavPropertyETMediaOne/$value", "ESMedia"));
  }

  @Test
  void singletonWithAction() throws Exception {
    assertTrue(mustValidate("SINav/Namespace1_Alias.BA_RTETTwoKeyNav", "SINav"));
  }

  @Test
  void singletonWithActionAndNavigation() throws Exception {
    assertTrue(mustValidate("SINav/NavPropertyETKeyNavOne/Namespace1_Alias.BA_RTETTwoKeyNav", "ESKeyNav"));
  }

  @Test
  void simpleEntityValueValidationNotActiveForMedia() throws Exception {
    final UriInfo uriInfo = new Parser(edm, odata).parseUri("ESMedia(1)/$value", null, null, null);

    CustomETagSupport support = mock(CustomETagSupport.class);
    when(support.hasETag(any(EdmBindingTarget.class))).thenReturn(true);
    when(support.hasMediaETag(any(EdmBindingTarget.class))).thenReturn(false);

    assertFalse(new PreconditionsValidator(uriInfo).mustValidatePreconditions(support, true));
  }

  // -------------- IGNORE VALIDATION TESTS -----------------------------------------------------------------------

  @Test
  void entitySetMustBeIgnored() throws Exception {
    assertFalse(mustValidate("ESAllPrim", "ESAllPrim"));
  }

  @Test
  void navigationToManyMustBeIgnored() throws Exception {
    assertFalse(mustValidate("ESAllPrim(1)/NavPropertyETTwoPrimMany", "ESTwoPrim"));
  }

  @Test
  void navigationOnFunctionWithoutEntitySetMustBeIgnored() throws Exception {
    assertFalse(mustValidate("FICRTETTwoKeyNavParam(ParameterInt16=1)/NavPropertyETKeyNavOne", null));
  }

  @Test
  void navigationToManyToActionMustBeIgnored() throws Exception {
    assertFalse(mustValidate("ESTwoPrim(1)/NavPropertyETAllPrimMany/Namespace1_Alias.BAESAllPrimRTETAllPrim", null));
  }

  @Test
  void navigationWithoutBindingMustBeIgnored() throws Exception {
    assertFalse(mustValidate("ESTwoBaseTwoKeyNav(PropertyInt16=1,PropertyString='test')"
        + "/NavPropertyETBaseTwoKeyNavMany(PropertyInt16=1,PropertyString='test')",
        null));
  }

  @Test
  void referencesKeepTheirBindingTarget() throws Exception {
    // Previously a $ref segment discarded the binding target, so preconditions were never
    // evaluated on the reference write paths at all. [OData-Protocol] 13.1.1 item 26 requires an
    // updatable service to support If-Match on updates and deletes of resources with ETags, and a
    // reference write is such an update, so the segment now leaves the target in place -- the same
    // binding target the equivalent navigation path without /$ref resolves.
    assertTrue(mustValidate("ESAllPrim(1)/NavPropertyETTwoPrimOne/$ref", "ESTwoPrim"));
    assertTrue(mustValidate("ESAllPrim(1)/NavPropertyETTwoPrimMany(1)/$ref", "ESTwoPrim"));
    assertTrue(mustValidate("SINav/NavPropertyETKeyNavOne/$ref", "ESKeyNav"));
  }

  @Test
  void nonResourceMustBeIgnored() throws Exception {
    assertFalse(mustValidate("$all", null));
  }

  private boolean mustValidate(final String uri, final String entitySetName)
      throws UriParserException, UriValidationException, PreconditionException {
    final UriInfo uriInfo = new Parser(edm, odata).parseUri(uri, null, null, null);
    final List<UriResource> parts = uriInfo.getUriResourceParts();
    final boolean isMedia = parts.size() >= 2
        && parts.get(parts.size() - 1) instanceof UriResourceValue
        && parts.get(parts.size() - 2) instanceof UriResourceEntitySet;

    CustomETagSupport support = mock(CustomETagSupport.class);
    final Answer<Boolean> answer = new Answer<Boolean>() {
      public Boolean answer(final InvocationOnMock invocation) throws Throwable {
        if (entitySetName != null) {
          assertEquals(entitySetName, ((EdmBindingTarget) invocation.getArguments()[0]).getName());
        }
        return true;
      }};
    when(support.hasETag(any(EdmBindingTarget.class))).thenAnswer(answer);
    when(support.hasMediaETag(any(EdmBindingTarget.class))).thenAnswer(answer);

    return new PreconditionsValidator(uriInfo).mustValidatePreconditions(support, isMedia);
  }
}
