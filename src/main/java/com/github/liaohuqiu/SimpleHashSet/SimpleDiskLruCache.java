/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.liaohuqiu.SimpleHashSet;

import java.io.File;
import java.io.IOException;

public final class SimpleDiskLruCache implements IDiskCache {

    private static final String LOG_TAG = "simple-lru";
    private static final boolean DEBUG = true;

    private LruActionTracer mActionTracer;

    private SimpleDiskLruCache(File directory, int appVersion, long capacity) {
        mActionTracer = new LruActionTracer(this, directory, appVersion, capacity);
        if (DEBUG) {
            CLog.d(LOG_TAG, "Construct: path: %s version: %s capacity: %s", directory, appVersion, capacity);
        }
    }

    /**
     * Opens the cache in {@code directory}, creating a cache if none exists
     * there.
     *
     * @param directory  a writable directory
     * @param appVersion
     * @param maxSize    the maximum number of bytes this cache should use to store
     * @throws java.io.IOException if reading or writing the cache directory fails
     */
    public static SimpleDiskLruCache open(File directory, int appVersion, long maxSize)
            throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        // prefer to pick up where we left off
        SimpleDiskLruCache cache = new SimpleDiskLruCache(directory, appVersion, maxSize);
        cache.mActionTracer.tryToResume();

        return cache;
    }

    /**
     * clear all the content
     */
    @Override
    public synchronized void clear() throws IOException {
        mActionTracer.clear();
    }

    @Override
    public boolean has(String key) {
        return mActionTracer.has(key);
    }

    /**
     * Returns a {@link CacheEntry} named {@code key}, or null if it doesn't
     * exist is not currently readable. If a value is returned, it is moved to
     * the head of the LRU queue.
     */
    @Override
    public synchronized CacheEntry getEntry(String key) throws IOException {
        return mActionTracer.getEntry(key);
    }

    @Override
    public synchronized CacheEntry beginEdit(String key) throws IOException {
        return mActionTracer.beginEdit(key);
    }

    /**
     * abort edit
     *
     * @param cacheEntry
     */
    @Override
    public void abortEdit(CacheEntry cacheEntry) {
        mActionTracer.abortEdit(cacheEntry);
    }

    @Override
    public void commitEdit(CacheEntry cacheEntry) throws IOException {
        mActionTracer.commitEdit(cacheEntry);
    }

    /**
     * Drops the entry for {@code key} if it exists and can be removed. Entries
     * actively being edited cannot be removed.
     *
     * @return true if an entry was removed.
     */
    public synchronized boolean delete(String key) throws IOException {
        return mActionTracer.delete(key);
    }

    @Override
    public long getCapacity() {
        return mActionTracer.getCapacity();
    }

    /**
     * Returns the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    @Override
    public synchronized long getSize() {
        return mActionTracer.getSize();
    }

    /**
     * Returns the directory where this cache stores its data.
     */
    public File getDirectory() {
        return mActionTracer.getDirectory();
    }

    /**
     * Force buffered operations to the filesystem.
     */
    @Override
    public synchronized void flush() throws IOException {
        mActionTracer.flush();
    }

    /**
     * Closes this cache. Stored values will remain on the filesystem.
     */
    @Override
    public synchronized void close() throws IOException {
        mActionTracer.close();
    }
}
