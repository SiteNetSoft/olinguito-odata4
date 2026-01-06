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
package org.sitenetsoft.olinguito.server.core.uri;

import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceComplexProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceKind;

public class UriResourceComplexPropertyImpl extends UriResourceTypedImpl implements UriResourceComplexProperty {

  private final EdmProperty property;

  public UriResourceComplexPropertyImpl(final EdmProperty property) {
    super(UriResourceKind.complexProperty);
    this.property = property;
  }

  @Override
  public EdmProperty getProperty() {
    return property;
  }

  @Override
  public EdmComplexType getComplexType() {
    return (EdmComplexType) getType();
  }

  @Override
  public EdmComplexType getComplexTypeFilter() {
    return (EdmComplexType) getTypeFilter();
  }

  @Override
  public EdmType getType() {
    return property.getType();
  }

  @Override
  public boolean isCollection() {
    return property.isCollection();
  }

  @Override
  public String getSegmentValue() {
    return property.getName();
  }
}
