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

import com.cyrilmottier.android.greendroid.R;

/**
 * A LongTextItem is very similar to a regular {@link TextItem}. The only
 * difference is it may display the text on several lines.
 * 
 * @author Cyril Mottier
 */
public class LongTextItem extends TextItem {

    public LongTextItem() {
        this(null);
    }

    public LongTextItem(String text) {
        super(text);
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_long_text_item_view, parent);
    }

}
