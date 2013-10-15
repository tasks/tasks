/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.todoroo.andlib.service.ContextManager;

import java.io.IOException;

import edu.mit.mobile.android.imagecache.ImageCache;

public class AsyncImageView extends ImageView {

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
    }

    private static volatile ImageCache imageCacheInstance = null;

    public static ImageCache getImageCache() {
        if (imageCacheInstance == null) {
            synchronized(AsyncImageView.class) {
                if (imageCacheInstance == null) {
                    try {
                        if (Looper.myLooper() == null) {
                            Looper.prepare();
                        }
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
