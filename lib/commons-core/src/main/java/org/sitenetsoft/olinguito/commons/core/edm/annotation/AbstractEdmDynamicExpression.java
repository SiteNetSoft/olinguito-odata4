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
 * Copyright 2026 SiteNetSoft - Modernized instanceof to pattern matching
 */
package org.sitenetsoft.olinguito.commons.core.edm.annotation;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmAnd;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmAnnotationPath;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmApply;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmCast;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmCollection;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmDynamicExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmEq;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmGe;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmGt;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmIf;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLabeledElementReference;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLe;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmLt;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmNe;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmNot;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmNull;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmOr;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPath;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmRecord;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmUrlRef;

public abstract class AbstractEdmDynamicExpression extends AbstractEdmExpression implements EdmDynamicExpression {

  public AbstractEdmDynamicExpression(Edm edm, String name) {
    super(edm, name);
  }

  @Override
  public boolean isNot() {
    return this instanceof EdmNot;
  }

  @Override
  public EdmNot asNot() {
    return this instanceof EdmNot e ? e : null;
  }

  @Override
  public boolean isAnd() {
    return this instanceof EdmAnd;
  }

  @Override
  public EdmAnd asAnd() {
    return this instanceof EdmAnd e ? e : null;
  }

  @Override
  public boolean isOr() {
    return this instanceof EdmOr;
  }

  @Override
  public EdmOr asOr() {
    return this instanceof EdmOr e ? e : null;
  }

  @Override
  public boolean isEq() {
    return this instanceof EdmEq;
  }

  @Override
  public EdmEq asEq() {
    return this instanceof EdmEq e ? e : null;
  }

  @Override
  public boolean isNe() {
    return this instanceof EdmNe;
  }

  @Override
  public EdmNe asNe() {
    return this instanceof EdmNe e ? e : null;
  }

  @Override
  public boolean isGt() {
    return this instanceof EdmGt;
  }

  @Override
  public EdmGt asGt() {
    return this instanceof EdmGt e ? e : null;
  }

  @Override
  public boolean isGe() {
    return this instanceof EdmGe;
  }

  @Override
  public EdmGe asGe() {
    return this instanceof EdmGe e ? e : null;
  }

  @Override
  public boolean isLt() {
    return this instanceof EdmLt;
  }

  @Override
  public EdmLt asLt() {
    return this instanceof EdmLt e ? e : null;
  }

  @Override
  public boolean isLe() {
    return this instanceof EdmLe;
  }

  @Override
  public EdmLe asLe() {
    return this instanceof EdmLe e ? e : null;
  }

  @Override
  public boolean isAnnotationPath() {
    return this instanceof EdmAnnotationPath;
  }

  @Override
  public EdmAnnotationPath asAnnotationPath() {
    return this instanceof EdmAnnotationPath e ? e : null;
  }

  @Override
  public boolean isApply() {
    return this instanceof EdmApply;
  }

  @Override
  public EdmApply asApply() {
    return this instanceof EdmApply e ? e : null;
  }

  @Override
  public boolean isCast() {
    return this instanceof EdmCast;
  }

  @Override
  public EdmCast asCast() {
    return this instanceof EdmCast e ? e : null;
  }

  @Override
  public boolean isCollection() {
    return this instanceof EdmCollection;
  }

  @Override
  public EdmCollection asCollection() {
    return this instanceof EdmCollection e ? e : null;
  }

  @Override
  public boolean isIf() {
    return this instanceof EdmIf;
  }

  @Override
  public EdmIf asIf() {
    return this instanceof EdmIf e ? e : null;
  }

  @Override
  public boolean isIsOf() {
    return this instanceof EdmIsOf;
  }

  @Override
  public EdmIsOf asIsOf() {
    return this instanceof EdmIsOf e ? e : null;
  }

  @Override
  public boolean isLabeledElement() {
    return this instanceof EdmLabeledElement;
  }

  @Override
  public EdmLabeledElement asLabeledElement() {
    return this instanceof EdmLabeledElement e ? e : null;
  }

  @Override
  public boolean isLabeledElementReference() {
    return this instanceof EdmLabeledElementReference;
  }

  @Override
  public EdmLabeledElementReference asLabeledElementReference() {
    return this instanceof EdmLabeledElementReference e ? e : null;
  }

  @Override
  public boolean isNull() {
    return this instanceof EdmNull;
  }

  @Override
  public EdmNull asNull() {
    return this instanceof EdmNull e ? e : null;
  }

  @Override
  public boolean isNavigationPropertyPath() {
    return this instanceof EdmNavigationPropertyPath;
  }

  @Override
  public EdmNavigationPropertyPath asNavigationPropertyPath() {
    return this instanceof EdmNavigationPropertyPath e ? e : null;
  }

  @Override
  public boolean isPath() {
    return this instanceof EdmPath;
  }

  @Override
  public EdmPath asPath() {
    return this instanceof EdmPath e ? e : null;
  }

  @Override
  public boolean isPropertyPath() {
    return this instanceof EdmPropertyPath;
  }

  @Override
  public EdmPropertyPath asPropertyPath() {
    return this instanceof EdmPropertyPath e ? e : null;
  }

  @Override
  public boolean isPropertyValue() {
    return this instanceof EdmPropertyValue;
  }

  @Override
  public EdmPropertyValue asPropertyValue() {
    return this instanceof EdmPropertyValue e ? e : null;
  }

  @Override
  public boolean isRecord() {
    return this instanceof EdmRecord;
  }

  @Override
  public EdmRecord asRecord() {
    return this instanceof EdmRecord e ? e : null;
  }

  @Override
  public boolean isUrlRef() {
    return this instanceof EdmUrlRef;
  }

  @Override
  public EdmUrlRef asUrlRef() {
    return this instanceof EdmUrlRef e ? e : null;
  }

}
