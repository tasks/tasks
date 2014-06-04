package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorCatchingListView extends ListView {

    private static final Logger log = LoggerFactory.getLogger(ErrorCatchingListView.class);

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
            log.error(e.getMessage(), e);
            return true;
        }
    }

}
