package org.tasks.tasklist;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;

import org.tasks.data.TaskContainer;
import org.tasks.dialogs.DateTimePicker;
import org.tasks.intents.TaskIntents;
import org.tasks.tasklist.ViewHolder.ViewHolderCallbacks;

import java.util.List;

public abstract class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ViewHolderCallbacks, ListUpdateCallback {

  private final TaskAdapter adapter;
  private final TaskListFragment taskList;
  private final ViewHolderFactory viewHolderFactory;
  private final TaskDao taskDao;

  TaskListRecyclerAdapter(
      TaskAdapter adapter,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      TaskDao taskDao) {
    this.adapter = adapter;
    this.viewHolderFactory = viewHolderFactory;
    this.taskList = taskList;
    this.taskDao = taskDao;
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
      holder.bindView(task, taskList.getFilter(), adapter.supportsManualSorting());
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
    if (taskList.isActionModeActive()) {
      toggle(viewHolder);
    } else {
      taskList.onTaskListItemClicked(viewHolder.task.getTask());
    }
  }

  @Override
  public void onClick(Filter filter) {
    if (!taskList.isActionModeActive()) {
      FragmentActivity context = taskList.getActivity();
      if (context != null) {
        context.startActivity(TaskIntents.getTaskListIntent(context, filter));
      }
    }
  }

  @Override
  public boolean onLongPress(ViewHolder viewHolder) {
    if (!dragAndDropEnabled()) {
      taskList.startActionMode();
    }
    if (taskList.isActionModeActive() && !viewHolder.isMoving()) {
      toggle(viewHolder);
    }
    return true;
  }

  @Override
  public void onChangeDueDate(TaskContainer task) {
    taskList.showDateTimePicker(task);
  }

  protected abstract boolean dragAndDropEnabled();

  @Override
  public void toggleSubtasks(TaskContainer task, boolean collapsed) {
    taskDao.setCollapsed(task.getId(), collapsed);
    taskList.broadcastRefresh();
  }

  void toggle(ViewHolder viewHolder) {
    adapter.toggleSelection(viewHolder.task);
    notifyItemChanged(viewHolder.getAdapterPosition());
    if (adapter.getSelected().isEmpty()) {
      taskList.finishActionMode();
    } else {
      taskList.updateModeTitle();
    }
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
