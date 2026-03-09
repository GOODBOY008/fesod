#!/bin/bash
#
# Comprehensive Benchmark Runner with GC and Memory Profiling
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/target/benchmark-results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

echo "=========================================="
echo "Fesod CSV Parser Comprehensive Benchmark"
echo "=========================================="
echo ""

mkdir -p "$OUTPUT_DIR"

# Build benchmarks
echo "Building benchmarks..."
cd "$PROJECT_ROOT"
./mvnw clean package -DskipTests -pl fesod-benchmark-commons,fesod-benchmark-univocity -am -q

echo ""
echo "=== Running Commons CSV Benchmark with GC Logging ==="
echo ""

# Run Commons CSV with GC logging
java -Xlog:gc*:file=$OUTPUT_DIR/gc-commons-$TIMESTAMP.log:time,uptime:filecount=1,filesize=10M \
    -XX:+UseG1GC \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -XX:HeapDumpPath=$OUTPUT_DIR/ \
    -XX:+HeapDumpOnOutOfMemoryError \
    -jar fesod-benchmark-commons/target/benchmark-commons.jar \
    -wi 2 -i 3 -f 1 -r 5s \
    -rf json \
    -rff $OUTPUT_DIR/commons-results-$TIMESTAMP.json \
    2>&1 | tee $OUTPUT_DIR/commons-output-$TIMESTAMP.txt

echo ""
echo "=== Running uniVocity Benchmark with GC Logging ==="
echo ""

# Run uniVocity with GC logging
java -Xlog:gc*:file=$OUTPUT_DIR/gc-univocity-$TIMESTAMP.log:time,uptime:filecount=1,filesize=10M \
    -XX:+UseG1GC \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -XX:HeapDumpPath=$OUTPUT_DIR/ \
    -XX:+HeapDumpOnOutOfMemoryError \
    -jar fesod-benchmark-univocity/target/benchmark-univocity.jar \
    -wi 2 -i 3 -f 1 -r 5s \
    -rf json \
    -rff $OUTPUT_DIR/univocity-results-$TIMESTAMP.json \
    2>&1 | tee $OUTPUT_DIR/univocity-output-$TIMESTAMP.txt

echo ""
echo "=========================================="
echo "Benchmark Complete!"
echo "=========================================="
echo ""
echo "Results saved to:"
echo "  - Commons CSV JSON: $OUTPUT_DIR/commons-results-$TIMESTAMP.json"
echo "  - uniVocity JSON: $OUTPUT_DIR/univocity-results-$TIMESTAMP.json"
echo "  - Commons CSV GC: $OUTPUT_DIR/gc-commons-$TIMESTAMP.log"
echo "  - uniVocity GC: $OUTPUT_DIR/gc-univocity-$TIMESTAMP.log"
echo ""

# Extract GC statistics
echo "=== GC Statistics Summary ==="
echo ""

echo "Commons CSV GC Summary:"
if [ -f "$OUTPUT_DIR/gc-commons-$TIMESTAMP.log" ]; then
    echo "  Total GC Pauses: $(grep -c 'Pause' $OUTPUT_DIR/gc-commons-$TIMESTAMP.log 2>/dev/null || echo 'N/A')"
    echo "  GC Log Lines: $(wc -l < $OUTPUT_DIR/gc-commons-$TIMESTAMP.log)"
else
    echo "  GC log not found"
fi

echo ""
echo "uniVocity GC Summary:"
if [ -f "$OUTPUT_DIR/gc-univocity-$TIMESTAMP.log" ]; then
    echo "  Total GC Pauses: $(grep -c 'Pause' $OUTPUT_DIR/gc-univocity-$TIMESTAMP.log 2>/dev/null || echo 'N/A')"
    echo "  GC Log Lines: $(wc -l < $OUTPUT_DIR/gc-univocity-$TIMESTAMP.log)"
else
    echo "  GC log not found"
fi

echo ""
echo "To analyze GC logs in detail:"
echo "  cat $OUTPUT_DIR/gc-commons-$TIMESTAMP.log | grep -E 'Pause|GC'"
echo "  cat $OUTPUT_DIR/gc-univocity-$TIMESTAMP.log | grep -E 'Pause|GC'"
echo ""
