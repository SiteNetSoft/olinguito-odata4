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
 * Copyright 2026 SiteNetSoft - OData 4.01: alternate keys declared on both entity type and entity set
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;
import org.sitenetsoft.olinguito.server.core.uri.testutil.TestUriValidator;
import org.junit.jupiter.api.Test;

/**
 * Alternate keys may be declared on the entity type and on the entity set (URL Conventions 4.3.5).
 * An identical declaration on both must count as one group, a conflicting one must not resolve.
 */
class AlternateKeyDeclarationTest {

  private static final String NAMESPACE = "ns.alt";

  private final TestUriValidator sameOnBoth = new TestUriValidator().setEdm(createEdm(false));
  private final TestUriValidator conflicting = new TestUriValidator().setEdm(createEdm(true));

  @Test
  void identicalDeclarationOnTypeAndSetResolves() throws Exception {
    sameOnBoth.run("ESA(Name='x')").goPath().first()
        .isEntitySet("ESA")
        .isKeyPredicate(0, "Name", "'x'")
        .isKeyPredicateAlternateKeyProperty(0, "Name");
  }

  @Test
  void primaryKeyIsUnaffected() throws Exception {
    sameOnBoth.run("ESA(1)").goPath().first()
        .isKeyPredicate(0, "Id", "1")
        .isKeyPredicateAlternateKeyProperty(0, null);
  }

  @Test
  void conflictingDeclarationsDoNotResolve() {
    // the same URL name addresses two different properties, so the predicate is not an alternate key
    conflicting.runEx("ESA(Name='x')").isExValidation(UriValidationException.MessageKeys.INVALID_KEY_PROPERTY);
  }

  private static Edm createEdm(final boolean conflicting) {
    return OData.newInstance()
        .createServiceMetadata(new AlternateKeyProvider(conflicting), Collections.emptyList())
        .getEdm();
  }

  /** EDM with one entity set whose alternate key is declared on both the entity type and the entity set. */
  private static final class AlternateKeyProvider extends CsdlAbstractEdmProvider {

    private static final FullQualifiedName ETA = new FullQualifiedName(NAMESPACE, "ETA");
    private static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, "Container");

    private final boolean conflicting;

    private AlternateKeyProvider(final boolean conflicting) {
      this.conflicting = conflicting;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
      return List.of(new CsdlSchema()
          .setNamespace(NAMESPACE)
          .setEntityTypes(List.of(getEntityType(ETA)))
          .setEntityContainer(getEntityContainer()));
    }

    @Override
    public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {
      if (ETA.equals(entityTypeName)) {
        return new CsdlEntityType().setName("ETA")
            .setKey(List.of(new CsdlPropertyRef().setName("Id")))
            .setProperties(List.of(
                new CsdlProperty().setName("Id").setType("Edm.Int32").setNullable(false),
                new CsdlProperty().setName("Name").setType("Edm.String"),
                new CsdlProperty().setName("Other").setType("Edm.String")))
            .setAnnotations(List.of(alternateKey("Name", "Name")));
      }
      return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
        throws ODataException {
      if (CONTAINER.equals(entityContainer) && "ESA".equals(entitySetName)) {
        return new CsdlEntitySet().setName("ESA").setType(ETA)
            .setAnnotations(List.of(conflicting ? alternateKey("Other", "Name") : alternateKey("Name", "Name")));
      }
      return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
      return new CsdlEntityContainer().setName("Container")
          .setEntitySets(List.of(getEntitySet(CONTAINER, "ESA")));
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName)
        throws ODataException {
      return entityContainerName == null || CONTAINER.equals(entityContainerName) ?
          new CsdlEntityContainerInfo().setContainerName(CONTAINER) :
          null;
    }

    /** A single-part <code>Core.AlternateKeys</code> group addressing the given property under the given name. */
    private static CsdlAnnotation alternateKey(final String propertyName, final String urlName) {
      final List<CsdlPropertyValue> ref = new ArrayList<>();
      ref.add(new CsdlPropertyValue().setProperty("Name").setValue(new CsdlPropertyPath().setValue(propertyName)));
      if (!propertyName.equals(urlName)) {
        ref.add(new CsdlPropertyValue().setProperty("Alias")
            .setValue(new CsdlConstantExpression(ConstantExpressionType.String, urlName)));
      }
      final CsdlExpression group = new CsdlRecord().setPropertyValues(List.of(
          new CsdlPropertyValue().setProperty("Key").setValue(
              new CsdlCollection().setItems(List.<CsdlExpression> of(new CsdlRecord().setPropertyValues(ref))))));
      return new CsdlAnnotation().setTerm("Core.AlternateKeys")
          .setExpression(new CsdlCollection().setItems(List.of(group)));
    }
  }
}
