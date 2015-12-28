/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.FilterSettingsActivity;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.widget.TasksWidget;

import org.tasks.R;
import org.tasks.activities.SortActivity;
import org.tasks.analytics.Tracker;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.BasicPreferences;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.NavigationDrawerFragment;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.astrid.voice.VoiceInputAssistant.voiceInputAvailable;
import static org.tasks.ui.NavigationDrawerFragment.OnFilterItemClickedListener;

public class TaskListActivity extends AstridActivity implements OnFilterItemClickedListener {

    @Inject TagDataDao tagDataDao;
    @Inject ActivityPreferences preferences;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject VoiceInputAssistant voiceInputAssistant;
    @Inject Tracker tracker;

    private static final int REQUEST_EDIT_TAG = 11543;
    private static final int REQUEST_EDIT_FILTER = 11544;
    private static final int REQUEST_SORT = 11545;

    private final RepeatConfirmationReceiver repeatConfirmationReceiver = new RepeatConfirmationReceiver(this);
    private NavigationDrawerFragment navigationDrawer;

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
        preferences.applyTheme();

        setContentView(R.layout.task_list_wrapper);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_menu_24dp));
            DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
            toolbar.setNavigationIcon(drawable);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationDrawer.openDrawer();
            }
        });

        navigationDrawer = getNavigationDrawerFragment();
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawer.setUp(drawerLayout);

        View editFragment = findViewById(R.id.taskedit_fragment_container);

        if(editFragment != null) {
            fragmentLayout = LAYOUT_DOUBLE;
        } else {
            fragmentLayout = LAYOUT_SINGLE;
        }

        readIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        readIntent();
    }

    private void readIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            extras = (Bundle) extras.clone();
        }

        if (extras == null) {
            extras = new Bundle();
        }

        Filter savedFilter = getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER);
        if (savedFilter == null) {
            savedFilter = getDefaultFilter();
            extras.putAll(configureIntentAndExtrasWithFilter(getIntent(), savedFilter));
        }

        extras.putParcelable(TaskListFragment.TOKEN_FILTER, savedFilter);
        setupTasklistFragmentWithFilter(savedFilter, extras);
        setListsTitle(savedFilter.listingTitle);
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
        getTaskListFragment().setSyncOngoing(gtasksPreferenceService.isOngoing());
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
        } else {
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
                                                                                    like (
                                                                                              "%" + //$NON-NLS-1$
                                                                                              query + "%"
                                                                                         ),
                                                                                    Task.TITLE.
                                                                                     like (
                                                                                              "%" + //$NON-NLS-1$
                                                                                              query + "%"
                                                                                          )
                                                                                  )
                                                                    ),null);

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
    public boolean onFilterItemClicked(FilterListItem item) {
        TaskEditFragment.removeExtrasFromIntent(getIntent());
        TaskEditFragment tef = getTaskEditFragment();
        if (tef != null) {
            onBackPressed();
        }

        if ((item instanceof Filter)) {
            setSelectedItem((Filter) item);
        }
        // If showing both fragments, directly update the tasklist-fragment
        Intent intent = getIntent();

        if(item instanceof Filter) {
            Filter filter = (Filter)item;

            Bundle extras = configureIntentAndExtrasWithFilter(intent, filter);
            if (fragmentLayout == LAYOUT_DOUBLE && getTaskEditFragment() != null) {
                onBackPressed(); // remove the task edit fragment when switching between lists
            }
            setupTasklistFragmentWithFilter(filter, extras);

            // no animation for dualpane-layout
            AndroidUtilities.callOverridePendingTransition(this, 0, 0);
            return true;
        }
        return false;
    }

    public void setupTasklistFragmentWithFilter(Filter filter, Bundle extras) {
        Class<?> customTaskList = null;

        if (subtasksHelper.shouldUseSubtasksFragmentForFilter(filter)) {
            customTaskList = SubtasksHelper.subtasksClassForFilter(filter);
        }

        TaskListFragment newFragment = TaskListFragment.instantiateWithFilterAndExtras(filter, extras, customTaskList);

        try {
            FragmentManager manager = getFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.tasklist_fragment_container, newFragment, TaskListFragment.TAG_TASKLIST_FRAGMENT);
            transaction.commit();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getFragmentManager().executePendingTransactions();
                }
            });
        } catch (Exception e) {
            // Don't worry about it
            Timber.e(e, e.getMessage());
        }
    }

    @Override
    public void setupActivityFragment(TagData tagData) {
        super.setupActivityFragment(tagData);

        if (fragmentLayout == LAYOUT_DOUBLE) {
            View container = findViewById(R.id.taskedit_fragment_container);
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (!Flags.checkAndClear(Flags.TLA_DISMISSED_FROM_TASK_EDIT)) {
            TaskEditFragment tea = getTaskEditFragment();
            if (tea != null) {
                onBackPressed();
            }
        }

        if (getIntent().hasExtra(TOKEN_SWITCH_TO_FILTER)) {
            Filter newList = getIntent().getParcelableExtra(TOKEN_SWITCH_TO_FILTER);
            getIntent().removeExtra(TOKEN_SWITCH_TO_FILTER);
            onFilterItemClicked(newList);
//        } else {
//            navigationDrawer.restoreLastSelected();
        }

        if (getIntent().hasExtra(OPEN_TASK)) {
            long id = getIntent().getLongExtra(OPEN_TASK, 0);
            if (id > 0) {
                onTaskListItemClicked(id);
            } else {
                TaskListFragment tlf = getTaskListFragment();
                if (tlf != null) {
                    tlf.quickAddBar.quickAddTask(); //$NON-NLS-1$
                }
            }
            if (fragmentLayout == LAYOUT_SINGLE) {
                getIntent().removeExtra(OPEN_TASK);
            }
        }

        if (getIntent().getBooleanExtra(TOKEN_CREATE_NEW_LIST, false)) {
            newListFromLaunch();
        }

        if (getResources().getBoolean(R.bool.google_play_store_available) &&
                !preferences.getBoolean(R.string.p_collect_statistics_notification, false)) {
            try {
                View taskList = findViewById(R.id.task_list_coordinator);
                String text = getString(R.string.anonymous_usage_blurb);
                //noinspection ResourceType
                Snackbar.make(taskList, text, 10000)
                        .setActionTextColor(getResources().getColor(R.color.snackbar_undo))
                        .setCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                preferences.setBoolean(R.string.p_collect_statistics_notification, true);
                            }
                        })
                        .setAction(R.string.opt_out, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivityForResult(new Intent(TaskListActivity.this, BasicPreferences.class), FilterAdapter.REQUEST_SETTINGS);
                            }
                        })
                        .show();
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
                tracker.reportException(e);
            }
        }
    }

    private void newListFromLaunch() {
        Intent thisIntent = getIntent();
        Intent newTagIntent = new Intent(this, TagSettingsActivity.class);
        newTagIntent.putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_NAME, thisIntent.getStringExtra(TOKEN_CREATE_NEW_LIST_NAME));
        thisIntent.removeExtra(TOKEN_CREATE_NEW_LIST_NAME);
        startActivityForResult(newTagIntent, NavigationDrawerFragment.REQUEST_NEW_LIST);
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        if (fragmentLayout != LAYOUT_SINGLE) {
            getIntent().putExtra(OPEN_TASK, taskId);
        }
        super.onTaskListItemClicked(taskId);
    }

    public void setListsTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    public void setSelectedItem(Filter item) {
        getSupportActionBar().setTitle(item.listingTitle);
    }

    @Override
    public void onBackPressed() {
        if (navigationDrawer.isDrawerOpen()) {
            navigationDrawer.closeMenu();
            return;
        }

        // manage task edit visibility
        View taskeditFragmentContainer = findViewById(R.id.taskedit_fragment_container);
        if(taskeditFragmentContainer != null && taskeditFragmentContainer.getVisibility() == View.VISIBLE) {
            Flags.set(Flags.TLA_DISMISSED_FROM_TASK_EDIT);
            onPostResume();
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Callback<String> quickAddTask = new Callback<String>() {
            @Override
            public void apply(String title) {
                getTaskListFragment().quickAddBar.quickAddTask(title);
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
        } else if (requestCode == TaskListFragment.ACTIVITY_EDIT_TASK && resultCode != Activity.RESULT_CANCELED) {
            // Handle switch to assigned filter when it comes from TaskEditActivity finishing
            // For cases when we're in a multi-frame layout, the TaskEditFragment will notify us here directly
            TaskListFragment tlf = getTaskListFragment();
            if (tlf != null) {
                if (data != null) {
                    if (data.getBooleanExtra(TaskEditFragment.TOKEN_TAGS_CHANGED, false)) {
                        tagsChanged(true);
                    }
                }
                tlf.refresh();
                if (data != null) {
                    tlf.onTaskCreated(
                            data.getLongExtra(TaskEditFragment.TOKEN_ID, 0L),
                            data.getStringExtra(TaskEditFragment.TOKEN_UUID));
                }
            }
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
        }

        super.onActivityResult(requestCode, resultCode, data);
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
                    putExtra(TagViewFragment.EXTRA_TAG_DATA, getTaskListFragment().getActiveTagData());
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            TaskEditFragment tef = getTaskEditFragment();
            if (tef != null && tef.onKeyDown(keyCode)) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
