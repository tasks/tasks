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
 * Progress indicator that displays a centered text with a circular progress bar
 * when something is in progress.@
 * 
 * @author Cyril Mottier
 */
public class ProgressItem extends TextItem {

    private static final boolean DEFAULT_IS_IN_PROGRESS = false;

    /**
     * The state of this item. When set to true, a circular progress bar
     * indicates something is going on/being computed.
     */
    public boolean isInProgress;

    /**
     * @hide
     */
    public ProgressItem() {
        this(null);
    }

    /**
     * Constructs a ProgressItem with the given text. By default, the circular
     * progress bar is not visible ... which indicates nothing is currently in
     * progress.
     * 
     * @param text The text for this item
     */
    public ProgressItem(String text) {
        this(text, DEFAULT_IS_IN_PROGRESS);
    }

    /**
     * Constructs a ProgressItem with the given text and state.
     * 
     * @param text The text for this item
     * @param isInProgress The state for this item
     */
    public ProgressItem(String text, boolean isInProgress) {
        super(text);
        this.isInProgress = isInProgress;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_progress_item_view, parent);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException,
            IOException {
        super.inflate(r, parser, attrs);

        TypedArray a = r.obtainAttributes(attrs, R.styleable.ProgressItem);
        isInProgress = a.getBoolean(R.styleable.ProgressItem_isInProgress, DEFAULT_IS_IN_PROGRESS);
        a.recycle();
    }

}
