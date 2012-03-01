package com.todoroo.astrid.ui;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskListFragmentPagerAdapter;
import com.todoroo.astrid.api.Filter;

public class TaskListFragmentPager extends ViewPager {

    public TaskListFragmentPager(Context context, AttributeSet attrs) {
        super(context, attrs);
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
}
