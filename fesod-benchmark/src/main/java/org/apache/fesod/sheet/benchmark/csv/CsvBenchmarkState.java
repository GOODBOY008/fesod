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

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Base state for CSV benchmarks.
 * Generates test data of various sizes for benchmarking CSV read/write operations.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class CsvBenchmarkState {

    public static class CsvRow {
        public String field1;
        public String field2;
        public String field3;
        public String field4;
        public String field5;
        public Integer number1;
        public Double number2;

        public CsvRow() {}

        public CsvRow(String f1, String f2, String f3, String f4, String f5, Integer n1, Double n2) {
            this.field1 = f1;
            this.field2 = f2;
            this.field3 = f3;
            this.field4 = f4;
            this.field5 = f5;
            this.number1 = n1;
            this.number2 = n2;
        }
    }

    // Test data sizes based on design document scenarios
    public static final int SMALL_SIZE = 1_000;      // Quick tests
    public static final int MEDIUM_SIZE = 10_000;    // Standard tests
    public static final int LARGE_SIZE = 100_000;    // Comprehensive tests
    public static final int EXTRA_LARGE_SIZE = 1_000_000; // Performance validation

    private final Random random = new Random(42); // Fixed seed for reproducibility

    public File smallCsvFile;
    public File mediumCsvFile;
    public File largeCsvFile;
    public File extraLargeCsvFile;

    public List<CsvRow> smallData;
    public List<CsvRow> mediumData;
    public List<CsvRow> largeData;
    public List<CsvRow> extraLargeData;

    @org.openjdk.jmh.annotations.Setup
    public void setup() throws IOException {
        Path tempDir = Files.createTempDirectory("csv-benchmark");
        
        // Generate test data
        smallData = generateData(SMALL_SIZE);
        mediumData = generateData(MEDIUM_SIZE);
        largeData = generateData(LARGE_SIZE);
        extraLargeData = generateData(EXTRA_LARGE_SIZE);

        // Create CSV files
        smallCsvFile = createCsvFile(tempDir, "small.csv", smallData);
        mediumCsvFile = createCsvFile(tempDir, "medium.csv", mediumData);
        largeCsvFile = createCsvFile(tempDir, "large.csv", largeData);
        extraLargeCsvFile = createCsvFile(tempDir, "extra_large.csv", extraLargeData);
    }

    @org.openjdk.jmh.annotations.TearDown
    public void tearDown() {
        // Clean up temp files
        if (smallCsvFile != null) smallCsvFile.delete();
        if (mediumCsvFile != null) mediumCsvFile.delete();
        if (largeCsvFile != null) largeCsvFile.delete();
        if (extraLargeCsvFile != null) extraLargeCsvFile.delete();
    }

    private List<CsvRow> generateData(int size) {
        List<CsvRow> data = new ArrayList<>(size);
        String[] specialChars = {"", ",", "\"", "\n", " ", "  ", null};
        
        for (int i = 0; i < size; i++) {
            String special = specialChars[random.nextInt(specialChars.length)];
            data.add(new CsvRow(
                "value_" + i + (special != null ? special : ""),
                "data_" + (i * 2),
                "test_" + (i * 3) + (special != null ? special : ""),
                "field_" + i,
                "column_" + (i * 4),
                i,
                i * 1.5 + 0.5
            ));
        }
        return data;
    }

    private File createCsvFile(Path dir, String filename, List<CsvRow> data) throws IOException {
        File file = dir.resolve(filename).toFile();
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
            // Header
            writer.println("field1,field2,field3,field4,field5,number1,number2");
            // Data rows
            for (CsvRow row : data) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f%n",
                    escapeCsv(row.field1),
                    escapeCsv(row.field2),
                    escapeCsv(row.field3),
                    escapeCsv(row.field4),
                    escapeCsv(row.field5),
                    row.number1 != null ? row.number1 : "",
                    row.number2 != null ? row.number2 : ""
                );
            }
        }
        return file;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
