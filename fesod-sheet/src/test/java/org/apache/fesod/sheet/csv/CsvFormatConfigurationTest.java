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

package org.apache.fesod.sheet.csv;

import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriterSettings;
import org.apache.fesod.sheet.metadata.csv.CsvConstant;
import org.apache.fesod.sheet.metadata.csv.CsvFormatConfiguration;
import org.apache.fesod.sheet.metadata.csv.CsvQuoteMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CsvFormatConfiguration}.
 */
public class CsvFormatConfigurationTest {

    @Test
    void defaultsShouldMatchCsvFormatDefault() {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder().build();

        Assertions.assertEquals(",", config.getDelimiter());
        Assertions.assertEquals(Character.valueOf('"'), config.getQuoteCharacter());
        Assertions.assertEquals(CsvQuoteMode.MINIMAL, config.getQuoteMode());
        Assertions.assertNull(config.getEscapeCharacter());
        Assertions.assertEquals("\r\n", config.getRecordSeparator());
        Assertions.assertNull(config.getNullString());
        Assertions.assertFalse(config.isTrim());
        Assertions.assertFalse(config.isSkipHeaderRecord());
        Assertions.assertFalse(config.isIgnoreEmptyLines());
    }

    @Test
    void builderShouldSetAllFields() {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder()
                .delimiter(";")
                .quoteCharacter('\'')
                .quoteMode(CsvQuoteMode.ALL)
                .escapeCharacter('\\')
                .recordSeparator("\n")
                .nullString("NULL")
                .trim(true)
                .skipHeaderRecord(true)
                .ignoreEmptyLines(true)
                .build();

        Assertions.assertEquals(";", config.getDelimiter());
        Assertions.assertEquals(Character.valueOf('\''), config.getQuoteCharacter());
        Assertions.assertEquals(CsvQuoteMode.ALL, config.getQuoteMode());
        Assertions.assertEquals(Character.valueOf('\\'), config.getEscapeCharacter());
        Assertions.assertEquals("\n", config.getRecordSeparator());
        Assertions.assertEquals("NULL", config.getNullString());
        Assertions.assertTrue(config.isTrim());
        Assertions.assertTrue(config.isSkipHeaderRecord());
        Assertions.assertTrue(config.isIgnoreEmptyLines());
    }

    @Test
    void builderCharDelimiterOverload() {
        CsvFormatConfiguration config =
                CsvFormatConfiguration.builder().delimiter('\t').build();

        Assertions.assertEquals("\t", config.getDelimiter());
    }

    @Test
    void buildShouldRejectNullDelimiter() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> CsvFormatConfiguration.builder().delimiter((String) null).build());
    }

    @Test
    void buildShouldRejectEmptyDelimiter() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> CsvFormatConfiguration.builder().delimiter("").build());
    }

    @Test
    void toParserSettingsShouldMapDefaults() {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder().build();
        CsvParserSettings settings = config.toParserSettings();

        Assertions.assertEquals(',', settings.getFormat().getDelimiter());
        Assertions.assertEquals('"', settings.getFormat().getQuote());
        /*Assertions.assertEquals("\r\n", settings.getFormat().getLineSeparatorString());*/
        Assertions.assertFalse(settings.isHeaderExtractionEnabled());
        Assertions.assertFalse(settings.getSkipEmptyLines());
    }

    @Test
    void toParserSettingsShouldMapCustomValues() {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder()
                .delimiter("|")
                .quoteCharacter('\'')
                .escapeCharacter('\\')
                .recordSeparator("\n")
                .nullString("N/A")
                .trim(true)
                .skipHeaderRecord(true)
                .ignoreEmptyLines(true)
                .build();

        CsvParserSettings settings = config.toParserSettings();

        Assertions.assertEquals('|', settings.getFormat().getDelimiter());
        Assertions.assertEquals('\'', settings.getFormat().getQuote());
        Assertions.assertEquals('\\', settings.getFormat().getQuoteEscape());
        Assertions.assertEquals("\n", settings.getFormat().getLineSeparatorString());
        Assertions.assertEquals("N/A", settings.getNullValue());
        Assertions.assertTrue(settings.isHeaderExtractionEnabled());
        Assertions.assertTrue(settings.getSkipEmptyLines());
    }

    @Test
    void toParserSettingsNoneQuoteShouldDisableQuoting() {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder()
                .quoteCharacter(CsvConstant.NONE_QUOTE)
                .build();

        CsvParserSettings settings = config.toParserSettings();

        Assertions.assertEquals(CsvConstant.NONE_QUOTE, settings.getFormat().getQuote());
        Assertions.assertEquals(CsvConstant.NONE_QUOTE, settings.getFormat().getQuoteEscape());
    }

    @Test
    void toWriterSettingsShouldMapDefaults() {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder().build();
        CsvWriterSettings settings = config.toWriterSettings();

        Assertions.assertEquals(',', settings.getFormat().getDelimiter());
        Assertions.assertEquals('"', settings.getFormat().getQuote());
        Assertions.assertEquals("\r\n", settings.getFormat().getLineSeparatorString());
        Assertions.assertFalse(settings.getQuoteAllFields());
    }

    @Test
    void toWriterSettingsQuoteModeAll() {
        CsvFormatConfiguration config =
                CsvFormatConfiguration.builder().quoteMode(CsvQuoteMode.ALL).build();

        CsvWriterSettings settings = config.toWriterSettings();
        Assertions.assertTrue(settings.getQuoteAllFields());
    }

    @Test
    void toWriterSettingsQuoteModeNone() {
        CsvFormatConfiguration config =
                CsvFormatConfiguration.builder().quoteMode(CsvQuoteMode.NONE).build();

        CsvWriterSettings settings = config.toWriterSettings();
        Assertions.assertFalse(settings.getQuoteAllFields());
        Assertions.assertEquals(CsvConstant.NONE_QUOTE, settings.getFormat().getQuote());
        Assertions.assertEquals(CsvConstant.NONE_QUOTE, settings.getFormat().getQuoteEscape());
    }

    @Test
    void toWriterSettingsShouldMapCustomValues() {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder()
                .delimiter("\t")
                .quoteCharacter('\'')
                .escapeCharacter('\\')
                .recordSeparator("\n")
                .nullString("NULL")
                .quoteMode(CsvQuoteMode.ALL_NON_NULL)
                .build();

        CsvWriterSettings settings = config.toWriterSettings();

        Assertions.assertEquals('\t', settings.getFormat().getDelimiter());
        Assertions.assertEquals('\'', settings.getFormat().getQuote());
        Assertions.assertEquals('\\', settings.getFormat().getQuoteEscape());
        Assertions.assertEquals("\n", settings.getFormat().getLineSeparatorString());
        Assertions.assertEquals("NULL", settings.getNullValue());
        Assertions.assertTrue(settings.getQuoteAllFields());
    }
}
