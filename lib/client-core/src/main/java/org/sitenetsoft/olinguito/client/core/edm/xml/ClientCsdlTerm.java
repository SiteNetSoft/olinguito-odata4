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
 * Copyright 2026 SiteNetSoft - Removed commons-lang3 dependency
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Removed unnecessary boxing and modernized length checks
 */
package org.sitenetsoft.olinguito.client.core.edm.xml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;


import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@JsonDeserialize(using = ClientCsdlTerm.TermDeserializer.class)
class ClientCsdlTerm extends CsdlTerm implements Serializable {

  @Serial
  private static final long serialVersionUID = -8350072064720586186L;

  static class TermDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlTerm> {
    @Override
    protected ClientCsdlTerm doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {
      final ClientCsdlTerm term = new ClientCsdlTerm();

      for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.getCurrentToken();
        if (token == JsonToken.FIELD_NAME) {
          if ("Name".equals(jp.currentName())) {
            term.setName(jp.nextTextValue());
          } else if ("Type".equals(jp.currentName())) {
            term.setType(jp.nextTextValue());
          } else if ("BaseTerm".equals(jp.currentName())) {
            term.setBaseTerm(jp.nextTextValue());
          } else if ("DefaultValue".equals(jp.currentName())) {
            term.setDefaultValue(jp.nextTextValue());
          } else if ("Nullable".equals(jp.currentName())) {
            term.setNullable(Boolean.parseBoolean(jp.nextTextValue()));
          } else if ("MaxLength".equals(jp.currentName())) {
            final String maxLength = jp.nextTextValue();
            term.setMaxLength("max".equalsIgnoreCase(maxLength) ? Integer.MAX_VALUE : Integer.parseInt(maxLength));
          } else if ("Precision".equals(jp.currentName())) {
            term.setPrecision(Integer.parseInt(jp.nextTextValue()));
          } else if ("Scale".equals(jp.currentName())) {
            final String scale = jp.nextTextValue();
            term.setScale("variable".equalsIgnoreCase(scale) || "floating".equalsIgnoreCase(scale) ?
                0 : Integer.parseInt(scale));
          } else if ("SRID".equals(jp.currentName())) {
            final String srid = jp.nextTextValue();
            if (srid != null) {
              term.setSrid(SRID.valueOf(srid));
            }
          } else if ("AppliesTo".equals(jp.currentName())) {
            String text = jp.nextTextValue();
            term.getAppliesTo().addAll(List.of(
                text != null ? text.trim().split("\\s+") : new String[0]));
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            term.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return term;
    }
  }
}
