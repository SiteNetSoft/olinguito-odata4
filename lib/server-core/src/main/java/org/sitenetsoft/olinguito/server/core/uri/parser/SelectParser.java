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
 * Copyright 2026 SiteNetSoft - Add OpenType support ($select of dynamic properties)
 * Copyright 2026 SiteNetSoft - OData 4.01: map ambiguous optional-parameter overloads to a 400 response
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 2: nested option group on a selected property
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoKind;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePartTyped;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.core.uri.UriInfoImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceActionImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceComplexPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceDynamicPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceFunctionImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceNavigationPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourcePrimitivePropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriTokenizer.TokenKind;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOptionKind;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.CountOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SkipOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.TopOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SelectItemImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SelectOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

public class SelectParser {

  private final Edm edm;
  /** Needed by the nested $filter and $orderby parsers; null when no nested option can occur. */
  private final OData odata;

  public SelectParser(final Edm edm) {
    this(edm, null);
  }

  public SelectParser(final Edm edm, final OData odata) {
    this.edm = edm;
    this.odata = odata;
  }

  public SelectOption parse(UriTokenizer tokenizer, final EdmStructuredType referencedType,
      final boolean referencedIsCollection) throws UriParserException, UriValidationException {
    List<SelectItem> selectItems = new ArrayList<>();
    SelectItem item;
    do {
      item = parseItem(tokenizer, referencedType, referencedIsCollection);
      selectItems.add(item);
    } while (tokenizer.next(TokenKind.COMMA));

    return new SelectOptionImpl().setSelectItems(selectItems);
  }

  private SelectItem parseItem(UriTokenizer tokenizer,
      final EdmStructuredType referencedType, final boolean referencedIsCollection)
      throws UriParserException, UriValidationException {
    SelectItemImpl item = new SelectItemImpl();
    if (tokenizer.next(TokenKind.STAR)) {
      item.setStar(true);

    } else if (tokenizer.next(TokenKind.QualifiedName)) {
      // The namespace or its alias could consist of dot-separated OData identifiers.
      final FullQualifiedName allOperationsInSchema = parseAllOperationsInSchema(tokenizer);
      if (allOperationsInSchema != null) {
        item.addAllOperationsInSchema(allOperationsInSchema);

      } else {
        ensureReferencedTypeNotNull(referencedType);
        final FullQualifiedName qualifiedName = new FullQualifiedName(tokenizer.getText());
        EdmStructuredType type = edm.getEntityType(qualifiedName);
        if (type == null) {
          type = edm.getComplexType(qualifiedName);
        }
        if (type == null) {
          item.setResourcePath(new UriInfoImpl().setKind(UriInfoKind.resource).addResourcePart(
              parseBoundOperation(tokenizer, qualifiedName, referencedType, referencedIsCollection)));

        } else {
          if (type.compatibleTo(referencedType)) {
            item.setTypeFilter(type);
            if (tokenizer.next(TokenKind.SLASH)) {
              ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
              UriInfoImpl resource = new UriInfoImpl().setKind(UriInfoKind.resource);
              addSelectPath(tokenizer, type, resource, item);
              item.setResourcePath(resource);
            }
          } else {
            throw new UriParserSemanticException("The type cast is not compatible.",
                UriParserSemanticException.MessageKeys.INCOMPATIBLE_TYPE_FILTER, type.getName());
          }
        }
      }

    } else {
      ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
      // The namespace or its alias could be a single OData identifier.
      final FullQualifiedName allOperationsInSchema = parseAllOperationsInSchema(tokenizer);
      if (allOperationsInSchema != null) {
        item.addAllOperationsInSchema(allOperationsInSchema);

      } else {
        ensureReferencedTypeNotNull(referencedType);
        UriInfoImpl resource = new UriInfoImpl().setKind(UriInfoKind.resource);
        addSelectPath(tokenizer, referencedType, resource, item);
        item.setResourcePath(resource);
      }
    }

    return item;
  }

  private FullQualifiedName parseAllOperationsInSchema(UriTokenizer tokenizer) throws UriParserException {
    final String namespace = tokenizer.getText();
    if (tokenizer.next(TokenKind.DOT)) {
      if (tokenizer.next(TokenKind.STAR)) {
        // Validate the namespace.  Currently a namespace from a non-default schema is not supported.
        // There is no direct access to the namespace without loading the whole schema;
        // however, the default entity container should always be there, so its access methods can be used.
        if (edm.getEntityContainer(new FullQualifiedName(namespace, edm.getEntityContainer().getName())) == null) {
          throw new UriParserSemanticException("Wrong namespace '" + namespace + "'.",
              UriParserSemanticException.MessageKeys.UNKNOWN_PART, namespace);
        }
        return new FullQualifiedName(namespace, tokenizer.getText());
      } else {
        throw new UriParserSemanticException("Expected star after dot.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, "");
      }
    }
    return null;
  }

  private void ensureReferencedTypeNotNull(final EdmStructuredType referencedType) throws UriParserException {
    if (referencedType == null) {
      throw new UriParserSemanticException("The referenced part is not typed.",
          UriParserSemanticException.MessageKeys.ONLY_FOR_TYPED_PARTS, "select");
    }
  }

  private UriResourcePartTyped parseBoundOperation(UriTokenizer tokenizer, final FullQualifiedName qualifiedName,
      final EdmStructuredType referencedType, final boolean referencedIsCollection) throws UriParserException {
    final EdmAction boundAction = edm.getBoundAction(qualifiedName,
        referencedType.getFullQualifiedName(),
        referencedIsCollection);
    if (boundAction == null) {
      final List<String> parameterNames = parseFunctionParameterNames(tokenizer);
      final EdmFunction boundFunction = ParserHelper.getBoundFunction(edm, qualifiedName,
          referencedType.getFullQualifiedName(), referencedIsCollection, parameterNames);
      if (boundFunction == null) {
        throw new UriParserSemanticException("Function not found.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, qualifiedName.getFullQualifiedNameAsString());
      } else {
        return new UriResourceFunctionImpl(null, boundFunction, null);
      }
    } else {
      return new UriResourceActionImpl(boundAction);
    }
  }

  private List<String> parseFunctionParameterNames(UriTokenizer tokenizer) throws UriParserException {
    List<String> names = new ArrayList<>();
    if (tokenizer.next(TokenKind.OPEN)) {
      do {
        ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
        names.add(tokenizer.getText());
      } while (tokenizer.next(TokenKind.COMMA));
      ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    }
    return names;
  }

  private void addSelectPath(UriTokenizer tokenizer, final EdmStructuredType referencedType,
      UriInfoImpl resource, SelectItemImpl item) throws UriParserException, UriValidationException {
    final String name = tokenizer.getText();
    final EdmProperty property = referencedType.getStructuralProperty(name);

    if (property == null) {
      final EdmNavigationProperty navigationProperty = referencedType.getNavigationProperty(name);
      if (navigationProperty == null) {
        if (referencedType.isOpenType()) {
          // Dynamic (undeclared) property of an open type; its type is unknown until runtime,
          // so it must be a leaf: nothing else may follow it in the select path.
          resource.addResourcePart(new UriResourceDynamicPropertyImpl(name));
          return;
        }
        throw new UriParserSemanticException("Selected property not found.",
            UriParserSemanticException.MessageKeys.EXPRESSION_PROPERTY_NOT_IN_TYPE,
            referencedType.getName(), name);
      } else {
        resource.addResourcePart(new UriResourceNavigationPropertyImpl(navigationProperty));
        // [OData-ABNF] selectProperty gives a bare navigationProperty no bracketed option group.
        rejectOptionGroup(tokenizer, name);
      }

    } else if (property.isPrimitive()
        || property.getType().getKind() == EdmTypeKind.ENUM
        || property.getType().getKind() == EdmTypeKind.DEFINITION) {
      resource.addResourcePart(new UriResourcePrimitivePropertyImpl(property));
      if (property.isCollection()) {
        // [OData-ABNF]: a primitiveColProperty takes selectOptionPC only -- no nested $select.
        parseSelectOptions(tokenizer, referencedType, property, item, false);
      } else {
        rejectOptionGroup(tokenizer, name);
      }

    } else {
      UriResourceComplexPropertyImpl complexPart = new UriResourceComplexPropertyImpl(property);
      resource.addResourcePart(complexPart);
      if (tokenizer.next(TokenKind.SLASH)) {
        if (tokenizer.next(TokenKind.QualifiedName)) {
          final FullQualifiedName qualifiedName = new FullQualifiedName(tokenizer.getText());
          final EdmComplexType type = edm.getComplexType(qualifiedName);
          if (type == null) {
            throw new UriParserSemanticException("Type not found.",
                UriParserSemanticException.MessageKeys.UNKNOWN_TYPE, qualifiedName.getFullQualifiedNameAsString());
          } else if (type.compatibleTo(property.getType())) {
            complexPart.setTypeFilter(type);
            if (tokenizer.next(TokenKind.SLASH)) {
              if (tokenizer.next(TokenKind.ODataIdentifier)) {
                addSelectPath(tokenizer, type, resource, item);
              } else {
                throw new UriParserSemanticException("Unknown part after '/'.",
                    UriParserSemanticException.MessageKeys.UNKNOWN_PART, "");
              }
            }
          } else {
            throw new UriParserSemanticException("The type cast is not compatible.",
                UriParserSemanticException.MessageKeys.INCOMPATIBLE_TYPE_FILTER, type.getName());
          }
        } else if (tokenizer.next(TokenKind.ODataIdentifier)) {
          addSelectPath(tokenizer, (EdmStructuredType) property.getType(), resource, item);
        } else if (tokenizer.next(TokenKind.SLASH)) {
          throw new UriParserSyntaxException("Illegal $select expression.",
              UriParserSyntaxException.MessageKeys.SYNTAX);
        } else {
          throw new UriParserSemanticException("Unknown part after '/'.",
              UriParserSemanticException.MessageKeys.UNKNOWN_PART, "");
        }
      } else {
        // [OData-ABNF]: a selectPath -- a complex property, optionally cast -- takes the full
        // selectOption group, which is $select plus the collection options when it is a collection.
        parseSelectOptions(tokenizer, referencedType, property, item, true);
      }
    }
  }

  /**
   * Parses the parenthesized, semicolon-separated option group a selected property may carry.
   * [OData-Protocol] 11.2.5.1: "Allowed system query options are $select and $compute for complex
   * properties, plus $filter, $search, $count, $orderby, $skip, and $top for collection-valued
   * properties." $compute is not accepted yet: the option does not exist elsewhere in the service.
   * @param allowNested whether $select may appear, i.e. whether the property is complex
   */
  private void parseSelectOptions(UriTokenizer tokenizer, final EdmStructuredType referencedType,
      final EdmProperty property, SelectItemImpl item, final boolean allowNested)
      throws UriParserException, UriValidationException {
    if (!tokenizer.next(TokenKind.OPEN)) {
      return;
    }
    final boolean isCollection = property.isCollection();
    final EdmStructuredType nestedType = property.getType() instanceof EdmStructuredType structured
        ? structured
        : referencedType;
    do {
      if (allowNested && tokenizer.next(TokenKind.SELECT)) {
        ParserHelper.requireNext(tokenizer, TokenKind.EQ);
        item.setSelectOption(new SelectParser(edm, odata).parse(tokenizer, nestedType, isCollection));

      } else if (isCollection && tokenizer.next(TokenKind.FILTER)) {
        ParserHelper.requireNext(tokenizer, TokenKind.EQ);
        item.setFilterOption(new FilterParser(edm, odata).parse(tokenizer, nestedType, null, null, null));

      } else if (isCollection && tokenizer.next(TokenKind.ORDERBY)) {
        ParserHelper.requireNext(tokenizer, TokenKind.EQ);
        item.setOrderByOption(new OrderByParser(edm, odata).parse(tokenizer, nestedType, null, null, null));

      } else if (isCollection && tokenizer.next(TokenKind.SEARCH)) {
        ParserHelper.requireNext(tokenizer, TokenKind.EQ);
        ParserHelper.bws(tokenizer);
        item.setSearchOption(new SearchParser().parse(tokenizer));

      } else if (isCollection && tokenizer.next(TokenKind.COUNT)) {
        ParserHelper.requireNext(tokenizer, TokenKind.EQ);
        ParserHelper.requireNext(tokenizer, TokenKind.BooleanValue);
        CountOptionImpl countOption = new CountOptionImpl();
        countOption.setText(tokenizer.getText());
        countOption.setValue(Boolean.parseBoolean(tokenizer.getText()));
        item.setCountOption(countOption);

      } else if (isCollection && tokenizer.next(TokenKind.SKIP)) {
        ParserHelper.requireNext(tokenizer, TokenKind.EQ);
        ParserHelper.requireNext(tokenizer, TokenKind.IntegerValue);
        SkipOptionImpl skipOption = new SkipOptionImpl();
        skipOption.setText(tokenizer.getText());
        skipOption.setValue(ParserHelper.parseNonNegativeInteger(
            SystemQueryOptionKind.SKIP.toString(), tokenizer.getText(), true));
        item.setSkipOption(skipOption);

      } else if (isCollection && tokenizer.next(TokenKind.TOP)) {
        ParserHelper.requireNext(tokenizer, TokenKind.EQ);
        ParserHelper.requireNext(tokenizer, TokenKind.IntegerValue);
        TopOptionImpl topOption = new TopOptionImpl();
        topOption.setText(tokenizer.getText());
        topOption.setValue(ParserHelper.parseNonNegativeInteger(
            SystemQueryOptionKind.TOP.toString(), tokenizer.getText(), true));
        item.setTopOption(topOption);

      } else {
        throw new UriParserSemanticException("Not allowed as select option.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, property.getName());
      }
    } while (tokenizer.next(TokenKind.SEMI));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
  }

  /**
   * Rejects an option group on a select item that may not carry one: a plain primitive property or
   * a bare navigation property ([OData-ABNF] selectProperty).
   */
  private void rejectOptionGroup(UriTokenizer tokenizer, final String name) throws UriParserException {
    if (tokenizer.next(TokenKind.OPEN)) {
      throw new UriParserSemanticException("Options are not allowed on this select item.",
          UriParserSemanticException.MessageKeys.UNKNOWN_PART, name);
    }
  }
}
