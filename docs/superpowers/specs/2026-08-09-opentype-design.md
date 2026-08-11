# OpenType Support — Design

**Date:** 2026-08-09
**Status:** Approved (design session 2026-08-09)
**Tickets:** OLINGO-737 / OLINGO-1645 (upstream, never delivered; upstream PR #175 rejected — built fresh here)

## Goal

Support OData V4 open types (`OpenType="true"` on entity and complex types): instances may carry *dynamic properties* not declared in `$metadata`, on both the server and client sides, including single-level query options over them.

## Scope

**In scope**

- Server: accept dynamic properties in JSON request payloads; emit them in JSON responses.
- Server: `$select`, `$filter`, `$orderby`, and direct path addressing (`/Entity(1)/DynName`) of dynamic properties on open types. **Deviation (see "Deviations from this design" below): direct path addressing ended up implemented at the URI-parse level only — serving it was deferred.**
- Client: verified round-trip of dynamic properties (deserialize + serialize) with tests; documented usage via the existing `ClientEntity` property API.
- Dynamic value kinds: primitives (with optional `@odata.type` annotation) and collections of primitives.
- tecsvc open entity type + fit end-to-end integration tests.

**Out of scope (unchanged current behavior, pinned by tests)**

- XML/Atom payloads: dynamic properties remain rejected.
- Dynamic complex values (JSON objects in dynamic slots): rejected (`UNKNOWN_CONTENT`).
- `$expand` / `$apply` on dynamic names: rejected as today.
- Nested/lambda dynamic-property paths in expressions (`any`/`all`, multi-segment): rejected as today. **Clarification (see "Deviations from this design" below): this means a dynamic name may not itself be followed by further segments (it's always a leaf) — it does NOT mean a dynamic leaf nested under one or more declared complex segments is rejected; that case (e.g. `$filter=PropertyComp/CompDynamic eq ...`) is supported and tested.**
- `ext/client-proxy` (`AbstractOpenType`): legacy, untouched.

## Approach

Approach A — inline open-type branching at each schema gate. At every point a layer validates names against the EDM, add a fall-through guarded by `EdmStructuredType.isOpenType()`. No new data structures, no policy/strategy abstraction, no port of upstream PR #175.

## Design

### 1. Data model & semantics

- A dynamic property is an ordinary `Property` (server) / `ClientProperty` (client) in the same property list as declared properties. Dynamic-ness is always computed against the EDM (`type.getProperty(name) == null`), never stored.
- Open-type behavior activates only when `isOpenType()` is true; closed types keep today's strict rejection exactly.
- Applies to entity types and complex types alike.
- No shadowing: a name resolving in the EDM is processed as a declared property.
- Server-side type inference for unannotated dynamic JSON values mirrors the client's existing `JsonDeserializer.guessPrimitiveTypeKind` table:
  - boolean → `Edm.Boolean`
  - integral number → `Edm.Int16`/`Edm.Int32`/`Edm.Int64` by magnitude
  - decimal number → `Edm.Double` / `Edm.Decimal` per Jackson's parse
  - string → `Edm.String`
  - `null` → typeless null property
  - array → `Collection(<inferred element type>)`; element type inferred from the first element; empty array → `Collection(Edm.String)`
- An explicit `propName@odata.type` annotation wins over inference; the value is parsed as that primitive type; unparseable → deserializer error naming the annotation.

### 2. Server payload layer (JSON)

- **Deserializer (`ODataJsonDeserializer`)**: at the structural `UNKNOWN_CONTENT` throw sites (entity/complex property consumption and the consumed-fields check), branch on `isOpenType()`: consume the field plus its `@odata.type` sibling, build the inferred `Property` instead of throwing. `checkJsonTypeBasedOnPrimitiveType` pre-validation is bypassed for dynamic values (inference is the validation). JSON objects in dynamic slots still throw `UNKNOWN_CONTENT`.
- **Serializer (`ODataJsonSerializer`)**: at the `type.getPropertyNames()` iteration sites, for open types add a second pass writing every instance property whose name is not declared, using the `Property`'s carried type:
  - `metadata=full`: emit `name@odata.type` for all non-String primitive dynamic values.
  - `metadata=minimal`: JSON-native values (string/boolean/number) bare; non-native primitives (dates, GUIDs, etc.) still annotated so clients can re-type them.

### 3. URI & query layer

- **New additive API in `server-api`**: `UriResourceDynamicProperty` (a `UriResourcePartTyped` with a new `UriResourceKind`), carrying the dynamic property name; untyped (`getType()` has no EDM type to report), `isCollection() == false`.
- **Resolution fall-throughs** (each guarded by owning type `isOpenType()`):
  - `ResourcePathParser` (`PROPERTY_NOT_IN_TYPE` site): path segments resolve to `UriResourceDynamicProperty`.
  - `ExpressionParser` (`EXPRESSION_PROPERTY_NOT_IN_TYPE` site): `$filter`/`$orderby` members resolve to a dynamic member.
  - `ExpandParser`, `ApplyParser`: unchanged — still reject (dynamic names are not navigation properties; `$apply` out of scope).
- **Type-checking**: a dynamic member is compatible with any primitive operand in expression operand checks. Typing happens at evaluation time; runtime mismatches follow normal comparison semantics (no parse error).
- **`$select`**: select parsing accepts unknown names on open types, producing a select item over the dynamic resource. Serializer select helpers match by name, so selected dynamic properties flow through; a selected dynamic name absent from an instance is omitted (spec behavior).
- **tecsvc evaluation**: tecsvc's expression visitor/data provider resolve dynamic members from the entity's property list at runtime.

### 4. Client

- Deserialization and serialization are already schema-agnostic (`guessPropertyType`); work is verification tests pinning dynamic-property round-trip and `@odata.type` emission symmetry with the server rule. Code changes only if a pin test exposes a gap.
- No new client API: dynamic properties are added via the existing object-factory/property-list API; documented on the feature docs page.

### 5. Error handling

| Case | Behavior |
|------|----------|
| Undeclared property, closed type | `UNKNOWN_CONTENT` / `PROPERTY_NOT_IN_TYPE` — unchanged, regression-pinned |
| JSON object in dynamic slot | `UNKNOWN_CONTENT` |
| `$expand`/`$apply` on dynamic name | current errors, unchanged |
| Dynamic `@odata.type` unknown/non-primitive | deserializer error naming the annotation |
| Selected dynamic name absent from instance | omitted from output (no error) |

### 6. Testing

1. **server-core unit** (TDD per gate): deserializer accept/infer/annotated/null/array/object-rejected/closed-type-regression; serializer round-trip + metadata-level annotation rules + `$select`; parser path/`$filter`/`$orderby`/`$expand`-rejection.
2. **tecsvc**: new open entity type (`ETOpen`) with seeded dynamic properties.
3. **fit**: `OpenTypeITCase` — HTTP round-trip: POST with dynamic properties, GET, `$filter`/`$orderby`/`$select` on a dynamic property, plus a client-API create/read leg.
4. **Full plain build** (checkstyle/RAT/all tests) gates every commit.

## Deviations from this design

Recorded during the final whole-branch review (post-2026-08-09 implementation), where they were
found not to match what this document originally scoped:

- **Direct path addressing is parsed, not served.** Section 3's `UriResourceDynamicProperty`
  (`getType()` null, `isCollection()` false) is fully implemented and `/Entity(1)/DynName` resolves
  correctly at the URI-parser layer — but `ODataDispatcher.handleResourceDispatching` (server-core)
  has no `dynamicProperty` case in its switch, so it falls to the default branch and returns
  **501 Not Implemented** on the modern, processor-based dispatch stack (`ODataHandler`/
  `ODataDispatcher`), regardless of which processors are registered or whether the addressed
  instance has a value for that property. The older `ServiceHandler`-based stack in
  `server-adapter-servlet`/`server-core-ext` **404s** the same request (no equivalent hook exists
  there either). This was a deliberate decision, not an oversight to silently patch: serving it
  would require a new processor contract (there is no `DynamicPropertyProcessor` today), which was
  judged out of scope for this feature. Pinned by
  `ODataHandlerImplTest#dynamicPropertyDirectPathAddressingIsNotServedByDispatcher` (server-test)
  and documented in `docs/site/guides/open-types-guide.md`'s "Direct path addressing (parses, not
  served)" section. Revisit only as a deliberate follow-up feature.
- **"Nested/lambda dynamic-property paths… rejected" only means a dynamic name is a leaf.** The
  out-of-scope bullet above was ambiguous: it could be misread as rejecting any path that merely
  *passes through* a declared complex segment on the way to a dynamic leaf. That case is in fact
  supported (`parsePropertyPathExpr`/`SelectParser#addSelectPath` recurse generically through
  declared complex segments regardless of depth) and is exercised by
  `OpenTypeUriParserTest#filterOnNestedDynamicPropertyUnderDeclaredComplexParses`,
  `#selectNestedDynamicPropertyUnderDeclaredComplexParses`, and
  `ODataJsonSerializerOpenTypeTest#selectOfNestedDynamicPropertyUnderDeclaredComplexEmitsOnlyThatMember`.
  What genuinely IS rejected: a segment following the dynamic name itself
  (`$filter=DynamicInt/Foo`, pinned as a `UriParserSyntaxException` leftover-token error by
  `OpenTypeUriParserTest#filterOnDynamicPropertyWithTrailingSegmentRejected`) and a dynamic property
  immediately before `$value` (`ESOpen(1)/DynamicString/$value`, pinned as a `UriValidator`
  `UNALLOWED_KIND_BEFORE_VALUE` rejection by
  `UriValidatorTest#dynamicPropertyValueRejectedByUriValidator` — this parses fine and is only
  caught at validation time, not at parse time as one might expect).
- **Client-side write annotation support for dynamic values covers scalars, not collections.** A
  later commit (`18eec232a`, pre-dating this review) closed a silent-corruption gap for dynamic
  *scalar* primitives under `odata.metadata=minimal` by always annotating non-JSON-native kinds.
  The same gap still exists for dynamic *collections*: `JsonSerializer#scalarPrimitiveKind` returns
  `null` for any collection value, so a dynamic `Collection(Edm.Guid)` property is written with no
  `name@odata.type` annotation under minimal metadata and will silently mistype as
  `Collection(Edm.String)` on read by an open-type server. The read side already handles an
  annotated dynamic collection correctly when one is present (no gap there). Pinned, not fixed, by
  `OpenTypeClientTest#dynamicGuidCollectionWrittenUnderMinimalMetadataHasNoTypeAnnotationYet` and
  `#deserializesAnAnnotatedDynamicGuidCollectionProperty`; extending the write side to collections
  is a candidate for a dedicated follow-up.

## Rollout

Three independently green, landable stages matching the sections: (1) server payload layer, (2) URI/query layer + tecsvc evaluation, (3) tecsvc model + fit + client pinning + docs.
