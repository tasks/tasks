package org.tasks.dialogs;

import android.app.Activity;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.themes.CustomIcons;

public class IconPickerAdapter extends ListAdapter<Integer, IconPickerHolder> {

  private final Activity activity;
  private final Inventory inventory;
  private final int current;
  private final Callback<Integer> onSelected;

  public IconPickerAdapter(
      Activity activity, Inventory inventory, int current, Callback<Integer> onSelected) {
    super(new DiffCallback());
    this.activity = activity;
    this.inventory = inventory;
    this.current = current;
    this.onSelected = onSelected;
  }

  @NonNull
  @Override
  public IconPickerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new IconPickerHolder(
        activity,
        inventory,
        activity.getLayoutInflater().inflate(R.layout.dialog_icon_picker_cell, parent, false),
        onSelected);
  }

  @Override
  public void onBindViewHolder(@NonNull IconPickerHolder holder, int position) {
    int index = CustomIcons.getIndex(position);
    Integer icon = CustomIcons.getIconResId(index);
    if (icon != null) {
      holder.bind(index, icon, index == current);
    }
  }

  private static class DiffCallback extends ItemCallback<Integer> {
    @Override
    public boolean areItemsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
      return oldItem.equals(newItem);
    }

    @Override
    public boolean areContentsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
      return true;
    }
  }
}
