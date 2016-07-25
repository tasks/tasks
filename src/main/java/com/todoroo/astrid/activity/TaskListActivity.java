/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
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

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.SortDialog;
import org.tasks.fragments.CommentBarFragment;
import org.tasks.gtasks.GoogleTaskListSelectionHandler;
import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.EmptyTaskEditFragment;
import org.tasks.ui.NavigationDrawerFragment;
import org.tasks.ui.PriorityControlSet;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
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
        CommentBarFragment.CommentBarFragmentCallback,
        SortDialog.SortDialogCallback,
        GoogleTaskListSelectionHandler {

    @Inject Preferences preferences;
    @Inject StartupService startupService;
    @Inject SubtasksHelper subtasksHelper;
    @Inject TaskService taskService;
    @Inject RepeatConfirmationReceiver repeatConfirmationReceiver;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject GtasksListService gtasksListService;
    @Inject TagDataDao tagDataDao;
    @Inject Theme theme;
    @Inject Broadcaster broadcaster;
    @Inject ThemeCache themeCache;
    @Inject SyncAdapterHelper syncAdapterHelper;
    @Inject Tracker tracker;

    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;

    public static final int REQUEST_UPGRADE = 505;

    private NavigationDrawerFragment navigationDrawer;

    /** For indicating the new list screen should be launched at fragment setup time */
    public static final String TOKEN_CREATE_NEW_LIST_NAME = "newListName"; //$NON-NLS-1$
    private static final String FRAG_TAG_TASK_LIST = "frag_tag_task_list";

    public static final String OPEN_FILTER = "open_filter"; //$NON-NLS-1$
    public static final String LOAD_FILTER = "load_filter";
    public static final String OPEN_TASK = "open_task"; //$NON-NLS-1$
    private int currentNightMode;

    private Filter filter;

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentNightMode = getNightMode();

        startupService.onStartupApplication(this);

        setContentView(R.layout.task_list_activity);

        ButterKnife.bind(this);

        navigationDrawer = getNavigationDrawerFragment();
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
        if (taskEditFragment != null) {
            if (intent.hasExtra(OPEN_FILTER) || intent.hasExtra(LOAD_FILTER) || intent.hasExtra(OPEN_TASK)) {
                taskEditFragment.save();
                taskEditFragment = null;
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
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_dual, new EmptyTaskEditFragment())
                    .commit();
        }

        if (taskEditFragment != null) {
            loadTaskEditFragment(taskEditFragment);
        }
    }

    private void loadTaskListFragment(TaskListFragment taskListFragment) {
        filter = taskListFragment.filter;
        ThemeColor themeColor = filter.tint >= 0
                ? themeCache.getThemeColor(filter.tint)
                : theme.getThemeColor();
        themeColor.applyStatusBarColor(drawerLayout);
        themeColor.applyTaskDescription(this, filter.listingTitle);
        theme.withColor(themeColor).applyToContext(this);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentManager.beginTransaction()
                .replace(isDoublePaneLayout() ? R.id.master_dual : R.id.single_pane, taskListFragment, FRAG_TAG_TASK_LIST)
                .addToBackStack(FRAG_TAG_TASK_LIST)
                .commit();
    }

    private void loadTaskEditFragment(TaskEditFragment taskEditFragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(isDoublePaneLayout() ? R.id.detail_dual : R.id.single_pane, taskEditFragment, TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                .addToBackStack(TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                .commit();
    }

    public NavigationDrawerFragment getNavigationDrawerFragment() {
        return (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(NavigationDrawerFragment.FRAGMENT_NAVIGATION_DRAWER);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentNightMode != getNightMode()) {
            tracker.reportEvent(Tracking.Events.NIGHT_MODE_MISMATCH);
            restart();
            return;
        }

        registerReceiver(
                repeatConfirmationReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_EVENT_TASK_REPEATED));

        if (syncAdapterHelper.shouldShowBackgroundSyncWarning() && !preferences.getBoolean(R.string.p_sync_warning_shown, false)) {
            TaskListFragment taskListFragment = getTaskListFragment();
            if (taskListFragment != null) {
                taskListFragment.makeSnackbar(R.string.master_sync_warning)
                        .setAction(R.string.TLA_menu_settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS) {{
                                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                }});
                            }
                        })
                        .setCallback(new Snackbar.Callback() {
                            @Override
                            public void onShown(Snackbar snackbar) {
                                preferences.setBoolean(R.string.p_sync_warning_shown, true);
                            }
                        })
                        .show();
            }
        }
    }

    public void restart() {
        Intent intent = getIntent();
        intent.putExtra(TaskListActivity.OPEN_FILTER, getCurrentFilter());
        finish();
        startActivity(intent);
    }

    private int getNightMode() {
        return getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
        theme.applyTheme(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(this, repeatConfirmationReceiver);
    }

    @Override
    public void onFilterItemClicked(FilterListItem item) {
        if (item == null) {
            item = defaultFilterProvider.getDefaultFilter();
        }
        TaskEditFragment tef = getTaskEditFragment();
        if (tef != null) {
            getTaskEditFragment().save();
        }

        if(item instanceof Filter) {
            startActivity(TaskIntents.getTaskListIntent(this, (Filter) item));
        }
    }

    private TaskListFragment newTaskListFragment(Filter filter) {
        navigationDrawer.closeDrawer();
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
            navigationDrawer.closeDrawer();
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
        loadTaskEditFragment(newTaskEditFragment(isNewTask, task));
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
        } else {
            getSupportFragmentManager().popBackStackImmediate(FRAG_TAG_TASK_LIST, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
            }

            repopulateNavigationDrawer();
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

    public void repopulateNavigationDrawer() {
        navigationDrawer.repopulateList();
    }

    public TaskListFragment getTaskListFragment() {
        return (TaskListFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAG_TAG_TASK_LIST);
    }

    public TaskEditFragment getTaskEditFragment() {
        return (TaskEditFragment) getSupportFragmentManager()
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
        getSupportFragmentManager().popBackStackImmediate(TaskEditFragment.TAG_TASKEDIT_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

    @Override
    public void sortChanged() {
        broadcaster.refresh();
        reloadCurrentFilter();
    }

    void reloadCurrentFilter() {
        onFilterItemClicked(getCurrentFilter());
    }

    public Filter getCurrentFilter() {
        return filter;
    }

    @Override
    public void selectedList(GtasksList list) {
        getTaskEditFragment().onGoogleTaskListChanged(list);
    }
}
