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
package org.sitenetsoft.olinguito.commons.core.edm.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmDynamicExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression.EdmExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.core.edm.annotation.AbstractEdmExpression;
import org.junit.jupiter.api.Test;

class EdmLabeledElementImplTest extends AbstractAnnotationTest {

  @Test
  void initialLabeledElement() {
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), new CsdlLabeledElement());

    EdmDynamicExpression dynExp = assertDynamic(exp);
    assertTrue(dynExp.isLabeledElement());
    assertNotNull(dynExp.asLabeledElement());

    assertEquals("LabeledElement", dynExp.getExpressionName());
    assertEquals(EdmExpressionType.LabeledElement, dynExp.getExpressionType());
    assertSingleKindDynamicExpression(dynExp);

    EdmLabeledElement asLabeled = dynExp.asLabeledElement();

    try {
      asLabeled.getName();
      fail("EdmException expected");
    } catch (EdmException e) {
      assertEquals("The LabeledElement expression must have a name attribute.", e.getMessage());
    }

    try {
      asLabeled.getValue();
      fail("EdmException expected");
    } catch (EdmException e) {
      assertEquals("The LabeledElement expression must have a child expression", e.getMessage());
    }

    assertNotNull(asLabeled.getAnnotations());
    assertTrue(asLabeled.getAnnotations().isEmpty());
  }

  @Test
  void labeledElementWithNameAndValue() {
    CsdlLabeledElement csdlLabeledElement = new CsdlLabeledElement();
    csdlLabeledElement.setName("name");
    csdlLabeledElement.setValue(new CsdlConstantExpression(ConstantExpressionType.String));
    List<CsdlAnnotation> csdlAnnotations = new ArrayList<>();
    csdlAnnotations.add(new CsdlAnnotation().setTerm("ns.term"));
    csdlLabeledElement.setAnnotations(csdlAnnotations);
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlLabeledElement);
    EdmLabeledElement asLabeled = exp.asDynamic().asLabeledElement();

    assertEquals("name", asLabeled.getName());
    assertNotNull(asLabeled.getValue());
    assertTrue(asLabeled.getValue().isConstant());

    assertNotNull(asLabeled.getAnnotations());
    assertEquals(1, asLabeled.getAnnotations().size());
  }
}
