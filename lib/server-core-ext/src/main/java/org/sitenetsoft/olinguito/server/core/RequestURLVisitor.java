/*
 * Copyright 2026 SiteNetSoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2026 SiteNetSoft - Add UriResourceDynamicProperty visit hook for OpenType support
 * Copyright 2026 SiteNetSoft - Made the new visit(UriResourceDynamicProperty) hook a default
 * no-op instead of widening visit(UriInfo)/visit(UriInfoResource) with a checked exception,
 * to avoid a binary/source-incompatible change to this shipped public interface
 */
package org.sitenetsoft.olinguito.server.core;

import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoAll;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoBatch;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoCrossjoin;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoEntityId;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoMetadata;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoResource;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoService;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceAction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceComplexProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceCount;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceDynamicProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceIt;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceLambdaAll;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceLambdaAny;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceLambdaVariable;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePrimitiveProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceRef;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceRoot;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceSingleton;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceValue;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ApplyOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CountOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.DeltaTokenOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FormatOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.IdOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SearchOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SkipOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SkipTokenOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.TopOption;

public interface RequestURLVisitor {

  void visit(UriInfo info);

  void visit(UriInfoService info);

  void visit(UriInfoAll info);

  void visit(UriInfoBatch info);

  void visit(UriInfoCrossjoin info);

  void visit(UriInfoEntityId info);

  void visit(UriInfoMetadata info);

  void visit(UriInfoResource info);

  // Walk UriInfoResource
  void visit(ExpandOption option);

  void visit(FilterOption info);

  void visit(FormatOption info);

  void visit(IdOption info, EdmEntityType type);

  void visit(CountOption info);

  void visit(OrderByOption option);

  void visit(SearchOption option);

  void visit(SelectOption option);

  void visit(SkipOption option);

  void visit(SkipTokenOption option);

  void visit(TopOption option);

  void visit(UriResourceCount option);
  
  void visit(DeltaTokenOption option);
  
  void visit(UriResourceRef info);

  void visit(UriResourceRoot info);

  void visit(UriResourceValue info);

  void visit(UriResourceAction info);

  void visit(UriResourceEntitySet info);

  void visit(UriResourceFunction info);

  void visit(UriResourceIt info);

  void visit(UriResourceLambdaAll info);

  void visit(UriResourceLambdaAny info);

  void visit(UriResourceLambdaVariable info);

  void visit(UriResourceNavigation info);

  void visit(UriResourceSingleton info);

  void visit(UriResourceComplexProperty info);

  void visit(UriResourcePrimitiveProperty info);

  /**
   * Visits a dynamic (undeclared, open-type) property path segment. See
   * {@link UriResourceDynamicProperty} - unlike {@link #visit(UriResourcePrimitiveProperty)} /
   * {@link #visit(UriResourceComplexProperty)}, there is no {@code EdmProperty} backing it.
   * {@code default} (rather than abstract, like every other {@code visit} method here) so adding
   * it does not break existing implementers of this shipped public interface; the 404 rejection
   * for dispatchers with no dynamic-property support is applied earlier, as a pre-check over the
   * parsed URI resource parts, rather than from within this hook - see
   * {@code ServiceDispatcher#rejectDynamicPropertySegments}.
   */
  default void visit(UriResourceDynamicProperty info) {
  }

  void visit(ApplyOption option);
}
