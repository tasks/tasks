package org.tasks.tasklist;

import android.os.Parcelable;
import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.RecyclerView;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.dao.TaskDao;
import java.util.List;
import org.tasks.data.TaskContainer;

public class PagedListRecyclerAdapter extends TaskListRecyclerAdapter {

  private final RecyclerView recyclerView;
  private final AsyncPagedListDiffer<TaskContainer> differ;

  public PagedListRecyclerAdapter(
      TaskAdapter adapter,
      RecyclerView recyclerView,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      List<TaskContainer> list,
      TaskDao taskDao) {
    super(adapter, viewHolderFactory, taskList, taskDao);
    this.recyclerView = recyclerView;
    differ =
        new AsyncPagedListDiffer<>(
            this, new AsyncDifferConfig.Builder<>(new ItemCallback()).build());
    if (list instanceof PagedList) {
      differ.submitList((PagedList<TaskContainer>) list);
    }
  }

  @Override
  public TaskContainer getItem(int position) {
    return differ.getItem(position);
  }

  public void submitList(List<TaskContainer> list) {
    differ.submitList((PagedList<TaskContainer>) list);
  }

  @Override
  public void onMoved(int fromPosition, int toPosition) {
    Parcelable recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();

    super.onMoved(fromPosition, toPosition);

    recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
  }

  @Override
  protected boolean dragAndDropEnabled() {
    return false;
  }

  @Override
  public int getItemCount() {
    return differ.getItemCount();
  }
}
