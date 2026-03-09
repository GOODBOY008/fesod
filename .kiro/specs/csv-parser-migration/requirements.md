# Requirements Document

## Introduction

This document specifies the requirements for migrating the Apache Fesod CSV SDK from Apache Commons CSV to uniVocity-parsers. The migration targets improved performance and reduced memory usage for large CSV file processing while preserving the existing public API contract and behavioral compatibility. The migration also introduces an abstraction layer to decouple fesod's CSV logic from any specific third-party parser library.

## Glossary

- **CSV_Reader_Builder**: The `CsvReaderBuilder` class that provides the fluent API for configuring and executing CSV read operations (`FesodSheet.read().csv()`).
- **CSV_Writer_Builder**: The `CsvWriterBuilder` class that provides the fluent API for configuring and executing CSV write operations (`FesodSheet.write().csv()`).
- **CSV_Read_Executor**: The `CsvExcelReadExecutor` class responsible for parsing CSV input streams into fesod Cell objects via row-by-row iteration.
- **CSV_Write_Engine**: The `CsvSheet` class responsible for writing fesod Cell data to CSV output via `CSVPrinter` (currently) or equivalent uniVocity writer.
- **Read_Workbook_Holder**: The `CsvReadWorkbookHolder` class that holds parser format configuration and the active parser instance during a read operation.
- **Parser_Settings**: A fesod-owned configuration object that replaces direct use of `CSVFormat` in the public API, encapsulating delimiter, quote character, quote mode, escape character, record separator, null string representation, trim behavior, and header skip settings.
- **uniVocity_Parser**: The `com.univocity.parsers.csv.CsvParser` class from the uniVocity-parsers library used for reading CSV data.
- **uniVocity_Writer**: The `com.univocity.parsers.csv.CsvWriter` class from the uniVocity-parsers library used for writing CSV data.
- **BOM_Handling**: Byte Order Mark detection and handling for character-encoded CSV files (UTF-8, UTF-16, UTF-32).

## Requirements

### Requirement 1: Replace Apache Commons CSV Dependency with uniVocity-parsers

**User Story:** As a project maintainer, I want to replace the Apache Commons CSV dependency with uniVocity-parsers, so that the project benefits from improved parsing performance and lower memory consumption.

#### Acceptance Criteria

1. WHEN the project is built, THE build system SHALL resolve `com.univocity:univocity-parsers` as the CSV parsing dependency instead of `org.apache.commons:commons-csv`.
2. WHEN the project is built, THE build system SHALL contain no compile-time or runtime references to `org.apache.commons:commons-csv` classes.
3. WHEN the dependency is upgraded, THE build system SHALL manage the uniVocity-parsers version in the root `pom.xml` dependencyManagement section.

### Requirement 2: Introduce a Fesod-Owned CSV Configuration Abstraction

**User Story:** As a library consumer, I want CSV configuration to be expressed through fesod-owned types rather than third-party types, so that future parser library changes do not break my code.

#### Acceptance Criteria

1. THE Parser_Settings SHALL encapsulate all configurable CSV properties: delimiter, quote character, quote mode, escape character, record separator, null string, trim, and skip header record.
2. WHEN a user configures CSV reading via CSV_Reader_Builder, THE CSV_Reader_Builder SHALL accept Parser_Settings or individual setter methods without exposing uniVocity-parsers types in the public API.
3. WHEN a user configures CSV writing via CSV_Writer_Builder, THE CSV_Writer_Builder SHALL accept Parser_Settings or individual setter methods without exposing uniVocity-parsers types in the public API.
4. WHEN a Parser_Settings object is converted to uniVocity `CsvParserSettings` or `CsvWriterSettings`, THE conversion SHALL preserve all configured values faithfully.
5. WHEN no custom configuration is provided, THE Parser_Settings SHALL default to values equivalent to the current `CSVFormat.DEFAULT` behavior (comma delimiter, double-quote quoting, no escape character, CRLF record separator).

### Requirement 3: Migrate CSV Reading to uniVocity-parsers

**User Story:** As a developer using fesod to read CSV files, I want the reading engine to use uniVocity-parsers internally, so that I get faster parsing and lower memory usage on large files.

#### Acceptance Criteria

1. WHEN a CSV file is read, THE CSV_Read_Executor SHALL use uniVocity_Parser to parse the input stream row by row.
2. WHEN a CSV record is parsed, THE CSV_Read_Executor SHALL convert each field into a fesod `ReadCellData` object with correct row index, column index, data type, and string value.
3. WHEN the auto-trim setting is enabled, THE CSV_Read_Executor SHALL trim whitespace from parsed field values.
4. WHEN the auto-strip setting is enabled, THE CSV_Read_Executor SHALL strip leading and trailing whitespace (including Unicode whitespace) from parsed field values.
5. WHEN a CSV file contains a Byte Order Mark, THE CSV_Read_Executor SHALL detect and handle the BOM correctly for UTF-8, UTF-16, and UTF-32 encodings.
6. IF a CSV file contains a truncated quoted field or unexpected EOF within a quoted field, THEN THE CSV_Read_Executor SHALL finish reading gracefully and log a warning instead of throwing an exception.
7. WHEN a CSV file is read with a custom charset, THE CSV_Read_Executor SHALL decode the input stream using the specified charset.

### Requirement 4: Migrate CSV Writing to uniVocity-parsers

**User Story:** As a developer using fesod to write CSV files, I want the writing engine to use uniVocity-parsers internally, so that I get consistent performance improvements for both read and write paths.

#### Acceptance Criteria

1. WHEN CSV data is written, THE CSV_Write_Engine SHALL use uniVocity_Writer to emit CSV-formatted output to the provided `Appendable`.
2. WHEN a row of cells is written, THE CSV_Write_Engine SHALL correctly handle null cells by emitting empty fields at the appropriate column positions.
3. WHEN the BOM setting is enabled, THE CSV_Write_Engine SHALL prepend the appropriate Byte Order Mark to the output before writing any CSV data.
4. WHEN a row cache reaches its configured threshold, THE CSV_Write_Engine SHALL flush the cached rows to the uniVocity_Writer.
5. WHEN the sheet is closed, THE CSV_Write_Engine SHALL flush all remaining cached rows and close the uniVocity_Writer.

### Requirement 5: Preserve Public API Compatibility

**User Story:** As an existing fesod user, I want the migration to not break my existing code, so that I can upgrade without modifying my application.

#### Acceptance Criteria

1. THE CSV_Reader_Builder SHALL continue to expose the same fluent methods: `delimiter()`, `quote()`, `escape()`, `recordSeparator()`, `nullString()`, `trim()`, and `skipHeader()`.
2. THE CSV_Writer_Builder SHALL continue to expose the same fluent methods: `delimiter()`, `quote()`, `escape()`, `recordSeparator()`, `nullString()`, `trim()`, and `skipHeader()`.
3. WHEN a CSV file is read with default settings, THE CSV_Read_Executor SHALL produce identical `ReadCellData` output as the previous Apache Commons CSV implementation for the same input.
4. WHEN a CSV file is written with default settings, THE CSV_Write_Engine SHALL produce byte-identical output as the previous Apache Commons CSV implementation for the same input data.
5. IF a user passes a `CSVFormat` object via the deprecated compatibility path, THEN THE system SHALL convert the `CSVFormat` to Parser_Settings and log a deprecation warning.

### Requirement 6: Maintain Read/Write Round-Trip Fidelity

**User Story:** As a developer, I want data written by fesod's CSV writer to be readable by fesod's CSV reader without data loss, so that I can trust the library for data interchange.

#### Acceptance Criteria

1. WHEN data is written to CSV and then read back using the same Parser_Settings, THE system SHALL produce cell values identical to the original input data.
2. WHEN data containing special characters (delimiters, quotes, newlines, null strings) is written and read back, THE system SHALL preserve the original values without corruption.
3. WHEN data containing empty fields and null values is written and read back, THE system SHALL distinguish between empty strings and null values according to the configured null string setting.

### Requirement 7: Update Existing Tests to Validate Migration

**User Story:** As a project maintainer, I want all existing CSV tests to pass against the new uniVocity-parsers implementation, so that I can verify behavioral equivalence.

#### Acceptance Criteria

1. WHEN the existing `CsvFormatTest` test suite is executed, THE test suite SHALL pass with the uniVocity-parsers implementation without modifying test assertions.
2. WHEN the existing `CsvReadTest` example tests are executed, THE test suite SHALL pass with the uniVocity-parsers implementation without modifying test assertions.
3. WHEN new property-based tests are executed, THE test suite SHALL validate round-trip fidelity and configuration mapping correctness across randomized inputs.
