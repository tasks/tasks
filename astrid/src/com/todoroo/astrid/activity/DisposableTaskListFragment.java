package com.todoroo.astrid.activity;

import android.app.Activity;

/**
 * Task list fragment that will remove itself from the filterlist/fragment pager when it is detached
 * @author Sam
 *
 */
public class DisposableTaskListFragment extends TaskListFragment {

    @Override
    public void onDetach() {
        Activity activity = getActivity();
        if (activity instanceof TaskListActivity) {
            TaskListActivity tla = (TaskListActivity) activity;
            tla.getFragmentPagerAdapter().remove(filter);
        }
        super.onDetach();
    }

}