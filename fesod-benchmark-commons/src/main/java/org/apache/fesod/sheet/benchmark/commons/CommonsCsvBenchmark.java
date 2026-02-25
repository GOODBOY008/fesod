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

package org.apache.fesod.sheet.benchmark.commons;

import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for Fesod with Apache Commons CSV (released version 2.0.1-incubating)
 * 
 * Measures:
 * - Read Throughput (rows/sec)
 * - Write Throughput (rows/sec)
 * - Memory Overhead (via Blackhole)
 * - JVM GC Pressure (allocation rate)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
public class CommonsCsvBenchmark {

    @Param({"1000", "10000", "50000", "100000", "500000", "1000000"})
    public int rowCount;

    public File csvFile;
    public List<TestData> testData;
    private final Random random = new Random(42);

    public static class TestData {
        public String field1;
        public String field2;
        public String field3;
        public Integer number1;
        public Double number2;

        public TestData() {}

        public TestData(String f1, String f2, String f3, Integer n1, Double n2) {
            this.field1 = f1;
            this.field2 = f2;
            this.field3 = f3;
            this.number1 = n1;
            this.number2 = n2;
        }
    }

    @Setup(Level.Iteration)
    public void setup() throws IOException {
        testData = generateData(rowCount);
        Path tempDir = Files.createTempDirectory("commons-benchmark");
        csvFile = Files.createTempFile(tempDir, "benchmark-", ".csv").toFile();
        writeCsvFile(csvFile, testData);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        if (csvFile != null && csvFile.exists()) {
            csvFile.delete();
        }
    }

    private List<TestData> generateData(int size) {
        List<TestData> data = new ArrayList<>(size);
        String[] specialChars = {"", ",", "\"", "\n", " ", "  "};
        
        for (int i = 0; i < size; i++) {
            String special = specialChars[random.nextInt(specialChars.length)];
            data.add(new TestData(
                "value_" + i + (special != null ? special : ""),
                "data_" + (i * 2),
                "test_" + (i * 3) + (special != null ? special : ""),
                i,
                i * 1.5 + 0.5
            ));
        }
        return data;
    }

    private void writeCsvFile(File file, List<TestData> data) throws IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
            writer.println("field1,field2,field3,number1,number2");
            for (TestData row : data) {
                writer.printf("\"%s\",\"%s\",\"%s\",%d,%.2f%n",
                    escapeCsv(row.field1),
                    escapeCsv(row.field2),
                    escapeCsv(row.field3),
                    row.number1 != null ? row.number1 : "",
                    row.number2 != null ? row.number2 : ""
                );
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    // ========================================================================
    // READ BENCHMARKS
    // ========================================================================

    /**
     * Read throughput - streaming with listener
     */
    @Benchmark
    public int readThroughput() {
        ReadCountingListener listener = new ReadCountingListener();
        FesodSheet.read(csvFile, TestData.class, listener)
            .sheet()
            .doRead();
        return listener.getCount();
    }

    /**
     * Read with processing - measures allocation and GC pressure
     */
    @Benchmark
    public int readWithProcessing(Blackhole bh) {
        ProcessingListener listener = new ProcessingListener(bh);
        FesodSheet.read(csvFile, TestData.class, listener)
            .sheet()
            .doRead();
        return listener.getCount();
    }

    /**
     * Batch read to memory - measures peak memory usage
     */
    @Benchmark
    public List<TestData> readBatchToMemory() {
        BatchReadListener listener = new BatchReadListener();
        FesodSheet.read(csvFile, TestData.class, listener)
            .sheet()
            .doRead();
        return listener.getData();
    }

    // ========================================================================
    // WRITE BENCHMARKS
    // ========================================================================

    /**
     * Write throughput
     */
    @Benchmark
    public int writeThroughput() throws IOException {
        File outputFile = Files.createTempFile("write-benchmark-", ".csv").toFile();
        try {
            FesodSheet.write(outputFile, TestData.class)
                .sheet()
                .doWrite(testData);
            return testData.size();
        } finally {
            outputFile.delete();
        }
    }

    /**
     * Write with transformation - measures allocation during write
     */
    @Benchmark
    public int writeWithTransformation() throws IOException {
        File outputFile = Files.createTempFile("write-transform-", ".csv").toFile();
        try {
            List<TestData> transformed = transformData(testData);
            FesodSheet.write(outputFile, TestData.class)
                .sheet()
                .doWrite(transformed);
            return transformed.size();
        } finally {
            outputFile.delete();
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private List<TestData> transformData(List<TestData> data) {
        List<TestData> transformed = new ArrayList<>(data.size());
        for (TestData row : data) {
            transformed.add(new TestData(
                row.field1 != null ? row.field1.toUpperCase() : null,
                row.field2 != null ? row.field2.toUpperCase() : null,
                row.field3 != null ? row.field3.toUpperCase() : null,
                row.number1 != null ? row.number1 * 2 : null,
                row.number2 != null ? row.number2 * 2 : null
            ));
        }
        return transformed;
    }

    // ========================================================================
    // LISTENER CLASSES
    // ========================================================================

    public static class ReadCountingListener implements ReadListener<TestData> {
        private int count = 0;

        @Override
        public void invoke(TestData data, org.apache.fesod.sheet.context.AnalysisContext context) {
            count++;
        }

        @Override
        public void doAfterAllAnalysed(org.apache.fesod.sheet.context.AnalysisContext context) {
            // No-op
        }

        public int getCount() {
            return count;
        }
    }

    public static class ProcessingListener implements ReadListener<TestData> {
        private final Blackhole bh;
        private int count = 0;

        public ProcessingListener(Blackhole bh) {
            this.bh = bh;
        }

        @Override
        public void invoke(TestData data, org.apache.fesod.sheet.context.AnalysisContext context) {
            bh.consume(data.field1);
            bh.consume(data.field2);
            bh.consume(data.number1);
            bh.consume(data.number2);
            count++;
        }

        @Override
        public void doAfterAllAnalysed(org.apache.fesod.sheet.context.AnalysisContext context) {
            // No-op
        }

        public int getCount() {
            return count;
        }
    }

    public static class BatchReadListener implements ReadListener<TestData> {
        private List<TestData> data = new ArrayList<>();

        @Override
        public void invoke(TestData data, org.apache.fesod.sheet.context.AnalysisContext context) {
            this.data.add(data);
        }

        @Override
        public void doAfterAllAnalysed(org.apache.fesod.sheet.context.AnalysisContext context) {
            // No-op
        }

        public List<TestData> getData() {
            return data;
        }
    }
}
