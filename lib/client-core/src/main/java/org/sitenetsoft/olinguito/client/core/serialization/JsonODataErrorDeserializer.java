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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1419: retain unknown/annotation error fields
 */
package org.sitenetsoft.olinguito.client.core.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.ex.ODataError;
import org.sitenetsoft.olinguito.commons.api.ex.ODataErrorDetail;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonODataErrorDeserializer extends JsonDeserializer {

  static final Set<String> KNOWN_ERROR_FIELDS = Set.of(
      Constants.ERROR_CODE, Constants.ERROR_MESSAGE, Constants.ERROR_TARGET,
      Constants.ERROR_DETAILS, Constants.ERROR_INNERERROR);

  public JsonODataErrorDeserializer(final boolean serverMode) {
    super(serverMode);
  }

  /**
   * Collects every field of the given error node whose name is not one of the standard OData
   * error members into a map, preserving structure. This keeps instance annotations (such as
   * Redfish's {@code @Message.ExtendedInfo}) and other vendor extensions that would otherwise
   * be silently dropped.
   */
  static Map<String, Object> collectAdditionalProperties(final JsonNode node, final JsonParser parser,
      final Set<String> knownFields) throws IOException {
    final Map<String, Object> additionalProperties = new LinkedHashMap<>();
    for (final Iterator<String> itor = node.fieldNames(); itor.hasNext();) {
      final String name = itor.next();
      if (!knownFields.contains(name)) {
        additionalProperties.put(name,
            node.get(name).traverse(parser.getCodec()).readValueAs(Object.class));
      }
    }
    return additionalProperties;
  }

  protected ODataError doDeserialize(final JsonParser parser) throws IOException {

    final ODataError error = new ODataError();

    final ObjectNode tree = parser.getCodec().readTree(parser);
    if (tree.has(Constants.JSON_ERROR)) {
      final JsonNode errorNode = tree.get(Constants.JSON_ERROR);

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
      if (errorNode.hasNonNull(Constants.ERROR_DETAILS)) {
        List<ODataErrorDetail> details = new ArrayList<>();
        JsonODataErrorDetailDeserializer detailDeserializer = new JsonODataErrorDetailDeserializer(serverMode);
        for (JsonNode jsonNode : errorNode.get(Constants.ERROR_DETAILS)) {
          details.add(detailDeserializer.doDeserialize(jsonNode.traverse(parser.getCodec()))
              .getPayload());
        }

        error.setDetails(details);
      }
      if (errorNode.hasNonNull(Constants.ERROR_INNERERROR)) {
        HashMap<String, String> innerErrorMap = new HashMap<>();
        final JsonNode innerError = errorNode.get(Constants.ERROR_INNERERROR);
        for (final Iterator<String> itor = innerError.fieldNames(); itor.hasNext();) {
          final String keyTmp = itor.next();
          final String val = innerError.get(keyTmp).toString();
          innerErrorMap.put(keyTmp, val);
        }
        error.setInnerError(innerErrorMap);
      }

      final Map<String, Object> additionalProperties =
          collectAdditionalProperties(errorNode, parser, KNOWN_ERROR_FIELDS);
      if (!additionalProperties.isEmpty()) {
        error.setAdditionalProperties(additionalProperties);
      }
    }

    return error;
  }
}
