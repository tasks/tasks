package com.todoroo.astrid.adapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;

public class TaskListFragmentPagerAdapter extends FragmentStatePagerAdapter {

    private final Filter[] filters;

    public TaskListFragmentPagerAdapter(FragmentManager fm, Filter[] filters) {
        super(fm);
        this.filters = filters;
    }

    @Override
    public Fragment getItem(int position) {
        return getFragmentForFilter(filters[position]);
    }

    @Override
    public int getCount() {
        return filters.length;
    }

    private Fragment getFragmentForFilter(Filter filter) {
        Bundle extras = getExtrasForFilter(filter);
        return TaskListFragment.instantiateWithFilterAndExtras(filter, extras, TaskListFragment.class);
    }

    private Bundle getExtrasForFilter(Filter filter) {
        Bundle extras;
        if (filter instanceof FilterWithCustomIntent) {
            extras = ((FilterWithCustomIntent) filter).customExtras;
        } else {
            extras = new Bundle();
        }
        extras.putParcelable(TaskListFragment.TOKEN_FILTER, filter);
        return extras;
    }

}
