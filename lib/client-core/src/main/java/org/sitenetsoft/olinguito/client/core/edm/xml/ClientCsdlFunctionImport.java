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

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlFunctionImport.FunctionImportDeserializer.class)
class ClientCsdlFunctionImport extends CsdlFunctionImport implements Serializable {

  @Serial
  private static final long serialVersionUID = -1686801084142932402L;

  static class FunctionImportDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlFunctionImport> {
    @Override
    protected ClientCsdlFunctionImport doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final ClientCsdlFunctionImport functImpImpl = new ClientCsdlFunctionImport();

      for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.getCurrentToken();
        if (token == JsonToken.FIELD_NAME) {
          if ("Name".equals(jp.currentName())) {
            functImpImpl.setName(jp.nextTextValue());
          } else if ("Function".equals(jp.currentName())) {
            functImpImpl.setFunction(jp.nextTextValue());
          } else if ("EntitySet".equals(jp.currentName())) {
            functImpImpl.setEntitySet(jp.nextTextValue());
          } else if ("IncludeInServiceDocument".equals(jp.currentName())) {
            functImpImpl.setIncludeInServiceDocument(Boolean.parseBoolean(jp.nextTextValue()));
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            functImpImpl.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return functImpImpl;
    }
  }
}
