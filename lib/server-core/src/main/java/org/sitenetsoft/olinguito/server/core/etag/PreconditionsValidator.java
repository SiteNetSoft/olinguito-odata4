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
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 2 fix: preserve the resolved binding target for
 * dynamicProperty segments so ETag preconditions are enforced on dynamic-property writes/deletes
 * Copyright 2026 SiteNetSoft - Tier 7 Wave 2: keep the binding target across a $ref segment
 */
package org.sitenetsoft.olinguito.server.core.etag;

import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.server.api.etag.CustomETagSupport;
import org.sitenetsoft.olinguito.server.api.etag.PreconditionException;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceSingleton;

public class PreconditionsValidator {

  private final EdmBindingTarget affectedEntitySetOrSingleton;

  public PreconditionsValidator(final UriInfo uriInfo) throws PreconditionException {
    affectedEntitySetOrSingleton = extractInformation(uriInfo);
  }

  public boolean mustValidatePreconditions(final CustomETagSupport customETagSupport, final boolean isMediaValue) {
    return affectedEntitySetOrSingleton != null
        && (isMediaValue ?
            customETagSupport.hasMediaETag(affectedEntitySetOrSingleton) :
              customETagSupport.hasETag(affectedEntitySetOrSingleton));
  }

  private EdmBindingTarget extractInformation(final UriInfo uriInfo) throws PreconditionException {
    EdmBindingTarget lastFoundEntitySetOrSingleton = null;
    int counter = 0;
    for (UriResource uriResourcePart : uriInfo.getUriResourceParts()) {
      switch (uriResourcePart.getKind()) {
      case function:
        lastFoundEntitySetOrSingleton = getEntitySetFromFunctionImport((UriResourceFunction) uriResourcePart);
        break;
      case singleton:
        lastFoundEntitySetOrSingleton = ((UriResourceSingleton) uriResourcePart).getSingleton();
        break;
      case entitySet:
        lastFoundEntitySetOrSingleton = getEntitySet((UriResourceEntitySet) uriResourcePart);
        break;
      case navigationProperty:
        lastFoundEntitySetOrSingleton = getEntitySetFromNavigation(lastFoundEntitySetOrSingleton,
            (UriResourceNavigation) uriResourcePart);
        break;
      case primitiveProperty:
      case complexProperty:
      case dynamicProperty:
        break;
      case ref:
        // A $ref segment addresses the reference to the entity the preceding segment resolved, so
        // the affected entity set is unchanged and must not be lost: without this the reference
        // write paths could never validate preconditions at all ([OData-Protocol] 13.1.1 item 26).
        // The URI parser already enforces that $ref is the last segment.
        break;
      case value:
      case action:
        // This should not be possible since the URI Parser validates this but to be sure we throw an exception.
        if (counter != uriInfo.getUriResourceParts().size() - 1) {
          throw new PreconditionException("$value or Action must be the last segment in the URI.",
              PreconditionException.MessageKeys.INVALID_URI);
        }
        break;
      default:
        lastFoundEntitySetOrSingleton = null;
        break;
      }
      if (lastFoundEntitySetOrSingleton == null) {
        // Once we loose track of the entity set there is no way to retrieve it.
        break;
      }
      counter++;
    }
    return lastFoundEntitySetOrSingleton;
  }

  private EdmBindingTarget getEntitySetFromFunctionImport(final UriResourceFunction uriResourceFunction) {
    EdmFunctionImport functionImport = uriResourceFunction.getFunctionImport();
    if (functionImport != null && functionImport.getReturnedEntitySet() != null
        && !uriResourceFunction.isCollection()) {
      return functionImport.getReturnedEntitySet();
    }
    return null;
  }

  private EdmBindingTarget getEntitySet(final UriResourceEntitySet uriResourceEntitySet) {
    return uriResourceEntitySet.isCollection() ? null : uriResourceEntitySet.getEntitySet();
  }

  private EdmBindingTarget getEntitySetFromNavigation(final EdmBindingTarget lastFoundEntitySetOrSingleton,
      final UriResourceNavigation uriResourceNavigation) {
    if (lastFoundEntitySetOrSingleton != null && !uriResourceNavigation.isCollection()) {
      EdmNavigationProperty navProp = uriResourceNavigation.getProperty();
      return lastFoundEntitySetOrSingleton.getRelatedBindingTarget(navProp.getName());
    }
    return null;
  }
}
