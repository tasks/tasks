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
package greendroid.widget.item;

import greendroid.widget.itemview.ItemView;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.cyrilmottier.android.greendroid.R;

/**
 * A ThumbnailItem item is a complex item that wraps a drawable and two strings
 * : a title and a subtitle. The representation of that item is quite common to
 * Android users: The drawable is on the left of the item view and on the right
 * the title and the subtitle are displayed like a {@link SubtitleItem}.
 * 
 * @author Cyril Mottier
 */
public class ThumbnailItem extends SubtitleItem {

    /**
     * The resource ID for the drawable.
     */
    public int drawableId;

    /**
     * @hide
     */
    public ThumbnailItem() {
    }

    /**
     * @param text
     * @param subtitle
     * @param drawableId
     */
    public ThumbnailItem(String text, String subtitle, int drawableId) {
        super(text, subtitle);
        this.drawableId = drawableId;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_thumbnail_item_view, parent);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException,
            IOException {
        super.inflate(r, parser, attrs);

        TypedArray a = r.obtainAttributes(attrs, R.styleable.ThumbnailItem);
        drawableId = a.getResourceId(R.styleable.ThumbnailItem_thumbnail, drawableId);
        a.recycle();
    }

}
