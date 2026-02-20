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
 * Copyright 2026 SiteNetSoft - Replaced manual hashCode with Objects.hash()
 * Copyright 2026 SiteNetSoft - Modernized equals/hashCode with Objects utility methods
 */
package org.sitenetsoft.olinguito.commons.api.edm.provider.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;

public class CsdlApply extends CsdlDynamicExpression implements CsdlAnnotatable {

  private String function;
  private List<CsdlExpression> parameters = new ArrayList<>();
  private List<CsdlAnnotation> annotations = new ArrayList<>();

  @Override
  public List<CsdlAnnotation> getAnnotations() {
    return annotations;
  }
  
  public CsdlApply setAnnotations(List<CsdlAnnotation> annotations) {
    this.annotations = annotations;
    return this;
  }
  
  /**
   * A QualifiedName specifying the name of the client-side function to apply.
   * <br/>
   * OData defines three canonical functions. Services MAY support additional functions that MUST be qualified with a
   * namespace or alias other than odata. Function names qualified with odata are reserved for this specification and
   * its future versions.
   *
   * @return function full qualified name
   * @see Constants#CANONICAL_FUNCTION_CONCAT
   * @see Constants#CANONICAL_FUNCTION_FILLURITEMPLATE
   * @see Constants#CANONICAL_FUNCTION_URIENCODE
   */
  public String getFunction() {
    return function;
  }

  public CsdlApply setFunction(final String function) {
    this.function = function;
    return this;
  }

  /**
   * Returns the expressions applied to the parameters of the function
   * @return List of expression
   */
  public List<CsdlExpression> getParameters() {
    return parameters;
  }

  public CsdlApply setParameters(List<CsdlExpression> parameters) {
    this.parameters = parameters;
    return this;
  }
  
  @Override
  public boolean equals (Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof CsdlApply annotApply)) {
      return false;
    }
    return Objects.equals(this.getFunction(), annotApply.getFunction())
        && Objects.equals(this.getParameters(), annotApply.getParameters())
        && Objects.equals(this.getAnnotations(), annotApply.getAnnotations());
  }

  @Override
  public int hashCode() {
    return Objects.hash(function, parameters, annotations);
  }
}
