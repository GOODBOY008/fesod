# Implementation Plan: CSV Parser Migration

## Overview

Migrate fesod's CSV subsystem from Apache Commons CSV to uniVocity-parsers. The implementation proceeds bottom-up: dependency swap → new abstraction types → read path migration → write path migration → builder/metadata migration → test validation.

## Tasks

- [x] 1. Update Maven dependencies
  - [x] 1.1 Replace commons-csv with univocity-parsers in pom.xml
    - In root `pom.xml`: replace `commons-csv.version` property with `univocity-parsers.version` (use 2.9.1), update `dependencyManagement` entry
    - In `fesod-sheet/pom.xml`: replace `commons-csv` dependency with `com.univocity:univocity-parsers`
    - _Requirements: 1.1, 1.3_

- [x] 2. Create fesod-owned CSV configuration abstraction
  - [x] 2.1 Create `CsvQuoteMode` enum
    - Create `org.apache.fesod.sheet.metadata.csv.CsvQuoteMode` with values: ALL, ALL_NON_NULL, MINIMAL, NON_NUMERIC, NONE
    - _Requirements: 2.1_

  - [x] 2.2 Create `CsvFormatConfiguration` class with Builder
    - Create `org.apache.fesod.sheet.metadata.csv.CsvFormatConfiguration` with fields: delimiter, quoteCharacter, quoteMode, escapeCharacter, recordSeparator, nullString, trim, skipHeaderRecord, ignoreEmptyLines
    - Implement static `Builder` inner class with fluent setters
    - Implement `toParserSettings()` converting to uniVocity `CsvParserSettings`
    - Implement `toWriterSettings()` converting to uniVocity `CsvWriterSettings`
    - Set defaults matching `CSVFormat.DEFAULT`: comma delimiter, double-quote, MINIMAL quote mode, CRLF separator
    - _Requirements: 2.1, 2.4, 2.5_

  - [ ]* 2.3 Write property test for configuration mapping (Property 1)
    - **Property 1: Configuration mapping preserves all values**
    - Generate random `CsvFormatConfiguration` instances, convert to uniVocity settings, verify all fields preserved
    - **Validates: Requirements 2.4**

  - [x] 2.4 Create `AppendableWriter` adapter
    - Create `org.apache.fesod.sheet.metadata.csv.AppendableWriter` extending `Writer`
    - Delegate `write()`, `flush()`, `close()` to the wrapped `Appendable`
    - _Requirements: 4.1_

- [x] 3. Migrate CSV read path to uniVocity-parsers
  - [x] 3.1 Update `CsvReadWorkbookHolder`
    - Replace `CSVFormat csvFormat` field with `CsvFormatConfiguration csvFormatConfiguration`
    - Replace `CSVParser csvParser` field with `com.univocity.parsers.csv.CsvParser csvParser`
    - Update constructor to read from `ReadWorkbook.getCsvFormatConfiguration()`
    - Remove all `org.apache.commons.csv` imports
    - _Requirements: 3.1_

  - [x] 3.2 Rewrite `CsvExcelReadExecutor` to use uniVocity parser
    - Replace `CSVParser` creation with uniVocity `CsvParser` using `CsvFormatConfiguration.toParserSettings()`
    - Replace `CSVRecord` iteration with `parseNext()` loop returning `String[]`
    - Update `dealRecord(String[] record, int rowIndex)` to iterate `String[]` instead of `CSVRecord`
    - Update `isBenignCsvParseException()` to detect uniVocity's `TextParsingException` for truncated quotes
    - Preserve BOM handling via `BOMInputStream`
    - Remove all `org.apache.commons.csv` imports
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [ ]* 3.3 Write property test for parsed cell metadata (Property 2)
    - **Property 2: Parsed cell metadata correctness**
    - Generate random CSV data, parse, verify rowIndex, columnIndex, stringValue for each cell
    - **Validates: Requirements 3.2**

  - [ ]* 3.4 Write property test for whitespace handling (Property 3)
    - **Property 3: Whitespace handling modes**
    - Generate random strings with whitespace, test trim/strip/none modes
    - **Validates: Requirements 3.3, 3.4**

- [x] 4. Migrate CSV write path to uniVocity-parsers
  - [x] 4.1 Rewrite `CsvSheet` to use uniVocity writer
    - Replace `CSVPrinter csvPrinter` field with `com.univocity.parsers.csv.CsvWriter csvWriter`
    - Replace `CSVFormat csvFormat` field with `CsvFormatConfiguration csvFormatConfiguration`
    - Update `initSheet()` to create `CsvWriter` via `new CsvWriter(writer, settings)`
    - Update `flushData()` to use `csvWriter.writeRow(String[])` per row
    - Update `close()` to call `csvWriter.close()`
    - Preserve BOM prefix writing logic
    - Remove all `org.apache.commons.csv` imports
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 4.2 Update `CsvWorkbook`
    - Replace `CSVFormat csvFormat` field with `CsvFormatConfiguration csvFormatConfiguration`
    - Remove `org.apache.commons.csv.CSVFormat` import
    - _Requirements: 2.1_

- [x] 5. Checkpoint - Verify read and write paths compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Migrate builder and metadata layer
  - [x] 6.1 Update `ReadWorkbook` and `WriteWorkbook`
    - Replace `CSVFormat csvFormat` field with `CsvFormatConfiguration csvFormatConfiguration` in both classes
    - Add `@Deprecated` bridge method `setCsvFormat(CSVFormat)` that converts to `CsvFormatConfiguration` and logs deprecation warning
    - Add corresponding getter `getCsvFormatConfiguration()`
    - Remove `org.apache.commons.csv.CSVFormat` import (keep only in deprecated bridge)
    - _Requirements: 2.2, 2.3, 5.5_

  - [x] 6.2 Rewrite `CsvReaderBuilder` to use `CsvFormatConfiguration`
    - Replace `CSVFormat.Builder csvFormatBuilder` with `CsvFormatConfiguration.Builder configBuilder`
    - Update all fluent methods (`delimiter()`, `quote()`, `escape()`, `recordSeparator()`, `nullString()`) to delegate to `configBuilder`
    - Update `buildExcelReader()` to call `configBuilder.build()` and store in `ReadWorkbook`
    - Replace `QuoteMode` parameter in `quote()` with `CsvQuoteMode`
    - Remove all `org.apache.commons.csv` imports
    - _Requirements: 5.1, 2.2_

  - [x] 6.3 Rewrite `CsvWriterBuilder` to use `CsvFormatConfiguration`
    - Replace `CSVFormat.Builder csvFormatBuilder` with `CsvFormatConfiguration.Builder configBuilder`
    - Update all fluent methods to delegate to `configBuilder`
    - Update `buildExcelWriter()` to call `configBuilder.build()` and store in `WriteWorkbook`
    - Replace `QuoteMode` parameter in `quote()` with `CsvQuoteMode`
    - Remove all `org.apache.commons.csv` imports
    - _Requirements: 5.2, 2.3_

- [x] 7. Remove all remaining Commons CSV references
  - [x] 7.1 Remove commons-csv dependency and clean up imports
    - Verify no remaining `org.apache.commons.csv` imports in `fesod-sheet/src/main`
    - Remove commons-csv from `pom.xml` dependencyManagement if not already done
    - Remove `commons-csv.version` property from root `pom.xml`
    - _Requirements: 1.2_

- [x] 8. Checkpoint - Full compilation and existing tests
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Update and add tests
  - [x] 9.1 Update `CsvFormatTest` for uniVocity-parsers
    - Update any test helper methods that directly use `CSVFormat` (e.g., `writeWithCommonCsv`) to use `CsvFormatConfiguration` or uniVocity equivalents
    - Ensure all existing test assertions pass without modification
    - _Requirements: 7.1_

  - [x] 9.2 Update `CsvReadTest` examples for uniVocity-parsers
    - Update any direct `CSVFormat` usage in example tests
    - Ensure all existing test assertions pass without modification
    - _Requirements: 7.2_

  - [ ]* 9.3 Write property test for write-then-read round trip (Property 4)
    - **Property 4: Write-then-read round trip**
    - Generate random row data with special characters, write via CsvSheet, read back via CsvExcelReadExecutor, assert value and position equality
    - **Validates: Requirements 6.1, 6.2, 6.3, 4.2**

  - [ ]* 9.4 Write unit tests for edge cases
    - Test BOM handling for UTF-8, UTF-16LE, UTF-16BE
    - Test truncated quoted field graceful handling
    - Test NONE_QUOTE mode disables quoting
    - Test deprecated CSVFormat bridge conversion
    - _Requirements: 3.5, 3.6, 5.5_

- [x] 10. Performance Optimization Layer - Read Path
  - [x] 10.1 Create `ArrayCellMap` class
    - Create `org.apache.fesod.sheet.metadata.csv.ArrayCellMap` implementing `Map<Integer, Cell>`
    - Back with `ReadCellData<?>[]` array for O(1) get/put via array index
    - Implement `reset(int newCapacity)` for cross-row reuse without reallocation
    - Implement `entrySet()`, `keySet()`, `values()`, `size()` as thin wrappers over the array
    - Only implement Map methods actually used by fesod's read pipeline (get, put, entrySet, size, isEmpty)
    - _Validates: Performance Optimization Layer — Read Path Optimization 1_

  - [x] 10.2 Add `ReadCellData` factory methods
    - Add `ReadCellData.ofString(int rowIndex, int columnIndex, String value)` static factory
    - Add `ReadCellData.ofEmpty(int rowIndex, int columnIndex)` static factory
    - These replace the multi-setter pattern (new + setRowIndex + setColumnIndex + setType + setStringValue)
    - _Validates: Performance Optimization Layer — Read Path Optimization 2_

  - [x] 10.3 Optimize `FesodRowProcessor.dealRecord()` to use `ArrayCellMap` and reuse `ReadRowHolder`
    - Replace `new LinkedHashMap<>(mapCapacity)` with reusable `ArrayCellMap` field
    - Call `arrayCellMap.reset(columnCount)` at start of each row instead of allocating new map
    - Replace multi-setter `ReadCellData` creation with `ReadCellData.ofString()` / `ReadCellData.ofEmpty()` factory calls
    - Add reusable `ReadRowHolder` field; update fields via setters instead of allocating new instance per row
    - Remove the `mapCapacity` calculation (`Math.max` + `/ 0.75f + 1`)
    - _Validates: Performance Optimization Layer — Read Path Optimizations 1, 2, 3, 4_

  - [x] 10.4 Tune uniVocity parser settings in `CsvFormatConfiguration`
    - Add `settings.setColumnReorderingEnabled(false)` in `createParserSettings()`
    - Add `settings.setInputBufferSize(2 * 1024 * 1024)` in `createParserSettings()` (2MB buffer)
    - Invalidate cached settings if these are changed (already handled by immutable config pattern)
    - _Validates: Performance Optimization Layer — Configuration Optimization_

- [x] 11. Performance Optimization Layer - Write Path
  - [x] 11.1 Optimize `CsvSheet.flushData()` with single-pass iteration and reusable buffer
    - Add `private String[] writeBuffer` field to `CsvSheet`
    - Replace double-iteration (sizeIterator + cellIterator) with single-pass iteration
    - Track `maxColumnIndex` while building values in the same loop
    - Reuse `writeBuffer` across rows; grow only when needed via `ensureWriteBuffer()`
    - Clear used portion with `Arrays.fill()` after each row write
    - _Validates: Performance Optimization Layer — Write Path Optimizations 1, 2_

- [x] 12. Checkpoint - Verify optimizations compile and all tests pass
  - Run `./mvnw clean install -Dmaven.test.skip=false` to verify compilation and test correctness
  - Ensure all existing `CsvFormatTest` and `CsvReadTest` tests pass without assertion changes
  - Verify no behavioral regressions in read/write round-trip

- [ ] 13. Benchmark validation
  - Re-run comprehensive benchmarks to measure improvement from fesod-layer optimizations
  - Compare against baseline (parser migration only) and original Commons CSV numbers
  - Target: +60-80% read throughput, +35-50% write throughput, 10-20% fewer GC events vs Commons CSV baseline
  - Document results and update design document projections if needed

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik with minimum 100 iterations per property
- The deprecated `CSVFormat` bridge (task 6.1) allows existing users to upgrade without immediate code changes
- Checkpoints at tasks 5, 8, 12, and 13 ensure incremental validation
- Tasks 10-13 address the fesod abstraction layer performance bottleneck identified via benchmark analysis
- The performance optimization tasks (10-13) do not change the public API or require requirements.md updates
