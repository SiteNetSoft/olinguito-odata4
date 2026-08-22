# OData 4.01 Minimal Conformance — Tier 7 Citations Digest

Primary sources downloaded in full and converted to plain text; every quote below is verbatim from
those documents, with section numbers taken from the documents' own headings.

- **[OData-Protocol]** OData Version 4.01 Part 1: Protocol (OS, 23 April 2020) —
  <https://docs.oasis-open.org/odata/odata/v4.01/os/part1-protocol/odata-v4.01-os-part1-protocol.html>
- **[OData-URL]** OData Version 4.01 Part 2: URL Conventions (OS, 23 April 2020) —
  <https://docs.oasis-open.org/odata/odata/v4.01/os/part2-url-conventions/odata-v4.01-os-part2-url-conventions.html>
- **[OData-ABNF]** OData ABNF Construction Rules Version 4.01 (OS) —
  <https://docs.oasis-open.org/odata/odata/v4.01/os/abnf/odata-abnf-construction-rules.txt>
- **[OData-VocCore]** referenced for the `Core.ODataVersions` term; **[OData-VocCap]** for `Capabilities.*`.

Scope: only the clauses Tier 7 closes — the OData 4.01 Minimal Conformance Level, §13.2.1. Anything the
sources do not state is called out explicitly as **SPEC SILENT** and collected in the closing Gaps section.

---

## 0. The conformance clause itself

**[OData-Protocol] §13.2.1 OData 4.01 Minimal Conformance Level**, verbatim (items Tier 7 acts on):

> "In order to conform to the OData 4.01 Minimal Conformance Level, a service:
> 1. MUST conform to the OData 4.0 Minimal Conformance Level
> 2. MUST be compliant with version 4.01 of the [OData-JSON] format
> 3. MUST return the AsyncResult result header in the final response to an asynchronous request if asynchronous operations are supported.
> 4. MUST support both prefixed and non-prefixed variants of supported headers and preference values
> 5. MUST reject a request with an incompatible $schemaversion system query option if a Core.SchemaVersion annotation is returned in $metadata
> 6. MUST support specifying supported system query options with or without the $ prefix
> 7. MUST support case-insensitive query option, operator, and canonical function names
> 8. MUST return identifiers in the case they are specified in $metadata
> 9. MUST support both 4.0 and 4.01 syntax in URLs for supported functionality regardless of requested OData-MaxVersion
>    a. MUST support casting strings to primitive types in URLs
>    b. MUST support enumeration and duration literals in URLs with or without the type prefix
>    c. MUST support invoking parameter-less function imports with or without parentheses
>    d. MUST support an empty object or no-content for the request body when invoking an action with no non-binding parameters
>    e. MUST support invoking functions and actions in a default namespace with or without namespace qualification
>    f. MUST support parameter aliases for key values and function parameter values if they allow the octets 00 (NUL), 2F (forward slash), or 5C (backslash) in string literals
>    g. SHOULD support implicit aliasing of parameters
>    h. SHOULD support eq/ne null comparison for navigation properties with a maximum cardinality of one
>    i. SHOULD support the in operator
>    j. SHOULD support divby
>    k. SHOULD support negative indexes for the substring function
>    l. MAY support Key-As-Segment URL convention
>       a. MUST also support canonical URL conventions (described in [OData-URL]) or include URLs in payload"

**How conformance is *declared*** — [OData-Protocol] §13.2 (immediately preceding §13.2.1):

> "OData services can report conformance to the OData 4.01 specification by including 4.01 in the list of
> supported protocol versions in the Core.ODataVersions annotation, as defined in [OData-VocCore]. As all
> OData 4.01 compliant services must also be fully OData 4.0 compliant, OData 4.01 services do not need to
> separately list 4.0 as a supported version."

This makes §13.2.1 item 12 (`Core.ODataVersions`, a SHOULD) the mechanism by which the whole tier's claim
becomes visible to clients — it is not a decorative annotation.

---

## 1. Item 6 — system query options with or without the `$` prefix

**(a)** [OData-URL] **§5.1 System Query Options**; [OData-Protocol] **§11.2.6 Querying Collections**

**(b) Verbatim ([OData-URL] §5.1):**
> "System query options are query string parameters that control the amount and order of the data returned
> for the resource identified by the URL. The names of all system query options are optionally prefixed with
> a dollar ($) character. 4.01 Services MUST support case-insensitive system query option names specified
> with or without the $ prefix. Clients that want to work with 4.0 services MUST use lower case names and
> specify the $ prefix."

**Verbatim ([OData-Protocol] §11.2.6)**, the same rule restated:
> "The target collection is specified through a URL, and query operations such as filter, sort, paging, and
> projection are specified as system query options optionally prefixed with a dollar ($) character. 4.01
> Services MUST support case-insensitive system query option names specified with or without the $ prefix.
> Clients that want to work with 4.0 services MUST use lower case names and specify the $ prefix."

**(c) The collision rule — [OData-Protocol] §6.1 Query Option Extensibility**, verbatim:
> "OData-defined system query options are optionally prefixed with \"$\". Services may support additional
> custom query options not defined in the OData specification, but they MUST NOT begin with the \"$\" or
> \"@\" character and MUST NOT conflict with any OData-defined system query options defined in the OData
> version supported by the service."

**Consequence, and a correction to the Tier 7 brainstorm:** the design provisionally assumed that a custom
query option named `filter` would win over the `$`-less system option. That is **wrong**. In a 4.01 service
a custom option is *forbidden* from conflicting with a system option name, so there is no collision to
arbitrate: the system option wins, and a service that declares a conflicting custom option is the
non-conformant party. See Gaps §G1 for the compatibility consequence.

**(d) The duplicate rule, which normalization must now honor — [OData-URL] §5.1**, verbatim:
> "The same system query option, irrespective of casing or whether or not it is prefixed with a $, MUST NOT
> be specified more than once for any resource."

So `?$filter=…&FILTER=…` and `?filter=…&$filter=…` are both errors. Duplicate detection has to run on the
*normalized* name, not the raw one.

**(e) Unsupported options — [OData-Protocol] §11.2.6**, verbatim:
> "An OData service MAY support some or all of the system query options defined. If a data service does not
> support a system query option, it MUST fail any request that contains the unsupported option and SHOULD
> return 501 Not Implemented."

---

## 2. Item 7 — case-insensitive query option, operator and function names

**(a)** [OData-URL] §5.1 (quoted above, covers option names); [OData-Protocol] **§11.2.6.1.1 Built-in Filter
Operations**; [OData-URL] §5.1.1.1 / §5.1.1.2 / §5.1.1.4.

**(b) Verbatim ([OData-Protocol] §11.2.6.1.1):**
> "OData supports a set of built-in filter operations, as described in this section.
> 4.01 services MUST support case-insensitive operation names. Clients that want to work with 4.0 services
> MUST use lower case operation names."

**Verbatim ([OData-Protocol] §11.2.6.1.2, built-in query functions):**
> "4.01 services MUST support case-insensitive built-in [query function names]."

**Verbatim ([OData-URL] §5.1.1.x)**, stated once per operator family and once for canonical functions:
> "4.01 Services MUST support case-insensitive operator names. Clients that want to work with 4.0 services
> MUST use lower case operator names."
> "4.01 Services MUST support case-insensitive canonical function names. Clients that want to work with 4.0
> services MUST use lower case canonical function names."

**Verbatim ([OData-URL] §5.1.1.10, lambda operators):**
> "4.01 Services MUST support case-insensitive lambda operator [names]."

**(c) What is NOT covered.** The clause enumerates *option, operator, and function names*. It does not make
literal values case-insensitive, and the ABNF fixes their case: `nullValue = "null"`, `booleanValue`,
`nanValue = "NaN"`, `INF`, and the geo WKT keywords each have their own casing rules ([OData-ABNF]).
`true`/`false`/`null`/`NaN`/`INF` therefore stay case-sensitive. Identifiers likewise stay case-sensitive —
item 8 requires returning identifiers in the case declared in `$metadata`, which presupposes exact matching.

---

## 3. Item 9a — casting strings to primitive types in URLs

**(a)** [OData-Protocol] §13.2.1 item 9a (quoted above); semantic rule in [OData-URL] **§5.1.1.10.1 cast**.

**(b) Verbatim ([OData-URL] §5.1.1.10.1), the governing assignment rule:**
> "Edm.String, or a type definition based on Edm.String, can be cast to a primitive type if the string
> contains a literal representation for the target type."

and the companion rules bounding it:
> "The null value can be cast to any type."
> "Primitive types are cast to Edm.String or a type definition based on it by using the literal
> representation used in payloads, and WKT (well-known text) format for Geo types […]. The cast fails if the
> target type specifies an insufficient MaxLength."
> "Numeric primitive types are cast to each other with appropriate rounding. The cast fails if the integer
> part doesn't fit into the target type."

**(c) Reading for Tier 7.** `Employees('1')` against an `Edm.Int32` key is a string being cast to a
primitive, and it succeeds precisely when the quoted content is a valid literal for the target type. This is
exactly a `valueOfString` delegation: a *fallback* after the strict token kind fails, not a relaxation of
what counts as a valid value. `Employees('abc')` still fails, because `abc` is not a literal representation
of an Int32.

**(d) Key-predicate grammar — [OData-ABNF]**, verbatim:
> ```
> keyPredicate     = simpleKey / compoundKey / keyPathSegments
> simpleKey        = OPEN ( parameterAlias / keyPropertyValue ) CLOSE
> compoundKey      = OPEN keyValuePair *( COMMA keyValuePair ) CLOSE
> keyValuePair     = ( primitiveKeyProperty / keyPropertyAlias  ) EQ ( parameterAlias / keyPropertyValue )
> keyPropertyValue = primitiveLiteral
> ```
The grammar admits any `primitiveLiteral` in key position — a quoted string is grammatically valid there
regardless of the key's declared type, so this is a *semantic* acceptance, not a grammar change.

---

## 4. Item 9b — enumeration and duration literals with or without the type prefix

**(a)** [OData-URL] **§5.1.1.14.1 Primitive Literals**.

**(b) Verbatim:**
> "Duration literals in OData 4.0 required prefixing with \"duration\". Enumeration literals in OData 4.0
> required prefixing with the qualified type name of the enumeration.
> In OData 4.01, services MUST support duration and enumeration literals with or without the type prefix.
> OData clients that want to operate across OData 4.0 and OData 4.01 services should always include the
> prefix for duration and enumeration types."

**(c)** The same section's examples show both spellings side by side:
> "ColorEnumValue eq Sales.Pattern'Yellow',"
> "ColorEnumValue eq 'Yellow',"
> "DurationValue eq 'P12DT23H59M59.999999999999S'"
> "DurationValue eq duration'P12DT23H59M59.999999999999S'"

Note the unprefixed forms are still **quoted** — dropping the prefix does not drop the quotes, so the
unprefixed literal is lexically a `StringValue` that must be interpreted against the expected type. This
makes 9b the same shape as 9a: expected-type-driven reinterpretation of a quoted string.

---

## 5. Item 9c — parameter-less function calls with or without parentheses

**(a)** [OData-Protocol] §13.2.1 item 9c; grammar in [OData-ABNF].

**(b) Verbatim ([OData-ABNF]):**
> ```
> functionImportCallNoParens     = entityFunctionImport
>                                / entityColFunctionImport
>                                / complexFunctionImport
>                                / complexColFunctionImport
>                                / primitiveFunctionImport
>                                / primitiveColFunctionImport
>
> boundFunctionCallNoParens     = [ namespace "." ] entityFunction
>                               / [ namespace "." ] entityColFunction
>                               / [ namespace "." ] complexFunction
>                               / [ namespace "." ] complexColFunction
>                               / [ namespace "." ] primitiveFunction
>                               / [ namespace "." ] primitiveColFunction
> ```
> and in the resource-path rules:
> ```
>              / functionImportCallNoParens     [ querySegment ]
>              / boundFunctionCallNoParens      [ querySegment ]
> ```

**(c) Wider than the conformance clause says.** Item 9c names function *imports*, but the ABNF defines a
no-parens form for **bound** functions too, and both appear in the resource-path production. Standards-first,
Tier 7 implements both; implementing only the import form would satisfy §13.2.1 while leaving the grammar
half-supported.

**(d)** `functionParameters = OPEN [ functionParameter *( COMMA functionParameter ) ] CLOSE` — the parameter
list is already optional *inside* the parentheses, so `Fn()` and `Fn` differ only by the parentheses
themselves.

---

## 6. Item 4 — prefixed and non-prefixed preference names

**(a)** [OData-Protocol] **§8.2.8** and its subsections (one Note per preference).

**(b) Verbatim (§8.2.8.2, `callback` — the fullest statement, including the tie-break):**
> "Note: The callback preference was named odata.callback in OData version 4.0. Services that support the
> callback preference SHOULD also support odata.callback for OData 4.0 clients and clients SHOULD use
> odata.callback for compatibility with OData 4.0 services. If both callback and odata.callback preferences
> are specified in the same request, the value of the callback preference SHOULD be used."

**Verbatim (§8.2.8.1, `allow-entityreferences`):**
> "Note: The allow-entityreferences preference was named odata.allow-entityreferences in OData version 4.0.
> Services that support the allow-entityreferences preference SHOULD also support odata.allow-entityreferences
> for OData 4.0 clients and clients SHOULD use odata.allow-entityreferences for compatibility with OData 4.0
> services."

**Verbatim (§8.2.8.3, `continue-on-error`):**
> "Note: The continue-on-error preference was named odata.continue-on-error in OData version 4.0. Services
> that support the continue-on-error preference SHOULD also support odata.continue-on-error for OData 4.0
> clients and clients SHOULD use odata.continue-on-error for compatibility with OData 4.0 services."

**Verbatim (§8.2.8.4, `include-annotations`):**
> "Note: The include-annotations preference was named odata.include-annotations in OData version 4.0.
> Services that support the include-annotations preference SHOULD also support odata.include-annotations for
> OData 4.0 clients and clients SHOULD use odata.include-annotations for compatibility with OData 4.0
> services. If both include-annotations and odata.include-annotations preferences are specified in the same
> request, the value of the include-annotations preference SHOULD be used."

**(c) Direction of travel.** The **4.01 name is the bare one**; `odata.`-prefixed is the legacy 4.0 spelling
retained for compatibility. Olinguito currently models only the prefixed spelling for
`allow-entityreferences`, `callback`, `continue-on-error`, `include-annotations`, `maxpagesize` and
`track-changes` — i.e. it implements the legacy name and is missing the modern one, the opposite of how the
gap reads at first glance. Where the spec states a tie-break, the **bare (4.01) name wins**; the two
preferences that state it explicitly are `callback` and `include-annotations`, and applying the same
precedence uniformly is the consistent reading (see Gaps §G2).

---

## 7. Items 9j, 9k — `divby` and negative substring indexes

**(a)** [OData-URL] **§5.1.1.2.5 (Division)** and **§5.1.1.5.7 (substring)**.

**(b) Verbatim (§5.1.1.2.5):**
> "The div and divby operators divide the left numeric operand by the right numeric operand. […] The divby
> operator, on the other hand, promotes both operands to decimal before computing the result, may yield a
> fractional result, and does not fail for divby zero, returning -INF, INF, or NaN depending on the sign of
> the left operand."

`divby` is therefore **not** an alias for `div`: it has different promotion rules and different
divide-by-zero behavior. Implementing it as a synonym would be wrong.

**(c) Verbatim (§5.1.1.5.7):**
> "A negative start index N, if supported, returns a string/collection starting N characters/items before the
> end of the string/collection."

Note "if supported" — this is the SHOULD at item 9k. Today Olinguito clamps a negative start to 0, which is
a third behavior, neither the 4.0 nor the 4.01 one.

---

## 8. Item 12 — `Core.ODataVersions`

**(a)** [OData-Protocol] **§5.1** (version negotiation) and §13.2.

**(b) Verbatim (§5.1):**
> "Services SHOULD advertise supported versions of OData through the Core.ODataVersions term, defined in
> [OData-VocCore]. This version of the specification defines OData version values 4.0 and 4.01."

**(c)** Combined with §13.2 (quoted in §0 above), emitting `Core.ODataVersions` with `4.01` is how the
service *states* the conformance Tier 7 achieves.

---

## Gaps — what the sources do not settle

**G1. Compatibility fallout of item 6 is SPEC SILENT.** §6.1 forbids a conflicting custom query option, but
says nothing about a service that already has one from its 4.0 days. Once Olinguito routes `filter=…` to the
system option, any existing deployment using `filter` (or `select`, `top`, …) as a *custom* option silently
changes behavior. The spec's position is that such a service was already non-conformant; that is a defensible
ruling but it is ours, not a quotation, and it belongs in the design's deviations list.

**G2. The preference tie-break is stated for only two preferences.** §8.2.8.2 (`callback`) and §8.2.8.4
(`include-annotations`) say the bare name wins when both are sent; §8.2.8.1, §8.2.8.3 and the remaining
subsections state no tie-break. Applying "bare wins" uniformly is an inference, and it is also a SHOULD, not
a MUST, everywhere it *is* stated.

**G3. Implicit parameter aliasing (item 9g) is undefined in all three sources.** The phrase "implicit
aliasing of parameters" appears exactly once across [OData-Protocol], [OData-URL] and [OData-ABNF] — in the
conformance list itself. §11.2.5/§5.3 define *explicit* parameter aliases (`@p1` assigned as a query option)
with no shorthand form, and [OData-ABNF] has no production for a bare `name` parameter:
`functionParameter = parameterName EQ ( parameterAlias / primitiveLiteral )`. There is no normative grammar
to implement against. **Recommendation: defer 9g** and record it as a known open SHOULD rather than invent
semantics; it does not affect the 4.01 Minimal claim.

**G4. Item 13 (Capabilities vocabulary) prescribes no minimum set.** [OData-Protocol] §13.2.1 item 13 says a
service SHOULD report capabilities via the Capabilities vocabulary but names no required terms, so "done" is
a judgment call about which terms tecsvc annotates.

**G5. Item 9a's boundary in *compound* keys is SPEC SILENT.** The cast rule is stated for values generally;
nothing addresses whether a compound key may mix quoted and unquoted forms for different properties. Reading
each key value independently is the natural extension and is what Tier 7 assumes.
