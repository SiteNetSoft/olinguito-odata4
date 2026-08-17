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
 * Copyright 2026 SiteNetSoft - OData 4.01: alternate keys (Core.AlternateKeys)
 */
package org.sitenetsoft.olinguito.commons.api.edm;

/**
 * A single property reference of an {@link EdmAlternateKey} (Core.PropertyRef).
 */
public interface EdmAlternateKeyPropertyRef {

  /**
   * @return the Core.PropertyRef Name path as declared (e.g. "PropertyString" or "Address/City")
   */
  String getName();

  /**
   * @return the Core.PropertyRef Alias, or null
   */
  String getAlias();

  /**
   * @return the name used in the URL key predicate: the alias when present, else the declared name
   */
  String getUrlName();

  /**
   * @return the top-level primitive property this reference resolves to, or null when the path is nested
   *         or unresolvable (nested paths are out of scope: such a group is never matched by the URI parser)
   */
  EdmProperty getProperty();
}
