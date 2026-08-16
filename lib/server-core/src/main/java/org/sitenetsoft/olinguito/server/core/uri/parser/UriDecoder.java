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
 * Copyright 2026 SiteNetSoft - Removed static mutable formEncoding state (thread-safety fix)
 * Copyright 2026 SiteNetSoft - OLINGO-1518: Parenthesis- and quote-aware splitter so function
 *   parameter values containing literal '/' (or '&' in options) are not broken up.
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2: exposed the paren-/quote-aware splitter so the batch
 *   part handler can inspect raw query options with the same tokenization the parser uses
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.core.Decoder;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.QueryOption;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.CustomQueryOptionImpl;

public class UriDecoder {

  static final String ACCEPT_FORM_ENCODING = "odata-accept-forms-encoding";

  /** Splits the path string at '/' characters and percent-decodes the resulting path segments. */
  protected static List<String> splitAndDecodePath(final String path) throws UriParserSyntaxException {
    List<String> pathSegmentsDecoded = new ArrayList<>();
    for (final String segment : split(path, '/')) {
      pathSegmentsDecoded.add(decode(segment));
    }
    return pathSegmentsDecoded;
  }

  /**
   * Splits the query-option string at '&' characters, the resulting parts at '=' characters,
   * and separately percent-decodes names and values of the resulting name-value pairs.
   * If there is no '=' character in an option, the whole option is considered as name.
   */
  protected static List<QueryOption> splitAndDecodeOptions(final String queryOptionString)
      throws UriParserSyntaxException {
    List<QueryOption> queryOptions = new ArrayList<>();
    for (final String option : split(queryOptionString, '&')) {
      final int pos = option.indexOf('=');
      final String name = pos >= 0 ? option.substring(0, pos)  : option;
      final String text = pos >= 0 ? option.substring(pos + 1) : "";
      queryOptions.add(new CustomQueryOptionImpl()
          .setName(decode(name).trim())
          .setText(decode(text).trim()));
    }
    return queryOptions;
  }

  /**
   * Splits the input string at the given character.
   * <p>
   * OLINGO-1518: The splitter skips occurrences of {@code c} that appear inside a
   * parenthesized group or inside a single-quoted string literal. This lets function
   * parameter values carry literal slashes (e.g. {@code Func(x='a/b')}) or ampersands
   * without being broken apart. OData-style escaped quotes ({@code ''}) are preserved.
   *
   * @param input string to split
   * @param c character at which to split
   * @return list of elements (can be empty)
   */
  public static List<String> split(final String input, final char c) {
    List<String> list = new LinkedList<>();

    int start = 0;
    int parenDepth = 0;
    boolean inQuote = false;
    final int len = input.length();
    for (int i = 0; i < len; i++) {
      final char ch = input.charAt(i);
      if (ch == '\'') {
        if (inQuote && i + 1 < len && input.charAt(i + 1) == '\'') {
          // Escaped single quote inside a string literal
          i++;
        } else {
          inQuote = !inQuote;
        }
      } else if (!inQuote) {
        if (ch == '(') {
          parenDepth++;
        } else if (ch == ')' && parenDepth > 0) {
          parenDepth--;
        } else if (ch == c && parenDepth == 0) {
          list.add(input.substring(start, i));
          start = i + 1;
        }
      }
    }

    list.add(input.substring(start));

    return list;
  }

  public static String decode(final String encoded) throws UriParserSyntaxException {
    try {
      return Decoder.decode(encoded);
    } catch (final IllegalArgumentException e) {
      throw new UriParserSyntaxException("Wrong percent encoding!", e, UriParserSyntaxException.MessageKeys.SYNTAX);
    }
  }
}
