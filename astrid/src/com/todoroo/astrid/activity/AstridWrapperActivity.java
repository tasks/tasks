package com.todoroo.astrid.activity;

import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

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

    public FilterListActivity getFilterListFragment() {
        FilterListActivity frag = (FilterListActivity) getSupportFragmentManager()
                .findFragmentByTag(FilterListActivity.TAG_FILTERLIST_FRAGMENT);

        return frag;
    }

    public TaskListActivity getTaskListFragment() {
        TaskListActivity frag = (TaskListActivity) getSupportFragmentManager()
                .findFragmentByTag(TaskListActivity.TAG_TASKLIST_FRAGMENT);

        return frag;
    }

    public TaskEditActivity getTaskEditFragment() {
        TaskEditActivity frag = (TaskEditActivity) getSupportFragmentManager()
                .findFragmentByTag(TaskEditActivity.TAG_TASKEDIT_FRAGMENT);

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
        if (this instanceof TaskListWrapperActivity && (item instanceof Filter) ) {
            ((TaskListWrapperActivity) this).setSelectedItem((Filter) item);
        }
        if (item instanceof SearchFilter) {
            onSearchRequested();
            StatisticsService.reportEvent(StatisticsConstants.FILTER_SEARCH);
            return false;
        } else {
            // If showing both fragments, directly update the tasklist-fragment
            Intent intent = getIntent();

            if(item instanceof Filter) {
                Filter filter = (Filter)item;
                if(filter instanceof FilterWithCustomIntent) {
                    intent = ((FilterWithCustomIntent)filter).getCustomIntent();
                } else {
                    intent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
                }

                setIntent(intent);

                setupTasklistFragmentWithFilter(filter);
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

    protected void setupTasklistFragmentWithFilter(Filter filter) {
        Class<?> component = TaskListActivity.class;
        if (filter instanceof FilterWithCustomIntent) {
            try {
                component = Class.forName(((FilterWithCustomIntent) filter).customTaskList.getClassName());
            } catch (Exception e) {
                // Invalid
            }
        }
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        try {
            TaskListActivity newFragment = (TaskListActivity) component.newInstance();
            transaction.replace(R.id.tasklist_fragment_container, newFragment, TaskListActivity.TAG_TASKLIST_FRAGMENT);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace(); //Uh ohs
        }
    }

    protected void setupFilterlistFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        FilterListActivity newFragment = new FilterListActivity();
        if (findViewById(R.id.filterlist_fragment_container) != null) {
            if (getFilterListFragment() != null) {
                transaction.remove(getFilterListFragment());
                transaction.commit();
                transaction = manager.beginTransaction();
            }
            transaction.replace(R.id.filterlist_fragment_container, newFragment, FilterListActivity.TAG_FILTERLIST_FRAGMENT);
        } else {
            if (getFilterListFragment() != null)
                return;
            transaction.add(newFragment, FilterListActivity.TAG_FILTERLIST_FRAGMENT);
        }
        transaction.commit();
    }

    public boolean isMultipleFragments() {
        return mMultipleFragments;
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        Intent intent = new Intent(this, TaskEditWrapperActivity.class);
        intent.putExtra(TaskEditActivity.TOKEN_ID, taskId);
        if (intent.hasExtra(TaskListActivity.TOKEN_FILTER))
            intent.putExtra(TaskListActivity.TOKEN_FILTER, intent.getParcelableExtra(TaskListActivity.TOKEN_FILTER));
        if (this instanceof TaskEditWrapperActivity) {
            TaskEditActivity editActivity = getTaskEditFragment();
            editActivity.save(true);
            editActivity.repopulateFromScratch(intent);
        } else {
            startActivityForResult(intent, TaskListActivity.ACTIVITY_EDIT_TASK);
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
        }
    }

    @Override
    public void onTaskEditDetailsClicked(int category, int position) {
        // TODO Auto-generated method stub

    }
}
