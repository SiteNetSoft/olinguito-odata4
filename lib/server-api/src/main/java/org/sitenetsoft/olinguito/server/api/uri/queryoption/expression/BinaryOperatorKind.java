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
 * Copyright 2026 SiteNetSoft - Tier 7 Task 9: added the 4.01 divby operator
 */
package org.sitenetsoft.olinguito.server.api.uri.queryoption.expression;

/**
 * Enumeration of supported binary operators<br>
 * For the semantic of these operators please see the ODATA specification for URL conventions
 */
public enum BinaryOperatorKind {

  /**
   * OData has operator used for OData enumerations
   */
  HAS("has"),
  
  /**
   * In operator
   */
  IN("in"),

  /**
   * Multiplication operator
   */
  MUL("mul"),

  /**
   * Division operator
   */
  DIV("div"),
  /**
   * Division promoting both operands to decimal, per [OData-URL] 5.1.1.2.5. Unlike {@link #DIV}
   * it may yield a fractional result and does not fail for division by zero, returning
   * -INF, INF or NaN depending on the sign of the left operand.
   */
  DIVBY("divby"),

  /**
   * Modulo operator
   */
  MOD("mod"),

  /**
   * Addition operator
   */
  ADD("add"),

  /**
   * Subtraction operator
   */
  SUB("sub"),

  /**
   * Greater than operator (">")
   */
  GT("gt"),

  /**
   * Greater than or equals (">=") operator
   */
  GE("ge"),

  /**
   * Lesser than operator ("<")
   */
  LT("lt"),

  /**
   * Lesser operator or equals ("<=") operator
   */
  LE("le"),

  /**
   * Equality operator
   */
  EQ("eq"),

  /**
   * Inequality operator
   */
  NE("ne"),

  /**
   * And operator
   */
  AND("and"),

  /**
   * Or operator
   */
  OR("or");

  private String syntax;

  /**
   * Constructor for enumeration value
   * @param Syntax used in the URI
   */
  private BinaryOperatorKind(final String syntax) {
    this.syntax = syntax;
  }

  /**
   * URI syntax to enumeration value
   * @param operator Operator in the syntax used in the URI
   * @return Operator kind which represents the given syntax
   */
  public static BinaryOperatorKind get(final String operator) {
    for (BinaryOperatorKind op : BinaryOperatorKind.values()) {
      if (op.toString().equals(operator)) {
        return op;
      }
    }
    return null;
  }

  /**
   * @return URI syntax for that operator kind
   */
  @Override
  public String toString() {
    return syntax;
  }

}
