package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class ErrorCatchingListView extends ListView {

    public ErrorCatchingListView(Context context) {
        super(context);
    }

    public ErrorCatchingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ErrorCatchingListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (Exception e) {
            return true;
        }
    }

}
