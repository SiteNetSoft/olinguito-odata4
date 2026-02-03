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
 */
package org.sitenetsoft.olinguito.server.adapter.quarkus.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.processor.EntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.processor.EntityProcessor;
import org.sitenetsoft.olinguito.server.api.serializer.EntityCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.EntitySerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerResult;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;

import java.io.InputStream;
import java.util.List;

/**
 * Test entity processor for integration tests.
 * Handles requests for Product entities.
 */
@ApplicationScoped
public class TestEntityProcessor implements EntityCollectionProcessor, EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response,
            UriInfo uriInfo, ContentType responseFormat) {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        EntityCollection entityCollection = getData(edmEntitySet);

        try {
            ODataSerializer serializer = odata.createSerializer(responseFormat);
            EdmEntityType edmEntityType = edmEntitySet.getEntityType();

            ContextURL contextUrl = ContextURL.with()
                    .entitySet(edmEntitySet)
                    .build();

            EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
                    .contextURL(contextUrl)
                    .build();

            SerializerResult serializerResult = serializer.entityCollection(
                    serviceMetadata, edmEntityType, entityCollection, opts);
            InputStream serializedContent = serializerResult.getContent();

            response.setContent(serializedContent);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing entity collection", e);
        }
    }

    @Override
    public void readEntity(ODataRequest request, ODataResponse response,
            UriInfo uriInfo, ContentType responseFormat) {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        Entity entity = getProduct(1);

        try {
            ODataSerializer serializer = odata.createSerializer(responseFormat);
            EdmEntityType edmEntityType = edmEntitySet.getEntityType();

            ContextURL contextUrl = ContextURL.with()
                    .entitySet(edmEntitySet)
                    .build();

            EntitySerializerOptions opts = EntitySerializerOptions.with()
                    .contextURL(contextUrl)
                    .build();

            SerializerResult serializerResult = serializer.entity(
                    serviceMetadata, edmEntityType, entity, opts);
            InputStream serializedContent = serializerResult.getContent();

            response.setContent(serializedContent);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing entity", e);
        }
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response,
            UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) {
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response,
            UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    private EntityCollection getData(EdmEntitySet edmEntitySet) {
        EntityCollection entityCollection = new EntityCollection();

        if (edmEntitySet.getName().equals(TestEdmProvider.ES_PRODUCTS_NAME)) {
            entityCollection.getEntities().add(getProduct(1));
            entityCollection.getEntities().add(getProduct(2));
            entityCollection.getEntities().add(getProduct(3));
        }

        return entityCollection;
    }

    private Entity getProduct(int id) {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, id));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Product " + id));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                "Description for Product " + id));
        return entity;
    }
}
