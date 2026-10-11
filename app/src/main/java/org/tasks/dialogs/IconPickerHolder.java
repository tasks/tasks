package org.tasks.dialogs;

import android.content.Context;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import org.tasks.Callback;
import org.tasks.databinding.DialogIconPickerCellBinding;
import org.tasks.themes.DrawableUtil;

public class IconPickerHolder extends RecyclerView.ViewHolder {

  private final Context context;
  private final Callback<Integer> onClick;
  private final Runnable onProRequired;
  private final AppCompatImageView imageView;

  private int index;
  private boolean isEnabled;

  IconPickerHolder(
      Context context,
      DialogIconPickerCellBinding binding,
      Callback<Integer> onClick,
      Runnable onProRequired) {
    super(binding.getRoot());

    imageView = binding.icon;
    imageView.setOnClickListener(v -> onClick());

    this.context = context;
    this.onClick = onClick;
    this.onProRequired = onProRequired;
  }

  private void onClick() {
    if (isEnabled) {
      onClick.call(index);
    } else {
      onProRequired.run();
    }
  }

  public void bind(int index, int icon, int tint, float alpha, boolean isEnabled) {
    this.index = index;
    this.isEnabled = isEnabled;
    imageView.setAlpha(alpha);
    imageView.setImageDrawable(DrawableUtil.getWrapped(context, icon));
    DrawableUtil.setTint(imageView.getDrawable(), tint);
  }
}
