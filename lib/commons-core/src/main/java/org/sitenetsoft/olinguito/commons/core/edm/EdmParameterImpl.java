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
 * Copyright 2026 SiteNetSoft - OData 4.01: optional function parameters (Core.OptionalParameter)
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmMapping;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTerm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;

public class EdmParameterImpl extends AbstractEdmNamed implements EdmParameter {

  /** Fully qualified name of the OData 4.01 Core.OptionalParameter term. */
  public static final String OPTIONAL_PARAMETER_TERM = "Org.OData.Core.V1.OptionalParameter";
  /** Alias form of the OData 4.01 Core.OptionalParameter term. */
  public static final String OPTIONAL_PARAMETER_TERM_ALIAS = "Core.OptionalParameter";
  private static final String DEFAULT_VALUE_PROPERTY = "DefaultValue";

  private final CsdlParameter parameter;
  private EdmType typeImpl;

  public EdmParameterImpl(final Edm edm, final CsdlParameter parameter) {
    super(edm, parameter.getName(), parameter);
    this.parameter = parameter;
  }

  @Override
  public boolean isCollection() {
    return parameter.isCollection();
  }

  @Override
  public EdmMapping getMapping() {
    return parameter.getMapping();
  }

  @Override
  public boolean isNullable() {
    return parameter.isNullable();
  }

  @Override
  public Integer getMaxLength() {
    return parameter.getMaxLength();
  }

  @Override
  public Integer getPrecision() {
    return parameter.getPrecision();
  }

  @Override
  public Integer getScale() {
    return parameter.getScale();
  }

  @Override
  public SRID getSrid() {
    return parameter.getSrid();
  }

  @Override
  public EdmType getType() {
    if (typeImpl == null) {
      if (parameter.getType() == null) {
        throw new EdmException("Parameter " + parameter.getName() + " must hava a full qualified type.");
      }
      typeImpl = new EdmTypeInfo.Builder().setEdm(edm).setTypeExpression(parameter.getType()).build().getType();
      if (typeImpl == null) {
        throw new EdmException("Cannot find type with name: " + parameter.getTypeFQN());
      }
    }

    return typeImpl;
  }

  @Override
  public boolean isOptional() {
    return getOptionalParameterAnnotation() != null;
  }

  @Override
  public String getOptionalDefaultValue() {
    final EdmAnnotation annotation = getOptionalParameterAnnotation();
    if (annotation == null) {
      return null;
    }
    final EdmExpression expression = annotation.getExpression();
    if (expression == null || !expression.isDynamic() || !expression.asDynamic().isRecord()) {
      return null;
    }
    for (final EdmPropertyValue propertyValue : expression.asDynamic().asRecord().getPropertyValues()) {
      if (DEFAULT_VALUE_PROPERTY.equals(propertyValue.getProperty())) {
        final EdmExpression value = propertyValue.getValue();
        return value != null && value.isConstant() ? value.asConstant().getValueAsString() : null;
      }
    }
    return null;
  }

  private EdmAnnotation getOptionalParameterAnnotation() {
    for (final EdmAnnotation annotation : getAnnotations()) {
      if (isOptionalParameterTerm(annotation)) {
        return annotation;
      }
    }
    return null;
  }

  /**
   * Matches the raw term name first (OLINGO-1399: the vocabulary may not be served, in which case
   * the term cannot be resolved), then falls back to the resolved term's full qualified name.
   */
  private static boolean isOptionalParameterTerm(final EdmAnnotation annotation) {
    final String termName = annotation.getTermName();
    if (OPTIONAL_PARAMETER_TERM.equals(termName) || OPTIONAL_PARAMETER_TERM_ALIAS.equals(termName)) {
      return true;
    }
    try {
      final EdmTerm term = annotation.getTerm();
      return term != null
          && OPTIONAL_PARAMETER_TERM.equals(term.getFullQualifiedName().getFullQualifiedNameAsString());
    } catch (final EdmException e) {
      return false;
    }
  }
}
