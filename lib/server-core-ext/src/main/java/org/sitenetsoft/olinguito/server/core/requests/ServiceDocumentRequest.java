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

package org.sitenetsoft.olinguito.server.core.requests;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.RepresentationType;
import org.sitenetsoft.olinguito.server.core.ContentNegotiator;
import org.sitenetsoft.olinguito.server.core.ContentNegotiatorException;
import org.sitenetsoft.olinguito.server.core.ServiceHandler;
import org.sitenetsoft.olinguito.server.core.ServiceRequest;
import org.sitenetsoft.olinguito.server.core.responses.ServiceDocumentResponse;

public class ServiceDocumentRequest extends ServiceRequest {

  public ServiceDocumentRequest(OData odata, ServiceMetadata serviceMetadata) {
    super(odata, serviceMetadata);
  }

  @Override
  public ContentType getResponseContentType() throws ContentNegotiatorException {
    return ContentNegotiator.doContentNegotiation(getUriInfo().getFormatOption(),
        getODataRequest(), getCustomContentTypeSupport(), RepresentationType.SERVICE);
  }

  @Override
  public void execute(ServiceHandler handler, ODataResponse response)
      throws ODataLibraryException, ODataApplicationException {

    // check for valid HTTP Verb
    assertHttpMethod(response);
    
    handler.readServiceDocument(this,
        ServiceDocumentResponse.getInstace(this, response, getResponseContentType()));
  }
}
