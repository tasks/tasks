package org.tasks.tasklist;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.utility.Flags;
import java.util.List;
import java.util.Objects;
import org.tasks.data.TaskContainer;
import org.tasks.intents.TaskIntents;

public abstract class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ViewHolder.ViewHolderCallbacks, ListUpdateCallback {

  protected final TaskAdapter adapter;
  final TaskListFragment taskList;
  private final RecyclerView recyclerView;
  private final ViewHolderFactory viewHolderFactory;
  private final ActionModeProvider actionModeProvider;
  private final boolean isRemoteList;
  private ActionMode mode = null;

  TaskListRecyclerAdapter(
      TaskAdapter adapter,
      RecyclerView recyclerView,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      ActionModeProvider actionModeProvider) {
    this.adapter = adapter;
    this.recyclerView = recyclerView;
    this.viewHolderFactory = viewHolderFactory;
    this.taskList = taskList;
    this.actionModeProvider = actionModeProvider;
    isRemoteList = taskList.getFilter() instanceof GtasksFilter || taskList.getFilter() instanceof CaldavFilter;
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
      holder.bindView(task, isRemoteList);
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
    if (!adapter.supportsParentingOrManualSort()) {
      startActionMode();
    }
    if (mode != null && !viewHolder.isMoving()) {
      toggle(viewHolder);
    }
    return true;
  }

  public void startActionMode() {
    if (mode == null) {
      mode = actionModeProvider.startActionMode(adapter, taskList, this);
      updateModeTitle();
      if (adapter.supportsParentingOrManualSort()) {
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
    LinearLayoutManager layoutManager =
        (LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager());
    View firstChild = layoutManager.getChildAt(0);
    int firstChildPosition = layoutManager.findFirstVisibleItemPosition();

    notifyItemMoved(fromPosition, toPosition);

    if (firstChildPosition > 0 && firstChild != null) {
      layoutManager.scrollToPositionWithOffset(firstChildPosition - 1, firstChild.getTop());
    } else if (firstChildPosition >= 0) {
      layoutManager.scrollToPosition(firstChildPosition);
    }
  }

  @Override
  public void onChanged(int position, int count, @Nullable Object payload) {
    notifyItemRangeChanged(position, count, payload);
  }
}
