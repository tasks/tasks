package org.tasks.tasklist;

import android.os.Bundle;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.primitives.Longs;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.utility.Flags;
import java.util.List;
import org.tasks.data.TaskContainer;
import org.tasks.intents.TaskIntents;

public abstract class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ViewHolder.ViewHolderCallbacks, ListUpdateCallback {

  private static final String EXTRA_SELECTED_TASK_IDS = "extra_selected_task_ids";

  protected final TaskAdapter adapter;
  final TaskListFragment taskList;
  private final ViewHolderFactory viewHolderFactory;
  private final ActionModeProvider actionModeProvider;
  private final boolean isGoogleTaskList;
  private ActionMode mode = null;

  TaskListRecyclerAdapter(
      TaskAdapter adapter,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      ActionModeProvider actionModeProvider) {
    this.adapter = adapter;
    this.viewHolderFactory = viewHolderFactory;
    this.taskList = taskList;
    this.actionModeProvider = actionModeProvider;
    isGoogleTaskList = taskList.getFilter() instanceof GtasksFilter;
  }

  public Bundle getSaveState() {
    Bundle information = new Bundle();
    List<Long> selectedTaskIds = adapter.getSelected();
    information.putLongArray(EXTRA_SELECTED_TASK_IDS, Longs.toArray(selectedTaskIds));
    return information;
  }

  public void restoreSaveState(Bundle savedState) {
    long[] longArray = savedState.getLongArray(EXTRA_SELECTED_TASK_IDS);
    if (longArray != null && longArray.length > 0) {
      mode = actionModeProvider.startActionMode(adapter, taskList, this);
      adapter.setSelected(longArray);

      updateModeTitle();
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return viewHolderFactory.newViewHolder(parent, this);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    TaskContainer task = getItem(position);
    if (task != null) {
      holder.bindView(task, isGoogleTaskList);
      holder.setMoving(false);
      int indent = adapter.getIndent(task);
      task.setIndent(indent);
      holder.setIndent(indent);
      holder.setSelected(adapter.isSelected(task));
    }
  }

  @Override
  public void onCompletedTask(TaskContainer task, boolean newState) {
    adapter.onCompletedTask(task, newState);
    taskList.loadTaskListContent();
  }

  @Override
  public void onClick(ViewHolder viewHolder) {
    if (mode == null) {
      taskList.onTaskListItemClicked(viewHolder.task.getTask());
    } else {
      toggle(viewHolder);
    }
  }

  @Override
  public void onClick(Filter filter) {
    if (mode == null) {
      FragmentActivity context = taskList.getActivity();
      if (context != null) {
        context.startActivity(TaskIntents.getTaskListIntent(context, filter));
      }
    }
  }

  @Override
  public boolean onLongPress(ViewHolder viewHolder) {
    if (!adapter.isManuallySorted()) {
      startActionMode();
    }
    if (mode != null && !viewHolder.isMoving()) {
      toggle(viewHolder);
    }
    return true;
  }

  void startActionMode() {
    if (mode == null) {
      mode = actionModeProvider.startActionMode(adapter, taskList, this);
      updateModeTitle();
      if (adapter.isManuallySorted()) {
        Flags.set(Flags.TLFP_NO_INTERCEPT_TOUCH);
      }
    }
  }

  void toggle(ViewHolder viewHolder) {
    adapter.toggleSelection(viewHolder.task);
    notifyItemChanged(viewHolder.getAdapterPosition());
    if (adapter.getSelected().isEmpty()) {
      finishActionMode();
    } else {
      updateModeTitle();
    }
  }

  private void updateModeTitle() {
    if (mode != null) {
      int count = Math.max(1, adapter.getNumSelected());
      mode.setTitle(Integer.toString(count));
    }
  }

  public void finishActionMode() {
    if (mode != null) {
      mode.finish();
    }
  }

  boolean isActionModeActive() {
    return mode != null;
  }

  void onDestroyActionMode() {
    mode = null;
  }

  public abstract TaskContainer getItem(int position);

  public abstract void submitList(List<TaskContainer> list);

  @Override
  public void onInserted(int position, int count) {
    notifyItemRangeInserted(position, count);
  }

  @Override
  public void onRemoved(int position, int count) {
    notifyItemRangeRemoved(position, count);
  }

  @Override
  public void onMoved(int fromPosition, int toPosition) {
    notifyItemMoved(fromPosition, toPosition);
  }

  @Override
  public void onChanged(int position, int count, @Nullable Object payload) {
    notifyItemRangeChanged(position, count, payload);
  }
}
