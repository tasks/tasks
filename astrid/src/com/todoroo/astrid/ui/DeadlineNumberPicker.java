/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.timsu.astrid.R;

public class DeadlineNumberPicker extends NumberPicker {

    public DeadlineNumberPicker(Context context) {
        super(context);
    }

    public DeadlineNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeadlineNumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int getLayout() {
        return R.layout.deadline_number_picker;
    }

    @Override
    protected int getMaxDigits() {
        return 2;
    }
}
