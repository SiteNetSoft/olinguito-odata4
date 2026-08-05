# Client Development Guide

## Introduction

This guide covers consuming OData V4 services using the Olinguito client library. You'll learn how to:

* Connect to OData services
* Read entities and collections
* Create, update, and delete entities
* Use query options
* Navigate relationships
* Handle batch requests
* Work with actions and functions

## Getting Started

### Maven Dependency

```xml
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-client-core</artifactId>
    <version>${olinguito.version}</version>
</dependency>
```

### Creating a Client

```java
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;

// Create client instance
ODataClient client = ODataClientFactory.getClient();

// Service root URL
String serviceRoot = "https://services.odata.org/V4/TripPinService";
```

## Reading Data

### Read Service Document

```java
ODataServiceDocumentRequest request =
    client.getRetrieveRequestFactory().getServiceDocumentRequest(serviceRoot);
ODataRetrieveResponse<ClientServiceDocument> response = request.execute();
ClientServiceDocument serviceDocument = response.getBody();

// List entity sets
for (String entitySet : serviceDocument.getEntitySetNames()) {
    System.out.println("Entity Set: " + entitySet);
}
```

### Read Metadata

```java
EdmMetadataRequest metadataRequest =
    client.getRetrieveRequestFactory().getMetadataRequest(serviceRoot);
ODataRetrieveResponse<Edm> metadataResponse = metadataRequest.execute();
Edm edm = metadataResponse.getBody();

// Explore entity types
for (EdmSchema schema : edm.getSchemas()) {
    for (EdmEntityType entityType : schema.getEntityTypes()) {
        System.out.println("Entity Type: " + entityType.getName());
        for (String propName : entityType.getPropertyNames()) {
            EdmProperty prop = entityType.getStructuralProperty(propName);
            System.out.println("  " + propName + ": " + prop.getType().getName());
        }
    }
}
```

### Read Entity Collection

```java
URI customersUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .build();

ODataEntitySetRequest<ClientEntitySet> request =
    client.getRetrieveRequestFactory().getEntitySetRequest(customersUri);

// Optional: set format
request.setFormat(ContentType.APPLICATION_JSON);

ODataRetrieveResponse<ClientEntitySet> response = request.execute();
ClientEntitySet entitySet = response.getBody();

System.out.println("Total count: " + entitySet.getCount());

for (ClientEntity entity : entitySet.getEntities()) {
    String id = entity.getProperty("ID").getPrimitiveValue().toString();
    String name = entity.getProperty("Name").getPrimitiveValue().toString();
    System.out.println(id + ": " + name);
}
```

### Read Single Entity

```java
// By key
URI customerUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .appendKeySegment("ALFKI")
    .build();

ODataEntityRequest<ClientEntity> request =
    client.getRetrieveRequestFactory().getEntityRequest(customerUri);
ODataRetrieveResponse<ClientEntity> response = request.execute();
ClientEntity customer = response.getBody();

// Access properties
String name = customer.getProperty("Name").getPrimitiveValue().toString();
System.out.println("Customer: " + name);

// Access complex property
ClientComplexValue address = customer.getProperty("Address").getComplexValue();
String city = address.get("City").getPrimitiveValue().toString();
System.out.println("City: " + city);
```

### Read with Composite Key

```java
Map<String, Object> keys = new LinkedHashMap<>();
keys.put("OrderID", 10248);
keys.put("ProductID", 11);

URI orderDetailUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Order_Details")
    .appendKeySegment(keys)
    .build();

ODataEntityRequest<ClientEntity> request =
    client.getRetrieveRequestFactory().getEntityRequest(orderDetailUri);
ClientEntity orderDetail = request.execute().getBody();
```

## Query Options

### $filter

```java
// Simple filter
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .filter("Price gt 100")
    .build();

// Complex filter
URI uri2 = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .filter("Price gt 100 and contains(Name, 'Widget')")
    .build();

// Filter with functions
URI uri3 = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .filter("startswith(Name, 'A') and year(CreatedAt) eq 2024")
    .build();
```

### $orderby

```java
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .orderBy("Price desc, Name asc")
    .build();
```

### $select

```java
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .select("ID", "Name", "Email")
    .build();
```

### $expand

```java
// Simple expand
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .expand("Orders")
    .build();

// Nested expand
URI uri2 = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .expand("Orders($expand=OrderDetails)")
    .build();

// Expand with filter
URI uri3 = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .expand("Orders($filter=TotalAmount gt 1000)")
    .build();
```

### $top and $skip (Paging)

```java
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .top(10)
    .skip(20)
    .build();
```

### $count

```java
// Include count in response
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .count(true)
    .build();

// Get count only
URI countUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .count()
    .build();

ODataValueRequest countRequest =
    client.getRetrieveRequestFactory().getValueRequest(countUri);
ODataRetrieveResponse<ClientPrimitiveValue> countResponse = countRequest.execute();
int count = countResponse.getBody().toCastValue(Integer.class);
```

### $search

```java
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .search("laptop OR tablet")
    .build();
```

### Combined Query Options

```java
URI uri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .filter("Category eq 'Electronics'")
    .select("ID", "Name", "Price")
    .orderBy("Price desc")
    .top(10)
    .skip(0)
    .count(true)
    .expand("Supplier")
    .build();
```

## Creating Entities

### Simple Create

```java
// Build entity
ClientEntity newCustomer = client.getObjectFactory()
    .newEntity(new FullQualifiedName("MyService", "Customer"));

newCustomer.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("Name",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildString("Acme Corporation")));

newCustomer.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("Email",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildString("contact@acme.com")));

// Send request
URI customersUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .build();

ODataEntityCreateRequest<ClientEntity> createRequest =
    client.getCUDRequestFactory().getEntityCreateRequest(customersUri, newCustomer);
createRequest.setFormat(ContentType.APPLICATION_JSON);

ODataEntityCreateResponse<ClientEntity> createResponse = createRequest.execute();

// Get created entity with server-generated ID
ClientEntity created = createResponse.getBody();
String newId = created.getProperty("ID").getPrimitiveValue().toString();
System.out.println("Created customer with ID: " + newId);
```

### Create with Complex Property

```java
ClientEntity newCustomer = client.getObjectFactory()
    .newEntity(new FullQualifiedName("MyService", "Customer"));

// Add primitive properties
newCustomer.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("Name",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildString("Acme Corporation")));

// Build complex property
ClientComplexValue address = client.getObjectFactory().newComplexValue("Address");
address.add(client.getObjectFactory().newPrimitiveProperty("Street",
    client.getObjectFactory().newPrimitiveValueBuilder()
        .buildString("123 Main St")));
address.add(client.getObjectFactory().newPrimitiveProperty("City",
    client.getObjectFactory().newPrimitiveValueBuilder()
        .buildString("New York")));
address.add(client.getObjectFactory().newPrimitiveProperty("PostalCode",
    client.getObjectFactory().newPrimitiveValueBuilder()
        .buildString("10001")));

newCustomer.getProperties().add(
    client.getObjectFactory().newComplexProperty("Address", address));
```

### Deep Insert (Create with Related Entities)

```java
ClientEntity newOrder = client.getObjectFactory()
    .newEntity(new FullQualifiedName("MyService", "Order"));

newOrder.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("OrderDate",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildString("2024-01-15")));

// Create inline order details
ClientEntitySet orderDetails = client.getObjectFactory().newEntitySet();

ClientEntity detail1 = client.getObjectFactory()
    .newEntity(new FullQualifiedName("MyService", "OrderDetail"));
detail1.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("ProductID",
        client.getObjectFactory().newPrimitiveValueBuilder().buildInt32(1)));
detail1.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("Quantity",
        client.getObjectFactory().newPrimitiveValueBuilder().buildInt32(5)));
orderDetails.getEntities().add(detail1);

// Add as navigation link
ClientLink detailsLink = client.getObjectFactory()
    .newDeepInsertEntitySet("OrderDetails", orderDetails);
newOrder.getNavigationLinks().add(detailsLink);
```

## Updating Entities

### Full Update (PUT)

```java
// Read existing entity
URI customerUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .appendKeySegment("ALFKI")
    .build();

ODataEntityRequest<ClientEntity> readRequest =
    client.getRetrieveRequestFactory().getEntityRequest(customerUri);
ClientEntity customer = readRequest.execute().getBody();

// Modify property
customer.getProperties().removeIf(p -> p.getName().equals("Name"));
customer.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("Name",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildString("Updated Name")));

// Send PUT request
ODataEntityUpdateRequest<ClientEntity> updateRequest =
    client.getCUDRequestFactory().getEntityUpdateRequest(
        customerUri, UpdateType.REPLACE, customer);
updateRequest.execute();
```

### Partial Update (PATCH)

```java
// Build entity with only changed properties
ClientEntity patch = client.getObjectFactory()
    .newEntity(new FullQualifiedName("MyService", "Customer"));

patch.getProperties().add(
    client.getObjectFactory().newPrimitiveProperty("Email",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildString("newemail@example.com")));

// Send PATCH request
URI customerUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .appendKeySegment("ALFKI")
    .build();

ODataEntityUpdateRequest<ClientEntity> patchRequest =
    client.getCUDRequestFactory().getEntityUpdateRequest(
        customerUri, UpdateType.PATCH, patch);
patchRequest.execute();
```

### Update with ETag (Optimistic Concurrency)

```java
// Read entity to get ETag
ODataEntityRequest<ClientEntity> readRequest =
    client.getRetrieveRequestFactory().getEntityRequest(customerUri);
ODataRetrieveResponse<ClientEntity> readResponse = readRequest.execute();
String etag = readResponse.getETag();

// Update with If-Match header
ODataEntityUpdateRequest<ClientEntity> updateRequest =
    client.getCUDRequestFactory().getEntityUpdateRequest(
        customerUri, UpdateType.PATCH, patch);
updateRequest.setIfMatch(etag);

try {
    updateRequest.execute();
} catch (ODataClientErrorException e) {
    if (e.getStatusLine().getStatusCode() == 412) {
        System.out.println("Entity was modified by another user");
    }
}
```

## Deleting Entities

```java
URI customerUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .appendKeySegment("ALFKI")
    .build();

ODataDeleteRequest deleteRequest =
    client.getCUDRequestFactory().getDeleteRequest(customerUri);
ODataDeleteResponse deleteResponse = deleteRequest.execute();

if (deleteResponse.getStatusCode() == 204) {
    System.out.println("Entity deleted successfully");
}
```

## Navigation Properties

### Read Related Entities

```java
// Get orders for a customer
URI ordersUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .appendKeySegment("ALFKI")
    .appendNavigationSegment("Orders")
    .build();

ODataEntitySetRequest<ClientEntitySet> request =
    client.getRetrieveRequestFactory().getEntitySetRequest(ordersUri);
ClientEntitySet orders = request.execute().getBody();
```

### Create Related Entity

```java
// Create an order for a customer
URI ordersUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .appendKeySegment("ALFKI")
    .appendNavigationSegment("Orders")
    .build();

ClientEntity newOrder = client.getObjectFactory()
    .newEntity(new FullQualifiedName("MyService", "Order"));
// ... set properties

ODataEntityCreateRequest<ClientEntity> createRequest =
    client.getCUDRequestFactory().getEntityCreateRequest(ordersUri, newOrder);
createRequest.execute();
```

### Create Reference (Link Entities)

```java
// Link existing product to category
URI productRef = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .appendKeySegment(1)
    .build();

URI categoryNavUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Categories")
    .appendKeySegment(5)
    .appendNavigationSegment("Products")
    .appendRefSegment()
    .build();

ODataReferenceAddingRequest refRequest =
    client.getCUDRequestFactory().getReferenceAddingRequest(
        categoryNavUri, productRef);
refRequest.execute();
```

## Actions and Functions

### Invoke Unbound Function

```java
URI functionUri = client.newURIBuilder(serviceRoot)
    .appendOperationCallSegment("GetProductCount")
    .build();

ODataInvokeRequest<ClientPrimitiveValue> request =
    client.getInvokeRequestFactory().getFunctionInvokeRequest(
        functionUri, ClientPrimitiveValue.class);
ClientPrimitiveValue result = request.execute().getBody();
int count = result.toCastValue(Integer.class);
```

### Invoke Bound Function

```java
URI functionUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .appendKeySegment(1)
    .appendOperationCallSegment("MyService.GetRelatedProducts")
    .build();

ODataInvokeRequest<ClientEntitySet> request =
    client.getInvokeRequestFactory().getFunctionInvokeRequest(
        functionUri, ClientEntitySet.class);
ClientEntitySet relatedProducts = request.execute().getBody();
```

### Invoke Action with Parameters

```java
URI actionUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .appendKeySegment(1)
    .appendOperationCallSegment("MyService.ApplyDiscount")
    .build();

Map<String, ClientValue> parameters = new HashMap<>();
parameters.put("percent", client.getObjectFactory()
    .newPrimitiveValueBuilder().buildInt32(10));

ODataInvokeRequest<ClientEntity> request =
    client.getInvokeRequestFactory().getActionInvokeRequest(
        actionUri, ClientEntity.class, parameters);
ClientEntity discountedProduct = request.execute().getBody();
```

## Batch Requests

### Simple Batch

```java
// Create batch request
URI batchUri = client.newURIBuilder(serviceRoot).appendBatchSegment().build();
ODataBatchRequest batchRequest =
    client.getBatchRequestFactory().getBatchRequest(serviceRoot);

BatchManager manager = batchRequest.payloadManager();

// Add read request
URI customersUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Customers")
    .top(5)
    .build();
ODataEntitySetRequest<ClientEntitySet> readRequest =
    client.getRetrieveRequestFactory().getEntitySetRequest(customersUri);
manager.addRequest(readRequest);

// Add another read request
URI productsUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .top(5)
    .build();
ODataEntitySetRequest<ClientEntitySet> readRequest2 =
    client.getRetrieveRequestFactory().getEntitySetRequest(productsUri);
manager.addRequest(readRequest2);

// Execute batch
ODataBatchResponse batchResponse = manager.getResponse();

// Process responses
Iterator<ODataBatchResponseItem> iter = batchResponse.getBody();
while (iter.hasNext()) {
    ODataBatchResponseItem item = iter.next();
    // Process each response
}
```

### Changeset (Atomic Operations)

```java
ODataBatchRequest batchRequest =
    client.getBatchRequestFactory().getBatchRequest(serviceRoot);
BatchManager manager = batchRequest.payloadManager();

// Start changeset
ODataChangeset changeset = manager.addChangeset();

// Add create operation
ClientEntity newProduct = // ... build entity
URI productsUri = client.newURIBuilder(serviceRoot)
    .appendEntitySetSegment("Products")
    .build();
ODataEntityCreateRequest<ClientEntity> createRequest =
    client.getCUDRequestFactory().getEntityCreateRequest(productsUri, newProduct);
changeset.addRequest(createRequest);

// Add update operation
URI productUri = // ...
ODataEntityUpdateRequest<ClientEntity> updateRequest =
    client.getCUDRequestFactory().getEntityUpdateRequest(
        productUri, UpdateType.PATCH, updateEntity);
changeset.addRequest(updateRequest);

// Execute - all operations succeed or fail together
ODataBatchResponse response = manager.getResponse();
```

## Error Handling

```java
try {
    ODataRetrieveResponse<ClientEntity> response = request.execute();
    ClientEntity entity = response.getBody();
} catch (ODataClientErrorException e) {
    // 4xx errors
    int statusCode = e.getStatusLine().getStatusCode();
    ODataError error = e.getODataError();
    System.err.println("Error: " + error.getMessage());
    System.err.println("Code: " + error.getCode());
} catch (ODataServerErrorException e) {
    // 5xx errors
    System.err.println("Server error: " + e.getMessage());
} catch (HttpClientException e) {
    // Connection errors
    System.err.println("Connection error: " + e.getMessage());
}
```

## Configuration

### HTTP Configuration

```java
ODataClient client = ODataClientFactory.getClient();
HttpClientConfiguration config = client.getConfiguration();

// Set timeouts
config.setHttpConnectionTimeout(30000);  // 30 seconds
config.setHttpReceiveTimeout(60000);     // 60 seconds

// Set default headers
config.setDefaultHeaders(Map.of(
    "Accept-Language", "en-US",
    "X-Custom-Header", "value"
));
```

### Proxy Configuration

```java
HttpClientConfiguration config = client.getConfiguration();
config.setProxyHost("proxy.example.com");
config.setProxyPort(8080);
config.setProxyUsername("user");
config.setProxyPassword("password");
```

### Authentication

```java
// Basic authentication
ODataClient client = ODataClientFactory.getClient();
client.getConfiguration().setHttpCredentials(
    new UsernamePasswordCredentials("username", "password"));

// OAuth (add token to requests)
ODataEntityRequest<ClientEntity> request = // ...
request.addCustomHeader("Authorization", "Bearer " + accessToken);
```

## See Also

* [Getting Started Guide](../getting-started.md)
* [Architecture Overview](architecture.md)
* [Server Development Guide](server-guide.md)
