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
 */
package org.sitenetsoft.olinguito.client.core.edm.xml.annotation;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

import org.sitenetsoft.olinguito.client.core.edm.xml.AbstractClientCsdlEdmDeserializer;
import org.sitenetsoft.olinguito.client.core.edm.xml.ClientCsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ClientCsdlRecord.RecordDeserializer.class)
class ClientCsdlRecord extends CsdlRecord implements Serializable {

  @Serial
  private static final long serialVersionUID = 4275271751615410709L;

  static class RecordDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlRecord> {
    @Override
    protected ClientCsdlRecord doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
        throws IOException {
      final ClientCsdlRecord record = new ClientCsdlRecord();
      for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.getCurrentToken();
        if (token == JsonToken.FIELD_NAME) {
          if ("Type".equals(jp.currentName())) {
            record.setType(jp.nextTextValue());
          } else if ("Annotation".equals(jp.currentName())) {
			jp.nextToken();
            record.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          } else if ("PropertyValue".equals(jp.currentName())) {
			jp.nextToken();
            record.getPropertyValues().add(jp.readValueAs(ClientCsdlPropertyValue.class));
          }
        }
      }
      return record;
    }
  }
}
