/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import static android.app.Activity.RESULT_OK;
import static android.support.v4.content.ContextCompat.getColor;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksSubtaskListFragment;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskMover;
import com.todoroo.astrid.timers.TimerPlugin;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.FilterSettingsActivity;
import org.tasks.activities.RemoteListSupportPicker;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.SortDialog;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingFragment;
import org.tasks.preferences.Device;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;
import org.tasks.tasklist.ActionModeProvider;
import org.tasks.tasklist.TaskListRecyclerAdapter;
import org.tasks.tasklist.ViewHolderFactory;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.ProgressDialogAsyncTask;
import org.tasks.ui.TaskListViewModel;

/**
 * Primary activity for the Bente application. Shows a list of upcoming tasks and a user's coaches.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TaskListFragment extends InjectingFragment
    implements SwipeRefreshLayout.OnRefreshListener, Toolbar.OnMenuItemClickListener {

  public static final String TAGS_METADATA_JOIN = "for_tags"; // $NON-NLS-1$
  public static final String GTASK_METADATA_JOIN = "for_gtask"; // $NON-NLS-1$
  public static final String CALDAV_METADATA_JOIN = "for_caldav"; // $NON-NLS-1$
  public static final String FILE_METADATA_JOIN = "for_actions"; // $NON-NLS-1$
  public static final int REQUEST_MOVE_TASKS = 11545;
  private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
  private static final String EXTRA_FILTER = "extra_filter";
  private static final String FRAG_TAG_SORT_DIALOG = "frag_tag_sort_dialog";
  // --- instance variables
  private static final int REQUEST_EDIT_FILTER = 11544;
  private final RefreshReceiver refreshReceiver = new RefreshReceiver();
  @Inject protected Tracker tracker;
  protected Filter filter;
  @Inject SyncAdapters syncAdapters;
  @Inject TaskDeleter taskDeleter;
  @Inject @ForActivity Context context;
  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;
  @Inject CheckBoxes checkBoxes;
  @Inject TaskCreator taskCreator;
  @Inject TimerPlugin timerPlugin;
  @Inject ViewHolderFactory viewHolderFactory;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Device device;
  @Inject TaskMover taskMover;
  @Inject ActionModeProvider actionModeProvider;

  @BindView(R.id.swipe_layout)
  SwipeRefreshLayout swipeRefreshLayout;

  @BindView(R.id.swipe_layout_empty)
  SwipeRefreshLayout emptyRefreshLayout;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.task_list_coordinator)
  CoordinatorLayout coordinatorLayout;

  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;

  private TaskListViewModel taskListViewModel;
  private TaskAdapter taskAdapter = null;
  private TaskListRecyclerAdapter recyclerAdapter;

  /*
   * ======================================================================
   * ======================================================= initialization
   * ======================================================================
   */
  private TaskListFragmentCallbackHandler callbacks;

  public static TaskListFragment newTaskListFragment(Filter filter) {
    TaskListFragment fragment = new TaskListFragment();
    fragment.filter = filter;
    return fragment;
  }

  @Override
  public void onRefresh() {
    if (!syncAdapters.syncNow()) {
      refresh();
    }
  }

  protected void setSyncOngoing(final boolean ongoing) {
    Activity activity = getActivity();
    if (activity != null) {
      activity.runOnUiThread(
          () -> {
            swipeRefreshLayout.setRefreshing(ongoing);
            emptyRefreshLayout.setRefreshing(ongoing);
          });
    }
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);

    if (savedInstanceState != null) {
      recyclerAdapter.restoreSaveState(savedInstanceState);
    }
  }

  @Override
  public void onAttach(Activity activity) {
    taskListViewModel = ViewModelProviders.of(getActivity()).get(TaskListViewModel.class);

    super.onAttach(activity);

    callbacks = (TaskListFragmentCallbackHandler) activity;
  }

  @Override
  public void inject(FragmentComponent component) {
    component.inject(this);
  }

  /** Called when loading up the activity */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      filter = savedInstanceState.getParcelable(EXTRA_FILTER);
    }

    if (filter == null) {
      filter = BuiltInFilterExposer.getMyTasksFilter(getResources());
    }

    filter.setFilterQueryOverride(null);

    setTaskAdapter();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_FILTER, filter);
    outState.putAll(recyclerAdapter.getSaveState());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View parent = inflater.inflate(R.layout.fragment_task_list, container, false);
    ButterKnife.bind(this, parent);
    setupRefresh(swipeRefreshLayout);
    setupRefresh(emptyRefreshLayout);

    toolbar.setTitle(filter.listingTitle);
    toolbar.setNavigationIcon(R.drawable.ic_menu_24dp);
    toolbar.setNavigationOnClickListener(v -> callbacks.onNavigationIconClicked());
    inflateMenu(toolbar);
    setupMenu(toolbar.getMenu());
    toolbar.setOnMenuItemClickListener(this);
    MenuColorizer.colorToolbar(context, toolbar);

    return parent;
  }

  protected void inflateMenu(Toolbar toolbar) {
    toolbar.inflateMenu(R.menu.menu_task_list_fragment);
    if (filter instanceof CustomFilter && ((CustomFilter) filter).getId() > 0) {
      toolbar.inflateMenu(R.menu.menu_custom_filter);
    }
  }

  private void setupMenu(Menu menu) {
    MenuItem hidden = menu.findItem(R.id.menu_show_hidden);
    if (preferences.getBoolean(R.string.p_show_hidden_tasks, false)) {
      hidden.setChecked(true);
    }
    MenuItem completed = menu.findItem(R.id.menu_show_completed);
    if (preferences.getBoolean(R.string.p_show_completed_tasks, false)) {
      completed.setChecked(true);
    }
    if (taskAdapter.isManuallySorted()) {
      completed.setChecked(true);
      completed.setEnabled(false);
      hidden.setChecked(true);
      hidden.setEnabled(false);
    }

    menu.findItem(R.id.menu_voice_add).setVisible(device.voiceInputAvailable());
    final MenuItem item = menu.findItem(R.id.menu_search);
    final SearchView actionView = (SearchView) MenuItemCompat.getActionView(item);
    actionView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {
            query = query.trim();
            String title = getString(R.string.FLA_search_filter, query);
            Filter savedFilter =
                new Filter(
                    title,
                    new QueryTemplate()
                        .where(
                            Criterion.and(
                                Task.DELETION_DATE.eq(0),
                                Criterion.or(
                                    Task.NOTES.like("%" + query + "%"),
                                    Task.TITLE.like("%" + query + "%")))));
            ((MainActivity) getActivity()).onFilterItemClicked(savedFilter);
            MenuItemCompat.collapseActionView(item);
            return true;
          }

          @Override
          public boolean onQueryTextChange(String query) {
            return false;
          }
        });
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_voice_add:
        Intent recognition = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognition.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognition.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognition.putExtra(
            RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_create_prompt));
        startActivityForResult(recognition, TaskListFragment.VOICE_RECOGNITION_REQUEST_CODE);
        return true;
      case R.id.menu_sort:
        SortDialog.newSortDialog(hasDraggableOption())
            .show(getChildFragmentManager(), FRAG_TAG_SORT_DIALOG);
        return true;
      case R.id.menu_show_hidden:
        item.setChecked(!item.isChecked());
        preferences.setBoolean(R.string.p_show_hidden_tasks, item.isChecked());
        loadTaskListContent();
        localBroadcastManager.broadcastRefresh();
        return true;
      case R.id.menu_show_completed:
        item.setChecked(!item.isChecked());
        preferences.setBoolean(R.string.p_show_completed_tasks, item.isChecked());
        loadTaskListContent();
        localBroadcastManager.broadcastRefresh();
        return true;
      case R.id.menu_filter_settings:
        Intent intent = new Intent(getActivity(), FilterSettingsActivity.class);
        intent.putExtra(FilterSettingsActivity.TOKEN_FILTER, filter);
        startActivityForResult(intent, REQUEST_EDIT_FILTER);
        return true;
      case R.id.menu_clear_completed:
        dialogBuilder
            .newMessageDialog(R.string.clear_completed_tasks_confirmation)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> clearCompleted())
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        return true;
      default:
        return onOptionsItemSelected(item);
    }
  }

  protected void clearCompleted() {
    tracker.reportEvent(Tracking.Events.CLEAR_COMPLETED);
    new ProgressDialogAsyncTask(getActivity(), dialogBuilder) {
      @Override
      protected Integer doInBackground(Void... params) {
        return taskDeleter.clearCompleted(filter);
      }

      @Override
      protected int getResultResource() {
        return R.string.delete_multiple_tasks_confirmation;
      }
    }.execute();
  }

  @OnClick(R.id.fab)
  void createNewTask() {
    onTaskListItemClicked(addTask(""));
  }

  public Task addTask(String title) {
    return taskCreator.createWithValues(filter.valuesForNewTasks, title);
  }

  private void setupRefresh(SwipeRefreshLayout layout) {
    layout.setOnRefreshListener(this);
    layout.setColorSchemeColors(checkBoxes.getPriorityColorsArray());
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    taskListViewModel
        .getTasks(filter, taskProperties())
        .observe(
            getActivity(),
            list -> {
              if (list.isEmpty()) {
                swipeRefreshLayout.setVisibility(View.GONE);
                emptyRefreshLayout.setVisibility(View.VISIBLE);
              } else {
                swipeRefreshLayout.setVisibility(View.VISIBLE);
                emptyRefreshLayout.setVisibility(View.GONE);
              }

              // stash selected items
              Bundle saveState = recyclerAdapter.getSaveState();

              recyclerAdapter.setList(list);

              recyclerAdapter.restoreSaveState(saveState);
            });

    ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    recyclerAdapter.applyToRecyclerView(recyclerView);
    recyclerView.setLayoutManager(new LinearLayoutManager(context));
  }

  @Override
  public void onResume() {
    super.onResume();

    localBroadcastManager.registerRefreshReceiver(refreshReceiver);

    refresh();
  }

  public Snackbar makeSnackbar(String text) {
    Snackbar snackbar =
        Snackbar.make(coordinatorLayout, text, 8000)
            .setActionTextColor(getColor(context, R.color.snackbar_text_color));
    snackbar.getView().setBackgroundColor(getColor(context, R.color.snackbar_background));
    return snackbar;
  }

  @Override
  public void onPause() {
    super.onPause();

    localBroadcastManager.unregisterReceiver(refreshReceiver);
  }

  /**
   * Called by the RefreshReceiver when the task list receives a refresh broadcast. Subclasses
   * should override this.
   */
  private void refresh() {
    // TODO: compare indents in diff callback, then animate this
    loadTaskListContent(!(this instanceof GtasksSubtaskListFragment));

    setSyncOngoing(preferences.isSyncOngoing());
  }

  /*
   * ======================================================================
   * =================================================== managing list view
   * ======================================================================
   */

  public void loadTaskListContent() {
    loadTaskListContent(true);
  }

  public void loadTaskListContent(boolean animate) {
    recyclerAdapter.setAnimate(animate);

    taskListViewModel.invalidate();
  }

  protected TaskAdapter createTaskAdapter() {
    return new TaskAdapter();
  }

  /** Fill in the Task List with current items */
  protected void setTaskAdapter() {
    if (filter == null) {
      return;
    }

    // set up list adapters
    taskAdapter = createTaskAdapter();
    recyclerAdapter =
        new TaskListRecyclerAdapter(taskAdapter, viewHolderFactory, this, actionModeProvider);
    taskAdapter.setHelper(recyclerAdapter.getAsyncPagedListDiffer());
  }

  protected Property<?>[] taskProperties() {
    return TaskAdapter.PROPERTIES;
  }

  public Filter getFilter() {
    return filter;
  }

  public void onTaskCreated(List<Task> tasks) {
    for (Task task : tasks) {
      onTaskCreated(task.getUuid());
    }
    syncAdapters.syncNow();
  }

  public void onTaskCreated(String uuid) {}

  /*
   * ======================================================================
   * ============================================================== actions
   * ======================================================================
   */

  public void onTaskSaved() {
    recyclerAdapter.onTaskSaved();
  }

  public void onTaskDelete(List<Task> tasks) {
    for (Task task : tasks) {
      onTaskDelete(task);
    }
  }

  protected void onTaskDelete(Task task) {
    MainActivity activity = (MainActivity) getActivity();
    TaskEditFragment tef = activity.getTaskEditFragment();
    if (tef != null) {
      if (task.getId() == tef.model.getId()) {
        tef.discard();
      }
    }
    timerPlugin.stopTimer(task);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        List<String> match = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (match != null && match.size() > 0 && match.get(0).length() > 0) {
          String recognizedSpeech = match.get(0);
          recognizedSpeech =
              recognizedSpeech.substring(0, 1).toUpperCase()
                  + recognizedSpeech.substring(1).toLowerCase();

          onTaskListItemClicked(addTask(recognizedSpeech));
        }
      }
    } else if (requestCode == REQUEST_EDIT_FILTER) {
      if (resultCode == RESULT_OK) {
        String action = data.getAction();
        MainActivity activity = (MainActivity) getActivity();
        if (FilterSettingsActivity.ACTION_FILTER_DELETED.equals(action)) {
          activity.onFilterItemClicked(null);
        } else if (FilterSettingsActivity.ACTION_FILTER_RENAMED.equals(action)) {
          activity
              .getIntent()
              .putExtra(
                  MainActivity.OPEN_FILTER,
                  (Filter) data.getParcelableExtra(FilterSettingsActivity.TOKEN_FILTER));
          activity.recreate();
        }
      }
    } else if (requestCode == REQUEST_MOVE_TASKS) {
      if (resultCode == RESULT_OK) {
        tracker.reportEvent(Tracking.Events.MULTISELECT_MOVE);
        taskMover.move(
            taskAdapter.getSelected(),
            data.getParcelableExtra(RemoteListSupportPicker.EXTRA_SELECTED_FILTER));
        recyclerAdapter.finishActionMode();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    return onOptionsItemSelected(item);
  }

  public void onTaskListItemClicked(Task task) {
    callbacks.onTaskListItemClicked(task);
  }

  protected boolean hasDraggableOption() {
    return BuiltInFilterExposer.isInbox(context, filter)
        || BuiltInFilterExposer.isTodayFilter(context, filter);
  }

  /**
   * Container Activity must implement this interface and we ensure that it does during the
   * onAttach() callback
   */
  public interface TaskListFragmentCallbackHandler {

    void onTaskListItemClicked(Task task);

    void onNavigationIconClicked();
  }

  /**
   * Receiver which receives refresh intents
   *
   * @author Tim Su <tim@todoroo.com>
   */
  protected class RefreshReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      refresh();
    }
  }
}
