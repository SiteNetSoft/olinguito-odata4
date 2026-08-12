# OData 4.01 Tier-5/6 Feature Citations Digest

Primary sources fetched directly (full HTML downloaded and converted to plain text; all quotes below are verbatim from these documents, section numbers taken from the documents' own tables of contents / headings):

- **[OData-Protocol]** OData Version 4.01 Part 1: Protocol (errata-latest / "OS" pointer page): https://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html
- **[OData-URL]** OData Version 4.01 Part 2: URL Conventions (errata-latest / "OS" pointer page): https://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part2-url-conventions.html
- **[OData-JSON]** OData JSON Format Version 4.01: https://docs.oasis-open.org/odata/odata-json-format/v4.01/odata-json-format-v4.01.html
- **[OData-VocCore]** OASIS OData Core Vocabulary (raw CSDL, `main` branch): https://raw.githubusercontent.com/oasis-tcs/odata-vocabularies/main/vocabularies/Org.OData.Core.V1.xml (human-readable companion: https://github.com/oasis-tcs/odata-vocabularies/blob/main/vocabularies/Org.OData.Core.V1.md)

Note: these "part1"/"part2" URLs are OASIS's own "latest errata" redirect pages (they resolve to the current errata-consolidated OS text), which is what the task asked to prefer.

---

## 1. matchesPattern filter function

Sources used: [OData-URL] §5.1.1.7 / §5.1.1.7.1

**(a) Section number(s) and title(s):**
- Part 2, §5.1 System Query Option $filter → §5.1.1.7 String Functions → **§5.1.1.7.1 matchesPattern**

**(b) Verbatim normative text:**
> "The matchesPattern function has the following signature:
> `Edm.Boolean matchesPattern(Edm.String,Edm.String)`
>
> The second parameter MUST evaluate to a string containing an [ECMAScript] (JavaScript) regular expression. The matchesPattern function returns true if the first parameter evaluates to a string matching that regular expression, using syntax and semantics of [ECMAScript] regular expressions, otherwise it returns false."

Example given in the spec:
> "Example 81: all customers with a CompanyName that match the (percent-encoded) regular expression ^A.*e$
> `http://host/service/Customers?$filter=matchesPattern(CompanyName,'%5EA.*e$')`"

**Regex dialect:** The spec normatively references **[ECMAScript] (JavaScript)** regular expressions — NOT XPath `fn:matches`, NOT POSIX/PCRE. The `[ECMAScript]` citation is a formal normative reference in the spec's reference list (standard ECMA-262 pointer).

**Matching semantics (anchored vs. match-anywhere):** The spec text itself does not add any anchoring qualifier ("MUST fully match" / "MUST match at start") — it just says the function returns true "if the first parameter evaluates to a string matching that regular expression, using syntax and semantics of [ECMAScript] regular expressions." Because ECMAScript regex semantics are inherently match-anywhere (substring search) unless the pattern itself uses `^`/`$` anchors, and the spec's own example uses explicit `^...$` anchors in the pattern to force a full match, the anchoring behavior is delegated entirely to ECMAScript regex semantics and the pattern string supplied by the caller — the function itself does not implicitly anchor. SPEC SILENT on an explicit statement like "matching is not anchored by default"; this is inferred from ECMAScript semantics + the example's explicit use of `^`/`$`, not stated as a separate sentence.

**(c) Mandated error behaviors / status codes:** SPEC SILENT — no error/status-code language tied specifically to `matchesPattern` (e.g., invalid regex syntax) was found in Part 1 or Part 2.

**(d) Conformance-clause references:** SPEC SILENT — `matchesPattern` does **not** appear anywhere in Part 1 §13 Conformance (Minimal/Intermediate/Advanced, 4.0 or 4.01 lists). It appears only in the Part 2 **Revision History appendix** (Committee Specification Draft 05 changelog: "New functions matchesPattern and case"), which is not a conformance clause.

**(e) N/A** — not vocabulary-driven.

---

## 2. Prefer: omit-values

Sources used: [OData-Protocol] §8.2.8.6; [OData-JSON] §24 (Conformance, item 22)

**(a) Section number(s) and title(s):**
- Part 1, §8.2.8 Preference → **§8.2.8.6 Preference omit-values**
- [OData-JSON] §24 Conformance, general conformance list item 22

**(b) Verbatim normative text (Part 1 §8.2.8.6, in full):**
> "The omit-values preference specifies values that MAY be omitted from a response payload. Valid values are nulls or defaults.
>
> If nulls is specified, then the service MAY omit properties containing null values from the response, in which case it MUST specify the Preference-Applied response header with omit-values=nulls.
>
> If defaults is specified, then the service MAY omit properties containing default values from the response, including nulls for properties that have no other defined default value. Nulls MUST be included for properties that have a non-null default value defined. If the service omits default values, it MUST specify the Preference-Applied response header with omit-values=defaults.
>
> Properties with instance annotations are not affected by this preference and MUST be included in the payload if they would be included without this preference. Clients MUST NOT try to reconstruct a null or default value for properties for which an instance annotation is present and no property value is present, for example if the property is omitted due to permissions and has been replaced with the instance annotation Core.Permissions and a value of None, see [OData-VocCore].
>
> Properties with null or default values MUST be included in delta payloads, if modified.
>
> The response to a POST operation MUST include any properties not set to their default value, and the response to a PUT/PATCH operation MUST include any properties whose values were changed as part of the operation.
>
> The omit-values preference does not affect a request payload."

**Which values may be omitted:** exactly two categories, selected by the preference's value: (1) `nulls` — properties with a null value; (2) `defaults` — properties with the property's default value, which per the text explicitly *includes* null for properties that have no other defined (non-null) default — but a property with a defined **non-null** default MUST still include an explicit `null` when its actual value is null (i.e., `defaults` omission does not let a null silently stand in for "value equals the declared non-null default").

**What the service MUST do when applying it:** MUST set `Preference-Applied: omit-values=nulls` (or `=defaults`) in the response. This is the only "apply" obligation the section states — there's no separate MUST about *which specific* qualifying properties must be dropped (dropping remains a MAY even once the preference is honored/announced).

**How clients reconstruct omitted values:** The spec does **not** give clients an affirmative reconstruction procedure/algorithm. It states the negative case only: clients MUST NOT attempt to reconstruct a null/default value for a property that instead carries an instance annotation with no value present (annotation-driven omission is a different mechanism than omit-values and must not be confused with it). SPEC SILENT on any positive "how to reconstruct" guidance for omit-values-driven omissions (the implication, though not spelled out as a MUST/SHOULD sentence, is: absence + `Preference-Applied: omit-values=nulls|defaults` on the response + knowledge of $metadata defaults tells the client what the omitted value was).

**Dynamic properties / open types under omit-values:** SPEC SILENT. Neither Part 1 §8.2.8.6 nor the [OData-JSON] spec's discussion of dynamic properties (§ on dynamic-property type control information) makes any statement connecting omit-values specifically to dynamic/open-type properties (e.g., whether a dynamic property's `@type` control information may/must still be emitted when its value is omitted). No such sentence exists in either fetched document.

**Interaction with control information:** Per the quoted text, "Properties with instance annotations are not affected by this preference and MUST be included in the payload if they would be included without this preference" — i.e., control information / instance annotations override/are orthogonal to omit-values and are never suppressed by it.

**(c) Mandated error behaviors / status codes:** SPEC SILENT — no error status code tied to omit-values.

**(d) Conformance-clause references:** Does NOT appear in Part 1 §13 Conformance (Minimal/Intermediate/Advanced) at all — grep of the full conformance section (§13.1 and §13.2, all three 4.0 and all three 4.01 levels) found zero mentions of "omit-values" (it only appears in the Part 1 Revision History appendix, as a changelog entry for "Committee Specification Draft 01", not as a conformance requirement). It DOES appear as a general conformance requirement in **[OData-JSON] §24 Conformance, item 22**:
> "MUST NOT omit null or default values unless the omit-values preference is specified in the Prefer request header and the omit-values preference is included in the Preference-Applied response header"
This JSON-format conformance item is listed under the JSON spec's own (single, undifferentiated) conformance list — it is not broken out by Minimal/Intermediate/Advanced the way Part 1 §13 is.

**(e) N/A directly** — omit-values is a `Prefer` header mechanism, not a Core-vocabulary term. (The only vocabulary term referenced in its normative text is `Core.Permissions`, cited only as an example of the *different*, annotation-driven omission mechanism that omit-values must not be confused with.)

---

## 3. Passing query options in the request body: POST {path}/$query

Source used: [OData-URL] §4.17

**(a) Section number and title:**
- Part 2, **§4.17 Passing Query Options in the Request Body**

**(b) Verbatim normative text (in full):**
> "The query options part of an OData URL can be quite long, potentially exceeding the maximum length of URLs supported by components involved in transmitting or processing the request. One way to avoid this is wrapping the request in a batch request, which has the penalty of needing to construct a well-formed batch request body.
>
> An easier alternative for GET requests is to append /$query to the resource path of the URL, use the POST verb instead of GET, and pass the query options part of the URL in the request body.
>
> Requests to paths ending in /$query MUST use the POST verb. Query options specified in the request body and query options specified in the request URL are processed together.
>
> The request body MUST use the content-type text/plain. It contains the query portion of the URL and MUST use the same percent-encoding as in URLs (especially: no spaces, tabs, or line breaks allowed) and MUST follow the syntax rules described in chapter Query Options."

Example:
> "Example 49: passing a filter condition in the request body
> `POST http://host/service/People/$query`
> `Content-Type: text/plain`
> `$filter=[FirstName,LastName]%20in%20[["John","Doe"],["Jane","Smith"]]`"

**Required Content-Type:** MUST be `text/plain` (exact wording: "The request body MUST use the content-type text/plain").

**Body format:** Percent-encoded query-option string, identical encoding rules to the URL query string ("MUST use the same percent-encoding as in URLs... MUST follow the syntax rules described in chapter Query Options"); no literal spaces/tabs/line breaks allowed.

**URL query options combined with body — merge or forbidden?** MERGED, not forbidden and not mutually exclusive: "Query options specified in the request body and query options specified in the request URL are processed together." The spec states this as a bare fact/rule, not hedged by MAY/SHOULD — i.e., both sources of query options apply together to the same request.

**Which requests it applies to:** The section frames `/$query` explicitly as "an easier alternative for GET requests" — i.e., it is presented as a mechanism to avoid URL-length limits specifically for **read/GET-style query requests** whose query option string is too long. §4.17 itself does not generalize this to arbitrary non-GET operations (create/update/delete) — it is scoped to appending `/$query` to a resource path to convey what would otherwise be GET query options.

**(c) Mandated error behaviors / status codes:** SPEC SILENT — no explicit status code (e.g., 400/415) is stated in §4.17 for a wrong Content-Type, malformed body, or unsupported verb. The only hard requirement stated is "Requests to paths ending in /$query MUST use the POST verb" (implying non-POST verbs to such a path would be non-conformant), but no numeric status code is prescribed by the spec text for that or any other violation in this section.

**(d) Conformance-clause references:** Does NOT appear in Part 1 §13 Conformance (Minimal/Intermediate/Advanced, 4.0 or 4.01) — zero mentions of "$query" anywhere in the conformance section text. It appears only in the Part 2 Revision History appendix (Committee Specification Draft 05 changelog: "/$query path segment"), which is not a conformance requirement.

**(e) N/A** — not vocabulary-driven.

---

## 4. $schemaversion system query option

Sources used: [OData-Protocol] §11.2.12; §13.2.1 item 5; [OData-VocCore] `SchemaVersion` term

**(a) Section number and title:**
- Part 1, **§11.2.12 System Query Option $schemaversion**
- Also referenced in Part 1 §13.2.1 (OData 4.01 Minimal Conformance Level), item 5.

**(b) Verbatim normative text (§11.2.12, in full):**
> "The $schemaversion system query option MAY be included in any request. For a metadata document request the value of the $schemaversion system query option addresses a specific schema version. For all other request types the value specifies the version of the schema against which the request is made. The syntax of the $schemaversion system query option is defined in [OData-ABNF].
>
> The value of the $schemaversion system query option MUST be a version of the schema as returned in the Core.SchemaVersion annotation, defined in [OData-VocCore], of a previous request to the metadata document, or * in order to specify the current version of the metadata.
>
> If specified, the service MUST process the request according to the specified version of the metadata.
>
> Clients can retrieve the current version of the metadata by making a metadata document request with a $schemaversion system query option value of *, and SHOULD include the value from the returned Core.SchemaVersion annotation in the $schemaversion system query option of subsequent requests.
>
> If the $schemaversion system query option is not specified in a request for the metadata document, the service MUST return a version of the metadata with no breaking changes over time, and the processing of all other requests that omit the $schemaversion system query option MUST be compatible with that "unversioned" schema. For more information on breaking changes, see Model Versioning.
>
> If the $schemaversion system query option is specified on an individual request within a batch, then it specifies the version of the schema to apply to that individual request. Individual requests within a batch that don't include the $schemaversion system query option inherit the schema version of the overall batch request.
>
> If the $schemaversion system query option is specified, but the version of the schema doesn't exist, the request is answered with a response code 404 Not Found. The response body SHOULD provide additional information."

**`*` semantics:** `*` means "specify the current version of the metadata" — used by clients to discover the current/latest schema version (typically against the metadata document request).

**Relationship to Core.SchemaVersion:** The allowed values of $schemaversion are defined relative to the `Core.SchemaVersion` annotation — the option's value "MUST be a version of the schema as returned in the Core.SchemaVersion annotation... of a previous request to the metadata document, or *".

**Mandated behavior on version mismatch:** "If the $schemaversion system query option is specified, but the version of the schema doesn't exist, the request is answered with a response code **404 Not Found**. The response body SHOULD provide additional information." — this is the only mismatch/error case the spec addresses (nonexistent version), and it prescribes exactly one status code, 404.

**(c) Mandated error behaviors / status codes:** 404 Not Found for a specified-but-nonexistent schema version (quoted above). No other status code is prescribed for $schemaversion specifically.

**(d) Conformance-clause references:** YES — explicitly present in **Part 1 §13.2.1 OData 4.01 Minimal Conformance Level**, item 5:
> "5. MUST reject a request with an incompatible $schemaversion system query option if a Core.SchemaVersion annotation is returned in $metadata"
This is a **conditional MUST at the Minimal level**: it only binds a service that returns a `Core.SchemaVersion` annotation in `$metadata` in the first place; support for $schemaversion handling is not unconditionally required of every Minimal-conformant service (a service that never versions its schema/never emits Core.SchemaVersion is not bound by this item). Not otherwise mentioned in Intermediate (§13.2.2) or Advanced (§13.2.3) conformance items, nor in the 4.0 conformance levels (§13.1, which predate 4.01's schema versioning feature entirely — $schemaversion did not exist in 4.0).

**(e) Core vocabulary term (from [OData-VocCore] `Org.OData.Core.V1.xml`, lines ~73-75):**
```xml
<Term Name="SchemaVersion" Type="Edm.String" Nullable="false" AppliesTo="Schema Reference">
  <Annotation Term="Core.Description" String="Service-defined value representing the version of the schema. Services MAY use semantic versioning, but clients MUST NOT assume this is the case." />
</Term>
```
- Name: `SchemaVersion`
- Type: `Edm.String`, `Nullable="false"`
- AppliesTo: `Schema Reference`
- Description (verbatim): "Service-defined value representing the version of the schema. Services MAY use semantic versioning, but clients MUST NOT assume this is the case."

---

## 5. Optional function parameters

Sources used: [OData-Protocol] §11.5.4.1.1, §11.5.4.2, §11.5.5.1, §11.5.5.2; [OData-VocCore] `OptionalParameter` / `OptionalParameterType`

**(a) Section number(s) and title(s):**
- Part 1, §11.5.4 Functions → §11.5.4.1 Invoking a Function → **§11.5.4.1.1 Inline Parameter Syntax** (the omission rule for function parameters lives here)
- Part 1, **§11.5.4.2 Function overload resolution**
- Part 1, **§11.5.5.1 Invoking an Action** (parallel omission rule for action parameters)
- Part 1, §11.5.5.2 Action Overload Resolution
- **Important finding:** Part 2 (URL Conventions) contains **zero** mentions of "OptionalParameter" or "optional parameter" — the entire normative treatment of optional parameters (both the omission rule and overload resolution) lives in Part 1 (Protocol), not Part 2 (URL Conventions), contrary to what the section name might suggest.

**(b) Verbatim normative text:**

Function inline-parameter omission (§11.5.4.1.1):
> "Non-binding parameters annotated with the term Core.OptionalParameter defined in [OData-VocCore] MAY be omitted. If it is annotated and the annotation specifies a DefaultValue, the omitted parameter is interpreted as having that default value. If omitted and the annotation does not specify a default value, the service is free on how to interpret the omitted parameter."

Function overload resolution (§11.5.4.2, in full):
> "The same function name may be used multiple times within a schema, each with a different set of parameters. For unbound overloads the combination of the function name and the unordered set of parameter names MUST identify a particular function overload. For bound overloads the combination of the function name, the binding parameter type, and the unordered set of names of the non-binding parameters MUST identify a particular function overload.
>
> All unbound overloads MUST have the same return type. Also, all bound overloads with a given binding parameter type MUST have the same return type.
>
> If the function is bound and the binding parameter type is part of an inheritance hierarchy, the function overload is selected based on the type of the URL segment preceding the function name. A type-cast segment can be used to select a function defined on a particular type in the hierarchy, see [OData‑URL].
>
> Non-binding parameters MAY be marked as optional by annotating them with the term Core.OptionalParameter defined in [OData-VocCore]. All parameters marked as optional MUST come after any parameters not marked as optional.
>
> A function overload is selected if
> · The set of specified parameters exactly matches a function overload, or else
> · The set of specified parameters matches a subset of parameters that includes all non-optional parameters of exactly one function overload.
>
> Services SHOULD avoid ambiguity, i.e. the combination of the function name, the unordered set of non-optional non-binding parameter names, plus the binding parameter type for bound overloads SHOULD identify a particular function overload. If there is ambiguity, then services MAY return 400 Bad Request with an error response body stating that the request was ambiguous."

Action parameter omission (§11.5.5.1):
> "Non-binding parameters that are nullable or annotated with the term Core.OptionalParameter defined in [OData-VocCore] MAY be omitted from the request body. If an omitted parameter is not annotated (and thus nullable), it MUST be interpreted as having the null value. If it is annotated and the annotation specifies a DefaultValue, the omitted parameter is interpreted as having that default value. If omitted and the annotation does not specify a default value, the service is free on how to interpret the omitted parameter. Note: a nullable non-binding parameter is equivalent to being annotated as optional with a default value of null.
>
> 4.01 services MUST support invoking actions with no non-binding parameters and parameterless action imports both without a request body and with a request body representing no parameters, according to the particular format. Interoperable clients SHOULD always include a request body, even when invoking actions with no non-binding parameters and parameterless action imports."

Action overload resolution (§11.5.5.2, in full — notably has NO optional-parameter-based subset-matching mechanism, unlike functions):
> "The same action name may be used multiple times within a schema provided there is at most one unbound overload, and each bound overload specifies a different binding parameter type.
>
> If the action is bound and the binding parameter type is part of an inheritance hierarchy, the action overload is selected based on the type of the URL segment preceding the action name. A type-cast segment can be used to select an action defined on a particular type in the hierarchy, see [OData‑URL]."

**URL conventions for omitting optional parameters:** For **inline parameter syntax** in function calls (i.e., `Function(Param=value,...)` in the URL), an optional parameter is simply left out of the parenthesized parameter list; no special placeholder syntax is required or defined. For **actions** (invoked via POST with parameters in the request body), the parameter is simply absent from the JSON/body representation.

**Overload-resolution rules involving optional parameters:**
- Functions: "A function overload is selected if the set of specified parameters exactly matches a function overload, or else... matches a subset of parameters that includes all non-optional parameters of exactly one function overload" — i.e., optional parameters can be freely present-or-absent as long as all *non-optional* parameters of the chosen overload are supplied, and the CSDL-level constraint "All parameters marked as optional MUST come after any parameters not marked as optional" plus "The binding parameter must not be marked as optional" (from the vocabulary term's LongDescription, see (e)) bound how overloads may be declared.
- Ambiguity handling: services SHOULD avoid ambiguous overload sets; if ambiguous, a service MAY (not MUST) return 400 Bad Request with an error body — this is the only status code tied to optional-parameter overload resolution.
- Actions: no subset-matching / optional-parameter overload mechanism is defined at all (§11.5.5.2) — action overload selection is purely by "at most one unbound overload" + "each bound overload has a different binding parameter type" + type-cast segment for inheritance disambiguation. Optional/nullable parameters affect only whether they may be omitted from the *body* of an already-selected action, not which overload is selected.

**(c) Mandated error behaviors / status codes:** Only the function-overload ambiguity case: services MAY return 400 Bad Request (not MUST) — see quoted §11.5.4.2 text above.

**(d) Conformance-clause references:** SPEC SILENT / not found. Grep of the full Part 1 §13 Conformance section text (all six conformance levels, 4.0 and 4.01 × Minimal/Intermediate/Advanced) found zero occurrences of "OptionalParameter." Not called out as a conformance-level requirement anywhere. (Item 9.d/9.e in §13.2.1 Minimal — "MUST support an empty object or no-content for the request body when invoking an action with no non-binding parameters" / "MUST support invoking functions and actions in a default namespace with or without namespace qualification" — are adjacent 4.01-syntax-support items in the same numbered list but are NOT about Core.OptionalParameter specifically; they concern zero-parameter actions/imports, not optional-parameter overload selection.)

**(e) Core vocabulary term (from [OData-VocCore] `Org.OData.Core.V1.xml`, lines ~507-516):**
```xml
<Term Name="OptionalParameter" Type="Core.OptionalParameterType" Nullable="false" AppliesTo="Parameter">
  <Annotation Term="Core.Description" String="Supplying a value for the action or function parameter is optional." />
  <Annotation Term="Core.LongDescription" String="All parameters marked as optional must come after any parameters not marked as optional. The binding parameter must not be marked as optional." />
</Term>
<ComplexType Name="OptionalParameterType">
  <Property Name="DefaultValue" Type="Edm.String" Nullable="true">
    <Annotation Term="Core.Description" String="Default value for an optional parameter of primitive or enumeration type, using the same rules as the `cast` function in URLs." />
    <Annotation Term="Core.LongDescription" String="If no explicit DefaultValue is specified, the service is free on how to interpret omitting the parameter from the request. For example, a service might interpret an omitted optional parameter `KeyDate` as having the current date." />
  </Property>
</ComplexType>
```
- Name: `OptionalParameter`
- Type: `Core.OptionalParameterType`, `Nullable="false"`
- AppliesTo: `Parameter`
- Description: "Supplying a value for the action or function parameter is optional."
- LongDescription: "All parameters marked as optional must come after any parameters not marked as optional. The binding parameter must not be marked as optional."
- `DefaultValue` (the complex type's sole property): `Edm.String`, `Nullable="true"`, description "Default value for an optional parameter of primitive or enumeration type, using the same rules as the `cast` function in URLs"; long description: "If no explicit DefaultValue is specified, the service is free on how to interpret omitting the parameter from the request. For example, a service might interpret an omitted optional parameter `KeyDate` as having the current date."

---

## 6. Key-as-segment convention

Source used: [OData-URL] §4.3.6; [OData-Protocol] §13.2.1 item 9.l

**(a) Section number and title:**
- Part 2, **§4.3.6 Key-as-Segment Convention**
- Also Part 1 §13.2.1 (OData 4.01 Minimal Conformance Level), item 9.l / 9.l.a

**(b) Verbatim normative text (§4.3.6, in full):**
> "Services MAY support an alternate convention for addressing entities by appending a segment containing the unquoted key value to the URL of the collection containing the entity. Forward-slashes in key value segments MUST be percent-encoded; single quotes within key value segments are treated as part of the key value and do not need to be doubled or percent encoded."
>
> [examples: `http://host/service/Employees/A1245`, `http://host/service/People/O'Neil`, `http://host/service/People/O%27Neil`, `http://host/service/Categories/Smartphone%2FTablet`]
>
> "For multi-part keys, the entity MUST be addressed by multiple segments applied, one for each key value, in the order they appear in the metadata description of the entity key."
>
> [example: `https://host/service/OrderItems(OrderID=1,ItemNo=2)` ≡ `https://host/service/OrderItems/1/2`]
>
> "If a navigation property leading to a related entity type has a partner navigation property that specifies a referential constraint, then those key properties of the related entity that take part in the referential constraint MUST be omitted from URLs using key-as-segment convention."
>
> "Because representing key values as segments could be ambiguous with other URL construction conventions, services that support key-as segment MUST implement the following precedence rules:
>
> If a segment following an entity collection:
> 1. matches a defined OData segment (starting with "$"), treat it as such
> 2. matches a qualified bound function, bound action, or type name, treat it as such
> 3. matches an unqualified bound function, bound action, or type name defined in a default namespace (see [OData-Protocol]) treat it as such
> 4. treat as a key value
>
> For maximum interoperability, services that support the key-as-segment convention SHOULD also support the canonical parentheses-style convention for addressing an entity within a collection, otherwise they MUST specify the URL for each returned entity in a response, as specified by the particular format.
>
> Note: the key-as-segment convention can only be used with the canonical (primary) key and cannot be used with alternate keys as the key property names are not present in the keys and an alternative key cannot be determined."

**MAY/SHOULD level:** Support for the convention itself is a **MAY** ("Services MAY support..."). Within that: the disambiguation precedence rules are a **MUST** once a service does support key-as-segment ("...MUST implement the following precedence rules"). Supporting the canonical parenthesized form alongside it is a **SHOULD** ("SHOULD also support the canonical parentheses-style convention"), with a fallback obligation if it doesn't: "otherwise they MUST specify the URL for each returned entity in a response."

**Required disambiguation/precedence rule:** exactly the 4-step ordered list quoted above — `$`-segment > qualified bound function/action/type-cast > unqualified bound function/action/type-cast in a default namespace > else treat as key value.

**Multi-part keys:** Explicitly covered — "the entity MUST be addressed by multiple segments applied, one for each key value, in the order they appear in the metadata description of the entity key" (e.g., `/OrderItems/1/2` for a two-part key). Referential-constraint key parts on related entities MUST be omitted from the segment chain (mirrors the parenthesized-form shortening rule in §4.3.3).

**Must services supporting it still accept parenthesized keys?** SHOULD (not MUST) support the canonical parenthesized form too — but if they choose not to, they carry a compensating MUST: they "MUST specify the URL for each returned entity in a response, as specified by the particular format" (i.e., they must supply explicit hypermedia links rather than relying on clients to construct parenthesized URLs themselves).

**Alternate keys:** Explicitly and normatively **excluded** — "the key-as-segment convention can only be used with the canonical (primary) key and cannot be used with alternate keys as the key property names are not present in the keys and an alternative key cannot be determined."

**(c) Mandated error behaviors / status codes:** SPEC SILENT — no status code prescribed for e.g. an ambiguous or malformed key-as-segment URL.

**(d) Conformance-clause references:** Appears in **Part 1 §13.2.1 OData 4.01 Minimal Conformance Level**, item 9, sub-item l (and its own sub-a):
> "9. MUST support both 4.0 and 4.01 syntax in URLs for supported functionality regardless of requested OData-MaxVersion
> ...
> l. MAY support Key-As-Segment URL convention
>    a. MUST also support canonical URL conventions (described in [OData‑URL]) or include URLs in payload"
So: support for key-as-segment itself is **MAY** at the Minimal level (nested under a top-level MUST-support-both-syntaxes item, but the sub-item itself is explicitly MAY); *if* a service does support it, sub-item "a" imposes a conditional MUST — support canonical parenthesized URL conventions **or** include URLs in the payload (this is the conformance-section restatement of the SHOULD-with-fallback-MUST from §4.3.6, but phrased here as an unconditional MUST-once-you-opt-in, i.e., the conformance clause is stricter/clearer than the base URL Conventions text's SHOULD). Not mentioned in Intermediate (§13.2.2) or Advanced (§13.2.3) conformance items, nor in any 4.0 conformance level (§13.1) — key-as-segment is a 4.01-only feature per the Part 2 Revision History appendix.

**(e) N/A** — not vocabulary-driven (no Core term governs it).

---

## 7. Alternate keys

Sources used: [OData-URL] §4.3.5; [OData-Protocol] §4.3.1/§13 (canonical URL / conformance cross-checks); [OData-VocCore] `AlternateKeys` / `AlternateKey` / `PropertyRef`

**(a) Section number and title:**
- Part 2, **§4.3.5 Alternate Keys**
- Cross-referenced: Part 2 §4.3.1 Canonical URL (for the canonical-URL question); Part 2 §4.3.6 Key-as-Segment Convention (for the explicit exclusion, see Feature 6 above)

**(b) Verbatim normative text (§4.3.5, in full):**
> "In addition to the canonical (primary) key an entity set or entity type can specify one or more alternate keys with the Core.AlternateKeys term (see [OData-VocCore]). Entities can be addressed via an alternate key using the same parentheses-style convention as for the canonical key, with one difference: single-part alternate keys MUST specify the key property name to unambiguously determine the alternate key."
>
> "Example 24: the same employee identified via the alternate key SSN, the canonical (primary) key ID using the non-canonical long form with specified key property name, and the canonical short form without key property name
> `http://host/service/Employees(SSN='123-45-6789')`
> `http://host/service/Employees(ID='A1245')`
> `http://host/service/Employees('A1245')`"

**URL syntax for single-part vs multi-part alternate keys:** The spec explicitly states the rule only for **single-part** alternate keys: "single-part alternate keys MUST specify the key property name to unambiguously determine the alternate key" — i.e., `EntitySet(PropertyName=value)`, never the bare `EntitySet(value)` short form (that short form is reserved for the canonical primary key, and is ambiguous/disallowed for a single-part alternate key). For **multi-part** alternate keys, §4.3.5's text does not add a separate sentence — it says addressing uses "the same parentheses-style convention as for the canonical key" generally, and the canonical-key convention for multi-part keys (per §4.3.1/analogous parenthesized-key syntax elsewhere in the doc) is comma-separated `name=value` pairs — so by extension a multi-part alternate key is addressed as `EntitySet(Prop1=value1,Prop2=value2)`. SPEC SILENT on an explicit standalone normative sentence for the multi-part alternate-key case (it is covered only by the general "same... convention as for the canonical key" cross-reference, not spelled out with its own example).

**Data modification through alternate-key URLs — permitted/constrained?** SPEC SILENT. Neither §4.3.5 nor any other located passage in Part 1 or Part 2 states whether PATCH/PUT/DELETE (or POST to a related/contained resource) against an alternate-key-addressed entity URL is permitted, forbidden, or constrained. No sentence restricts data-modification verbs to canonical/primary-key URLs only, and none explicitly extends modification support to alternate-key URLs either — this is simply not addressed by either document as fetched.

**Canonical-URL requirements (Location/context URL must use primary key)?** SPEC SILENT as an explicit statement about alternate keys specifically. What IS stated (Part 2 §4.3.1 Canonical URL): "the canonical form of an absolute URL identifying a non-contained entity is formed by adding a single path segment to the service root URL. The path segment is made up of the name of the entity set associated with the entity followed by **the key predicate identifying the entity within the collection**... The canonical key predicate for single-part keys consists only of the key property value without the key property name." This describes "the" canonical URL in terms of "the key predicate" (singular, definite article) without explicitly saying "primary key, not alternate key" — but since CSDL only has one `<Key>` element per entity type (the primary key; alternate keys are a separate CSDL/vocabulary-level concept layered on top via `Core.AlternateKeys`), "the key predicate identifying the entity" as used in §4.3.1 can only refer to the primary key. No sentence anywhere explicitly says "Location header / context URL / canonical URL MUST use the primary key and MUST NOT use an alternate key" — this is an inference from how "canonical URL"/"key predicate" is defined elsewhere, not a stated rule tied to alternate keys. Flagging as SPEC SILENT on the explicit cross-reference the task asked about.

**Explicit exclusion from key-as-segment:** (already quoted under Feature 6) — "the key-as-segment convention can only be used with the canonical (primary) key and cannot be used with alternate keys as the key property names are not present in the keys and an alternative key cannot be determined." This is the one place the spec explicitly contrasts alternate keys against a URL-addressing convention and rules something out.

**(c) Mandated error behaviors / status codes:** SPEC SILENT — no status code prescribed specifically for alternate-key addressing (e.g., an alternate-key value that doesn't resolve to any entity would presumably follow ordinary "entity not found" handling, but no sentence ties a specific code to this case in the fetched text).

**(d) Conformance-clause references:** SPEC SILENT / not found. Grep of the full Part 1 §13 Conformance section (all conformance levels, 4.0 and 4.01) found zero occurrences of "alternate key." It appears only in the Part 2 Revision History appendix (Committee Specification Draft 01 changelog: "Alternate keys" as a 4.01-feature bullet), which is not a conformance requirement.

**(e) Core vocabulary term (from [OData-VocCore] `Org.OData.Core.V1.xml`, lines ~479-494):**
```xml
<Term Name="AlternateKeys" AppliesTo="EntityType EntitySet NavigationProperty" Type="Collection(Core.AlternateKey)" Nullable="false">
  <Annotation Term="Core.Description" String="Communicates available alternate keys" />
</Term>
<ComplexType Name="AlternateKey">
  <Property Type="Collection(Core.PropertyRef)" Name="Key" Nullable="false">
    <Annotation Term="Core.Description" String="The set of properties that make up this key" />
  </Property>
</ComplexType>
<ComplexType Name="PropertyRef">
  <Property Type="Edm.PropertyPath" Name="Name" Nullable="false">
    <Annotation Term="Core.Description" String="A path expression resolving to a primitive property of the entity type itself or to a primitive property of a complex or navigation property (recursively) of the entity type. The names of the properties in the path are joined together by forward slashes." />
  </Property>
  <Property Type="Edm.String" Name="Alias" Nullable="true">
    <Annotation Term="Core.Description" String="A SimpleIdentifier that MUST be unique within the set of aliases, structural and navigation properties of the containing entity type that MUST be used in the key predicate of URLs" />
  </Property>
</ComplexType>
```
- Term `AlternateKeys`: Type `Collection(Core.AlternateKey)`, `Nullable="false"`, AppliesTo `EntityType EntitySet NavigationProperty`, Description "Communicates available alternate keys."
- `AlternateKey` complex type: single property `Key` of type `Collection(Core.PropertyRef)`, `Nullable="false"`, "The set of properties that make up this key" — i.e., an alternate key is a *set* of `PropertyRef`s, natively supporting multi-part alternate keys at the model level.
- `PropertyRef` complex type: `Name` (`Edm.PropertyPath`, `Nullable="false"`) — "A path expression resolving to a primitive property of the entity type itself or to a primitive property of a complex or navigation property (recursively) of the entity type. The names of the properties in the path are joined together by forward slashes"; `Alias` (`Edm.String`, `Nullable="true"`) — "A SimpleIdentifier that MUST be unique within the set of aliases, structural and navigation properties of the containing entity type that MUST be used in the key predicate of URLs." (This `Alias` is the name that appears on the left of `=` in the alternate-key URL syntax — e.g., `SSN` in `Employees(SSN='123-45-6789')` — when the underlying property path isn't itself a bare simple identifier, e.g., a path property or a nested-property alternate key.)

---

## Appendix: raw local copies used for verbatim-grep verification

Fetched and converted to plain text for exact-quote extraction (not redistributed, working files only):
- `part1.html` / `part1.txt` — OData 4.01 Part 1 Protocol
- `part2.html` / `part2.txt` — OData 4.01 Part 2 URL Conventions
- `json-format.html` / `json-format.txt` — OData JSON Format 4.01
- `Core.V1.xml` / `Core.V1.md` — OASIS Core vocabulary (raw CSDL + human-readable companion)
