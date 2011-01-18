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
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ListView;

import com.cyrilmottier.android.greendroid.R;

/**
 * Acts as a separator between important {@link ListView} sections. A separator
 * display a text on a single line.
 * 
 * @author Cyril Mottier
 */
public class SeparatorItem extends TextItem {

    /**
     * @hide
     */
    public SeparatorItem() {
        this(null);
    }

    /**
     * Constructs a SeparatorItem made of the given text
     * 
     * @param text The text for this SeparatorItem
     */
    public SeparatorItem(String text) {
        super(text);
        enabled = false;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_separator_item_view, parent);
    }

}
