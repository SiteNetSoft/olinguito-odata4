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
package org.sitenetsoft.olinguito.server.core;

import java.io.ByteArrayInputStream;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchDeserializerException;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.responses.ErrorResponse;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserException;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSemanticException;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSyntaxException;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;

public class ErrorHandler {
  private final OData odata;
  private final ServiceHandler handler;
  private final ContentType contentType;
  private final ServiceMetadata metadata;
  
  public ErrorHandler(OData odata, ServiceMetadata metadata,
      ServiceHandler handler, ContentType contentType) {
    this.odata = odata;
    this.handler = handler;
    this.contentType = contentType;
    this.metadata = metadata;
  }

  public void handleException(Exception e, ODataRequest request, ODataResponse response) {
    if (e instanceof UriValidationException uriValidationException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(uriValidationException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof UriParserSemanticException uriParserSemanticException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(uriParserSemanticException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof UriParserSyntaxException uriParserSyntaxException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(uriParserSyntaxException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof UriParserException uriParserException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(uriParserException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof ContentNegotiatorException contentNegotiatorException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(contentNegotiatorException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof SerializerException serializerException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(serializerException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof BatchDeserializerException batchDeserializerException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(batchDeserializerException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof DeserializerException deserializerException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(deserializerException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof ODataHandlerException oDataHandlerException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(oDataHandlerException, null);
      handleServerError(request, response, serverError);
    } else if(e instanceof ODataApplicationException oDataApplicationException) {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(oDataApplicationException);
      handleServerError(request, response, serverError);
    }else {
      ODataServerError serverError = ODataExceptionHelper.createServerErrorObject(e);
      handleServerError(request, response, serverError);
    }
  }

  void handleServerError(final ODataRequest request, final ODataResponse response,
      final ODataServerError serverError) {
    try {
      ODataSerializer serializer = this.odata.createSerializer(this.contentType);
      ErrorResponse errorResponse = new ErrorResponse(this.metadata, serializer, this.contentType, response);
      handler.processError(serverError, errorResponse);
    } catch (Exception e) {
      // This should never happen but to be sure we have this catch here
      // to prevent sending a stacktrace to a client.
      String responseContent = "{\"error\":{\"code\":null,\"message\":\"An unexpected exception occurred during "
          + "error processing with message: " + e.getMessage() + "\"}}"; //$NON-NLS-1$ //$NON-NLS-2$
      response.setContent(new ByteArrayInputStream(responseContent.getBytes()));
      response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE,
          ContentType.APPLICATION_JSON.toContentTypeString());
    }
  }
}
