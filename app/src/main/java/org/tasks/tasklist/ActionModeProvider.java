package org.tasks.tasklist;

import static com.todoroo.astrid.activity.TaskListFragment.REQUEST_MOVE_TASKS;
import static org.tasks.activities.RemoteListSupportPicker.newRemoteListSupportPicker;

import android.content.Context;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskDuplicator;
import com.todoroo.astrid.service.TaskMover;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.sync.SyncAdapters;
import org.tasks.ui.MenuColorizer;

public class ActionModeProvider {

  private static final String FRAG_TAG_REMOTE_LIST_PICKER = "frag_tag_remote_list_picker";

  private final Context context;
  private final DialogBuilder dialogBuilder;
  private final TaskDeleter taskDeleter;
  private final TaskDuplicator taskDuplicator;
  private final TaskMover taskMover;
  private final Tracker tracker;
  private final SyncAdapters syncAdapters;

  @Inject
  public ActionModeProvider(
      @ForActivity Context context,
      DialogBuilder dialogBuilder,
      TaskDeleter taskDeleter,
      TaskDuplicator taskDuplicator,
      TaskMover taskMover,
      Tracker tracker,
      SyncAdapters syncAdapters) {
    this.context = context;
    this.dialogBuilder = dialogBuilder;
    this.taskDeleter = taskDeleter;
    this.taskDuplicator = taskDuplicator;
    this.taskMover = taskMover;
    this.tracker = tracker;
    this.syncAdapters = syncAdapters;
  }

  public ActionMode startActionMode(
      TaskAdapter adapter,
      TaskListFragment taskList,
      TaskListRecyclerAdapter taskListRecyclerAdapter) {
    return ((MainActivity) context)
        .startSupportActionMode(
            new ActionMode.Callback() {
              @Override
              public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.menu_multi_select, menu);
                if (!syncAdapters.isSyncEnabled()) {
                  menu.findItem(R.id.move_tasks).setVisible(false);
                }
                MenuColorizer.colorMenu(context, menu);
                return true;
              }

              @Override
              public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
              }

              @Override
              public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                  case R.id.move_tasks:
                    Filter singleFilter = taskMover.getSingleFilter(adapter.getSelected());
                    (singleFilter == null
                            ? newRemoteListSupportPicker(taskList, REQUEST_MOVE_TASKS)
                            : newRemoteListSupportPicker(
                                singleFilter, taskList, REQUEST_MOVE_TASKS))
                        .show(taskList.getFragmentManager(), FRAG_TAG_REMOTE_LIST_PICKER);
                    return true;
                  case R.id.delete:
                    dialogBuilder
                        .newMessageDialog(R.string.delete_selected_tasks)
                        .setPositiveButton(
                            android.R.string.ok, (dialogInterface, i) -> deleteSelectedItems())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                    return true;
                  case R.id.copy_tasks:
                    dialogBuilder
                        .newMessageDialog(R.string.copy_selected_tasks)
                        .setPositiveButton(
                            android.R.string.ok, ((dialogInterface, i) -> copySelectedItems()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                    return true;
                  default:
                    return false;
                }
              }

              @Override
              public void onDestroyActionMode(ActionMode actionMode) {
                adapter.clearSelections();
                taskListRecyclerAdapter.onDestroyActionMode();
              }

              private void deleteSelectedItems() {
                tracker.reportEvent(Tracking.Events.MULTISELECT_DELETE);
                List<Long> tasks = adapter.getSelected();
                taskListRecyclerAdapter.finishActionMode();
                List<Task> result = taskDeleter.markDeleted(tasks);
                taskList.onTaskDelete(result);
                taskList
                    .makeSnackbar(
                        context.getString(
                            R.string.delete_multiple_tasks_confirmation,
                            Integer.toString(result.size())))
                    .show();
              }

              private void copySelectedItems() {
                tracker.reportEvent(Tracking.Events.MULTISELECT_CLONE);
                List<Long> tasks = adapter.getSelected();
                taskListRecyclerAdapter.finishActionMode();
                List<Task> duplicates = taskDuplicator.duplicate(tasks);
                taskList.onTaskCreated(duplicates);
                taskList
                    .makeSnackbar(
                        context.getString(
                            R.string.copy_multiple_tasks_confirmation,
                            Integer.toString(duplicates.size())))
                    .show();
              }
            });
  }
}
