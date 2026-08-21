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
 * Copyright 2026 SiteNetSoft - Added test for the streamed collection serializer defaults
 */
package org.sitenetsoft.olinguito.server.core.serializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.data.AbstractEntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityIterator;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.PropertyIterator;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.ComplexSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.EntityCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.EntitySerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ReferenceCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ReferenceSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerResult;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;

class PropertyStreamedDefaultsTest {

  /** A PropertyIterator over a fixed list, used by this test and reused by Task 2's tests. */
  static PropertyIterator iteratorOver(final String name, final ValueType valueType, final List<?> values) {
    final Iterator<?> delegate = values.iterator();
    final PropertyIterator iterator = new PropertyIterator() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public Property next() {
        final Object value = delegate.next();
        return new Property(null, name, elementValueType(valueType), value);
      }
    };
    iterator.setName(name);
    iterator.setValueType(valueType);
    return iterator;
  }

  private static ValueType elementValueType(final ValueType collectionValueType) {
    switch (collectionValueType) {
    case COLLECTION_PRIMITIVE: return ValueType.PRIMITIVE;
    case COLLECTION_ENUM: return ValueType.ENUM;
    case COLLECTION_GEOSPATIAL: return ValueType.GEOSPATIAL;
    case COLLECTION_COMPLEX: return ValueType.COMPLEX;
    default: throw new IllegalArgumentException(collectionValueType.name());
    }
  }

  @Test
  void serializerWithoutStreamedSupportReportsNotImplemented() {
    final MinimalSerializer serializer = new MinimalSerializer();

    final SerializerException primitive = assertThrows(SerializerException.class,
        () -> serializer.primitiveCollectionStreamed(null, null,
            iteratorOver("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, List.of("a")), null));
    assertEquals(SerializerException.MessageKeys.NOT_IMPLEMENTED, primitive.getMessageKey());

    final SerializerException complex = assertThrows(SerializerException.class,
        () -> serializer.complexCollectionStreamed(null, null,
            iteratorOver("CollPropertyComp", ValueType.COLLECTION_COMPLEX, List.of()), null));
    assertEquals(SerializerException.MessageKeys.NOT_IMPLEMENTED, complex.getMessageKey());
  }

  @Test
  void propertyIteratorCarriesCollectionMetadata() {
    final PropertyIterator iterator =
        iteratorOver("CollPropertyString", ValueType.COLLECTION_PRIMITIVE, List.of("a", "b"));
    iterator.setCount(2);
    iterator.setNext(URI.create("http://host/svc/next"));

    assertEquals("CollPropertyString", iterator.getName());
    assertEquals(ValueType.COLLECTION_PRIMITIVE, iterator.getValueType());
    assertEquals(Integer.valueOf(2), iterator.getCount());
    assertEquals(URI.create("http://host/svc/next"), iterator.getNext());
    assertEquals("a", iterator.next().getValue());
    assertEquals("b", iterator.next().getValue());
  }

  /** Implements only the methods ODataSerializer declared before Wave 3 — nothing else. */
  static final class MinimalSerializer implements org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer {
    private static UnsupportedOperationException no() {
      return new UnsupportedOperationException("not part of this test");
    }

    @Override
    public SerializerResult serviceDocument(final ServiceMetadata serviceMetadata, final String serviceRoot) {
      throw no();
    }

    @Override
    public SerializerResult metadataDocument(final ServiceMetadata serviceMetadata) {
      throw no();
    }

    @Override
    public SerializerResult error(final ODataServerError error) {
      throw no();
    }

    @Override
    public SerializerResult entityCollection(final ServiceMetadata metadata, final EdmEntityType entityType,
        final AbstractEntityCollection entitySet, final EntityCollectionSerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerStreamResult entityCollectionStreamed(final ServiceMetadata metadata,
        final EdmEntityType entityType, final EntityIterator entities,
        final EntityCollectionSerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerResult entity(final ServiceMetadata metadata, final EdmEntityType entityType,
        final Entity entity, final EntitySerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerResult primitive(final ServiceMetadata metadata, final EdmPrimitiveType type,
        final Property property, final PrimitiveSerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerResult complex(final ServiceMetadata metadata, final EdmComplexType type,
        final Property property, final ComplexSerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerResult primitiveCollection(final ServiceMetadata metadata, final EdmPrimitiveType type,
        final Property property, final PrimitiveSerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerResult complexCollection(final ServiceMetadata metadata, final EdmComplexType type,
        final Property property, final ComplexSerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerResult reference(final ServiceMetadata metadata, final EdmEntitySet edmEntitySet,
        final Entity entity, final ReferenceSerializerOptions options) {
      throw no();
    }

    @Override
    public SerializerResult referenceCollection(final ServiceMetadata metadata, final EdmEntitySet edmEntitySet,
        final AbstractEntityCollection entityCollection, final ReferenceCollectionSerializerOptions options) {
      throw no();
    }
  }
}
