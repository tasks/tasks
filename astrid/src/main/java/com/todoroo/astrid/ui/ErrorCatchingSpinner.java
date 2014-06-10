/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorCatchingSpinner extends Spinner {

    private static final Logger log = LoggerFactory.getLogger(ErrorCatchingSpinner.class);

    public ErrorCatchingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        } catch (IllegalArgumentException e) {
            // Bad times
            log.error(e.getMessage(), e);
        }
    }

}
