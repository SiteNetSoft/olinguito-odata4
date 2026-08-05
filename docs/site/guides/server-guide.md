# Server Development Guide

## Introduction

This guide covers building OData V4 services with Olinguito. You'll learn how to:

* Define your Entity Data Model (EDM)
* Implement processors for CRUD operations
* Handle navigation properties
* Support query options ($filter, $orderby, $select, $expand)
* Implement actions and functions
* Handle batch requests

## Entity Data Model (EDM)

The EDM defines the structure of your OData service - entity types, properties, relationships, and operations.

### Creating an EdmProvider

Extend `CsdlAbstractEdmProvider` to define your model:

```java
public class MyEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "MyService";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER =
        new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity type names
    public static final String ET_CUSTOMER = "Customer";
    public static final String ET_ORDER = "Order";
    public static final FullQualifiedName ET_CUSTOMER_FQN =
        new FullQualifiedName(NAMESPACE, ET_CUSTOMER);
    public static final FullQualifiedName ET_ORDER_FQN =
        new FullQualifiedName(NAMESPACE, ET_ORDER);

    // Entity set names
    public static final String ES_CUSTOMERS = "Customers";
    public static final String ES_ORDERS = "Orders";
}
```

### Defining Entity Types

```java
@Override
public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
    if (entityTypeName.equals(ET_CUSTOMER_FQN)) {
        // Properties
        CsdlProperty id = new CsdlProperty()
            .setName("ID")
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);

        CsdlProperty name = new CsdlProperty()
            .setName("Name")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setMaxLength(100);

        CsdlProperty email = new CsdlProperty()
            .setName("Email")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        CsdlProperty createdAt = new CsdlProperty()
            .setName("CreatedAt")
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());

        // Key
        CsdlPropertyRef keyRef = new CsdlPropertyRef().setName("ID");

        // Navigation property to Orders
        CsdlNavigationProperty navOrders = new CsdlNavigationProperty()
            .setName("Orders")
            .setType(ET_ORDER_FQN)
            .setCollection(true)
            .setPartner("Customer");

        return new CsdlEntityType()
            .setName(ET_CUSTOMER)
            .setProperties(Arrays.asList(id, name, email, createdAt))
            .setKey(Collections.singletonList(keyRef))
            .setNavigationProperties(Collections.singletonList(navOrders));
    }
    return null;
}
```

### Defining Complex Types

```java
@Override
public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) {
    if (complexTypeName.equals(CT_ADDRESS_FQN)) {
        CsdlProperty street = new CsdlProperty()
            .setName("Street")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        CsdlProperty city = new CsdlProperty()
            .setName("City")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        CsdlProperty postalCode = new CsdlProperty()
            .setName("PostalCode")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        return new CsdlComplexType()
            .setName("Address")
            .setProperties(Arrays.asList(street, city, postalCode));
    }
    return null;
}
```

### Defining Enum Types

```java
@Override
public CsdlEnumType getEnumType(FullQualifiedName enumTypeName) {
    if (enumTypeName.equals(ENUM_STATUS_FQN)) {
        return new CsdlEnumType()
            .setName("OrderStatus")
            .setMembers(Arrays.asList(
                new CsdlEnumMember().setName("Pending").setValue("0"),
                new CsdlEnumMember().setName("Processing").setValue("1"),
                new CsdlEnumMember().setName("Shipped").setValue("2"),
                new CsdlEnumMember().setName("Delivered").setValue("3"),
                new CsdlEnumMember().setName("Cancelled").setValue("4")
            ))
            .setUnderlyingType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
    }
    return null;
}
```

### Entity Sets and Container

```java
@Override
public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer,
                                   String entitySetName) {
    if (entityContainer.equals(CONTAINER)) {
        if (entitySetName.equals(ES_CUSTOMERS)) {
            CsdlEntitySet entitySet = new CsdlEntitySet()
                .setName(ES_CUSTOMERS)
                .setType(ET_CUSTOMER_FQN);

            // Navigation property binding
            entitySet.setNavigationPropertyBindings(Collections.singletonList(
                new CsdlNavigationPropertyBinding()
                    .setPath("Orders")
                    .setTarget(ES_ORDERS)
            ));

            return entitySet;
        }
    }
    return null;
}

@Override
public CsdlEntityContainer getEntityContainer() {
    List<CsdlEntitySet> entitySets = Arrays.asList(
        getEntitySet(CONTAINER, ES_CUSTOMERS),
        getEntitySet(CONTAINER, ES_ORDERS)
    );

    return new CsdlEntityContainer()
        .setName(CONTAINER_NAME)
        .setEntitySets(entitySets);
}
```

## Implementing Processors

Processors handle OData requests. Implement the appropriate interface for each operation type.

### EntityCollectionProcessor

Handles GET requests on entity collections:

```java
public class CustomerCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private DataProvider dataProvider;

    public CustomerCollectionProcessor(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(ODataRequest request,
                                     ODataResponse response,
                                     UriInfo uriInfo,
                                     ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // 1. Get the entity set
        EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
        EdmEntityType entityType = edmEntitySet.getEntityType();

        // 2. Fetch data
        EntityCollection entityCollection = dataProvider.readAll(edmEntitySet);

        // 3. Apply system query options
        entityCollection = applyQueryOptions(uriInfo, entityCollection);

        // 4. Serialize
        ContextURL contextUrl = ContextURL.with()
            .entitySet(edmEntitySet)
            .build();

        EntityCollectionSerializerOptions options =
            EntityCollectionSerializerOptions.with()
                .contextURL(contextUrl)
                .select(uriInfo.getSelectOption())
                .expand(uriInfo.getExpandOption())
                .count(uriInfo.getCountOption())
                .build();

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult result = serializer.entityCollection(
            serviceMetadata, entityType, entityCollection, options);

        // 5. Set response
        response.setContent(result.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE,
            responseFormat.toContentTypeString());
    }

    private EdmEntitySet getEdmEntitySet(UriInfo uriInfo) {
        List<UriResource> resources = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResource = (UriResourceEntitySet) resources.get(0);
        return uriResource.getEntitySet();
    }
}
```

### EntityProcessor

Handles single entity operations (GET, PUT, PATCH, DELETE):

```java
public class CustomerEntityProcessor implements EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private DataProvider dataProvider;

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntity(ODataRequest request, ODataResponse response,
                          UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // Get entity set and key
        UriResourceEntitySet uriResource =
            (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
        EdmEntitySet edmEntitySet = uriResource.getEntitySet();
        List<UriParameter> keyPredicates = uriResource.getKeyPredicates();

        // Fetch entity
        Entity entity = dataProvider.read(edmEntitySet, keyPredicates);

        if (entity == null) {
            throw new ODataApplicationException("Entity not found",
                HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }

        // Serialize
        ContextURL contextUrl = ContextURL.with()
            .entitySet(edmEntitySet)
            .suffix(ContextURL.Suffix.ENTITY)
            .build();

        EntitySerializerOptions options = EntitySerializerOptions.with()
            .contextURL(contextUrl)
            .select(uriInfo.getSelectOption())
            .expand(uriInfo.getExpandOption())
            .build();

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult result = serializer.entity(
            serviceMetadata, edmEntitySet.getEntityType(), entity, options);

        response.setContent(result.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE,
            responseFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response,
                            UriInfo uriInfo, ContentType requestFormat,
                            ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
        EdmEntityType entityType = edmEntitySet.getEntityType();

        // Deserialize request body
        ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(
            request.getBody(), entityType);
        Entity requestEntity = result.getEntity();

        // Create entity
        Entity createdEntity = dataProvider.create(edmEntitySet, requestEntity);

        // Serialize response
        ContextURL contextUrl = ContextURL.with()
            .entitySet(edmEntitySet)
            .build();

        EntitySerializerOptions options = EntitySerializerOptions.with()
            .contextURL(contextUrl)
            .build();

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serResult = serializer.entity(
            serviceMetadata, entityType, createdEntity, options);

        response.setContent(serResult.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE,
            responseFormat.toContentTypeString());
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response,
                            UriInfo uriInfo, ContentType requestFormat,
                            ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
        EdmEntityType entityType = edmEntitySet.getEntityType();

        UriResourceEntitySet uriResource =
            (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
        List<UriParameter> keyPredicates = uriResource.getKeyPredicates();

        // Deserialize
        ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(
            request.getBody(), entityType);
        Entity requestEntity = result.getEntity();

        // Determine if PUT or PATCH
        HttpMethod method = request.getMethod();
        boolean isPatch = method == HttpMethod.PATCH;

        // Update
        dataProvider.update(edmEntitySet, keyPredicates, requestEntity, isPatch);

        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response,
                            UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {

        EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
        UriResourceEntitySet uriResource =
            (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
        List<UriParameter> keyPredicates = uriResource.getKeyPredicates();

        dataProvider.delete(edmEntitySet, keyPredicates);

        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
}
```

## Query Options

### $filter

Parse and apply filter expressions:

```java
public class FilterExpressionVisitor implements ExpressionVisitor<Object> {

    private Entity currentEntity;

    public FilterExpressionVisitor(Entity entity) {
        this.currentEntity = entity;
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind operator,
                                       Object left, Object right) {
        if (operator == BinaryOperatorKind.EQ) {
            return left.equals(right);
        } else if (operator == BinaryOperatorKind.NE) {
            return !left.equals(right);
        } else if (operator == BinaryOperatorKind.GT) {
            return ((Comparable) left).compareTo(right) > 0;
        } else if (operator == BinaryOperatorKind.GE) {
            return ((Comparable) left).compareTo(right) >= 0;
        } else if (operator == BinaryOperatorKind.LT) {
            return ((Comparable) left).compareTo(right) < 0;
        } else if (operator == BinaryOperatorKind.LE) {
            return ((Comparable) left).compareTo(right) <= 0;
        } else if (operator == BinaryOperatorKind.AND) {
            return (Boolean) left && (Boolean) right;
        } else if (operator == BinaryOperatorKind.OR) {
            return (Boolean) left || (Boolean) right;
        }
        throw new ODataApplicationException("Unsupported operator: " + operator,
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    @Override
    public Object visitMember(Member member) {
        List<UriResource> resources = member.getResourcePath().getUriResourceParts();
        UriResourcePrimitiveProperty property =
            (UriResourcePrimitiveProperty) resources.get(0);
        return currentEntity.getProperty(property.getProperty().getName()).getValue();
    }

    @Override
    public Object visitLiteral(Literal literal) {
        String text = literal.getText();
        if (literal.getType() instanceof EdmString) {
            return text.substring(1, text.length() - 1); // Remove quotes
        }
        return text;
    }

    // ... implement other visit methods
}

// Usage in processor:
private EntityCollection applyFilter(FilterOption filterOption,
                                      EntityCollection collection) {
    if (filterOption == null) {
        return collection;
    }

    EntityCollection filtered = new EntityCollection();
    for (Entity entity : collection.getEntities()) {
        FilterExpressionVisitor visitor = new FilterExpressionVisitor(entity);
        Object result = filterOption.getExpression().accept(visitor);
        if (Boolean.TRUE.equals(result)) {
            filtered.getEntities().add(entity);
        }
    }
    return filtered;
}
```

### $orderby

```java
private EntityCollection applyOrderBy(OrderByOption orderByOption,
                                       EntityCollection collection) {
    if (orderByOption == null) {
        return collection;
    }

    List<Entity> entities = new ArrayList<>(collection.getEntities());

    for (OrderByItem item : orderByOption.getOrders()) {
        Expression expression = item.getExpression();
        boolean isDescending = item.isDescending();

        if (expression instanceof Member) {
            Member member = (Member) expression;
            String propertyName = ((UriResourcePrimitiveProperty)
                member.getResourcePath().getUriResourceParts().get(0))
                .getProperty().getName();

            entities.sort((e1, e2) -> {
                Object v1 = e1.getProperty(propertyName).getValue();
                Object v2 = e2.getProperty(propertyName).getValue();
                int result = ((Comparable) v1).compareTo(v2);
                return isDescending ? -result : result;
            });
        }
    }

    EntityCollection sorted = new EntityCollection();
    sorted.getEntities().addAll(entities);
    return sorted;
}
```

### $select and $expand

These are handled by the serializer options:

```java
EntityCollectionSerializerOptions options =
    EntityCollectionSerializerOptions.with()
        .contextURL(contextUrl)
        .select(uriInfo.getSelectOption())
        .expand(uriInfo.getExpandOption())
        .build();
```

For $expand, you need to populate the navigation properties:

```java
private void applyExpand(ExpandOption expandOption, Entity entity,
                         EdmEntitySet edmEntitySet) {
    if (expandOption == null) {
        return;
    }

    for (ExpandItem item : expandOption.getExpandItems()) {
        UriResource resource = item.getResourcePath().getUriResourceParts().get(0);
        if (resource instanceof UriResourceNavigation) {
            String navPropName = ((UriResourceNavigation) resource)
                .getProperty().getName();

            // Fetch related entities
            EntityCollection relatedEntities =
                dataProvider.getRelatedEntities(entity, navPropName);

            // Set as link
            Link link = new Link();
            link.setTitle(navPropName);
            link.setInlineEntitySet(relatedEntities);
            entity.getNavigationLinks().add(link);
        }
    }
}
```

### Server-Driven Paging

Use `PagingHelper` for automatic paging:

```java
// In your processor
int pageSize = 20; // Your page size

String skipToken = null;
if (uriInfo.getSkipTokenOption() != null) {
    skipToken = uriInfo.getSkipTokenOption().getValue();
}

// Apply paging - modifies collection in place and sets nextLink
PagingHelper.applyPaging(entityCollection, pageSize,
    request.getRawRequestUri(), skipToken);
```

## Actions and Functions

### Defining Actions

```java
@Override
public List<CsdlAction> getActions(FullQualifiedName actionName) {
    if (actionName.equals(ACTION_RESET_FQN)) {
        // Unbound action
        CsdlAction action = new CsdlAction()
            .setName("Reset")
            .setReturnType(new CsdlReturnType()
                .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName()));

        return Collections.singletonList(action);
    }

    if (actionName.equals(ACTION_DISCOUNT_FQN)) {
        // Bound action
        CsdlParameter bindingParam = new CsdlParameter()
            .setName("product")
            .setType(ET_PRODUCT_FQN)
            .setNullable(false);

        CsdlParameter percentParam = new CsdlParameter()
            .setName("percent")
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());

        CsdlAction action = new CsdlAction()
            .setName("ApplyDiscount")
            .setBound(true)
            .setParameters(Arrays.asList(bindingParam, percentParam))
            .setReturnType(new CsdlReturnType()
                .setType(ET_PRODUCT_FQN));

        return Collections.singletonList(action);
    }

    return null;
}
```

### Implementing ActionProcessor

```java
public class MyActionProcessor implements ActionEntityProcessor {

    @Override
    public void processActionEntity(ODataRequest request,
                                    ODataResponse response,
                                    UriInfo uriInfo,
                                    ContentType requestFormat,
                                    ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // Get action info
        List<UriResource> resources = uriInfo.getUriResourceParts();
        UriResourceAction actionResource =
            (UriResourceAction) resources.get(resources.size() - 1);
        EdmAction action = actionResource.getAction();

        // Parse parameters
        Map<String, Parameter> parameters = null;
        if (requestFormat != null) {
            ODataDeserializer deserializer =
                odata.createDeserializer(requestFormat);
            parameters = deserializer.actionParameters(
                request.getBody(), action).getActionParameters();
        }

        // Execute action
        Entity result = executeAction(action.getName(), parameters);

        // Serialize response
        if (result != null) {
            EdmEntityType entityType = (EdmEntityType) action.getReturnType().getType();
            ODataSerializer serializer = odata.createSerializer(responseFormat);
            SerializerResult serResult = serializer.entity(
                serviceMetadata, entityType, result,
                EntitySerializerOptions.with().build());

            response.setContent(serResult.getContent());
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE,
                responseFormat.toContentTypeString());
        } else {
            response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
        }
    }
}
```

### Defining Functions

```java
@Override
public List<CsdlFunction> getFunctions(FullQualifiedName functionName) {
    if (functionName.equals(FUNC_GET_COUNT_FQN)) {
        CsdlParameter param = new CsdlParameter()
            .setName("category")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        CsdlFunction function = new CsdlFunction()
            .setName("GetProductCount")
            .setParameters(Collections.singletonList(param))
            .setReturnType(new CsdlReturnType()
                .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName()));

        return Collections.singletonList(function);
    }
    return null;
}
```

## Batch Processing

Implement `BatchProcessor` for $batch requests:

```java
public class MyBatchProcessor implements BatchProcessor {

    @Override
    public void processBatch(BatchFacade facade, ODataRequest request,
                            ODataResponse response)
            throws ODataApplicationException, ODataLibraryException {

        // Parse batch request
        String boundary = facade.extractBoundaryFromContentType(
            request.getHeader(HttpHeader.CONTENT_TYPE));
        BatchOptions options = BatchOptions.with().rawBaseUri(
            request.getRawBaseUri()).build();
        List<BatchRequestPart> requestParts = odata.createFixedFormatDeserializer()
            .parseBatchRequest(request.getBody(), boundary, options);

        // Process each part
        List<ODataResponsePart> responseParts = new ArrayList<>();
        for (BatchRequestPart part : requestParts) {
            ODataResponsePart responsePart = facade.handleBatchRequest(part);
            responseParts.add(responsePart);
        }

        // Serialize response
        String responseBoundary = "batch_" + UUID.randomUUID();
        InputStream responseContent = odata.createFixedFormatSerializer()
            .batchResponse(responseParts, responseBoundary);

        response.setHeader(HttpHeader.CONTENT_TYPE,
            ContentType.MULTIPART_MIXED + ";boundary=" + responseBoundary);
        response.setContent(responseContent);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    }
}
```

## Error Handling

Throw `ODataApplicationException` for business errors:

```java
if (entity == null) {
    throw new ODataApplicationException(
        "Entity not found",
        HttpStatusCode.NOT_FOUND.getStatusCode(),
        Locale.ROOT,
        "ENTITY_NOT_FOUND"  // Optional error code
    );
}

if (!isAuthorized(request)) {
    throw new ODataApplicationException(
        "Access denied",
        HttpStatusCode.FORBIDDEN.getStatusCode(),
        Locale.ROOT
    );
}
```

## ETag Support

For optimistic concurrency:

```java
// In your entity
entity.setETag("W/\"" + version + "\"");

// In update processor
String ifMatch = request.getHeader(HttpHeader.IF_MATCH);
if (ifMatch != null) {
    String currentETag = existingEntity.getETag();
    if (!ifMatch.equals(currentETag) && !ifMatch.equals("*")) {
        throw new ODataApplicationException(
            "Precondition failed",
            HttpStatusCode.PRECONDITION_FAILED.getStatusCode(),
            Locale.ROOT
        );
    }
}
```

## See Also

* [Getting Started Guide](../getting-started.md)
* [Architecture Overview](architecture.md)
* [Quarkus Integration Guide](quarkus-guide.md)
* [Servlet Integration Guide](servlet-guide.md)
