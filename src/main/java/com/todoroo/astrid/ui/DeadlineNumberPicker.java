/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;

import org.tasks.R;

public class DeadlineNumberPicker extends NumberPicker {

    public DeadlineNumberPicker(Context context) {
        super(context);
    }

    public DeadlineNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
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
