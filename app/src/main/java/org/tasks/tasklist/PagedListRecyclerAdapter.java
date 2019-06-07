package org.tasks.tasklist;

import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.AsyncDifferConfig;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import java.util.List;
import org.tasks.data.TaskContainer;

public class PagedListRecyclerAdapter extends TaskListRecyclerAdapter {

  private AsyncPagedListDiffer<TaskContainer> differ;

  public PagedListRecyclerAdapter(
      TaskAdapter adapter,
      ViewHolderFactory viewHolderFactory,
      TaskListFragment taskList,
      ActionModeProvider actionModeProvider,
      List<TaskContainer> list) {
    super(adapter, viewHolderFactory, taskList, actionModeProvider);

    differ =
        new AsyncPagedListDiffer<>(
            this, new AsyncDifferConfig.Builder<>(new ItemCallback()).build());
    if (list instanceof PagedList) {
      differ.submitList((PagedList<TaskContainer>) list);
    }
  }

  @Override
  public int getItemCount() {
    return differ.getItemCount();
  }

  @Override
  public TaskContainer getItem(int position) {
    return differ.getItem(position);
  }

  @Override
  public void submitList(List<TaskContainer> list) {
    differ.submitList((PagedList<TaskContainer>) list);
  }
}
