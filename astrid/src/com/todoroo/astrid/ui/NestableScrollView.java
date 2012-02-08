package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ScrollView;

public class NestableScrollView extends ScrollView {
    private int[] scrollableViews = new int[0];

    public NestableScrollView(Context context, AttributeSet attrs) {
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
