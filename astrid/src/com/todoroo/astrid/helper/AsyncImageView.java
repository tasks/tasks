/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.todoroo.andlib.service.ContextManager;

import edu.mit.mobile.android.imagecache.ImageCache;

/**
 * Subclass of greendroid.widget.AsyncImageView, so that we can cache the image
 * locally when user is offline
 *
 * IMPORTANT: cannot load a cached image by setting the url in an xml file.
 * ImageDiskCache object is created after object is loaded from xml
 */
public class AsyncImageView extends greendroid.widget.AsyncImageView {

    private final ImageCache imageDiskCache;
    private Bitmap cacheImage;
    private String cacheURL = ""; //$NON-NLS-1$
    public AsyncImageView(Context context) {
        super(context);
        imageDiskCache = getImageCache();
    }
    public AsyncImageView(Context context, AttributeSet set) {
        super(context, set);
        imageDiskCache = getImageCache();
    }
    public AsyncImageView(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
        imageDiskCache = getImageCache();
    }
    @Override
    public void setUrl(String url) {
        if (cacheImage != null && cacheURL.equals(url) && !TextUtils.isEmpty(url)) {
            setImageBitmap(cacheImage);
            return;
        } else if (url != null && imageDiskCache != null && imageDiskCache.contains(url)) {
            try {
                cacheImage = imageDiskCache.get(url);
                setImageBitmap(cacheImage);
                cacheURL = url;
                return;
            } catch (IOException e) {
                //
            }
        }
        super.setUrl(url);
    }

    public Bitmap getImageBitmap() {
        setDrawingCacheEnabled(true);

        // this is the important code :)
        // Without it the view will have a dimension of 0,0 and the bitmap will be null
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());

        buildDrawingCache(true);
        Bitmap drawingCache = getDrawingCache();
        if (drawingCache == null)
            return null;
        Bitmap b = Bitmap.createBitmap(getDrawingCache());
        setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }

    private static volatile ImageCache imageCacheInstance = null;

    public static ImageCache getImageCache() {
        if (imageCacheInstance == null) {
            synchronized(AsyncImageView.class) {
                if (imageCacheInstance == null) {
                    try {
                        if (Looper.myLooper() == null)
                            Looper.prepare();
                    } catch (Exception e) {
                        // Ignore
                    }
                    imageCacheInstance = new ImageCache(ContextManager.getContext(), CompressFormat.JPEG, 85);
                }
            }
        }
        return imageCacheInstance;
    }

}
