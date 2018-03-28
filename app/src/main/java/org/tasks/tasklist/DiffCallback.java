package org.tasks.tasklist;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil.ItemCallback;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;

class DiffCallback extends ItemCallback<Task> {

  private final TaskAdapter adapter;

  public DiffCallback(TaskAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override
  public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
    return oldItem.equals(newItem) && oldItem.getIndent() == adapter.getIndent(newItem);
  }
}
