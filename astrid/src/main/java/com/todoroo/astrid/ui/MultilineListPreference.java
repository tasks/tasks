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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultilineListPreference extends ListPreference {

    private static final Logger log = LoggerFactory.getLogger(MultilineListPreference.class);

    public MultilineListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        try {
            super.onBindView(view);
        } catch (Exception e) {
            // happens on 4.0 emulators
            log.error(e.getMessage(), e);
        }
        MultilineHelper.makeMultiline(view);
    }
}
