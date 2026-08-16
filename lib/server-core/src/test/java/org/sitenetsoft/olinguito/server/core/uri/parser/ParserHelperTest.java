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
 * Copyright 2026 SiteNetSoft - OData 4.01: pin the ambiguous-overload to 400 conversion
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAmbiguousOverloadException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;

/** Tests the mapping of EDM function-resolution errors onto URI-parser errors. */
class ParserHelperTest {

  private static final FullQualifiedName FUNCTION_NAME = new FullQualifiedName("ns", "func");
  private static final FullQualifiedName BINDING_TYPE = new FullQualifiedName("ns", "ET");
  private static final List<String> PARAMETER_NAMES = List.of("A");

  @Test
  void ambiguousUnboundOverloadBecomesSemanticException() {
    final Edm edm = mock(Edm.class);
    when(edm.getUnboundFunction(any(FullQualifiedName.class), anyList()))
        .thenThrow(new EdmAmbiguousOverloadException("Ambiguous function overload for 'ns.func'"));

    final UriParserSemanticException exception = assertThrows(UriParserSemanticException.class,
        () -> ParserHelper.getUnboundFunction(edm, FUNCTION_NAME, PARAMETER_NAMES));
    assertEquals(UriParserSemanticException.MessageKeys.FUNCTION_AMBIGUOUS, exception.getMessageKey());
  }

  @Test
  void ambiguousBoundOverloadBecomesSemanticException() {
    final Edm edm = mock(Edm.class);
    when(edm.getBoundFunction(any(FullQualifiedName.class), any(FullQualifiedName.class), anyBoolean(), anyList()))
        .thenThrow(new EdmAmbiguousOverloadException("Ambiguous function overload for 'ns.func'"));

    final UriParserSemanticException exception = assertThrows(UriParserSemanticException.class,
        () -> ParserHelper.getBoundFunction(edm, FUNCTION_NAME, BINDING_TYPE, false, PARAMETER_NAMES));
    assertEquals(UriParserSemanticException.MessageKeys.FUNCTION_AMBIGUOUS, exception.getMessageKey());
  }

  @Test
  void ambiguousFunctionImportOverloadBecomesSemanticException() {
    final EdmFunctionImport functionImport = mock(EdmFunctionImport.class);
    when(functionImport.getName()).thenReturn("func");
    when(functionImport.getUnboundFunction(anyList()))
        .thenThrow(new EdmAmbiguousOverloadException("Ambiguous function overload for 'func'"));

    final UriParserSemanticException exception = assertThrows(UriParserSemanticException.class,
        () -> ParserHelper.getUnboundFunction(functionImport, PARAMETER_NAMES));
    assertEquals(UriParserSemanticException.MessageKeys.FUNCTION_AMBIGUOUS, exception.getMessageKey());
  }

  @Test
  void genuineModelErrorKeepsPropagatingAsEdmException() {
    final Edm edm = mock(Edm.class);
    when(edm.getUnboundFunction(any(FullQualifiedName.class), anyList()))
        .thenThrow(new EdmException("model broken"));

    final EdmException exception = assertThrows(EdmException.class,
        () -> ParserHelper.getUnboundFunction(edm, FUNCTION_NAME, PARAMETER_NAMES));
    assertEquals("model broken", exception.getMessage());
  }

  @Test
  void genuineModelErrorKeepsPropagatingFromBoundFunction() {
    final Edm edm = mock(Edm.class);
    when(edm.getBoundFunction(any(FullQualifiedName.class), any(FullQualifiedName.class), anyBoolean(), anyList()))
        .thenThrow(new EdmException("No parameter specified for bound function: ns.func"));

    final EdmException exception = assertThrows(EdmException.class,
        () -> ParserHelper.getBoundFunction(edm, FUNCTION_NAME, BINDING_TYPE, false, PARAMETER_NAMES));
    assertEquals("No parameter specified for bound function: ns.func", exception.getMessage());
  }
}
