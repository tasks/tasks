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
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;

/**
 * <p>
 * An image download-and-cacher that also knows how to efficiently generate
 * thumbnails of various sizes.
 * </p>
 *
 * <p>
 * The cache is shared with the entire process, so make sure you
 * {@link #registerOnImageLoadListener(OnImageLoadListener)} and
 * {@link #unregisterOnImageLoadListener(OnImageLoadListener)} any load
 * listeners in your activities.
 * </p>
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 */
@SuppressWarnings("nls")
public class ImageDiskCache extends DiskCache<String, Bitmap> {
	private static final String TAG = ImageDiskCache.class.getSimpleName();

	static final boolean DEBUG = false;


	private long mIDCounter = 0;

	private static ImageDiskCache mInstance;


	private final CompressFormat mCompressFormat;
	private final int mQuality;

    public static ImageDiskCache getInstance() {
        if (mInstance == null) {
            mInstance = new ImageDiskCache(ContextManager.getContext().getCacheDir(), CompressFormat.JPEG, 85);
        }
        return mInstance;
    }

	private ImageDiskCache(File file, CompressFormat format, int quality) {
		super(file, null, getExtension(format));

		mCompressFormat = format;
		mQuality = quality;
	}
    private static String getExtension(CompressFormat format) {
		String extension;
		switch (format) {
		case JPEG:
			extension = ".jpg";
			break;

		case PNG:
			extension = ".png";
			break;

		default:
			throw new IllegalArgumentException();
		}

		return extension;
	}

	/**
	 * If loading a number of images where you don't have a unique ID to
	 * represent the individual load, this can be used to generate a sequential
	 * ID.
	 *
	 * @return a new unique ID
	 */
	public synchronized long getNewID() {
		return mIDCounter++;
	}

	@Override
	protected Bitmap fromDisk(String key, InputStream in) {

		if (DEBUG) {
			Log.d(TAG, "disk cache hit");
		}
		try {
			final Bitmap image = BitmapFactory.decodeStream(in);
			return image;

		} catch (final OutOfMemoryError oom) {
			return null;
		}
	}

	@Override
	protected void toDisk(String key, Bitmap image, OutputStream out) {
		if (DEBUG) {
			Log.d(TAG, "cache write for key " + key);
		}
		if (image != null) {
			if (!image.compress(mCompressFormat, mQuality, out)) {
				Log.e(TAG, "error writing compressed image to disk for key "
						+ key);
			}
		} else {
			Log.e(TAG, "attempting to write null image to cache");
		}
	}


	/**
	 * @param uri
	 *            the image uri
	 * @return a key unique to the given uri
	 */
	public String getKey(Uri uri) {
		return uri.toString();
	}


	/**
	 * Returns an opaque cache key representing the given uri, width and height.
	 *
	 * @param uri
	 *            an image uri
	 * @param width
	 *            the desired image max width
	 * @param height
	 *            the desired image max height
	 * @return a cache key unique to the given parameters
	 */
	public String getKey(Uri uri, int width, int height) {
		return uri.buildUpon()
				.appendQueryParameter("width", String.valueOf(width))
				.appendQueryParameter("height", String.valueOf(height)).build()
				.toString();
	}



	/**
	 * Cancels all the asynchronous image loads.
	 * Note: currently does not function properly.
	 *
	 */
	public void cancelLoads() {
		// TODO actually make it possible to cancel tasks
	}

}
