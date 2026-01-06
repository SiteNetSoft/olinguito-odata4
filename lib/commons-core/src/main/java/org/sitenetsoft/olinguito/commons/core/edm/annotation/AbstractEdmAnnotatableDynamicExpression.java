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
package org.sitenetsoft.olinguito.commons.core.edm.annotation;

import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTerm;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.core.edm.AbstractEdmAnnotatable;

public abstract class AbstractEdmAnnotatableDynamicExpression extends AbstractEdmDynamicExpression implements
    EdmAnnotatable {

  private final AnnotationHelper helper;

  public AbstractEdmAnnotatableDynamicExpression(Edm edm, String name, CsdlAnnotatable annotatable) {
    super(edm, name);
    helper = new AnnotationHelper(edm, annotatable);
  }

  @Override
  public EdmAnnotation getAnnotation(final EdmTerm term, String qualifier) {
    return helper.getAnnotation(term, qualifier);
  }

  @Override
  public List<EdmAnnotation> getAnnotations() {
    return helper.getAnnotations();
  }

  private class AnnotationHelper extends AbstractEdmAnnotatable {

    public AnnotationHelper(Edm edm, CsdlAnnotatable annotatable) {
      super(edm, annotatable);
    }
  }
}
