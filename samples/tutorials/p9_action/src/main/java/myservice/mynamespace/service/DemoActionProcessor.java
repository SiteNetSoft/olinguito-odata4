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
 * Copyright 2026 SiteNetSoft - Fixed contains(null) NPE with List.of() collections
 */
package myservice.mynamespace.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL.Builder;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL.Suffix;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Parameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences.Return;
import org.sitenetsoft.olinguito.server.api.prefer.PreferencesApplied;
import org.sitenetsoft.olinguito.server.api.processor.ActionEntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionEntityProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ActionVoidProcessor;
import org.sitenetsoft.olinguito.server.api.serializer.EntityCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.EntitySerializerOptions;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceAction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;

import myservice.mynamespace.data.DemoEntityActionResult;
import myservice.mynamespace.data.Storage;

public class DemoActionProcessor implements ActionVoidProcessor, ActionEntityCollectionProcessor, ActionEntityProcessor {

  private OData odata;
  private Storage storage;
  private ServiceMetadata serviceMetadata;
  
  public DemoActionProcessor(final Storage storage) {
    this.storage = storage;
  }

  @Override
  public void init(final OData odata, final ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
  }

  @Override
  public void processActionVoid(ODataRequest request, ODataResponse response, UriInfo uriInfo,
      ContentType requestFormat) throws ODataApplicationException, ODataLibraryException {

    // 1st Get the action from the resource path
    final EdmAction edmAction = ((UriResourceAction) uriInfo.asUriInfoResource().getUriResourceParts()
                                                                                .get(0)).getAction();

    // 2nd Deserialize the parameter
    // In our case there is only one action. So we can be sure that parameter "Amount" has been provided by the client
    if (requestFormat == null) {
      throw new ODataApplicationException("The content type has not been set in the request.",
          HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }
    
    final ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
    final Map<String, Parameter> actionParameter = deserializer.actionParameters(request.getBody(), edmAction)
        .getActionParameters();
    final Parameter parameterAmount = actionParameter.get(DemoEdmProvider.PARAMETER_AMOUNT);
    
    // The parameter amount is nullable
    if(parameterAmount.isNull()) {
      storage.resetDataSet();
    } else {
      final Integer amount = (Integer) parameterAmount.asPrimitive();
      storage.resetDataSet(amount);
    }

    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Override
  public void processActionEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
      ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

    EdmAction action = null;
    Map<String, Parameter> parameters = new HashMap<String, Parameter>(); 
    DemoEntityActionResult entityResult = null;
    if (requestFormat == null) {
      throw new ODataApplicationException("The content type has not been set in the request.",
          HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }
    
    final ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
    final List<UriResource> resourcePaths = uriInfo.asUriInfoResource().getUriResourceParts();
    UriResourceEntitySet boundEntity = (UriResourceEntitySet) resourcePaths.get(0);
    if (resourcePaths.size() > 1) {
      if (resourcePaths.get(1) instanceof UriResourceNavigation) {
        action = ((UriResourceAction) resourcePaths.get(2))
            .getAction();
        throw new ODataApplicationException("Action " + action.getName() + " is not yet implemented.",
              HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
      } else if (resourcePaths.get(0) instanceof UriResourceEntitySet) {
        action = ((UriResourceAction) resourcePaths.get(1))
            .getAction();
        parameters = deserializer.actionParameters(request.getBody(), action)
            .getActionParameters();
        entityResult =
            storage.processBoundActionEntity(action, parameters, boundEntity.getKeyPredicates());
      }
    }
    final EdmEntitySet edmEntitySet = boundEntity.getEntitySet();
    final EdmEntityType type = (EdmEntityType) action.getReturnType().getType();

    if (entityResult == null || entityResult.getEntity() == null) {
      if (action.getReturnType().isNullable()) {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      } else {
        // Not nullable return type so we have to give back a 500
        throw new ODataApplicationException("The action could not be executed.",
            HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
      }
    } else {
      final Return returnPreference = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getReturn();
      if (returnPreference == null || returnPreference == Return.REPRESENTATION) {
        response.setContent(odata.createSerializer(responseFormat).entity(
            serviceMetadata,
            type,
            entityResult.getEntity(),
            EntitySerializerOptions.with()
                .contextURL(isODataMetadataNone(responseFormat) ? null : 
                  getContextUrl(action.getReturnedEntitySet(edmEntitySet), type, true))
                .build())
            .getContent());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        response.setStatusCode((entityResult.isCreated() ? HttpStatusCode.CREATED : HttpStatusCode.OK)
            .getStatusCode());
      } else {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      }
      if (returnPreference != null) {
        response.setHeader(HttpHeader.PREFERENCE_APPLIED,
            PreferencesApplied.with().returnRepresentation(returnPreference).build().toValueString());
      }
      if (entityResult.isCreated()) {
        final String location = request.getRawBaseUri() + '/'
            + odata.createUriHelper().buildCanonicalURL(edmEntitySet, entityResult.getEntity());
        response.setHeader(HttpHeader.LOCATION, location);
        if (returnPreference == Return.MINIMAL) {
          response.setHeader(HttpHeader.ODATA_ENTITY_ID, location);
        }
      }
      if (entityResult.getEntity().getETag() != null) {
        response.setHeader(HttpHeader.ETAG, entityResult.getEntity().getETag());
      }
    }    
  }

  @Override
  public void processActionEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
      ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

    Map<String, Parameter> parameters = new HashMap<String, Parameter>();
    EdmAction action = null;
    EntityCollection collection = null;
    
    if (requestFormat == null) {
      throw new ODataApplicationException("The content type has not been set in the request.",
          HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }
    
    List<UriResource> resourcePaths = uriInfo.asUriInfoResource().getUriResourceParts();
    final ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
    UriResourceEntitySet boundEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
    if (resourcePaths.size() > 1) {
      if (resourcePaths.get(1) instanceof UriResourceNavigation) {
        action = ((UriResourceAction) resourcePaths.get(2))
            .getAction();
        throw new ODataApplicationException("Action " + action.getName() + " is not yet implemented.",
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
      } else {
        action = ((UriResourceAction) resourcePaths.get(1))
            .getAction();
        parameters = deserializer.actionParameters(request.getBody(), action)
            .getActionParameters();
        collection =
            storage.processBoundActionEntityCollection(action, parameters);
      }
    }
    // Collections must never be null.
    // Not nullable return types must not contain a null value.
    if (collection == null
        || collection.getEntities().stream().anyMatch(Objects::isNull) && !action.getReturnType().isNullable()) {
      throw new ODataApplicationException("The action could not be executed.",
          HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
    }

    final Return returnPreference = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getReturn();
    if (returnPreference == null || returnPreference == Return.REPRESENTATION) {
      final EdmEntitySet edmEntitySet = boundEntitySet.getEntitySet();
      final EdmEntityType type = (EdmEntityType) action.getReturnType().getType();
      final EntityCollectionSerializerOptions options = EntityCollectionSerializerOptions.with()
          .contextURL(isODataMetadataNone(responseFormat) ? null : 
            getContextUrl(action.getReturnedEntitySet(edmEntitySet), type, false))
          .build();
      response.setContent(odata.createSerializer(responseFormat)
          .entityCollection(serviceMetadata, type, collection, options).getContent());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    } else {
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
    if (returnPreference != null) {
      response.setHeader(HttpHeader.PREFERENCE_APPLIED,
          PreferencesApplied.with().returnRepresentation(returnPreference).build().toValueString());
    }
  }
  
  private ContextURL getContextUrl(final EdmEntitySet entitySet, final EdmEntityType entityType,
      final boolean isSingleEntity) throws ODataLibraryException {
    Builder builder = ContextURL.with();
    builder = entitySet == null ?
        isSingleEntity ? builder.type(entityType) : builder.asCollection().type(entityType) :
        builder.entitySet(entitySet);
    builder = builder.suffix(isSingleEntity && entitySet != null ? Suffix.ENTITY : null);
    return builder.build();
  }
  
  protected boolean isODataMetadataNone(final ContentType contentType) {
    return contentType.isCompatible(ContentType.APPLICATION_JSON)
        && ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(
            contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA));
  }
}
