# Tier 5 Follow-ups Implementation Plan — Review Tickets Deferred by Waves 1–3

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the small tickets the Tier 5 Wave 1/2/3 reviews deferred: make `AbstractEdm`'s entity-container cache identity-stable, honor the key-as-segment flag in `UriHelperImpl.parseEntityId`, replace tecsvc's accidental "empty key list = first entity" convention with an explicit first-entity call, resolve referential-constraint-completed key predicates in the tecsvc data layer (turning a 400 `Wrong key!` into a real 200), stop tecsvc from falsely echoing `odata.track-changes` on `$ref` collection reads, add the missing `PreferencesApplied.Builder.omitValues(...)` convenience, deduplicate the three copies of the `valueOfString`-with-`EdmMapping` block into one `server-core` helper that reports a malformed `Core.OptionalParameter` `DefaultValue` as 400 instead of 500 on the URL path, exercise the previously unreachable bad-default operations end to end, and add the three-way `/$query` × `omit-values=nulls` × `matchesPattern` composition pin plus the `/$query` trailing-slash comment.

**Architecture:** No new subsystem. Five seams are touched. (1) `lib/commons-core` EDM cache: `AbstractEdm` already uses `ConcurrentHashMap` + `putIfAbsent` in `getEntityContainer`, but `EdmSchemaImpl.createEntityContainer` re-caches a *fresh* `EdmEntityContainerImpl` under both the FQN and the `null` key with a plain `put`, so `edm.getEntityContainer()` returns a different object before and after `edm.getSchemas()`; the fix is lookup-before-create plus putIfAbsent-with-return. (2) `lib/server-core` URI helper: `UriHelperImpl.parseEntityId` builds its own `Parser` with the key-as-segment flag defaulted off; the flag becomes an opt-in on the concrete impl, reachable through a new *concrete* (non-abstract) `OData.createUriHelper(boolean)` overload. (3) `lib/server-tecsvc` data layer: `DataProvider.read`/`readDataFromEntity` treat an empty key list as "match everything" (returning the first entity) and cannot resolve a `UriParameter` whose value comes from a referenced property; both are fixed by moving the first-entity choice into `TechnicalProcessor` explicitly and by passing the *source* entity into the key matcher. (4) preferences: `TechnicalEntityProcessor#readEntityCollection`'s Preference-Applied block echoes `odata.track-changes` even for `$ref` collections, whose serializer (`ODataJsonSerializer#referenceCollection`) never writes a delta link — a false claim, gated the same way `omit-values` already is; `PreferencesApplied.Builder` gains `omitValues(Preferences.OmitValues)`. (5) optional-parameter defaults: one new `server-core` helper class centralizes the URI-literal → typed-value conversion used in three places and lets the URI parser reject a malformed `DefaultValue` with a 400 before tecsvc's 500 path can be reached.

**Tech Stack:** Java 17, Maven; modules `lib/commons-core`, `lib/server-api`, `lib/server-core`, `lib/server-tecsvc`, `lib/server-test`, `fit`. No new dependencies.

**Spec:** `docs/superpowers/specs/2026-08-12-tier5-odata401-design.md` (Features 1–7) + `docs/superpowers/specs/2026-08-12-tier5-odata401-citations.md`, plus the follow-up ticket list carried by this plan (tickets A–J, with E and part of D re-scoped by scouting — see **Dropped / Re-scoped** below).

## Global Constraints

- Branch: `feature/tier5-followups` off master `38a594b99` (already created and pushed).
- **NO AI attribution in commit messages.**
- SiteNetSoft copyright line before the Apache header's closing `*/` on EVERY modified Java source file — main AND test. New files copy a sibling's full header. `.properties`/`.xml` resources carry no header line (repo precedent).
- Checkstyle LineLength max **120**.
- `server-test`/`fit` run against installed jars: `mvn install -pl <changed modules> -Pbuild.fast -DskipTests -q` before testing there.
- `fit` skips checkstyle under `build.fast`, so whenever fit tests change also run a plain non-fast `mvn -B install -pl fit -DskipTests -q` before committing.
- Full plain build gate at the end: `mvn -B install --fail-at-end -Dquarkus.http.test-port=8083`. Never use port 8081.
- Be generous with tests (user directive): every behavior change gets a pin, and every existing behavior keeps its pin. When a pin's expectation flips, rename the test so the name states the new truth and keep an inverse pin for the old-but-still-valid case.
- Each task ends with exactly one `git commit -m "<plain imperative subject>"`. Do NOT push.

---

### Task 1: Reuse the cached entity container when schemas are materialized (ticket A)

**Files:**
- Modify: `lib/commons-core/src/main/java/org/sitenetsoft/olinguito/commons/core/edm/AbstractEdm.java` — `cacheEntityContainer` at lines 521–524 (plus two new methods next to it); the `NULL_CONTAINER_KEY` sentinel is at lines 45–47 and `getEntityContainer(FullQualifiedName)` at lines 157–173 (already `putIfAbsent`-correct — do not change it).
- Modify: `lib/commons-core/src/main/java/org/sitenetsoft/olinguito/commons/core/edm/EdmSchemaImpl.java` — `createEntityContainer()` at lines 152–162.
- New test: `lib/commons-core/src/test/java/org/sitenetsoft/olinguito/commons/core/edm/EdmEntityContainerCacheTest.java` (JUnit 5; copy the full Apache header + a SiteNetSoft line from the sibling `lib/commons-core/src/test/java/org/sitenetsoft/olinguito/commons/core/edm/EdmAlternateKeysTest.java`).

**Interfaces:**
- Produces `AbstractEdm.cacheEntityContainerIfAbsent(FullQualifiedName, EdmEntityContainer) : EdmEntityContainer` and `AbstractEdm.cachedEntityContainer(FullQualifiedName) : EdmEntityContainer`.
- Consumes nothing. `public void cacheEntityContainer(FullQualifiedName, EdmEntityContainer)` keeps its signature and return type (binary compatibility) and delegates.

- [ ] **Step 1: Write the failing test.** New file `EdmEntityContainerCacheTest.java`:

```java
package org.sitenetsoft.olinguito.commons.core.edm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

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
}
```
- [ ] **Step 2: RED** — `mvn -B -Pbuild.fast test -pl lib/commons-core -Dtest=EdmEntityContainerCacheTest`. Expect `containerCachedBeforeSchemasSurvivesSchemaMaterialization` to fail on the first `assertSame` (the schema re-cached a fresh container with `put`). `containerCachedThroughSchemasIsReusedByBothKeys` already passes — it is the keep-green pin for the other order.
- [ ] **Step 3: Implement.**
  - In `AbstractEdm` (right after the existing `cacheEntityContainer`, lines 521–524) add:

```java
  /**
   * Caches the entity container under the given key unless one is already cached there, and returns
   * the instance that is in effect afterwards. The EDM is reachable through two independent build
   * routes ({@link #getEntityContainer(FullQualifiedName)} and {@link #getSchemas()}); both must end
   * up handing out the very same container instance, because a second instance would silently
   * discard the caches populated on the first one and would break identity comparisons.
   * @param containerFQN the container name; <code>null</code> addresses the default container
   * @param container the container to cache when the key is still free
   * @return the cached container, which may be a previously cached instance
   */
  public EdmEntityContainer cacheEntityContainerIfAbsent(final FullQualifiedName containerFQN,
      final EdmEntityContainer container) {
    final FullQualifiedName key = containerFQN != null ? containerFQN : NULL_CONTAINER_KEY;
    final EdmEntityContainer existing = entityContainers.putIfAbsent(key, container);
    return existing != null ? existing : container;
  }

  /**
   * Looks the entity container up in the cache without creating one.
   * @param containerFQN the container name; <code>null</code> addresses the default container
   * @return the cached container, or <code>null</code> when none is cached yet
   */
  public EdmEntityContainer cachedEntityContainer(final FullQualifiedName containerFQN) {
    return entityContainers.get(containerFQN != null ? containerFQN : NULL_CONTAINER_KEY);
  }
```
  - Change the existing `cacheEntityContainer` body from `entityContainers.put(key, container);` to a delegation that keeps the `void` signature (binary compatibility for external callers):

```java
  public void cacheEntityContainer(final FullQualifiedName containerFQN, final EdmEntityContainer container) {
    cacheEntityContainerIfAbsent(containerFQN, container);
  }
```
  - In `EdmSchemaImpl.createEntityContainer()` (lines 152–162) replace the body of the `if` with lookup-before-create:

```java
  protected EdmEntityContainer createEntityContainer() {
    if (schema.getEntityContainer() != null) {
      FullQualifiedName containerFQN = new FullQualifiedName(namespace, schema.getEntityContainer().getName());
      final EdmEntityContainer cached = edm.cachedEntityContainer(containerFQN);
      if (cached != null) {
        // Another route already built and populated this container; reusing it keeps container
        // identity stable and preserves the caches that instance has already filled.
        edm.cacheEntityContainerIfAbsent(null, cached);
        return cached;
      }
      edm.addEntityContainerAnnotations(schema.getEntityContainer(), containerFQN);
      EdmEntityContainer impl = new EdmEntityContainerImpl(edm, provider, containerFQN, schema.getEntityContainer());
      final EdmEntityContainer effective = edm.cacheEntityContainerIfAbsent(containerFQN, impl);
      edm.cacheEntityContainerIfAbsent(null, effective);
      return effective;
    }
    return null;
  }
```
- [ ] **Step 4: GREEN** — `mvn -B -Pbuild.fast test -pl lib/commons-core -Dtest=EdmEntityContainerCacheTest`; then the whole module `mvn -B -Pbuild.fast test -pl lib/commons-core` (keeps `EdmImplCachingTest#cacheEntityContainer` and `EdmSchemaImplTest#getContainer` green — both already assert reference identity in the schema-first order); then `mvn install -pl lib/commons-core -Pbuild.fast -DskipTests -q && mvn -B -Pbuild.fast test -pl lib/server-test,lib/server-tecsvc`; finally plain non-fast `mvn -B install -pl lib/commons-core -DskipTests -q`.
- [ ] **Step 5: Commit** — `git commit -m "Reuse the cached entity container when schemas are materialized"`

---

### Task 2: Honor the key-as-segment convention in UriHelper.parseEntityId (ticket D)

**Files:**
- Modify: `lib/server-api/src/main/java/org/sitenetsoft/olinguito/server/api/OData.java` — add a concrete overload next to the abstract `createUriHelper()` (line 166).
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/ODataImpl.java` — `createUriHelper()` at lines 222–224.
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/uri/UriHelperImpl.java` — class body (line 51) and `parseEntityId` (lines 145–174; the `new Parser(edm, new ODataImpl())` call is at line 156).
- Modify: `lib/server-core-ext/src/main/java/org/sitenetsoft/olinguito/server/core/ServiceRequest.java` (line 345) and `lib/server-core-ext/src/main/java/org/sitenetsoft/olinguito/server/core/ServiceDispatcher.java` (line 105) — **javadoc only** (documented seam, see rationale below).
- Modify test: `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/uri/UriHelperTest.java` — sibling tests `parseEntityIdWithKeys` (line 115), `parseEntityIdWithoutKeys` (line 122), `parseEntityIdWithServiceRoot` (line 127).

**Interfaces:**
- Produces `UriHelperImpl.setKeyAsSegment(boolean) : UriHelperImpl` (fluent, concrete class only — the `UriHelper` **interface is unchanged**) and `OData.createUriHelper(boolean keyAsSegment) : UriHelper`.
- Consumes `Parser.setKeyAsSegment(boolean)` (`lib/server-core/.../uri/parser/Parser.java` line 107, added in Wave 3).

**Rationale for the server-core-ext seam (record this in the commit body is NOT needed; the javadoc carries it):** `server-core-ext` has no `ODataHandler` at all — callers construct `ServiceDispatcher`/`ServiceRequest` directly (`ServiceRequest.java:347`, `ServiceDispatcher.java:88`, `requests/BatchRequest.java:155`), so there is no service-level flag in that module to thread. Wiring one would mean adding a new public constructor parameter or setter to three classes plus every caller. It stays a documented seam; the per-model `keyAsSegmentAllowed` flags still work there, because `ResourcePathParser` consults them independently of the service flag (`ResourcePathParser.java:222,226`).

- [ ] **Step 1: Write the failing tests.** Append to `UriHelperTest` (model on the existing `parseEntityIdWithKeys`):

```java
  @Test
  void parseEntityIdKeyAsSegmentIsOffByDefault() {
    // The convention is a MAY and off unless the service opts in, so a bare key segment is not a key.
    assertThrows(DeserializerException.class, () -> helper.parseEntityId(edm, "ESAllPrim/32767", null));
  }

  @Test
  void parseEntityIdKeyAsSegmentWhenEnabled() throws Exception {
    final UriResourceEntitySet result =
        new UriHelperImpl().setKeyAsSegment(true).parseEntityId(edm, "ESAllPrim/32767", null);
    assertEquals("ESAllPrim", result.getEntitySet().getName());
    assertEquals(1, result.getKeyPredicates().size());
    assertEquals("PropertyInt16", result.getKeyPredicates().get(0).getName());
    assertEquals("32767", result.getKeyPredicates().get(0).getText());
  }

  @Test
  void parseEntityIdParenthesizedStillWorksWithKeyAsSegmentEnabled() throws Exception {
    final UriResourceEntitySet result =
        new UriHelperImpl().setKeyAsSegment(true).parseEntityId(edm, "ESAllPrim(32767)", null);
    assertEquals("ESAllPrim", result.getEntitySet().getName());
    assertEquals("32767", result.getKeyPredicates().get(0).getText());
  }

  @Test
  void createUriHelperWithKeyAsSegmentParsesSegmentKeys() throws Exception {
    final UriResourceEntitySet result =
        OData.newInstance().createUriHelper(true).parseEntityId(edm, "ESAllPrim/32767", null);
    assertEquals("32767", result.getKeyPredicates().get(0).getText());
  }
```
  Add the imports the file does not have yet: `org.sitenetsoft.olinguito.server.core.uri.UriHelperImpl` and `org.sitenetsoft.olinguito.server.api.OData` (check the existing import block first; `helper`, `edm`, `assertThrows` and `DeserializerException` are already present).
- [ ] **Step 2: RED** — `mvn install -pl lib/server-api,lib/server-core -Pbuild.fast -DskipTests -q && mvn -B -Pbuild.fast test -pl lib/server-test -Dtest=UriHelperTest`. Expect compile failure (no `setKeyAsSegment`, no `createUriHelper(boolean)`); `parseEntityIdKeyAsSegmentIsOffByDefault` is the keep-green pin.
- [ ] **Step 3: Implement.**
  - `UriHelperImpl`: add the field and fluent setter above `buildContextURLSelectList`, and use the flag in `parseEntityId`.

```java
  private boolean keyAsSegment;

  /**
   * Enables the OData 4.01 key-as-segment URL convention (URL Conventions section 4.3.6) for
   * {@link #parseEntityId(Edm, String, String)}. A service that serves key-as-segment URLs also
   * receives entity ids and binding links written that way, so the entity-id parser has to be told
   * about the convention explicitly; it is off by default, exactly as on {@code ODataHandler}.
   * @param enabled whether key-as-segment entity ids are parsed
   * @return this helper
   */
  public UriHelperImpl setKeyAsSegment(final boolean enabled) {
    this.keyAsSegment = enabled;
    return this;
  }
```
    and at line 156 replace `new Parser(edm, new ODataImpl()).parseUri(...)` with
    `new Parser(edm, new ODataImpl()).setKeyAsSegment(keyAsSegment).parseUri(oDataPath, null, null, rawServiceRoot)`.
  - `OData` (server-api), directly after the abstract `createUriHelper()`:

```java
  /**
   * Creates a URI helper. Unlike {@link #createUriHelper()} the returned helper parses entity ids
   * written in the OData 4.01 key-as-segment form (URL Conventions section 4.3.6) when
   * <code>keyAsSegment</code> is <code>true</code>. The default implementation ignores the flag and
   * returns {@link #createUriHelper()}.
   * @param keyAsSegment whether entity ids may use the key-as-segment convention
   * @return a URI helper
   */
  public UriHelper createUriHelper(final boolean keyAsSegment) {
    return createUriHelper();
  }
```
  - `ODataImpl`: override it as `return new UriHelperImpl().setKeyAsSegment(keyAsSegment);` (keep `createUriHelper()` returning `new UriHelperImpl()`).
  - `ServiceRequest.java` (above the `new Parser(...)` at line 345) and `ServiceDispatcher.java` (above the `new Parser(...)` at line 105) get an identical explanatory comment:

```java
    // Documented seam: this module has no ODataHandler, so there is no service-level key-as-segment
    // flag (OData 4.01 URL Conventions section 4.3.6) to thread here; the parser is built with the
    // convention off. Per-entity-set and per-navigation-property keyAsSegmentAllowed model flags are
    // still honored, because ResourcePathParser consults them independently of the service flag.
```
- [ ] **Step 4: GREEN** — `mvn install -pl lib/server-api,lib/server-core -Pbuild.fast -DskipTests -q && mvn -B -Pbuild.fast test -pl lib/server-test -Dtest=UriHelperTest`; then the whole module `mvn -B -Pbuild.fast test -pl lib/server-test`; then plain non-fast `mvn -B install -pl lib/server-api,lib/server-core,lib/server-core-ext -DskipTests -q`.
- [ ] **Step 5: Commit** — `git commit -m "Parse key-as-segment entity ids when the URI helper opts in"`

---

### Task 3: Make an empty key list address nothing and pick the first entity explicitly (ticket B)

**Files:**
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataProvider.java` — `read(EdmEntitySet, List<UriParameter>)` lines 109–112, `read(EdmEntityType, EntityCollection, List<UriParameter>)` lines 138–146, the `entityMatchesKeys` javadoc at lines 159–170, and `readDataFromEntity` lines 1073–1082.
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/processor/TechnicalProcessor.java` — `readEntity(UriInfoResource, boolean)` lines 141–213 (call sites at line 150 and line 204).
- Modify test: `lib/server-tecsvc/src/test/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataProviderTest.java` — the existing pin `readWithoutKeysMatchesTheFirstEntity` at lines 257–264.

**Interfaces:**
- Produces `DataProvider.readFirst(EdmEntitySet) : Entity` and `DataProvider.readFirst(EdmEntityType) : Entity`, and the new strict contract of `DataProvider.read(...)`/`readDataFromEntity(...)`: an empty key list matches nothing and yields `null`.
- Consumed by Task 4 (which adds the source-entity overloads on the same two methods).

**Blast radius (scouted):** `DataProvider.read(EdmEntitySet, List<UriParameter>)` / `read(EdmEntityType, EntityCollection, List<UriParameter>)` / `readDataFromEntity` have exactly four production call sites, all in `TechnicalProcessor` (lines 150, 164, 202, 204). Lines 164 and 202 are already guarded by `key.isEmpty()` checks above them, so only lines 150 and 204 can reach the data layer with an empty list. `ActionData` never calls `read`; every one of its key uses is already guarded by `if (!keyList.isEmpty())` (`ActionData.java:94,188,235,256,328,350,366,394`), so `TechnicalActionProcessor` is unaffected. The earlier attempt broke `BasicBoundFunctionITCase`, `EntityReferencesITCase` and `KeyAsSegmentITCase#qualifiedBoundOperationStillWins` precisely because it made line 150 return `null` (→ "Nothing found." 404) for collection-bound operations; the explicit `readFirst` call at line 150 preserves that behavior byte-for-byte.

- [ ] **Step 1: Write the failing tests.** In `DataProviderTest`, **replace** `readWithoutKeysMatchesTheFirstEntity` (lines 257–264) with these three tests (model on the neighbouring `unknownAlternateKeyValueReturnsNull`):

```java
  @Test
  void readWithoutKeysMatchesNothing() throws Exception {
    // An empty key list addresses no entity. The "first entity of the collection" choice that
    // collection-bound operations rely on is made explicitly by the processor through readFirst.
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertNull(dataProvider.read(esAllPrim, Collections.<UriParameter> emptyList()));
  }

  @Test
  void readFirstReturnsTheFirstEntityOfTheEntitySet() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertEquals(dataProvider.readAll(esAllPrim).getEntities().get(0),
        dataProvider.readFirst(esAllPrim));
  }

  @Test
  void readDataFromEntityWithoutKeysMatchesNothing() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertNull(dataProvider.readDataFromEntity(
        edm.getEntityType(new FullQualifiedName(SchemaProvider.NAMESPACE, "ETCont")),
        Collections.<UriParameter> emptyList()));
  }

  @Test
  void readFirstByEntityTypeReturnsTheFirstContainmentEntity() throws Exception {
    // Containment collections are keyed by entity-type name in the data map (DataCreator line 118).
    final DataProvider dataProvider = new DataProvider(oData, edm);
    Assertions.assertNotNull(dataProvider.readFirst(
        edm.getEntityType(new FullQualifiedName(SchemaProvider.NAMESPACE, "ETCont"))));
  }
```
- [ ] **Step 2: RED** — `mvn -B -Pbuild.fast test -pl lib/server-tecsvc -Dtest=DataProviderTest`. Expect compile failure on `readFirst` and failures on the two "matches nothing" tests.
- [ ] **Step 3: Implement.**
  - `DataProvider.read(EdmEntityType, EntityCollection, List<UriParameter>)` — add the guard as the first statement:

```java
    if (keys.isEmpty()) {
      // An empty key list addresses no entity: matching every entity would silently return the
      // first one. Callers that deliberately want the first entity call readFirst instead.
      return null;
    }
```
  - `DataProvider.readDataFromEntity(EdmEntityType, List<UriParameter>)` — same guard as the first statement (before `data.get(...)`).
  - Add both `readFirst` overloads next to `readAll` (lines 100–107):

```java
  /**
   * Returns the first entity of an entity set, or <code>null</code> when it is empty. A
   * collection-bound operation (<code>ESAllPrim/olingo.odata.test1.BFCESAllPrimRT...()</code>)
   * addresses a collection without a key predicate; this reference service then works on the first
   * entity of that collection. That choice is made here, explicitly, and never by key matching.
   * @param edmEntitySet the entity set to read from
   * @return the first entity, or <code>null</code>
   */
  public Entity readFirst(final EdmEntitySet edmEntitySet) throws DataProviderException {
    final EntityCollection entityCollection = readAll(edmEntitySet);
    return entityCollection.getEntities().isEmpty() ? null : entityCollection.getEntities().get(0);
  }

  /**
   * Returns the first entity stored under an entity type's own name, or <code>null</code>.
   * Containment collections are keyed by entity-type name (see {@code DataCreator}); a derived-type
   * cast on a single-valued navigation reaches the data layer without a key predicate and then
   * works on the first entity of that collection, see {@link #readFirst(EdmEntitySet)}.
   * @param edmEntityType the entity type whose collection is read
   * @return the first entity, or <code>null</code>
   */
  public Entity readFirst(final EdmEntityType edmEntityType) {
    final EntityCollection entityCollection = data.get(edmEntityType.getName());
    return entityCollection == null || entityCollection.getEntities().isEmpty()
        ? null : entityCollection.getEntities().get(0);
  }
```
  - Rewrite the `entityMatchesKeys` javadoc (lines 159–164) — drop the "empty key list matches every entity on purpose" paragraph and replace it with: `Tells whether the entity matches all given key predicates (primary key or alternate key). Callers guarantee a non-empty key list; {@link #read} and {@link #readDataFromEntity} reject an empty one before reaching here.`
  - `TechnicalProcessor.readEntity`, line 148–150, becomes:

```java
    if (resourcePaths.get(0) instanceof UriResourceEntitySet uriResource) {
      EdmEntitySet entitySet = getEntitySetBasedOnTypeCast(uriResource);
      final List<UriParameter> keyPredicates = uriResource.getKeyPredicates();
      // A collection-bound operation or a $ref on the collection reaches here without key
      // predicates; this service then operates on the first entity of the collection.
      entity = keyPredicates.isEmpty()
          ? dataProvider.readFirst(entitySet)
          : dataProvider.read(entitySet, keyPredicates);
    }
```
  - `TechnicalProcessor.readEntity`, line 204, becomes:

```java
      EdmEntityType edmEntityType = getEntityTypeBasedOnNavPropertyTypeCast(uriNavigationResource);
      if (edmEntityType != null) {
        entity = key.isEmpty()
            ? dataProvider.readFirst(edmEntityType)
            : dataProvider.readDataFromEntity(edmEntityType, key);
      }
```
- [ ] **Step 4: GREEN** — `mvn -B -Pbuild.fast test -pl lib/server-tecsvc`; then `mvn install -pl lib/server-tecsvc -Pbuild.fast -DskipTests -q && mvn verify -pl fit -Pbuild.fast -Dit.test=BasicBoundFunctionITCase+EntityReferencesITCase+EntityReferenceITCase+KeyAsSegmentITCase+BasicBoundActionsITCase`; then FULL fit `mvn verify -pl fit -Pbuild.fast`; then plain non-fast `mvn -B install -pl lib/server-tecsvc -DskipTests -q`.
- [ ] **Step 5: Commit** — `git commit -m "Address no entity with an empty key list and select the first entity explicitly"`

---

### Task 4: Resolve referential-constraint-completed key predicates from the source entity (ticket C)

**Files:**
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataProvider.java` — `read(EdmEntityType, EntityCollection, List<UriParameter>)` (Task 3 shape), `readDataFromEntity`, and `entityMatchesKeys` (lines 165–202).
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/processor/TechnicalProcessor.java` — the navigation loop, lines 190–212.
- Modify test: `lib/server-tecsvc/src/test/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataProviderTest.java`.
- Modify test: `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/http/KeyAsSegmentITCase.java` — `referentialConstraintKeyIsOmitted` at lines 100–119 (currently asserts 400 `Wrong key!` for both conventions).

**Interfaces:**
- Consumes Task 3's strict `read`/`readDataFromEntity` and `UriParameter.getReferencedProperty()` (`lib/server-api/.../uri/UriParameter.java` line 53; set by `ParserHelper.parseKeyPredicate` for referential-constraint-covered key properties).
- Produces the overloads `DataProvider.read(EdmEntityType, EntityCollection, List<UriParameter>, Entity sourceEntity)` and `DataProvider.readDataFromEntity(EdmEntityType, List<UriParameter>, Entity sourceEntity)`; the 3-argument forms delegate with `sourceEntity == null`.

**Seed check (`DataCreator`):** `ESKeyNav` holds `ETKeyNav` entities with `PropertyInt16` 1, 2, 3 (lines 883–892). `ESTwoKeyNav` holds `(1,'1')`, `(1,'2')`, `(2,'1')`, `(3,'1')` (lines 936–948). `linkESKeyNav` (lines 2028–2032) links `ESKeyNav[0]` (PropertyInt16 = 1) to `NavPropertyETTwoKeyNavMany = {esTwoKeyNav[0], esTwoKeyNav[1]}` = `(1,'1')` and `(1,'2')`. So `ESKeyNav(1)/NavPropertyETTwoKeyNavMany('1')` — with `PropertyInt16` supplied by the referential constraint from `ESKeyNav(1).PropertyInt16 = 1` — must resolve to the entity `(PropertyInt16 = 1, PropertyString = '1')`.

- [ ] **Step 1: Write the failing tests.** In `DataProviderTest`, add next to `readByPrimaryKeyUnchanged` (line 243):

```java
  @Test
  void readResolvesReferencedKeyPropertyFromTheSourceEntity() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmEntitySet esKeyNav = entityContainer.getEntitySet("ESKeyNav");
    final Entity source = dataProvider.read(esKeyNav, List.of(mockParameter("PropertyInt16", "1")));
    Assertions.assertNotNull(source);

    final EdmEntitySet esTwoKeyNav = entityContainer.getEntitySet("ESTwoKeyNav");
    final Entity target = dataProvider.read(esTwoKeyNav.getEntityType(), dataProvider.readAll(esTwoKeyNav),
        List.of(referencedParameter("PropertyInt16", "PropertyInt16"), mockParameter("PropertyString", "'1'")),
        source);

    Assertions.assertNotNull(target);
    Assertions.assertEquals((short) 1, target.getProperty("PropertyInt16").getValue());
    Assertions.assertEquals("1", target.getProperty("PropertyString").getValue());
  }

  @Test
  void readWithReferencedKeyPropertyPicksTheRightSourceValue() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmEntitySet esKeyNav = entityContainer.getEntitySet("ESKeyNav");
    // ESKeyNav(3).PropertyInt16 == 3, so the same PropertyString must now select ESTwoKeyNav(3,'1').
    final Entity source = dataProvider.read(esKeyNav, List.of(mockParameter("PropertyInt16", "3")));

    final EdmEntitySet esTwoKeyNav = entityContainer.getEntitySet("ESTwoKeyNav");
    final Entity target = dataProvider.read(esTwoKeyNav.getEntityType(), dataProvider.readAll(esTwoKeyNav),
        List.of(referencedParameter("PropertyInt16", "PropertyInt16"), mockParameter("PropertyString", "'1'")),
        source);

    Assertions.assertNotNull(target);
    Assertions.assertEquals((short) 3, target.getProperty("PropertyInt16").getValue());
  }

  @Test
  void readWithReferencedKeyPropertyAndNoSourceEntityMatchesNothing() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmEntitySet esTwoKeyNav = entityContainer.getEntitySet("ESTwoKeyNav");
    Assertions.assertNull(dataProvider.read(esTwoKeyNav.getEntityType(), dataProvider.readAll(esTwoKeyNav),
        List.of(referencedParameter("PropertyInt16", "PropertyInt16"), mockParameter("PropertyString", "'1'"))));
  }
```
  and add the helper next to `alternateKeyParameter` (line 286):

```java
  private static UriParameter referencedParameter(final String name, final String referencedProperty) {
    return new UriParameterImpl().setName(name).setReferencedProperty(referencedProperty);
  }
```
  In `fit/.../http/KeyAsSegmentITCase.java`, **replace** `referentialConstraintKeyIsOmitted` (lines 100–119, javadoc included) with:

```java
  /**
   * A key property covered by a referential constraint of the navigation property is omitted from the
   * URL (URL Conventions section 4.3.6 MUST) and its value is taken from the source entity's
   * referencing property. ESKeyNav(1) has PropertyInt16 == 1, so the single segment '1' completes the
   * key of ESTwoKeyNav to (PropertyInt16 = 1, PropertyString = '1'). Both URL conventions must resolve
   * the very same entity.
   */
  @Test
  public void referentialConstraintKeyIsOmitted() throws Exception {
    final HttpURLConnection keyAsSegment = get(KAS_URI + "ESKeyNav/1/NavPropertyETTwoKeyNavMany/1");
    assertEquals(HttpStatusCode.OK.getStatusCode(), keyAsSegment.getResponseCode());
    final String body = readResponse(keyAsSegment);
    assertTrue("the referenced key property must come from the source entity",
        body.contains("\"PropertyInt16\":1"));
    assertTrue("the segment-supplied key property must be applied",
        body.contains("\"PropertyString\":\"1\""));

    final HttpURLConnection parenthesized = get(DEFAULT_URI + "ESKeyNav(1)/NavPropertyETTwoKeyNavMany('1')");
    assertEquals("omitting a constrained key must behave the same in both URL conventions",
        parenthesized.getResponseCode(), keyAsSegment.getResponseCode());
    assertEquals(readResponse(parenthesized), body);
  }

  /** An unmatchable segment for the free key part is a plain 404, not a 400. */
  @Test
  public void referentialConstraintKeyWithUnknownRemainderIsNotFound() throws Exception {
    final HttpURLConnection connection = get(KAS_URI + "ESKeyNav/1/NavPropertyETTwoKeyNavMany/9");
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), connection.getResponseCode());
  }
```
  (`referentialConstraintKeyTakesExactlyOneSegment` at lines 121–131 stays untouched — it pins the parser, not the data layer.)
- [ ] **Step 2: RED** — `mvn -B -Pbuild.fast test -pl lib/server-tecsvc -Dtest=DataProviderTest` (compile failure on the 4-argument `read`), and after installing tecsvc, `mvn verify -pl fit -Pbuild.fast -Dit.test=KeyAsSegmentITCase` (400 instead of 200).
- [ ] **Step 3: Implement.**
  - `DataProvider`: add the source-entity parameter through the chain.

```java
  public Entity read(final EdmEntityType edmEntityType, final EntityCollection entitySet,
      final List<UriParameter> keys) throws DataProviderException {
    return read(edmEntityType, entitySet, keys, null);
  }

  /**
   * Reads a single entity by key predicates.
   * @param sourceEntity the entity the navigation leading here started from; it supplies the values
   *                     of key predicates completed by a referential constraint
   *                     ({@link UriParameter#getReferencedProperty()}), and may be <code>null</code>
   *                     when there is no such entity - such a predicate then matches nothing.
   */
  public Entity read(final EdmEntityType edmEntityType, final EntityCollection entitySet,
      final List<UriParameter> keys, final Entity sourceEntity) throws DataProviderException {
    if (keys.isEmpty()) {
      return null;
    }
    for (final Entity entity : entitySet.getEntities()) {
      if (entityMatchesKeys(edmEntityType, entity, keys, sourceEntity)) {
        return entity;
      }
    }
    return null;
  }
```
    and the same shape for `readDataFromEntity(EdmEntityType, List<UriParameter>)` → delegating to `readDataFromEntity(EdmEntityType, List<UriParameter>, Entity)`.
  - `entityMatchesKeys` gains the `final Entity sourceEntity` parameter and, immediately after `final Object value = findPropertyValue(entity, propertyPath);` / the `value == null` check, this branch (before the `keyProperty`/`EdmPrimitiveType` lines):

```java
        if (key.getReferencedProperty() != null) {
          // The predicate carries no literal: its value comes from the referencing property of the
          // entity the navigation started from (URL Conventions section 4.3.6, referential
          // constraints). Both sides are already typed model values, so they compare directly and no
          // URI-literal round trip is needed.
          if (sourceEntity == null) {
            return false;
          }
          if (!value.equals(findPropertyValue(sourceEntity, key.getReferencedProperty()))) {
            return false;
          }
          continue;
        }
```
  - `TechnicalProcessor.readEntity`: capture the source entity before the navigation step and pass it on. Inside the `while` loop, replace lines 195–205 with:

```java
      final Entity sourceEntity = entity;
      final Link link = entity.getNavigationLink(navigationProperty.getName());
      entity = link == null ? null :
          key.isEmpty() ?
              link.getInlineEntity() :
              dataProvider.read(navigationProperty.getType(), link.getInlineEntitySet(), key, sourceEntity);
      EdmEntityType edmEntityType = getEntityTypeBasedOnNavPropertyTypeCast(uriNavigationResource);
      if (edmEntityType != null) {
        entity = key.isEmpty()
            ? dataProvider.readFirst(edmEntityType)
            : dataProvider.readDataFromEntity(edmEntityType, key, sourceEntity);
      }
```
- [ ] **Step 4: GREEN** — `mvn -B -Pbuild.fast test -pl lib/server-tecsvc`; `mvn install -pl lib/server-tecsvc -Pbuild.fast -DskipTests -q`; `mvn -B install -pl fit -DskipTests -q` (plain, for checkstyle on the changed fit test); `mvn verify -pl fit -Pbuild.fast -Dit.test=KeyAsSegmentITCase+NavigationITCase+EntityReferencesITCase`; then FULL fit `mvn verify -pl fit -Pbuild.fast`; then plain non-fast `mvn -B install -pl lib/server-tecsvc -DskipTests -q`.
- [ ] **Step 5: Commit** — `git commit -m "Resolve referential-constraint key predicates from the source entity"`

---

### Task 5: Stop echoing track-changes on reference collections and add the omit-values builder (tickets E, F)

**Files:**
- Modify: `lib/server-api/src/main/java/org/sitenetsoft/olinguito/server/api/prefer/PreferencesApplied.java` — `Builder` (the `trackChanges()` method is at the block starting after `maxPageSize`; add `omitValues` right after `trackChanges()`).
- Modify test: `lib/server-api/src/test/java/org/sitenetsoft/olinguito/server/api/prefer/PreferencesAppliedTest.java` — siblings `omitValuesRendersUnquoted` (line 49) and `omitValuesDefaultsRendersUnquoted` (line 55).
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/processor/TechnicalEntityProcessor.java` — the Preference-Applied block at lines 737–757 (and line 525, which already uses `preference(...)` and switches to the new builder method).
- Modify test: `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/http/PreferHeaderForGetAndDeleteITCase.java` — siblings `maxPageSizeCombinedWithTrackChanges_GetEntityCollection` (line 453) and `omitValuesNulls_NotAppliedOrEchoedOnReferenceCollection` (line 470).

**Interfaces:**
- Produces `PreferencesApplied.Builder.omitValues(Preferences.OmitValues) : Builder`.
- Consumes `Preferences.OmitValues` (`lib/server-api/.../prefer/Preferences.java`) and `PreferenceName.OMIT_VALUES`.

**Scouting result that re-scopes ticket E:** the ticket described the bug as "Preference-Applied for `maxpagesize`/`track-changes` is only emitted on `$ref` requests". That is not what the code does — `TechnicalEntityProcessor#readEntityCollection` serves both shapes (`readEntityCollection` → `isReference = false`, `readReferenceCollection` → `isReference = true`, lines 117–122 and 454–458) and its Preference-Applied block (lines 737–757) is **outside** the `isReference` branch, so both preferences are echoed for both shapes; `maxPageSizeCombinedWithTrackChanges_GetEntityCollection` pins the non-`$ref` case. The real defect found while scouting is the mirror image: for a `$ref` collection, `ODataJsonSerializer#referenceCollection` (lines 1624–1653) writes only the context URL, the inline count, the `@odata.id` array and the next link — it **never writes a delta link** — yet `preferences.hasTrackChanges()` still adds `odata.track-changes` to Preference-Applied, a false claim about a preference that was not applied. `odata.maxpagesize` is genuinely applied to `$ref` collections (`ServerSidePagingHandler.applyServerSidePaging` runs at line 642, before the `isReference` branch, and `writeNextLink` is called at line 1651), so it must stay ungated. The fix gates only `trackChanges`, symmetric with the existing `omit-values` gate.

- [ ] **Step 1: Write the failing tests.** In `PreferencesAppliedTest` add next to `omitValuesRendersUnquoted`:

```java
  @Test
  void omitValuesBuilderRendersNulls() {
    assertEquals("omit-values=nulls",
        PreferencesApplied.with().omitValues(Preferences.OmitValues.NULLS).build().toValueString());
  }

  @Test
  void omitValuesBuilderRendersDefaults() {
    assertEquals("omit-values=defaults",
        PreferencesApplied.with().omitValues(Preferences.OmitValues.DEFAULTS).build().toValueString());
  }

  @Test
  void omitValuesBuilderIgnoresNull() {
    assertEquals("", PreferencesApplied.with().omitValues(null).build().toValueString());
  }
```
  (add the import `org.sitenetsoft.olinguito.server.api.prefer.Preferences` if the file does not already have it — it is in the same package, so no import is needed for `Preferences` itself.)
  In `PreferHeaderForGetAndDeleteITCase` add next to `omitValuesNulls_NotAppliedOrEchoedOnReferenceCollection`:

```java
  // ODataJsonSerializer#referenceCollection writes only the context URL, the count, the @odata.id
  // array and the next link -- never a delta link. Claiming odata.track-changes was applied to a
  // $ref collection is therefore false; it is gated the same way omit-values already is.
  @Test
  public void trackChanges_NotEchoedOnReferenceCollection() throws Exception {
    URL url = new URL(SERVICE_URI + "ESAllPrim/$ref");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.setRequestProperty(HttpHeader.PREFER, "odata.track-changes");
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertNull(connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertFalse("a reference collection never carries a delta link", content.contains("@odata.deltaLink"));
  }

  // odata.maxpagesize IS genuinely applied to a $ref collection: server-side paging runs before the
  // reference branch and referenceCollection does write a next link. It must keep being echoed.
  @Test
  public void maxPageSize_EchoedOnReferenceCollection() throws Exception {
    URL url = new URL(SERVICE_URI + "ESServerSidePaging/$ref");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.setRequestProperty(HttpHeader.PREFER, "odata.maxpagesize=7");
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("odata.maxpagesize=7", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue("the page must be followable", content.contains("@odata.nextLink"));
  }

  // Both together on a $ref collection: only the one that was really applied is echoed.
  @Test
  public void maxPageSizeWithTrackChanges_OnlyMaxPageSizeEchoedOnReferenceCollection() throws Exception {
    URL url = new URL(SERVICE_URI + "ESServerSidePaging/$ref");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.setRequestProperty(HttpHeader.PREFER, "odata.maxpagesize=7, odata.track-changes");
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("odata.maxpagesize=7", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));
  }
```
- [ ] **Step 2: RED** — `mvn -B -Pbuild.fast test -pl lib/server-api -Dtest=PreferencesAppliedTest` (compile failure on `omitValues`); then `mvn install -pl lib/server-api,lib/server-tecsvc -Pbuild.fast -DskipTests -q && mvn verify -pl fit -Pbuild.fast -Dit.test=PreferHeaderForGetAndDeleteITCase` — `trackChanges_NotEchoedOnReferenceCollection` and `maxPageSizeWithTrackChanges_OnlyMaxPageSizeEchoedOnReferenceCollection` fail (the header carries `odata.track-changes`).
- [ ] **Step 3: Implement.**
  - `PreferencesApplied.Builder`, directly after `trackChanges()`:

```java
    /**
     * Sets the value of the applied preference <code>omit-values</code>
     * (OData 4.01, Part 1: Protocol, section 8.2.8.6). A <code>null</code> argument adds nothing.
     * @param omitValues the omit-values variant that was applied
     * @return this builder
     */
    public Builder omitValues(final Preferences.OmitValues omitValues) {
      if (omitValues != null) {
        add(PreferenceName.OMIT_VALUES.getName(), omitValues.name().toLowerCase(Locale.ROOT));
      }
      return this;
    }
```
  - `TechnicalEntityProcessor`, line 525: replace `.preference(PreferenceName.OMIT_VALUES.getName(), "nulls")` with `.omitValues(Preferences.OmitValues.NULLS)`.
  - `TechnicalEntityProcessor`, lines 741–755: gate track-changes and use the new builder method.

```java
    if (preferences.hasTrackChanges() && !isReference) {
      // A reference collection carries no delta link (ODataJsonSerializer#referenceCollection writes
      // only the context URL, the count, the @odata.id array and the next link), so track-changes is
      // not applied there and must not be echoed -- symmetric with the omit-values gate below.
      // odata.maxpagesize above stays ungated: paging really is applied to $ref collections.
      preferencesAppliedBuilder.trackChanges();
      anyPreferenceApplied = true;
    }
    if (omitNulls && delta == null && !isReference) {
      // Delta payloads never omit nulls (see above), so a delta response must not claim to have
      // applied omit-values either. Reference collections ($ref) never carry properties to omit
      // either (serializeReferenceCollection only ever writes @odata.id), so an omit-values
      // request against a $ref collection must not claim to have been applied there -- symmetric
      // with readEntity's `!isReference` gate on the single-entity $ref case.
      preferencesAppliedBuilder.omitValues(Preferences.OmitValues.NULLS);
      anyPreferenceApplied = true;
    }
```
    Also move the `if (preferences.hasTrackChanges())` delta-link block at lines 703–708 under the same `!isReference` condition so the state and the header stay consistent:
    `if (preferences.hasTrackChanges() && !isReference) { ... entitySetSerialization.setDeltaLink(...); }`
    Remove the now-unused `PreferenceName` import from `TechnicalEntityProcessor` only if no other usage remains (grep the file first).
- [ ] **Step 4: GREEN** — `mvn -B -Pbuild.fast test -pl lib/server-api`; `mvn -B -Pbuild.fast test -pl lib/server-tecsvc`; `mvn install -pl lib/server-api,lib/server-tecsvc -Pbuild.fast -DskipTests -q`; `mvn -B install -pl fit -DskipTests -q` (plain, checkstyle); `mvn verify -pl fit -Pbuild.fast -Dit.test=PreferHeaderForGetAndDeleteITCase`; then FULL fit `mvn verify -pl fit -Pbuild.fast`; then plain non-fast `mvn -B install -pl lib/server-api,lib/server-tecsvc -DskipTests -q`.
- [ ] **Step 5: Commit** — `git commit -m "Echo only the preferences a reference collection really applies"`

---

### Task 6: Centralize optional-parameter default resolution and reject malformed defaults with 400 (tickets I, J)

**Files:**
- New: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/uri/parser/OptionalParameterDefaults.java` (copy the full Apache header from the sibling `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/uri/parser/ParserHelper.java` and add a SiteNetSoft line).
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/uri/parser/ParserHelper.java` — `validateFunctionParameterFacets` at lines 829–862.
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/deserializer/FixedFormatDeserializerImpl.java` — the `parameter(String, EdmParameter)` conversion block at lines 103–122.
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/deserializer/json/ODataJsonDeserializer.java` — `createDefaultParameter` at lines 667–696.
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataProvider.java` — `addOptionalParameterDefaults` at lines 778–802.
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/provider/FunctionProvider.java` — add `nameUFCRTStringOptionalBadDefault` next to `nameUFCRTStringOptionalNoDefault` (lines 193–194) and its `getFunctions` branch next to the one at lines 523–534.
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/provider/SchemaProvider.java` — register the new function next to the existing `UFCRTStringOptionalNoDefault` registration (grep `nameUFCRTStringOptionalNoDefault` in that file).
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/provider/ContainerProvider.java` — add `FunctionImport` `FINRTStringOptionalBadDefault` next to lines 1040–1046, and `ActionImport` `AIRTStringOptionalBadDefault` for `ActionProvider.nameUARTStringOptionalBadDefault` next to line 824.
- Modify test: `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/uri/parser/UriParserTest.java` (a function-parameter test class in that package; if the file does not exist under that exact name, use the sibling that owns the existing optional-parameter parser tests — `grep -rln "UFCRTStringOptionalParam" lib/server-test/src/test`).
- Modify test: `lib/server-tecsvc/src/test/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataProviderTest.java` (siblings `optionalParameterDefaultApplied` around line 190–220).
- Modify test: `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/http/BasicHttpExceptionHandlingITCase.java` (or add the two pins to `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/http/BasicHttpITCase.java` — pick the class whose raw-`HttpURLConnection` helper is closest; state the choice in the commit).

**Interfaces:**
- Produces `OptionalParameterDefaults.defaultLiteral(EdmParameter) : String`, `OptionalParameterDefaults.targetClass(EdmParameter, EdmPrimitiveType) : Class<?>`, `OptionalParameterDefaults.valueOfUriLiteral(EdmParameter, EdmPrimitiveType, String) : Object`, and `OptionalParameterDefaults.isPrimitiveLike(EdmParameter) : boolean`.
- Consumed by `ParserHelper.validateFunctionParameterFacets`, `FixedFormatDeserializerImpl.parameter`, `ODataJsonDeserializer.createDefaultParameter` — all three lose their local copy of the `mapping == null ? getDefaultType() : getMappedJavaClass()` branch.
- Produces the tecsvc EDM names `olingo.odata.test1.UFCRTStringOptionalBadDefault` (function, imported as `FINRTStringOptionalBadDefault`) and the action import `AIRTStringOptionalBadDefault` for the existing `olingo.odata.test1.UARTStringOptionalBadDefault`.

**Scouting result for ticket J:** `UARTStringOptionalBadDefault` is *not* inert — `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/deserializer/json/ODataJsonDeserializerActionParametersTest.java:298` (`optionalParameterInvalidDefault`) already exercises it and pins `MessageKeys.INVALID_VALUE_FOR_PROPERTY`. What it lacks is an `ActionImport`, so it is unreachable over HTTP and the 400 was never proven end to end. This task adds that import plus a fit pin, and adds the symmetric *function* fixture the URL path needs. Metadata assertions in the repo use `containsString` only (`lib/server-test/.../serializer/xml/MetadataDocumentTest.java`), so the two new container children are safe.

- [ ] **Step 1: Write the failing tests.**
  - In the parser test class (the one that owns `UFCRTStringOptionalParam`):

```java
  @Test
  void functionWithMalformedOptionalParameterDefaultIsBadRequest() {
    // Core.OptionalParameter DefaultValue "-notALiteral" is not a valid Edm.String URI literal;
    // the URI parser must reject the call with 400 instead of letting the service fail with 500.
    testUri.runEx("FINRTStringOptionalBadDefault(ParameterString='x')")
        .isExValidation(UriValidationException.MessageKeys.INVALID_VALUE_FOR_PROPERTY);
  }

  @Test
  void functionWithValidOptionalParameterDefaultStillParses() throws Exception {
    testUri.run("FINRTStringOptionalParam(ParameterString='x')").goPath()
        .first().isFunctionImport("FINRTStringOptionalParam");
  }
```
  (use the validator name and assertion helpers the surrounding tests in that file use — `testUri`/`TestUriValidator` and `isExValidation`; check the neighbouring optional-parameter tests and match them exactly. `FINRTStringOptionalParam` is the existing import name at `ContainerProvider.java:1040` — read the actual `setName(...)` there and use it verbatim.)
  - In `DataProviderTest`, next to the existing optional-parameter tests:

```java
  @Test
  void malformedOptionalParameterDefaultIsBadRequest() throws Exception {
    final DataProvider dataProvider = new DataProvider(oData, edm);
    final EdmFunction function = edm.getUnboundFunction(
        new FullQualifiedName(SchemaProvider.NAMESPACE, "UFCRTStringOptionalBadDefault"),
        List.of("ParameterString"));
    Assertions.assertNotNull(function);
    final DataProvider.DataProviderException exception =
        Assertions.assertThrows(DataProvider.DataProviderException.class,
            () -> dataProvider.readFunctionPrimitiveComplex(function,
                List.of(mockParameter("ParameterString", "'x'")), null));
    Assertions.assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), exception.getStatusCode());
  }
```
  (check `DataProviderException`'s accessor name — `grep -n "class DataProviderException" -A 20 lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataProvider.java` — and use it verbatim; add the `HttpStatusCode` import.)
  - In the chosen fit http test class:

```java
  /** A malformed Core.OptionalParameter DefaultValue is bad client-visible model input, not a 500. */
  @Test
  public void functionWithMalformedOptionalParameterDefaultIsBadRequest() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET,
        "FINRTStringOptionalBadDefault(ParameterString='x')");
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
  }

  /** The same malformed default reached through an action body is a 400 as well. */
  @Test
  public void actionWithMalformedOptionalParameterDefaultIsBadRequest() throws Exception {
    final HttpURLConnection connection = post("AIRTStringOptionalBadDefault", "{}");
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
  }
```
  (use the class's own connection helpers; if it has none matching, copy the `getConnection(...)` helper from `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/http/QueryPostITCase.java` lines 190–225.)
- [ ] **Step 2: RED** — `mvn install -pl lib/server-core,lib/server-tecsvc -Pbuild.fast -DskipTests -q && mvn -B -Pbuild.fast test -pl lib/server-test -Dtest=UriParserTest` and `mvn -B -Pbuild.fast test -pl lib/server-tecsvc -Dtest=DataProviderTest`. Expect: the parser test fails because the parser does not look at defaults at all; the tecsvc test fails because `addOptionalParameterDefaults` reports `INTERNAL_SERVER_ERROR`.
- [ ] **Step 3: Implement.**
  - New `OptionalParameterDefaults`:

```java
package org.sitenetsoft.olinguito.server.core.uri.parser;

import org.sitenetsoft.olinguito.commons.api.edm.EdmMapping;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;

/**
 * Single place where an operation parameter's URI literal is turned into a typed value, and where
 * the default value of an omitted optional parameter (OData 4.01, Part 1: Protocol, section
 * 11.5.4.1.1; Core vocabulary term <code>Core.OptionalParameter</code>) is read. The Core vocabulary
 * states the default value uses "the same rules as the cast function in URLs", so it is a URI
 * literal and goes through exactly the same conversion as a value supplied in the request.
 */
public final class OptionalParameterDefaults {

  private OptionalParameterDefaults() {
    // utility class
  }

  /**
   * @param parameter the operation parameter
   * @return the parameter's default value as URI literal, or <code>null</code> when the parameter is
   *         not optional or declares no default value
   */
  public static String defaultLiteral(final EdmParameter parameter) {
    return parameter.isOptional() ? parameter.getOptionalDefaultValue() : null;
  }

  /**
   * @param parameter the operation parameter
   * @return whether the parameter is a non-collection primitive, type definition or enumeration and
   *         therefore carries a URI literal rather than a JSON payload
   */
  public static boolean isPrimitiveLike(final EdmParameter parameter) {
    final EdmType type = parameter.getType();
    final EdmTypeKind kind = type == null ? null : type.getKind();
    return !parameter.isCollection()
        && (kind == EdmTypeKind.PRIMITIVE || kind == EdmTypeKind.DEFINITION || kind == EdmTypeKind.ENUM);
  }

  /**
   * @param parameter the operation parameter
   * @param primitiveType the parameter's type
   * @return the Java class a value of this parameter is materialized as, honoring an
   *         {@link EdmMapping} when the provider declared one
   */
  public static Class<?> targetClass(final EdmParameter parameter, final EdmPrimitiveType primitiveType) {
    final EdmMapping mapping = parameter.getMapping();
    return mapping == null || mapping.getMappedJavaClass() == null
        ? primitiveType.getDefaultType() : mapping.getMappedJavaClass();
  }

  /**
   * Converts a URI literal into the parameter's typed value, applying the parameter's facets.
   * @param parameter the operation parameter
   * @param primitiveType the parameter's type
   * @param uriLiteral the URI literal to convert; may be <code>null</code>
   * @return the typed value
   * @throws EdmPrimitiveTypeException if the literal is not valid for the parameter
   */
  public static Object valueOfUriLiteral(final EdmParameter parameter, final EdmPrimitiveType primitiveType,
      final String uriLiteral) throws EdmPrimitiveTypeException {
    return primitiveType.valueOfString(primitiveType.fromUriLiteral(uriLiteral),
        parameter.isNullable(), parameter.getMaxLength(), parameter.getPrecision(), parameter.getScale(),
        true, targetClass(parameter, primitiveType));
  }
}
```
  - `ParserHelper.validateFunctionParameterFacets`: replace the duplicated `if (edmParameter.getMapping() == null) { ... } else { ... }` block (lines 845–856) with a single `OptionalParameterDefaults.valueOfUriLiteral(edmParameter, primitiveType, text);`, replace the local `kind`/`isCollection` test with `OptionalParameterDefaults.isPrimitiveLike(edmParameter)`, and append the new default-validation loop before the method's closing brace:

```java
    // OData 4.01, Part 1: Protocol section 11.5.4.1.1 -- an optional parameter omitted from the URL
    // takes its Core.OptionalParameter DefaultValue. A default that is not a valid URI literal is
    // rejected here, as bad input to this call (400), rather than surfacing later as a 500.
    final Set<String> supplied = new HashSet<>();
    for (final UriParameter parameter : parameters) {
      supplied.add(parameter.getName());
    }
    for (final String name : function.getParameterNames()) {
      if (supplied.contains(name)) {
        continue;
      }
      final EdmParameter edmParameter = function.getParameter(name);
      final String defaultLiteral = OptionalParameterDefaults.defaultLiteral(edmParameter);
      if (defaultLiteral == null || !OptionalParameterDefaults.isPrimitiveLike(edmParameter)) {
        continue;
      }
      try {
        OptionalParameterDefaults.valueOfUriLiteral(edmParameter,
            (EdmPrimitiveType) edmParameter.getType(), defaultLiteral);
      } catch (final EdmPrimitiveTypeException e) {
        throw new UriValidationException(
            "Invalid default value '" + defaultLiteral + "' for parameter " + name, e,
            UriValidationException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, name);
      }
    }
```
    (add the `java.util.HashSet` / `java.util.Set` imports if missing.)
  - `FixedFormatDeserializerImpl.parameter`: replace the mapping if/else (lines 105–117) with

```java
        result.setValue(type.getKind() == EdmTypeKind.ENUM ? ValueType.ENUM : ValueType.PRIMITIVE,
            OptionalParameterDefaults.valueOfUriLiteral(parameter, primitiveType, content));
```
    and replace the surrounding `kind` test with `OptionalParameterDefaults.isPrimitiveLike(parameter)`; keep the existing `catch (EdmPrimitiveTypeException)` → `DeserializerException` unchanged.
  - `ODataJsonDeserializer.createDefaultParameter`: replace the first line with `final String defaultValue = OptionalParameterDefaults.defaultLiteral(edmParameter);`, and the mapping if/else (lines 679–692) with `final Object value = OptionalParameterDefaults.valueOfUriLiteral(edmParameter, primitiveType, defaultValue);`; keep the `catch` and the `ValueType.ENUM` decision unchanged.
  - `DataProvider.addOptionalParameterDefaults`: change `HttpStatusCode.INTERNAL_SERVER_ERROR` to `HttpStatusCode.BAD_REQUEST` and extend the javadoc with: `The URI parser already rejects a malformed default (ParserHelper#validateFunctionParameterFacets), so this branch is defense in depth for callers that bypass the parser; it reports 400 for the same reason.`
  - `FunctionProvider`: add

```java
  public static final FullQualifiedName nameUFCRTStringOptionalBadDefault =
      new FullQualifiedName(SchemaProvider.NAMESPACE, "UFCRTStringOptionalBadDefault");
```
    and the `getFunctions` branch (mirroring `nameUFCRTStringOptionalNoDefault`, lines 523–534) whose `ParameterSuffix` carries `optionalParameterAnnotation("-notALiteral")` — the same malformed literal `ActionProvider` uses at line 216, with the comment `// The default value is not a valid Edm.String URI literal (no quotes); invoking this function must be a 400.`
  - `SchemaProvider`: register the new function alongside the other `UFCRTStringOptional*` registrations.
  - `ContainerProvider`: add a `CsdlFunctionImport` named `FINRTStringOptionalBadDefault` for it (copy the shape of the `FINRTStringOptionalNoDefault`/`...OptionalParam` imports at lines 1040–1046) and a `CsdlActionImport` named `AIRTStringOptionalBadDefault` for `ActionProvider.nameUARTStringOptionalBadDefault` (copy the shape at line 824).
- [ ] **Step 4: GREEN** — `mvn -B -Pbuild.fast test -pl lib/server-core,lib/server-tecsvc`; `mvn install -pl lib/server-core,lib/server-tecsvc -Pbuild.fast -DskipTests -q`; `mvn -B -Pbuild.fast test -pl lib/server-test` (must include `ODataJsonDeserializerActionParametersTest#optionalParameterInvalidDefault` and the whole `UriParserTest`/`ResourcePathParserTest` suites staying green); `mvn -B install -pl fit -DskipTests -q` (plain, checkstyle); `mvn verify -pl fit -Pbuild.fast -Dit.test=FunctionImportITCase+ActionImportITCase+BasicHttpExceptionHandlingITCase`; then FULL fit `mvn verify -pl fit -Pbuild.fast`; then plain non-fast `mvn -B install -pl lib/server-core,lib/server-tecsvc -DskipTests -q`.
- [ ] **Step 5: Commit** — `git commit -m "Reject malformed optional-parameter defaults with one shared resolver"`

---

### Task 7: Pin the three-way /$query composition and document the trailing-slash edge (tickets G, H)

**Files:**
- Modify test: `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/http/QueryPostITCase.java` — add three tests next to `queryPostOmitValuesNullsComposesWithGetOnlyGate` (lines 147–159); reuse the file's own `getConnection(method, pathAndQuery, body, contentType, preferHeader)` (lines 209–225), `readResponse` (line 253) and `countOccurrences` (line 160) helpers.
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/ODataHandlerImpl.java` — `handleQueryPathIfPresent`, the early return at lines 312–315.
- Modify test: `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/ODataHandlerImplTest.java` — add next to `queryPostCaseVarianceNotStripped` (line 1621), reusing `dispatchQueryPost` (line 1736).

**Interfaces:** consumes only existing behavior. Produces no new API.

**Seed check (`DataCreator#createESTwoPrim`, lines 1054–1077):** `ESTwoPrim` holds `(32766,"Test String1")`, `(-365,"Test String2")`, `(-32766, null)`, `(32767,"Test String4")`. The null-valued `PropertyString` on `ESTwoPrim(-32766)` is the only declared null in this set — that is what `omit-values=nulls` omits, and `matchesPattern(PropertyString, ...) eq null` is exactly the filter that selects it (`matchesPattern` over a null input is null, see `FilterSystemQueryITCase#matchesPatternWithNull`).

- [ ] **Step 1: Write the failing tests.** In `QueryPostITCase`:

```java
  /**
   * Three-way composition pin: <tt>/$query</tt> POST (URL Conventions section 4.17) x
   * <tt>Prefer: omit-values=nulls</tt> (Protocol section 8.2.8.6) x the 4.01 <tt>matchesPattern</tt>
   * filter function, with a pattern built entirely from regex metacharacters that are also URL
   * metacharacters, so the body's percent-encoding is genuinely stressed:
   * <tt>^Test+String?[0-9]+$</tt> sent as <tt>%5ETest%2BString%3F%5B0-9%5D%2B%24</tt>.
   * <tt>matchesPattern</tt> over a null input is null, so <tt>eq null</tt> selects exactly
   * ESTwoPrim(-32766) - the one entity whose PropertyString is null and therefore the one entity for
   * which omit-values=nulls has anything to omit.
   */
  @Test
  public void queryPostOmitValuesAndMatchesPatternComposeOnNullMatch() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESTwoPrim/$query",
        "$filter=matchesPattern(PropertyString,'%5ETest%2BString%3F%5B0-9%5D%2B%24') eq null",
        TEXT_PLAIN, "omit-values=nulls");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("omit-values=nulls", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));

    final String body = readResponse(connection);
    assertEquals("exactly one entity has a null PropertyString", 1, countOccurrences(body, "\"PropertyInt16\":"));
    assertTrue("the null-valued entity must be the one selected", body.contains("\"PropertyInt16\":-32766"));
    assertFalse("the null property must be omitted, not written as null", body.contains("PropertyString"));
  }

  /**
   * The same percent-encoded metacharacters must reach the filter parser intact: with the literal
   * space instead of <tt>+</tt> the pattern <tt>^Test String[0-9]+$</tt> matches the three entities
   * whose PropertyString is "Test String1", "Test String2" and "Test String4".
   */
  @Test
  public void queryPostMatchesPatternMetacharactersSurviveBodyEncoding() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESTwoPrim/$query",
        "$filter=matchesPattern(PropertyString,'%5ETest%20String%5B0-9%5D%2B%24')",
        TEXT_PLAIN, "omit-values=nulls");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("omit-values=nulls", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));

    final String body = readResponse(connection);
    assertEquals("three seeded entities match the pattern", 3, countOccurrences(body, "\"PropertyInt16\":"));
    assertTrue("non-null strings are untouched by omit-values", body.contains("\"PropertyString\":\"Test String1\""));
    assertFalse("the null-valued entity must not match", body.contains("\"PropertyInt16\":-32766"));
  }

  /**
   * An empty result still honors the preference: the Preference-Applied header is driven by the
   * request, not by whether any property happened to be omitted.
   */
  @Test
  public void queryPostOmitValuesEchoedOnEmptyMatchesPatternResult() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "ESTwoPrim/$query",
        "$filter=matchesPattern(PropertyString,'%5EZZZ%5B0-9%5D%2B%24')", TEXT_PLAIN, "omit-values=nulls");
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("omit-values=nulls", connection.getHeaderField(HttpHeader.PREFERENCE_APPLIED));
    assertTrue("the result must be empty", readResponse(connection).contains("\"value\":[]"));
  }
```
  In `ODataHandlerImplTest`, next to `queryPostCaseVarianceNotStripped`:

```java
  @Test
  void queryPostTrailingSlashIsNotAQueryPathAndIsRejected() throws Exception {
    // Deliberate: section 4.17 names the segment "/$query", and the segment-match is on the path's
    // exact suffix, so "ESAllPrim/$query/" is not rewritten. It then reaches the URI parser as a
    // path with a literal "$query" segment after an entity collection, which is not a resource -
    // a 400, and the processor is never reached.
    final EntityCollectionProcessor processor = mock(EntityCollectionProcessor.class);
    final ODataResponse response = dispatchQueryPost("ESAllPrim/$query/", null, "$top=1",
        ContentType.TEXT_PLAIN.toContentTypeString(), processor);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatusCode());
    verifyNoInteractions(processor);
  }
```
- [ ] **Step 2: RED** — `mvn -B -Pbuild.fast test -pl lib/server-test -Dtest=ODataHandlerImplTest`; `mvn -B install -pl fit -DskipTests -q && mvn verify -pl fit -Pbuild.fast -Dit.test=QueryPostITCase`. If any expectation turns out different from the assertions above (the exact status code of the trailing-slash case, or the entity counts), fix the *test* to the observed truth and record the observed value in the commit body — do not weaken an assertion into a tautology.
- [ ] **Step 3: Implement.** Only the explanatory comment in `ODataHandlerImpl.handleQueryPathIfPresent`, immediately above the early return:

```java
    // OData 4.01 URL Conventions section 4.17 names the segment "/$query" and this match is on the
    // path's exact suffix, deliberately: a trailing slash ("ESAllPrim/$query/") is NOT a $query
    // request. It is left untouched here and the URI parser then rejects the literal "$query"
    // segment with a 400, which is the intended outcome - silently accepting the variant would make
    // the rewritten path ambiguous with a real resource segment named "$query".
    if (rawODataPath == null || !rawODataPath.endsWith(QUERY_PATH_SEGMENT)) {
      return;
    }
```
- [ ] **Step 4: GREEN** — `mvn -B install -pl lib/server-core -DskipTests -q` (plain, checkstyle) and `mvn install -pl lib/server-core -Pbuild.fast -DskipTests -q`; `mvn -B -Pbuild.fast test -pl lib/server-test -Dtest=ODataHandlerImplTest`; `mvn -B install -pl fit -DskipTests -q`; `mvn verify -pl fit -Pbuild.fast -Dit.test=QueryPostITCase+FilterSystemQueryITCase+PreferHeaderForGetAndDeleteITCase`; then FULL fit `mvn verify -pl fit -Pbuild.fast`.
- [ ] **Step 5: Commit** — `git commit -m "Pin the query-body, omit-values and matchesPattern composition"`

---

### Task 8: Update the feature guide and run the full gate

**Files:**
- Modify: `docs/site/guides/odata-401-features-guide.md` — four passages that these fixes invalidate:
  - lines ~468–472 ("A default that fails to parse is the service's own model error, so it is reported as 500, not as a 400 like bad client input.") → now a 400, raised by the URI parser before the service is reached (Task 6).
  - lines ~646–651, the "One reference-service caveat" paragraph asserting `ESKeyNav/1/NavPropertyETTwoKeyNavMany/1` answers **400 `Wrong key!`** → now a 200 returning `ESTwoKeyNav(1,'1')` in both conventions (Task 4).
  - lines ~709–711, the bullet "The flag reaches the URI parser through `ODataHandler` only. `UriHelper.parseEntityId(...)` and `server-core-ext`'s `ServiceRequest` construct their own `Parser` without it" → `UriHelperImpl.setKeyAsSegment(boolean)` / `OData.createUriHelper(boolean)` now cover `parseEntityId`; `server-core-ext` remains the documented seam and keeps a bullet of its own with the reason (no `ODataHandler` in that module; model flags still honored).
  - line ~713, the bullet "A key value covered by a referential constraint cannot be supplied as a segment (see above)" → keep the parser statement (it still MUST be omitted) but drop the data-layer caveat it referred to.
  - In the omit-values section, extend the "Never on `$ref` / reference reads" bullet to state that `odata.track-changes` is now gated the same way (a reference collection carries no delta link) while `odata.maxpagesize` is still echoed there because paging really applies (Task 5).
- Modify (optional but preferred): the same guide's Preference section — mention `PreferencesApplied.Builder.omitValues(Preferences.OmitValues)` where the other builder methods are listed.

- [ ] **Step 1: Docs.** Make the five edits above. Verify every quoted status code and URL against the tests written in Tasks 4–6 (`KeyAsSegmentITCase#referentialConstraintKeyIsOmitted`, `PreferHeaderForGetAndDeleteITCase#trackChanges_NotEchoedOnReferenceCollection`, the new function-import 400 pins). Commit: `git commit -m "Document the Tier 5 follow-up fixes"`.
- [ ] **Step 2: Full gate.** `mvn -B install --fail-at-end -Dquarkus.http.test-port=8083`. Never use port 8081.
- [ ] **Step 3:** Fix anything the gate finds that this branch caused; separate commits, plain imperative subjects. Do NOT push.

---

## Dropped / Re-scoped

- **Ticket E, as described, is dropped: there is no `$ref` gating of `maxpagesize`/`track-changes`.** Evidence: `TechnicalEntityProcessor#readEntityCollection` is one method serving both shapes (`readEntityCollection` passes `isReference = false` at line 121, `readReferenceCollection` passes `true` at line 458), and its Preference-Applied block at lines 737–757 sits outside the `if (isReference)` branch at line 707, so both preferences are already emitted for both shapes. `PreferHeaderForGetAndDeleteITCase#maxPageSizeCombinedWithTrackChanges_GetEntityCollection` (line 453) pins the non-`$ref` case today. Task 5 instead fixes the *inverse* defect found while scouting — `odata.track-changes` is echoed for `$ref` collections although `ODataJsonSerializer#referenceCollection` (lines 1624–1653) never writes a delta link — and adds the missing `$ref` pins for both preferences.
- **Ticket D's server-core-ext half is dropped as code and kept as a documented seam.** Evidence: `lib/server-core-ext` contains no `ODataHandler` implementation; `ServiceDispatcher` is constructed directly by callers (`ServiceRequest.java:347`, `requests/BatchRequest.java:155`) and `ServiceRequest.parseLink` builds its own `Parser` (line 345), so there is no service-level flag in that module to thread without adding public constructor/setter surface to three classes and every caller. Per-model `keyAsSegmentAllowed` flags still work there (`ResourcePathParser.java:222,226`). Task 2 adds the javadoc at both sites and Task 8 records it in the guide.
- **Ticket D is also not wired into the tecsvc key-as-segment endpoint.** Evidence: the `DataProvider` that calls `parseEntityId` (line 937) is created once per session and shared by both endpoints (`TechnicalServlet.java:91`, quarkus `SessionManager.java:53`), while the key-as-segment opt-in lives on the per-request handler (`TechnicalKeyAsSegmentServlet#configureHandler`). Threading it would mean a per-endpoint `DataProvider`. The new API is pinned by unit tests in `UriHelperTest` instead.
- **Ticket J's "inert" premise is dropped.** Evidence: `UARTStringOptionalBadDefault` is already exercised by `lib/server-test/.../ODataJsonDeserializerActionParametersTest.java:298` (`optionalParameterInvalidDefault`). What it lacked was an `ActionImport` making it reachable over HTTP; Task 6 adds that import and the end-to-end 400 pin rather than removing the fixture.

## Self-Review Notes

**Spec coverage.** Ticket A (EDM cache identity) is an implementation-quality fix with no spec surface. Ticket D touches URL Conventions §4.3.6, whose key-as-segment support is a MAY: the convention stays off by default on every entry point, and the parenthesized form keeps parsing in both modes (pinned by `parseEntityIdParenthesizedStillWorksWithKeyAsSegmentEnabled`). Tickets B and C are reference-service behavior, not spec text; C makes the reference service actually satisfy §4.3.6's MUST that referential-constraint-covered key properties be omitted from the URL — before this, the URL parsed but the service could not answer it. Ticket E is Protocol §8.2.8.6 plus RFC 7240: `Preference-Applied` must only name preferences that were applied, which is exactly the `track-changes`-on-`$ref` false claim being removed; `odata.maxpagesize` remains echoed there because paging is genuinely applied. Ticket F is a convenience API only — the rendered header is byte-identical to the `preference("omit-values", ...)` form already pinned by `omitValuesRendersUnquoted`. Ticket I is Protocol §11.5.4.1.1 and the `Core.OptionalParameter` vocabulary term: the default is a URI literal, and a malformed one is now surfaced as client-visible 400 on both the URL path and the action-body path, closing the asymmetry the Wave 2 review flagged. Tickets G and H are pins and documentation for §4.17 and §8.2.8.6 composition; no behavior change beyond a comment.

**Type consistency.** `AbstractEdm.cacheEntityContainer` keeps its `void` return type (binary compatibility for any external subclass or caller) and delegates to the new `cacheEntityContainerIfAbsent`, which returns `EdmEntityContainer`; the new `cachedEntityContainer` returns `null` rather than creating, so it can never recurse into `createEntityContainer`. `UriHelper` (the interface) is unchanged — the flag lives on the concrete `UriHelperImpl` and on a new *concrete* `OData.createUriHelper(boolean)` that defaults to `createUriHelper()`, so neither addition breaks an existing implementor. `DataProvider.read`/`readDataFromEntity` keep their existing arities as delegating overloads, so the two `TechnicalProcessor` call sites are the only production changes and `ActionData` compiles untouched; the new `readFirst` overloads are distinguished by `EdmEntitySet` vs `EdmEntityType`, which are unrelated interfaces, so no ambiguity arises. `entityMatchesKeys` compares a referenced key predicate as *model values on both sides* (`Object.equals`) rather than round-tripping through a URI literal, which avoids reintroducing the `EdmPrimitiveTypeException` → `Wrong key!` path this ticket exists to remove; a missing source entity returns `false` (404) instead of throwing (400/500). `PreferencesApplied.Builder.omitValues` takes the existing `Preferences.OmitValues` enum and lowercases its name exactly as `returnRepresentation(Return)` does, so `OmitValues.NULLS` renders as `nulls` and stays in `SAFE_PREFERENCE_NAMES` (unquoted). `OptionalParameterDefaults` is a package-public utility in `server-core`'s `uri.parser` package returning plain `Object`/`Class<?>`/`String`, so all three call sites keep their own exception translation (`UriValidationException` in the parser, `DeserializerException` in both deserializers) and no exception type crosses a module boundary it did not already cross.
