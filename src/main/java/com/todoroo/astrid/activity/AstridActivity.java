/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.UpgradeActivity;
import com.todoroo.astrid.subtasks.SubtasksHelper;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.ui.NavigationDrawerFragment;

import javax.inject.Inject;

import timber.log.Timber;

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
public abstract class AstridActivity extends InjectingAppCompatActivity
    implements TaskListFragment.OnTaskListItemClickedListener {

    public static final int LAYOUT_SINGLE = 0;
    public static final int LAYOUT_DOUBLE = 1;

    public static final int REQUEST_UPGRADE = 505;

    protected int fragmentLayout = LAYOUT_SINGLE;

    public TaskListFragment getTaskListFragment() {
        return (TaskListFragment) getFragmentManager()
                .findFragmentByTag(TaskListFragment.TAG_TASKLIST_FRAGMENT);
    }

    public TaskEditFragment getTaskEditFragment() {
        return (TaskEditFragment) getFragmentManager()
                .findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
    }

    @Inject StartupService startupService;
    @Inject SubtasksHelper subtasksHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startupService.onStartupApplication(this);
    }

    protected Bundle configureIntentAndExtrasWithFilter(Intent intent, Filter filter) {
        if(filter instanceof FilterWithCustomIntent) {
            int lastSelectedList = intent.getIntExtra(NavigationDrawerFragment.TOKEN_LAST_SELECTED, 0);
            intent = ((FilterWithCustomIntent)filter).getCustomIntent();
            intent.putExtra(NavigationDrawerFragment.TOKEN_LAST_SELECTED, lastSelectedList);
        } else {
            intent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        }

        setIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            extras = (Bundle) extras.clone();
        }
        return extras;
    }

    public void setupActivityFragment(TagData tagData) {
        if (fragmentLayout == LAYOUT_SINGLE) {
            return;
        }

        if (fragmentLayout == LAYOUT_DOUBLE) {
            findViewById(R.id.taskedit_fragment_container).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        Intent intent = new Intent(this, TaskEditActivity.class);
        intent.putExtra(TaskEditFragment.TOKEN_ID, taskId);
        getIntent().putExtra(TaskEditFragment.TOKEN_ID, taskId); // Needs to be in activity intent so that TEA onResume doesn't create a blank activity
        if (getIntent().hasExtra(TaskListFragment.TOKEN_FILTER)) {
            intent.putExtra(TaskListFragment.TOKEN_FILTER, getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER));
        }

        startEditActivity(intent);
    }

    protected void startEditActivity(Intent intent) {
        if (fragmentLayout == LAYOUT_SINGLE) {
            startActivityForResult(intent, TaskListFragment.ACTIVITY_EDIT_TASK);
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
        } else {
            TaskEditFragment editActivity = getTaskEditFragment();
            findViewById(R.id.taskedit_fragment_container).setVisibility(View.VISIBLE);

            if(editActivity == null) {
                editActivity = new TaskEditFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.add(R.id.taskedit_fragment_container, editActivity, TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
                transaction.addToBackStack(null);
                transaction.commit();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Force the transaction to occur so that we can be guaranteed of the fragment existing if we try to present it
                        getFragmentManager().executePendingTransactions();
                    }
                });
            } else {
                editActivity.save(true);
                editActivity.repopulateFromScratch(intent);
            }

            TaskListFragment tlf = getTaskListFragment();
            if (tlf != null) {
                tlf.loadTaskListContent();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isFinishing()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_UPGRADE && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra(UpgradeActivity.EXTRA_RESTART, false)) {
                Timber.w("Upgrade requires restart");
                finish();
                startActivity(getIntent());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
