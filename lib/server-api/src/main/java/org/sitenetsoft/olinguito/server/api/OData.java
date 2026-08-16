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
 * Copyright 2026 SiteNetSoft - Replaced Class.forName with ServiceLoader to eliminate circular dependency
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 2 fix round 1: additive versioned
 * createServiceMetadata overload
 */
package org.sitenetsoft.olinguito.server.api;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.debug.DebugResponseHelper;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.FixedFormatDeserializer;
import org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer;
import org.sitenetsoft.olinguito.server.api.etag.ETagHelper;
import org.sitenetsoft.olinguito.server.api.etag.ServiceMetadataETagSupport;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences;
import org.sitenetsoft.olinguito.server.api.serializer.EdmAssistedSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.EdmDeltaSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.FixedFormatSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.uri.UriHelper;

/**
 * Root object for serving factory tasks and support loose coupling of implementation (core) from the API.
 * This is not a singleton (static variables) to avoid issues with synchronization, OSGi, hot deployment and so on.
 * Each thread (request) should keep its own instance.
 */
public abstract class OData {

  /**
   * Use this method to create a new OData instance. Each thread/request should keep its own instance.
   * <p>The implementation is discovered via {@link ServiceLoader}. Exactly one provider of
   * {@code OData} must be available on the classpath (typically {@code odata-server-core}).</p>
   * @return a new OData instance
   */
  public static OData newInstance() {
    /*
     * We explicitly do not use the singleton pattern to keep the server state free
     * and avoid class loading issues also during hot deployment.
     */
    final ServiceLoader<OData> loader = ServiceLoader.load(OData.class);
    final var iterator = loader.iterator();
    if (!iterator.hasNext()) {
      throw new ODataRuntimeException("No OData implementation found on the classpath. "
          + "Ensure odata-server-core is included as a dependency.");
    }
    return iterator.next();
  }

  /**
   * Creates a new serializer object for rendering content in the specified format.
   * Serializers are used in Processor implementations.
   *
   * @param contentType any format supported by Olingo (XML, JSON ...)
   */
  public abstract ODataSerializer createSerializer(ContentType contentType) throws SerializerException;
 
  /**
   * Creates a new serializer object for rendering content in the specified format.
   * Serializers are used in Processor implementations.
   *
   * @param contentType any format supported by Olingo (XML, JSON ...)
   * @param versions any v4 version supported by Olingo (4.0, 4.01 ...)
   */
  public abstract ODataSerializer createSerializer(ContentType contentType, 
      final List<String> versions) throws SerializerException;

  /**
   * Creates a new serializer object for rendering content in a fixed format, e.g., for binary output or multipart/mixed
   * outpu.
   * Serializers are used in Processor implementations.
   */
  public abstract FixedFormatSerializer createFixedFormatSerializer();

  /**
   * Creates a new deserializer object for reading content in a fixed format, e.g., for binary input.
   * Deserializers are used in Processor implementations.
   */
  public abstract FixedFormatDeserializer createFixedFormatDeserializer();

  /**
   * Creates a new ODataRequestHandler for handling OData requests.
   *
   * @param serviceMetadata - metadata object required to handle an OData request
   */
  public abstract ODataRequestHandler createHandler(ServiceMetadata serviceMetadata);

  /**
   * Creates a new ODataHandler for handling OData requests.
   *
   * @param serviceMetadata - metadata object required to handle an OData request
   */
  public abstract ODataHandler createRawHandler(ServiceMetadata serviceMetadata);

  /**
   * Creates a metadata object for this service.
   *
   * @param edmProvider a custom or default implementation for creating metadata
   * @param references list of edmx references
   * @return a service metadata implementation
   */
  public abstract ServiceMetadata createServiceMetadata(CsdlEdmProvider edmProvider, List<EdmxReference> references);

  /**
   * Creates a metadata object for this service.
   *
   * @param edmProvider a custom or default implementation for creating metadata
   * @param references list of edmx references
   * @return a service metadata implementation
   */
  public abstract ServiceMetadata createServiceMetadata(CsdlEdmProvider edmProvider, List<EdmxReference> references,
      ServiceMetadataETagSupport serviceMetadataETagSupport);

  /**
   * Creates a metadata object for this service that additionally carries a schema version, for
   * {@link ServiceMetadata#getSchemaVersion()} / the <code>$schemaversion</code> system query option
   * (OData 4.01, Part 1: Protocol, section 11.2.12 / <code>Core.SchemaVersion</code>).
   * <p>This is a concrete, additive method with a default-throwing body so that existing {@code OData}
   * subclasses keep compiling unchanged; implementations that support versioned metadata should
   * override it.</p>
   *
   * @param edmProvider a custom or default implementation for creating metadata
   * @param references list of edmx references
   * @param serviceMetadataETagSupport ETag support for the metadata document (may be {@code null})
   * @param schemaVersion the schema version this service's data model conforms to, or {@code null}
   * if this service is unversioned
   * @return a service metadata implementation carrying the given schema version
   */
  public ServiceMetadata createServiceMetadata(CsdlEdmProvider edmProvider, List<EdmxReference> references,
      ServiceMetadataETagSupport serviceMetadataETagSupport, String schemaVersion) {
    throw new UnsupportedOperationException(
        "createServiceMetadata(CsdlEdmProvider, List, ServiceMetadataETagSupport, String) "
            + "is not supported by this OData implementation.");
  }

  /**
   * Creates a new URI helper object for performing URI-related tasks.
   * It can be used in Processor implementations.
   */
  public abstract UriHelper createUriHelper();

  /**
   * Creates a new deserializer object for reading content in the specified format.
   * Deserializers are used in Processor implementations.
   *
   * @param contentType any content type supported by Olingo (XML, JSON ...)
   */
  public abstract ODataDeserializer createDeserializer(ContentType contentType) throws DeserializerException;

  /**
   * Creates a new deserializer object for reading content in the specified format.
   * Deserializers are used in Processor implementations.
   *
   * @param contentType any content type supported by Olingo (XML, JSON ...)
   * @param metadata ServiceMetada of the service
   */
  public abstract ODataDeserializer createDeserializer(ContentType contentType,
      ServiceMetadata metadata) throws DeserializerException;
  
  /**
  * Creates a new deserializer object for reading content in the specified format.
  * Deserializers are used in Processor implementations.
    *
    * @param contentType any content type supported by Olingo (XML, JSON ...)
    * @param versions version
   */
  public abstract ODataDeserializer createDeserializer(ContentType contentType, 
      final List<String> versions) throws DeserializerException;

  /**
   * Creates a new deserializer object for reading content in the specified format.
   * Deserializers are used in Processor implementations.
   *
   * @param contentType any content type supported by Olingo (XML, JSON ...)
   * @param metadata ServiceMetada of the service
   * @param versions version
   */
  public abstract ODataDeserializer createDeserializer(ContentType contentType,
      ServiceMetadata metadata, final List<String> versions) throws DeserializerException;
  
  /**
   * Creates a primitive-type instance.
   * @param kind the kind of the primitive type
   * @return an {@link EdmPrimitiveType} instance for the type kind
   */
  public abstract EdmPrimitiveType createPrimitiveTypeInstance(EdmPrimitiveTypeKind kind);

  /**
   * Creates a new ETag helper object for performing ETag-related tasks.
   * It can be used in Processor implementations.
   */
  public abstract ETagHelper createETagHelper();

  /**
   * Creates a new Preferences object out of Prefer HTTP request headers.
   * It can be used in Processor implementations.
   */
  public abstract Preferences createPreferences(Collection<String> preferHeaders);

  /**
   * Creates a DebugResponseHelper for the given debugFormat.
   * If the format is not supported no exception is thrown.
   * Instead we give back the implementation for the JSON format.
   * @param debugFormat format to be used
   * @return a debug-response helper
   */
  public abstract DebugResponseHelper createDebugResponseHelper(String debugFormat);

  /**
   * Creates a new serializer object capable of working without EDM information
   * for rendering content in the specified format.
   * @param contentType a content type supported by Olingo
   */
  public abstract EdmAssistedSerializer createEdmAssistedSerializer(final ContentType contentType)
      throws SerializerException;
  
  /**
   * Creates a new serializer object capable of working without EDM information
   * for rendering content in the specified format.
   * @param contentType a content type supported by Olingo
   * @param versions Odata Version v4 or v4.01
   */
  public abstract EdmAssistedSerializer createEdmAssistedSerializer(final ContentType contentType, 
		  final List<String> versions) throws SerializerException;
  
  
  /**
   * Creates a new serializer object capable of working without EDM information
   * for rendering delta content in the specified format.
   * @param contentType a content type supported by Olingo
   * @param versions versions supported by Olingo
   */
  public abstract EdmDeltaSerializer createEdmDeltaSerializer(final ContentType contentType,
      final List<String> versions) throws SerializerException;
}
