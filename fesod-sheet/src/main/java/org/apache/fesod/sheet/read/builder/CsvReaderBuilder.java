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

package org.apache.fesod.sheet.read.builder;

import java.util.List;
import org.apache.fesod.sheet.ExcelReader;
import org.apache.fesod.sheet.event.SyncReadListener;
import org.apache.fesod.sheet.exception.ExcelGenerateException;
import org.apache.fesod.sheet.metadata.csv.CsvFormatConfiguration;
import org.apache.fesod.sheet.metadata.csv.CsvQuoteMode;
import org.apache.fesod.sheet.read.metadata.ReadSheet;
import org.apache.fesod.sheet.read.metadata.ReadWorkbook;
import org.apache.fesod.sheet.support.ExcelTypeEnum;

/**
 * Builder for CSV file reading
 */
public class CsvReaderBuilder extends AbstractExcelReaderParameterBuilder<CsvReaderBuilder, ReadSheet> {
    private ReadWorkbook readWorkbook;
    private ReadSheet readSheet;
    private CsvFormatConfiguration.Builder configBuilder;

    private CsvReaderBuilder() {}

    public CsvReaderBuilder(ReadWorkbook readWorkbook) {
        readWorkbook.setExcelType(ExcelTypeEnum.CSV);
        this.readWorkbook = readWorkbook;
        this.readSheet = new ReadSheet();
        this.configBuilder = CsvFormatConfiguration.builder();
    }

    /**
     * Sets the delimiter character
     *
     * @param delimiter the delimiter character
     * @return Returns a CsvReaderBuilder object, enabling method chaining
     */
    public CsvReaderBuilder delimiter(String delimiter) {
        if (delimiter != null) {
            this.configBuilder.delimiter(delimiter);
        }
        return this;
    }

    /**
     * Sets the quote character
     * <p>
     * If set to {@link org.apache.fesod.sheet.metadata.csv.CsvConstant#NONE_QUOTE}, the quote parsing logic will be disabled,
     * and quote characters will be treated as regular text.This is equivalent to setting
     * {@code quote} to {@code null} in Apache Commons CSV.
     * </p>
     *
     * @param quote the quote character
     * @return Returns a CsvReaderBuilder object, enabling method chaining
     */
    public CsvReaderBuilder quote(Character quote) {
        return quote(quote, CsvQuoteMode.MINIMAL);
    }

    /**
     * Sets the quote character and the quoting behavior
     *
     * @param quote     the quote character
     * @param quoteMode defines the quoting behavior
     * @return Returns a CsvReaderBuilder object, enabling method chaining
     */
    public CsvReaderBuilder quote(Character quote, CsvQuoteMode quoteMode) {
        if (quote != null) {
            this.configBuilder.quoteCharacter(quote);
        }
        if (quoteMode != null) {
            this.configBuilder.quoteMode(quoteMode);
        }
        return this;
    }

    /**
     * Sets the line separator
     *
     * @param recordSeparator the line separator
     * @return Returns a CsvReaderBuilder object, enabling method chaining
     */
    public CsvReaderBuilder recordSeparator(String recordSeparator) {
        if (recordSeparator != null) {
            this.configBuilder.recordSeparator(recordSeparator);
        }
        return this;
    }

    /**
     * Sets the null string
     *
     * @param nullString the String to convert to and from {@code null}
     * @return Returns a CsvReaderBuilder object, enabling method chaining
     */
    public CsvReaderBuilder nullString(String nullString) {
        if (nullString != null) {
            this.configBuilder.nullString(nullString);
        }
        return this;
    }

    /**
     * Sets the escape character.
     *
     * @param escape the Character used to escape special characters in values
     * @return Returns a CsvReaderBuilder object, enabling method chaining
     */
    public CsvReaderBuilder escape(Character escape) {
        if (escape != null) {
            this.configBuilder.escapeCharacter(escape);
        }
        return this;
    }

    private ExcelReader buildExcelReader() {
        // Never delegate trimming to the uniVocity parser.  Trimming/stripping is
        // handled by CsvExcelReadExecutor.dealRecord() which respects autoTrim and
        // autoStrip independently.  Parser-level trimming interferes with nullString
        // round-tripping (e.g. \u0000 is trimmed away before the nullValue check).
        this.configBuilder.trim(false);
        if (this.readWorkbook.getIgnoreEmptyRow() != null) {
            this.configBuilder.ignoreEmptyLines(this.readWorkbook.getIgnoreEmptyRow());
        }
        this.readWorkbook.setCsvFormatConfiguration(this.configBuilder.build());
        return new ExcelReader(this.readWorkbook);
    }

    public void doRead() {
        if (this.readWorkbook == null) {
            throw new ExcelGenerateException("Must use 'FesodSheet.read().csv()' to call this method");
        }
        ExcelReader excelReader = buildExcelReader();
        excelReader.read(this.readSheet);
        excelReader.finish();
    }

    /**
     * synchronous read and returns the results
     *
     * @return Returns a list containing the read data
     */
    public <T> List<T> doReadSync() {
        if (this.readWorkbook == null) {
            throw new ExcelGenerateException("Must use 'FesodSheet.read().csv()' to call this method");
        }
        ExcelReader excelReader = buildExcelReader();
        // Register a synchronous read listener
        SyncReadListener syncReadListener = new SyncReadListener();
        registerReadListener(syncReadListener);
        excelReader.read(this.readSheet);
        excelReader.finish();
        return (List<T>) syncReadListener.getList();
    }

    @Override
    protected ReadSheet parameter() {
        return this.readSheet;
    }
}
