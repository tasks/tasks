package com.todoroo.astrid.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

public class NestableViewPager extends ViewPager {
    private int[] scrollableViews = new int[0];

    public NestableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
     @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
         for(int i = 0; i < scrollableViews.length; i++) {
            View view = findViewById(scrollableViews[i]);
            if (view instanceof ViewParent)
                ((ViewParent)view).requestDisallowInterceptTouchEvent(true);
         }
        return super.onInterceptTouchEvent(event);
    }

    public void setScrollabelViews(int[] views) {
        this.scrollableViews = views;
    }
}
