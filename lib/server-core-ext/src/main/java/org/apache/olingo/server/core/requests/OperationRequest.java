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

import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.edm.EdmReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.RepresentationType;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.ContentNegotiator;
import org.sitenetsoft.olinguito.server.core.ContentNegotiatorException;
import org.sitenetsoft.olinguito.server.core.ServiceRequest;

public abstract class OperationRequest extends ServiceRequest {

  private boolean countRequest;

  public OperationRequest(OData odata, ServiceMetadata serviceMetadata) {
    super(odata, serviceMetadata);
  }

  @Override
  public ContentType getResponseContentType() throws ContentNegotiatorException {
    if (!hasReturnType()) {
      // this default content type
      return ContentType.APPLICATION_OCTET_STREAM;
    }

    if (isReturnTypePrimitive()) {
      return ContentNegotiator.doContentNegotiation(uriInfo.getFormatOption(), this.request,
          getCustomContentTypeSupport(), isCollection() ? RepresentationType.COLLECTION_PRIMITIVE
              : RepresentationType.PRIMITIVE);
    } else if (isReturnTypeComplex()) {
      return ContentNegotiator.doContentNegotiation(uriInfo.getFormatOption(), this.request,
          getCustomContentTypeSupport(), isCollection() ? RepresentationType.COLLECTION_COMPLEX
              : RepresentationType.COMPLEX);
    } else {
      return ContentNegotiator.doContentNegotiation(uriInfo.getFormatOption(), this.request,
          getCustomContentTypeSupport(), isCollection() ? RepresentationType.COLLECTION_ENTITY
              : RepresentationType.ENTITY);
    }
  }

  public abstract boolean isBound();

  public abstract boolean isCollection();

  public abstract EdmReturnType getReturnType();

  public abstract boolean hasReturnType();

  public ContextURL getContextURL(OData odata) throws SerializerException {
    if (!hasReturnType()) {
      return null;
    }
   
    if (isReturnTypePrimitive() || isReturnTypeComplex()) {
      // Part 1 {10.14, 10.14} since the function return properties does not
      // represent a Entity property
      ContextURL.Builder builder = ContextURL.with().type(getReturnType().getType());
      if (isCollection()) {
        builder.asCollection();
      }
      return builder.build();
    }

    // EdmTypeKind.ENTITY; Not Bound
    // Here we do not know the EntitySet, then follow directions from
    // Part-1{10.2. 10.3} to use
    // {context-url}#{type-name}
    ContextURL.Builder builder = ContextURL.with().type(getReturnType().getType());
    if (isCollection()) {
      builder.asCollection();
    }
    return builder.build();
  }

  public boolean isReturnTypePrimitive() {
    return getReturnType().getType().getKind() == EdmTypeKind.PRIMITIVE;
  }

  public boolean isReturnTypeComplex() {
    return getReturnType().getType().getKind() == EdmTypeKind.COMPLEX;
  }
  
  public boolean isCountRequest() {
    return countRequest;
  }

  public void setCountRequest(boolean countRequest) {
    this.countRequest = countRequest;
  }  
}