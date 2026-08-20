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
  client-adapter-apache/  # Apache HttpComponents HTTP client adapter (default)
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
client-adapter-apache           server-core-ext
client-adapter-okhttp
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
- Embedded Tomcat 10.1.x for integration tests (port 9080)

**Package namespace:** `org.sitenetsoft.olinguito.*`

## Testing

- Unit tests: JUnit 5 (Jupiter 5.11.4) + Mockito 5.21.0
- Integration tests (`fit/`): JUnit 4 with vintage engine, run with `mvn verify`
- Integration test naming convention: `*ITCase.java`
- Test server runs on port 9080 (configurable via `tomcat.servlet.port`)
- Code coverage: JaCoCo (profile `build.quality`)

## Current Development

All development happens on `master` (single-branch repo). The runtime-decoupling effort (formerly the `feature/without-servlet-continue` branch, 143 commits) was merged via PR #1 on 2026-08-05. Key changes from that effort:

- **Servlet decoupling**: `server-core` and `server-api` are now runtime-agnostic; servlet code lives in `server-adapter-servlet`
- **Netty decoupling**: Async streaming extracted to `server-adapter-netty`
- **Quarkus extensions**: Native Quarkus support for both client (`client-adapter-quarkus`) and server (`server-adapter-quarkus`)
- **Dependency cleanup**: Removed `commons-lang3`, `commons-codec`, `commons-io`, and `commons-logging` from core modules; explicit used-but-undeclared dependencies added; unused dependencies removed
- **Testing modernization**: Migrated to JUnit 5, JaCoCo, XMLUnit 2.x, Mockito 5.21.0
- **HTTP client abstraction**: Transport-agnostic `ODataHttpClient`/`ODataHttpRequest`/`ODataHttpResponse` interfaces in `client-api`; Apache HttpComponents adapter in `client-core`, OkHttp adapter in `client-adapter-okhttp`
- **Android modernization**: Replaced deprecated `AndroidHttpClient` with OkHttp adapter
- **Karaf OSGi**: Updated features.xml bundles to match new module structure; renamed features from `olingo-*` to `olinguito-*`
- **Samples/Tutorials**: Migrated from javax.servlet 2.5 to Jakarta Servlet 6.1.0; updated web.xml namespaces to Jakarta EE 6.0
- **Quarkus**: Updated to 3.38.1 (Vert.x aligned to 4.5.30)
- **Upstream bug-fix ports**: 80 Apache Olingo JIRA tickets + 5 GitHub PRs ported with tests (full audit of OLINGO-700..1647 completed; remaining candidates deliberately deferred)
- **OpenType support**: dynamic properties on open entity/complex types (JSON; payload + $select/$filter/$orderby + direct-path CRUD)
- **OData 4.01 (Tier 5, Wave 1)**: matchesPattern, Prefer: omit-values=nulls, /$query POST-body query options (server + client); Wave 2: $schemaversion, optional function parameters; Wave 3: key-as-segment, alternate keys. **Tier 6 Wave 1**: CSDL JSON metadata (reader on server + client, conformant writer)
