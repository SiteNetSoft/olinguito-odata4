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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1419: retain unknown/annotation error fields
 */
package org.sitenetsoft.olinguito.client.core.serialization;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.ex.ODataErrorDetail;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonODataErrorDetailDeserializer extends JsonDeserializer {

  private static final Set<String> KNOWN_DETAIL_FIELDS = Set.of(
      Constants.ERROR_CODE, Constants.ERROR_MESSAGE, Constants.ERROR_TARGET);

  public JsonODataErrorDetailDeserializer(final boolean serverMode) {
    super(serverMode);
  }

  protected ResWrap<ODataErrorDetail> doDeserialize(final JsonParser parser) throws IOException {

    final ODataErrorDetail error = new ODataErrorDetail();
    final JsonNode errorNode = parser.getCodec().readTree(parser);
    if (errorNode.has(Constants.ERROR_CODE)) {
      error.setCode(errorNode.get(Constants.ERROR_CODE).textValue());
    }
    if (errorNode.has(Constants.ERROR_MESSAGE)) {
      final JsonNode message = errorNode.get(Constants.ERROR_MESSAGE);
      if (message.isValueNode()) {
        error.setMessage(message.textValue());
      } else if (message.isObject()) {
        error.setMessage(message.get(Constants.VALUE).asText());
      }
    }
    if (errorNode.has(Constants.ERROR_TARGET)) {
      error.setTarget(errorNode.get(Constants.ERROR_TARGET).textValue());
    }

    final Map<String, Object> additionalProperties =
        JsonODataErrorDeserializer.collectAdditionalProperties(errorNode, parser, KNOWN_DETAIL_FIELDS);
    if (!additionalProperties.isEmpty()) {
      error.setAdditionalProperties(additionalProperties);
    }

    return new ResWrap<>(null, null, error);
  }
}
