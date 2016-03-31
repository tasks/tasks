/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.UpgradeActivity;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.timers.TimerControlSet;

import org.tasks.R;
import org.tasks.fragments.CommentBarFragment;
import org.tasks.fragments.TaskEditControlSetFragmentManager;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.ThemeApplicator;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.ui.EmptyTaskEditFragment;
import org.tasks.ui.NavigationDrawerFragment;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.TaskEditControlFragment;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.astrid.activity.TaskEditFragment.newTaskEditFragment;
import static org.tasks.ui.NavigationDrawerFragment.OnFilterItemClickedListener;

public class TaskListActivity extends InjectingAppCompatActivity implements
        OnFilterItemClickedListener,
        TaskListFragment.TaskListFragmentCallbackHandler,
        PriorityControlSet.OnPriorityChanged,
        TimerControlSet.TimerControlSetCallback,
        RepeatControlSet.RepeatChangedListener,
        TaskEditFragment.TaskEditFragmentCallbackHandler,
        CommentBarFragment.CommentBarFragmentCallback {

    @Inject Preferences preferences;
    @Inject StartupService startupService;
    @Inject SubtasksHelper subtasksHelper;
    @Inject TaskService taskService;
    @Inject TaskEditControlSetFragmentManager taskEditControlSetFragmentManager;
    @Inject RepeatConfirmationReceiver repeatConfirmationReceiver;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject GtasksListService gtasksListService;
    @Inject TagDataDao tagDataDao;
    @Inject ThemeApplicator themeApplicator;

    public static final int REQUEST_UPGRADE = 505;

    private NavigationDrawerFragment navigationDrawer;

    /** For indicating the new list screen should be launched at fragment setup time */
    public static final String TOKEN_CREATE_NEW_LIST_NAME = "newListName"; //$NON-NLS-1$

    public static final String OPEN_FILTER = "open_filter"; //$NON-NLS-1$
    public static final String LOAD_FILTER = "load_filter";
    public static final String OPEN_TASK = "open_task"; //$NON-NLS-1$

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeApplicator.applyTheme();

        startupService.onStartupApplication(this);

        setContentView(R.layout.task_list_activity);

        navigationDrawer = getNavigationDrawerFragment();
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawer.setUp(drawerLayout);

        handleIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        handleIntent();
    }

    private void handleIntent() {
        Intent intent = getIntent();

        TaskEditFragment taskEditFragment = getTaskEditFragment();
        List<TaskEditControlFragment> taskEditControlFragments = null;
        if (taskEditFragment != null) {
            if (intent.hasExtra(OPEN_FILTER) || intent.hasExtra(LOAD_FILTER) || intent.hasExtra(OPEN_TASK)) {
                taskEditFragment.save();
                taskEditFragment = null;
            } else {
                taskEditControlFragments = taskEditControlSetFragmentManager.getFragmentsInDisplayOrder();
            }
        }

        TaskListFragment taskListFragment;
        if (intent.hasExtra(OPEN_FILTER)) {
            Filter filter = intent.getParcelableExtra(OPEN_FILTER);
            intent.removeExtra(OPEN_FILTER);
            taskListFragment = newTaskListFragment(filter);
        } else if (intent.hasExtra(LOAD_FILTER)) {
            Filter filter = defaultFilterProvider.getFilterFromPreference(intent.getStringExtra(LOAD_FILTER));
            intent.removeExtra(LOAD_FILTER);
            taskListFragment = newTaskListFragment(filter);
        } else {
            taskListFragment = getTaskListFragment();
        }

        if (taskListFragment == null) {
            taskListFragment = newTaskListFragment(defaultFilterProvider.getDefaultFilter());
        }
        loadTaskListFragment(taskListFragment);

        if (isDoublePaneLayout()) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.detail_dual, new EmptyTaskEditFragment())
                    .commit();
        }

        if (taskEditFragment != null) {
            loadTaskEditFragment(true, taskEditFragment, taskEditControlFragments);
        }
    }

    private void loadTaskListFragment(TaskListFragment taskListFragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentManager.beginTransaction()
                .replace(isDoublePaneLayout() ? R.id.master_dual : R.id.single_pane, taskListFragment, TaskListFragment.TAG_TASKLIST_FRAGMENT)
                .addToBackStack(TaskListFragment.TAG_TASKLIST_FRAGMENT)
                .commit();
    }

    private void loadTaskEditFragment(boolean onCreate, TaskEditFragment taskEditFragment, List<TaskEditControlFragment> taskEditControlFragments) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(isDoublePaneLayout() ? R.id.detail_dual : R.id.single_pane, taskEditFragment, TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                .addToBackStack(TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                .commit();

        if (onCreate) {
            fragmentManager.executePendingTransactions();
        }

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        for (int i = 0 ; i < taskEditControlFragments.size() ; i++) {
            TaskEditControlFragment taskEditControlFragment = taskEditControlFragments.get(i);
            String tag = getString(taskEditControlFragment.controlId());
            fragmentTransaction.replace(TaskEditControlSetFragmentManager.TASK_EDIT_CONTROL_FRAGMENT_ROWS[i], taskEditControlFragment, tag);
        }
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public NavigationDrawerFragment getNavigationDrawerFragment() {
        return (NavigationDrawerFragment) getFragmentManager()
                .findFragmentById(NavigationDrawerFragment.FRAGMENT_NAVIGATION_DRAWER);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(
                repeatConfirmationReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_EVENT_TASK_REPEATED));
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(this, repeatConfirmationReceiver);
    }

    @Override
    public void onFilterItemClicked(FilterListItem item) {
        TaskEditFragment tef = getTaskEditFragment();
        if (tef != null) {
            getTaskEditFragment().save();
        }

        if(item instanceof Filter) {
            startActivity(TaskIntents.getTaskListIntent(this, (Filter) item));
        }
    }

    private TaskListFragment newTaskListFragment(Filter filter) {
        if (filter instanceof TagFilter) {
            TagFilter tagFilter = (TagFilter) filter;
            TagData tagData = tagDataDao.getByUuid(tagFilter.getUuid());
            if (tagData != null) {
                return preferences.getBoolean(R.string.p_manual_sort, false)
                        ? SubtasksTagListFragment.newSubtasksTagListFragment(tagFilter, tagData)
                        : TagViewFragment.newTagViewFragment(tagFilter, tagData);
            }
        } else if (filter instanceof GtasksFilter) {
            GtasksFilter gtasksFilter = (GtasksFilter) filter;
            GtasksList list = gtasksListService.getList(gtasksFilter.getStoreId());
            if (list != null) {
                return GtasksListFragment.newGtasksListFragment(gtasksFilter, list);
            }
        } else if (filter != null) {
            return subtasksHelper.shouldUseSubtasksFragmentForFilter(filter)
                    ? SubtasksListFragment.newSubtasksListFragment(filter)
                    : TaskListFragment.newTaskListFragment(filter);
        }

        return null;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        Intent intent = getIntent();

        if (intent.hasExtra(OPEN_TASK)) {
            long taskId = intent.getLongExtra(OPEN_TASK, 0);
            intent.removeExtra(OPEN_TASK);
            if (taskId > 0) {
                onTaskListItemClicked(taskId);
            } else {
                Task task = getTaskListFragment().addTask("");
                onTaskListItemClicked(task.getId());
            }
        }

        if (intent.hasExtra(TOKEN_CREATE_NEW_LIST_NAME)) {
            final String listName = intent.getStringExtra(TOKEN_CREATE_NEW_LIST_NAME);
            intent.removeExtra(TOKEN_CREATE_NEW_LIST_NAME);
            startActivityForResult(new Intent(TaskListActivity.this, TagSettingsActivity.class) {{
                putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_NAME, listName);
            }}, NavigationDrawerFragment.REQUEST_NEW_LIST);
        }
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        TaskEditFragment taskEditFragment = getTaskEditFragment();

        if (taskEditFragment != null) {
            taskEditFragment.save();
        }

        Task task = loadItem(taskId);
        if (task == null) {
            Timber.e(new NullPointerException(), "Failed to load task id %s", taskId);
            return;
        }
        boolean isNewTask = task.getTitle().length() == 0;
        loadTaskEditFragment(
                false,
                newTaskEditFragment(isNewTask, task),
                taskEditControlSetFragmentManager.createNewFragments(isNewTask, task));
    }

    @Override
    public void onNavigationIconClicked() {
        hideKeyboard();
        navigationDrawer.openDrawer();
    }

    @Override
    public void onBackPressed() {
        if (navigationDrawer.isDrawerOpen()) {
            navigationDrawer.closeDrawer();
            return;
        }

        TaskEditFragment taskEditFragment = getTaskEditFragment();
        if (taskEditFragment != null) {
            if (preferences.backButtonSavesTask()) {
                taskEditFragment.save();
            } else {
                taskEditFragment.discardButtonClick();
            }
        } else if (!isFinishing()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == NavigationDrawerFragment.REQUEST_NEW_LIST ||
                requestCode == TaskListFragment.ACTIVITY_REQUEST_NEW_FILTER) &&
                resultCode == Activity.RESULT_OK) {
            if(data == null) {
                return;
            }

            Filter newList = data.getParcelableExtra(TagSettingsActivity.TOKEN_NEW_FILTER);
            if (newList != null) {
                onFilterItemClicked(newList);
                navigationDrawer.clear();
            }

            navigationDrawer.refresh();
        } else if (requestCode == REQUEST_UPGRADE) {
            if (resultCode == RESULT_OK) {
                if (data != null && data.getBooleanExtra(UpgradeActivity.EXTRA_RESTART, false)) {
                    Timber.w("Upgrade requires restart");
                    finish();
                    startActivity(getIntent());
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void refreshNavigationDrawer() {
        navigationDrawer.refresh();
    }

    public void clearNavigationDrawer() {
        navigationDrawer.clear();
    }

    protected void tagsChanged() {
        tagsChanged(false);
    }

    private void tagsChanged(boolean onActivityResult) {
        if (onActivityResult) {
            navigationDrawer.clear();
        } else {
            navigationDrawer.refresh();
        }
    }

    public void refreshFilterCount() {
        navigationDrawer.refreshFilterCount();
    }

    public TaskListFragment getTaskListFragment() {
        return (TaskListFragment) getFragmentManager()
                .findFragmentByTag(TaskListFragment.TAG_TASKLIST_FRAGMENT);
    }

    public TaskEditFragment getTaskEditFragment() {
        return (TaskEditFragment) getFragmentManager()
                .findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
    }

    /**
     * Loads action item from the given intent
     */
    private Task loadItem(long taskId) {
        Task model = null;

        if (taskId> -1L) {
            model = taskService.fetchById(taskId, Task.PROPERTIES);
        }

        // not found by id or was never passed an id
        if (model == null) {
            Intent intent = getIntent();
            String valuesAsString = intent.getStringExtra(TaskEditFragment.TOKEN_VALUES);
            ContentValues values = null;
            try {
                if (valuesAsString != null) {
                    valuesAsString = PermaSql.replacePlaceholders(valuesAsString);
                    values = AndroidUtilities.contentValuesFromSerializedString(valuesAsString);
                }
            } catch (Exception e) {
                // oops, can't serialize
                Timber.e(e, e.getMessage());
            }
            model = taskService.createWithValues(values, null);
        }

        return model;
    }

    @Override
    public void onPriorityChange(int priority) {
        getTaskEditFragment().onPriorityChange(priority);
    }

    @Override
    public void repeatChanged(boolean repeat) {
        getTaskEditFragment().onRepeatChanged(repeat);
    }

    @Override
    public Task stopTimer() {
        return getTaskEditFragment().stopTimer();
    }

    @Override
    public Task startTimer() {
        return getTaskEditFragment().startTimer();
    }

    public boolean isDoublePaneLayout() {
        return getResources().getBoolean(R.bool.two_pane_layout);
    }

    @Override
    public void taskEditFinished() {
        getFragmentManager().popBackStackImmediate(TaskEditFragment.TAG_TASKEDIT_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        hideKeyboard();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void addComment(String message, String actionCode, String picture) {
        TaskEditFragment taskEditFragment = getTaskEditFragment();
        if (taskEditFragment != null) {
            taskEditFragment.addComment(message, actionCode, picture);
        }
    }
}
