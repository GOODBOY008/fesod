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

package org.apache.fesod.sheet.analysis.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.fesod.common.util.StringUtils;
import org.apache.fesod.sheet.analysis.ExcelReadExecutor;
import org.apache.fesod.sheet.context.csv.CsvReadContext;
import org.apache.fesod.sheet.enums.ByteOrderMarkEnum;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.enums.RowTypeEnum;
import org.apache.fesod.sheet.exception.ExcelAnalysisException;
import org.apache.fesod.sheet.exception.ExcelAnalysisStopSheetException;
import org.apache.fesod.sheet.metadata.Cell;
import org.apache.fesod.sheet.metadata.csv.CsvFormatConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.read.metadata.ReadSheet;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.read.metadata.holder.ReadRowHolder;
import org.apache.fesod.sheet.read.metadata.holder.csv.CsvReadWorkbookHolder;
import org.apache.fesod.sheet.util.SheetUtils;

/**
 * CSV Excel Read Executor, responsible for reading and processing CSV files.
 * Uses uniVocity-parsers for CSV parsing.
 */
@Slf4j
public class CsvExcelReadExecutor implements ExcelReadExecutor {

    // List of sheets to be read
    private final List<ReadSheet> sheetList;
    // Context for CSV reading operation
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

    /**
     * Overrides the execute method to parse and process CSV files.
     * This method first attempts to create a uniVocity CSV parser, then iterates through each sheet,
     * and processes each record in the CSV file using {@code parseNext()}.
     */
    @Override
    public void execute() {
        CsvParser csvParser;
        CsvReadWorkbookHolder workbookHolder = csvReadContext.csvReadWorkbookHolder();
        try {
            // Create a uniVocity CSV parser instance
            csvParser = csvParser();
            // Store the CSV parser instance in the context for subsequent processing
            workbookHolder.setCsvParser(csvParser);
        } catch (IOException e) {
            throw new ExcelAnalysisException(e);
        }

        // Hoist per-row config lookups out of the hot loop — these values never change
        // during a single read operation, so resolving them once avoids repeated holder
        // chain traversals on every row.
        GlobalConfiguration globalConfig = workbookHolder.getGlobalConfiguration();
        boolean autoTrim = Boolean.TRUE.equals(globalConfig.getAutoTrim());
        boolean autoStrip = Boolean.TRUE.equals(globalConfig.getAutoStrip());
        String nullString = workbookHolder.getCsvFormatConfiguration().getNullString();

        // Track column count across rows to pre-size the cellMap
        int lastColumnCount = 0;

        // Iterate through each sheet in the sheet list
        for (ReadSheet readSheet : sheetList) {
            // Match and update the readSheet object
            readSheet = SheetUtils.match(readSheet, csvReadContext);
            // If the match result is null, skip the current sheet
            if (readSheet == null) {
                continue;
            }
            try {
                // Set the current sheet being processed in the context
                csvReadContext.currentSheet(readSheet);

                // Initialize the row index
                int rowIndex = 0;

                // Use uniVocity's parseNext() loop to iterate row by row
                String[] record;
                while ((record = csvParser.parseNext()) != null) {
                    // Process the current record, incrementing the row index after each processing
                    lastColumnCount = dealRecord(record, rowIndex++, autoTrim, autoStrip,
                            nullString, globalConfig, lastColumnCount);
                }
            } catch (ExcelAnalysisStopSheetException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Custom stop!", e);
                }
            } catch (TextParsingException e) {
                // uniVocity throws TextParsingException for truncated quoted fields or unexpected EOF
                // within a quoted field. Treat such cases as benign and end the current sheet gracefully.
                if (isBenignCsvParseException(e)) {
                    if (log.isDebugEnabled()) {
                        log.debug("CSV parse finished early due to benign parse error: {}", e.getMessage());
                    } else if (log.isWarnEnabled()) {
                        log.warn("CSV parse finished early due to benign parse error.");
                    }
                } else {
                    throw new ExcelAnalysisException(e);
                }
            }

            // The last sheet is read
            csvReadContext.analysisEventProcessor().endSheet(csvReadContext);
        }
    }

    /**
     * Initializes and returns a uniVocity {@link CsvParser} instance based on the configuration
     * provided in the CsvReadContext. This method determines the appropriate input stream and
     * character set to create the CSV parser.
     *
     * @return A uniVocity CsvParser instance for parsing CSV files.
     * @throws IOException If an I/O error occurs while accessing the input stream or file.
     */
    private CsvParser csvParser() throws IOException {
        // Retrieve the CsvReadWorkbookHolder instance from the CsvReadContext.
        CsvReadWorkbookHolder csvReadWorkbookHolder = csvReadContext.csvReadWorkbookHolder();
        // Get the CSV format configuration from the CsvReadWorkbookHolder.
        CsvFormatConfiguration csvFormatConfiguration = csvReadWorkbookHolder.getCsvFormatConfiguration();
        // Convert to uniVocity parser settings
        CsvParserSettings parserSettings = csvFormatConfiguration.toParserSettings();
        // Determine the ByteOrderMarkEnum based on the character set name.
        ByteOrderMarkEnum byteOrderMark = ByteOrderMarkEnum.valueOfByCharsetName(
                csvReadContext.csvReadWorkbookHolder().getCharset().name());

        // Build the reader with BOM handling and charset decoding
        Reader reader;
        InputStream inputStream;

        // If the configuration mandates the use of an input stream, use it.
        if (csvReadWorkbookHolder.getMandatoryUseInputStream()) {
            inputStream = csvReadWorkbookHolder.getInputStream();
        } else if (csvReadWorkbookHolder.getFile() != null) {
            // If a file is provided in the configuration, use the file's input stream.
            inputStream = Files.newInputStream(csvReadWorkbookHolder.getFile().toPath());
        } else {
            // As a fallback, use the input stream.
            inputStream = csvReadWorkbookHolder.getInputStream();
        }

        reader = buildReader(inputStream, byteOrderMark);

        // Create and begin parsing with the uniVocity CsvParser
        CsvParser parser = new CsvParser(parserSettings);
        parser.beginParsing(reader);
        return parser;
    }

    /**
     * Builds a {@link Reader} from the provided InputStream, handling BOM detection when needed.
     *
     * @param inputStream   The input stream from which the CSV data will be read.
     * @param byteOrderMark The enumeration representing the Byte Order Mark (BOM) of the file's character set.
     * @return A Reader configured to decode the CSV data with proper BOM handling.
     */
    private Reader buildReader(InputStream inputStream, ByteOrderMarkEnum byteOrderMark) {
        if (byteOrderMark == null) {
            return new InputStreamReader(
                    inputStream, csvReadContext.csvReadWorkbookHolder().getCharset());
        }
        return new InputStreamReader(
                new BOMInputStream(inputStream, byteOrderMark.getByteOrderMark()),
                csvReadContext.csvReadWorkbookHolder().getCharset());
    }

    /**
     * Processes a single CSV record (as a {@code String[]}) and maps its content to a structured
     * format for further analysis.
     *
     * <p>Performance note: config values ({@code autoTrim}, {@code autoStrip}, {@code nullString},
     * {@code globalConfig}) are resolved once in {@link #execute()} and passed in to avoid
     * repeated holder-chain traversals on every row.
     *
     * @param record          The CSV record as a String array to be processed.
     * @param rowIndex        The index of the current row being processed.
     * @param autoTrim        Whether to trim cell values.
     * @param autoStrip       Whether to strip cell values.
     * @param nullString      The configured null-string representation, or {@code null}.
     * @param globalConfig    The global configuration (passed to ReadRowHolder).
     * @param lastColumnCount The column count from the previous row (used to pre-size the map).
     * @return The column count of this record (for pre-sizing the next row's map).
     */
    private int dealRecord(String[] record, int rowIndex, boolean autoTrim, boolean autoStrip,
                           String nullString, GlobalConfiguration globalConfig, int lastColumnCount) {
        int columnCount = record.length;
        // Pre-size the map: use the larger of current and last column count to reduce rehashing.
        // The load-factor-adjusted capacity avoids a resize for the expected number of entries.
        int mapCapacity = Math.max(columnCount, lastColumnCount);
        mapCapacity = (int) (mapCapacity / 0.75f) + 1;
        Map<Integer, Cell> cellMap = new LinkedHashMap<>(mapCapacity);

        boolean hasData = false;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            String cellString = record[columnIndex];
            ReadCellData<String> readCellData = new ReadCellData<>();
            readCellData.setRowIndex(rowIndex);
            readCellData.setColumnIndex(columnIndex);

            // Treat the configured nullString as an empty cell so that null values
            // round-trip correctly (the writer emits nullString for null fields).
            if (nullString != null && nullString.equals(cellString)) {
                cellString = null;
            }

            // csv is an empty string of whether <code>,,</code> is read or <code>,"",</code>
            if (StringUtils.isNotBlank(cellString)) {
                readCellData.setType(CellDataTypeEnum.STRING);
                if (autoStrip) {
                    readCellData.setStringValue(StringUtils.strip(cellString));
                } else if (autoTrim) {
                    readCellData.setStringValue(cellString.trim());
                } else {
                    readCellData.setStringValue(cellString);
                }
                hasData = true;
            } else {
                readCellData.setType(CellDataTypeEnum.EMPTY);
            }
            cellMap.put(columnIndex, readCellData);
        }

        RowTypeEnum rowType = hasData ? RowTypeEnum.DATA : RowTypeEnum.EMPTY;
        ReadRowHolder readRowHolder = new ReadRowHolder(rowIndex, rowType, globalConfig, cellMap);
        csvReadContext.readRowHolder(readRowHolder);

        csvReadContext.csvReadSheetHolder().setCellMap(cellMap);
        csvReadContext.csvReadSheetHolder().setRowIndex(rowIndex);
        csvReadContext.analysisEventProcessor().endRow(csvReadContext);

        return columnCount;
    }

    /**
     * Determine whether a {@link TextParsingException} from uniVocity is benign, i.e., caused by
     * truncated quoted fields or early EOF while parsing a quoted field. In such cases
     * we should stop reading the current sheet gracefully rather than failing the whole read.
     */
    private static boolean isBenignCsvParseException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                // Messages from uniVocity-parsers for truncated/unterminated quoted fields
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
