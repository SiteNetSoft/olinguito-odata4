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
 * Copyright 2026 SiteNetSoft - Add OpenType support (getSelectedPaths null-vs-set semantics
 * for dynamic-only and mixed selects, one-arg overload)
 */
package org.sitenetsoft.olinguito.server.core.serializer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectItem;
import org.sitenetsoft.olinguito.server.core.uri.parser.Parser;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.junit.jupiter.api.Test;

/**
 * Pins the null-vs-set semantics of the one-arg {@link ExpandSelectHelper#getSelectedPaths(List)}
 * overload for dynamic (open-type) select items: a {@code null} return means "no filter, write
 * everything", so a dynamic-only or mixed select item list must never come back as {@code null}
 * (nor silently drop the dynamic name from a mixed set), or callers such as
 * {@code ODataJsonSerializer#complexCollection}/{@code #complex} would over-include every
 * property instead of honoring the requested selection.
 */
class ExpandSelectHelperOpenTypeTest {

  private static final OData odata = OData.newInstance();
  private static final Edm edm = odata.createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList()).getEdm();

  @Test
  void dynamicOnlySelectYieldsNonNullPathSetNotAllProperties() throws Exception {
    final List<SelectItem> selectItems =
        new Parser(edm, odata).parseUri("ESOpen", "$select=Custom", null, null)
            .getSelectOption().getSelectItems();

    final Set<List<String>> selectedPaths = ExpandSelectHelper.getSelectedPaths(selectItems);

    assertNotNull(selectedPaths, "A dynamic-only select must not degrade to null (= select all)");
    assertEquals(1, selectedPaths.size());
    assertTrue(selectedPaths.contains(List.of("Custom")));
  }

  @Test
  void mixedDeclaredAndDynamicSelectKeepsBothNames() throws Exception {
    final List<SelectItem> selectItems =
        new Parser(edm, odata).parseUri("ESOpen", "$select=PropertyString,Custom", null, null)
            .getSelectOption().getSelectItems();

    final Set<List<String>> selectedPaths = ExpandSelectHelper.getSelectedPaths(selectItems);

    assertNotNull(selectedPaths);
    assertEquals(2, selectedPaths.size());
    assertTrue(selectedPaths.contains(List.of("PropertyString")),
        "Declared property name must still be present");
    assertTrue(selectedPaths.contains(List.of("Custom")),
        "Dynamic property name must not be silently dropped from a mixed select");
  }
}
