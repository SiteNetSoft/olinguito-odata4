# OData 4.01 Features Guide

## Introduction

This guide covers the OData 4.01 features Olinguito adds on top of its OData 4.0 baseline,
delivered by the 4.01 compliance milestone.

**Tier 5, Wave 1:**

* [`matchesPattern` filter function](#matchespattern-filter-function)
* [`Prefer: omit-values=nulls`](#prefer-omit-values-nulls)
* [`/$query` — query options in the request body](#query---query-options-in-the-request-body)

**Tier 5, Wave 2:**

* [`$schemaversion` — requesting a specific schema version](#schemaversion--requesting-a-specific-schema-version)
* [Optional function parameters](#optional-function-parameters)

**Tier 5, Wave 3:**

* [Key-as-segment URLs](#key-as-segment-urls)
* [Alternate keys](#alternate-keys)

Each section names the governing [OASIS OData 4.01](https://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html)
clause, describes server behavior including status codes, gives a client usage snippet, and
records any deviations or spec-silent decisions made along the way. These features are additive:
with none of the new syntax/header/toggle in play, existing behavior is unchanged.

## `matchesPattern` Filter Function

Normative reference: [OData-URL] §5.1.1.7.1 (String Functions).

`matchesPattern(Edm.String, Edm.String) → Edm.Boolean` is a `$filter` string function, alongside
`contains`/`startswith`/`endswith`. It returns `true` when the first argument matches the regular
expression given as the second argument:

```
GET Products?$filter=matchesPattern(CompanyName,'^A.*e$')
```

The function name is camelCase and matched **case-sensitively** — `matchespattern(...)` or
`MATCHESPATTERN(...)` are not recognized as the function and are parsed as an (unknown) property
path instead, failing exactly like any other unrecognized identifier.

### Regex Dialect (Recorded Deviation)

The OData 4.01 spec normatively requires the second parameter to be an **ECMAScript (JavaScript)**
regular expression. Olinguito evaluates patterns with Java's `java.util.regex` instead — no
dependency on a JavaScript engine is justified for a server library. This is a deliberate,
recorded deviation: constructs that differ between the two dialects (ECMAScript-specific escapes,
lookbehind edge semantics, and so on) follow **Java regex semantics**, not ECMAScript semantics.

Matching uses `Matcher.find()` — **unanchored**, matching anywhere in the string — because neither
the spec nor `java.util.regex` implicitly anchors a pattern; anchoring is left to the pattern
itself (`^`/`$`), matching the spec's own example, which anchors explicitly.

### Server Behavior and Status Codes

* A `null` operand (property or literal) propagates as a typed null, like the sibling string
  functions — the whole `matchesPattern(...)` expression evaluates to `null`, not `false`.
* An invalid regular expression (a `PatternSyntaxException` at evaluation time) is reported as
  **400 Bad Request**. The spec is silent on this case; 400 matches how the codebase already
  treats other semantically invalid filter input.
* A non-`Edm.String` operand fails with the same 400 the other two-string-parameter functions
  (`contains`, `startswith`, and so on) already produce.

### Client Usage

```java
FilterFactory filterFactory = client.getFilterFactory();
FilterArgFactory argFactory = filterFactory.getArgFactory();

URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .filter(filterFactory.match(
        argFactory.matchesPattern(argFactory.property("CompanyName"), argFactory.literal("^A.*e$"))))
    .build();
```

`FilterArgFactory.matchesPattern(FilterArg first, FilterArg second)` follows the same two-argument
shape as `contains`/`startswith`/`endswith` and composes with `and()`/`or()` normally.

## `Prefer: omit-values=nulls`

Normative reference: [OData-Protocol] §8.2.8.6 (Preference `omit-values`); [OData-JSON] §24,
conformance item 22.

A client sends `Prefer: omit-values=nulls` on a `GET` to ask the server to drop null-valued
properties from the response body instead of writing them as JSON `null`:

```
GET Products(1) HTTP/1.1
Prefer: omit-values=nulls
```

```
HTTP/1.1 200 OK
Preference-Applied: omit-values=nulls
Content-Type: application/json

{"@odata.context":"...","ID":1,"Name":"Widget"}
```

`Description`, if null on this instance, is entirely absent from the payload — no field name, no
`@odata.type` control annotation, no `null` literal.

### What Is Omitted, and What Never Is

Only **nullable, declared, non-collection** properties whose current value is null are omitted,
subject to these carve-outs (all spec MUSTs, except where noted):

| Case | Omitted? |
|---|---|
| Null nullable declared property, no instance annotation | Yes |
| Property carrying an instance annotation, even if its value is also null | **No** — the property and the annotation are both written (§8.2.8.6 MUST) |
| Dynamic (open-type) property with a null value | **No** — spec-silent decision, see below |
| Null collection-valued declared property | **No** — a null collection serializes as `[]`, which is not a null value |
| Non-nullable property that is missing/null | **No** — still a `SerializerException`, unaffected by the preference |

Applies recursively to nested complex values, and to both `entity()` and `entityCollection()`
reads — including streamed collections.

### Where It Applies

* **Read (`GET`) responses only** — entity and entity-collection reads. This is a spec-safe
  simplification (declining a MAY, §8.2.8.6): the spec permits applying the preference on writes
  too, but requiring POST responses to still include non-default properties and PUT/PATCH
  responses to still include changed properties is trivially satisfied by never omitting on writes
  at all, with no changed-property tracking needed.
* **Never on write responses** (`POST`/`PUT`/`PATCH`) — no omission happens, and no
  `Preference-Applied` header is sent even if the client asked, which is conformant per
  [OData-JSON] §24 item 22 (the MUST NOT only binds when the preference actually applied).
* **Never on delta payloads** — §8.2.8.6 requires modified null/default properties to appear in
  delta payloads regardless of the preference; Olinguito's delta serialization path is untouched
  by `omitNulls` entirely.
* **Never on `$ref` / reference reads** — a reference payload has no properties to omit, so
  `Preference-Applied` is not echoed even if requested. The reference service gates
  `odata.track-changes` the same way on a reference collection: a `$ref` payload carries no delta
  link, so claiming the preference was applied would be a false claim (§8.2.8.6 / RFC 7240 allow
  `Preference-Applied` to name only preferences that really were applied). `odata.maxpagesize` *is*
  still echoed on `$ref` collections, because server-side paging genuinely applies there.
* `omit-values=defaults` is **accepted but not applied** (a declined MAY): the preference parses
  and would render correctly in `Preference-Applied` if it were ever set, but no code path
  currently omits default-valued properties or sends the header for it — the data layer has no
  notion of an EDM-declared instance default to compare against.
* JSON only. The XML/Atom serializer has no omit-values support.

A processor that applies the preference renders the response header with
`PreferencesApplied.with().omitValues(Preferences.OmitValues.NULLS).build()` — a typed builder
method alongside `returnRepresentation(Return)`, `maxPageSize(Integer)` and the generic
`preference(String, String)`. It lowercases the enum constant, so it renders the unquoted
`omit-values=nulls` token, byte-identical to the generic form; a `null` argument adds nothing.

### Dynamic Properties (Spec-Silent Decision)

The spec does not say whether `omit-values=nulls` should touch open-type dynamic properties.
Olinguito never omits a null dynamic property: in this codebase, an *absent* dynamic property
means "this property does not exist on the instance" (the same DELETE-removes semantics used by
[direct-path dynamic-property CRUD](open-types-guide.md#direct-path-crud-on-dynamic-properties)).
Omitting a dynamic property that has an explicit null value would be indistinguishable on the wire
from the property never having existed, silently losing information a client cannot reconstruct.
Declared properties don't have this ambiguity — their existence is fixed by `$metadata` — so only
declared nulls are eligible for omission.

### Client Usage

```java
ODataEntityRequest<ClientEntity> request = client.getRetrieveRequestFactory()
    .getEntityRequest(entityUri);
request.setPrefer(client.newPreferences().omitValuesNulls());

ODataRetrieveResponse<ClientEntity> response = request.execute();
// response.getHeader("Preference-Applied") contains "omit-values=nulls" when honored
ClientEntity entity = response.getBody();

// A declared property the server omitted reads back as a plain Java null,
// exactly like a $select-excluded property — not a ClientProperty with hasNullValue()==true.
ClientProperty description = entity.getProperty("Description");
```

`ODataPreferences.omitValuesNulls()` returns the literal token `omit-values=nulls`; pass it to
`ODataRequest.setPrefer(String)`. This preference is an OData 4.01 addition — it has no effect
against a 4.0-only service.

## `/$query` — Query Options in the Request Body

Normative reference: [OData-URL] §4.17 (Passing Query Options in the Request Body).

A request path ending in `/$query` lets a client submit a (potentially very long) `$filter`/
`$select`/other query-option string as a request body instead of a URL query string — useful when
the encoded query would exceed a URL length limit:

```
POST People/$query HTTP/1.1
Content-Type: text/plain

$filter=[FirstName,LastName]%20in%20[["John","Doe"],["Jane","Smith"]]
```

is processed exactly like:

```
GET People?$filter=[FirstName,LastName]%20in%20[["John","Doe"],["Jane","Smith"]] HTTP/1.1
```

### Server Behavior and Status Codes

Olinguito intercepts a `/$query` path early in request processing, before URI parsing, and
rewrites the request in place as the equivalent `GET`:

1. The `/$query` path segment is stripped from both the raw OData path and the raw request URI.
2. The request body (read as UTF-8 `text/plain`) and any query string already on the URL are
   **merged** — per the spec's "processed together" wording — and the merged option string
   replaces the request's query. An empty body behaves exactly like a plain `GET` with only the
   URL's own query options.
3. The rewritten request is dispatched as `GET`, including next-link generation for a paginated
   collection: the generated `@odata.nextLink` reflects the stripped path and merged query, not the
   original `/$query` POST.

Status codes, all spec-silent and chosen to match existing codebase conventions:

| Condition | Status |
|---|---|
| Non-`POST` verb on a `/$query` path | **405 Method Not Allowed** |
| Missing or non-`text/plain` `Content-Type` | **415 Unsupported Media Type** (the first use of 415 in this codebase) |
| Same query option present in both the URL and the body | **400 Bad Request** — the pre-existing duplicate-system-query-option error; "processed together" mandates merging the two sources, not silently resolving a conflict between them |
| Malformed body content (bad percent-encoding, literal whitespace, and so on) | **400 Bad Request**, via the ordinary query-option parser — no special-cased validation is added for the body beyond what the parser already rejects |

`/$query` is scoped to GET-style read requests. A `POST $batch/$query` is rejected with 405 —
after rewrite it becomes a `GET $batch`, which the batch dispatcher's own POST-only check rejects
in the ordinary way (batch requests are POST-only regardless of `/$query`).

### Client Usage

```java
client.getConfiguration().setUseQueryPostRequest(true);

URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .filter("Price gt 100")
    .build();

ODataEntitySetRequest<ClientEntitySet> request = client.getRetrieveRequestFactory()
    .getEntitySetRequest(uri);
ODataRetrieveResponse<ClientEntitySet> response = request.execute();
```

With `isUseQueryPostRequest()` set, any retrieve request whose URI carries a query string is sent
as `POST {path}/$query` with the query string moved into a `text/plain` body (UTF-8-encoded,
`Content-Type: text/plain` set at request construction) instead of `GET {path}?{query}`. A
query-less URI is unaffected and still goes out as a plain `GET`. The toggle is honored uniformly
across the synchronous request path, the async request wrapper, and batch part serialization — all
three read the same request state, so a `/$query` retrieve request embedded in a `$batch` carries
the correct `Content-Type: text/plain` part header too. The default is `false` (plain `GET`
behavior, unchanged).

## `$schemaversion` — Requesting a Specific Schema Version

Normative reference: [OData-Protocol] §11.2.12 (`$schemaversion`); conformance §13.2.1 Minimal
item 5 (a service that returns `Core.SchemaVersion` in `$metadata` MUST reject an incompatible
`$schemaversion`).

A client that was built against a particular version of a service's schema can pin that version on
any request. The value is either a version the service previously published through the
`Core.SchemaVersion` annotation in `$metadata`, or `*` meaning "whatever the current version is":

```
GET $metadata?$schemaversion=1.0.0
GET ESAllPrim?$schemaversion=*
```

The option is a system query option like any other — it parses, validates and is exposed on
`UriInfo` as `SchemaVersionOption`:

```java
SchemaVersionOption option = uriInfo.getSchemaVersionOption();   // null when not specified
String requested = option == null ? null : option.getSchemaVersion();
```

`getSchemaVersionOption()` is declared once on `UriInfo` itself rather than on each kind-specific
sub-interface (the way `getFormatOption()` is), because §11.2.12 allows the option on *any*
request — a handler holding a plain `UriInfo` never has to narrow to read it.

### Publishing a Schema Version

A service publishes its version as a schema-level `Core.SchemaVersion` annotation and hands the
same value to the `ServiceMetadata` it builds:

```java
ServiceMetadata serviceMetadata = odata.createServiceMetadata(
    new MyEdmProvider(), Collections.emptyList(), null, "1.0.0");
```

The four-argument `OData.createServiceMetadata(CsdlEdmProvider, List<EdmxReference>,
ServiceMetadataETagSupport, String schemaVersion)` overload is additive and concrete — third-party
`OData` subclasses keep compiling (the default body throws `UnsupportedOperationException`, and
`ODataImpl` overrides it). `new ServiceMetadataImpl(provider, references, eTagSupport,
schemaVersion)` is the equivalent direct-construction path. The version is readable back through
`ServiceMetadata.getSchemaVersion()` (`null` for an unversioned service).

The reference service (tecsvc) is versioned `1.0.0` — a single source of truth,
`SchemaProvider.SCHEMA_VERSION`, is both wired into `createServiceMetadata` and emitted as the
annotation, so the published and the enforced version cannot drift. In XML metadata a constant
annotation is written as a child element, not an attribute:

```xml
<Schema Namespace="olingo.odata.test1" Alias="Namespace1_Alias">
  ...
  <Annotation Term="Core.SchemaVersion"><String>1.0.0</String></Annotation>
</Schema>
```

and in JSON metadata as `"@Core.SchemaVersion":"1.0.0"` on the schema object.

### Server Behavior and Status Codes

| Condition | Result |
|---|---|
| `$schemaversion=*` | Always matches — the current version is served |
| Requested version equals the service's version | Served normally |
| Requested version differs from the service's version | **404 Not Found**, with an explanatory error body naming the requested version (`Schema version '9.9.9' does not exist.`) — spec MUST |
| Service publishes no schema version | Option is **accepted and ignored** (recorded decision, see below) |
| Empty value (`$schemaversion=`) | **400 Bad Request** — `WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION`, like every other valueless system query option |
| Option specified twice | **400 Bad Request** — the standard duplicate-system-query-option error |
| `$SchemaVersion=...` (any other casing) | **400 Bad Request** — `UNKNOWN_SYSTEM_QUERY_OPTION`; system query options are case-sensitive lower-case tokens |

Validation runs in `ODataHandlerImpl` after URI parsing and validation but **before** dispatch, so
no processor ever sees a request for a version the service cannot serve.

**Unversioned services accept and ignore (recorded decision).** §13.2.1 Minimal item 5 makes the
rejection MUST conditional on the service actually returning `Core.SchemaVersion` in `$metadata`.
Reading §11.2.12's 404 clause as forcing an unversioned service to reject every `$schemaversion`
would contradict that conditionality and would break existing consumers, so a service with no
version source performs syntax validation only. Services that do publish a version are bound by the
MUST, and tecsvc is one of them.

### Where the Option Is Allowed

Because §11.2.12 permits the option on any request, `$schemaversion` is valid for every URI type,
including `$batch` and media-stream (`$value`) endpoints, which accept no other system query option
at all. It is also **exempt from the non-read query-option rejection** in `UriValidator`: system
query options are otherwise rejected outright on `POST`/`PUT`/`PATCH`/`DELETE`, which would have
made `POST $batch?$schemaversion=...` a 400 and put batch inheritance out of reach. The exemption is
narrow — any *other* disallowed option travelling next to `$schemaversion` on a write is still
rejected exactly as before.

The option also works through a Wave 1 `/$query` POST body, since `/$query` is rewritten to the
equivalent `GET` before URI parsing:

```
POST $metadata/$query HTTP/1.1
Content-Type: text/plain

$schemaversion=*
```

### Batch Requests

Per §11.2.12, a batch part that carries its own `$schemaversion` uses it; a part that does not
**inherits the outer `$batch` request's** value (the batch handler pre-injects the outer value into
the part's raw query before the part is re-parsed, because parts are parsed independently with no
link back to the envelope).

One consequence is worth stating explicitly: the outer `$batch` request is itself a request, so it
is version-checked before dispatch. An unknown version on the `$batch` URL therefore **404s the
whole batch envelope** — there is no multipart body in which per-part results could be reported.
A part's own unknown version, by contrast, produces a `404` for that part inside an otherwise
`200` batch response.

### Client Usage

```java
URI uri = client.newURIBuilder(serviceRoot)
    .appendMetadataSegment()
    .schemaVersion("1.0.0")     // or "*"
    .build();
// http://host/service/$metadata?%24schemaversion=1.0.0
```

`URIBuilder.schemaVersion(String)` sets `QueryOption.SCHEMAVERSION`, replacing any previously set
value, exactly like `format(String)`. A version the service does not know surfaces on the client as
an `ODataClientErrorException` with status 404.

## Optional Function Parameters

Normative reference: [OData-Protocol] §11.5.4.1.1 (optional parameters), §11.5.4.2 (overload
resolution), §11.5.5.1 (action parameter bodies); Core vocabulary terms `Core.OptionalParameter` and
the `Core.OptionalParameterType` complex type (`DefaultValue`).

A function parameter annotated with `Core.OptionalParameter` may be omitted by the caller. With a
`DefaultValue` in the annotation, an omitted parameter takes that value; without one, the parameter
is simply absent and its interpretation is left to the service:

```xml
<Function Name="UFCRTStringOptionalParam" IsComposable="true">
  <Parameter Name="ParameterString" Type="Edm.String" Nullable="false"/>
  <Parameter Name="ParameterSuffix" Type="Edm.String">
    <Annotation Term="Core.OptionalParameter">
      <Record>
        <PropertyValue Property="DefaultValue" String="'-default'"/>
      </Record>
    </Annotation>
  </Parameter>
  <ReturnType Type="Edm.String"/>
</Function>
```

```
GET FICRTStringOptionalParam(ParameterString='base')                      → "base-default"
GET FICRTStringOptionalParam(ParameterString='base',ParameterSuffix='-x') → "base-x"
```

### `DefaultValue` Is a URI Literal

`Core.OptionalParameter`'s `DefaultValue` is typed `Edm.String` but holds the value "using the same
rules as the `cast` function in URLs" — that is, a **URI literal**, not a plain value. A string
default is therefore quoted (`'-default'`), an enum default is type-prefixed
(`olingo.odata.test1.ENString'String1'`), and a numeric default is bare (`42`). Both injection paths
below run the annotation value through `fromUriLiteral` before using it.

### EDM Surface

```java
EdmParameter parameter = function.getParameter("ParameterSuffix");
boolean optional  = parameter.isOptional();               // default false
String  defaulted = parameter.getOptionalDefaultValue();  // URI literal, or null
```

Both are `default` methods on `EdmParameter`, so existing implementors are unaffected.
`EdmParameterImpl` reads the annotation by raw term name first (`Org.OData.Core.V1.OptionalParameter`
or the `Core.OptionalParameter` alias) and only then by resolved term, so optionality is detected
even when the service does not itself serve the Core vocabulary. Both inline `<Annotation>` elements
on the parameter and out-of-line `Annotations Target="Ns.Function/ParamName"` forms are honored.

### Overload Resolution

Function overload selection follows §11.5.4.2 in two steps:

1. **Exact match** — an overload whose non-binding parameter names are exactly the specified set
   wins immediately, regardless of declaration order. This is the pre-4.01 behavior, unchanged.
2. **Covering match** — otherwise, an overload qualifies when it declares every specified parameter
   *and* every parameter it declares that was not specified is optional. If exactly one overload
   qualifies, it is selected.

| Outcome | Result |
|---|---|
| One covering overload | Resolved and invoked |
| No overload covers the specified set (e.g. a required parameter omitted, or an unknown parameter name) | **400 Bad Request** — `FUNCTION_NOT_FOUND`, exactly as before 4.01 |
| More than one overload covers the specified set | **400 Bad Request** — `FUNCTION_AMBIGUOUS`: *The function '…' with parameters '…' matches more than one function overload.* (adopting the §11.5.4.2 MAY) |

Ambiguity is raised as `EdmAmbiguousOverloadException` (a new, additive `EdmException` subclass in
`commons-api`) and translated to a `UriParserSemanticException` at every function-resolution site in
the URI parser — resource paths, `$filter`/`$orderby` expressions, `$apply`, and `$select` bound
operations. Every *other* `EdmException` from function resolution keeps propagating as a genuine
model error (500), unchanged.

**The optional-after-required CSDL rule is a provider-authoring rule, not a matcher check
(deviation).** §11.5.4.1.1 requires optional parameters to be declared after all non-optional ones,
and the binding parameter of a bound function must never be optional. The overload matcher is
order-independent and does not validate that authoring rule — a non-compliant provider still
resolves. The binding parameter is structurally excluded from the covering set, so marking it
optional has no effect.

Actions have no overloads at all; optional parameters affect them only through the request-body
rules below.

### Applying Default Values

**URL-invoked functions — the service applies defaults.** `server-core` does *not* inject default
values into the parameter list a processor receives; it exposes `isOptional()` /
`getOptionalDefaultValue()` and leaves materialization to the service. The reference service shows
the pattern in `DataProvider.getFunctionParameters`: after reading the URL-supplied parameters, walk
`function.getParameterNames()`, skip the ones already present, and for each remaining parameter with
`isOptional() && getOptionalDefaultValue() != null`, parse the default through the very same
fixed-format deserializer the URL values go through:

```java
odata.createFixedFormatDeserializer().parameter(parameter.getOptionalDefaultValue(), parameter);
```

An omitted optional parameter *without* a default is left absent. A `DefaultValue` that is not a
valid URI literal for the parameter's type is rejected by the **URI parser**, before the service is
ever reached, **for function calls in the resource path** — function imports and bound functions
addressed as a path segment: the parser walks the optional parameters that the URL did not supply
and raises `UriValidationException` / `INVALID_VALUE_FOR_PROPERTY` — **400 Bad Request**, the same
status the action-body path already returned. The check only looks at parameters the URL omitted,
so a malformed default is irrelevant when the caller supplies the parameter explicitly.

Functions invoked **inside a `$filter` or `$orderby` expression** are *not* checked at parse time —
the expression parser does not run the optional-parameter facet validation — so a malformed default
there falls through to the service, which is where it is caught. That is why the reference service
keeps its own 400 on the same condition (`DataProvider.addOptionalParameterDefaults`); it is not
merely defense in depth for callers that bypass the parser, it is the only check on that path.

Both the URL path and the action-body path share one resolver,
`OptionalParameterDefaults` in `server-core`'s `uri.parser` package, which owns reading the
annotation literal, deciding whether the parameter type can take a default (primitive, type
definition or enum, non-collection), and running the literal through `fromUriLiteral`. Each call
site keeps its own exception translation (`UriValidationException` in the parser,
`DeserializerException` in the two deserializers).

**Action bodies — defaults are injected by the JSON deserializer.** For actions, the JSON parameter
deserializer applies a `DefaultValue` when a parameter is **omitted** from the body, for primitive
and enum (non-collection) parameters:

```json
{"ParameterString":"base"}
```

yields `ParameterSuffix = "-default"` alongside the supplied `ParameterString`. Two behaviors are
pinned deliberately:

* **An explicit JSON `null` is not an omission.** `{"ParameterSuffix":null}` leaves the parameter
  null; the default is applied only when the member is absent from the body entirely. §11.5.5.1's
  omission rules govern omission, and a client that writes `null` said something.
* **An omitted optional parameter with no `DefaultValue` stays null/absent** — the service-free
  interpretation, not an error.

An omitted *non-nullable, non-optional* parameter still fails with `INVALID_NULL_PARAMETER`,
unchanged. A malformed `DefaultValue` literal surfaces as `INVALID_VALUE_FOR_PROPERTY`. Complex and
collection-typed defaults are not applied in either path — the Core vocabulary types `DefaultValue`
as `Edm.String`, and such a parameter falls back to the ordinary omitted-parameter handling.

### Client Usage

No new client API is needed: the invoke-request factory already sends whatever parameter subset the
caller supplies, so omitting an optional parameter is just leaving it out of the map.

```java
Map<String, ClientValue> parameters = new HashMap<>();
parameters.put("ParameterString", factory.newPrimitiveValueBuilder().buildString("base"));
// ParameterSuffix deliberately omitted — the service applies its DefaultValue

ODataInvokeRequest<ClientProperty> request = client.getInvokeRequestFactory()
    .getFunctionInvokeRequest(
        client.newURIBuilder(serviceRoot)
            .appendOperationCallSegment("FICRTStringOptionalParam").build(),
        ClientProperty.class, parameters);

ODataInvokeResponse<ClientProperty> response = request.execute();
// "base-default"
```

Omitting a *required* parameter fails the overload match and surfaces as an
`ODataClientErrorException` with status 400.

## Key-as-Segment URLs

Normative reference: [OData-URL] §4.3.6 (Key-as-Segment Convention); conformance §13.2.1 item 9.l
(supporting the convention is a MAY; a service that does support it must also support canonical
parenthesized URLs — Olinguito always does).

The key-as-segment convention addresses an entity by appending its key value as an ordinary path
segment instead of a parenthesized predicate:

```
GET ESAllPrim/32767      ≡  GET ESAllPrim(32767)
GET ESTwoKeyNav/1/abc    ≡  GET ESTwoKeyNav(PropertyInt16=1,PropertyString='abc')
```

Because §4.3.6 changes how *every* segment following an entity collection is interpreted, the
convention is **opt-in per service and off by default**. With it off, every URI parses exactly as it
did before — pinned by tests.

```java
ODataHandler handler = odata.createHandler(serviceMetadata);
handler.setKeyAsSegment(true);      // whole-service switch, default false
```

`setKeyAsSegment(boolean)` is an additive `default` method on `ODataHandler` whose base implementation
throws `UnsupportedOperationException`; `ODataHandlerImpl` implements it, and both
`ODataRequestHandlerImpl` (server-core) and `ODataNettyHandlerImpl` (server-adapter-netty) forward it
to the handler they wrap. There is no other server-side configuration object to thread it through.

### Segment Precedence

A segment following an entity **collection** is resolved by the four §4.3.6 rules, in order:

| # | Segment shape | Interpretation |
|---|---|---|
| 1 | `$`-prefixed OData segment (`$count`, `$ref`, `$value`, …) | The OData segment. A `$…` segment is *never* a key value — including in the middle of a multi-part key, where it is rejected as an invalid key value rather than quoted into a string |
| 2 | Qualified name of a bound function/action or of a derived type | Bound operation or type cast |
| 3 | Unqualified name of a bound function/action or type declared in a schema annotated `Core.DefaultNamespace` | Bound operation or type cast |
| 4 | Anything else | Key value |

Rule 3 is opt-in per schema: the schema must carry a `Core.DefaultNamespace` annotation, and the
unqualified name must actually resolve *in that namespace* to a bound operation that binds to the
current collection, or to an entity type. Otherwise the segment falls through to rule 4 and is read as
a key value.

The precedence applies to entity **collections** only — entity sets and collection-valued navigation
properties. A segment after a single entity is a property or navigation segment as before, and an
entity that already carries key predicates is never a key-as-segment target, so
`ESAllPrim(32767)/PropertyString` and `ESAllPrim/32767/PropertyString` behave identically.

### Multi-Part Keys

A compound key takes one segment per key value, **in metadata key order** (the order key properties
are declared in the CSDL, using key aliases where the model declares them):

```
GET ESTwoKeyNav/1/abc
GET ESKeyNav/1/NavPropertyETKeyNavMany/2/NavPropertyETTwoKeyNavMany/abc
```

A type filter in front of the key selects the type whose key is addressed
(`ESTwoKeyNav/olingo.odata.test1.ETBaseTwoKeyNav/1/abc`).

A key that runs out of segments before it is complete is a **400 Bad Request**
(`WRONG_NUMBER_OF_KEY_PROPERTIES` — *There are 1 key properties instead of the expected 2.*), reported
when the resource path ends.

### Referential Constraints

Key properties of a related entity that a referential constraint already fixes MUST be omitted from a
key-as-segment URL, so a constrained navigation consumes only the segments for the remaining key
properties:

```
GET ESKeyNav(1)/NavPropertyETTwoKeyNavMany('1')   parenthesized: constrained key omitted
GET ESKeyNav/1/NavPropertyETTwoKeyNavMany/1       key-as-segment: one segment, same key
```

The omitted values are prefilled by the parser as referenced-property predicates, exactly as the
parenthesized form does. A consequence: a constraint-covered key value can no longer be *supplied* as
a segment (there is no way to tell it apart from the next path segment) — supplying an extra segment
is a parse error. The parenthesized form still accepts both shapes.

### Key Values in Segments

* Values are written **unquoted**: `ESAllPrim/32767`, `Categories/Smartphone`. Single quotes inside a
  value are **literal** characters, not delimiters (`People/O'Neil` addresses the key `O'Neil`).
* A forward slash inside a key value MUST be percent-encoded (`Categories/Smartphone%2FTablet`);
  after decoding it is part of the key value, not a path separator.
* Type conversion and facet validation follow the key property's EDM type, exactly as for the
  parenthesized form.

### Server Behavior and Status Codes

| Condition | Result |
|---|---|
| Flag off, `ESAllPrim/32767` | Unchanged pre-4.01 behavior — a syntax error, or `PROPERTY_AFTER_COLLECTION` for an identifier-shaped segment (**400**) |
| Flag on, complete key | Resolved like the parenthesized address; response bodies are byte-identical apart from the relative context URL (`../$metadata#ESAllPrim/$entity`, one path level deeper) |
| Incomplete multi-part key | **400 Bad Request** — `WRONG_NUMBER_OF_KEY_PROPERTIES` |
| Value of the wrong type, or out of range for the property's facets | **400 Bad Request** — `INVALID_KEY_VALUE` / `INVALID_KEY_PROPERTY`, the same errors the parenthesized form raises |
| `$`-segment where a key value was expected | **400 Bad Request** — `INVALID_KEY_VALUE` |
| Parenthesized address on a key-as-segment service | Still works — both conventions are live at once |

Enabling the flag changes the *error kind* for malformed URLs on every collection
(`ESAllPrim/PropertyString` becomes an invalid key value instead of "property after collection"). That
is what rule 4 mandates, and it only happens for a service that opts in.

### Model-Level Key-as-Segment Flags (Pre-Existing, Non-Standard)

The fork already carried `CsdlEntitySet.setKeyAsSegmentAllowed(boolean)` and
`CsdlNavigationProperty.setKeyAsSegmentAllowed(boolean)`, which enable segment keys for a single
entity set or navigation property. They are **not** part of §4.3.6 and are kept for compatibility: a
segment is treated as a key when the service flag **or** the model flag applies. Two differences from
the standard path:

* Multi-part model-flagged sets now work (`ESComplexKeyAsSegment/thisIsAKey/42`), and an incomplete
  key reports `WRONG_NUMBER_OF_KEY_PROPERTIES` instead of `PROPERTY_AFTER_COLLECTION`.
* A model-flagged **single-valued** navigation property keeps its old *fallback* semantics: a segment
  is resolved as a property or navigation property first, and only a name that is not a member of the
  target type is taken as a key. The standard service flag never does this — it applies to entity
  collections only, per the spec.

### Reference Service

The technical service is deployed twice: unchanged at `/odata.svc`, and with the flag on at
`/odata-kas.svc` (`http://localhost:9080/odata-server-tecsvc/odata-kas.svc` in the integration tests).
The servlet deployment adds a `configureHandler(ODataHandler)` hook on `TechnicalServlet` that
`TechnicalKeyAsSegmentServlet` overrides; the Quarkus deployment registers a second route with a
handler constructed with `keyAsSegment = true`.

A request whose key is completed by a referential constraint is served in both conventions:
`ESKeyNav(1)/NavPropertyETTwoKeyNavMany('1')` and `ESKeyNav/1/NavPropertyETTwoKeyNavMany/1` both
return **200** with `ESTwoKeyNav(PropertyInt16=1,PropertyString='1')`. tecsvc's `DataProvider` takes
the constraint-covered value from the entity the navigation started at and compares the two typed
model values directly, instead of round-tripping a predicate that carries no literal through the
URI-literal path (which used to fail with 400 `Wrong key!`). A remaining segment that matches no
related entity is a plain **404**.

### Client Usage

```java
client.getConfiguration().setKeyAsSegment(true);

URI single = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("ESAllPrim")
    .appendKeySegment(32767)
    .build();                                   // .../ESAllPrim/32767

Map<String, Object> key = new LinkedHashMap<>();  // metadata key order
key.put("PropertyInt16", 1);
key.put("PropertyString", "1");

URI compound = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("ESTwoKeyNav")
    .appendKeySegment(key)
    .build();                                   // .../ESTwoKeyNav/1/1
```

* `appendKeySegment(Object)` writes a string value **raw** — no surrounding quotes and no doubled
  embedded quote. Non-string values keep their URI-literal formatting minus the quoted form a segment
  never carries: `Edm.Duration`, `Edm.Binary` and enumeration values lose their type prefix
  (`duration'P1D'` → `P1D`, `binary'0a0b'` → `0a0b`, `ns.Enum'A'` → `A`), because the server builds the
  literal from the segment text again; numbers, `Edm.Guid` and `Edm.DateTimeOffset` are unchanged.
  `appendKeySegment(EdmEnumType, String)` emits the bare member name in key-as-segment mode.
* `appendKeySegment(Map<String, Object>)` writes one segment per map *value*, in map iteration order —
  the key names are not part of the URL, so pass a `LinkedHashMap` in metadata key order.
* String key values are percent-encoded per RFC 3986 `pchar`: unreserved characters, the sub-delimiters
  `!$&'()*+,;=`, `:` and `@` stay literal (so `'` is literal per §4.3.6), while `/`, `?`, `#`, `%`,
  `[`, `]`, space, control characters and non-ASCII are encoded (`Smartphone/Tablet` →
  `Smartphone%2FTablet`, `Ünïcode` → `%C3%9Cn%C3%AFcode`).
* A percent-encoded slash (`%2F`) in a key segment may never reach the OData handler: servlet
  containers reject or normalize it by default (Tomcat's `ALLOW_ENCODED_SLASH` is off), so a service
  whose keys can contain `/` needs the container configured accordingly, or the parenthesized form.
* An empty string key throws `IllegalArgumentException` — an empty segment would silently address a
  different resource. A `null` value renders the literal `null` segment, as the parenthesized form
  does; a null or empty map adds no segment.
* `appendKeySegment(Map<String, Map.Entry<EdmEnumType, String>>, Map<String, Object>)` throws
  `IllegalStateException` in key-as-segment mode: its two maps carry no combined ordering, so no
  correct segment order can be derived. Use the single-map overload with
  `EdmEnumType.toUriLiteral(...)` values instead.

With the configuration flag off, both overloads emit the parenthesized form exactly as before.

### Recorded Deviations and Limitations

* **`Core.DefaultNamespace` is matched by term name only** (`Org.OData.Core.V1.DefaultNamespace` or the
  `Core.DefaultNamespace` alias), using the raw annotation term text. The term is deliberately **not**
  added to the trimmed Core vocabulary the fork ships, so the annotation works without the service
  serving the vocabulary; a service that aliases the Core vocabulary to some *other* prefix is not
  recognized.
* Resolving an unqualified segment calls `edm.getSchemas()`, which materializes the whole EDM. It only
  happens when key-as-segment is effective and the segment is a bare identifier, and the resulting list
  of default namespaces is computed once per parsed URI.
* `UriHelper.parseEntityId(...)` honors the convention when the helper is built for it:
  `OData.createUriHelper(boolean keyAsSegment)` (a concrete overload defaulting to
  `createUriHelper()`) and the concrete `UriHelperImpl.setKeyAsSegment(boolean)` set the flag the
  parser then uses. The `UriHelper` interface itself is unchanged, so no implementor breaks. The
  reference service does not wire this into its key-as-segment endpoint: the `DataProvider` that
  calls `parseEntityId` is created once per session and shared by both deployed endpoints, while the
  opt-in lives on the per-request handler.
* `server-core-ext` remains a documented seam: that module contains no `ODataHandler`, so there is no
  service-level flag to thread — `ServiceRequest` and `ServiceDispatcher` build their own `Parser`
  without it, and an entity id or link written in key-as-segment form is not parsed there. The
  per-model `keyAsSegmentAllowed` flags still work in that module, because `ResourcePathParser`
  consults them independently of the service flag.
* A key value covered by a referential constraint cannot be *supplied* as a segment — the parser
  prefills it, so it MUST be omitted from the URL (see above).

## Alternate Keys

Normative reference: [OData-URL] §4.3.5 (Addressing Entities by Alternate Key); Core vocabulary terms
`Core.AlternateKeys`, `Core.AlternateKey` and `Core.PropertyRef`.

An alternate key is a second, equally unique way to address an entity — a natural key such as an
e-mail address or an SSN — declared as a `Core.AlternateKeys` annotation on an entity type or an
entity set:

```xml
<Annotation Term="Core.AlternateKeys">
  <Collection>
    <Record Type="Core.AlternateKey">
      <PropertyValue Property="Key">
        <Collection>
          <Record Type="Core.PropertyRef">
            <PropertyValue Property="Name" PropertyPath="PropertyString"/>
            <PropertyValue Property="Alias" String="StringPart"/>
          </Record>
          <Record Type="Core.PropertyRef">
            <PropertyValue Property="Name" PropertyPath="PropertyGuid"/>
          </Record>
        </Collection>
      </PropertyValue>
    </Record>
  </Collection>
</Annotation>
```

The `AlternateKeys` term and the `AlternateKey`/`PropertyRef` complex types are now part of the Core
vocabulary the fork serves (`lib/odata-vocabularies`, `lib/server-core-ext`, `lib/test-fixtures` and
the fit copy).

### Addressing

Alternate keys use the same parenthesized predicate syntax as the canonical key, and a single-part
alternate key MUST name its property — the bare short form is always the primary key:

```
GET ESAllPrim(PropertyString='Employee1@company.example')            alternate key
GET ESAllPrim(StringPart='First Resource - positive values',
              PropertyGuid=01234567-89ab-cdef-0123-456789abcdef)     aliased multi-part alternate key
GET ESAllPrim(32767)                                                 always the primary key
```

`Alias`, when present, is the name used in the URL; `Name` stays the property the value addresses.

### EDM Surface

```java
List<EdmAlternateKey> groups = edmEntityType.getAlternateKeys();   // and EdmEntitySet.getAlternateKeys()
for (EdmAlternateKeyPropertyRef ref : groups.get(0).getPropertyRefs()) {
  ref.getName();      // declared property name
  ref.getAlias();     // alias, or null
  ref.getUrlName();   // alias when present, else name — the name used in the URL
  ref.getProperty();  // resolved EdmProperty, or null when it cannot be resolved
}
```

Both `getAlternateKeys()` methods are `default` methods returning `List.of()`, so existing EDM
implementations are unaffected. Annotations are matched by raw term name first
(`Org.OData.Core.V1.AlternateKeys` / `Core.AlternateKeys`) and only then by resolved term, so a service
that does not serve the Core vocabulary still gets its alternate keys read.

A group is **skipped** — silently, rather than failing the model — when it is malformed (no `Key`, a
`Key` that is not a collection, an empty collection, a `PropertyRef` without a usable `Name`), or when
its references do not have unique URL names (including an `Alias` that shadows another reference's
`Name`). Alternate keys are read from the annotations of the type or set itself and are **not
inherited from base entity types**.

### Parser Rules

A name=value predicate set that is not the primary key is matched against the candidate groups:

* Candidates are the entity type's own groups plus, on the **leading entity-set segment**, the entity
  set's groups. Navigation, type-cast and function-return key predicates see type-level groups only.
* The set of names in the URL must match **exactly one** candidate group, completely. Order in the URL
  does not matter; the parsed predicates are returned in the group's declared order.
* Primary key names always win: a group whose URL names equal the primary key's names is not a
  candidate, and the bare short form `ESAllPrim(32767)` is never an alternate key.
* A group containing a reference that cannot be resolved to a single primitive property — notably a
  nested path such as `Address/City`, which is **out of scope for this milestone** — is dropped from
  the candidates.
* Alternate keys are not attempted on a navigation predicate covered by a **referential constraint**:
  there the URI supplies only the remaining part of the *primary* key, whereas an alternate key always
  addresses the entity completely.
* **Key-as-segment never resolves alternate keys** (§4.3.6's exclusion) — a segment key is always the
  canonical key.

Processors read which property a predicate addresses from the parsed `UriParameter`:

```java
String urlName  = key.getName();                       // the URL name — alias when aliased
String property = key.getAlternateKeyPropertyName();    // the entity-type property, null for a primary key
```

`getAlternateKeyPropertyName()` is an additive `default` method on `UriParameter` returning `null`, so
a primary-key predicate and every existing implementation are unchanged.

### Server Behavior and Status Codes

| Condition | Result |
|---|---|
| Complete match of exactly one alternate-key group | Resolved; the request proceeds as if addressed by the primary key |
| Name set matching no group (or more than one) | Unchanged pre-4.01 errors — **400 Bad Request** (`Unknown key property …`, `WRONG_NUMBER_OF_KEY_PROPERTIES`, …) |
| Partial group (`ESAllPrim(StringPart='…')` of a two-part group) | **400 Bad Request**, as an unmatched name set |
| Value of the wrong type for an alternate-key property | **400 Bad Request** — `INVALID_KEY_VALUE`. Once a URL name resolves to an alternate-key reference the alternate interpretation is the only one that can succeed, so the parser fails loudly rather than falling back |
| Duplicate URL name in the predicate | **400 Bad Request** — `DOUBLE_KEY_PROPERTY`, as for the primary key |
| No entity has the given alternate-key value | **404 Not Found** |

### Writes and Canonical URLs (Spec-Silent Decisions)

The spec is silent on writing through an alternate-key URL. Since alternate-key addressing resolves to
the same entity, **GET, PATCH and DELETE all work through alternate keys** in the reference service.

Response payloads always identify entities by their **primary** key: `@odata.id`, edit links,
navigation links and the `Location` header of a POST are built from the entity itself
(`UriHelper.buildKeyPredicate(EdmEntityType, Entity)`), never from the request predicate. So
`GET ESAllPrim(PropertyString='Employee1@company.example')` answers with `"@odata.id":"ESAllPrim(10)"`,
matching §4.3.1's definition of the canonical URL. Property-level `@odata.context` URLs in tecsvc are
built the same way and also show the primary key (`$metadata#ESAllPrim(10)/PropertyInt16`).

Downstream services should note that `UriHelper.buildContextURLKeyPredicate(List<UriParameter>)` builds
its key path from the *request* predicates, so a service that feeds it the parsed key predicates emits
context URLs carrying the alternate-key names. It was left unchanged; tecsvc builds its property-level
context URLs from the entity instead (`UriHelper.buildKeyPredicate(EdmEntityType, Entity)`), and only
`server-core-ext`'s `DataRequest` uses the request-predicate form.

### Reference Service

`ESAllPrim` carries two set-level groups — `[{PropertyString}]` and
`[{PropertyString Alias StringPart}, {PropertyGuid}]` — and `ETKeyNav` carries the type-level group
`[{PropertyString}]`. `DataProvider` resolves both primary and alternate keys through one matching
path, so every entity-set read/write site honors them.

### Client Usage

No new client API is needed — the URI builder already emits named key predicates:

```java
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("ESAllPrim")
    .appendKeySegment(Collections.singletonMap("PropertyString", "Employee1@company.example"))
    .build();
// http://host/service/ESAllPrim(PropertyString='Employee1@company.example')
```

The returned entity's id and edit link carry the primary key, so a follow-up write through
`getUpdateRequest(entity.getEditLink(), …)` addresses the canonical URL.

### Limitations

* Nested `PropertyRef` paths (`Address/City`) are out of scope: such a group is dropped and the
  predicate fails as an unmatched name set.
* Set-level groups apply on the leading entity-set segment only. A navigation, type-cast or
  function-return predicate resolves type-level groups only, because the binding target is not
  available at those call sites.
* Alternate keys are not inherited from base entity types.
* Identical alternate-key groups declared on both the entity type and the entity set count as one; two
  groups that map the same URL name to *different* properties are ambiguous and never resolve.
* A `null` value is rejected as an alternate-key value (`INVALID_KEY_VALUE`) even for a nullable
  property, because it can never identify an entity.
* Alternate keys are never resolvable through key-as-segment URLs (spec exclusion).

## See Also

* [Server Development Guide](server-guide.md)
* [Client Development Guide](client-guide.md)
* [Open Types Guide](open-types-guide.md)
* [OData V4 Overview](overview.md)
