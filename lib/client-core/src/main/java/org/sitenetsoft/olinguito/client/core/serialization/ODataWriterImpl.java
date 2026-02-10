/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Code quality improvements
 */
package org.sitenetsoft.olinguito.client.core.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import java.io.IOException;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientLink;
import org.sitenetsoft.olinguito.client.api.domain.ClientProperty;
import org.sitenetsoft.olinguito.client.api.serialization.ODataSerializer;
import org.sitenetsoft.olinguito.client.api.serialization.ODataSerializerException;
import org.sitenetsoft.olinguito.client.api.serialization.ODataWriter;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

public class ODataWriterImpl implements ODataWriter {

  protected final ODataClient client;

  public ODataWriterImpl(final ODataClient client) {
    this.client = client;
  }

  @Override
  public InputStream writeEntities(final Collection<ClientEntity> entities, final ContentType contentType)
      throws ODataSerializerException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
    try {
      final ODataSerializer serializer = client.getSerializer(contentType);
      for (ClientEntity entity : entities) {
        serializer.write(writer, client.getBinder().getEntity(entity));
      }

      return new ByteArrayInputStream(output.toByteArray());
    } finally {
      try { writer.close(); } catch (IOException ignored) { }
    }
  }

  @Override
  public InputStream writeEntity(final ClientEntity entity, final ContentType contentType)
      throws ODataSerializerException {
    return writeEntities(Collections.singleton(entity), contentType);
  }

  @Override
  public InputStream writeProperty(final ClientProperty property, final ContentType contentType)
      throws ODataSerializerException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
    try {
      client.getSerializer(contentType).write(writer, client.getBinder().getProperty(property));

      return new ByteArrayInputStream(output.toByteArray());
    } finally {
      try { writer.close(); } catch (IOException ignored) { }
    }
  }

  @Override
  public InputStream writeLink(final ClientLink link, final ContentType contentType) throws ODataSerializerException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
    try {
      client.getSerializer(contentType).write(writer, client.getBinder().getLink(link));

      return new ByteArrayInputStream(output.toByteArray());
    } finally {
      try { writer.close(); } catch (IOException ignored) { }
    }
  }

  @Override
  public InputStream writeReference(ResWrap<URI> reference, ContentType contenType) throws ODataSerializerException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
    try {
      client.getSerializer(contenType).write(writer, reference);

      return new ByteArrayInputStream(output.toByteArray());
    } finally {
      try { writer.close(); } catch (IOException ignored) { }
    }
  }
}
