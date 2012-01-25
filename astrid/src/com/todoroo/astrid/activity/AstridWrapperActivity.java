package com.todoroo.astrid.activity;

import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

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

    public static final int LAYOUT_SINGLE = 0;
    public static final int LAYOUT_DOUBLE = 1;
    public static final int LAYOUT_TRIPLE = 2;

    protected int fragmentLayout = LAYOUT_SINGLE;

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        FilterListActivity frag = getFilterListFragment();
        if (frag != null) {
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
                    int lastSelectedList = intent.getIntExtra(FilterListActivity.TOKEN_LAST_SELECTED, 0);
                    intent = ((FilterWithCustomIntent)filter).getCustomIntent();
                    intent.putExtra(FilterListActivity.TOKEN_LAST_SELECTED, lastSelectedList);
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
            transaction.replace(R.id.tasklist_fragment_container, newFragment,
                    TaskListActivity.TAG_TASKLIST_FRAGMENT);
            transaction.commit();
        } catch (Exception e) {
            // Don't worry about it
        }
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        Intent intent = new Intent(this, TaskEditWrapperActivity.class);
        intent.putExtra(TaskEditActivity.TOKEN_ID, taskId);
        getIntent().putExtra(TaskEditActivity.TOKEN_ID, taskId); // Needs to be in activity intent so that TEA onResume doesn't create a blank activity
        if (getIntent().hasExtra(TaskListActivity.TOKEN_FILTER))
            intent.putExtra(TaskListActivity.TOKEN_FILTER, getIntent().getParcelableExtra(TaskListActivity.TOKEN_FILTER));

        if (fragmentLayout != LAYOUT_SINGLE) {
            TaskEditActivity editActivity = getTaskEditFragment();
            if (fragmentLayout == LAYOUT_TRIPLE)
                findViewById(R.id.taskedit_fragment_container).setVisibility(View.VISIBLE);

            if(editActivity == null) {
                editActivity = new TaskEditActivity();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.taskedit_fragment_container, editActivity, TaskEditActivity.TAG_TASKEDIT_FRAGMENT);
                transaction.addToBackStack(null);
                transaction.commit();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Force the transaction to occur so that we can be guaranteed of the fragment existing if we try to present it
                        getSupportFragmentManager().executePendingTransactions();
                    }
                });
            }

            editActivity.save(true);
            editActivity.repopulateFromScratch(intent);

        } else {
            startActivityForResult(intent, TaskListActivity.ACTIVITY_EDIT_TASK);
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onTaskEditDetailsClicked(int category, int position) {
        //

    }

    // --- fragment helpers

    protected void removeFragment(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        if(fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.commit();
        }
    }

    protected Fragment setupFragment(String tag, int container, Class<? extends Fragment> cls) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        if(fragment == null) {
            System.err.println("creating fragment of type " + cls.getSimpleName()); //$NON-NLS-1$
            try {
                fragment = cls.newInstance();
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (container == 0)
                ft.add(fragment, tag);
            else
                ft.replace(container, fragment, tag);
            ft.commit();
        }
        return fragment;
    }

    /**
     * @return LAYOUT_SINGLE, LAYOUT_DOUBLE, or LAYOUT_TRIPLE
     */
    public int getFragmentLayout() {
        return fragmentLayout;
    }

    /**
     * @deprecated please use the getFragmentLayout method instead
     */
    @Deprecated
    public boolean isMultipleFragments() {
        return fragmentLayout != LAYOUT_SINGLE;
    }

}
