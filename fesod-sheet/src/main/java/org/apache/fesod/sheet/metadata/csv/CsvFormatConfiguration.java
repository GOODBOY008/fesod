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

package org.apache.fesod.sheet.metadata.csv;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriterSettings;

/**
 * Fesod-owned immutable CSV format configuration, replacing direct use of
 * {@code org.apache.commons.csv.CSVFormat} in the public API.
 *
 * <p>Use {@link #builder()} to construct instances. Convert to uniVocity settings
 * via {@link #toParserSettings()} and {@link #toWriterSettings()}.</p>
 */
public class CsvFormatConfiguration {

    /** Defaults matching {@code CSVFormat.DEFAULT}. */
    private static final String DEFAULT_DELIMITER = ",";
    private static final Character DEFAULT_QUOTE_CHARACTER = '"';
    private static final CsvQuoteMode DEFAULT_QUOTE_MODE = CsvQuoteMode.MINIMAL;
    private static final String DEFAULT_RECORD_SEPARATOR = "\r\n";

    private final String delimiter;
    private final Character quoteCharacter;
    private final CsvQuoteMode quoteMode;
    private final Character escapeCharacter;
    private final String recordSeparator;
    private final String nullString;
    private final boolean trim;
    private final boolean skipHeaderRecord;
    private final boolean ignoreEmptyLines;

    private CsvFormatConfiguration(Builder builder) {
        this.delimiter = builder.delimiter;
        this.quoteCharacter = builder.quoteCharacter;
        this.quoteMode = builder.quoteMode;
        this.escapeCharacter = builder.escapeCharacter;
        this.recordSeparator = builder.recordSeparator;
        this.nullString = builder.nullString;
        this.trim = builder.trim;
        this.skipHeaderRecord = builder.skipHeaderRecord;
        this.ignoreEmptyLines = builder.ignoreEmptyLines;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ---- Getters ----

    public String getDelimiter() {
        return delimiter;
    }

    public Character getQuoteCharacter() {
        return quoteCharacter;
    }

    public CsvQuoteMode getQuoteMode() {
        return quoteMode;
    }

    public Character getEscapeCharacter() {
        return escapeCharacter;
    }

    public String getRecordSeparator() {
        return recordSeparator;
    }

    public String getNullString() {
        return nullString;
    }

    public boolean isTrim() {
        return trim;
    }

    public boolean isSkipHeaderRecord() {
        return skipHeaderRecord;
    }

    public boolean isIgnoreEmptyLines() {
        return ignoreEmptyLines;
    }

    // ---- Conversion to uniVocity settings ----

    /**
     * Converts this configuration to uniVocity {@link CsvParserSettings}.
     *
     * @return parser settings reflecting this configuration
     */
    public CsvParserSettings toParserSettings() {
        CsvParserSettings settings = new CsvParserSettings();
        CsvFormat format = settings.getFormat();

        // Delimiter — use first char of the delimiter string
        if (delimiter != null && !delimiter.isEmpty()) {
            format.setDelimiter(delimiter.charAt(0));
        }

        // Quote character
        if (quoteCharacter != null) {
            format.setQuote(quoteCharacter);
            // NONE_QUOTE disables quoting
            if (quoteCharacter == CsvConstant.NONE_QUOTE) {
                format.setQuoteEscape(CsvConstant.NONE_QUOTE);
            }
        }

        // Escape character
        if (escapeCharacter != null) {
            format.setQuoteEscape(escapeCharacter);
        }

        // Record separator — only set explicitly if the user specified a non-default value.
        // uniVocity auto-detects line separators (\n, \r\n, \r) by default, which is the
        // desired behavior for reading. Setting it explicitly disables auto-detection.
        if (recordSeparator != null && !DEFAULT_RECORD_SEPARATOR.equals(recordSeparator)) {
            format.setLineSeparator(recordSeparator);
        } else {
            settings.setLineSeparatorDetectionEnabled(true);
        }

        // Null string
        if (nullString != null) {
            settings.setNullValue(nullString);
        }

        // Trim
        settings.trimValues(trim);

        // Skip header record
        settings.setHeaderExtractionEnabled(skipHeaderRecord);

        // Ignore empty lines
        settings.setSkipEmptyLines(ignoreEmptyLines);

        // Do not impose a column limit — let the parser handle any width
        settings.setMaxColumns(8192);

        return settings;
    }

    /**
     * Converts this configuration to uniVocity {@link CsvWriterSettings}.
     *
     * @return writer settings reflecting this configuration
     */
    public CsvWriterSettings toWriterSettings() {
        CsvWriterSettings settings = new CsvWriterSettings();
        CsvFormat format = settings.getFormat();

        // Delimiter
        if (delimiter != null && !delimiter.isEmpty()) {
            format.setDelimiter(delimiter.charAt(0));
        }

        // Quote character
        if (quoteCharacter != null) {
            format.setQuote(quoteCharacter);
            if (quoteCharacter == CsvConstant.NONE_QUOTE) {
                format.setQuoteEscape(CsvConstant.NONE_QUOTE);
            }
        }

        // Escape character
        if (escapeCharacter != null) {
            format.setQuoteEscape(escapeCharacter);
        }

        // Record separator
        if (recordSeparator != null) {
            format.setLineSeparator(recordSeparator);
        }

        // Null string
        if (nullString != null) {
            settings.setNullValue(nullString);
        }

        // Prevent uniVocity from trimming values during writing — the application
        // layer handles trimming so the writer must preserve values as-is.
        settings.trimValues(false);

        // Quote mode mapping
        if (quoteMode != null) {
            switch (quoteMode) {
                case ALL:
                    settings.setQuoteAllFields(true);
                    break;
                case ALL_NON_NULL:
                    settings.setQuoteAllFields(true);
                    break;
                case MINIMAL:
                    settings.setQuoteAllFields(false);
                    break;
                case NON_NUMERIC:
                    settings.setQuoteAllFields(false);
                    break;
                case NONE:
                    settings.setQuoteAllFields(false);
                    // Disable quoting entirely
                    format.setQuote(CsvConstant.NONE_QUOTE);
                    format.setQuoteEscape(CsvConstant.NONE_QUOTE);
                    break;
                default:
                    break;
            }
        }

        return settings;
    }

    // ---- Builder ----

    /**
     * Fluent builder for {@link CsvFormatConfiguration}.
     */
    public static class Builder {

        private String delimiter = DEFAULT_DELIMITER;
        private Character quoteCharacter = DEFAULT_QUOTE_CHARACTER;
        private CsvQuoteMode quoteMode = DEFAULT_QUOTE_MODE;
        private Character escapeCharacter = null;
        private String recordSeparator = DEFAULT_RECORD_SEPARATOR;
        private String nullString = null;
        private boolean trim = false;
        private boolean skipHeaderRecord = false;
        private boolean ignoreEmptyLines = false;

        private Builder() {
        }

        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder delimiter(char delimiter) {
            this.delimiter = String.valueOf(delimiter);
            return this;
        }

        public Builder quoteCharacter(Character quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        public Builder quoteMode(CsvQuoteMode quoteMode) {
            this.quoteMode = quoteMode;
            return this;
        }

        public Builder escapeCharacter(Character escapeCharacter) {
            this.escapeCharacter = escapeCharacter;
            return this;
        }

        public Builder recordSeparator(String recordSeparator) {
            this.recordSeparator = recordSeparator;
            return this;
        }

        public Builder nullString(String nullString) {
            this.nullString = nullString;
            return this;
        }

        public Builder trim(boolean trim) {
            this.trim = trim;
            return this;
        }

        public Builder skipHeaderRecord(boolean skipHeaderRecord) {
            this.skipHeaderRecord = skipHeaderRecord;
            return this;
        }

        public Builder ignoreEmptyLines(boolean ignoreEmptyLines) {
            this.ignoreEmptyLines = ignoreEmptyLines;
            return this;
        }

        /**
         * Builds an immutable {@link CsvFormatConfiguration}.
         *
         * @return the configuration
         * @throws IllegalArgumentException if delimiter is null or empty
         */
        public CsvFormatConfiguration build() {
            if (delimiter == null || delimiter.isEmpty()) {
                throw new IllegalArgumentException("Delimiter must not be null or empty");
            }
            return new CsvFormatConfiguration(this);
        }
    }
}
