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
 * Copyright 2026 SiteNetSoft - New file: dynamic (open-type) property path-segment API
 * Copyright 2026 SiteNetSoft - Document the getType()/isCollection() contract for dynamic
 * properties
 */
package org.sitenetsoft.olinguito.server.api.uri;

/**
 * Used to describe a dynamic property used within a resource path on an open type.
 * Dynamic properties are not declared in the EDM; they are only known by name.
 * For example: http://.../serviceroot/entityset(1)/dynamicProperty
 *
 * <p>Because a dynamic property carries no EDM declaration, its EDM type is not known until an
 * actual instance value is inspected at runtime: {@link #getType()} always returns {@code null}
 * and {@link #isCollection()} always returns {@code false} for a dynamic property, regardless of
 * what value the addressed instance may actually hold.
 */
public interface UriResourceDynamicProperty extends UriResourcePartTyped {

  /**
   * @return Name of the dynamic property used in the resource path
   */
  String getPropertyName();

}
