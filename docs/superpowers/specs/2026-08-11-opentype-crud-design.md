# OpenType Follow-up: Dynamic-Property CRUD & Client Collection Annotation — Design

**Date:** 2026-08-11
**Status:** Approved (design session 2026-08-11)
**Base:** merged OpenType feature (master `0c1711a69`); supersedes two entries in
`2026-08-09-opentype-design.md` §Deviations: "Direct path addressing is parsed, not served" and
"Client collection-write annotation gap".

## Goal

1. Serve direct dynamic-property paths (`/ESOpen(1)/DynamicString`) with full CRUD on the modern
   processor-based dispatch stack (currently a deliberate 501).
2. Close the client gap: annotate dynamic collection writes (`name@odata.type: "#Collection(Kind)"`)
   for non-JSON-native element kinds under minimal metadata.

## Scope

**In scope**

- Server dispatch: `GET`/`PUT`/`PATCH`/`DELETE` on a dynamic-property path segment (JSON).
- tecsvc reference-processor semantics for all four methods + fit end-to-end coverage.
- Additive server-api deserialization entry point for single-property payloads without an
  `EdmProperty`.
- Client: collection `@odata.type` write annotation symmetric with the scalar rule.
- Docs/spec updates replacing the two superseded deviation notes.

**Out of scope (current behavior kept, pinned)**

- `$value` on dynamic paths: stays rejected (existing pin).
- Legacy `server-core-ext`/`ServiceHandler` stack: keeps its 404 pre-check.
- XML payloads; dynamic complex values; `$expand`/`$apply`; everything already out of scope in the
  base spec.

## Design

### 1. Dispatch & validation (server-core)

- `ODataDispatcher.handleResourceDispatching`: `case dynamicProperty` routes to the primitive
  dispatch flow (`RepresentationType.PRIMITIVE`) for GET/PUT/PATCH/DELETE, replacing the 501
  default. The 501 pin test flips into routing assertions.
- `UriValidator.validatePropertyOperations` (and any method-permission gate the flow hits): accept
  PUT/PATCH/DELETE for dynamic segments. Nullability checks don't apply (no EDM property).
- Collection-valued dynamics: parse-time `isCollection()` is `false`, so dispatch selects
  `PrimitiveProcessor`; the reference processor branches on the stored value's shape at runtime and
  serializes a collection payload when the value is one.

### 2. Semantics (tecsvc processor)

| Method | Behavior |
|--------|----------|
| GET, present | 200; primitive (or primitive-collection) payload; EDM kind resolved from the stored `Property` type string, `Edm.String` fallback; context URL from that type |
| GET, absent | 404 (spec: dynamic property that does not exist) |
| PUT / PATCH | identical for dynamic scalars: replace the value; payload `@odata.type` honored and may change the property's type; unannotated → inference (entity-payload rules) |
| DELETE | removes the property from the instance (undeclared → no schema slot to null; mirrors shipped PUT-omission semantics). 204; subsequent GET → 404 |
| Closed types | untouched; unknown segments still `PROPERTY_NOT_IN_TYPE` (pins kept) |

### 3. Server-api addition (additive, compatible)

`ODataDeserializer` gains
`default DeserializerResult dynamicProperty(InputStream stream, String propertyName)` throwing
`DeserializerException` not-implemented by default; `ODataJsonDeserializer` overrides it, reusing
the existing dynamic-property inference/annotation helpers (scalar + collection, null, error cases
identical to the entity-payload path). Same compatibility pattern as `EdmAnnotation.getTermName()`.

### 4. Client collection annotation (client-core)

`JsonSerializer.valuable()` collection branch mirrors the scalar rule: element kind outside
`JSON_NATIVE_KINDS` (8 kinds) → write `name@odata.type` as `"#Collection(<Kind>)"` whenever
metadata level is not `none`. The pinned known-gap test flips to the positive assertion; the server
already ingests this exact form.

### 5. Error handling

| Case | Behavior |
|------|----------|
| GET absent dynamic property | 404 |
| Write payload with unresolvable `@odata.type` | deserializer error (existing idiom) |
| JSON object payload for a dynamic property | rejected (`UNKNOWN_CONTENT`-class error) |
| `$value` after dynamic segment | rejected, unchanged (pin) |
| Any method on unknown segment of a closed type | unchanged (pins) |

### 6. Testing (generous by directive)

- **fit `OpenTypeITCase`**: GET present / absent-404 / annotated Guid / collection-valued; PUT
  changing value and type; PATCH; DELETE → 204 → re-GET 404; `$value` rejection pin; closed-type
  pins; mutations session-isolated.
- **server-test unit**: dispatcher routing per method; validator method permissions;
  `dynamicProperty` deserializer entry (bare/annotated/null/collection/object-rejected/garbage);
  serializer context URL for dynamic reads.
- **client-core**: collection write annotation (flip pin), collection read round-trip, and a
  client→server symmetry test (client-serialized payload accepted by server deserializer with
  types preserved).
- **Full plain build** gate at the end (`-Dquarkus.http.test-port=8083` while port 8081 is held by
  an unrelated local container).

## Rollout

Three landable stages: (1) server dispatch+validation+deserializer API with unit tests,
(2) tecsvc semantics + fit CRUD suite, (3) client collection annotation + symmetry tests + docs.
