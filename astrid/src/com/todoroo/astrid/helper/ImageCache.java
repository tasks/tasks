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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

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
public class ImageCache extends DiskCache<String, Bitmap> {
	private static final String TAG = ImageCache.class.getSimpleName();

	static final boolean DEBUG = false;


	private long mIDCounter = 0;

	private static ImageCache mInstance;


	private final CompressFormat mCompressFormat;
	private final int mQuality;

	private final Resources mRes;

	// TODO make it so this is customizable on the instance level.
	/**
	 * Gets an instance of the cache.
	 *
	 * @param context
	 * @return an instance of the cache
	 */
	public static ImageCache getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new ImageCache(context, CompressFormat.JPEG, 85);
		}
		return mInstance;
	}

	private ImageCache(Context context, CompressFormat format, int quality) {
		super(context.getCacheDir(), null, getExtension(format));
		mRes = context.getResources();

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

	/**
	 * Blocking call to scale a local file. Scales using preserving aspect ratio
	 *
	 * @param localFile
	 *            local image file to be scaled
	 * @param width
	 *            maximum width
	 * @param height
	 *            maximum height
	 * @return the scaled image
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private static Bitmap scaleLocalImage(File localFile, int width, int height)
			throws ClientProtocolException, IOException {

		if (DEBUG){
			Log.d(TAG, "scaleLocalImage(" + localFile + ", "+ width +", "+ height + ")");
		}

		if (!localFile.exists()) {
			throw new IOException("local file does not exist: " + localFile);
		}
		if (!localFile.canRead()) {
			throw new IOException("cannot read from local file: " + localFile);
		}

		// the below borrowed from:
		// https://github.com/thest1/LazyList/blob/master/src/com/fedorvlasov/lazylist/ImageLoader.java

		// decode image size
		final BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;

		BitmapFactory.decodeStream(new FileInputStream(localFile), null, o);

		// Find the correct scale value. It should be the power of 2.
		//final int REQUIRED_WIDTH = width, REQUIRED_HEIGHT = height;
		int width_tmp = o.outWidth, height_tmp = o.outHeight;
		int scale = 1;
		while (true) {
			if (width_tmp / 2 <= width || height_tmp / 2 <= height) {
				break;
			}
			width_tmp /= 2;
			height_tmp /= 2;
			scale *= 2;
		}

		// decode with inSampleSize
		final BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		final Bitmap prescale = BitmapFactory.decodeStream(new FileInputStream(localFile), null, o2);

		if (prescale == null) {
			Log.e(TAG, localFile + " could not be decoded");
		}

		return prescale;
	}
	private static final boolean USE_APACHE_NC = true;

}
