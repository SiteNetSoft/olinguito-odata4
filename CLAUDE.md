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
  server-api/           # OData Server API
  server-core/          # OData Server core (engine-agnostic)
  server-core-ext/      # Server extensions
  server-adapter-servlet/  # Jakarta Servlet adapter
  server-adapter-quarkus/  # Quarkus adapter
  server-tecsvc/        # Test Entity Container service
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
client-api/client-core    server-api/server-core
                              ↓
                          server-core-ext
                              ↓
              server-adapter-servlet / server-adapter-quarkus
```

**Key technologies:**
- Jakarta Servlet 6.1.0 for web layer
- Apache CXF for JAX-RS REST services
- Jackson for JSON serialization
- Aalto/STAX for XML processing
- Netty for async streaming support
- Apache HttpComponents for HTTP client
- Embedded Tomcat 10.0.27 for integration tests (port 9080)

**Package namespace:** `org.sitenetsoft.olinguito.*`

## Testing

- Unit tests: JUnit 4.13.2 + Mockito 5.3.1
- Integration tests: Located in `fit/` module, run with `mvn verify`
- Integration test naming convention: `*ITCase.java`
- Test server runs on port 9080 (configurable via `tomcat.servlet.port`)

## Current Development

The `feature/without-servlet-continue` branch is refactoring servlet-specific code into the separate `server-adapter-servlet` module to decouple the core server from servlet dependencies.
