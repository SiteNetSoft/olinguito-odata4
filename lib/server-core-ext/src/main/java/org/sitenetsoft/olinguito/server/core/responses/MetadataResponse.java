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
 */
package org.sitenetsoft.olinguito.server.core.responses;

import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.ContentNegotiatorException;
import org.sitenetsoft.olinguito.server.core.ServiceRequest;

public class MetadataResponse extends ServiceResponse {
  private final ODataSerializer serializer;
  private final ContentType responseContentType;

  public static MetadataResponse getInstance(ServiceRequest request,
      ODataResponse response) throws ContentNegotiatorException, SerializerException {
    return new MetadataResponse(request.getServiceMetaData(), response, request.getSerializer(),
        request.getResponseContentType(), request.getPreferences());
  }

  private MetadataResponse(ServiceMetadata metadata, ODataResponse response, ODataSerializer serializer,
      ContentType responseContentType, Map<String, String> preferences) {
    super(metadata, response, preferences);
    this.serializer = serializer;
    this.responseContentType = responseContentType;
  }

  public void writeMetadata()throws ODataLibraryException {
    assert (!isClosed());
    this.response.setContent(this.serializer.metadataDocument(this.metadata).getContent());
    writeOK(responseContentType);
    close();
  }

  @Override
  public void accepts(ServiceResponseVisior visitor) throws ODataLibraryException,
      ODataApplicationException {
    visitor.visit(this);
  }
  
  public void writeError(ODataServerError error) {
    try {
      writeHeader(HttpHeader.CONTENT_TYPE, this.responseContentType.toContentTypeString());
      writeContent(this.serializer.error(error).getContent(), error.getStatusCode(), true);
    } catch (SerializerException e) {
      writeServerError(true);
    }
  }  
}
