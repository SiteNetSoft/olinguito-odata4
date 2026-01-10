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
package org.sitenetsoft.olinguito.server.adapter.servlet.ext;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.processor.Processor;
import org.sitenetsoft.olinguito.server.api.serializer.CustomContentTypeSupport;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.ODataHandlerException;
import org.sitenetsoft.olinguito.server.core.RequestMethodResolver;
import org.sitenetsoft.olinguito.server.core.RequestUriResolver;
import org.sitenetsoft.olinguito.server.core.ServiceHandler;
import org.sitenetsoft.olinguito.server.core.legacy.ProcessorServiceHandler;
import org.sitenetsoft.olinguito.server.adapter.servlet.ServletHeaderCopier;
import org.sitenetsoft.olinguito.server.adapter.servlet.ServletResponseWriter;

public class OData4HttpHandler {

  private ServiceHandler handler;
  private final ServiceMetadata serviceMetadata;
  private final OData odata;
  private int split = 0;
  private CustomContentTypeSupport customContentTypeSupport;

  public OData4HttpHandler(OData odata, ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
    this.handler = new ProcessorServiceHandler();
    this.handler.init(odata, serviceMetadata);
  }

  public void process(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
    ODataRequest request = null;
    ODataResponse response = new ODataResponse();

    try {
      request = createODataRequest(httpRequest, this.split);
      validateODataVersion(request);

      ServiceDispatcher dispatcher = new ServiceDispatcher(this.odata, this.serviceMetadata,
              handler, this.customContentTypeSupport);
      dispatcher.execute(request, response);

    } catch (Exception e) {
      ErrorHandler handler = new ErrorHandler(this.odata, this.serviceMetadata,
              this.handler, ContentType.JSON);
      handler.handleException(e, request, response);
    }

    ServletResponseWriter.write(httpResponse, response);
  }

  ODataRequest createODataRequest(final HttpServletRequest httpRequest, final int split)
          throws ODataLibraryException {

    try {
      ODataRequest odRequest = new ODataRequest();
      odRequest.setBody(httpRequest.getInputStream());

      ServletHeaderCopier.copyHeaders(odRequest, httpRequest);

      odRequest.setMethod(RequestMethodResolver.resolve(
              httpRequest.getMethod(),
              httpRequest.getHeader(HttpHeader.X_HTTP_METHOD),
              httpRequest.getHeader(HttpHeader.X_HTTP_METHOD_OVERRIDE)));

      RequestUriResolver.fillUriInformation(
              odRequest,
              httpRequest.getRequestURL().toString(),
              httpRequest.getRequestURI(),
              httpRequest.getQueryString(),
              httpRequest.getContextPath(),
              httpRequest.getServletPath(),
              httpRequest.getAttribute("requestMapping"),
              split);

      return odRequest;
    } catch (final IOException e) {
      throw new SerializerException(
              "An I/O exception occurred.", e, SerializerException.MessageKeys.IO_EXCEPTION);
    }
  }

  void validateODataVersion(final ODataRequest request) throws ODataHandlerException {
    final String maxVersion = request.getHeader(HttpHeader.ODATA_MAX_VERSION);

    // Always respond with V4.0
    // NOTE: you currently set it on the response elsewhere; keep that behavior if needed.

    if (maxVersion != null
            && ODataServiceVersion.isBiggerThan(ODataServiceVersion.V40.toString(), maxVersion)) {
      throw new ODataHandlerException("ODataVersion not supported: " + maxVersion,
              ODataHandlerException.MessageKeys.ODATA_VERSION_NOT_SUPPORTED, maxVersion);
    }
  }

  public void register(final Processor processor) {
    if (processor instanceof ServiceHandler) {
      this.handler = (ServiceHandler) processor;
      this.handler.init(this.odata, this.serviceMetadata);
    }

    if (this.handler instanceof ProcessorServiceHandler) {
      ((ProcessorServiceHandler) this.handler).register(processor);
    }
  }

  public void register(final CustomContentTypeSupport customContentTypeSupport) {
    this.customContentTypeSupport = customContentTypeSupport;
  }

  public void setSplit(int split) {
    this.split = split;
  }
}
