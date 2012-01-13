package com.todoroo.astrid.ui;

import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionWidget;

import java.util.List;

import android.R;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;


public class ListDropdownPopover extends QuickActionWidget {

    private final ListView listView;

    public ListDropdownPopover(Context context, int layout) {
        super(context);
        setContentView(layout);

        listView = (ListView) getContentView().findViewById(R.id.list);

        setFocusable(true);
        setTouchable(true);
    }

    public void setAdapter(ListAdapter adapter, OnItemClickListener listener) {
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(listener);
    }

    @Override
    protected void populateQuickActions(List<QuickAction> quickActions) {
        // Do nothing
    }

    @Override
    protected void onMeasureAndLayout(Rect anchorRect, View contentView) {
        contentView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentView.measure(MeasureSpec.makeMeasureSpec(getScreenWidth(), MeasureSpec.EXACTLY),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        int rootHeight = contentView.getMeasuredHeight();

        int offsetY = getArrowOffsetY();
        int dyTop = anchorRect.top;
        int dyBottom = getScreenHeight() - anchorRect.bottom;

        boolean onTop = (dyTop > dyBottom);
        int popupY = (onTop) ? anchorRect.top - rootHeight + offsetY : anchorRect.bottom - offsetY;

        setWidgetSpecs(popupY, onTop);
    }
}
