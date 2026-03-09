Sure. Here's the full picture of what was broken and why, for future reference.

There were two test failures, both rooted in how the CSV parser migration from Apache Commons CSV to uniVocity-parsers handled trimming and null strings.

The uniVocity trim default trap
uniVocity's CsvParserSettings.trimValues defaults to true, and its trim uses char <= ' ' (same as String.trim()), which strips control characters like \u0000. Apache Commons CSV did not trim by default. This behavioral difference was the root of both bugs.

Failure 1: AutoStripParameterTest.testCSV
The trimming architecture has two layers:

Parser-level trim: uniVocity's trimValues setting, applied before values reach application code
Application-level trim: CsvExcelReadExecutor.dealRecord(), which checks autoTrim / autoStrip from GlobalConfiguration
CsvReaderBuilder.buildExcelReader() was setting trim=true on the CsvFormatConfiguration whenever autoTrim was null (the default) or true. This passed through to toParserSettings() → settings.trimValues(true), causing the uniVocity parser to strip whitespace before dealRecord ever saw the values.

On the write side, CsvFormatConfiguration.toWriterSettings() never called settings.trimValues(...) at all, so uniVocity's writer also defaulted to trimValues(true), stripping spaces during writing too.

The result: even with autoTrim=false, values like " String Data1 " were trimmed to "String Data1" at both the write and read parser level, making the application-level trim/strip logic in dealRecord completely ineffective.

Fix:

CsvReaderBuilder.buildExcelReader(): always set trim(false) — let dealRecord handle trimming
CsvFormatConfiguration.toWriterSettings(): explicitly set settings.trimValues(false) — the writer should preserve values as-is
Failure 2: CsvFormatTest.testNullString
This one involved a misunderstanding of uniVocity's setNullValue semantics, which work opposite to what the code assumed:

On the writer: setNullValue("X") means "when writing a null array element, emit X" — this works as expected
On the parser: setNullValue("X") means "when the parser encounters an empty/null field, return X instead of null" — it does NOT mean "when you see X, return null"
So the round-trip was broken by design. The writer correctly wrote \u0000 for null Date fields, but the parser returned \u0000 as a literal string value, not as null.

Then in dealRecord, StringUtils.isNotBlank("\u0000") returns true (because Character.isWhitespace('\u0000') is false in Java), so the cell was classified as STRING type with value \u0000. This reached DateStringConverter → DateUtils.switchDateFormat("") (after the string was processed), which threw IllegalArgumentException because no date format matched.

Before the first fix, parser-level trimValues(true) would have trimmed \u0000 to "" (since \u0000 <= ' '), which would have made isNotBlank return false and accidentally masked the problem for some nullString values. But \\N (SQL_NULL_STRING) would not be trimmed, so it would still fail.

Fix: in dealRecord, explicitly check if cellString equals the configured nullString before the blank check, and set it to null if it matches. This correctly handles the nullString round-trip at the application level, independent of uniVocity's semantics.

Summary of changes
File	Change
CsvReaderBuilder.buildExcelReader()	Always set trim(false) instead of deriving from autoTrim/autoStrip
CsvFormatConfiguration.toWriterSettings()	Added settings.trimValues(false)
CsvExcelReadExecutor.dealRecord()	Added nullString equality check before the isNotBlank gate
The principle: uniVocity is a raw I/O layer that should pass values through unchanged. All semantic processing (trimming, stripping, null-string recognition) belongs in the application layer (dealRecord), where it can respect the autoTrim, autoStrip, and nullString configurations independently.