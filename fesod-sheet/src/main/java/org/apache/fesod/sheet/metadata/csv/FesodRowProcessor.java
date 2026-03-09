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

import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.RowProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.context.csv.CsvReadContext;
import org.apache.fesod.sheet.enums.RowTypeEnum;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.read.metadata.holder.ReadRowHolder;

/**
 * Implements uniVocity's {@link RowProcessor} interface to receive parsed rows via callback.
 * This is the high-performance alternative to {@code parseNext()} iteration, eliminating
 * iterator overhead and enabling direct method invocation from parser to fesod's {@link ReadListener}.
 *
 * <p>The parser calls {@link #rowProcessed(String[], ParsingContext)} directly with each parsed row,
 * achieving zero-overhead streaming.</p>
 */
@Slf4j
public class FesodRowProcessor implements RowProcessor {

    private final CsvReadContext context;
    private final GlobalConfiguration globalConfig;
    private final boolean autoTrim;
    private final boolean autoStrip;
    private final String nullString;
    private int currentRowIndex;

    /**
     * Reusable array-backed cell map — avoids LinkedHashMap allocation per row.
     */
    private final ArrayCellMap arrayCellMap;

    /**
     * Reusable row holder — avoids object allocation per row.
     * Safe because the holder is consumed synchronously within endRow() before the next row.
     */
    private ReadRowHolder reusableRowHolder;

    public FesodRowProcessor(
            CsvReadContext context,
            GlobalConfiguration globalConfig,
            boolean autoTrim,
            boolean autoStrip,
            String nullString) {
        this.context = context;
        this.globalConfig = globalConfig;
        this.autoTrim = autoTrim;
        this.autoStrip = autoStrip;
        this.nullString = nullString;
        this.currentRowIndex = 0;
        this.arrayCellMap = new ArrayCellMap(16);
    }

    @Override
    public void processStarted(ParsingContext context) {
        // No initialization needed - parsing starts automatically
    }

    @Override
    public void rowProcessed(String[] row, ParsingContext parsingContext) {
        // Direct callback - no iterator overhead
        // Convert String[] to ReadCellData and invoke listener
        dealRecord(row, currentRowIndex++);
    }

    @Override
    public void processEnded(ParsingContext parsingContext) {
        // Signal completion to all listeners via AnalysisEventProcessor
        if (this.context.analysisEventProcessor() != null) {
            this.context.analysisEventProcessor().endSheet(this.context);
        }
    }

    private void dealRecord(String[] record, int rowIndex) {
        int columnCount = record.length;
        arrayCellMap.reset(columnCount);

        boolean hasData = false;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            String cellString = record[columnIndex];

            if (nullString != null && nullString.equals(cellString)) {
                cellString = null;
            }

            if (cellString != null && !cellString.isEmpty()) {
                if (autoStrip) {
                    cellString = org.apache.fesod.common.util.StringUtils.strip(cellString);
                } else if (autoTrim) {
                    cellString = cellString.trim();
                }
                arrayCellMap.put(columnIndex, ReadCellData.newInstance(cellString, rowIndex, columnIndex));
                hasData = true;
            } else {
                arrayCellMap.put(columnIndex, ReadCellData.newEmptyInstance(rowIndex, columnIndex));
            }
        }

        RowTypeEnum rowType = hasData ? RowTypeEnum.DATA : RowTypeEnum.EMPTY;

        if (reusableRowHolder == null) {
            reusableRowHolder = new ReadRowHolder(rowIndex, rowType, globalConfig, arrayCellMap);
        } else {
            reusableRowHolder.setRowIndex(rowIndex);
            reusableRowHolder.setRowType(rowType);
            reusableRowHolder.setCellMap(arrayCellMap);
        }
        context.readRowHolder(reusableRowHolder);

        context.csvReadSheetHolder().setCellMap(arrayCellMap);
        context.csvReadSheetHolder().setRowIndex(rowIndex);
        context.analysisEventProcessor().endRow(context);
    }
}
