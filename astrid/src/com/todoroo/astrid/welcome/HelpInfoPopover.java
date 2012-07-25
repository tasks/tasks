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
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.timsu.astrid.R;

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
        parent.post(new Runnable() {
            @Override
            public void run() {
                try {
                    toShow.show(parent);
                } catch (Exception e) {
                    //Activity wasn't running or something like that
                }
            }
        });
        return toShow;
    }

    private HelpInfoPopover(Context context, int textId) {
        super(context);
        setContentView(R.layout.help_popover);
        TextView message = (TextView)getContentView().findViewById(R.id.gdi_message);
        message.setText(textId);
        setFocusable(false);
        setTouchable(true);
    }

    @Override
    protected void populateQuickActions(List<QuickAction> quickActions) {
        // Do nothing
    }

    @Override
    protected int getArrowLeftMargin(View arrow) {
        return mRect.width() / 2;
    }

    protected int getShowAtX() {
        return mRect.left;
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
