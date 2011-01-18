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
import greendroid.widget.item.SubtextItem;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyrilmottier.android.greendroid.R;

public class SubtextItemView extends LinearLayout implements ItemView {

    private TextView mTextView;
    private TextView mSubtextView;

    public SubtextItemView(Context context) {
        this(context, null);
    }

    public SubtextItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepareItemView() {
        mTextView = (TextView) findViewById(R.id.gd_text);
        mSubtextView = (TextView) findViewById(R.id.gd_subtext);
    }

    public void setObject(Item object) {
        final SubtextItem item = (SubtextItem) object;
        mTextView.setText(item.text);
        mSubtextView.setText(item.subtext);
    }

}
