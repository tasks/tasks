package org.tasks.tasklist;

import androidx.recyclerview.widget.DiffUtil;
import com.todoroo.astrid.adapter.TaskAdapter;
import java.util.List;
import org.tasks.data.TaskContainer;

class DiffCallback extends DiffUtil.Callback {

  private final List<TaskContainer> oldList;
  private final List<TaskContainer> newList;
  @Deprecated private final TaskAdapter adapter;

  DiffCallback(List<TaskContainer> oldList, List<TaskContainer> newList, TaskAdapter adapter) {
    this.oldList = oldList;
    this.newList = newList;
    this.adapter = adapter;
  }

  @Override
  public int getOldListSize() {
    return oldList.size();
  }

  @Override
  public int getNewListSize() {
    return newList.size();
  }

  @Override
  public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
    return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
  }

  @Override
  public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
    TaskContainer oldItem = oldList.get(oldItemPosition);
    TaskContainer newItem = newList.get(newItemPosition);
    return oldItem.equals(newItem) && oldItem.getIndent() == adapter.getIndent(newItem);
  }
}
