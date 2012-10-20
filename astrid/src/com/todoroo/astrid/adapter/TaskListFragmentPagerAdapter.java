/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.HashMap;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.FilterAdapter.FilterDataSourceChangedListener;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.subtasks.SubtasksHelper;

public class TaskListFragmentPagerAdapter extends FragmentStatePagerAdapter implements FilterDataSourceChangedListener {

    private final HashMap<Integer, Fragment> positionToFragment;

    private final FilterAdapter filterAdapter; // Shares an adapter instance with the filter list fragment

    public TaskListFragmentPagerAdapter(FragmentManager fm, FilterAdapter filterAdapter) {
        super(fm);
        this.filterAdapter = filterAdapter;
        filterAdapter.setDataSourceChangedListener(this);
        positionToFragment = new HashMap<Integer, Fragment>();
    }

    @Override
    public void filterDataSourceChanged() {
        notifyDataSetChanged();
    }

    /**
     * Instantiates and returns a fragment for the filter at the specified position.
     * Also maps the position to the fragment in a cache for later lookup
     */
    @Override
    public Fragment getItem(int position) {
        Filter filter = filterAdapter.getItem(position);
        Fragment fragment = getFragmentForFilter(filter);
        positionToFragment.put(position, fragment);
        return fragment;
    }

    /**
     * Lookup the fragment for the specified position
     * @param position
     * @return
     */
    public Fragment lookupFragmentForPosition(int position) {
        return positionToFragment.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return filterAdapter.getItem(position).title;
    }

    /**
     * Adds the specified filter to the data source if it doesn't exist,
     * returning the position of that filter regardless
     * @param filter
     * @return
     */
    public int addOrLookup(Filter filter) {
        return filterAdapter.addOrLookup(filter);
    }

    public int getPosition(Filter filter) {
        return filterAdapter.getPosition(filter);
    }

    public void remove(Filter filter) {
        filterAdapter.remove(filter);
    }

    /**
     * Get the filter at the specified position
     * @param position
     * @return
     */
    public Filter getFilter(int position) {
        return filterAdapter.getItem(position);
    }

    @Override
    public int getCount() {
        return filterAdapter.getCount();
    }

    private Fragment getFragmentForFilter(Filter filter) {
        Bundle extras = getExtrasForFilter(filter);
        Class<?> customList = null;
        if (SubtasksHelper.shouldUseSubtasksFragmentForFilter(filter))
            customList = SubtasksHelper.subtasksClassForFilter(filter);
        return TaskListFragment.instantiateWithFilterAndExtras(filter, extras, customList);
    }

    // Constructs extras corresponding to the specified filter that can be used as arguments to the fragment
    private Bundle getExtrasForFilter(Filter filter) {
        Bundle extras = null;
        if (filter instanceof FilterWithCustomIntent)
            extras = ((FilterWithCustomIntent) filter).customExtras;

        if (extras == null)
            extras = new Bundle();

        if (filter != null)
            extras.putParcelable(TaskListFragment.TOKEN_FILTER, filter);
        return extras;
    }

    @Override
    public Parcelable saveState() {
        return null; // Don't save state
    }

}
