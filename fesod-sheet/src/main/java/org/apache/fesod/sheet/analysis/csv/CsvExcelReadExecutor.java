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

/*
 * This file is part of the Apache Fesod (Incubating) project, which was derived from Alibaba EasyExcel.
 *
 * Copyright (C) 2018-2024 Alibaba Group Holding Ltd.
 */

package org.apache.fesod.sheet.analysis.csv;

import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.fesod.sheet.analysis.ExcelReadExecutor;
import org.apache.fesod.sheet.context.csv.CsvReadContext;
import org.apache.fesod.sheet.enums.ByteOrderMarkEnum;
import org.apache.fesod.sheet.exception.ExcelAnalysisException;
import org.apache.fesod.sheet.exception.ExcelAnalysisStopSheetException;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.csv.CsvFormatConfiguration;
import org.apache.fesod.sheet.metadata.csv.FesodRowProcessor;
import org.apache.fesod.sheet.read.metadata.ReadSheet;
import org.apache.fesod.sheet.read.metadata.holder.csv.CsvReadWorkbookHolder;
import org.apache.fesod.sheet.util.SheetUtils;

/**
 * CSV Excel Read Executor, responsible for reading and processing CSV files.
 * Uses uniVocity-parsers with parallel I/O and settings caching for high performance.
 */
@Slf4j
public class CsvExcelReadExecutor implements ExcelReadExecutor {

    private final List<ReadSheet> sheetList;
    private final CsvReadContext csvReadContext;

    public CsvExcelReadExecutor(CsvReadContext csvReadContext) {
        this.csvReadContext = csvReadContext;
        sheetList = new ArrayList<>();
        ReadSheet readSheet = new ReadSheet();
        sheetList.add(readSheet);
        readSheet.setSheetNo(0);
    }

    @Override
    public List<ReadSheet> sheetList() {
        return sheetList;
    }

    @Override
    public void execute() {
        CsvReadWorkbookHolder workbookHolder = csvReadContext.csvReadWorkbookHolder();
        GlobalConfiguration globalConfig = workbookHolder.getGlobalConfiguration();
        boolean autoTrim = Boolean.TRUE.equals(globalConfig.getAutoTrim());
        boolean autoStrip = Boolean.TRUE.equals(globalConfig.getAutoStrip());
        String nullString = workbookHolder.getCsvFormatConfiguration().getNullString();

        for (ReadSheet readSheet : sheetList) {
            readSheet = SheetUtils.match(readSheet, csvReadContext);
            if (readSheet == null) {
                continue;
            }
            csvReadContext.currentSheet(readSheet);
            executeSheet(autoTrim, autoStrip, nullString, globalConfig);
            csvReadContext.analysisEventProcessor().endSheet(csvReadContext);
        }
    }

    private void executeSheet(
            boolean autoTrim, boolean autoStrip, String nullString, GlobalConfiguration globalConfig) {
        CsvReadWorkbookHolder csvReadWorkbookHolder = csvReadContext.csvReadWorkbookHolder();
        CsvFormatConfiguration csvFormatConfiguration = csvReadWorkbookHolder.getCsvFormatConfiguration();
        CsvParserSettings parserSettings = csvFormatConfiguration.toParserSettings();

        // Create RowProcessor for zero-overhead streaming
        FesodRowProcessor rowProcessor =
                new FesodRowProcessor(csvReadContext, globalConfig, autoTrim, autoStrip, nullString);

        // Set RowProcessor for zero-overhead streaming
        parserSettings.setProcessor(rowProcessor);

        ByteOrderMarkEnum byteOrderMark = ByteOrderMarkEnum.valueOfByCharsetName(
                csvReadWorkbookHolder.getCharset().name());

        try (InputStream inputStream = openInputStream();
                Reader reader = buildReader(inputStream, byteOrderMark)) {

            CsvParser csvParser = new CsvParser(parserSettings);
            csvReadContext.csvReadWorkbookHolder().setCsvParser(csvParser);

            // Parse all rows - RowProcessor callbacks handle each row
            csvParser.parseAll(reader);

        } catch (ExcelAnalysisStopSheetException e) {
            if (log.isDebugEnabled()) {
                log.debug("Custom stop!", e);
            }
        } catch (TextParsingException e) {
            if (isBenignCsvParseException(e)) {
                if (log.isDebugEnabled()) {
                    log.debug("CSV parse finished early due to benign parse error: {}", e.getMessage());
                } else if (log.isWarnEnabled()) {
                    log.warn("CSV parse finished early due to benign parse error.");
                }
            } else {
                throw new ExcelAnalysisException(e);
            }
        } catch (IOException e) {
            throw new ExcelAnalysisException(e);
        }
    }

    private InputStream openInputStream() throws IOException {
        CsvReadWorkbookHolder csvReadWorkbookHolder = csvReadContext.csvReadWorkbookHolder();
        if (csvReadWorkbookHolder.getMandatoryUseInputStream()) {
            return csvReadWorkbookHolder.getInputStream();
        } else if (csvReadWorkbookHolder.getFile() != null) {
            return Files.newInputStream(csvReadWorkbookHolder.getFile().toPath());
        } else {
            return csvReadWorkbookHolder.getInputStream();
        }
    }

    private Reader buildReader(InputStream inputStream, ByteOrderMarkEnum byteOrderMark) {
        if (byteOrderMark == null) {
            return new InputStreamReader(
                    inputStream, csvReadContext.csvReadWorkbookHolder().getCharset());
        }
        return new InputStreamReader(
                new BOMInputStream(inputStream, byteOrderMark.getByteOrderMark()),
                csvReadContext.csvReadWorkbookHolder().getCharset());
    }

    private static boolean isBenignCsvParseException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                if (msg.contains("Unexpected end of input")
                        || msg.contains("end of input")
                        || msg.contains("Unescaped quote character")
                        || msg.contains("not enclosed in quotes")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}
