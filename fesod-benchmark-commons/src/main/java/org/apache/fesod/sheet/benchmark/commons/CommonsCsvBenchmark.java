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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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

    @Param({"10000", "50000", "100000", "500000"})
    public int rowCount;

    public File csvFile;
    public List<TestData> testData;
    private final Random random = new Random(42);

    public static class TestData {
        // String fields (5)
        public String stringField1;
        public String stringField2;
        public String stringField3;
        public String stringField4;
        public String stringField5;

        // Numeric fields (4)
        public Integer intField;
        public Long longField;
        public Double doubleField1;
        public Double doubleField2;

        // Boolean field (1)
        public Boolean booleanField;

        // Date field (1)
        public String dateField;

        // Decimal field (1)
        public java.math.BigDecimal decimalField;

        public TestData() {}

        public TestData(
                String f1,
                String f2,
                String f3,
                String f4,
                String f5,
                Integer i1,
                Long l1,
                Double d1,
                Double d2,
                Boolean b1,
                String date1,
                java.math.BigDecimal dec1) {
            this.stringField1 = f1;
            this.stringField2 = f2;
            this.stringField3 = f3;
            this.stringField4 = f4;
            this.stringField5 = f5;
            this.intField = i1;
            this.longField = l1;
            this.doubleField1 = d1;
            this.doubleField2 = d2;
            this.booleanField = b1;
            this.dateField = date1;
            this.decimalField = dec1;
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
        String[] prefixes = {"alpha", "beta", "gamma", "delta", "epsilon"};

        for (int i = 0; i < size; i++) {
            String special = specialChars[random.nextInt(specialChars.length)];
            String prefix = prefixes[i % prefixes.length];
            data.add(new TestData(
                    // String fields (5)
                    "value_" + prefix + "_" + i + (special != null ? special : ""),
                    "data_" + prefix + "_" + (i * 2) + (special != null ? special : ""),
                    "test_" + prefix + "_" + (i * 3) + (special != null ? special : ""),
                    "description_" + i + "_item",
                    "category_" + (i % 10),
                    // Numeric fields (4)
                    i,
                    (long) i * 1000,
                    i * 1.5 + 0.5,
                    i * 2.5 + 1.5,
                    // Boolean field (1)
                    i % 2 == 0,
                    // Date field (1)
                    "2024-" + String.format("%02d", i % 12 + 1) + "-" + String.format("%02d", i % 28 + 1),
                    // Decimal field (1)
                    new java.math.BigDecimal(i * 10.5).setScale(2, java.math.RoundingMode.HALF_UP)));
        }
        return data;
    }

    private void writeCsvFile(File file, List<TestData> data) throws IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
            writer.println(
                    "stringField1,stringField2,stringField3,stringField4,stringField5,intField,longField,doubleField1,doubleField2,booleanField,dateField,decimalField");
            for (TestData row : data) {
                writer.printf(
                        "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%.2f,%.2f,%s,\"%s\",%.2f%n",
                        escapeCsv(row.stringField1),
                        escapeCsv(row.stringField2),
                        escapeCsv(row.stringField3),
                        escapeCsv(row.stringField4),
                        escapeCsv(row.stringField5),
                        row.intField != null ? row.intField : "",
                        row.longField != null ? row.longField : "",
                        row.doubleField1 != null ? row.doubleField1 : "",
                        row.doubleField2 != null ? row.doubleField2 : "",
                        row.booleanField != null ? row.booleanField : "",
                        escapeCsv(row.dateField),
                        row.decimalField != null ? row.decimalField : "");
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
        FesodSheet.read(csvFile, TestData.class, listener).sheet().doRead();
        return listener.getCount();
    }

    /**
     * Read with processing - measures allocation and GC pressure
     */
    @Benchmark
    public int readWithProcessing(Blackhole bh) {
        ProcessingListener listener = new ProcessingListener(bh);
        FesodSheet.read(csvFile, TestData.class, listener).sheet().doRead();
        return listener.getCount();
    }

    /**
     * Batch read to memory - measures peak memory usage
     */
    @Benchmark
    public List<TestData> readBatchToMemory() {
        BatchReadListener listener = new BatchReadListener();
        FesodSheet.read(csvFile, TestData.class, listener).sheet().doRead();
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
            FesodSheet.write(outputFile, TestData.class).sheet().doWrite(testData);
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
            FesodSheet.write(outputFile, TestData.class).sheet().doWrite(transformed);
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
                    row.stringField1 != null ? row.stringField1.toUpperCase() : null,
                    row.stringField2 != null ? row.stringField2.toUpperCase() : null,
                    row.stringField3 != null ? row.stringField3.toUpperCase() : null,
                    row.stringField4 != null ? row.stringField4.toUpperCase() : null,
                    row.stringField5 != null ? row.stringField5.toUpperCase() : null,
                    row.intField != null ? row.intField * 2 : null,
                    row.longField != null ? row.longField * 2 : null,
                    row.doubleField1 != null ? row.doubleField1 * 2 : null,
                    row.doubleField2 != null ? row.doubleField2 * 2 : null,
                    row.booleanField != null ? !row.booleanField : null,
                    row.dateField != null ? row.dateField : null,
                    row.decimalField != null ? row.decimalField.multiply(new java.math.BigDecimal("2")) : null));
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
            bh.consume(data.stringField1);
            bh.consume(data.stringField2);
            bh.consume(data.stringField3);
            bh.consume(data.stringField4);
            bh.consume(data.stringField5);
            bh.consume(data.intField);
            bh.consume(data.longField);
            bh.consume(data.doubleField1);
            bh.consume(data.doubleField2);
            bh.consume(data.booleanField);
            bh.consume(data.dateField);
            bh.consume(data.decimalField);
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
