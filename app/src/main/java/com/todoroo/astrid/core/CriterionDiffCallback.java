package com.todoroo.astrid.core;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class CriterionDiffCallback extends DiffUtil.ItemCallback<CriterionInstance> {

  @Override
  public boolean areItemsTheSame(
      @NonNull CriterionInstance oldItem, @NonNull CriterionInstance newItem) {
    return oldItem.getId().equals(newItem.getId());
  }

  @Override
  public boolean areContentsTheSame(
      @NonNull CriterionInstance oldItem, @NonNull CriterionInstance newItem) {
    return oldItem.equals(newItem);
  }
}
