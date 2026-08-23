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
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 4: evaluate the $compute system query option
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options;

import java.util.List;
import java.util.Locale;

import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ComputeOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.apply.ComputeExpression;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.ExpressionVisitorImpl;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.TypedOperand;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.VisitorOperand;

/**
 * Evaluates the <code>$compute</code> system query option, attaching each result to the entity as a
 * dynamic property named by its alias.
 * <p>
 * [OData-Protocol] 11.2.5.3: computed properties "SHOULD be included as dynamic properties in the
 * result and MUST be included if $select is specified with the computed property name, or star (*)",
 * and they are usable from <code>$select</code>, <code>$filter</code> and <code>$orderby</code> --
 * so this runs before those options are applied.
 */
public final class ComputeHandler {

  private ComputeHandler() {
    // Utility class, mirroring the other handlers in this package.
  }

  /**
   * Computes the properties for every entity of the collection. Each entity is <em>replaced by a
   * copy</em> carrying them: the collection the processor passes here is a shallow copy whose
   * entities are the ones the data provider holds, so adding a property in place would leave the
   * computed value in the stored data for every later request.
   */
  public static void applyCompute(final ComputeOption computeOption,
      final EntityCollection entityCollection, final Edm edm) throws ODataApplicationException {
    if (computeOption == null || entityCollection == null) {
      return;
    }
    final List<Entity> entities = entityCollection.getEntities();
    for (int i = 0; i < entities.size(); i++) {
      final Entity copy = copyOf(entities.get(i));
      applyCompute(computeOption, copy, edm);
      entities.set(i, copy);
    }
  }

  /**
   * Computes the properties for one entity, which the caller must already own -- see the collection
   * overload for why.
   */
  public static void applyCompute(final ComputeOption computeOption, final Entity entity, final Edm edm)
      throws ODataApplicationException {
    if (computeOption == null || entity == null) {
      return;
    }
    for (final ComputeExpression computeExpression : computeOption.getExpressions()) {
      final VisitorOperand operand;
      try {
        operand = computeExpression.getExpression().accept(new ExpressionVisitorImpl(entity, null, edm));
      } catch (final ExpressionVisitException e) {
        throw new ODataApplicationException("Error computing '" + computeExpression.getAlias() + "'.",
            HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT, e);
      }
      final TypedOperand value = operand.asTypedOperand();
      // The parser rejects an alias that is already a declared property, so nothing is shadowed;
      // removing first keeps a repeated application idempotent.
      entity.getProperties().removeIf(property -> computeExpression.getAlias().equals(property.getName()));
      entity.addProperty(new Property(null, computeExpression.getAlias(),
          ValueType.PRIMITIVE, value.getValue()));
    }
  }

  /**
   * @return a copy of the entity carrying the same properties, links and control information;
   *         mirrors {@code ExpandSystemQueryOptionHandler#newEntity}. The property objects are
   *         shared, and are never mutated -- only added to.
   */
  private static Entity copyOf(final Entity entity) {
    final Entity copy = new Entity();
    copy.getProperties().addAll(entity.getProperties());
    copy.getAnnotations().addAll(entity.getAnnotations());
    copy.setId(entity.getId());
    copy.setBaseURI(entity.getBaseURI());
    copy.setType(entity.getType());
    copy.setETag(entity.getETag());
    copy.setMediaContentSource(entity.getMediaContentSource());
    copy.setMediaContentType(entity.getMediaContentType());
    copy.setMediaETag(entity.getMediaETag());
    copy.setSelfLink(entity.getSelfLink());
    copy.setEditLink(entity.getEditLink());
    copy.getAssociationLinks().addAll(entity.getAssociationLinks());
    copy.getNavigationBindings().addAll(entity.getNavigationBindings());
    copy.getOperations().addAll(entity.getOperations());
    copy.getNavigationLinks().addAll(entity.getNavigationLinks());
    return copy;
  }
}
