# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Olinguito is a community-maintained fork of Apache Olingo, a Java library implementing the OData V4.0 specification. It provides both client and server implementations for building OData-based services. Not affiliated with the Apache Software Foundation.

## Build Commands

```bash
# Full build with tests
mvn -B install --fail-at-end

# Run unit tests only
mvn test

# Run unit + integration tests
mvn verify

# Run a specific test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName

# Fast build (skip checkstyle, rat, pmd)
mvn -B -Pbuild.fast install

# Build with code coverage
mvn -B -Pbuild.quality install

# Skip tests entirely
mvn install -DskipTests
```

Requires Maven 3.9+ and Java 17+.

## Project Structure

```
lib/                    # Core libraries
  commons-api/          # Common API interfaces
  commons-core/         # Common implementation
  client-api/           # OData Client API
  client-core/          # OData Client implementation
  client-adapter-okhttp/  # OkHttp HTTP client adapter
  client-adapter-quarkus/  # Quarkus client CDI extension
  server-api/           # OData Server API
  server-core/          # OData Server core (engine-agnostic)
  server-core-ext/      # Server extensions
  server-adapter-servlet/  # Jakarta Servlet adapter
  server-adapter-netty/ # Netty async streaming adapter
  server-adapter-quarkus/  # Quarkus server extension
  server-tecsvc/        # Test Entity Container service (core)
  server-tecsvc-servlet/  # TecSvc servlet integration
  server-tecsvc-quarkus/  # TecSvc Quarkus integration
  server-test/          # Server test utilities
  test-fixtures/        # Test fixtures
  odata-vocabularies/   # OData vocabulary support

ext/                    # Extensions
  pojogen-maven-plugin/ # POJO generation from OData metadata
  client-proxy/         # Client proxy generation
  client-android/       # Android client

fit/                    # Functional/Integration tests (WAR with embedded Tomcat)
dist/                   # Distribution packages
samples/                # Example applications and tutorials (p0-p12)
```

## Architecture

**Module dependency flow:**
```
commons-api/commons-core
    ↓
client-api/client-core          server-api/server-core
    ↓                               ↓
client-adapter-okhttp           server-core-ext
client-adapter-quarkus
                                    ↓
              server-adapter-servlet / server-adapter-quarkus / server-adapter-netty
```

**Key technologies:**
- Jakarta Servlet 6.1.0 for web layer
- Apache CXF for JAX-RS REST services
- Jackson for JSON serialization
- Aalto/STAX for XML processing
- Netty for async streaming support
- Apache HttpComponents for HTTP client (default adapter)
- OkHttp 5.x for HTTP client (alternative adapter, used by Android module)
- Embedded Tomcat 10.1.52 for integration tests (port 9080)

**Package namespace:** `org.sitenetsoft.olinguito.*`

## Testing

- Unit tests: JUnit 5 (Jupiter 5.11.4) + Mockito 5.14.2
- Integration tests (`fit/`): JUnit 4 with vintage engine, run with `mvn verify`
- Integration test naming convention: `*ITCase.java`
- Test server runs on port 9080 (configurable via `tomcat.servlet.port`)
- Code coverage: JaCoCo (profile `build.quality`)

## Current Development

The `feature/without-servlet-continue` branch has completed the decoupling of servlet-specific code into separate adapter modules. Key changes:

- **Servlet decoupling**: `server-core` and `server-api` are now runtime-agnostic; servlet code lives in `server-adapter-servlet`
- **Netty decoupling**: Async streaming extracted to `server-adapter-netty`
- **Quarkus extensions**: Native Quarkus support for both client (`client-adapter-quarkus`) and server (`server-adapter-quarkus`)
- **Dependency cleanup**: Removed `commons-lang3`, `commons-codec`, and `commons-io` from core modules
- **Testing modernization**: Migrated to JUnit 5, JaCoCo, XMLUnit 2.x
- **HTTP client abstraction**: Transport-agnostic `ODataHttpClient`/`ODataHttpRequest`/`ODataHttpResponse` interfaces in `client-api`; Apache HttpComponents adapter in `client-core`, OkHttp adapter in `client-adapter-okhttp`
- **Android modernization**: Replaced deprecated `AndroidHttpClient` with OkHttp adapter
