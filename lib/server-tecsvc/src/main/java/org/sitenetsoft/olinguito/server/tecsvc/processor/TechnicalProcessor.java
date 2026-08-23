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
 * Copyright 2026 SiteNetSoft - Modernized instanceof to pattern matching
 * Copyright 2026 SiteNetSoft - Select the first entity explicitly when no key predicates are given
 * Copyright 2026 SiteNetSoft - OData 4.01: referential-constraint key predicates from the source entity
 * Copyright 2026 SiteNetSoft - Restrict streamed collection serialization to JSON response formats
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 1: filter the addressed collection instead of substituting one
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 1: honor a derived-type cast on an operation result
 * Copyright 2026 SiteNetSoft - Tier 8 Wave 2: 501 for select options that are not evaluated yet
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor;

import java.util.List;
import java.util.Locale;

import org.sitenetsoft.olinguito.commons.api.data.DeletedEntity;
import org.sitenetsoft.olinguito.commons.api.data.DeltaLink;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Link;
import org.sitenetsoft.olinguito.commons.api.edm.EdmBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmSingleton;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.processor.Processor;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoResource;
import org.sitenetsoft.olinguito.server.api.uri.UriParameter;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectItem;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceAction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceSingleton;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Binary;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Member;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;

/**
 * Technical Processor base.
 */
public abstract class TechnicalProcessor implements Processor {

  protected final DataProvider dataProvider;
  protected OData odata;
  protected ServiceMetadata serviceMetadata;

  protected TechnicalProcessor(final DataProvider dataProvider) {
    this(dataProvider, null);
  }

  protected TechnicalProcessor(final DataProvider dataProvider, final ServiceMetadata serviceMetadata) {
    this.dataProvider = dataProvider;
    this.serviceMetadata = serviceMetadata;
  }

  @Override
  public void init(final OData odata, final ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
  }

  protected EdmEntitySet getEdmEntitySet(final UriInfoResource uriInfo) throws ODataApplicationException {
    EdmEntitySet entitySet = null;
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    EdmSingleton singleton = null;
    
    // First must be an entity, an entity collection, a function import, or an action import.
    if (resourcePaths.get(0) instanceof UriResourceEntitySet uriResourceEntitySet) {
      entitySet = getEntitySetBasedOnTypeCast(uriResourceEntitySet);
    } else if (resourcePaths.get(0) instanceof UriResourceFunction uriResourceFunc) {
      entitySet = uriResourceFunc.getFunctionImport().getReturnedEntitySet();
    } else if (resourcePaths.get(0) instanceof UriResourceAction uriResourceAction) {
      entitySet = uriResourceAction.getActionImport().getReturnedEntitySet();
    }else if (resourcePaths.get(0) instanceof UriResourceSingleton uriResourceSingleton) {
      singleton = uriResourceSingleton.getSingleton();
    } else {
      throw new ODataApplicationException("Invalid resource type.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    entitySet = (EdmEntitySet) getEntitySetForNavigation(entitySet, singleton, resourcePaths);

    return entitySet;
  }
  
  private EdmBindingTarget getEntitySetForNavigation(EdmEntitySet entitySet, EdmSingleton singleton,
      List<UriResource> resourcePaths) throws ODataApplicationException {
    int navigationCount = 0;
      while ((entitySet != null || singleton!=null)
          && ++navigationCount < resourcePaths.size()
          && resourcePaths.get(navigationCount) instanceof UriResourceNavigation uriResourceNavigation) {
        if (uriResourceNavigation.getProperty().containsTarget()) {
          return entitySet;
        }
        EdmBindingTarget target = null ;
        if(entitySet!=null){
          target = entitySet.getRelatedBindingTarget(uriResourceNavigation.getProperty().getName());
        }else if(singleton != null){
          target = singleton.getRelatedBindingTarget(uriResourceNavigation.getProperty().getName());
        }
        if (target instanceof EdmEntitySet edmEntitySet) {
          entitySet = edmEntitySet;
        }
      }
    return entitySet;
  }

  /**
   * Reads an entity as specified in the resource path, including navigation.
   * If there is navigation and the navigation ends on an entity collection,
   * returns the entity before the final navigation segment.
   */
  protected Entity readEntity(final UriInfoResource uriInfo) throws ODataApplicationException {
    return readEntity(uriInfo, false);
  }

  /**
   * If ignoreLastNavigation is set to false see {@link #readEntity(UriInfoResource)}
   * otherwise returns the second last entity (Ignores the last navigation) 
   * If no such entity exists throws an ODataApplicationException
   */
  protected Entity readEntity(final UriInfoResource uriInfo, final boolean ignoreLastNavigation)
      throws ODataApplicationException {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

    Entity entity = null;
    if (resourcePaths.get(0) instanceof UriResourceEntitySet uriResource) {
      EdmEntitySet entitySet = getEntitySetBasedOnTypeCast(uriResource);
      final List<UriParameter> keyPredicates = uriResource.getKeyPredicates();
      // A collection-bound operation or a $ref on the collection reaches here without key
      // predicates; this service then operates on the first entity of the collection.
      entity = keyPredicates.isEmpty()
          ? dataProvider.readFirst(entitySet)
          : dataProvider.read(entitySet, keyPredicates);
    }else if (resourcePaths.get(0) instanceof UriResourceSingleton uriResource) {
      entity = dataProvider.read( uriResource.getSingleton());
    } else if (resourcePaths.get(0) instanceof UriResourceFunction uriResource) {
      final EdmFunction function = uriResource.getFunction();
      if (function.getReturnType().getType() instanceof EdmEntityType returnEntityType) {
        final List<UriParameter> key = uriResource.getKeyPredicates();
        if (key.isEmpty()) {
          if (uriResource.isCollection()) { // handled in readEntityCollection()
            return null;
          } else {
            entity = dataProvider.readFunctionEntity(function, uriResource.getParameters(), uriInfo);
          }
        } else {
          entity = dataProvider.read(returnEntityType,
              dataProvider.readFunctionEntityCollection(function, uriResource.getParameters(), uriInfo),
              key);
        }
      } else {
        return null;
      }
    }
    if (entity == null) {
      throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
    }

    int readAtMostNavigations = resourcePaths.size();
    if (ignoreLastNavigation) {
      readAtMostNavigations = 0;
      for (int i = 1; i < resourcePaths.size(); i++) {
        if (resourcePaths.get(i) instanceof UriResourceNavigation) {
          readAtMostNavigations++;
        } else {
          break;
        }
      }
    }

    int navigationCount = 0;
	int navigationResCount = getNavigationResourceCount(resourcePaths);
    Link previous = null;
    while (++navigationCount < readAtMostNavigations
        && resourcePaths.get(navigationCount) instanceof UriResourceNavigation uriNavigationResource) {
      final EdmNavigationProperty navigationProperty = uriNavigationResource.getProperty();
      final List<UriParameter> key = uriNavigationResource.getKeyPredicates();
      if (navigationProperty.isCollection() && key.isEmpty()) { // handled in readEntityCollection()
        return entity;
      }
      // Key predicates that a referential constraint of this navigation property completes take
      // their value from the entity the navigation starts at, so it has to be kept across the step.
      final Entity sourceEntity = entity;
      final Link link = entity.getNavigationLink(navigationProperty.getName());
      entity = link == null ? null :
          key.isEmpty() ?
              link.getInlineEntity() :
              dataProvider.read(navigationProperty.getType(), link.getInlineEntitySet(), key, sourceEntity);
      EdmEntityType edmEntityType = getEntityTypeBasedOnNavPropertyTypeCast(uriNavigationResource);
      if (edmEntityType != null) {
        entity = key.isEmpty()
            ? dataProvider.readFirst(edmEntityType)
            : dataProvider.readDataFromEntity(edmEntityType, key, sourceEntity);
      }
      if (entity == null) {
        if (key.isEmpty() && (previous != null || navigationResCount == 1)) {
          throw new ODataApplicationException("No Content", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
        } else {
          throw new ODataApplicationException("Not Found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }
      }
	  previous = link;
    }

    return entity;
  }
  
  private int getNavigationResourceCount(List<UriResource> resourcePaths) {
    int count = 0;
    for (UriResource resource : resourcePaths) {
      if (resource instanceof UriResourceNavigation) {
        count ++;
      }
    }
    return count;
  }
  
  private EdmEntityType getEntityTypeBasedOnNavPropertyTypeCast(UriResourceNavigation uriNavigationResource) {
    if (uriNavigationResource.getTypeFilterOnCollection() != null) {
      return (EdmEntityType) uriNavigationResource.getTypeFilterOnCollection();
    } else if (uriNavigationResource.getTypeFilterOnEntry() != null) {
      return (EdmEntityType) uriNavigationResource.getTypeFilterOnEntry();
    }
    return null;
    
  }

  protected EdmEntitySet getEntitySetBasedOnTypeCast(UriResourceEntitySet uriResource) {
    EdmEntitySet entitySet = null;
    EdmEntityContainer container = this.serviceMetadata.getEdm().getEntityContainer();
    if (uriResource.getTypeFilterOnEntry() != null ||
        uriResource.getTypeFilterOnCollection() != null) {
      List<EdmEntitySet> entitySets = container.getEntitySets();
      for (EdmEntitySet entitySet1 : entitySets) {
        EdmEntityType entityType = entitySet1.getEntityType();
        if ((uriResource.getTypeFilterOnEntry() != null && 
            entityType.getName().equalsIgnoreCase(uriResource.getTypeFilterOnEntry().getName())) ||
            (uriResource.getTypeFilterOnCollection() != null && 
            entityType.getName().equalsIgnoreCase(uriResource.getTypeFilterOnCollection().getName()))) {
          entitySet = entitySet1;
          break;
        }
      }
    } else {
      entitySet = uriResource.getEntitySet();
    }
    return entitySet;
  }
  
  protected List<DeletedEntity> readDeletedEntities(final UriInfoResource uriInfo) throws ODataApplicationException {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    return dataProvider.readDeletedEntities(((UriResourceEntitySet) resourcePaths.get(0)).getEntitySet());
  }


  protected List<DeltaLink> readAddedLinks(final UriInfoResource uriInfo) throws ODataApplicationException {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    return dataProvider.readAddedLinks(((UriResourceEntitySet) resourcePaths.get(0)).getEntitySet());
  }
  
  protected List<DeltaLink> readDeletedLinks(final UriInfoResource uriInfo) throws ODataApplicationException {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    return dataProvider.readDeletedLinks(((UriResourceEntitySet) resourcePaths.get(0)).getEntitySet());
  }
  
  
  protected EntityCollection readEntityCollection(final UriInfoResource uriInfo) throws ODataApplicationException {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    if (resourcePaths.size() > 1 && resourcePaths.get(1) instanceof UriResourceNavigation) {
      final Entity entity = readEntity(uriInfo);
      final Link link = entity.getNavigationLink(getLastNavigation(uriInfo).getProperty().getName());
      return link == null ? null : link.getInlineEntitySet();
    } else {
      if (resourcePaths.get(0) instanceof UriResourceFunction uriResource) {
        final EntityCollection result = dataProvider.readFunctionEntityCollection(
            uriResource.getFunction(), uriResource.getParameters(), uriInfo);
        // A type filter on an operation result is part of [OData-Protocol] 13.1.2 item 4's "casting
        // to a derived type": it restricts the returned entities to those of the cast type. This
        // used to be rejected outright with 501.
        final EdmType typeFilter = uriResource.getTypeFilterOnCollection() != null
            ? uriResource.getTypeFilterOnCollection()
            : uriResource.getTypeFilterOnEntry();
        if (typeFilter != null && result != null) {
          result.getEntities().removeIf(candidate -> !isCompatible(candidate, typeFilter));
        }
        return result;
      } else {
        // The addressed collection is read as it stands. A cast inside the filter is evaluated
        // against each instance by ExpressionVisitorImpl ([OData-URL] 5.1.1.10), so the collection
        // must not be narrowed here: the previous special case recognised only a filter whose
        // top-level left operand carried the type filter -- answering 500 for every other shape --
        // and resolved that type to the first entity set declaring it, which returned entities from
        // an entity set the request never addressed.
        EdmEntitySet entitySet = getEntitySetBasedOnTypeCast(((UriResourceEntitySet)resourcePaths.get(0)));
        return dataProvider.readAll(entitySet);
      }
    }
  }

  /**
   * @return whether the entity's own type is the given type or derives from it
   */
  private boolean isCompatible(final Entity instance, final EdmType type) {
    if (instance == null || instance.getType() == null) {
      return false;
    }
    final EdmEntityType entityType = serviceMetadata.getEdm()
        .getEntityType(new FullQualifiedName(instance.getType()));
    return entityType != null && entityType.compatibleTo(type);
  }

  protected UriResourceNavigation getLastNavigation(final UriInfoResource uriInfo) {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    int navigationCount = 1;
    while (navigationCount < resourcePaths.size()
        && resourcePaths.get(navigationCount) instanceof UriResourceNavigation) {
      navigationCount++;
    }
    
    final UriResource lastSegment = resourcePaths.get(--navigationCount);
    return (lastSegment instanceof UriResourceNavigation uriResourceNav) ? uriResourceNav : null;
  }

  protected void validateOptions(final UriInfoResource uriInfo) throws ODataApplicationException {
    if (uriInfo.getApplyOption() != null) {
      throw new ODataApplicationException("Not all of the specified options are supported.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
    if (uriInfo.getSelectOption() != null) {
      for (final SelectItem item : uriInfo.getSelectOption().getSelectItems()) {
        // Parsed by SelectParser but not evaluated against the selected collection's value yet.
        // [OData-Protocol] 13.1.2 item 2 requires 501 for parsed-but-unsupported functionality;
        // accepting the option and ignoring it would be worse than refusing it. A nested $select
        // is deliberately absent from this check -- it is projected by ExpandSelectHelper.
        if (item.getFilterOption() != null || item.getSearchOption() != null
            || item.getOrderByOption() != null) {
          throw new ODataApplicationException("Query options on a selected collection-valued "
              + "property are not supported.",
              HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }
      }
    }
  }

  protected void blockBoundActions(final UriInfo uriInfo) throws ODataApplicationException {
    final List<UriResource> uriResourceParts = uriInfo.asUriInfoResource().getUriResourceParts();
    if (uriResourceParts.size() > 1
        && uriResourceParts.get(uriResourceParts.size() - 1) instanceof UriResourceAction) {
      throw new ODataApplicationException("Bound actions are not supported yet.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
  }

  protected void checkRequestFormat(final ContentType requestFormat) throws ODataApplicationException {
    if (requestFormat == null) {
      throw new ODataApplicationException("The content type has not been set in the request.",
          HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }
  }

  /**
   * Tells whether the given content type is a JSON one. Streamed serialization is only implemented
   * by the JSON serializer, so callers use this to keep every other format on the buffered path.
   */
  protected boolean isJsonContentType(final ContentType contentType) {
    return contentType != null && contentType.isCompatible(ContentType.APPLICATION_JSON);
  }

  protected boolean isODataMetadataNone(final ContentType contentType) {
    return contentType.isCompatible(ContentType.APPLICATION_JSON)
        && ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(
            contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA));
  }
}
