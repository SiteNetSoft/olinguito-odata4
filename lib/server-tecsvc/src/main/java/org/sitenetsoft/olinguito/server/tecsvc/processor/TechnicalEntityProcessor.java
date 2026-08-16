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
 * Copyright 2026 SiteNetSoft - Apply omit-values=nulls on entity/entity-collection reads
 * (OData 4.01, Protocol Section 8.2.8.6)
 * Copyright 2026 SiteNetSoft - Gate omit-values Preference-Applied on !isReference in
 * readEntityCollection, symmetric with readEntity
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL.Builder;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL.Suffix;
import org.sitenetsoft.olinguito.commons.api.data.DeletedEntity;
import org.sitenetsoft.olinguito.commons.api.data.Delta;
import org.sitenetsoft.olinguito.commons.api.data.DeltaLink;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.EntityIterator;
import org.sitenetsoft.olinguito.commons.api.data.EntityMediaObject;
import org.sitenetsoft.olinguito.commons.api.data.Operation;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.format.PreferenceName;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerResult;
import org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences.Return;
import org.sitenetsoft.olinguito.server.api.prefer.PreferencesApplied;
import org.sitenetsoft.olinguito.server.api.processor.CountEntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.EntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.EntityProcessor;
import org.sitenetsoft.olinguito.server.api.processor.MediaEntityProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ReferenceCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ReferenceProcessor;
import org.sitenetsoft.olinguito.server.api.serializer.EntityCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.EntitySerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ReferenceCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ReferenceSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerResult;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePartTyped;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceSingleton;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CountOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.IdOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOption;
import org.sitenetsoft.olinguito.server.tecsvc.async.AsyncProcessor;
import org.sitenetsoft.olinguito.server.tecsvc.async.TechnicalAsyncService;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.tecsvc.data.RequestValidator;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.ExpandSystemQueryOptionHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.CountHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.DeltaTokenHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.FilterHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.OrderByHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.SearchHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.ServerSidePagingHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.SkipHandler;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.options.TopHandler;
import org.sitenetsoft.olinguito.server.tecsvc.provider.ContainerProvider;

/**
 * Technical Processor for entity-related functionality.
 */
public class TechnicalEntityProcessor extends TechnicalProcessor
    implements EntityCollectionProcessor, CountEntityCollectionProcessor, EntityProcessor, MediaEntityProcessor,
    ReferenceCollectionProcessor, ReferenceProcessor {

  private static final String DELTATOKEN = "deltatoken";
  
  public TechnicalEntityProcessor(final DataProvider dataProvider, final ServiceMetadata serviceMetadata) {
    super(dataProvider, serviceMetadata);
  }

  @Override
  public void readEntityCollection(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestedContentType) throws ODataApplicationException, ODataLibraryException {
    validateOptions(uriInfo.asUriInfoResource());

    readEntityCollection(request, response, uriInfo, requestedContentType, false);
  }

  @Override
  public void countEntityCollection(final ODataRequest request, final ODataResponse response,
      final UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
    validateOptions(uriInfo.asUriInfoResource());
    getEdmEntitySet(uriInfo); // including checks
    final EntityCollection entitySetInitial = readEntityCollection(uriInfo);
    EntityCollection entitySet = new EntityCollection();
    entitySet.getEntities().addAll(entitySetInitial.getEntities());
    FilterHandler.applyFilterSystemQuery(uriInfo.getFilterOption(), entitySet, uriInfo, serviceMetadata.getEdm());
    SearchHandler.applySearchSystemQueryOption(uriInfo.getSearchOption(), entitySet);
    int count =  entitySet.getEntities().size();
    for (SystemQueryOption systemQueryOption : uriInfo.getSystemQueryOptions()) {
      if (systemQueryOption.getName().contains(DELTATOKEN)) {
        count = count + getDeltaCount(uriInfo);
        break;
      }
    }
    response.setContent(odata.createFixedFormatSerializer().count(count));
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
  }

  private int getDeltaCount(UriInfo uriInfo) throws ODataApplicationException {
    List<DeletedEntity> deletedEntity = readDeletedEntities(uriInfo);
    List<DeltaLink> addedLink = readAddedLinks(uriInfo);
    List<DeltaLink> deletedLink = readDeletedLinks(uriInfo);   
    List<Entity> listofNavigationEntities = readNavigationEntities(uriInfo);
    return deletedEntity.size() + addedLink.size() + deletedLink.size() + listofNavigationEntities.size();
  }
  
  @Override
  public void readEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestedContentType) throws ODataApplicationException, ODataLibraryException {
    validateOptions(uriInfo.asUriInfoResource());

    readEntity(request, response, uriInfo, requestedContentType, false);
  }

  @Override
  public void readMediaEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    getEdmEntitySet(uriInfo); // including checks
    final Entity entity = readEntity(uriInfo);
    final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());
    if (isMediaStreaming(edmEntitySet)) {
		EntityMediaObject mediaEntity = new EntityMediaObject();
		mediaEntity.setBytes(dataProvider.readMedia(entity));
	    response.setODataContent(odata.createFixedFormatSerializer()
	    		.mediaEntityStreamed(mediaEntity).getODataContent());
    } else {
    	response.setContent(odata.createFixedFormatSerializer().binary(dataProvider.readMedia(entity)));
    }
    response.setContent(odata.createFixedFormatSerializer().binary(dataProvider.readMedia(entity)));
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, entity.getMediaContentType());
    if (entity.getMediaETag() != null) {
      response.setHeader(HttpHeader.ETAG, entity.getMediaETag());
    }
  }

  @Override
  public void createMediaEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    createEntity(request, response, uriInfo, requestFormat, responseFormat);
  }

  @Override
  public void createEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    final boolean isContNav = checkIfContNavigation(uriInfo);
    if (uriInfo.asUriInfoResource().getUriResourceParts().size() > 1 && !isContNav ||
        isContNav && uriInfo.asUriInfoResource().getUriResourceParts().size() > 2) {
      throw new ODataApplicationException("Invalid resource type.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
    checkRequestFormat(requestFormat);

    if (odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).hasRespondAsync()) {
      TechnicalAsyncService asyncService = TechnicalAsyncService.getInstance();
      TechnicalEntityProcessor processor = new TechnicalEntityProcessor(dataProvider, serviceMetadata);
      processor.init(odata, serviceMetadata);
      AsyncProcessor<EntityProcessor> asyncProcessor = asyncService.register(processor, EntityProcessor.class);
      asyncProcessor.prepareFor().createEntity(request, response, uriInfo, requestFormat, responseFormat);
      String location = asyncProcessor.processAsync();
      TechnicalAsyncService.acceptedResponse(response, location);
      return;
    }

    final UriResourceEntitySet resourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
    final EdmEntitySet edmEntitySet = resourceEntitySet.getEntitySet();
    EdmEntityType edmEntityType = null;
    edmEntityType = getEdmTypeForContNavProperty(uriInfo);
    if (edmEntityType == null) {
      edmEntityType = edmEntitySet.getEntityType();
    }

    final Entity entity;
    ExpandOption expand = null;
    if (edmEntityType.hasStream()) { // called from createMediaEntity(...), not directly
      entity = dataProvider.create(edmEntitySet);
      dataProvider.setMedia(entity, odata.createFixedFormatDeserializer().binary(request.getBody()),
          requestFormat.toContentTypeString());
    } else {
      final DeserializerResult deserializerResult =
          odata.createDeserializer(requestFormat,serviceMetadata).entity(request.getBody(), edmEntityType);
      new RequestValidator(dataProvider, request.getRawBaseUri())
          .validate(edmEntitySet, deserializerResult.getEntity());

      if (isContNav) {
        entity = dataProvider.createContNav(edmEntitySet, edmEntityType, deserializerResult.getEntity(), 
            ((UriResourceEntitySet)uriInfo.getUriResourceParts().get(0)).getKeyPredicates(), 
            ((UriResourceNavigation)uriInfo.getUriResourceParts().get(1)).getSegmentValue());
      } else {
        entity = dataProvider.create(edmEntitySet);
        dataProvider.update(request.getRawBaseUri(), edmEntitySet, entity, deserializerResult.getEntity(), false, true);
      }
      expand = deserializerResult.getExpandTree();
    }

    final String location = request.getRawBaseUri() + '/'
        + odata.createUriHelper().buildCanonicalURL(edmEntitySet, entity);
    final Return returnPreference = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getReturn();
    if (returnPreference == null || returnPreference == Return.REPRESENTATION) {
      response.setContent(serializeEntity(request, entity, edmEntitySet, edmEntityType, responseFormat, 
          expand, null, isContNav).getContent());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
      response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
    } else {
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      response.setHeader(HttpHeader.ODATA_ENTITY_ID, location);
    }
    if (returnPreference != null) {
      response.setHeader(HttpHeader.PREFERENCE_APPLIED,
          PreferencesApplied.with().returnRepresentation(returnPreference).build().toValueString());
    }
    response.setHeader(HttpHeader.LOCATION, location);
    if (entity.getETag() != null) {
      response.setHeader(HttpHeader.ETAG, entity.getETag());
    }
  }

  @Override
  public void updateEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
    final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

    Entity entity;
    try {
      entity = readEntity(uriInfo);
    } catch (ODataApplicationException e) {
      if (e.getStatusCode() == HttpStatusCode.NOT_FOUND.getStatusCode()) {
        // Perform upsert
        createEntity(request, response, uriInfo, requestFormat, responseFormat);
        return;
      } else {
        throw e;
      }
    }

    odata.createETagHelper().checkChangePreconditions(entity.getETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));
    final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
    final Entity changedEntity = deserializer.entity(request.getBody(), edmEntitySet.getEntityType()).getEntity();

    new RequestValidator(dataProvider,
        true, // Update
        request.getMethod() == HttpMethod.PATCH,
        request.getRawBaseUri()).validate(edmEntitySet, changedEntity);

    dataProvider.update(request.getRawBaseUri(), edmEntitySet, entity, changedEntity,
        request.getMethod() == HttpMethod.PATCH, false);

    final Return returnPreference = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getReturn();
    if (returnPreference == null || returnPreference == Return.REPRESENTATION) {
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setContent(serializeEntity(request, entity, edmEntitySet, edmEntityType, responseFormat)
          .getContent());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    } else {
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
    if (returnPreference != null) {
      response.setHeader(HttpHeader.PREFERENCE_APPLIED,
          PreferencesApplied.with().returnRepresentation(returnPreference).build().toValueString());
    }
    if (entity.getETag() != null) {
      response.setHeader(HttpHeader.ETAG, entity.getETag());
    }
  }

  @Override
  public void updateMediaEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
    final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

    Entity entity = readEntity(uriInfo);
    odata.createETagHelper().checkChangePreconditions(entity.getMediaETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));
    checkRequestFormat(requestFormat);
    dataProvider.setMedia(entity, odata.createFixedFormatDeserializer().binary(request.getBody()),
        requestFormat.toContentTypeString());

    final Return returnPreference = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getReturn();
    if (returnPreference == null || returnPreference == Return.REPRESENTATION) {
      response.setContent(serializeEntity(request, entity, edmEntitySet, edmEntityType, responseFormat)
          .getContent());
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    } else {
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
    if (returnPreference != null) {
      response.setHeader(HttpHeader.PREFERENCE_APPLIED,
          PreferencesApplied.with().returnRepresentation(returnPreference).build().toValueString());
    }
    if (entity.getETag() != null) {
      response.setHeader(HttpHeader.ETAG, entity.getETag());
    }
  }

  @Override
  public void deleteEntity(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataLibraryException, ODataApplicationException {
    final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
    final Entity entity = readEntity(uriInfo);

    odata.createETagHelper().checkChangePreconditions(entity.getETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));
    dataProvider.delete(edmEntitySet, entity);
    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Override
  public void deleteMediaEntity(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataLibraryException, ODataApplicationException {
    final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
    final Entity entity = readEntity(uriInfo);

    odata.createETagHelper().checkChangePreconditions(entity.getMediaETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));
    dataProvider.delete(edmEntitySet, entity);
    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Override
  public void readReference(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType requestedContentType) throws ODataApplicationException, ODataLibraryException {
    readEntity(request, response, uriInfo, requestedContentType, true);
  }

  @Override
  public void createReference(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat) throws ODataApplicationException, ODataLibraryException {

    final ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
    final DeserializerResult references = deserializer.entityReferences(request.getBody());

    if (references.getEntityReferences().size() != 1) {
      throw new ODataApplicationException("A post request to a collection navigation property must "
          + "contain a single entity reference", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }

    final Entity entity = readEntity(uriInfo, true);
    final UriResourceNavigation navigationProperty = getLastNavigation(uriInfo);
    ensureNavigationPropertyNotNull(navigationProperty);
    dataProvider.createReference(entity, navigationProperty.getProperty(), references.getEntityReferences().get(0),
        request.getRawBaseUri());

    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Override
  public void updateReference(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat) throws ODataApplicationException, ODataLibraryException {

    final ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
    final DeserializerResult references = deserializer.entityReferences(request.getBody());

    if (references.getEntityReferences().size() != 1) {
      throw new ODataApplicationException("A post request to a collection navigation property must "
          + "contain a single entity reference", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }

    final Entity entity = readEntity(uriInfo, true);
    final UriResourceNavigation navigationProperty = getLastNavigation(uriInfo);
    ensureNavigationPropertyNotNull(navigationProperty);
    dataProvider.createReference(entity, navigationProperty.getProperty(), references.getEntityReferences().get(0),
        request.getRawBaseUri());

    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Override
  public void deleteReference(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException {

    final UriResourceNavigation lastNavigation = getLastNavigation(uriInfo);
    final IdOption idOption = uriInfo.getIdOption();

    ensureNavigationPropertyNotNull(lastNavigation);
    if (lastNavigation.isCollection() && idOption == null) {
      throw new ODataApplicationException("Id system query option must be provided",
          HttpStatusCode.BAD_REQUEST.getStatusCode(),
          Locale.ROOT);
    } else if (!lastNavigation.isCollection() && idOption != null) {
      throw new ODataApplicationException("Id system query option must not be provided",
          HttpStatusCode.BAD_REQUEST.getStatusCode(),
          Locale.ROOT);
    }

    final Entity entity = readEntity(uriInfo, true);
    dataProvider.deleteReference(entity,
        lastNavigation.getProperty(),
        (uriInfo.getIdOption() != null) ? uriInfo.getIdOption().getValue() : null,
        request.getRawBaseUri());

    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Override
  public void readReferenceCollection(final ODataRequest request, final ODataResponse response,
      final UriInfo uriInfo, final ContentType requestedContentType)
      throws ODataApplicationException, ODataLibraryException {
    readEntityCollection(request, response, uriInfo, requestedContentType, true);
  }

  private void readEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType requestedFormat, final boolean isReference)
      throws ODataApplicationException, ODataLibraryException {
    //
    if (odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).hasRespondAsync()) {
      TechnicalAsyncService asyncService = TechnicalAsyncService.getInstance();
      TechnicalEntityProcessor processor = new TechnicalEntityProcessor(dataProvider, serviceMetadata);
      processor.init(odata, serviceMetadata);
      AsyncProcessor<EntityProcessor> asyncProcessor = asyncService.register(processor, EntityProcessor.class);
      asyncProcessor.prepareFor().readEntity(request, response, uriInfo, requestedFormat);
      String location = asyncProcessor.processAsync();
      TechnicalAsyncService.acceptedResponse(response, location);
      //
      return;
    }
    //

    final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
    
    //for Singleton/$ref edmEntityset will be null throw error
    validateSingletonRef(isReference,edmEntitySet);
   
    EdmEntityType edmEntityType = null;
    edmEntityType = getEdmTypeForContNavProperty(uriInfo);
    final boolean iscontNav = checkIfContNavigation(uriInfo);
    if (edmEntityType == null) {
      edmEntityType = getEdmType(uriInfo, edmEntitySet);
    }

    final Entity entity = readEntity(uriInfo);

    if (odata.createETagHelper().checkReadPreconditions(entity.getETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH))) {
      response.setStatusCode(HttpStatusCode.NOT_MODIFIED.getStatusCode());
      response.setHeader(HttpHeader.ETAG, entity.getETag());
      return;
    }

    final ExpandOption expand = uriInfo.getExpandOption();
    final SelectOption select = uriInfo.getSelectOption();

    final ExpandSystemQueryOptionHandler expandHandler = new ExpandSystemQueryOptionHandler();
    final Entity entitySerialization = expandHandler.transformEntityGraphToTree(entity, edmEntitySet, expand, null);
    expandHandler.applyExpandQueryOptions(entitySerialization, edmEntitySet, expand, uriInfo,
        serviceMetadata.getEdm());

    // omit-values=nulls is applied on reads only; write responses (create/update) never omit
    // properties and never echo this preference in Preference-Applied.
    final boolean omitNulls = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getOmitValues()
        == Preferences.OmitValues.NULLS;

    final SerializerResult serializerResult = isReference ?
        serializeReference(entity, edmEntitySet, requestedFormat) :
        serializeEntity(request, entitySerialization, edmEntitySet, edmEntityType,
            requestedFormat, expand, select, iscontNav, omitNulls);

    if (entity.getETag() != null) {
      response.setHeader(HttpHeader.ETAG, entity.getETag());
    }
    response.setContent(serializerResult.getContent());
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, requestedFormat.toContentTypeString());
    if (omitNulls && !isReference) {
      response.setHeader(HttpHeader.PREFERENCE_APPLIED,
          PreferencesApplied.with().preference(PreferenceName.OMIT_VALUES.getName(), "nulls")
              .build().toValueString());
    }
  }
  
  private boolean checkIfContNavigation(UriInfo uriInfo) {
    List<UriResource> pathSegments = uriInfo.getUriResourceParts();
    for(UriResource resource : pathSegments) {
      if (resource instanceof UriResourceNavigation navResource) {
        if (navResource.getProperty().containsTarget()) {
          return true;
        }
      }
    }
    return false;
  }

  private EdmEntityType getEdmTypeForContNavProperty(UriInfo uriInfo) {
    List<UriResource> pathSegments = uriInfo.getUriResourceParts();
    EdmEntityType type = null;
    for(UriResource resource : pathSegments) {
      if (resource instanceof UriResourceNavigation navResource) {
        if (navResource.getProperty().containsTarget()) {
          if (navResource.getTypeFilterOnCollection() != null) {
            type = ((EdmEntityType) navResource.getTypeFilterOnCollection());
          } else if (navResource.getTypeFilterOnEntry() != null) {
            type = ((EdmEntityType) navResource.getTypeFilterOnEntry());
          } else {
            type = ((EdmEntityType) navResource.getType());
          }
        }
      }
    }
    return type;
  }

  /*This method validates if the $ref is called directly on Singleton
   * Error is thrown when $ref is called on a Singleton as it is not implemented*/
  private void validateSingletonRef(boolean isReference, EdmEntitySet edmEntitySet) throws
  ODataApplicationException {
   if(isReference && edmEntitySet==null){
         throw new ODataApplicationException("$ref not implemented on singleton",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        
      }
  
  }
  
  /*This method returns edmType of the entityset or Singleton*/  
  private EdmEntityType getEdmType(UriInfo uriInfo, EdmEntitySet edmEntitySet) {
    if(edmEntitySet!=null){
      return edmEntitySet.getEntityType();
    }else if(uriInfo.getUriResourceParts()
              .get(uriInfo.getUriResourceParts().size() - 1) instanceof UriResourcePartTyped partTyped){
      return  (EdmEntityType) partTyped.getType();
    }else if(uriInfo.getUriResourceParts()
              .get(0) instanceof UriResourceSingleton singleton){
      return (EdmEntityType) singleton.getType();
    }

  return null;
  }

  private void readEntityCollection(final ODataRequest request, final ODataResponse response,
      final UriInfo uriInfo, final ContentType requestedContentType, final boolean isReference)
      throws ODataApplicationException, ODataLibraryException {
    //
    if (odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).hasRespondAsync()) {
      TechnicalAsyncService asyncService = TechnicalAsyncService.getInstance();
      TechnicalEntityProcessor processor = new TechnicalEntityProcessor(dataProvider, serviceMetadata);
      processor.init(odata, serviceMetadata);
      AsyncProcessor<EntityCollectionProcessor> asyncProcessor =
          asyncService.register(processor, EntityCollectionProcessor.class);
      asyncProcessor.prepareFor().readEntityCollection(request, response, uriInfo, requestedContentType);
      String location = asyncProcessor.processAsync();
      TechnicalAsyncService.acceptedResponse(response, location);
      //
      return;
    }
    //

    // Shared Preferences view of the Prefer request header: reused below for maxpagesize,
    // track-changes, and omit-values so that a Prefer header combining several of them results
    // in a single, accumulated Preference-Applied response header.
    final Preferences preferences = odata.createPreferences(request.getHeaders(HttpHeader.PREFER));
    final boolean omitNulls = preferences.getOmitValues() == Preferences.OmitValues.NULLS;

    final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());
    final boolean isContNav = checkIfContNavigation(uriInfo);
    EdmEntityType edmEntityType = null;
    edmEntityType = getEdmTypeForContNavProperty(uriInfo);
    if (edmEntityType == null) {
      edmEntityType = edmEntitySet == null ?
          (EdmEntityType) ((UriResourcePartTyped) uriInfo.getUriResourceParts()
              .get(uriInfo.getUriResourceParts().size() - 1)).getType() :
          edmEntitySet.getEntityType();
    }

    EntityCollection entitySetInitial = readEntityCollection(uriInfo);
    Delta delta = null;
    if (entitySetInitial == null) {
      entitySetInitial = new EntityCollection();
    }

    // Modifying the original entitySet means modifying the "database", so we have to make a shallow
    // copy of the entity set (new EntitySet, but exactly the same data).
    EntityCollection entitySet = new EntityCollection();
    entitySet.getEntities().addAll(entitySetInitial.getEntities());
    entitySet.getOperations().addAll(entitySetInitial.getOperations());

    // Apply system query options.
    SearchHandler.applySearchSystemQueryOption(uriInfo.getSearchOption(), entitySet);
    FilterHandler.applyFilterSystemQuery(uriInfo.getFilterOption(), entitySet, uriInfo, serviceMetadata.getEdm());
    CountHandler.applyCountSystemQueryOption(uriInfo.getCountOption(), entitySet);
    OrderByHandler.applyOrderByOption(uriInfo.getOrderByOption(), entitySet, uriInfo, serviceMetadata.getEdm());
    SkipHandler.applySkipSystemQueryHandler(uriInfo.getSkipOption(), entitySet);
    TopHandler.applyTopSystemQueryOption(uriInfo.getTopOption(), entitySet);

    final Integer pageSize = preferences.getMaxPageSize();
    final Integer serverPageSize = ServerSidePagingHandler.applyServerSidePaging(uriInfo.getSkipTokenOption(),
        entitySet,
        edmEntitySet,
        request.getRawRequestUri(),
        pageSize);

    // Apply expand system query option
    final ExpandOption expand = uriInfo.getExpandOption();
    final SelectOption select = uriInfo.getSelectOption();

    // Transform the entity graph to a tree. The construction is controlled by the expand tree.
    // Apply all expand system query options to the tree.
    // So the expanded navigation properties can be modified for serialization,
    // without affecting the data stored in the database.
    final ExpandSystemQueryOptionHandler expandHandler = new ExpandSystemQueryOptionHandler();
    final EntityCollection entitySetSerialization = expandHandler.transformEntitySetGraphToTree(entitySet,
        edmEntitySet,
        expand, null);
    expandHandler.applyExpandQueryOptions(entitySetSerialization, edmEntitySet, expand, uriInfo,
        serviceMetadata.getEdm());
    final CountOption countOption = uriInfo.getCountOption();
    final List<SystemQueryOption> systemQueryOptions = uriInfo.getSystemQueryOptions();
    String deltaToken = null;
    for (SystemQueryOption systemQueryOption : systemQueryOptions) {
      if (systemQueryOption.getName().contains(DELTATOKEN)) {
        deltaToken = systemQueryOption.getText();
        delta = new Delta();
        Integer count = 0;
        if (deltaToken != null) {
          String deltaTokenValue = generateDeltaToken();
          List<DeletedEntity> listOfDeletedEntities = readDeletedEntities(uriInfo);
          List<DeltaLink> listOfAddedLinks = readAddedLinks(uriInfo);
          List<DeltaLink> listOfDeletedLinks = readDeletedLinks(uriInfo);
          List<Entity> listofNavigationEntities = readNavigationEntities(uriInfo);
          delta.getDeletedEntities().addAll(listOfDeletedEntities);
          delta.getAddedLinks().addAll(listOfAddedLinks);
          delta.getDeletedLinks().addAll(listOfDeletedLinks);
          delta.getEntities().addAll(listofNavigationEntities);
          count = listOfDeletedLinks.size()+listOfAddedLinks.size()+listOfDeletedEntities.size();
          delta.setDeltaLink(DeltaTokenHandler.createDeltaLink(
          request.getRawRequestUri(),
          deltaTokenValue));
        }
        
        delta.getEntities().addAll(entitySetSerialization.getEntities()); 
        count = count +  delta.getEntities().size();
        delta.setCount(count);
        break;
      }
    } 
    String id;
    if (edmEntitySet == null) {
      // Used for functions, function imports etc.
      id = request.getRawODataPath();
    } else {
      id = request.getRawBaseUri() + edmEntitySet.getName();
    }
    if(preferences.hasTrackChanges()) {
      String deltaTokenValue = generateDeltaToken();
      entitySetSerialization.setDeltaLink(DeltaTokenHandler.createDeltaLink(
          request.getRawRequestUri(),
          deltaTokenValue));
    }
    if(isReference) {
      final SerializerResult serializerResult =
          serializeReferenceCollection(entitySetSerialization, edmEntitySet, requestedContentType, countOption);
      response.setContent(serializerResult.getContent());
    } else if(isStreaming(edmEntitySet, requestedContentType)) {
      final SerializerStreamResult serializerResult =
          serializeEntityCollectionStreamed(request,
              entitySetSerialization, edmEntitySet, edmEntityType, requestedContentType,
              expand, select, countOption, id, omitNulls);

      response.setODataContent(serializerResult.getODataContent());
    } else if(delta != null){
      // serializeDeltaPayloads intentionally never sees omitNulls: [OData-Protocol] Section 8.2.8.6
      // requires modified null properties to appear in delta payloads regardless of this preference.
      final SerializerResult serializerResult =
          serializeDeltaPayloads(request,
          delta, edmEntitySet, edmEntityType, requestedContentType,
          expand, select, countOption, id);
      response.setContent(serializerResult.getContent());
    } else {
      final SerializerResult serializerResult =
          serializeEntityCollection(request,
              entitySetSerialization, edmEntitySet, edmEntityType, requestedContentType,
              expand, select, countOption, id, isContNav, omitNulls);
      response.setContent(serializerResult.getContent());
    }

    //
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
    final PreferencesApplied.Builder preferencesAppliedBuilder = PreferencesApplied.with();
    boolean anyPreferenceApplied = false;
    if (pageSize != null) {
      preferencesAppliedBuilder.maxPageSize(serverPageSize);
      anyPreferenceApplied = true;
    }
    if (preferences.hasTrackChanges()) {
      preferencesAppliedBuilder.trackChanges();
      anyPreferenceApplied = true;
    }
    if (omitNulls && delta == null && !isReference) {
      // Delta payloads never omit nulls (see above), so a delta response must not claim to have
      // applied omit-values either. Reference collections ($ref) never carry properties to omit
      // either (serializeReferenceCollection only ever writes @odata.id), so an omit-values
      // request against a $ref collection must not claim to have been applied there -- symmetric
      // with readEntity's `!isReference` gate on the single-entity $ref case.
      preferencesAppliedBuilder.preference(PreferenceName.OMIT_VALUES.getName(), "nulls");
      anyPreferenceApplied = true;
    }
    if (anyPreferenceApplied) {
      response.setHeader(HttpHeader.PREFERENCE_APPLIED, preferencesAppliedBuilder.build().toValueString());
    }
    if(delta!=null && request.getHeaders(HttpHeader.ODATA_MAX_VERSION) != null){
      response.setHeader(HttpHeader.ODATA_VERSION,request.getHeaders(HttpHeader.ODATA_MAX_VERSION).get(0));
    }
  }
  private List<Entity> readNavigationEntities(final UriInfo uriInfo) {   

    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    return dataProvider.readNavigationEntities(((UriResourceEntitySet) resourcePaths.get(0)).getEntitySet());
  }

  private String generateDeltaToken() {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.000")
        .format(LocalDateTime.now());
  } 
  
  private SerializerResult serializeDeltaPayloads(final ODataRequest request, final Delta delta, 
      final EdmEntitySet edmEntitySet, final EdmEntityType edmEntityType,
      final ContentType requestedFormat, final ExpandOption expand, final SelectOption select,
      final CountOption countOption, String id) throws ODataLibraryException {

    return odata.createEdmDeltaSerializer(requestedFormat, request.getHeaders(HttpHeader.ODATA_VERSION))
        .entityCollection(serviceMetadata,
        edmEntityType, delta,
        EntityCollectionSerializerOptions.with()
        .contextURL(isODataMetadataNone(requestedFormat) ? null :
          getContextUrl(request.getRawODataPath(), edmEntitySet, edmEntityType, false, expand, select, false))
      .count(countOption)
      .expand(expand).select(select)
      .id(id)
      .build());
   
  }

  /**
   * Check is streaming is enabled for this entity set in combination with the given content type.
   * <code>TRUE</code> if the technical scenario supports streaming for this combination,
   * otherwise <code>FALSE</code>.
   *
   * @param edmEntitySet entity set of the request
   * @param contentType requested content type of the request
   * @return <code>TRUE</code> if the technical scenario supports streaming for this combination,
   *          otherwise <code>FALSE</code>.
   */
  private boolean isStreaming(EdmEntitySet edmEntitySet, ContentType contentType) {
    return (ContainerProvider.ES_STREAM.equalsIgnoreCase(edmEntitySet.getName())||
        ContainerProvider.ES_STREAM_SERVER_PAGINATION.equalsIgnoreCase(edmEntitySet.getName()));
  }
  
  private boolean isMediaStreaming(EdmEntitySet edmEntitySet) {
	return (ContainerProvider.ES_MEDIA_STREAM.equalsIgnoreCase(edmEntitySet.getName()));
  }

  private SerializerResult serializeEntityCollection(final ODataRequest request, final EntityCollection
      entityCollection, final EdmEntitySet edmEntitySet, final EdmEntityType edmEntityType,
      final ContentType requestedFormat, final ExpandOption expand, final SelectOption select,
      final CountOption countOption, String id, final boolean isContNav, final boolean omitNulls)
      throws ODataLibraryException {

    return odata.createSerializer(requestedFormat, request.getHeaders(HttpHeader.ODATA_VERSION))
        .entityCollection(
        serviceMetadata,
        edmEntityType,
        entityCollection,
        EntityCollectionSerializerOptions.with()
            .contextURL(isODataMetadataNone(requestedFormat) ? null :
                getContextUrl(request.getRawODataPath(), edmEntitySet, edmEntityType, false, expand, select, isContNav))
            .count(countOption)
            .expand(expand).select(select)
            .id(id)
            .omitNulls(omitNulls)
            .build());
  }

  // serialise as streamed collection
  private SerializerStreamResult serializeEntityCollectionStreamed(final ODataRequest request,
      final EntityCollection entityCollection, final EdmEntitySet edmEntitySet,
      final EdmEntityType edmEntityType,
      final ContentType requestedFormat, final ExpandOption expand, final SelectOption select,
      final CountOption countOption, final String id, final boolean omitNulls) throws ODataLibraryException {

    EntityIterator streamCollection = new EntityIterator() {
      Iterator<Entity> entityIterator = entityCollection.iterator();
      private URI next = entityCollection.getNext();
      private Integer count = entityCollection.getCount();
      @Override
      public List<Operation> getOperations() {
        return entityCollection.getOperations();
      } 
      
      public URI getNext() {
        return next;
      }
      
      public Integer getCount() {
        return count;
      }
      
      @Override
      public boolean hasNext() {
        return entityIterator.hasNext();
      }

      @Override
      public Entity next() {
        return addToPrimitiveProperty(entityIterator.next(), "PropertyString", "->streamed");
      }

      private Entity addToPrimitiveProperty(Entity entity, String name, Object data) {
        List<Property> properties = entity.getProperties();
        addTo(name, data, properties);
        return entity;
      }

      private void addTo(String name, Object data, List<Property> properties) {
        int pos = 0;
        for (Property property : properties) {
          if (property.isComplex()) {
            @SuppressWarnings("unchecked")
            final List<ComplexValue> cvs = property.isCollection() ?
                (List<ComplexValue>) property.asCollection() :
                List.of(property.asComplex());
            for (ComplexValue cv : cvs) {
              final List<Property> value = cv.getValue();
              if (value != null) {
                addTo(name, data, value);
              }
            }
          }

          if (name.equals(property.getName())) {
            properties.remove(pos);
            final String old = property.getValue().toString();
            String newValue = (old == null ? "": old) + data.toString();
            properties.add(pos, new Property(null, name, ValueType.PRIMITIVE, newValue));
            break;
          }
          pos++;
        }
      }
    };

    return odata.createSerializer(requestedFormat).entityCollectionStreamed(
        serviceMetadata,
        edmEntityType,
        streamCollection,
        EntityCollectionSerializerOptions.with()
            .contextURL(isODataMetadataNone(requestedFormat) ? null :
                getContextUrl(request.getRawODataPath(), edmEntitySet, edmEntityType, false, expand, select, false))
            .count(countOption)
            .expand(expand).select(select)
            .id(id)
            .omitNulls(omitNulls)
            .build());
  }

  private SerializerResult serializeReferenceCollection(final EntityCollection entityCollection,
      final EdmEntitySet edmEntitySet, final ContentType requestedFormat, final CountOption countOption)
      throws ODataLibraryException {

    return odata.createSerializer(requestedFormat)
        .referenceCollection(serviceMetadata, edmEntitySet, entityCollection,
            ReferenceCollectionSerializerOptions.with()
                .contextURL(ContextURL.with().asCollection().suffix(Suffix.REFERENCE).build())
                .count(countOption).build());
  }

  private SerializerResult serializeReference(final Entity entity, final EdmEntitySet edmEntitySet,
      final ContentType requestedFormat) throws ODataLibraryException {
    return odata.createSerializer(requestedFormat)
        .reference(serviceMetadata, edmEntitySet, entity, ReferenceSerializerOptions.with()
            .contextURL(ContextURL.with().suffix(Suffix.REFERENCE).build()).build());

  }

  private SerializerResult serializeEntity(final ODataRequest request, final Entity entity,
      final EdmEntitySet edmEntitySet, final EdmEntityType edmEntityType,
      final ContentType requestedFormat) throws ODataLibraryException {
    // Write responses (create/update) always go through this overload and never omit nulls.
    return serializeEntity(request, entity, edmEntitySet, edmEntityType, requestedFormat, null, null, false, false);
  }

  private SerializerResult serializeEntity(final ODataRequest request, final Entity entity,
      final EdmEntitySet edmEntitySet, final EdmEntityType edmEntityType,
      final ContentType requestedFormat,
      final ExpandOption expand, final SelectOption select, final boolean isContNav)
      throws ODataLibraryException {
    // Write responses (create/update) always go through this overload and never omit nulls.
    return serializeEntity(request, entity, edmEntitySet, edmEntityType, requestedFormat, expand, select,
        isContNav, false);
  }

  private SerializerResult serializeEntity(final ODataRequest request, final Entity entity,
      final EdmEntitySet edmEntitySet, final EdmEntityType edmEntityType,
      final ContentType requestedFormat,
      final ExpandOption expand, final SelectOption select, final boolean isContNav, final boolean omitNulls)
      throws ODataLibraryException {

    ContextURL contextUrl = isODataMetadataNone(requestedFormat) ? null :
        getContextUrl(request.getRawODataPath(), edmEntitySet, edmEntityType, true, expand, select,isContNav);
    return odata.createSerializer(requestedFormat, request.getHeaders(HttpHeader.ODATA_VERSION)).entity(
        serviceMetadata,
        edmEntityType,
        entity,
        EntitySerializerOptions.with()
            .contextURL(contextUrl)
            .expand(expand).select(select)
            .omitNulls(omitNulls)
            .build());
  }

  private ContextURL getContextUrl(String rawODataPath, final EdmEntitySet entitySet, final EdmEntityType entityType,
      final boolean isSingleEntity, final ExpandOption expand, final SelectOption select, final boolean isContNav)
      throws ODataLibraryException {
    Builder builder = ContextURL.with().oDataPath(rawODataPath);
    builder = entitySet == null ?
        isSingleEntity ? builder.type(entityType) : builder.asCollection().type(entityType) :
          !isContNav ? builder.entitySet(entitySet) : builder.entitySetOrSingletonOrType(rawODataPath.substring(1));
    builder = builder
        .selectList(odata.createUriHelper().buildContextURLSelectList(entityType, expand, select))
        .suffix(isSingleEntity && entitySet != null ? Suffix.ENTITY : null);
    return builder.build();
  }

  private void ensureNavigationPropertyNotNull(final UriResourceNavigation navigationProperty)
      throws ODataApplicationException {
    if (navigationProperty == null) {
      throw new ODataApplicationException("Missing navigation segment", HttpStatusCode.BAD_REQUEST.getStatusCode(),
          Locale.ROOT);
    }
  }
}
