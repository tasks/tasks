/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskListFragmentPagerAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.utility.Flags;

public class TaskListFragmentPager extends ViewPager {

    public static final String PREF_SHOWED_SWIPE_HELPER = "showed_swipe_helper"; //$NON-NLS-1$

    public TaskListFragmentPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOffscreenPageLimit(1);
        setPageMargin(1);
        setPageMarginDrawable(R.drawable.vertical_separator);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (!(adapter instanceof TaskListFragmentPagerAdapter))
            throw new ClassCastException("TaskListFragmentPager requires an adapter of type TaskListFragmentPagerAdapter"); //$NON-NLS-1$
        super.setAdapter(adapter);
    }

    /**
     * Hack to enable reloading fragments while they are being viewed
     */
    public void forceReload() {
        int position = getCurrentItem();
        setAdapter(getAdapter());
        setCurrentItem(position, false);
    }

    /**
     * Show the supplied filter, adding it to the data source if it doesn't exist
     * @param f
     */
    public void showFilter(Filter f) {
        TaskListFragmentPagerAdapter adapter = (TaskListFragmentPagerAdapter) getAdapter();
        showFilter(adapter.addOrLookup(f));
    }

    /**
     * Show the filter at the supplied index, with animation
     * @param index
     */
    public void showFilter(int index) {
        setCurrentItem(index, true);
    }

    /**
     * Returns the currently showing fragment
     * @return
     */
    public TaskListFragment getCurrentFragment() {
        return (TaskListFragment) ((TaskListFragmentPagerAdapter) getAdapter()).lookupFragmentForPosition(getCurrentItem());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (checkForPeopleHeaderScroll(ev))
            return false;

        if (Flags.check(Flags.TLFP_NO_INTERCEPT_TOUCH))
            return false;

        return super.onInterceptTouchEvent(ev);
    }

    private boolean checkForPeopleHeaderScroll(MotionEvent ev) {
        TaskListFragment current = getCurrentFragment();
        if (current != null) {
            View v = current.getView();
            if (v != null) {
                View peopleView = v.findViewById(R.id.shared_with);
                if (peopleView != null) {
                    Rect rect = new Rect();
                    peopleView.getHitRect(rect);
                    if (rect.contains((int) ev.getX(), (int) ev.getY()))
                        return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    public static void showSwipeBetweenHelper(Activity activity) {
        if (!Preferences.getBoolean(PREF_SHOWED_SWIPE_HELPER, false)) {
            String body = String.format("<h3>%s</h3><img src='%s'><br><br>%s",
                    activity.getString(R.string.swipe_lists_helper_header),
                    "subtasks_horizontal.png",
                    activity.getString(R.string.swipe_lists_helper_subtitle));

            String color = ThemeService.getDialogTextColorString();
            String html = String.format("<html><body style='text-align:center;color:%s'>%s</body></html>",
                    color, body);

            DialogUtilities.htmlDialog(activity, html, R.string.swipe_lists_helper_title);

            Preferences.setBoolean(PREF_SHOWED_SWIPE_HELPER, true);
        }
    }
}
