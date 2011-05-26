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
package greendroid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cyrilmottier.android.greendroid.R;

public class ActionBarHost extends LinearLayout {

    private ActionBar mActionBar;
    private FrameLayout mContentView;

    public ActionBarHost(Context context) {
        this(context, null);
    }

    public ActionBarHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mActionBar = (ActionBar) findViewById(R.id.gd_action_bar);
        if (mActionBar == null || !(mActionBar instanceof ActionBar)) {
            throw new IllegalArgumentException("No ActionBar with the id R.id.gd_action_bar found in the layout.");
        }

        mContentView = (FrameLayout) findViewById(R.id.gd_action_bar_content_view);
        if (mContentView == null || !(mContentView instanceof FrameLayout)) {
            throw new IllegalArgumentException("No FrameLayout with the id R.id.gd_action_bar_content_view found in the layout.");
        }
    }

    public ActionBar getActionBar() {
        return mActionBar;
    }

    public FrameLayout getContentView() {
        return mContentView;
    }

}
