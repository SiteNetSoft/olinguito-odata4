/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2026 SiteNetSoft - Code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 */
package org.sitenetsoft.olinguito.client.core.communication.request;

import org.apache.commons.lang3.StringUtils;
import org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpRequest;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.core.communication.header.ODataErrorResponseChecker;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRequest {

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractRequest.class);

  protected void checkRequest(final ODataClient odataClient, final ODataHttpRequest request) {
    // If using an Edm enabled client, checks that the cached service root matches the request URI
    if (odataClient instanceof EdmEnabledODataClient
            && !request.getURI().toASCIIString().startsWith(
                    ((EdmEnabledODataClient) odataClient).getServiceRoot())) {

      throw new IllegalArgumentException(
              String.format("The current request URI %s does not match the configured service root %s",
                      request.getURI().toASCIIString(),
                      ((EdmEnabledODataClient) odataClient).getServiceRoot()));
    }
  }

  protected void checkResponse(
          final ODataClient odataClient, final ODataHttpResponse response, final String accept) {

    if (response.getStatusCode() >= 400) {
      final ContentType contentType = determineContentType(response, accept);
      final ODataRuntimeException exception = ODataErrorResponseChecker.checkResponse(
              odataClient,
              response.getStatusCode(),
              response.getReasonPhrase(),
              response.getBody(),
              contentType);
      if (exception instanceof ODataClientErrorException clientError) {
        clientError.setHeaderInfo(response.getHeaders());
      }
      throw exception;
    }
  }

  private static ContentType determineContentType(ODataHttpResponse response, String accept) {
    final String ct = response.getContentType();
    if (ct == null || StringUtils.isBlank(ct)) {
      return ContentType.fromAcceptHeader(accept);
    }
    try {
      return ContentType.create(ct);
    } catch (Exception exception) {
      return ContentType.JSON;
    }
  }

}
