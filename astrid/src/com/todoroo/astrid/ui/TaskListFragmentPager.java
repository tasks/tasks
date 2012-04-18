package com.todoroo.astrid.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskListFragmentPagerAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.utility.Flags;

public class TaskListFragmentPager extends ViewPager {

    private static final int OFFSCREEN_PAGE_LIMIT = 2;

    public TaskListFragmentPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (!(adapter instanceof TaskListFragmentPagerAdapter))
            throw new ClassCastException("TaskListFragmentPager requires an adapter of type TaskListFragmentPagerAdapter"); //$NON-NLS-1$
        super.setAdapter(adapter);
    }

    /**
     * Show the supplied filter, adding it to the data source if it doesn't exist
     * @param f
     */
    public void showFilter(Filter f) {
        TaskListFragmentPagerAdapter adapter = (TaskListFragmentPagerAdapter) getAdapter();
        showFilter(adapter.addOrLookup(f));
    }

    public void showFilterWithCustomTaskList(Filter f, Class<?> customTaskList) {
        TaskListFragmentPagerAdapter adapter = (TaskListFragmentPagerAdapter) getAdapter();
        adapter.setCustomTaskListForFilter(f, customTaskList);
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
}
