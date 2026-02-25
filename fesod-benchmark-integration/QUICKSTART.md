# Fesod CSV Parser Integration Benchmark - Quick Start Guide

## Overview

This guide helps you run integration benchmarks comparing **Apache Commons CSV** (released Fesod version) vs **uniVocity-parsers** (current version) with metrics for:

- ✅ **Read Throughput** (rows/sec)
- ✅ **Write Throughput** (rows/sec)
- ✅ **Memory Overhead** (bytes/row)
- ✅ **JVM GC Pressure** (allocation rate, pause time)

---

## Prerequisites

```bash
# Java 8 or later
java -version

# Maven 3.6+
./mvnw -version

# Optional: jq for JSON parsing
brew install jq  # macOS
apt-get install jq  # Linux
```

---

## Quick Start

### 1. Build the Benchmark Module

```bash
cd /Volumes/AidenExternal/aiden/IdeaProjects/fesod-ori

# Build everything (first time)
./mvnw clean install -DskipTests

# Or build just the benchmark module
./mvnw clean package -DskipTests -pl fesod-benchmark-integration -am
```

### 2. Run Benchmarks

#### Standard Run (Recommended)
```bash
./fesod-benchmark-integration/run-benchmark.sh
```

#### Quick Run (Development)
```bash
./fesod-benchmark-integration/run-benchmark.sh --quick
```

#### With GC Profiling
```bash
./fesod-benchmark-integration/run-benchmark.sh --gc
```

#### With Memory Profiling
```bash
./fesod-benchmark-integration/run-benchmark.sh --memory
```

### 3. View Results

Results are saved to:
```
fesod-benchmark-integration/target/benchmark-results/
├── results-YYYYMMDD-HHMMSS.json    # Raw JSON results
├── results-YYYYMMDD-HHMMSS.txt     # Human-readable output
├── summary-YYYYMMDD-HHMMSS.md      # Markdown summary
└── gc-YYYYMMDD-HHMMSS.log          # GC logs (if --gc flag used)
```

---

## Manual Execution

### Run Specific Benchmark

```bash
cd fesod-benchmark-integration

# Read throughput only
java -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark.readThroughput"

# Write throughput only
java -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark.writeThroughput"

# Memory/GC pressure (with Blackhole)
java -jar target/integration-benchmarks.jar \
    "FesodIntegrationBenchmark.readWithProcessing"

# Specific data size
java -jar target/integration-benchmarks.jar \
    -p rowCount=10000 \
    "FesodIntegrationBenchmark"
```

### Advanced JVM Options

```bash
# With GC logging
java -Xlog:gc*:file=gc.log:time,uptime \
    -jar target/integration-benchmarks.jar

# With JFR profiling
java -XX:StartFlightRecording=duration=60s,filename=benchmark.jfr \
    -jar target/integration-benchmarks.jar

# With heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Xmx512m \
    -jar target/integration-benchmarks.jar \
    -p rowCount=100000
```

---

## Understanding Results

### Sample Output

```
Benchmark                                    (parserType)  (rowCount)   Mode  Cnt     Score     Error  Units
FesodIntegrationBenchmark.readThroughput     commons-csv       10000   thrpt    5   450.123 ±  15.234  ops/s
FesodIntegrationBenchmark.readThroughput     univocity         10000   thrpt    5  1250.456 ±  25.678  ops/s
FesodIntegrationBenchmark.writeThroughput    commons-csv       10000   thrpt    5   180.234 ±  10.123  ops/s
FesodIntegrationBenchmark.writeThroughput    univocity         10000   thrpt    5   650.789 ±  20.456  ops/s
```

### Interpreting Metrics

| Metric | What It Measures | Good Value |
|--------|-----------------|------------|
| **Score** | Throughput (ops/sec) | Higher is better |
| **Error** | Statistical variance (±) | Lower is better |
| **Cnt** | Number of measurement iterations | 5+ recommended |
| **Units** | Measurement unit | ops/s |

### Calculating Improvement

```bash
# Read improvement
Read Improvement = uniVocity_score / Commons_CSV_score

# Example: 1250.456 / 450.123 = 2.78x faster
```

---

## GC Analysis

### View GC Logs

```bash
# Quick summary
cat target/benchmark-results/gc-*.log | grep -E "Pause|GC"

# Detailed analysis with GCViewer
# Download: https://github.com/chewiebug/GCViewer
java -jar gcviewer.jar target/benchmark-results/gc-*.log
```

### JFR Analysis

```bash
# View JFR recording
jfr print target/benchmark-results/benchmark.jfr

# Analyze memory allocations
jfr print --events JDK.ObjectAllocationInNewTLAB target/benchmark-results/benchmark.jfr
```

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Benchmark

on: [pull_request]

jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run benchmarks
        run: |
          ./fesod-benchmark-integration/run-benchmark.sh --quick
          
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: benchmark-results
          path: fesod-benchmark-integration/target/benchmark-results/
```

### Regression Detection

```bash
# Save baseline
cp target/benchmark-results/results-latest.json baseline.json

# After changes, compare
python3 << 'EOF'
import json

with open('baseline.json') as f:
    baseline = json.load(f)
    
with open('current.json') as f:
    current = json.load(f)

for b, c in zip(baseline['benchmarks'], current['benchmarks']):
    if b['benchmark'] == c['benchmark']:
        improvement = c['primaryMetric']['score'] / b['primaryMetric']['score']
        print(f"{b['benchmark']}: {improvement:.2f}x")
EOF
```

---

## Troubleshooting

### Build Failures

```bash
# Clean and rebuild
./mvnw clean install -U -DskipTests

# Check Maven version
./mvnw -version  # Should be 3.6+
```

### Benchmark Runs Slowly

```bash
# Close other applications
# Check CPU frequency
sysctl -n hw.cpufrequency_max  # macOS
lscpu | grep MHz  # Linux

# Ensure JVM is in server mode
java -server -jar target/integration-benchmarks.jar
```

### Out of Memory

```bash
# Reduce dataset size
java -jar target/integration-benchmarks.jar \
    -p rowCount=1000

# Or increase heap
java -Xmx8g -jar target/integration-benchmarks.jar
```

### Inconsistent Results

```bash
# Increase iterations for stability
java -jar target/integration-benchmarks.jar \
    -wi 5 -i 10 -f 2

# Run multiple times and average
for i in {1..3}; do
    java -jar target/integration-benchmarks.jar > run-$i.txt
done
```

---

## Expected Results

Based on the library-level benchmarks:

| Metric | Expected Improvement |
|--------|---------------------|
| Read Throughput | 2.5-3.0x |
| Write Throughput | 3.0-4.0x |
| Memory Overhead | 60-70% reduction |
| GC Pressure | 50-60% reduction |

**Note:** Integration benchmarks may show slightly lower improvements due to Fesod framework overhead.

---

## Next Steps

1. **Run the benchmark** using the standard configuration
2. **Review results** in the generated markdown summary
3. **Compare with baseline** if available
4. **Share results** with the team for review

For detailed documentation, see:
- [README.md](README.md) - Full usage guide
- [BENCHMARK-REPORT-TEMPLATE.md](BENCHMARK-REPORT-TEMPLATE.md) - Report format

---

## Support

For questions or issues:
- Check the [README.md](README.md)
- Review [BENCHMARK-RESULTS.md](../fesod-benchmark/BENCHMARK-RESULTS.md)
- Contact the Fesod development team
