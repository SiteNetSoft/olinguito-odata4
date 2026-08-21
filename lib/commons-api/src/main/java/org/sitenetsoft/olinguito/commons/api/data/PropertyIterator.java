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
 * Copyright 2026 SiteNetSoft - Added the streamed property-collection iterator
 */
package org.sitenetsoft.olinguito.commons.api.data;

import java.net.URI;
import java.util.Iterator;

import org.sitenetsoft.olinguito.commons.api.ex.ODataNotSupportedException;

/**
 * Iterator over the elements of a single collection-valued property, for use with the streamed
 * serializer entry points {@code ODataSerializer.primitiveCollectionStreamed} and
 * {@code complexCollectionStreamed}.
 *
 * <p>This is the property-level counterpart of {@link EntityIterator}: it carries the metadata the
 * collection as a whole needs ({@link #getName()}, {@link #getValueType()}, {@link #getCount()},
 * {@link #getNext()}), and yields one {@link Property} per element, each holding a single element
 * value. {@link #getValueType()} reports the value type of the collection
 * ({@code COLLECTION_PRIMITIVE}, {@code COLLECTION_ENUM}, {@code COLLECTION_GEOSPATIAL} or
 * {@code COLLECTION_COMPLEX}), exactly as a buffered collection-valued {@link Property} does.</p>
 */
public abstract class PropertyIterator implements Iterator<Property> {

  private String name;

  private ValueType valueType;

  private URI next;

  private Integer count;

  @Override
  public abstract boolean hasNext();

  /**
   * {@inheritDoc}
   * <p/>
   * Which is one element of the collection, wrapped in a {@link Property}, for this type of
   * iterator.
   */
  @Override
  public abstract Property next();

  /**
   * {@inheritDoc}
   * <p/>
   * <b>ATTENTION:</b> <code>remove</code> is not supported by default.
   */
  @Override
  public void remove() {
    throw new ODataNotSupportedException("Property Iterator does not support remove()");
  }

  /** Gets the name of the collection-valued property. */
  public String getName() {
    return name;
  }

  /** Sets the name of the collection-valued property. */
  public void setName(final String name) {
    this.name = name;
  }

  /** Gets the value type of the collection as a whole. */
  public ValueType getValueType() {
    return valueType;
  }

  /** Sets the value type of the collection as a whole. */
  public void setValueType(final ValueType valueType) {
    this.valueType = valueType;
  }

  /**
   * Gets the inline count.
   *
   * <p>Note: the inline count is limited to {@code Integer.MAX_VALUE}. The OData specification
   * defines {@code @odata.count} as {@code Edm.Int64}; widening this to {@code long} is deferred
   * (OLINGO-1540) because it would be a breaking public-API change.</p>
   */
  public Integer getCount() {
    return count;
  }

  /** Sets the inline count. */
  public void setCount(final Integer count) {
    this.count = count;
  }

  /** Gets the next link. */
  public URI getNext() {
    return next;
  }

  /** Sets the next link. */
  public void setNext(final URI next) {
    this.next = next;
  }
}
