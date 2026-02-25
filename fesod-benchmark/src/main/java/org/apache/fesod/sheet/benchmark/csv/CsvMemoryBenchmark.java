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
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CSV Memory Usage Benchmark
 * 
 * Tests memory allocation and GC pressure during CSV operations.
 * 
 * Based on Design Document Performance Analysis:
 * - Expected: 60-70% reduction in per-record memory overhead
 * - uniVocity: String[] (~16 + 4*N bytes) vs Commons CSV: CSVRecord (~160 bytes overhead)
 * - For 1M rows × 10 columns: ~160 MB less garbage per 1M rows
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
public class CsvMemoryBenchmark {

    @State(Scope.Benchmark)
    public static class MemoryState {
        public CsvBenchmarkState benchmarkState;
        public Path tempDir;

        @Setup
        public void setup() {
            benchmarkState = new CsvBenchmarkState();
            try {
                benchmarkState.setup();
                tempDir = Files.createTempDirectory("csv-memory-benchmark");
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
     * Read and process small dataset - measures allocation rate
     * Processes each row immediately (simulating real usage)
     */
    @Benchmark
    public int readAndProcessSmall(MemoryState state, Blackhole bh) {
        ProcessingListener listener = new ProcessingListener(bh);
        FesodSheet.read(state.benchmarkState.smallCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .sheet()
            .doRead();
        return listener.getCount();
    }

    /**
     * Read and process medium dataset - measures allocation rate
     */
    @Benchmark
    public int readAndProcessMedium(MemoryState state, Blackhole bh) {
        ProcessingListener listener = new ProcessingListener(bh);
        FesodSheet.read(state.benchmarkState.mediumCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .sheet()
            .doRead();
        return listener.getCount();
    }

    /**
     * Read and process large dataset - primary memory metric
     * Tests GC pressure with 100K rows
     */
    @Benchmark
    public int readAndProcessLarge(MemoryState state, Blackhole bh) {
        ProcessingListener listener = new ProcessingListener(bh);
        FesodSheet.read(state.benchmarkState.largeCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .sheet()
            .doRead();
        return listener.getCount();
    }

    /**
     * Batch read to memory - measures peak memory usage
     * Loads all data into memory at once
     */
    @Benchmark
    public List<Map<Integer, String>> batchReadToMemory(MemoryState state) {
        return FesodSheet.read(state.benchmarkState.mediumCsvFile)
            .csv()
            .doReadSync();
    }

    /**
     * Write from memory - measures allocation during write
     */
    @Benchmark
    public int writeFromMemory(MemoryState state, Blackhole bh) throws IOException {
        File outputFile = state.createTempFile("write-memory");

        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.mediumData);

        bh.consume(outputFile.length());
        return state.benchmarkState.mediumData.size();
    }

    /**
     * Streaming read with transformation - measures GC pressure
     * Transforms each row (creates new objects)
     */
    @Benchmark
    public int streamingTransform(MemoryState state, Blackhole bh) {
        TransformListener listener = new TransformListener(bh);
        FesodSheet.read(state.benchmarkState.mediumCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .doRead();
        return listener.getCount();
    }

    /**
     * Read with auto-trim enabled - measures string allocation
     * Tests additional string allocations from trimming
     */
    @Benchmark
    public int readWithTrim(MemoryState state, Blackhole bh) {
        ProcessingListener listener = new ProcessingListener(bh);
        FesodSheet.read(state.benchmarkState.mediumCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .autoTrim(true)
            .doRead();
        return listener.getCount();
    }

    /**
     * Read without auto-trim - baseline for string allocation
     */
    @Benchmark
    public int readWithoutTrim(MemoryState state, Blackhole bh) {
        ProcessingListener listener = new ProcessingListener(bh);
        FesodSheet.read(state.benchmarkState.mediumCsvFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .autoTrim(false)
            .doRead();
        return listener.getCount();
    }

    /**
     * Listener that processes data (simulates real usage).
     */
    public static class ProcessingListener implements ReadListener<CsvBenchmarkState.CsvRow> {
        private final Blackhole bh;
        private int count = 0;

        public ProcessingListener(Blackhole bh) {
            this.bh = bh;
        }

        @Override
        public void invoke(CsvBenchmarkState.CsvRow data, AnalysisContext context) {
            // Process the data (consume to prevent dead code elimination)
            bh.consume(data.field1);
            bh.consume(data.field2);
            bh.consume(data.number1);
            bh.consume(data.number2);
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

    /**
     * Listener that transforms data (creates new objects).
     */
    public static class TransformListener implements ReadListener<CsvBenchmarkState.CsvRow> {
        private final Blackhole bh;
        private int count = 0;

        public TransformListener(Blackhole bh) {
            this.bh = bh;
        }

        @Override
        public void invoke(CsvBenchmarkState.CsvRow data, AnalysisContext context) {
            // Transform: create new object with modified data
            CsvBenchmarkState.CsvRow transformed = new CsvBenchmarkState.CsvRow(
                data.field1 != null ? data.field1.toUpperCase() : null,
                data.field2 != null ? data.field2.toUpperCase() : null,
                data.field3 != null ? data.field3.toUpperCase() : null,
                data.field4 != null ? data.field4.toUpperCase() : null,
                data.field5 != null ? data.field5.toUpperCase() : null,
                data.number1 != null ? data.number1 * 2 : null,
                data.number2 != null ? data.number2 * 2 : null
            );
            bh.consume(transformed);
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
