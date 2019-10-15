package org.tasks.tasklist;

import static androidx.recyclerview.widget.ItemTouchHelper.DOWN;
import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;
import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;
import static androidx.recyclerview.widget.ItemTouchHelper.UP;

import android.graphics.Canvas;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.todoroo.astrid.adapter.CaldavTaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.utility.Flags;
import org.tasks.data.TaskContainer;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
  private final TaskAdapter adapter;
  private final ManualSortRecyclerAdapter recyclerAdapter;
  private final Runnable onClear;
  private int from = -1;
  private int to = -1;
  private boolean dragging;

  ItemTouchHelperCallback(
      TaskAdapter adapter, ManualSortRecyclerAdapter recyclerAdapter, Runnable onClear) {
    this.adapter = adapter;
    this.recyclerAdapter = recyclerAdapter;
    this.onClear = onClear;
  }

  @Override
  public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
    super.onSelectedChanged(viewHolder, actionState);
    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
      recyclerAdapter.startActionMode();
      ((ViewHolder) viewHolder).setMoving(true);
      dragging = true;
      int position = viewHolder.getAdapterPosition();
      updateIndents((ViewHolder) viewHolder, position, position);
    }
  }

  @Override
  public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
    return adapter.supportsParentingOrManualSort() && adapter.getNumSelected() == 0
        ? makeMovementFlags(UP | DOWN | LEFT | RIGHT, 0)
        : makeMovementFlags(0, 0);
  }

  @Override
  public boolean onMove(
      @NonNull RecyclerView recyclerView,
      @NonNull RecyclerView.ViewHolder src,
      @NonNull RecyclerView.ViewHolder target) {
    recyclerAdapter.finishActionMode();
    int fromPosition = src.getAdapterPosition();
    int toPosition = target.getAdapterPosition();
    ViewHolder source = (ViewHolder) src;
    if (!adapter.canMove(source, (ViewHolder) target)) {
      return false;
    }
    if (from == -1) {
      source.setSelected(false);
      from = fromPosition;
    }
    to = toPosition;
    recyclerAdapter.notifyItemMoved(fromPosition, toPosition);
    updateIndents(source, from, to);
    return true;
  }

  private void updateIndents(ViewHolder source, int from, int to) {
    TaskContainer task = source.task;
    source.setMinIndent(
        to == 0 || to == recyclerAdapter.getItemCount() - 1
            ? 0
            : adapter.minIndent(from <= to ? to + 1 : to, task));
    source.setMaxIndent(to == 0 ? 0 : adapter.maxIndent(from >= to ? to - 1 : to, task));
  }

  @Override
  public void onChildDraw(
      Canvas c,
      RecyclerView recyclerView,
      RecyclerView.ViewHolder viewHolder,
      float dX,
      float dY,
      int actionState,
      boolean isCurrentlyActive) {
    ViewHolder vh = (ViewHolder) viewHolder;
    TaskContainer task = vh.task;
    float shiftSize = vh.getShiftSize();
    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
      int currentIndent = ((ViewHolder) viewHolder).getIndent();
      int maxIndent = vh.getMaxIndent();
      int minIndent = vh.getMinIndent();
      if (isCurrentlyActive) {
        float dxAdjusted;
        if (dX > 0) {
          dxAdjusted = Math.min(dX, (maxIndent - currentIndent) * shiftSize);
        } else {
          dxAdjusted = Math.max((currentIndent - minIndent) * -shiftSize, dX);
        }

        int targetIndent = currentIndent + Float.valueOf(dxAdjusted / shiftSize).intValue();

        if (targetIndent != task.getIndent()) {
          if (from == -1) {
            recyclerAdapter.finishActionMode();
            vh.setSelected(false);
          }
        }
        if (targetIndent < minIndent) {
          task.setTargetIndent(minIndent);
        } else if (targetIndent > maxIndent) {
          task.setTargetIndent(maxIndent);
        } else {
          task.setTargetIndent(targetIndent);
        }
      }

      dX = (task.getTargetIndent() - task.getIndent()) * shiftSize;
    }
    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
  }

  @Override
  public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
    super.clearView(recyclerView, viewHolder);
    ViewHolder vh = (ViewHolder) viewHolder;
    vh.setMoving(false);
    dragging = false;
    onClear.run();
    if (recyclerAdapter.isActionModeActive()) {
      recyclerAdapter.toggle(vh);
    } else {
      TaskContainer task = vh.task;
      int targetIndent = task.getTargetIndent();
      if (from >= 0 && from != to) {
        if (from < to) {
          to++;
        }
        if (!(adapter instanceof CaldavTaskAdapter)) {
          vh.task.setIndent(targetIndent);
          vh.setIndent(targetIndent);
        }
        recyclerAdapter.moved(from, to, targetIndent);
      } else if (task.getIndent() != targetIndent) {
        int position = vh.getAdapterPosition();
        if (!(adapter instanceof CaldavTaskAdapter)) {
          vh.task.setIndent(targetIndent);
          vh.setIndent(targetIndent);
        }
        recyclerAdapter.moved(position, position, targetIndent);
      }
    }
    from = -1;
    to = -1;
    Flags.checkAndClear(Flags.TLFP_NO_INTERCEPT_TOUCH);
  }

  @Override
  public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
    throw new UnsupportedOperationException();
  }

  boolean isDragging() {
    return dragging;
  }
}
