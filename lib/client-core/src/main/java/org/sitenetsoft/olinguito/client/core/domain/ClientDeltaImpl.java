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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced manual hashCode with Objects.hash()
 */
package org.sitenetsoft.olinguito.client.core.domain;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sitenetsoft.olinguito.client.api.domain.ClientDeletedEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientDelta;
import org.sitenetsoft.olinguito.client.api.domain.ClientDeltaLink;

public class ClientDeltaImpl extends ClientEntitySetImpl implements ClientDelta {

  private final List<ClientDeletedEntity> deletedEntities = new ArrayList<>();

  private final List<ClientDeltaLink> addedLinks = new ArrayList<>();

  private final List<ClientDeltaLink> deletedLinks = new ArrayList<>();

  public ClientDeltaImpl() {
    super();
  }

  public ClientDeltaImpl(final URI next) {
    super(next);
  }

  @Override
  public List<ClientDeletedEntity> getDeletedEntities() {
    return deletedEntities;
  }

  @Override
  public List<ClientDeltaLink> getAddedLinks() {
    return addedLinks;
  }

  @Override
  public List<ClientDeltaLink> getDeletedLinks() {
    return deletedLinks;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), addedLinks, deletedEntities, deletedLinks);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof ClientDeltaImpl other)) {
      return false;
    }
      if (!addedLinks.equals(other.addedLinks)) {
        return false;
      }
      if (!deletedEntities.equals(other.deletedEntities)) {
        return false;
      }
      return deletedLinks.equals(other.deletedLinks);
  }

  @Override
  public String toString() {
    return "ClientDeltaImpl [deletedEntities=" + deletedEntities + ", addedLinks=" + addedLinks + ", deletedLinks="
        + deletedLinks + "super[" + super.toString() + "]]";
  }
}
