package org.tasks.tasklist;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import com.todoroo.astrid.adapter.TaskAdapter;
import org.tasks.data.TaskContainer;

class DiffCallback extends ItemCallback<TaskContainer> {

  private final TaskAdapter adapter;

  public DiffCallback(TaskAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  public boolean areItemsTheSame(@NonNull TaskContainer oldItem, @NonNull TaskContainer newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override
  public boolean areContentsTheSame(
      @NonNull TaskContainer oldItem, @NonNull TaskContainer newItem) {
    return oldItem.equals(newItem) && oldItem.getIndent() == adapter.getIndent(newItem);
  }
}
