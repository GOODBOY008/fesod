# CSV Parser Migration Benchmark

This benchmark module measures the performance improvements from migrating the CSV subsystem from Apache Commons CSV to uniVocity-parsers.

## Overview

The benchmarks validate the performance claims in the [Design Document](../../../../.kiro/specs/csv-parser-migration/design.md#performance--resource-impact-analysis):

### Expected Improvements

| Metric | Commons CSV (Before) | uniVocity-parsers (After) | Improvement |
|---|---|---|---|
| Read throughput | ~150K-200K rows/sec | ~500K-800K rows/sec | **3-4x faster** |
| Write throughput | ~200K-300K rows/sec | ~400K-600K rows/sec | **1.5-2x faster** |
| Per-record memory | ~160 bytes overhead | ~0 bytes (buffer reused) | **60-70% reduction** |
| GC pressure (1M rows) | ~160 MB garbage | ~0 MB garbage | **160 MB saved** |

## Benchmark Suites

### 1. CsvReadBenchmark - Read Performance

Tests CSV read throughput in various scenarios:

| Benchmark | Description | Dataset Size |
|---|---|---|
| `readSmallStreaming` | Streaming read with listener | 1,000 rows |
| `readMediumStreaming` | Streaming read with listener | 10,000 rows |
| `readLargeStreaming` | **Primary metric** - streaming read | 100,000 rows |
| `readSmallBatch` | Batch read (doReadSync) | 1,000 rows |
| `readMediumBatch` | Batch read (doReadSync) | 10,000 rows |
| `readLargeBatch` | Batch read with memory pressure | 100,000 rows |
| `readLargeWithConfig` | Streaming with custom config | 100,000 rows |
| `readMediumFromInputStream` | Read from InputStream | 10,000 rows |
| `readMediumFromReader` | Read from Reader | 10,000 rows |

### 2. CsvWriteBenchmark - Write Performance

Tests CSV write throughput:

| Benchmark | Description | Dataset Size |
|---|---|---|
| `writeSmall` | Standard write | 1,000 rows |
| `writeMedium` | Standard write | 10,000 rows |
| `writeLarge` | **Primary metric** - standard write | 100,000 rows |
| `writeLargeWithDelimiter` | Custom delimiter (;) | 100,000 rows |
| `writeLargeWithQuoteAll` | Quote all fields | 100,000 rows |
| `writeLargeWithLineFeed` | LF line endings | 100,000 rows |
| `writeLargeWithoutHeader` | No header row | 100,000 rows |
| `writeLargeWithNullString` | Custom null string | 100,000 rows |
| `writeLargeWithoutTrim` | No auto-trim | 100,000 rows |
| `writeLargeWithRowCache` | Custom row cache (1000) | 100,000 rows |

### 3. CsvRoundTripBenchmark - Write-Then-Read

Tests data integrity and round-trip performance:

| Benchmark | Description | Dataset Size |
|---|---|---|
| `roundTripSmall` | Write then read back | 1,000 rows |
| `roundTripMedium` | Write then read back | 10,000 rows |
| `roundTripLarge` | **Primary metric** - round trip | 100,000 rows |
| `roundTripWithDelimiter` | Custom delimiter preservation | 10,000 rows |
| `roundTripWithQuoteAll` | Quote mode preservation | 10,000 rows |
| `roundTripWithSpecialChars` | Special characters handling | 1,000,000 rows |
| `roundTripStreaming` | Streaming-to-streaming | 100,000 rows |
| `roundTripWithNulls` | Null value preservation | 10,000 rows |

### 4. CsvMemoryBenchmark - Memory Usage

Tests memory allocation and GC pressure:

| Benchmark | Description | Metric |
|---|---|---|
| `readAndProcessSmall` | Read with processing | Allocation rate |
| `readAndProcessMedium` | Read with processing | Allocation rate |
| `readAndProcessLarge` | **Primary metric** - GC pressure | Allocation rate |
| `batchReadToMemory` | Load all to memory | Peak memory |
| `writeFromMemory` | Write from memory | Allocation rate |
| `streamingTransform` | Transform during read | GC pressure |
| `readWithTrim` | Read with auto-trim | String allocation |
| `readWithoutTrim` | Read without trim | Baseline |

## Running Benchmarks

### Quick Run (Development)

```bash
cd fesod-benchmark
mvn clean package -Pbenchmark-quick
java -jar target/csv-benchmarks.jar CsvReadBenchmark.readLargeStreaming
```

### Standard Run (CI/CD)

```bash
cd fesod-benchmark
mvn clean package -Pbenchmark-standard
java -jar target/csv-benchmarks.jar
```

### Comprehensive Run (Nightly/Release)

```bash
cd fesod-benchmark
mvn clean package -Pbenchmark-comprehensive
java -jar target/csv-benchmarks.jar -wi 5 -i 10 -f 2
```

### Run Specific Benchmark

```bash
# Read benchmarks only
java -jar target/csv-benchmarks.jar CsvReadBenchmark

# Write benchmarks only
java -jar target/csv-benchmarks.jar CsvWriteBenchmark

# Single benchmark
java -jar target/csv-benchmarks.jar CsvReadBenchmark.readLargeStreaming
```

### Run with Custom Settings

```bash
# Custom warmup, measurement, forks
java -jar target/csv-benchmarks.jar \
  -wi 5 -i 10 -f 2 \
  -r 5s -w 3s \
  CsvReadBenchmark.readLargeStreaming

# Output to file
java -jar target/csv-benchmarks.jar > results.txt

# JSON output
java -jar target/csv-benchmarks.jar -rf json > results.json
```

## Interpreting Results

### Throughput Mode (ops/sec)

Higher is better. Example:
```
Benchmark                    Mode  Cnt     Score   Error  Units
CsvReadBenchmark.readLarge   thrpt   5  650000.5 ± 15000  ops/s
```
This means ~650K rows/sec throughput.

### Average Time Mode (ms/op)

Lower is better. Example:
```
Benchmark                    Mode  Cnt  Score   Error  Units
CsvMemoryBenchmark.readAndProcess  avgt   5  0.025 ± 0.001  ms/op
```
This means 0.025ms per row on average.

### Expected Results

Based on the design document analysis:

| Benchmark | Expected Score (uniVocity) |
|---|---|
| `CsvReadBenchmark.readLargeStreaming` | 500K-800K ops/s |
| `CsvWriteBenchmark.writeLarge` | 400K-600K ops/s |
| `CsvRoundTripBenchmark.roundTripLarge` | 200K-300K ops/s |
| `CsvMemoryBenchmark.readAndProcessLarge` | < 0.05 ms/op |

## Comparing Results

### Before vs After Migration

To compare with the old Commons CSV implementation:

1. Checkout the pre-migration branch:
   ```bash
   git checkout <commit-before-migration>
   ```

2. Run benchmarks and save results:
   ```bash
   mvn clean package -Pbenchmark-standard
   java -jar target/csv-benchmarks.jar > before.txt
   ```

3. Checkout current branch and run again:
   ```bash
   git checkout feat/csv-parser-migration-benchmark
   mvn clean package -Pbenchmark-standard
   java -jar target/csv-benchmarks.jar > after.txt
   ```

4. Compare results:
   ```bash
   # Use jmh-compare or manual comparison
   diff before.txt after.txt
   ```

### Performance Validation Checklist

- [ ] Read throughput improved by 3-4x
- [ ] Write throughput improved by 1.5-2x
- [ ] Memory allocation reduced by 60-70%
- [ ] GC pressure reduced proportionally
- [ ] Round-trip data integrity verified
- [ ] Special characters handled correctly
- [ ] Null values preserved

## Contributing New Benchmarks

### Adding a New Benchmark

1. Create a new class in `src/main/java/org/apache/fesod/sheet/benchmark/csv/`
2. Annotate with JMH annotations:
   ```java
   @State(Scope.Benchmark)
   @BenchmarkMode(Mode.Throughput)
   @OutputTimeUnit(TimeUnit.SECONDS)
   public class MyNewBenchmark {
       @Benchmark
       public void myBenchmark() {
           // ...
       }
   }
   ```

3. Follow naming conventions:
   - Use descriptive names
   - Include dataset size in documentation
   - Document expected performance

4. Add to this README

### Best Practices

- Use `@State(Scope.Benchmark)` for shared state
- Use `Blackhole` to prevent dead code elimination
- Include warmup and measurement iterations
- Document expected results
- Test with various dataset sizes
- Clean up resources in `@TearDown`

## Troubleshooting

### Benchmarks Running Slowly

- Check if running in production mode (`-Pbenchmark-standard`)
- Ensure no other CPU-intensive processes running
- Increase heap size if needed: `-Xmx4g`
- Check for GC overhead: add `-verbose:gc`

### Inconsistent Results

- Increase number of iterations: `-i 10`
- Increase number of forks: `-f 2`
- Ensure consistent test data (fixed random seed)
- Check for external factors (network, disk I/O)

### Out of Memory Errors

- Reduce dataset size
- Increase heap size: `-Xmx8g`
- Use streaming benchmarks instead of batch
- Check for memory leaks in test code

## References

- [Design Document](../../../../.kiro/specs/csv-parser-migration/design.md)
- [JMH Documentation](http://openjdk.java.net/projects/code-tools/jmh/)
- [uniVocity-parsers Documentation](https://www.univocity.com/pages/univocity_parsers_javadoc)
- [Apache Commons CSV Documentation](https://commons.apache.org/proper/commons-csv/)

## License

Licensed under the Apache License, Version 2.0.
