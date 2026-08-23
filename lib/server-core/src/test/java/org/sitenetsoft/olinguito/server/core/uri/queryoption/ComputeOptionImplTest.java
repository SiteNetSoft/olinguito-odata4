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
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 4: pinned the $compute option type
 */
package org.sitenetsoft.olinguito.server.core.uri.queryoption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOptionKind;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.apply.ComputeExpressionImpl;

/**
 * The $compute option is a system query option wrapping the same {@code ComputeExpression} the
 * $apply transformation of that name uses ([OData-Protocol] 11.2.5.3).
 */
class ComputeOptionImplTest {

  @Test
  void reportsItsKindAndCollectsExpressions() {
    final ComputeOptionImpl option = new ComputeOptionImpl();
    assertEquals(SystemQueryOptionKind.COMPUTE, option.getKind());
    assertTrue(option.getExpressions().isEmpty());

    final ComputeExpressionImpl expression = new ComputeExpressionImpl().setAlias("Total");
    assertSame(expression, option.add(expression).getExpressions().get(0));
  }

  @Test
  void exposesItsExpressionsReadOnly() {
    final ComputeOptionImpl option = new ComputeOptionImpl();
    assertThrows(UnsupportedOperationException.class,
        () -> option.getExpressions().add(new ComputeExpressionImpl()));
  }
}
