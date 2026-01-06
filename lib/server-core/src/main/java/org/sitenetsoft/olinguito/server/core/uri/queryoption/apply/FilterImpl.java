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
package org.sitenetsoft.olinguito.server.core.uri.queryoption.apply;

import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.apply.Filter;

/**
 * Represents the filter transformation.
 */
public class FilterImpl implements Filter {

  private FilterOption filterOption = null;

  @Override
  public Kind getKind() {
    return Kind.FILTER;
  }

  @Override
  public FilterOption getFilterOption() {
    return filterOption;
  }

  public FilterImpl setFilterOption(final FilterOption filterOption) {
    this.filterOption = filterOption;
    return this;
  }
}
