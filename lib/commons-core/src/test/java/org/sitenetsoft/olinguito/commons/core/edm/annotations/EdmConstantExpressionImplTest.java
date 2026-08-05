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
import static org.mockito.Mockito.mock;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression.EdmExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.core.edm.annotation.AbstractEdmExpression;
import org.junit.jupiter.api.Test;

class EdmConstantExpressionImplTest extends AbstractAnnotationTest {

  @Test
  void binaryExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.Binary, "qrvM3e7_");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Binary, exp.getExpressionType());
    assertEquals("Binary", exp.asConstant().getExpressionName());
    assertEquals("qrvM3e7_", exp.asConstant().getValueAsString());
  }

  @Test
  void boolExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.Bool, "true");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Bool, exp.getExpressionType());
    assertEquals("Bool", exp.asConstant().getExpressionName());
    assertEquals("true", exp.asConstant().getValueAsString());
  }

  @Test
  void dateExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.Date, "2012-02-29");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Date, exp.getExpressionType());
    assertEquals("Date", exp.asConstant().getExpressionName());
    assertEquals("2012-02-29", exp.asConstant().getValueAsString());
  }

  @Test
  void dateTimeOffsetExpression() {
    CsdlConstantExpression csdlExp =
        new CsdlConstantExpression(ConstantExpressionType.DateTimeOffset, "2012-02-29T01:02:03Z");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.DateTimeOffset, exp.getExpressionType());
    assertEquals("DateTimeOffset", exp.asConstant().getExpressionName());
    assertEquals("2012-02-29T01:02:03Z", exp.asConstant().getValueAsString());
  }

  @Test
  void decimalExpression() {
    CsdlConstantExpression csdlExp =
        new CsdlConstantExpression(ConstantExpressionType.Decimal, "-123456789012345678901234567890");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Decimal, exp.getExpressionType());
    assertEquals("Decimal", exp.asConstant().getExpressionName());
    assertEquals("-123456789012345678901234567890", exp.asConstant().getValueAsString());
  }

  @Test
  void durationExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.Duration, "PT10S");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Duration, exp.getExpressionType());
    assertEquals("Duration", exp.asConstant().getExpressionName());
    assertEquals("PT10S", exp.asConstant().getValueAsString());
  }

  @Test
  void enumMemberExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.EnumMember, "Enum/enumMember");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.EnumMember, exp.getExpressionType());
    assertEquals("EnumMember", exp.asConstant().getExpressionName());
    assertEquals("Enum/enumMember", exp.asConstant().getValueAsString());
  }

  @Test
  void floatExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.Float, "1.42");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Float, exp.getExpressionType());
    assertEquals("Float", exp.asConstant().getExpressionName());
    assertEquals("1.42", exp.asConstant().getValueAsString());
  }

  @Test
  void guidExpression() {
    CsdlConstantExpression csdlExp =
        new CsdlConstantExpression(ConstantExpressionType.Guid, "aabbccdd-aabb-ccdd-eeff-aabbccddeeff");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Guid, exp.getExpressionType());
    assertEquals("Guid", exp.asConstant().getExpressionName());
    assertEquals("aabbccdd-aabb-ccdd-eeff-aabbccddeeff", exp.asConstant().getValueAsString());
  }

  @Test
  void intExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.Int, "42");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.Int, exp.getExpressionType());
    assertEquals("Int", exp.asConstant().getExpressionName());
    assertEquals("42", exp.asConstant().getValueAsString());
  }

  @Test
  void stringExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.String, "ABCD");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.String, exp.getExpressionType());
    assertEquals("String", exp.asConstant().getExpressionName());
    assertEquals("ABCD", exp.asConstant().getValueAsString());
  }

  @Test
  void timeOfDayExpression() {
    CsdlConstantExpression csdlExp = new CsdlConstantExpression(ConstantExpressionType.TimeOfDay, "00:00:00.999");
    EdmExpression exp = AbstractEdmExpression.getExpression(mock(Edm.class), csdlExp);

    assertConstant(exp);

    assertEquals(EdmExpressionType.TimeOfDay, exp.getExpressionType());
    assertEquals("TimeOfDay", exp.asConstant().getExpressionName());
    assertEquals("00:00:00.999", exp.asConstant().getValueAsString());
  }
}
