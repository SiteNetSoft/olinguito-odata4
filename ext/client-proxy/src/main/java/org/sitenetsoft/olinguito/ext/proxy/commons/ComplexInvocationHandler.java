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
 *
 * Copyright 2026 SiteNetSoft - Removed commons-lang3 dependency
 * Copyright 2026 SiteNetSoft - Fixed type erasure override warning
 */
package org.sitenetsoft.olinguito.ext.proxy.commons;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataPropertyRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientComplexValue;
import org.sitenetsoft.olinguito.client.api.domain.ClientLinked;
import org.sitenetsoft.olinguito.client.api.domain.ClientProperty;
import org.sitenetsoft.olinguito.client.api.uri.URIBuilder;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.ext.proxy.AbstractService;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.ComplexType;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.NavigationProperty;
import org.sitenetsoft.olinguito.ext.proxy.utils.ClassUtils;

public class ComplexInvocationHandler extends AbstractStructuredInvocationHandler {

  private static Map.Entry<ClientComplexValue, Class<?>> init(
          final Class<?> typeRef,
          final AbstractService<?> service) {

    final Class<?> complexTypeRef;
    if (Collection.class.isAssignableFrom(typeRef)) {
      complexTypeRef = ClassUtils.extractTypeArg(typeRef);
    } else {
      complexTypeRef = typeRef;
    }

    final ComplexType annotation = complexTypeRef.getAnnotation(ComplexType.class);
    if (annotation == null) {
      throw new IllegalArgumentException("Invalid complex type " + complexTypeRef);
    }

    final FullQualifiedName typeName =
            new FullQualifiedName(ClassUtils.getNamespace(complexTypeRef), annotation.name());
    final ClientComplexValue complex =
            service.getClient().getObjectFactory().newComplexValue(typeName.toString());

    return Map.entry(complex, complexTypeRef);
  }

  public static ComplexInvocationHandler getInstance(final EntityInvocationHandler handler, final Class<?> typeRef) {
    final Map.Entry<ClientComplexValue, Class<?>> init = init(typeRef, handler.service);
    return new ComplexInvocationHandler(init.getKey(), init.getValue(), handler);
  }

  public static ComplexInvocationHandler getInstance(
          final ClientComplexValue complex,
          final Class<?> typeRef,
          final AbstractService<?> service) {

    return new ComplexInvocationHandler(complex, typeRef, service);
  }

  public static ComplexInvocationHandler getInstance(
          final Class<?> typeRef,
          final AbstractService<?> service) {
      
    final Map.Entry<ClientComplexValue, Class<?>> init = init(typeRef, service);
    return new ComplexInvocationHandler(init.getKey(), init.getValue(), service);
  }

  public static ComplexInvocationHandler getInstance(
          final Class<?> typeRef,
          final AbstractService<?> service,
          final URIBuilder uri) {

    final Map.Entry<ClientComplexValue, Class<?>> init = init(typeRef, service);
    return new ComplexInvocationHandler(init.getKey(), init.getValue(), service, uri);
  }

  public static ComplexInvocationHandler getInstance(
          final ClientComplexValue complex,
          final Class<?> typeRef,
          final AbstractService<?> service,
          final URIBuilder uri) {
      
    return new ComplexInvocationHandler(complex, typeRef, service, uri);
  }

  private ComplexInvocationHandler(
          final ClientComplexValue complex,
          final Class<?> typeRef,
          final AbstractService<?> service,
          final URIBuilder uri) {

    super(typeRef, complex, service);
    this.uri = uri;
    this.baseURI = this.uri == null ? null : this.uri.build();
  }

  private ComplexInvocationHandler(
          final ClientComplexValue complex,
          final Class<?> typeRef,
          final EntityInvocationHandler handler) {

    super(typeRef, complex, handler);
    this.uri = null;
  }

  private ComplexInvocationHandler(
          final ClientComplexValue complex,
          final Class<?> typeRef,
          final AbstractService<?> service) {

    super(typeRef, complex, service);
    this.uri = null;
  }

  public ClientComplexValue getComplex() {
    return (ClientComplexValue) this.internal;
  }

  @Override
  protected Object getNavigationPropertyValue(final NavigationProperty property, final Method getter) {
    if (!(internal instanceof ClientLinked)) {
      throw new UnsupportedOperationException("Internal object is not navigable");
    }

    return retrieveNavigationProperty(property, getter);
  }

  @Override
  protected void load() {
    try {
      if (this.uri != null) {
        final ODataPropertyRequest<ClientProperty> req =
                getClient().getRetrieveRequestFactory().getPropertyRequest(uri.build());

        final ODataRetrieveResponse<ClientProperty> res = req.execute();
        this.internal = res.getBody().getValue();
      }
    } catch (IllegalArgumentException e) {
      LOG.warn("Complex at '" + uri + "' not found", e);
      throw e;
    } catch (Exception e) {
      LOG.warn("Error retrieving complex '" + uri + "'", e);
      throw new IllegalArgumentException("Error retrieving " + typeRef.getSimpleName(), e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <T extends ClientProperty> List<T> getInternalProperties() {
    final List<ClientProperty> res = new ArrayList<ClientProperty>();
    if (getComplex() != null) {
      for (ClientProperty property : getComplex()) {
        res.add(property);
      }
    }
    return (List<T>) res;
  }

  @Override
  protected ClientProperty getInternalProperty(final String name) {
    return getComplex() == null ? null : getComplex().get(name);
  }
}
