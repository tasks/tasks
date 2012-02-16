package com.todoroo.astrid.helper;
/*
 * Copyright (C) 2011 MIT Mobile Experience Lab
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

import android.util.Log;

/**
 * A simple disk cache.
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 * @param <K> the key to store/retrieve the value
 * @param <V> the value that will be stored to disk
 */
// TODO add automatic cache cleanup so low disk conditions can be met
@SuppressWarnings("nls")
public abstract class DiskCache<K, V> {
    private static final String TAG = "DiskCache";

	private MessageDigest hash;

	private final File mCacheBase;
	private final String mCachePrefix, mCacheSuffix;

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
	 * @param cacheBase The base directory within which all the cache files will be stored.
	 * @param cachePrefix If you want a prefix to the filenames, place one here. Otherwise, pass null.
	 * @param cacheSuffix A suffix to the cache filename. Null is also ok here.
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
	}

	/**
	 * Gets the cache filename for the given key.
	 *
	 * @param key
	 * @return
	 */
	protected File getFile(K key){
		return new File(mCacheBase,
				(mCachePrefix != null ? mCachePrefix :"" )
				+ hash(key)
				+ (mCacheSuffix  != null ? mCacheSuffix : "")
			);
	}

	/**
	 * Writes the value stored in the cache to disk by calling {@link #toDisk(Object, Object, OutputStream)}.
	 *
	 * @param key The key to find the value.
	 * @param value the data to be written to disk.
	 */
	public void put(K key, V value) throws IOException, FileNotFoundException {
		final File saveHere = getFile(key);

		final OutputStream os = new FileOutputStream(saveHere);
		toDisk(key, value, os);
		os.close();
	}

	/**
	 * Writes the contents of the InputStream straight to disk. It is the
	 * caller's responsibility to ensure it's the same type as what would be
	 * written with {@link #toDisk(Object, Object, OutputStream)}
	 *
	 * @param key
	 * @param value
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void putRaw(K key, InputStream value) throws IOException, FileNotFoundException {
		final File saveHere = getFile(key);

		final OutputStream os = new FileOutputStream(saveHere);

		inputStreamToOutputStream(value, os);
		os.close();
	}

	/**
	 * Reads from an inputstream, dumps to an outputstream
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	static public void inputStreamToOutputStream(InputStream is, OutputStream os) throws IOException {
		final int bufsize = 8196 * 10;
		final byte[] cbuf = new byte[bufsize];

		for (int readBytes = is.read(cbuf, 0, bufsize);
			readBytes > 0;
			readBytes = is.read(cbuf, 0, bufsize)) {
			os.write(cbuf, 0, readBytes);
		}
	}

	public boolean move(K from, K to) {
        final File moveFrom = getFile(from);
        final File moveTo = getFile(to);
	    moveFrom.renameTo(moveTo);
	    return true;
	}


	/**
	 * Reads the value from disk using {@link #fromDisk(Object, InputStream)}.
	 *
	 * @param key
	 * @return The value for key or null if the key doesn't map to any existing entries.
	 */
	public V get(K key) throws IOException {
		final File readFrom = getFile(key);

		if (!readFrom.exists()){
			return null;
		}

		final InputStream is = new FileInputStream(readFrom);
		final V out = fromDisk(key, is);
		is.close();
		return out;
	}

	/**
	 * Checks the disk cache for a given key.
	 *
	 * @param key
	 * @return true if the disk cache contains the given key
	 */
	public boolean contains(K key) {
		final File readFrom = getFile(key);

		return readFrom.exists();
	}

	/**
	 * Removes the item from the disk cache.
	 *
	 * @param key
	 * @return true if the cached item has been removed or was already removed, false if it was not able to be removed.
	 */
	public boolean clear(K key){
		final File readFrom = getFile(key);

		if (!readFrom.exists()){
			return true;
		}

		return readFrom.delete();
	}

	/**
	 * Clears the cache files from disk.
	 *
	 * Note: this only clears files that match the given prefix/suffix.
	 *
	 * @return true if the operation succeeded without error. It is possible that it will fail and the cache ends up being partially cleared.
	 */
	public boolean clear() {
		boolean success = true;

		for (final File cacheFile : mCacheBase.listFiles(mCacheFileFilter)){
			if (!cacheFile.delete()){
				// throw new IOException("cannot delete cache file");
				Log.e(TAG, "error deleting "+ cacheFile);
				success = false;
			}
		}
		return success;
	}

	/**
	 * @return the size of the cache as it is on disk.
	 */
	public int getCacheSize(){
		return mCacheBase.listFiles(mCacheFileFilter).length;
	}

	private final CacheFileFilter mCacheFileFilter = new CacheFileFilter();

	private class CacheFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			final String path = pathname.getName();
			return (mCachePrefix != null ? path.startsWith(mCachePrefix) : true)
				&& (mCacheSuffix != null ? path.endsWith(mCacheSuffix)   : true);
		}
	};

	/**
	 * Implement this to do the actual disk writing. Do not close the OutputStream; it will be closed for you.
	 *
	 * @param key
	 * @param in
	 * @param out
	 */
	protected abstract void toDisk(K key, V in, OutputStream out);

	/**
	 * Implement this to do the actual disk reading.
	 * @param key
	 * @param in
	 * @return a new instance of {@link V} containing the contents of in.
	 */
	protected abstract V fromDisk(K key, InputStream in);

	/**
	 * Using the key's {@link Object#toString() toString()} method, generates a string suitable for using as a filename.
	 *
	 * @param key
	 * @return a string uniquely representing the the key.
	 */
	public String hash(K key){
		final byte[] ba;
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
