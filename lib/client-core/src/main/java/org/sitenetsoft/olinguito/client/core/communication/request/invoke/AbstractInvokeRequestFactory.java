/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.sitenetsoft.olinguito.client.core.communication.request.invoke;

import java.net.URI;
import java.util.Map;

import org.sitenetsoft.olinguito.client.api.communication.request.invoke.InvokeRequestFactory;
import org.sitenetsoft.olinguito.client.api.communication.request.invoke.ODataInvokeRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.invoke.ClientNoContent;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.api.domain.ClientProperty;
import org.sitenetsoft.olinguito.client.api.domain.ClientInvokeResult;
import org.sitenetsoft.olinguito.client.api.domain.ClientValue;
import org.sitenetsoft.olinguito.commons.api.edm.EdmReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

public abstract class AbstractInvokeRequestFactory implements InvokeRequestFactory {

  @Override
  public <RES extends ClientInvokeResult> ODataInvokeRequest<RES> getFunctionInvokeRequest(
          final URI uri, final Class<RES> resultRef) {

    return getFunctionInvokeRequest(uri, resultRef, null);
  }

  @Override
  public <RES extends ClientInvokeResult> ODataInvokeRequest<RES> getFunctionInvokeRequest(
          final URI uri, final Class<RES> resultRef, final Map<String, ClientValue> parameters) {

    return getInvokeRequest(HttpMethod.GET, uri, resultRef, parameters);
  }

  @Override
  public <RES extends ClientInvokeResult> ODataInvokeRequest<RES> getActionInvokeRequest(
          final URI uri, final Class<RES> resultRef) {

    return getActionInvokeRequest(uri, resultRef, null);
  }

  @Override
  public <RES extends ClientInvokeResult> ODataInvokeRequest<RES> getActionInvokeRequest(
          final URI uri, final Class<RES> resultRef, final Map<String, ClientValue> parameters) {

    return getInvokeRequest(HttpMethod.POST, uri, resultRef, parameters);
  }

  @SuppressWarnings("unchecked")
  protected <RES extends ClientInvokeResult> Class<RES> getResultReference(final EdmReturnType returnType) {
    Class<RES> result;

    if (returnType == null) {
      result = (Class<RES>) ClientNoContent.class;
    } else {
      if (returnType.isCollection() && returnType.getType().getKind() == EdmTypeKind.ENTITY) {
        result = (Class<RES>) ClientEntitySet.class;
      } else if (!returnType.isCollection() && returnType.getType().getKind() == EdmTypeKind.ENTITY) {
        result = (Class<RES>) ClientEntity.class;
      } else {
        result = (Class<RES>) ClientProperty.class;
      }
    }

    return result;
  }
}
