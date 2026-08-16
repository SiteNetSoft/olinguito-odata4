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
 * Copyright 2026 SiteNetSoft - OData 4.01: covering-set overload resolution for optional parameters
 * Copyright 2026 SiteNetSoft - OData 4.01: assert the dedicated ambiguous-overload exception
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 review: cover Core vocabulary aliases other than Core.
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
import org.sitenetsoft.olinguito.commons.api.edm.EdmAmbiguousOverloadException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAliasInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;
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

  private static CsdlParameter optionalParameter(final String name, final FullQualifiedName type) {
    CsdlParameter parameter = new CsdlParameter().setName(name).setType(type);
    parameter.setAnnotations(
        new ArrayList<>(List.of(new CsdlAnnotation().setTerm("Org.OData.Core.V1.OptionalParameter"))));
    return parameter;
  }

  /** Function 'n.opt' with a required parameter 'A' followed by an optional parameter 'B'. */
  private Edm edmWithOptionalParameter(final FullQualifiedName functionName) throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    CsdlFunction function = new CsdlFunction()
        .setName(functionName.getName())
        .setParameters(List.of(
            new CsdlParameter().setName("A").setType(operationType1),
            optionalParameter("B", operationType1)));
    when(provider.getFunctions(functionName)).thenReturn(List.of(function));
    when(provider.getEntityType(operationType1)).thenReturn(new CsdlEntityType().setProperties(new ArrayList<>()));
    return new EdmProviderImpl(provider);
  }

  @Test
  void unboundFunctionResolvesWithOmittedOptionalParameter() throws Exception {
    final FullQualifiedName functionName = new FullQualifiedName("n", "opt");
    final Edm localEdm = edmWithOptionalParameter(functionName);

    // exact match (existing behaviour, pinned)
    EdmFunction exact = localEdm.getUnboundFunction(functionName, List.of("A", "B"));
    assertNotNull(exact);
    assertEquals(2, exact.getParameterNames().size());

    // covering set: the omitted parameter 'B' is optional
    EdmFunction covering = localEdm.getUnboundFunction(functionName, List.of("A"));
    assertNotNull(covering, "Overload must resolve when only the optional parameter is omitted");
    assertTrue(covering.getParameter("B").isOptional());

    // 'A' is required, so specifying only 'B' must not resolve
    assertNull(localEdm.getUnboundFunction(functionName, List.of("B")));

    // an unknown parameter name must not resolve either
    assertNull(localEdm.getUnboundFunction(functionName, List.of("A", "X")));
  }

  @Test
  void optionalParameterDeclaredAfterRequiredOnesResolves() throws Exception {
    // CSDL authoring rule (OData 4.01, Core.OptionalParameter): optional parameters come last.
    // The matcher itself is order-independent; this pins the compliant declaration order.
    final FullQualifiedName functionName = new FullQualifiedName("n", "opt");
    final Edm localEdm = edmWithOptionalParameter(functionName);
    EdmFunction function = localEdm.getUnboundFunction(functionName, List.of("A"));
    assertNotNull(function);
    assertEquals(List.of("A", "B"), function.getParameterNames());
    assertFalse(function.getParameter("A").isOptional());
    assertTrue(function.getParameter("B").isOptional());
  }

  @Test
  void unboundFunctionResolvesWithOutOfLineOptionalAnnotation() throws Exception {
    final FullQualifiedName functionName = new FullQualifiedName("n", "opt");
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    CsdlFunction function = new CsdlFunction()
        .setName(functionName.getName())
        .setParameters(List.of(
            new CsdlParameter().setName("A").setType(operationType1),
            new CsdlParameter().setName("B").setType(operationType1)));
    when(provider.getFunctions(functionName)).thenReturn(List.of(function));
    when(provider.getEntityType(operationType1)).thenReturn(new CsdlEntityType().setProperties(new ArrayList<>()));
    CsdlSchema schema = new CsdlSchema().setNamespace("n").setAnnotationsGroup(List.of(
        new CsdlAnnotations().setTarget("n.opt/B")
            .setAnnotations(new ArrayList<>(
                List.of(new CsdlAnnotation().setTerm("Org.OData.Core.V1.OptionalParameter"))))));
    when(provider.getSchemas()).thenReturn(List.of(schema));
    // the two-argument constructor populates the out-of-line annotations map
    final Edm localEdm = new EdmProviderImpl(provider, new ArrayList<>());

    EdmFunction covering = localEdm.getUnboundFunction(functionName, List.of("A"));
    assertNotNull(covering, "Out-of-line Core.OptionalParameter must be honoured during matching");
    assertTrue(covering.getParameter("B").isOptional());
  }

  /** Function 'n.opt' whose optional parameter 'B' is annotated with the given term name. */
  private Edm edmWithOptionalParameterTerm(final FullQualifiedName functionName, final String term,
      final List<CsdlAliasInfo> aliasInfos) throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    CsdlParameter optional = new CsdlParameter().setName("B").setType(operationType1);
    optional.setAnnotations(new ArrayList<>(List.of(new CsdlAnnotation().setTerm(term))));
    CsdlFunction function = new CsdlFunction()
        .setName(functionName.getName())
        .setParameters(List.of(new CsdlParameter().setName("A").setType(operationType1), optional));
    when(provider.getFunctions(functionName)).thenReturn(List.of(function));
    when(provider.getEntityType(operationType1)).thenReturn(new CsdlEntityType().setProperties(new ArrayList<>()));
    when(provider.getAliasInfos()).thenReturn(aliasInfos);
    // Serve the Core vocabulary term so that the EDM surface can resolve an aliased term name too
    when(provider.getTerm(new FullQualifiedName("Org.OData.Core.V1", "OptionalParameter")))
        .thenReturn(new CsdlTerm().setName("OptionalParameter").setType("Edm.Boolean"));
    return new EdmProviderImpl(provider);
  }

  @Test
  void optionalParameterWithConventionalCoreAliasResolves() throws Exception {
    final FullQualifiedName functionName = new FullQualifiedName("n", "opt");
    final Edm localEdm = edmWithOptionalParameterTerm(functionName, "Core.OptionalParameter", null);

    EdmFunction covering = localEdm.getUnboundFunction(functionName, List.of("A"));
    assertNotNull(covering, "The conventional 'Core.' alias must keep resolving");
    assertTrue(covering.getParameter("B").isOptional());
  }

  @Test
  void optionalParameterWithCustomCoreAliasResolves() throws Exception {
    // A provider may alias Org.OData.Core.V1 to anything (legal CSDL); the covering-set matcher
    // must resolve the alias instead of only accepting the two conventional literals.
    final FullQualifiedName functionName = new FullQualifiedName("n", "opt");
    final Edm localEdm = edmWithOptionalParameterTerm(functionName, "C.OptionalParameter",
        List.of(new CsdlAliasInfo().setAlias("C").setNamespace("Org.OData.Core.V1")));

    EdmFunction covering = localEdm.getUnboundFunction(functionName, List.of("A"));
    assertNotNull(covering, "A custom alias for Org.OData.Core.V1 must resolve the optional parameter");
    assertTrue(covering.getParameter("B").isOptional());
  }

  @Test
  void unrelatedAliasedTermIsNotOptional() throws Exception {
    final FullQualifiedName functionName = new FullQualifiedName("n", "opt");
    final Edm localEdm = edmWithOptionalParameterTerm(functionName, "X.OptionalParameter",
        List.of(new CsdlAliasInfo().setAlias("X").setNamespace("Some.Other.Vocabulary")));

    assertNull(localEdm.getUnboundFunction(functionName, List.of("A")),
        "A term from another vocabulary must not make a parameter optional");
  }

  @Test
  void ambiguousOverloadWithOptionalParametersThrows() throws Exception {
    final FullQualifiedName functionName = new FullQualifiedName("n", "opt");
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    CsdlFunction functionB = new CsdlFunction()
        .setName(functionName.getName())
        .setParameters(List.of(
            new CsdlParameter().setName("A").setType(operationType1),
            optionalParameter("B", operationType1)));
    CsdlFunction functionC = new CsdlFunction()
        .setName(functionName.getName())
        .setParameters(List.of(
            new CsdlParameter().setName("A").setType(operationType1),
            optionalParameter("C", operationType1)));
    when(provider.getFunctions(functionName)).thenReturn(List.of(functionB, functionC));
    when(provider.getEntityType(operationType1)).thenReturn(new CsdlEntityType().setProperties(new ArrayList<>()));
    final Edm localEdm = new EdmProviderImpl(provider);

    EdmAmbiguousOverloadException exception = assertThrows(EdmAmbiguousOverloadException.class,
        () -> localEdm.getUnboundFunction(functionName, List.of("A")));
    assertTrue(exception.getMessage().contains("Ambiguous function overload"), exception.getMessage());
  }

  @Test
  void boundFunctionResolvesWithOmittedOptionalParameter() throws Exception {
    final FullQualifiedName functionName = new FullQualifiedName("n", "boundOpt");
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    CsdlFunction function = new CsdlFunction()
        .setName(functionName.getName())
        .setBound(true)
        .setParameters(List.of(
            new CsdlParameter().setName("bindingParam").setType(operationType1).setCollection(false),
            new CsdlParameter().setName("A").setType(operationType1),
            optionalParameter("B", operationType1)));
    when(provider.getFunctions(functionName)).thenReturn(List.of(function));
    when(provider.getEntityType(operationType1)).thenReturn(new CsdlEntityType().setProperties(new ArrayList<>()));
    final Edm localEdm = new EdmProviderImpl(provider);

    EdmFunction exact = localEdm.getBoundFunction(functionName, operationType1, false, List.of("A", "B"));
    assertNotNull(exact);

    EdmFunction covering = localEdm.getBoundFunction(functionName, operationType1, false, List.of("A"));
    assertNotNull(covering, "Bound overload must resolve when only the optional parameter is omitted");
    assertTrue(covering.getParameter("B").isOptional());

    assertNull(localEdm.getBoundFunction(functionName, operationType1, false, List.of("B")));
  }

  @Test
  void boundFunctionAmbiguousOverloadThrows() throws Exception {
    final FullQualifiedName functionName = new FullQualifiedName("n", "boundOpt");
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    CsdlFunction functionB = new CsdlFunction()
        .setName(functionName.getName())
        .setBound(true)
        .setParameters(List.of(
            new CsdlParameter().setName("bindingParam").setType(operationType1).setCollection(false),
            new CsdlParameter().setName("A").setType(operationType1),
            optionalParameter("B", operationType1)));
    CsdlFunction functionC = new CsdlFunction()
        .setName(functionName.getName())
        .setBound(true)
        .setParameters(List.of(
            new CsdlParameter().setName("bindingParam").setType(operationType1).setCollection(false),
            new CsdlParameter().setName("A").setType(operationType1),
            optionalParameter("C", operationType1)));
    when(provider.getFunctions(functionName)).thenReturn(List.of(functionB, functionC));
    when(provider.getEntityType(operationType1)).thenReturn(new CsdlEntityType().setProperties(new ArrayList<>()));
    final Edm localEdm = new EdmProviderImpl(provider);

    assertThrows(EdmAmbiguousOverloadException.class,
        () -> localEdm.getBoundFunction(functionName, operationType1, false, List.of("A")));
  }

}
