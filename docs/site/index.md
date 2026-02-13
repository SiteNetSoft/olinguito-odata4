# Olinguito OData V4 Documentation

## Welcome

Olinguito is a Java library for building and consuming OData V4.0 services. It is a community-maintained fork of Apache Olingo, providing both client and server implementations.

## Documentation

### Getting Started

- [Getting Started Guide](getting-started.md) - Quick start for new users, installation, and basic examples

### Core Guides

- [Server Development Guide](guides/server-guide.md) - Complete guide to building OData services
- [Client Development Guide](guides/client-guide.md) - Complete guide to consuming OData services

### Platform Integration

- [Servlet Integration Guide](guides/servlet-guide.md) - Deploying with Jakarta Servlet (Tomcat, Jetty, Spring Boot)
- [Quarkus Integration Guide](guides/quarkus-guide.md) - Native Quarkus extension with build-time optimization

### Reference

- [Architecture Overview](guides/architecture.md) - Module structure, design patterns, and technical details

### Tutorials

Step-by-step tutorials are available covering server and client development:

| Tutorial | Description |
|---|---|
| [Part 1: Read](tutorials/server/read.md) | Basic read operations |
| [Part 2: Read Entity](tutorials/server/readep.md) | Reading entity properties |
| [Part 3: Write](tutorials/server/write.md) | Create, update, and delete operations |
| [Part 4: Navigation](tutorials/server/navigation.md) | Navigation properties |
| [Part 5: Query Options (TCS)](tutorials/server/sqo-tcs.md) | Query options (top, count, skip) |
| [Part 6: Query Options (ES)](tutorials/server/sqo-es.md) | Query options (expand, select) |
| [Part 7: Query Options (O)](tutorials/server/sqo-o.md) | Query options (ordering) |
| [Part 8: Query Options (F)](tutorials/server/sqo-f.md) | Query options (filtering) |
| [Part 9: Action](tutorials/server/action.md) | Actions |
| [Part 10: Media](tutorials/server/media.md) | Media entities |
| [Part 11: Batch](tutorials/server/batch.md) | Batch operations |
| [Part 12: Deep Insert](tutorials/server/deep-insert.md) | Deep insert operations |
| [Streaming](tutorials/server/streaming.md) | Streaming support |

## Quick Reference

### Maven Coordinates

```xml
<!-- Server (Servlet) -->
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-server-adapter-servlet</artifactId>
    <version>${olinguito.version}</version>
</dependency>

<!-- Server (Quarkus) -->
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-server-adapter-quarkus</artifactId>
    <version>${olinguito.version}</version>
</dependency>

<!-- Client -->
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-client-core</artifactId>
    <version>${olinguito.version}</version>
</dependency>
```

### Key URLs

Once your service is running:

| URL | Description |
|---|---|
| `/odata/` | Service document |
| `/odata/$metadata` | Service metadata (EDM) |
| `/odata/EntitySet` | Entity collection |
| `/odata/EntitySet(key)` | Single entity |
| `/odata/EntitySet?$filter=...` | Filtered results |
| `/odata/$batch` | Batch requests |

## Quick Links

- [Source Repository](https://github.com/sitenetsoft/olinguito-odata4)
- [OData V4 Specification](https://www.odata.org/documentation/)

## Building from Source

```bash
# Clone
git clone https://github.com/sitenetsoft/olinguito-odata4.git
cd olinguito-odata4

# Build with tests
mvn -B install

# Build without tests
mvn -B install -DskipTests

# Run integration tests
mvn verify
```

Requires: Java 17+, Maven 3.9+

## License

This project is a derivative work based on Apache Olingo and is licensed under the Apache License 2.0.

Copyright 2026 SiteNetSoft. See LICENSE file for details.
