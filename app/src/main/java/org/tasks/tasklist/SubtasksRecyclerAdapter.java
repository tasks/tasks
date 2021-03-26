package org.tasks.tasklist;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import org.tasks.data.TaskContainer;
import org.tasks.databinding.SubtaskAdapterRowBodyBinding;
import org.tasks.tasklist.SubtaskViewHolder.Callbacks;
import org.tasks.ui.CheckBoxProvider;
import org.tasks.ui.ChipProvider;

import java.util.List;

public class SubtasksRecyclerAdapter extends RecyclerView.Adapter<SubtaskViewHolder>
    implements ListUpdateCallback {

  private final DisplayMetrics metrics;
  private final Activity activity;
  private final ChipProvider chipProvider;
  private final CheckBoxProvider checkBoxProvider;
  private final Callbacks callbacks;
  private final AsyncListDiffer<TaskContainer> differ;
  private boolean multiLevelSubtasks;

  public SubtasksRecyclerAdapter(
      Activity activity,
      ChipProvider chipProvider,
      CheckBoxProvider checkBoxProvider,
      SubtaskViewHolder.Callbacks callbacks) {
    this.activity = activity;
    this.chipProvider = chipProvider;
    this.checkBoxProvider = checkBoxProvider;
    this.callbacks = callbacks;
    differ =
        new AsyncListDiffer<>(
            this, new AsyncDifferConfig.Builder<>(new ItemCallback()).build());
    metrics = activity.getResources().getDisplayMetrics();
  }

  @NonNull
  @Override
  public SubtaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new SubtaskViewHolder(
            SubtaskAdapterRowBodyBinding.inflate(LayoutInflater.from(activity), parent, false),
            callbacks,
            metrics,
            chipProvider,
            checkBoxProvider
    );
  }

  @Override
  public void onBindViewHolder(@NonNull SubtaskViewHolder holder, int position) {
    TaskContainer task = differ.getCurrentList().get(position);
    if (task != null) {
      task.setIndent(multiLevelSubtasks ? task.indent : 0);
      holder.bindView(task);
    }
  }

  public void submitList(List<TaskContainer> list) {
    differ.submitList(list);
  }

  @Override
  public void onInserted(int position, int count) {
    notifyItemRangeInserted(position, count);
  }

  @Override
  public void onRemoved(int position, int count) {
    notifyDataSetChanged(); // remove animation is janky
  }

  @Override
  public void onMoved(int fromPosition, int toPosition) {
    notifyItemMoved(fromPosition, toPosition);
  }

  @Override
  public void onChanged(int position, int count, @Nullable Object payload) {
    notifyItemRangeChanged(position, count, payload);
  }

  @Override
  public int getItemCount() {
    return differ.getCurrentList().size();
  }

  public void setMultiLevelSubtasksEnabled(boolean enabled) {
    if (multiLevelSubtasks != enabled) {
      multiLevelSubtasks = enabled;
      notifyItemRangeChanged(0, differ.getCurrentList().size());
    }
  }
}
