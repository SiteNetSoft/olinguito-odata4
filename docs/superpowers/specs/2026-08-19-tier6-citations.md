# OData 4.01 Tier-6 Feature Citations Digest

Primary sources fetched directly (full documents downloaded and converted to plain text; all quotes below are verbatim from these documents, section numbers taken from the documents' own tables of contents / headings):

- **[OData-CSDLJSON]** OData Common Schema Definition Language (CSDL) JSON Representation Version 4.01 (OS, 11 May 2020): https://docs.oasis-open.org/odata/odata-csdl-json/v4.01/odata-csdl-json-v4.01.html
- **[OData-CSDLXML]** OData Common Schema Definition Language (CSDL) XML Representation Version 4.01 (cross-references only): https://docs.oasis-open.org/odata/odata-csdl-xml/v4.01/odata-csdl-xml-v4.01.html
- **[OData-JSON]** OData JSON Format Version 4.01: https://docs.oasis-open.org/odata/odata-json-format/v4.01/odata-json-format-v4.01.html
- **[OData-Protocol]** OData Version 4.01 Part 1: Protocol: https://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part1-protocol.html
- **[OData-URL]** OData Version 4.01 Part 2: URL Conventions: https://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part2-url-conventions.html
- **[OData-ABNF]** OData ABNF Construction Rules Version 4.01 (OS): https://docs.oasis-open.org/odata/odata/v4.01/os/abnf/odata-abnf-construction-rules.txt
- **[GeoJSON]** RFC 7946, The GeoJSON Format (only the parts [OData-JSON] relies on): https://www.rfc-editor.org/rfc/rfc7946.txt

Note: the "part1"/"part2" URLs are OASIS's own "latest errata" pointer pages (they resolve to the current errata-consolidated OS text), which is what this digest prefers. The CSDL and JSON-format URLs are the corresponding version-latest pointer pages.

Scope: only the clauses needed by the Tier-6 features (CSDL JSON metadata, geo types, entity-typed values in payloads, stream-property control information, asynchronous requests). Everything the sources do not state is called out explicitly as **SPEC SILENT** and collected in the closing **Gaps** section.

---

## 1. CSDL JSON metadata document (OLINGO-1300)

Sources used: [OData-CSDLJSON] §2.1–2.2, §3.1, §3.6, §4–§14; [OData-CSDLXML] (cross-refs only); [OData-Protocol] §11.1.2, §13.1/§13.2

### 1.1 Requesting the JSON representation of $metadata

**(a)** [OData-CSDLJSON], **§2.1 Requesting the JSON Representation** (+ §2.1.1, §2.1.2); [OData-Protocol] **§11.1.2 Metadata Document Request**

**(b) Verbatim (§2.1):**
> "The OData CSDL JSON representation can be requested using the $format query option in the request URL with the media type application/json, optionally followed by media type parameters, or the case-insensitive abbreviation json which MUST NOT be followed by media type parameters."

> "Alternatively, this representation can be requested using the Accept header with the media type application/json, optionally followed by media type parameters."

> "If specified, $format overrides any value specified in the Accept header."

> "The response MUST contain the Content-Type header with a value of application/json, optionally followed by media type parameters."

> "Possible media type parameters are:
> ·         IEEE754Compatible
> ·         metadata
> The names and values of these parameters are case-insensitive."

**§2.1.1 Controlling the Representation of Numbers** (bears on `$DefaultValue` and constant annotation values):
> "The IEEE754Compatible=true parameter indicates that the service MUST serialize Edm.Int64 and Edm.Decimal numbers as strings. This is in conformance with [RFC7493]. If not specified, or specified as IEEE754Compatible=false, all numbers MUST be serialized as JSON numbers."

> "Responses that format Edm.Int64 and Edm.Decimal values as strings MUST specify this parameter in the media type returned in the Content-Type header."

**§2.1.2 Controlling the Amount of Control Information** (bears on the `@type` written into annotation values):
> "A client application can use the metadata format parameter in the Accept header when requesting a CSDL JSON document to influence how much control information will be included in the response."

- §2.1.2.1: "The metadata=minimal format parameter indicates that the service SHOULD remove computable control information from the payload wherever possible." — SHOULD
- §2.1.2.2: "The metadata=full format parameter indicates that the service MUST include all control information explicitly in the payload." — MUST
- §2.1.2.3: "The metadata=none format parameter indicates that the service SHOULD omit all control information." — SHOULD

**Verbatim ([OData-Protocol] §11.1.2 Metadata Document Request):**
> "An OData metadata document is a representation of the data model that describes the data and operations exposed by an OData service."

> "[OData-CSDLJSON] describes a JSON representation for OData metadata documents and provides a JSON schema to validate their contents. The media type of the JSON representation of an OData metadata document is application/json."

> "[OData-CSDLXML] describes an XML representation for OData metadata documents and provides an XML schema to validate their contents. The media type of the XML representation of an OData metadata document is application/xml."

> "OData services can expose a metadata document that describes the data model exposed by the service. The metadata document URL MUST be the root URL of the service with $metadata appended. To retrieve this document the client issues a GET request to the metadata document URL."

> "If a request for metadata does not specify a format preference (via Accept header or $format) then the XML representation MUST be returned."

**(d) Strength:** MUST for the response `Content-Type`, for "the `json` abbreviation MUST NOT be followed by media type parameters", and — critically — **XML is the default** when no format preference is given.

**Conformance lines ([OData-Protocol] §13):**
- §13.1.1 (4.0 Minimal): "13.  SHOULD publish metadata at $metadata according to [OData-CSDLXML] and MAY publish metadata according to [OData-CSDLJSON] (section 11.1.2)"
- §13.1.2 (4.0 Intermediate): "8.     SHOULD publish metadata at $metadata according to [OData-CSDLXML] (section 11.1.2)"
- §13.1.3 (4.0 Advanced): "2.     MUST publish metadata at $metadata according to [OData-CSDLXML] (section 11.1.2)"
- §13.2.1 (4.01 Minimal): "10.  SHOULD publish metadata at $metadata according to both [OData-CSDLXML] and [OData-CSDLJSON] (section 11.1.2)"
- §13.2.3 (4.01 Advanced): "6.     MUST publish metadata at $metadata according to [OData-CSDLJSON] (section 11.1.2)"

**(d)** CSDL JSON is MAY at 4.0 Minimal, SHOULD at 4.01 Minimal, and **MUST at OData 4.01 Advanced** — it is required only for 4.01 Advanced conformance.

### 1.2 Foundational concepts

**§3.1 Nominal Types:**
> "A nominal type has a name that MUST be a simple identifier. Nominal types are referenced using their qualified name. The qualified type name MUST be unique within a model as it facilitates references to the element from other parts of the model."

> "Names are case-sensitive, but service authors SHOULD NOT choose names that differ only in case."

**§3.6 Annotations:**
> "Many parts of the model can be decorated with additional information using annotations. Annotations are identified by their term name and an optional qualifier that allows applying the same term multiple times to the same model element."

> "A model element MUST NOT specify more than one annotation for a given combination of term and qualifier."

**§2.2 Design Considerations** (non-normative but load-bearing for a writer):
> "To avoid name collisions, all fixed member names are prefixed with a dollar ($) sign and otherwise have the same name and capitalization as their counterparts in the CSDL XML representation [OData-CSDLXML] (with one exception: the counterpart of the EntitySet element's EntityType attribute is $Type, to harmonize it with all other type references)."

> "While the XML representation of CSDL allows referencing model elements with alias-qualified names as well as with namespace-qualified names, this JSON representation requires the use of alias-qualified names if an alias is specified for an included or document-defined schema."

> "In general, all members that have a default value SHOULD be omitted if they have the default value."

### 1.3 The document object (§4)

**(b) Verbatim:**
> "A CSDL JSON document consists of a single JSON object. This document object MUST contain the member $Version."

> "The document object MAY contain the member $Reference to reference other CSDL documents."

> "It also MAY contain members for schemas."

> "If the CSDL JSON document is the metadata document of an OData service, the document object MUST contain the member $EntityContainer."

> "The value of $Version is a string containing either 4.0 or 4.01."

> "The value of $EntityContainer is value is the namespace-qualified name of the entity container of that service. This is the only place where a model element MUST be referenced with its namespace-qualified name and use of the alias-qualified name is not allowed."

*(sic — "is value is" is verbatim in the spec text.)*

**(c) Example (Example 2):**
```json
{
  "$Version": "4.01",
  "$EntityContainer": "org.example.DemoService",
  …
}
```

**(d)** `$Version` MUST always; `$EntityContainer` MUST for a service metadata document only; `$Reference` and schema members MAY. The namespace-qualified (never alias-qualified) rule for `$EntityContainer` is a MUST and is unique in the whole format.

**§4.1 Reference — `$Reference`:**
> "A reference MUST specify a URI that uniquely identifies the referenced document, so two references MUST NOT specify the same URI. The URI SHOULD be a URL that locates the referenced document. If the URI is not dereferencable it SHOULD identify a well-known schema. The URI MAY be absolute or relative URI; relative URLs are relative to the URL of the document containing the reference, or relative to a base URL specified in a format-specific way."

> "The value of $Reference is an object that contains one member per referenced CSDL document. The name of the pair is a URI for the referenced document. The URI MAY be relative to the document containing the $Reference. The value of each member is a reference object."

> "The reference object MAY contain the members $Include and $IncludeAnnotations as well as annotations."

> "The Core.SchemaVersion annotation, defined in [OData-VocCore], MAY be used to indicate a particular version of the referenced document. If the Core.SchemaVersion annotation is present, the $schemaversion system query option, defined [OData‑Protocol], SHOULD be used when retrieving the referenced schema document."

**§4.2 Included Schema — `$Include` / `$Namespace` / `$Alias`:**
> "The included schemas are identified via their namespace. The same namespace MUST NOT be included more than once, even if it is declared in more than one referenced document."

> "If an included schema specifies an alias, the alias MUST be used in qualified names throughout the document to identify model elements of the included schema. A mixed use of namespace-qualified names and alias-qualified names is not allowed."

> "Aliases are document-global, so all schemas defined within or included into a document MUST have different aliases, and aliases MUST differ from the namespaces of all schemas defined within or included into a document."

> "The alias MUST NOT be one of the reserved values Edm, odata, System, or Transient."

> "The value of $Include is an array. Array items are objects that MUST contain the member $Namespace and MAY contain the member $Alias."

> "The item objects MAY contain annotations."

> "The value of $Namespace is a string containing the namespace of the included schema."

> "The value of $Alias is a string containing the alias for the included schema."

**(c) Example (Example 4, excerpt):**
```json
"http://vocabs.odata.org/core/v1": {
  "$Include": [
    {
      "$Namespace": "Org.OData.Core.V1",
      "$Alias": "Core",
      "@Core.DefaultNamespace": true
    }
  ]
}
```

**§4.3 Included Annotations — `$IncludeAnnotations` / `$TermNamespace` / `$Qualifier` / `$TargetNamespace`:**
> "The value of $IncludeAnnotations is an array. Array items are objects that MUST contain the member $TermNamespace and MAY contain the members $Qualifier and $TargetNamespace."

> "The value of $TermNamespace is a namespace."

> "The value of $Qualifier is a simple identifier."

> "The value of $TargetNamespace is a namespace."

> "If a qualifier is specified, only those annotations from the specified term namespace with the specified qualifier (applied to a model element of the target namespace, if present) SHOULD be included. If no qualifier is specified, all annotations within the referenced document from the specified term namespace (taking into account the target namespace, if present) SHOULD be included."

> "If a target namespace is specified, only those annotations which apply a term form the specified term namespace to a model element of the target namespace (with the specified qualifier, if present) SHOULD be included. If no target namespace is specified, all annotations within the referenced document from the specified term namespace (taking into account the qualifier, if present) SHOULD be included."

**(d)** `$TermNamespace` MUST; `$Qualifier`/`$TargetNamespace` MAY; the filtering behaviour itself is SHOULD.

### 1.4 Schema (§5)

> "A schema is identified by a namespace. Schema namespaces MUST be unique within the scope of a document and SHOULD be globally unique. A schema cannot span more than one document."

> "The namespace MUST NOT be one of the reserved values Edm, odata, System, or Transient."

> "A schema is represented as a member of the document object whose name is the schema namespace. Its value is an object that MAY contain the members $Alias and $Annotations."

> "The schema object MAY contain members representing entity types, complex types, enumeration types, type definitions, actions, functions, terms, and an entity container."

> "The schema object MAY also contain annotations that apply to the schema itself."

**§5.1 Alias — `$Alias`:**
> "A schema MAY specify an alias which MUST be a simple identifier."

> "If a schema specifies an alias, the alias MUST be used instead of the namespace within qualified names throughout the document to identify model elements of that schema. A mixed use of namespace-qualified names and alias-qualified names is not allowed."

> "Aliases are document-global, so all schemas defined within or included into a document MUST have different aliases, and aliases MUST differ from the namespaces of all schemas defined within or included into a document. Aliases defined by a schema can be used throughout the containing document and are not restricted to the schema that defines them."

> "The value of $Alias is a string containing the alias for the schema."

**§5.2 Annotations with External Targeting — `$Annotations`:**
> "The value of $Annotations is an object with one member per annotation target. The member name is a path identifying the annotation target, the member value is an object containing annotations for that target."

**(c) Example (Example 7):**
```json
"org.example": {
  "$Alias": "self",
  "$Annotations": {
    "self.Person": {
      "@Core.Description#Tablet": "Dummy",
      …
    }
  }
},
```

### 1.5 Entity Type (§6)

> "An entity type is represented as a member of the schema object whose name is the unqualified name of the entity type and whose value is an object."

> "The entity type object MUST contain the member $Kind with a string value of EntityType."

> "It MAY contain the members $BaseType, $Abstract, $OpenType, $HasStream, and $Key."

> "It also MAY contain members representing structural properties and navigation properties as well as annotations."

> "All properties MUST have a unique name within an entity type. Properties MUST NOT have the same name as the declaring entity type. They MAY have the same name as one of the direct or indirect base types or derived types."

**(c) Example (Example 8):**
```json
"Employee": {
  "$Kind": "EntityType",
  "$Key": [
    "ID"
  ],
  "ID": {},
  "FirstName": {},
  "LastName": {},
  "Manager": {
    "$Kind": "NavigationProperty",
    "$Nullable": true,
    "$Type": "self.Manager"
  }
}
```

**§6.1 Derived Entity Type — `$BaseType`:**
> "An entity type inherits the key as well as structural and navigation properties of its base type."

> "An entity type MUST NOT introduce an inheritance cycle by specifying a base type."

> "The value of $BaseType is the qualified name of the base type."

**§6.2 Abstract Entity Type — `$Abstract`:**
> "An entity type MAY indicate that it is abstract and cannot have instances."

> "For OData 4.0 responses a non-abstract entity type MUST define a key or derive from a base type with a defined key."

> "An abstract entity type MUST NOT inherit from a non-abstract entity type."

> "The value of $Abstract is one of the Boolean literals true or false. Absence of the member means false."

**§6.3 Open Entity Type — `$OpenType`:**
> "An entity type MAY indicate that it is open and allows clients to add properties dynamically to instances of the type by specifying uniquely named property values in the payload used to insert or update an instance of the type."

> "An entity type derived from an open entity type MUST indicate that it is also open."

> "The value of $OpenType is one of the Boolean literals true or false. Absence of the member means false."

**§6.4 Media Entity Type — `$HasStream`:**
> "An entity type that does not specify a base type MAY indicate that it is a media entity type."

> "An entity type derived from a media entity type MUST indicate that it is also a media entity type."

> "Media entity types MAY specify a list of acceptable media types using an annotation with term Core.AcceptableMediaTypes, see [OData-VocCore]."

> "The value of $HasStream is one of the Boolean literals true or false. Absence of the member means false."

**§6.5 Key — `$Key`:**
> "An entity is uniquely identified within an entity set by its key. A key MAY be specified if the entity type does not specify a base type that already has a key declared."

> "In order to be specified as the type of an entity set or a collection-valued containment navigation property, the entity type MUST either specify a key or inherit its key from its base type."

> "In OData 4.01 responses entity types used for singletons or single-valued navigation properties do not require a key. In OData 4.0 responses entity types used for singletons or single-valued navigation properties MUST have a key defined."

> "An entity type's key refers to the set of properties whose values uniquely identify an instance of the entity type within an entity set. The key MUST consist of at least one property."

> "Key properties MUST NOT be nullable and MUST be typed with an enumeration type, one of the following primitive types, or a type definition based on one of these primitive types:
> - Edm.Boolean
> - Edm.Byte
> - Edm.Date
> - Edm.DateTimeOffset
> - Edm.Decimal
> - Edm.Duration
> - Edm.Guid
> - Edm.Int16
> - Edm.Int32
> - Edm.Int64
> - Edm.SByte
> - Edm.String
> - Edm.TimeOfDay"

> "A key property MUST be a non-nullable primitive property of the entity type itself, including non-nullable primitive properties of non-nullable single-valued complex properties, recursively."

> "In OData 4.01 the key properties of a directly related entity type MAY also be part of the key if the navigation property is single-valued and not nullable. This includes navigation properties of non-nullable single-valued complex properties (recursively) of the entity type. If a key property of a related entity type is part of the key, all key properties of the related entity type MUST also be part of the key."

> "If the key property is a property of a complex property (recursively) or of a directly related entity type, the key MUST specify an alias for that property that MUST be a simple identifier and MUST be unique within the set of aliases, structural and navigation properties of the declaring entity type and any of its base types."

> "An alias MUST NOT be defined if the key property is a primitive property of the entity type itself."

> "For key properties that are a property of a complex or navigation property, the alias MUST be used in the key predicate of URLs instead of the path to the property because the required percent-encoding of the forward slash separating segments of the path to the property would make URL construction and parsing rather complicated. The alias MUST NOT be used in the query part of URLs, where paths to properties don't require special encoding and are a standard constituent of expressions anyway."

> "The value of $Key is an array with one item per key property."

> "Key properties without a key alias are represented as strings containing the property name."

> "Key properties with a key alias are represented as objects with one member whose name is the key alias and whose value is a string containing the path to the property."

**(c) Example (Example 11 — the alias/object form):**
```json
"Category": {
  "$Kind": "EntityType",
  "$Key": [
    {
      "EntityInfoID": "Info/ID"
    }
  ],
  "Info": {
    "$Type": "self.EntityInfo"
  },
  "Name": {
    "$Nullable": true
  }
},
```

**(d)** `$Kind` MUST; every other entity-type member MAY. Within `$Key`, the alias form is a MUST when the key property is reached through a complex/navigation path, and MUST NOT be used otherwise — a hard two-way constraint.

### 1.6 Structural Property (§7)

> "A structural property MUST specify a unique name as well as a type."

> "Structural properties are represented as members of the object representing a structured type. The member name is the property name, the member value is an object."

> "The property object MAY contain the member $Kind with a string value of Property. This member SHOULD be omitted to reduce document size."

> "It MAY contain the member $Type, $Collection, $Nullable, $MaxLength, $Unicode, $Precision, $Scale, $SRID, and $DefaultValue."

> "It also MAY contain annotations."

**§7.1 Type — `$Type` / `$Collection`:**
> "The property's type MUST be a primitive type, complex type, or enumeration type in scope, or a collection of one of these types."

> "For single-valued properties the value of $Type is the qualified name of the property's type."

> "For collection-valued properties the value of $Type is the qualified name of the property's item type, and the member $Collection MUST be present with the literal value true."

> "Absence of the $Type member means the type is Edm.String. This member SHOULD be omitted for string properties to reduce document size."

**(c) Example (Example 16):**
```json
"Units": {
  "$Collection": true
}
```

**(d)** `$Kind: "Property"` is MAY and SHOULD-omitted (unlike navigation properties, where `$Kind` is MUST). `$Type` defaults to `Edm.String`. `$Collection` is MUST when the property is collection-valued.

**§7.2 Type Facets:**
> "Facets modify or constrain the acceptable values of a property."

> "For single-valued properties the facets apply to the value of the property. For collection-valued properties the facets apply to the items in the collection."

- **§7.2.1 `$Nullable`:**
  > "The value of $Nullable is one of the Boolean literals true or false. Absence of the member means false."

  > "For collection-valued properties the property value will always be a collection that MAY be empty. In this case $Nullable applies to items of the collection and specifies whether the collection MAY contain null values."
- **§7.2.2 `$MaxLength`:**
  > "The value of $MaxLength is a positive integer."

  > "If no maximum length is specified, clients SHOULD expect arbitrary length."

  > "Note: [OData-CSDLXML] defines a symbolic value max that is only allowed in OData 4.0 responses. This symbolic value is not allowed in CDSL JSON documents at all. Services MAY instead specify the concrete maximum length supported for the type by the service or omit the member entirely."

  *(sic — "CDSL" typo verbatim.)* Implementation note: `max` MUST NOT appear in CSDL JSON at all.
- **§7.2.3 `$Precision`:**
  > "For a decimal value: the maximum number of significant decimal digits of the property's value; it MUST be a positive integer."

  > "For a temporal value (datetime-with-timezone-offset, duration, or time-of-day): the number of decimal places allowed in the seconds portion of the value; it MUST be a non-negative integer between zero and twelve."

  > "The value of $Precision is a number."

  > "Absence of $Precision means arbitrary precision."
- **§7.2.4 `$Scale`:**
  > "A non-negative integer value specifying the maximum number of digits allowed to the right of the decimal point, or one of the symbolic values floating or variable."

  > "The value floating means that the decimal property represents a decimal floating-point number whose number of significant digits is the value of the Precision facet. OData 4.0 responses MUST NOT specify the value floating."

  > "The value variable means that the number of digits to the right of the decimal point can vary from zero to the value of the Precision facet."

  > "The value of Scale MUST be less than or equal to the value of Precision."

  > "The value of $Scale is a number or a string with one of the symbolic values floating or variable."

  > "Services SHOULD use lower-case values; clients SHOULD accept values in a case-insensitive manner."

  > "Absence of $Scale means variable."

  **(c) Example (Example 21, floating scale):**
  ```json
  "Amount7f": {
    "$Nullable": true,
    "$Type": "Edm.Decimal",
    "$Precision": 7,
    "$Scale": "floating"
  }
  ```
- **§7.2.5 `$Unicode`:**
  > "If no value is specified, the Unicode facet defaults to true."

  > "The value of $Unicode is one of the Boolean literals true or false. Absence of the member means true."
- **§7.2.6 `$SRID`:**
  > "The value of the SRID facet MUST be a non-negative integer or the special value variable. If no value is specified, the facet defaults to 0 for Geometry types or 4326 for Geography types."

  > "The value of $SRID is a string containing a number or the symbolic value variable."

  Implementation note: `$SRID` is a **string** even when it holds a number — unlike `$Precision`/`$Scale`/`$MaxLength`.
- **§7.2.7 `$DefaultValue`:**
  > "A primitive or enumeration property MAY define a default value that is used if the property is not explicitly represented in an annotation or the body of a request or response."

  > "If no value is specified, the client SHOULD NOT assume a default value."

  > "The value of $DefaultValue is the type-specific JSON representation of the default value of the property, see [OData-JSON]. For properties of type Edm.Decimal and Edm.Int64 the representation depends on the media type parameter IEEE754Compatible."

**(d)** All facet members are MAY/optional; the constraints on their **values** (positive integer, Scale ≤ Precision, no `max`, no `floating` in 4.0) are MUST.

### 1.7 Navigation Property (§8)

> "Navigation properties are represented as members of the object representing a structured type. The member name is the property name, the member value is an object."

> "The navigation property object MUST contain the member $Kind with a string value of NavigationProperty."

> "It MUST contain the member $Type, and it MAY contain the members $Collection, $Nullable, $Partner, $ContainsTarget, $ReferentialConstraint, and $OnDelete."

> "It also MAY contain annotations."

**§8.1 `$Type` / `$Collection`:**
> "The navigation property's type MUST be an entity type in scope, the abstract type Edm.EntityType, or a collection of one of these types."

> "For single-valued navigation properties the value of $Type is the qualified name of the navigation property's type."

> "For collection-valued navigation properties the value of $Type is the qualified name of the navigation property's item type, and the member $Collection MUST be present with the literal value true."

Implementation note: unlike structural properties, `$Type` is **required** (MUST) on navigation properties — there is no `Edm.String` default.

**§8.2 `$Nullable`:**
> "Nullable MUST NOT be specified for a collection-valued navigation property, a collection is allowed to have zero items."

> "The value of $Nullable is one of the Boolean literals true or false. Absence of the member means false."

**§8.3 `$Partner`:**
> "A navigation property of an entity type MAY specify a partner navigation property. Navigation properties of complex types MUST NOT specify a partner."

> "If specified, the partner navigation property is identified by a path relative to the entity type specified as the type of the navigation property. This path MUST lead to a navigation property defined on that type or a derived type. The path MAY traverse complex types, including derived complex types, but MUST NOT traverse any navigation properties. The type of the partner navigation property MUST be the declaring entity type of the current navigation property or one of its parent entity types."

> "The value of $Partner is a string containing the path to the partner navigation property."

**§8.4 `$ContainsTarget`:**
> "A navigation property MAY indicate that instances of its declaring structured type contain the targets of the navigation property, in which case the navigation property is called a containment navigation property."

> "Entity types used in collection-valued containment navigation properties MUST have a key defined."

> "Containment navigation properties MUST NOT be specified as the last path segment in the path of a navigation property binding."

> "The value of $ContainsTarget is one of the Boolean literals true or false. Absence of the member means false."

**§8.5 `$ReferentialConstraint`:**
> "A single-valued navigation property MAY define one or more referential constraints. A referential constraint asserts that the dependent property (the property defined on the structured type declaring the navigation property) MUST have the same value as the principal property (the referenced property declared on the entity type that is the target of the navigation)."

> "The type of the dependent property MUST match the type of the principal property, or both types MUST be complex types."

> "If the navigation property on which the referential constraint is defined is nullable, or the principal property is nullable, then the dependent property MUST also be nullable. If both the navigation property and the principal property are not nullable, then the dependent property MUST NOT be nullable."

> "The value of $ReferentialConstraint is an object with one member per referential constraint. The member name is the path to the dependent property, this path is relative to the structured type declaring the navigation property. The member value is a string containing the path to the principal property, this path is relative to the entity type that is the target of the navigation property."

> "It also MAY contain annotations. These are prefixed with the path of the dependent property of the annotated referential constraint."

**(c) Example (Example 23, excerpt — note the spec's own missing comma after `"Kind"`):**
```json
"$ReferentialConstraint": {
  "CategoryID": "ID",
  "CategoryKind": "Kind"
  "CategoryID@Core.Description": "Referential Constraint to non-key property"
}
```

**§8.6 `$OnDelete`:**
> "The action can have one of the following values:
> - Cascade, meaning the related entities will be deleted if the source entity is deleted,
> - None, meaning a DELETE request on a source entity with related entities will fail,
> - SetNull, meaning all properties of related entities that are tied to properties of the source entity via a referential constraint and that do not participate in other referential constraints will be set to null,
> - SetDefault, meaning all properties of related entities that are tied to properties of the source entity via a referential constraint and that do not participate in other referential constraints will be set to their default value."

> "The value of $OnDelete is a string with one of the values Cascade, None, SetNull, or SetDefault."

> "Annotations for $OnDelete are prefixed with $OnDelete."

**(c) Example (Example 24, excerpt):**
```json
"Products": {
  "$Kind": "NavigationProperty",
  "$Collection": true,
  "$Type": "self.Product",
  "$Partner": "Category",
  "$OnDelete": "Cascade",
  "$OnDelete@Core.Description": "Delete all products in this category"
}
```

### 1.8 Complex Type (§9)

> "Complex types are keyless nominal structured types."

> "A complex type is represented as a member of the schema object whose name is the unqualified name of the complex type and whose value is an object."

> "The complex type object MUST contain the member $Kind with a string value of ComplexType. It MAY contain the members $BaseType, $Abstract, and $OpenType. It also MAY contain members representing structural properties and navigation properties as well as annotations."

- §9.1: "The value of $BaseType is the qualified name of the base type." / "A complex type MUST NOT introduce an inheritance cycle by specifying a base type."
- §9.2: "The value of $Abstract is one of the Boolean literals true or false. Absence of the member means false."
- §9.3: "A complex type derived from an open complex type MUST indicate that it is also open." / "The value of $OpenType is one of the Boolean literals true or false. Absence of the member means false."

**(d)** `$Kind` MUST; everything else MAY. Complex types have **no** `$HasStream` and **no** `$Key`.

### 1.9 Enumeration Type (§10)

> "An enumeration type is represented as a member of the schema object whose name is the unqualified name of the enumeration type and whose value is an object."

> "The enumeration type object MUST contain the member $Kind with a string value of EnumType."

> "It MAY contain the members $UnderlyingType and $IsFlags."

> "The enumeration type object MUST contain members representing the enumeration type members."

> "The enumeration type object MAY contain annotations."

**§10.1 `$UnderlyingType`:**
> "An enumeration type MAY specify one of Edm.Byte, Edm.SByte, Edm.Int16, Edm.Int32, or Edm.Int64 as its underlying type."

> "If not explicitly specified, Edm.Int32 is used as the underlying type."

> "The value of $UnderlyingType is the qualified name of the underlying type."

**§10.2 `$IsFlags`:**
> "An enumeration type MAY indicate that the enumeration type allows multiple members to be selected simultaneously."

> "If not explicitly specified, only one enumeration type member MAY be selected simultaneously."

> "The value of $IsFlags is one of the Boolean literals true or false. Absence of the member means false."

**§10.3 Enumeration Type Member:**
> "Each member MUST specify an associated numeric value that MUST be a valid value for the underlying type of the enumeration type."

> "Enumeration types can have multiple members with the same value. Members with the same numeric value compare as equal, and members with the same numeric value can be used interchangeably."

> "Enumeration members are sorted by their numeric value."

> "For flag enumeration types the combined numeric value of simultaneously selected members is the bitwise OR of the discrete numeric member values."

> "Enumeration type members are represented as JSON object members, where the object member name is the enumeration member name and the object member value is the enumeration member value."

> "Annotations for enumeration members are prefixed with the enumeration member name."

**(c) Example (Example 26):**
```json
"FileAccess": {
  "$Kind": "EnumType",
  "$UnderlyingType": "Edm.Int32",
  "$IsFlags": true,
  "Read": 1,
  "Write": 2,
  "Create": 4,
  "Delete": 8
}
```

### 1.10 Type Definition (§11)

> "A type definition is represented as a member of the schema object whose name is the unqualified name of the type definition and whose value is an object."

> "The type definition object MUST contain the member $Kind with a string value of TypeDefinition and the member $UnderlyingType. It MAY contain the members $MaxLength, $Unicode, $Precision, $Scale, and $SRID, and it MAY contain annotations."

**§11.1 `$UnderlyingType`:**
> "The underlying type of a type definition MUST be a primitive type that MUST NOT be another type definition."

> "The value of $UnderlyingType is the qualified name of the underlying type."

> "The type definition MAY specify facets applicable to the underlying type. Possible facets are: $MaxLength, $Unicode, $Precision, $Scale, or $SRID."

> "Additional facets appropriate for the underlying type MAY be specified when the type definition is used but the facets specified in the type definition MUST NOT be re-specified."

> "For a type definition with underlying type Edm.PrimitiveType no facets are applicable, neither in the definition itself nor when the type definition is used, and these should be ignored by the client."

**(c) Example (Example 29, excerpt):**
```json
"Length": {
  "$Kind": "TypeDefinition",
  "$UnderlyingType": "Edm.Int32",
  "@Measures.Unit": "Centimeters"
}
```

### 1.11 Action and Function (§12)

**§12.2 Action Overloads:**
> "An action is represented as a member of the schema object whose name is the unqualified name of the action and whose value is an array. The array contains one object per action overload."

> "The action overload object MUST contain the member $Kind with a string value of Action."

> "It MAY contain the members $IsBound, $EntitySetPath, $Parameter, and $ReturnType, and it MAY contain annotations."

> "Bound actions support overloading (multiple actions having the same name within the same schema) by binding parameter type. The combination of action name and the binding parameter type MUST be unique within a schema."

> "Unbound actions do not support overloads. The names of all unbound actions MUST be unique within a schema."

**§12.4 Function Overloads:**
> "A function is represented as a member of the schema object whose name is the unqualified name of the function and whose value is an array. The array contains one object per function overload."

> "The function overload object MUST contain the member $Kind with a string value of Function."

> "It MUST contain the member $ReturnType, and it MAY contain the members $IsBound, $EntitySetPath, and $Parameter, and it MAY contain annotations."

Implementation note: the array-of-overloads shape is the same for both, but `$ReturnType` is **MUST for functions, MAY for actions**. Cf. §12.3: "Functions ... MUST NOT have observable side effects and MUST return a single instance or a collection of instances of any type." vs §12.1: "Actions are service-defined operations that MAY have observable side effects".

**§12.5 `$IsBound`:**
> "An action or function overload MAY indicate that it is bound. If not explicitly indicated, it is unbound."

> "Bound actions or functions are invoked on resources matching the type of the binding parameter. The binding parameter can be of any type, and it MAY be nullable."

> "The value of $IsBound is one of the Boolean literals true or false. Absence of the member means false."

**§12.6 `$EntitySetPath`:**
> "Bound actions and functions that return an entity or a collection of entities MAY specify an entity set path if the entity set of the returned entities depends on the entity set of the binding parameter value."

> "The first segment of the entity set path MUST be the name of the binding parameter. The remaining segments of the entity set path MUST represent navigation segments or type casts."

> "The value of $EntitySetPath is a string containing the entity set path."

**§12.7 `$IsComposable`:**
> "A function MAY indicate that it is composable. If not explicitly indicated, it is not composable."

> "The value of $IsComposable is one of the Boolean literals true or false. Absence of the member means false."

**§12.8 `$ReturnType`:**
> "The value of $ReturnType is an object. It MAY contain the members $Type, $Collection, $Nullable, $MaxLength, $Unicode, $Precision, $Scale, and $SRID."

> "It also MAY contain annotations."

> "For single-valued return types the value of $Type is the qualified name of the returned type."

> "For collection-valued return types the value of $Type is the qualified name of the returned item type, and the member $Collection MUST be present with the literal value true."

> "Absence of the $Type member means the type is Edm.String."

> "The value of $Nullable is one of the Boolean literals true or false. Absence of the member means false."

> "If the return type is a collection of entity types, the $Nullable member has no meaning and MUST NOT be specified."

> "The facets Nullable, MaxLength, Precision, Scale, and SRID can be used as appropriate to specify value restrictions of the return type, as well as the Unicode facet for 4.01 and greater payloads."

**§12.9 `$Parameter`:**
> "A bound action or function overload MUST specify at least one parameter; the first parameter is its binding parameter. The order of parameters MUST NOT change unless the schema version changes."

> "Each parameter MUST have a name that is a simple identifier. The parameter name MUST be unique within the action or function overload."

> "The parameter MUST specify a type. It MAY be any type in scope, or a collection of any type in scope."

> "The value of $Parameter is an array. The array contains one object per parameter."

> "A parameter object MUST contain the member $Name, and it MAY contain the members $Type, $Collection, $Nullable, $MaxLength, $Unicode, $Precision, $Scale, and $SRID."

> "Parameter objects MAY also contain annotations."

> "The value of $Name is a string containing the parameter name."

> "Absence of the $Type member means the type is Edm.String."

**(c) Example (Example 30, excerpt):**
```json
"TopSellingProducts": [
  {
    "$Kind": "Function",
    "$Parameter": [
      {
        "$Name": "Year",
        "$Nullable": true,
        "$Type": "Edm.Decimal",
        "$Precision": 4,
        "$Scale": 0
      }
    ]
  }
]
```

Implementation note: parameters are an **ordered array of objects carrying `$Name`**, not name-keyed members — unlike properties, entity sets, etc.

### 1.12 Entity Container (§13)

> "Each metadata document used to describe an OData service MUST define exactly one entity container."

> "Entity set, singleton, action import, and function import names MUST be unique within an entity container."

> "An entity container is represented as a member of the schema object whose name is the unqualified name of the entity container and whose value is an object."

> "The entity container object MUST contain the member $Kind with a string value of EntityContainer."

> "The entity container object MAY contain the member $Extends, members representing entity sets, singletons, action imports, and function imports, as well as annotations."

**§13.1 `$Extends`:**
> "An entity container MAY specify that it extends another entity container in scope. All children of the "base" entity container are added to the "extending" entity container."

> "If the "extending" entity container defines an entity set with the same name as defined in any of its "base" containers, then the entity set's type MUST specify an entity type derived from the entity type specified for the identically named entity set in the "base" container. The same holds for singletons. Action imports and function imports cannot be redefined, nor can the "extending" container define a child with the same name as a child of a different kind in a "base" container."

> "The value of $Extends is the qualified name of the entity container to be extended."

> "Note: services should not introduce cycles by extending entity containers. Clients should be prepared to process cycles introduced by extending entity containers."

**§13.2 Entity Set:**
> "An entity set MUST specify a type that MUST be an entity type in scope."

> "An entity set MUST contain only instances of its specified entity type or its subtypes. The entity type MAY be abstract but MUST have a key defined."

> "An entity set MAY indicate whether it is included in the service document. If not explicitly indicated, it is included."

> "Entity sets that cannot be queried without specifying additional query options SHOULD NOT be included in the service document."

> "An entity set is represented as a member of the entity container object whose name is the name of the entity set and whose value is an object."

> "The entity set object MUST contain the members $Collection and $Type."

> "It MAY contain the members $IncludeInServiceDocument and $NavigationPropertyBinding as well as annotations."

> "The value of $Collection is the Booelan value true."

*(sic — "Booelan" typo verbatim.)*

> "The value of $Type is the qualified name of an entity type."

> "The value of $IncludeInServiceDocument is one of the Boolean literals true or false. Absence of the member means true."

**§13.3 Singleton:**
> "A singleton MUST specify a type that MUST be an entity type in scope."

> "A singleton MUST reference an instance its entity type."

> "A singleton is represented as a member of the entity container object whose name is the name of the singleton and whose value is an object."

> "The singleton object MUST contain the member $Type and it MAY contain the member $Nullable."

> "It MAY contain the member $NavigationPropertyBinding as well as annotations."

> "The value of $Nullable is one of the Boolean literals true or false. Absence of the member means false.In OData 4.0 responses this member MUST NOT be specified."

*(sic — the missing space after "false." is verbatim.)* Implementation note: a singleton has **no** `$Collection`, and `$Nullable` is 4.01-only (MUST NOT in 4.0).

**§13.4 Navigation Property Binding:**
> "An entity set or a singleton SHOULD specify a navigation property binding for each navigation property of its entity type, including navigation properties defined on complex typed properties or derived types."

> "If omitted, clients MUST assume that the target entity set or singleton can vary per related entity."

§13.4.1 Navigation Property Path Binding:
> "A navigation property binding MUST specify a path to a navigation property of the entity set's or singleton's declared entity type, or a navigation property reached through a chain of type casts, complex properties, or containment navigation properties."

> "The path can traverse one or more containment navigation properties, but the last navigation property segment MUST be a non-containment navigation property and there MUST NOT be any non-containment navigation properties prior to the final navigation property segment."

> "OData 4.01 services MAY have a type-cast segment as the last path segment, allowing to bind instances of different sub-types to different targets."

§13.4.2 Binding Target:
> "A navigation property binding MUST specify a target via a simple identifier or target path. It specifies the entity set, singleton, or containment navigation property that contains the related entities."

> "If the target is a simple identifier, it MUST resolve to an entity set or singleton defined in the same entity container."

> "The value of $NavigationPropertyBinding is an object. It consists of members whose name is the navigation property binding path and whose value is a string containing the navigation property binding target. If the target is in the same entity container, the target MUST NOT be prefixed with the qualified entity container name."

**(c) Example (Example 33, excerpt — the whole container object shape):**
```json
"DemoService": {
  "$Kind": "EntityContainer",
  "Products": {
    "$Collection": true,
    "$Type": "self.Product",
    "$NavigationPropertyBinding": {
      "Category": "Categories",
      "Supplier": "Suppliers"
    },
    "@UI.DisplayName": "Product Catalog"
  },
  "MainSupplier": {
    "$Type": "self.Supplier"
  },
  "LeaveRequestApproval": {
    "$Action": "self.Approval"
  },
  "ProductsByRating": {
    "$EntitySet": "Products",
    "$Function": "self.ProductsByRating"
  }
}
```

**§13.5 Action Import:**
> "Action imports sets are top-level resources that are never included in the service document."

> "An action import MUST specify the name of an unbound action in scope."

> "If the imported action returns an entity or a collection of entities, a simple identifier or target path value MAY be specified to identify the entity set that contains the returned entities. If a simple identifier is specified, it MUST resolve to an entity set defined in the same entity container. If a target path is specified, it MUST resolve to an entity set in scope."

> "The action import object MUST contain the member $Action."

> "It MAY contain the member $EntitySet."

> "The value of $Action is a string containing the qualified name of an unbound action."

> "The value of $EntitySet is a string containing either the unqualified name of an entity set in the same entity container or a path to an entity set in a different entity container."

**§13.6 Function Import:**
> "A function import MUST specify the name of an unbound function in scope. All unbound overloads of the imported function can be invoked from the entity container."

> "A function import for a parameterless function MAY indicate whether it is included in the service document. If not explicitly indicated, it is not included."

> "The function import object MUST contain the member $Function."

> "It MAY contain the members $EntitySet and $IncludeInServiceDocument."

> "The value of $Function is a string containing the qualified name of an unbound function."

> "The value of $EntitySet is a string containing either the unqualified name of an entity set in the same entity container or a path to an entity set in a different entity container."

> "The value of $IncludeInServiceDocument is one of the Boolean literals true or false. Absence of the member means false."

**(d) Default-value trap:** `$IncludeInServiceDocument` defaults to **true** for entity sets (§13.2) but **false** for function imports (§13.6). Action imports have no such member at all — they are "never included in the service document".

### 1.13 Annotations (§14.2)

> "An annotation is represented as a member whose name consists of an at (@) character, followed by the qualified name of a term, optionally followed by a hash (#) and a qualifier."

> "The value of the annotation MUST be a constant expression or dynamic expression."

> "The annotation can be a member of the object representing the model element it annotates, or a second-level member of the $Annotations member of a schema object."

> "An annotation can itself be annotated. Annotations on annotations are represented as a member whose name consists of the annotation name (including the optional qualifier), followed by an at (@) character, followed by the qualified name of a term, optionally followed by a hash (#) and a qualifier."

> "Structured types "inherit" annotations from their direct or indirect base types. If both the type and one of its base types is annotated with the same term and qualifier, the annotation on the type completely replaces the annotation on the base type; structured or collection-valued annotation values are not merged. Similarly, properties of a structured type inherit annotations from identically named properties of a base type."

**(c) Example (Example 40 — annotation and annotation-on-annotation):**
```json
"AmountInReportingCurrency": {
  "$Nullable": true,
  "$Type": "Edm.Decimal",
  "$Scale": 0,
  "@Measures.ISOCurrency": "USD",
  "@Measures.ISOCurrency@Core.Description": "The parent company's currency"
},
"AmountInTransactionCurrency": {
  "$Nullable": true,
  "$Type": "Edm.Decimal",
  "$Scale": 0,
  "@Measures.ISOCurrency": {
    "$Path": "Currency"
  }
}
```

**§14.2.1 Qualifier:**
> "A term can be applied multiple times to the same model element by providing a qualifier to distinguish the annotations. The qualifier is a simple identifier."

> "The combination of target model element, term, and qualifier uniquely identifies an annotation."

**§14.2.2 Target:**
> "The target of an annotation MAY be specified indirectly by "nesting" the annotation within the model element. Whether and how this is possible is described per model element in this specification."

> "The target of an annotation MAY also be specified directly; this allows defining an annotation in a different schema than the targeted model element."

> "This external targeting is only possible for model elements that are uniquely identified within their parent, and all their ancestor elements are uniquely identified within their parent:
> - Action (single or all overloads)
> - Action Import
> - Complex Type
> - Entity Container
> - Entity Set
> - Entity Type
> - Enumeration Type
> - Enumeration Type Member
> - Function (single or all overloads)
> - Function Import
> - Navigation Property (via type, entity set, or singleton)
> - Parameter of an action or function (single overloads or all overloads defining the parameter)
> - Property (via type, entity set, or singleton)
> - Return Type of an action or function (single or all overloads)
> - Singleton
> - Type Definition"

Allowed target-path forms (excerpt of the bullet list):
> "·         qualified name of an action followed by parentheses containing the qualified name of the binding parameter type of a bound action overload to identify that bound overload, or by empty parentheses to identify the unbound overload
>
> ·         qualified name of a function followed by parentheses containing the comma-separated list of qualified names of the parameter types of a bound or unbound function overload in the order of their definition in the function overload
>
> ·         qualified name of an action or function, optionally followed by parentheses as described in the two previous bullet points to identify a single overload, followed by a forward slash and either a parameter name or $ReturnType"

> "All qualified names used in a target path MUST be in scope."

**(c) Example (Example 42, excerpt — representative target expressions):**
```
MySchema.MyEntityType/MyProperty
MySchema.MyEnumType/MyMember
MySchema.MyAction(MySchema.MyBindingType)
MySchema.MyAction()
MySchema.MyFunction(First.NonBinding.ParamType,Second.NonBinding.ParamType)
MySchema.MyFunction/MyParameter
MySchema.MyEntityContainer/MyEntitySet/MySchema.MyEntityType/MyProperty
```

### 1.14 Constant Expressions (§14.3)

> "Constant expressions allow assigning a constant value to an applied term."

**Critical implementation point:** in CSDL **JSON** there are **no** `$Binary`/`$Date`/`$DateTimeOffset`/`$Decimal`/`$Duration`/`$EnumMember`/`$Float`/`$Guid`/`$Int`/`$String`/`$TimeOfDay` members — those are CSDL **XML** `edm:*` element names. In JSON every constant is a plain JSON string, boolean, or number, with the type inferred from the term's declared type. Verbatim per subsection:

| § | Type | Verbatim rule |
|---|------|---------------|
| 14.3.1 | Binary | "Binary expressions are represented as a string containing the base64url-encoded binary value." |
| 14.3.2 | Boolean | "Boolean expressions are represented as the literals true or false." |
| 14.3.3 | Date | "Date expressions are represented as a string containing the date value. The value MUST conform to type xs:date, see [XML‑Schema‑2], section 3.3.9. The value MUST also conform to rule dateValue in [OData‑ABNF], i.e. it MUST NOT contain a time-zone offset." |
| 14.3.4 | DateTimeOffset | "Datetimestamp expressions are represented as a string containing the timestamp value. The value MUST conform to type xs:dateTimeStamp, see [XML‑Schema‑2], section 3.4.28. The value MUST also conform to rule dateTimeOffsetValue in [OData‑ABNF], i.e. it MUST NOT contain an end-of-day fragment (24:00:00)." |
| 14.3.5 | Decimal | "Decimal expressions are represented as either a number or a string. The special values INF, -INF, or NaN are represented as strings. Numeric values are represented as numbers or strings depending on the media type parameter IEEE754Compatible." |
| 14.3.6 | Duration | "Duration expressions are represented as a string containing the duration value. The value MUST conform to type xs:dayTimeDuration, see [XML‑Schema‑2], section 3.4.27." |
| 14.3.7 | Enumeration Member | "Enumeration member expressions are represented as a string containing the numeric or symbolic enumeration value." |
| 14.3.8 | Floating-Point | "Floating-point expressions are represented as a number or as a string containing one of the special values INF, -INF, or NaN." |
| 14.3.9 | Guid | "Guid expressions are represented as a string containing the uuid value. The value MUST conform to the rule guidValue in [OData‑ABNF]." |
| 14.3.10 | Integer | "Integer expressions are represented as either a number or a string, depending on the media type parameter IEEE754Compatible." |
| 14.3.11 | String | "String expressions are represented as a JSON string." |
| 14.3.12 | Time of Day | "Time-of-day expressions are represented as a string containing the time-of-day value. The value MUST conform to the rule timeOfDayValue in [OData‑ABNF]." |

**(c) Representative examples, verbatim from the spec:**
```json
"@UI.Thumbnail": "T0RhdGE"                        // Example 43, binary
"@UI.ReadOnly": true                              // Example 44, boolean
"@vCard.birthDay": "2000-01-01"                   // Example 45, date
"@UI.LastUpdated": "2000-01-01T16:00:00.000Z"     // Example 46, datetimeoffset
"@UI.Width": 3.14                                 // Example 47, decimal as number
"@UI.Width": "3.14"                               // Example 48, decimal as string
"@task.duration": "P7D"                           // Example 49, duration
"@self.HasPattern": "Red,Striped"                 // Example 51, combined enum member
"@UI.FloatWidth": "INF"                           // Example 52, special float
"@UI.Id": "21EC2020-3AEA-1069-A2DD-08002B30309D"  // Example 53, guid
"@A.Very.Long.Int": "9007199254740992"            // Example 55, "safe" int as string
"@UI.EndTime": "21:45:00"                         // Example 57, time of day
```

**(d)** The representation choices are stated flatly ("are represented as"); the lexical value constraints are MUST.

### 1.15 Dynamic Expressions (§14.4)

**`$Path` (§14.4.1.7 / §14.4.1.1):**
> "Path expressions are represented as an object with a single member $Path whose value is a string containing a path."

> "A path MUST be composed of zero or more path segments joined together by forward slashes (/)."

> "Paths starting with a forward slash (/) are absolute paths, and the first path segment MUST be the qualified name of a model element, e.g. an entity container. The remaining path after the second forward slash is interpreted relative to that model element."

**§14.4.2 Comparison and Logical Operators.** Operator table (verbatim contents): Logical — `And` (Logical and), `Or` (Logical or), `Not` (Logical negation); Comparison — `Eq` (Equal), `Ne` (Not equal), `Gt` (Greater than), `Ge` (Greater than or equal), `Lt` (Less than), `Le` (Less than or equal), `Has` (Has enumeration flag(s) set), `In` (Is in collection).
> "The And and Or operators require two operand expressions that evaluate to Boolean values. The Not operator requires a single operand expression that evaluates to a Boolean value."

> "The And and Or logical expressions are represented as an object with a single member whose value is an array with two annotation expressions. The member name is one of $And, or $Or."

> "Negation expressions are represented as an object with a single member $Not whose value is an annotation expression."

> "All comparison expressions are represented as an object with a single member whose value is an array with two annotation expressions. The member name is one of $Eq, $Ne, $Gt, $Ge, $Lt, $Le, $Has, or $In."

> "They MAY contain annotations."

**§14.4.4 `$Apply` / `$Function`:**
> "Apply expressions are represented as an object with a member $Apply whose value is an array of annotation expressions, and a member $Function whose value is a string containing the qualified name of the client-side function to be applied."

> "It MAY contain annotations."

> "OData defines the following functions. Services MAY support additional functions that MUST be qualified with a namespace other than odata. Function names qualified with odata are reserved for this specification and its future versions."

**§14.4.5 `$Cast`:**
> "Cast expressions are represented as an object with a member $Cast whose value is an annotation expression, a member $Type whose value is a string containing the qualified type name, and optionally a member $Collection with a value of true."

> "If the specified type is a primitive type or a collection of primitive types, the facet members $MaxLength, $Unicode, $Precision, $Scale, and $SRID MAY be specified if applicable to the specified primitive type. If the facet members are not specified, their values are considered unspecified."

**§14.4.6 Collection:**
> "The collection expression enables a value to be obtained from zero or more item expressions. The value calculated by the collection expression is the collection of the values calculated by each of the item expressions. The values of the child expressions MUST all be type compatible."

> "Collection expressions are represented as arrays with one array item per item expression within the collection expression."

Implementation note: a collection expression is a **bare JSON array** — there is no `$Collection` wrapper member for it.

**§14.4.7 `$If`:**
> "The if-then-else expression enables a value to be obtained by evaluating a condition expression. It MUST contain exactly three child expressions. There is one exception to this rule: if and only if the if-then-else expression is an item of a collection expression, the third child expression MAY be omitted, reducing it to an if-then expression."

> "Conditional expressions are represented as an object with a member $If whose value is an array of two or three annotation expressions."

**§14.4.8 `$IsOf`:**
> "Is-of expressions are represented as an object with a member $IsOf whose value is an annotation expression, a member $Type whose value is a string containing an qualified type name, and optionally a member $Collection with a value of true."

> "If the specified type is a primitive type or a collection of primitive types, the facet members $MaxLength, $Unicode, $Precision, $Scale, and $SRID MAY be specified if applicable to the specified primitive type."

**§14.4.9 `$LabeledElement`:**
> "A labeled element expression MUST contain exactly one child expression. The value of the child expression is also the value of the labeled element expression."

> "A labeled element expression MUST provide a simple identifier value as its name that MUST be unique within the schema containing the expression."

> "Labeled element expressions are represented as an object with a member $LabeledElement whose value is an annotation expression, and a member $Name whose value is a string containing the labeled element's name."

**(c) Example (Example 82):**
```json
"@UI.DisplayName": {
  "$LabeledElement": {
    "$Path": "FirstName"
  },
  "$Name": "CustomerFirstName"
}
```

**§14.4.10 `$LabeledElementReference`:**
> "The labeled element reference expression MUST specify the qualified name of a labeled element expression in scope and returns the value of the identified labeled element expression as its value."

> "Labeled element reference expressions are represented as an object with a member $LabeledElementReference whose value is a string containing an qualified name."

**§14.4.11 `$Null`:**
> "The null expression indicates the absence of a value. The null expression MAY be annotated."

> "Null expressions that do not contain annotations are represented as the literal null."

> "Null expression containing annotations are represented as an object with a member $Null whose value is the literal null."

**(c) Examples (84 and 85):**
```json
"@UI.DisplayName": null,
```
```json
"@UI.Address": {
  "$Null": null,
  "@self.Reason": "Private"
}
```

**§14.4.12 Record:**
> "A record expression MAY specify the structured type of its result, which MUST be an entity type or complex type in scope. If not explicitly specified, the type is derived from the expression's context."

> "A record expression contains zero or more property value expressions. For each single-valued structural or navigation property of the record expression's type that is neither nullable nor specifies a default value a property value expression MUST be provided."

> "For collection-valued properties the absence of a property value expression is equivalent to specifying an empty collection as its value."

> "Record expressions are represented as objects with one member per property value expression. The member name is the property name, and the member value is the property value expression."

> "The type of a record expression is represented as the @type control information, see  [OData‑JSON]."

> "It MAY contain annotations for itself and its members. Annotations for record members are prefixed with the member name."

**(c) Example (Example 86, excerpt):**
```json
"@person.Employee": {
  "@type": "https://example.org/vocabs/person#org.example.person.Manager",
  "@Core.Description": "Annotation on record",
  "GivenName": {
    "$Path": "FirstName"
  },
  "GivenName@Core.Description": "Annotation on record member",
  "Surname": {
    "$Path": "LastName"
  }
}
```

Implementation note: there is **no `$Record` member and no `$Type` member** on a record expression in CSDL JSON — a record is a bare JSON object, and its type is carried by the JSON-format `@type` control information (note the `@type` value in Example 86 is a URI-with-fragment, not a bare qualified name).

**§14.4.13 `$UrlRef`:**
> "The URL reference expression MUST contain exactly one expression of type Edm.String. Its value is treated as a URL that MAY be relative or absolute; relative URLs are relative to the URL of the document containing the URL reference expression, or relative to a base URL specified in a format-specific way."

> "The response body of the GET request MUST be returned as the result of the URL reference expression. The result of the URL reference expression MUST be type compatible with the type expected by the surrounding expression."

> "URL reference expressions are represented as an object with a single member $UrlRef whose value is an annotation expression."

---

## 2. Geo types (OLINGO-918)

Sources used: [OData-JSON] §7.1; [OData-URL] §5.1.1.1, §5.1.1.11, §5.1.1.14, §5.1.1.14.1; [OData-ABNF] geo rules; [OData-Protocol] §11.2.4.1, §11.2.6.2; [OData-CSDLXML]/[OData-CSDLJSON] §7.2.6, §4.1; [GeoJSON] §3.1.1, §3.1.8, §4

### 2.1 GeoJSON representation of Edm.Geography*/Edm.Geometry* values

**(a)** [OData-JSON], §7 Structural Property → **§7.1 Primitive Value**

**(b) Verbatim — this is the complete geo clause of the JSON Format spec:**
> "Geography and geometry values are represented as geometry types as defined in [RFC7946], with the following modifications:
>
> ·         Keys SHOULD be ordered with type first, then coordinates, then any other keys
>
> ·         If the optional CRS object is present, it MUST be of type name, where the value of the name member of the contained properties object is an EPSG SRID legacy identifier, see [GeoJSON-2008].
>
> Geography and geometry types have the same representation in a JSON payload. Whether the value represents a geography type or geometry type is inferred from its usage or specified using the type control information."

**(c) Example** (Example 12, tail of the object, verbatim):
```
  "ColorEnumValue": "Yellow",

  "GeographyPoint": {"type": "Point","coordinates":[142.1,64.1]}

}
```
That is the **only** geo example payload in the JSON Format spec.

Normative references cited by the clause:
> "[RFC7946]               Howard Butler, Martin Daly, Alan Doyle, Sean Gillies, Stefan Hagen and Tim Schaub, \"The GeoJSON Format\", RFC 7946, August 2016."

> "[GeoJSON-2008]     Butler, H., Daly, M., Doyle, A., Gillies, S., Schaub, T., and C. Schmidt, \"The GeoJSON Format Specification\", June 2008. http://geojson.org/geojson-spec.html."

**(d) Strength:**
- **SHOULD** — member ordering `type`, `coordinates`, then others. A parser MUST NOT depend on order.
- **MUST** — *if* a CRS object is present it must be `type: "name"` with `properties.name` = an EPSG SRID legacy identifier ([GeoJSON-2008] style). The CRS object itself is optional ("the optional CRS object"), and RFC 7946 §4 removed CRS entirely — OData 4.01 deliberately re-admits the 2008-style `crs` member as an optional extension.
- No keyword — geography vs geometry is *not* distinguishable from the JSON value; it comes from usage (declared type) or from the `type` control information (`@odata.type` / `@type`).

### 2.2 Geo literals in URLs

**(a)** [OData-URL], **§5.1.1.14.1 Primitive Literals**

**(b) Verbatim:**
> "Primitive literals can appear in the resource path as key property values, and in the query part, for example, as operands in $filter expressions. They are represented according to the primitiveLiteral rule in [OData-ABNF]."

**(c) Example** (the only geo literal shown in Part 2, from Example 102: expressions using primitive literals):
```
geo.distance(Location,geography'SRID=0;Point(142.1 64.1)')
```

Part 2 delegates the whole geo-literal syntax (the `geography'…'` / `geometry'…'` prefix + `SRID=nnn;` + WKT body) to [OData-ABNF] — there is no prose grammar for it in Part 2. See §2.3.

Casting geo to string, [OData-URL] §5.1.1.14 (cast):
> "2.  Primitive types are cast to Edm.String or a type definition based on it by using the literal representation used in payloads, and WKT (well-known text) format for Geo types, see rules fullCollectionLiteral, fullLineStringLiteral, fullMultiPointLiteral, fullMultiLineStringLiteral, fullMultiPolygonLiteral, fullPointLiteral, and fullPolygonLiteral in [OData-ABNF]."

Raw-value format, [OData-Protocol] §11.2.4.1:
> "The default format for Edm.Geo types is text/plain using the WKT (well-known text) format, see rules fullCollectionLiteral, fullLineStringLiteral, fullMultiPointLiteral, fullMultiLineStringLiteral, fullMultiPolygonLiteral, fullPointLiteral, and fullPolygonLiteral in [OData-ABNF]."

> "The default format for single primitive values except Edm.Binary and the Edm.Geo types is text/plain. […]"

### 2.3 Geo functions

**(a)** [OData-URL], **§5.1.1.11 Geo Functions** → §5.1.1.11.1 geo.distance, §5.1.1.11.2 geo.intersects, §5.1.1.11.3 geo.length

**(b) Verbatim, complete:**
> "**5.1.1.11 Geo Functions**
>
> **5.1.1.11.1 geo.distance**
>
> The geo.distance function has the following signatures:
> ```
> Edm.Double geo.distance(Edm.GeographyPoint,Edm.GeographyPoint)
> Edm.Double geo.distance(Edm.GeometryPoint,Edm.GeometryPoint)
> ```
> The geo.distance function returns the shortest distance between the two points in the coordinate reference system signified by the two points' SRIDs.
>
> **5.1.1.11.2 geo.intersects**
>
> The geo.intersects function has the following signatures:
> ```
> Edm.Boolean geo.intersects(Edm.GeographyPoint,Edm.GeographyPolygon)
> Edm.Boolean geo.intersects(Edm.GeometryPoint,Edm.GeometryPolygon)
> ```
> The geo.intersects function returns true if the specified point lies within the interior or on the boundary of the specified polygon, otherwise it returns false.
>
> **5.1.1.11.3 geo.length**
>
> The geo.length function has the following signatures:
> ```
> Edm.Double geo.length(Edm.GeographyLineString)
> Edm.Double geo.length(Edm.GeometryLineString)
> ```
> The geo.length function returns the total length of its line string parameter in the coordinate reference system signified by its SRID."

**(d) Strength:** **none** — the three Geo Function subsections contain no RFC 2119 keywords at all; they are pure signature + semantics definitions, and none carries its own example. Note the spec's ordering: distance, intersects, length (5.1.1.11.1/.2/.3).

### 2.4 ABNF geo literal grammar

**(a)** [OData-ABNF] — `primitiveLiteral` / `primitiveValue` alternatives and the geo rule block.

**(b) Verbatim (copied exactly, including the spec's own indentation and inline comments):**
```
primitiveLiteral = nullValue                  ; plain values up to int64Value
...
                 / string                     ; single-quoted
...
                 / geographyCollection 
                 / geographyLineString 
                 / geographyMultiLineString 
                 / geographyMultiPoint 
                 / geographyMultiPolygon 
                 / geographyPoint 
                 / geographyPolygon 
                 / geometryCollection 
                 / geometryLineString 
                 / geometryMultiLineString 
                 / geometryMultiPoint 
                 / geometryMultiPolygon 
                 / geometryPoint 
                 / geometryPolygon
```
```
; in Atom and JSON message bodies and CSDL DefaultValue attributes                 
primitiveValue = booleanValue
               / guidValue
               / durationValue
               / dateTimeOffsetValue 
               / dateValue
               / timeOfDayValue
               / enumValue
               / fullCollectionLiteral
               / fullLineStringLiteral
               / fullMultiPointLiteral
               / fullMultiLineStringLiteral
               / fullMultiPolygonLiteral
               / fullPointLiteral
               / fullPolygonLiteral
               / decimalValue 
               / doubleValue 
               / singleValue 
               / sbyteValue 
               / byteValue
               / int16Value 
               / int32Value
```
```
geographyCollection   = geographyPrefix SQUOTE fullCollectionLiteral SQUOTE
fullCollectionLiteral = sridLiteral collectionLiteral
collectionLiteral     = "Collection(" geoLiteral *( COMMA geoLiteral ) CLOSE
geoLiteral            = collectionLiteral
                      / lineStringLiteral
                      / multiPointLiteral
                      / multiLineStringLiteral
                      / multiPolygonLiteral
                      / pointLiteral
                      / polygonLiteral

geographyLineString   = geographyPrefix SQUOTE fullLineStringLiteral SQUOTE
fullLineStringLiteral = sridLiteral lineStringLiteral
lineStringLiteral     = "LineString" lineStringData
lineStringData        = OPEN positionLiteral 1*( COMMA positionLiteral ) CLOSE

geographyMultiLineString   = geographyPrefix SQUOTE fullMultiLineStringLiteral SQUOTE
fullMultiLineStringLiteral = sridLiteral multiLineStringLiteral
multiLineStringLiteral     = "MultiLineString(" [ lineStringData *( COMMA lineStringData ) ] CLOSE

geographyMultiPoint   = geographyPrefix SQUOTE fullMultiPointLiteral SQUOTE
fullMultiPointLiteral = sridLiteral multiPointLiteral
multiPointLiteral     = "MultiPoint(" [ pointData *( COMMA pointData ) ] CLOSE

geographyMultiPolygon   = geographyPrefix SQUOTE fullMultiPolygonLiteral SQUOTE
fullMultiPolygonLiteral = sridLiteral multiPolygonLiteral
multiPolygonLiteral     = "MultiPolygon(" [ polygonData *( COMMA polygonData ) ] CLOSE

geographyPoint   = geographyPrefix SQUOTE fullPointLiteral SQUOTE
fullPointLiteral = sridLiteral pointLiteral
sridLiteral      = "SRID" EQ 1*5DIGIT SEMI
pointLiteral     ="Point" pointData
pointData        = OPEN positionLiteral CLOSE
positionLiteral  = doubleValue SP doubleValue [ SP doubleValue ] [ SP doubleValue ] ; longitude, latitude, altitude/elevation (optional), linear referencing measure (optional)

geographyPolygon   = geographyPrefix SQUOTE fullPolygonLiteral SQUOTE
fullPolygonLiteral = sridLiteral polygonLiteral
polygonLiteral     = "Polygon" polygonData
polygonData        = OPEN ringLiteral *( COMMA ringLiteral ) CLOSE
ringLiteral        = OPEN positionLiteral *( COMMA positionLiteral ) CLOSE
                   ; Within each ringLiteral, the first and last positionLiteral elements MUST be an exact syntactic match to each other.
                   ; Within the polygonData, the ringLiterals MUST specify their points in appropriate winding order. 
                   ; In order of traversal, points to the left side of the ring are interpreted as being in the polygon.

geometryCollection      = geometryPrefix SQUOTE fullCollectionLiteral      SQUOTE
geometryLineString      = geometryPrefix SQUOTE fullLineStringLiteral      SQUOTE
geometryMultiLineString = geometryPrefix SQUOTE fullMultiLineStringLiteral SQUOTE
geometryMultiPoint      = geometryPrefix SQUOTE fullMultiPointLiteral      SQUOTE
geometryMultiPolygon    = geometryPrefix SQUOTE fullMultiPolygonLiteral    SQUOTE
geometryPoint           = geometryPrefix SQUOTE fullPointLiteral           SQUOTE
geometryPolygon         = geometryPrefix SQUOTE fullPolygonLiteral         SQUOTE

geographyPrefix = "geography"
geometryPrefix  = "geometry" 
```

**(d) Grammar notes (derived, NOT quoted):**
- `sridLiteral` is **not** bracketed in any `full*Literal` rule — the `SRID=nnn;` prefix is **mandatory** in URL geo literals per the ABNF, unlike the JSON `crs` object which is optional. `1*5DIGIT` caps the SRID at 5 digits.
- Only the `full*Literal` (SRID-prefixed, unquoted) forms are legal as `primitiveValue` in payloads / CSDL `DefaultValue`; the `geographyX`/`geometryX` (prefix + single-quoted) forms are the URL forms.
- `lineStringData` requires **≥2** positions; `ringLiteral` requires ≥1 plus the closure MUST in the comment.
- `multiPointLiteral` / `multiLineStringLiteral` / `multiPolygonLiteral` allow an **empty** body; `collectionLiteral` does **not** (requires ≥1 `geoLiteral`).
- `positionLiteral` order is **longitude, latitude** — matching GeoJSON, not "lat, long".
- Two **MUST**s live only in ABNF comments: ring closure (first == last position, *exact syntactic match*) and ring winding order.

### 2.5 Restrictions on comparing / ordering / keying geo values

**Not spec-silent — there is an explicit prohibition for comparison and for sorting.**

**(a)** [OData-URL], **§5.1.1.1 Logical Operators**

**(b) Verbatim (section context + the restriction):**
> "OData defines a set of logical operators that evaluate to true or false (i.e. a boolCommonExpr as defined in [OData-ABNF]). Logical operators are typically used to filter a collection of resources.
>
> The syntax rules for the logical operators are defined in [OData-ABNF].  4.01 Services MUST support case-insensitive operator names. Clients that want to work with 4.0 services MUST use lower case operator names.
>
> The six comparison operators can be used with all primitive values except Edm.Binary, Edm.Stream, and the Edm.Geo types. Edm.Binary, Edm.Stream, and the Edm.Geo types can only be compared to the null value using the eq and ne operators.
>
> When applied to operands of numeric types, numeric promotion rules are applied.
>
> The eq, ne, and in operators can be used with collection-valued operands, and the eq and ne operators can be used with structured operands."

**(d) Strength:** no RFC 2119 keyword ("can be used" / "can only be compared"), but unambiguous: the *only* permitted geo comparison in `$filter` is `GeoProp eq null` / `GeoProp ne null`; `lt`/`le`/`gt`/`ge` are excluded outright, as are `eq`/`ne` against a non-null geo operand.

**$orderby** — [OData-Protocol] §11.2.6.2:
> "Values of type Edm.Stream or any of the Geo types cannot be sorted."

**As a key** — [OData-CSDL] §4.1 Key (geo types excluded by omission from a closed allow-list):
> "Key properties MUST NOT be nullable and MUST be typed with an enumeration type, one of the following primitive types, or a type definition based on one of these primitive types:
> - Edm.Boolean
> - Edm.Byte
> - Edm.Date
> - Edm.DateTimeOffset
> - Edm.Decimal
> - Edm.Duration
> - Edm.Guid
> - Edm.Int16
> - Edm.Int32
> - Edm.Int64
> - Edm.SByte
> - Edm.String
> - Edm.TimeOfDay"

> "A key property MUST be a non-nullable primitive property of the entity type itself, including non-nullable primitive properties of non-nullable single-valued complex properties, recursively."

**(d)** MUST with a closed enumeration — `Edm.Geography*`, `Edm.Geometry*`, `Edm.Binary`, `Edm.Stream`, `Edm.Single`, `Edm.Double` are all absent, so geo types MUST NOT be key properties. There is **no separate prose sentence naming the Geo types as forbidden keys** — the prohibition is by exclusion only.

### 2.6 SRID type facet

**(a)** [OData-CSDLXML] / [OData-CSDLJSON], **§7.2.6 SRID**

**(b) Verbatim (CSDL XML):**
> "For a geometry or geography property the SRID facet identifies which spatial reference system is applied to values of the property on type instances.
>
> The value of the SRID facet MUST be a non-negative integer or the special value variable. If no value is specified, the attribute defaults to 0 for Geometry types or 4326 for Geography types.
>
> The valid values of the SRID facet and their meanings are as defined by the European Petroleum Survey Group [EPSG].
>
> **Attribute SRID**
>
> The value of $SRID is a number or the symbolic value variable."

**Verbatim (CSDL JSON — identical prose; differs in "facet defaults" and the representation line):**
> "For a geometry or geography property the SRID facet identifies which spatial reference system is applied to values of the property on type instances.
>
> The value of the SRID facet MUST be a non-negative integer or the special value variable. If no value is specified, the facet defaults to 0 for Geometry types or 4326 for Geography types.
>
> The valid values of the SRID facet and their meanings are as defined by the European Petroleum Survey Group [EPSG].
>
> **$SRID**
>
> The value of $SRID is a string containing a number or the symbolic value variable."

**(d) Strength / notes:**
- **MUST** — value is a non-negative integer or the literal `variable`.
- Defaults (not keyworded but normative): **0 for Geometry**, **4326 for Geography**.
- Representation differs between formats: CSDL-XML says "a number"; CSDL-JSON says "**a string containing** a number or the symbolic value variable".
- The CSDL-XML section header says "Attribute SRID" but its body refers to `$SRID` (JSON syntax) — quoted verbatim from the source; an upstream editorial defect, not a transcription slip.
- Where the facet may appear (MAY): `Property`, `TypeDefinition`, `ReturnType`, `Parameter`, `Term`, and annotation-expression casts:
  > "The edm:Property element MUST contain the Name and the Type attribute, and it MAY contain the facet attributes Nullable, MaxLength, Unicode, Precision, Scale, SRID, and DefaultValue."

  > "The type definition MAY specify facets applicable to the underlying type. Possible facets are: MaxLength, Unicode, Precision, Scale, or SRID."

  > "If the specified type is a primitive type or a collection of a primitive type, the facet attributes MaxLength, Unicode, Precision, Scale, and SRID MAY be specified if applicable to the specified primitive type. If the facet attributes are not specified, their values are considered unspecified."

### 2.7 RFC 7946 — the two clauses OData relies on

**(a)** [GeoJSON] **§3.1.1 Position**

**(b) Verbatim:**
> "   A position is the fundamental geometry construct.  The "coordinates"
>    member of a Geometry object is composed of either:
>
>    o  one position in the case of a Point geometry,
>
>    o  an array of positions in the case of a LineString or MultiPoint
>       geometry,
>
>    o  an array of LineString or linear ring (see Section 3.1.6)
>       coordinates in the case of a Polygon or MultiLineString geometry,
>       or
>
>    o  an array of Polygon coordinates in the case of a MultiPolygon
>       geometry.
>
>    A position is an array of numbers.  There MUST be two or more
>    elements.  The first two elements are longitude and latitude, or
>    easting and northing, precisely in that order and using decimal
>    numbers.  Altitude or elevation MAY be included as an optional third
>    element.
>
>    Implementations SHOULD NOT extend positions beyond three elements
>    because the semantics of extra elements are unspecified and
>    ambiguous."

**(d) Strength:** MUST ≥2 elements; order is normative (longitude first — matching OData's `positionLiteral` comment); MAY third element (altitude); SHOULD NOT extend beyond three. **Tension:** OData's ABNF `positionLiteral` explicitly allows a **fourth** element (linear referencing measure), which RFC 7946 SHOULD-NOTs — a 4-element URL literal serialized into GeoJSON contravenes a SHOULD NOT, not a MUST.

**(a)** [GeoJSON] **§4 Coordinate Reference System**

**(b) Verbatim:**
> "   The coordinate reference system for all GeoJSON coordinates is a
>    geographic coordinate reference system, using the World Geodetic
>    System 1984 (WGS 84) [WGS84] datum, with longitude and latitude units
>    of decimal degrees.  This is equivalent to the coordinate reference
>    system identified by the Open Geospatial Consortium (OGC) URN
>    urn:ogc:def:crs:OGC::CRS84.  An OPTIONAL third-position element SHALL
>    be the height in meters above or below the WGS 84 reference
>    ellipsoid.  In the absence of elevation values, applications
>    sensitive to height or depth SHOULD interpret positions as being at
>    local ground or sea level.
>
>    Note: the use of alternative coordinate reference systems was
>    specified in [GJ2008], but it has been removed from this version of
>    the specification because the use of different coordinate reference
>    systems -- especially in the manner specified in [GJ2008] -- has
>    proven to have interoperability issues."

**(d)** RFC 7946 **fixes** the CRS to WGS 84 / CRS84 (≈ EPSG:4326) and **removes** the `crs` member that [GeoJSON-2008] defined. [OData-JSON] §7.1 deliberately re-introduces it as optional, pinned to the 2008 `name`-type form — so an OData payload carrying a `crs` object is valid OData but a documented divergence from RFC 7946.

**GeometryCollection member naming** — only normative via RFC 7946 §3.1.8:
> "A GeometryCollection has a member with the name "geometries".  The value of "geometries" is an array.  Each element of this array is a GeoJSON Geometry object.  It is possible for this array to be empty."

[OData-JSON] itself does not restate this (see Gaps).

---

## 3. Entity-typed values in JSON payloads (OLINGO-1588, "ValueType.Entity")

Sources used: [OData-JSON] §4.5, §4.5.8, §6, §7, §8.5, §13, §14, §18; [OData-Protocol] §11.5.1, §11.5.4.1, §11.5.5.1; [OData-CSDLXML]/[OData-CSDLJSON] §7, §7.1, §8.1, §12.8, §12.9

### 3.1 An entity as a value

**(a)** [OData-JSON], **§6 Entity**

**(b) Verbatim:**
> "An entity is serialized as a JSON object. It MAY contain context, type, or deltaLink control information.
>
> Each property to be transmitted is represented as a name/value pair within the object. The order properties appear within the object is considered insignificant.
>
> An entity in a payload may be a complete entity, a projected entity (see System Query Option $select [OData-Protocol]), or a partial entity update (see Update an Entity in [OData-Protocol]).
>
> An entity representation can be (modified and) round-tripped to the service directly. The context URL is used in requests only as a base for relative URLs."

**(d)** entity → JSON object (unconditional); control-information members are MAY. (Examples 10 and 11 show `metadata=minimal` and `metadata=full` entity objects.)

### 3.2 A structural property value is never an entity

**(a)** [OData-JSON], **§7 Structural Property**

**(b) Verbatim:**
> "A property within an entity or complex type instance is represented as a name/value pair. The name MUST be the name of the property; the value is represented depending on its type as a primitive value, a complex value, a collection of primitive values, or a collection of complex values."

**(d)** The enumeration of representable structural-property value shapes contains no "entity value" — the JSON-format counterpart of the CSDL restriction in §3.6 below.

### 3.3 Collection of entities

**(a)** [OData-JSON], **§13 Collection of Entities**

**(b) Verbatim:**
> "A collection of entities is represented as a JSON object containing a name/value pair named value. It MAY contain context, type, count, nextLink, or deltaLink control information.
>
> If present, the context control information MUST be the first name/value pair in the response.
>
> […]
>
> The value of the value name/value pair is a JSON array where each element is representation of an entity or a representation of an entity reference. An empty collection is represented as an empty JSON array."

**(d)** The array elements are *either* entity objects *or* entity-reference objects.

### 3.4 Entity-typed action parameter values — the load-bearing clause

**(a)** [OData-JSON], **§18 Action Invocation**

**(b) Verbatim:**
> "Action parameter values are encoded in a single JSON object in the request body.
>
> Each non-binding parameter value is encoded as a separate name/value pair in this JSON object. The name is the name of the parameter. The value is the parameter value in the JSON representation appropriate for its type. Entity typed parameter values MAY include a subset of the properties, or just the entity reference, as appropriate to the action.
>
> Non-binding parameters that are nullable or annotated with the term Core.OptionalParameter defined in [OData-VocCore] MAY be omitted from the request body. If an omitted parameter is not annotated (and thus nullable), it MUST be interpreted as having the null value. If it is annotated and the annotation specifies a DefaultValue, the omitted parameter is interpreted as having that default value. If omitted and the annotation does not specify a default value, the service is free on how to interpret the omitted parameter. Note: a nullable non-binding parameter is equivalent to being annotated as optional with a default value of null."

**(c) Example 46** (flat name/value object with a nested object value for the structured parameter):
```json
{
  "param1": 42,
  "param2": {
    "Street": "One Microsoft Way",
    "Zip": 98052
  },
  "param3": [ 1, 42, 99 ],
  "param4": null
}
```

> "In order to invoke an action with no non-binding parameters, the client passes an empty JSON object in the body of the request. 4.01 Services MUST also support clients passing an empty request body for this case."

**(d) Reading for the implementation:** "the value is the parameter value in the JSON representation appropriate for its type" + §6 ⇒ an entity-typed action parameter value is a **JSON entity object**; the same sentence explicitly permits the *entity-reference* form ("or just the entity reference").

### 3.5 Passing an entity by reference

**(a)** [OData-JSON], **§14 Entity Reference** and **§4.5.8 Control Information: id (odata.id)**

**(b) Verbatim (§14):**
> "An entity reference (see [OData-Protocol]) MAY take the place of an entity in a JSON payload, based on the client request. It is serialized as a JSON object that MUST contain the id of the referenced entity and MAY contain the type control information and instance annotations, but no additional properties or control information.
>
> A collection of entity references is represented as a collection of entities, with entity reference representations instead of entity representations as items in the array value of the value name/value pair."

**(c) Examples 29 / 30:**
```json
{
  "@context": "http://host/service/$metadata#$ref",
  "@id": "Orders(10643)"
}
```
```json
{
  "@context": "http://host/service/$metadata#Collection($ref)",
  "value": [
    { "@id": "Orders(10643)" },
    { "@id": "Orders(10759)" }
  ]
}
```

**Verbatim (§4.5.8):**
> "The id control information contains the entity-id, see [OData-Protocol]. By convention the entity-id is identical to the canonical URL of the entity, as defined in [OData-URL]."

**4.0-vs-4.01 spelling of that member (§4.5 Control Information):**
> "In requests and responses with an OData-Version header with a value of 4.0 control information names are prefixed with @odata., e.g. @odata.context. In requests and responses without such a header the "odata." infix SHOULD be omitted, e.g @context.
>
> In some cases, control information is required in request payloads; this is called out in the following subsections.
>
> Receivers that encounter unknown annotations in any namespace or unknown control information MUST NOT stop processing and MUST NOT signal an error."

**(d)** `@odata.id` (4.0) / `@id` (4.01) is the by-reference member; the last sentence forbids erroring on unknown control information.

Related (the same `@id`-based mechanism, for navigation binds), §8.5 Bind Operation:
> "For requests containing an OData-Version header with a value of 4.01, a relationship is bound to an existing entity using the same representation as for an expanded entity reference."

### 3.6 Protocol: binding parameters and entity-typed parameter values

**(a)** [OData-Protocol], **§11.5.1 Binding an Operation to a Resource**

**(b) Verbatim:**
> "Actions and Functions MAY be bound to any type or collection, similar to defining a method in a class in object-oriented programming. The first parameter of a bound operation is the binding parameter.
>
> The namespace- or alias-qualified name of a bound operation may be appended to any URL that identifies a resource whose type matches, or is derived from, the type of the binding parameter. The resource identified by that URL is used as the binding parameter value. Only aliases defined in the metadata document of the service can be used in URLs."

> "A bound operation with a single-valued binding parameter can be applied to each member of a collection by appending the path segment /$each to the resource path of the collection, followed by a forward slash and the namespace- or alias-qualified name of the bound operation. In this case the type of the collection members MUST match or be derived from the type of the binding parameter."

**(a)** **§11.5.5.1 Invoking an Action**

**(b) Verbatim:**
> "To invoke an action bound to a resource, the client issues a POST request to an action URL. An action URL may be obtained from a previously returned entity representation or constructed by appending the namespace- or alias-qualified action name to a URL that identifies a resource whose type is the same as, or derives from, the type of the binding parameter of the action. The value for the binding parameter is the resource identified by the URL preceding the action name, and only the non-binding parameter values are passed in the request body according to the particular format."

> "To invoke an action through an action import, the client issues a POST request to a URL identifying the action import. The canonical URL for an action import is the service root, followed by the name of the action import. When invoking an action through an action import all parameter values MUST be passed in the request body according to the particular format."

> "4.01 services MUST support invoking actions with no non-binding parameters and parameterless action imports both without a request body and with a request body representing no parameters, according to the particular format. Interoperable clients SHOULD always include a request body, even when invoking actions with no non-binding parameters and parameterless action imports."

> "To request processing of the action only if the binding parameter value, an entity or collection of entities, is unmodified, the client includes the If-Match header with the latest known ETag value for the entity or collection of entities. The ETag value for a collection as a whole is transported in the ETag header of a collection response."

**(d)** The last sentence is the strongest verbatim protocol evidence that a parameter value can be an entity: "the binding parameter value, an entity or collection of entities".

**§11.5.4.1 Invoking a Function** (function binding parameter, for symmetry):
> "[…] The value for the binding parameter is the value of the resource identified by the URL prior to appending the function name, and additional parameter values are specified using inline parameter syntax. […]"

Note: for URL-encoded (inline) function parameters, §11.5.4.1.1 defines only `Name=Value` literal/alias syntax — entity-typed values are a *body* (action) construct, not an inline-URL one.

### 3.7 CSDL: structural properties CANNOT be entity-typed

**(a)** [OData-CSDL] **§7 Structural Property**, **§7.1 Type**, **§8.1 Navigation Property Type**, **§12.8 Return Type**, **§12.9 Parameter**

**(b) Verbatim (§7, CSDL XML):**
> "A structural property is a property (of a structural type) that has one of the following types:
> ·         Primitive type
> ·         Complex type
> ·         Enumeration type
> ·         A collection of one of the above"

**Verbatim (§7.1, identical in both CSDL representations) — the decisive sentence:**
> "The property's type MUST be a primitive type, complex type, or enumeration type in scope, or a collection of one of these types."

**Verbatim (§8.1, identical in both) — navigation properties are the entity-typed ones:**
> "The navigation property's type MUST be an entity type in scope, the abstract type Edm.EntityType, or a collection of one of these types.
>
> If the type is a collection, an arbitrary number of entities can be related. Otherwise there is at most one related entity.
>
> The related entities MUST be of the specified entity type or one of its subtypes."

**Verbatim (§12.9 Parameter) — entity types ARE allowed:**
> "An action or function overload MAY specify parameters.
>
> A bound action or function overload MUST specify at least one parameter; the first parameter is the binding parameter. The order of parameters MUST NOT change unless the schema version changes.
>
> Each parameter MUST have a name that is a simple identifier. The parameter name MUST be unique within the action or function overload.
>
> The parameter MUST specify a type. It MAY be any type in scope, or a collection of any type in scope."

(CSDL-JSON wording of the second paragraph differs by one word: "…the first parameter is its binding parameter.")

**Verbatim (§12.8 Return Type) — entity types ARE allowed:**
> "The return type of an action or function overload MAY be any type in scope, or a collection of any type in scope."

> "If the return type is a collection of entity types, the $Nullable member has no meaning and MUST NOT be specified."

**(d) Net normative picture for OLINGO-1588:** `Property` → primitive/complex/enum/collection-thereof (MUST); `NavigationProperty` → entity type (MUST); `Parameter`/`ReturnType` → *any type in scope* (MAY). So entity-typed action parameters are legal CSDL, and their JSON wire form is an entity object (§6) or an entity-reference `@id` object (§14, §18). **There is no single prohibitive sentence** saying a structural property MUST NOT be an entity type — the prohibition is a closed-list argument.

---

## 4. Stream property instance annotations (OLINGO-1505)

Sources used: [OData-JSON] §3.1, §3.1.1, §3.1.2, §3.1.3, §4.5, §4.5.12, §9, §10; [OData-Protocol] §11.4.8, §11.4.8.1

### 4.1 The media* control information

**(a)** [OData-JSON], **§4.5.12 Control Information: media* (odata.media*)** — a single combined section covering all four members; there are **no** per-member subsections.

**(b) Verbatim, in full:**
> "For media entities and stream properties at least one of the control information mediaEditLink and mediaReadLink MUST be included in responses if they don't follow standard URL conventions as defined in [OData-URL] or if metadata=full is requested.
>
> The mediaEditLink control information contains a URL that can be used to update the binary stream associated with the media entity or stream property. It MUST be included for updatable streams if it differs from standard URL conventions relative to the edit link of the entity.
>
> The mediaReadLink control information contains a URL that can be used to read the binary stream associated with the media entity or stream property. It MUST be included if its value differs from the value of the associated mediaEditLink, if present, or if it doesn't follow standard URL conventions relative to the read link of the entity and the associated mediaEditLink is not present.
>
> The mediaContentType control information MAY be included; its value SHOULD match the media type of the binary stream represented by the mediaReadLink URL. This is only a hint; the actual media type will be included in the Content-Type header when the resource is requested.
>
> The mediaEtag control information MAY be included; its value is the ETag of the binary stream represented by this media entity or stream property.
>
> The media* control information is not written in responses if metadata=none is requested.
>
> If a stream property is provided inline in a request, the mediaContentType control information may be specified.
>
> If a stream property is annotated with Capabilities.MediaLocationUpdateSupported (see [OData-VocCap]) and a value of true, clients MAY specify the mediaEditLink and/or mediaReadLink control information for that stream property in order to change the association between the stream property and a media stream.
>
> In all other cases media* control information is ignored in request payloads."

**(c) Example 7** (media entity form):
```json
{
  "@context": "http://host/service/$metadata#Employees/$entity",
  "@mediaReadLink": "Employees(1)/$value",
  "@mediaContentType": "image/jpeg",
  "ID": 1,
  ...
}
```

**(d) Normative summary per member:**

| member | request payloads | responses |
|---|---|---|
| `mediaEditLink` | ignored, except the `MediaLocationUpdateSupported` case (MAY) | MUST for updatable streams when it differs from standard URL conventions |
| `mediaReadLink` | ignored, except the `MediaLocationUpdateSupported` case (MAY) | MUST when it differs from the associated `mediaEditLink`, or doesn't follow standard URL conventions and no `mediaEditLink` is present |
| `mediaContentType` | MAY be specified when a stream property is provided inline (and MUST be present if inline stream data is included — §9) | MAY be included; value SHOULD match the stream's media type ("only a hint") |
| `mediaEtag` | ignored | MAY be included |
| all four | — | not written if `metadata=none` |

### 4.2 Stream property representation

**(a)** [OData-JSON], **§9 Stream Property**

**(b) Verbatim:**
> "An entity or complex type instance can have one or more stream properties.
>
> The actual stream data is not usually contained in the representation. Instead stream property data is generally read and edited via URLs.
>
> Depending on the metadata level, the stream property MAY be annotated to provide the read link, edit link, media type, and ETag of the media stream through a set of media* control information.
>
> If the actual stream data is included inline, the control information mediaContentType MUST be present to indicate how the included stream property value is represented. Stream property values of media type application/json or one of its subtypes, optionally with format parameters, are represented as native JSON. Values of top-level type text, for example text/plain, are represented as a string, with JSON string escaping rules applied. Included stream data of other media types is represented as a base64url-encoded string value, see [RFC4648], section 5.
>
> If the included stream property has no value, the non-existing stream data is represented as null and the control information mediaContentType is not necessary."

**(c) Example 21** — the canonical `Name@media*` prefixed-annotation form OLINGO-1505 targets:
```json
{
  "@context": "http://host/service/$metadata#Products/$entity",
  ...
  "Thumbnail@mediaReadLink": "http://server/Thumbnail546.jpg",
  "Thumbnail@mediaEditLink": "http://server/uploads/Thumbnail546.jpg",
  "Thumbnail@mediaContentType": "image/jpeg",
  "Thumbnail@mediaEtag": "W/\"####\"",
  "Thumbnail": "...base64url encoded value...",
  ...
}
```

### 4.3 Media entity (contrast case — unprefixed `@media*`)

**(a)** [OData-JSON], **§10 Media Entity**

**(b) Verbatim:**
> "Media entities are entities that describe a media resource, for example a photo. They are represented as entities that contain additional media* control information.
>
> If the actual stream data for the media entity is included, it is represented as property named $value whose string value is the base64url-encoded value of the media stream, see [RFC4648], section 5."

**(c) Example 22:**
```json
{
  "@context": "http://host/service/$metadata#Employees/$entity",
  "@mediaReadLink": "Employees(1)/$value",
  "@mediaContentType": "image/jpeg",
  "$value": "...base64url encoded value...",
  "ID": 1,
  ...
}
```

### 4.4 metadata=minimal / =full / =none omission rules

**(a)** [OData-JSON], **§3.1.1 metadata=minimal (odata.metadata=minimal)**

**(b) Verbatim (media* portion):**
> "The metadata=minimal format parameter indicates that the service SHOULD remove computable control information from the payload wherever possible. The response payload MUST contain at least the following control information:
>
> ·         context: the root context URL of the payload and the context URL for any deleted entries or added or deleted links in a delta response, or for entities or entity collections whose set cannot be determined from the root context URL
>
> ·         etag: the ETag of the entity or collection, as appropriate
>
> ·         count: the total count of a collection of entities or collection of entity references, if requested
>
> ·         nextLink: the next link of a collection with partial results
>
> ·         deltaLink: the delta link for obtaining changes to the result, if requested
>
> In addition, control information MUST appear in the payload for cases where actual values are not the same as the computed values and MAY appear otherwise. When control information appears in the payload, it is treated as exceptions to the computed values.
>
> Media entities and stream properties MAY in addition contain the following control information:
>
> ·         mediaEtag: the ETag of the stream, as appropriate
>
> ·         mediaContentType: the media type of the stream"

**(d)** Note the asymmetry: at `metadata=minimal` the enumerated *optional* media members are only `mediaEtag` and `mediaContentType`; the links are governed by the "MUST appear … where actual values are not the same as the computed values" rule plus §4.5.12's explicit MUSTs.

**(a)** **§3.1.2 metadata=full (odata.metadata=full)**

**(b) Verbatim (relevant portion):**
> "The metadata=full format parameter indicates that the service MUST include all control information explicitly in the payload."

> "Media entities and stream properties may in addition contain the following control information:
>
> ·         mediaReadLink: the link used to read the stream
>
> ·         mediaEditLink: the link used to edit/update the stream
>
> ·         mediaEtag: the ETag of the stream, as appropriate
>
> ·         mediaContentType: the media type of the stream"

**(d)** Combined with §4.5.12's first sentence ("…MUST be included in responses if they don't follow standard URL conventions as defined in [OData-URL] **or if metadata=full is requested**"), `metadata=full` makes at least one of `mediaEditLink`/`mediaReadLink` mandatory for every media entity and stream property.

**(a)** **§3.1.3 metadata=none (odata.metadata=none)**

**(b) Verbatim:**
> "The metadata=none format parameter indicates that the service SHOULD omit control information other than nextLink and count. This control information MUST continue to be included, as applicable, even in the metadata=none case.
>
> It is not valid to specify metadata=none on a delta request."

Reinforced by §4.5.12: "The media* control information is not written in responses if metadata=none is requested."

### 4.5 4.01 vs 4.0 prefix (`@mediaReadLink` vs `@odata.mediaReadLink`)

**(b) Verbatim (§4.5 Control Information — the general rule that covers media* by class):**
> "In requests and responses with an OData-Version header with a value of 4.0 control information names are prefixed with @odata., e.g. @odata.context. In requests and responses without such a header the "odata." infix SHOULD be omitted, e.g @context."

**Verbatim (§3.1, the parallel statement for the `metadata` format parameter itself):**
> "Note that in OData 4.0 the metadata format parameter was prefixed with "odata.". Payloads with an OData-Version header equal to 4.0 MUST include the "odata." prefix. Payloads with an OData-Version header equal to 4.01 or greater SHOULD NOT include the "odata." prefix."

**(d)** 4.0 payload ⇒ `Thumbnail@odata.mediaReadLink` (MUST); 4.01+ payload ⇒ `Thumbnail@mediaReadLink` (SHOULD omit the `odata.` infix). All spec examples (7, 21, 22) use the unprefixed 4.01 form.

### 4.6 Protocol context for stream properties

**(a)** [OData-Protocol], **§11.4.8 Managing Stream Properties**, **§11.4.8.1 Update Stream Values**

**(b) Verbatim:**
> "An entity may have one or more stream properties. Stream properties are properties of type Edm.Stream.
>
> The values for stream properties do not usually appear in the entity payload. Instead, the values are generally read or written through URLs."

> "Clients MAY change the association between a stream property and a media stream by modifying the edit URL or read URL of the stream property. Services supporting this SHOULD advertise it by annotating the stream property with the term Capabilities.MediaLocationUpdateSupported defined in [OData-VocCap]."

Also relevant to serializing inline stream data (§11.2.4.2 $expand):
> "The value of the $expand query option is a comma-separated list of navigation property names, stream property names, or $value indicating the stream content of a media-entity."

And on sorting (§11.2.6.2):
> "Values of type Edm.Stream or any of the Geo types cannot be sorted."

---

## 5. Streaming / asynchronous requests (OLINGO-1066 internal, OLINGO-1235 async Prefer)

Sources used: [OData-Protocol] §8.2.8.2, §8.2.8.8, §8.2.8.10, §8.3.1, §8.3.3, §8.3.6, §8.3.7, §9.1.3, §9.5, §11.6, §11.7.7.6, §13.1.1/§13.1.3/§13.2.1/§13.3; [OData-JSON] §4.4, §19.3, §24

### 5.1 Preference `respond-async`

**(a)** Part 1, §8.2.8 Preference → **§8.2.8.8 Preference respond-async**

**(b) Verbatim, in full:**
> "The respond-async preference, as defined in [RFC7240], allows clients to request that the service process the request asynchronously.
>
> If the client has specified respond-async in the request, the service MAY process the request asynchronously and return a 202 Accepted response.
>
> The respond-async preference MAY be used for batch requests, in which case it applies to the batch request as a whole and not to individual requests within the batch request.
>
> In the case that the service applies the respond-async preference it MUST include a Preference-Applied response header containing the respond-async preference.
>
> A service MAY specify the support for the respond-async preference using an annotation with term Capabilities.AsynchronousRequestsSupported, see [OData-VocCap]."

**(c) Example (Example 9):**
> "Example 9: a service receiving the following header might choose to respond
> ·         asynchronously if the synchronous processing of the request will take longer than 10 seconds
> ·         synchronously after 5 seconds
> ·         asynchronously (ignoring the wait preference)
> ·         synchronously after 15 seconds (ignoring respond-async preference and the wait preference)"
```
Prefer: respond-async, wait=10
```

**(d) Strength:** the only MUST is the `Preference-Applied: respond-async` echo. Processing asynchronously at all is a MAY — a service may always answer synchronously.

### 5.2 Preference `wait`

**(a)** Part 1, **§8.2.8.10 Preference wait** — NOTE: it is §8.2.8.**10**, not §8.2.8.9 (§8.2.8.9 is `track-changes`).

**(b) Verbatim, in full:**
> "The wait preference, as defined in [RFC7240], is used to establish an upper bound on the length of time, in seconds, the client is prepared to wait for the service to process the request synchronously once it has been received.
>
> If the respond-async preference is also specified, the client requests that the service respond asynchronously after the specified length of time.
>
> If the respond-async preference has not been specified, the service MAY interpret the wait as a request to timeout after the specified period of time.
>
> If the wait preference is specified on an individual request within a batch, then it specifies the maximum amount of time to wait for that individual request. If the wait preference is specified on a batch, then it specifies the maximum time to wait for the entire batch."

**(d) Strength:** entirely MAY-level for the service; `wait` without `respond-async` is a timeout hint.

### 5.3 Preference `callback` (async-relevant sentences)

**(a)** Part 1, **§8.2.8.2 Preference callback**

**(b) Verbatim (relevant sentences):**
> "The callback preference can be specified:
> - when requesting asynchronous processing of a request with the respond-async preference, or
> - on a GET request to a delta link."

> "The callback preference MUST include the parameter url whose value is the URL of a callback endpoint to be invoked by the OData service when data is available. The syntax of the callback preference is defined in [OData-ABNF]."

> "If the service applies the callback preference it MUST include the callback preference in the Preference-Applied response header."

> "When the callback preference is applied to asynchronous requests, the OData service invokes the callback endpoint once it has finished processing the request. The status monitor resource, returned in the Location header of the previously returned 202 Accepted response, can then be used to retrieve the results of the asynchronously executed request."

### 5.4 Asynchronous Requests / status monitor resource

**(a)** Part 1, **§11.6 Asynchronous Requests**

**(b) Verbatim, whole section:**
> "A Prefer header with a respond-async preference allows clients to request that the service process a Data Service Request asynchronously.
>
> If the client has specified respond-async in the request, the service MAY process the request asynchronously and return a 202 Accepted response. A service MUST NOT reply to a Data Service Request with 202 Accepted if the request has not included the respond-async preference.
>
> Responses that return 202 Accepted MUST include a Location header pointing to a status monitor resource that represents the current state of the asynchronous processing in addition to an optional Retry-After header indicating the time, in seconds, the client should wait before querying the service for status. Services MAY include a response body, for example, to provide additional status information.
>
> A GET request to the status monitor resource again returns 202 Accepted response if the asynchronous processing has not finished. This response MUST again include a Location header and MAY include a Retry-After header to be used for a subsequent request. The Location header and optional Retry-After header may or may not contain the same values as returned by the previous request.
>
> A GET request to the status monitor resource returns 200 OK once the asynchronous processing has completed. For OData 4.01 and greater responses, or OData 4.0 requests that include an Accept header that does not specify application/http, the response MUST include the AsyncResult response header. Any other headers, along with the response body, represent the result of the completed asynchronous operation. If the GET request to the status monitor includes an OData-MaxVersion header with a value of 4.0 and no Accept header, or an Accept header that includes application/http, then the body of the final 200 OK response MUST be represented as an HTTP message, as described in [RFC7230], which is the full HTTP response to the completed asynchronous operation.
>
> A DELETE request sent to the status monitor resource requests that the asynchronous processing be canceled. A 200 OK or a 204 No Content response indicates that the asynchronous processing has been successfully canceled. A client can request that the DELETE should be executed asynchronously. A 202 Accepted response indicates that the cancellation is being processed asynchronously; the client can use the returned Location header (which MUST be different from the status monitor resource of the initial request) to query for the status of the cancellation. If a delete request is not supported by the service, the service returns 405 Method Not Allowed.
>
> After a successful DELETE request against the status monitor resource, any subsequent GET requests for the same status monitor resource returns 404 Not Found.
>
> If an asynchronous request is cancelled for reasons other than the consumers issuing a DELETE request against the status monitor resource, a GET request to the status monitor resource returns 200 OK with a response body containing a single HTTP response with a status code in the 5xx Server Error range indicating that the operation was cancelled.
>
> The service MUST ensure that no observable change has occurred as a result of a canceled request.
>
> If the client waits too long to request the result of the asynchronous processing, the service responds with a 410 Gone or 404 Not Found.
>
> The status monitor resource URL MUST differ from any other resource URL."

**(c) Examples:** **§11.6 contains NO example blocks** (example numbering runs 95 in §11.5 → 96 in §11.7.1). The only async example request/response blocks in Part 1 are Example 105 in §11.7.7.6, reproduced in §5.5 below.

**(d) MUST/SHOULD/MAY summary:**
- MUST NOT return 202 without `respond-async` in the request.
- 202 responses MUST carry `Location` (status monitor); `Retry-After` optional (MAY).
- Repeat 202 from the monitor MUST again include `Location`; `Retry-After` MAY.
- Final 200 OK MUST include `AsyncResult` for 4.01+ (or 4.0 where Accept does not specify `application/http`).
- Final 200 OK MUST be an `application/http` HTTP message when `OData-MaxVersion: 4.0` with no Accept, or Accept includes `application/http`.
- DELETE cancel → 200 OK or 204 No Content; async cancel → 202 with a *different* `Location`; unsupported → 405 Method Not Allowed.
- After cancel, subsequent GET → 404 Not Found. Waited too long → 410 Gone or 404 Not Found.
- Service MUST guarantee no observable change from a canceled request; monitor URL MUST be unique.

### 5.5 Asynchronous batch requests (the spec's only async examples)

**(a)** Part 1, **§11.7.7.6 Asynchronous Batch Requests**

**(b) Verbatim:**
> "Batch requests MAY be executed asynchronously by including the respond-async preference in the Prefer header. If the service responds with a multipart batch response, it MUST ignore the respond-async preference for individual requests within a batch.
>
> After successful execution of the batch request the response to the batch request is returned in the body of a response to an interrogation request against the status monitor resource URL (see Asynchronous Requests).
>
> A service MAY return interim results to an asynchronously executing batch. It does this by responding with 200 OK to a GET request to the monitor resource and including a 202 Accepted response as the last part of the multipart response. The client can use the monitor URL returned in this 202 Accepted response to continue processing the batch response.
>
> Since a change set is executed atomically, 202 Accepted MUST NOT be returned within a change set."

Related constraint, §11.7.7.5:
> "·         Asynchronously processed batch requests can return interim results and end with a 202 Accepted as the last part of the multipart response. Therefore, the respond-async preference MUST NOT be applied to individual requests within a batch if the batch response is a multipart response."

**(c) Example 105, verbatim** — initial 202:
```
HTTP/1.1 202 Accepted
Location: http://service-root/async-monitor-0
Retry-After: ###
```
> "When interrogating the monitor URL only the first request in the batch has finished processing and all the remaining requests are still being processed. Note that the actual multipart batch response itself is contained in an application/http wrapper as it is a response to a status monitor resource:"
```
HTTP/1.1 200 Ok

Content-Type: application/http
HTTP/1.1 200 Ok
OData-Version: 4.0
Content-Length: ####
Content-Type: multipart/mixed; boundary=b_243234_25424_ef_892u748

--b_243234_25424_ef_892u748
Content-Type: application/http
HTTP/1.1 200 Ok
Content-Type: application/json
Content-Length: ###
<JSON representation of the Customer entity with key ALFKI>

--b_243234_25424_ef_892u748
Content-Type: application/http
HTTP/1.1 202 Accepted
Location: http://service-root/async-monitor
Retry-After: ###

--b_243234_25424_ef_892u748--
```
> "After some time the client makes a second request using the returned monitor URL, not explicitly accepting application/http. The batch is completely processed and the response is the final result."
```
HTTP/1.1 200 Ok
AsyncResult: 200
OData-Version: 4.0
...
```
Note the two contrasting shapes: the first monitor response is **wrapped** (`Content-Type: application/http` then a nested full HTTP response, no `AsyncResult`); the second is **unwrapped** with `AsyncResult: 200` because the client did not accept `application/http`.

### 5.6 `Content-Transfer-Encoding: binary` — where it actually belongs

Its only normative mention is in the multipart *batch* body-part rules (§11.7.7.1), and it is explicitly de-emphasized:
> "A body part representing an individual request MUST include a Content-Type header with value application/http."

> "A Content-Transfer-Encoding header with value binary may be included for historic reasons although this header is not used by HTTP and only needed for transmission via E-Mail. Neither clients nor services should rely on this header being present."

**SPEC SILENT** on `Content-Transfer-Encoding` in §11.6 / status-monitor responses — it is NOT part of the async wrapper.

### 5.7 Header definitions used by async

**§8.3.1 Header AsyncResult** (full):
> "A 4.01 service MUST include the AsyncResult header in 200 OK response from a status monitor resource in order to indicate the final HTTP Response Status Code of an asynchronously executed request.
>
> The AsyncResult header SHOULD NOT be applied to individual responses within a batch."

**§8.3.3 Header Location** (async-relevant sentence):
> "The Location header MUST be returned in the response from a Create Entity or Create Media Entity request to specify the edit URL, or for read-only entities the read URL, of the created entity, and in responses returning 202 Accepted to specify the URL that the client can use to request the status of an asynchronous request."

**§8.3.6 Header Preference-Applied:**
> "In a response to a request that specifies a Prefer header, a service MAY include a Preference-Applied header, as defined in [RFC7240], specifying how individual preferences within the request were handled.
>
> The value of the Preference-Applied header is a comma-separated list of preferences applied in the response. For more information on the individual preferences, see the Prefer header."
(The async-specific MUST lives in §8.2.8.8, quoted above.)

**§8.3.7 Header Retry-After** (full):
> "A service MAY include a Retry-After header, as defined in [RFC7231], in 202 Accepted and in 3xx Redirect responses
>
> The Retry-After header specifies the duration of time, in seconds, that the client is asked to wait before retrying the request or issuing a request to the resource returned as the value of the Location header."

**§9.1.3 Response Code 202 Accepted:**
> "202 Accepted indicates that the Data Service Request has been accepted and has not yet completed executing asynchronously. The asynchronous handling of requests is defined in the sections on Asynchronous Requests and Asynchronous Batch Requests.."

### 5.8 Conformance lines for async

**§13.1.1 OData 4.0 Minimal Conformance Level:**
> "Additionally, if async operations are supported:
> 16.  MUST return an HTTP message as the final response to an asynchronous request with an OData-MaxVersion value of 4.0 and an Accept header including application/http.
> 17.  MAY return the AsyncResult header in the final response to an asynchronous request"

**§13.1.3 OData 4.0 Advanced Conformance Level:**
> "13.  SHOULD support asynchronous requests (section 11.6)"

**§13.2.1 OData 4.01 Minimal Conformance Level:**
> "3.     MUST return the AsyncResult result header in the final response to an asynchronous request if asynchronous operations are supported."

**§13.2.2 (4.01 Intermediate) / §13.2.3 (4.01 Advanced):** SPEC SILENT — neither lists an async item; they inherit via "MUST conform to the OData 4.0 Intermediate/Advanced Conformance Level".

**§13.3 Interoperable OData Clients:**
> "13.  MAY support asynchronous responses (section 11.6)"

> "15.  MAY support streaming in a JSON response (see [OData-JSON])"

### 5.9 Streaming / chunked delivery

Part 1 has **no** section on streaming responses or chunked delivery as such. The genuinely normative hits are about in-stream errors, not about a streaming serializer:

**§9.5 In-Stream Errors** (full):
> "In the case that the service encounters an error after sending a success status to the client, the service MUST leave the response malformed according to its content-type. Clients MUST treat the entire response as being in error.
>
> Services MAY include the header OData-Error as a trailing header if supported by the transport protocol (e.g. HTTP/1.1 with chunked transfer encoding, or HTTP/2)."

**§8.3.5 Header OData-Error:**
> "A response with an in-stream error MAY include an OData-Error trailing header if the transport protocol supports trailing headers (e.g. HTTP/1.1 with chunked transfer encoding, or HTTP/2)."

**§11.7.1 Batch Request Headers** (the only mention of streaming results):
> "If the set of request headers of a batch request are valid the service MUST return a 200 OK HTTP response code to indicate that the batch request was accepted for processing, but the processing is yet to be completed. The individual requests within the body of the batch request may subsequently fail or be malformed; however, this enables batch implementations to stream the results."

**SPEC SILENT** in Part 1 on chunked/`Transfer-Encoding` requirements for ordinary responses. The actual streaming contract lives in **[OData-JSON] §4.4 Payload Ordering Constraints**, which is what a streaming serializer must honor:
> "Ordering constraints MAY be imposed on the JSON payload in order to support streaming scenarios. These ordering constraints MUST only be assumed if explicitly specified as some clients (and services) might not be able to control, or might not care about, the order of the JSON properties in the payload.
>
> Clients can request that a JSON response conform to these ordering constraints by specifying a media type of application/json with the streaming=true parameter in the Accept header or $format query option. Services MUST return 406 Not Acceptable if the client only requests streaming and the service does not support it.
>
> Clients may specify the streaming=true parameter in the Content-Type header of requests to indicate that the request body follows the payload ordering constraints. In the absence of this parameter, the service must assume that the JSON properties in the request are unordered.
>
> Processors MUST only assume streaming support if it is explicitly indicated in the Content-Type header via the streaming=true parameter."

> "To support streaming scenarios the following payload ordering constraints have to be met:
> ·         If present, the context control information MUST be the first property in the JSON object.
> ·         The type control information, if present, MUST appear next in the JSON object.
> ·         The id and etag control information MUST appear before any property, property annotation, or property control information.
> ·         All annotations or control information for a structural or navigation property MUST appear as a group immediately before the property itself. The one exception is the nextLink of a collection which MAY appear after the collection it annotates.
> ·         All other control information can appear anywhere in the payload as long as it does not violate any of the above rules.
> ·         For 4.0 payloads, annotations and control information for navigation properties MUST appear after all structural properties. 4.01 clients MUST NOT assume this ordering."

> "Note that, in OData 4.0, the streaming format parameter was prefixed with \"odata.\".  Payloads with an OData-Version header equal to 4.0 MUST include the \"odata.\" prefix. Payloads with an OData-Version header equal to 4.01 or greater SHOULD NOT include the \"odata.\" prefix."

Relevant to a streaming serializer emitting `@count`:
> "The count name/value pair represents the number of entities in the collection. If present and the streaming=true content-type parameter is set, it MUST come before the value name/value pair. If the response represents a partial result, the count name/value pair MUST appear in the first partial response, and it MAY appear in subsequent partial responses (in which case it may vary from response to response)."

[OData-JSON] §24 Conformance:
> "6.     MUST NOT require streaming=true in the Content-Type header (section 4.4)"

> "20.  MAY support the odata.streaming=true parameter in the Accept header (section 4.4)"

### 5.10 JSON batch async (unwrapped variant)

**(a)** [OData-JSON] **§19.3** (asynchronous JSON batch)

**(b) Verbatim:**
> "A batch request that specifies the respond-async preference MAY be executed asynchronously. This means that the \"outer\" batch request is executed asynchronously; this preference does not automatically cascade down to the individual requests within the batch. After successful execution of the batch request the response to the batch request is returned in the body of a response to an interrogation request against the status monitor resource URL, see section \"Asynchronous Requests\" in [OData-Protocol].
>
> A service MAY return interim results to an asynchronously executing batch. It does this by responding with 200 OK to a GET request to the monitor resource and including a nextLink control information in the JSON batch response, thus signaling that the response is only a partial result. A subsequent GET request to the next link MAY result in a 202 Accepted response with a location header pointing to a new status monitor resource."

> "In addition to the above interaction pattern individual requests within a batch with no other requests depending on it and not part of an atomicity group MAY be executed asynchronously if they specify the respond-async preference and if the service responds with a JSON batch response. In this case the response array contains a response object for each asynchronously executed individual request with a status of 202, a location header pointing to an individual status monitor resource, and optionally a retry-after header."

**(c) Example 51, verbatim** (initial 202, then unwrapped partial result):
```
HTTP/1.1 202 Accepted
Location: http://service-root/async-monitor-0
Retry-After: ###
```
```
HTTP/1.1 200 Ok
AsyncResult: 200
OData-Version: 4.01
Content-Length: ####
Content-Type: application/json

{
  "responses": [
    {
      "id": "0",
      "status": 200,
      "body": <JSON representation of the Customer entity with key ALFKI>
  ],
  "@nextLink": "...?$skiptoken=YmF0Y2gx"
}
```

---

## Gaps

Everything below could **not** be found verbatim in the primary sources. Recording it here so the Tier-6 design can document the deviation rather than silently invent a rule.

### Feature 1 — CSDL JSON metadata document
- **`$Binary`, `$Date`, `$DateTimeOffset`, `$Decimal`, `$Duration`, `$EnumMember`, `$Float`, `$Guid`, `$Int`, `$String`, `$TimeOfDay` as CSDL JSON member names — DO NOT EXIST.** A full-text grep of the CSDL JSON spec returns zero hits for all of them; they are CSDL **XML** element names (`edm:Binary`, …). CSDL JSON §14.3 uses plain JSON strings/booleans/numbers only. An implementation emitting `{"$Binary": …}` in CSDL JSON would be non-conformant.
- **`$Type` on a `$Record` expression — not found.** §14.4.12 specifies only the `@type` control information; there is no `$Type` member for record expressions and no `$Record` wrapper member at all (a record is a bare JSON object).
- **`$Collection` as a named member for the §14.4.6 collection expression — not found.** Collection expressions are bare JSON arrays; `$Collection` exists only as the "this is a collection type" flag on properties, navigation properties, return types, parameters, `$Cast`, and `$IsOf`.
- **No statement about the `OData-Version` header value for a CSDL JSON `$metadata` response.** Part 1 §8.1.5 gives only the general rule; neither spec states a metadata-document-specific rule. The `$Version` document member (`"4.0"`/`"4.01"`) is the only metadata-specific version marker.
- **No single normative sentence joining `$format=json` on `$metadata` to CSDL JSON specifically** (as opposed to the OData JSON payload format). CSDL JSON §2.1 and Protocol §11.1.2 each cover half.
- Spec typos preserved verbatim in this digest and worth flagging to implementers: "The value of $EntityContainer is value is…" (§4), "CDSL JSON" (§7.2.2 note), "Booelan" (§13.2), missing space in "…means false.In OData 4.0 responses…" (§13.3), a missing comma in Example 23's `$ReferentialConstraint`, "an qualified type name" (§14.4.8, §14.4.10).

### Feature 2 — Geo types
- **GeometryCollection member naming is NOT stated in [OData-JSON].** A full-text grep of the JSON Format spec for `GeometryCollection`, `geometries`, `crs`, `SRID` returns exactly **one** hit — the CRS bullet in §7.1. The `"geometries"` member name is normative only via the incorporated reference to RFC 7946 §3.1.8.
- **No `crs` object example payload anywhere in [OData-JSON].** The CRS bullet is prose-only; Example 12's single `"GeographyPoint"` line is the only geo payload in the document, and it carries no `crs`.
- **No examples in [OData-URL] §5.1.1.11.** None of geo.distance / geo.intersects / geo.length carries its own example; the only geo URL literal in Part 2 is inside Example 102.
- **No `geo.intersects` overload beyond Point × Polygon**, and no `geo.within` / `geo.relate` / LineString-intersection text. Anything beyond the three functions is out of spec.
- **No prose sentence forbidding Geo types as key properties** — the prohibition exists only as exclusion from the closed CSDL §4.1 allow-list (inferred by omission).
- **`variable` SRID has no defined wire behaviour.** CSDL §7.2.6 permits `SRID="variable"` but neither [OData-JSON] nor [OData-URL] says how a variable-SRID value is serialized, or what SRID a literal without `crs` then carries. SPEC SILENT.
- **OData ABNF permits a 4th position element (linear referencing measure); RFC 7946 SHOULD-NOTs positions beyond three.** No spec text reconciles the two — SPEC SILENT on serializing an M-coordinate into GeoJSON.
- **`sridLiteral` mandatory-vs-optional is grammar-only.** No prose in Part 1 or Part 2 states that the `SRID=nnn;` prefix is required in URL literals; it is inferable only from the unbracketed `sridLiteral` in each `full*Literal` ABNF rule.
- CSDL-XML §7.2.6's "Attribute SRID" body text says `$SRID` (JSON member syntax) rather than `SRID` — quoted verbatim above; an upstream editorial defect.

### Feature 3 — Entity-typed values in JSON payloads
- **No "Advanced parameter" clause exists** in the JSON Format spec; the only action-parameter clause is §18 Action Invocation.
- **No explicit sentence saying an entity-typed action parameter value "is represented as an entity object."** The conclusion follows from §18 + §6, not from one sentence.
- **No `@odata.id`-specific action-parameter sentence.** The by-reference mechanism is stated generically in §14 and §4.5.8 and referenced from §18 only as "just the entity reference"; there is no clause spelling out `"param": {"@id": "..."}` for a parameter.
- **[OData-Protocol] never uses the phrase "entity typed parameter."** It delegates non-binding parameter representation to "the particular format". The strongest protocol evidence is §11.5.5.1's If-Match sentence ("the binding parameter value, an entity or collection of entities").
- **No single prohibitive clause forbidding entity-typed structural properties** — see §3.7; the proof is a closed-list argument (CSDL §7/§7.1 positive list vs §8.1 navigation-property rule), not a "MUST NOT be an entity type" sentence.

### Feature 4 — Stream property instance annotations
- **No per-member subsections for the media control information.** [OData-JSON] has only the single combined §4.5.12 "Control Information: media* (odata.media*)"; there are no four distinct section numbers to cite.
- **No 4.01-vs-4.0 prefix rule stated specifically for the `media*` names** — only the §4.5 general control-information rule, which covers them by class.
- **No verbatim rule on whether `mediaContentType` may be omitted at `metadata=full`.** §3.1.2 lists it under "may in addition contain", in tension with the same section's "MUST include all control information explicitly"; the spec does not resolve this for media members.

### Feature 5 — Streaming / async
- **No examples in Part 1 §11.6.** The section is prose-only (example numbering jumps 95 → 96). The only async request/response example blocks in Part 1 are Example 105 in §11.7.7.6 (async *batch*).
- **`Content-Transfer-Encoding: binary` is NOT part of the async wrapper.** Its only normative mention (§11.7.7.1) is about multipart batch body parts and says not to rely on it. SPEC SILENT for §11.6.
- **`Retry-After` value format is unspecified beyond "duration of time, in seconds"** — the spec's examples literally print `Retry-After: ###`, so no concrete verbatim value exists.
- **No normative Part 1 text on chunked delivery / `Transfer-Encoding` for ordinary responses.** Only in-stream-error trailing headers (§9.5, §8.3.5) and the batch note (§11.7.1) touch streaming/chunking; the streaming-serializer contract lives in [OData-JSON] §4.4.
- **No async items in OData 4.01 Intermediate (§13.2.2) or Advanced (§13.2.3) conformance lists** — async conformance appears only as 4.0 Minimal items 16/17, 4.0 Advanced item 13, and 4.01 Minimal item 3.
- **No `AsyncResult` value grammar** beyond "the final HTTP Response Status Code"; examples show `AsyncResult: 200` only.
- **Section-number correction:** the `wait` preference is **§8.2.8.10**, not §8.2.8.9 (§8.2.8.9 is `track-changes`).
