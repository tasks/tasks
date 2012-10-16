/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.welcome;

import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionWidget;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.utility.AstridPreferences;

/**
 * Displays a popover with some help text for first time users.
 *
 * @author Sam Bosley <sam@astrid.com>
 *
 */
public class HelpInfoPopover extends QuickActionWidget {

    public static HelpInfoPopover showPopover(final Activity activity, final View parent,
            final int textId, OnDismissListener dismissListener) {
        final HelpInfoPopover toShow = new HelpInfoPopover(activity, textId);
        if (dismissListener != null) {
            toShow.setOnDismissListener(dismissListener);
        }
        parent.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    toShow.show(parent);
                } catch (Exception e) {
                    //Activity wasn't running or something like that
                }
            }
        }, 250);
        return toShow;
    }

    private final boolean tablet;
    private int measuredWidth;

    private HelpInfoPopover(Context context, int textId) {
        super(context);
        setContentView(R.layout.help_popover);
        TextView message = (TextView)getContentView().findViewById(R.id.gdi_message);
        message.setText(textId);
        setFocusable(false);
        setTouchable(true);
        measuredWidth = -1; // uninitialized
        tablet = AstridPreferences.useTabletLayout(context);
    }

    @Override
    protected void populateQuickActions(List<QuickAction> quickActions) {
        // Do nothing
    }

    @Override
    protected int getArrowLeftMargin(View arrow) {
        if (measuredWidth > 0)
            return (measuredWidth - arrow.getMeasuredWidth()) / 2;

        if (tablet)
            return mRect.width() / 4;
        return mRect.width() / 2;
    }

    @Override
    protected int getShowAtX() {
        if (measuredWidth > 0)
            return mRect.left + (mRect.width() - measuredWidth) / 2;
        return mRect.left;
    }

    @Override
    protected void onMeasureAndLayout(Rect anchorRect, View contentView) {
        contentView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        int rootHeight = contentView.getMeasuredHeight();
        measuredWidth = contentView.getMeasuredWidth();

        int offsetY = getArrowOffsetY();
        int dyTop = anchorRect.top;
        int dyBottom = getScreenHeight() - anchorRect.bottom;

        boolean onTop = (dyTop > dyBottom);
        int popupY = (onTop) ? anchorRect.top - rootHeight + offsetY : anchorRect.bottom - offsetY;

        setWidgetSpecs(popupY, onTop);
    }

}
