package com.todoroo.astrid.adapter;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.FilterAdapter.FilterDataSourceChangedListener;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;

public class TaskListFragmentPagerAdapter extends FragmentStatePagerAdapter implements FilterDataSourceChangedListener {

    private final FilterAdapter filterAdapter;

    public TaskListFragmentPagerAdapter(FragmentManager fm, FilterAdapter filterAdapter) {
        super(fm);
        this.filterAdapter = filterAdapter;
        filterAdapter.setDataSourceChangedListener(this);
    }

    @Override
    public void filterDataSourceChanged() {
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        return getFragmentForFilter(filterAdapter.getItem(position));
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return filterAdapter.getItem(position).title;
    }

    public int addOrLookup(Filter filter) {
        return filterAdapter.addOrLookup(filter);
    }

    public Filter getFilter(int position) {
        return filterAdapter.getItem(position);
    }

    @Override
    public int getCount() {
        return filterAdapter.getCount();
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
        if (filter != null)
            extras.putParcelable(TaskListFragment.TOKEN_FILTER, filter);
        return extras;
    }

    @Override
    public Parcelable saveState() {
        return null; // Don't save state
    }

}
