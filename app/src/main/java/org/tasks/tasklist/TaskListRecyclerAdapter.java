package org.tasks.tasklist;

import android.app.Activity;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapterHelper;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v7.recyclerview.extensions.ListAdapterConfig;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import com.google.common.primitives.Longs;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskDuplicator;
import com.todoroo.astrid.utility.Flags;
import java.util.List;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.ui.MenuColorizer;

public class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ViewHolder.ViewHolderCallbacks, ListUpdateCallback {

  private static final String EXTRA_SELECTED_TASK_IDS = "extra_selected_task_ids";

  private final Activity activity;
  private final TaskAdapter adapter;
  private final ViewHolderFactory viewHolderFactory;
  private final TaskListFragment taskList;
  private final TaskDeleter taskDeleter;
  private final TaskDuplicator taskDuplicator;
  private final Tracker tracker;
  private final DialogBuilder dialogBuilder;
  private final ItemTouchHelper itemTouchHelper;
  private final PagedListAdapterHelper<Task> adapterHelper;

  private ActionMode mode = null;
  private boolean dragging;
  private boolean animate;
  private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
      MenuInflater inflater = actionMode.getMenuInflater();
      inflater.inflate(R.menu.menu_multi_select, menu);
      MenuColorizer.colorMenu(activity, menu);
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      switch (item.getItemId()) {
        case R.id.delete:
          dialogBuilder.newMessageDialog(R.string.delete_selected_tasks)
              .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> deleteSelectedItems())
              .setNegativeButton(android.R.string.cancel, null)
              .show();
          return true;
        case R.id.copy_tasks:
          dialogBuilder.newMessageDialog(R.string.copy_selected_tasks)
              .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> copySelectedItems()))
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
      TaskListRecyclerAdapter.this.mode = null;
      if (!dragging) {
        notifyDataSetChanged();
      }
    }
  };

  public TaskListRecyclerAdapter(Activity activity, TaskAdapter adapter,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList, TaskDeleter taskDeleter,
      TaskDuplicator taskDuplicator, Tracker tracker,
      DialogBuilder dialogBuilder) {
    this.activity = activity;
    this.adapter = adapter;
    this.viewHolderFactory = viewHolderFactory;
    this.taskList = taskList;
    this.taskDeleter = taskDeleter;
    this.taskDuplicator = taskDuplicator;
    this.tracker = tracker;
    this.dialogBuilder = dialogBuilder;
    itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
    adapterHelper = new PagedListAdapterHelper<>(this,
        new ListAdapterConfig.Builder<Task>().setDiffCallback(new DiffCallback(adapter)).build());
  }

  public void applyToRecyclerView(RecyclerView recyclerView) {
    recyclerView.setAdapter(this);
    itemTouchHelper.attachToRecyclerView(recyclerView);
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
      mode = ((TaskListActivity) activity).startSupportActionMode(actionModeCallback);
      adapter.setSelected(longArray);

      updateModeTitle();
    }
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    ViewGroup view = (ViewGroup) LayoutInflater.from(activity)
        .inflate(R.layout.task_adapter_row_simple, parent, false);
    return viewHolderFactory.newViewHolder(view, this);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    Task task = adapterHelper.getItem(position);
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
    return adapterHelper.getItemCount();
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
      dragging = false;
      finishActionMode();
    } else {
      if (mode == null) {
        mode = ((TaskListActivity) activity).startSupportActionMode(actionModeCallback);
        if (adapter.isManuallySorted()) {
          dragging = true;
          Flags.set(Flags.TLFP_NO_INTERCEPT_TOUCH);
        } else {
          dragging = false;
        }
      } else {
        dragging = false;
      }
      updateModeTitle();
    }
  }

  private void deleteSelectedItems() {
    tracker.reportEvent(Tracking.Events.MULTISELECT_DELETE);
    List<Long> tasks = adapter.getSelected();
    finishActionMode();
    List<Task> result = taskDeleter.markDeleted(tasks);
    taskList.onTaskDelete(result);
    taskList.makeSnackbar(activity
        .getString(R.string.delete_multiple_tasks_confirmation, Integer.toString(result.size())))
        .show();
  }

  private void copySelectedItems() {
    tracker.reportEvent(Tracking.Events.MULTISELECT_CLONE);
    List<Long> tasks = adapter.getSelected();
    finishActionMode();
    List<Task> duplicates = taskDuplicator.duplicate(tasks);
    taskList.onTaskCreated(duplicates);
    taskList.makeSnackbar(activity
        .getString(R.string.copy_multiple_tasks_confirmation, Integer.toString(duplicates.size())))
        .show();
  }

  private void updateModeTitle() {
    if (mode != null) {
      mode.setTitle(Integer.toString(adapter.getSelected().size()));
    }
  }

  private void finishActionMode() {
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
      notifyItemMoved(fromPosition, toPosition);
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

  public void setList(PagedList<Task> list) {
    adapterHelper.setList(list);
  }

  public void setAnimate(boolean animate) {
    this.animate = animate;
  }

  public PagedListAdapterHelper<Task> getHelper() {
    return adapterHelper;
  }

  private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private int from = -1;
    private int to = -1;

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
      super.onSelectedChanged(viewHolder, actionState);
      if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
        ((ViewHolder) viewHolder).setMoving(true);
      }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
      if (!adapter.isManuallySorted()) {
        return makeMovementFlags(0, 0);
      }
      ViewHolder vh = (ViewHolder) viewHolder;
      if (mode == null) {
        int indentFlags = 0;
        if (vh.isIndented()) {
          indentFlags |= ItemTouchHelper.LEFT;
        }
        int position = vh.getAdapterPosition();
        if (position > 0 && adapter.canIndent(position, vh.task)) {
          indentFlags |= ItemTouchHelper.RIGHT;
        }
        return makeMovementFlags(0, indentFlags);
      }
      if (dragging) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
      }
      return makeMovementFlags(0, 0);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source,
        RecyclerView.ViewHolder target) {
      finishActionMode();
      int fromPosition = source.getAdapterPosition();
      int toPosition = target.getAdapterPosition();
      if (from == -1) {
        ((ViewHolder) source).setSelected(false);
        from = fromPosition;
      }
      to = toPosition;
      notifyItemMoved(fromPosition, toPosition);
      return true;
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
      return .2f;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
        float dX, float dY, int actionState, boolean isCurrentlyActive) {
      if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
        float shiftSize = ((ViewHolder) viewHolder).getShiftSize();
        dX = Math.max(-shiftSize, Math.min(shiftSize, dX));
      }
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
      super.clearView(recyclerView, viewHolder);
      ViewHolder vh = (ViewHolder) viewHolder;
      if (dragging) {
        vh.setMoving(false);
        dragging = false;
        if (from != -1) {
          if (from >= 0 && from != to) {
            if (from < to) {
              to++;
            }
            adapter.moved(from, to);
            taskList.loadTaskListContent(false);
          }
        }
      }
      from = -1;
      to = -1;
      Flags.checkAndClear(Flags.TLFP_NO_INTERCEPT_TOUCH);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
      adapter.indented(
          viewHolder.getAdapterPosition(),
          direction == ItemTouchHelper.RIGHT ? 1 : -1);
      taskList.loadTaskListContent(false);
    }
  }
}
