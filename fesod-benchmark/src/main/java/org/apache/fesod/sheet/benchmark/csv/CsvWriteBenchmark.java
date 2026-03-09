/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fesod.sheet.benchmark.csv;

import org.apache.fesod.sheet.FesodSheet;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CSV Write Performance Benchmark
 * 
 * Tests the write throughput of uniVocity-parsers (current implementation)
 * 
 * Based on Design Document Performance Analysis:
 * - Expected: ~400K-600K rows/sec with uniVocity (1.5-2x faster than Commons CSV)
 * - Tests streaming write with row cache and batch write
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
public class CsvWriteBenchmark {

    @State(Scope.Benchmark)
    public static class WriteState {
        public CsvBenchmarkState benchmarkState;
        public Path tempDir;

        @Setup
        public void setup() {
            benchmarkState = new CsvBenchmarkState();
            try {
                benchmarkState.setup();
                tempDir = Files.createTempDirectory("csv-write-benchmark");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @TearDown
        public void tearDown() {
            benchmarkState.tearDown();
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                        .map(Path::toFile)
                        .forEach(File::delete);
                    tempDir.toFile().delete();
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
        }

        public File createTempFile(String prefix) throws IOException {
            return Files.createTempFile(tempDir, prefix, ".csv").toFile();
        }
    }

    /**
     * Write small dataset (1K rows) - Standard configuration
     */
    @Benchmark
    public int writeSmall(WriteState state) throws IOException {
        File outputFile = state.createTempFile("small");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.smallData);
        return state.benchmarkState.smallData.size();
    }

    /**
     * Write medium dataset (10K rows) - Standard configuration
     */
    @Benchmark
    public int writeMedium(WriteState state) throws IOException {
        File outputFile = state.createTempFile("medium");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.mediumData);
        return state.benchmarkState.mediumData.size();
    }

    /**
     * Write large dataset (100K rows) - Standard configuration
     * Primary performance metric for write operations
     */
    @Benchmark
    public int writeLarge(WriteState state) throws IOException {
        File outputFile = state.createTempFile("large");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.largeData);
        return state.benchmarkState.largeData.size();
    }

    /**
     * Write with custom delimiter - Large dataset
     * Tests performance with semicolon delimiter
     */
    @Benchmark
    public int writeLargeWithDelimiter(WriteState state) throws IOException {
        File outputFile = state.createTempFile("large-delimiter");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .delimiter(";")
            .doWrite(state.benchmarkState.largeData);
        return state.benchmarkState.largeData.size();
    }

    /**
     * Write with quote mode ALL - Large dataset
     * Tests performance when quoting all fields
     */
    @Benchmark
    public int writeLargeWithQuoteAll(WriteState state) throws IOException {
        File outputFile = state.createTempFile("large-quote-all");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .quote('"', org.apache.fesod.sheet.metadata.csv.CsvQuoteMode.ALL)
            .doWrite(state.benchmarkState.largeData);
        return state.benchmarkState.largeData.size();
    }

    /**
     * Write with custom record separator - Large dataset
     * Tests performance with LF line endings
     */
    @Benchmark
    public int writeLargeWithLineFeed(WriteState state) throws IOException {
        File outputFile = state.createTempFile("large-lf");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .recordSeparator("\n")
            .doWrite(state.benchmarkState.largeData);
        return state.benchmarkState.largeData.size();
    }

    /**
     * Write without header - Large dataset
     * Tests performance when skipping header row
     */
    @Benchmark
    public int writeLargeWithoutHeader(WriteState state) throws IOException {
        File outputFile = state.createTempFile("large-no-header");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .needHead(false)
            .csv()
            .doWrite(state.benchmarkState.largeData);
        return state.benchmarkState.largeData.size();
    }

    /**
     * Write with null string handling - Large dataset
     * Tests performance with custom null value representation
     */
    @Benchmark
    public int writeLargeWithNullString(WriteState state) throws IOException {
        File outputFile = state.createTempFile("large-null");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .nullString("NULL")
            .doWrite(state.benchmarkState.largeData);
        return state.benchmarkState.largeData.size();
    }

    /**
     * Write with auto-trim disabled - Large dataset
     * Tests performance without trimming whitespace
     */
    @Benchmark
    public int writeLargeWithoutTrim(WriteState state) throws IOException {
        File outputFile = state.createTempFile("large-no-trim");
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .autoTrim(false)
            .csv()
            .doWrite(state.benchmarkState.largeData);
        return state.benchmarkState.largeData.size();
    }
}
