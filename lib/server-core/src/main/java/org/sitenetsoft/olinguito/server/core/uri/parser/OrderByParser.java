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
 * Copyright 2026 SiteNetSoft - OLINGO-1557: $it in $expand resolves to parent entity type
 * Copyright 2026 SiteNetSoft - Refuse sorting by a geo value
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.Collection;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Expression;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Literal;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Member;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriTokenizer.TokenKind;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.OrderByItemImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.OrderByOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

public class OrderByParser {

  private final Edm edm;
  private final OData odata;

  public OrderByParser(final Edm edm, final OData odata) {
    this.edm = edm;
    this.odata = odata;
  }

  public OrderByOption parse(UriTokenizer tokenizer, final EdmStructuredType referencedType,
      final Collection<String> crossjoinEntitySetNames, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    return parse(tokenizer, referencedType, crossjoinEntitySetNames, aliases, null);
  }

  public OrderByOption parse(UriTokenizer tokenizer, final EdmStructuredType referencedType,
      final Collection<String> crossjoinEntitySetNames, final Map<String, AliasQueryOption> aliases,
      final EdmType rootType)
      throws UriParserException, UriValidationException {
    OrderByOptionImpl orderByOption = new OrderByOptionImpl();
    do {
      final Expression orderByExpression = new ExpressionParser(edm, odata)
          .parse(tokenizer, referencedType, crossjoinEntitySetNames, aliases, rootType);
      OrderByItemImpl item = new OrderByItemImpl();
      item.setExpression(orderByExpression);
      // [OData-Protocol] section 11.2.6.2: "Values of type Edm.Stream or any of the Geo types cannot
      // be sorted."
      final EdmType orderByType = orderByExpression instanceof Member member
          ? member.getType()
          : orderByExpression instanceof Literal literal ? literal.getType() : null;
      if (ExpressionParser.isGeoType(orderByType)) {
        throw new UriParserSemanticException("A Geo type cannot be sorted.",
            UriParserSemanticException.MessageKeys.TYPES_NOT_COMPATIBLE,
            orderByType.getFullQualifiedName().getFullQualifiedNameAsString(),
            "", orderByExpression.toString(), "");
      }
      if (tokenizer.next(TokenKind.AscSuffix)) {
        item.setDescending(false);
      } else if (tokenizer.next(TokenKind.DescSuffix)) {
        item.setDescending(true);
      }
      orderByOption.addOrder(item);
    } while (tokenizer.next(TokenKind.COMMA));
    return orderByOption;
  }
}
