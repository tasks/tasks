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
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.UpgradeActivity;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.ui.CalendarControlSet;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.DescriptionControlSet;
import org.tasks.ui.EmptyTaskEditFragment;
import org.tasks.ui.NavigationDrawerFragment;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.TaskEditControlFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        TaskEditFragment.TaskEditFragmentCallbackHandler {

    @Inject ActivityPreferences preferences;
    @Inject StartupService startupService;
    @Inject SubtasksHelper subtasksHelper;
    @Inject TaskService taskService;

    public static final int REQUEST_UPGRADE = 505;

    private final RepeatConfirmationReceiver repeatConfirmationReceiver = new RepeatConfirmationReceiver(this);
    private final Map<String, Integer> controlSetFragments = new HashMap<>();
    private NavigationDrawerFragment navigationDrawer;
    private ArrayList<String> controlOrder;

    /** For indicating the new list screen should be launched at fragment setup time */
    public static final String TOKEN_CREATE_NEW_LIST = "createNewList"; //$NON-NLS-1$
    public static final String TOKEN_CREATE_NEW_LIST_NAME = "newListName"; //$NON-NLS-1$

    public static final String OPEN_FILTER = "open_filter"; //$NON-NLS-1$
    public static final String OPEN_TASK = "open_task"; //$NON-NLS-1$

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupService.onStartupApplication(this);
        preferences.applyTheme();

        setContentView(R.layout.task_list_activity);

        navigationDrawer = getNavigationDrawerFragment();
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawer.setUp(drawerLayout);

        registerFragment(EditTitleControlSet.TAG);
        registerFragment(DeadlineControlSet.TAG);
        registerFragment(CalendarControlSet.TAG);
        registerFragment(PriorityControlSet.TAG);
        registerFragment(DescriptionControlSet.TAG);
        registerFragment(HideUntilControlSet.TAG);
        registerFragment(ReminderControlSet.TAG);
        registerFragment(FilesControlSet.TAG);
        registerFragment(TimerControlSet.TAG);
        registerFragment(TagsControlSet.TAG);
        registerFragment(RepeatControlSet.TAG);

        controlOrder = BeastModePreferences.constructOrderedControlList(preferences, this);
        controlOrder.add(0, getString(EditTitleControlSet.TAG));

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
        List<TaskEditControlFragment> taskEditControlFragments = new ArrayList<>();
        if (taskEditFragment != null) {
            if (intent.hasExtra(OPEN_FILTER) || intent.hasExtra(OPEN_TASK)) {
                taskEditFragment.save();
                taskEditFragment = null;
            } else {
                taskEditControlFragments.addAll(taskEditFragment.getFragments());
            }
        }

        TaskListFragment taskListFragment;
        if (intent.hasExtra(OPEN_FILTER)) {
            Filter filter = intent.getParcelableExtra(OPEN_FILTER);
            intent.removeExtra(OPEN_FILTER);
            taskListFragment = newTaskListFragment(filter);
        } else {
            taskListFragment = getTaskListFragment();
            if (taskListFragment == null) {
                taskListFragment = newTaskListFragment(getDefaultFilter());
            }
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
            fragmentTransaction.replace(TaskEditFragment.rowIds[i], taskEditControlFragment, tag);
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
    protected void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(this, repeatConfirmationReceiver);
    }

    protected Filter getDefaultFilter() {
        return BuiltInFilterExposer.getMyTasksFilter(getResources());
    }

    @Override
    public void onFilterItemClicked(FilterListItem item) {
        TaskEditFragment.removeExtrasFromIntent(getIntent());
        TaskEditFragment tef = getTaskEditFragment();
        if (tef != null) {
            getTaskEditFragment().save();
        }

        if(item instanceof Filter) {
            startActivity(TaskIntents.getTaskListIntent(this, (Filter) item));
        }
    }

    private TaskListFragment newTaskListFragment(Filter filter) {
        Class<?> customTaskList = null;

        if (subtasksHelper.shouldUseSubtasksFragmentForFilter(filter)) {
            customTaskList = SubtasksHelper.subtasksClassForFilter(filter);
        }

        Class<?> component = customTaskList;
        if (filter instanceof FilterWithCustomIntent && component == null) {
            try {
                component = Class.forName(((FilterWithCustomIntent) filter).customTaskList.getClassName());
            } catch (Exception e) {
                // Invalid
                Timber.e(e, e.getMessage());
            }
        }
        if (component == null) {
            component = TaskListFragment.class;
        }

        TaskListFragment newFragment;
        try {
            newFragment = (TaskListFragment) component.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Timber.e(e, e.getMessage());
            newFragment = new TaskListFragment();
        }
        newFragment.initialize(filter);
        return newFragment;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (getIntent().hasExtra(OPEN_TASK)) {
            long taskId = getIntent().getLongExtra(OPEN_TASK, 0);
            getIntent().removeExtra(OPEN_TASK);
            if (taskId > 0) {
                onTaskListItemClicked(taskId);
            } else {
                Task task = getTaskListFragment().addTask("");
                onTaskListItemClicked(task.getId());
            }
        }

        if (getIntent().getBooleanExtra(TOKEN_CREATE_NEW_LIST, false)) {
            Intent thisIntent = getIntent();
            Intent newTagIntent = new Intent(this, TagSettingsActivity.class);
            newTagIntent.putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_NAME, thisIntent.getStringExtra(TOKEN_CREATE_NEW_LIST_NAME));
            thisIntent.removeExtra(TOKEN_CREATE_NEW_LIST_NAME);
            startActivityForResult(newTagIntent, NavigationDrawerFragment.REQUEST_NEW_LIST);
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

        String hideAlwaysTrigger = getString(R.string.TEA_ctrl_hide_section_pref);

        taskEditFragment = newTaskEditFragment(isNewTask, task);
        List<TaskEditControlFragment> taskEditControlFragments = new ArrayList<>();
        for (int i = 0 ; i < controlOrder.size() ; i++) {
            String item = controlOrder.get(i);
            if (item.equals(hideAlwaysTrigger)) {
                break;
            }
            Integer resId = controlSetFragments.get(item);
            if (resId == null) {
                Timber.e("Unknown task edit control %s", item);
                continue;
            }

            TaskEditControlFragment fragment = createFragment(resId);
            fragment.initialize(isNewTask, task);
            taskEditControlFragments.add(fragment);
        }
        loadTaskEditFragment(false, taskEditFragment, taskEditControlFragments);
    }

    @Override
    public void onNavigationIconClicked() {
        hideKeyboard();
        navigationDrawer.openDrawer();
    }

    private void registerFragment(int resId) {
        controlSetFragments.put(getString(resId), resId);
    }

    private TaskEditControlFragment createFragment(int fragmentId) {
        switch (fragmentId) {
            case R.string.TEA_ctrl_title_pref:
                return new EditTitleControlSet();
            case R.string.TEA_ctrl_when_pref:
                return new DeadlineControlSet();
            case R.string.TEA_ctrl_importance_pref:
                return new PriorityControlSet();
            case R.string.TEA_ctrl_notes_pref:
                return new DescriptionControlSet();
            case R.string.TEA_ctrl_gcal:
                return new CalendarControlSet();
            case R.string.TEA_ctrl_hide_until_pref:
                return new HideUntilControlSet();
            case R.string.TEA_ctrl_reminders_pref:
                return new ReminderControlSet();
            case R.string.TEA_ctrl_files_pref:
                return new FilesControlSet();
            case R.string.TEA_ctrl_timer_pref:
                return new TimerControlSet();
            case R.string.TEA_ctrl_lists_pref:
                return new TagsControlSet();
            case R.string.TEA_ctrl_repeat_pref:
                return new RepeatControlSet();
            default:
                throw new RuntimeException("Unsupported fragment");
        }
    }

    @Override
    public void onBackPressed() {
        if (navigationDrawer.isDrawerOpen()) {
            navigationDrawer.closeMenu();
            return;
        }

        if (getTaskEditFragment() != null) {
            getTaskEditFragment().discardButtonClick();
            return;
        }

        if (isFinishing()) {
            return;
        }

        super.onBackPressed();
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
        getFragmentManager().popBackStack(TaskEditFragment.TAG_TASKEDIT_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        hideKeyboard();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
