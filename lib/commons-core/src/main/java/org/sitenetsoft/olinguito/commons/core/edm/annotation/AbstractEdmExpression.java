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
 * Copyright 2026 SiteNetSoft - Converted switch statements to switch expressions
 * Copyright 2026 SiteNetSoft - Modernized instanceof to pattern matching
 */
package org.sitenetsoft.olinguito.commons.core.edm.annotation;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmDynamicExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlDynamicExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;

public abstract class AbstractEdmExpression implements EdmExpression {

  private final String name;
  protected final Edm edm;

  public AbstractEdmExpression(Edm edm, String name) {
    this.edm = edm;
    this.name = name;
  }

  @Override
  public String getExpressionName() {
    return name;
  };

  @Override
  public boolean isConstant() {
    return this instanceof EdmConstantExpression;
  }

  @Override
  public EdmConstantExpression asConstant() {
    return this instanceof EdmConstantExpression c ? c : null;
  }

  @Override
  public boolean isDynamic() {
    return this instanceof EdmDynamicExpression;
  }

  @Override
  public EdmDynamicExpression asDynamic() {
    return this instanceof EdmDynamicExpression d ? d : null;
  }
  
  public static EdmExpression getExpression(Edm edm, final CsdlExpression exp) {
    EdmExpression _expression = null;

    if (exp.isConstant()) {
      _expression = new EdmConstantExpressionImpl(edm, exp.asConstant());
    } else if (exp.isDynamic()) {
      _expression = getDynamicExpression(edm, exp.asDynamic());
    }

    return _expression;
  }

  private static EdmDynamicExpression getDynamicExpression(Edm edm, final CsdlDynamicExpression exp) {

    EdmDynamicExpression _expression = null;

    if (exp.isLogicalOrComparison()) {
      CsdlLogicalOrComparisonExpression expLocal = exp.asLogicalOrComparison();
      _expression = switch (exp.asLogicalOrComparison().getType()) {
        case Not -> new EdmNotImpl(edm, expLocal);
        case And -> new EdmAndImpl(edm, expLocal);
        case Or -> new EdmOrImpl(edm, expLocal);
        case Eq -> new EdmEqImpl(edm, expLocal);
        case Ne -> new EdmNeImpl(edm, expLocal);
        case Ge -> new EdmGeImpl(edm, expLocal);
        case Gt -> new EdmGtImpl(edm, expLocal);
        case Le -> new EdmLeImpl(edm, expLocal);
        case Lt -> new EdmLtImpl(edm, expLocal);
      };
    } else if (exp.isAnnotationPath()) {
      _expression = new EdmAnnotationPathImpl(edm, exp.asAnnotationPath());
    } else if (exp.isApply()) {
      _expression = new EdmApplyImpl(edm, exp.asApply());
    } else if (exp.isCast()) {
      _expression = new EdmCastImpl(edm, exp.asCast());
    } else if (exp.isCollection()) {
      _expression = new EdmCollectionImpl(edm, exp.asCollection());
    } else if (exp.isIf()) {
      _expression = new EdmIfImpl(edm, exp.asIf());
    } else if (exp.isIsOf()) {
      _expression = new EdmIsOfImpl(edm, exp.asIsOf());
    } else if (exp.isLabeledElement()) {
      _expression = new EdmLabeledElementImpl(edm, exp.asLabeledElement());
    } else if (exp.isLabeledElementReference()) {
      _expression = new EdmLabeledElementReferenceImpl(edm, exp.asLabeledElementReference());
    } else if (exp.isNull()) {
      _expression = new EdmNullImpl(edm, exp.asNull());
    } else if (exp.isNavigationPropertyPath()) {
      _expression = new EdmNavigationPropertyPathImpl(edm, exp.asNavigationPropertyPath());
    } else if (exp.isPath()) {
      _expression = new EdmPathImpl(edm, exp.asPath());
    } else if (exp.isPropertyPath()) {
      _expression = new EdmPropertyPathImpl(edm, exp.asPropertyPath());
    } else if (exp.isRecord()) {
      _expression = new EdmRecordImpl(edm, exp.asRecord());
    } else if (exp.isUrlRef()) {
      _expression = new EdmUrlRefImpl(edm, exp.asUrlRef());
    }

    return _expression;
  }
}
