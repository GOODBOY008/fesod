#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Fesod CSV Parser Integration Benchmark Runner
# Compares Commons CSV (released version) vs uniVocity (current version)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/target/benchmark-results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Fesod CSV Parser Integration Benchmark${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Parse arguments
QUICK_RUN=false
GC_PROFILING=false
MEMORY_PROFILING=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --quick)
            QUICK_RUN=true
            shift
            ;;
        --gc)
            GC_PROFILING=true
            shift
            ;;
        --memory)
            MEMORY_PROFILING=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --quick     Quick run (fewer iterations)"
            echo "  --gc        Enable GC profiling"
            echo "  --memory    Enable memory profiling"
            echo "  --help      Show this help"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Set benchmark parameters
if [ "$QUICK_RUN" = true ]; then
    WARMUP_ITERATIONS=2
    MEASURE_ITERATIONS=3
    FORK_COUNT=1
    RUN_TIME=3
    echo -e "${YELLOW}Running quick benchmark...${NC}"
else
    WARMUP_ITERATIONS=3
    MEASURE_ITERATIONS=5
    FORK_COUNT=1
    RUN_TIME=5
    echo -e "${YELLOW}Running standard benchmark...${NC}"
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Build the benchmark module
echo ""
echo -e "${BLUE}Building benchmark module...${NC}"
cd "$PROJECT_ROOT"
./mvnw clean package -DskipTests -pl fesod-benchmark-integration -am -q

# Prepare JVM options
JVM_OPTS=""

if [ "$GC_PROFILING" = true ]; then
    JVM_OPTS="$JVM_OPTS -Xlog:gc*:file=$OUTPUT_DIR/gc-$TIMESTAMP.log:time,uptime"
    JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"
    echo -e "${YELLOW}GC profiling enabled${NC}"
fi

if [ "$MEMORY_PROFILING" = true ]; then
    JVM_OPTS="$JVM_OPTS -XX:NativeMemoryTracking=summary"
    echo -e "${YELLOW}Memory profiling enabled${NC}"
fi

# Run benchmarks
echo ""
echo -e "${BLUE}Running benchmarks...${NC}"
echo ""

BENCHMARK_JAR="$PROJECT_ROOT/fesod-benchmark-integration/target/integration-benchmarks.jar"

if [ ! -f "$BENCHMARK_JAR" ]; then
    echo -e "${RED}Error: Benchmark JAR not found at $BENCHMARK_JAR${NC}"
    exit 1
fi

# Run with specified parameters
java $JVM_OPTS \
    -jar "$BENCHMARK_JAR" \
    -wi $WARMUP_ITERATIONS \
    -i $MEASURE_ITERATIONS \
    -f $FORK_COUNT \
    -r ${RUN_TIME}s \
    -rf json \
    -rff "$OUTPUT_DIR/results-$TIMESTAMP.json" \
    -rff "$OUTPUT_DIR/results-latest.json" \
    "FesodIntegrationBenchmark" 2>&1 | tee "$OUTPUT_DIR/results-$TIMESTAMP.txt"

# Generate summary report
echo ""
echo -e "${BLUE}Generating summary report...${NC}"

cat > "$OUTPUT_DIR/summary-$TIMESTAMP.md" << EOF
# Fesod CSV Parser Integration Benchmark Results

**Date:** $(date)
**Run Type:** $([ "$QUICK_RUN" = true ] && echo "Quick" || echo "Standard")

## Configuration

- Warmup Iterations: $WARMUP_ITERATIONS
- Measurement Iterations: $MEASURE_ITERATIONS
- Forks: $FORK_COUNT
- Run Time: ${RUN_TIME}s
- GC Profiling: $GC_PROFILING
- Memory Profiling: $MEMORY_PROFILING

## Results Summary

### Read Throughput (ops/sec)

| Parser | 1K rows | 10K rows | 50K rows |
|--------|---------|----------|----------|
| Commons CSV | - | - | - |
| uniVocity | - | - | - |
| **Improvement** | - | - | - |

### Write Throughput (ops/sec)

| Parser | 1K rows | 10K rows | 50K rows |
|--------|---------|----------|----------|
| Commons CSV | - | - | - |
| uniVocity | - | - | - |
| **Improvement** | - | - | - |

### Memory Overhead (bytes/row)

| Parser | Allocation Rate | GC Frequency |
|--------|----------------|--------------|
| Commons CSV | - | - |
| uniVocity | - | - |
| **Reduction** | - | - |

## GC Analysis

$([ "$GC_PROFILING" = true ] && echo "See: gc-$TIMESTAMP.log" || echo "GC profiling not enabled. Run with --gc flag.")

## Raw Results

- JSON: results-$TIMESTAMP.json
- Text: results-$TIMESTAMP.txt

## Next Steps

1. Review the detailed results in the JSON file
2. Compare with baseline if available
3. Check GC logs for memory pressure analysis
4. Run with --memory for detailed allocation profiling
EOF

echo -e "${GREEN}Summary report generated: $OUTPUT_DIR/summary-$TIMESTAMP.md${NC}"

# Show results location
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Benchmark Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Results saved to:"
echo "  - JSON: $OUTPUT_DIR/results-$TIMESTAMP.json"
echo "  - Text: $OUTPUT_DIR/results-$TIMESTAMP.txt"
echo "  - Summary: $OUTPUT_DIR/summary-$TIMESTAMP.md"
echo ""

# Show quick summary from results
if command -v jq &> /dev/null; then
    echo -e "${BLUE}Quick Summary:${NC}"
    jq -r '.benchmarks[] | "\(.benchmark): \(.primaryMetric.score) ± \(.primaryMetric.scoreError) \(.primaryMetric.scoreUnit)"' \
        "$OUTPUT_DIR/results-$TIMESTAMP.json" 2>/dev/null || true
fi

echo ""
echo -e "${YELLOW}To view detailed GC analysis:${NC}"
echo "  cat $OUTPUT_DIR/gc-$TIMESTAMP.log | grep -E 'Pause|GC'"
echo ""
echo -e "${YELLOW}To compare with previous run:${NC}"
echo "  diff $OUTPUT_DIR/results-previous.json $OUTPUT_DIR/results-$TIMESTAMP.json"
echo ""
