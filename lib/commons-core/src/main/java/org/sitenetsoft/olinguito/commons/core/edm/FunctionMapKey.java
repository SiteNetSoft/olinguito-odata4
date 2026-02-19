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
 * Copyright 2026 SiteNetSoft - Converted to Java record
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;

public record FunctionMapKey(
    FullQualifiedName functionName,
    FullQualifiedName bindingParameterTypeName,
    Boolean isBindingParameterCollection,
    List<String> parameterNames) {

  public FunctionMapKey {
    if (bindingParameterTypeName != null && isBindingParameterCollection == null) {
      throw new EdmException(
          "Indicator that the bindingparameter is a collection must not be null if its an bound function.");
    }
    List<String> sorted = new ArrayList<>();
    if (parameterNames != null) {
      sorted.addAll(parameterNames);
      Collections.sort(sorted);
    }
    parameterNames = Collections.unmodifiableList(sorted);
  }
}
