# Tier 6: OData 4.01 Compliance Milestone — Design

Date: 2026-08-19
Status: approved-in-session (design dialogue 2026-08-18/19); spec review pending
Baseline: master `d53b8c272` (Tier 5 milestone + follow-up sweep merged and pushed)
Normative citations: [2026-08-19-tier6-citations.md](2026-08-19-tier6-citations.md) — verbatim OASIS clause quotes for every feature, plus an explicit **Gaps** list of everything that could not be found verbatim. This design defers to that file wherever they overlap; the citations file governs.

## Goal

Implement the seven Tier 6 items (OLINGO-1300, 918, 1588, 1505, 1066, 1235, plus PR#11 as a free rider) across server and client, with tecsvc + fit round-trip proof for each. **Standards compliance is the governing criterion** (user directive 2026-08-19): where the current implementation deviates from OASIS, the implementation is corrected rather than worked around; where the reference service cannot fully implement a computation, it answers 501 and the limitation is recorded here as a tecsvc limitation, never as a library one.

Everything is additive at the API level: new classes, new interfaces, and `default` interface methods only — no signature changes, no removals. With no new format, flag, or preference in play, existing behavior is byte-identical (pinned).

## Milestone structure

- **Three waves, each its own feature branch off master, merged and pushed between waves:**
  - Wave 1 `feature/tier6-wave1`: CSDL JSON metadata reader, server + client (OLINGO-1300) + JSON metadata writer conformance fixes + PR#11
  - Wave 2 `feature/tier6-wave2`: geo types end to end (OLINGO-918) + entity-typed values in JSON payloads (OLINGO-1588)
  - Wave 3 `feature/tier6-wave3`: streamed primitive/complex collections (OLINGO-1066), stream-property instance annotations (OLINGO-1505), framework-level `respond-async` (OLINGO-1235)
- Each wave: implementation plan (superpowers:writing-plans) → subagent-driven tasks with per-task reviews → final whole-branch review → full plain 38-module gate (`mvn -B install --fail-at-end -Dquarkus.http.test-port=8083`) → merge → push.
- Standing constraints: NO AI attribution in commit messages; SiteNetSoft copyright line before the Apache header's closing `*/` on every modified Java source file (main and test), new files copy a sibling's full header; checkstyle LineLength 120; generous tests, with a closed-behavior pin for every gate opened; client+server parity where a feature has a client half; `fit` skips checkstyle under `-Pbuild.fast`, so a plain non-fast `mvn -B install -pl fit -DskipTests` runs before any commit touching fit.
- Environment note: this machine has hardware-level instability (MCE on CPU 12) producing transient JVM SIGSEGVs in forked build JVMs. A build that dies with SIGSEGV/exit 134 is re-run once after deleting `hs_err_pid*.log`/`replay_pid*.log`; it is not a code regression.
- Closing tag at milestone end (`5.0.1-M3` or the release itself — decided then).

---

## Feature 1: CSDL JSON metadata document (OLINGO-1300) — Wave 1

Normative: [OData-CSDLJSON] §2.1 (requesting the JSON representation), §4 document object, §5 schema, §6 entity type, §7 structural property, §8 navigation property, §9 complex type, §10 enum type, §11 type definition, §12 action/function, §13 entity container, §14.2–14.4 annotations and expressions; [OData-Protocol] §11.1.2 (XML is the default when no format preference is given) and the §13 conformance ladder — CSDL JSON is a **MAY** at 4.0-Minimal, a **SHOULD** at 4.01-Minimal and a **MUST at 4.01-Advanced**.

### 1a. Server: reading CSDL JSON

Today `lib/server-core-ext` has `MetadataParser` (StAX + an `ElementReader` visitor) producing a `SchemaBasedEdmProvider`; there is no JSON equivalent.

Design:
- New `MetadataJsonParser` in the same package, Jackson tree-based (`ObjectNode` walk), producing the **same** `SchemaBasedEdmProvider` from the same fluent configuration surface (`parseAnnotations`, `referenceResolver`, `useLocalCoreVocabularies`, `recursivelyLoadReferences`, `implicitlyLoadCoreVocabularies`) and the same entry-point shapes (`buildServiceMetadata(Reader)`, `buildEdmProvider(Reader)`, `addToEdmProvider(SchemaBasedEdmProvider, Reader)`).
- The reference-loading / core-vocabulary / global-reference-dedupe block is extracted from `MetadataParser` into a shared package-private collaborator so both parsers resolve `$Reference`/`edmx:Reference` identically. The StAX visitor itself is **not** shared — it is format-bound, and an artificial abstraction over both would only pretend to be DRY.
- JSON defaults are applied, not XML defaults. The load-bearing case: `$Nullable` defaults to **false** in CSDL JSON, whereas the XML `Nullable` attribute defaults to **true**. Every facet default is taken from the citations file, not from the XML parser.
- Version handling: the XML parser accepts only `"4.0"` and rejects `4.01`. The JSON parser accepts `$Version` `"4.0"` and `"4.01"`, and the XML parser's gate is widened to match (a 4.01 metadata document is not an error in a 4.01 library). Unknown versions are rejected with the existing error taxonomy.

Decisions on spec-silent points:
- Malformed input → `ODataException`-family failure carrying the JSON member path that failed (e.g. `Namespace.Type/$Key[0]`). The spec prescribes no error model for a provider-side parse failure; a path-carrying message is what makes a 1300-line metadata document debuggable.
- The parser is strict-to-spec on everything it *emits* into the Csdl model, and **tolerant on input** for exactly the two structural quirks the old Olinguito writer produced (see 1b): a container nested under an `Extending` member, and a document with no top-level `$EntityContainer`. Both are accepted and normalized; neither is ever written.

### 1b. Server: JSON metadata writer conformance

`MetadataDocumentJsonSerializer` already emits CSDL JSON, but deviates from OASIS in ways a conformant client cannot read. Because the milestone is standards-first and CSDL JSON is a 4.01-Advanced MUST, the writer is corrected:

| Deviation today | Correction |
|---|---|
| No document-level `$EntityContainer` member | Emit `$EntityContainer` with the container's fully qualified name (§4) |
| Container inheritance emitted as a nested `Extending` object with `$Kind`/`$Extends` | Emit `$Extends` directly on the container object (§13) |
| Enum member values written as strings | Emit numeric member values (§10) |
| Type-definition facets written as strings while property facets are numbers | Emit numbers consistently (§7, §11) |
| `$IsFlags` always written | Emit only when true (§10) |
| `$Version` hard-coded `"4.01"` | Emit the version the service is serving |

`$SRID` stays a string — that is what CSDL JSON specifies. XML metadata output is untouched; the XML serializer tests are the closed-behavior pin.

### 1c. Client: reading CSDL JSON

Today the client's metadata path is XML-only and hard-wired: `AbstractMetadataRequestImpl` forces `Accept`/`Content-Type` to `application/xml` and no-ops the setters; `ClientODataDeserializerImpl.toMetadata` is Jackson-XML; `EdmMetadataRequestImpl` composes the XML request to build an `Edm`.

Design:
- A JSON metadata deserializer in `client-core` produces the **same** `XMLMetadata`/`CsdlSchema` object graph the XML path produces, so `ODataReaderImpl.readMetadata` and all `Edm` construction are reused unchanged. (The type is named `XMLMetadata` for history; it is the client's format-neutral CSDL holder.)
- `client-api`: new `JSONMetadataRequest` alongside `XMLMetadataRequest`, and `RetrieveRequestFactory.getJSONMetadataRequest(String serviceRoot)` added as a **`default` method** so third-party factory implementations keep compiling. The JSON deserializer is reached through a new method on the deserializer interface, also `default`.
- `AbstractMetadataRequestImpl` gains a format-aware constructor; the existing constructor delegates with `APPLICATION_XML`, so the XML request's wire behavior is byte-identical.
- Format selection for `getMetadataRequest(...)` (the `Edm`-returning convenience): a new `Configuration.setMetadataFormat(ContentType)` defaulting to XML, overridable per request. Default-path behavior is unchanged, which also satisfies [OData-Protocol] §11.1.2 (XML when no format preference is expressed).

### 1d. Free rider

PR#11 — `MetadataParser` applies the wrong `Nullable` default for navigation properties. One-line fix in `server-core-ext`, pinned by a test, carried in this wave because it lives in the file being extended.

Tests: XML↔JSON equivalence (parse the same model from both representations, assert equal `Csdl*` graphs) over the TripPin fixture; writer→parser round trip through the corrected serializer; per-element unit coverage for the tricky shapes ($Key with aliases, action/function overload arrays, `$ReferentialConstraint`, `$OnDelete`, annotations incl. dynamic expressions); JSON-default pins ($Nullable false); client resource-driven `toMetadata` tests mirroring `MetadataTest`; fit round trip against tecsvc `$metadata?$format=json` producing an `Edm` equal to the XML-derived one; a pin that the XML parser now accepts a `4.01` document (the widened version gate) and still rejects unknown versions; closed pins (XML metadata output byte-identical; default client metadata format still XML).

---

## Feature 2: Geo types end to end (OLINGO-918) — Wave 2

Normative: [OData-JSON] §7.1 (GeoJSON with two modifications: keys SHOULD be ordered `type`, `coordinates`, then others; an optional CRS object MUST be `type: "name"` with an EPSG legacy identifier); [OData-URL] §5.1.1.14.1 primitive literals and §5.1.1.11.1–.3 (`geo.distance`, `geo.length`, `geo.intersects`); [OData-ABNF] geo literal rules; [OData-CSDLXML]/[OData-CSDLJSON] §7.2.6 SRID facet.

What exists: the geo model (`commons-api .../edm/geo`), 16 `EdmGeography*`/`EdmGeometry*` primitive types with URI-literal *parsing*, GeoJSON read/write in the JSON serializer and deserializer, and full URI tokenizer/parser support for geo literals and the three `geo.*` methods.

What is missing, and the design:
- **URI literal round trip.** `toUriLiteral`/`fromUriLiteral` are not overridden for geospatial types, so a parsed geo value cannot be rendered back into the `geography'SRID=…;Point(…)'` form. Implemented for all 16 types per the ABNF rules quoted in the citations file.
- **GeoJSON conformance verification.** Key order `type`, `coordinates`, then others (SHOULD); CRS emitted as `type: "name"` with the EPSG legacy identifier when an SRID must be conveyed; `crs` accepted on read. The JSON Format spec carries exactly one geo example and never names the GeometryCollection member (recorded gap) — the implementation follows RFC 7946 (`"type": "GeometryCollection"`, `"geometries": [...]`), which is what the citation's normative reference points at.
- **tecsvc reference implementation.** A geo entity type and set (`ETGeo`/`ESGeo`) with properties covering point/line/polygon/collection in both geography and geometry flavors, seeded in `DataCreator`, supporting full CRUD.
- **Filter evaluation.** `geo.distance`, `geo.length`, `geo.intersects` implemented in tecsvc's expression evaluation, which today answers 501 for all three. Geometry (planar) values use Cartesian math; Geography values use great-circle (haversine) distance on the WGS-84 mean radius. `geo.intersects` is implemented for Point × Polygon (ray casting), which is the only overload the spec defines.
- **Comparison restrictions.** Geo values are not comparable or orderable and cannot be key properties; the existing parser restrictions are pinned rather than relaxed.

Decisions on spec-silent points (each also listed under Recorded deviations):
- `SRID="variable"` has no defined wire behavior in any spec text. Values keep whatever SRID they carry; a literal without an explicit `SRID=` prefix uses the type's default (4326 geography / 0 geometry) per the CSDL facet defaults.
- The OData ABNF permits a fourth position element (a linear-referencing measure) that RFC 7946 advises against. A literal carrying one is parsed and validated, then the fourth element is dropped: the geo model has no M coordinate (see recorded deviation 3).
- tecsvc's geo math is a reference implementation, not a geodesy library: haversine (not Vincenty/geodesic) distance, planar polygon containment, no CRS re-projection. Anything beyond Point × Polygon intersection answers 501. This is a **tecsvc** limitation; the library imposes none of it.

Tests: primitive-type units (literal round trip per type, SRID handling, malformed literal errors); serializer/deserializer units (key order, CRS, GeometryCollection, all seven geo shapes); URI parser pins (existing geo literal tests kept; comparison/key restrictions pinned); tecsvc `DataProviderTest` for the geo set; fit `GeoITCase` through the real client — CRUD round trip, `$filter` with each of the three functions, an unsupported-overload 501 pin, and a closed pin that non-geo requests are unaffected.

---

## Feature 3: Entity-typed values in JSON payloads (OLINGO-1588) — Wave 2

Normative: [OData-JSON] §6 (an entity as a value), §13 (collection of entities), §18 Action Invocation — "Entity typed parameter values MAY include a subset of the properties, or just the entity reference"; §14 / §4.5.8 (`@id` by-reference form); [OData-CSDLXML] §7/§7.1 vs §8.1 (structural properties are primitive/complex/enum/typedef only — entity types reach payloads through navigation properties, action/function parameters, and return types, never as a structural property value).

Today `ValueType.ENTITY`/`COLLECTION_ENTITY` are *produced* by the JSON and XML deserializers for entity-typed action parameters, but `ODataJsonSerializer.writePropertyValue` throws `UNSUPPORTED_PROPERTY_TYPE` for them — the round trip is asymmetric, and any entity value reaching the writer is a 500.

Design:
- The JSON serializer gains `ENTITY` and `COLLECTION_ENTITY` branches that delegate to the entity writer (and to the entity-collection writer for the collection case), including the by-reference `@id`-only form when the value carries just an entity reference.
- Because a structural property can never be entity-typed, the branches serve parameter and return-value writing; the property writer's error for a genuinely illegal shape is preserved (a `Property` whose declared type is not entity-typed still fails, and the message says why).
- XML serializer parity is checked; where the XML path already handles `COLLECTION_ENTITY` it is left alone.
- tecsvc gains actions taking an entity-typed parameter and a collection-of-entities parameter, so the deserialize → evaluate → serialize round trip is exercised end to end. Adding these changes `$metadata` fixtures; those expectations are updated honestly.

Decisions on spec-silent points:
- A parameter value carrying only `@id` is resolved against the service's own entity sets; an unresolvable reference is 400 (the spec states the by-reference form is legal but not the failure mode).
- A subset-of-properties entity parameter value is accepted as-is (§18 explicitly permits a subset); missing non-key properties are not defaulted.

Tests: serializer units for entity value, entity-collection value, and `@id`-only form; deserialize→serialize round-trip unit; tecsvc action wiring; fit ITs invoking both new actions through the client; closed pins (a non-entity `Property` with an entity `ValueType` still errors; existing action payloads unchanged).

---

## Feature 4: Streamed primitive and complex collections (OLINGO-1066) — Wave 3

Normative: [OData-JSON] §4.4 (payload ordering and the `streaming=true` contract). Part 1 is silent on chunked delivery for ordinary responses (recorded gap) — this is a delivery/efficiency feature whose observable payload is byte-identical to the buffered form.

Today streaming exists for entity collections only: `EntityIterator` + `ODataSerializer.entityCollectionStreamed` + `ODataWritableContent`. Primitive and complex collections have no streaming path, so a large collection is fully materialized.

Design:
- `commons-api` gains a `PropertyIterator` abstraction mirroring `EntityIterator` (an iterator of `Property` values with the collection's metadata).
- `ODataSerializer` gains `primitiveCollectionStreamed(...)` and `complexCollectionStreamed(...)` as **`default` methods** throwing the existing not-implemented `SerializerException`, so every downstream implementor of the interface keeps compiling.
- `ODataWritableContent` gains the corresponding content shapes; the Netty adapter already transports `ODataContent`, so no adapter change is required.
- tecsvc exposes streamed primitive and complex collection endpoints mirroring `ESStreamServerSidePaging`, proving the path end to end.
- The `omit-values` gate and per-write state resets that the entity-collection streaming path performs are replicated, so Tier 5 behavior is not regressed.

Tests: serializer units comparing streamed output byte-for-byte with the buffered output for the same data; an empty-collection pin; tecsvc endpoint units; fit round trip; closed pins (existing buffered collection responses unchanged; a serializer that does not implement the new methods still gets the documented not-implemented error).

---

## Feature 5: Stream-property instance annotations (OLINGO-1505) — Wave 3

Normative: [OData-JSON] §4.5.12 (the combined `media*` control information — which links are MUST and which MAY), §9 (stream property representation, `Name@media*` form), §10 (media entity contrast case), §3.1.2 (`metadata=minimal`/`full`/`none` omission rules), §4.5 (the 4.01 unprefixed vs 4.0 `odata.`-prefixed names).

Today `name@mediaEtag`/`name@mediaContentType` are written for stream properties, but `name@mediaReadLink`/`name@mediaEditLink` only at `metadata=full`.

Design:
- Emit read/edit links at `metadata=minimal` whenever they are **not computable** from the metadata URL and the entity's id — which is precisely the §3.1.2 rule the current gate ignores. At `metadata=full` behavior is unchanged; at `metadata=none` all control information stays suppressed.
- Verify and, where missing, implement custom-term instance annotations on stream properties (`Name@Namespace.Term`), which the instance-annotation serializer already models for other property kinds.
- XML serializer parity for the same information.
- The Tier 5 `omit-values` interaction is preserved: a stream property carrying annotations is never omitted.

Decisions on spec-silent points:
- Whether `mediaContentType` may be omitted at `metadata=full` is genuinely ambiguous in §3.1.2 (recorded gap). We always write it when known — the safer reading of "MUST include all control information".

Tests: serializer units for each metadata level × (computable | non-computable) links; annotation-on-stream-property unit; XML parity unit; tecsvc `ETWithStream`/`ESWithStream` fit round trips; closed pins (media *entity* links unchanged; 4.0 vs 4.01 name prefixes unchanged).

---

## Feature 6: Framework-level `respond-async` (OLINGO-1235) — Wave 3

Normative: [OData-Protocol] §8.2.8.8 (`respond-async`), §8.2.8.10 (`wait`), §8.2.8.2 (`callback`), §11.6 (asynchronous requests and the status-monitor resource: 202 + `Location`, the monitor returning 202 while running or the wrapped `application/http` result when done, `Preference-Applied: respond-async`, `Retry-After`), and the §13 async conformance lines.

Today the preference is parsed (`Preferences.hasRespondAsync()`) and echoed (`PreferencesApplied.Builder.respondAsync()`), but every decision to go async is duplicated in *user* processor code: tecsvc checks the header in four places and hand-rolls registration, and the status-monitor resource is implemented in the servlet module.

Design:
- A small `server-api` SPI — an async-support interface with the three operations the flow needs (register an invocation, expose a monitor resource, dispatch/resolve it) plus the value types for a pending result.
- `ODataHandlerImpl` consults the SPI when `Prefer: respond-async` is present, before processor dispatch: with no SPI registered, behavior is exactly today's (preference ignored, not echoed); with an SPI, the handler registers the invocation and returns 202 + `Location` + `Preference-Applied: respond-async`, and it owns recognizing the monitor URL.
- Adapters (servlet, netty, quarkus) route the monitor URL to the handler rather than each re-implementing it; the existing `AsyncResponseSerializer` produces the `application/http` envelope.
- tecsvc's `TechnicalAsyncService` is migrated onto the SPI and its four duplicated header checks are removed, with the fit async suites as the behavioral pin.
- `wait` and `callback` are parsed and echoed as today; honoring `callback` is out of scope for this milestone and recorded as such.

Decisions on spec-silent points:
- `Retry-After` has no specified value format beyond "duration of time, in seconds" (recorded gap); the monitor emits a fixed conservative interval.
- `Content-Transfer-Encoding: binary` is **not** part of the async wrapper (its only normative mention is about multipart batch parts) — the envelope omits it.

Tests: handler units (preference present with and without an SPI; monitor states 202 → 200 → 404 after retrieval); SPI contract unit; adapter routing units where a test harness exists; tecsvc migration keeps `AsyncSupportITCase`/`BasicAsyncITCase`/`AsyncTestITCase` green unchanged — those are the closed-behavior pins; a new pin that a service without async support is byte-identical to today.

---

## Conformance summary

- **MUST-level clauses implemented and pinned:** CSDL JSON document structure and element representations for the full model (§4–§14), and its availability at `$metadata?$format=json` (4.01-Advanced MUST); GeoJSON representation with the CRS `type: "name"` rule and the geo literal grammar; the three geo function signatures; entity-typed parameter values including the by-reference form (§18); the `media*` control information and its metadata-level omission rules (§4.5.12, §3.1.2); the §11.6 asynchronous request flow and status-monitor semantics.
- **SHOULD-level honored:** GeoJSON key ordering (`type`, `coordinates`, then others).
- **MAY-level declined, deliberately:** `callback` is not honored (parsed and echoed only); geo computations beyond the specified overloads are not attempted by tecsvc.
- **Recorded deviations:**
  1. tecsvc geo math is a reference implementation — haversine distance for geography, planar for geometry, Point × Polygon intersection only, no CRS re-projection; anything else is 501 (tecsvc only, not the library).
  2. `SRID="variable"` wire behavior is spec-silent; values keep the SRID they carry and literals without a prefix use the CSDL facet default.
  3. The ABNF's fourth position element (linear-referencing measure) is parsed and validated but not retained (the geo model has no M coordinate: `Point` carries `x`, `y` and `z`, and adding an M coordinate would change `equals`/`hashCode` on a public value class). A literal carrying one is accepted but does not round-trip byte-identically.
  4. GeometryCollection member naming follows RFC 7946 (`"geometries"`), since [OData-JSON] never names it.
  5. The CSDL JSON parsers tolerate on input every shape the old Olinguito writer emitted — the `Extending`-nested container, an absent `$EntityContainer`, `"$Kind": "EntitySet"` without `$Collection`, the `$OnDelete` object, `$Type` on a record, and the CSDL-XML-named constant members `$Binary`/`$Int`/… — and never write any of them. Scouting the real documents added the last four to the list the design named.
  6. Malformed CSDL JSON errors carry a JSON member path — a spec-silent implementation choice.
  7. `mediaContentType` is always written when known at `metadata=full`, resolving a §3.1.2 ambiguity conservatively.
  8. `Retry-After` uses a fixed interval; the format is spec-silent. The async wrapper omits `Content-Transfer-Encoding`.
  9. An entity parameter value carrying an unresolvable `@id` is 400 — spec-silent failure mode.
  10. CSDL JSON carries no per-value type marker for a constant expression (§14.3 renders `Binary`, `Date`, `DateTimeOffset`, `Duration`, `EnumMember`, `Guid`, `String` and `TimeOfDay` alike, as JSON strings), so a constant's type is recovered from the term's declared type and falls back to `String` when the term cannot be resolved. Values are exact; only the type tag normalizes.
  11. `$Has` and `$In` (§14.4.2) are parse errors, not silent drops: `CsdlLogicalOrComparisonExpression` declares no constants for them and growing that public enum is not an additive change. The parser names the member it refused.
  12. Geography `geo.distance`/`geo.length` are answered in **metres** (haversine on the WGS-84/IUGG mean radius 6 371 008.8 m), not in the CRS's own degree units. [OData-URL] §5.1.1.11 says only "in the coordinate reference system signified by … the SRID"; a distance in degrees is not a distance. Geometry values are planar and therefore genuinely in the SRID's units. tecsvc only. SRID itself is never compared — only the dimension is — so no re-projection or rejection happens between differing SRIDs.
  13. Geo values are served as JSON only, and this is a **behavior change for any existing service**, not just for `ESGeo`. The JSON deserializer now tags every geo value `ValueType.GEOSPATIAL`, where such a value used to arrive as a `PRIMITIVE` string that the XML/Atom and JSON-delta writers emitted as a (non-conformant) WKT string. Those writers now refuse it: `ODataXmlSerializer` throws `UNSUPPORTED_PROPERTY_TYPE` for geospatial values (scalar and collection alike), and so do `JsonDeltaSerializer` (335-337, 374-379) and `JsonDeltaSerializerWithNavigations` (399-404, 444-446). An Atom/XML representation of a geo entity is not available in this release and the request answers 400; a delta response carrying a geo property likewise. Pinned by `GeoITCase#esGeoInXmlIsNotSupported`. Two follow-on consequences: `Valuable.asPrimitive()` returns `null` for a deserialized geo property (a custom processor reading geo that way silently gets null and must use `asGeospatial()`), and a **type definition** whose underlying type is geospatial is still tagged `PRIMITIVE` by `valueTypeFor` - matching `readPrimitiveValue` - so it serializes as a WKT string rather than GeoJSON.
  14. The [OData-URL] §5.1.1.1 restriction "Edm.Binary, Edm.Stream, and the Edm.Geo types can only be compared to the null value" is enforced for the **Geo** types only. `Edm.Binary` and `Edm.Stream` comparisons remain permitted, because `ExpressionParserTest` pins `PropertyBinary eq binary'VGVzdA=='` as valid and tightening them is outside this wave's feature boundary.
  15. Entity-typed values are **JSON-only**: `ODataXmlSerializer`, the client's `AtomSerializer` and the delta serializers all still refuse `ValueType.ENTITY`/`COLLECTION_ENTITY`, while the XML *deserializer* produces them — the XML round trip stays asymmetric exactly as JSON was before this wave. The §18 partial-entity form is a send-only shape: `writeProperty` writes an explicit `null` for a declared-but-absent nullable property and throws `MISSING_PROPERTY` for a non-nullable one, so a service answers with a complete value or the `@id`-only reference.
  16. Two entity-parameter readings are decisions, not spec requirements: an `@id` **alongside** properties is ignored (the value is read as §18's "subset of the properties", so no lookup happens and the unresolvable-reference 400 cannot apply), and a reference matches the stored canonical id by equality **or** by a `/`-bounded suffix, so both the relative `ESTwoPrim(32767)` of §4.5.8 and the absolute canonical URL resolve while a partial suffix such as `(32767)` does not. Reading the id stays confined to the parameter path: on an ordinary entity payload a 4.0 `@odata.id` remains ignorable control information (§4.5) and never reaches `Entity.getId()`, which would put a client-controlled value into `OData-EntityId`.
  17. Minimal metadata cannot convey a geo dimension to a non-EDM client (`{"type":"Point","coordinates":[…]}` is byte-identical for the geometry and geography flavours) and the client's fallback guess is `Edm.Geography*`. Pinned by `GeoITCase#readGeoCollectionAtMinimalMetadata` rather than papered over; emitting `@odata.type` for geo at minimal metadata would be a wire-format change outside this wave.
  18. `SRID="variable"` (deviation 2) is a hard **URL literal round-trip break**, pre-existing: the model renders `SRID=variable;`, for which the ABNF (`1*5DIGIT`) has no production, so there is no compliant literal to emit. A genuine `z == 0.0` is likewise dropped on output, because `Point.getZ()` is a primitive `double` that cannot distinguish "no altitude" from "altitude zero" — the same rule the JSON serializer has always applied.
  19. The streamed collection writers emit **no `operations` and no instance annotations**, because a `PropertyIterator` carries neither. The buffered writers still emit both, so byte-for-byte parity between the two forms holds only for data that has neither — which is all of tecsvc's.
  20. Streamed collection properties are **JSON-only**: `ODataXmlSerializer` deliberately inherits the not-implemented `default` methods added to `ODataSerializer` and answers `NOT_IMPLEMENTED`. A property-stream caller also cannot register an `ODataContentWriteErrorCallback`, since neither `PrimitiveSerializerOptions` nor `ComplexSerializerOptions` carries one; the write path is null-safe about it rather than exposing the option, which would be a public API change.
  21. The stream-property data model carries **one `Link` per property**, so a service can publish a read link **or** an edit link, never both, and [OData-JSON] §4.5.12's "mediaReadLink MUST be included if its value differs from the value of the associated mediaEditLink" is unreachable. `Property`/`Link` were deliberately not extended in this wave.
  22. The **computed stream link** is defined as `<the entity's edit link, else its id> + "/" + <property name>`; §4.5.12 says only "standard URL conventions" and never writes the URL out. A link equal to that is omitted at `metadata=minimal` (§3.1.1's "MAY be omitted if the client can compute it"), and any other link is written.
  23. A stream property inside a **`ComplexValue` has no computable link** — the complex value has no edit link or id of its own — so its link is always published above `metadata=none`.
  24. `isComputedStreamLink` compares the href to the computed string **as raw strings**, with no URI resolution or normalisation. An absolute stream href therefore never matches a relative entity id and vice versa, so a service mixing the two forms publishes a link the client could have computed. Safe under §3.1.1 ("MAY appear otherwise"), but conservative; resolving both sides against the service root would tighten it.
  25. **Stream properties are refused in XML** (`UNSUPPORTED_PROPERTY_TYPE`, a 400 naming the property): [OData-Atom] is outside this milestone's cited sources, so there is no conformant XML form to write. Before this wave the XML serializer wrote the link's bare href as the element's text content — a payload with no defined meaning, not an exception. Because a *streamed* entity-collection response commits its status and content type before serialization reaches the property, the refusal for such an entity set is made by a static, data-free EDM check on `entityCollectionStreamed` (cycle-guarded, walks complex properties), so `ESStreamServerSidePaging?$format=xml` answers a clean 400 rather than a truncated 200. `Edm.Stream` is likewise refused by the standalone `primitive(...)`/`primitiveCollection(...)` JSON entry points, which have no entity in scope to compute a link against.
  26. **The exception to the XML refusal**: a nullable stream property whose value is null still serializes as `m:null="true"`. Nulls are handled before the guard, and a null *is* representable in XML.
  27. `Retry-After` is whatever the registered `AsyncSupport` returns and is **absent by default**; [OData-Protocol] §8.3.7 specifies only "the duration of time, in seconds", and the reference service supplies none.
  28. The async wrapper omits **`Content-Transfer-Encoding`** — its only normative mention (§11.7.7.1) is about multipart batch parts.
  29. A monitor request with **neither `Accept` nor `OData-MaxVersion`** gets the unwrapped `AsyncResult` form. §11.6's two sentences do not partition the space (one names 4.01-or-greater responses, the other only "MaxVersion 4.0 with no Accept"), and §13.2.1 makes `AsyncResult` the 4.01-Minimal MUST. The client reads both shapes, so nothing depends on the choice.
  30. The `Accept` test for the wrapped form is a **look at the media ranges, not a negotiation**: a literal `application/http` or the `application/*` range asks for the wrapper; `*/*`, a bare `*` and every other type do not, and `q=0` is not honoured. §11.6 says "an Accept header that includes application/http"; a full `AcceptType.parse` would reject monitor requests the OData negotiator dislikes, which §11.6 never asks for. Reading `*/*` as coverage was tried and reverted: §11.6's two triggers are a matched pair (`includes` vs. `does not specify`), so a wildcard would owe both a wrapper and an `AsyncResult` header at once, and RFC 9110 equates an absent `Accept` with `*/*` while the two requests were being answered differently.
  31. **`Allow: GET` on the monitor's 405** comes from RFC 7231, not from §11.6, which mandates only the status code.
  32. On the unwrapped shape, a completed result carrying **no `OData-Version` of its own keeps the handler's preset `4.0`**. The header copy replaces only names the result actually has. Unreachable for handler-produced results (`processInternal` always sets it); possible for a custom `AsyncInvocation` that hand-builds an `ODataResponse`, where a version-less response would be worse.
  33. **`Prefer: wait` is ignored.** §8.2.8.8 says a service SHOULD process synchronously for up to that many seconds before falling back to 202; `respond-async` with `wait=10` is answered 202 immediately. Honouring it means running the invocation on the request thread with a timeout — deliberately out of scope.
  34. **`Prefer: callback` is parsed and echoed but never invoked** (§8.2.8.2 is a MAY); no HTTP call is made to the callback URL.
  35. **`AsyncSupport.cancel` returns a `boolean`**, so the SPI cannot distinguish "unknown monitor" from "cancellation not supported": a DELETE against a monitor that never existed answers 405 (or 204 for a service that always cancels) rather than 404. §11.6 names 405 as the correct answer for a service that does not support cancellation, which the reference service does not.
  36. **tecsvc's monitor is one-shot**: a second GET after the result has been retrieved answers 404, where §11.6 makes 404 correct after a successful DELETE or once retention lapses, not after one read. Pinned pre-existing behaviour of the test service, not of the library.
  37. **Asynchronous processing is available in the servlet deployment only.** The Quarkus tecsvc deployment deliberately does not register the SPI: it wires only the two OData base paths and has no `/status` route, so registering it produced a 202 whose `Location` pointed at a URL that deployment does not serve — and §11.6 requires that `Location` to point at a real status-monitor resource. Ignoring `respond-async` is explicitly conformant (§8.2.8.8 makes async a MAY); a dangling `Location` is not. A one-line registration was made and then reverted for exactly this reason. Enabling it there means routing `/status/*` into the same handler, the way the monitor servlet does.
  38. **The monitor's completed response is now unwrapped for a client that does not accept `application/http`**, where it was previously always the wrapped `application/http` message. That is the wire change of Feature 6 and the only way the §13.2.1 `AsyncResult` MUST becomes reachable. `AsyncRequestWrapperImpl` reads both shapes (branching on the monitor response's `Content-Type`); a foreign `ODataResponse` template that is not an `AbstractODataResponse` now fails loudly with a `ClassCastException` on the unwrapped path rather than silently yielding a closed stream and a 200. `Prefer: respond-async` is deliberately **left on** the detached replay request: `Prefer` carries many preferences, and stripping one means re-parsing and re-serialising the list.

- **Corrections to what the design predicted** (items a-f from Features 2 and 3; g-j from Wave 3's Features 4-6) (scouting and implementation found the baseline better than the design assumed; none of these changed the wave's scope):
  a. There are **16** `EdmGeography*`/`EdmGeometry*` primitive types, not 17 — eight geography kinds and eight geometry ones — of which the two abstract kinds (`Edm.Geography`, `Edm.Geometry`) have no value representation. All sixteen inherit the new `toUriLiteral`/`fromUriLiteral` from `AbstractGeospatialType`.
  b. **The URL literal round trip already worked.** `AbstractGeospatialType.valueToString` already emitted `geography'SRID=4326;Point(142.1 64.1)'`, and with `toUriLiteral` inherited as the identity the URL form round-tripped. What was genuinely missing was *acceptance* of the bare `full*Literal` form, application of the §7.2.6 SRID facet default, validation (`fromUriLiteral("test")` used to return `"test"`), case-insensitive matching per RFC 5234 §2.3, and the optional 3rd/4th position elements.
  c. **GeoJSON was already conformant on all three points the design named** — key order `type` → `coordinates`/`geometries` → `crs`, the `{"type":"name","properties":{"name":"EPSG:…"}}` CRS object, and the RFC 7946 `"GeometryCollection"`/`"geometries"` spelling. The defects actually found were different: the deserializer never tagged a geo value `ValueType.GEOSPATIAL` (so a value read back came out as a WKT string), a collection-valued geo property was written through the primitive writer, a `GeographyCollection`'s members were read with dimension `GEOMETRY`, the `crs` reader neither enforced the `type: "name"` MUST nor survived a missing member (a 500, now a 400), the reference service's create path mis-tagged geo values as `PRIMITIVE`, and the *client's* `JsonDeserializer` sent every geo collection member to the complex reader.
  d. **Only the relational half of the §5.1.1.1 comparison restriction was already enforced.** `RELATION_KINDS` is a closed allow-list with no geo kind, but equality (`checkEqualityTypes`) applied only an `isCompatible` test and `$orderby` applied no type test at all; both were added.
  e. **`ActionProvider` already declared an entity-typed parameter and a collection-of-entities parameter** (`UARTByteNineParam`), reachable through `AIRTByteNineParam` and pinned by existing unit and fit tests, so Feature 3's tecsvc work is **one** action rather than two: what was absent is an action that *returns* a value derived from those parameters, which is the only way the new serializer branches become reachable at the service level.
  f. **The geo-key prohibition is an inference, not prose**, so no key-type validator was added to `EdmEntityTypeImpl`; the restriction is pinned in the reference model (`ETGeo` keyed by `Edm.Int16`) instead.

  g. **Custom instance annotations on stream properties already worked.** `ODataJsonSerializer.writeProperty` has always called `writeInstanceAnnotationsOnProperties` before the stream branch, and Task 5's restructuring did not regress it, so Feature 5's second bullet was a **pin** (two new tests) rather than an implementation. The same applies to `Prefer: omit-values=nulls`, which never omitted a stream property.
  h. **No adapter change was needed to route the monitor URL to the handler.** The design predicted "adapters route the monitor URL"; in fact tecsvc's own `TechnicalStatusMonitorServlet` became the bridge — it now extends `TechnicalServlet`, keeps its `/status/*` mapping (so the pinned `Location` is unchanged) and delegates to the same handler — and the netty and Quarkus adapters never had asynchronous processing to route in the first place. The handler's monitor branch runs before URI parsing, so `/status/<n>` never has to be a parsable OData path. The tecsvc migration was a net **deletion** (`AsyncProcessor` and `TechnicalAsyncServletService` removed whole, all four duplicated `hasRespondAsync` blocks gone).
  i. **The catch chain of `ODataHandlerImpl.process` extracted cleanly.** The single riskiest change of the wave — moving twelve catch clauses into a package-private `processCatching` so the asynchronous invocation gets the same error representation — was verified byte-identical and same-order, with the `"ODataHandler"/"process"` runtime measurement deliberately left behind in `process` (an asynchronous invocation is not a `process` call).
  j. **A follow-up recorded, not fixed:** `handleException` reads the handler's mutable `uriInfo` field for error-format negotiation, which an asynchronous error path can reach on a handler shared across requests. Not reachable in committed code (the class Javadoc forbids sharing, and only `ODataNettyHandlerImpl` structurally allows it — a pre-existing condition this wave does not create). The proper fix is a `handleException` overload taking a `UriInfo`, which changes the error path for every request in the product.

## Testing and rollout

Every feature: unit tests at each touched layer, tecsvc wiring, fit round trips through the real client, negative/closed pins, and the format-absent / flag-off identity pins. Each wave ends with the full plain 38-module gate before merge, and the guide (`docs/site/guides/odata-401-features-guide.md`) plus this spec's deviation list are updated in the wave that changes the behavior they describe. Merge and push between waves; no wave depends on an unmerged predecessor except through master.
