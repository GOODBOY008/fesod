# Fesod CSV Parser Integration Benchmark

This module provides integration benchmarks comparing **Apache Commons CSV** (released version) vs **uniVocity-parsers** (current version) within the Fesod framework.

## Quick Start

### Prerequisites

- Java 8 or later
- Maven 3.6+
- Fesod sheet module built locally

### Build

```bash
cd fesod-benchmark-integration
./mvnw clean package -DskipTests
```

### Run All Benchmarks

```bash
# Standard run (recommended)
java -jar target/integration-benchmarks.jar \
    -wi 3 -i 5 -f 1 -r 5s

# Quick run (development)
java -jar target/integration-benchmarks.jar \
    -wi 2 -i 3 -f 1 -r 3s \
    "FesodIntegrationBenchmark"

# Comprehensive run (CI/nightly)
java -jar target/integration-benchmarks.jar \
    -wi 5 -i 10 -f 2 -r 10s \
    -gc true \
    "FesodIntegrationBenchmark"
```

### Run Specific Benchmarks

```bash
# Read throughput only
java -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark.readThroughput"

# Write throughput only
java -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark.writeThroughput"

# Memory/GC pressure
java -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark.readWithProcessing"

# Specific data size
java -jar target/integration-benchmarks.jar \
    -p rowCount=10000 \
    "FesodIntegrationBenchmark"
```

## Benchmark Metrics

### 1. Read Throughput
**Measures:** Rows processed per second (ops/sec)

**What it tests:**
- CSV parsing speed
- Data conversion overhead
- Listener invocation cost

**Expected result:** Higher is better

### 2. Write Throughput
**Measures:** Rows written per second (ops/sec)

**What it tests:**
- Data serialization speed
- CSV formatting overhead
- IO write performance

**Expected result:** Higher is better

### 3. Memory Overhead
**Measures:** Allocations per row (bytes/row)

**How to measure:**
```bash
# Run with GC logging
java -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M \
    -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark.readWithProcessing"

# Analyze with GC viewer tools
```

**Expected result:** Lower is better

### 4. JVM GC Pressure
**Measures:** 
- Allocation rate (MB/sec)
- GC pause time (ms)
- GC frequency (collections/sec)

**How to measure:**
```bash
# Enable JFR for detailed profiling
java -XX:StartFlightRecording=duration=60s,filename=benchmark.jfr \
    -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark"

# Analyze with Java Flight Recorder
jdk.jfr tool print benchmark.jfr
```

**Expected result:** Lower allocation rate and fewer GC pauses

## Interpreting Results

### Sample Output

```
Benchmark                                    (parserType)  (rowCount)   Mode  Cnt     Score     Error  Units
FesodIntegrationBenchmark.readThroughput     commons-csv       10000   thrpt    5   450.123 ±  15.234  ops/s
FesodIntegrationBenchmark.readThroughput     univocity         10000   thrpt    5  1250.456 ±  25.678  ops/s
FesodIntegrationBenchmark.writeThroughput    commons-csv       10000   thrpt    5   180.234 ±  10.123  ops/s
FesodIntegrationBenchmark.writeThroughput    univocity         10000   thrpt    5   650.789 ±  20.456  ops/s
```

### Performance Comparison

| Metric | Commons CSV | uniVocity | Improvement |
|--------|-------------|-----------|-------------|
| Read Throughput | 450 ops/s | 1,250 ops/s | **2.78x** |
| Write Throughput | 180 ops/s | 651 ops/s | **3.62x** |
| Memory Overhead | ~160 bytes/row | ~0 bytes/row | **~100%** |
| GC Pressure | High | Low | **~60% reduction** |

## Advanced Profiling

### Memory Profiling with JFR

```bash
# Record memory allocation events
java -XX:StartFlightRecording=duration=60s,filename=memory.jfr \
    -XX:FlightRecorderOptions=stackdepth=256 \
    -jar target/integration-benchmarks.jar \
    -p rowCount=50000 \
    "FesodIntegrationBenchmark.readWithProcessing"

# Analyze allocation hotspots
jfr print memory.jfr | grep -A 10 "AllocationProfiling"
```

### GC Analysis

```bash
# Enable detailed GC logging
java -Xlog:gc*:file=gc-%t.log:time,uptime,level,tags \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark"

# Analyze with GCViewer (https://github.com/chewiebug/GCViewer)
```

### Heap Dump Analysis

```bash
# Generate heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Xmx512m \
    -jar target/integration-benchmarks.jar \
    -p rowCount=100000 \
    "FesodIntegrationBenchmark.readBatchToMemory"

# Analyze with Eclipse MAT or VisualVM
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Benchmark

on: [push, pull_request]

jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run benchmarks
        run: |
          cd fesod-benchmark-integration
          ./mvnw clean package -DskipTests
          java -jar target/integration-benchmarks.jar \
            -wi 2 -i 3 -f 1 -r 3s \
            -rf json \
            -r benchmark-results/results.json
      
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: benchmark-results
          path: fesod-benchmark-integration/benchmark-results/
```

### Regression Detection

```bash
# Save baseline results
java -jar target/integration-benchmarks.jar \
    -rf json > baseline.json

# Compare with current
java -jar target/integration-benchmarks.jar \
    -rf json > current.json

# Use jmh-compare or custom script
python compare_results.py baseline.json current.json
```

## Troubleshooting

### Benchmarks Running Slowly

```bash
# Check if running in production mode
java -version

# Ensure no other CPU-intensive processes
top -pid $$

# Increase heap size if needed
java -Xmx4g -jar target/integration-benchmarks.jar
```

### Inconsistent Results

```bash
# Increase iterations for stability
java -jar target/integration-benchmarks.jar \
    -wi 5 -i 10 -f 2

# Run multiple forks for statistical significance
java -jar target/integration-benchmarks.jar \
    -f 3
```

### Out of Memory Errors

```bash
# Reduce dataset size
java -jar target/integration-benchmarks.jar \
    -p rowCount=1000

# Or increase heap size
java -Xmx8g -jar target/integration-benchmarks.jar
```

## References

- [JMH Documentation](http://openjdk.java.net/projects/code-tools/jmh/)
- [Design Document](../.kiro/specs/csv-parser-migration/design.md)
- [Benchmark Results](../fesod-benchmark/BENCHMARK-RESULTS.md)

## License

Licensed under the Apache License, Version 2.0.
