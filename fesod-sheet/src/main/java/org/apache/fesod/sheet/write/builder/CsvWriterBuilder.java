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

package org.apache.fesod.sheet.write.builder;

import java.util.Collection;
import java.util.function.Supplier;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.exception.ExcelGenerateException;
import org.apache.fesod.sheet.metadata.csv.CsvFormatConfiguration;
import org.apache.fesod.sheet.metadata.csv.CsvQuoteMode;
import org.apache.fesod.sheet.support.ExcelTypeEnum;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.WriteWorkbook;

/**
 * Builder for CSV file writing
 */
public class CsvWriterBuilder extends AbstractExcelWriterParameterBuilder<CsvWriterBuilder, WriteSheet> {
    private WriteWorkbook writeWorkbook;
    private CsvFormatConfiguration.Builder configBuilder;
    private WriteSheet writeSheet;

    private CsvWriterBuilder() {}

    public CsvWriterBuilder(WriteWorkbook writeWorkbook) {
        writeWorkbook.setExcelType(ExcelTypeEnum.CSV);
        this.writeWorkbook = writeWorkbook;
        this.writeSheet = new WriteSheet();
        this.configBuilder = CsvFormatConfiguration.builder();
    }

    /**
     * Sets the delimiter character
     *
     * @param delimiter the delimiter character
     * @return Returns a CsvWriterBuilder object, enabling method chaining
     */
    public CsvWriterBuilder delimiter(String delimiter) {
        if (delimiter != null) {
            this.configBuilder.delimiter(delimiter);
        }
        return this;
    }

    /**
     * Sets the quote character
     *
     * @param quote the quote character
     * @return Returns a CsvWriterBuilder object, enabling method chaining
     */
    public CsvWriterBuilder quote(Character quote) {
        return quote(quote, CsvQuoteMode.MINIMAL);
    }

    /**
     * Sets the quote character and the quoting behavior
     *
     * @param quote     the quote character
     * @param quoteMode defines the quoting behavior
     * @return Returns a CsvWriterBuilder object, enabling method chaining
     */
    public CsvWriterBuilder quote(Character quote, CsvQuoteMode quoteMode) {
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
     * @return Returns a CsvWriterBuilder object, enabling method chaining
     */
    public CsvWriterBuilder recordSeparator(String recordSeparator) {
        if (recordSeparator != null) {
            this.configBuilder.recordSeparator(recordSeparator);
        }
        return this;
    }

    /**
     * Sets the null string
     *
     * @param nullString the String to convert to and from {@code null}
     * @return Returns a CsvWriterBuilder object, enabling method chaining
     */
    public CsvWriterBuilder nullString(String nullString) {
        if (nullString != null) {
            this.configBuilder.nullString(nullString);
        }
        return this;
    }

    /**
     * Sets the escape character.
     *
     * @param escape the Character used to escape special characters in values
     * @return Returns a CsvWriterBuilder object, enabling method chaining
     */
    public CsvWriterBuilder escape(Character escape) {
        if (escape != null) {
            this.configBuilder.escapeCharacter(escape);
        }
        return this;
    }

    private ExcelWriter buildExcelWriter() {
        this.configBuilder.trim(this.writeWorkbook.getAutoTrim() == null
                || this.writeWorkbook.getAutoTrim()
                || Boolean.TRUE.equals(this.writeWorkbook.getAutoStrip()));
        if (this.writeWorkbook.getNeedHead() != null) {
            this.configBuilder.skipHeaderRecord(!this.writeWorkbook.getNeedHead());
        }
        this.writeWorkbook.setCsvFormatConfiguration(this.configBuilder.build());
        return new ExcelWriter(this.writeWorkbook);
    }

    public void doWrite(Collection<?> data) {
        if (writeWorkbook == null) {
            throw new ExcelGenerateException("Must use 'FesodSheet.write().csv()' to call this method");
        }
        ExcelWriter excelWriter = buildExcelWriter();
        excelWriter.write(data, this.writeSheet);
        excelWriter.finish();
    }

    public void doWrite(Supplier<Collection<?>> supplier) {
        doWrite(supplier.get());
    }

    @Override
    protected WriteSheet parameter() {
        return writeSheet;
    }
}
