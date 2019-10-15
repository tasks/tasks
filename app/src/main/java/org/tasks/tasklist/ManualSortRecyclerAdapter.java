package org.tasks.tasklist;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.tasks.data.TaskContainer;

public class ManualSortRecyclerAdapter extends TaskListRecyclerAdapter {

  private final ItemTouchHelperCallback itemTouchHelperCallback;
  private List<TaskContainer> list;
  private PublishSubject<List<TaskContainer>> publishSubject = PublishSubject.create();
  private CompositeDisposable disposables = new CompositeDisposable();
  private Queue<Pair<List<TaskContainer>, DiffResult>> updates = new LinkedList<>();

  public ManualSortRecyclerAdapter(
      TaskAdapter adapter,
      RecyclerView recyclerView,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      ActionModeProvider actionModeProvider,
      List<TaskContainer> list) {
    super(adapter, recyclerView, viewHolderFactory, taskList, actionModeProvider);
    this.list = list;
    itemTouchHelperCallback = new ItemTouchHelperCallback(adapter, this, this::drainQueue);
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

  private Pair<List<TaskContainer>, DiffResult> calculateDiff(
      Pair<List<TaskContainer>, DiffResult> last, List<TaskContainer> next) {
    assertNotMainThread();

    DiffCallback cb = new DiffCallback(last.first, next, adapter);
    DiffResult result = DiffUtil.calculateDiff(cb, true);

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
      update.second.dispatchUpdatesTo((ListUpdateCallback) this);
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

  @Override
  public TaskContainer getItem(int position) {
    return list.get(position);
  }

  @Override
  public void submitList(List<TaskContainer> list) {
    publishSubject.onNext(list);
  }

  void moved(int from, int to, int indent) {
    adapter.moved(from, to, indent);
    if (list instanceof ArrayList) {
      TaskContainer task = list.remove(from);
      list.add(from < to ? to - 1 : to, task);
    }
    taskList.loadTaskListContent();
  }
}
