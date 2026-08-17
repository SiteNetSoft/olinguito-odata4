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
 * Copyright 2026 SiteNetSoft - OData 4.01: expose Core.AlternateKeys on the entity set
 */
package org.sitenetsoft.olinguito.commons.api.edm;

import java.util.List;

/**
 * A CSDL EntitySet element.
 * <br/>
 * EdmEntitySet is the container for entity type instances as described in the OData protocol. It can be the target of a
 * navigation property binding.
 */
public interface EdmEntitySet extends EdmBindingTarget {

  /**
   * @return true if entity set must be included in the service document
   */
  boolean isIncludeInServiceDocument();

  /**
   * @return true if the entity set allows for the next segment to be the key.
   */
  boolean isKeyAsSegmentAllowed();

  /**
   * Gets the alternate keys declared on this entity set with the {@code Core.AlternateKeys} term
   * (OData 4.01 URL Conventions 4.3.5).
   * <br/>
   * These are the set-level declarations only; the URI parser considers them together with the type-level
   * declarations of {@link EdmEntityType#getAlternateKeys()}.
   *
   * @return the declared alternate keys, in declaration order; never null, empty when none are declared
   */
  default List<EdmAlternateKey> getAlternateKeys() {
    return List.of();
  }
}
