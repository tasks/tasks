package com.todoroo.astrid.ui;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

public class MultilinePreference extends Preference {
    public MultilinePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        MultilineHelper.makeMultiline(view);
    }
}
