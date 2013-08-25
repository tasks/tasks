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
 * A description item displays a text on several lines. The default
 * implementation makes it disabled.
 * 
 * @author Cyril Mottier
 */
public class DescriptionItem extends TextItem {

    /**
     * @hide
     */
    public DescriptionItem() {
        this(null);
    }

    /**
     * Creates a new DescriptionItem with the given description.
     * 
     * @param description The description for the current item.
     */
    public DescriptionItem(String description) {
        super(description);
        enabled = false;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.gd_description_item_view, parent);
    }

}
