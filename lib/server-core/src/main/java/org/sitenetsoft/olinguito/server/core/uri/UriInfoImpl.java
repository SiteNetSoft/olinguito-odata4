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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 1: $schemaversion system query option
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 4: the $compute system query option
 */
package org.sitenetsoft.olinguito.server.core.uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoAll;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoBatch;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoCrossjoin;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoEntityId;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoKind;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoMetadata;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoResource;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoService;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ApplyOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ComputeOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CountOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.DeltaTokenOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CustomQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FormatOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.IdOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.QueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SchemaVersionOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SearchOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SkipOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SkipTokenOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOptionKind;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.TopOption;

public class UriInfoImpl implements UriInfo {

  private UriInfoKind kind;

  // for $entity
  private List<String> entitySetNames = new ArrayList<>();
  // for $entity
  private EdmEntityType entityTypeCast;

  private UriResource lastResourcePart;
  private List<UriResource> pathParts = new ArrayList<>();

  private Map<SystemQueryOptionKind, SystemQueryOption> systemQueryOptions =
      new EnumMap<>(SystemQueryOptionKind.class);
  private Map<String, AliasQueryOption> aliases = new HashMap<>();
  private List<CustomQueryOption> customQueryOptions = new ArrayList<>();

  private String fragment;

  public UriInfoImpl setKind(final UriInfoKind kind) {
    this.kind = kind;
    return this;
  }

  @Override
  public UriInfoKind getKind() {
    return kind;
  }

  @Override
  public UriInfoAll asUriInfoAll() {
    return this;
  }

  @Override
  public UriInfoBatch asUriInfoBatch() {
    return this;
  }

  @Override
  public UriInfoCrossjoin asUriInfoCrossjoin() {
    return this;
  }

  @Override
  public UriInfoEntityId asUriInfoEntityId() {
    return this;
  }

  @Override
  public UriInfoService asUriInfoService() {
    return this;
  }

  @Override
  public UriInfoMetadata asUriInfoMetadata() {
    return this;
  }

  @Override
  public UriInfoResource asUriInfoResource() {
    return this;
  }

  public UriInfoImpl addEntitySetName(final String entitySet) {
    entitySetNames.add(entitySet);
    return this;
  }

  @Override
  public List<String> getEntitySetNames() {
    return Collections.unmodifiableList(entitySetNames);
  }

  public UriInfoImpl setEntityTypeCast(final EdmEntityType type) {
    entityTypeCast = type;
    return this;
  }

  @Override
  public EdmEntityType getEntityTypeCast() {
    return entityTypeCast;
  }

  public UriInfoImpl addResourcePart(final UriResource uriPathInfo) {
    pathParts.add(uriPathInfo);
    lastResourcePart = uriPathInfo;
    return this;
  }

  public UriResource getLastResourcePart() {
    return lastResourcePart;
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return Collections.unmodifiableList(pathParts);
  }

  public UriInfoImpl setQueryOption(final QueryOption option) {
    if (option instanceof SystemQueryOption systemQueryOption) {
      setSystemQueryOption(systemQueryOption);
    } else if (option instanceof AliasQueryOption aliasQueryOption) {
      addAlias(aliasQueryOption);
    } else if (option instanceof CustomQueryOption customQueryOption) {
      addCustomQueryOption(customQueryOption);
    }
    return this;
  }

  /**
   * Adds system query option.
   * @param systemOption the option to be added
   * @return this object for method chaining
   * @throws ODataRuntimeException if an unsupported option is provided
   * or an option of this kind has been added before
   */
  public UriInfoImpl setSystemQueryOption(final SystemQueryOption systemOption) {
    final SystemQueryOptionKind systemQueryOptionKind = systemOption.getKind();
    if (systemQueryOptions.containsKey(systemQueryOptionKind)) {
      throw new ODataRuntimeException("Double System Query Option: " + systemOption.getName());
    }

    switch (systemQueryOptionKind) {
    case EXPAND:
    case FILTER:
    case FORMAT:
    case ID:
    case COUNT:
    case ORDERBY:
    case SEARCH:
    case SELECT:
    case SKIP:
    case SKIPTOKEN:
    case DELTATOKEN:
    case TOP:
    case LEVELS:
    case APPLY:
    case SCHEMAVERSION:
    case COMPUTE:
      systemQueryOptions.put(systemQueryOptionKind, systemOption);
      break;
    default:
      throw new ODataRuntimeException("Unsupported System Query Option: " + systemOption.getName());
    }
    return this;
  }

  @Override
  public ExpandOption getExpandOption() {
    return (ExpandOption) systemQueryOptions.get(SystemQueryOptionKind.EXPAND);
  }

  @Override
  public FilterOption getFilterOption() {
    return (FilterOption) systemQueryOptions.get(SystemQueryOptionKind.FILTER);
  }

  @Override
  public FormatOption getFormatOption() {
    return (FormatOption) systemQueryOptions.get(SystemQueryOptionKind.FORMAT);
  }

  @Override
  public IdOption getIdOption() {
    return (IdOption) systemQueryOptions.get(SystemQueryOptionKind.ID);
  }

  @Override
  public CountOption getCountOption() {
    return (CountOption) systemQueryOptions.get(SystemQueryOptionKind.COUNT);
  }

  @Override
  public OrderByOption getOrderByOption() {
    return (OrderByOption) systemQueryOptions.get(SystemQueryOptionKind.ORDERBY);
  }

  @Override
  public SearchOption getSearchOption() {
    return (SearchOption) systemQueryOptions.get(SystemQueryOptionKind.SEARCH);
  }

  @Override
  public SelectOption getSelectOption() {
    return (SelectOption) systemQueryOptions.get(SystemQueryOptionKind.SELECT);
  }

  @Override
  public SkipOption getSkipOption() {
    return (SkipOption) systemQueryOptions.get(SystemQueryOptionKind.SKIP);
  }

  @Override
  public SkipTokenOption getSkipTokenOption() {
    return (SkipTokenOption) systemQueryOptions.get(SystemQueryOptionKind.SKIPTOKEN);
  }

  @Override
  public TopOption getTopOption() {
    return (TopOption) systemQueryOptions.get(SystemQueryOptionKind.TOP);
  }

  @Override
  public ApplyOption getApplyOption() {
    return (ApplyOption) systemQueryOptions.get(SystemQueryOptionKind.APPLY);
  }

  @Override
  public List<SystemQueryOption> getSystemQueryOptions() {
    return Collections.unmodifiableList(new ArrayList<SystemQueryOption>(systemQueryOptions.values()));
  }

  public UriInfoImpl addAlias(final AliasQueryOption alias) {
    if (aliases.containsKey(alias.getName())) {
      throw new ODataRuntimeException("Alias " + alias.getName() + " is already there.");
    } else {
      aliases.put(alias.getName(), alias);
    }
    return this;
  }

  @Override
  public String getValueForAlias(final String alias) {
    final AliasQueryOption aliasQueryOption = aliases.get(alias);
    return aliasQueryOption == null ? null : aliasQueryOption.getText();
  }

  public Map<String, AliasQueryOption> getAliasMap() {
    return Collections.unmodifiableMap(aliases);
  }

  @Override
  public List<AliasQueryOption> getAliases() {
    return Collections.unmodifiableList(new ArrayList<AliasQueryOption>(aliases.values()));
  }

  public UriInfoImpl addCustomQueryOption(final CustomQueryOption option) {
    if (option.getName() != null && !option.getName().isEmpty()) {
      customQueryOptions.add(option);
    }
    return this;
  }

  @Override
  public List<CustomQueryOption> getCustomQueryOptions() {
    return Collections.unmodifiableList(customQueryOptions);
  }

  public UriInfoImpl setFragment(final String fragment) {
    this.fragment = fragment;
    return this;
  }

  @Override
  public String getFragment() {
    return fragment;
  }
  
  @Override
  public DeltaTokenOption getDeltaTokenOption() {
    return (DeltaTokenOption) systemQueryOptions.get(SystemQueryOptionKind.DELTATOKEN);
  }

  @Override
  public SchemaVersionOption getSchemaVersionOption() {
    return (SchemaVersionOption) systemQueryOptions.get(SystemQueryOptionKind.SCHEMAVERSION);
  }

  @Override
  public ComputeOption getComputeOption() {
    return (ComputeOption) systemQueryOptions.get(SystemQueryOptionKind.COMPUTE);
  }
}
