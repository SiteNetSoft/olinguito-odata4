# Tier 7 — OData 4.01 Minimal Conformance

**Date:** 2026-08-21. **Status:** design, awaiting review. **Baseline:** master `5bd488323`.
**Citations:** `docs/superpowers/specs/2026-08-21-tier7-citations.md` — every normative quote this design
relies on, with section numbers. **Gap source:** `docs/superpowers/specs/2026-08-20-conformance-audit.md`
([OData-Protocol] §13 audit, six auditors, evidence-backed).

## Goal

Make Olinguito reach the **OData 4.01 Minimal Conformance Level** ([OData-Protocol] §13.2.1) and say so in
`$metadata` via `Core.ODataVersions`. Tier 6 Wave 3 closed item 3 (`AsyncResult`); this tier closes
everything else the audit found open at that level, at MUST *and* SHOULD, plus the one 4.0 Minimal gap the
level inherits through its own item 1.

This is a conformance tier, not a feature tier. Its output is a claim that can be evidenced clause by clause.

## Non-goals

- **The §13.2.1 MAY items** — filtering on annotation values (14), `$compute` (15), `$search` on every
  collection (16), defaulting to 4.01 payloads without an `OData-MaxVersion` header (17). MAYs do not affect
  the claim.
- **Higher levels.** 4.0 Intermediate's derived-type-cast gap, 4.01 Intermediate's nested `$select` options,
  the 4.0 Advanced evaluator tail (`$levels`, `$search`-in-`$expand`, cast-in-expand, `$crossjoin`, `all`),
  and 4.01 Advanced's `$compute` are later tiers. See `memory/odata-conformance-roadmap.md`.
- **Two stray MUST-level bugs** the audit caught in passing — the path-key `.../NavPropMany(5)/$ref` DELETE
  form, and the `NavPropSingle eq null` NPE at `ExpressionVisitorImpl:306`. User ruling 2026-08-21: deferred,
  "we will add them later".
- **Item 9g, implicit parameter aliasing.** Deferred as undefined — see Deviations D4.

## The closing set

| § | Level | State today | Tier 7 |
|---|---|---|---|
| 6 | MUST | `Parser.java:334` routes only `$`-prefixed names | Accept the `$`-less spelling |
| 7 | MUST | `SystemQueryOptionKind.get:117` exact match; `UriTokenizer.nextConstant:740` case-sensitive | Case-insensitive option, operator, function, lambda names |
| 9a | MUST | `ParserHelper.nextPrimitiveTypeValue:706` demands the exact token kind | Quoted string reinterpreted against the expected type |
| 9b | MUST (partial) | `duration'…'` / `Ns.Type'…'` prefix mandatory (`UriTokenizer:1098,1162`) | Also accept the unprefixed quoted forms |
| 9c | MUST | `ResourcePathParser:557` always calls `functionCall` | Parameter-less calls without `()`, imports **and** bound |
| 4 | MUST (partial) | only `track-changes` accepts both spellings (`PreferencesImpl:83`) | Both spellings for all six preferences |
| 9j | SHOULD | only `div` | `divby`, with its own semantics |
| 9k | SHOULD | `MethodCallOperator:151` clamps negative start to 0 | Count from the end |
| 12 | SHOULD | no hits | Emit `Core.ODataVersions` = `4.01` |
| 13 | SHOULD | no hits | `Capabilities.*` annotations on tecsvc's model |
| 4.0 §26 | MUST | `handleReferenceDispatching` never checks preconditions | `If-Match` enforced on `$ref` writes |
| — | new | duplicate detection is raw-name based | Reject duplicates on the **normalized** name |

That last row is a requirement the audit did not surface. [OData-URL] §5.1: *"The same system query option,
irrespective of casing or whether or not it is prefixed with a $, MUST NOT be specified more than once for
any resource."* Accepting new spellings without normalizing duplicate detection would trade one
non-conformance for another.

## Architecture

The work divides on a clean seam that already exists in the codebase: **parsing lives in `server-core`
(and its enums in `server-api`); evaluation lives in `server-tecsvc`.** Items 6, 7, 9a, 9b, 9c and the
duplicate rule are purely `server-core`. Items 9j and 9k are split — the operator/function must be *parsed*
in `server-core` and *evaluated* in `server-tecsvc` (`BinaryOperator`, `MethodCallOperator`). Items 4, 12, 13
and the `$ref` ETag fix touch neither parser: they are `commons-api`/`server-core` header handling, the
metadata serializers, tecsvc's model, and `ODataDispatcher` respectively.

Three principles govern every change:

**Additive, never substitutive.** Every strict 4.0 spelling keeps working byte-identically. The tier is
always "also accept X", never "accept X instead of Y". This is what the closed-behavior pins in the testing
section exist to prove.

**Unconditional, not version-gated.** The relaxations apply to every request regardless of
`OData-MaxVersion`. §13.2.1 item 9 says as much in its own words — *"MUST support both 4.0 and 4.01 syntax in
URLs for supported functionality regardless of requested OData-MaxVersion"* — so gating on the header would
itself be non-conformant, quite apart from the plumbing it would need through `Parser`/`UriTokenizer`/
`ParserHelper`.

**One mechanism where the spec describes one mechanism.** 9a and 9b look like separate items but are the same
operation: the unprefixed enum and duration forms are still *quoted* (`ColorEnumValue eq 'Yellow'`,
`DurationValue eq 'P12DT23H59M59.999999999999S'` — [OData-URL] §5.1.1.14.1), so both reduce to "a
`StringValue` token appeared where a typed literal was expected; reinterpret it against the expected type."
They share one code path.

### 1. Option-name normalization (items 6 + 7, and the duplicate rule)

One change, not three. `SystemQueryOptionKind.get(String)` becomes the single normalization point: it lowers
the name, tolerates a missing `$`, and returns the kind or `null`. `Parser.parseOption` then routes on
whatever `get()` resolves rather than on `optionName.startsWith(DOLLAR)`.

Precedence is settled by [OData-Protocol] §6.1 — custom query options *"MUST NOT conflict with any
OData-defined system query options"* — so there is no collision to arbitrate: a resolvable name is a system
option, full stop. Unresolvable names keep today's behavior, which already splits correctly: `$`-prefixed and
unknown is `UNKNOWN_SYSTEM_QUERY_OPTION`; not `$`-prefixed and unknown is a `CustomQueryOption`, which is
§6.1's extensibility rule.

Duplicate detection moves onto the normalized name so `?$filter=…&FILTER=…` is rejected.

Operator, function and lambda names are four one-line changes in `UriTokenizer` — `nextBinaryOperator:897`,
`nextUnaryOperator:905`, `nextMethod:913`, `nextSuffix:921` — each switching from `nextConstant` to
`nextConstantIgnoreCase`. Both helpers already exist; the ignore-case one is used today for `true`/`false`
and the geo keywords.

**`nextConstant` itself must stay case-sensitive.** Item 7 enumerates *option, operator and function* names.
Literal values are not in that list and the ABNF fixes their case (`nullValue = "null"`, `nanValue = "NaN"`,
`INF`), and identifiers must stay exact because item 8 requires returning them in their declared case.
Changing the shared helper instead of its four operator-facing callers would quietly make `NULL` and `Nan`
valid literals — a conformance regression disguised as a conformance fix.

### 2. Quoted-string reinterpretation (items 9a + 9b)

`ParserHelper.nextPrimitiveTypeValue` keeps its existing strict ladder unchanged and gains a **fallback**
reached only when every strict alternative has failed: if the next token is a `StringValue`, hand its
unquoted content to the expected type's `valueOfString`. Success accepts; failure falls through to today's
invalid-key-value error.

This is exactly the spec's own rule ([OData-URL] §5.1.1.10.1): *"Edm.String, or a type definition based on
Edm.String, can be cast to a primitive type if the string contains a literal representation for the target
type."* `Employees('1')` resolves against an Int32 key; `Employees('abc')` still fails, one layer deeper than
before. Because the fallback runs last, no currently-accepted URL changes meaning.

The same fallback serves 9b: an unprefixed `'Yellow'` against an expected enum type, or an unprefixed
`'P12DT23H59M59.999999999999S'` against `Edm.Duration`, is a `StringValue` reinterpreted against the expected
type. The prefixed forms keep their dedicated token kinds and are still tried first.

### 3. Parameter-less calls without parentheses (item 9c)

`ResourcePathParser` looks ahead for `(` before committing to `functionCall`. Absent, it resolves the
zero-parameter overload; if the function has required parameters, the existing missing-parameter error
stands. [OData-ABNF] defines `functionImportCallNoParens` **and** `boundFunctionCallNoParens`, both reachable
from the resource-path production, so both get the treatment — implementing only the import form would
satisfy the letter of item 9c while leaving the grammar half-supported.

### 4. Preference name variants (item 4)

The direction of travel is the reverse of how the gap reads: the **bare** name is the 4.01 name and
`odata.`-prefixed is the retained 4.0 spelling, so Olinguito currently implements the legacy name and is
missing the modern one. `PreferencesImpl` gets one helper that resolves a preference by trying the bare name
then the prefixed one, and every accessor goes through it. The one-off `TRACK_CHANGES_PREF` special case
disappears into the general rule. Where the spec states a tie-break ([OData-Protocol] §8.2.8.2, §8.2.8.4) the
bare name wins; the helper applies that uniformly (Deviations D2).

### 5. `divby` and negative substring (items 9j, 9k)

`divby` is **not** an alias for `div`. [OData-URL] §5.1.1.2.5: it *"promotes both operands to decimal before
computing the result, may yield a fractional result, and does not fail for divby zero, returning -INF, INF,
or NaN depending on the sign of the left operand."* So it needs its own `BinaryOperatorKind` constant, its
own tokenizer alternative (ordered before `div` so the longer keyword wins), and its own evaluation in
tecsvc's `BinaryOperator` — decimal promotion and signed-infinity behavior included, with `0 divby 0` → NaN.

Negative `substring` indexes replace the clamp at `MethodCallOperator:151` with the spec's rule: *"A negative
start index N […] returns a string/collection starting N characters/items before the end."* Today's clamp is
a third behavior, neither 4.0's nor 4.01's, so this corrects rather than extends (Deviations D3).

### 6. Declaring conformance (item 12) and reporting capabilities (item 13)

`Core.ODataVersions` carrying `4.01` is emitted in `$metadata` by both the XML and JSON metadata serializers,
kept consistent between them. Per [OData-Protocol] §13.2 this is *how a service reports 4.01 conformance*, so
it lands last, after the clauses it asserts are actually true.

Item 13 annotates **tecsvc's model** with `Capabilities.*` terms. Stated plainly: this makes our reference
service report its capabilities; it is not a library capability, and no downstream service inherits it — each
annotates its own model. The spec names no required terms (Gaps G4), so the tier annotates the ones tecsvc
can answer honestly: supported conformance level, and the batch/filter/count/sort restrictions its processors
actually impose.

### 7. `If-Match` on `$ref` writes (4.0 Minimal item 26)

`ODataDispatcher.handleReferenceDispatching` (`:309-349`) calls `validatePreconditions` on the `$ref`
create/update/delete branches, the way the entity, media and primitive-value paths already do at `:653/658/669`.
The precondition machinery exists and is pinned by `ODataHandlerImplTest:956`; only these branches skip it.

## Waves

**Wave 1 — the URI parser (`server-core`, `server-api`).** Option-name normalization (6 + 7 + the duplicate
rule), operator/function/lambda case-insensitivity, the quoted-string fallback (9a + 9b), no-parens calls
(9c), and the *parsing* half of `divby` (9j) — item 9k has no parsing half, `substring(Name,-3)` already parses, so it is entirely Wave 2. Eight items, one subsystem,
shared machinery.

**Wave 2 — everything else**, in this order: preference variants (4), the *evaluation* halves of 9j/9k in
tecsvc, `Capabilities.*` on tecsvc's model (13), the `$ref` `If-Match` fix, then the proof task, and only
then `Core.ODataVersions` (12). Independent of Wave 1 in principle; sequenced after it so `divby` and
`substring` land parse-then-evaluate in that order.

**The proof task is the second-to-last step:** re-run the §13.2.1 slice of the conformance audit against the
merged tree and rewrite its verdict table. The claim has to be evidenced clause by clause, not asserted — and
the audit's own recurring finding was *parsed then silently dropped at evaluation*, a failure mode that only
an evaluation-level re-check catches. Item 12 is deliberately the **last** task in the tier: emitting
`Core.ODataVersions: 4.01` is the service asserting the claim in its own payload, so it may only be written
once the verdict table backing it is green.

## Testing

Following Tiers 5 and 6:

- **Unit** — `UriTokenizerTest`/`UriParserTest` in `server-core` for every new spelling, and `PreferencesTest`
  for the six preference pairs including both-sent tie-breaks.
- **Reference service + integration** — tecsvc endpoints exercised from `fit` through the real client, so each
  spelling is proven end to end rather than at the parser boundary.
- **Closed-behavior pins** — the load-bearing ones. Every strict 4.0 form (`$filter`, `duration'…'`,
  `Ns.Type'Yellow'`, `FnImport()`, `div`) keeps a test asserting it still parses and evaluates identically.
  A conformance tier that quietly breaks 4.0 clients has failed.
- **Negative pins** — `NULL`/`Nan` still rejected as literals; `Employees('abc')` still 400; `?$filter=…&FILTER=…`
  rejected as a duplicate; a bare function import that has required parameters still errors.
- **Gate** — plain `mvn -B install --fail-at-end` (never `-Pbuild.fast`, which skips checkstyle/RAT/PMD) across
  all 38 modules before either wave is declared done.

## Error handling

Everything stays inside the existing taxonomy; no new exception families.

| Situation | Result |
|---|---|
| `$`-prefixed name that resolves to nothing | `UNKNOWN_SYSTEM_QUERY_OPTION` (unchanged) |
| `$`-less name that resolves to nothing | `CustomQueryOption` ([OData-Protocol] §6.1) |
| Same option twice in any spelling mix | Existing duplicate-option syntax error, on the normalized name |
| Quoted string that is not a literal for the expected type | Existing invalid-key-value / type error, 400 |
| Bare function call whose function has required parameters | Existing missing-parameter error |
| `x divby 0` | `-INF`/`INF`/`NaN` by the sign of the left operand — **not** an error ([OData-URL] §5.1.1.2.5) |
| `$ref` write failing a precondition | Same 412/428 the entity path already returns |

## Deviations and rulings

**D1 — item 6 changes behavior for a service with a conflicting custom option.** Once `filter=…` routes to
the system option, a deployment that used `filter` as a *custom* query option sees different behavior. §6.1
forbids such an option outright, so our ruling is that such a service was already non-conformant; the spec is
silent on migrating one (Gaps G1). Recorded here because it is our call, not a quotation.

**D2 — the preference tie-break is generalized.** Only `callback` (§8.2.8.2) and `include-annotations`
(§8.2.8.4) state that the bare name wins when both spellings are sent. Applying that uniformly to all six is
an inference, and a SHOULD even where stated (Gaps G2).

**D3 — item 9k is a behavior change, not an addition.** Negative `substring` indexes are clamped to 0 today.
Any existing test asserting the clamp is pinning the old semantics; such a fixture will be corrected and the
correction called out in the commit, not quietly edited.

**D4 — item 9g is deferred as undefined.** "Implicit aliasing of parameters" appears exactly once across
[OData-Protocol], [OData-URL] and [OData-ABNF] — in the conformance list itself. §11.2.5/§5.3 define only
*explicit* aliases, and the ABNF has no bare-name production
(`functionParameter = parameterName EQ ( parameterAlias / primitiveLiteral )`). There is no normative grammar
to implement against, so Tier 7 records it as a known open SHOULD rather than inventing semantics. It does
not affect the Minimal claim (Gaps G3).

**D5 — item 13 is scoped to tecsvc.** A service-modelling change, not a library capability (Gaps G4).

**D6 — item 9c is implemented wider than §13.2.1 asks**, covering bound functions as well as function
imports, because [OData-ABNF] defines both no-parens productions.

## Risks

- **The blast radius is the URI parser**, which every request crosses. Mitigated by the additive principle,
  the fallback-last ordering in 9a/9b, and the closed-behavior pins.
- **`nextConstant` is shared.** The obvious "make it ignore case" edit is wrong and would loosen literals and
  identifiers. Called out in the design so a plan task cannot take the shortcut.
- **`divby` must be ordered before `div`** in the tokenizer's alternatives, or `divby` lexes as `div`
  followed by a stray `by`.
- **Item 12 asserts the tier is finished.** Emitting `4.01` while any clause is still open is a false claim in
  the payload; it lands last, after the proof task's verdict table is green.
