# OData 4.01 Features Guide

## Introduction

This guide covers the OData 4.01 features Olinguito adds on top of its OData 4.0 baseline,
delivered as **Tier 5, Wave 1** of the 4.01 compliance milestone:

* [`matchesPattern` filter function](#matchespattern-filter-function)
* [`Prefer: omit-values=nulls`](#prefer-omit-values-nulls)
* [`/$query` — query options in the request body](#query---query-options-in-the-request-body)

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

## See Also

* [Server Development Guide](server-guide.md)
* [Client Development Guide](client-guide.md)
* [Open Types Guide](open-types-guide.md)
* [OData V4 Overview](overview.md)
