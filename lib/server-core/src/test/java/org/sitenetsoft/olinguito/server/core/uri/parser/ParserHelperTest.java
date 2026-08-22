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
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.server.api.OData;

/** Tests the mapping of EDM function-resolution errors onto URI-parser errors. */
class ParserHelperTest {

  @Test
  void quotedLiteralIsReinterpretedAgainstTheExpectedType() {
    final OData odata = OData.newInstance();
    final EdmPrimitiveType int32 = odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Int32);
    final EdmPrimitiveType duration = odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Duration);
    final EdmPrimitiveType string = odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.String);

    // [OData-Protocol] 13.2.1 item 9a: a quoted string is cast to the target primitive type.
    assertEquals("1", ParserHelper.reinterpretQuotedLiteral("'1'", int32));
    // item 9b: the unprefixed duration form is still quoted, so restore the prefix.
    assertEquals("duration'P12D'", ParserHelper.reinterpretQuotedLiteral("'P12D'", duration));
    // Already-correct forms are untouched.
    assertEquals("1", ParserHelper.reinterpretQuotedLiteral("1", int32));
    assertEquals("duration'P12D'", ParserHelper.reinterpretQuotedLiteral("duration'P12D'", duration));
    // A string-typed target keeps its quotes, and an escaped quote survives unquoting.
    assertEquals("'abc'", ParserHelper.reinterpretQuotedLiteral("'abc'", string));
    assertEquals("a'b", ParserHelper.reinterpretQuotedLiteral("'a''b'", int32));
    // Nulls and non-literals are passed through.
    assertEquals(null, ParserHelper.reinterpretQuotedLiteral(null, int32));
    assertEquals("'1'", ParserHelper.reinterpretQuotedLiteral("'1'", null));
  }

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
