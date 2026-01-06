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
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTerm;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.core.edm.annotation.AbstractEdmExpression;

public class EdmAnnotationImpl extends AbstractEdmAnnotatable implements EdmAnnotation {

  private final CsdlAnnotation annotation;
  private EdmTerm term;
  private EdmExpression expression;

  public EdmAnnotationImpl(final Edm edm, final CsdlAnnotation annotation) {
    super(edm, annotation);
    this.annotation = annotation;
  }

  @Override
  public EdmTerm getTerm() {
    if (term == null) {
      if (annotation.getTerm() == null) {
        throw new EdmException("Term must not be null for an annotation.");
      }
      term = edm.getTerm(new FullQualifiedName(annotation.getTerm()));
    }
    return term;
  }

  @Override
  public String getQualifier() {
    return annotation.getQualifier();
  }

 

  @Override
  public EdmExpression getExpression() {
    if (expression == null && annotation.getExpression() != null) {
      expression = AbstractEdmExpression.getExpression(edm, annotation.getExpression());
    }
    return expression;
  }
}
