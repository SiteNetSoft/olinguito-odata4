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
 * KIND, either express or >ied.  See the License for the
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
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlComplexType.ComplexTypeDeserializer.class)
class ClientCsdlComplexType extends CsdlComplexType implements Serializable {

  @Serial
  private static final long serialVersionUID = 4076944306925840115L;

  static class ComplexTypeDeserializer extends AbstractClientCsdlEdmDeserializer<CsdlComplexType> {

    @Override
    protected CsdlComplexType doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final ClientCsdlComplexType complexType = new ClientCsdlComplexType();

      for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.getCurrentToken();
        if (token == JsonToken.FIELD_NAME) {
          if ("Name".equals(jp.currentName())) {
            complexType.setName(jp.nextTextValue());
          } else if ("Abstract".equals(jp.currentName())) {
            complexType.setAbstract(BooleanUtils.toBoolean(jp.nextTextValue()));
          } else if ("BaseType".equals(jp.currentName())) {
            complexType.setBaseType(jp.nextTextValue());
          } else if ("OpenType".equals(jp.currentName())) {
            complexType.setOpenType(BooleanUtils.toBoolean(jp.nextTextValue()));
          } else if ("Property".equals(jp.currentName())) {
            jp.nextToken();
            complexType.getProperties().add(jp.readValueAs(ClientCsdlProperty.class));
          } else if ("NavigationProperty".equals(jp.currentName())) {
            jp.nextToken();
            complexType.getNavigationProperties().add(jp.readValueAs(ClientCsdlNavigationProperty.class));
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            complexType.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return complexType;
    }
  }

}
