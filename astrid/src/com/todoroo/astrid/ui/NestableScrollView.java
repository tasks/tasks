/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class NestableScrollView extends ScrollView {
    private int[] scrollableViews = new int[0];

    public NestableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (scrollableViews != null) {
            for (int scrollableView : scrollableViews) {
                View view = findViewById(scrollableView);
                if (view != null) {
                    Rect rect = new Rect();
                    view.getHitRect(rect);
                    if (rect.contains((int) event.getX(), (int) event.getY())) {
                        return false;
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    public void setScrollabelViews(int[] views) {
        this.scrollableViews = views;
    }
}
