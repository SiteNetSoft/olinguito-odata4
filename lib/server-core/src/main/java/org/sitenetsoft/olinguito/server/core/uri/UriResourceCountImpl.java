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
 * Copyright 2026 SiteNetSoft - Added filter and search options to UriResourceCountImpl
 */
package org.sitenetsoft.olinguito.server.core.uri;

import org.sitenetsoft.olinguito.server.api.uri.UriResourceCount;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceKind;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SearchOption;

public class UriResourceCountImpl extends UriResourceImpl implements UriResourceCount {

  private FilterOption filterOption;
  private SearchOption searchOption;

  public UriResourceCountImpl() {
    super(UriResourceKind.count);
  }

  @Override
  public FilterOption getFilterOption() {
    return filterOption;
  }

  public UriResourceCountImpl setFilterOption(final FilterOption filterOption) {
    this.filterOption = filterOption;
    return this;
  }

  @Override
  public SearchOption getSearchOption() {
    return searchOption;
  }

  public UriResourceCountImpl setSearchOption(final SearchOption searchOption) {
    this.searchOption = searchOption;
    return this;
  }

  @Override
  public String getSegmentValue() {
    return "$count";
  }
}
