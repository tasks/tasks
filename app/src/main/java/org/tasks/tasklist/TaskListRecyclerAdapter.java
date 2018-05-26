package org.tasks.tasklist;

import android.arch.paging.AsyncPagedListDiffer;
import android.arch.paging.PagedList;
import android.os.Bundle;
import android.support.v7.recyclerview.extensions.AsyncDifferConfig;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ViewGroup;
import com.google.common.primitives.Longs;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Flags;
import java.util.List;

public class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ViewHolder.ViewHolderCallbacks, ListUpdateCallback {

  private static final String EXTRA_SELECTED_TASK_IDS = "extra_selected_task_ids";

  private final TaskAdapter adapter;
  private final ViewHolderFactory viewHolderFactory;
  private final TaskListFragment taskList;
  private final ActionModeProvider actionModeProvider;
  private final AsyncPagedListDiffer<Task> asyncPagedListDiffer;
  private final ItemTouchHelperCallback itemTouchHelperCallback;

  private ActionMode mode = null;
  private boolean animate;
  private RecyclerView recyclerView;

  public TaskListRecyclerAdapter(
      TaskAdapter adapter,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      ActionModeProvider actionModeProvider) {
    this.adapter = adapter;
    this.viewHolderFactory = viewHolderFactory;
    this.taskList = taskList;
    this.actionModeProvider = actionModeProvider;
    itemTouchHelperCallback = new ItemTouchHelperCallback(adapter, this, taskList);
    asyncPagedListDiffer =
        new AsyncPagedListDiffer<>(
            this, new AsyncDifferConfig.Builder<>(new DiffCallback(adapter)).build());
  }

  public void applyToRecyclerView(RecyclerView recyclerView) {
    this.recyclerView = recyclerView;
    recyclerView.setAdapter(this);

    new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
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

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return viewHolderFactory.newViewHolder(parent, this);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    Task task = asyncPagedListDiffer.getItem(position);
    if (task != null) {
      holder.bindView(task);
      holder.setMoving(false);
      int indent = adapter.getIndent(task);
      task.setIndent(indent);
      holder.setIndent(indent);
      holder.setSelected(adapter.isSelected(task));
    }
  }

  @Override
  public int getItemCount() {
    return asyncPagedListDiffer.getItemCount();
  }

  @Override
  public void onCompletedTask(Task task, boolean newState) {
    adapter.onCompletedTask(task, newState);
  }

  @Override
  public void onClick(ViewHolder viewHolder) {
    if (mode == null) {
      taskList.onTaskListItemClicked(viewHolder.task);
    } else {
      toggle(viewHolder);
    }
  }

  @Override
  public boolean onLongPress(ViewHolder viewHolder) {
    toggle(viewHolder);
    return true;
  }

  private void toggle(ViewHolder viewHolder) {
    adapter.toggleSelection(viewHolder.task);
    notifyItemChanged(viewHolder.getAdapterPosition());
    if (adapter.getSelected().isEmpty()) {
      itemTouchHelperCallback.setDragging(false);
      finishActionMode();
    } else {
      if (mode == null) {
        mode = actionModeProvider.startActionMode(adapter, taskList, this);
        if (adapter.isManuallySorted()) {
          itemTouchHelperCallback.setDragging(true);
          Flags.set(Flags.TLFP_NO_INTERCEPT_TOUCH);
        } else {
          itemTouchHelperCallback.setDragging(false);
        }
      } else {
        itemTouchHelperCallback.setDragging(false);
      }
      updateModeTitle();
    }
  }

  private void updateModeTitle() {
    if (mode != null) {
      mode.setTitle(Integer.toString(adapter.getSelected().size()));
    }
  }

  public void finishActionMode() {
    if (mode != null) {
      mode.finish();
    }
  }

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
    if (animate) {
      notifyItemChanged(fromPosition);
      notifyItemMoved(fromPosition, toPosition);
      recyclerView.scrollToPosition(fromPosition);
    } else {
      notifyDataSetChanged();
    }
  }

  @Override
  public void onChanged(int position, int count, Object payload) {
    if (animate) {
      notifyItemRangeChanged(position, count, payload);
    } else {
      notifyDataSetChanged();
    }
  }

  public void onTaskSaved() {
    setAnimate(true);
    int scrollY = recyclerView.getScrollY();
    notifyDataSetChanged();
    recyclerView.setScrollY(scrollY);
  }

  public void setList(PagedList<Task> list) {
    asyncPagedListDiffer.submitList(list);
  }

  public void setAnimate(boolean animate) {
    this.animate = animate;
  }

  public AsyncPagedListDiffer<Task> getAsyncPagedListDiffer() {
    return asyncPagedListDiffer;
  }

  public boolean isActionModeActive() {
    return mode != null;
  }

  void onDestroyActionMode() {
    mode = null;
    if (!itemTouchHelperCallback.isDragging()) {
      notifyDataSetChanged();
    }
  }
}
