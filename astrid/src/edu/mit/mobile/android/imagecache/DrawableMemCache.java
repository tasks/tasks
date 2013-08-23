package edu.mit.mobile.android.imagecache;

/*
 * Copyright (C) 2012 MIT Mobile Experience Lab
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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;

public class DrawableMemCache<T> extends LruCache<T, Drawable> {

    @SuppressWarnings("unused")
    private static final String TAG = DrawableMemCache.class.getSimpleName();

    public DrawableMemCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(T key, Drawable value) {
        int size = 0;
        if (value instanceof BitmapDrawable) {
            final Bitmap b = ((BitmapDrawable) value).getBitmap();
            if (b != null) {
                size = b.getRowBytes() * b.getHeight();
            }
        }
        return size;
    }
}
