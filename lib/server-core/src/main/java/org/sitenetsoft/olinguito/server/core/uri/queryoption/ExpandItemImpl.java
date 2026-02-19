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
package org.sitenetsoft.olinguito.server.core.uri.queryoption;

import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoResource;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ApplyOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CountOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.LevelsExpandOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SearchOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SkipOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.TopOption;

public class ExpandItemImpl implements ExpandItem {
  private LevelsExpandOption levelsExpandOption;
  private FilterOption filterOption;
  private SearchOption searchOption;
  private OrderByOption orderByOption;
  private SkipOption skipOption;
  private TopOption topOption;
  private CountOption inlineCountOption;
  private SelectOption selectOption;
  private ExpandOption expandOption;
  private ApplyOption applyOption;

  private UriInfoResource resourceInfo;

  private boolean isStar;

  private boolean isRef;
  private boolean hasCountPath;
  private EdmType startTypeFilter;

  public ExpandItemImpl setSystemQueryOption(final SystemQueryOption sysItem) {

    if (sysItem instanceof ApplyOption applyOpt) {
      validateDoubleSystemQueryOption(applyOption, sysItem);
      applyOption = applyOpt;
    } else if (sysItem instanceof ExpandOption expandOpt) {
      validateDoubleSystemQueryOption(expandOption, sysItem);
      expandOption = expandOpt;
    } else if (sysItem instanceof FilterOption filterOpt) {
      validateDoubleSystemQueryOption(filterOption, sysItem);
      filterOption = filterOpt;
    } else if (sysItem instanceof CountOption countOpt) {
      validateDoubleSystemQueryOption(inlineCountOption, sysItem);
      inlineCountOption = countOpt;
    } else if (sysItem instanceof OrderByOption orderByOpt) {
      validateDoubleSystemQueryOption(orderByOption, sysItem);
      orderByOption = orderByOpt;
    } else if (sysItem instanceof SearchOption searchOpt) {
      validateDoubleSystemQueryOption(searchOption, sysItem);
      searchOption = searchOpt;
    } else if (sysItem instanceof SelectOption selectOpt) {
      validateDoubleSystemQueryOption(selectOption, sysItem);
      selectOption = selectOpt;
    } else if (sysItem instanceof SkipOption skipOpt) {
      validateDoubleSystemQueryOption(skipOption, sysItem);
      skipOption = skipOpt;
    } else if (sysItem instanceof TopOption topOpt) {
      validateDoubleSystemQueryOption(topOption, sysItem);
      topOption = topOpt;
    } else if (sysItem instanceof LevelsExpandOption levelsOpt) {
      if (levelsExpandOption != null) {
        throw new ODataRuntimeException("$levels");
      }
      levelsExpandOption = levelsOpt;
    }
    return this;
  }

  private void validateDoubleSystemQueryOption(final SystemQueryOption oldOption, final SystemQueryOption newOption) {
    if (oldOption != null) {
      throw new ODataRuntimeException(newOption.getName());
    }
  }

  public ExpandItemImpl setSystemQueryOptions(final List<SystemQueryOption> list) {
    for (SystemQueryOption item : list) {
      setSystemQueryOption(item);
    }
    return this;
  }

  @Override
  public LevelsExpandOption getLevelsOption() {
    return levelsExpandOption;
  }

  @Override
  public FilterOption getFilterOption() {
    return filterOption;
  }

  @Override
  public SearchOption getSearchOption() {
    return searchOption;
  }

  @Override
  public OrderByOption getOrderByOption() {
    return orderByOption;
  }

  @Override
  public SkipOption getSkipOption() {
    return skipOption;
  }

  @Override
  public TopOption getTopOption() {
    return topOption;
  }

  @Override
  public CountOption getCountOption() {
    return inlineCountOption;
  }

  @Override
  public SelectOption getSelectOption() {

    return selectOption;
  }

  @Override
  public ExpandOption getExpandOption() {
    return expandOption;
  }

  @Override
  public ApplyOption getApplyOption() {
    return applyOption;
  }

  public ExpandItemImpl setResourcePath(final UriInfoResource resourceInfo) {
    this.resourceInfo = resourceInfo;
    return this;
  }

  @Override
  public UriInfoResource getResourcePath() {

    return resourceInfo;
  }

  @Override
  public boolean isStar() {
    return isStar;
  }

  public ExpandItemImpl setIsStar(final boolean isStar) {
    this.isStar = isStar;
    return this;
  }

  @Override
  public boolean isRef() {
    return isRef;
  }

  public ExpandItemImpl setIsRef(final boolean isRef) {
    this.isRef = isRef;
    return this;
  }

  @Override
  public EdmType getStartTypeFilter() {
    return startTypeFilter;
  }

  public ExpandItemImpl setTypeFilter(final EdmType startTypeFilter) {
    this.startTypeFilter = startTypeFilter;
    return this;
  }

  @Override
  public boolean hasCountPath() {
    return this.hasCountPath;
  }
  
  public void setCountPath(boolean value) {
    this.hasCountPath = value;
  }
}
