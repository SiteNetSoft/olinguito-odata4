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
 * Copyright 2026 SiteNetSoft - OData 4.01: unqualified names of a default namespace in key-as-segment URLs
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.core.uri.testutil.TestUriValidator;
import org.junit.jupiter.api.Test;

/**
 * Tests of precedence rule 3 of URL Conventions 4.3.6: a segment following an entity collection that is
 * an unqualified name of a bound operation or type defined in a default namespace is resolved as that
 * name, not as a key value.
 */
class KeyAsSegmentDefaultNamespaceTest {

  private static final String NAMESPACE = "ns.def";

  private final TestUriValidator kasA = new TestUriValidator()
      .setEdm(createEdm(true)).setKeyAsSegment(true);
  private final TestUriValidator kasB = new TestUriValidator()
      .setEdm(createEdm(false)).setKeyAsSegment(true);

  @Test
  void unqualifiedBoundFunctionInDefaultNamespaceWins() throws Exception {
    kasA.run("ESX/FX()").goPath().first().isEntitySet("ESX").n().isFunction("FX");
  }

  @Test
  void unqualifiedBoundActionInDefaultNamespaceWins() throws Exception {
    kasA.run("ESX/AX").goPath().first().isEntitySet("ESX").n().isAction("AX");
  }

  @Test
  void unqualifiedTypeCastInDefaultNamespaceWins() throws Exception {
    kasA.run("ESX/ETXSub").goPath().first().isEntitySet("ESX")
        .isTypeFilterOnCollection(new FullQualifiedName(NAMESPACE, "ETXSub"));
  }

  @Test
  void unknownUnqualifiedNameIsAKeyValue() {
    kasA.runEx("ESX/FY").isExSemantic(UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE);
  }

  @Test
  void withoutDefaultNamespaceUnqualifiedNameIsAKeyValue() {
    kasB.runEx("ESX/FX()").isExSemantic(UriParserSemanticException.MessageKeys.INVALID_KEY_VALUE);
  }

  @Test
  void numericSegmentIsAlwaysAKey() throws Exception {
    kasA.run("ESX/7").goPath().first().isEntitySet("ESX").isKeyPredicate(0, "Id", "7");
  }

  private static Edm createEdm(final boolean defaultNamespace) {
    return OData.newInstance()
        .createServiceMetadata(new DefaultNamespaceProvider(defaultNamespace), Collections.emptyList())
        .getEdm();
  }

  /** Tiny EDM with one entity set, one derived type, one bound action and one bound function. */
  private static final class DefaultNamespaceProvider extends CsdlAbstractEdmProvider {

    private static final FullQualifiedName ETX = new FullQualifiedName(NAMESPACE, "ETX");
    private static final FullQualifiedName ETX_SUB = new FullQualifiedName(NAMESPACE, "ETXSub");
    private static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, "Container");

    private final boolean defaultNamespace;

    private DefaultNamespaceProvider(final boolean defaultNamespace) {
      this.defaultNamespace = defaultNamespace;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
      final CsdlSchema schema = new CsdlSchema()
          .setNamespace(NAMESPACE)
          .setEntityTypes(List.of(getEntityType(ETX), getEntityType(ETX_SUB)))
          .setActions(getActions(new FullQualifiedName(NAMESPACE, "AX")))
          .setFunctions(getFunctions(new FullQualifiedName(NAMESPACE, "FX")))
          .setEntityContainer(getEntityContainer());
      if (defaultNamespace) {
        schema.setAnnotations(List.of(new CsdlAnnotation()
            .setTerm("Core.DefaultNamespace")
            .setExpression(new CsdlConstantExpression(ConstantExpressionType.Bool, "true"))));
      }
      return List.of(schema);
    }

    @Override
    public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {
      if (ETX.equals(entityTypeName)) {
        return new CsdlEntityType().setName("ETX")
            .setKey(List.of(new CsdlPropertyRef().setName("Id")))
            .setProperties(List.of(
                new CsdlProperty().setName("Id").setType("Edm.Int32").setNullable(false),
                new CsdlProperty().setName("Name").setType("Edm.String")));
      } else if (ETX_SUB.equals(entityTypeName)) {
        return new CsdlEntityType().setName("ETXSub").setBaseType(ETX);
      }
      return null;
    }

    @Override
    public List<CsdlAction> getActions(final FullQualifiedName actionName) throws ODataException {
      if (new FullQualifiedName(NAMESPACE, "AX").equals(actionName)) {
        return List.of(new CsdlAction().setName("AX").setBound(true)
            .setParameters(List.of(bindingParameter())));
      }
      return null;
    }

    @Override
    public List<CsdlFunction> getFunctions(final FullQualifiedName functionName) throws ODataException {
      if (new FullQualifiedName(NAMESPACE, "FX").equals(functionName)) {
        return List.of(new CsdlFunction().setName("FX").setBound(true)
            .setParameters(List.of(bindingParameter()))
            .setReturnType(new CsdlReturnType().setType("Edm.String")));
      }
      return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
      return new CsdlEntityContainer().setName("Container")
          .setEntitySets(List.of(new CsdlEntitySet().setName("ESX").setType(ETX)));
    }

    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
        throws ODataException {
      return CONTAINER.equals(entityContainer) && "ESX".equals(entitySetName) ?
          new CsdlEntitySet().setName("ESX").setType(ETX) :
          null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName)
        throws ODataException {
      return entityContainerName == null || CONTAINER.equals(entityContainerName) ?
          new CsdlEntityContainerInfo().setContainerName(CONTAINER) :
          null;
    }

    private static CsdlParameter bindingParameter() {
      return new CsdlParameter().setName("bindingParam").setType(ETX).setCollection(true).setNullable(false);
    }
  }
}
