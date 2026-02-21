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
 * Copyright 2026 SiteNetSoft - Dynamic property options for aggregate expressions (OLINGO PR#171)
 */
package org.sitenetsoft.olinguito.server.api.uri.queryoption.apply;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Expression;

/**
 * Represents an aggregate expression.
 * @see Aggregate
 */
public interface AggregateExpression extends Expression {

  /** Standard aggregation method. */
  public enum StandardMethod { SUM, MIN, MAX, AVERAGE, COUNT_DISTINCT }

  /**
   * Gets the path prefix and the path segment.
   * @return a (potentially empty) list of path segments (and never <code>null</code>)
   */
  List<UriResource> getPath();

  /**
   * Gets the common expression to be aggregated.
   * @return an {@link Expression} that could be <code>null</code>
   */
  Expression getExpression();

  /**
   * Gets the standard aggregation method if used.
   * @return a {@link StandardMethod} or <code>null</code>
   * @see #getCustomMethod()
   */
  StandardMethod getStandardMethod();

  /**
   * Gets the name of the custom aggregation method if used.
   * @return a {@link FullQualifiedName} or <code>null</code>
   * @see #getStandardMethod()
   */
  FullQualifiedName getCustomMethod();

  /**
   * Gets the name of the aggregate if an alias name has been set.
   * @return an identifier String or <code>null</code>
   */
  String getAlias();

  /**
   * Gets the inline aggregation expression to be applied to the target of the path if used.
   * @return an aggregation expression or <code>null</code>
   * @see #getPath()
   */
  AggregateExpression getInlineAggregateExpression();

  /**
   * Gets the aggregate expressions for <code>from</code>.
   * @return a (potentially empty) list of aggregate expressions (but never <code>null</code>)
   */
  List<AggregateExpression> getFrom();
  
  /**
   * Gets the dynamic properties for aggregation expression.
   * @return the set of properties
   */
  Set<String> getDynamicProperties();

  /**
   * Adds the dynamic property for aggregation expression.
   * @param name an identifier
   */
  void addDynamicProperty(String name);

  /**
   * Gets the dynamic properties with their options for aggregation expression.
   * @return an unmodifiable map of property names to their options
   */
  Map<String, AggregateExpressionDynamicPropertyOptions> getDynamicPropertiesWithOptions();

  /**
   * Adds the dynamic property with options for aggregation expression.
   * @param name an identifier
   * @param options the options for this dynamic property
   */
  void addDynamicProperty(String name, AggregateExpressionDynamicPropertyOptions options);
}
