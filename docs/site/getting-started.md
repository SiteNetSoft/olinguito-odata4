# Getting Started with Olinguito

## Introduction

Olinguito is a Java library for building and consuming OData V4.0 services. It provides:

- **Server library** - Build OData services with Jakarta Servlet or Quarkus
- **Client library** - Consume OData services from Java applications

This guide will help you get started with both use cases.

## Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

## Installation

Add Olinguito to your Maven project. Choose the modules you need:

### For OData Server (Servlet)

```xml
<dependencies>
    <!-- Server core -->
    <dependency>
        <groupId>org.sitenetsoft.olinguito</groupId>
        <artifactId>odata-server-core</artifactId>
        <version>${olinguito.version}</version>
    </dependency>

    <!-- Server extensions (optional, recommended) -->
    <dependency>
        <groupId>org.sitenetsoft.olinguito</groupId>
        <artifactId>odata-server-core-ext</artifactId>
        <version>${olinguito.version}</version>
    </dependency>

    <!-- Servlet adapter -->
    <dependency>
        <groupId>org.sitenetsoft.olinguito</groupId>
        <artifactId>odata-server-adapter-servlet</artifactId>
        <version>${olinguito.version}</version>
    </dependency>
</dependencies>
```

### For OData Server (Quarkus)

```xml
<dependencies>
    <dependency>
        <groupId>org.sitenetsoft.olinguito</groupId>
        <artifactId>odata-server-adapter-quarkus</artifactId>
        <version>${olinguito.version}</version>
    </dependency>
</dependencies>
```

### For OData Client

```xml
<dependencies>
    <dependency>
        <groupId>org.sitenetsoft.olinguito</groupId>
        <artifactId>odata-client-core</artifactId>
        <version>${olinguito.version}</version>
    </dependency>
</dependencies>
```

## Quick Start: Building an OData Server

### Step 1: Define Your Data Model (EDM)

Create an `EdmProvider` to describe your entity types and entity sets:

```java
public class DemoEdmProvider extends CsdlAbstractEdmProvider {

    // Service namespace
    public static final String NAMESPACE = "Demo";

    // Entity type name
    public static final String ET_PRODUCT_NAME = "Product";
    public static final FullQualifiedName ET_PRODUCT_FQN =
        new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

    // Entity set name
    public static final String ES_PRODUCTS_NAME = "Products";

    // Entity container name
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER =
        new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        if (entityTypeName.equals(ET_PRODUCT_FQN)) {
            // Define properties
            CsdlProperty id = new CsdlProperty()
                .setName("ID")
                .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty()
                .setName("Name")
                .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty price = new CsdlProperty()
                .setName("Price")
                .setType(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName());

            // Define key
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            // Build entity type
            return new CsdlEntityType()
                .setName(ET_PRODUCT_NAME)
                .setProperties(Arrays.asList(id, name, price))
                .setKey(Collections.singletonList(propertyRef));
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer,
                                       String entitySetName) {
        if (entityContainer.equals(CONTAINER) &&
            entitySetName.equals(ES_PRODUCTS_NAME)) {
            return new CsdlEntitySet()
                .setName(ES_PRODUCTS_NAME)
                .setType(ET_PRODUCT_FQN);
        }
        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));

        return new CsdlEntityContainer()
            .setName(CONTAINER_NAME)
            .setEntitySets(entitySets);
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);
        schema.setEntityTypes(Collections.singletonList(
            getEntityType(ET_PRODUCT_FQN)));
        schema.setEntityContainer(getEntityContainer());
        return Collections.singletonList(schema);
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(
            FullQualifiedName entityContainerName) {
        if (entityContainerName == null ||
            entityContainerName.equals(CONTAINER)) {
            return new CsdlEntityContainerInfo()
                .setContainerName(CONTAINER);
        }
        return null;
    }
}
```

### Step 2: Implement an Entity Processor

Create a processor to handle entity collection requests:

```java
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

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
            throws ODataApplicationException, SerializerException {

        // Get the entity set from the URI
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet =
            (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // Fetch data (from your data source)
        EntityCollection entityCollection = getData(edmEntitySet);

        // Serialize the response
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        EdmEntityType entityType = edmEntitySet.getEntityType();

        ContextURL contextUrl = ContextURL.with()
            .entitySet(edmEntitySet)
            .build();

        EntityCollectionSerializerOptions opts =
            EntityCollectionSerializerOptions.with()
                .contextURL(contextUrl)
                .build();

        SerializerResult serializerResult = serializer
            .entityCollection(serviceMetadata, entityType,
                            entityCollection, opts);

        // Set response
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE,
            responseFormat.toContentTypeString());
    }

    private EntityCollection getData(EdmEntitySet edmEntitySet) {
        EntityCollection collection = new EntityCollection();

        // Add sample data
        Entity product1 = new Entity()
            .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1))
            .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Laptop"))
            .addProperty(new Property(null, "Price", ValueType.PRIMITIVE, 999.99));

        Entity product2 = new Entity()
            .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2))
            .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Phone"))
            .addProperty(new Property(null, "Price", ValueType.PRIMITIVE, 599.99));

        collection.getEntities().add(product1);
        collection.getEntities().add(product2);

        return collection;
    }
}
```

### Step 3: Create the Servlet

```java
@WebServlet(urlPatterns = "/odata/*")
public class DemoServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            // Create OData handler
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(
                new DemoEdmProvider(), new ArrayList<>());
            ODataHttpHandler handler = odata.createHandler(edm);

            // Register processors
            handler.register(new DemoEntityCollectionProcessor());

            // Process the request
            handler.process(req, resp);

        } catch (RuntimeException e) {
            throw new ServletException(e);
        }
    }
}
```

### Step 4: Test Your Service

Start your application and test with these URLs:

- Service document: `http://localhost:8080/odata/`
- Metadata: `http://localhost:8080/odata/$metadata`
- Products: `http://localhost:8080/odata/Products`
- Single product: `http://localhost:8080/odata/Products(1)`

## Quick Start: Using the OData Client

### Basic Read Operations

```java
// Create client
ODataClient client = ODataClientFactory.getClient();

// Read entity collection
URI productsUri = client.newURIBuilder("http://localhost:8080/odata")
    .appendEntitySetSegment("Products")
    .build();

ODataEntitySetRequest<ClientEntitySet> request =
    client.getRetrieveRequestFactory().getEntitySetRequest(productsUri);
ODataRetrieveResponse<ClientEntitySet> response = request.execute();
ClientEntitySet entitySet = response.getBody();

for (ClientEntity entity : entitySet.getEntities()) {
    System.out.println("Product: " + entity.getProperty("Name").getValue());
}
```

### Read Single Entity

```java
URI productUri = client.newURIBuilder("http://localhost:8080/odata")
    .appendEntitySetSegment("Products")
    .appendKeySegment(1)
    .build();

ODataEntityRequest<ClientEntity> request =
    client.getRetrieveRequestFactory().getEntityRequest(productUri);
ODataRetrieveResponse<ClientEntity> response = request.execute();
ClientEntity product = response.getBody();

System.out.println("Name: " + product.getProperty("Name").getValue());
System.out.println("Price: " + product.getProperty("Price").getValue());
```

### Create Entity

```java
// Build entity
ClientEntity newProduct = client.getObjectFactory()
    .newEntity(new FullQualifiedName("Demo", "Product"));

newProduct.getProperties().add(client.getObjectFactory()
    .newPrimitiveProperty("Name",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildString("Tablet")));

newProduct.getProperties().add(client.getObjectFactory()
    .newPrimitiveProperty("Price",
        client.getObjectFactory().newPrimitiveValueBuilder()
            .buildDecimal(new BigDecimal("399.99"))));

// Send create request
URI productsUri = client.newURIBuilder("http://localhost:8080/odata")
    .appendEntitySetSegment("Products")
    .build();

ODataEntityCreateRequest<ClientEntity> request =
    client.getCUDRequestFactory().getEntityCreateRequest(productsUri, newProduct);
ODataEntityCreateResponse<ClientEntity> response = request.execute();

System.out.println("Created with ID: " +
    response.getBody().getProperty("ID").getValue());
```

## Using Query Options

OData supports powerful query options:

```java
URI uri = client.newURIBuilder("http://localhost:8080/odata")
    .appendEntitySetSegment("Products")
    .filter("Price gt 500")           // $filter
    .orderBy("Name asc")              // $orderby
    .select("ID", "Name", "Price")    // $select
    .top(10)                          // $top
    .skip(0)                          // $skip
    .count(true)                      // $count
    .build();
```

## Server-Driven Paging

For large datasets, implement server-driven paging:

```java
// In your processor, use PagingHelper
int pageSize = 20;
String skipToken = uriInfo.getSkipTokenOption() != null
    ? uriInfo.getSkipTokenOption().getValue()
    : null;

PagingHelper.applyPaging(entityCollection, pageSize,
    request.getRawRequestUri(), skipToken);
```

The response will automatically include `@odata.nextLink` when more results exist.

## Next Steps

- See the [Architecture Overview](guides/architecture.md) for understanding the module structure
- Explore the [Tutorials](tutorials/index.md) for step-by-step guides
- Review the OData V4 specification at [odata.org](http://www.odata.org/documentation/)

## Building from Source

```bash
# Clone the repository
git clone https://github.com/sitenetsoft/olinguito-odata4.git
cd olinguito-odata4

# Build with tests
mvn -B install

# Build without tests (faster)
mvn -B install -DskipTests

# Run only unit tests
mvn test

# Run integration tests
mvn verify
```
