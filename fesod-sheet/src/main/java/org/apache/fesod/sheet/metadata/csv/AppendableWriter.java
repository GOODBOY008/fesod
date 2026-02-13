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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

/**
 * Adapter that wraps a {@link java.lang.Appendable} as a {@link Writer} so it
 * can be passed to APIs (such as uniVocity's {@code CsvWriter}) that require a
 * {@code Writer}.
 *
 * <p>If the wrapped {@code Appendable} also implements {@link Flushable} or
 * {@link Closeable}, those operations are delegated; otherwise they are no-ops.</p>
 */
public class AppendableWriter extends Writer {

    private final Appendable appendable;

    public AppendableWriter(Appendable appendable) {
        if (appendable == null) {
            throw new NullPointerException("appendable must not be null");
        }
        this.appendable = appendable;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        appendable.append(new String(cbuf, off, len));
    }

    @Override
    public void write(int c) throws IOException {
        appendable.append((char) c);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        appendable.append(str, off, off + len);
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        appendable.append(csq);
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        appendable.append(csq, start, end);
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        appendable.append(c);
        return this;
    }

    @Override
    public void flush() throws IOException {
        if (appendable instanceof Flushable) {
            ((Flushable) appendable).flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (appendable instanceof Closeable) {
            ((Closeable) appendable).close();
        }
    }
}
