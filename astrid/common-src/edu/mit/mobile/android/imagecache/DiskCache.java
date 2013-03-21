package edu.mit.mobile.android.imagecache;

/*
 * Copyright (C) 2011-2013 MIT Mobile Experience Lab
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Build;
import android.os.StatFs;
import android.util.Log;

import com.todoroo.astrid.utility.Constants;

/**
 * <p>
 * A simple disk cache.
 * </p>
 *
 * <p>
 * By default, the maximum size of the cache is automatically set based on the amount of free space
 * available to the cache. Alternatively, a fixed size can be specified using
 * {@link #setCacheMaxSize(long)}.
 * </p>
 *
 * <p>
 * By default, the cache will automatically maintain its size by periodically checking to see if it
 * estimates that a trim is needed and if it is, proceeding to running {@link #trim()} on a worker
 * thread. This feature can be controlled by {@link #setAutoTrimFrequency(int)}.
 * </p>
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 * @param <K>
 *            the key to store/retrieve the value
 * @param <V>
 *            the value that will be stored to disk
 */
public abstract class DiskCache<K, V> {
    private static final String TAG = "DiskCache";

    /**
     * Automatically determines the maximum size of the cache based on available free space.
     */
    public static final int AUTO_MAX_CACHE_SIZE = 0;

    /**
     * The default number of cache hits before {@link #trim()} is automatically triggered. See
     * {@link #setAutoTrimFrequency(int)}.
     */
    public static final int DEFAULT_AUTO_TRIM_FREQUENCY = 10;

    /**
     * Pass to {@link #setAutoTrimFrequency(int)} to disable automatic trimming. See {@link #trim()}
     * .
     */
    public static final int AUTO_TRIM_DISABLED = 0;

    // /////////////////////////////////////////////

    private long mMaxDiskUsage = AUTO_MAX_CACHE_SIZE;

    private MessageDigest hash;

    private final File mCacheBase;
    private final String mCachePrefix, mCacheSuffix;

    private final ConcurrentLinkedQueue<File> mQueue = new ConcurrentLinkedQueue<File>();

    /**
     * In auto max cache mode, the maximum is set to the total free space divided by this amount.
     */
    private static final int AUTO_MAX_CACHE_SIZE_DIVISOR = 10;

    private int mAutoTrimFrequency = DEFAULT_AUTO_TRIM_FREQUENCY;

    private final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(1, 5, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    private int mAutoTrimHitCount = 1;

    private long mEstimatedDiskUsage;

    private long mEstimatedFreeSpace;

    /**
     * Creates a new disk cache with no cachePrefix or cacheSuffix
     *
     * @param cacheBase
     */
    public DiskCache(File cacheBase) {
        this(cacheBase, null, null);
    }

    /**
     * Creates a new disk cache.
     *
     * @param cacheBase
     *            The base directory within which all the cache files will be stored.
     * @param cachePrefix
     *            If you want a prefix to the filenames, place one here. Otherwise, pass null.
     * @param cacheSuffix
     *            A suffix to the cache filename. Null is also ok here.
     */
    public DiskCache(File cacheBase, String cachePrefix, String cacheSuffix) {
        mCacheBase = cacheBase;
        mCachePrefix = cachePrefix;
        mCacheSuffix = cacheSuffix;

        try {
            hash = MessageDigest.getInstance("SHA-1");

        } catch (final NoSuchAlgorithmException e) {
            try {
                hash = MessageDigest.getInstance("MD5");
            } catch (final NoSuchAlgorithmException e2) {
                final RuntimeException re = new RuntimeException("No available hashing algorithm");
                re.initCause(e2);
                throw re;
            }
        }

        updateDiskUsageInBg();
    }

    /**
     * Sets the maximum size of the cache, in bytes. The default is to automatically manage the max
     * size based on the available disk space. This can be explicitly set by passing this
     * {@link #AUTO_MAX_CACHE_SIZE}.
     *
     * @param maxSize
     *            maximum size of the cache, in bytes.
     */
    public void setCacheMaxSize(long maxSize) {
        mMaxDiskUsage = maxSize;
    }

    /**
     * After this many puts, if it looks like there's a low space condition, {@link #trim()} will
     * automatically be called.
     *
     * @param autoTrimFrequency
     *            Set to {@link #AUTO_TRIM_DISABLED} to turn off auto trim. The default is
     *            {@link #DEFAULT_AUTO_TRIM_FREQUENCY}.
     */
    public void setAutoTrimFrequency(int autoTrimFrequency) {
        mAutoTrimFrequency = autoTrimFrequency;
    }

    /**
     * Updates cached estimates on the
     */
    private void updateDiskUsageEstimates() {
        final long diskUsage = getCacheDiskUsage();

        final long availableSpace = getFreeSpace();

        synchronized (this) {
            mEstimatedDiskUsage = diskUsage;
            mEstimatedFreeSpace = availableSpace;
        }
    }

    private void updateDiskUsageInBg() {
        mExecutor.execute(new Runnable() {

            @Override
            public void run() {
                updateDiskUsageEstimates();
            }
        });
    }

    /**
     * Gets the amount of space free on the cache volume.
     *
     * @return free space in bytes.
     */
    private long getFreeSpace() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return mCacheBase.getUsableSpace();
        } else {
            // maybe make singleton
            final StatFs stat = new StatFs(mCacheBase.getAbsolutePath());
            return (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
    }

    /**
     * Gets the cache filename for the given key.
     *
     * @param key
     * @return
     */
    protected File getFile(K key) {
        return new File(mCacheBase, (mCachePrefix != null ? mCachePrefix : "") + hash(key)
                + (mCacheSuffix != null ? mCacheSuffix : ""));
    }

    /**
     * Writes the value stored in the cache to disk by calling
     * {@link #toDisk(Object, Object, OutputStream)}.
     *
     * @param key
     *            The key to find the value.
     * @param value
     *            the data to be written to disk.
     */
    public final synchronized void put(K key, V value) throws IOException, FileNotFoundException {
        final File saveHere = getFile(key);

        final OutputStream os = new FileOutputStream(saveHere);
        toDisk(key, value, os);
        os.close();

        mEstimatedDiskUsage += saveHere.length();

        touchEntry(saveHere);

        autotrim();
    }

    /**
     * Writes the contents of the InputStream straight to disk. It is the caller's responsibility to
     * ensure it's the same type as what would be written with
     * {@link #toDisk(Object, Object, OutputStream)}
     *
     * @param key
     * @param value
     * @throws IOException
     * @throws FileNotFoundException
     */
    public final void putRaw(K key, InputStream value) throws IOException, FileNotFoundException {

        final File saveHere = getFile(key);

        final File tempFile = new File(saveHere.getAbsolutePath() + ".temp");

        boolean allGood = false;
        try {
            final OutputStream os = new FileOutputStream(tempFile);

            inputStreamToOutputStream(value, os);
            os.close();

            synchronized (this) {
                // overwrite
                saveHere.delete();
                tempFile.renameTo(saveHere);
            }
            allGood = true;
        } finally {
            // clean up on any exception
            if (!allGood) {
                saveHere.delete();
                tempFile.delete();
            }
        }
        if (allGood) {
            mEstimatedDiskUsage += saveHere.length();

            touchEntry(saveHere);

            autotrim();
        }
    }

    /**
     * Puts the key at the end of the queue, removing it if it's already present. This will cause it
     * to be removed last when {@link #trim()} is called.
     *
     * @param cacheFile
     */
    private void touchEntry(File cacheFile) {
        if (mQueue.contains(cacheFile)) {
            mQueue.remove(cacheFile);
        }
        mQueue.add(cacheFile);
    }

    /**
     * Marks the given key as accessed recently. This will deprioritize it from automatically being
     * purged upon {@link #trim()}.
     *
     * @param key
     */
    protected void touchKey(K key) {
        touchEntry(getFile(key));
    }

    /**
     * Call this every time you may be able to start a trim in the background. This implicitly runs
     * {@link #updateDiskUsageInBg()} each time it's called.
     */
    private void autotrim() {
        if (mAutoTrimFrequency == 0) {
            return;
        }

        mAutoTrimHitCount = (mAutoTrimHitCount + 1) % mAutoTrimFrequency;

        if (mAutoTrimHitCount == 0
                && mEstimatedDiskUsage > Math.min(mEstimatedFreeSpace, mMaxDiskUsage)) {

            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    trim();
                }
            });
        }

        updateDiskUsageInBg();
    }

    /**
     * Reads from an inputstream, dumps to an outputstream
     *
     * @param is
     * @param os
     * @throws IOException
     */
    static public void inputStreamToOutputStream(InputStream is, OutputStream os)
            throws IOException {
        final int bufsize = 8196 * 10;
        final byte[] cbuf = new byte[bufsize];

        for (int readBytes = is.read(cbuf, 0, bufsize); readBytes > 0; readBytes = is.read(cbuf, 0,
                bufsize)) {
            os.write(cbuf, 0, readBytes);
        }
    }

    /**
     * Reads the value from disk using {@link #fromDisk(Object, InputStream)}.
     *
     * @param key
     * @return The value for key or null if the key doesn't map to any existing entries.
     */
    public final synchronized V get(K key) throws IOException {
        final File readFrom = getFile(key);

        if (!readFrom.exists()) {
            return null;
        }

        final InputStream is = new FileInputStream(readFrom);
        final V out = fromDisk(key, is);
        is.close();

        touchEntry(readFrom);

        return out;
    }

    /**
     * Checks the disk cache for a given key.
     *
     * @param key
     * @return true if the disk cache contains the given key
     */
    public final synchronized boolean contains(K key) {
        final File readFrom = getFile(key);

        return readFrom.exists();
    }

    /**
     * Removes the item from the disk cache.
     *
     * @param key
     * @return true if the cached item has been removed or was already removed, false if it was not
     *         able to be removed.
     */
    public synchronized boolean clear(K key) {
        final File readFrom = getFile(key);

        if (!readFrom.exists()) {
            return true;
        }
        final long size = readFrom.length();

        final boolean success = readFrom.delete();

        if (success) {
            mEstimatedDiskUsage -= size;
        }

        return success;
    }

    /**
     * Removes the item from the disk cache.
     *
     * @param cacheFile
     * @return true if the cached item has been removed or was already removed, false if it was not
     *         able to be removed.
     */
    private synchronized boolean clear(File cacheFile) {

        if (!cacheFile.exists()) {
            return true;
        }
        final long size = cacheFile.length();

        final boolean success = cacheFile.delete();

        if (success) {
            mEstimatedDiskUsage -= size;
        }

        return success;
    }

    /**
     * Clears the cache files from disk.
     *
     * Note: this only clears files that match the given prefix/suffix.
     *
     * @return true if the operation succeeded without error. It is possible that it will fail and
     *         the cache ends up being partially cleared.
     */
    public synchronized boolean clear() {
        boolean success = true;

        for (final File cacheFile : mCacheBase.listFiles(mCacheFileFilter)) {
            if (!cacheFile.delete()) {
                Log.e(TAG, "error deleting " + cacheFile);
                success = false;
            }
        }
        return success;
    }

    /**
     * @return the number of files in the cache
     * @deprecated please use {@link #getCacheEntryCount()} or {@link #getCacheDiskUsage()} instead.
     */
    @Deprecated
    public int getCacheSize() {
        return getCacheEntryCount();
    }

    /**
     * @return the number of files in the cache as it is on disk.
     */
    public int getCacheEntryCount() {
        return mCacheBase.listFiles(mCacheFileFilter).length;
    }

    /**
     * @return the size of the cache in bytes, as it is on disk.
     */
    public long getCacheDiskUsage() {
        long usage = 0;
        for (final File cacheFile : mCacheBase.listFiles(mCacheFileFilter)) {
            usage += cacheFile.length();
        }
        return usage;
    }

    private final CacheFileFilter mCacheFileFilter = new CacheFileFilter();

    private class CacheFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            final String path = pathname.getName();
            return (mCachePrefix != null ? path.startsWith(mCachePrefix) : true)
                    && (mCacheSuffix != null ? path.endsWith(mCacheSuffix) : true);
        }
    };

    private final Comparator<File> mLastModifiedOldestFirstComparator = new Comparator<File>() {

        @Override
        public int compare(File lhs, File rhs) {
            return Long.valueOf(lhs.lastModified()).compareTo(rhs.lastModified());
        }
    };

    /**
     * Clears out cache entries in order to reduce the on-disk usage to the desired maximum size.
     * This is a somewhat expensive operation, so it should be done on a background thread.
     *
     * @return the number of bytes worth of files that were trimmed.
     * @see #setCacheMaxSize(long)
     */
    public synchronized long trim() {

        long desiredSize;
        final long freeSpace = getFreeSpace();

        if (mMaxDiskUsage > 0) {
            desiredSize = mMaxDiskUsage;
        } else {
            desiredSize = getFreeSpace() / AUTO_MAX_CACHE_SIZE_DIVISOR;
        }

        desiredSize = Math.min(freeSpace, desiredSize);

        final long sizeToTrim = Math.max(0, getCacheDiskUsage() - desiredSize);

        if (sizeToTrim == 0) {
            return 0;
        }

        long trimmed = 0;

        final List<File> sorted = Arrays.asList(mCacheBase.listFiles(mCacheFileFilter));
        Collections.sort(sorted, mLastModifiedOldestFirstComparator);

        // first clear out any files that aren't in the queue
        for (final File cacheFile : sorted) {
            if (mQueue.contains(cacheFile)) {
                continue;
            }

            final long size = cacheFile.length();
            if (clear(cacheFile)) {
                trimmed += size;
                if (Constants.DEBUG) {
                    Log.d(TAG, "trimmed unqueued " + cacheFile.getName() + " from cache.");
                }
            }

            if (trimmed >= sizeToTrim) {
                break;
            }
        }

        while (trimmed < sizeToTrim && !mQueue.isEmpty()) {
            final File cacheFile = mQueue.poll();

            // shouldn't happen due to the check above, but just in case...
            if (cacheFile == null) {
                break;
            }

            final long size = cacheFile.length();

            if (clear(cacheFile)) {
                trimmed += size;
                if (Constants.DEBUG) {
                    Log.d(TAG, "trimmed " + cacheFile.getName() + " from cache.");
                }
            } else {
                Log.e(TAG, "error deleting " + cacheFile);
            }
        }

        if (Constants.DEBUG) {
            Log.d(TAG, "trimmed a total of " + trimmed + " bytes from cache.");
        }
        return trimmed;
    }

    /**
     * Implement this to do the actual disk writing. Do not close the OutputStream; it will be
     * closed for you.
     *
     * @param key
     * @param in
     * @param out
     */
    protected abstract void toDisk(K key, V in, OutputStream out);

    /**
     * Implement this to do the actual disk reading.
     *
     * @param key
     * @param in
     * @return a new instance of {@link V} containing the contents of in.
     */
    protected abstract V fromDisk(K key, InputStream in);

    /**
     * Using the key's {@link Object#toString() toString()} method, generates a string suitable for
     * using as a filename.
     *
     * @param key
     * @return a string uniquely representing the the key.
     */
    public String hash(K key) {
        final byte[] ba;

        // MessageDigest isn't threadsafe, so we need to ensure it doesn't tread on itself.
        synchronized (hash) {
            hash.update(key.toString().getBytes());
            ba = hash.digest();
        }
        final BigInteger bi = new BigInteger(1, ba);
        final String result = bi.toString(16);
        if (result.length() % 2 != 0) {
            return "0" + result;
        }
        return result;

    }
}
