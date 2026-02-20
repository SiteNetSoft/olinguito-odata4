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

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmDynamicExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression.EdmExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLogicalOrComparisonExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
//CHECKSTYLE:OFF
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType;
//CHECKSTYLE:ON
import org.sitenetsoft.olinguito.commons.core.edm.annotation.AbstractEdmExpression;
import org.junit.jupiter.api.Test;

class EdmLogicalOrComparisonImplTest extends AbstractAnnotationTest {

  @Test
  void initialLogicalOrOperationsClasses() {
    for (LogicalOrComparisonExpressionType type : LogicalOrComparisonExpressionType.values()) {
      EdmExpression path = AbstractEdmExpression.getExpression(
          mock(Edm.class),
          new CsdlLogicalOrComparisonExpression(type));

      EdmDynamicExpression dynExp = assertDynamic(path);
      assertEquals(type.toString(), dynExp.getExpressionName());
      assertEquals(EdmExpressionType.valueOf(type.toString()), dynExp.getExpressionType());
      assertSingleKindDynamicExpression(dynExp);

      EdmLogicalOrComparisonExpression logicOrComparisonExp = (EdmLogicalOrComparisonExpression) dynExp;
      try {
        logicOrComparisonExp.getLeftExpression();
        fail("EdmException expected");
      } catch (EdmException e) {
        assertEquals("Comparison Or Logical expression MUST have a left and right expression.", e.getMessage());
      }

      try {
        logicOrComparisonExp.getRightExpression();
        fail("EdmException expected");
      } catch (EdmException e) {
        assertEquals("Comparison Or Logical expression MUST have a left and right expression.", e.getMessage());
      }
    }
  }

  @Test
  void logicalOrOperationsClassesWithExpressions() {
    for (LogicalOrComparisonExpressionType type : LogicalOrComparisonExpressionType.values()) {
      EdmExpression path = AbstractEdmExpression.getExpression(
          mock(Edm.class),
          new CsdlLogicalOrComparisonExpression(type)
              .setLeft(new CsdlConstantExpression(ConstantExpressionType.String))
              .setRight(new CsdlLogicalOrComparisonExpression(type)));

      EdmDynamicExpression dynExp = assertDynamic(path);
      assertEquals(type.toString(), dynExp.getExpressionName());
      assertSingleKindDynamicExpression(dynExp);

      EdmLogicalOrComparisonExpression logicOrComparisonExp = (EdmLogicalOrComparisonExpression) dynExp;
      assertNotNull(logicOrComparisonExp.getLeftExpression());
      assertNotNull(logicOrComparisonExp.getRightExpression());
      if (type == LogicalOrComparisonExpressionType.Not) {
        assertTrue(logicOrComparisonExp.getLeftExpression() == logicOrComparisonExp.getRightExpression());
      } else {
        assertTrue(logicOrComparisonExp.getLeftExpression() instanceof EdmConstantExpression);
        assertTrue(logicOrComparisonExp.getRightExpression() instanceof EdmDynamicExpression);
      }
    }
  }

}
