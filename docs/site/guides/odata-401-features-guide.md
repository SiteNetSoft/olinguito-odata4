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
  `Preference-Applied` is not echoed even if requested.
* `omit-values=defaults` is **accepted but not applied** (a declined MAY): the preference parses
  and would render correctly in `Preference-Applied` if it were ever set, but no code path
  currently omits default-valued properties or sends the header for it — the data layer has no
  notion of an EDM-declared instance default to compare against.
* JSON only. The XML/Atom serializer has no omit-values support.

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

An omitted optional parameter *without* a default is left absent. A default that fails to parse is
the service's own model error, so it is reported as 500, not as a 400 like bad client input.

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

## See Also

* [Server Development Guide](server-guide.md)
* [Client Development Guide](client-guide.md)
* [Open Types Guide](open-types-guide.md)
* [OData V4 Overview](overview.md)
