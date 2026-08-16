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
 * Copyright 2026 SiteNetSoft - Improved test assertions
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Added nullable collection parameter tests (OLINGO-1633)
 * Copyright 2026 SiteNetSoft - Updated entity collection parameter test
 * to use List instead of EntityCollection (OLINGO-1638)
 * Copyright 2026 SiteNetSoft - OData 4.01: optional action parameters with default values
 */
package org.sitenetsoft.olinguito.server.core.deserializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Parameter;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException.MessageKeys;
import org.sitenetsoft.olinguito.server.core.deserializer.AbstractODataDeserializerTest;
import org.junit.jupiter.api.Test;

class ODataJsonDeserializerActionParametersTest extends AbstractODataDeserializerTest {

  @Test
  void empty() throws Exception {
    final Map<String, Parameter> parameters = deserialize("{}", "UART", null);
    assertNotNull(parameters);
    assertTrue(parameters.isEmpty());
  }

  @Test
  void primitive() throws Exception {
    final Map<String, Parameter> parameters = deserialize(
        "{\"ParameterDuration\":\"P42DT11H22M33S\",\"ParameterInt16\":42}",
        "UARTTwoParam", null);
    assertNotNull(parameters);
    assertEquals(2, parameters.size());
    Parameter parameter = parameters.get("ParameterInt16");
    assertNotNull(parameter);
    assertTrue(parameter.isPrimitive());
    assertFalse(parameter.isCollection());
    assertEquals((short) 42, parameter.getValue());
    parameter = parameters.get("ParameterDuration");
    assertNotNull(parameter);
    assertEquals(BigDecimal.valueOf(3669753), parameter.getValue());
  }

  @Test
  void primitiveCollection() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("CollParameterByte", "[1,42]");
    assertTrue(parameter.isPrimitive());
    assertTrue(parameter.isCollection());
    assertEquals((short) 1, parameter.asCollection().get(0));
    assertEquals((short) 42, parameter.asCollection().get(1));
  }

  @Test
  void enumeration() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("ParameterEnum", "\"String3,String1\"");
    assertTrue(parameter.isEnum());
    assertFalse(parameter.isCollection());
    assertEquals((short) 5, parameter.getValue());
  }

  @Test
  void enumCollection() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("CollParameterEnum",
        "[ \"String1,String2\", \"String3,String3,String3\" ]");
    assertTrue(parameter.isEnum());
    assertTrue(parameter.isCollection());
    assertEquals((short) 3, parameter.asCollection().get(0));
    assertEquals((short) 4, parameter.asCollection().get(1));
  }

  @Test
  void typeDefinition() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("ParameterDef", "\"Test String\"");
    assertTrue(parameter.isPrimitive());
    assertFalse(parameter.isCollection());
    assertEquals("Test String", parameter.getValue());
  }

  @Test
  void typeDefinitionCollection() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("CollParameterDef",
        "[ \"Test String\", \"Another String\" ]");
    assertTrue(parameter.isPrimitive());
    assertTrue(parameter.isCollection());
    assertEquals("Test String", parameter.asCollection().get(0));
    assertEquals("Another String", parameter.asCollection().get(1));
  }

  @Test
  void complex() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("ParameterComp",
        "{ \"PropertyString\": \"Yes\", \"PropertyInt16\": 42 }");
    assertTrue(parameter.isComplex());
    assertFalse(parameter.isCollection());
    final List<Property> complexValues = parameter.asComplex().getValue();
    assertEquals((short) 42, complexValues.get(0).getValue());
    assertEquals("Yes", complexValues.get(1).getValue());
  }

  @Test
  void complexCollection() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("CollParameterComp",
        "[ { \"PropertyInt16\": 9999, \"PropertyString\": \"One\" },"
        + "  { \"PropertyInt16\": -123, \"PropertyString\": \"Two\" }]");
    assertTrue(parameter.isComplex());
    assertTrue(parameter.isCollection());
    ComplexValue complexValue = (ComplexValue) parameter.asCollection().get(0);
    assertEquals((short) 9999, complexValue.getValue().get(0).getValue());
    assertEquals("One", complexValue.getValue().get(1).getValue());

    complexValue = (ComplexValue) parameter.asCollection().get(1);
    assertEquals((short) -123, complexValue.getValue().get(0).getValue());
    assertEquals("Two", complexValue.getValue().get(1).getValue());
  }

  @Test
  void entity() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("ParameterETTwoPrim",
        "{ \"PropertyInt16\": 42, \"PropertyString\": \"Yes\" }");
    assertTrue(parameter.isEntity());
    assertFalse(parameter.isCollection());
    final List<Property> entityValues = parameter.asEntity().getProperties();
    assertEquals((short) 42, entityValues.get(0).getValue());
    assertEquals("Yes", entityValues.get(1).getValue());
  }

  @Test
  void entityCollection() throws Exception {
    final Parameter parameter = deserializeUARTByteNineParam("CollParameterETTwoPrim",
        "[ { \"PropertyInt16\": 1234, \"PropertyString\": \"One\" },"
        + "  { \"PropertyInt16\": -321, \"PropertyString\": \"Two\" }]");
    assertTrue(parameter.isEntity());
    assertTrue(parameter.isCollection());
    @SuppressWarnings("unchecked")
    final List<Entity> entities = (List<Entity>) parameter.asCollection();
    assertNotNull(entities);
    assertEquals(2, entities.size());
    assertEquals((short) 1234, entities.get(0).getProperties().get(0).getValue());
    assertEquals("One", entities.get(0).getProperties().get(1).getValue());

    assertEquals((short) -321, entities.get(1).getProperties().get(0).getValue());
    assertEquals("Two", entities.get(1).getProperties().get(1).getValue());
  }

  @Test
  void boundEmpty() throws Exception {
    final Map<String, Parameter> parameters = deserialize("{}", "BAETAllPrimRT", "ETAllPrim");
    assertNotNull(parameters);
    assertTrue(parameters.isEmpty());
  }

  @Test
  void ignoreODataAnnotations() throws Exception {
    final String input =
        "{\"ParameterDuration@odata.type\":\"Edm.Duration\","
            + "\"ParameterDuration\":\"P42DT11H22M33S\",\"ParameterInt16\":42}";
    final Map<String, Parameter> parameters = deserialize(input, "UARTTwoParam", null);
    assertNotNull(parameters);
    assertEquals(2, parameters.size());
    Parameter parameter = parameters.get("ParameterInt16");
    assertNotNull(parameter);
    assertEquals((short) 42, parameter.getValue());
    parameter = parameters.get("ParameterDuration");
    assertNotNull(parameter);
    assertEquals(BigDecimal.valueOf(3669753), parameter.getValue());
  }

  @Test
  void parameterWithNullLiteral() throws Exception {
    final Map<String, Parameter> parameters = deserialize("{\"ParameterInt16\":1,\"ParameterDuration\":null}",
        "UARTCollStringTwoParam", null);
    assertNotNull(parameters);
    assertEquals(2, parameters.size());
    Parameter parameter = parameters.get("ParameterInt16");
    assertNotNull(parameter);
    assertEquals((short) 1, parameter.getValue());
    parameter = parameters.get("ParameterDuration");
    assertNotNull(parameter);
    assertNull(parameter.getValue());
  }

  @Test
  void nullableCollectionParameterWithNull() throws Exception {
    final Map<String, Parameter> parameters = deserialize(
        "{\"CollParameterByte\":null,"
            + "\"CollParameterEnum\":[],\"CollParameterDef\":[],\"CollParameterComp\":[],"
            + "\"CollParameterETTwoPrim\":[]}",
        "UARTByteNineParam", null);
    assertNotNull(parameters);
    assertEquals(9, parameters.size());
    final Parameter parameter = parameters.get("CollParameterByte");
    assertNotNull(parameter);
    assertNull(parameter.getValue());
  }

  @Test
  void nullableCollectionParameterOmitted() throws Exception {
    final Map<String, Parameter> parameters = deserialize(
        "{\"CollParameterEnum\":[],\"CollParameterDef\":[],\"CollParameterComp\":[],"
            + "\"CollParameterETTwoPrim\":[]}",
        "UARTByteNineParam", null);
    assertNotNull(parameters);
    assertEquals(9, parameters.size());
    final Parameter parameter = parameters.get("CollParameterByte");
    assertNotNull(parameter);
    assertNull(parameter.getValue());
  }

  @Test
  void optionalParameterOmitted() throws Exception {
    // OData 4.01, Part 1: Protocol, section 11.5.5.1: an omitted optional parameter annotated with
    // a default value is treated as if that value had been passed.
    final Map<String, Parameter> parameters = deserialize("{\"ParameterString\":\"Test\"}",
        "UARTStringOptionalParam", null);
    assertNotNull(parameters);
    assertEquals(3, parameters.size());
    final Parameter suffix = parameters.get("ParameterSuffix");
    assertNotNull(suffix);
    assertTrue(suffix.isPrimitive());
    assertEquals("-default", suffix.getValue());
    // An omitted optional parameter without default value is service-defined; here it stays null.
    final Parameter noDefault = parameters.get("ParameterOptionalNoDefault");
    assertNotNull(noDefault);
    assertNull(noDefault.getValue());
  }

  @Test
  void optionalParameterPresent() throws Exception {
    final Map<String, Parameter> parameters = deserialize(
        "{\"ParameterString\":\"Test\",\"ParameterSuffix\":\"-explicit\"}",
        "UARTStringOptionalParam", null);
    assertNotNull(parameters);
    assertEquals("-explicit", parameters.get("ParameterSuffix").getValue());
  }

  @Test
  void optionalParameterExplicitNull() throws Exception {
    // The default-value rules apply to omission only, an explicitly passed null stays null.
    final Map<String, Parameter> parameters = deserialize(
        "{\"ParameterString\":\"Test\",\"ParameterSuffix\":null}",
        "UARTStringOptionalParam", null);
    assertNotNull(parameters);
    final Parameter suffix = parameters.get("ParameterSuffix");
    assertNotNull(suffix);
    assertNull(suffix.getValue());
  }

  @Test
  void noContent() throws Exception {
	  Map<String, Parameter> parameters = deserialize("", "UARTTwoParam", null);
	  assertNotNull(parameters);
	  parameters = deserialize("", "BAETAllPrimRT", "ETAllPrim");
	  assertNotNull(parameters);
  }

  @Test
  void bindingParameter() throws Exception {
    expectException("{\"ParameterETAllPrim\":{\"PropertyInt16\":42}}", "BAETAllPrimRT", "ETAllPrim",
        MessageKeys.UNKNOWN_CONTENT);
  }

  @Test
  void missingParameter() throws Exception {
    expectException("{\"ParameterWrong\":null}", "UARTParam", null, MessageKeys.UNKNOWN_CONTENT);
    expectException("{}", "UARTCTTwoPrimParam", null, MessageKeys.INVALID_NULL_PARAMETER);
  }

  @Test
  void parameterTwice() throws Exception {
    expectException("{\"ParameterInt16\":1,\"ParameterInt16\":2}", "UARTParam", null, MessageKeys.DUPLICATE_PROPERTY);
  }

  @Test
  void wrongType() throws Exception {
    expectException("{\"ParameterInt16\":null}", "UARTCTTwoPrimParam", null, MessageKeys.INVALID_NULL_PARAMETER);
    expectException("{\"ParameterInt16\":\"42\"}", "UARTParam", null, MessageKeys.INVALID_VALUE_FOR_PROPERTY);
    expectException("{\"ParameterInt16\":123456}", "UARTParam", null, MessageKeys.INVALID_VALUE_FOR_PROPERTY);
    expectException("{\"ParameterInt16\":[42]}", "UARTParam", null, MessageKeys.INVALID_JSON_TYPE_FOR_PROPERTY);
  }

  private Parameter deserializeUARTByteNineParam(final String parameterName, final String parameterJsonValue)
      throws DeserializerException {
    final Map<String, Parameter> parameters = deserialize(
        "{" + (parameterName.equals("CollParameterByte") ? "" : "\"CollParameterByte\":[],")
            + (parameterName.equals("CollParameterEnum") ? "" : "\"CollParameterEnum\":[],")
            + (parameterName.equals("CollParameterDef") ? "" : "\"CollParameterDef\":[],")
            + (parameterName.equals("CollParameterComp") ? "" : "\"CollParameterComp\":[],")
            + (parameterName.equals("CollParameterETTwoPrim") ? "" : "\"CollParameterETTwoPrim\":[],")
            + "\"" + parameterName + "\":" + parameterJsonValue + "}",
        "UARTByteNineParam", null);
    assertNotNull(parameters);
    assertEquals(9, parameters.size());
    Parameter parameter = parameters.get(parameterName);
    assertNotNull(parameter);
    return parameter;
  }

  private Map<String, Parameter> deserialize(final String input, final String actionName, final String bindingTypeName)
      throws DeserializerException {
    return OData.newInstance().createDeserializer(ContentType.JSON, metadata)
        .actionParameters(new ByteArrayInputStream(input.getBytes()),
            bindingTypeName == null ?
                edm.getUnboundAction(new FullQualifiedName(NAMESPACE, actionName)) :
                edm.getBoundAction(new FullQualifiedName(NAMESPACE, actionName),
                    new FullQualifiedName(NAMESPACE, bindingTypeName),
                    false))
        .getActionParameters();
  }

  private void expectException(final String input, final String actionName, final String bindingTypeName,
      final DeserializerException.MessageKeys messageKey) {
    try {
      deserialize(input, actionName, bindingTypeName);
      fail("Expected exception not thrown.");
    } catch (final DeserializerException e) {
      assertEquals(messageKey, e.getMessageKey());
    }
  }
}
