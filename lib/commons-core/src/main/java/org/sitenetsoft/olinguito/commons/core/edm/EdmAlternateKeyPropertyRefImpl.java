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
package org.sitenetsoft.olinguito.commons.core.edm;

import org.sitenetsoft.olinguito.commons.api.edm.EdmAlternateKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;

public class EdmAlternateKeyPropertyRefImpl implements EdmAlternateKeyPropertyRef {

  private final String name;
  private final String alias;
  private final EdmProperty property;

  public EdmAlternateKeyPropertyRefImpl(final String name, final String alias, final EdmProperty property) {
    this.name = name;
    this.alias = alias;
    this.property = property;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getAlias() {
    return alias;
  }

  @Override
  public String getUrlName() {
    return alias == null ? name : alias;
  }

  @Override
  public EdmProperty getProperty() {
    return property;
  }
}
