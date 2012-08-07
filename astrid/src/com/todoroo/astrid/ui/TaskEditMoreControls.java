/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;


public class TaskEditMoreControls extends LinearLayout {

    public TaskEditMoreControls(Context context) {
        super(context);
    }

    public TaskEditMoreControls(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public void setViewHeightBasedOnChildren(LayoutParams params) {

        int totalHeight = 0;
        int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
        for (int i = 0; i < getChildCount(); i++) {
            View listItem = getChildAt(i);
            listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        params.height = totalHeight;
        setLayoutParams(params);
        requestLayout();
    }

}
