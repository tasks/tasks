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
 * A TextItem is a very basic item that only contains a single text. This text
 * will be displayed on a single line onto the screen.
 * 
 * @author Cyril Mottier
 */
public class TextItem extends Item {

    /**
     * The item's text.
     */
    public String text;

    public TextItem() {
    }

    /**
     * Constructs a new TextItem with the specified text.
     * 
     * @param text The text used to create this item.
     */
    public TextItem(String text) {
        this.text = text;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_text_item_view, parent);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException,
            IOException {
        super.inflate(r, parser, attrs);

        TypedArray a = r.obtainAttributes(attrs, R.styleable.TextItem);
        text = a.getString(R.styleable.TextItem_text);
        a.recycle();
    }

}
