# SiteNetSoft Olinguito — OData V4

<p align="center">
  <img src="./assets/olinguito.png" style="width: 350px;"/>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)

A Java library for building and consuming OData V4.0 services. Olinguito is a community-maintained fork of Apache Olingo, providing both client and server implementations.

**Supported specification:** [OData 4.0](http://www.odata.org/documentation/odata-version-4-0/)

## Quick Start

### Server (Servlet)

```xml
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-server-adapter-servlet</artifactId>
    <version>${olinguito.version}</version>
</dependency>
```

### Server (Quarkus)

```xml
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-server-adapter-quarkus</artifactId>
    <version>${olinguito.version}</version>
</dependency>
```

### Client

```xml
<dependency>
    <groupId>org.sitenetsoft.olinguito</groupId>
    <artifactId>odata-client-core</artifactId>
    <version>${olinguito.version}</version>
</dependency>
```

## Building

Requires Maven 3.9+ and Java 17+.

```bash
# Build with tests
mvn -B install

# Build without tests
mvn -B install -DskipTests

# Run unit tests only
mvn test

# Run unit + integration tests
mvn verify

# Fast build (skip static analysis)
mvn -B -Pbuild.fast install
```

## Documentation

Complete documentation is available in the [docs](./docs/) directory:

- [Getting Started Guide](./docs/getting-started.adoc) — Quick start for new users
- [Server Development Guide](./docs/server-guide.adoc) — Building OData services
- [Client Development Guide](./docs/client-guide.adoc) — Consuming OData services
- [Servlet Integration Guide](./docs/servlet-guide.adoc) — Jakarta Servlet deployment
- [Quarkus Integration Guide](./docs/quarkus-guide.adoc) — Native Quarkus extension
- [Architecture Overview](./docs/architecture.adoc) — Module structure and design

**Additional resources:**
- [OData V4 Specification](https://www.odata.org/documentation/)
- [Apache Olingo Documentation](http://olingo.apache.org/) (upstream reference)

## Upstream / Affiliation

Olinguito is a community-maintained fork of Apache Olingo.
This project is **not affiliated** with the Apache Software Foundation.

## License

This project is a derivative work based on Apache Olingo and is licensed under the Apache License 2.0.

- Original work: Copyright 2014 The Apache Software Foundation
- Derivative work: Copyright 2026 SiteNetSoft

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

See [LICENSE](./LICENSE) file for full details.

## Dependencies

SiteNetSoft Olinguito uses some libraries with open source licenses that require reciprocal licensing when modified. These libraries are included in unmodified binary form and can be redistributed under terms compatible with the Apache License.

Some libraries are dual-licensed under different open source licenses and are redistributed under the license whose terms are compatible with the Apache License.