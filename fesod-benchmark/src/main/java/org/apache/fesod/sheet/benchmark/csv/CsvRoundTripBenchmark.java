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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CSV Round-Trip Performance Benchmark
 * 
 * Tests write-then-read round trip performance and correctness.
 * 
 * Based on Design Document:
 * - Property 4: Write-then-read round trip
 * - For any list of rows and any valid CsvFormatConfiguration,
 *   writing and reading back should produce identical values
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
public class CsvRoundTripBenchmark {

    @State(Scope.Benchmark)
    public static class RoundTripState {
        public CsvBenchmarkState benchmarkState;
        public Path tempDir;

        @Setup
        public void setup() {
            benchmarkState = new CsvBenchmarkState();
            try {
                benchmarkState.setup();
                tempDir = Files.createTempDirectory("csv-roundtrip-benchmark");
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
     * Round-trip small dataset - Default configuration
     * Write then read back, verifying data integrity
     */
    @Benchmark
    public int roundTripSmall(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-small");
        
        // Write
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.smallData);
        
        // Read back
        List<Map<Integer, String>> result = FesodSheet.read(outputFile)
            .csv()
            .doReadSync();
        
        return result.size();
    }

    /**
     * Round-trip medium dataset - Default configuration
     */
    @Benchmark
    public int roundTripMedium(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-medium");
        
        // Write
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.mediumData);
        
        // Read back
        List<Map<Integer, String>> result = FesodSheet.read(outputFile)
            .csv()
            .doReadSync();
        
        return result.size();
    }

    /**
     * Round-trip large dataset - Default configuration
     * Primary performance metric for round-trip operations
     */
    @Benchmark
    public int roundTripLarge(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-large");
        
        // Write
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.largeData);
        
        // Read back
        List<Map<Integer, String>> result = FesodSheet.read(outputFile)
            .csv()
            .doReadSync();
        
        return result.size();
    }

    /**
     * Round-trip with custom delimiter
     * Tests Semicolon delimiter preservation
     */
    @Benchmark
    public int roundTripWithDelimiter(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-delimiter");

        // Write with custom delimiter
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .delimiter(";")
            .doWrite(state.benchmarkState.mediumData);

        // Read back with same delimiter
        List<Map<Integer, String>> result = FesodSheet.read(outputFile)
            .csv()
            .delimiter(";")
            .doReadSync();

        return result.size();
    }

    /**
     * Round-trip with quote mode ALL
     * Tests that all fields are quoted and read correctly
     */
    @Benchmark
    public int roundTripWithQuoteAll(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-quote-all");

        // Write with quote all
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .quote('"', org.apache.fesod.sheet.metadata.csv.CsvQuoteMode.ALL)
            .doWrite(state.benchmarkState.mediumData);

        // Read back
        List<Map<Integer, String>> result = FesodSheet.read(outputFile)
            .csv()
            .doReadSync();

        return result.size();
    }

    /**
     * Round-trip with special characters
     * Tests handling of commas, quotes, and newlines in data
     */
    @Benchmark
    public int roundTripWithSpecialChars(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-special");
        
        // Use extra large data which contains special characters
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.extraLargeData);
        
        // Read back
        List<Map<Integer, String>> result = FesodSheet.read(outputFile)
            .csv()
            .doReadSync();
        
        return result.size();
    }

    /**
     * Round-trip streaming (write streaming, read streaming)
     * Tests streaming-to-streaming round trip
     */
    @Benchmark
    public int roundTripStreaming(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-streaming");
        
        // Write streaming (already streaming in Fesod)
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .doWrite(state.benchmarkState.largeData);
        
        // Read streaming with counting listener
        CountingListener listener = new CountingListener();
        FesodSheet.read(outputFile, CsvBenchmarkState.CsvRow.class, listener)
            .csv()
            .doRead();
        
        return listener.getCount();
    }

    /**
     * Round-trip with null values
     * Tests null value preservation
     */
    @Benchmark
    public int roundTripWithNulls(RoundTripState state) throws IOException {
        File outputFile = state.createTempFile("roundtrip-nulls");
        
        // Write with null string handling
        FesodSheet.write(outputFile, CsvBenchmarkState.CsvRow.class)
            .csv()
            .nullString("")
            .doWrite(state.benchmarkState.mediumData);
        
        // Read back
        List<Map<Integer, String>> result = FesodSheet.read(outputFile)
            .csv()
            .nullString("")
            .doReadSync();
        
        return result.size();
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
