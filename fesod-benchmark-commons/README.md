# Fesod CSV Benchmark

Comprehensive benchmark suite for comparing Apache Commons CSV vs uniVocity-parsers in Fesod.

## GitHub Actions Workflows

### Comprehensive Benchmark (Manual Trigger)

**Workflow:** `.github/workflows/benchmark-comprehensive.yml`

**How to Run:**
1. Go to **Actions** tab in GitHub
2. Select **"CSV Benchmark - Comprehensive"**
3. Click **"Run workflow"**
4. Configure parameters:
   - **Dataset sizes:** `10000,50000,100000,500000` (comma-separated)
   - **Warmup iterations:** `2` (default)
   - **Measurement iterations:** `3` (default)
   - **Run time:** `5` seconds (default)
   - **Heap size:** `4g` (default)
   - **Include GC logging:** `true` (default)
5. Click **"Run workflow"**

**Artifacts Available (after completion):**
- `benchmark-results-all.zip` - **Combined package** with Commons CSV + uniVocity + Comparison Report ⭐
- `benchmark-results-commons-csv.zip` - Commons CSV raw results only (JSON, TXT, GC logs)
- `benchmark-results-univocity.zip` - uniVocity raw results only (JSON, TXT, GC logs)
- `benchmark-comparison-report.zip` - Generated comparison report (MD)

**Note:** Download `benchmark-results-all.zip` for all results in a single package!

**Scheduled Runs:** Every Sunday at 2 AM UTC

### Quick Benchmark (Manual Trigger)

**Workflow:** `.github/workflows/benchmark-quick.yml`

**How to Run:**
1. Go to **Actions** tab in GitHub
2. Select **"CSV Benchmark - Quick"**
3. Click **"Run workflow"**
4. Configure parameters:
   - **Benchmark type:** `write` | `read` | `all`
   - **Dataset size:** `10000` | `50000` | `100000` | `500000`
5. Click **"Run workflow"**

**Artifacts Available (after completion):**
- `quick-benchmark-results.zip` - Quick benchmark results for both implementations

**Runtime:** ~10 minutes

## Download Results from CI

After workflow completion:

1. Navigate to the workflow run in **Actions** tab
2. Scroll to **"Artifacts"** section at the bottom
3. Click on the artifact name to download:
   - `benchmark-results-all` - **Combined package** (Commons CSV + uniVocity + Report) ⭐
   - `benchmark-results-commons-csv` - Commons CSV results
   - `benchmark-results-univocity` - uniVocity results
   - `benchmark-comparison-report` - Generated comparison report
   - `quick-benchmark-results` - Quick benchmark results

**Retention:** Results are kept for 30 days (comprehensive) or 7 days (quick)

### Combined Package Structure

```
benchmark-results-all/
├── README.txt                  # Package summary
├── COMPARISON-REPORT.md        # Side-by-side comparison
├── commons-csv/
│   ├── commons-results.json    # Raw JSON results
│   ├── commons-output.txt      # Console output
│   └── gc-commons.log          # GC logs (if enabled)
└── univocity/
    ├── univocity-results.json  # Raw JSON results
    ├── univocity-output.txt    # Console output
    └── gc-univocity.log        # GC logs (if enabled)
```

## Local Execution

```bash
# Build benchmark modules
./mvnw clean package -DskipTests -pl fesod-benchmark-commons,fesod-benchmark-univocity -am

# Run Commons CSV benchmark
java -Xmx4g -jar fesod-benchmark-commons/target/benchmark-commons.jar \
    -wi 2 -i 3 -f 1 -r 5s \
    -rf json \
    -rff fesod-benchmark-commons/target/benchmark-results/commons-results.json

# Run uniVocity benchmark
java -Xmx4g -jar fesod-benchmark-univocity/target/benchmark-univocity.jar \
    -wi 2 -i 3 -f 1 -r 5s \
    -rf json \
    -rff fesod-benchmark-univocity/target/benchmark-results/univocity-results.json

# Run with GC logging
java -Xmx4g -Xlog:gc*:file=gc.log -jar fesod-benchmark-commons/target/benchmark-commons.jar
```

## Benchmark Configuration

### Dataset Sizes

| Size | Use Case |
|------|----------|
| 10,000 | Small batches, daily reports |
| 50,000 | Medium batches, ETL operations |
| 100,000 | Large batches, weekly aggregations |
| 500,000 | Very large batches, bulk imports |

### Recommended Parameters

| Scenario | Warmup | Measurement | Run Time | Heap |
|----------|--------|-------------|----------|------|
| Quick test | 1 | 2 | 3s | 2g |
| Standard | 2 | 3 | 5s | 4g |
| Comprehensive | 3 | 5 | 10s | 4g |
| Production validation | 5 | 10 | 30s | 8g |

## Results Format

### JSON Output

```json
{
  "benchmarks": [
    {
      "benchmark": "CommonsCsvBenchmark.writeThroughput",
      "params": {"rowCount": "50000"},
      "primaryMetric": {
        "score": 162.407,
        "scoreError": 43.800,
        "scoreUnit": "ops/s"
      }
    }
  ]
}
```

### Text Output

```
Benchmark                            (rowCount)   Mode  Cnt     Score     Error  Units
CommonsCsvBenchmark.writeThroughput       50000  thrpt    3   162.407 ±  43.800  ops/s
```

## Related Documentation

- [Comprehensive Benchmark Report](BENCHMARK-COMPREHENSIVE.md)
- [Production Sizes Report](BENCHMARK-PRODUCTION-SIZES.md)
- [Large Dataset Report](BENCHMARK-LARGE-DATASET.md)
- [Design Document](../../.kiro/specs/csv-parser-migration/design.md)
