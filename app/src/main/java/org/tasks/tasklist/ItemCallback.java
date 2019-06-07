package org.tasks.tasklist;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import org.tasks.data.TaskContainer;

public class ItemCallback extends DiffUtil.ItemCallback<TaskContainer> {

  @Override
  public boolean areItemsTheSame(@NonNull TaskContainer oldItem, @NonNull TaskContainer newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override
  public boolean areContentsTheSame(
      @NonNull TaskContainer oldItem, @NonNull TaskContainer newItem) {
    return oldItem.equals(newItem);
  }
}
