package org.tasks.tags;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.util.Objects;
import org.tasks.data.TagData;

class TagDiffCallback extends DiffUtil.ItemCallback<TagData> {

  @Override
  public boolean areItemsTheSame(@NonNull TagData oldItem, @NonNull TagData newItem) {
    return Objects.equals(oldItem.getId(), newItem.getId());
  }

  @Override
  public boolean areContentsTheSame(@NonNull TagData oldItem, @NonNull TagData newItem) {
    return oldItem.equals(newItem);
  }
}
