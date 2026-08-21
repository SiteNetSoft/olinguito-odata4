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
 * Copyright 2026 SiteNetSoft - Added streamed primitive and complex collection serialization
 */
package org.sitenetsoft.olinguito.server.api.serializer;

import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.AbstractEntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.EntityIterator;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.PropertyIterator;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;

/** OData serializer */
public interface ODataSerializer {

  /** The default character set is UTF-8. */
  public static final String DEFAULT_CHARSET =  Constants.UTF8;

  /**
   * Writes the service document into an InputStream.
   * @param serviceMetadata the metadata information for the service
   * @param serviceRoot the service-root URI of this OData service
   */
  SerializerResult serviceDocument(ServiceMetadata serviceMetadata, String serviceRoot) throws SerializerException;

  /**
   * Writes the metadata document into an InputStream.
   * @param serviceMetadata the metadata information for the service
   */
  SerializerResult metadataDocument(ServiceMetadata serviceMetadata) throws SerializerException;

  /**
   * Writes an ODataError into an InputStream.
   * @param error the main error
   */
  SerializerResult error(ODataServerError error) throws SerializerException;

  /**
   * Writes entity-collection data into an InputStream.
   * @param metadata metadata for the service
   * @param entityType the {@link EdmEntityType}
   * @param entitySet the data of the entity set
   * @param options options for the serializer
   */
  SerializerResult entityCollection(ServiceMetadata metadata, EdmEntityType entityType,
      AbstractEntityCollection entitySet, EntityCollectionSerializerOptions options) throws SerializerException;

  /**
   * Writes entity-collection data into an InputStream.
   * @param metadata metadata for the service
   * @param entityType the {@link EdmEntityType}
   * @param entities the data of the entity set
   * @param options options for the serializer
   */
  SerializerStreamResult entityCollectionStreamed(ServiceMetadata metadata, EdmEntityType entityType,
      EntityIterator entities, EntityCollectionSerializerOptions options) throws SerializerException;

  /**
   * Writes collection-of-primitive data into an {@link org.sitenetsoft.olinguito.server.api.ODataContent}
   * that is serialized directly onto the response channel instead of being buffered in memory.
   *
   * <p>The payload is byte-for-byte the payload
   * {@link #primitiveCollection(ServiceMetadata, EdmPrimitiveType, Property, PrimitiveSerializerOptions)}
   * produces for the same data; only the delivery differs. The ordering it emits — context control
   * information first, then the count, then the value — is the one [OData-JSON] section 4.4
   * requires of a streaming payload.</p>
   *
   * <p>This method is a {@code default} so that implementations written before it existed keep
   * compiling; such an implementation reports {@link SerializerException.MessageKeys#NOT_IMPLEMENTED}.</p>
   *
   * @param metadata metadata of the service
   * @param type primitive type of the collection's elements
   * @param properties iterator over the collection's elements
   * @param options options for the serializer
   * @return serializer stream result
   * @throws SerializerException if serialization fails, or if this serializer does not implement
   *         streamed primitive collections
   */
  default SerializerStreamResult primitiveCollectionStreamed(final ServiceMetadata metadata,
      final EdmPrimitiveType type, final PropertyIterator properties,
      final PrimitiveSerializerOptions options) throws SerializerException {
    throw new SerializerException("Streamed serialization of a primitive collection is not implemented "
        + "by " + getClass().getName() + ".", SerializerException.MessageKeys.NOT_IMPLEMENTED);
  }

  /**
   * Writes collection-of-complex data into an {@link org.sitenetsoft.olinguito.server.api.ODataContent}
   * that is serialized directly onto the response channel instead of being buffered in memory.
   *
   * <p>The payload is byte-for-byte the payload
   * {@link #complexCollection(ServiceMetadata, EdmComplexType, Property, ComplexSerializerOptions)}
   * produces for the same data; only the delivery differs.</p>
   *
   * <p>This method is a {@code default} so that implementations written before it existed keep
   * compiling; such an implementation reports {@link SerializerException.MessageKeys#NOT_IMPLEMENTED}.</p>
   *
   * @param metadata metadata of the service
   * @param type complex type of the collection's elements
   * @param properties iterator over the collection's elements
   * @param options options for the serializer
   * @return serializer stream result
   * @throws SerializerException if serialization fails, or if this serializer does not implement
   *         streamed complex collections
   */
  default SerializerStreamResult complexCollectionStreamed(final ServiceMetadata metadata,
      final EdmComplexType type, final PropertyIterator properties,
      final ComplexSerializerOptions options) throws SerializerException {
    throw new SerializerException("Streamed serialization of a complex collection is not implemented "
        + "by " + getClass().getName() + ".", SerializerException.MessageKeys.NOT_IMPLEMENTED);
  }

  /**
   * Writes entity data into an InputStream.
   * @param metadata metadata for the service
   * @param entityType the {@link EdmEntityType}
   * @param entity the data of the entity
   * @param options options for the serializer
   */
  SerializerResult entity(ServiceMetadata metadata, EdmEntityType entityType, Entity entity,
      EntitySerializerOptions options) throws SerializerException;

  /**
   * Writes primitive-type instance data into an InputStream.
   * @param metadata metadata for the service
   * @param type primitive type
   * @param property property value
   * @param options options for the serializer
   */
  SerializerResult primitive(ServiceMetadata metadata, EdmPrimitiveType type, Property property,
      PrimitiveSerializerOptions options) throws SerializerException;

  /**
   * Writes complex-type instance data into an InputStream.
   * @param metadata metadata for the service
   * @param type complex type
   * @param property property value
   * @param options options for the serializer
   */
  SerializerResult complex(ServiceMetadata metadata, EdmComplexType type, Property property,
      ComplexSerializerOptions options) throws SerializerException;

  /**
   * Writes data of a collection of primitive-type instances into an InputStream.
   * @param metadata metadata for the service
   * @param type primitive type
   * @param property property value
   * @param options options for the serializer
   */
  SerializerResult primitiveCollection(ServiceMetadata metadata, EdmPrimitiveType type, Property property,
      PrimitiveSerializerOptions options) throws SerializerException;

  /**
   * Writes data of a collection of complex-type instances into an InputStream.
   * @param metadata metadata for the service
   * @param type complex type
   * @param property property value
   * @param options options for the serializer
   */
  SerializerResult complexCollection(ServiceMetadata metadata, EdmComplexType type, Property property,
      ComplexSerializerOptions options) throws SerializerException;

  /**
   * Writes a single entity reference into an InputStream.
   * @param metadata metadata for the service
   * @param edmEntitySet {@link EdmEntitySet}
   * @param entity data of the entity
   * @param options {@link ReferenceSerializerOptions}
   */
  SerializerResult reference(ServiceMetadata metadata, EdmEntitySet edmEntitySet, Entity entity,
      ReferenceSerializerOptions options) throws SerializerException;

  /**
   * Writes entity-collection references into an InputStream.
   * @param metadata metadata for the service
   * @param edmEntitySet {@link EdmEntitySet}
   * @param entityCollection data of the entity collection
   * @param options {@link ReferenceCollectionSerializerOptions}
   */
  SerializerResult referenceCollection(ServiceMetadata metadata, EdmEntitySet edmEntitySet,
      AbstractEntityCollection entityCollection, ReferenceCollectionSerializerOptions options)
      throws SerializerException;
}
