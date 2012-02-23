package com.todoroo.astrid.helper;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;


/*
 * Subclass of greendroid.widget.AsyncImageView, so that we can cache the image locally when user is offline
 * IMPORTANT: cannot load a cached image by setting the url in an xml file. --ImageDiskCache object is created after object is loaded from xml
 */
public class AsyncImageView extends greendroid.widget.AsyncImageView {

    private final ImageDiskCache imageDiskCache;
    private Bitmap cacheImage;
    private String cacheURL = ""; //$NON-NLS-1$
    public AsyncImageView(Context context) {
        super(context);

        imageDiskCache = ImageDiskCache.getInstance();
    }
    public AsyncImageView(Context context, AttributeSet set) {
        super(context, set);
        imageDiskCache = ImageDiskCache.getInstance();
    }
    public AsyncImageView(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
        imageDiskCache = ImageDiskCache.getInstance();
    }
    @Override
    public void setUrl(String url) {
        if (cacheImage != null && cacheURL.equals(url) && !TextUtils.isEmpty(url)) {
            setImageBitmap(cacheImage);
            return;
        }
        else if(imageDiskCache != null && imageDiskCache.contains(url)) {
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
        Bitmap b = Bitmap.createBitmap(getDrawingCache());
        setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }

}
