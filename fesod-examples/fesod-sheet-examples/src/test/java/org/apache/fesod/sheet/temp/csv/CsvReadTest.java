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

package org.apache.fesod.sheet.temp.csv;

import com.alibaba.fastjson2.JSON;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.FastExcel;
import org.apache.fesod.sheet.metadata.csv.AppendableWriter;
import org.apache.fesod.sheet.metadata.csv.CsvFormatConfiguration;
import org.apache.fesod.sheet.util.TestFileUtil;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class CsvReadTest {

    @Test
    public void write() throws Exception {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder().build();
        Writer writer = new AppendableWriter(new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(TestFileUtil.createNewFile("csvWrite1.csv")))));
        CsvWriter csvWriter = new CsvWriter(writer, config.toWriterSettings());
        csvWriter.writeHeaders("userId", "userName");
        for (int i = 0; i < 10; i++) {
            csvWriter.writeRow("userId" + i, "userName" + i);
        }
        csvWriter.flush();
        csvWriter.close();
    }

    @Test
    public void read1() throws Exception {
        CsvFormatConfiguration config = CsvFormatConfiguration.builder()
                .nullString("")
                .build();
        CsvParser parser = new CsvParser(config.toParserSettings());
        parser.beginParsing(new FileReader("src/test/resources/poi/last_row_number_xssf_date_test.csv"));
        String[] record;
        while ((record = parser.parseNext()) != null) {
            String lastName = record[0];
            String firstName = record.length > 1 ? record[1] : null;
            log.info("row:{},{}", lastName, firstName);
        }
        parser.stopParsing();
    }

    @Test
    public void csvWrite() throws Exception {
        // 写法1
        String fileName = TestFileUtil.getPath() + "simpleWrite" + System.currentTimeMillis() + ".csv";
        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        FastExcel.write(fileName, CsvData.class).sheet().doWrite(data());

        // 读
        List<Object> list = FastExcel.read(fileName).sheet(0).headRowNumber(0).doReadSync();
        log.info("数据：{}", list.size());
        for (Object data : list) {
            log.info("返回数据：{}", JSON.toJSONString(data));
        }
    }

    @Test
    public void writev2() throws Exception {
        // 写法1
        String fileName = TestFileUtil.getPath() + "simpleWrite" + System.currentTimeMillis() + ".csv";
        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        FastExcel.write(fileName, CsvData.class).sheet().doWrite(data());

        FastExcel.read(fileName, CsvData.class, new CsvDataListener()).sheet().doRead();
    }

    @Test
    public void writeFile() throws Exception {
        FileMagic fileMagic = FileMagic.valueOf(new File("src/test/resources/poi/last_row_number_xssf_date_test.csv"));
        Assertions.assertEquals(FileMagic.UNKNOWN, fileMagic);
        log.info("{}", fileMagic);
    }

    private List<CsvData> data() {
        List<CsvData> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CsvData data = new CsvData();
            data.setString("字符,串" + i);
            // data.setDate(new Date());
            data.setDoubleData(0.56);
            data.setIgnore("忽略" + i);
            list.add(data);
        }
        return list;
    }

    @Test
    public void read() {
        //
        // CsvFormatConfiguration config = CsvFormatConfiguration.builder()
        //     .skipHeaderRecord(true)
        //     .build();
        // CsvParser parser = new CsvParser(config.toParserSettings());
        // parser.beginParsing(in);
        // String[] record;
        // while ((record = parser.parseNext()) != null) {
        //     String lastName = record[0]; // "id"
        //     String firstName = record[1]; // "name"
        //     System.out.println(lastName);
        //     System.out.println(firstName);
        // }
        // parser.stopParsing();

    }
}
