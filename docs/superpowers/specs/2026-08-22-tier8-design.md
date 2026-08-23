# Tier 8 — OData 4.01 Intermediate Conformance

**Date:** 2026-08-22. **Status:** design, awaiting review. **Baseline:** master `3569bd76c`.
**Citations:** `docs/superpowers/specs/2026-08-22-tier8-citations.md`.
**Gap source:** `docs/superpowers/specs/2026-08-20-conformance-audit.md` (§13 audit; its §13.2.1 slice
was re-walked by Tier 7, the other slices still read as of `8df82f77a`).

## Goal

Make Olinguito reach the **OData 4.01 Intermediate Conformance Level** ([OData-Protocol] §13.2.2),
the next declarable level above the 4.01 Minimal that Tier 7 closed.

Like Tier 7, this is a conformance tier: its output is a claim evidenced clause by clause, ending
with the `Core.ODataVersions` story still true and the audit's verdict table rewritten to match.

## The closing set

§13.2.2 items 1 and 2 are roll-ups. Item 1 (4.01 Minimal) is done. Item 2 pulls in §13.1.2, whose
only open gap is its item 4. Items 4, 6 and 7 already pass. Items 10 and 11 are MAY.

| Clause | Level | State today | Tier 8 |
|---|---|---|---|
| §13.1.2 item 4 | MUST | Cast honored on path segments; **HTTP 500** when a cast appears in a filter alongside any other clause; 501 on bound functions | Evaluate casts per instance |
| §13.2.2 item 3 | MUST | Parses, then NPEs in the evaluator | `NavPropSingle eq null` answers true/false |
| §13.2.2 item 5 | MUST | `SelectParser` has no parenthesized-options grammar | `$select` nested within `$select` |
| §13.2.2 item 9 | SHOULD | same | `$filter`/`$orderby`/`$count`/`$search`/`$skip`/`$top` nested in `$select` |
| §13.2.2 item 8 | SHOULD | Exists only as an `$apply` transformation, never evaluated | Standalone `$compute` |

**Out of scope:** items 10 and 11 (MAY) — nested parameter alias assignments, and the `/$filter` path
segment. The 4.0 Advanced tail (`any`/`all`, cast-in-expand, `$levels`, `$search`-in-`$expand`,
`$crossjoin`) and 4.01 Advanced's `$compute`-as-MUST remain later tiers; note that Tier 8's `$compute`
work discharges most of the latter in advance.

## Non-goals

- **The path-key `$ref` DELETE bug** (4.01 Minimal item 19) stays deferred; it is unrelated to this
  level. The *other* bug deferred in Tier 7 — the nav-eq-null NPE — is item 3 here and is in scope.
- **Making `$apply` work.** `$compute` reuses `$apply`'s parse layer, but `$apply` itself keeps
  answering 501. Nothing here implies aggregation support.

## Architecture

The tier splits on a clean seam: **Wave 1 evaluates constructs that already parse; Wave 2 adds new
grammar and evaluates it.** The two share no code, so the order is a preference rather than a
dependency — Wave 1 goes first because it is smaller and because one of its defects is a 500 today.

Three principles carry over from Tier 7, and one is new:

**Additive, never substitutive.** Wave 1 replaces a special case with the general rule it was
approximating, and the answers it already gives must not change (see Deviations D1); everything else
only adds accepted syntax or makes a 500 answer properly.

**Evidence at the level the clause operates.** The audit's recurring finding is *parsed then dropped
at evaluation*, and Tier 7 found two gaps that only real HTTP requests exposed. Every clause here
gets a test that exercises it end to end through tecsvc, not merely at the parser boundary.

**Reuse the parse layer that exists.** `$compute` is not a greenfield feature: `ComputeImpl`,
`ComputeExpressionImpl`, `DynamicProperty` and `DynamicStructuredType` already exist for `$apply`, and
the OpenType work already serializes dynamic properties. The new code is the evaluator and the option
plumbing.

**New: the spec's option split is the parser's split.** [OData-Protocol] §11.2.5.1 allows different
nested options on complex versus collection-valued selected properties, and none on a plain primitive.
The parser enforces that distinction rather than accepting one union everywhere.

### Wave 1 — derived-type casts and single-valued navigation

**1. A cast in a filter answers HTTP 500 as soon as any other clause joins it.**
`TechnicalProcessor.readEntityCollection` (`:304-317`) pattern-matches a top-level `Binary` whose left
operand is a `Member` carrying a start type filter, and narrows the collection to the derived entity
set. Where it fires the answer is right — the caller still applies `FilterHandler`, so
`?$filter=Ns.ETBase/Additional eq 'nope'` correctly returns nothing. But it only recognises that one
expression shape. Add a second clause and the top-level operand is an `and`, the special case does not
fire, the collection stays the base entity set, and evaluating the cast member against an entity that
is not of the cast type dereferences a null property:

```
ESTwoPrim?$filter=olingo.odata.test1.ETBase/AdditionalPropertyString_5 eq 'TEST A 0815'
          and PropertyInt16 eq 222
  -> 500 Cannot invoke "Property.getValue()" because "currentProperty" is null
```

(Verified against the running service, 2026-08-22. **The 2026-08-20 audit described this as the cast
set being returned with the rest of the filter ignored; that reading is wrong** — the filter is
applied. The defect is the 500, and the special case is a narrowing hack that hides it for one shape.)

The fix removes the special case and makes the evaluator handle the cast per instance, so the base
collection can be filtered directly like every other collection.

**2. Casts inside member expressions are then honored per instance.**
With the special case gone, `ExpressionVisitorImpl.visitMember` must handle a
`UriResourceStartingTypeFilter` segment: if the entity is not of the cast type the member evaluates to
null, which makes the surrounding comparison false. This is exactly [OData-URL] §5.1.1.10 — *"If the
type cast is part of a Boolean expression, the type cast will evaluate to null"* — and it is the
general rule the special case was approximating for one expression shape.

**3. Bound-function casts answer 501.**
`TechnicalProcessor.blockTypeFilters` rejects any `UriResourceFunction` carrying a type filter. Lifting
it means treating the filtered type as the function's effective return type; `getEffectiveType`, added
for OLINGO-1184, already has that shape and is the model to follow.

**4. `NavPropSingle eq null` NPEs.**
`ExpressionVisitorImpl.visitMember`'s `UriResourceNavigation` branch loops
`for (i = 1; i < uriResourceParts.size(); i++)`, which never executes for a bare navigation property,
leaving `currentEdmProperty` null before `currentEdmProperty.getType()`. The branch returns an operand
for the navigation link itself — the related entity, or null when unlinked — so `eq null` yields a
boolean. The evaluation rule is our reading; the spec names the capability without defining it (G1).

### Wave 2 — nested `$select` options and `$compute`

**Nested options** land in three layers.

*API.* `SelectItem` (server-api) gains the sub-option accessors `ExpandItem` already carries:
`getFilterOption`, `getSearchOption`, `getOrderByOption`, `getSkipOption`, `getTopOption`,
`getCountOption`, `getSelectOption`, `getComputeOption`. Additive, and `SelectItemImpl` is the only
implementation, so no consumer breaks.

*Parser.* `SelectParser` gains a parenthesized-options loop mirroring `ExpandParser`'s dispatch
(`ExpandParser:280-320`) and reusing the same sub-parsers — `FilterParser`, `OrderByParser`,
`SearchParser`, and `SelectParser` recursively. The existing `/`-path nesting is untouched. Which
options are admissible depends on what was selected, per §11.2.5.1 and the ABNF: `$select` and
`$compute` on a complex or navigation path; `$filter`, `$search`, `$count`, `$orderby`, `$skip`,
`$top` on a collection-valued property; none on a plain primitive.

*Evaluation.* `ExpandSystemQueryOptionHandler` already applies precisely this option set to expanded
collections. The selected-property path gets the same treatment for collection-valued properties,
which is where item 9's `$filter`/`$orderby`-on-selected-collections lands.

§11.2.5.1 also carries an acceptance rule, not just an evaluation one: *"A property MUST NOT have
select options specified in more than one place in a request and MUST NOT have both select options and
expand options specified."* Both clear cases are rejected at parse time (D4).

**`$compute`** reuses what exists: a `COMPUTE` constant on `SystemQueryOptionKind`, a `ComputeParser`
built from the `parseComputeTrafo` logic, and `getComputeOption()` on `UriInfo` and on the new
`SelectItem`/existing `ExpandItem` option sets. The new work is evaluation: for each entity, evaluate
each `ComputeExpression` through `ExpressionVisitorImpl` and attach the result as a dynamic property
named by its alias.

Ordering follows from the clause rather than from a rule the spec states outright (G2): because
computed properties are usable *from* `$select`, `$filter` and `$orderby`, `$compute` is evaluated
before all three. Serialization is mostly free — §11.2.5.3 says computed properties *"SHOULD be
included as dynamic properties in the result and MUST be included if $select is specified with the
computed property name, or star (*)"*, and the OpenType tier already serializes dynamic properties and
honors them in `$select`.

## Waves

**Wave 1** — the four Wave 1 defects above, then a gate. Entirely `server-tecsvc` plus whatever the
cast work needs in `server-core`'s expression evaluation.

**Wave 2**, in order: `SelectItem` API → `SelectParser` grammar → nested-option evaluation →
`$compute` parse plumbing → `$compute` evaluation → end-to-end ITs → gate → **re-audit §13.2.2 and
§13.1.2** → record the tier.

As in Tier 7, the re-audit is the second-to-last step and the conformance claim depends on it. Unlike
Tier 7, no new `Core.ODataVersions` value is needed: the annotation already says `4.01`, and
[OData-Protocol] §13.2 has services report the *protocol version*, not the level. The claim therefore
lives in the audit document and in `CLAUDE.md`, not in the payload.

**If `$compute` grows**, it splits into its own wave rather than swelling Wave 2. It is the item most
likely to, being the only one that adds a system query option end to end.

## Testing

- **Unit** — `SelectParserTest` for the new grammar including the per-kind option split and both
  rejection cases; `ExpressionParserTest` for `$compute` expressions; tokenizer coverage as needed.
- **Evaluator** — tecsvc-level tests for cast filtering, nav-eq-null, nested select options, and
  computed properties.
- **Integration** — `fit` ITs through the real client for every clause, extending the
  `Conformance401ITCase` pattern Tier 7 established. This is the layer that caught Tier 7's last two
  bugs.
- **Closed-behavior pins** — `/`-path `$select` nesting, existing `$filter` casts that are already
  correct, `$expand`'s own nested options, and `$apply=compute(...)` still answering 501.
- **A failing-first pin for the 500**: a filter combining a cast clause with a second clause, asserting
  the filtered rows. It answers 500 today.
- **Gate** — plain `mvn -B install --fail-at-end` (never `-Pbuild.fast`). Baseline: 38 modules, 4204
  tests.

## Error handling

| Situation | Result |
|---|---|
| Nested option not allowed for the selected property's kind | Syntax error, 400 |
| Same property carrying select options twice, or in both `$select` and `$expand` | 400 (§11.2.5.1) |
| Cast to a type not compatible with the property | Existing `INCOMPATIBLE_TYPE_FILTER` |
| Instance not of the cast type, inside a Boolean expression | Member evaluates to null → comparison false, not an error |
| Compute alias colliding with a declared property | 400 (D3) |
| Compute expression returning a non-primitive | Existing `ONLY_FOR_PRIMITIVE_TYPES` from the reused parse layer |
| `$apply=compute(...)` | Still 501 — unchanged |

## Deviations and rulings

**D1 — Wave 1 removes a special case whose answers were right.** Where the narrowing hack fires today
the result is correct, so removing it must not change those answers: evaluating the cast per instance
over the base collection yields the same rows, because an instance of the wrong type makes the
comparison false. What changes is that the previously-500 shapes now answer. The equivalence is pinned
by keeping the existing `DerivedAndMixedTypeTestITCase` assertions green.

**D2 — item 3's evaluation rule is ours.** No source defines what a single-valued navigation property
evaluates to (G1). We take: the related entity, or null when unlinked.

**D3 — a computed alias may not collide with a declared property.** The ABNF makes `computedProperty`
a bare identifier with no uniqueness rule (G3). We reject the collision with 400 rather than shadowing
a declared property, because shadowing would make `$select=Name` ambiguous.

**D4 — §11.2.5.1's "more than one place" is enforced for its two clear cases only** (G4): the same
property with options twice in one `$select`, and options in both `$select` and `$expand`. Whether two
different paths reaching the same property count is not stated, and we do not guess.

**D5 — `$compute` evaluation order is an inference** from the dependency in §11.2.5.3 (G2): before
`$select`, `$filter` and `$orderby`.

**D6 — no payload change announces this level.** `Core.ODataVersions` already reports `4.01`, which is
the protocol version, not the conformance level. The claim is recorded in the audit and `CLAUDE.md`.

## Risks

- **`SelectItem` is public API.** Additive only, but it ships in `server-api`; the new accessors must
  match `ExpandItem`'s names exactly so the two read alike.
- **`$compute` touches the whole request pipeline** — parse, evaluate, select, filter, orderby,
  serialize. It is the split candidate.
- **The cast fix removes a special case that existing tests depend on.** `DerivedAndMixedTypeTestITCase`
  exercises it and must stay green unchanged — that is the equivalence check, not a fixture to correct.
- **Dynamic properties already exist for open types.** Computed properties must not be confused with
  them at serialization time: an entity may carry both, and only computed ones are named by `$compute`.
