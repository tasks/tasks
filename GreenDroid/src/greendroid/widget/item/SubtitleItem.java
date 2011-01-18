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
 * An Item that contains two Strings : a text and a subtitle. The representation
 * of this Item is a view containing two lines of text. Uf you want to be sure,
 * the subtitle can occupy more than a single line, please use a
 * {@link SubtextItem}
 * 
 * @author Cyril Mottier
 */
public class SubtitleItem extends TextItem {

    /**
     * The subtitle of this item
     */
    public String subtitle;

    /**
     * @hide
     */
    public SubtitleItem() {
    }

    /**
     * Constructs a new SubtitleItem with the specified text and subtitle.
     * 
     * @param text The text for this item
     * @param subtitle The item's subtitle
     */
    public SubtitleItem(String text, String subtitle) {
        super(text);
        this.subtitle = subtitle;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_subtitle_item_view, parent);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException,
            IOException {
        super.inflate(r, parser, attrs);

        TypedArray a = r.obtainAttributes(attrs, R.styleable.SubtitleItem);
        subtitle = a.getString(R.styleable.SubtitleItem_subtitle);
        a.recycle();
    }
}
