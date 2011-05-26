/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
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
package greendroid.image;

import greendroid.image.ImageLoader.ImageLoaderCallback;

import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * An {@link ImageRequest} may be used to request an image from the network. The
 * process of requesting for an image is done in three steps:
 * <ul>
 * <li>Instantiate a new {@link ImageRequest}</li>
 * <li>Call {@link #load(Context)} to start loading the image</li>
 * <li>Listen to loading state changes using a {@link ImageRequestCallback}</li>
 * </ul>
 * 
 * @author Cyril Mottier
 */
public class ImageRequest {

    /**
     * @author Cyril Mottier
     */
    public static interface ImageRequestCallback {
        void onImageRequestStarted(ImageRequest request);

        void onImageRequestFailed(ImageRequest request, Throwable throwable);

        void onImageRequestEnded(ImageRequest request, Bitmap image);

        void onImageRequestCancelled(ImageRequest request);
    }

    private static ImageLoader sImageLoader;

    private Future<?> mFuture;
    private String mUrl;
    private ImageRequestCallback mCallback;
    private ImageProcessor mBitmapProcessor;
    private BitmapFactory.Options mOptions;

    public ImageRequest(String url, ImageRequestCallback callback) {
        this(url, callback, null);
    }
    
    public ImageRequest(String url, ImageRequestCallback callback, ImageProcessor bitmapProcessor) {
    	this(url, callback, bitmapProcessor, null);
    }
    
    public ImageRequest(String url, ImageRequestCallback callback, ImageProcessor bitmapProcessor, BitmapFactory.Options options) {
        mUrl = url;
        mCallback = callback;
        mBitmapProcessor = bitmapProcessor;
        mOptions = options;
    }

    public void setImageRequestCallback(ImageRequestCallback callback) {
        mCallback = callback;
    }
    
    public String getUrl() {
        return mUrl;
    }

    public void load(Context context) {
        if (mFuture == null) {
            if (sImageLoader == null) {
                sImageLoader = new ImageLoader(context);
            }
            mFuture = sImageLoader.loadImage(mUrl, new InnerCallback(), mBitmapProcessor, mOptions);
        }
    }

    public void cancel() {
        if (!isCancelled()) {
            // Here we do not want to force the task to be interrupted. Indeed,
            // it may be useful to keep the result in a cache for a further use
            mFuture.cancel(false);
            if (mCallback != null) {
                mCallback.onImageRequestCancelled(this);
            }
        }
    }
    
    public final boolean isCancelled() {
        return mFuture.isCancelled();
    }

    private class InnerCallback implements ImageLoaderCallback {
        
        public void onImageLoadingStarted(ImageLoader loader) {
            if (mCallback != null) {
                mCallback.onImageRequestStarted(ImageRequest.this);
            }
        }

        public void onImageLoadingEnded(ImageLoader loader, Bitmap bitmap) {
            if (mCallback != null && !isCancelled()) {
                mCallback.onImageRequestEnded(ImageRequest.this, bitmap);
            }
            mFuture = null;
        }

        public void onImageLoadingFailed(ImageLoader loader, Throwable exception) {
            if (mCallback != null && !isCancelled()) {
                mCallback.onImageRequestFailed(ImageRequest.this, exception);
            }
            mFuture = null;
        }
    }

}
