package org.tasks.tasklist;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.utility.Flags;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import org.tasks.data.TaskContainer;
import org.tasks.intents.TaskIntents;

public class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ViewHolder.ViewHolderCallbacks, ListUpdateCallback {

  private static final int LONG_LIST_SIZE = 500;

  private final TaskAdapter adapter;
  private final TaskListFragment taskList;
  private final RecyclerView recyclerView;
  private final ViewHolderFactory viewHolderFactory;
  private final ActionModeProvider actionModeProvider;
  private final boolean isRemoteList;
  private final ItemTouchHelperCallback itemTouchHelperCallback;
  private final TaskDao taskDao;
  private ActionMode mode = null;
  private List<TaskContainer> list;
  private PublishSubject<List<TaskContainer>> publishSubject = PublishSubject.create();
  private CompositeDisposable disposables = new CompositeDisposable();
  private Queue<Pair<List<TaskContainer>, DiffResult>> updates = new LinkedList<>();

  public TaskListRecyclerAdapter(
      TaskAdapter adapter,
      RecyclerView recyclerView,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      ActionModeProvider actionModeProvider,
      List<TaskContainer> list,
      TaskDao taskDao) {
    this.adapter = adapter;
    this.recyclerView = recyclerView;
    this.viewHolderFactory = viewHolderFactory;
    this.taskList = taskList;
    this.actionModeProvider = actionModeProvider;
    isRemoteList =
        taskList.getFilter() instanceof GtasksFilter
            || taskList.getFilter() instanceof CaldavFilter;
    this.list = list;
    itemTouchHelperCallback = new ItemTouchHelperCallback(adapter, this, this::drainQueue);
    this.taskDao = taskDao;
    new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    Pair<List<TaskContainer>, DiffResult> initial = Pair.create(list, null);
    disposables.add(
        publishSubject
            .observeOn(Schedulers.computation())
            .scan(initial, this::calculateDiff)
            .skip(1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyDiff));
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
      holder.bindView(task, isRemoteList, adapter.supportsManualSorting());
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

  @Override
  public void toggleSubtasks(TaskContainer task, boolean collapsed) {
    taskDao.setCollapsed(task.getId(), collapsed);
    taskList.broadcastRefresh();
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

  private Pair<List<TaskContainer>, DiffResult> calculateDiff(
      Pair<List<TaskContainer>, DiffResult> last, List<TaskContainer> next) {
    assertNotMainThread();

    DiffCallback cb = new DiffCallback(last.first, next, adapter);
    boolean shortList = next.size() < LONG_LIST_SIZE;
    boolean calculateDiff = last.first.size() != next.size() || shortList;
    DiffResult result = calculateDiff ? DiffUtil.calculateDiff(cb, shortList) : null;

    return Pair.create(next, result);
  }

  private void applyDiff(Pair<List<TaskContainer>, DiffResult> update) {
    assertMainThread();

    updates.add(update);

    if (!itemTouchHelperCallback.isDragging()) {
      drainQueue();
    }
  }

  private void drainQueue() {
    assertMainThread();

    Pair<List<TaskContainer>, DiffResult> update = updates.poll();
    while (update != null) {
      list = update.first;
      if (update.second == null) {
        notifyDataSetChanged();
      } else {
        update.second.dispatchUpdatesTo((ListUpdateCallback) this);
      }
      update = updates.poll();
    }
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    disposables.dispose();
  }

  @Override
  public int getItemCount() {
    return list.size();
  }

  void moved(int from, int to, int indent) {
    adapter.moved(from, to, indent);
    TaskContainer task = list.remove(from);
    list.add(from < to ? to - 1 : to, task);
    taskList.loadTaskListContent();
  }
}
