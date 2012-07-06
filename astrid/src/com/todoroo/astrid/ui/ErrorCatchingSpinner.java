package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

public class ErrorCatchingSpinner extends Spinner {

    public ErrorCatchingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        } catch (IllegalArgumentException e) {
            // Bad times
        }
    }

}
