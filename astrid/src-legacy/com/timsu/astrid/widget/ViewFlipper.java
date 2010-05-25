package com.timsu.astrid.widget;

import android.content.Context;
import android.util.AttributeSet;

/**
 * This class exists solely to suppress an Android 2.1 error
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ViewFlipper extends android.widget.ViewFlipper {
    public ViewFlipper(Context context) {
        super(context);
    }

    public ViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        } catch (Exception e) {
            // stupid Android 2.1 exception
        }
    }
}
