# Quarkus Integration Guide

## Introduction

Olinguito provides a native Quarkus extension for building OData V4 services. The extension leverages Quarkus build-time optimization for:

* Fast startup times
* Low memory footprint
* Native image compilation support (GraalVM)
* Vert.x-based HTTP handling

## Getting Started

### Maven Dependency

```xml
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-server-adapter-quarkus</artifactId>
    <version>${olinguito.version}</version>
</dependency>
```

### Project Structure

```
my-odata-service/
├── src/main/java/
│   └── com/example/
│       ├── MyEdmProvider.java      # EDM definition
│       ├── MyEntityProcessor.java   # Request processor
│       └── MyApplication.java       # Optional: custom config
├── src/main/resources/
│   └── application.properties       # Quarkus config
└── pom.xml
```

## Configuration

### application.properties

```properties
# OData service path (default: /odata)
olinguito.odata.path=/api/odata

# Enable debug mode (default: false)
olinguito.odata.debug=true

# Service namespace
olinguito.odata.namespace=MyService

# Maximum page size for server-driven paging
olinguito.odata.max-page-size=100
```

### Configuration Reference

| Property | Default | Description |
| --- | --- | --- |
| `olinguito.odata.path` | `/odata` | Base path for the OData service |
| `olinguito.odata.debug` | `false` | Enable debug output (?odata-debug=json) |
| `olinguito.odata.namespace` | `OData` | Default namespace for the service |
| `olinguito.odata.max-page-size` | `1000` | Maximum entities per page |
| `olinguito.odata.batch.enabled` | `true` | Enable $batch endpoint |

## Implementing Your Service

### Step 1: Create the EDM Provider

Use CDI to make your provider discoverable:

```java
import jakarta.enterprise.context.ApplicationScoped;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.*;

@ApplicationScoped
public class ProductEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "ProductService";
    public static final FullQualifiedName CONTAINER =
        new FullQualifiedName(NAMESPACE, "Container");

    public static final String ET_PRODUCT = "Product";
    public static final FullQualifiedName ET_PRODUCT_FQN =
        new FullQualifiedName(NAMESPACE, ET_PRODUCT);

    public static final String ES_PRODUCTS = "Products";

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        if (entityTypeName.equals(ET_PRODUCT_FQN)) {
            CsdlProperty id = new CsdlProperty()
                .setName("ID")
                .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
                .setNullable(false);

            CsdlProperty name = new CsdlProperty()
                .setName("Name")
                .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                .setMaxLength(100);

            CsdlProperty price = new CsdlProperty()
                .setName("Price")
                .setType(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName())
                .setPrecision(10)
                .setScale(2);

            CsdlPropertyRef keyRef = new CsdlPropertyRef().setName("ID");

            return new CsdlEntityType()
                .setName(ET_PRODUCT)
                .setProperties(Arrays.asList(id, name, price))
                .setKey(Collections.singletonList(keyRef));
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer,
                                       String entitySetName) {
        if (entityContainer.equals(CONTAINER) &&
            entitySetName.equals(ES_PRODUCTS)) {
            return new CsdlEntitySet()
                .setName(ES_PRODUCTS)
                .setType(ET_PRODUCT_FQN);
        }
        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        return new CsdlEntityContainer()
            .setName("Container")
            .setEntitySets(Collections.singletonList(
                getEntitySet(CONTAINER, ES_PRODUCTS)));
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        CsdlSchema schema = new CsdlSchema()
            .setNamespace(NAMESPACE)
            .setEntityTypes(Collections.singletonList(
                getEntityType(ET_PRODUCT_FQN)))
            .setEntityContainer(getEntityContainer());
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

### Step 2: Create Entity Processor

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sitenetsoft.olinguito.server.api.processor.*;

@ApplicationScoped
public class ProductProcessor implements EntityCollectionProcessor, EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    @Inject
    ProductRepository productRepository;  // Your data access

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

        EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
        EdmEntityType entityType = edmEntitySet.getEntityType();

        // Use injected repository
        List<Product> products = productRepository.findAll();
        EntityCollection entityCollection = toEntityCollection(products);

        // Apply query options
        applyQueryOptions(uriInfo, entityCollection);

        // Serialize
        ODataSerializer serializer = odata.createSerializer(responseFormat);
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

        SerializerResult result = serializer.entityCollection(
            serviceMetadata, entityType, entityCollection, options);

        response.setContent(result.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE,
            responseFormat.toContentTypeString());
    }

    @Override
    public void readEntity(ODataRequest request, ODataResponse response,
                          UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        // Implementation similar to collection, but for single entity
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response,
                            UriInfo uriInfo, ContentType requestFormat,
                            ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        // Parse request and create entity
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response,
                            UriInfo uriInfo, ContentType requestFormat,
                            ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        // Parse request and update entity
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response,
                            UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {
        // Delete entity
    }

    private EntityCollection toEntityCollection(List<Product> products) {
        EntityCollection collection = new EntityCollection();
        for (Product p : products) {
            Entity entity = new Entity()
                .addProperty(new Property(null, "ID",
                    ValueType.PRIMITIVE, p.getId()))
                .addProperty(new Property(null, "Name",
                    ValueType.PRIMITIVE, p.getName()))
                .addProperty(new Property(null, "Price",
                    ValueType.PRIMITIVE, p.getPrice()));
            collection.getEntities().add(entity);
        }
        return collection;
    }
}
```

## Using with Panache (JPA)

Combine with Quarkus Panache for easy data access:

### Entity

```java
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends PanacheEntity {
    public String name;
    public BigDecimal price;
    public String category;
}
```

### Repository

```java
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public List<Product> findByCategory(String category) {
        return find("category", category).list();
    }

    public List<Product> findByPriceGreaterThan(BigDecimal minPrice) {
        return find("price > ?1", minPrice).list();
    }
}
```

### Processor with Panache

```java
@ApplicationScoped
public class ProductProcessor implements EntityCollectionProcessor {

    @Inject
    ProductRepository repository;

    @Override
    public void readEntityCollection(ODataRequest request,
                                     ODataResponse response,
                                     UriInfo uriInfo,
                                     ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // Use Panache for data access
        List<Product> products = repository.listAll();

        // Convert to OData entities
        EntityCollection collection = new EntityCollection();
        for (Product p : products) {
            Entity entity = new Entity()
                .addProperty(new Property(null, "ID",
                    ValueType.PRIMITIVE, p.id))
                .addProperty(new Property(null, "Name",
                    ValueType.PRIMITIVE, p.name))
                .addProperty(new Property(null, "Price",
                    ValueType.PRIMITIVE, p.price));
            collection.getEntities().add(entity);
        }

        // Serialize and respond...
    }
}
```

## Reactive Support

Use Quarkus reactive with Mutiny:

```java
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Product extends PanacheEntity {
    public String name;
    public BigDecimal price;

    public static Uni<List<Product>> findAllAsync() {
        return listAll();
    }
}
```

## Dev Mode

Run in Quarkus dev mode for hot reload:

```bash
./mvnw quarkus:dev
```

Your OData service will be available at:

* Service document: http://localhost:8080/odata/
* Metadata: http://localhost:8080/odata/$metadata
* Entities: http://localhost:8080/odata/Products

Changes to your code will be automatically reloaded.

## Testing

### Unit Testing

```java
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class ProductODataTest {

    @Test
    public void testGetProducts() {
        given()
            .when().get("/odata/Products")
            .then()
                .statusCode(200)
                .contentType("application/json")
                .body("value", notNullValue());
    }

    @Test
    public void testGetMetadata() {
        given()
            .when().get("/odata/$metadata")
            .then()
                .statusCode(200)
                .contentType(containsString("xml"));
    }

    @Test
    public void testFilterProducts() {
        given()
            .when().get("/odata/Products?$filter=Price gt 100")
            .then()
                .statusCode(200)
                .body("value.size()", greaterThan(0));
    }
}
```

### Integration Testing with Test Containers

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class ProductODataIntegrationTest {
    // Tests with real database
}
```

## Native Image

Build a native executable:

```bash
./mvnw package -Pnative
```

Or with Docker:

```bash
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

Run the native executable:

```bash
./target/my-odata-service-1.0.0-runner
```

## Docker Deployment

### Dockerfile.jvm

```dockerfile
FROM registry.access.redhat.com/ubi8/openjdk-17:1.18

ENV LANGUAGE='en_US:en'

COPY target/quarkus-app/lib/ /deployments/lib/
COPY target/quarkus-app/*.jar /deployments/
COPY target/quarkus-app/app/ /deployments/app/
COPY target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
```

### Build and Run

```bash
./mvnw package
docker build -f src/main/docker/Dockerfile.jvm -t my-odata-service .
docker run -p 8080:8080 my-odata-service
```

## OpenAPI Integration

Expose OData metadata alongside OpenAPI:

```properties
# application.properties
quarkus.smallrye-openapi.path=/openapi
quarkus.swagger-ui.path=/swagger-ui
quarkus.swagger-ui.always-include=true
```

## Health Checks

Add health check for your OData service:

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import jakarta.enterprise.context.ApplicationScoped;

@Readiness
@ApplicationScoped
public class ODataHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("OData service is ready");
    }
}
```

## See Also

* [Getting Started Guide](../getting-started.md)
* [Server Development Guide](server-guide.md)
* [Architecture Overview](architecture.md)
* [Quarkus Guides](https://quarkus.io/guides/)
