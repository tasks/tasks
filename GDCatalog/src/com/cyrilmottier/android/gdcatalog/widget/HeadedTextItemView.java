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
package com.cyrilmottier.android.gdcatalog.widget;

import greendroid.widget.item.Item;
import greendroid.widget.itemview.ItemView;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyrilmottier.android.gdcatalog.R;

public class HeadedTextItemView extends LinearLayout implements ItemView {

    private TextView mHeaderView;
    private TextView mTextView;

    public HeadedTextItemView(Context context) {
        this(context, null);
    }

    public HeadedTextItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepareItemView() {
        mHeaderView = (TextView) findViewById(R.id.gd_separator_text);
        mTextView = (TextView) findViewById(R.id.gd_text);
    }

    public void setObject(Item object) {

        final HeadedTextItem item = (HeadedTextItem) object;
        final String headerText = item.headerText;

        if (!TextUtils.isEmpty(headerText)) {
            mHeaderView.setText(headerText);
            mHeaderView.setVisibility(View.VISIBLE);
        } else {
            mHeaderView.setVisibility(View.GONE);
        }

        mTextView.setText(item.text);
    }

}
