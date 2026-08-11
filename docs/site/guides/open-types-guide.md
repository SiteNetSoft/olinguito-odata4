# Open Types Guide

## Introduction

OData V4 [open types](https://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html#sec_OpenType)
are entity or complex types whose instances may carry **dynamic properties** — properties that
are not declared in `$metadata`. Olinguito supports open types end to end: server payload
handling (JSON), server-side `$select` / `$filter` / `$orderby` and direct path addressing of
dynamic properties, and client round-tripping.

This guide covers:

* Declaring an open type in your EDM
* Reading and writing dynamic properties on the server
* Reading and writing dynamic properties on the client
* How dynamic values are typed on the wire
* What is explicitly out of scope

## The `OpenType` Flag

`OpenType` is a standard EDM/CSDL concept, not something Olinguito invented — it maps directly
to `OpenType="true"` on `<EntityType>` / `<ComplexType>` in `$metadata` XML. Mark a type open in
your `CsdlAbstractEdmProvider`:

```java
@Override
public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
    if (entityTypeName.equals(ET_PRODUCT_FQN)) {
        return new CsdlEntityType()
            .setName("Product")
            .setOpenType(true)   // instances may carry undeclared properties
            .setKey(Collections.singletonList(new CsdlPropertyRef().setName("ID")))
            .setProperties(Arrays.asList(id, name));
    }
    return null;
}
```

`CsdlComplexType` has the same `setOpenType(true)` method, so nested complex values can be open
independently of their containing entity.

At runtime, `EdmStructuredType.isOpenType()` reports whether a type accepts dynamic properties.
Closed types (the default, `OpenType` unset or `false`) keep their existing strict behavior:
any property not in `$metadata` is rejected exactly as before.

## Server: Dynamic Properties in JSON Payloads

Dynamic properties travel in the same JSON property list as declared ones — there is no separate
"extensions" object. A request body for an open entity might look like:

```json
{
  "PropertyInt16": 1,
  "PropertyString": "open type 1",
  "DynamicString": "dynamic",
  "DynamicInt": 42
}
```

`PropertyInt16` and `PropertyString` are declared on the entity type; `DynamicString` and
`DynamicInt` are not — they are accepted only because the entity type is open. This works for
both entity types and complex types (a complex-typed property can itself be open, e.g. a
`CTOpen`-typed `PropertyComp` on an open entity, accepting its own dynamic members).

On the server side no code changes are needed to accept or emit these — the JSON
(de)serializers branch on `isOpenType()` automatically. A processor reads and writes dynamic
properties through the same `Entity.getProperties()` / `Property` API used for declared ones;
there is no separate dynamic-property API on the server.

### Type Inference for Unannotated Values

When a dynamic value has no `@odata.type` annotation, its EDM primitive type is inferred from
the JSON value itself:

| JSON value | Inferred `Edm` type |
|---|---|
| `true` / `false` | `Edm.Boolean` |
| Integral number | `Edm.Int16`, `Edm.Int32`, or `Edm.Int64`, by magnitude |
| Decimal number | `Edm.Double` or `Edm.Decimal`, per how Jackson parses the literal |
| String | `Edm.String` |
| `null` | Typeless null property (no inferred type) |
| Array | `Collection(<inferred element type>)`, inferred from the first non-null element; an empty (or all-null) array infers `Collection(Edm.String)` |

A JSON **object** in a dynamic slot (a dynamic complex value) is not supported and is rejected
with an `UNKNOWN_CONTENT` deserializer error — see [Out of Scope](#out-of-scope) below.

### The `@odata.type` Annotation

An explicit `propertyName@odata.type` annotation overrides inference and is required for any
primitive type the JSON-native inference table above can't express (`Edm.Guid`,
`Edm.DateTimeOffset`, `Edm.Duration`, and so on). The wire format uses the same bare, `#`-prefixed
short type name OData uses elsewhere for instance annotations — not the fully-qualified
`Edm.`-prefixed name:

```json
{
  "PropertyInt16": 1,
  "TrackingId@odata.type": "#Guid",
  "TrackingId": "01234567-89ab-cdef-0123-456789abcdef"
}
```

Collections use the same convention: `"Refs@odata.type": "#Collection(Guid)"`. An unparseable or
unrecognized `@odata.type` annotation fails deserialization with an error naming the annotation.
On the way in, the deserializer is lenient about the exact form: `#Guid`, `#Edm.Guid`, and bare
`Guid` (no `#`, no `Edm.` prefix) all resolve to the same type, so either form on the wire round-
trips correctly.

On output, `odata.metadata=minimal` (the default) omits the annotation for any dynamic value whose
`Edm` kind is self-describing from its JSON representation alone — `String`, `Boolean`, and every
numeric kind (`Int16`/`Int32`/`Int64`/`Single`/`Double`/`Decimal`) — and emits it for everything
else (`Guid`, `DateTimeOffset`, `Duration`, and other non-JSON-native primitives), so a
round-tripping client can re-type them correctly. `odata.metadata=full` always emits
`name@odata.type` for every dynamic value. The client applies the same rule when writing (see
[Writing](#writing) below), since it has no EDM either and so cannot otherwise tell which of its
properties a receiving open-type server will treat as dynamic.

## Server: Querying Dynamic Properties

Dynamic properties can be addressed directly in the URI, wherever a single, unqualified property
name is legal:

```
GET Products(1)/Brand
GET Products?$select=Brand
GET Products?$filter=Brand eq 'Acme'
GET Products?$orderby=Brand desc
```

* **`$select`** — a selected dynamic name that is absent from a given instance is simply omitted
  from that instance's output (not an error), matching the spec.
* **`$filter` / `$orderby`** — the URI layer treats a dynamic member as compatible with any
  primitive comparison operand at parse time; type mismatches are resolved (or fail) at
  evaluation time using normal comparison semantics rather than a URI parse-time type error.
  Evaluation itself (including how a missing/absent dynamic property sorts or compares) is up to
  whatever expression visitor and data provider the service uses — in the tecsvc reference
  implementation bundled with this project, a missing/absent dynamic property behaves as `null`,
  sorting before any present value in ascending order (per `OrderByHandler`'s null-ordering rule).
  A custom service's own processor is free to evaluate this differently.
* **Direct path addressing** (`/Entity(1)/DynamicName`) resolves to a dedicated
  `UriResourceDynamicProperty` in the parsed URI resource tree — an untyped path segment (there is
  no EDM type to report for it) representing the dynamic property by name. This is supported end
  to end by the processor-based dispatch stack tecsvc uses (`ODataHandler`/`ODataDispatcher`). The
  older, `ServiceHandler`-based dispatch stack in `server-adapter-servlet`/`server-core-ext` (used
  by simple samples such as the bundled TripPin example) has no equivalent hook for serving a
  dynamic property's value at all, so on that stack every dynamic-property path segment 404s,
  regardless of whether the instance actually has a value for it.

## Client: Reading and Writing Dynamic Properties

The client library never consults an EDM when reading or writing JSON — its deserializer infers
property types from the JSON payload the same way the server does, and its serializer writes
whatever is in a `ClientEntity`'s property list. This means dynamic properties need **no special
client API**: use the same `ClientObjectFactory` / `ClientEntity.getProperties()` calls you use
for declared properties.

### Reading

```java
ODataEntityRequest<ClientEntity> request = client.getRetrieveRequestFactory()
    .getEntityRequest(client.newURIBuilder(serviceRoot)
        .appendEntitySetSegment("Products")
        .appendKeySegment(1)
        .build());
ClientEntity entity = request.execute().getBody();

// "Brand" need not be declared in $metadata for this to work.
ClientProperty brand = entity.getProperty("Brand");
if (brand != null) {
    System.out.println("Brand: " + brand.getPrimitiveValue().toValue());
}
```

### Writing

```java
ClientObjectFactory factory = client.getObjectFactory();
ClientEntity entity = factory.newEntity(new FullQualifiedName("MyService", "Product"));
entity.getProperties().add(factory.newPrimitiveProperty("Name",
    factory.newPrimitiveValueBuilder().buildString("Widget")));
// Undeclared (dynamic) property - only legal because Product is an open type on the server.
entity.getProperties().add(factory.newPrimitiveProperty("Brand",
    factory.newPrimitiveValueBuilder().buildString("Acme")));

ODataEntityCreateRequest<ClientEntity> request = client.getCUDRequestFactory()
    .getEntityCreateRequest(entitySetUri, entity);
request.execute();
```

A full `PUT` (`UpdateType.REPLACE`) replaces the stored entity wholesale, including its dynamic
properties — omitting a previously-set dynamic property in a PUT body drops it, the same as any
declared property.

Because the client has no EDM, it cannot tell which of a `ClientEntity`'s properties a receiving
server will treat as declared versus dynamic. So for any primitive property whose `Edm` kind is
not self-describing from its bare JSON form (a `Guid`, `DateTimeOffset`, `Duration`, and so on —
the same non-JSON-native set from the [inference table](#type-inference-for-unannotated-values)
above), the client writes a `name@odata.type` annotation even under minimal metadata, using the
same `#`-prefixed short-name convention as the server. Without it, a receiving open-type server's
inference would default an unannotated non-native value to `Edm.String`, silently corrupting it.
JSON-native kinds (strings, booleans, and numbers) are unaffected and stay unannotated under
minimal metadata, exactly as before.

## Out of Scope

The following remain unsupported and are pinned by regression tests — behavior is unchanged from
before open-type support was added:

* **XML/Atom payloads.** Dynamic properties are JSON-only; the XML instance serializer and
  deserializer only ever handle the EDM-declared property set.
* **Dynamic complex values.** A JSON object in a dynamic property slot (as opposed to a
  primitive or an array of primitives) is rejected with `UNKNOWN_CONTENT`. A dynamic *primitive*
  member nested inside an already-open complex value (e.g. `PropertyComp/CompDynamic` where
  `PropertyComp` is itself an open complex type) is supported.
* **`$expand` on a dynamic name.** Dynamic properties are never navigation properties, so
  `$expand=SomeDynamicName` is rejected exactly as it is for any other unknown name.
* **`$apply` on a dynamic name.** Out of scope for this feature.
* **Nested or lambda paths in expressions.** Multi-segment or `any`/`all` lambda expressions over
  dynamic properties are rejected, matching existing behavior for unresolvable paths.

## See Also

* [Server Development Guide](server-guide.md)
* [Client Development Guide](client-guide.md)
* [OData V4 Overview](overview.md)
