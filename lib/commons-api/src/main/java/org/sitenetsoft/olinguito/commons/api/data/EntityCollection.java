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
 * Copyright 2026 SiteNetSoft - Modernized equals/hashCode with Objects utility methods
 * Copyright 2026 SiteNetSoft - OLINGO-1540: document Int32 limit of the inline count (deferred)
 */
package org.sitenetsoft.olinguito.commons.api.data;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Data representation for a collection of single entities.
 */
public class EntityCollection extends AbstractEntityCollection {

  private final List<Entity> entities = new ArrayList<>();
  private Integer count;
  private URI next;
  private URI deltaLink;
  private final List<Operation> operations = new ArrayList<>();
  
  /**
   * Sets number of entries.
   *
   * @param count number of entries
   */
  public void setCount(final Integer count) {
    this.count = count;
  }

  /**
   * Gets number of entries - if it was required.
   *
   * <p>Note: the inline count is limited to {@code Integer.MAX_VALUE}. The OData specification
   * defines {@code @odata.count} as {@code Edm.Int64}; widening this to {@code long} is deferred
   * (OLINGO-1540) because it would be a breaking public-API change.</p>
   *
   * @return number of entries into the entity set.
   */
  @Override
  public Integer getCount() {
    return count;
  }

  /**
   * Gets entities.
   *
   * @return entries.
   */
  public List<Entity> getEntities() {
    return entities;
  }

  /**
   * Sets next link.
   *
   * @param next next link.
   */
  public void setNext(final URI next) {
    this.next = next;
  }

  /**
   * Gets next link if exists.
   *
   * @return next link if exists; null otherwise.
   */
  @Override
  public URI getNext() {
    return next;
  }

  /**
   * Gets delta link if exists.
   *
   * @return delta link if exists; null otherwise.
   */
  @Override
  public URI getDeltaLink() {
    return deltaLink;
  }

  /**
   * Sets delta link.
   *
   * @param deltaLink delta link.
   */
  public void setDeltaLink(final URI deltaLink) {
    this.deltaLink = deltaLink;
  }
  
  /**
   * Gets operations.
   *
   * @return operations.
   */
  @Override
  public List<Operation> getOperations() {
    return operations;
  }  

  @Override
  public Iterator<Entity> iterator() {
    return this.entities.iterator();
  }

  @Override
  public boolean equals(final Object o) {
    if (!super.equals(o)) {
      return false;
    }
    final EntityCollection other = (EntityCollection) o;
    return entities.equals(other.entities)
        && Objects.equals(count, other.count)
        && Objects.equals(next, other.next)
        && Objects.equals(deltaLink, other.deltaLink);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + entities.hashCode();
    result = 31 * result + (count == null ? 0 : count.hashCode());
    result = 31 * result + (next == null ? 0 : next.hashCode());
    result = 31 * result + (deltaLink == null ? 0 : deltaLink.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return entities.toString();
  }
}
