/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NestableViewPager extends ViewPager {

    public NestableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Prevent horizontal scrolling
     */
     @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
         return false;
    }

}
