# OData 4.01 Intermediate Conformance — Tier 8 Citations Digest

Sources are the same full documents used for Tier 7, downloaded and converted to text; every quote
below is verbatim, with section numbers from the documents' own headings.

- **[OData-Protocol]** OData Version 4.01 Part 1: Protocol (OS, 23 April 2020)
- **[OData-URL]** OData Version 4.01 Part 2: URL Conventions (OS, 23 April 2020)
- **[OData-ABNF]** OData ABNF Construction Rules Version 4.01 (OS)

Scope: only the clauses Tier 8 closes — the OData 4.01 Intermediate Conformance Level (§13.2.2) and
the 4.0 Intermediate gap it inherits (§13.1.2 item 4). Anything the sources do not state is marked
**SPEC SILENT** and collected in the Gaps section.

---

## 0. The conformance clauses

**[OData-Protocol] §13.2.2 OData 4.01 Intermediate Conformance Level**, verbatim:

> "In order to conform to the OData 4.01 Intermediate Conformance Level, a service:
> 1. MUST conform to the OData 4.01 Minimal Conformance Level
> 2. MUST conform to the OData 4.0 Intermediate Conformance Level
> 3. MUST support eq/ne null comparison for navigation properties with a maximum cardinality of one
> 4. MUST support the in operator
> 5. MUST support the $select option nested within $select
> 6. SHOULD support the count of a filtered collection in a common expression
> 7. SHOULD support equal and non-equal structural comparison
> 8. SHOULD support $compute system query option
> 9. SHOULD support nested query options in $select
> 10. MAY support nested parameter alias assignments in $select and $expand
> 11. MAY support filtering a collection using a /$filter path segment"

**[OData-Protocol] §13.1.2 OData 4.0 Intermediate Conformance Level**, item 4 verbatim:

> "MUST support casting to a derived type according to [OData‑URL] if derived types are present in
> the model"

Note the condition: the requirement binds only when the model has derived types. Olinguito's
reference service does have them (`ETBaseTwoKeyNav`, `ETTwoBaseTwoKeyNav`, `ETBase`, …), so it binds
here.

Items 1 and 2 of §13.2.2 are roll-ups. Item 1 is satisfied by Tier 7; item 2 brings §13.1.2 in,
whose only open gap per the 2026-08-20 audit is item 4. Items 4, 6 and 7 of §13.2.2 are already
SUPPORTED. Items 10 and 11 are MAY and out of scope.

---

## 1. Item 3 — eq/ne null on single-valued navigation properties

**(a)** [OData-Protocol] §13.2.2 item 3 (quoted above). The same capability appears at §13.2.1 item
9h as a **SHOULD**; at Intermediate it is a **MUST**, which is why Tier 7 could defer it and Tier 8
cannot.

**(b) SPEC SILENT on the mechanics.** Neither [OData-Protocol] nor [OData-URL] spells out the
evaluation rule beyond naming the capability; the ABNF admits a bare navigation property as a
terminal member path. The natural reading, and the one Tier 8 implements: a single-valued navigation
property evaluates to the related entity, or to null when no such entity is linked, so `eq null` is
true exactly when the link is absent.

---

## 2. Item 4 of §13.1.2 — casting to a derived type

**(a)** [OData-URL] **§4.11 Addressing Derived Types** and the `cast` rules of §5.1.1.10.1.

**(b) Verbatim ([OData-URL] §5.1.1.10, type-cast segments in expressions):**
> "If the type cast is part of a Boolean expression, the type cast will evaluate to null."

This is the rule the current implementation violates: it returns the whole cast collection and
discards the surrounding comparison, instead of evaluating the cast per instance and letting a
non-matching instance make the expression null (hence false).

**(c) Verbatim ([OData-URL] §5.1.1.10.1 `cast`), the assignment rule that governs a structural cast:**
> "Structured types are assignable to their type or a direct or indirect base type."

**(d)** The example forms in [OData-URL] §5.1.1.10 show a cast inside a filter alongside further
path navigation, i.e. the cast is one segment of a member expression rather than a whole-collection
selector.

---

## 3. Items 5 and 9 — nested options in `$select`

**(a)** [OData-Protocol] **§11.2.5.1 System Query Option `$select`**; grammar in [OData-ABNF].

**(b) Verbatim (§11.2.5.1), the authoritative list of allowed nested options:**
> "Query options can be applied to a selected property by appending a semicolon-separated list of
> query options, enclosed in parentheses, to the property. Allowed system query options are $select
> and $compute for complex properties, plus $filter, $search, $count, $orderby, $skip, and $top for
> collection-valued properties."

**(c) Verbatim (§11.2.5.1), a MUST that constrains acceptance, not just evaluation:**
> "A property MUST NOT have select options specified in more than one place in a request and MUST
> NOT have both select options and expand options specified."

**(d) Verbatim ([OData-ABNF]):**
> ```
> selectItem     = STAR
>                / allOperationsInSchema
>                / selectProperty
>                / optionallyQualifiedActionName
>                / optionallyQualifiedFunctionName
>                / ( optionallyQualifiedEntityTypeName / optionallyQualifiedComplexTypeName )
>                  "/" ( selectProperty / optionallyQualifiedActionName / optionallyQualifiedFunctionName )
> selectProperty = primitiveProperty / primitiveAnnotationInQuery
>                / ( primitiveColProperty / primitiveColAnnotationInQuery ) [ OPEN selectOptionPC *( SEMI selectOptionPC ) CLOSE ]
>                / navigationProperty
>                / selectPath [ OPEN selectOption *( SEMI selectOption ) CLOSE
>                             / "/" selectProperty
>                             ]
> selectPath     = ( complexProperty / complexColProperty / complexAnnotationInQuery ) [ "/" optionallyQualifiedComplexTypeName ]
> selectOptionPC = filter / search / inlinecount / orderby / skip / top
> selectOption   = selectOptionPC / compute / select / aliasAndValue
> ```

**(e) Reading.** The grammar and the prose agree on a two-set split: a **primitive collection**
property admits only `selectOptionPC` (filter/search/count/orderby/skip/top), while a **complex or
navigation** `selectPath` additionally admits `$select`, `$compute` and alias assignments. A plain
(non-collection) primitive property admits no options at all. The parser should enforce that split
rather than accept one union of options everywhere, or `$select=Name($select=X)` on a string
property would parse and mean nothing.

`aliasAndValue` inside `selectOption` is §13.2.2 item 10, a **MAY**, and is out of Tier 8's scope.

---

## 4. Item 8 — the `$compute` system query option

**(a)** [OData-Protocol] **§11.2.5.3 System Query Option `$compute`**; grammar in [OData-ABNF].

**(b) Verbatim (§11.2.5.3):**
> "The $compute system query option allows clients to define computed properties that can be used in
> a $select or within a $filter or $orderby expression."

> "Computed properties SHOULD be included as dynamic properties in the result and MUST be included if
> $select is specified with the computed property name, or star (*)."

**(c) Verbatim (§11.2.5.3, Example 45)** — note the computed property is used by a sibling `$select`
inside the same `$expand`, so compute must be visible to the options evaluated alongside it:
> ```
> GET http://host/service/Customers?
>     $filter=Orders/any(o:o/TotalPrice gt 100)
>     &$expand=Orders($compute=Price mult Qty as TotalPrice
>                     ;$select=Name,Price,Qty,TotalPrice)
> ```

**(d) Verbatim ([OData-ABNF]):**
> ```
> compute          = ( "$compute" / "compute" ) EQ computeItem *( COMMA computeItem )
> computeItem      = commonExpr RWS "as" RWS computedProperty
> computedProperty = odataIdentifier
> ```

**(e) Consequence for ordering.** Because computed properties are usable *from* `$select`, `$filter`
and `$orderby`, `$compute` must be evaluated before all three. The spec states the dependency
(quote (b)) rather than an explicit ordering rule — see Gaps G2.

---

## 5. What already exists in the codebase

Recorded here because it changes the shape of the work rather than the requirements.

`$apply` already carries a `compute` transformation: `ApplyParser.parseComputeTrafo` builds
`ComputeImpl`/`ComputeExpressionImpl` over `DynamicProperty`/`DynamicStructuredType`, and the public
`Compute`/`ComputeExpression` interfaces live in `server-api`. **None of it is evaluated anywhere** —
`lib/server-tecsvc` contains no reference to `Compute` or `ComputeExpression`, because `$apply` is
rejected wholesale with 501 at `TechnicalProcessor.validateOptions`. So `$compute` is "reuse the
parse layer, build the evaluator", and the dynamic-property serialization added by the OpenType work
gives the payload side.

---

## Gaps — what the sources do not settle

**G1. The evaluation rule for item 3 is SPEC SILENT.** The clause names the capability; no source
states what a single-valued navigation property evaluates to. Tier 8 takes the natural reading (the
related entity, or null when unlinked) — recorded as our inference, not a quotation.

**G2. `$compute`'s position in the evaluation order is stated only as a dependency.** §11.2.5.3 says
computed properties can be used in `$select`, `$filter` and `$orderby`; it does not enumerate an
ordering alongside §11.2.6's other options. Evaluating `$compute` before those three is the only
reading that satisfies the dependency, but the ordering itself is an inference.

**G3. Collision between a computed alias and a declared property is SPEC SILENT.** `computedProperty`
is a bare `odataIdentifier` with no stated uniqueness rule against the entity type's own properties.
Tier 8 rejects the collision with a 400 rather than shadowing a declared property; that is our
ruling.

**G4. "More than one place" in §11.2.5.1's MUST is not defined precisely.** It plainly forbids the
same property carrying options twice in one `$select`, and forbids options in both `$select` and
`$expand` for the same property. Whether two *different* paths that happen to reach the same property
count as "more than one place" is not stated; Tier 8 enforces the two clear cases only.

**G5. Item 4 of §13.1.2 binds conditionally** — "if derived types are present in the model". A
service whose model has none is conformant without any cast support. This does not soften the
requirement for tecsvc, whose model does have derived types.
