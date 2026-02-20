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
 * Copyright 2026 SiteNetSoft - Modernized equals/hashCode with Objects utility methods
 */
package org.sitenetsoft.olinguito.client.core.domain;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sitenetsoft.olinguito.client.api.domain.ClientAnnotation;
import org.sitenetsoft.olinguito.client.api.domain.ClientDeltaLink;
import org.sitenetsoft.olinguito.client.api.domain.ClientItem;

public class ClientDeltaLinkImpl extends ClientItem implements ClientDeltaLink {

  private URI source;

  private String relationship;

  private URI target;

  private final List<ClientAnnotation> annotations = new ArrayList<>();

  public ClientDeltaLinkImpl() {
    super(null);
  }

  @Override
  public URI getSource() {
    return source;
  }

  @Override
  public void setSource(final URI source) {
    this.source = source;
  }

  @Override
  public String getRelationship() {
    return relationship;
  }

  @Override
  public void setRelationship(final String relationship) {
    this.relationship = relationship;
  }

  @Override
  public URI getTarget() {
    return target;
  }

  @Override
  public void setTarget(final URI target) {
    this.target = target;
  }

  @Override
  public List<ClientAnnotation> getAnnotations() {
    return annotations;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), annotations, relationship, source, target);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof ClientDeltaLinkImpl other)) {
      return false;
    }
    return annotations.equals(other.annotations)
        && Objects.equals(relationship, other.relationship)
        && Objects.equals(source, other.source)
        && Objects.equals(target, other.target);
  }

  @Override
  public String toString() {
    return "ClientDeltaLinkImpl [source=" + source + ", relationship=" + relationship + ", target=" + target
        + ", annotations=" + annotations + "super[" + super.toString() + "]]";
  }

}
