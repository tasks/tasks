package org.tasks.tasklist;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;

import android.os.Bundle;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.primitives.Longs;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.utility.Flags;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.tasks.data.TaskContainer;
import org.tasks.intents.TaskIntents;

public class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ViewHolder.ViewHolderCallbacks, ListUpdateCallback {

  private static final String EXTRA_SELECTED_TASK_IDS = "extra_selected_task_ids";

  private final TaskAdapter adapter;
  private final ViewHolderFactory viewHolderFactory;
  private final TaskListFragment taskList;
  private final ActionModeProvider actionModeProvider;
  private final ItemTouchHelperCallback itemTouchHelperCallback;
  private final boolean isGoogleTaskList;
  private ActionMode mode = null;
  private List<TaskContainer> list;
  private PublishSubject<List<TaskContainer>> publishSubject = PublishSubject.create();
  private Queue<Pair<List<TaskContainer>, DiffResult>> updates = new LinkedList<>();
  private CompositeDisposable disposables = new CompositeDisposable();

  public TaskListRecyclerAdapter(
      TaskAdapter adapter,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      ActionModeProvider actionModeProvider,
      List<TaskContainer> list) {
    this.adapter = adapter;
    this.viewHolderFactory = viewHolderFactory;
    this.taskList = taskList;
    this.actionModeProvider = actionModeProvider;
    this.list = list;
    itemTouchHelperCallback =
        new ItemTouchHelperCallback(adapter, this, taskList, this::drainQueue);
    isGoogleTaskList = taskList.getFilter() instanceof GtasksFilter;
    Pair<List<TaskContainer>, DiffResult> initial = Pair.create(list, null);
    disposables.add(
        publishSubject
            .observeOn(Schedulers.computation())
            .scan(initial, this::calculateDiff)
            .skip(1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyDiff));
  }

  private Pair<List<TaskContainer>, DiffResult> calculateDiff(
      Pair<List<TaskContainer>, DiffResult> last, List<TaskContainer> next) {
    assertNotMainThread();

    DiffCallback cb = new DiffCallback(last.first, next, adapter);
    DiffResult result = DiffUtil.calculateDiff(cb, true);

    return Pair.create(next, result);
  }

  private void drainQueue() {
    assertMainThread();

    Pair<List<TaskContainer>, DiffResult> update = updates.poll();
    Bundle selections = getSaveState();
    while (update != null) {
      list = update.first;
      update.second.dispatchUpdatesTo((ListUpdateCallback) this);
      update = updates.poll();
    }
    restoreSaveState(selections);
  }

  private void applyDiff(Pair<List<TaskContainer>, DiffResult> update) {
    assertMainThread();

    updates.add(update);

    if (!itemTouchHelperCallback.isDragging() && !itemTouchHelperCallback.isSwiping()) {
      drainQueue();
    }
  }

  public ItemTouchHelperCallback getItemTouchHelperCallback() {
    return itemTouchHelperCallback;
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
  public int getItemCount() {
    return list.size();
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
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    disposables.dispose();
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

  public TaskContainer getItem(int position) {
    return list.get(position);
  }

  public void submitList(List<TaskContainer> list) {
    publishSubject.onNext(list);
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
    notifyItemMoved(fromPosition, toPosition);
  }

  @Override
  public void onChanged(int position, int count, @Nullable Object payload) {
    notifyItemRangeChanged(position, count, payload);
  }

  void moved(int from, int to, int indent) {
    adapter.moved(from, to, indent);
    TaskContainer task = list.remove(from);
    list.add(from < to ? to - 1 : to, task);
  }

  void swiped(int position, int delta) {
    adapter.swiped(position, delta);
  }
}
