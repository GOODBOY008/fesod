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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.fesod.sheet.metadata.Cell;
import org.apache.fesod.sheet.metadata.data.ReadCellData;

/**
 * A lightweight {@link Map Map&lt;Integer, Cell&gt;} backed by a {@code ReadCellData[]} array.
 * Optimized for CSV's dense, sequential column indices (0..N-1).
 *
 * <p>Avoids {@link java.util.LinkedHashMap} overhead: no Entry objects, no hashing, no Node chains.
 * Provides O(1) {@code get} and {@code put} via direct array indexing.</p>
 *
 * <p>Instances are designed to be reused across rows via {@link #reset(int)}.</p>
 */
public class ArrayCellMap extends AbstractMap<Integer, Cell> {

    private ReadCellData<?>[] cells;
    private int size;

    public ArrayCellMap(int capacity) {
        this.cells = new ReadCellData<?>[capacity];
        this.size = 0;
    }

    /**
     * Reuse for next row — clear references without reallocating the array
     * unless the new capacity exceeds the current length.
     */
    public void reset(int newCapacity) {
        if (newCapacity > cells.length) {
            cells = new ReadCellData<?>[newCapacity];
        } else {
            Arrays.fill(cells, 0, size, null);
        }
        size = 0;
    }

    @Override
    public Cell get(Object key) {
        int idx = (Integer) key;
        return (idx >= 0 && idx < cells.length) ? cells[idx] : null;
    }

    @Override
    public Cell put(Integer key, Cell value) {
        int idx = key;
        ensureCapacity(idx + 1);
        Cell old = cells[idx];
        cells[idx] = (ReadCellData<?>) value;
        if (old == null) {
            size++;
        }
        return old;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        int idx = (Integer) key;
        return idx >= 0 && idx < cells.length && cells[idx] != null;
    }

    @Override
    public Set<Entry<Integer, Cell>> entrySet() {
        return new EntrySet();
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > cells.length) {
            int newCapacity = Math.max(minCapacity, cells.length + (cells.length >> 1));
            cells = Arrays.copyOf(cells, newCapacity);
        }
    }

    private class EntrySet extends AbstractSet<Entry<Integer, Cell>> {
        @Override
        public Iterator<Entry<Integer, Cell>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return ArrayCellMap.this.size;
        }
    }

    private class EntryIterator implements Iterator<Entry<Integer, Cell>> {
        private int cursor;
        private int remaining;

        EntryIterator() {
            this.cursor = 0;
            this.remaining = size;
            advanceToNext();
        }

        private void advanceToNext() {
            while (cursor < cells.length && cells[cursor] == null) {
                cursor++;
            }
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public Entry<Integer, Cell> next() {
            if (remaining <= 0) {
                throw new NoSuchElementException();
            }
            Entry<Integer, Cell> entry = new AbstractMap.SimpleImmutableEntry<>(cursor, cells[cursor]);
            remaining--;
            cursor++;
            advanceToNext();
            return entry;
        }
    }
}
