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
 * Copyright 2026 SiteNetSoft - OData 4.01: alternate keys (Core.AlternateKeys)
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.EdmAlternateKey;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAlternateKeyPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmTerm;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmRecord;

/**
 * Reads the {@code Core.AlternateKeys} annotation of an annotatable EDM element
 * (OData 4.01 URL Conventions 4.3.5). Malformed key groups are skipped instead of failing the model.
 */
final class AlternateKeysReader {

  private static final String ALTERNATE_KEYS_TERM = "Org.OData.Core.V1.AlternateKeys";
  private static final String ALTERNATE_KEYS_TERM_ALIAS = "Core.AlternateKeys";
  private static final String KEY_PROPERTY = "Key";
  private static final String NAME_PROPERTY = "Name";
  private static final String ALIAS_PROPERTY = "Alias";

  private AlternateKeysReader() {
    // static helper
  }

  /**
   * @param annotatable the annotated element (entity type or entity set)
   * @param entityType the entity type the property references are resolved against, may be null
   * @return the declared alternate keys, in declaration order; never null
   */
  static List<EdmAlternateKey> read(final EdmAnnotatable annotatable, final EdmEntityType entityType) {
    final List<EdmAlternateKey> result = new ArrayList<>();
    for (final EdmAnnotation annotation : annotatable.getAnnotations()) {
      if (!isAlternateKeysTerm(annotation)) {
        continue;
      }
      final EdmExpression expression = annotation.getExpression();
      if (expression == null || !expression.isDynamic() || !expression.asDynamic().isCollection()) {
        continue;
      }
      for (final EdmExpression item : expression.asDynamic().asCollection().getItems()) {
        final EdmAlternateKey alternateKey = readAlternateKey(item, entityType);
        if (alternateKey != null) {
          result.add(alternateKey);
        }
      }
    }
    return List.copyOf(result);
  }

  /**
   * Matches the raw term name first (OLINGO-1399: the vocabulary may not be served, in which case
   * the term cannot be resolved), then falls back to the resolved term's full qualified name.
   */
  private static boolean isAlternateKeysTerm(final EdmAnnotation annotation) {
    final String termName = annotation.getTermName();
    if (ALTERNATE_KEYS_TERM.equals(termName) || ALTERNATE_KEYS_TERM_ALIAS.equals(termName)) {
      return true;
    }
    try {
      final EdmTerm term = annotation.getTerm();
      return term != null
          && ALTERNATE_KEYS_TERM.equals(term.getFullQualifiedName().getFullQualifiedNameAsString());
    } catch (final EdmException e) {
      return false;
    }
  }

  private static EdmAlternateKey readAlternateKey(final EdmExpression item, final EdmEntityType entityType) {
    final EdmRecord record = asRecord(item);
    if (record == null) {
      return null;
    }
    final EdmExpression key = propertyValue(record, KEY_PROPERTY);
    if (key == null || !key.isDynamic() || !key.asDynamic().isCollection()) {
      return null;
    }
    final List<EdmAlternateKeyPropertyRef> refs = new ArrayList<>();
    for (final EdmExpression keyItem : key.asDynamic().asCollection().getItems()) {
      final EdmAlternateKeyPropertyRef ref = readPropertyRef(keyItem, entityType);
      if (ref == null) {
        return null;
      }
      refs.add(ref);
    }
    return refs.isEmpty() ? null : new EdmAlternateKeyImpl(refs);
  }

  private static EdmAlternateKeyPropertyRef readPropertyRef(final EdmExpression item,
      final EdmEntityType entityType) {
    final EdmRecord record = asRecord(item);
    if (record == null) {
      return null;
    }
    final String name = stringValue(propertyValue(record, NAME_PROPERTY));
    if (name == null || name.isEmpty()) {
      return null;
    }
    return new EdmAlternateKeyPropertyRefImpl(name, stringValue(propertyValue(record, ALIAS_PROPERTY)),
        resolveProperty(name, entityType));
  }

  private static EdmRecord asRecord(final EdmExpression expression) {
    return expression != null && expression.isDynamic() && expression.asDynamic().isRecord()
        ? expression.asDynamic().asRecord() : null;
  }

  private static EdmExpression propertyValue(final EdmRecord record, final String property) {
    if (record.getPropertyValues() == null) {
      return null;
    }
    for (final EdmPropertyValue propertyValue : record.getPropertyValues()) {
      if (property.equals(propertyValue.getProperty())) {
        return propertyValue.getValue();
      }
    }
    return null;
  }

  /**
   * Reads a path or string value; the vocabulary declares the name as {@code Edm.PropertyPath} but a
   * constant string is tolerated.
   */
  private static String stringValue(final EdmExpression expression) {
    if (expression == null) {
      return null;
    }
    if (expression.isConstant()) {
      return expression.asConstant().getValueAsString();
    }
    if (expression.isDynamic()) {
      if (expression.asDynamic().isPropertyPath()) {
        return expression.asDynamic().asPropertyPath().getValue();
      }
      if (expression.asDynamic().isPath()) {
        return expression.asDynamic().asPath().getValue();
      }
    }
    return null;
  }

  /**
   * Resolves a top-level primitive property; nested paths are out of scope and resolve to null.
   */
  private static EdmProperty resolveProperty(final String name, final EdmEntityType entityType) {
    if (entityType == null || name.indexOf('/') >= 0) {
      return null;
    }
    final EdmProperty property = entityType.getStructuralProperty(name);
    return property != null && !property.isCollection() && property.getType() instanceof EdmPrimitiveType
        ? property : null;
  }
}
