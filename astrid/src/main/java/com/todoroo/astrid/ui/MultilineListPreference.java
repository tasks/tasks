/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;

public class MultilineListPreference extends ListPreference {
    public MultilineListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        try {
            super.onBindView(view);
        } catch (Exception e) {
            // happens on 4.0 emulators
        }
        MultilineHelper.makeMultiline(view);
    }
}
