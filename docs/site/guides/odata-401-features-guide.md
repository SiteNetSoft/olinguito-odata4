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

**Tier 6, Wave 1:**

* [CSDL JSON metadata](#csdl-json-metadata-metadata-as-applicationjson)

**Tier 6, Wave 2:**

* [Geospatial types](#geospatial-types-edmgeography-edmgeometry)
* [Entity-typed values in JSON payloads](#entity-typed-values-in-json-payloads)

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

## CSDL JSON Metadata (`$metadata` as `application/json`)

Normative reference: [OData-CSDLJSON] §1.1 (conformance), §2.2 (defaults), §4 (document object),
§5.1 (aliases), §7.1/§7.2 (properties and facets), §8.1/§8.2/§8.5/§8.6 (navigation properties),
§6.3/§9.3 (open entity and complex types), §10.3 (enumeration members), §11 (type definitions),
§12.8/§12.9 (return types and parameters), §13.1–§13.6 (entity container),
§14.3/§14.4 (annotations and expressions);
[OData-Protocol] §11.1.2 (metadata format selection).

CSDL JSON is the JSON representation of the metadata document. A service `MAY` support it at
4.0-Minimal conformance, `SHOULD` support it at 4.01-Minimal, and `MUST` support it at
4.01-Advanced (§1.1). Olinguito now serves a conformant CSDL JSON document, and reads one on both
the server and the client.

Ask for it with the `Accept` header:

```
GET /service/$metadata
Accept: application/json
```

`$format=json` works too: `ContentNegotiator` applies the query option to the metadata
representation exactly as it does to a data payload, and it wins over `Accept`
([OData-Protocol] §11.1.2). `SchemaVersionITCase` exercises `$metadata?$format=json`. What is not
wired to `$format` is the *client*: the client asks for a representation through
`Configuration.getMetadataFormat()` and the `Accept` header, never through a query option.

`ContentNegotiator` lists `application/xml` and `application/json` for the metadata representation,
and `application/xml` stays the default: a request that expresses no format preference still gets
CSDL XML, as [OData-Protocol] §11.1.2 requires.

### Writer Corrections — What Changed on the Wire

The JSON metadata serializer already existed; it was not conformant. These corrections change the
bytes an existing consumer sees, so read them before upgrading a consumer that parses this document.

| Member | Before | Now | Clause |
| --- | --- | --- | --- |
| `$Version` | always `"4.01"` | the version the service actually serves (`"4.0"`, or `"4.01"` when `ServiceMetadata.getDataServiceVersion()` says so) | §4 |
| `$EntityContainer` | absent | written at the document level, always **namespace**-qualified (never the alias) | §4 |
| `$Extends` | nested in an `{"Extending": {...}}` object | written flat on the container object | §13.1 |
| entity sets | `$Kind: "EntitySet"`, no `$Collection` | `$Collection: true` + `$Type`, no `$Kind` | §13.2 |
| singletons, action imports, function imports | carried `$Kind` | no `$Kind` — the shape (`$Type` / `$Action` / `$Function`) distinguishes them | §13.3, §13.5, §13.6 |
| `$EntitySet` on an import | qualified with the container namespace | the unqualified entity-set name for a same-container set | §13.5, §13.6 |
| `$Nullable` | XML polarity (written when `false`) | JSON polarity — **absence means `false`**, so the member is written when the value is nullable and omitted otherwise | §7.2.1 |
| `$Nullable` on a collection navigation property | written | never written — "MUST NOT be specified for a collection-valued navigation property" | §8.2 |
| `$Nullable` on a collection-of-entities return type | written | never written — the member "has no meaning and MUST NOT be specified" | §12.8 |
| `$Type` on an `Edm.String` structural property | always written | omitted — absence means `Edm.String` | §7.1 |
| `$OpenType` | never written | written as `true` for an open entity or complex type | §6.3, §9.3 |
| enum member values | strings | JSON numbers | §10.3 |
| type definitions | `$Kind` missing, facets as strings | `$Kind: "TypeDefinition"` and §7.2 facet value types (`$SRID` stays a string, per §7.2.6) | §11, §7.2 |
| `$ReferentialConstraint` | one object per constraint | one object with one member per constraint | §8.5 |
| `$OnDelete` | an object | a string, with `$OnDelete`-prefixed annotations | §8.6 |
| constant expressions | `{"$String": "x"}`-style wrappers | bare JSON values (`"x"`, `42`, `true`) | §14.3 |
| enumeration member constants | the CSDL XML form, `Ns.Enum/Red Ns.Enum/Striped` | the spec form: unqualified members joined with a comma, `"Red,Striped"` | §14.3.7 |
| records | `$Type` member | the `@type` control information, `#`-prefixed short form | §14.4.12 |

Two decisions the writer records explicitly:

* **Integers outside the IEEE-754 safe range** are written as JSON strings rather than as numbers
  that a JSON parser using a double would silently corrupt. `Decimal` always goes out through
  `BigDecimal`'s own textual form, never a `double`.
* **A record's type** uses the `#`-prefixed short form of `@type` (`"@type": "#ns.Record"`), the
  spelling §14.4.12 names.

Defaults are omitted throughout (§2.2): `$IsFlags`, `$IsBound`, `$Abstract`, `$OpenType`,
`$HasStream`, `$Nullable` and `$ContainsTarget` appear only when they differ from the default.

### Reading CSDL JSON on the Server

`org.sitenetsoft.olinguito.server.core.MetadataJsonParser` (module `server-core-ext`) is the CSDL
JSON counterpart of `MetadataParser`. It returns the same types, so the two are interchangeable at
the call site:

```java
SchemaBasedEdmProvider provider = new MetadataJsonParser()
    .parseAnnotations(true)
    .referenceResolver(resolver)
    .buildEdmProvider(new InputStreamReader(csdl, StandardCharsets.UTF_8));

ServiceMetadata metadata = new MetadataJsonParser().buildServiceMetadata(reader);
```

The fluent switches are the same ones `MetadataParser` carries: `parseAnnotations`,
`referenceResolver`, `recursivelyLoadReferences`, `implicitlyLoadCoreVocabularies` and
`useLocalCoreVocabularies`. Both parsers now share one `ReferenceLoader`, so a referenced document
is loaded and de-duplicated identically whichever format the root document is in.

* **Both `$Version` values are accepted** — `"4.0"` and `"4.01"` (§4). Any other value, or a missing
  `$Version`, is a parse error.
* **JSON defaults, not XML defaults.** The single most load-bearing one: `$Nullable` defaults to
  **false** in CSDL JSON and to **true** in CSDL XML. `$Type` defaults to `Edm.String`. `$Unicode`
  defaults to true. A collection navigation property gets no `$Nullable` default at all, because
  §8.2 prohibits the member.
* **Aliases are resolved at parse time.** §5.1 makes alias use mandatory once a schema declares one
  ("A mixed use … is not allowed"), so a JSON document that declares `$Alias` is entirely
  alias-qualified. The parser rewrites `$Type`, `$BaseType`, `$UnderlyingType`, `$BaseTerm`,
  `$Action`, `$Function`, `$Extends` and term names to their namespace-qualified form, using both
  the schema aliases and the `$Include` aliases of the document. An unknown prefix passes through
  unchanged.
* **Errors carry their JSON path.** `CsdlJsonParseException extends ODataException` and its
  `getJsonPath()` names the exact member (`ns/ET/@ns.Term/$Path`).
* **Legacy tolerance on input.** Documents written by earlier Olinguito releases — the nested
  `Extending` object, `$Kind: "EntitySet"` without `$Collection`, the `$OnDelete` object, and the
  CSDL-XML-named constant members `$Binary`/`$Int`/… — still parse, even though nothing writes them
  any more.

### Reading CSDL JSON on the Client

The client has its own reader (the two do not share code: `commons-*` declares no Jackson
dependency, and `client-core` must not depend on `server-core-ext` — the same split CSDL XML has
lived with for years). It produces the same `XMLMetadata`/`CsdlSchema` graph the CSDL XML path
produces, so `ODataReader.readMetadata(...)`, `ClientCsdlEdmProvider` and `Edm` construction are
reused unchanged.

```java
// Explicit request for the CSDL JSON document
JSONMetadataRequest request = client.getRetrieveRequestFactory().getJSONMetadataRequest(serviceRoot);
XMLMetadata metadata = request.execute().getBody();          // Accept: application/json
Edm edm = client.getReader().readMetadata(metadata.getSchemaByNsOrAlias());

// Or let the convenience Edm request pick the representation
client.getConfiguration().setMetadataFormat(ContentType.APPLICATION_JSON);
Edm sameEdm = client.getRetrieveRequestFactory().getMetadataRequest(serviceRoot).execute().getBody();
```

* `Configuration.getMetadataFormat()` defaults to `ContentType.APPLICATION_XML`. Anything that is
  not `application/json`-compatible — including an unknown or `null` value — means the XML
  representation, which is [OData-Protocol] §11.1.2's rule for a request with no format preference.
* `ClientODataDeserializer.toJSONMetadata(InputStream)` is the deserializer entry point;
  `toMetadata(InputStream)` is unchanged and still CSDL XML.
* Every addition is a `default` interface method or a new type, so an existing implementor of
  `Configuration` or `RetrieveRequestFactory` keeps compiling.

### Reference Service

`MetadataJsonITCase` reads the technical service's `$metadata` in both representations and compares
the two resulting `Edm` graphs member by member: every entity and complex type with its properties
(type, collection-ness, nullability, `MaxLength`/`Precision`/`Scale`/`Unicode`/`DefaultValue`),
navigation properties (type, nullability, partner, `ContainsTarget`, `OnDelete`, constraint count),
key and stream markers; every enum type (underlying type, `IsFlags`, member names and values); every
type definition and its facets; every action and function overload with its binding, parameters and
return type; and the entity container's sets, singletons, imports and navigation property bindings.

### Known Limitations

* **A constant expression loses its per-value type marker.** §14.3 renders `Binary`, `Date`,
  `DateTimeOffset`, `Duration`, `EnumMember`, `Guid`, `String` and `TimeOfDay` constants identically,
  as JSON strings; the format defines no `$Binary`/`$Date`/… member to disambiguate them. The reader
  recovers the four shapes JSON does distinguish (`Bool`, `Int`, `Float`, `String`) and takes the
  rest from the term's declared type — so a constant whose term cannot be resolved re-reads as
  `String`. Values are exact either way; only the type tag normalizes.
* **The client does not follow `$Reference` on the JSON path.** `JSONMetadataRequestImpl` returns the
  primary document's schemas; the client's CSDL XML request, by contrast, follows references
  recursively with cycle detection. A service whose vocabularies live behind `$Reference` therefore
  yields a client-side `Edm` without them, and annotation terms from those vocabularies do not
  resolve. **The server parser is not affected**: `MetadataJsonParser` follows references through the
  shared `ReferenceLoader` whenever a `ReferenceResolver` is configured, and the loader sniffs each
  referenced document, so a referenced CSDL JSON document is parsed as JSON and a referenced CSDL XML
  document as XML.
* **Parse-time alias resolution leaves the graph namespace-qualified while `$Alias` stays on the
  schema.** That is invisible to every reader, but a future writer that emitted such a graph as-is
  would produce a document violating §5.1's mixed-use rule.
* **`$Annotations` groups are read only under `parseAnnotations(true)`**, while the CSDL XML parser
  always creates the (empty) group. Anything depending on the group existing without its annotations
  sees a difference.
* **An enumeration member constant loses its enumeration type.** §14.3.7 puts only "a string
  containing the numeric or symbolic enumeration value" on the wire (Example 51 is `"Red,Striped"`),
  so the writer strips the `Ns.Enum/` qualification the `Csdl*` model carries from the CSDL XML form.
  A reader recovers the member names but neither the enumeration type nor the fact that the string is
  an enumeration value at all — both come from the declared type of the applied term. This is the same
  loss as the constant type marker above. Both readers still accept the old qualified form through the
  legacy `$EnumMember` member; nothing writes it any more.
* **The XML parser's corrected `Nullable` defaults change XML `$metadata` bytes for a service whose
  EDM comes from `MetadataParser`.** `MetadataDocumentXmlSerializer` is unchanged and, given a fixed
  model, writes byte-identical XML — but the model is no longer the same one. The PR#11 family fix
  gives operation return types, parameters and terms the CSDL default `Nullable="true"` instead of
  `false`, and the XML serializer writes `Nullable` only when it is `false`, so those `Nullable="false"`
  attributes disappear from the re-serialized document. A service that builds its EDM in code is
  unaffected; only the parser → serializer pipeline is.
* **XML-parsed annotations now expose a non-null `getQualifier()`.** The CSDL XML parser dropped the
  `Qualifier` attribute; it no longer does. This is a downstream-visible change from a real bug fix —
  two annotations of the same term that differ only by qualifier used to collapse.
* **`getSchemaNamespaces()` returns `null` for client JSON metadata** (a JSON document has no
  `edmx:Edmx` wrapper to delegate to), so `CsdlTypeValidator.isV4MetaData` throws for such a
  document. Validate CSDL XML metadata, or skip validation on the JSON path.
* **`$Has` and `$In` (§14.4.2) are parse errors on both readers.** `CsdlLogicalOrComparisonExpression`
  declares `And, Or, Not, Eq, Ne, Gt, Ge, Lt, Le` and nothing else, so there is no model to build;
  adding constants to that public enum is not an additive change. The parser names the member it
  refused.
* **`$Collection` on `$Cast`/`$IsOf`, `$Nullable` on a singleton, and document-level annotations**
  have no home in the `Csdl*` model and are dropped, exactly as the CSDL XML parser drops their XML
  equivalents.
* **The `IEEE754Compatible` and `metadata` media-type parameters of §2.1 are not implemented for
  `$metadata`.** Numbers go out as JSON numbers (the `IEEE754Compatible=false` default) and the same
  amount of control information is always written.
* **Two format-inherent asymmetries survive an XML→JSON→model comparison**, and neither parser should
  "fix" them: a collection-of-entities return type has no nullability on the JSON wire at all
  (§12.8), and §13.4.2 keys navigation property bindings by path in one object, so a model that
  declares the same binding path twice can only carry it once.

## Geospatial Types (`Edm.Geography*` / `Edm.Geometry*`)

Normative reference: [OData-JSON] §7.1 (geospatial values are GeoJSON objects, RFC 7946) and its CRS
clause; [OData-ABNF] `geographyLiteral`/`geometryLiteral` and `positionLiteral` (the URL literal
grammar); [OData-URL] §5.1.1.11 (`geo.distance`, `geo.intersects`, `geo.length`) and §5.1.1.1 (geo
values compare only to `null`); [OData-Protocol] §11.2.6.2 (geo values cannot be sorted);
[OData-CSDL] §7.2.6 (the `SRID` facet and its defaults).

The sixteen geospatial primitive types — `Edm.Geography`, `Edm.GeographyPoint`,
`…LineString`, `…Polygon`, `…MultiPoint`, `…MultiLineString`, `…MultiPolygon`, `…Collection` and
the eight `Edm.Geometry*` counterparts — now travel end to end: they read and write as GeoJSON in
JSON payloads, they parse from and render to the ABNF URL literal form, and the three `geo.*`
filter functions are evaluated by the reference service.

### The JSON Wire Form

A geo value is a bare GeoJSON object whose members are written in the order `type`, then
`coordinates` (or `geometries` for a collection), then `crs`:

```json
{
  "PropertyGeometryPoint": {"type": "Point", "coordinates": [1.5, 2.5]},
  "PropertyGeometryLineString": {"type": "LineString", "coordinates": [[0.0, 0.0], [3.0, 4.0]]},
  "PropertyGeometryPolygon": {"type": "Polygon",
      "coordinates": [[[0.0, 0.0], [4.0, 0.0], [4.0, 4.0], [0.0, 4.0], [0.0, 0.0]]]},
  "PropertyGeographyCollection": {"type": "GeometryCollection",
      "geometries": [{"type": "Point", "coordinates": [1.0, 1.0]}]},
  "CollPropertyGeometryPoint": [{"type": "Point", "coordinates": [0.0, 0.0]},
                                {"type": "Point", "coordinates": [1.0, 1.0]}]
}
```

A non-default SRID is carried by a `crs` member, which [OData-JSON] requires to be of type `name`
with an EPSG legacy identifier:

```json
{"type": "Point", "coordinates": [1.5, 2.5],
 "crs": {"type": "name", "properties": {"name": "EPSG:42"}}}
```

Both `Edm.GeographyCollection` and `Edm.GeometryCollection` serialize as GeoJSON
`"type": "GeometryCollection"` with a `"geometries"` member — GeoJSON has no "GeographyCollection",
and [OData-JSON] never names the member itself, so RFC 7946 §3.1.8 is followed.

Four read/write defects were closed in this wave, all on the JSON path:

| # | Symptom | Fix |
|---|---|---|
| 1 | A geo value read from a payload came back out as the WKT string `geometry'SRID=0;Point(1.5 2.5)'` | The deserializer tags a geo value `ValueType.GEOSPATIAL` / `COLLECTION_GEOSPATIAL` (it used to tag it `PRIMITIVE`, and `writePrimitive` tests `isPrimitive()` before `isGeospatial()`). The same tagging is applied by the reference service's create path |
| 2 | A **collection-valued** geo property was written as an array of WKT strings | `COLLECTION_GEOSPATIAL` has its own serializer arm writing each member as a GeoJSON object |
| 3 | Members of an `Edm.GeographyCollection` were read with dimension `GEOMETRY` | The collection's dimension is inherited by its members |
| 4 | A malformed `crs` was a `NullPointerException` (a 500) | `crs` is validated: `type` must be `name`, `properties.name` must be present, textual and `EPSG:`-prefixed; anything else is a **400** (`INVALID_VALUE_FOR_PROPERTY`). Both `EPSG:4326` and the legacy `EPSG::4326` resolve |

On the client, a collection-valued geo property used to deserialize into complex values; it now
deserializes into `Geospatial` members tagged `COLLECTION_GEOSPATIAL`, using the declared element
type when the payload carries one and a GeoJSON shape test (`type` plus `coordinates`/`geometries`,
no `@odata.type`) when it does not.

### The URL Literal Grammar

A geo literal in a URL is the prefixed, quoted, SRID-carrying form the ABNF defines:

```
$filter=geo.distance(PropertyGeometryPoint,geometry'SRID=0;Point(1.5 2.5)') lt 0.5
$filter=geo.intersects(PropertyGeometryPoint,geometry'SRID=0;Polygon((10 10,14 10,14 14,10 14,10 10))')
```

`EdmPrimitiveType.toUriLiteral`/`fromUriLiteral` are now implemented for all sixteen types
(inherited from one place, `AbstractGeospatialType`), so a geo literal survives the parse →
evaluate → render round trip:

```
uriLiteral       = ("geography" / "geometry") "'" "SRID=" 1*5DIGIT ";" geoLiteral "'"
geoLiteral       = simpleGeoLiteral / collectionLiteral
simpleGeoLiteral = "Point" pointData / "LineString" lineStringData
                 / "MultiPoint(" [ pointData *("," pointData) ] ")"
                 / "MultiLineString(" [ lineStringData *("," lineStringData) ] ")"
                 / "Polygon" polygonData
                 / "MultiPolygon(" [ polygonData *("," polygonData) ] ")"
collectionLiteral = "Collection(" geoLiteral *("," geoLiteral) ")"
pointData        = "(" positionLiteral ")"
lineStringData   = "(" positionLiteral 1*("," positionLiteral) ")"    ; >= 2 positions
ringLiteral      = "(" positionLiteral *("," positionLiteral) ")"
polygonData      = "(" ringLiteral *("," ringLiteral) ")"
positionLiteral  = doubleValue SP doubleValue [SP doubleValue] [SP doubleValue]
doubleValue      = "-INF" / "INF" / "NaN" / [SIGN] 1*DIGIT ["." 1*DIGIT] [("e"/"E") [SIGN] 1*DIGIT]
```

Three things follow from that grammar, and all three are implemented:

* **Matching is case-insensitive**, because RFC 5234 §2.3 makes ABNF quoted-string literals
  case-insensitive: `GEOMETRY'SRID=0;POINT(1 2)'` is the same literal as
  `geometry'SRID=0;Point(1 2)'`. This matches the URI tokenizer, which has always read the nine geo
  keyword sites with `nextConstantIgnoreCase`. Output is always normalized to the lower-case prefix,
  with the body preserved verbatim.
* **The `SRID=` prefix may be omitted**, in which case the [OData-CSDL] §7.2.6 facet default is
  applied — `SRID=0;` for a geometry type, `SRID=4326;` for a geography type. A bare
  `SRID=nnn;Point(…)` (the ABNF's `fullPointLiteral`) is accepted too and wrapped with the type's
  own prefix. `toUriLiteral` never double-wraps a literal that already carries a prefix.
* **A position may carry 3 or 4 elements**, not just 2: `positionLiteral` allows an optional
  altitude and an optional linear-referencing measure. `UriTokenizer.nextPosition()` accepts 2–4
  space-separated coordinates; the third becomes the point's `Z`, and the fourth is validated and
  then dropped (the geo model has no M coordinate). One consequence of the variable length: a `.`
  that corrupts a position separator can be re-absorbed as part of a still-valid position, so
  `LineString(1 2,3 4)` disturbed into `LineString(1 2.3 4)` re-parses as a legal 3-element position
  instead of failing — which is why `UriTokenizerTest`'s `wrongToken` pin had to switch its disturb
  character from `.` to one that is not itself part of the position grammar.

`fromUriLiteral` returns the *wrapped* form rather than the bare literal, because that is what
`valueOfString` consumes in this codebase and what `VisitorOperand.tryCast` chains into. It
validates: `fromUriLiteral("test")` used to return `"test"` and now raises
`EdmPrimitiveTypeException` (*The literal 'test' has illegal content.*).

### Comparison and Ordering Restrictions

[OData-URL] §5.1.1.1 says geo values "can only be compared to the null value using the `eq` and `ne`
operators", and [OData-Protocol] §11.2.6.2 says they "cannot be sorted". Both are enforced now:

| Request | Result |
|---|---|
| `$filter=PropertyGeometryPoint eq null` / `ne null` | Allowed |
| `$filter=PropertyGeometryPoint eq geometry'SRID=0;Point(1.5 2.5)'` | **400** — `TYPES_NOT_COMPATIBLE`, naming the property |
| `$filter=PropertyGeometryPoint lt …` (any relational operator) | **400** — unchanged; the parser's relational allow-list never contained a geo kind |
| `$orderby=PropertyGeometryPoint` | **400** — `TYPES_NOT_COMPATIBLE` |
| `geo.distance(…) lt 500000`, `geo.length(…) eq 5` | Allowed — the *result* of a geo function is `Edm.Double`/`Edm.Boolean`, not a geo value |

A dynamic (open-type) member, whose type is unknown at parse time, is treated like a `null` literal
and still parses.

### EDM Surface: the `ESGeo` Reference Set

The technical reference service (tecsvc) gained an entity type `olingo.odata.test1.ETGeo` and an
entity set `ESGeo`:

| Property | Type |
|---|---|
| `PropertyInt16` | `Edm.Int16` — the key |
| `PropertyGeography…` (7) | `Edm.GeographyPoint`, `…LineString`, `…Polygon`, `…MultiPoint`, `…MultiLineString`, `…MultiPolygon`, `…Collection` |
| `PropertyGeometry…` (7) | the seven concrete `Edm.Geometry*` counterparts (the abstract `Edm.Geometry` has no value representation) |
| `CollPropertyGeometryPoint` | `Collection(Edm.GeometryPoint)`, declared `SRID="0"` |

The key is `Edm.Int16` because [OData-CSDL] §4.1 does not list any geospatial type among the
primitive types a key property may have. The library does not enforce that (the prohibition exists
only as an exclusion from a closed allow-list, never as prose), so the restriction is pinned in the
reference model rather than validated in `EdmEntityTypeImpl`.

Only `CollPropertyGeometryPoint` declares the `SRID` facet, so `$metadata` carries
`SRID="0"` on that one property and nothing on the other fifteen — a client applies the §7.2.6
default, and at runtime `SRID.getValue()` reports `"0"` for a geometry value and `"4326"` for a
geography one.

Three entities are seeded: entity **1** with shapes at the origin, entity **2** with the same
shapes translated by +10 on both axes, and entity **3** with every geo property `null` (its
collection property is an empty array, not `null`). Entity 1's geometry line string is a 3-4-5
triangle, so its length is exactly `5.0`.

### The Three Geo Functions

[OData-URL] §5.1.1.11 fixes the signatures, the overload set and the return types, and nothing else —
no algorithm, no ellipsoid, no tolerance. The reference service evaluates them as follows:

| Function | Signature | Reference implementation | Unit |
|---|---|---|---|
| `geo.distance` | (Point, Point) → `Edm.Double`, geography and geometry flavours | Geometry: planar Euclidean `sqrt(dx² + dy²)`. Geography: haversine great circle, `2R·asin(min(1, √a))` with `R = 6371008.8` m (the WGS-84/IUGG mean radius R₁) | Geometry: the SRID's own linear units. Geography: **metres** |
| `geo.length` | (LineString) → `Edm.Double` | The sum of the per-segment distances, each measured by the rule above. A line of 0 or 1 points is `0.0`, not an error | as above |
| `geo.intersects` | (Point, Polygon) → `Edm.Boolean` | An explicit boundary test on each ring first (a vertex or an edge point is `true`, per "within the interior **or on the boundary**"), then even-odd ray casting over the exterior ring; a point strictly inside an interior ring (a hole) is `false`, a point on a hole's own edge is `true`. Planar in both dimensions, tolerance `1e-12` | – |

Any operand outside those overloads — a non-Point/LineString/Polygon value, or a geography operand
paired with a geometry one — answers **501 Not Implemented** with a message naming the function and
the reason (*The reference service does not implement this overload of geo.distance: GEOMETRY and
GEOGRAPHY operands cannot be mixed.*). Most malformed calls never get that far: the URI parser
type-checks the operands and answers 400 first, so 501 is the last line of defence for what the
parser lets through. A `null` operand yields a typed null result, never an exception, so an
all-`null` entity is filterable rather than a 500.

Worked examples against the seeded data, all verified over HTTP:

```
GET ESGeo?$filter=geo.length(PropertyGeometryLineString) eq 5                       -> 1, 2
GET ESGeo?$filter=geo.distance(PropertyGeometryPoint,geometry'SRID=0;Point(1.5 2.5)') lt 0.5   -> 1
GET ESGeo?$filter=geo.intersects(PropertyGeometryPoint,PropertyGeometryPolygon)     -> 1, 2
GET ESGeo?$filter=geo.intersects(PropertyGeometryPoint,
                                 geometry'SRID=0;Polygon((10 10,14 10,14 14,10 14,10 10))')    -> 2
GET ESGeo?$filter=geo.distance(PropertyGeographyPoint,geography'SRID=4326;Point(0 0)')
          gt 324000 and lt 324200                                                   -> 1
```

Geometry answers are exact and may be compared with `eq`; geography answers are metres on a mean
sphere and should be bracketed with `gt`/`lt`.

### Client Usage

```java
ODataEntityRequest<ClientEntity> request = client.getRetrieveRequestFactory()
    .getEntityRequest(client.newURIBuilder(serviceRoot)
        .appendEntitySetSegment("ESGeo").appendKeySegment(1).build());
request.setFormat(ContentType.JSON_FULL_METADATA);

ClientEntity entity = request.execute().getBody();
Point point = (Point) entity.getProperty("PropertyGeometryPoint").getPrimitiveValue().toValue();
// point.getX() == 1.5, point.getY() == 2.5, point.getDimension() == Dimension.GEOMETRY
```

**Full metadata matters for geo.** `{"type":"Point","coordinates":[1.5,2.5]}` is byte-identical for
`Edm.GeometryPoint` and `Edm.GeographyPoint`. At `odata.metadata=minimal` the server writes no
`Name@odata.type`, so a client with no EDM in hand cannot know the dimension and guesses
`Edm.Geography*`. At `odata.metadata=full` the dimension and the SRID are read exactly.

### Recorded Deviations and Limitations

* **Geo values are JSON-only, and that is a behavior change for every existing service.** Before
  this wave a geo value read from a payload arrived as a plain `ValueType.PRIMITIVE` string and the
  XML/Atom and JSON-delta writers happily emitted it as a (non-conformant) WKT string. The JSON
  deserializer now tags **any** geo value `ValueType.GEOSPATIAL`, and the writers that cannot render
  that refuse it: `ODataXmlSerializer` throws `UNSUPPORTED_PROPERTY_TYPE`, and so do
  `JsonDeltaSerializer` (lines 335-337, 374-379) and `JsonDeltaSerializerWithNavigations` (lines
  399-404, 444-446), scalar and collection alike. Requesting a geo entity as `application/xml`
  answers a **400** (*The type of the property 'PropertyGeographyPoint' is not yet supported.*), and
  a delta payload carrying a geo property does the same. This is **not** confined to the reference
  `ESGeo` set — any service whose entities carry geospatial properties and are served as Atom/XML or
  as a delta response changes from emitting WKT to answering an error. No GML serializer was
  written; Atom is not on the 4.01 conformance ladder this milestone targets.
* **`Valuable.asPrimitive()` returns `null` for a deserialized geo property**, because the value is
  now tagged `GEOSPATIAL` rather than `PRIMITIVE`; a custom processor that reads geo that way gets a
  silent null instead of the old string, and must call `asGeospatial()` (or check `getValueType()`).
* **A type definition over a geospatial underlying type is still tagged `PRIMITIVE`.**
  `valueTypeFor` returns `PRIMITIVE` for `EdmTypeKind.DEFINITION`, matching `readPrimitiveValue`,
  which also only treats `kind == PRIMITIVE` as geo — so such a property serializes as a WKT string
  rather than as GeoJSON. Widening one side without the other would break the pair; widening both
  was out of scope for this wave.
* **The reference service's geo math is a reference implementation, not a geodesy library** —
  haversine rather than an ellipsoidal geodesic (up to ~0.5 % off), planar polygon containment in
  degree space for geography (exact for small axis-aligned shapes, wrong near the poles or across
  the antimeridian), `Point × Polygon` intersection only, no CRS re-projection. Everything else is
  501. This is a **tecsvc** decision; the library imposes none of it and another service may answer
  in whatever unit its SRID implies.
* **Geography distances and lengths are answered in metres**, although §5.1.1.11 says only "in the
  coordinate reference system signified by … the SRID". Under EPSG:4326 those units are degrees, and
  a shortest distance in degrees is not a distance. Geometry values are planar and genuinely in the
  SRID's units.
* **SRID is not compared, only the dimension is.** Two geometry values with different SRIDs are
  measured against each other without re-projection or rejection.
* **`SRID="variable"` breaks the URL literal round trip.** The model accepts it and renders
  `SRID=variable;`, which the ABNF (`1*5DIGIT`) has no production for — there is no compliant literal
  to emit. Pre-existing, spec-silent, and left for a decision rather than patched. No `ESGeo`
  property declares it.
* **The ABNF's fourth position element is parsed and dropped**, not retained: `Point` carries `x`,
  `y` and `z` only, and adding an M coordinate would change `equals`/`hashCode` on a public value
  class. A `Point(1 2 3 4)` literal is accepted but does not round-trip byte-identically.
* **A genuine `z == 0.0` is dropped on output.** `Point.getZ()` is a primitive `double`, so "no
  altitude" and "altitude zero" are indistinguishable; `Point(1 2 0)` re-emits as `Point(1 2)`. The
  JSON serializer has always applied the same rule.
* **Minimal metadata cannot convey a geo dimension to a non-EDM client**, and the client's fallback
  guess is `Edm.Geography*` for both dimensions (see Client Usage above). Pinned by a fit test, not
  hidden.
* **Collection nesting deeper than two levels is rejected** by the URL literal validator: the ABNF's
  `collectionLiteral` is recursive and Java regular expressions are not, so it is unrolled twice —
  which is as deep as the existing value parser could build anyway.
* **The `Edm.Binary` and `Edm.Stream` halves of the §5.1.1.1 restriction stay unenforced.** The same
  sentence names all three, but `PropertyBinary eq binary'VGVzdA=='` is pinned as valid by existing
  tests and tightening it is outside this feature's boundary.
* **A geo type is still accepted as a key property.** The prohibition is an inference from a closed
  allow-list, never prose, so no validator was added; the reference model keys `ETGeo` by
  `Edm.Int16` and a test pins that.

## Entity-Typed Values in JSON Payloads

Normative reference: [OData-JSON] §6 (an entity is serialized as a JSON object), §13 (a collection
of entities: "each element is representation of an entity or a representation of an entity
reference"), §18 Action Invocation ("Entity typed parameter values MAY include a subset of the
properties, or just the entity reference"), §14 and §4.5.8 (the `@id` by-reference form);
[OData-CSDL] §7.1 (a structural property is primitive, complex or enumeration typed — never entity
typed).

An entity-typed value reaches a payload as an **action or function parameter, or as a return
value** — never as a structural property, which CSDL forbids. Both directions of that round trip
now work in JSON: the deserializer already produced `ValueType.ENTITY`/`COLLECTION_ENTITY`, and the
serializer used to throw `UNSUPPORTED_PROPERTY_TYPE` (a 500) for them. It no longer does.

### The Three Shapes of an Entity-Typed Value

```json
{
  "ParameterETTwoPrim": {"PropertyInt16": 42, "PropertyString": "Yes"},
  "CollParameterETTwoPrim": [{"PropertyInt16": 1, "PropertyString": "One"},
                             {"PropertyInt16": 2, "PropertyString": "Two"}]
}
```

1. **A complete or partial entity value** is a plain JSON object of name/value pairs. §18 permits a
   subset of the properties; nothing is defaulted for the properties left out.
2. **A collection of entities** is a JSON array, each element being either an entity object or an
   entity reference — the choice is made per element.
3. **An entity reference** is an object carrying only the id:

```json
{"ParameterETTwoPrim": {"@odata.id": "ESTwoPrim(32767)"}}
```

   The member is spelled `@odata.id` on a request negotiated as OData 4.0 and `@id` on 4.01, because
   the writer and the reader both read the name from the version's `Constants`. A value is treated
   as a reference only when it carries an id and nothing else — no properties, no navigation links,
   no annotations.

At `odata.metadata=full` an entity-typed value additionally carries its type control information
*inside* the object (`"@odata.type": "#olingo.odata.test1.ETTwoPrim"`), and a collection's declared
type is written as `#Collection(olingo.odata.test1.ETTwoPrim)`.

### How an `@id` Reference Is Resolved

[OData-JSON] §4.5.8 says only that "by convention the entity-id is identical to the canonical URL of
the entity", so both spellings a client may reasonably send are accepted: the relative
`ESTwoPrim(32767)` and the absolute `http://host/service/ESTwoPrim(32767)`. A reference matches when
it equals the stored canonical id or ends with `/` + that id — the path-segment boundary is
deliberate, so `(32767)` and `Prim(32767)` do **not** address `ESTwoPrim(32767)`.

| Parameter value | Result |
|---|---|
| An entity object with properties | Used as it stands; a subset stays a subset |
| `{"@odata.id": "ESTwoPrim(32767)"}` (relative or absolute canonical) | Resolved against the entity set |
| A partial suffix such as `{"@odata.id": "(32767)"}` | **400** — *Cannot resolve the entity reference '(32767)'.* |
| An id that matches nothing | **400**, naming the reference (spec-silent failure mode; recorded decision) |
| An id **alongside** properties | The properties win and the id is ignored — a value carrying properties is read as "a subset of the properties", so no lookup happens |
| `{"@odata.id": null}` | Not a reference; the value is an empty entity |
| Omitted altogether (a nullable parameter) | The null value |

Reading the id is confined to the **parameter** path: on an ordinary entity payload (create, update,
an expanded navigation, a delta) a client-supplied `@odata.id` remains ignorable control information
for 4.0 requests, exactly as [OData-JSON] §4.5 requires — it must not reach `Entity.getId()`, which
would put a client-controlled value into the `OData-EntityId` response header.

### EDM Surface: the Echo Action

The reference service gained an unbound action whose sole purpose is to make the round trip
observable:

```xml
<Action Name="UARTETTwoPrimEchoParam" IsBound="false">
  <Parameter Name="ParameterETTwoPrim" Type="Namespace1_Alias.ETTwoPrim"/>
  <Parameter Name="CollParameterETTwoPrim" Type="Collection(Namespace1_Alias.ETTwoPrim)"/>
  <ReturnType Type="Namespace1_Alias.ETTwoPrim"/>
</Action>
<ActionImport Name="AIRTETTwoPrimEchoParam" Action="Namespace1_Alias.UARTETTwoPrimEchoParam"/>
```

Both parameters are nullable. The result's `PropertyInt16` is the parameter entity's (0 when
absent), and its `PropertyString` is the parameter entity's suffixed with the collection's size:

```
POST /odata.svc/AIRTETTwoPrimEchoParam
{"ParameterETTwoPrim":{"PropertyInt16":7,"PropertyString":"echo me"},
 "CollParameterETTwoPrim":[{"PropertyInt16":1},{"PropertyInt16":2}]}

200 OK
{"@odata.context":"$metadata#olingo.odata.test1.ETTwoPrim",
 "PropertyInt16":7,"PropertyString":"echo me (2)"}
```

The action import declares no entity set, so the returned entity is transient: there is no
`Location` header.

### Server Behavior and Status Codes

| Condition | Result |
|---|---|
| Complete or partial entity value | **200**, evaluated as sent |
| Collection of entity values and/or references | **200**; an empty collection is `[]`, not an error |
| Resolvable `@id` reference | **200**, the referenced entity's values |
| Unresolvable or partially-matching `@id` | **400**, message naming the reference |
| Omitted nullable entity-typed parameter | **200**, the null value |
| A `Property` whose declared type is *not* entity typed but whose value is an `Entity` | **500**-class serializer error `INCONSISTENT_PROPERTY_TYPE`, unchanged — the illegal shape still fails, and the message names the property |
| The 4.01 `@id` spelling on a request negotiated as 4.0 | **400** (*The requested deserialization method has not been implemented yet.*) — the pre-existing rejection of unknown `@`-annotations in 4.0. Negotiate 4.01 to use `@id` |

### Client Usage

The client has no `ClientValue` shape for an entity, so an entity-typed parameter is expressed as a
`ClientComplexValue`, whose JSON form is exactly the name/value object [OData-JSON] §18 requires:

```java
Map<String, ClientValue> parameters = new HashMap<>();
parameters.put("ParameterETTwoPrim", client.getObjectFactory().newComplexValue(null)
    .add(client.getObjectFactory().newPrimitiveProperty("PropertyInt16",
        client.getObjectFactory().newPrimitiveValueBuilder().buildInt16((short) 7)))
    .add(client.getObjectFactory().newPrimitiveProperty("PropertyString",
        client.getObjectFactory().newPrimitiveValueBuilder().buildString("echo me"))));

ODataInvokeRequest<ClientEntity> request = client.getInvokeRequestFactory().getActionInvokeRequest(
    client.newURIBuilder(serviceRoot).appendActionCallSegment("AIRTETTwoPrimEchoParam").build(),
    ClientEntity.class, parameters);
ClientEntity result = request.execute().getBody();   // PropertyString == "echo me (0)"
```

### Recorded Deviations and Limitations

* **Entity-typed values are JSON-only.** `ODataXmlSerializer` and the client's `AtomSerializer` both
  still refuse `ValueType.ENTITY`/`COLLECTION_ENTITY` (*Entities cannot appear in this payload*), as
  do the delta serializers. The XML *deserializer* does produce those value types, so the XML round
  trip stays asymmetric exactly as JSON was before this wave.
* **The §18 partial-entity form does not round-trip.** It is what a client *sends*; a service cannot
  write it back, because `writeProperty` emits an explicit `null` for a declared-but-absent nullable
  property and throws `MISSING_PROPERTY` for a non-nullable one. Return a complete value or the
  `@id`-only reference.
* **An unresolvable `@id` is a 400** — the spec states the by-reference form is legal but not the
  failure mode.
* **An `@id` sent alongside properties is ignored**, and the properties are the value. Defensible
  under §18 (a value carrying properties is the "subset of the properties" case, so no lookup
  happens), but it is a decision, not a spec requirement.
* **There is no client-side id-only `ClientValue`.** `ClientValue` offers primitive, complex,
  collection and enum shapes only, so the by-reference form cannot be expressed through the client
  without hand-rolling the request body. It is pinned at the serializer and data-provider layers
  instead; adding an entity-reference `ClientValue` is client public API and belongs in its own wave.
* **`writeEntity` still emits `"@odata.type":"#null"` at full metadata for an `Entity` carrying no
  type name.** The echo action sets the type on the result entity it builds, so this path is
  correct; the durable fix — defaulting to the declared parameter/return type in the serializer or
  the deserializer — is a server-core change deferred to a later wave.
* **The `@odata.id` of an action-import result that declares no entity set** is a type name in an
  entity-set position (`olingo.odata.test1.ETTwoPrim(7)`). Pre-existing, shared with other action
  imports, and untouched: transient-entity id generation has a much wider blast radius.

## See Also

* [Server Development Guide](server-guide.md)
* [Client Development Guide](client-guide.md)
* [Open Types Guide](open-types-guide.md)
* [OData V4 Overview](overview.md)
