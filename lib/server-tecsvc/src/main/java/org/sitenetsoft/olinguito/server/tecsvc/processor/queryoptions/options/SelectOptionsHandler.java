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
      final EntityCollection entityCollection) throws ODataApplicationException {
    if (selectOption == null || entityCollection == null) {
      return;
    }
    for (final Entity entity : entityCollection.getEntities()) {
      applySelectOptions(selectOption, entity);
    }
  }

  public static void applySelectOptions(final SelectOption selectOption, final Entity entity)
      throws ODataApplicationException {
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
        replaceProperty(entity, property, applyToValue(item, property));
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
  private static Property applyToValue(final SelectItem item, final Property property)
      throws ODataApplicationException {
    List<Object> values = new ArrayList<>(property.asCollection());

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
    return result;
  }

  private static void replaceProperty(final Entity entity, final Property original,
      final Property replacement) {
    final int index = entity.getProperties().indexOf(original);
    if (index >= 0) {
      entity.getProperties().set(index, replacement);
    }
  }
}
