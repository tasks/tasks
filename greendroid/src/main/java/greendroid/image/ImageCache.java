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

import greendroid.app.GDApplication.OnLowMemoryListener;
import greendroid.util.GDUtils;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;

public class ImageCache implements OnLowMemoryListener {
    
    private final HashMap<String, SoftReference<Bitmap>> mSoftCache;
    
    public ImageCache(Context context) {
        mSoftCache = new HashMap<String, SoftReference<Bitmap>>();
        GDUtils.getGDApplication(context).registerOnLowMemoryListener(this);
    }
    
    public static ImageCache from(Context context) {
        return GDUtils.getImageCache(context);
    }
    
    public Bitmap get(String url) {
        final SoftReference<Bitmap> ref = mSoftCache.get(url);
        if (ref == null) {
            return null;
        }
        
        final Bitmap bitmap = ref.get();
        if (bitmap == null) {
            mSoftCache.remove(url);
        }
        
        return bitmap;
    }
    
    public void put(String url, Bitmap bitmap) {
        mSoftCache.put(url, new SoftReference<Bitmap>(bitmap));
    }
    
    public void flush() {
        mSoftCache.clear();
    }

    public void onLowMemoryReceived() {
        flush();
    }
}
