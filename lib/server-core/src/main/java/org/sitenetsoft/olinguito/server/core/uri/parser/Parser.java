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
 * Copyright 2026 SiteNetSoft - Modernized Collections usage
 * Copyright 2026 SiteNetSoft - OLINGO-1324: Tolerate trailing slash in URI paths
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 1: $schemaversion system query option
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 1: key-as-segment URL convention
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoKind;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceAction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceCount;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePartTyped;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceRef;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceValue;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.AliasQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ApplyItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ApplyOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.QueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SearchOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOptionKind;
import org.sitenetsoft.olinguito.server.core.uri.UriInfoImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceStartingTypeFilterImpl;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriTokenizer.TokenKind;
import org.sitenetsoft.olinguito.server.core.uri.parser.search.SearchParser;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.AliasQueryOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.ApplyOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.CountOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.DeltaTokenOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.ExpandOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.FilterOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.FormatOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.IdOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.OrderByOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SchemaVersionOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SearchOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SelectOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SkipOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SkipTokenOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SystemQueryOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.TopOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.apply.DynamicStructuredType;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

public class Parser {
  private static final String ATOM = "atom";
  private static final String JSON = "json";
  private static final String XML = "xml";
  private static final String DOLLAR = "$";
  private static final String AT = "@";
  private static final String NULL = "null";
  private static final String ENTITY = "$entity";
  private static final String HTTP = "http";

  private final Edm edm;
  private final OData odata;
  private boolean keyAsSegment;

  public Parser(final Edm edm, final OData odata) {
    this.edm = edm;
    this.odata = odata;
  }

  /**
   * Enables or disables the key-as-segment URL convention (URL Conventions section 4.3.6) for all
   * entity collections. Off by default; entity sets and navigation properties flagged in the model
   * support the convention regardless of this setting.
   * @param enabled whether the convention is enabled service-wide
   * @return this parser
   */
  public Parser setKeyAsSegment(final boolean enabled) {
    keyAsSegment = enabled;
    return this;
  }

  public UriInfo parseUri(final String path, final String query, final String fragment, String baseUri)
      throws UriParserException, UriValidationException {

    UriInfoImpl contextUriInfo = new UriInfoImpl();
   
    // Read the query options (system and custom options).
    // This is done before parsing the resource path because the aliases have to be available there.
    // System query options that can only be parsed with context from the resource path will be post-processed later.
    final List<QueryOption> options =
        query == null ? List.of() : UriDecoder.splitAndDecodeOptions(query);
    final boolean formEncoding = options.stream()
        .anyMatch(o -> UriDecoder.ACCEPT_FORM_ENCODING.equals(o.getName())
            && Boolean.parseBoolean(o.getText()));
    for (final QueryOption option : options) {
      final String optionName = option.getName();
      String value = option.getText();
      if (formEncoding) {
        value = getFormEncodedValue(value);
      }
      // Parse the untyped option and retrieve a system-option or alias-option instance (or null for a custom option).
      final QueryOption parsedOption = parseOption(optionName, value);
      try {
        contextUriInfo.setQueryOption(parsedOption == null ? option : parsedOption);
      } catch (final ODataRuntimeException e) {
        throw new UriParserSyntaxException(
            parsedOption instanceof SystemQueryOption ?
                "Double system query option!" :
                "Alias already specified! Name: " + optionName,
            e,
            parsedOption instanceof SystemQueryOption ?
                UriParserSyntaxException.MessageKeys.DOUBLE_SYSTEM_QUERY_OPTION :
                UriParserSyntaxException.MessageKeys.DUPLICATED_ALIAS,
            optionName);
      }
    }

    // Read the decoded path segments.
    EdmType contextType = null;
    boolean contextIsCollection = false;

    List<String> pathSegmentsDecoded = UriDecoder.splitAndDecodePath(path);
    int numberOfSegments = pathSegmentsDecoded.size();
    // Remove an initial empty segment resulting from the expected '/' at the beginning of the path.
    if (numberOfSegments > 1 && pathSegmentsDecoded.get(0).isEmpty()) {
      pathSegmentsDecoded.remove(0);
      numberOfSegments--;
    }
    // Remove a trailing empty segment resulting from a trailing '/' in the path,
    // but only if the preceding segment is a non-empty resource segment (not a double-slash).
    if (numberOfSegments > 1 && pathSegmentsDecoded.get(numberOfSegments - 1).isEmpty()
        && !pathSegmentsDecoded.get(numberOfSegments - 2).isEmpty()
        && !pathSegmentsDecoded.get(numberOfSegments - 2).startsWith("$")) {
      pathSegmentsDecoded.remove(numberOfSegments - 1);
      numberOfSegments--;
    }

    final String firstSegment = pathSegmentsDecoded.get(0);

    if (firstSegment.isEmpty()) {
      ensureLastSegment(firstSegment, 1, numberOfSegments);
      contextUriInfo.setKind(UriInfoKind.service);

    } else if ("$batch".equals(firstSegment)) {
      ensureLastSegment(firstSegment, 1, numberOfSegments);
      contextUriInfo.setKind(UriInfoKind.batch);

    } else if ("$metadata".equals(firstSegment)) {
      ensureLastSegment(firstSegment, 1, numberOfSegments);
      contextUriInfo.setKind(UriInfoKind.metadata);
      contextUriInfo.setFragment(fragment);

    } else if ("$all".equals(firstSegment)) {
      ensureLastSegment(firstSegment, 1, numberOfSegments);
      contextUriInfo.setKind(UriInfoKind.all);
      contextIsCollection = true;

    } else if ("$entity".equals(firstSegment)) {
      if (null != contextUriInfo.getIdOption()) {
        String idOptionText = contextUriInfo.getIdOption().getText();
        if (idOptionText.startsWith(HTTP)) {
          baseUri = UriDecoder.decode(baseUri);
          if (idOptionText.contains(baseUri)) {
            idOptionText = idOptionText.substring(baseUri.length() + 1);
          } else {
            throw new UriParserSemanticException("$id cannot have an absolute path",
                UriParserSemanticException.MessageKeys.NOT_IMPLEMENTED_SYSTEM_QUERY_OPTION);
          }
        }
        if (numberOfSegments > 1) {
          /**
           * If url is of the form 
           * http://localhost:8080/odata-server-tecsvc/odata.svc/$entity/
           * olingo.odata.test1.ETAllPrim?$id=ESAllPrim(32767)
           */
          final ResourcePathParser resourcePathParser = new ResourcePathParser
            (edm, contextUriInfo.getAliasMap(), keyAsSegment);
          String typeCastSegment = pathSegmentsDecoded.get(1);
          ensureLastSegment(typeCastSegment, 2, numberOfSegments);
          contextType = resourcePathParser.parseDollarEntityTypeCast(typeCastSegment);
          contextUriInfo = (UriInfoImpl) new Parser(edm, odata).setKeyAsSegment(keyAsSegment).
              parseUri("/" + idOptionText, query, fragment, baseUri);
          contextUriInfo.setEntityTypeCast((EdmEntityType) contextType);
        } else if (numberOfSegments == 1) {
          /**
           * If url is of the form 
           * http://localhost:8080/odata-server-tecsvc/odata.svc/$entity?$id=ESAllPrim(32527)
           */
          contextUriInfo = (UriInfoImpl) new Parser(edm, odata).setKeyAsSegment(keyAsSegment).
                  parseUri("/" + idOptionText, query, fragment, baseUri);
        }
        contextType = contextUriInfo.getEntityTypeCast();
        contextUriInfo.setKind(UriInfoKind.entityId);
        contextIsCollection = false;
      } else {
        /**
         * If url is of the form 
         * http://localhost:8080/odata-server-tecsvc/odata.svc/$entity/olingo.odata.test1.ETKeyNav/$ref
         */
        ensureLastSegment(firstSegment, 2, numberOfSegments);
        /**
         * If url is of the form 
         * http://localhost:8080/odata-server-tecsvc/odata.svc/$entity/olingo.odata.test1.ETKeyNav
         */
        throw new UriParserSyntaxException("The entity-id MUST be specified using the system query option $id",
                  UriParserSyntaxException.MessageKeys.ENTITYID_MISSING_SYSTEM_QUERY_OPTION_ID);
      }
    } else if (firstSegment.startsWith("$crossjoin")) {
      ensureLastSegment(firstSegment, 1, numberOfSegments);
      contextUriInfo.setKind(UriInfoKind.crossjoin);
      final List<String> entitySetNames =
          new ResourcePathParser(edm, contextUriInfo.getAliasMap(), keyAsSegment)
              .parseCrossjoinSegment(firstSegment);
      for (final String name : entitySetNames) {
        contextUriInfo.addEntitySetName(name);
      }
      contextIsCollection = true;

    } else {
      contextUriInfo.setKind(UriInfoKind.resource);
      final ResourcePathParser resourcePathParser =
          new ResourcePathParser(edm, contextUriInfo.getAliasMap(), keyAsSegment);
      int count = 0;
      UriResource lastSegment = null;
      for (final String pathSegment : pathSegmentsDecoded) {
        count++;
        if (pathSegment.startsWith(ENTITY)) {
          /**
           * If url is of the form 
           * http://localhost:8080/odata-server-tecsvc/odata.svc/ESAllPrim/$entity
           */
          throw new UriParserSyntaxException("The entity-id MUST be specified using the system query option $id",
                    UriParserSyntaxException.MessageKeys.ENTITYID_MISSING_SYSTEM_QUERY_OPTION_ID);
        } else {
          final UriResource segment = resourcePathParser.parsePathSegment(pathSegment, lastSegment);
          if (segment != null) {
            if (segment instanceof UriResourceCount
                || segment instanceof UriResourceRef
                || segment instanceof UriResourceValue) {
              ensureLastSegment(pathSegment, count, numberOfSegments);
            } else if (segment instanceof UriResourceAction
                || segment instanceof UriResourceFunction uriResFunc
                && !uriResFunc.getFunction().isComposable()) {
              if (count < numberOfSegments) {
                throw new UriValidationException(
                    "The segment of an action or of a non-composable function must be the last resource-path segment.",
                    UriValidationException.MessageKeys.UNALLOWED_RESOURCE_PATH,
                    pathSegmentsDecoded.get(count));
              }
              lastSegment = segment;
            } else if (segment instanceof UriResourceStartingTypeFilterImpl) {
              throw new UriParserSemanticException("First resource-path segment must not be namespace-qualified.",
                  UriParserSemanticException.MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT);
            } else {
              lastSegment = segment;
            }
            contextUriInfo.addResourcePart(segment);
          }
        }
      }

      resourcePathParser.requireCompleteKeySegments();

      if (lastSegment instanceof UriResourcePartTyped typed) {
        contextType = ParserHelper.getTypeInformation(typed);
        if (contextType != null && ((lastSegment instanceof UriResourceEntitySet lastEntitySet &&
            (lastEntitySet.getTypeFilterOnCollection() != null
                || lastEntitySet.getTypeFilterOnEntry() != null))
            || contextUriInfo.getIdOption() != null) && contextType instanceof EdmEntityType edmEntityType) {
          contextUriInfo.setEntityTypeCast(edmEntityType);
        }
        contextIsCollection = typed.isCollection();
      }
    }

    // Post-process system query options that need context information from the resource path.
    if (contextType instanceof EdmStructuredType structuredType && contextUriInfo.getApplyOption() != null) {
      // Data aggregation may change the structure of the result.
      contextType = new DynamicStructuredType(structuredType);
    }
    parseApplyOption(contextUriInfo.getApplyOption(), contextType,
        contextUriInfo.getEntitySetNames(), contextUriInfo.getAliasMap());
    parseFilterOption(contextUriInfo.getFilterOption(), contextType,
        contextUriInfo.getEntitySetNames(), contextUriInfo.getAliasMap());
    parseOrderByOption(contextUriInfo.getOrderByOption(), contextType,
        contextUriInfo.getEntitySetNames(), contextUriInfo.getAliasMap());
    parseExpandOption(contextUriInfo.getExpandOption(), contextType,
        contextUriInfo.getKind() == UriInfoKind.all, contextUriInfo.getEntitySetNames(),
        contextUriInfo.getAliasMap());
    parseSelectOption(contextUriInfo.getSelectOption(), contextType, contextIsCollection);

    return contextUriInfo;
  }

  private String getFormEncodedValue(String value) {
    if(value.contains("+")){
      value = value.replaceAll("\\+", " ");
    }
    return value;    
  }

  private QueryOption parseOption(final String optionName, final String optionValue)
      throws UriParserException, UriValidationException {
    if (optionName.startsWith(DOLLAR)) {
      final SystemQueryOptionKind kind = SystemQueryOptionKind.get(optionName);
      if (kind == null) {
        throw new UriParserSyntaxException("Unknown system query option!",
            UriParserSyntaxException.MessageKeys.UNKNOWN_SYSTEM_QUERY_OPTION, optionName);
      }
      SystemQueryOptionImpl systemOption;
      switch (kind) {
      case SEARCH:
        SearchOption searchOption = new SearchParser().parse(optionValue);
        SearchOptionImpl tmp = new SearchOptionImpl();
        tmp.setSearchExpression(searchOption.getSearchExpression());
        systemOption = tmp;
        break;
      case FILTER:
        systemOption = new FilterOptionImpl();
        break;
      case COUNT:
        if ("true".equals(optionValue) || "false".equals(optionValue)) {
          systemOption = new CountOptionImpl().setValue(Boolean.parseBoolean(optionValue));
        } else {
          throw new UriParserSyntaxException("Illegal value of $count option!",
              UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
              optionName, optionValue);
        }
        break;
      case ORDERBY:
        systemOption = new OrderByOptionImpl();
        break;
      case SKIP:
        systemOption = new SkipOptionImpl()
            .setValue(ParserHelper.parseNonNegativeInteger(optionName, optionValue, true));
        break;
      case SKIPTOKEN:
        if (optionValue.isEmpty()) {
          throw new UriParserSyntaxException("Illegal value of $skiptoken option!",
              UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
              optionName, optionValue);
        }
        systemOption = new SkipTokenOptionImpl().setValue(optionValue);
        break;
      case DELTATOKEN:
        if (optionValue.isEmpty()) {
          throw new UriParserSyntaxException("Illegal value of $deltatoken option!",
              UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
              optionName, optionValue);
        }
        systemOption = new DeltaTokenOptionImpl().setValue(optionValue);
        break;
      case TOP:
        systemOption = new TopOptionImpl()
            .setValue(ParserHelper.parseNonNegativeInteger(optionName, optionValue, true));
        break;
      case EXPAND:
        systemOption = new ExpandOptionImpl();
        break;
      case SELECT:
        systemOption = new SelectOptionImpl();
        break;
      case FORMAT:
        if (optionValue.equalsIgnoreCase(JSON)
            || optionValue.equalsIgnoreCase(XML)
            || optionValue.equalsIgnoreCase(ATOM)
            || isFormatSyntaxValid(optionValue)) {
          systemOption = new FormatOptionImpl().setFormat(optionValue);
        } else {
          throw new UriParserSyntaxException("Illegal value of $format option!",
              UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION_FORMAT, optionValue);
        }
        break;
      case ID:
        if (optionValue.isEmpty()) {
          throw new UriParserSyntaxException("Illegal value of $id option!",
              UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
              optionName, optionValue);
        }
        systemOption = new IdOptionImpl().setValue(optionValue);
        break;
      case SCHEMAVERSION:
        if (optionValue.isEmpty()) {
          throw new UriParserSyntaxException("Illegal value of $schemaversion option!",
              UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
              optionName, optionValue);
        }
        systemOption = new SchemaVersionOptionImpl().setValue(optionValue);
        break;
      case LEVELS:
        throw new UriParserSyntaxException("System query option '$levels' is allowed only inside '$expand'!",
            UriParserSyntaxException.MessageKeys.SYSTEM_QUERY_OPTION_LEVELS_NOT_ALLOWED_HERE);
      case APPLY:
        systemOption = new ApplyOptionImpl();
        break;
      default:
          throw new UriParserSyntaxException("System query option '" + kind + "' is not known!",
              UriParserSyntaxException.MessageKeys.UNKNOWN_SYSTEM_QUERY_OPTION, optionName);
      }
      systemOption.setText(optionValue);
      return systemOption;

    } else if (optionName.startsWith(AT)) {
      // Aliases can only be parsed in the context of their usage, so the value is not checked here.
      return new AliasQueryOptionImpl()
          .setName(optionName)
          .setText(NULL.equals(optionValue) ? null : optionValue);

    } else {
      // The option is a custom query option; the caller can re-use its query option.
      return null;
    }
  }

  private void parseFilterOption(FilterOption filterOption, final EdmType contextType,
      final List<String> entitySetNames, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    if (filterOption != null) {
      final String optionValue = filterOption.getText();
      UriTokenizer filterTokenizer = new UriTokenizer(optionValue);
      // The referring type could be a primitive type or a structured type.
      ((FilterOptionImpl) filterOption).setExpression(
          new FilterParser(edm, odata).parse(filterTokenizer, contextType, entitySetNames, aliases)
              .getExpression());
      checkOptionEOF(filterTokenizer, filterOption.getName(), optionValue);
    }
  }

  private void parseOrderByOption(OrderByOption orderByOption, final EdmType contextType,
      final List<String> entitySetNames, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    if (orderByOption != null) {
      final String optionValue = orderByOption.getText();
      UriTokenizer orderByTokenizer = new UriTokenizer(optionValue);
      final OrderByOption option = new OrderByParser(edm, odata).parse(orderByTokenizer,
          contextType instanceof EdmStructuredType structuredType ? structuredType : null,
          entitySetNames,
          aliases);
      checkOptionEOF(orderByTokenizer, orderByOption.getName(), optionValue);
      for (final OrderByItem item : option.getOrders()) {
        ((OrderByOptionImpl) orderByOption).addOrder(item);
      }
    }
  }

  private void parseExpandOption(ExpandOption expandOption, final EdmType contextType, final boolean isAll,
      final List<String> entitySetNames, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    if (expandOption != null) {
      if (!(contextType instanceof EdmStructuredType || isAll
      || (entitySetNames != null && !entitySetNames.isEmpty()))) {
        throw new UriValidationException("Expand is only allowed on structured types!",
            UriValidationException.MessageKeys.SYSTEM_QUERY_OPTION_NOT_ALLOWED, expandOption.getName());
      }
      final String optionValue = expandOption.getText();
      UriTokenizer expandTokenizer = new UriTokenizer(optionValue);
      final ExpandOption option = new ExpandParser(edm, odata, aliases, entitySetNames).parse(expandTokenizer,
          contextType instanceof EdmStructuredType structuredType ? structuredType : null);
      checkOptionEOF(expandTokenizer, expandOption.getName(), optionValue);
      for (final ExpandItem item : option.getExpandItems()) {
        ((ExpandOptionImpl) expandOption).addExpandItem(item);
      }
    }
  }

  private void parseSelectOption(SelectOption selectOption, final EdmType contextType,
      final boolean contextIsCollection) throws UriParserException, UriValidationException {
    if (selectOption != null) {
      final String optionValue = selectOption.getText();
      UriTokenizer selectTokenizer = new UriTokenizer(optionValue);
      ((SelectOptionImpl) selectOption).setSelectItems(
          new SelectParser(edm).parse(selectTokenizer,
              contextType instanceof EdmStructuredType structuredType ? structuredType : null,
              contextIsCollection)
              .getSelectItems());
      checkOptionEOF(selectTokenizer, selectOption.getName(), optionValue);
    }
  }

  private void parseApplyOption(ApplyOption applyOption, EdmType contextType,
      final List<String> entitySetNames, final Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    if (applyOption != null) {
      final String optionValue = applyOption.getText();
      UriTokenizer applyTokenizer = new UriTokenizer(optionValue);
      final ApplyOption option = new ApplyParser(edm, odata).parse(applyTokenizer,
          contextType instanceof EdmStructuredType structuredType ? structuredType : null,
          entitySetNames,
          aliases);
      checkOptionEOF(applyTokenizer, applyOption.getName(), optionValue);
      for (final ApplyItem item : option.getApplyItems()) {
        ((ApplyOptionImpl) applyOption).add(item);
      }
      ((ApplyOptionImpl) applyOption).setEdmStructuredType(option.getEdmStructuredType());
    }
  }

  private void ensureLastSegment(final String segment, final int pos, final int size)
      throws UriParserSyntaxException {
    if (pos < size) {
      throw new UriParserSyntaxException(segment + " must be the last segment.",
          UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT, segment);
    }
  }

  private boolean isFormatSyntaxValid(final String value) {
    final int index = value.indexOf('/');
    return index > 0 && index < value.length() - 1 && index == value.lastIndexOf('/');
  }

  private void checkOptionEOF(UriTokenizer tokenizer, final String optionName, final String optionValue)
      throws UriParserException {
    if (!tokenizer.next(TokenKind.EOF)) {
      throw new UriParserSyntaxException("Illegal value of '" + optionName + "' option!",
          UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION,
          optionName, optionValue);
    }
  }
}
