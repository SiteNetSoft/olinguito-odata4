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
 * Copyright 2026 SiteNetSoft - Fixed deprecated API usages
 * Copyright 2026 SiteNetSoft - Replaced Apache StatusLine with statusCode/reasonPhrase
 */
package org.sitenetsoft.olinguito.client.core.communication.header;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.communication.ODataServerErrorException;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.commons.api.ex.ODataError;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ODataErrorResponseChecker {

  protected static final Logger LOG = LoggerFactory.getLogger(ODataErrorResponseChecker.class);

  private static ODataError getGenericError(final int code, final String errorMsg) {
    final ODataError error = new ODataError();
    error.setCode(String.valueOf(code));
    error.setMessage(errorMsg);
    return error;
  }

  public static ODataRuntimeException checkResponse(
      final ODataClient odataClient, final int statusCode, final String reasonPhrase,
      final InputStream entity, final ContentType contentType) {

    ODataRuntimeException result;
    InputStream entityForException = null;

    if (entity == null) {
      result = new ODataClientErrorException(statusCode, reasonPhrase);
    } else {

      ODataError error = new ODataError();
      if (!contentType.isCompatible(ContentType.TEXT_PLAIN)) {
        try {
          byte[] bytes = entity.readAllBytes();
          entityForException = new ByteArrayInputStream(bytes);
          error = odataClient.getReader().readError(new ByteArrayInputStream(bytes), contentType);
          if (error != null) {
            Map<String, String> innerError = error.getInnerError();
            if (innerError != null) {
              if (innerError.get("internalexception") != null) {
                error.setMessage(error.getMessage() + innerError.get("internalexception"));
              } else {
                error.setMessage(error.getMessage() + innerError.get("message"));
              }
            }
          }
        } catch (final RuntimeException | ODataDeserializerException | IOException e) {
          LOG.warn("Error deserializing error response", e);
          error = getGenericError(statusCode, reasonPhrase);
        }
      } else {
        error.setCode(String.valueOf(statusCode));
        error.setTarget(reasonPhrase);
        try {
          error.setMessage(new String(entity.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
          LOG.warn("Error deserializing error response", e);
          error = getGenericError(statusCode, reasonPhrase);
        }
      }

      if (statusCode >= 500 && error != null &&
          (error.getDetails() == null || error.getDetails().isEmpty()) &&
          (error.getInnerError() == null || error.getInnerError().isEmpty())) {
        result = new ODataServerErrorException(statusCode, reasonPhrase, entityForException);
      } else {
        result = new ODataClientErrorException(statusCode, reasonPhrase, error, entityForException);
      }
    }

    return result;
  }

  private ODataErrorResponseChecker() {
    // private constructor for static utility class
  }
}
