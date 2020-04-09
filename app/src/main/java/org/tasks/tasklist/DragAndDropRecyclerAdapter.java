package org.tasks.tasklist;

import static androidx.recyclerview.widget.ItemTouchHelper.DOWN;
import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;
import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;
import static androidx.recyclerview.widget.ItemTouchHelper.UP;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;

import android.graphics.Canvas;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.utility.Flags;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.tasks.data.TaskContainer;

public class DragAndDropRecyclerAdapter extends TaskListRecyclerAdapter {

  private static final int LONG_LIST_SIZE = 500;

  private final TaskAdapter adapter;
  private final TaskListFragment taskList;
  private final RecyclerView recyclerView;
  private List<TaskContainer> list;
  private final PublishSubject<List<TaskContainer>> publishSubject = PublishSubject.create();
  private final CompositeDisposable disposables = new CompositeDisposable();
  private final Queue<Pair<List<TaskContainer>, DiffResult>> updates = new LinkedList<>();
  private boolean dragging;

  public DragAndDropRecyclerAdapter(
      TaskAdapter adapter,
      RecyclerView recyclerView,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      List<TaskContainer> list,
      TaskDao taskDao) {
    super(adapter, viewHolderFactory, taskList, taskDao);

    this.adapter = adapter;
    this.recyclerView = recyclerView;
    this.taskList = taskList;
    this.list = list;
    new ItemTouchHelper(new ItemTouchHelperCallback()).attachToRecyclerView(recyclerView);
    Pair<List<TaskContainer>, DiffResult> initial = Pair.create(list, null);
    disposables.add(
        publishSubject
            .observeOn(Schedulers.computation())
            .scan(initial, this::calculateDiff)
            .skip(1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyDiff));
  }

  @Override
  protected boolean dragAndDropEnabled() {
    return adapter.supportsParentingOrManualSort();
  }

  @Override
  public TaskContainer getItem(int position) {
    return list.get(position);
  }

  @Override
  public void submitList(List<TaskContainer> list) {
    publishSubject.onNext(list);
  }

  private Pair<List<TaskContainer>, DiffResult> calculateDiff(
      Pair<List<TaskContainer>, DiffResult> last, List<TaskContainer> next) {
    assertNotMainThread();

    DiffCallback cb = new DiffCallback(last.first, next, adapter);
    DiffResult result = DiffUtil.calculateDiff(cb, next.size() < LONG_LIST_SIZE);

    return Pair.create(next, result);
  }

  private void applyDiff(Pair<List<TaskContainer>, DiffResult> update) {
    assertMainThread();

    updates.add(update);

    if (!dragging) {
      drainQueue();
    }
  }

  private void drainQueue() {
    assertMainThread();

    Parcelable recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();

    Pair<List<TaskContainer>, DiffResult> update = updates.poll();
    while (update != null) {
      list = update.first;
      update.second.dispatchUpdatesTo((ListUpdateCallback) this);
      update = updates.poll();
    }

    recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    disposables.dispose();
  }

  @Override
  public int getItemCount() {
    return list.size();
  }

  private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private int from = -1;
    private int to = -1;

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
      super.onSelectedChanged(viewHolder, actionState);
      if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
        taskList.startActionMode();
        ((ViewHolder) viewHolder).setMoving(true);
        dragging = true;
        int position = viewHolder.getAdapterPosition();
        updateIndents((ViewHolder) viewHolder, position, position);
      }
    }

    @Override
    public int getMovementFlags(
        @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      return adapter.supportsParentingOrManualSort() && adapter.getNumSelected() == 0
          ? makeMovementFlags(UP | DOWN | LEFT | RIGHT, 0)
          : makeMovementFlags(0, 0);
    }

    @Override
    public boolean onMove(
        @NonNull RecyclerView recyclerView,
        @NonNull RecyclerView.ViewHolder src,
        @NonNull RecyclerView.ViewHolder target) {
      taskList.finishActionMode();
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
      notifyItemMoved(fromPosition, toPosition);
      updateIndents(source, from, to);
      return true;
    }

    private void updateIndents(ViewHolder source, int from, int to) {
      TaskContainer task = source.task;
      source.setMinIndent(
          to == 0 || to == getItemCount() - 1
              ? 0
              : adapter.minIndent(from <= to ? to + 1 : to, task));
      source.setMaxIndent(to == 0 ? 0 : adapter.maxIndent(from >= to ? to - 1 : to, task));
    }

    @Override
    public void onChildDraw(
        @NonNull Canvas c,
        @NonNull RecyclerView recyclerView,
        @NonNull RecyclerView.ViewHolder viewHolder,
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
              taskList.finishActionMode();
              vh.setSelected(false);
            }
          }
          if (targetIndent < minIndent) {
            task.setTargetIndent(minIndent);
          } else
            task.setTargetIndent(Math.min(targetIndent, maxIndent));
        }

        dX = (task.getTargetIndent() - task.getIndent()) * shiftSize;
      }
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(
        @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      super.clearView(recyclerView, viewHolder);
      ViewHolder vh = (ViewHolder) viewHolder;
      vh.setMoving(false);
      dragging = false;
      drainQueue();
      if (taskList.isActionModeActive()) {
        toggle(vh);
      } else {
        TaskContainer task = vh.task;
        int targetIndent = task.getTargetIndent();
        if (from >= 0 && from != to) {
          if (from < to) {
            to++;
          }
          vh.task.setIndent(targetIndent);
          vh.setIndent(targetIndent);
          moved(from, to, targetIndent);
        } else if (task.getIndent() != targetIndent) {
          int position = vh.getAdapterPosition();
          vh.task.setIndent(targetIndent);
          vh.setIndent(targetIndent);
          moved(position, position, targetIndent);
        }
      }
      from = -1;
      to = -1;
      Flags.clear(Flags.TLFP_NO_INTERCEPT_TOUCH);
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
      throw new UnsupportedOperationException();
    }

    private void moved(int from, int to, int indent) {
      adapter.moved(from, to, indent);
      TaskContainer task = list.remove(from);
      list.add(from < to ? to - 1 : to, task);
      taskList.loadTaskListContent();
    }
  }
}
