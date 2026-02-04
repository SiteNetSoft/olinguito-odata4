# Olinguito OData Technical Service for Quarkus

This module provides a Quarkus-based implementation of the OData Technical Service, serving as both a test harness for the Quarkus adapter and a **reference implementation** for building optimized native OData applications.

## Quick Start

### JVM Mode
```bash
mvn quarkus:dev -pl lib/server-tecsvc-quarkus
```
Access at: http://localhost:8080/odata-server-tecsvc/odata.svc

### Native Build (Standard)
```bash
# With Podman
mvn package -Pnative -Dquarkus.native.container-build=true \
    -Dquarkus.native.container-runtime=podman -pl lib/server-tecsvc-quarkus

# Run the native executable
./lib/server-tecsvc-quarkus/target/server-tecsvc-quarkus-runner
```

## Profile-Guided Optimization (PGO)

PGO improves native image performance by **10-30%** by optimizing hot code paths based on actual execution profiles. This is particularly effective for OData services where JSON/XML serialization and URI parsing are performance-critical.

### Prerequisites

- [K6 load testing tool](https://k6.io/docs/get-started/installation/)
- **Oracle GraalVM installed locally** (PGO is not available in Mandrel/Community editions)
- ~15 minutes for the full workflow

> **Important**: PGO requires Oracle GraalVM installed locally (not container builds). Mandrel does not support PGO.
>
> Install Oracle GraalVM with SDKMAN:
> ```bash
> sdk install java 21.0.2-graal
> sdk use java 21.0.2-graal
> ```
> Or download from: https://www.graalvm.org/downloads/

### PGO Workflow

#### Step 1: Build Instrumented Binary

Build a native image that collects execution profiles (requires local Oracle GraalVM):

```bash
mvn package -Pnative-pgo-instrument -pl lib/server-tecsvc-quarkus
```

#### Step 2: Run Instrumented Binary

Start the instrumented application:

```bash
./lib/server-tecsvc-quarkus/target/server-tecsvc-quarkus-runner
```

#### Step 3: Generate Load with K6

In another terminal, run the load test to exercise code paths:

```bash
cd lib/server-tecsvc-quarkus
k6 run src/test/k6/load-test.js
```

The K6 script exercises:
- Service document requests (10%)
- Metadata queries - JSON and XML (10%)
- Entity set reads (20%)
- Queries with $select (15%)
- Queries with $filter (15%)
- Single entity reads (10%)
- Queries with $orderby (10%)
- Pagination with $top/$skip (10%)

#### Step 4: Stop and Collect Profile

Stop the application with `Ctrl+C`. This writes `default.iprof` to the current directory.

Copy the profile to the resources directory:

```bash
cp default.iprof src/main/resources/
```

#### Step 5: Build Optimized Binary

Build the final optimized native image (requires local Oracle GraalVM):

```bash
mvn package -Pnative-pgo -pl lib/server-tecsvc-quarkus
```

### Fully Optimized Build

For maximum performance, use the `native-optimized` profile which combines PGO with:

- **G1 GC**: Better throughput under concurrent load
- **CPU targeting**: Optimized for modern x86-64-v3 processors (AVX2, BMI2)
- **ML-based inlining**: Improved optimization decisions

```bash
mvn package -Pnative-optimized -pl lib/server-tecsvc-quarkus
```

> **Note**: The `-march=x86-64-v3` flag requires CPUs from ~2013 onwards (Haswell/Excavator or later). Remove this flag for broader compatibility.

## CPU-Optimized Build (Works with Mandrel)

For improved performance without requiring Oracle GraalVM, use CPU targeting:

```bash
mvn package -Pnative-cpu-optimized \
    -Dquarkus.native.container-build=true \
    -Dquarkus.native.container-runtime=podman \
    -pl lib/server-tecsvc-quarkus
```

This enables modern CPU instructions (AVX2, BMI2) for ~5-15% performance improvement on processors from 2013+.

## Oracle GraalVM vs Mandrel Feature Comparison

| Feature | Oracle GraalVM | Mandrel |
|---------|---------------|---------|
| PGO (Profile-Guided Optimization) | Yes | No |
| G1 GC | Yes | No |
| `-march` CPU targeting | Yes | Yes |
| Quick builds (`-Ob`) | Yes | Yes |
| Container builds | Limited* | Yes |

\* Oracle GraalVM container images have compatibility issues with Quarkus; local installation recommended.

## Available Build Profiles

| Profile | Description | Requires |
|---------|-------------|----------|
| `native` | Standard native build | Mandrel (container) or GraalVM (local) |
| `native-fast` | Quick build, less optimization | Mandrel (container) or GraalVM (local) |
| `native-cpu-optimized` | CPU targeting for modern processors | Mandrel (container) or GraalVM (local) |
| `native-pgo-instrument` | Builds instrumented binary for PGO | Oracle GraalVM (local only) |
| `native-pgo` | Builds with collected profiles | Oracle GraalVM (local only) |
| `native-optimized` | PGO + G1 GC + CPU targeting | Oracle GraalVM (local only) |

## Adapting for Your Application

This module serves as a template. To optimize your own OData application:

### 1. Copy the K6 Script

Copy `src/test/k6/load-test.js` to your project and modify:

```javascript
// Update base URL and path
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ODATA_PATH = __ENV.ODATA_PATH || '/your-service-path';

// Update entity sets to match your data model
const entitySets = ['YourEntitySet1', 'YourEntitySet2'];

// Add application-specific queries
function testYourCustomQuery() {
    const res = http.get(`${SERVICE_URL}/Products?$expand=Category`, { headers });
    // ...
}
```

### 2. Copy Maven Profiles

Add the PGO profiles from this module's `pom.xml` to your project.

### 3. Customize Load Distribution

Adjust the scenario distribution in `load-test.js` to match your application's actual usage patterns:

```javascript
// Example: If your app is read-heavy with lots of filtering
if (scenario < 0.05) {
    testServiceDocument();      // 5%
} else if (scenario < 0.10) {
    testMetadata();             // 5%
} else if (scenario < 0.50) {
    testEntitySetWithFilter();  // 40% - your most common operation
} else if (scenario < 0.80) {
    testEntitySetRead();        // 30%
} else {
    testSingleEntity();         // 20%
}
```

### 4. Run the PGO Workflow

Follow the same 5-step workflow with your customized scripts.

## Testing

### JVM Tests
```bash
mvn test -pl lib/server-tecsvc-quarkus
```

### Native Integration Tests
```bash
mvn verify -Pnative \
    -Dquarkus.native.container-build=true \
    -Dquarkus.native.container-runtime=podman \
    -pl lib/server-tecsvc-quarkus
```

## Performance Tips

1. **Profile with realistic data**: The more representative your K6 workload, the better the optimization.

2. **Re-profile after major changes**: If you significantly change your data model or add new operations, regenerate profiles.

3. **Test both cold and warm performance**: Native images have fast startup but may need warmup for peak throughput.

4. **Monitor in production**: Use Quarkus Micrometer extension to track actual performance metrics.

## Troubleshooting

### "Profile file not found"
Ensure `default.iprof` is copied to `src/main/resources/` before building with `-Pnative-pgo`.

### K6 connection errors
Make sure the application is running before starting K6. Check the service is accessible at the configured URL.

### Container build fails
Ensure Podman/Docker has sufficient resources. Native builds require ~4GB RAM minimum.
