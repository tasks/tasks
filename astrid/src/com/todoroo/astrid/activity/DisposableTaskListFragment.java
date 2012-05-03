package com.todoroo.astrid.activity;

import android.app.Activity;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

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
                Preferences.getIntegerFromString(R.string.p_swipe_lists_performance_key, 0)> 0) {
            TaskListActivity tla = (TaskListActivity) activity;
            if (tla.getFragmentLayout() == AstridActivity.LAYOUT_SINGLE)
                tla.getFragmentPagerAdapter().remove(filter);
        }
        super.onDetach();
    }

}