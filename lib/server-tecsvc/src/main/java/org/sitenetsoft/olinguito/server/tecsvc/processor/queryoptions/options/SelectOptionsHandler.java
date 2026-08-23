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
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 3: options carried by a $select item
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options;

import java.util.ArrayList;
import java.util.List;

import java.util.Locale;
import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FilterOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.OrderByOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Expression;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.ExpressionVisitorImpl;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.TypedOperand;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceProperty;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectItem;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;

/**
 * Applies the query options a <code>$select</code> item carries to the value of the property it
 * names ([OData-Protocol] 11.2.5.1, conformance 13.2.2 item 9).
 * <p>
 * These options act on a property's collection <em>value</em>, which is why
 * {@code ExpandSystemQueryOptionHandler} cannot serve them: that one applies the same option names
 * to an expanded <em>entity</em> collection, a different thing entirely.
 */
public final class SelectOptionsHandler {

  private SelectOptionsHandler() {
    // Utility class, mirroring the other handlers in this package.
  }

  public static void applySelectOptions(final SelectOption selectOption,
      final EntityCollection entityCollection, final Edm edm) throws ODataApplicationException {
    if (selectOption == null || entityCollection == null) {
      return;
    }
    for (final Entity entity : entityCollection.getEntities()) {
      applySelectOptions(selectOption, entity, edm);
    }
  }

  public static void applySelectOptions(final SelectOption selectOption, final Entity entity,
      final Edm edm) throws ODataApplicationException {
    if (selectOption == null || entity == null) {
      return;
    }
    for (final SelectItem item : selectOption.getSelectItems()) {
      final String propertyName = selectedPropertyName(item);
      if (propertyName == null) {
        continue;
      }
      final Property property = entity.getProperty(propertyName);
      if (property != null && property.isCollection()) {
        replaceProperty(entity, property, applyToValue(item, property, edm));
      }
    }
  }

  /**
   * @return the name of the property a select item names, or <code>null</code> when the item names
   *         something else -- a star, an operation, or a path of more than one segment
   */
  private static String selectedPropertyName(final SelectItem item) {
    if (item.getResourcePath() == null) {
      return null;
    }
    final List<UriResource> parts = item.getResourcePath().getUriResourceParts();
    return parts.size() == 1 && parts.get(0) instanceof UriResourceProperty resourceProperty
        ? resourceProperty.getProperty().getName()
        : null;
  }

  /**
   * @return a new property holding the transformed value; the original is never mutated, because
   *         the entity being serialized shares its {@link Property} objects with the entity the
   *         data provider holds -- the copies made upstream are shallow
   */
  private static Property applyToValue(final SelectItem item, final Property property, final Edm edm)
      throws ODataApplicationException {
    List<Object> values = new ArrayList<>(property.asCollection());

    // [OData-Protocol] 11.2.5.1: the count reports the collection, so it is taken before $skip
    // and $top page it.
    final Integer count = item.getCountOption() != null && item.getCountOption().getValue()
        ? values.size()
        : null;

    if (item.getFilterOption() != null) {
      values = filter(values, item.getFilterOption(), edm);
    }
    if (item.getOrderByOption() != null) {
      values = order(values, item.getOrderByOption(), edm);
    }
    if (item.getSkipOption() != null) {
      final int skip = Math.min(item.getSkipOption().getValue(), values.size());
      values = new ArrayList<>(values.subList(skip, values.size()));
    }
    if (item.getTopOption() != null) {
      final int top = Math.min(item.getTopOption().getValue(), values.size());
      values = new ArrayList<>(values.subList(0, top));
    }

    final Property result = new Property(property.getType(), property.getName(),
        property.getValueType(), values);
    result.getAnnotations().addAll(property.getAnnotations());
    result.setCount(count);
    return result;
  }

  /**
   * Keeps the complex values for which the expression is true. A collection of primitive values
   * cannot be filtered here -- naming the item needs $it, which this service's expression visitor
   * does not implement -- and is refused in TechnicalProcessor.validateOptions before reaching this.
   */
  private static List<Object> filter(final List<Object> values, final FilterOption filterOption,
      final Edm edm) throws ODataApplicationException {
    final List<Object> result = new ArrayList<>();
    for (final Object value : values) {
      if (value instanceof ComplexValue complexValue
          && Boolean.TRUE.equals(evaluate(filterOption.getExpression(), complexValue, edm).getValue())) {
        result.add(value);
      }
    }
    return result;
  }

  /** Orders the complex values, mirroring the comparison {@code OrderByHandler} applies to entities. */
  private static List<Object> order(final List<Object> values, final OrderByOption orderByOption,
      final Edm edm) throws ODataApplicationException {
    final List<Object> result = new ArrayList<>(values);
    try {
      result.sort((first, second) -> compare(first, second, orderByOption, edm));
    } catch (final IllegalStateException e) {
      if (e.getCause() instanceof ODataApplicationException cause) {
        throw cause;
      }
      throw e;
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static int compare(final Object first, final Object second,
      final OrderByOption orderByOption, final Edm edm) {
    int result = 0;
    for (int i = 0; i < orderByOption.getOrders().size() && result == 0; i++) {
      final OrderByItem item = orderByOption.getOrders().get(i);
      try {
        final TypedOperand operand1 = evaluate(item.getExpression(), (ComplexValue) first, edm);
        final TypedOperand operand2 = evaluate(item.getExpression(), (ComplexValue) second, edm);

        if (operand1.isNull() || operand2.isNull()) {
          result = operand1.isNull() && operand2.isNull() ? 0 : operand1.isNull() ? -1 : 1;
        } else {
          final Object value1 = operand1.getValue();
          final Object value2 = operand2.getValue();
          result = value1.getClass() == value2.getClass() && value1 instanceof Comparable
              ? ((Comparable<Object>) value1).compareTo(value2)
              : 0;
        }
        result = item.isDescending() ? result * -1 : result;
      } catch (final ODataApplicationException e) {
        throw new IllegalStateException(e);
      }
    }
    return result;
  }

  private static TypedOperand evaluate(final Expression expression, final ComplexValue value, final Edm edm)
      throws ODataApplicationException {
    try {
      return expression.accept(new ExpressionVisitorImpl(value, null, edm)).asTypedOperand();
    } catch (final ExpressionVisitException e) {
      throw new ODataApplicationException("Error evaluating a select option.",
          HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT, e);
    }
  }

  private static void replaceProperty(final Entity entity, final Property original,
      final Property replacement) {
    final int index = entity.getProperties().indexOf(original);
    if (index >= 0) {
      entity.getProperties().set(index, replacement);
    }
  }
}
