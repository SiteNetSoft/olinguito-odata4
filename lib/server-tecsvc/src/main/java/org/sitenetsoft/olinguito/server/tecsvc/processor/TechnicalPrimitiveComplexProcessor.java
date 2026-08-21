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
 * Copyright 2026 SiteNetSoft - OLINGO-1596: Add $count support for collection properties
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 3: serve GET for direct dynamic-property paths
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 4: serve PUT/PATCH/DELETE for direct
 * dynamic-property paths
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 3: serve ESCollAllPrim/CollPropertyString and
 * ESMixPrimCollComp/CollPropertyComp from the streamed serializer path
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL.Builder;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Link;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.PropertyIterator;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmStructuredType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.deserializer.FixedFormatDeserializer;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences.Return;
import org.sitenetsoft.olinguito.server.api.prefer.PreferencesApplied;
import org.sitenetsoft.olinguito.server.api.processor.ComplexCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.ComplexProcessor;
import org.sitenetsoft.olinguito.server.api.processor.CountComplexCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.CountPrimitiveCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.PrimitiveCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.PrimitiveProcessor;
import org.sitenetsoft.olinguito.server.api.processor.PrimitiveValueProcessor;
import org.sitenetsoft.olinguito.server.api.serializer.ComplexSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.FixedFormatSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveValueSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.RepresentationType;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerResult;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;
import org.sitenetsoft.olinguito.server.api.uri.UriHelper;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceComplexProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceDynamicProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceKind;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceProperty;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.CountOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SelectOption;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider.DataProviderException;
import org.sitenetsoft.olinguito.server.tecsvc.data.DynamicPropertyTypeResolver;

/**
 * Technical Processor which provides functionality related to primitive and complex types and collections thereof.
 */
public class TechnicalPrimitiveComplexProcessor extends TechnicalProcessor
    implements PrimitiveProcessor, PrimitiveValueProcessor,
    PrimitiveCollectionProcessor, CountPrimitiveCollectionProcessor,
    ComplexProcessor, ComplexCollectionProcessor, CountComplexCollectionProcessor {

  private static final String EDMSTREAM = "Edm.Stream";

  private static final String NESTED_DYNAMIC_WRITE_MESSAGE =
      "Writing a dynamic property nested inside a complex property is not supported.";

  /**
   * The two collection-property endpoints this reference service serves from the streamed
   * serializer path, so that streamed and buffered delivery are exercised against the same
   * already-pinned payloads. Everything else stays buffered.
   */
  private static final String ES_COLL_ALL_PRIM = "ESCollAllPrim";
  private static final String ES_MIX_PRIM_COLL_COMP = "ESMixPrimCollComp";
  private static final String COLL_PROPERTY_STRING = "CollPropertyString";
  private static final String COLL_PROPERTY_COMP = "CollPropertyComp";

  public TechnicalPrimitiveComplexProcessor(final DataProvider dataProvider,
      final ServiceMetadata serviceMetadata) {
    super(dataProvider, serviceMetadata);
  }

  @Override
  public void readPrimitive(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType contentType) throws ODataApplicationException, ODataLibraryException {
    readProperty(request, response, uriInfo, contentType, RepresentationType.PRIMITIVE);
  }

  @Override
  public void readPrimitiveValue(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType contentType) throws ODataApplicationException, ODataLibraryException {
    readProperty(request, response, uriInfo, contentType, RepresentationType.VALUE);
  }

  @Override
  public void updatePrimitive(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    updateProperty(request, response, uriInfo, requestFormat, responseFormat, RepresentationType.PRIMITIVE);
  }

  @Override
  public void updatePrimitiveValue(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    updateProperty(request, response, uriInfo, requestFormat, responseFormat, RepresentationType.VALUE);
  }

  @Override
  public void deletePrimitive(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    deleteProperty(request, response, uriInfo, false);
  }

  @Override
  public void deletePrimitiveValue(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    deleteProperty(request, response, uriInfo, true);
  }

  @Override
  public void readPrimitiveCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType contentType) throws ODataApplicationException, ODataLibraryException {
    readProperty(request, response, uriInfo, contentType, RepresentationType.COLLECTION_PRIMITIVE);
  }

  @Override
  public void countPrimitiveCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    readProperty(request, response, uriInfo, ContentType.TEXT_PLAIN, RepresentationType.COUNT);
  }

  @Override
  public void updatePrimitiveCollection(final ODataRequest request, ODataResponse response,
      final UriInfo uriInfo, final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    updateProperty(request, response, uriInfo, requestFormat, responseFormat, RepresentationType.COLLECTION_PRIMITIVE);
  }

  @Override
  public void deletePrimitiveCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    deleteProperty(request, response, uriInfo, false);
  }

  @Override
  public void readComplex(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType contentType) throws ODataApplicationException, ODataLibraryException {
    readProperty(request, response, uriInfo, contentType, RepresentationType.COMPLEX);
  }

  @Override
  public void updateComplex(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    updateProperty(request, response, uriInfo, requestFormat, responseFormat, RepresentationType.COMPLEX);
  }

  @Override
  public void deleteComplex(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    deleteProperty(request, response, uriInfo, false);
  }

  @Override
  public void readComplexCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType contentType) throws ODataApplicationException, ODataLibraryException {
    readProperty(request, response, uriInfo, contentType, RepresentationType.COLLECTION_COMPLEX);
  }

  @Override
  public void countComplexCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    readProperty(request, response, uriInfo, ContentType.TEXT_PLAIN, RepresentationType.COUNT);
  }

  @Override
  public void updateComplexCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat)
      throws ODataApplicationException, ODataLibraryException {
    updateProperty(request, response, uriInfo, requestFormat, responseFormat, RepresentationType.COLLECTION_COMPLEX);
  }

  @Override
  public void deleteComplexCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
      throws ODataApplicationException, ODataLibraryException {
    deleteProperty(request, response, uriInfo, false);
  }

  private void readProperty(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType contentType, final RepresentationType representationType)
          throws ODataApplicationException, ODataLibraryException {
    final UriInfoResource resource = uriInfo.asUriInfoResource();
    validateOptions(resource);

    final List<UriResource> resourceParts = resource.getUriResourceParts();
    final int trailing =
        representationType == RepresentationType.COUNT || representationType == RepresentationType.VALUE ? 1 : 0;
    final int lastIndex = resourceParts.size() - trailing - 1;

    if (resourceParts.get(lastIndex) instanceof UriResourceDynamicProperty dynamicProperty) {
      // A dynamic-property segment is only ever valid as the GET-served leaf handled below; it is
      // deliberately NOT added to validatePath()'s shared allow-list, because that method also
      // gates updateProperty (PUT/PATCH) and deleteProperty (DELETE) - Task 4's job, not this
      // task's. Validate every OTHER segment (still rejecting anything validatePath would reject),
      // then handle the dynamic segment itself here.
      for (int i = 1; i < resourceParts.size(); i++) {
        if (i != lastIndex) {
          validateSegmentKind(resourceParts.get(i));
        }
      }
      final EdmEntitySet edmEntitySet = getEdmEntitySet(resource);
      final List<String> parentPath = getPropertyPath(resourceParts, trailing + 1);
      readDynamicProperty(request, response, uriInfo, contentType, edmEntitySet, dynamicProperty, parentPath);
      return;
    }

    validatePath(resource);
    final EdmEntitySet edmEntitySet = getEdmEntitySet(resource);

    final List<String> path = getPropertyPath(resourceParts, trailing);

    final Entity entity = readEntity(uriInfo);

    if (entity != null && entity.getETag() != null) {
      if (odata.createETagHelper().checkReadPreconditions(entity.getETag(),
          request.getHeaders(HttpHeader.IF_MATCH),
          request.getHeaders(HttpHeader.IF_NONE_MATCH))) {
        response.setStatusCode(HttpStatusCode.NOT_MODIFIED.getStatusCode());
        response.setHeader(HttpHeader.ETAG, entity.getETag());
        return;
      }
    }

    final Property property = entity == null ?
        getPropertyData(
            dataProvider.readFunctionPrimitiveComplex(((UriResourceFunction) resourceParts.get(0)).getFunction(),
            ((UriResourceFunction) resourceParts.get(0)).getParameters(), resource), path) :
        getData(entity, path, resourceParts, resource);

    // TODO: implement filter on collection properties (on a shallow copy of the values)
    // FilterHandler.applyFilterSystemQuery(uriInfo.getFilterOption(), property, uriInfo, serviceMetadata.getEdm());

    if (property == null && representationType != RepresentationType.COUNT) {
      if (representationType == RepresentationType.VALUE) {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      } else {
        throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
      }
    } else {
      if (property.getValue() == null && representationType != RepresentationType.COUNT) {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      } else {
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        if (representationType == RepresentationType.COUNT) {
          response.setContent(odata.createFixedFormatSerializer().count(
              property.asCollection().size()));
        } else {
          final EdmProperty edmProperty = path.isEmpty() ? null :
              ((UriResourceProperty) resourceParts.get(resourceParts.size() - trailing - 1)).getProperty();
          EdmType type = null;
          if (resourceParts.get(resourceParts.size() - trailing - 1)
             instanceof UriResourceComplexProperty complexProp &&
              complexProp.getComplexTypeFilter() != null) {
            type = complexProp.getComplexTypeFilter();
          }else if(resourceParts.get(resourceParts.size() - trailing - 1)
             instanceof UriResourceFunction funcResource &&
              funcResource.getFunction() != null){
            type = funcResource.getType();
          }else {
            type = edmProperty == null ?
                ((UriResourceFunction) resourceParts.get(0)).getType() :
                edmProperty.getType();
          }
          final EdmReturnType returnType = resourceParts.get(0) instanceof UriResourceFunction func0 ?
              func0.getFunction().getReturnType() :
                resourceParts.get(1) instanceof UriResourceFunction func1 ?
                    func1.getFunction().getReturnType():null ;

          if (representationType == RepresentationType.VALUE) {
            response.setContent(serializePrimitiveValue(property, edmProperty, (EdmPrimitiveType) type, returnType));
          }else if(representationType == RepresentationType.PRIMITIVE && type.getFullQualifiedName()
              .getFullQualifiedNameAsString().equals(EDMSTREAM)){
            response.setContent(odata.createFixedFormatSerializer().binary(dataProvider.readStreamProperty(property)));
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, ((Link)property.getValue()).getType());
            if (entity.getMediaETag() != null) {
              response.setHeader(HttpHeader.ETAG, entity.getMediaETag());
            }
          }else {
            final CountOption countOption = uriInfo.getCountOption();
            final ExpandOption expand = uriInfo.getExpandOption();
            final SelectOption select = uriInfo.getSelectOption();
            if (countOption != null && countOption.getValue()
                && property.isCollection() && property.asCollection() != null) {
              property.setCount(property.asCollection().size());
            }
            if (isStreamingCollection(edmEntitySet, path, representationType)) {
              response.setODataContent(serializeCollectionStreamed(entity, edmEntitySet, path, property,
                  edmProperty, type, returnType, representationType, contentType, countOption, expand, select)
                  .getODataContent());
            } else {
              final SerializerResult result = serializeProperty(entity, edmEntitySet, path, property, edmProperty,
                  type, returnType, representationType, contentType, countOption, expand, select);
              response.setContent(result.getContent());
            }
          }
        }
        response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
      }
      if (entity != null && entity.getETag() != null) {
        response.setHeader(HttpHeader.ETAG, entity.getETag());
      }
    }
  }

  /**
   * Serves GET on a direct dynamic (open-type) property path, e.g. {@code ESOpen(1)/DynamicString}
   * or a nested one, e.g. {@code ESOpen(1)/PropertyComp/CompDynamic} (an open complex property may
   * itself carry dynamic properties). A dynamic property has no EDM declaration, so - unlike
   * {@link #readProperty} above - the type used for serialization is resolved at runtime from the
   * property's stored type name or its Java value's class (see {@link DynamicPropertyTypeResolver}),
   * defaulting to {@code Edm.String}. A collection-valued dynamic property
   * ({@link ValueType#COLLECTION_PRIMITIVE}) is served through the primitive-collection serializer
   * instead of the scalar one, even though dispatch (see {@code ODataDispatcher}) always routes a
   * dynamic-property segment through the scalar {@code readPrimitive} entry point, since the actual
   * shape of an open type's dynamic value is only known once the entity data is inspected.
   *
   * @param parentPath the declared property path (complex property names, if any) leading from the
   *                   entity down to - but not including - the dynamic property itself; empty when
   *                   the dynamic property is directly on the entity
   */
  private void readDynamicProperty(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
      final ContentType contentType, final EdmEntitySet edmEntitySet,
      final UriResourceDynamicProperty dynamicProperty, final List<String> parentPath)
      throws ODataApplicationException, ODataLibraryException {
    final Entity entity = readEntity(uriInfo);

    if (entity.getETag() != null
        && odata.createETagHelper().checkReadPreconditions(entity.getETag(),
            request.getHeaders(HttpHeader.IF_MATCH), request.getHeaders(HttpHeader.IF_NONE_MATCH))) {
      response.setStatusCode(HttpStatusCode.NOT_MODIFIED.getStatusCode());
      response.setHeader(HttpHeader.ETAG, entity.getETag());
      return;
    }

    final Property property = resolveDynamicProperty(entity, parentPath, dynamicProperty.getPropertyName());
    if (property == null) {
      throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
    }

    if (property.getValue() == null) {
      // Mirrors the declared-property path above (:258/:310): no Content-Type on a 204.
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    } else {
      final EdmPrimitiveTypeKind kind = DynamicPropertyTypeResolver.resolveKind(property);
      final EdmPrimitiveType type = odata.createPrimitiveTypeInstance(kind);
      final boolean isCollection = property.getValueType() == ValueType.COLLECTION_PRIMITIVE;
      final List<String> path = new ArrayList<>(parentPath);
      path.add(dynamicProperty.getPropertyName());
      final RepresentationType effectiveType =
          isCollection ? RepresentationType.COLLECTION_PRIMITIVE : RepresentationType.PRIMITIVE;
      final ContextURL contextURL = isODataMetadataNone(contentType) ? null :
          getContextUrl(edmEntitySet, entity, path, type, effectiveType, null, null);
      final PrimitiveSerializerOptions options =
          PrimitiveSerializerOptions.with().contextURL(contextURL).nullable(true).build();
      final ODataSerializer serializer = odata.createSerializer(contentType);
      final SerializerResult result = isCollection ?
          serializer.primitiveCollection(serviceMetadata, type, property, options) :
          serializer.primitive(serviceMetadata, type, property, options);
      response.setContent(result.getContent());
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }
    if (entity.getETag() != null) {
      response.setHeader(HttpHeader.ETAG, entity.getETag());
    }
  }

  /**
   * Resolves a dynamic property by name, optionally nested inside a chain of open complex
   * properties. When {@code parentPath} is empty, looks the name up directly on the entity.
   * Otherwise navigates to the parent complex property first (reusing the same
   * {@link #getPropertyData(Entity, List)} traversal the declared-property path uses) and looks
   * the name up inside it. Returns {@code null} - which the caller turns into a 404 - when the
   * parent complex property itself is absent on this particular entity, or when the name is not
   * present in its value.
   */
  private Property resolveDynamicProperty(final Entity entity, final List<String> parentPath, final String name) {
    if (parentPath.isEmpty()) {
      return entity.getProperty(name);
    }
    final Property parent = getPropertyData(entity, parentPath);
    if (parent == null || !parent.isComplex()) {
      return null;
    }
    for (final Property inner : parent.asComplex().getValue()) {
      if (inner.getName().equals(name)) {
        return inner;
      }
    }
    return null;
  }

  private Property getData(Entity entity, List<String> path, List<UriResource> resourceParts, UriInfoResource resource)
      throws DataProviderException {
    if(resourceParts.size()>1 && resourceParts.get(1) instanceof UriResourceFunction funcResource){
      return dataProvider.readFunctionPrimitiveComplex(funcResource.getFunction(),
          funcResource.getParameters(), resource);
    }
    return getPropertyData(entity, path);
  }

  /**
   * Reserved for future implementation of direct function data retrieval.
   * Currently unused - function data is retrieved through readFunctionPrimitiveComplex.
   */
  @SuppressWarnings("unused")
  private Property getFunctionData(UriResource uriResource) {
    return null;
  }

  private void updateProperty(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final ContentType requestFormat, final ContentType responseFormat, final RepresentationType representationType)
      throws ODataApplicationException, ODataLibraryException {
    final UriInfoResource resource = uriInfo.asUriInfoResource();
    final List<UriResource> resourceParts = resource.getUriResourceParts();
    final int trailing = representationType == RepresentationType.VALUE ? 1 : 0;
    final int lastIndex = resourceParts.size() - trailing - 1;

    if (resourceParts.get(lastIndex) instanceof UriResourceDynamicProperty dynamicProperty) {
      // Mirrors readProperty's dynamic branch (see its Javadoc): validate every OTHER segment
      // (still rejecting anything validatePath would reject), then handle the dynamic write itself
      // in updateDynamicProperty, entirely bypassing validatePath's shared allow-list and the
      // declared-property EdmProperty logic below (which would ClassCastException on a
      // UriResourceDynamicProperty).
      for (int i = 1; i < resourceParts.size(); i++) {
        if (i != lastIndex) {
          validateSegmentKind(resourceParts.get(i));
        }
      }
      final List<String> parentPath = getPropertyPath(resourceParts, trailing + 1);
      updateDynamicProperty(request, response, uriInfo, requestFormat, responseFormat, dynamicProperty, parentPath);
      return;
    }

    validatePath(resource);
    final EdmEntitySet edmEntitySet = getEdmEntitySet(resource);

    Entity entity = readEntity(uriInfo);
    odata.createETagHelper().checkChangePreconditions(entity.getETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));

    final List<String> path = getPropertyPath(resourceParts, trailing);
    final EdmProperty edmProperty = ((UriResourceProperty) resourceParts.get(resourceParts.size() - trailing - 1))
        .getProperty();

    Property property = getPropertyData(entity, path);

    if (representationType == RepresentationType.VALUE || 
    		edmProperty.getType().getFullQualifiedName()
    		.getFullQualifiedNameAsString().equalsIgnoreCase(EDMSTREAM)) {
      final FixedFormatDeserializer deserializer = odata.createFixedFormatDeserializer();
      final Object value = edmProperty.getType() == odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Binary) 
    		  || edmProperty.getType() == odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Stream) ?
          deserializer.binary(request.getBody()) :
          deserializer.primitiveValue(request.getBody(), edmProperty);
      dataProvider.updatePropertyValue(property, value);
    } else {
      final Property changedProperty = odata.createDeserializer(requestFormat)
          .property(request.getBody(), edmProperty).getProperty();
      if (changedProperty.isNull() && !edmProperty.isNullable()) {
        throw new ODataApplicationException("Not nullable.", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
      }
      dataProvider.updateProperty(edmProperty, property, changedProperty, request.getMethod() == HttpMethod.PATCH);
    }

    dataProvider.updateETag(entity);

    final Return returnPreference = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getReturn();
    if (returnPreference == null || returnPreference == Return.REPRESENTATION) {
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      if (representationType == RepresentationType.VALUE || 
        		edmProperty.getType().getFullQualifiedName()
          		.getFullQualifiedNameAsString().equalsIgnoreCase(EDMSTREAM)) {
        response.setContent(
            serializePrimitiveValue(property, edmProperty, (EdmPrimitiveType) edmProperty.getType(), null));
      } else {
        final SerializerResult result = serializeProperty(entity, edmEntitySet, path, property, edmProperty,
            edmProperty.getType(), null, representationType, responseFormat, null, null, null);
        response.setContent(result.getContent());
      }
      if (edmProperty.getType().getFullQualifiedName()
        		.getFullQualifiedNameAsString().equalsIgnoreCase(EDMSTREAM)) {
      	  response.setHeader(HttpHeader.CONTENT_TYPE, requestFormat.toContentTypeString());
        } else {
      	  response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        }
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

  private void deleteProperty(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
      final boolean isValue) throws ODataLibraryException, ODataApplicationException {
    final UriInfoResource resource = uriInfo.asUriInfoResource();
    final List<UriResource> resourceParts = resource.getUriResourceParts();
    final int trailing = isValue ? 1 : 0;
    final int lastIndex = resourceParts.size() - trailing - 1;

    if (resourceParts.get(lastIndex) instanceof UriResourceDynamicProperty dynamicProperty) {
      // See the matching branch in updateProperty above for the rationale.
      for (int i = 1; i < resourceParts.size(); i++) {
        if (i != lastIndex) {
          validateSegmentKind(resourceParts.get(i));
        }
      }
      final List<String> parentPath = getPropertyPath(resourceParts, trailing + 1);
      deleteDynamicProperty(request, response, uriInfo, dynamicProperty, parentPath);
      return;
    }

    validatePath(resource);
    getEdmEntitySet(uriInfo); // including checks

    Entity entity = readEntity(uriInfo);
    odata.createETagHelper().checkChangePreconditions(entity.getETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));

    final List<String> path = getPropertyPath(resourceParts, trailing);

    Property property = getPropertyData(entity, path);

    final EdmProperty edmProperty = ((UriResourceProperty) resourceParts.get(resourceParts.size() - trailing - 1))
        .getProperty();

    if (edmProperty.isNullable()) {
      property.setValue(property.getValueType(), edmProperty.isCollection() ? List.of() : null);
      dataProvider.updateETag(entity);
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
      if (entity.getETag() != null) {
        response.setHeader(HttpHeader.ETAG, entity.getETag());
      }
    } else {
      throw new ODataApplicationException("Not nullable.", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }
  }

  /**
   * Serves PUT/PATCH on a direct dynamic (open-type) property path, e.g. {@code ESOpen(1)/DynamicString}.
   * A dynamic property has no EDM declaration, so - unlike {@link #updateProperty} above - the
   * incoming payload is parsed via
   * {@link org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer#dynamicProperty(
   * InputStream, String)}
   * (Task 1), which resolves the property's type from an optional {@code value@odata.type}
   * annotation (or infers it) rather than from a known {@link EdmProperty}. PATCH is deliberately
   * handled identically to PUT: a dynamic scalar has no sub-structure to leave partially
   * untouched, so there is nothing for PATCH's "leave omitted fields alone" semantics to apply to
   * (the entire property value/type is always replaced wholesale, matching a full PUT). A
   * dynamic property absent on the addressed entity is upserted (created), not rejected with 404 -
   * see the design-decision note in the task report.
   *
   * <p>Nested dynamic-property writes (through an open complex property, e.g.
   * {@code ESOpen(1)/PropertyComp/CompDynamic}) are out of scope for this method and rejected with a
   * clean 501 by the caller before this method is even reached (see {@code parentPath} handling in
   * {@link #updateProperty}).
   */
  private void updateDynamicProperty(final ODataRequest request, final ODataResponse response,
      final UriInfo uriInfo, final ContentType requestFormat, final ContentType responseFormat,
      final UriResourceDynamicProperty dynamicProperty, final List<String> parentPath)
      throws ODataApplicationException, ODataLibraryException {
    if (!parentPath.isEmpty()) {
      throw new ODataApplicationException(NESTED_DYNAMIC_WRITE_MESSAGE,
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
    final UriInfoResource resource = uriInfo.asUriInfoResource();
    final EdmEntitySet edmEntitySet = getEdmEntitySet(resource);

    final Entity entity = readEntity(uriInfo);
    odata.createETagHelper().checkChangePreconditions(entity.getETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));

    final String name = dynamicProperty.getPropertyName();
    final Property changedProperty = odata.createDeserializer(requestFormat)
        .dynamicProperty(request.getBody(), name).getProperty();

    dataProvider.updateDynamicProperty(entity, changedProperty);
    dataProvider.updateETag(entity);

    final Property property = entity.getProperty(name);
    final List<String> path = List.of(name);

    final Return returnPreference = odata.createPreferences(request.getHeaders(HttpHeader.PREFER)).getReturn();
    if (returnPreference == null || returnPreference == Return.REPRESENTATION) {
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      final EdmPrimitiveTypeKind kind = DynamicPropertyTypeResolver.resolveKind(property);
      final EdmPrimitiveType type = odata.createPrimitiveTypeInstance(kind);
      final boolean isCollection = property.getValueType() == ValueType.COLLECTION_PRIMITIVE;
      final RepresentationType effectiveType =
          isCollection ? RepresentationType.COLLECTION_PRIMITIVE : RepresentationType.PRIMITIVE;
      final ContextURL contextURL = isODataMetadataNone(responseFormat) ? null :
          getContextUrl(edmEntitySet, entity, path, type, effectiveType, null, null);
      final PrimitiveSerializerOptions options =
          PrimitiveSerializerOptions.with().contextURL(contextURL).nullable(true).build();
      final ODataSerializer serializer = odata.createSerializer(responseFormat);
      final SerializerResult result = isCollection ?
          serializer.primitiveCollection(serviceMetadata, type, property, options) :
          serializer.primitive(serviceMetadata, type, property, options);
      response.setContent(result.getContent());
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

  /**
   * Serves DELETE on a direct dynamic (open-type) property path, e.g. {@code ESOpen(1)/DynamicString}:
   * removes the property from the entity's property list entirely (unlike deleting a declared,
   * nullable property, which only clears its value - a dynamic property has no EDM declaration to
   * fall back to, so "deleted" means "no longer present at all"). Deleting an absent dynamic
   * property 404s: unlike the upsert-on-write choice in {@link #updateDynamicProperty}, there is no
   * reasonable "create it first" interpretation for DELETE, and this mirrors
   * {@code readDynamicProperty}'s 404 for the same absent-property case on GET.
   *
   * <p>Nested dynamic-property deletes are out of scope, same as {@link #updateDynamicProperty}.
   */
  private void deleteDynamicProperty(final ODataRequest request, final ODataResponse response,
      final UriInfo uriInfo, final UriResourceDynamicProperty dynamicProperty, final List<String> parentPath)
      throws ODataApplicationException, ODataLibraryException {
    if (!parentPath.isEmpty()) {
      throw new ODataApplicationException(NESTED_DYNAMIC_WRITE_MESSAGE,
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
    final UriInfoResource resource = uriInfo.asUriInfoResource();
    getEdmEntitySet(resource); // including checks

    final Entity entity = readEntity(uriInfo);
    odata.createETagHelper().checkChangePreconditions(entity.getETag(),
        request.getHeaders(HttpHeader.IF_MATCH),
        request.getHeaders(HttpHeader.IF_NONE_MATCH));

    if (!dataProvider.removeDynamicProperty(entity, dynamicProperty.getPropertyName())) {
      throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
    }

    dataProvider.updateETag(entity);
    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    if (entity.getETag() != null) {
      response.setHeader(HttpHeader.ETAG, entity.getETag());
    }
  }

  private Property getPropertyData(final Entity entity, final List<String> path) {
    return getPropertyData(entity.getProperty(path.get(0)), path.subList(1, path.size()));
  }

  private Property getPropertyData(final Property property, final List<String> path) {
    Property result = property;
    for (final String name : path) {
      if (result != null && property.isComplex()) {
        final List<Property> complex = result.asComplex().getValue();
        result = null;
        for (final Property innerProperty : complex) {
          if (innerProperty.getName().equals(name)) {
            result = innerProperty;
            break;
          }
        }
      }
    }
    return result;
  }

  private List<String> getPropertyPath(final List<UriResource> path, final int trailing) {
    List<String> result = new LinkedList<>();
    int index = path.size() - trailing - 1;
    while (path.get(index) instanceof UriResourceProperty uriResourceProp) {
      result.add(0, uriResourceProp.getProperty().getName());
      index--;
    }
    return result;
  }

  private String buildPropertyPath(final List<String> path) {
    StringBuilder result = new StringBuilder();
    for (final String segment : path) {
      result.append(result.isEmpty() ? "" : '/').append(segment);
    }
    return result.toString();
  }

  private SerializerResult serializeProperty(final Entity entity, final EdmEntitySet edmEntitySet,
      final List<String> path, final Property property, final EdmProperty edmProperty,
      final EdmType type, final EdmReturnType returnType,
      final RepresentationType representationType, final ContentType responseFormat,
      final CountOption count, final ExpandOption expand, final SelectOption select)
      throws ODataLibraryException {
    ODataSerializer serializer = odata.createSerializer(responseFormat);
    final ContextURL contextURL = isODataMetadataNone(responseFormat) ? null :
        getContextUrl(edmEntitySet, entity, path, type, representationType, expand, select);
    SerializerResult result = null;
    switch (representationType) {
    case PRIMITIVE:
      result = serializer.primitive(serviceMetadata, (EdmPrimitiveType) type, property,
          PrimitiveSerializerOptions.with().contextURL(contextURL)
              .nullable(edmProperty == null ? returnType.isNullable() : edmProperty.isNullable())
              .maxLength(edmProperty == null ? returnType.getMaxLength() : edmProperty.getMaxLength())
              .precision(edmProperty == null ? returnType.getPrecision() : edmProperty.getPrecision())
              .scale(edmProperty == null ? returnType.getScale() : edmProperty.getScale())
              .unicode(edmProperty == null ? null : edmProperty.isUnicode())
              .build());
      break;
    case COMPLEX:
      result = serializer.complex(serviceMetadata, (EdmComplexType) type, property,
          ComplexSerializerOptions.with().contextURL(contextURL)
              .expand(expand).select(select)
              .build());
      break;
    case COLLECTION_PRIMITIVE:
      result = serializer.primitiveCollection(serviceMetadata, (EdmPrimitiveType) type, property,
          PrimitiveSerializerOptions.with().contextURL(contextURL)
              .count(count)
              .nullable(edmProperty == null ? returnType.isNullable() : edmProperty.isNullable())
              .maxLength(edmProperty == null ? returnType.getMaxLength() : edmProperty.getMaxLength())
              .precision(edmProperty == null ? returnType.getPrecision() : edmProperty.getPrecision())
              .scale(edmProperty == null ? returnType.getScale() : edmProperty.getScale())
              .unicode(edmProperty == null ? null : edmProperty.isUnicode())
              .build());
      break;
    case COLLECTION_COMPLEX:
      result = serializer.complexCollection(serviceMetadata, (EdmComplexType) type, property,
          ComplexSerializerOptions.with().contextURL(contextURL)
              .count(count).expand(expand).select(select)
              .build());
      break;
    default:
      break;
    }
    return result;
  }

  private boolean isStreamingCollection(final EdmEntitySet edmEntitySet, final List<String> path,
      final RepresentationType representationType) {
    if (edmEntitySet == null || path.size() != 1) {
      return false;
    }
    final String property = path.get(0);
    return (representationType == RepresentationType.COLLECTION_PRIMITIVE
        && ES_COLL_ALL_PRIM.equals(edmEntitySet.getName()) && COLL_PROPERTY_STRING.equals(property))
        || (representationType == RepresentationType.COLLECTION_COMPLEX
        && ES_MIX_PRIM_COLL_COMP.equals(edmEntitySet.getName()) && COLL_PROPERTY_COMP.equals(property));
  }

  /** Wraps an already-materialized collection value in a {@link PropertyIterator}. */
  private PropertyIterator asPropertyIterator(final Property property) {
    final Iterator<?> values = property.asCollection().iterator();
    final PropertyIterator iterator = new PropertyIterator() {
      @Override
      public boolean hasNext() {
        return values.hasNext();
      }

      @Override
      public Property next() {
        return new Property(null, property.getName(), elementValueType(property.getValueType()), values.next());
      }
    };
    iterator.setName(property.getName());
    iterator.setValueType(property.getValueType());
    iterator.setCount(property.getCount());
    return iterator;
  }

  private static ValueType elementValueType(final ValueType collectionValueType) {
    switch (collectionValueType) {
    case COLLECTION_PRIMITIVE: return ValueType.PRIMITIVE;
    case COLLECTION_ENUM: return ValueType.ENUM;
    case COLLECTION_GEOSPATIAL: return ValueType.GEOSPATIAL;
    case COLLECTION_COMPLEX: return ValueType.COMPLEX;
    default: return collectionValueType;
    }
  }

  /**
   * Streamed counterpart of {@link #serializeProperty}, built from its
   * {@code COLLECTION_PRIMITIVE}/{@code COLLECTION_COMPLEX} arms verbatim - same {@link ContextURL},
   * same option-builder chains - with the terminal call swapped for the streamed serializer entry
   * points and the property wrapped in a {@link PropertyIterator}. Kept separate from
   * {@link #serializeProperty} so that method (and the buffered path it serves) stays untouched.
   */
  private SerializerStreamResult serializeCollectionStreamed(final Entity entity, final EdmEntitySet edmEntitySet,
      final List<String> path, final Property property, final EdmProperty edmProperty,
      final EdmType type, final EdmReturnType returnType,
      final RepresentationType representationType, final ContentType responseFormat,
      final CountOption count, final ExpandOption expand, final SelectOption select)
      throws ODataLibraryException {
    final ODataSerializer serializer = odata.createSerializer(responseFormat);
    final ContextURL contextURL = isODataMetadataNone(responseFormat) ? null :
        getContextUrl(edmEntitySet, entity, path, type, representationType, expand, select);
    switch (representationType) {
    case COLLECTION_PRIMITIVE:
      return serializer.primitiveCollectionStreamed(serviceMetadata, (EdmPrimitiveType) type,
          asPropertyIterator(property),
          PrimitiveSerializerOptions.with().contextURL(contextURL)
              .count(count)
              .nullable(edmProperty == null ? returnType.isNullable() : edmProperty.isNullable())
              .maxLength(edmProperty == null ? returnType.getMaxLength() : edmProperty.getMaxLength())
              .precision(edmProperty == null ? returnType.getPrecision() : edmProperty.getPrecision())
              .scale(edmProperty == null ? returnType.getScale() : edmProperty.getScale())
              .unicode(edmProperty == null ? null : edmProperty.isUnicode())
              .build());
    case COLLECTION_COMPLEX:
      return serializer.complexCollectionStreamed(serviceMetadata, (EdmComplexType) type,
          asPropertyIterator(property),
          ComplexSerializerOptions.with().contextURL(contextURL)
              .count(count).expand(expand).select(select)
              .build());
    default:
      return null;
    }
  }

  private ContextURL getContextUrl(final EdmEntitySet entitySet, final Entity entity, final List<String> path,
      final EdmType type, final RepresentationType representationType,
      final ExpandOption expand, final SelectOption select) throws ODataLibraryException {
    final UriHelper helper = odata.createUriHelper();
    Builder builder = ContextURL.with();
    builder = entitySet == null ?
        representationType == RepresentationType.PRIMITIVE || representationType == RepresentationType.COMPLEX ?
            builder.type(type) :
            builder.type(type).asCollection() :
        builder.entitySet(entitySet).keyPath(helper.buildKeyPredicate(entitySet.getEntityType(), entity));
    if (entitySet != null && !path.isEmpty()) {
      builder = builder.navOrPropertyPath(buildPropertyPath(path));
    }
    builder = builder.selectList(
        type.getKind() == EdmTypeKind.PRIMITIVE
            || type.getKind() == EdmTypeKind.ENUM
            || type.getKind() == EdmTypeKind.DEFINITION ?
                null :
                helper.buildContextURLSelectList((EdmStructuredType) type, expand, select));
    return builder.build();
  }

  private InputStream serializePrimitiveValue(final Property property, final EdmProperty edmProperty,
      final EdmPrimitiveType type, final EdmReturnType returnType) throws SerializerException {
    final FixedFormatSerializer serializer = odata.createFixedFormatSerializer();
    return type == odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Binary) ||
    		type == odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Stream) ?
      serializer.binary((byte[]) property.getValue()) :
      serializer.primitiveValue(type, property.getValue(),
          PrimitiveValueSerializerOptions.with()
              .nullable(edmProperty == null ? returnType.isNullable() : edmProperty.isNullable())
              .maxLength(edmProperty == null ? returnType.getMaxLength() : edmProperty.getMaxLength())
              .precision(edmProperty == null ? returnType.getPrecision() : edmProperty.getPrecision())
              .scale(edmProperty == null ? returnType.getScale() : edmProperty.getScale())
              .unicode(edmProperty == null ? null : edmProperty.isUnicode())
              .build());
  }

  /**
   * Validates every path segment (excluding the first, the entity set/singleton/function root)
   * against the kinds this processor's write paths ({@link #updateProperty}/{@link #deleteProperty})
   * and the declared-property read path support. Deliberately does NOT allow
   * {@link UriResourceKind#dynamicProperty}: a dynamic-property segment is handled directly by
   * {@link #readProperty}, {@link #updateProperty} and {@link #deleteProperty} themselves - each
   * detects a trailing dynamic-property segment before ever calling this method, validates its
   * OTHER (non-dynamic) segments via {@link #validateSegmentKind} instead, and dispatches to its own
   * dynamic-specific handler ({@link #readDynamicProperty}, {@link #updateDynamicProperty} or
   * {@link #deleteDynamicProperty}). Loosening this shared allow-list to admit dynamicProperty here
   * instead would let the declared-property logic further down in {@link #updateProperty} and
   * {@link #deleteProperty} reach their unconditional {@code UriResourceProperty} casts on a
   * dynamic-property segment, which is not a {@code UriResourceProperty} and would throw an
   * unhandled {@code ClassCastException}.
   */
  private void validatePath(final UriInfoResource uriInfo) throws ODataApplicationException {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    for (final UriResource segment : resourcePaths.subList(1, resourcePaths.size())) {
      validateSegmentKind(segment);
    }
  }

  private void validateSegmentKind(final UriResource segment) throws ODataApplicationException {
    final UriResourceKind kind = segment.getKind();
    if (kind != UriResourceKind.navigationProperty
        && kind != UriResourceKind.primitiveProperty
        && kind != UriResourceKind.complexProperty
        && kind != UriResourceKind.count
        && kind != UriResourceKind.value
        && kind != UriResourceKind.function) {
      throw new ODataApplicationException("Invalid resource type.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
  }
}
