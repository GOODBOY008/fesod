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
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CSV Read Performance Benchmark
 * 
 * Tests the read throughput of uniVocity-parsers (current implementation)
 * 
 * Based on Design Document Performance Analysis:
 * - Expected: ~500K-800K rows/sec with uniVocity (3-4x faster than Commons CSV)
 * - Tests streaming read (primary use case) and batch read (doReadSync)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
public class CsvReadBenchmark {

    @State(Scope.Benchmark)
    public static class ReadState {
        public CsvBenchmarkState benchmarkState;

        @Setup
        public void setup() {
            benchmarkState = new CsvBenchmarkState();
            try {
                benchmarkState.setup();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @TearDown
        public void tearDown() {
            benchmarkState.tearDown();
        }
    }

    /**
     * Streaming read - Small dataset (1K rows)
     * Tests the primary use case: row-by-row streaming with listener
     */
    @Benchmark
    public int readSmallStreaming(ReadState state) {
        CountingListener listener = new CountingListener();
        FesodSheet.read(state.benchmarkState.smallCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .doRead();
        return listener.getCount();
    }

    /**
     * Streaming read - Medium dataset (10K rows)
     */
    @Benchmark
    public int readMediumStreaming(ReadState state) {
        CountingListener listener = new CountingListener();
        FesodSheet.read(state.benchmarkState.mediumCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .doRead();
        return listener.getCount();
    }

    /**
     * Streaming read - Large dataset (100K rows)
     * Primary performance metric for streaming use case
     */
    @Benchmark
    public int readLargeStreaming(ReadState state) {
        CountingListener listener = new CountingListener();
        FesodSheet.read(state.benchmarkState.largeCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .doRead();
        return listener.getCount();
    }

    /**
     * Batch read - Small dataset (doReadSync)
     * Tests synchronous read that loads all data into memory
     */
    @Benchmark
    public List<Map<Integer, String>> readSmallBatch(ReadState state) {
        return FesodSheet.read(state.benchmarkState.smallCsvFile)
            .csv()
            .doReadSync();
    }

    /**
     * Batch read - Medium dataset (doReadSync)
     */
    @Benchmark
    public List<Map<Integer, String>> readMediumBatch(ReadState state) {
        return FesodSheet.read(state.benchmarkState.mediumCsvFile)
            .csv()
            .doReadSync();
    }

    /**
     * Batch read - Large dataset (doReadSync)
     * Tests memory efficiency for batch operations
     */
    @Benchmark
    public List<Map<Integer, String>> readLargeBatch(ReadState state) {
        return FesodSheet.read(state.benchmarkState.largeCsvFile)
            .csv()
            .doReadSync();
    }

    /**
     * Streaming read with custom configuration - Large dataset
     * Tests performance with custom delimiter and quote settings
     */
    @Benchmark
    public int readLargeWithConfig(ReadState state) {
        CountingListener listener = new CountingListener();
        FesodSheet.read(state.benchmarkState.largeCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .delimiter(",")
            .quote('"')
            .autoTrim(true)
            .doRead();
        return listener.getCount();
    }

    /**
     * InputStream-based read - Medium dataset
     * Tests performance when reading from InputStream (common in web apps)
     */
    @Benchmark
    public int readMediumFromInputStream(ReadState state) throws IOException {
        CountingListener listener = new CountingListener();
        try (FileInputStream fis = new FileInputStream(state.benchmarkState.mediumCsvFile)) {
            FesodSheet.read(fis, CsvBenchmarkState.CsvRow.class, listener)
                .csv()
                .doRead();
        }
        return listener.getCount();
    }

    /**
     * Reader-based read - Medium dataset
     * Tests performance when reading from Reader
     * Note: Using FileInputStream instead as Reader API not available
     */
    @Benchmark
    public int readMediumFromInputStreamUtf8(ReadState state) throws IOException {
        CountingListener listener = new CountingListener();
        try (FileInputStream fis = new FileInputStream(state.benchmarkState.mediumCsvFile)) {
            FesodSheet.read(fis, CsvBenchmarkState.CsvRow.class, listener)
                .csv()
                .doRead();
        }
        return listener.getCount();
    }

    /**
     * Simple counting listener for streaming benchmarks.
     */
    public static class CountingListener implements ReadListener<CsvBenchmarkState.CsvRow> {
        private int count = 0;

        @Override
        public void invoke(CsvBenchmarkState.CsvRow data, AnalysisContext context) {
            count++;
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // No-op
        }

        public int getCount() {
            return count;
        }
    }
}
