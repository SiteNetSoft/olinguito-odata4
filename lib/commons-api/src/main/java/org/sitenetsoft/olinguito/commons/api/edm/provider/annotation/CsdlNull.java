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

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;

/**
 * The edm:Null expression returns an untyped null value.
 */
public class CsdlNull extends CsdlDynamicExpression implements CsdlAnnotatable {

  private List<CsdlAnnotation> annotations = new ArrayList<>();

  @Override
  public List<CsdlAnnotation> getAnnotations() {
    return annotations;
  }

  public CsdlNull setAnnotations(List<CsdlAnnotation> annotations) {
    this.annotations = annotations;
    return this;
  }
  
  @Override
  public boolean equals (Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof CsdlNull csdlNull)) {
      return false;
    }
    return Objects.equals(this.getAnnotations(), csdlNull.getAnnotations());
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations);
  }
}
