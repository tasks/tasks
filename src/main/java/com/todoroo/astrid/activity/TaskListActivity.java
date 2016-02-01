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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.FilterSettingsActivity;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.UpgradeActivity;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.widget.TasksWidget;

import org.tasks.R;
import org.tasks.activities.SortActivity;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.ui.CalendarControlSet;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.DescriptionControlSet;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.NavigationDrawerFragment;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.TaskEditControlFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.todoroo.astrid.activity.TaskEditFragment.newTaskEditFragment;
import static com.todoroo.astrid.voice.VoiceInputAssistant.voiceInputAvailable;
import static org.tasks.ui.NavigationDrawerFragment.OnFilterItemClickedListener;

public class TaskListActivity extends InjectingAppCompatActivity implements
        OnFilterItemClickedListener,
        TaskListFragment.OnTaskListItemClickedListener,
        PriorityControlSet.OnPriorityChanged,
        TimerControlSet.TimerControlSetCallback,
        RepeatControlSet.RepeatChangedListener,
        TaskEditFragment.TaskEditFragmentCallbackHandler {

    @Inject TagDataDao tagDataDao;
    @Inject ActivityPreferences preferences;
    @Inject VoiceInputAssistant voiceInputAssistant;
    @Inject StartupService startupService;
    @Inject SubtasksHelper subtasksHelper;
    @Inject TaskService taskService;
    @Inject TaskCreator taskCreator;

    @Bind(R.id.toolbar) Toolbar toolbar;

    public static final int REQUEST_UPGRADE = 505;
    private static final int REQUEST_EDIT_TAG = 11543;
    private static final int REQUEST_EDIT_FILTER = 11544;
    private static final int REQUEST_SORT = 11545;

    private final RepeatConfirmationReceiver repeatConfirmationReceiver = new RepeatConfirmationReceiver(this);
    private final Map<String, Integer> controlSetFragments = new HashMap<>();
    private NavigationDrawerFragment navigationDrawer;
    private ArrayList<String> controlOrder;

    public static final String TOKEN_SWITCH_TO_FILTER = "newListCreated"; //$NON-NLS-1$

    /** For indicating the new list screen should be launched at fragment setup time */
    public static final String TOKEN_CREATE_NEW_LIST = "createNewList"; //$NON-NLS-1$
    public static final String TOKEN_CREATE_NEW_LIST_NAME = "newListName"; //$NON-NLS-1$

    public static final String OPEN_TASK = "openTask"; //$NON-NLS-1$

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupService.onStartupApplication(this);
        preferences.applyTheme();

        setContentView(R.layout.task_list_wrapper);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        updateToolbar(R.drawable.ic_menu_24dp, true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskEditFragment taskEditFragment = getTaskEditFragment();
                if (isDoublePaneLayout() || taskEditFragment == null) {
                    hideKeyboard();
                    navigationDrawer.openDrawer();
                } else {
                    taskEditFragment.onBackPressed();
                }
            }
        });

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

        readIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        readIntent();
    }

    private void readIntent() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            extras = (Bundle) extras.clone();
        }

        if (extras == null) {
            extras = new Bundle();
        }

        TaskListFragment taskListFragment;
        if (intent.hasExtra(TaskListFragment.TOKEN_FILTER)) {
            Filter filter = intent.getParcelableExtra(TaskListFragment.TOKEN_FILTER);
            extras.putAll(configureIntentAndExtrasWithFilter(intent, filter));
            taskListFragment = newTaskListFragment(filter, extras);
        } else {
            taskListFragment = getTaskListFragment();
            if (taskListFragment == null) {
                Filter filter = getDefaultFilter();
                extras.putAll(configureIntentAndExtrasWithFilter(intent, filter));
                taskListFragment = newTaskListFragment(filter, extras);
            }
        }

        TaskEditFragment taskEditFragment = getTaskEditFragment();
        List<TaskEditControlFragment> taskEditControlFragments = new ArrayList<>();
        if (taskEditFragment != null) {
            for (int rowId : TaskEditFragment.rowIds) {
                TaskEditControlFragment fragment = (TaskEditControlFragment) getFragmentManager().findFragmentById(rowId);
                if (fragment == null) {
                    break;
                }
                taskEditControlFragments.add(fragment);
            }
        }

        loadTaskListFragment(true, taskListFragment);

        if (taskEditFragment != null) {
            loadTaskEditFragment(true, taskEditFragment, taskEditControlFragments);
        }
    }

    private void loadTaskListFragment(boolean onCreate, TaskListFragment taskListFragment) {
        Filter filter = taskListFragment.getFilter();
        getSupportActionBar().setTitle(filter.listingTitle);
        FragmentManager fragmentManager = getFragmentManager();
        if (onCreate) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fragmentManager.beginTransaction()
                .replace(isDoublePaneLayout() ? R.id.master_dual : R.id.single_pane, taskListFragment, TaskListFragment.TAG_TASKLIST_FRAGMENT)
                .addToBackStack(TaskListFragment.TAG_TASKLIST_FRAGMENT)
                .commit();
    }

    private void loadTaskEditFragment(boolean onCreate, TaskEditFragment taskEditFragment, List<TaskEditControlFragment> taskEditControlFragments) {
        if (isSinglePaneLayout()) {
            updateToolbar(R.drawable.ic_arrow_back_24dp, false);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_list_activity, menu);
        MenuColorizer.colorMenu(this, menu, getResources().getColor(android.R.color.white));
        TaskListFragment tlf = getTaskListFragment();
        MenuItem hidden = menu.findItem(R.id.menu_show_hidden);
        if (preferences.getBoolean(R.string.p_show_hidden_tasks, false)) {
            hidden.setChecked(true);
        }
        MenuItem completed = menu.findItem(R.id.menu_show_completed);
        if (preferences.getBoolean(R.string.p_show_completed_tasks, false)) {
            completed.setChecked(true);
        }
        if (tlf instanceof GtasksListFragment) {
            menu.findItem(R.id.menu_clear_completed).setVisible(true);
            menu.findItem(R.id.menu_sort).setVisible(false);
            completed.setChecked(true);
            completed.setEnabled(false);
        } else if(tlf instanceof TagViewFragment) {
            menu.findItem(R.id.menu_tag_settings).setVisible(true);
        } else if (tlf != null) {
            Filter filter = tlf.getFilter();
            if(filter != null && filter instanceof CustomFilter && ((CustomFilter) filter).getId() > 0) {
                menu.findItem(R.id.menu_filter_settings).setVisible(true);
            }
        }

        if (tlf instanceof SubtasksTagListFragment || tlf instanceof SubtasksListFragment) {
            hidden.setChecked(true);
            hidden.setEnabled(false);
        }

        menu.findItem(R.id.menu_voice_add).setVisible(voiceInputAvailable(this));
        final MenuItem item = menu.findItem(R.id.menu_search);
        final SearchView actionView = (SearchView) MenuItemCompat.getActionView(item);
        actionView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                query = query.trim();
                String title = getString(R.string.FLA_search_filter, query);
                Filter savedFilter = new Filter(title,
                        new QueryTemplate().where
                                (Criterion.or(Task.NOTES.
                                                        like(
                                                                "%" + //$NON-NLS-1$
                                                                        query + "%"
                                                        ),
                                                Task.TITLE.
                                                        like(
                                                                "%" + //$NON-NLS-1$
                                                                        query + "%"
                                                        )
                                        )
                                ), null);

                onFilterItemClicked(savedFilter);
                MenuItemCompat.collapseActionView(item);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });
        return true;
    }

    protected Filter getDefaultFilter() {
        return BuiltInFilterExposer.getMyTasksFilter(getResources());
    }

    @Override
    public void onFilterItemClicked(FilterListItem item) {
        TaskEditFragment.removeExtrasFromIntent(getIntent());
        TaskEditFragment tef = getTaskEditFragment();
        if (tef != null) {
            getTaskEditFragment().onBackPressed();
        }

        // If showing both fragments, directly update the tasklist-fragment
        Intent intent = getIntent();

        if(item instanceof Filter) {
            Filter filter = (Filter)item;

            Bundle extras = configureIntentAndExtrasWithFilter(intent, filter);
            TaskListFragment newFragment = newTaskListFragment(filter, extras);

            loadTaskListFragment(false, newFragment);
        }
    }

    private TaskListFragment newTaskListFragment(Filter filter, Bundle extras) {
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
        Bundle args = new Bundle();
        args.putBundle(TaskListFragment.TOKEN_EXTRAS, extras);
        newFragment.setArguments(args);
        return newFragment;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (getIntent().hasExtra(TOKEN_SWITCH_TO_FILTER)) {
            Filter newList = getIntent().getParcelableExtra(TOKEN_SWITCH_TO_FILTER);
            getIntent().removeExtra(TOKEN_SWITCH_TO_FILTER);
            onFilterItemClicked(newList);
        }

        if (getIntent().hasExtra(OPEN_TASK)) {
            long id = getIntent().getLongExtra(OPEN_TASK, 0);
            if (id > 0) {
                onTaskListItemClicked(id);
            } else {
                TaskListFragment tlf = getTaskListFragment();
                if (tlf != null) {
                    Task task = tlf.addTask("");//$NON-NLS-1$
                    onTaskListItemClicked(task.getId());
                }
            }
            getIntent().removeExtra(OPEN_TASK);
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
            taskEditFragment.onBackPressed();
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
            getTaskEditFragment().onBackPressed();
            return;
        }

        if (isFinishing()) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Callback<String> quickAddTask = new Callback<String>() {
            @Override
            public void apply(String title) {
                TaskListFragment taskListFragment = getTaskListFragment();
                Task task = taskListFragment.addTask(title);
                taskCreator.addToCalendar(task);
                onTaskListItemClicked(task.getId());
                taskListFragment.loadTaskListContent();
                taskListFragment.selectCustomId(task.getId());
                taskListFragment.onTaskCreated(task.getId(), task.getUUID());
            }
        };
        if (voiceInputAssistant.handleActivityResult(requestCode, resultCode, data, quickAddTask)) {
            return;
        }

        if ((requestCode == NavigationDrawerFragment.REQUEST_NEW_LIST ||
                requestCode == TaskListFragment.ACTIVITY_REQUEST_NEW_FILTER) &&
                resultCode == Activity.RESULT_OK) {
            if(data == null) {
                return;
            }

            Filter newList = data.getParcelableExtra(TagSettingsActivity.TOKEN_NEW_FILTER);
            if (newList != null) {
                getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, newList); // Handle in onPostResume()
                navigationDrawer.clear();
            }

            navigationDrawer.refresh();
        } else if (requestCode == REQUEST_EDIT_TAG) {
            if (resultCode == RESULT_OK) {
                String action = data.getAction();
                String uuid = data.getStringExtra(TagViewFragment.EXTRA_TAG_UUID);
                TaskListFragment tlf = getTaskListFragment();
                if (AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED.equals(action)) {
                    if (tlf != null) {
                        TagData td = tlf.getActiveTagData();
                        if (td != null && td.getUuid().equals(uuid)) {
                            td = tagDataDao.fetch(uuid, TagData.PROPERTIES);
                            if (td != null) {
                                Filter filter = TagFilterExposer.filterFromTagData(this, td);
                                getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, filter);
                            }
                        } else {
                            tlf.refresh();
                        }
                    }
                } else if (AstridApiConstants.BROADCAST_EVENT_TAG_DELETED.equals(action)) {
                    if (tlf != null) {
                        TagData tagData = tlf.getActiveTagData();
                        String activeUuid = RemoteModel.NO_UUID;
                        if (tagData != null) {
                            activeUuid = tagData.getUuid();
                        }
                        if (activeUuid.equals(uuid)) {
                            getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, BuiltInFilterExposer.getMyTasksFilter(getResources())); // Handle in onPostResume()
                            navigationDrawer.clear(); // Should auto refresh
                        } else {
                            tlf.refresh();
                        }
                    }
                }

                navigationDrawer.refresh();
            }
        } else if (requestCode == REQUEST_EDIT_FILTER) {
            if (resultCode == RESULT_OK) {
                String action = data.getAction();
                if (AstridApiConstants.BROADCAST_EVENT_FILTER_RENAMED.equals(action)) {
                    CustomFilter customFilter = data.getParcelableExtra(FilterSettingsActivity.TOKEN_FILTER);
                    getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, customFilter);
                } else if(AstridApiConstants.BROADCAST_EVENT_FILTER_DELETED.equals(action)) {
                    getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, BuiltInFilterExposer.getMyTasksFilter(getResources()));
                }

                navigationDrawer.refresh();
            }
        } else if (requestCode == REQUEST_SORT) {
            if (resultCode == RESULT_OK && data != null) {
                TasksWidget.updateWidgets(this);

                if (data.hasExtra(SortActivity.EXTRA_TOGGLE_MANUAL)) {
                    getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, getTaskListFragment().getFilter());
                } else {
                    getTaskListFragment().setUpTaskList();
                }
            }
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

    protected void refreshTaskList() {
        TaskListFragment tlf = getTaskListFragment();
        if (tlf != null) {
            tlf.refresh();
        }
    }

    public void refreshFilterCount() {
        navigationDrawer.refreshFilterCount();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final TaskListFragment tlf = getTaskListFragment();
        switch(item.getItemId()) {
            case R.id.menu_voice_add:
                voiceInputAssistant.startVoiceRecognitionActivity(R.string.voice_create_prompt);
                return true;
            case R.id.menu_sort:
                startActivityForResult(new Intent(this, SortActivity.class) {{
                    putExtra(SortActivity.EXTRA_MANUAL_ENABLED, tlf.hasDraggableOption());
                }}, REQUEST_SORT);
                return true;
            case R.id.menu_tag_settings:
                startActivityForResult(new Intent(this, TagSettingsActivity.class) {{
                    putExtra(TagSettingsActivity.EXTRA_TAG_DATA, getTaskListFragment().getActiveTagData());
                }}, REQUEST_EDIT_TAG);
                return true;
            case R.id.menu_show_hidden:
                item.setChecked(!item.isChecked());
                preferences.setBoolean(R.string.p_show_hidden_tasks, item.isChecked());
                tlf.reconstructCursor();
                TasksWidget.updateWidgets(this);
                return true;
            case R.id.menu_show_completed:
                item.setChecked(!item.isChecked());
                preferences.setBoolean(R.string.p_show_completed_tasks, item.isChecked());
                tlf.reconstructCursor();
                TasksWidget.updateWidgets(this);
                return true;
            case R.id.menu_filter_settings:
                startActivityForResult(new Intent(this, FilterSettingsActivity.class) {{
                    putExtra(FilterSettingsActivity.TOKEN_FILTER, tlf.getFilter());
                }}, REQUEST_EDIT_FILTER);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public TaskListFragment getTaskListFragment() {
        return (TaskListFragment) getFragmentManager()
                .findFragmentByTag(TaskListFragment.TAG_TASKLIST_FRAGMENT);
    }

    public TaskEditFragment getTaskEditFragment() {
        return (TaskEditFragment) getFragmentManager()
                .findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
    }

    protected void updateToolbar(int drawableResId, boolean showTitle) {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(showTitle);
            Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(drawableResId));
            DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
            supportActionBar.setHomeAsUpIndicator(drawable);
        }
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

        if (model.getTitle().length() == 0) {
            // set deletion date until task gets a title
            model.setDeletionDate(DateUtilities.now());
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

    public boolean isSinglePaneLayout() {
        return !isDoublePaneLayout();
    }

    public boolean isDoublePaneLayout() {
        return getResources().getBoolean(R.bool.two_pane_layout);
    }

    @Override
    public void taskEditFinished() {
        getFragmentManager().popBackStack(TaskEditFragment.TAG_TASKEDIT_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (isSinglePaneLayout()) {
            updateToolbar(R.drawable.ic_menu_24dp, true);
        }
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
