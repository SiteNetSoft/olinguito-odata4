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
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 1: add dynamicProperty entry point
 */
package org.sitenetsoft.olinguito.server.api.deserializer;

import java.io.InputStream;

import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;

/**
 * Deserializer on OData server side.
 */
public interface ODataDeserializer {

  /**
   * Deserializes an entity stream into an {@link Entity Entity} object.
   * Validates: property types, no double properties, correct json types.
   * Returns a deserialized {@link Entity Entity} object and an
   * {@link org.sitenetsoft.olinguito.server.api.uri.queryoption.ExpandOption ExpandOption} object.
   * @param stream
   * @param edmEntityType
   * @return {@link DeserializerResult#getEntity()} and {@link DeserializerResult#getExpandTree()}
   * @throws DeserializerException
   */
  DeserializerResult entity(InputStream stream, EdmEntityType edmEntityType) throws DeserializerException;

  /**
   * Deserializes an entity collection stream into an {@link EntityCollection
   * EntityCollection} object.
   * @param stream
   * @param edmEntityType
   * @return {@link DeserializerResult#getEntityCollection()}
   * @throws DeserializerException
   */
  DeserializerResult entityCollection(InputStream stream, EdmEntityType edmEntityType) throws DeserializerException;

  /**
   * Deserializes an action-parameters stream into a map of key/value pairs.
   * Validates: parameter types, no double parameters, correct json types.
   * @param stream
   * @param edmAction
   * @return {@link DeserializerResult#getActionParameters()}
   * @throws DeserializerException
   */
  DeserializerResult actionParameters(InputStream stream, EdmAction edmAction) throws DeserializerException;

  /**
   * Deserializes the Property or collections of properties (primitive & complex).
   * @param stream
   * @param edmProperty
   * @return {@link DeserializerResult#getProperty()}
   * @throws DeserializerException
   */
  DeserializerResult property(InputStream stream, EdmProperty edmProperty) throws DeserializerException;

  /**
   * Deserializes a single, schema-less dynamic (OpenType) property from a standard
   * property-payload document (the same <code>{"value": ...}</code> shape accepted by
   * {@link #property(InputStream, EdmProperty)}, plus an optional
   * <code>value@odata.type</code> sibling annotation used to resolve the property's type
   * since, unlike {@link #property(InputStream, EdmProperty)}, no {@link EdmProperty} is
   * available to supply it). The resulting {@link org.sitenetsoft.olinguito.commons.api.data.Property
   * Property} is named {@code propertyName}.
   * <p>
   * The default implementation always throws, so deserializers that do not support dynamic
   * properties (e.g. non-JSON formats) need not override this method.
   * @param stream the payload stream
   * @param propertyName the name to give the deserialized dynamic property
   * @return {@link DeserializerResult#getProperty()}
   * @throws DeserializerException if the payload is invalid, or dynamic property
   *           deserialization is not supported by this deserializer
   */
  default DeserializerResult dynamicProperty(InputStream stream, String propertyName)
      throws DeserializerException {
    throw new DeserializerException("Dynamic property deserialization is not supported by this deserializer.",
        DeserializerException.MessageKeys.NOT_IMPLEMENTED);
  }

  /**
   * Reads entity references from the provided document.
   * @param stream
   * @param keys
   * @return {@link DeserializerResult#getEntityReferences()}
   * @throws DeserializerException
   */
  DeserializerResult entityReferences(InputStream stream) throws DeserializerException;
}
