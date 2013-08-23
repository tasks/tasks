package edu.mit.mobile.android.imagecache;

/*
 * Copyright (C) 2011-2012 MIT Mobile Experience Lab
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

/**
 * <p>
 * An image download-and-cacher that also knows how to efficiently generate thumbnails of various
 * sizes.
 * </p>
 *
 * <p>
 * The cache is shared with the entire process, so make sure you
 * {@link #registerOnImageLoadListener(OnImageLoadListener)} and
 * {@link #unregisterOnImageLoadListener(OnImageLoadListener)} any load listeners in your
 * activities.
 * </p>
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 */
public class ImageCache extends DiskCache<String, Bitmap> {
    private static final String TAG = ImageCache.class.getSimpleName();

    static final boolean DEBUG = false;

    // whether to use Apache HttpClient or URL.openConnection()
    private static final boolean USE_APACHE_NC = true;

    // the below settings are copied from AsyncTask.java
    private static final int CORE_POOL_SIZE = 5; // thread
    private static final int MAXIMUM_POOL_SIZE = 128; // thread
    private static final int KEEP_ALIVE_TIME = 1; // second

    private final HashSet<OnImageLoadListener> mImageLoadListeners = new HashSet<ImageCache.OnImageLoadListener>();

    public static final int DEFAULT_CACHE_SIZE = (24 /* MiB */* 1024 * 1024); // in bytes

    private DrawableMemCache<String> mMemCache = new DrawableMemCache<String>(DEFAULT_CACHE_SIZE);

    private Integer mIDCounter = 0;

    private static ImageCache mInstance;

    // this is a custom Executor, as we want to have the tasks loaded in FILO order. FILO works
    // particularly well when scrolling with a ListView.
    private final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new PriorityBlockingQueue<Runnable>());

    // ignored as SparseArray isn't thread-safe
    @SuppressLint("UseSparseArrays")
    private final Map<Integer, Runnable> jobs = Collections
            .synchronizedMap(new HashMap<Integer, Runnable>());

    private final HttpClient hc;

    private CompressFormat mCompressFormat;
    private int mQuality;

    private final Resources mRes;

    private static final int MSG_IMAGE_LOADED = 100;

    private final KeyedLock<String> mDownloading = new KeyedLock<String>();

    private static class ImageLoadHandler extends Handler {
        private final ImageCache mCache;

        public ImageLoadHandler(ImageCache cache) {
            super();
            mCache = cache;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_IMAGE_LOADED:
                    mCache.notifyListeners((LoadResult) msg.obj);
                    break;
            }
        };
    }

    private final ImageLoadHandler mHandler = new ImageLoadHandler(this);

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

    /**
     * Generally, it's best to use the shared image cache using {@link #getInstance(Context)}. Use
     * this if you want to customize a cache or keep it separate.
     *
     * @param context
     * @param format
     * @param quality
     */
    public ImageCache(Context context, CompressFormat format, int quality) {
        super(context.getCacheDir(), null, getExtension(format));
        if (USE_APACHE_NC) {
            hc = getHttpClient();
        } else {
            hc = null;
        }

        mRes = context.getResources();

        mCompressFormat = format;
        mQuality = quality;
    }

    /**
     * Sets the compression format for resized images.
     *
     * @param format
     */
    public void setCompressFormat(CompressFormat format) {
        mCompressFormat = format;
    }

    /**
     * Set the image quality. Hint to the compressor, 0-100. 0 meaning compress for small size, 100
     * meaning compress for max quality. Some formats, like PNG which is lossless, will ignore the
     * quality setting
     *
     * @param quality
     */
    public void setQuality(int quality) {
        mQuality = quality;
    }

    /**
     * Sets the maximum size of the memory cache. Note, this will clear the memory cache.
     *
     * @param maxSize
     *            the maximum size of the memory cache in bytes.
     */
    public void setMemCacheMaxSize(int maxSize) {
        mMemCache = new DrawableMemCache<String>(maxSize);
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
     * If loading a number of images where you don't have a unique ID to represent the individual
     * load, this can be used to generate a sequential ID.
     *
     * @return a new unique ID
     */
    public int getNewID() {
        synchronized (mIDCounter) {
            return mIDCounter++;
        }
    }

    @Override
    protected Bitmap fromDisk(String key, InputStream in) {

        if (DEBUG) {
            Log.d(TAG, "disk cache hit for key " + key);
        }
        try {
            final Bitmap image = BitmapFactory.decodeStream(in);
            return image;

        } catch (final OutOfMemoryError oom) {
            oomClear();
            return null;
        }
    }

    @Override
    protected void toDisk(String key, Bitmap image, OutputStream out) {
        if (DEBUG) {
            Log.d(TAG, "disk cache write for key " + key);
        }
        if (image != null) {
            if (!image.compress(mCompressFormat, mQuality, out)) {
                Log.e(TAG, "error writing compressed image to disk for key " + key);
            }
        } else {
            Log.e(TAG, "Ignoring attempt to write null image to disk cache");
        }
    }

    /**
     * Gets an instance of AndroidHttpClient if the devices has it (it was introduced in 2.2), or
     * falls back on a http client that should work reasonably well.
     *
     * @return a working instance of an HttpClient
     */
    private HttpClient getHttpClient() {
        HttpClient ahc;
        try {
            final Class<?> ahcClass = Class.forName("android.net.http.AndroidHttpClient");
            final Method newInstance = ahcClass.getMethod("newInstance", String.class);
            ahc = (HttpClient) newInstance.invoke(null, "ImageCache");

        } catch (final ClassNotFoundException e) {
            DefaultHttpClient dhc = new DefaultHttpClient();
            final HttpParams params = dhc.getParams();
            dhc = null;

            params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20 * 1000);

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

            final ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params,
                    registry);
            ahc = new DefaultHttpClient(manager, params);

        } catch (final NoSuchMethodException e) {

            final RuntimeException re = new RuntimeException("Programming error");
            re.initCause(e);
            throw re;

        } catch (final IllegalAccessException e) {
            final RuntimeException re = new RuntimeException("Programming error");
            re.initCause(e);
            throw re;

        } catch (final InvocationTargetException e) {
            final RuntimeException re = new RuntimeException("Programming error");
            re.initCause(e);
            throw re;
        }
        return ahc;
    }

    /**
     * <p>
     * Registers an {@link OnImageLoadListener} with the cache. When an image is loaded
     * asynchronously either directly by way of {@link #scheduleLoadImage(int, Uri, int, int)} or
     * indirectly by {@link #loadImage(int, Uri, int, int)}, any registered listeners will get
     * called.
     * </p>
     *
     * <p>
     * This should probably be called from {@link Activity#onResume()}.
     * </p>
     *
     * @param onImageLoadListener
     */
    public void registerOnImageLoadListener(OnImageLoadListener onImageLoadListener) {
        mImageLoadListeners.add(onImageLoadListener);
    }

    /**
     * <p>
     * Unregisters the listener with the cache. This will not cancel any pending load requests.
     * </p>
     *
     * <p>
     * This should probably be called from {@link Activity#onPause()}.
     * </p>
     *
     * @param onImageLoadListener
     */
    public void unregisterOnImageLoadListener(OnImageLoadListener onImageLoadListener) {
        mImageLoadListeners.remove(onImageLoadListener);
    }

    private class LoadResult {
        public LoadResult(int id, Uri image, Drawable drawable) {
            this.id = id;
            this.drawable = drawable;
            this.image = image;
        }

        final Uri image;
        final int id;
        final Drawable drawable;
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
     * Gets the given key as a drawable, retrieving it from memory cache if it's present.
     *
     * @param key
     *            a key generated by {@link #getKey(Uri)} or {@link #getKey(Uri, int, int)}
     * @return the drawable if it's in the memory cache or null.
     */
    public Drawable getDrawable(String key) {
        final Drawable img = mMemCache.get(key);
        if (img != null) {
            if (DEBUG) {
                Log.d(TAG, "mem cache hit for key " + key);
            }
            touchKey(key);
            return img;
        }

        return null;
    }

    /**
     * Puts a drawable into memory cache.
     *
     * @param key
     *            a key generated by {@link #getKey(Uri)} or {@link #getKey(Uri, int, int)}
     * @param drawable
     */
    public void putDrawable(String key, Drawable drawable) {
        mMemCache.put(key, drawable);
    }

    /**
     * A blocking call to get an image. If it's in the cache, it'll return the drawable immediately.
     * Otherwise it will download, scale, and cache the image before returning it. For non-blocking
     * use, see {@link #loadImage(int, Uri, int, int)}
     *
     * @param uri
     * @param width
     * @param height
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws ImageCacheException
     */
    public Drawable getImage(Uri uri, int width, int height) throws ClientProtocolException,
            IOException, ImageCacheException {

        final String scaledKey = getKey(uri, width, height);

        mDownloading.lock(scaledKey);

        try {
            Drawable d = getDrawable(scaledKey);
            if (d != null) {
                return d;
            }

            Bitmap bmp = get(scaledKey);

            if (bmp == null) {
                if ("file".equals(uri.getScheme())) {
                    bmp = scaleLocalImage(new File(uri.getPath()), width, height);
                } else {
                    final String sourceKey = getKey(uri);

                    mDownloading.lock(sourceKey);

                    try {
                        if (!contains(sourceKey)) {
                            downloadImage(sourceKey, uri);
                        }
                    } finally {
                        mDownloading.unlock(sourceKey);
                    }

                    bmp = scaleLocalImage(getFile(sourceKey), width, height);
                    if (bmp == null) {
                        clear(sourceKey);
                    }
                }
                put(scaledKey, bmp);

            }
            if (bmp == null) {
                throw new ImageCacheException("got null bitmap from request to scale");

            }
            d = new BitmapDrawable(mRes, bmp);
            putDrawable(scaledKey, d);

            return d;

        } finally {
            mDownloading.unlock(scaledKey);
        }
    }

    private final SparseArray<String> mKeyCache = new SparseArray<String>();

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
        // collisions are possible, but unlikely.
        final int hashId = uri.hashCode() + width + height * 10000;

        String key = mKeyCache.get(hashId);
        if (key == null) {
            key = uri.buildUpon().appendQueryParameter("width", String.valueOf(width))
                    .appendQueryParameter("height", String.valueOf(height)).build().toString();
            mKeyCache.put(hashId, key);
        }
        return key;
    }

    @Override
    public synchronized boolean clear() {
        final boolean success = super.clear();

        mMemCache.evictAll();

        mKeyCache.clear();

        return success;
    }

    @Override
    public synchronized boolean clear(String key) {
        final boolean success = super.clear(key);

        mMemCache.remove(key);

        return success;
    }

    private class ImageLoadTask implements Runnable, Comparable<ImageLoadTask> {
        private final int id;
        private final Uri uri;
        private final int width;
        private final int height;
        private final long when = System.nanoTime();

        public ImageLoadTask(int id, Uri image, int width, int height) {
            this.id = id;
            this.uri = image;
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {

            if (DEBUG) {
                Log.d(TAG, "ImageLoadTask.doInBackground(" + id + ", " + uri + ", " + width + ", "
                        + height + ")");
            }

            try {
                final LoadResult result = new LoadResult(id, uri, getImage(uri, width, height));
                synchronized (jobs) {
                    if (jobs.containsKey(id)) {
                        // Job still valid.
                        jobs.remove(id);
                        mHandler.obtainMessage(MSG_IMAGE_LOADED, result).sendToTarget();
                    }
                }

                // TODO this exception came about, no idea why:
                // java.lang.IllegalArgumentException: Parser may not be null
            } catch (final IllegalArgumentException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            } catch (final OutOfMemoryError oom) {
                oomClear();
            } catch (final ClientProtocolException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            } catch (final IOException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            } catch (final ImageCacheException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }

        @Override
        public int compareTo(ImageLoadTask another) {
            return Long.valueOf(another.when).compareTo(when);
        };
    }

    private void oomClear() {
        Log.w(TAG, "out of memory, clearing mem cache");
        mMemCache.evictAll();
    }

    /**
     * Checks the cache for an image matching the given criteria and returns it. If it isn't
     * immediately available, calls {@link #scheduleLoadImage}.
     *
     * @param id
     *            An ID to keep track of image load requests. For one-off loads, this can just be
     *            the ID of the {@link ImageView}. Otherwise, an unique ID can be acquired using
     *            {@link #getNewID()}.
     *
     * @param image
     *            the image to be loaded. Can be a local file or a network resource.
     * @param width
     *            the maximum width of the resulting image
     * @param height
     *            the maximum height of the resulting image
     * @return the cached bitmap if it's available immediately or null if it needs to be loaded
     *         asynchronously.
     */
    public Drawable loadImage(int id, Uri image, int width, int height) throws IOException {
        if (DEBUG) {
            Log.d(TAG, "loadImage(" + id + ", " + image + ", " + width + ", " + height + ")");
        }
        final Drawable res = getDrawable(getKey(image, width, height));
        if (res == null) {
            if (DEBUG) {
                Log.d(TAG,
                        "Image not found in memory cache. Scheduling load from network / disk...");
            }
            scheduleLoadImage(id, image, width, height);
        }
        return res;
    }

    /**
     * Deprecated to make IDs ints instead of longs. See {@link #loadImage(int, Uri, int, int)}.
     *
     * @param id
     * @param image
     * @param width
     * @param height
     * @return
     * @throws IOException
     */
    @Deprecated
    public Drawable loadImage(long id, Uri image, int width, int height) throws IOException {
        return loadImage(id, image, width, height);
    }

    /**
     * Schedules a load of the given image. When the image has finished loading and scaling, all
     * registered {@link OnImageLoadListener}s will be called.
     *
     * @param id
     *            An ID to keep track of image load requests. For one-off loads, this can just be
     *            the ID of the {@link ImageView}. Otherwise, an unique ID can be acquired using
     *            {@link #getNewID()}.
     *
     * @param image
     *            the image to be loaded. Can be a local file or a network resource.
     * @param width
     *            the maximum width of the resulting image
     * @param height
     *            the maximum height of the resulting image
     */
    public void scheduleLoadImage(int id, Uri image, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "executing new ImageLoadTask in background...");
        }
        final ImageLoadTask imt = new ImageLoadTask(id, image, width, height);

        jobs.put(id, imt);
        mExecutor.execute(imt);
    }

    /**
     * Deprecated in favour of {@link #scheduleLoadImage(int, Uri, int, int)}.
     *
     * @param id
     * @param image
     * @param width
     * @param height
     */
    @Deprecated
    public void scheduleLoadImage(long id, Uri image, int width, int height) {
        scheduleLoadImage(id, image, width, height);
    }

    /**
     * Cancels all the asynchronous image loads. Note: currently does not function properly.
     *
     */
    public void cancelLoads() {
        jobs.clear();
        mExecutor.getQueue().clear();
    }

    public void cancel(int id) {
        synchronized (jobs) {
            final Runnable job = jobs.get(id);
            if (job != null) {
                jobs.remove(id);
                mExecutor.remove(job);
                if (DEBUG) {
                    Log.d(TAG, "removed load id " + id);
                }
            }
        }
    }

    /**
     * Deprecated in favour of {@link #cancel(int)}.
     *
     * @param id
     */
    @Deprecated
    public void cancel(long id) {
        cancel(id);
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

        if (DEBUG) {
            Log.d(TAG, "scaleLocalImage(" + localFile + ", " + width + ", " + height + ")");
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
        final Bitmap prescale = BitmapFactory
                .decodeStream(new FileInputStream(localFile), null, o2);

        if (prescale == null) {
            Log.e(TAG, localFile + " could not be decoded");
        } else if (DEBUG) {
            Log.d(TAG, "Successfully completed scaling of " + localFile + " to " + width + "x"
                    + height);
        }

        return prescale;
    }

    /**
     * Blocking call to download an image. The image is placed directly into the disk cache at the
     * given key.
     *
     * @param uri
     *            the location of the image
     * @return a decoded bitmap
     * @throws ClientProtocolException
     *             if the HTTP response code wasn't 200 or any other HTTP errors
     * @throws IOException
     */
    protected void downloadImage(String key, Uri uri) throws ClientProtocolException, IOException {
        if (DEBUG) {
            Log.d(TAG, "downloadImage(" + key + ", " + uri + ")");
        }
        if (USE_APACHE_NC) {
            final HttpGet get = new HttpGet(uri.toString());
            final HttpParams params = get.getParams();
            params.setParameter(ClientPNames.HANDLE_REDIRECTS, true);

            final HttpResponse hr = hc.execute(get);
            final StatusLine hs = hr.getStatusLine();
            if (hs.getStatusCode() != 200) {
                throw new HttpResponseException(hs.getStatusCode(), hs.getReasonPhrase());
            }

            final HttpEntity ent = hr.getEntity();

            // TODO I think this means that the source file must be a jpeg. fix this.
            try {

                putRaw(key, ent.getContent());
                if (DEBUG) {
                    Log.d(TAG, "source file of " + uri + " saved to disk cache at location "
                            + getFile(key).getAbsolutePath());
                }
            } finally {
                ent.consumeContent();
            }
        } else {
            final URLConnection con = new URL(uri.toString()).openConnection();
            putRaw(key, con.getInputStream());
            if (DEBUG) {
                Log.d(TAG,
                        "source file of " + uri + " saved to disk cache at location "
                                + getFile(key).getAbsolutePath());
            }
        }

    }

    private void notifyListeners(LoadResult result) {
        for (final OnImageLoadListener listener : mImageLoadListeners) {
            listener.onImageLoaded(result.id, result.image, result.drawable);
        }
    }

    /**
     * Implement this and register it using
     * {@link ImageCache#registerOnImageLoadListener(OnImageLoadListener)} to be notified when
     * asynchronous image loads have completed.
     *
     * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
     *
     */
    public interface OnImageLoadListener {
        /**
         * Called when the image has been loaded and scaled.
         *
         * @param id
         *            the ID provided by {@link ImageCache#loadImage(int, Uri, int, int)} or
         *            {@link ImageCache#scheduleLoadImage(int, Uri, int, int)}
         * @param imageUri
         *            the uri of the image that was originally requested
         * @param image
         *            the loaded and scaled image
         */
        public void onImageLoaded(int id, Uri imageUri, Drawable image);
    }
}
