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
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 4: the $compute system query option
 */
package org.sitenetsoft.olinguito.server.api.uri.queryoption;

import java.util.List;

import org.sitenetsoft.olinguito.server.api.uri.queryoption.apply.ComputeExpression;

/**
 * Represents the <code>$compute</code> system query option, which defines properties computed from
 * an expression: <code>$compute=Price mult Qty as TotalPrice</code>.
 * <p>
 * [OData-Protocol] 11.2.5.3: "The $compute system query option allows clients to define computed
 * properties that can be used in a $select or within a $filter or $orderby expression."
 * <p>
 * The computation itself is described by {@link ComputeExpression}, which the <code>$apply</code>
 * transformation of the same name also uses; only the option wrapper differs, because that one is
 * an apply transformation and this one is a system query option.
 */
public interface ComputeOption extends SystemQueryOption {

  /**
   * @return the computed-property definitions, in the order they were given (never <code>null</code>)
   */
  List<ComputeExpression> getExpressions();
}
