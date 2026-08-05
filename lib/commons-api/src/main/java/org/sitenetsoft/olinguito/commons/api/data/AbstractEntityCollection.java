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
 * Copyright 2026 SiteNetSoft - OLINGO-1540: document Int32 limit of the inline count (deferred)
 */
package org.sitenetsoft.olinguito.commons.api.data;

import java.net.URI;
import java.util.List;

public abstract class AbstractEntityCollection extends AbstractODataObject implements Iterable<Entity> {
  /**
   * Gets the inline count.
   *
   * <p>Note: the inline count is limited to {@code Integer.MAX_VALUE}. The OData specification
   * defines {@code @odata.count} as {@code Edm.Int64}; widening this to {@code long} is deferred
   * (OLINGO-1540) because it would be a breaking public-API change.</p>
   *
   * @return the inline count, or {@code null} if not set
   */
  public abstract Integer getCount();

  public abstract URI getNext();

  public abstract URI getDeltaLink();
  
  public abstract List<Operation> getOperations();
}
