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
 * A DrawableItem displays a single Drawable on the left of the item cell and a
 * description text on the right. A DrawableItem takes care of adapting itself
 * depending on the presence of its Drawable.
 * 
 * @author Cyril Mottier
 */
public class DrawableItem extends TextItem {

    /**
     * The resource identifier for the drawable.
     */
    public int drawableId;

    /**
     * @hide
     */
    public DrawableItem() {
        this(null);
    }

    /**
     * Constructs a new DrawableItem that has no Drawable and displays the given
     * text. Used as it, a DrawableItem is very similar to a TextItem
     * 
     * @param text The text of this DrawableItem
     */
    public DrawableItem(String text) {
        this(text, 0);
    }

    /**
     * Constructs a new DrawableItem that has no Drawable and displays the given
     * text. Used as it, a DrawableItem is very similar to a TextItem
     * 
     * @param text The text of this DrawableItem
     * @param drawableId The resource identifier of the Drawable
     */
    public DrawableItem(String text, int drawableId) {
        super(text);
        this.drawableId = drawableId;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_drawable_item_view, parent);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException,
            IOException {
        super.inflate(r, parser, attrs);

        TypedArray a = r.obtainAttributes(attrs, R.styleable.DrawableItem);
        drawableId = a.getResourceId(R.styleable.DrawableItem_drawable, 0);
        a.recycle();
    }

}
