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
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 4: the $compute system query option
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ComputeOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Expression;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriTokenizer.TokenKind;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.ComputeOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.apply.ComputeExpressionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.apply.DynamicProperty;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.apply.DynamicStructuredType;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

/**
 * Parses the <code>$compute</code> system query option ([OData-Protocol] 11.2.5.3, [OData-ABNF]
 * rule <code>compute</code>): a comma-separated list of <code>commonExpr "as" odataIdentifier</code>.
 * <p>
 * Each alias is added to the referenced type as a dynamic property, which is what lets
 * <code>$select</code>, <code>$filter</code> and <code>$orderby</code> name it -- so this parser
 * runs before those are parsed.
 * <p>
 * The equivalent <code>$apply</code> transformation is parsed by
 * {@code ApplyParser#parseComputeTrafo}, which cannot be reused directly because it ends by
 * requiring the closing parenthesis of <code>compute(...)</code>; the standalone option ends at
 * the end of its value.
 */
public class ComputeParser {

  private final Edm edm;
  private final OData odata;

  public ComputeParser(final Edm edm, final OData odata) {
    this.edm = edm;
    this.odata = odata;
  }

  public ComputeOption parse(UriTokenizer tokenizer, final EdmStructuredType referencedType,
      final Map<String, AliasQueryOption> aliases) throws UriParserException, UriValidationException {
    final ComputeOptionImpl option = new ComputeOptionImpl();
    do {
      final Expression expression = new ExpressionParser(edm, odata)
          .parse(tokenizer, referencedType, null, aliases);
      final EdmType expressionType = ExpressionParser.getType(expression);
      if (expressionType == null || expressionType.getKind() != EdmTypeKind.PRIMITIVE) {
        throw new UriParserSemanticException("Compute expressions must return primitive values.",
            UriParserSemanticException.MessageKeys.ONLY_FOR_PRIMITIVE_TYPES, "compute");
      }
      ParserHelper.requireNext(tokenizer, TokenKind.AsOperator);
      ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
      final String alias = tokenizer.getText();
      if (referencedType instanceof DynamicStructuredType dynamicType) {
        if (dynamicType.hasStaticProperty(alias)) {
          throw new UriParserSemanticException("Alias '" + alias + "' is already a property.",
              UriParserSemanticException.MessageKeys.IS_PROPERTY, alias);
        }
        dynamicType.addProperty(new DynamicProperty(alias, expressionType));
      }
      option.add(new ComputeExpressionImpl().setExpression(expression).setAlias(alias));
    } while (tokenizer.next(TokenKind.COMMA));
    return option;
  }
}
