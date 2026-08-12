# OpenType Dynamic-Property CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serve `GET`/`PUT`/`PATCH`/`DELETE` on direct dynamic-property paths (replacing the deliberate 501) and annotate client dynamic collection writes.

**Architecture:** Spec: `docs/superpowers/specs/2026-08-11-opentype-crud-design.md`. Extends the merged OpenType feature (`0c1711a69`): dispatcher case + validator permissions route dynamic segments into the existing primitive dispatch flow; an additive `default` method on `ODataDeserializer` handles single-property payloads without an `EdmProperty`; tecsvc implements the CRUD semantics; client mirrors the scalar annotation rule for collections.

**Tech Stack:** Java 17, Maven; modules `lib/server-api`, `lib/server-core`, `lib/server-tecsvc`, `lib/server-test`, `lib/client-core`, `fit`.

## Global Constraints

- Branch: `feature/opentype-crud` off master.
- **NO AI attribution in commit messages** (project directive 2026-08-08).
- SiteNetSoft copyright line before the Apache header's closing `*/` on modified source files; new files get the full header + line (copy a sibling's).
- Checkstyle LineLength max **120**.
- Closed-type behavior unchanged — every opened gate adds/keeps a closed-type pin.
- `$value` after a dynamic segment stays rejected — pin `pathValueAfterDynamicPropertyRejected` in `OpenTypeUriParserTest` must keep passing.
- `server-test`/`fit` run against installed jars: `mvn install -pl <changed modules> -Pbuild.fast -DskipTests -q` before testing there.
- Full plain build gate: `mvn -B install --fail-at-end -Dquarkus.http.test-port=8083` (port 8081 is held by the user's unrelated Keycloak container).
- Be generous with tests (user directive): cover error/edge cases beyond the listed minimum whenever cheap.
- tecsvc test model facts (from the merged feature): open type `olingo.odata.test1.ETOpen` at `ESOpen`; entity 1 = `PropertyInt16=1`, `DynamicString="dynamic"`, `DynamicInt=42` (Int64), `PropertyComp{CompString, CompDynamic}`; entity 2 = `DynamicInt=7`; entity 3 = no dynamics. Closed type `ETTwoPrim` for pins. Dynamic seeds carry `Property.getType()==null`.

---

### Task 1: `ODataDeserializer.dynamicProperty` — additive API + JSON implementation

**Files:**
- Modify: `lib/server-api/src/main/java/org/sitenetsoft/olinguito/server/api/deserializer/ODataDeserializer.java` (interface, `property(...)` is at :73 — add sibling default method)
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/deserializer/json/ODataJsonDeserializer.java` (override; reuse the existing dynamic helpers: `createDynamicProperty`, `createDynamicCollectionProperty`, `resolveAnnotatedPrimitiveType`, `inferPrimitiveTypeName`)
- Test: `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/deserializer/json/ODataJsonDeserializerDynamicPropertyTest.java` (create; extend `AbstractODataDeserializerTest`, mirror `ODataJsonDeserializerOpenTypeTest` setup)

**Interfaces:**
- Produces (exact, used by Tasks 3-4):

```java
default DeserializerResult dynamicProperty(InputStream stream, String propertyName)
    throws DeserializerException {
  throw new DeserializerException("Dynamic property deserialization is not supported by this deserializer.",
      DeserializerException.MessageKeys.NOT_IMPLEMENTED);
}
```

The JSON override parses a single-property JSON payload of the standard property-payload shape `{"value": <v>}` (mirror how declared-property payloads are read in `property(...)` — read that method first) plus an optional `"value@odata.type"` sibling (and top-level `@odata.type` variant if `property(...)` honors one — match its shape), builds the `Property` named `propertyName` via the existing dynamic inference/annotation helpers, and returns `DeserializerResult` the same way `property(...)` does.

- [ ] **Step 1: Write the failing tests** (generous set):

```java
@Test
void bareStringValue() throws Exception {
  final Property p = deserializeDynamic("{\"value\":\"hello\"}", "Custom");
  assertEquals("Custom", p.getName());
  assertEquals("hello", p.getValue());
  assertEquals("Edm.String", p.getType());
}

@Test
void annotatedGuidValue() throws Exception {
  final Property p = deserializeDynamic(
      "{\"value@odata.type\":\"#Guid\",\"value\":\"01234567-89ab-cdef-0123-456789abcdef\"}", "Ref");
  assertEquals("Edm.Guid", p.getType());
  assertEquals(java.util.UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"), p.getValue());
}

@Test
void nullValue() throws Exception {
  final Property p = deserializeDynamic("{\"value\":null}", "Gone");
  assertTrue(p.isNull());
}

@Test
void collectionValue() throws Exception {
  final Property p = deserializeDynamic("{\"value\":[\"a\",\"b\"]}", "Tags");
  assertEquals(ValueType.COLLECTION_PRIMITIVE, p.getValueType());
  assertEquals(List.of("a", "b"), p.asCollection());
}

@Test
void annotatedCollectionValue() throws Exception {
  final Property p = deserializeDynamic(
      "{\"value@odata.type\":\"#Collection(Guid)\",\"value\":[\"01234567-89ab-cdef-0123-456789abcdef\"]}", "Refs");
  assertEquals("Collection(Edm.Guid)".replace("Collection(", "").replace(")", ""), p.getType()); // adjust to actual convention: read how entity-level annotated collections store type and assert THAT
}

@Test
void objectValueRejected() {
  assertThrows(DeserializerException.class, () -> deserializeDynamic("{\"value\":{\"a\":1}}", "Nested"));
}

@Test
void unknownAnnotatedTypeRejected() {
  assertThrows(DeserializerException.class,
      () -> deserializeDynamic("{\"value@odata.type\":\"#No.Such\",\"value\":\"x\"}", "X"));
}

@Test
void garbagePayloadRejected() {
  assertThrows(DeserializerException.class, () -> deserializeDynamic("not json", "X"));
}

@Test
void defaultMethodThrowsNotImplemented() {
  final ODataDeserializer xml = odata.createDeserializer(ContentType.APPLICATION_XML);
  assertThrows(DeserializerException.class,
      () -> xml.dynamicProperty(new ByteArrayInputStream("<x/>".getBytes(UTF_8)), "X"));
}
```

(`deserializeDynamic(payload, name)` = 3-line helper calling `odata.createDeserializer(ContentType.JSON).dynamicProperty(...)`. For the annotated-collection assertion: read first how the entity-level path stores collection types on `Property.getType()` and assert the real convention — don't guess.)

- [ ] **Step 2: RED** — `mvn install -pl lib/server-api -Pbuild.fast -DskipTests -q` then compile tests: expected COMPILE ERROR (method missing) — valid RED.
- [ ] **Step 3: Implement** interface default + JSON override.
- [ ] **Step 4: GREEN** — `mvn install -pl lib/server-api,lib/server-core -Pbuild.fast -DskipTests -q && mvn test -Dtest=ODataJsonDeserializerDynamicPropertyTest -pl lib/server-test`; then regression `mvn test -Dtest='ODataJsonDeserializer*Test' -pl lib/server-test`.
- [ ] **Step 5: Commit** — `git commit -m "Add dynamic-property deserialization entry point"`

---

### Task 2: Dispatcher routing + validator permissions

**Files:**
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/ODataDispatcher.java` — add `case dynamicProperty:` beside `case primitiveProperty:` (:172), calling `handlePrimitiveDispatching(request, response, false)`. Read `handlePrimitiveDispatching` (:458+) end to end first: the GET branch needs nothing extra; the PUT/PATCH branch casts the last segment for the EDMSTREAM check (`instanceof UriResourcePrimitiveProperty`, :480) — a dynamic segment must skip that stream special-case cleanly (it's an `instanceof`, so it already does — verify); the DELETE branch may consult the EDM property for nullability — dynamic segments must bypass any such check (guard with `instanceof`).
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/uri/validator/UriValidator.java` — `validatePropertyOperations` (:361): its `property.isNullable()` DELETE check dereferences the EDM property; for a dynamic last segment there is none — allow DELETE/PUT/PATCH through (dynamic properties are always conceptually nullable/removable).
- Modify: `lib/server-test/.../server/core/ODataHandlerImplTest.java` — REPLACE the 501 pin (`dynamicPropertyDirectPathAddressingIsNotServedByDispatcher`) with routing assertions.
- Test: extend `lib/server-test/.../uri/validator/UriValidatorTest.java` for method permissions.

**Interfaces:** consumes Task 6 (merged feature) artifacts only; produces dispatch reaching `PrimitiveProcessor.readPrimitive/updatePrimitive/deletePrimitive` for dynamic segments.

- [ ] **Step 1: RED tests** — in `ODataHandlerImplTest`, using its `dispatch(...)` helper + a mocked `PrimitiveProcessor` (the file already mocks processors — follow `dispatch`-based sibling tests):

```java
@Test
void dynamicPropertyGetRoutesToPrimitiveProcessor() throws Exception {
  final PrimitiveProcessor processor = mock(PrimitiveProcessor.class);
  dispatch(HttpMethod.GET, "ESOpen(1)/DynamicString", processor);
  verify(processor).readPrimitive(any(), any(), any(), any());
}

@Test
void dynamicPropertyPutRoutesToUpdatePrimitive() throws Exception { /* same shape, HttpMethod.PUT + a JSON body + verify(processor).updatePrimitive(...) — copy body/header wiring from an existing PUT dispatch test */ }

@Test
void dynamicPropertyPatchRoutesToUpdatePrimitive() throws Exception { /* PATCH variant */ }

@Test
void dynamicPropertyDeleteRoutesToDeletePrimitive() throws Exception { /* verify(processor).deletePrimitive(...) */ }

@Test
void closedTypeUnknownSegmentStill404s() throws Exception { /* ESTwoPrim(...)/Nope -> assert 404/PROPERTY_NOT_IN_TYPE via the handler response, mirroring existing negative dispatch tests */ }
```

In `UriValidatorTest`: PUT/PATCH/DELETE on `ESOpen(1)/DynamicString` validate OK; keep/verify GET case; `$value` rejection pin still passing.

- [ ] **Step 2: RED** — GET/PUT/PATCH/DELETE tests fail (501 today). The old 501 pin now contradicts — delete it in the same commit as the fix (note in report).
- [ ] **Step 3: Implement** the dispatcher case + validator guard. Every EDM-property dereference on the dynamic path must be `instanceof`-guarded — compile/test failures guide.
- [ ] **Step 4: GREEN + regressions** — `mvn install -pl lib/server-core -Pbuild.fast -DskipTests -q && mvn test -Dtest='ODataHandlerImplTest,UriValidatorTest,OpenTypeUriParserTest' -pl lib/server-test`, then full `mvn test -pl lib/server-test`.
- [ ] **Step 5: Commit** — `git commit -m "Route dynamic-property paths through primitive dispatching"`

---

### Task 3: tecsvc GET semantics + fit read tests

**Files:**
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/processor/TechnicalPrimitiveComplexProcessor.java` — `readPrimitive` (:100) delegates to a shared read helper; READ THE WHOLE FLOW FIRST. Add the dynamic branch: when the last URI part is `UriResourceDynamicProperty`, resolve the entity via the existing entity-resolution helper, look up the property by name; absent → `ODataApplicationException(404)`; present → serialize via the primitive serializer with the kind resolved from `property.getType()` (`EdmPrimitiveTypeKind.valueOfFQN`, `Edm.String` fallback for null/unresolvable — the tecsvc `ExpressionVisitorImpl` from the merged feature has the same resolution idiom, reuse/extract it); collection-valued (`ValueType.COLLECTION_PRIMITIVE`) → serialize with the primitive-COLLECTION serializer instead. Context URL: build from the resolved type the way the declared-property read does.
- Test: extend `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/client/OpenTypeITCase.java`.

**Interfaces:** consumes Tasks 1-2. Produces GET behavior for Task 4's re-GET assertions.

- [ ] **Step 1: RED fit tests** (JSON-only `assumeTrue(isJson())`, exact values):

```java
@Test
public void readDynamicPropertyDirectly() { /* GET ESOpen(1)/DynamicString -> 200, property value "dynamic", type Edm.String */ }
@Test
public void readAbsentDynamicPropertyReturns404() { /* GET ESOpen(3)/DynamicString -> 404 */ }
@Test
public void readDynamicInt64Directly() { /* GET ESOpen(1)/DynamicInt -> 200, value 42 */ }
@Test
public void readDynamicPropertyOnClosedTypeStillRejected() { /* GET ESTwoPrim(...)/Nope -> 404 (pin) */ }
```

Run: `mvn install -pl lib/server-api,lib/server-core,lib/server-tecsvc -Pbuild.fast -DskipTests -q && mvn verify -pl fit -Pbuild.fast -Dit.test=OpenTypeITCase`. Expected: the three new positive tests fail (likely 500/501-class before the processor branch exists), pin passes.

- [ ] **Step 2: GREEN** — implement the processor branch; re-run; then full fit regression `mvn verify -pl fit -Pbuild.fast`.
- [ ] **Step 3: Commit** — `git commit -m "Serve GET for dynamic properties in tecsvc"`

---

### Task 4: tecsvc PUT/PATCH/DELETE semantics + fit mutation tests

**Files:**
- Modify: `TechnicalPrimitiveComplexProcessor.updatePrimitive` (:112) + `deletePrimitive` (:126): dynamic branch — update parses the body via Task 1's `deserializer.dynamicProperty(stream, name)` and REPLACES the stored property (value and type; PATCH identical to PUT for dynamic scalars — note in Javadoc); delete REMOVES the property from the entity's property list, 204. Absent property: update creates it (upsert semantics match declared-property tecsvc behavior — VERIFY what tecsvc does for declared absent-value updates and mirror; if declared 404s, dynamic 404s too on PATCH but PUT may create — decide by reading, document the choice in the report); delete of absent → 404.
- Modify (if needed): `lib/server-tecsvc/.../data/DataProvider.java` — reuse the dynamic-property update/removal logic added by the merged feature (`updateDynamicProperties`) rather than duplicating.
- Test: extend `OpenTypeITCase`.

**Interfaces:** consumes Task 1's `dynamicProperty(...)` and Task 3's GET (for re-GET verification).

- [ ] **Step 1: RED fit tests** (all session-isolated via the class's established fresh-cookie idiom):

```java
@Test
public void putReplacesDynamicPropertyValue() { /* PUT ESOpen(1)/DynamicString {"value":"changed"} -> 200/204; re-GET -> "changed" */ }
@Test
public void putChangesDynamicPropertyType() { /* PUT ESOpen(1)/DynamicInt {"value@odata.type":"#Guid","value":"<uuid>"} -> success; re-GET -> Guid value */ }
@Test
public void patchBehavesLikePutForDynamicScalar() { /* PATCH ESOpen(1)/DynamicString -> updated */ }
@Test
public void deleteRemovesDynamicProperty() { /* DELETE ESOpen(1)/DynamicString -> 204; re-GET -> 404; sibling DynamicInt still present */ }
@Test
public void deleteAbsentDynamicPropertyReturns404() { /* DELETE ESOpen(3)/DynamicString -> 404 */ }
@Test
public void writeObjectPayloadToDynamicPropertyRejected() { /* PUT {"value":{"a":1}} -> 400 */ }
```

- [ ] **Step 2: GREEN** — implement; rerun `-Dit.test=OpenTypeITCase`, then full fit + tecsvc module tests.
- [ ] **Step 3: Commit** — `git commit -m "Serve PUT, PATCH and DELETE for dynamic properties in tecsvc"`

---

### Task 5: Client collection annotation + symmetry

**Files:**
- Modify: `lib/client-core/src/main/java/org/sitenetsoft/olinguito/client/core/serialization/JsonSerializer.java` — `valuable()` (:392-417): the scalar branch annotates via `JSON_NATIVE_KINDS` (:415); add the collection sibling: when `valuable.isCollection()` and the ELEMENT kind (from `valuable.getType()`'s `Collection(X)` expression or element inspection — read how `collection(...)` at :375 resolves element types) is outside `JSON_NATIVE_KINDS` and `!isODataMetadataNone` and not already annotated by the full-metadata branch → `jgen.writeStringField(name + Constants.JSON_TYPE, "#Collection(" + kind.name() + ")")`.
- Test: `lib/client-core/.../OpenTypeClientTest.java` — FLIP the pinned gap test `dynamicGuidCollectionWrittenUnderMinimalMetadataHasNoTypeAnnotationYet` into `dynamicGuidCollectionWrittenUnderMinimalMetadataCarriesCollectionAnnotation` asserting `"...@odata.type":"#Collection(Guid)"`; add a native-element pin (String collection gets NO annotation under minimal); metadata=none pin (no annotation ever).
- Test (symmetry): add to `lib/server-test/.../ODataJsonDeserializerOpenTypeTest.java` — feed the exact client-produced payload string (copy from the client test's expected output) into the server entity deserializer for ETOpen and assert `Collection(Edm.Guid)` typing survives.

- [ ] **Step 1: RED** — flip the pin (fails today), add the new pins (some pass = pins).
- [ ] **Step 2: GREEN** — implement; `mvn test -pl lib/client-core -Pbuild.fast` full suite; then the server-side symmetry test (`mvn install -pl lib/client-core -Pbuild.fast -DskipTests -q` not needed for server-test — independent).
- [ ] **Step 3: Commit** — `git commit -m "Annotate dynamic collection writes with their element type"`

---

### Task 6: Docs, spec deviation cleanup, full build gate

**Files:**
- Modify: `docs/site/guides/open-types-guide.md` — rewrite "Direct path addressing (parses, not served)" into the CRUD semantics table (GET/PUT/PATCH/DELETE incl. 404-when-absent, DELETE-removes, PUT-may-retype, `$value` still rejected, legacy-stack 404 unchanged); update the client Writing section (collections now annotated; remove the known-gap note).
- Modify: `docs/superpowers/specs/2026-08-09-opentype-design.md` — in §Deviations, mark the two superseded entries as resolved by this follow-up (one line each, pointing at `2026-08-11-opentype-crud-design.md`).
- Modify: `CLAUDE.md` — extend the OpenType bullet: `payload + $select/$filter/$orderby + direct-path CRUD`.

**Steps:**
- [ ] **Step 1:** Docs/spec/CLAUDE.md edits; commit `git commit -m "Document dynamic-property CRUD"`.
- [ ] **Step 2: FULL GATE** — `mvn -B install --fail-at-end -Dquarkus.http.test-port=8083` from repo root, foreground. All 38 modules green; record totals.
- [ ] **Step 3:** Fix anything the gate finds (checkstyle 120 usual culprit); do NOT push — integration is the controller's step.

---

## Self-Review Notes

- Spec coverage: §1 dispatch/validation → Task 2; §2 semantics table → Tasks 3-4; §3 API → Task 1; §4 client → Task 5; §5 errors → spread pins (absent-404 T3, unresolvable/object T1+T4, $value pin T2, closed pins T2/T3); §6 testing → every task + T6 gate; rollout stages = T1-2 / T3-4 / T5-6.
- Deliberate executor-judgment points (test-guided, not placeholders): property-payload shape mirroring `property(...)` (T1), tecsvc shared-helper reuse and upsert-vs-404 on absent-update (T4, decision documented by reading declared behavior), element-kind resolution in the client collection branch (T5).
- Type consistency: `dynamicProperty(InputStream, String)` used identically in T1 (definition) and T4 (consumption); `UriResourceDynamicProperty` names match the merged feature's API.
