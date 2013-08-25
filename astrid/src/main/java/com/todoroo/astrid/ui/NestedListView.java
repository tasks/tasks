/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class NestedListView extends ListView  {
    private static final int MAXIMUM_LIST_ITEMS_VIEWABLE = 2;


    public NestedListView(Context context) {
        super(context);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int newHeight = 0;
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            ListAdapter listAdapter = getAdapter();
            if (listAdapter != null && !listAdapter.isEmpty()) {
                int listPosition = 0;
                for (listPosition = 0; listPosition < listAdapter.getCount()
                        && listPosition < MAXIMUM_LIST_ITEMS_VIEWABLE; listPosition++) {
                    View listItem = listAdapter.getView(listPosition, null,
                            this);
                    listItem.measure(widthMeasureSpec, heightMeasureSpec);
                    newHeight += listItem.getMeasuredHeight();
                }
                newHeight += getDividerHeight() * listPosition;
            }
            if ((heightMode == MeasureSpec.AT_MOST) && (newHeight > heightSize)) {
                if (newHeight > heightSize) {
                    newHeight = heightSize;
                }
            }
        } else {
            newHeight = getMeasuredHeight();
        }
        setMeasuredDimension(getMeasuredWidth(), newHeight);
    }

}
