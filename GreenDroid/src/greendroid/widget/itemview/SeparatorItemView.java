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
package greendroid.widget.itemview;

import greendroid.widget.item.Item;
import greendroid.widget.item.TextItem;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class SeparatorItemView extends TextView implements ItemView {

    public SeparatorItemView(Context context) {
        this(context, null);
    }

    public SeparatorItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeparatorItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void prepareItemView() {
    }

    public void setObject(Item object) {
        final TextItem item = (TextItem) object;
        setText(item.text);
    }

}
