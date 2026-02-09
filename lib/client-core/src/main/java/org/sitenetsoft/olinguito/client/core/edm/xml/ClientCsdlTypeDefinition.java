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
package org.sitenetsoft.olinguito.client.core.edm.xml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;

import org.apache.commons.lang3.BooleanUtils;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlTypeDefinition.TypeDefinitionDeserializer.class)
class ClientCsdlTypeDefinition extends CsdlTypeDefinition implements Serializable {

  @Serial
  private static final long serialVersionUID = -902407149079419602L;

  static class TypeDefinitionDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlTypeDefinition> {
    @Override
    protected ClientCsdlTypeDefinition doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {
      final ClientCsdlTypeDefinition typeDefinition = new ClientCsdlTypeDefinition();

      for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.getCurrentToken();
        if (token == JsonToken.FIELD_NAME) {
          if ("Name".equals(jp.currentName())) {
            typeDefinition.setName(jp.nextTextValue());
          } else if ("UnderlyingType".equals(jp.currentName())) {
            typeDefinition.setUnderlyingType(jp.nextTextValue());
          } else if ("MaxLength".equals(jp.currentName())) {
            typeDefinition.setMaxLength(jp.nextIntValue(0));
          } else if ("Unicode".equals(jp.currentName())) {
            typeDefinition.setUnicode(BooleanUtils.toBoolean(jp.nextTextValue()));
          } else if ("Precision".equals(jp.currentName())) {
            typeDefinition.setPrecision(jp.nextIntValue(0));
          } else if ("Scale".equals(jp.currentName())) {
            final String scale = jp.nextTextValue();
            typeDefinition.setScale("variable".equalsIgnoreCase(scale) || "floating".equalsIgnoreCase(scale) ?
                0 : Integer.parseInt(scale));
          } else if ("SRID".equals(jp.currentName())) {
            final String srid = jp.nextTextValue();
            if (srid != null) {
              typeDefinition.setSrid(SRID.valueOf(srid));
            }
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            typeDefinition.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return typeDefinition;
    }
  }
}
