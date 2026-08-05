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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced commons-io with Java standard library
 */
package org.sitenetsoft.olinguito.ext.proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import java.util.Base64;
import org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.client.core.edm.ClientCsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.core.edm.EdmProviderImpl;
import org.sitenetsoft.olinguito.ext.proxy.api.AbstractTerm;
import org.sitenetsoft.olinguito.ext.proxy.api.PersistenceManager;
import org.sitenetsoft.olinguito.ext.proxy.commons.EntityContainerInvocationHandler;
import org.sitenetsoft.olinguito.ext.proxy.commons.NonTransactionalPersistenceManagerImpl;
import org.sitenetsoft.olinguito.ext.proxy.commons.TransactionalPersistenceManagerImpl;
import org.sitenetsoft.olinguito.ext.proxy.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for proxy mode, gives access to entity container instances.
 *
 * @param <C> actual client class
 */
public abstract class AbstractService<C extends EdmEnabledODataClient> {

  /**
   * A set of classes which are allowed to be deserialized by default.
   */
  private static final Set<String> DEFAULT_ALLOWED_CLASSES;
  static {
    DEFAULT_ALLOWED_CLASSES = Collections.singleton("org.sitenetsoft.olinguito.*");
  }

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractService.class);

  private final Map<Class<?>, Object> ENTITY_CONTAINERS = new ConcurrentHashMap<Class<?>, Object>();

  private final EdmEnabledODataClient client;

  private final Context context;

  private final boolean transactional;

  private PersistenceManager persistenceManager;

  protected AbstractService(final String compressedMetadata, final String metadataETag,
      final ODataServiceVersion version, final String serviceRoot, final boolean transactional) {

    ByteArrayInputStream bais = null;
    GZIPInputStream gzis = null;
    ObjectInputStream ois = null;
    XMLMetadata metadata = null;
    try {
      bais = new ByteArrayInputStream(Base64.getDecoder().decode(compressedMetadata.getBytes(StandardCharsets.UTF_8)));
      gzis = new GZIPInputStream(bais);
      ois = createObjectInputStream(gzis);
      metadata = (XMLMetadata) ois.readObject();
    } catch (Exception e) {
      LOG.error("While deserializing compressed metadata", e);
    } finally {
      if (ois != null) {
        try {
          ois.close();
        } catch (IOException ignored) { }
      }
      if (gzis != null) {
        try {
          gzis.close();
        } catch (IOException ignored) { }
      }
      if (bais != null) {
        try {
          bais.close();
        } catch (IOException ignored) { }
      }
    }
    final Edm edm;
    if (metadata != null) {
      ClientCsdlEdmProvider provider = new ClientCsdlEdmProvider(metadata.getSchemaByNsOrAlias());
      edm = new EdmProviderImpl(provider);
    } else {
      edm = null;
    }
    if (version.compareTo(ODataServiceVersion.V40) < 0) {
      throw new ODataRuntimeException("Only OData V4 or higher supported.");
    }

    this.client = ODataClientFactory.getEdmEnabledClient(serviceRoot, edm, metadataETag);
    this.client.getConfiguration().setDefaultPubFormat(ContentType.JSON_FULL_METADATA);
    this.transactional = transactional;
    this.context = new Context();
  }

  public abstract Class<?> getEntityTypeClass(String name);

  public abstract Class<?> getComplexTypeClass(String name);

  public abstract Class<?> getEnumTypeClass(String name);

  public abstract Class<? extends AbstractTerm> getTermClass(String name);

  @SuppressWarnings("unchecked")
  public C getClient() {
    return (C) client;
  }

  public Context getContext() {
    return context;
  }

  public boolean isTransactional() {
    return transactional;
  }

  public PersistenceManager getPersistenceManager() {
    synchronized (this) {
      if (persistenceManager == null) {
        persistenceManager = transactional
            ? new TransactionalPersistenceManagerImpl(this)
            : new NonTransactionalPersistenceManagerImpl(this);
      }
    }
    return persistenceManager;
  }

  /**
   * Return an initialized concrete implementation of the passed EntityContainer interface.
   *
   * @param <T> interface annotated as EntityContainer
   * @param reference class object of the EntityContainer annotated interface
   * @return an initialized concrete implementation of the passed reference
   * @throws IllegalArgumentException if the passed reference is not an interface annotated as EntityContainer
   */
  public <T> T getEntityContainer(final Class<T> reference) throws IllegalStateException, IllegalArgumentException {
    if (!ENTITY_CONTAINERS.containsKey(reference)) {
      final Object entityContainer = Proxy.newProxyInstance(
          Thread.currentThread().getContextClassLoader(),
          new Class<?>[] { reference },
          EntityContainerInvocationHandler.getInstance(reference, this));
      ENTITY_CONTAINERS.put(reference, entityContainer);
    }
    return reference.cast(ENTITY_CONTAINERS.get(reference));
  }

  /**
   * Returns a set of classes which are allowed for deserialization.<br/>
   * By default, only classes from the "org.sitenetsoft.olinguito" package are allowed.
   * Subclasses should override this method if they expect other classes to be deserialized.
   *
   * @return A set of classes which are allowed for deserialization.
   */
  protected Set<String> getAllowedClasses() {
    return Collections.emptySet();
  }

  /**
   * Wraps a specified {@link InputStream} into an {@link ObjectInputStream}
   * which allows only a limited set of classes for deserialization.
   * The method calls {@link #getAllowedClasses()} to get a set of classes
   * which are allowed for deserialization.
   *
   * <p>SECURITY: This replaces the former {@code ValidatingObjectInputStream} from
   * commons-io. The {@code resolveClass} method validates each class name against
   * the allowed patterns before permitting deserialization.</p>
   *
   * @param is The input stream to be wrapped.
   * @return An {@link ObjectInputStream} that validates class names during deserialization.
   * @throws IOException If something went wrong.
   */
  private ObjectInputStream createObjectInputStream(InputStream is) throws IOException {
    final Set<String> allowedClasses = new HashSet<>(DEFAULT_ALLOWED_CLASSES);
    allowedClasses.addAll(getAllowedClasses());

    return new ObjectInputStream(is) {
      @Override
      protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final String className = desc.getName();
        for (String pattern : allowedClasses) {
          if (pattern.endsWith(".*")) {
            // Wildcard pattern: match package prefix
            String prefix = pattern.substring(0, pattern.length() - 1);
            if (className.startsWith(prefix)) {
              return super.resolveClass(desc);
            }
          } else if (pattern.equals(className)) {
            return super.resolveClass(desc);
          }
        }
        throw new InvalidClassException("Rejected deserialization of class: " + className);
      }
    };
  }
}
