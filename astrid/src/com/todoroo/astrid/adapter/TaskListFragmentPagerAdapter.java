package com.todoroo.astrid.adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
        if (filter instanceof FilterWithCustomIntent) {
            try {
                Class<?> component = Class.forName(((FilterWithCustomIntent) filter).customTaskList.getClassName());
                Constructor<?> constructor = component.getConstructor(Boolean.class, Filter.class);
                return (Fragment) constructor.newInstance(true, filter);
            } catch (NoSuchMethodException e) {
                return new TaskListFragment(null);
            } catch (InvocationTargetException e) {
                return new TaskListFragment(null);
            } catch (ClassNotFoundException e) {
                return new TaskListFragment(null);
            } catch (IllegalAccessException e) {
                return new TaskListFragment(null);
            } catch (InstantiationException e) {
                return new TaskListFragment(null);
            }
        } else {
            return new TaskListFragment(null);
        }
    }

}
