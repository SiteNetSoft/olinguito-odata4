# OpenType Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** OData V4 open types: dynamic (undeclared) properties accepted/emitted in JSON payloads and usable in `$select`/`$filter`/`$orderby` on both server and client.

**Architecture:** Approach A from the spec (`docs/superpowers/specs/2026-08-09-opentype-design.md`): inline `isOpenType()` fall-through branches at each schema gate. Dynamic properties are ordinary `Property`/`ClientProperty` objects whose names aren't declared in the EDM; no new data structures. New additive API only: `UriResourceDynamicProperty` in server-api.

**Tech Stack:** Java 17, Maven, JUnit 5, Mockito; modules `lib/server-tecsvc`, `lib/server-core`, `lib/server-api`, `lib/server-test`, `lib/client-core`, `fit`.

## Global Constraints

- **NO AI attribution anywhere**: commit messages contain no Co-Authored-By/Claude-Session/Generated-with lines (project directive 2026-08-08).
- Every modified source file gets a SiteNetSoft copyright line before the closing `*/` of its Apache header: `* Copyright 2026 SiteNetSoft - Add OpenType support (<short detail>)` (skip files that already carry an identical line).
- Closed (non-open) types keep current behavior exactly; every task that opens a gate adds a closed-type regression assertion.
- JSON only; XML/Atom, `$expand`/`$apply` on dynamic names, dynamic complex values, and `ext/client-proxy` are out of scope (spec §Scope).
- `server-test` tests run against **installed** JARs: after changing `server-core`/`server-tecsvc`/`server-api`, run `mvn install -pl lib/server-api,lib/server-core,lib/server-tecsvc -Pbuild.fast -DskipTests -q` before `mvn test -pl lib/server-test`.
- Final gate before declaring done: full plain `mvn -B install --fail-at-end` (NOT `-Pbuild.fast` — it skips checkstyle/RAT which have caught real violations before).
- Checkstyle: max line length applies; keep lines ≤ 128 chars.

---

### Task 1: tecsvc open entity type (ETOpen) — shared test model

**Files:**
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/provider/EntityTypeProvider.java`
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/provider/ContainerProvider.java`
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/provider/SchemaProvider.java` (if entity types are listed there; follow how `nameETKeyNav` is registered)
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/data/DataCreator.java`
- Test: `lib/server-tecsvc/src/test/java/org/sitenetsoft/olinguito/server/tecsvc/provider/OpenTypeProviderTest.java` (create)

**Interfaces:**
- Consumes: existing tecsvc provider registration pattern (`EntityTypeProvider.nameETAllPrim` et al.).
- Produces: FQN constant `EntityTypeProvider.nameETOpen` = `olingo.odata.test1.ETOpen` (key `PropertyInt16` Int16 + declared `PropertyString` String, `OpenType=true`); entity set `ESOpen` (3 seeded entities, entity 1 carrying dynamic `DynamicString`="dynamic" (Edm.String) and `DynamicInt`=Int64 42, entity 2 carrying `DynamicInt`=7, entity 3 with no dynamic properties). All later tasks reference these names verbatim.

- [ ] **Step 1: Write the failing test**

```java
/* Apache header + SiteNetSoft line per Global Constraints */
package org.sitenetsoft.olinguito.server.tecsvc.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.junit.jupiter.api.Test;

class OpenTypeProviderTest {

  @Test
  void etOpenIsDeclaredOpen() throws Exception {
    final CsdlEntityType type = new EntityTypeProvider().getEntityType(EntityTypeProvider.nameETOpen);
    assertNotNull(type);
    assertTrue(type.isOpenType());
    assertEquals("PropertyInt16", type.getKey().get(0).getName());
    assertEquals(2, type.getProperties().size());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OpenTypeProviderTest -pl lib/server-tecsvc -Pbuild.fast`
Expected: COMPILE ERROR — `nameETOpen` does not exist. (Compile failure is the valid RED here.)

- [ ] **Step 3: Implement the model**

In `EntityTypeProvider.java`, next to the other FQN constants:

```java
public static final FullQualifiedName nameETOpen = new FullQualifiedName(NAMESPACE, "ETOpen");
```

In `getEntityType`, following the exact pattern of the simplest existing type (e.g. `nameETTwoPrim`):

```java
} else if (entityTypeName.equals(nameETOpen)) {
  return new CsdlEntityType()
      .setName("ETOpen")
      .setOpenType(true)
      .setKey(Collections.singletonList(new CsdlPropertyRef().setName("PropertyInt16")))
      .setProperties(Arrays.asList(
          PropertyProvider.propertyInt16_NotNullable, PropertyProvider.propertyString));
```

In `ContainerProvider`: register entity set `ESOpen` bound to `nameETOpen`, following the existing `ESTwoPrim` registration verbatim (same code shape, no nav bindings). If `SchemaProvider` lists entity types explicitly, add `nameETOpen` there the same way `nameETTwoPrim` appears.

In `DataCreator`, following an existing `createESTwoPrim()`-style method: add `createESOpen()` seeding 3 entities. Dynamic properties are plain extra `Property` objects:

```java
private EntityCollection createESOpen() {
  final EntityCollection entityCollection = new EntityCollection();

  Entity entity = new Entity()
      .addProperty(createPrimitive("PropertyInt16", (short) 1))
      .addProperty(createPrimitive("PropertyString", "open type 1"))
      .addProperty(createPrimitive("DynamicString", "dynamic"))
      .addProperty(createPrimitive("DynamicInt", 42L));
  entityCollection.getEntities().add(entity);

  entity = new Entity()
      .addProperty(createPrimitive("PropertyInt16", (short) 2))
      .addProperty(createPrimitive("PropertyString", "open type 2"))
      .addProperty(createPrimitive("DynamicInt", 7L));
  entityCollection.getEntities().add(entity);

  entity = new Entity()
      .addProperty(createPrimitive("PropertyInt16", (short) 3))
      .addProperty(createPrimitive("PropertyString", "open type 3"));
  entityCollection.getEntities().add(entity);

  setEntityType(entityCollection, edm.getEntityType(EntityTypeProvider.nameETOpen));
  createEntityId(edm, odata, "ESOpen", entityCollection);
  return entityCollection;
}
```

Register it in the map alongside the other `create*` calls (`dataMap.put("ESOpen", createESOpen());` — copy the surrounding idiom exactly). Adapt helper names (`createPrimitive`, `setEntityType`, `createEntityId`) to what the file actually uses — read the neighboring methods first.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OpenTypeProviderTest -pl lib/server-tecsvc -Pbuild.fast`
Expected: PASS

- [ ] **Step 5: Run the whole tecsvc module + install for downstream tasks**

Run: `mvn install -pl lib/server-tecsvc -Pbuild.fast -q` (runs tests, then installs)
Expected: BUILD SUCCESS. If a metadata-fixture test elsewhere asserts the full schema (e.g. metadata document tests enumerate all entity sets), update those fixtures — new entity set `ESOpen` and type `ETOpen` are expected additions.

- [ ] **Step 6: Commit**

```bash
git add lib/server-tecsvc
git commit -m "tecsvc: add open entity type ETOpen with seeded dynamic properties"
```

---

### Task 2: JSON deserializer — dynamic primitives on open entity types

**Files:**
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/deserializer/json/ODataJsonDeserializer.java` (hook before `assertJsonNodeIsEmpty(tree)` at the end of `consumeEntityNode`, currently line ~256)
- Test: `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/deserializer/json/ODataJsonDeserializerOpenTypeTest.java` (create)

**Interfaces:**
- Consumes: `ETOpen`/`ESOpen` from Task 1; existing `AbstractODataDeserializerTest` harness (gives an `edm` from `EdmTechProvider` — copy the setup lines from `ODataJsonDeserializerEntityTest`).
- Produces: private method `consumeDynamicProperties(final EdmStructuredType edmType, final ObjectNode node, final List<Property> properties)` in `ODataJsonDeserializer` — Task 3 and Task 4 extend it.

- [ ] **Step 1: Write the failing tests**

New class extending `AbstractODataDeserializerTest`, same imports/setup idiom as `ODataJsonDeserializerEntityTest` (static `OData odata = OData.newInstance()`, entity type via `edm.getEntityType(new FullQualifiedName("olingo.odata.test1", "ETOpen"))`, deserializer via `odata.createDeserializer(ContentType.JSON)`):

```java
@Test
void dynamicPrimitivesAcceptedOnOpenType() throws Exception {
  final String payload = "{\"PropertyInt16\":1,\"PropertyString\":\"abc\","
      + "\"Custom\":\"hello\",\"CustomInt\":42,\"CustomBool\":true,\"CustomNull\":null}";
  final Entity entity = deserialize(payload, "ETOpen");   // use/adapt the harness helper
  assertEquals(6, entity.getProperties().size());
  final Property dynamic = entity.getProperty("Custom");
  assertNotNull(dynamic);
  assertEquals("hello", dynamic.getValue());
  assertEquals("Edm.String", dynamic.getType());
  assertEquals("Edm.Int64", entity.getProperty("CustomInt").getType());
  assertEquals("Edm.Boolean", entity.getProperty("CustomBool").getType());
  assertTrue(entity.getProperty("CustomNull").isNull());
}

@Test
void unknownPropertyStillRejectedOnClosedType() {
  final String payload = "{\"PropertyInt16\":1,\"PropertyString\":\"abc\",\"Custom\":\"x\"}";
  final DeserializerException e = assertThrows(DeserializerException.class,
      () -> deserialize(payload, "ETTwoPrim"));
  assertEquals(DeserializerException.MessageKeys.UNKNOWN_CONTENT, e.getMessageKey());
}
```

(`deserialize(payload, typeName)` = the class's own 4-line helper calling `deserializer.entity(new ByteArrayInputStream(payload.getBytes(UTF_8)), edm.getEntityType(new FullQualifiedName("olingo.odata.test1", typeName))).getEntity()` — mirror how `ODataJsonDeserializerEntityTest` invokes it.)

- [ ] **Step 2: Run tests to verify RED**

Run: `mvn install -pl lib/server-core -Pbuild.fast -DskipTests -q && mvn test -Dtest=ODataJsonDeserializerOpenTypeTest -pl lib/server-test`
Expected: `dynamicPrimitivesAcceptedOnOpenType` FAILS with `DeserializerException: Tree should be empty but still has content left: Custom` (UNKNOWN_CONTENT). `unknownPropertyStillRejectedOnClosedType` PASSES (pin).

- [ ] **Step 3: Implement**

In `ODataJsonDeserializer.consumeEntityNode`, immediately before `assertJsonNodeIsEmpty(tree);`:

```java
if (edmEntityType.isOpenType()) {
  consumeDynamicProperties(edmEntityType, tree, entity.getProperties());
}
```

New private methods (inference table mirrors client `JsonDeserializer.guessPrimitiveTypeKind`):

```java
private void consumeDynamicProperties(final EdmStructuredType edmType, final ObjectNode node,
    final List<Property> properties) throws DeserializerException {
  final List<String> consumed = new ArrayList<>();
  final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
  while (fields.hasNext()) {
    final Map.Entry<String, JsonNode> field = fields.next();
    final String name = field.getKey();
    if (name.contains(Constants.AT)) {
      continue;                       // annotations handled in Task 3
    }
    final JsonNode value = field.getValue();
    if (value.isObject()) {
      continue;                       // dynamic complex values unsupported -> falls through to UNKNOWN_CONTENT
    }
    properties.add(createDynamicProperty(name, value));
    consumed.add(name);
  }
  node.remove(consumed);
}

private Property createDynamicProperty(final String name, final JsonNode value) {
  final Property property = new Property();
  property.setName(name);
  if (value.isNull()) {
    property.setValue(ValueType.PRIMITIVE, null);
    return property;
  }
  property.setType(inferPrimitiveTypeName(value));
  property.setValue(ValueType.PRIMITIVE, inferPrimitiveValue(value));
  return property;
}

private String inferPrimitiveTypeName(final JsonNode value) {
  return (value.isShort() ? EdmPrimitiveTypeKind.Int16
      : value.isInt() ? EdmPrimitiveTypeKind.Int32
      : value.isLong() ? EdmPrimitiveTypeKind.Int64
      : value.isBoolean() ? EdmPrimitiveTypeKind.Boolean
      : value.isFloat() ? EdmPrimitiveTypeKind.Single
      : value.isDouble() ? EdmPrimitiveTypeKind.Double
      : value.isBigDecimal() ? EdmPrimitiveTypeKind.Decimal
      : EdmPrimitiveTypeKind.String).getFullQualifiedName().getFullQualifiedNameAsString();
}

private Object inferPrimitiveValue(final JsonNode value) {
  return value.isShort() ? value.shortValue()
      : value.isInt() ? value.intValue()
      : value.isLong() ? value.longValue()
      : value.isBoolean() ? value.booleanValue()
      : value.isFloat() ? value.floatValue()
      : value.isDouble() ? value.doubleValue()
      : value.isBigDecimal() ? value.decimalValue()
      : value.asText();
}
```

Note: Jackson maps small JSON integers to `IntNode` — `isShort()` is false for them, so bare `42` infers `Edm.Int32`… but the test asserts `Edm.Int64`. **Decide by the test**: the test above is the contract; change the assertion to `Edm.Int32` for in-range integers and add a `9999999999` → `Edm.Int64` case. (Spec says "integral by magnitude"; Jackson's node width IS the magnitude signal. Update the test in Step 1 accordingly before running RED.)

- [ ] **Step 4: Run tests to verify GREEN**

Run: `mvn install -pl lib/server-core -Pbuild.fast -DskipTests -q && mvn test -Dtest=ODataJsonDeserializerOpenTypeTest -pl lib/server-test`
Expected: both PASS.

- [ ] **Step 5: Regression: run the full deserializer test classes**

Run: `mvn test -Dtest='ODataJsonDeserializer*Test' -pl lib/server-test`
Expected: all PASS (closed-type behavior unchanged).

- [ ] **Step 6: Commit**

```bash
git add lib/server-core lib/server-test
git commit -m "Accept dynamic primitive properties when deserializing open entity types"
```

---

### Task 3: JSON deserializer — @odata.type annotations, collections, object rejection

**Files:**
- Modify: `lib/server-core/.../deserializer/json/ODataJsonDeserializer.java` (extend `consumeDynamicProperties`/`createDynamicProperty` from Task 2)
- Test: extend `ODataJsonDeserializerOpenTypeTest` from Task 2

**Interfaces:**
- Consumes: Task 2's `consumeDynamicProperties`.
- Produces: final deserializer behavior for spec §1/§2; nothing new for later tasks.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void annotatedDynamicPropertyParsedAsDeclaredType() throws Exception {
  final String payload = "{\"PropertyInt16\":1,"
      + "\"When@odata.type\":\"#DateTimeOffset\",\"When\":\"2026-08-09T12:00:00Z\"}";
  final Entity entity = deserialize(payload, "ETOpen");
  final Property when = entity.getProperty("When");
  assertEquals("Edm.DateTimeOffset", when.getType());
  assertTrue(when.getValue() instanceof java.sql.Timestamp
      || when.getValue() instanceof java.time.OffsetDateTime
      || when.getValue() instanceof java.util.Calendar);  // match what EdmDateTimeOffset.valueOfString returns here
}

@Test
void dynamicCollectionOfPrimitives() throws Exception {
  final String payload = "{\"PropertyInt16\":1,\"Tags\":[\"a\",\"b\"],\"Empty\":[]}";
  final Entity entity = deserialize(payload, "ETOpen");
  final Property tags = entity.getProperty("Tags");
  assertEquals(ValueType.COLLECTION_PRIMITIVE, tags.getValueType());
  assertEquals(List.of("a", "b"), tags.asCollection());
  assertEquals("Collection(Edm.String)", "Collection(" + entity.getProperty("Empty").getType() + ")");
}

@Test
void jsonObjectInDynamicSlotRejected() {
  final String payload = "{\"PropertyInt16\":1,\"Nested\":{\"a\":1}}";
  final DeserializerException e = assertThrows(DeserializerException.class,
      () -> deserialize(payload, "ETOpen"));
  assertEquals(DeserializerException.MessageKeys.UNKNOWN_CONTENT, e.getMessageKey());
}

@Test
void unknownAnnotatedTypeRejected() {
  final String payload = "{\"PropertyInt16\":1,\"X@odata.type\":\"#No.Such.Type\",\"X\":\"v\"}";
  assertThrows(DeserializerException.class, () -> deserialize(payload, "ETOpen"));
}
```

- [ ] **Step 2: Run RED** — `mvn test -Dtest=ODataJsonDeserializerOpenTypeTest -pl lib/server-test`. Expected: first two FAIL (annotation field currently skipped → `When@odata.type` left in tree → UNKNOWN_CONTENT; arrays not handled). `jsonObjectInDynamicSlotRejected` may already PASS (fine — it's a pin).

- [ ] **Step 3: Implement.** In `consumeDynamicProperties`: first collect `name@odata.type` fields into a `Map<String, String> dynamicTypes` (strip leading `#`; remove those fields from the node); pass the map into `createDynamicProperty`. There: if an annotated type exists, resolve via `EdmPrimitiveTypeKind.valueOfFQN(typeName)` (wrap `IllegalArgumentException` into `DeserializerException(..., MessageKeys.UNKNOWN_CONTENT, name)`) and parse with `EdmPrimitiveTypeFactory.getInstance(kind).valueOfString(value.asText(), true, null, ...)` using the same default facet arguments the file already uses for declared primitives (copy from its existing `valueOfString` call). For arrays: iterate elements (reject non-primitive elements → leave field unconsumed), build `property.setValue(ValueType.COLLECTION_PRIMITIVE, list)` with element type inferred from the first element or `Edm.String` when empty.

- [ ] **Step 4: Run GREEN** — same command, all PASS. Re-run `mvn test -Dtest='ODataJsonDeserializer*Test' -pl lib/server-test` for regressions (remember to reinstall server-core first).

- [ ] **Step 5: Commit** — `git commit -m "Support annotated types and primitive collections for dynamic properties"`

---

### Task 4: JSON deserializer — open complex types

**Files:**
- Modify: `lib/server-tecsvc/.../provider/ComplexTypeProvider.java` (+ `EntityTypeProvider`: give `ETOpen` a declared complex property `PropertyComp` of the new open complex type `CTOpen` with one declared member `CompString`), `DataCreator` seeds it on entity 1 with a dynamic member.
- Modify: `lib/server-core/.../deserializer/json/ODataJsonDeserializer.java` — locate the complex-value consumption path (the `assertJsonNodeIsEmpty(jsonNode)` calls at ~746/~922 inside the complex/property consumption methods) and add the same guarded `consumeDynamicProperties(complexType, node, complexValue.getValue())` call before the assertion.
- Test: extend `ODataJsonDeserializerOpenTypeTest`.

**Interfaces:**
- Consumes: Task 2's `consumeDynamicProperties` (already typed against `EdmStructuredType` — reused as-is).
- Produces: `CTOpen` (`olingo.odata.test1.CTOpen`, OpenType=true, declared `CompString` Edm.String) and `ETOpen.PropertyComp` — used by serializer/fit tasks.

- [ ] **Step 1: RED test**

```java
@Test
void dynamicPropertyInsideOpenComplexValue() throws Exception {
  final String payload = "{\"PropertyInt16\":1,"
      + "\"PropertyComp\":{\"CompString\":\"s\",\"CompDynamic\":5}}";
  final Entity entity = deserialize(payload, "ETOpen");
  final ComplexValue comp = entity.getProperty("PropertyComp").asComplex();
  assertEquals(2, comp.getValue().size());
  assertEquals("CompDynamic", comp.getValue().get(1).getName());
}
```

Run (after reinstalling tecsvc with the model change: `mvn install -pl lib/server-tecsvc -Pbuild.fast -DskipTests -q`): FAILS with UNKNOWN_CONTENT on `CompDynamic`.

- [ ] **Step 2: GREEN** — add the guarded call at the complex-value site(s); rerun → PASS; run full `ODataJsonDeserializer*Test` + `OpenTypeProviderTest` regressions.

- [ ] **Step 3: Commit** — `git commit -m "Accept dynamic properties on open complex types"`

---

### Task 5: JSON serializer — emit dynamic properties (entity + complex, minimal + full)

**Files:**
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/serializer/json/ODataJsonSerializer.java` — entity site: the `for (final String propertyName : type.getPropertyNames())` loop ending at ~574; complex site: same loop at ~1157.
- Test: `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/serializer/json/ODataJsonSerializerOpenTypeTest.java` (create, modeled on `ODataJsonSerializerTest` setup: `ServiceMetadata` from `EdmTechProvider`, data via tecsvc `DataProvider` or hand-built `Entity`).

**Interfaces:**
- Consumes: `ETOpen`/`CTOpen` model; a hand-built `Entity` matching Task 1's seed shape.
- Produces: private `writeDynamicProperties(ServiceMetadata, EdmStructuredType, List<Property>, Set<String> selected, boolean all, JsonGenerator)` used by both sites.

- [ ] **Step 1: RED tests**

```java
@Test
void dynamicPropertiesSerializedMinimalMetadata() throws Exception {
  final Entity entity = new Entity()
      .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
      .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"))
      .addProperty(new Property("Edm.String", "Custom", ValueType.PRIMITIVE, "hello"))
      .addProperty(new Property("Edm.Int64", "CustomInt", ValueType.PRIMITIVE, 42L));
  final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);   // class helper around serializer.entity(...)
  assertTrue(json.contains("\"Custom\":\"hello\""));
  assertTrue(json.contains("\"CustomInt\":42"));
  assertFalse(json.contains("Custom@odata.type"));   // String + number are JSON-native
}

@Test
void nonNativeDynamicValueAnnotatedEvenInMinimal() throws Exception {
  final Entity entity = new Entity()
      .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
      .addProperty(new Property("Edm.Guid", "Ref", ValueType.PRIMITIVE,
          java.util.UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")));
  final String json = serializeEntity(entity, "ETOpen", ContentType.JSON);
  assertTrue(json.contains("\"Ref@odata.type\":\"#Guid\""));
}

@Test
void closedTypeDropsUndeclaredSilentlyAsToday() throws Exception {
  final Entity entity = new Entity()
      .addProperty(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, (short) 1))
      .addProperty(new Property(null, "PropertyString", ValueType.PRIMITIVE, "abc"))
      .addProperty(new Property("Edm.String", "Sneaky", ValueType.PRIMITIVE, "x"));
  final String json = serializeEntity(entity, "ETTwoPrim", ContentType.JSON);
  assertFalse(json.contains("Sneaky"));   // pins current closed-type behavior
}
```

Add a `metadata=full` variant asserting `"Custom@odata.type":"#String"` appears with `ContentType.APPLICATION_JSON;odata.metadata=full` (build the ContentType the way `ODataJsonSerializerTest` does for its full-metadata cases).

- [ ] **Step 2: Run RED** — first two FAIL (`Custom` absent from output today); third PASSES (pin).

- [ ] **Step 3: Implement.** After each declared-properties loop, guarded by `type.isOpenType()`: compute `declared = new HashSet<>(type.getPropertyNames())`, iterate the instance property list, skip declared names and navigation/association names, honor selection (`all || selected.contains(name)` at the entity site; `ExpandSelectHelper.isSelected(selectedPaths, name)` at the complex site), and write each via the file's existing primitive-writing helper (`writePrimitive`/`writePrimitiveValue` — reuse, do not duplicate) after resolving `EdmPrimitiveTypeKind.valueOfFQN(property.getType())`. Annotation rule: with full metadata always write `name@odata.type` (`"#" + kind.name()`); with minimal metadata write it only when the kind is not String/Boolean/Int16/Int32/Int64/Double/Decimal/Single (i.e. not JSON-native). Collections: `ValueType.COLLECTION_PRIMITIVE` → write `name@odata.type` as `"#Collection(<Kind>)"` under the same rules and reuse the existing collection-writing helper.

- [ ] **Step 4: GREEN + regressions** — new class PASSES; then `mvn test -Dtest='ODataJsonSerializer*Test' -pl lib/server-test` all green (reinstall server-core first).

- [ ] **Step 5: Commit** — `git commit -m "Serialize dynamic properties of open types in JSON"`

---

### Task 6: UriResourceDynamicProperty API + path addressing

**Files:**
- Create: `lib/server-api/src/main/java/org/sitenetsoft/olinguito/server/api/uri/UriResourceDynamicProperty.java`
- Modify: `lib/server-api/src/main/java/org/sitenetsoft/olinguito/server/api/uri/UriResourceKind.java` (add enum constant `dynamicProperty`)
- Create: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/uri/UriResourceDynamicPropertyImpl.java`
- Modify: `lib/server-core/src/main/java/org/sitenetsoft/olinguito/server/core/uri/parser/ResourcePathParser.java` (~line 311-323, shown in exploration: after `getStructuralProperty`/`getNavigationProperty` both miss)
- Test: `lib/server-test/src/test/java/org/sitenetsoft/olinguito/server/core/uri/parser/OpenTypeUriParserTest.java` (create; build `Parser` against `EdmTechProvider` edm the way existing server-test URI parser tests do — find the class in that package that instantiates `new Parser(edm, odata)` and copy its setup)

**Interfaces:**
- Produces (exact, used by Tasks 7-9):

```java
public interface UriResourceDynamicProperty extends UriResourcePartTyped {
  String getPropertyName();
}
// impl: getType() returns null (no EDM type), isCollection() returns false,
// getSegmentValue()/toString() return the property name, getKind() returns UriResourceKind.dynamicProperty
```

- [ ] **Step 1: RED test**

```java
@Test
void dynamicPropertyPathSegmentResolvesOnOpenType() throws Exception {
  final UriInfo uriInfo = new Parser(edm, odata).parseUri("ESOpen(1)/Anything", null, null, null);
  final List<UriResource> parts = uriInfo.getUriResourceParts();
  final UriResource last = parts.get(parts.size() - 1);
  assertEquals(UriResourceKind.dynamicProperty, last.getKind());
  assertEquals("Anything", ((UriResourceDynamicProperty) last).getPropertyName());
}

@Test
void unknownSegmentStillRejectedOnClosedType() {
  assertThrows(UriParserSemanticException.class,
      () -> new Parser(edm, odata).parseUri("ESTwoPrim(1)/Anything", null, null, null));
}
```

Run: compile error (`UriResourceDynamicProperty` missing) — valid RED.

- [ ] **Step 2: Implement** interface + impl (impl extends the package's `UriResourceTypedImpl` base if that's what siblings do — mirror `UriResourcePrimitivePropertyImpl`'s shape), add the enum constant, and in `ResourcePathParser` replace the unconditional throw:

```java
final EdmNavigationProperty navigationProperty = structType.getNavigationProperty(name);
if (navigationProperty == null) {
  if (structType.isOpenType()) {
    ParserHelper.requireTokenEnd(tokenizer);
    return new UriResourceDynamicPropertyImpl(name);
  }
  throw new UriParserSemanticException(...unchanged...);
}
```

Check `UriValidator`: if it switches over `UriResourceKind` exhaustively, add a `dynamicProperty` case permitting GET (mirror the `primitiveProperty` case). Compile failures in the validator are the guide.

- [ ] **Step 3: GREEN + regressions** — new tests pass; run the whole URI parser/validator test suite in server-test (`mvn test -Dtest='*Uri*Test,*Parser*Test' -pl lib/server-test`) after reinstalling server-api/server-core.

- [ ] **Step 4: Commit** — `git commit -m "Add UriResourceDynamicProperty and resolve dynamic path segments on open types"`

---

### Task 7: ExpressionParser — dynamic members in $filter/$orderby

**Files:**
- Modify: `lib/server-core/.../uri/parser/ExpressionParser.java` — `parsePropertyPathExpr` (~line 944-951, shown in exploration) + the binary-operator type-compatibility check(s) (`checkType`-style methods in the same file).
- Test: extend `OpenTypeUriParserTest`.

**Interfaces:**
- Consumes: Task 6's `UriResourceDynamicPropertyImpl`.
- Produces: `$filter`/`$orderby` parse trees containing dynamic members; consumed by Task 9's tecsvc evaluation.

- [ ] **Step 1: RED tests**

```java
@Test
void filterOnDynamicPropertyParsesOnOpenType() throws Exception {
  final UriInfo uriInfo = new Parser(edm, odata)
      .parseUri("ESOpen", "$filter=DynamicInt gt 5", null, null);
  assertNotNull(uriInfo.getFilterOption().getExpression());
}

@Test
void orderByDynamicPropertyParsesOnOpenType() throws Exception {
  assertNotNull(new Parser(edm, odata)
      .parseUri("ESOpen", "$orderby=DynamicString desc", null, null).getOrderByOption());
}

@Test
void filterOnUnknownStillRejectedOnClosedType() {
  assertThrows(UriParserException.class,
      () -> new Parser(edm, odata).parseUri("ESTwoPrim", "$filter=Nope eq 1", null, null));
}

@Test
void expandOnDynamicNameStillRejected() {
  assertThrows(UriParserException.class,
      () -> new Parser(edm, odata).parseUri("ESOpen", "$expand=DynamicInt", null, null));
}
```

- [ ] **Step 2: Implement.** In `parsePropertyPathExpr`, when `property == null` and `structuredType.isOpenType()`: `uriInfo.addResourcePart(new UriResourceDynamicPropertyImpl(oDataIdentifier)); return;` (before the throw). In the operand type-checking helpers: where both operand types are compared for compatibility, treat an operand whose type is `null` **and** whose member path ends in a dynamic resource as compatible with any primitive type (add a small `isDynamicUntyped(Expression)` helper checking for a `Member` whose last resource part has kind `dynamicProperty`). Do not weaken checks for non-dynamic operands: the null-type bypass must require the dynamic kind, not merely a null type.

- [ ] **Step 3: GREEN + regressions** — the four tests pass; rerun `mvn test -Dtest='*Parser*Test,*Expression*Test' -pl lib/server-test` and the `ApplyParser`/search suites for collateral.

- [ ] **Step 4: Commit** — `git commit -m "Parse dynamic-property members in filter and orderby on open types"`

---

### Task 8: $select of dynamic names

**Files:**
- Modify: `lib/server-core/.../uri/parser/SelectParser.java` — its property-resolution rejection site (grep `SelectParser` for `PROPERTY_NOT_IN_TYPE`/`EXPRESSION_PROPERTY_NOT_IN_TYPE`; same fall-through shape as Task 6).
- Modify (only if the RED test demands): entity-site selection handling in `ODataJsonSerializer` from Task 5 — the `selected` name set must include dynamic names from select items (check `ExpandSelectHelper.getSelectedPropertyNames`).
- Test: extend `OpenTypeUriParserTest` (parse level) and `ODataJsonSerializerOpenTypeTest` (output level).

**Interfaces:** consumes Tasks 5-7 artifacts; produces end-to-end `$select` behavior.

- [ ] **Step 1: RED tests**

```java
// parser level
@Test
void selectDynamicNameParsesOnOpenType() throws Exception {
  final UriInfo uriInfo = new Parser(edm, odata).parseUri("ESOpen", "$select=DynamicInt", null, null);
  assertEquals(1, uriInfo.getSelectOption().getSelectItems().size());
}

// serializer level (in ODataJsonSerializerOpenTypeTest): serialize the Task 5 entity with a
// SelectOption built from parsing "$select=Custom" and assert output contains "Custom" but
// not "PropertyString"; assert selecting an absent dynamic name yields output without it and no error.
```

- [ ] **Step 2: Implement** the SelectParser fall-through (produce a select item whose resource path is `UriResourceDynamicPropertyImpl`); make `ExpandSelectHelper`'s selected-name extraction include dynamic parts (it matches on segment name strings — add the `dynamicProperty` kind to whatever switch/instanceof it uses). Adjust the Task 5 serializer selection check only if the test fails on it.

- [ ] **Step 3: GREEN + regressions + commit** — `git commit -m "Support selecting dynamic properties on open types"`

---

### Task 9: tecsvc runtime evaluation + fit end-to-end

**Files:**
- Modify: `lib/server-tecsvc/src/main/java/org/sitenetsoft/olinguito/server/tecsvc/processor/queryoptions/expression/ExpressionVisitorImpl.java` — `visitMember` (or equivalent): on `UriResourceDynamicProperty`, look up the property by name in the current entity's property list; absent → typed-null operand (matching how missing nullable primitives are represented).
- Check: tecsvc `$orderby`/`$select` apply paths (`SortOption`/server-side option appliers under `processor/queryoptions/options/`) handle the dynamic kind the same way — compile/test failures are the guide.
- Test: `fit/src/test/java/org/sitenetsoft/olinguito/fit/tecsvc/client/OpenTypeITCase.java` (create; JUnit 4 — fit uses the vintage engine; copy the class-level setup from `BatchClientITCase` which targets the same tecsvc base URL).

**Interfaces:** consumes everything prior; produces the feature's end-to-end proof.

- [ ] **Step 1: RED integration tests** (representative set — all against `ESOpen`):

```java
@Test
public void readEntityWithDynamicProperties() { /* GET ESOpen(1): client.getRetrieveRequestFactory()
  .getEntityRequest(uri); assert body contains DynamicString property with value "dynamic" */ }

@Test
public void filterOnDynamicProperty() { /* GET ESOpen?$filter=DynamicInt gt 10 -> exactly entity 1 */ }

@Test
public void orderByDynamicProperty() { /* GET ESOpen?$orderby=DynamicInt desc -> ids 1,2,3 order
  (absent dynamic sorts as null -> last per existing tecsvc null ordering) */ }

@Test
public void selectDynamicProperty() { /* GET ESOpen(1)?$select=DynamicString -> payload has
  DynamicString, lacks PropertyString */ }

@Test
public void createEntityWithDynamicProperty() { /* POST to ESOpen a ClientEntity built via
  client.getObjectFactory() with declared PropertyInt16/PropertyString + extra primitive property
  "Brand"="new"; assert 201 and returned entity echoes Brand */ }
```

Write them with the same request-factory idiom the neighboring ITCases use; exact assertions on property values, not just status codes.

- [ ] **Step 2: Iterate to GREEN**: `mvn install -pl lib/server-tecsvc,lib/server-core,lib/server-api -Pbuild.fast -DskipTests -q && mvn verify -pl fit -Pbuild.fast -Dit.test=OpenTypeITCase`. Failures here drive the `ExpressionVisitorImpl`/option-applier changes.

- [ ] **Step 3: Full fit regression**: `mvn verify -pl fit -Pbuild.fast` — all ITCases green.

- [ ] **Step 4: Commit** — `git commit -m "Evaluate dynamic properties in tecsvc and cover open types end to end"`

---

### Task 10: Client pin tests, docs, full-build gate

**Files:**
- Test: `lib/client-core/src/test/java/org/sitenetsoft/olinguito/client/core/serialization/OpenTypeClientTest.java` (create): (a) deserialize an entity JSON payload containing `"Custom":"hello"` and `"CustomInt":42` into `ClientEntity` via `client.getBinder()`/`client.getDeserializer(ContentType.JSON)` (mirror `EntityTest`'s read pattern) and assert both properties present with inferred types; (b) build a `ClientEntity` with an extra primitive property, serialize via `client.getWriter()`/serializer (mirror `EntityTest`'s write pattern), assert the JSON contains it. Code changes to client-core only if a pin fails.
- Modify: `docs/` MkDocs site — add `docs/features/open-types.md` (or the site's existing feature-page location — match nav structure in `mkdocs.yml`): one page documenting the flag, dynamic property usage server- and client-side, inference table, and the out-of-scope list from the spec.
- Modify: `CLAUDE.md` — add `OpenType support (JSON, single-level queries)` to the Current Development bullet list.

**Steps:**

- [ ] **Step 1: RED→GREEN client pins** — `mvn test -Dtest=OpenTypeClientTest -pl lib/client-core -Pbuild.fast`. Expectation from exploration: both pass without production changes (client is schema-agnostic); if (b) drops the property or omits a needed `@odata.type` for non-native primitives, fix `JsonSerializer` symmetrically with Task 5's rule.
- [ ] **Step 2: Docs page + CLAUDE.md**, commit separately: `git commit -m "Document open type support"`
- [ ] **Step 3: FULL PLAIN BUILD**: `mvn -B install --fail-at-end` — all 38 modules, checkstyle+RAT+tests+fit. Fix anything it finds (checkstyle line lengths are the usual culprit).
- [ ] **Step 4: Final commit of any stragglers + push** — `git push origin master`.
- [ ] **Step 5: Update memory**: mark OpenType DONE in `deferred-tail-plan.md` + MEMORY.md index (commit hashes, any lessons).

---

## Self-Review Notes

- Spec coverage: §1 data model → Tasks 2-3; §2 payload → Tasks 2-5; §3 URI → Tasks 6-8; tecsvc eval → Task 9; §4 client → Task 10; §5 errors → pins spread across Tasks 2,3,5,6,7; §6 testing → every task + Task 10 gate. Rollout stages 1/2/3 = Tasks 1-5 / 6-8 / 9-10.
- Known judgment points left to the executor *by design* (each guided by a failing test, not a placeholder): exact tecsvc helper-method names (Task 1), Jackson int-width inference contract (Task 2 note), `UriValidator` case (Task 6), `ExpandSelectHelper` extraction mechanics (Task 8), tecsvc option-applier surface (Task 9).
