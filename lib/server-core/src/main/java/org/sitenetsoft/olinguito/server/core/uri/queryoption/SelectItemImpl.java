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
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 2: nested query options on a select item
 */
package org.sitenetsoft.olinguito.server.core.uri.queryoption;

import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoResource;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SearchOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SkipOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.TopOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CountOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectItem;

public class SelectItemImpl implements SelectItem {

  private UriInfoResource path;

  private boolean isStar;
  private FullQualifiedName addOperationsInSchemaNameSpace;

  private EdmType startTypeFilter;

  private FilterOption filterOption;
  private SearchOption searchOption;
  private OrderByOption orderByOption;
  private SkipOption skipOption;
  private TopOption topOption;
  private CountOption countOption;
  private SelectOption selectOption;

  @Override
  public UriInfoResource getResourcePath() {

    return path;
  }

  public SelectItemImpl setResourcePath(final UriInfoResource path) {
    this.path = path;
    return this;
  }

  @Override
  public boolean isStar() {
    return isStar;
  }

  public SelectItemImpl setStar(final boolean isStar) {
    this.isStar = isStar;
    return this;
  }

  @Override
  public boolean isAllOperationsInSchema() {
    return addOperationsInSchemaNameSpace != null;
  }

  @Override
  public FullQualifiedName getAllOperationsInSchemaNameSpace() {
    return addOperationsInSchemaNameSpace;
  }

  public void addAllOperationsInSchema(final FullQualifiedName addOperationsInSchemaNameSpace) {
    this.addOperationsInSchemaNameSpace = addOperationsInSchemaNameSpace;
  }

  @Override
  public EdmType getStartTypeFilter() {
    return startTypeFilter;
  }

  public SelectItemImpl setTypeFilter(final EdmType startTypeFilter) {
    this.startTypeFilter = startTypeFilter;
    return this;
  }

  @Override
  public FilterOption getFilterOption() {
    return filterOption;
  }

  public SelectItemImpl setFilterOption(final FilterOption filterOption) {
    this.filterOption = filterOption;
    return this;
  }

  @Override
  public SearchOption getSearchOption() {
    return searchOption;
  }

  public SelectItemImpl setSearchOption(final SearchOption searchOption) {
    this.searchOption = searchOption;
    return this;
  }

  @Override
  public OrderByOption getOrderByOption() {
    return orderByOption;
  }

  public SelectItemImpl setOrderByOption(final OrderByOption orderByOption) {
    this.orderByOption = orderByOption;
    return this;
  }

  @Override
  public SkipOption getSkipOption() {
    return skipOption;
  }

  public SelectItemImpl setSkipOption(final SkipOption skipOption) {
    this.skipOption = skipOption;
    return this;
  }

  @Override
  public TopOption getTopOption() {
    return topOption;
  }

  public SelectItemImpl setTopOption(final TopOption topOption) {
    this.topOption = topOption;
    return this;
  }

  @Override
  public CountOption getCountOption() {
    return countOption;
  }

  public SelectItemImpl setCountOption(final CountOption countOption) {
    this.countOption = countOption;
    return this;
  }

  @Override
  public SelectOption getSelectOption() {
    return selectOption;
  }

  public SelectItemImpl setSelectOption(final SelectOption selectOption) {
    this.selectOption = selectOption;
    return this;
  }

}
