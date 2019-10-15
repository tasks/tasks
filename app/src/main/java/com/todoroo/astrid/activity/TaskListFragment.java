/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.activity;

import static android.app.Activity.RESULT_OK;
import static androidx.core.content.ContextCompat.getColor;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static org.tasks.caldav.CaldavCalendarSettingsActivity.EXTRA_CALDAV_CALENDAR;
import static org.tasks.ui.CheckBoxes.getPriorityColor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.primitives.Longs;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapterProvider;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.SearchFilter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskMover;
import com.todoroo.astrid.timers.TimerPlugin;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.FilterSettingsActivity;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.activities.RemoteListSupportPicker;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.caldav.CaldavCalendarSettingsActivity;
import org.tasks.data.Tag;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.SortDialog;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingFragment;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.Device;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;
import org.tasks.tasklist.ActionModeProvider;
import org.tasks.tasklist.ManualSortRecyclerAdapter;
import org.tasks.tasklist.PagedListRecyclerAdapter;
import org.tasks.tasklist.TaskListRecyclerAdapter;
import org.tasks.tasklist.ViewHolderFactory;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.TaskListViewModel;
import org.tasks.ui.Toaster;

public final class TaskListFragment extends InjectingFragment
    implements OnRefreshListener,
        OnMenuItemClickListener,
        OnActionExpandListener,
        OnQueryTextListener {

  public static final String TAGS_METADATA_JOIN = "for_tags"; // $NON-NLS-1$
  public static final String GTASK_METADATA_JOIN = "googletask"; // $NON-NLS-1$
  public static final String CALDAV_METADATA_JOIN = "for_caldav"; // $NON-NLS-1$
  public static final String ACTION_RELOAD = "action_reload";
  public static final String ACTION_DELETED = "action_deleted";
  public static final int REQUEST_MOVE_TASKS = 10103;
  private static final String EXTRA_SELECTED_TASK_IDS = "extra_selected_task_ids";
  private static final String EXTRA_SEARCH = "extra_search";
  private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
  private static final String EXTRA_FILTER = "extra_filter";
  private static final String FRAG_TAG_SORT_DIALOG = "frag_tag_sort_dialog";
  private static final int REQUEST_CALDAV_SETTINGS = 10101;
  private static final int REQUEST_GTASK_SETTINGS = 10102;
  private static final int REQUEST_FILTER_SETTINGS = 10104;
  private static final int REQUEST_TAG_SETTINGS = 10105;

  private static final int SEARCH_DEBOUNCE_TIMEOUT = 300;
  private final RefreshReceiver refreshReceiver = new RefreshReceiver();
  @Inject protected Tracker tracker;
  protected CompositeDisposable disposables;
  @Inject SyncAdapters syncAdapters;
  @Inject TaskDeleter taskDeleter;
  @Inject @ForActivity Context context;
  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;
  @Inject TaskCreator taskCreator;
  @Inject TimerPlugin timerPlugin;
  @Inject ViewHolderFactory viewHolderFactory;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Device device;
  @Inject TaskMover taskMover;
  @Inject ActionModeProvider actionModeProvider;
  @Inject Toaster toaster;
  @Inject TaskAdapterProvider taskAdapterProvider;

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
  private Filter filter;
  private PublishSubject<String> searchSubject = PublishSubject.create();
  private Disposable searchDisposable;
  private MenuItem search;
  private String searchQuery;

  private TaskListFragmentCallbackHandler callbacks;

  static TaskListFragment newTaskListFragment(Context context, Filter filter) {
    TaskListFragment fragment = new TaskListFragment();
    Bundle bundle = new Bundle();
    bundle.putParcelable(
        EXTRA_FILTER,
        filter == null ? BuiltInFilterExposer.getMyTasksFilter(context.getResources()) : filter);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public void onRefresh() {
    disposables.add(
        syncAdapters
            .sync(true)
            .doOnSuccess(
                initiated -> {
                  if (!initiated) {
                    refresh();
                  }
                })
            .delay(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe(
                initiated -> {
                  if (initiated) {
                    setSyncOngoing();
                  }
                }));
  }

  private void setSyncOngoing() {
    assertMainThread();

    boolean ongoing = preferences.isSyncOngoing();

    swipeRefreshLayout.setRefreshing(ongoing);
    emptyRefreshLayout.setRefreshing(ongoing);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);

    if (savedInstanceState != null) {
      long[] longArray = savedInstanceState.getLongArray(EXTRA_SELECTED_TASK_IDS);
      if (longArray != null && longArray.length > 0) {
        taskAdapter.setSelected(longArray);
        recyclerAdapter.startActionMode();
      }
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callbacks = (TaskListFragmentCallbackHandler) activity;
  }

  @Override
  public void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    List<Long> selectedTaskIds = taskAdapter.getSelected();
    outState.putLongArray(EXTRA_SELECTED_TASK_IDS, Longs.toArray(selectedTaskIds));
    outState.putString(EXTRA_SEARCH, searchQuery);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View parent = inflater.inflate(R.layout.fragment_task_list, container, false);
    ButterKnife.bind(this, parent);

    filter = getFilter();

    filter.setFilterQueryOverride(null);

    // set up list adapters
    taskAdapter = taskAdapterProvider.createTaskAdapter(filter);

    taskListViewModel = ViewModelProviders.of(getActivity()).get(TaskListViewModel.class);

    if (savedInstanceState != null) {
      searchQuery = savedInstanceState.getString(EXTRA_SEARCH);
    }

    taskListViewModel.setFilter(
        searchQuery == null ? filter : createSearchFilter(searchQuery),
        taskAdapter.isManuallySorted());

    recyclerAdapter =
        taskAdapter.supportsParentingOrManualSort()
            ? new ManualSortRecyclerAdapter(
                taskAdapter,
                recyclerView,
                viewHolderFactory,
                this,
                actionModeProvider,
                taskListViewModel.getValue())
            : new PagedListRecyclerAdapter(
                taskAdapter,
                recyclerView,
                viewHolderFactory,
                this,
                actionModeProvider,
                taskListViewModel.getValue());
    taskAdapter.setHelper(recyclerAdapter);
    ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    recyclerView.setLayoutManager(new LinearLayoutManager(context));

    taskListViewModel.observe(
        this,
        list -> {
          recyclerAdapter.submitList(list);

          if (list.isEmpty()) {
            swipeRefreshLayout.setVisibility(View.GONE);
            emptyRefreshLayout.setVisibility(View.VISIBLE);
          } else {
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            emptyRefreshLayout.setVisibility(View.GONE);
          }
        });

    recyclerView.setAdapter(recyclerAdapter);

    setupRefresh(swipeRefreshLayout);
    setupRefresh(emptyRefreshLayout);

    toolbar.setTitle(filter.listingTitle);
    toolbar.setNavigationIcon(R.drawable.ic_outline_menu_24px);
    toolbar.setNavigationOnClickListener(v -> callbacks.onNavigationIconClicked());
    toolbar.setOnMenuItemClickListener(this);
    setupMenu();

    return parent;
  }

  private void setupMenu() {
    Menu menu = toolbar.getMenu();
    menu.clear();
    toolbar.inflateMenu(R.menu.menu_task_list_fragment);
    if (filter.hasMenu()) {
      toolbar.inflateMenu(filter.getMenu());
    }
    MenuItem hidden = menu.findItem(R.id.menu_show_hidden);
    MenuItem completed = menu.findItem(R.id.menu_show_completed);
    if (!taskAdapter.supportsHiddenTasks() || filter instanceof SearchFilter) {
      completed.setChecked(true);
      completed.setEnabled(false);
      hidden.setChecked(true);
      hidden.setEnabled(false);
    } else {
      hidden.setChecked(preferences.getBoolean(R.string.p_show_hidden_tasks, false));
      completed.setChecked(preferences.getBoolean(R.string.p_show_completed_tasks, false));
    }

    menu.findItem(R.id.menu_voice_add).setVisible(device.voiceInputAvailable());

    search = menu.findItem(R.id.menu_search).setOnActionExpandListener(this);
    ((SearchView) search.getActionView()).setOnQueryTextListener(this);

    MenuColorizer.colorToolbar(context, toolbar);
  }

  private void openFilter(@Nullable Filter filter) {
    if (filter == null) {
      startActivity(TaskIntents.getTaskListByIdIntent(context, null));
    } else {
      startActivity(TaskIntents.getTaskListIntent(context, filter));
    }
  }

  private void searchByQuery(@Nullable String query) {
    searchQuery = query == null ? "" : query.trim();
    if (searchQuery.isEmpty()) {
      taskListViewModel.searchByFilter(
          BuiltInFilterExposer.getMyTasksFilter(context.getResources()));
    } else {
      Filter savedFilter = createSearchFilter(searchQuery);
      taskListViewModel.searchByFilter(savedFilter);
    }
  }

  private Filter createSearchFilter(String query) {
    String title = getString(R.string.FLA_search_filter, query);
    return new SearchFilter(
        title,
        new QueryTemplate()
            .join(Join.left(Tag.TABLE, Tag.TASK_UID.eq(Task.UUID)))
            .where(
                Criterion.and(
                    Task.DELETION_DATE.eq(0),
                    Criterion.or(
                        Task.NOTES.like("%" + query + "%"),
                        Task.TITLE.like("%" + query + "%"),
                        Tag.NAME.like("%" + query + "%")))));
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
        boolean supportsManualSort =
            filter.supportsSubtasks()
                || BuiltInFilterExposer.isInbox(context, filter)
                || BuiltInFilterExposer.isTodayFilter(context, filter);
        SortDialog.newSortDialog(supportsManualSort)
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
      case R.id.menu_clear_completed:
        dialogBuilder
            .newDialog(R.string.clear_completed_tasks_confirmation)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> clearCompleted())
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        return true;
      case R.id.menu_filter_settings:
        Intent filterSettings = new Intent(getActivity(), FilterSettingsActivity.class);
        filterSettings.putExtra(FilterSettingsActivity.TOKEN_FILTER, filter);
        startActivityForResult(filterSettings, REQUEST_FILTER_SETTINGS);
        return true;
      case R.id.menu_caldav_list_fragment:
        Intent caldavSettings = new Intent(getActivity(), CaldavCalendarSettingsActivity.class);
        caldavSettings.putExtra(EXTRA_CALDAV_CALENDAR, ((CaldavFilter) filter).getCalendar());
        startActivityForResult(caldavSettings, REQUEST_CALDAV_SETTINGS);
        return true;
      case R.id.menu_gtasks_list_settings:
        Intent gtasksSettings = new Intent(getActivity(), GoogleTaskListSettingsActivity.class);
        gtasksSettings.putExtra(
            GoogleTaskListSettingsActivity.EXTRA_STORE_DATA, ((GtasksFilter) filter).getList());
        startActivityForResult(gtasksSettings, REQUEST_GTASK_SETTINGS);
        return true;
      case R.id.menu_tag_settings:
        Intent tagSettings = new Intent(getActivity(), TagSettingsActivity.class);
        tagSettings.putExtra(TagSettingsActivity.EXTRA_TAG_DATA, ((TagFilter) filter).getTagData());
        startActivityForResult(tagSettings, REQUEST_TAG_SETTINGS);
        return true;
      default:
        return onOptionsItemSelected(item);
    }
  }

  private void clearCompleted() {
    tracker.reportEvent(Tracking.Events.CLEAR_COMPLETED);
    disposables.add(
        Single.fromCallable(() -> taskDeleter.clearCompleted(filter))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                count -> toaster.longToast(R.string.delete_multiple_tasks_confirmation, count)));
  }

  @OnClick(R.id.fab)
  void createNewTask() {
    onTaskListItemClicked(addTask(""));
  }

  private Task addTask(String title) {
    return taskCreator.createWithValues(filter, title);
  }

  private void setupRefresh(SwipeRefreshLayout layout) {
    layout.setOnRefreshListener(this);
    layout.setColorSchemeColors(
        getPriorityColor(context, 0),
        getPriorityColor(context, 1),
        getPriorityColor(context, 2),
        getPriorityColor(context, 3));
  }

  @Override
  public void onResume() {
    super.onResume();

    disposables = new CompositeDisposable();

    localBroadcastManager.registerRefreshReceiver(refreshReceiver);

    refresh();
  }

  public Snackbar makeSnackbar(String text) {
    Snackbar snackbar =
        Snackbar.make(coordinatorLayout, text, 8000)
            .setTextColor(getColor(context, R.color.snackbar_text_color))
            .setActionTextColor(getColor(context, R.color.snackbar_action_color));
    snackbar.getView().setBackgroundColor(getColor(context, R.color.snackbar_background));
    return snackbar;
  }

  @Override
  public void onPause() {
    super.onPause();

    disposables.dispose();

    localBroadcastManager.unregisterReceiver(refreshReceiver);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (searchDisposable != null && !searchDisposable.isDisposed()) {
      searchDisposable.dispose();
    }
  }

  boolean collapseSearchView() {
    return search.isActionViewExpanded() && search.collapseActionView();
  }

  private void refresh() {
    loadTaskListContent();

    setSyncOngoing();
  }

  public void loadTaskListContent() {
    taskListViewModel.invalidate();
  }

  public Filter getFilter() {
    return getArguments().getParcelable(EXTRA_FILTER);
  }

  public void onTaskCreated(List<Task> tasks) {
    for (Task task : tasks) {
      onTaskCreated(task.getUuid());
    }
    syncAdapters.sync();
  }

  void onTaskCreated(String uuid) {
    taskAdapter.onTaskCreated(uuid);
    loadTaskListContent();
  }

  public void onTaskDelete(Task task) {
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) {
      TaskEditFragment tef = activity.getTaskEditFragment();
      if (tef != null && task.getId() == tef.model.getId()) {
        tef.discard();
      }
    }
    timerPlugin.stopTimer(task);
    taskAdapter.onTaskDeleted(task);
    loadTaskListContent();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case VOICE_RECOGNITION_REQUEST_CODE:
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
        break;
      case REQUEST_MOVE_TASKS:
        if (resultCode == RESULT_OK) {
          tracker.reportEvent(Tracking.Events.MULTISELECT_MOVE);
          taskMover.move(
              taskAdapter.getSelected(),
              data.getParcelableExtra(RemoteListSupportPicker.EXTRA_SELECTED_FILTER));
          recyclerAdapter.finishActionMode();
        }
        break;
      case REQUEST_FILTER_SETTINGS:
      case REQUEST_CALDAV_SETTINGS:
      case REQUEST_GTASK_SETTINGS:
      case REQUEST_TAG_SETTINGS:
        if (resultCode == Activity.RESULT_OK) {
          String action = data.getAction();
          if (ACTION_DELETED.equals(action)) {
            openFilter(null);
          } else if (ACTION_RELOAD.equals(action)) {
            openFilter(data.getParcelableExtra(MainActivity.OPEN_FILTER));
          }
        }
        break;
      default:
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

  @Override
  public boolean onMenuItemActionExpand(MenuItem item) {
    searchDisposable =
        searchSubject
            .debounce(SEARCH_DEBOUNCE_TIMEOUT, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::searchByQuery);
    if (searchQuery == null) {
      searchByQuery("");
    }
    Menu menu = toolbar.getMenu();
    for (int i = 0; i < menu.size(); i++) {
      menu.getItem(i).setVisible(false);
    }
    return true;
  }

  @Override
  public boolean onMenuItemActionCollapse(MenuItem item) {
    taskListViewModel.searchByFilter(filter);
    searchDisposable.dispose();
    searchQuery = null;
    setupMenu();
    return true;
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    openFilter(createSearchFilter(query.trim()));
    search.collapseActionView();
    return true;
  }

  @Override
  public boolean onQueryTextChange(String query) {
    searchSubject.onNext(query);
    return true;
  }

  public interface TaskListFragmentCallbackHandler {
    void onTaskListItemClicked(Task task);

    void onNavigationIconClicked();
  }

  protected class RefreshReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      refresh();
    }
  }
}
