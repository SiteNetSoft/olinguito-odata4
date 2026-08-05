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
 * Copyright 2026 SiteNetSoft - OLINGO-972: Test bound action inheritance across multiple levels
 */
package org.sitenetsoft.olinguito.server.core.edm.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.core.edm.EdmProviderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EdmProviderImplOverloadingTest {

  private Edm edm;
  private final FullQualifiedName operationName1 = new FullQualifiedName("n", "o1");
  private final FullQualifiedName operationType1 = new FullQualifiedName("n", "t1");
  private final FullQualifiedName operationType2 = new FullQualifiedName("n", "t2");
  private final FullQualifiedName wrongOperationName = new FullQualifiedName("wrong", "wrong");
  private final FullQualifiedName badOperationName = new FullQualifiedName("bad", "bad");

  @BeforeEach
  void setup() throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);

    List<CsdlAction> actions = new ArrayList<>();
    CsdlAction action = new CsdlAction().setName(operationName1.getName());
    actions.add(action);
    List<CsdlParameter> action1Parameters = new ArrayList<>();
    action1Parameters.add(new CsdlParameter().setType(operationType1).setCollection(false));
    action =
        new CsdlAction().setName(operationName1.getName()).setBound(true).setParameters(action1Parameters);
    actions.add(action);
    List<CsdlParameter> action2Parameters = new ArrayList<>();
    action2Parameters.add(new CsdlParameter().setType(operationType1).setCollection(true));
    action =
        new CsdlAction().setName(operationName1.getName()).setBound(true).setParameters(action2Parameters);
    actions.add(action);
    when(provider.getActions(operationName1)).thenReturn(actions);
    CsdlEntityType type = new CsdlEntityType().setProperties(new ArrayList<>());
    when(provider.getEntityType(operationType1)).thenReturn(type);
    when(provider.getEntityType(operationType2)).thenReturn(type);
    List<CsdlFunction> functions = new ArrayList<>();
    CsdlFunction function = new CsdlFunction().setName(operationName1.getName());
    functions.add(function);
    List<CsdlParameter> function1Parameters = new ArrayList<>();
    function1Parameters.add(new CsdlParameter().setType(operationType1).setName("a"));
    function = new CsdlFunction().setName(operationName1.getName()).setParameters(function1Parameters);
    functions.add(function);
    List<CsdlParameter> function2Parameters = new ArrayList<>();
    function2Parameters.add(new CsdlParameter().setType(operationType1).setName("b"));
    function = new CsdlFunction().setName(operationName1.getName()).setParameters(function2Parameters);
    functions.add(function);
    List<CsdlParameter> function3Parameters = new ArrayList<>();
    function3Parameters.add(new CsdlParameter().setName("a").setType(operationType1));
    function3Parameters.add(new CsdlParameter().setName("b").setType(operationType1));
    function = new CsdlFunction().setName(operationName1.getName()).setParameters(function3Parameters).setBound(true);
    functions.add(function);
    List<CsdlParameter> function4Parameters = new ArrayList<>();
    function4Parameters.add(new CsdlParameter().setName("a").setType(operationType2));
    function4Parameters.add(new CsdlParameter().setName("b").setType(operationType2));
    function = new CsdlFunction().setName(operationName1.getName()).setParameters(function4Parameters).setBound(true);
    functions.add(function);
    when(provider.getFunctions(operationName1)).thenReturn(functions);

    List<CsdlFunction> badFunctions = new ArrayList<>();
    CsdlFunction badFunction = new CsdlFunction().setName(operationName1.getName()).setBound(true).setParameters(null);
    badFunctions.add(badFunction);

    when(provider.getFunctions(badOperationName)).thenReturn(badFunctions);

    edm = new EdmProviderImpl(provider);
  }

  @Test
  void simpleActionGet() {
    EdmAction action = edm.getUnboundAction(operationName1);
    assertNotNull(action);
    assertEquals(operationName1.getNamespace(), action.getNamespace());
    assertEquals(operationName1.getName(), action.getName());

    assertNull(edm.getUnboundAction(wrongOperationName));
  }

  @Test
  void boundActionOverloading() {
    EdmAction action = edm.getBoundAction(operationName1, operationType1, false);
    assertNotNull(action);
    assertEquals(operationName1.getNamespace(), action.getNamespace());
    assertEquals(operationName1.getName(), action.getName());
    assertTrue(action == edm.getBoundAction(operationName1, operationType1, false));

    EdmAction action2 = edm.getBoundAction(operationName1, operationType1, true);
    assertNotNull(action2);
    assertEquals(operationName1.getNamespace(), action2.getNamespace());
    assertEquals(operationName1.getName(), action2.getName());
    assertTrue(action2 == edm.getBoundAction(operationName1, operationType1, true));

    assertNotSame(action, action2);
  }

  @Test
  void simpleFunctionGet() {
    EdmFunction function = edm.getUnboundFunction(operationName1, null);
    assertNotNull(function);
    assertEquals(operationName1.getNamespace(), function.getNamespace());
    assertEquals(operationName1.getName(), function.getName());

    EdmFunction function2 = edm.getUnboundFunction(operationName1, new ArrayList<>());
    assertNotNull(function2);
    assertEquals(operationName1.getNamespace(), function2.getNamespace());
    assertEquals(operationName1.getName(), function2.getName());

    assertEquals(function, function2);

    assertNull(edm.getUnboundFunction(wrongOperationName, new ArrayList<>()));
  }

  @Test
  void functionOverloading() {
    ArrayList<String> parameter1Names = new ArrayList<>();
    parameter1Names.add("a");
    List<String> parameter2Names = new ArrayList<>();
    parameter2Names.add("b");
    EdmFunction function = edm.getUnboundFunction(operationName1, new ArrayList<>());
    assertNotNull(function);
    assertFalse(function.isBound());

    EdmFunction function1 = edm.getUnboundFunction(operationName1, parameter1Names);
    assertNotNull(function1);
    assertFalse(function1.isBound());

    assertFalse(function == function1);
    assertNotSame(function, function1);

    EdmFunction function2 = edm.getUnboundFunction(operationName1, parameter2Names);
    assertNotNull(function2);
    assertFalse(function2.isBound());

    assertFalse(function1 == function2);
    assertNotSame(function1, function2);

    EdmFunction function3 = edm.getBoundFunction(operationName1, operationType1, false, parameter2Names);
    assertNotNull(function3);
    assertTrue(function3.isBound());
    EdmFunction function4 = edm.getBoundFunction(operationName1, operationType2, false, parameter2Names);
    assertNotNull(function4);
    assertTrue(function4.isBound());

    assertFalse(function3 == function4);
    assertNotSame(function3, function4);

    assertFalse(function1 == function3);
    assertFalse(function1 == function4);
    assertFalse(function2 == function3);
    assertFalse(function2 == function4);
    assertNotSame(function1, function3);
    assertNotSame(function1, function4);
    assertNotSame(function2, function3);
    assertNotSame(function2, function4);
  }

  @Test
  void noParametersAtBoundFunctionReslutsInException() {
      assertThrows(EdmException.class, () -> edm.getBoundFunction(badOperationName, operationType1, true, null));
  }

  @Test
  void boundActionInheritedByGrandchildType() throws Exception {
    // OLINGO-972: A bound action defined on a base type must be callable on
    // grandchild (and deeper) derived types, not just direct children.
    final FullQualifiedName actionName = new FullQualifiedName("ns", "act");
    final FullQualifiedName baseType = new FullQualifiedName("ns", "Base");
    final FullQualifiedName childType = new FullQualifiedName("ns", "Child");
    final FullQualifiedName grandchildType = new FullQualifiedName("ns", "Grandchild");

    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);

    CsdlAction boundOnBase = new CsdlAction()
        .setName(actionName.getName())
        .setBound(true)
        .setParameters(List.of(new CsdlParameter().setType(baseType).setCollection(false)));
    when(provider.getActions(actionName)).thenReturn(List.of(boundOnBase));

    when(provider.getEntityType(baseType)).thenReturn(new CsdlEntityType().setName(baseType.getName()));
    when(provider.getEntityType(childType)).thenReturn(
        new CsdlEntityType().setName(childType.getName()).setBaseType(baseType.getFullQualifiedNameAsString()));
    when(provider.getEntityType(grandchildType)).thenReturn(
        new CsdlEntityType().setName(grandchildType.getName()).setBaseType(childType.getFullQualifiedNameAsString()));

    Edm localEdm = new EdmProviderImpl(provider);
    EdmAction fromChild = localEdm.getBoundAction(actionName, childType, false);
    assertNotNull(fromChild, "Bound action must resolve from direct child");
    EdmAction fromGrandchild = localEdm.getBoundAction(actionName, grandchildType, false);
    assertNotNull(fromGrandchild, "Bound action must resolve from grandchild (OLINGO-972)");
  }

}
