package org.tasks.dialogs;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import org.tasks.Callback;
import org.tasks.R;
import org.tasks.databinding.DialogIconPickerCellBinding;
import org.tasks.themes.DrawableUtil;

public class IconPickerHolder extends RecyclerView.ViewHolder {

  private final Context context;
  private final Callback<Integer> onClick;
  private final AppCompatImageView imageView;

  private int index;
  private boolean isEnabled;

  IconPickerHolder(Context context, DialogIconPickerCellBinding binding, Callback<Integer> onClick) {
    super(binding.getRoot());

    imageView = binding.icon;
    imageView.setOnClickListener(v -> onClick());

    this.context = context;
    this.onClick = onClick;
  }

  private void onClick() {
    if (isEnabled) {
      onClick.call(index);
    } else {
      Toast.makeText(context, R.string.requires_pro_subscription, Toast.LENGTH_SHORT).show();
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
