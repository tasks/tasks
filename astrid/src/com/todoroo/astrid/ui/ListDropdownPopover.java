package com.todoroo.astrid.ui;

import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionWidget;

import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.timsu.astrid.R;


public class ListDropdownPopover extends QuickActionWidget {

    public ListDropdownPopover(Context context) {
        super(context);
        setContentView(R.layout.list_dropdown_popover);

        setFocusable(true);
        setTouchable(true);
    }

    public void setContent(View content) {
        FrameLayout contentContainer = (FrameLayout) getContentView().findViewById(R.id.content);
        contentContainer.addView(content);
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
