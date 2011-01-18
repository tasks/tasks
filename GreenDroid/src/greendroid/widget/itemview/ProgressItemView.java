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
import greendroid.widget.item.ProgressItem;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cyrilmottier.android.greendroid.R;

public class ProgressItemView extends FrameLayout implements ItemView {

    private ProgressBar mProgressBar;
    private TextView mTextView;

    public ProgressItemView(Context context) {
        this(context, null);
    }

    public ProgressItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void prepareItemView() {
        mProgressBar = (ProgressBar) findViewById(R.id.gd_progress_bar);
        mTextView = (TextView) findViewById(R.id.gd_text);
    }

    public void setObject(Item object) {
        final ProgressItem item = (ProgressItem) object;
        mProgressBar.setVisibility(item.isInProgress ? View.VISIBLE : View.GONE);
        mTextView.setText(item.text);
    }

}
