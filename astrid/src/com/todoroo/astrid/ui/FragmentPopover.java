/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionWidget;

import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.timsu.astrid.R;


public class FragmentPopover extends QuickActionWidget {

    protected DisplayMetrics metrics;

    public FragmentPopover(Context context, int layout) {
        super(context);
        setContentView(layout);

        metrics = context.getResources().getDisplayMetrics();

        setFocusable(true);
        setTouchable(true);
    }

    public void setContent(View content) {
        FrameLayout contentContainer = (FrameLayout) getContentView().findViewById(R.id.content);
        contentContainer.addView(content);
    }

    public void setContent(View content, LayoutParams params) {
        FrameLayout contentContainer = (FrameLayout) getContentView().findViewById(R.id.content);
        contentContainer.addView(content, params);
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

    @Override
    protected int getArrowLeftMargin(View arrow) {
        return mRect.left + arrow.getMeasuredWidth() / 2 - (int) (10 * metrics.density);
    }

    @Override
    public void show(View anchor) {
        if (isShowing())
            return;
        super.show(anchor);
    }
}
