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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.fesod.sheet.metadata.csv.AppendableWriter;
import org.junit.jupiter.api.Test;

class AppendableWriterTest {

    @Test
    void writeCharArrayDelegatesToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("hello".toCharArray(), 0, 5);
        }
        assertEquals("hello", sb.toString());
    }

    @Test
    void writeCharArrayWithOffset() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("hello world".toCharArray(), 6, 5);
        }
        assertEquals("world", sb.toString());
    }

    @Test
    void writeSingleChar() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write('A');
        }
        assertEquals("A", sb.toString());
    }

    @Test
    void writeStringWithOffsetAndLength() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("hello world", 6, 5);
        }
        assertEquals("world", sb.toString());
    }

    @Test
    void appendCharSequence() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append("test");
        }
        assertEquals("test", sb.toString());
    }

    @Test
    void appendCharSequenceWithRange() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append("hello world", 0, 5);
        }
        assertEquals("hello", sb.toString());
    }

    @Test
    void appendSingleChar() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append('Z');
        }
        assertEquals("Z", sb.toString());
    }

    @Test
    void flushDelegatesToFlushableAppendable() throws IOException {
        StringWriter sw = new StringWriter(); // StringWriter implements Flushable
        AppendableWriter writer = new AppendableWriter(sw);
        writer.write("data");
        writer.flush(); // should not throw
        assertEquals("data", sw.toString());
    }

    @Test
    void flushIsNoOpForNonFlushableAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.flush(); // should not throw
    }

    @Test
    void closeDelegatesToCloseableAppendable() throws IOException {
        StringWriter sw = new StringWriter(); // StringWriter implements Closeable
        AppendableWriter writer = new AppendableWriter(sw);
        writer.write("data");
        writer.close(); // should not throw
        assertEquals("data", sw.toString());
    }

    @Test
    void closeIsNoOpForNonCloseableAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close(); // should not throw
    }

    @Test
    void flushAndCloseDelegateCorrectly() throws IOException {
        final boolean[] flushed = {false};
        final boolean[] closed = {false};

        Appendable appendable = new Appendable() {
            final StringBuilder sb = new StringBuilder();

            @Override
            public Appendable append(CharSequence csq) {
                sb.append(csq);
                return this;
            }

            @Override
            public Appendable append(CharSequence csq, int start, int end) {
                sb.append(csq, start, end);
                return this;
            }

            @Override
            public Appendable append(char c) {
                sb.append(c);
                return this;
            }
        };

        // Non-Flushable, non-Closeable — flush/close should be no-ops
        AppendableWriter writer = new AppendableWriter(appendable);
        writer.flush();
        writer.close();
    }

    @Test
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new AppendableWriter(null));
    }

    @Test
    void flushAndCloseOnFlushableCloseable() throws IOException {
        final boolean[] flushed = {false};
        final boolean[] closed = {false};

        // Custom Appendable that is both Flushable and Closeable
        class FlushableCloseableAppendable implements Appendable, Flushable, Closeable {
            final StringBuilder sb = new StringBuilder();

            @Override
            public Appendable append(CharSequence csq) {
                sb.append(csq);
                return this;
            }

            @Override
            public Appendable append(CharSequence csq, int start, int end) {
                sb.append(csq, start, end);
                return this;
            }

            @Override
            public Appendable append(char c) {
                sb.append(c);
                return this;
            }

            @Override
            public void flush() {
                flushed[0] = true;
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        }

        FlushableCloseableAppendable appendable = new FlushableCloseableAppendable();
        AppendableWriter writer = new AppendableWriter(appendable);
        writer.write("test");
        writer.flush();
        assertTrue(flushed[0], "flush should have been delegated");
        writer.close();
        assertTrue(closed[0], "close should have been delegated");
    }
}
