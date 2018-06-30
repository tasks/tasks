/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.astrid.activity.TaskEditFragment.newTaskEditFragment;
import static org.tasks.tasklist.ActionUtils.applySupportActionModeColor;
import static org.tasks.ui.NavigationDrawerFragment.OnFilterItemClickedListener;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.view.ActionMode;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksSubtaskListFragment;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.timers.TimerControlSet;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.caldav.CaldavListFragment;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.dialogs.SortDialog;
import org.tasks.fragments.CommentBarFragment;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.sync.SyncAdapters;
import org.tasks.tasklist.GtasksListFragment;
import org.tasks.tasklist.TagListFragment;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.EmptyTaskEditFragment;
import org.tasks.ui.NavigationDrawerFragment;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.TaskListViewModel;
import timber.log.Timber;

public class MainActivity extends InjectingAppCompatActivity
    implements OnFilterItemClickedListener,
        TaskListFragment.TaskListFragmentCallbackHandler,
        PriorityControlSet.OnPriorityChanged,
        TimerControlSet.TimerControlSetCallback,
        DeadlineControlSet.DueDateChangeListener,
        TaskEditFragment.TaskEditFragmentCallbackHandler,
        CommentBarFragment.CommentBarFragmentCallback,
        SortDialog.SortDialogCallback {

  /** For indicating the new list screen should be launched at fragment setup time */
  public static final String TOKEN_CREATE_NEW_LIST_NAME = "newListName"; // $NON-NLS-1$

  public static final String OPEN_FILTER = "open_filter"; // $NON-NLS-1$
  public static final String LOAD_FILTER = "load_filter";
  public static final String OPEN_TASK = "open_task"; // $NON-NLS-1$
  public static final String OPEN_NEW_TASK = "open_new_task"; // $NON-NLS-1$
  private static final String FRAG_TAG_TASK_LIST = "frag_tag_task_list";
  @Inject Preferences preferences;
  @Inject SubtasksHelper subtasksHelper;
  @Inject RepeatConfirmationReceiver repeatConfirmationReceiver;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject GtasksListService gtasksListService;
  @Inject TagDataDao tagDataDao;
  @Inject Theme theme;
  @Inject ThemeCache themeCache;
  @Inject SyncAdapters syncAdapters;
  @Inject Tracker tracker;
  @Inject TaskDao taskDao;
  @Inject CaldavDao caldavDao;
  @Inject LocalBroadcastManager localBroadcastManager;

  @BindView(R.id.drawer_layout)
  DrawerLayout drawerLayout;

  @BindView(R.id.master)
  FrameLayout master;

  @BindView(R.id.detail)
  FrameLayout detail;

  private NavigationDrawerFragment navigationDrawer;
  private TaskListViewModel viewModel;
  private int currentNightMode;

  private Filter filter;
  private ActionMode actionMode = null;

  /** @see android.app.Activity#onCreate(Bundle) */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    viewModel = ViewModelProviders.of(this).get(TaskListViewModel.class);

    getComponent().inject(viewModel);

    currentNightMode = getNightMode();

    setContentView(R.layout.task_list_activity);

    ButterKnife.bind(this);

    navigationDrawer = getNavigationDrawerFragment();
    navigationDrawer.setUp(drawerLayout);

    drawerLayout.addDrawerListener(
        new DrawerLayout.SimpleDrawerListener() {
          @Override
          public void onDrawerStateChanged(int newState) {
            finishActionMode();
          }
        });

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
    if (taskEditFragment == null) {
      hideDetailFragment();
    } else if (intent.hasExtra(OPEN_FILTER)
        || intent.hasExtra(LOAD_FILTER)
        || intent.hasExtra(OPEN_TASK)
        || intent.hasExtra(OPEN_NEW_TASK)) {
      taskEditFragment.save();
      taskEditFinished();
    } else {
      showDetailFragment();
    }

    TaskListFragment taskListFragment = getTaskListFragment();
    if (intent.hasExtra(OPEN_FILTER)) {
      Filter filter = intent.getParcelableExtra(OPEN_FILTER);
      intent.removeExtra(OPEN_FILTER);
      loadTaskListFragment(filter);
    } else if (intent.hasExtra(LOAD_FILTER)) {
      Filter filter =
          defaultFilterProvider.getFilterFromPreference(intent.getStringExtra(LOAD_FILTER));
      intent.removeExtra(LOAD_FILTER);
      loadTaskListFragment(filter);
    } else if (taskListFragment == null) {
      loadTaskListFragment(null);
    } else {
      applyTheme(taskListFragment);
    }

    if (intent.hasExtra(OPEN_TASK)) {
      long taskId = intent.getLongExtra(OPEN_TASK, 0);
      intent.removeExtra(OPEN_TASK);
      navigationDrawer.closeDrawer();
      if (taskId > 0) {
        Task task = taskDao.fetch(taskId);
        if (task != null) {
          onTaskListItemClicked(task);
        } else {
          Timber.e("Failed to find task %s", taskId);
        }
      } else {
        getSupportFragmentManager().executePendingTransactions();
        Task task = getTaskListFragment().addTask("");
        onTaskListItemClicked(task);
      }
    } else if (intent.hasExtra(OPEN_NEW_TASK)) {
      Task task = intent.getParcelableExtra(OPEN_NEW_TASK);
      intent.removeExtra(OPEN_NEW_TASK);
      onTaskListItemClicked(task);
    } else if (intent.hasExtra(TOKEN_CREATE_NEW_LIST_NAME)) {
      final String listName = intent.getStringExtra(TOKEN_CREATE_NEW_LIST_NAME);
      intent.removeExtra(TOKEN_CREATE_NEW_LIST_NAME);
      Intent activityIntent = new Intent(MainActivity.this, TagSettingsActivity.class);
      activityIntent.putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_NAME, listName);
      startActivityForResult(activityIntent, NavigationDrawerFragment.REQUEST_NEW_LIST);
    }
  }

  private void showDetailFragment() {
    if (!isDoublePaneLayout()) {
      detail.setVisibility(View.VISIBLE);
      master.setVisibility(View.GONE);
    }
  }

  private void hideDetailFragment() {
    if (isDoublePaneLayout()) {
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.detail, new EmptyTaskEditFragment())
          .commit();
    } else {
      master.setVisibility(View.VISIBLE);
      detail.setVisibility(View.GONE);
    }
  }

  private void loadTaskListFragment(Filter filter) {
    if (filter == null) {
      filter = defaultFilterProvider.getDefaultFilter();
    }
    TaskListFragment taskListFragment = newTaskListFragment(filter);

    finishActionMode();

    applyTheme(taskListFragment);

    navigationDrawer.setSelected(filter);

    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager
        .beginTransaction()
        .replace(R.id.master, taskListFragment, FRAG_TAG_TASK_LIST)
        .commit();
  }

  private void applyTheme(TaskListFragment taskListFragment) {
    filter = taskListFragment.filter;
    ThemeColor filterColor = getFilterColor();

    filterColor.applyToStatusBar(drawerLayout);
    filterColor.applyTaskDescription(this, filter.listingTitle);
    theme.withThemeColor(filterColor).applyToContext(this);
  }

  private ThemeColor getFilterColor() {
    return filter != null && filter.tint >= 0
        ? themeCache.getThemeColor(filter.tint)
        : theme.getThemeColor();
  }

  private void loadTaskEditFragment(TaskEditFragment taskEditFragment) {
    finishActionMode();

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.detail, taskEditFragment, TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
        .addToBackStack(TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
        .commitAllowingStateLoss();

    getSupportFragmentManager().executePendingTransactions();

    showDetailFragment();
  }

  private NavigationDrawerFragment getNavigationDrawerFragment() {
    return (NavigationDrawerFragment)
        getSupportFragmentManager()
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

    localBroadcastManager.registerRepeatReceiver(repeatConfirmationReceiver);

    syncAdapters.checkPlayServices(this);
  }

  public void restart() {
    recreate();
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

    localBroadcastManager.unregisterReceiver(repeatConfirmationReceiver);
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

    if (item instanceof Filter) {
      viewModel.clear();
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
            : TagListFragment.newTagViewFragment(tagFilter, tagData);
      }
    } else if (filter instanceof GtasksFilter) {
      GtasksFilter gtasksFilter = (GtasksFilter) filter;
      GoogleTaskList list = gtasksListService.getList(gtasksFilter.getStoreId());
      if (list != null) {
        return preferences.getBoolean(R.string.p_manual_sort, false)
            ? GtasksSubtaskListFragment.newGtasksSubtaskListFragment(gtasksFilter, list)
            : GtasksListFragment.newGtasksListFragment(gtasksFilter, list);
      }
    } else if (filter instanceof CaldavFilter) {
      CaldavFilter caldavFilter = (CaldavFilter) filter;
      CaldavCalendar calendar = caldavDao.getCalendarByUuid(caldavFilter.getUuid());
      if (calendar != null) {
        return CaldavListFragment.newCaldavListFragment(caldavFilter, calendar);
      }
    } else if (filter != null) {
      return subtasksHelper.shouldUseSubtasksFragmentForFilter(filter)
          ? SubtasksListFragment.newSubtasksListFragment(filter)
          : TaskListFragment.newTaskListFragment(filter);
    }

    return null;
  }

  @Override
  public void onTaskListItemClicked(Task task) {
    if (task == null) {
      return;
    }

    TaskEditFragment taskEditFragment = getTaskEditFragment();

    if (taskEditFragment != null) {
      taskEditFragment.save();
    }

    loadTaskEditFragment(newTaskEditFragment(task));
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
      finish();
    }
  }

  public TaskListFragment getTaskListFragment() {
    return (TaskListFragment) getSupportFragmentManager().findFragmentByTag(FRAG_TAG_TASK_LIST);
  }

  public TaskEditFragment getTaskEditFragment() {
    return (TaskEditFragment)
        getSupportFragmentManager().findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
  }

  @Override
  public void onPriorityChange(int priority) {
    getTaskEditFragment().onPriorityChange(priority);
  }

  @Override
  public Task stopTimer() {
    return getTaskEditFragment().stopTimer();
  }

  @Override
  public Task startTimer() {
    return getTaskEditFragment().startTimer();
  }

  private boolean isDoublePaneLayout() {
    return getResources().getBoolean(R.bool.two_pane_layout);
  }

  @Override
  public void taskEditFinished() {
    getSupportFragmentManager()
        .popBackStackImmediate(
            TaskEditFragment.TAG_TASKEDIT_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    hideDetailFragment();
    hideKeyboard();
    getTaskListFragment().loadTaskListContent();
  }

  private void hideKeyboard() {
    View view = getCurrentFocus();
    if (view != null) {
      InputMethodManager inputMethodManager =
          (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
      inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }

  @Override
  public void addComment(String message, String picture) {
    TaskEditFragment taskEditFragment = getTaskEditFragment();
    if (taskEditFragment != null) {
      taskEditFragment.addComment(message, picture);
    }
  }

  @Override
  public void sortChanged() {
    localBroadcastManager.broadcastRefresh();
    onFilterItemClicked(filter);
  }

  @Override
  public void onSupportActionModeStarted(@NonNull ActionMode mode) {
    super.onSupportActionModeStarted(mode);

    actionMode = mode;

    ThemeColor filterColor = getFilterColor();

    applySupportActionModeColor(filterColor, mode);

    filterColor.setStatusBarColor(this);
  }

  @Override
  @SuppressLint("NewApi")
  public void onSupportActionModeFinished(@NonNull ActionMode mode) {
    super.onSupportActionModeFinished(mode);

    if (atLeastLollipop()) {
      getWindow().setStatusBarColor(0);
    }
  }

  private void finishActionMode() {
    if (actionMode != null) {
      actionMode.finish();
      actionMode = null;
    }
  }

  @Override
  public void dueDateChanged(long dateTime) {
    getTaskEditFragment().onDueDateChanged(dateTime);
  }
}
