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
 * Copyright 2026 SiteNetSoft - tests for entity container cache identity across build routes
 * Copyright 2026 SiteNetSoft - pin the FQN-key-canonical fix (round 1) with a deterministic and a
 *                               best-effort concurrent regression test
 */
package org.sitenetsoft.olinguito.commons.core.edm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;

/**
 * The entity container is reachable through two independent routes -- {@code Edm#getEntityContainer()}
 * (which builds it through {@code EdmProviderImpl#createEntityContainer}) and
 * {@code Edm#getSchemas()} (which builds it through {@code EdmSchemaImpl#createEntityContainer}).
 * Both must yield the very same instance in either order, or a provider that compares containers or
 * entity sets by reference breaks and the caches populated on the first instance are thrown away.
 */
class EdmEntityContainerCacheTest {

  private static final String NAMESPACE = "ns";
  private static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, "container");
  private static final FullQualifiedName ET = new FullQualifiedName(NAMESPACE, "ETX");

  private static final class LocalProvider extends CsdlAbstractEdmProvider {

    private final CsdlEntityContainer container = new CsdlEntityContainer()
        .setName(CONTAINER.getName())
        .setEntitySets(List.of(new CsdlEntitySet().setName("ESX").setType(ET)));

    @Override
    public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) {
      return ET.equals(entityTypeName)
          ? new CsdlEntityType().setName(ET.getName())
              .setKey(List.of(new CsdlPropertyRef().setName("Id")))
              .setProperties(List.of(new CsdlProperty().setName("Id")
                  .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())))
          : null;
    }

    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName) {
      return CONTAINER.equals(entityContainer) && "ESX".equals(entitySetName)
          ? container.getEntitySets().get(0) : null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName) {
      return entityContainerName == null || CONTAINER.equals(entityContainerName)
          ? new CsdlEntityContainerInfo().setContainerName(CONTAINER) : null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
      return container;
    }

    @Override
    public List<CsdlSchema> getSchemas() {
      return List.of(new CsdlSchema().setNamespace(NAMESPACE)
          .setEntityTypes(List.of(getEntityType(ET)))
          .setEntityContainer(container));
    }
  }

  @Test
  void containerCachedBeforeSchemasSurvivesSchemaMaterialization() {
    final Edm edm = new EdmProviderImpl(new LocalProvider());

    final EdmEntityContainer first = edm.getEntityContainer();
    assertNotNull(first);
    final EdmEntitySet firstSet = first.getEntitySet("ESX");
    assertNotNull(firstSet);

    edm.getSchemas();

    assertSame(first, edm.getEntityContainer(), "getEntityContainer() must keep returning the cached instance");
    assertSame(first, edm.getEntityContainer(CONTAINER), "the FQN key must resolve to the same instance");
    assertSame(first, edm.getSchemas().get(0).getEntityContainer(),
        "the schema must expose the already-cached container, not a fresh one");
    assertSame(firstSet, edm.getEntityContainer().getEntitySet("ESX"),
        "the container's own entity-set cache must survive");
  }

  @Test
  void containerCachedThroughSchemasIsReusedByBothKeys() {
    final Edm edm = new EdmProviderImpl(new LocalProvider());

    final EdmEntityContainer fromSchema = edm.getSchemas().get(0).getEntityContainer();
    assertNotNull(fromSchema);

    assertSame(fromSchema, edm.getEntityContainer());
    assertSame(fromSchema, edm.getEntityContainer(CONTAINER));
  }

  /**
   * Deterministic pin for fix round 1: pre-populates the FQN-keyed slot directly (simulating a writer
   * that got there first), then drives the default-alias route. The FQN-keyed slot must win over a
   * freshly built instance, and the default alias must end up pointing at that same winner -- not at
   * whatever {@code getEntityContainer()} built for itself before consulting the cache.
   */
  @Test
  void preCachedFqnSlotWinsOverFreshDefaultBuild() {
    final AbstractEdm edm = new EdmProviderImpl(new LocalProvider());
    final EdmEntityContainer preCached = mock(EdmEntityContainer.class);
    when(preCached.getNamespace()).thenReturn(NAMESPACE);
    when(preCached.getName()).thenReturn(CONTAINER.getName());

    final EdmEntityContainer winner = edm.cacheEntityContainerIfAbsent(CONTAINER, preCached);
    assertSame(preCached, winner);

    final EdmEntityContainer resolved = edm.getEntityContainer();

    assertSame(preCached, resolved,
        "a container already cached under the FQN-keyed slot must win over a freshly built default");
    assertSame(preCached, edm.getEntityContainer(CONTAINER));
    assertSame(preCached, edm.cachedEntityContainer(null),
        "the default alias must be linked to the FQN-keyed winner, not to a fresh instance");
  }

  /**
   * Best-effort concurrent regression test: many threads race to materialize the container through
   * both routes at once. The assertion (identity) is deterministic regardless of whether the race
   * actually gets exercised on a given run -- it never passes on a split brain.
   */
  @Test
  void concurrentBuildersConvergeOnOneInstance() throws Exception {
    final int threadCount = 16;
    final ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    try {
      final Edm edm = new EdmProviderImpl(new LocalProvider());
      final CyclicBarrier barrier = new CyclicBarrier(threadCount);
      final List<Callable<EdmEntityContainer>> tasks = new ArrayList<>();
      for (int i = 0; i < threadCount; i++) {
        final boolean viaSchema = i % 2 == 0;
        tasks.add(() -> {
          barrier.await();
          return viaSchema ? edm.getSchemas().get(0).getEntityContainer() : edm.getEntityContainer();
        });
      }

      final List<Future<EdmEntityContainer>> results = pool.invokeAll(tasks);
      final EdmEntityContainer first = results.get(0).get();
      assertNotNull(first);
      for (final Future<EdmEntityContainer> result : results) {
        assertSame(first, result.get(), "all build routes must converge on the same container instance");
      }
      assertSame(first, edm.getEntityContainer(CONTAINER));
    } finally {
      pool.shutdownNow();
    }
  }
}
