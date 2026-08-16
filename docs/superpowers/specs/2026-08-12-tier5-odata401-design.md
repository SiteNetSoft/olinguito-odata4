# Tier 5: OData 4.01 Compliance Milestone — Design

Date: 2026-08-12
Status: approved-in-session (design dialogue 2026-08-12); spec review pending
Baseline: master `9f3a990c8`, tagged `5.0.1-M1`
Normative citations: [2026-08-12-tier5-odata401-citations.md](2026-08-12-tier5-odata401-citations.md) — verbatim OASIS 4.01 clause quotes for every feature; this design defers to that file wherever they overlap. **Conformance to the OData 4.01 standard is the milestone's primary success criterion** (user directive 2026-08-12): MUST-level clauses become mandatory behavior with pinning tests; MAY/SHOULD-level choices and spec-silent points are decided explicitly below and marked as such.

## Goal

Implement the seven Tier 5 OData 4.01 features (OLINGO-1539, 1598, 1643, 1326, 1634, 1084, 1570) across server and client, with tecsvc + fit round-trip proof for each. Everything is additive: with no new syntax/header/flag in play, existing behavior is byte-identical (pinned).

## Milestone structure

- **Three waves, each its own feature branch off master, merged and pushed between waves:**
  - Wave 1 `feature/tier5-wave1`: matchesPattern (1539), Prefer: omit-values (1598), /$query (1643)
  - Wave 2 `feature/tier5-wave2`: $schemaversion (1326), optional function parameters (1634)
  - Wave 3 `feature/tier5-wave3`: key-as-segment (1084) first, then alternate keys (1570)
- Each wave: implementation plan (superpowers:writing-plans) → subagent-driven tasks with per-task reviews → final whole-branch review → full plain 38-module gate (`mvn -B install --fail-at-end -Dquarkus.http.test-port=8083`) → merge → push.
- Standing constraints: NO AI attribution; SiteNetSoft copyright line before the Apache header's closing `*/` on modified source files; checkstyle LineLength 120; generous tests; closed-behavior pins for every opened gate; client+server parity where a feature has a client half; JSON-first (XML only where a feature is format-agnostic).
- Closing tag at milestone end (`5.0.1-M2` or the 5.0.1 release itself — decided then).

---

## Feature 1: `matchesPattern` filter function (OLINGO-1539) — Wave 1

Normative: [OData-URL] §5.1.1.7.1. Signature `Edm.Boolean matchesPattern(Edm.String, Edm.String)`; second parameter MUST be an ECMAScript (JavaScript) regular expression; returns true if the first parameter matches it. No implicit anchoring — anchoring lives in the pattern text (the spec's own example uses `^...$`). Not a §13 conformance item.

Design:
- `commons-api` `MethodKind`: add `MATCHESPATTERN("matchesPattern")`.
- `server-core` `UriTokenizer` + `ExpressionParser`: recognize the method token; parameter/return typing `(String, String) → Boolean`, beside `contains`/`startswith`. `FilterTreeToText` and URI-parser tests extended.
- Evaluation (tecsvc `ExpressionVisitorImpl`): `java.util.regex` with `Matcher.find()` (unanchored match-anywhere, matching ECMAScript `RegExp.prototype.test` semantics; patterns anchor themselves with `^`/`$`).
- Client: `FilterFactory`/`FilterArgFactory` gains `matchesPattern`.

Decisions on spec-silent points:
- Invalid regex at evaluation time → 400 Bad Request (ODataApplicationException from the visitor). Spec is silent; 400 matches the codebase's treatment of semantically invalid filter input.
- **Deviation (recorded):** patterns are evaluated with `java.util.regex`, not an ECMAScript engine. Divergent constructs (e.g. ECMAScript-specific escapes, lookbehind edge semantics) follow Java semantics. Documented in the guide; no dependency on a JS engine is justified for a server library.

Tests: parser units (valid form, wrong arity, wrong operand types), FilterTreeToText, tecsvc fit round trips (anchored + unanchored patterns, no-match, case sensitivity), invalid-regex 400 pin, closed pin (unknown method name still rejected identically).

## Feature 2: `Prefer: omit-values` (OLINGO-1598) — Wave 1

Normative: [OData-Protocol] §8.2.8.6 (quoted in full in the citations file); [OData-JSON] §24 conformance item 22 (MUST NOT omit null/default values unless the preference was specified AND echoed in Preference-Applied). Key MUSTs: when applying `nulls`, service MUST send `Preference-Applied: omit-values=nulls`; properties with instance annotations MUST still be included; modified null/default properties MUST be included in delta payloads; POST responses MUST include non-default properties, PUT/PATCH responses MUST include changed properties.

Design:
- `server-api` `Preferences`: typed accessor for `omit-values` (enum NULLS/DEFAULTS/none).
- JSON entity/entity-collection serializer: new serializer option; when active (nulls), omit declared properties whose value is null — EXCEPT properties carrying instance annotations (spec MUST) — and the framework/tecsvc sets `Preference-Applied: omit-values=nulls`.
- Application scope (spec-safe simplification): the preference is applied to **read (GET) responses only**. Write responses (POST/PUT/PATCH) and delta payloads never omit — this trivially satisfies the three MUST-include clauses without tracking changed-property sets. Applying on writes is a MAY we decline; not applying means no Preference-Applied is emitted there (conformant per JSON §24 item 22).
- `omit-values=defaults` is **not applied** (MAY declined): no omission, no Preference-Applied for defaults. Instance-default tracking doesn't exist in the data layer; declining is conformant.
- Client: `omit-values` preference builder on the client preferences API; no deserializer change (absent declared property already reads as null).

Decisions on spec-silent points:
- Dynamic (open-type) properties: **explicit nulls are never omitted.** Spec is silent; in this codebase an absent dynamic property means "does not exist" (DELETE-removes semantics from the OpenType feature), so omitting a null dynamic value would be lossy and unreconstructable. Documented in the guide.

Tests: preference parsing units; serializer units (null declared omitted + header applied, non-null kept, instance-annotated null kept, dynamic null kept, defaults → no-op + no Preference-Applied); fit round trip (GET with preference → body lacks nulls + Preference-Applied present; same GET without preference → nulls present, pin); write-response pin (PUT with preference → no omission, no Preference-Applied).

## Feature 3: `/$query` — query options in the request body (OLINGO-1643) — Wave 1

Normative: [OData-URL] §4.17 (quoted in full in the citations file). Requests to paths ending in `/$query` MUST use POST; body MUST be `text/plain`, percent-encoded exactly like a URL query string; **URL query options and body query options are processed together (merged)** — the spec states this unconditionally. Scoped to GET-style read requests. No status codes prescribed.

Design:
- `server-core`, early in request processing (before URI parsing): POST + path ending `/$query` + `Content-Type: text/plain` → strip the segment, read the body, **concatenate URL query string and body options** (both feed the same query-option parser, preserving existing duplicate-option error behavior), dispatch as the equivalent GET read.
- Client: request-builder toggle on retrieve requests to send in `/$query` form (POST, body carries the options).

Decisions on spec-silent points (status codes ours, consistent with codebase conventions):
- Non-POST verb on a `/$query` path → 405 Method Not Allowed.
- Wrong/missing Content-Type → 415 Unsupported Media Type.
- Malformed body (bad percent-encoding, literal whitespace) → 400, via the existing query-option parser's errors.
- Same option in both URL and body → the existing duplicate-query-option error (400). "Processed together" mandates merging the sets, not resolving duplicates; we keep the parser's standing rule.
- `/$query` applies to resource read paths, including `$metadata` (a GET-style request — `$schemaversion` in a `/$query` body must work once Wave 2 lands). It does not apply to `$batch`.

Tests: fit equivalence (POST `ESAllPrim/$query` body `$filter=…&$select=…` ≡ the GET form, byte-identical payload), URL+body merge test, duplicate-option 400, 405/415/400 pins, client round trip via the new toggle.

## Feature 4: `$schemaversion` (OLINGO-1326) — Wave 2

Normative: [OData-Protocol] §11.2.12 (quoted in full in the citations file); conformance §13.2.1 Minimal item 5 (conditional MUST: services emitting `Core.SchemaVersion` in `$metadata` MUST reject incompatible `$schemaversion`). Value MUST be a version previously returned via `Core.SchemaVersion`, or `*` (current). Specified-but-nonexistent version → **404 Not Found** (MUST), response body SHOULD explain. Batch: per-request option applies to that request; requests without it inherit the batch's version.

Design:
- URI parser: accept `$schemaversion` on any request (system query option, [OData-ABNF] syntax); expose on `UriInfo`.
- `server-api`/`server-core`: `ServiceMetadata` (or a small extension interface) exposes the service's schema version; the handler validates a specified `$schemaversion` against it before dispatch — mismatch → 404 with an error body; `*` and exact match pass. No version source configured → option accepted and ignored except syntax validation. Basis: §13.2.1 Minimal item 5 binds only services that return `Core.SchemaVersion` in `$metadata`; reading §11.2.12's 404 clause as forcing unversioned services to reject would contradict that conditionality and break existing consumers, so ignoring is the conformant-and-compatible reading (decision recorded).
- tecsvc: emits `Core.SchemaVersion` in `$metadata` and enforces the option (making the conditional Minimal-level MUST binding — and tested).
- Batch: the batch handler passes the outer request's `$schemaversion` down as the default for parts that don't specify their own.
- Client: typed `$schemaversion` method on the URI builder.

Tests: parser units; handler validation (match/`*`/mismatch-404-with-body/absent); `$metadata?$schemaversion=*` returns current with the annotation; batch inheritance fit test; closed pin (services without a version source behave as today).

Implementation notes (Wave 2, as built — deviations from this section's letter):
- `getSchemaVersionOption()` is declared once on `UriInfo` itself, not replicated across the kind-specific sub-interfaces the way `getFormatOption()` is (§11.2.12 allows the option on any request).
- The version is carried by `ServiceMetadata` via an additive concrete `OData.createServiceMetadata(provider, references, eTagSupport, String schemaVersion)` overload (default body throws `UnsupportedOperationException`; `ODataImpl` overrides it) — no extension interface was needed.
- `$schemaversion` also had to be exempted from `UriValidator.validateNonReadQueryOptions`, which otherwise rejects every system query option on POST/PUT/PATCH/DELETE — without the exemption `POST $batch?$schemaversion=…` 400s and batch inheritance is unreachable. The exemption is narrow (other options on writes still rejected).
- Batch: an unknown version on the OUTER `$batch` URL 404s the whole envelope (the envelope is itself a request, version-checked before dispatch) rather than producing per-part 404s inside a multipart body; a part's own unknown version yields a 404 for that part inside a 200 batch.

## Feature 5: Optional function parameters (OLINGO-1634) — Wave 2

Normative: [OData-Protocol] §11.5.4.1.1, §11.5.4.2, §11.5.5.1 (quoted in full in the citations file); Core vocabulary term `Core.OptionalParameter` / `Core.OptionalParameterType.DefaultValue`. Function inline parameters annotated `Core.OptionalParameter` MAY be omitted; with a `DefaultValue` the omitted parameter has that value, without one the service chooses. Overload resolution: select on exact match, else on a specified-set matching a subset that includes all non-optional parameters of exactly one overload; optional parameters MUST come after non-optional ones (CSDL rule); ambiguity → service MAY 400. **Actions have no optional-parameter overload mechanism** — only body-omission rules (§11.5.5.1: omitted nullable ⇒ null; omitted annotated ⇒ DefaultValue or service's choice).

Design:
- EDM layer (`commons`): `EdmParameter` exposes `isOptional()` and `getOptionalDefaultValue()` (read from the `Core.OptionalParameter` annotation; the vocabularies module ships Core). Provider-side: annotation attached via standard CSDL annotations — no new CSDL classes.
- URI parser (`server-core`) function overload resolution: implement the two-step rule verbatim (exact match, else all-non-optional-covered subset of exactly one overload). Ambiguous subset match → 400 (we adopt the MAY). Omitting a required parameter keeps failing exactly as today (pin).
- Processor access: an omitted optional parameter with a DefaultValue surfaces to processors with that value (materialized into the parameter list the same way the URI parser surfaces provided parameters); without a DefaultValue it is simply absent (service-free interpretation is the processor's).
- Actions: verify the JSON action-parameter deserializer's current omitted-nullable ⇒ null behavior against §11.5.5.1 and pin it; add DefaultValue interpretation for annotated action parameters if absent (small, same annotation plumbing).
- Client: no new API (the URI builder already emits caller-chosen parameter subsets) — client half is end-to-end fit proof.

Tests: EDM units (annotation surfacing, optional-after-required CSDL-rule validation); overload-resolution units (exact, subset, ambiguous-400, required-missing pin); tecsvc function with required+optional(+default) params; fit invocations with/without the optional parameter (default observed); action-body omission pins.

Implementation notes (Wave 2, as built — deviations from this section's letter):
- **Optional-after-required CSDL ordering is NOT enforced** by the overload matcher; it is a provider-authoring rule. The matcher is order-independent (a sanity test pins that a compliant model resolves). The conformance summary's "CSDL ordering" MUST claim is corrected accordingly.
- **DefaultValue is read as a URI literal** (Core vocabulary: "same rules as the `cast` function in URLs") — `'-default'`, `Ns.Enum'Member'`, `42` — and run through `fromUriLiteral` before use.
- **server-core does not inject defaults for URL-invoked functions.** It exposes `EdmParameter.isOptional()/getOptionalDefaultValue()` and the *service* materializes defaults; tecsvc's `DataProvider.getFunctionParameters` shows the pattern using the fixed-format deserializer. The JSON action-body deserializer does inject, but only for OMITTED parameters — an explicit JSON `null` stays null; an omitted optional without a default is left absent. Only primitive/enum non-collection defaults are applied.
- Ambiguity is signalled by a new additive `EdmAmbiguousOverloadException` (commons-api, extends `EdmException`) so that genuine model errors keep their 500 treatment; the URI parser maps only that type to 400 `FUNCTION_AMBIGUOUS`.

## Feature 6: Key-as-segment convention (OLINGO-1084) — Wave 3, first

Normative: [OData-URL] §4.3.6 (quoted in full in the citations file); conformance §13.2.1 item 9.l (MAY support; once supported, MUST also support canonical parenthesized URLs **or** include entity URLs in payloads). Once a service supports the convention it MUST implement the 4-step precedence for a segment following an entity collection: (1) `$`-prefixed OData segment → as such; (2) qualified bound function/action/type name → as such; (3) unqualified bound function/action/type name in a default namespace → as such; (4) else key value. Multi-part keys MUST be addressed as one segment per key value in metadata key order. Referential-constraint-covered key properties MUST be omitted from key-as-segment URLs of related entities. Forward slashes in key segments MUST be percent-encoded; single quotes are literal. **Normatively excluded from alternate keys.**

Design:
- **Opt-in server configuration flag** (off by default; spec-legal since support is MAY). With the flag off, every existing URI parses byte-identically — pinned.
- `server-core` `ResourcePathParser`: when enabled, apply the 4-step precedence verbatim where a segment follows an entity collection (entity set or collection-valued navigation). Multi-part keys consume consecutive segments in declared key order; referential-constraint shortening honored for navigation-related entities. Key values are unquoted; type conversion follows the key property's EDM type; percent-decoding per standard URL handling.
- Parenthesized form remains always-on (conformance 9.l.a satisfied structurally).
- Alternate keys are never resolvable via segments (spec exclusion; step 4 means canonical key only).
- tecsvc: flag enabled in a dedicated test configuration (fit exercises both modes).
- Client: URI-builder key-as-segment mode (verify whether the fork retains upstream's `Configuration.setKeyAsSegment`; reuse if present, else add) producing segment-form entity addressing, incl. multi-part.

Decisions on spec-silent points:
- Ambiguous/malformed segment that survives the precedence rules but fails key-type conversion → the parser's existing invalid-key-value error (400-class), same as the parenthesized form.

Tests: parser units for each precedence step (a `$`-segment, a bound operation name, a type-cast name, a default-namespace unqualified name, then a key), single-part and multi-part segment keys, percent-encoded slash and literal quote, referential-constraint omission, flag-off pins (all of the above 404/behave exactly as today), fit round trips both modes, client URI-builder units + fit.

## Feature 7: Alternate keys (OLINGO-1570) — Wave 3, second

Normative: [OData-URL] §4.3.5 (quoted in full in the citations file); Core vocabulary `Core.AlternateKeys` / `AlternateKey` / `PropertyRef{Name: Edm.PropertyPath, Alias}`. Addressing uses the same parenthesized convention as the canonical key; **single-part alternate keys MUST name the key property** (`Employees(SSN='123-45-6789')` — never the bare short form). Multi-part follows the same convention by cross-reference (comma-separated name=value; no standalone normative sentence — noted). Not a conformance item. Spec is SILENT on writes via alternate-key URLs and on an explicit canonical-URL/primary-key statement (inferable only).

Design:
- EDM layer: expose alternate key groups on `EdmEntityType`/entity set (read from `Core.AlternateKeys` on the type/set; `Alias` honored as the URL-facing name where present).
- URI parser: a parenthesized name=value predicate set that does not match the declared key is resolved against the alternate key groups; a complete match of exactly one group parses (predicates flagged as alternate-key on the URI resource), anything else fails exactly as today (pinned). The bare positional short form stays primary-key-only (spec MUST).
- **Scope bound (recorded):** alternate keys whose `PropertyRef.Name` is a top-level primitive property (with optional Alias) are supported; nested complex-property paths are out of scope for this milestone (parse-rejected as non-matching, documented). The vocabulary allows recursive paths; the URL spec has no normative treatment of them — deferring is safe and honest.
- tecsvc: an entity set annotated with a single-part and a multi-part alternate key; `DataProvider` resolves by alternate key.
- Writes (spec-silent decision): alternate-key addressing resolves to the same entity uniformly, so **all CRUD verbs work through alternate-key URLs in tecsvc**; canonical/context/edit URLs in responses are always built from the primary key (per §4.3.1's definition of canonical URL). Both choices documented.
- Client: no new API (URI builder already emits named key predicates); end-to-end fit proof.
- Interaction pin: with key-as-segment enabled, segments still never resolve alternate keys (Feature 6's exclusion, tested here too).

Tests: EDM units (annotation surfacing, Alias); parser units (single-part named alternate key, multi-part, bare short form stays primary, unknown name set still fails, nested-path group rejected); tecsvc/fit CRUD round trips via alternate key incl. canonical-URL-uses-primary-key assertion; key-as-segment exclusion pin.

---

## Conformance summary

- MUST-level clauses implemented and pinned: matchesPattern signature/dialect reference (§5.1.1.7.1); omit-values Preference-Applied + instance-annotation inclusion + write/delta inclusion (§8.2.8.6, JSON §24 item 22); /$query POST-only + text/plain + merge (§4.17); $schemaversion 404 + versioned processing + batch inheritance (§11.2.12, §13.2.1 item 5); optional-parameter overload rules (§11.5.4.2; CSDL optional-after-required ordering is a provider-authoring rule, not matcher-enforced — see Feature 5 notes); key-as-segment precedence + multi-part + referential-constraint omission + parenthesized coexistence (§4.3.6, §13.2.1 item 9.l.a); alternate-key single-part naming MUST (§4.3.5).
- MAY/SHOULD choices: apply omit-values=nulls on reads only; decline omit-values=defaults; 400 on ambiguous function overloads; key-as-segment opt-in flag.
- Spec-silent decisions (all documented in the guide): matchesPattern invalid-regex 400; /$query status codes (405/415/400) and duplicate-option handling; omit-values dynamic-null retention; alternate-key writes allowed + primary-key canonical URLs; alternate-key nested-path scope bound.
- Recorded deviations: java.util.regex instead of an ECMAScript regex engine (Feature 1); `$schemaversion` exempted from the non-read query-option rejection and unknown outer-`$batch` version 404s the envelope (Feature 4); optional-after-required CSDL ordering not matcher-enforced, and DefaultValue injection for URL-invoked functions left to the service (Feature 5); alternate-key nested `PropertyRef` paths deferred (Feature 7).

## Testing & rollout

Every feature: unit tests at each touched layer, tecsvc wiring, fit round trips through the real client, negative/closed pins, and the flag-off/preference-absent identity pins. Each wave ends with the full plain 38-module gate before merge. Docs (site guides + this spec's deviation list) updated in each wave's final task.
