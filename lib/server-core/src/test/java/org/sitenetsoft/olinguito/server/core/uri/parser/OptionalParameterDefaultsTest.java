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
 * Copyright 2026 SiteNetSoft - OData 4.01: single resolver for operation-parameter URI literals and
 * optional-parameter default values
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.edm.EdmMapping;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;

class OptionalParameterDefaultsTest {

  @Test
  void mappingWithoutJavaClassFallsBackToTheDefaultType() {
    final EdmMapping mapping = mock(EdmMapping.class);
    when(mapping.getMappedJavaClass()).thenReturn(null);
    final EdmParameter parameter = mock(EdmParameter.class);
    when(parameter.getMapping()).thenReturn(mapping);
    final EdmPrimitiveType primitiveType = mock(EdmPrimitiveType.class);
    when(primitiveType.getDefaultType()).thenAnswer(invocation -> String.class);

    assertSame(String.class, OptionalParameterDefaults.targetClass(parameter, primitiveType));
  }

  @Test
  void aParameterWithoutATypeIsNotPrimitiveLike() {
    final EdmParameter parameter = mock(EdmParameter.class);
    when(parameter.getType()).thenReturn(null);

    assertFalse(OptionalParameterDefaults.isPrimitiveLike(parameter));
  }
}
