/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.activity;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.astrid.activity.TaskEditFragment.newTaskEditFragment;
import static com.todoroo.astrid.activity.TaskListFragment.newTaskListFragment;
import static org.tasks.tasklist.ActionUtils.applySupportActionModeColor;
import static org.tasks.ui.NavigationDrawerFragment.REQUEST_NEW_LIST;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.timers.TimerControlSet;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.SortDialog;
import org.tasks.fragments.CommentBarFragment;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.EmptyTaskEditFragment;
import org.tasks.ui.NavigationDrawerFragment;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.TaskListViewModel;
import org.tasks.ui.Toaster;

public class MainActivity extends InjectingAppCompatActivity
    implements TaskListFragment.TaskListFragmentCallbackHandler,
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
  public static final String CREATE_TASK = "open_task"; // $NON-NLS-1$
  public static final String OPEN_TASK = "open_new_task"; // $NON-NLS-1$
  private static final String FRAG_TAG_TASK_LIST = "frag_tag_task_list";
  private static final String EXTRA_FILTER = "extra_filter";

  @Inject Preferences preferences;
  @Inject RepeatConfirmationReceiver repeatConfirmationReceiver;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject Theme theme;
  @Inject ThemeCache themeCache;
  @Inject Tracker tracker;
  @Inject TaskDao taskDao;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject TaskCreator taskCreator;
  @Inject PlayServices playServices;
  @Inject Toaster toaster;

  @BindView(R.id.drawer_layout)
  DrawerLayout drawerLayout;

  @BindView(R.id.master)
  FrameLayout master;

  @BindView(R.id.detail)
  FrameLayout detail;

  private CompositeDisposable disposables;
  private NavigationDrawerFragment navigationDrawer;
  private int currentNightMode;

  private Filter filter;
  private ActionMode actionMode = null;

  /** @see android.app.Activity#onCreate(Bundle) */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    TaskListViewModel viewModel = ViewModelProviders.of(this).get(TaskListViewModel.class);
    getComponent().inject(viewModel);

    currentNightMode = getNightMode();

    setContentView(R.layout.task_list_activity);

    ButterKnife.bind(this);

    if (savedInstanceState != null) {
      filter = savedInstanceState.getParcelable(EXTRA_FILTER);
      applyTheme();
    }

    navigationDrawer = getNavigationDrawerFragment();
    navigationDrawer.setUp(drawerLayout);

    drawerLayout.addDrawerListener(
        new DrawerLayout.SimpleDrawerListener() {
          @Override
          public void onDrawerStateChanged(int newState) {
            finishActionMode();
          }
        });
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    setIntent(intent);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_FILTER, filter);
  }

  private void clearUi() {
    finishActionMode();
    navigationDrawer.closeDrawer();
  }

  private @Nullable Task getTaskToLoad(Filter filter) {
    Intent intent = getIntent();
    if (intent.hasExtra(CREATE_TASK)) {
      intent.removeExtra(CREATE_TASK);
      return taskCreator.createWithValues(filter, "");
    }

    if (intent.hasExtra(OPEN_TASK)) {
      Task task = intent.getParcelableExtra(OPEN_TASK);
      intent.removeExtra(OPEN_TASK);
      return task;
    }

    return null;
  }

  private void openTask(Filter filter) {
    Task task = getTaskToLoad(filter);
    if (task != null) {
      onTaskListItemClicked(task);
    } else if (getTaskEditFragment() == null) {
      hideDetailFragment();
    } else {
      showDetailFragment();
    }
  }

  private void handleIntent() {
    Intent intent = getIntent();

    boolean openFilter = intent.hasExtra(OPEN_FILTER);
    boolean loadFilter = intent.hasExtra(LOAD_FILTER);

    TaskEditFragment tef = getTaskEditFragment();
    if (tef != null && (openFilter || loadFilter)) {
      tef.save();
    }

    if (loadFilter || (!openFilter && filter == null)) {
      disposables.add(
          Single.fromCallable(
                  () -> {
                    String filter = intent.getStringExtra(LOAD_FILTER);
                    intent.removeExtra(LOAD_FILTER);
                    return isNullOrEmpty(filter)
                        ? defaultFilterProvider.getDefaultFilter()
                        : defaultFilterProvider.getFilterFromPreference(filter);
                  })
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  filter -> {
                    clearUi();
                    openTaskListFragment(filter);
                    openTask(filter);
                  }));
    } else if (openFilter) {
      Filter filter = intent.getParcelableExtra(OPEN_FILTER);
      intent.removeExtra(OPEN_FILTER);
      clearUi();
      openTaskListFragment(filter);
      openTask(filter);
    } else {
      TaskListFragment existing = getTaskListFragment();
      openTaskListFragment(
          existing == null || existing.getFilter() != filter
              ? newTaskListFragment(getApplicationContext(), filter)
              : existing);
      openTask(filter);
    }

    if (intent.hasExtra(TOKEN_CREATE_NEW_LIST_NAME)) {
      final String listName = intent.getStringExtra(TOKEN_CREATE_NEW_LIST_NAME);
      intent.removeExtra(TOKEN_CREATE_NEW_LIST_NAME);
      Intent activityIntent = new Intent(MainActivity.this, TagSettingsActivity.class);
      activityIntent.putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_NAME, listName);
      startActivityForResult(activityIntent, REQUEST_NEW_LIST);
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

  private void openTaskListFragment(Filter filter) {
    openTaskListFragment(newTaskListFragment(getApplicationContext(), filter));
  }

  private void openTaskListFragment(@NonNull TaskListFragment taskListFragment) {
    assertMainThread();

    filter = taskListFragment.getFilter();
    navigationDrawer.setSelected(filter);
    applyTheme();
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager
        .beginTransaction()
        .replace(R.id.master, taskListFragment, FRAG_TAG_TASK_LIST)
        .commit();
    fragmentManager.executePendingTransactions();
  }

  private void applyTheme() {
    ThemeColor filterColor = getFilterColor();
    filterColor.applyToStatusBar(drawerLayout);
    filterColor.applyToNavigationBar(this);
    filterColor.applyTaskDescription(
        this, filter == null ? getString(R.string.app_name) : filter.listingTitle);
    theme.withThemeColor(filterColor).applyToContext(this);
  }

  private ThemeColor getFilterColor() {
    return filter != null && filter.tint >= 0
        ? themeCache.getThemeColor(filter.tint)
        : theme.getThemeColor();
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

    if (BuildConfig.DEBUG && disposables != null && !disposables.isDisposed()) {
      throw new IllegalStateException();
    }

    disposables = new CompositeDisposable(playServices.check(this));
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();

    handleIntent();
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

    if (disposables != null) {
      disposables.dispose();
    }
  }

  @Override
  public void onTaskListItemClicked(Task task) {
    assertMainThread();

    if (task == null) {
      return;
    }

    TaskEditFragment taskEditFragment = getTaskEditFragment();

    if (taskEditFragment != null) {
      taskEditFragment.save();
    }

    clearUi();

    disposables.add(
        Single.fromCallable(() -> task.isNew() ? task : taskDao.fetch(task.getId()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                t -> {
                  getSupportFragmentManager()
                      .beginTransaction()
                      .replace(
                          R.id.detail,
                          newTaskEditFragment(t),
                          TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                      .addToBackStack(TaskEditFragment.TAG_TASKEDIT_FRAGMENT)
                      .commit();

                  showDetailFragment();
                }));
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
      return;
    }

    TaskListFragment taskListFragment = getTaskListFragment();
    if (taskListFragment != null && taskListFragment.collapseSearchView()) {
      return;
    }

    finish();
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
  public void removeTaskEditFragment() {
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
  public void addComment(String message, Uri picture) {
    TaskEditFragment taskEditFragment = getTaskEditFragment();
    if (taskEditFragment != null) {
      taskEditFragment.addComment(message, picture);
    }
  }

  @Override
  public void sortChanged() {
    localBroadcastManager.broadcastRefresh();
    openTaskListFragment(filter);
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
