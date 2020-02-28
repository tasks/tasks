package org.tasks.dialogs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tasks.Callback;
import org.tasks.R;

public class IconPickerHolder extends RecyclerView.ViewHolder {

  private final Context context;
  private final Callback<Integer> onClick;

  @BindView(R.id.icon)
  AppCompatImageView imageView;

  private int index;
  private boolean isEnabled;

  IconPickerHolder(Context context, @NonNull View view, Callback<Integer> onClick) {
    super(view);

    ButterKnife.bind(this, view);

    this.context = context;
    this.onClick = onClick;
  }

  @OnClick(R.id.icon)
  void onClick() {
    if (isEnabled) {
      onClick.call(index);
    } else {
      Toast.makeText(context, R.string.requires_pro_subscription, Toast.LENGTH_SHORT).show();
    }
  }

  public void bind(int index, int icon, int tint, float alpha, boolean isEnabled) {
    this.index = index;
    this.isEnabled = isEnabled;
    imageView.setImageResource(icon);
    imageView.setAlpha(alpha);
    Drawable drawable = imageView.getDrawable();
    DrawableCompat.setTint(
        drawable instanceof LayerDrawable ? ((LayerDrawable) drawable).getDrawable(0) : drawable,
        tint);
  }
}
