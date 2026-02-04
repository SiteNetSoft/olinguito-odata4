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
 * Copyright 2026 SiteNetSoft - Added server-driven paging support
 */
package org.sitenetsoft.olinguito.server.core.responses;

import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.core.Encoder;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.EntityCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.ContentNegotiatorException;
import org.sitenetsoft.olinguito.server.core.ServiceRequest;
import org.sitenetsoft.olinguito.server.core.serializer.utils.ContentTypeHelper;

public class EntitySetResponse extends ServiceResponse {
  private final ODataSerializer serializer;
  private final EntityCollectionSerializerOptions options;
  private final ContentType responseContentType;

  private EntitySetResponse(ServiceMetadata metadata, ODataResponse response, ODataSerializer serializer,
      EntityCollectionSerializerOptions options,
      ContentType responseContentType, Map<String, String> preferences) {
    super(metadata, response, preferences);
    this.serializer = serializer;
    this.options = options;
    this.responseContentType = responseContentType;
  }

  public static EntitySetResponse getInstance(ServiceRequest request, ContextURL contextURL,
      boolean referencesOnly, ODataResponse response) throws ContentNegotiatorException, SerializerException {
    EntityCollectionSerializerOptions options = request.getSerializerOptions(
        EntityCollectionSerializerOptions.class, contextURL, referencesOnly);
    return new EntitySetResponse(request.getServiceMetaData(),response, request.getSerializer(), options,
        request.getResponseContentType(), request.getPreferences());
  }

  /**
   * Writes the entity collection to the response.
   * For server-driven paging, use {@link #writeReadEntitySet(EdmEntityType, EntityCollection, int, String, String)}
   * or set EntityCollection.setNext() with the next link URI before calling this method.
   */
  public void writeReadEntitySet(EdmEntityType entityType, EntityCollection entitySet)
      throws SerializerException {

    assert (!isClosed());

    if (entitySet == null) {
      writeNotFound(true);
      return;
    }

    if (ContentTypeHelper.isODataMetadataFull(this.responseContentType)) {
      buildOperations(entityType, entitySet);
    }
    // write the whole collection to response
    this.response.setContent(this.serializer.entityCollection(metadata, entityType, entitySet, this.options)
                                            .getContent());
    writeOK(responseContentType);
    close();
  }

  /**
   * Writes the entity collection to the response with server-driven paging.
   * <p>
   * This method applies paging to the collection before writing:
   * <ul>
   *   <li>Limits results to the specified page size</li>
   *   <li>Automatically sets the @odata.nextLink if more results exist</li>
   *   <li>Handles $skiptoken for subsequent page requests</li>
   * </ul>
   * </p>
   *
   * @param entityType     the entity type
   * @param entitySet      the full entity collection (will be modified to contain only current page)
   * @param pageSize       maximum number of entities per page
   * @param rawRequestUri  the raw request URI (from ODataRequest.getRawRequestUri())
   * @param skipToken      the current skip token value (from SkipTokenOption.getValue()), or null
   * @throws SerializerException if serialization fails
   */
  public void writeReadEntitySet(EdmEntityType entityType, EntityCollection entitySet,
      int pageSize, String rawRequestUri, String skipToken) throws SerializerException {

    if (entitySet != null && pageSize > 0) {
      PagingHelper.applyPaging(entitySet, pageSize, rawRequestUri, skipToken);
    }
    writeReadEntitySet(entityType, entitySet);
  }

  @Override
  public void accepts(ServiceResponseVisior visitor) throws ODataLibraryException,
      ODataApplicationException {
    visitor.visit(this);
  }
  
  public void writeError(ODataServerError error) {
    try {
      writeHeader(HttpHeader.CONTENT_TYPE, this.responseContentType.toContentTypeString());
      writeContent(this.serializer.error(error).getContent(), error.getStatusCode(), true);
    } catch (SerializerException e) {
      writeServerError(true);
    }
  }
    
  private void buildOperations(EdmEntityType entityType,
      EntityCollection entitySet) {
    EdmAction action = this.metadata.getEdm().getBoundActionWithBindingType(
        entityType.getFullQualifiedName(), true);
    if (action != null) {
      entitySet.getOperations().add(buildOperation(action, buildOperationTarget(options.getContextURL())));
    }
    
    action = this.metadata.getEdm().getBoundActionWithBindingType(
        entityType.getFullQualifiedName(), false);
    if (action != null) {
      for (Entity entity:entitySet.getEntities()) {
        entity.getOperations().add(buildOperation(action, entity.getId().toASCIIString()));
      }
    }      
    
    List<EdmFunction> functions = this.metadata.getEdm()
        .getBoundFunctionsWithBindingType(entityType.getFullQualifiedName(),true);
    
    for (EdmFunction function:functions) {
      entitySet.getOperations().add(buildOperation(function, buildOperationTarget(options.getContextURL())));
    }
    
    functions = this.metadata.getEdm()
        .getBoundFunctionsWithBindingType(entityType.getFullQualifiedName(),false);
    
    for (Entity entity:entitySet.getEntities()) {
      for (EdmFunction function:functions) {
        entity.getOperations().add(buildOperation(function, entity.getId().toASCIIString()));
      }
    }
  }
  
  private String buildOperationTarget(ContextURL contextURL) {
    StringBuilder result = new StringBuilder();
    if (contextURL.getServiceRoot() != null) {
      result.append(contextURL.getServiceRoot());
    }
    if (contextURL.getEntitySetOrSingletonOrType() != null) {
      if (result.length() != 0) {
        result.append("/");
      }
      result.append(Encoder.encode(contextURL.getEntitySetOrSingletonOrType()));
    }
    if (contextURL.getKeyPath() != null) {
      result.append('(').append(contextURL.getKeyPath()).append(')');
    }    
    if (contextURL.getNavOrPropertyPath() != null) {
      result.append('/').append(contextURL.getNavOrPropertyPath());
    }
    return result.toString();
  }
}
