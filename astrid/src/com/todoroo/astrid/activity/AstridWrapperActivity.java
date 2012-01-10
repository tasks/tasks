package com.todoroo.astrid.activity;

import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.IntentFilter;
import com.todoroo.astrid.core.SearchFilter;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;

/**
 * This wrapper activity contains all the glue-code to handle the callbacks between the different
 * fragments that could be visible on the screen in landscape-mode.
 * So, it basically contains all the callback-code from the filterlist-fragment, the tasklist-fragments
 * and the taskedit-fragment (and possibly others that should be handled).
 * Using this AstridWrapperActivity helps to avoid duplicated code because its all gathered here for sub-wrapperactivities
 * to use.
 *
 * @author Arne
 *
 */
public class AstridWrapperActivity extends FragmentActivity
    implements FilterListActivity.OnFilterItemClickedListener,
    TaskListActivity.OnTaskListItemClickedListener,
    TaskEditActivity.OnTaskEditDetailsClickedListener {

    /** This flag shows if the landscape-multipane layouts are active.
     * If a multipane-layout with two fragments is active, the callbacks implemented here
     * should not start a new activity, but update the target-fragment directly instead.
     */
    protected boolean mMultipleFragments = false;

    protected FilterListActivity getFilterListFragment() {
        FilterListActivity frag = (FilterListActivity) getSupportFragmentManager()
                .findFragmentById(R.id.filterlist_fragment);
        if (frag == null || !frag.isInLayout())
            return null;

        return frag;
    }

    protected TaskListActivity getTaskListFragment() {
        TaskListActivity frag = (TaskListActivity) getSupportFragmentManager()
                .findFragmentById(R.id.tasklist_fragment);
        if (frag == null || !frag.isInLayout())
            return null;

        return frag;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        FilterListActivity frag = getFilterListFragment();
        if (frag != null) {
            // forwarding for search-requests
            frag.onNewIntent(intent);
        }
    }

    /**
     * Handles items being clicked from the filterlist-fragment. Return true if item is handled.
     */
    public boolean onFilterItemClicked(FilterListItem item) {
        if (!mMultipleFragments || (item instanceof SearchFilter)) {
            if(item instanceof Filter) {
                Filter filter = (Filter)item;
                if(filter instanceof FilterWithCustomIntent) {
                    FilterWithCustomIntent customFilter = ((FilterWithCustomIntent)filter);
                    customFilter.start(this, FilterListActivity.REQUEST_VIEW_TASKS);
                } else {
                    Intent intent = new Intent(this, TaskListWrapperActivity.class);
                    intent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
                    intent.putExtra(TaskListActivity.TOKEN_OVERRIDE_ANIM, true);
                    startActivityForResult(intent, FilterListActivity.REQUEST_VIEW_TASKS);
                }
                AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
                StatisticsService.reportEvent(StatisticsConstants.FILTER_LIST);
                return true;
            } else if(item instanceof SearchFilter) {
                onSearchRequested();
                StatisticsService.reportEvent(StatisticsConstants.FILTER_SEARCH);
            } else if(item instanceof IntentFilter) {
                try {
                    ((IntentFilter)item).intent.send();
                } catch (CanceledException e) {
                    // ignore
                }
            }
            return false;
        } else {
            // If showing both fragments, directly update the tasklist-fragment
            TaskListActivity tasklist = (TaskListActivity) getSupportFragmentManager()
                    .findFragmentById(R.id.tasklist_fragment);

            if(item instanceof Filter) {
                Filter filter = (Filter)item;
                if(filter instanceof FilterWithCustomIntent) {
                    FilterWithCustomIntent customFilter = ((FilterWithCustomIntent)filter);
                    tasklist.onNewIntent(customFilter.getCustomIntent());
                } else {
                    Intent intent = new Intent(this, TaskListWrapperActivity.class);
                    intent.putExtra(TaskListActivity.TOKEN_FILTER, filter);

                    tasklist.onNewIntent(intent);
                }
                // no animation for dualpane-layout
                AndroidUtilities.callOverridePendingTransition(this, 0, 0);
                StatisticsService.reportEvent(StatisticsConstants.FILTER_LIST);
                return true;
            } else if(item instanceof IntentFilter) {
                try {
                    ((IntentFilter)item).intent.send();
                } catch (CanceledException e) {
                    // ignore
                }
            }
            return false;
        }
    }

    @Override
    public void onTaskListItemClicked(int category, int position) {
    }

    @Override
    public void onTaskEditDetailsClicked(int category, int position) {
        // TODO Auto-generated method stub

    }
}
