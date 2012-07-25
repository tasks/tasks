/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.adapter.TaskListFragmentPagerAdapter;

/**
 * Task list fragment that will remove itself from the filterlist/fragment pager when it is detached
 * @author Sam
 *
 */
public class DisposableTaskListFragment extends TaskListFragment {

    @Override
    public void onDetach() {
        Activity activity = getActivity();
        if (activity instanceof TaskListActivity &&
                Preferences.getIntegerFromString(R.string.p_swipe_lists_performance_key, 0) > 0) {
            TaskListActivity tla = (TaskListActivity) activity;
            if (tla.getFragmentLayout() == AstridActivity.LAYOUT_SINGLE) {
                TaskListFragmentPagerAdapter adapter = tla.getFragmentPagerAdapter();
                if (adapter != null)
                    adapter.remove(filter);
            }
        }
        super.onDetach();
    }

}