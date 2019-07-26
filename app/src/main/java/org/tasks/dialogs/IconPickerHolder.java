package org.tasks.dialogs;

import static org.tasks.preferences.ResourceResolver.getData;
import static org.tasks.preferences.ResourceResolver.getDimen;

import android.content.Context;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.billing.Inventory;

public class IconPickerHolder extends RecyclerView.ViewHolder {

  private final Context context;
  private final Callback<Integer> onClick;
  private final Inventory inventory;

  @BindView(R.id.icon)
  AppCompatImageView imageView;

  private int index;

  IconPickerHolder(
      Context context, Inventory inventory, @NonNull View view, Callback<Integer> onClick) {
    super(view);
    this.inventory = inventory;

    ButterKnife.bind(this, view);

    this.context = context;
    this.onClick = onClick;
  }

  @OnClick(R.id.icon)
  void onClick() {
    if (isEnabled()) {
      onClick.call(index);
    } else {
      Toast.makeText(context, R.string.requires_pro_subscription, Toast.LENGTH_SHORT).show();
    }
  }

  public void bind(int index, int icon, boolean selected) {
    this.index = index;
    imageView.setImageResource(icon);
    if (inventory.hasPro()) {
      imageView.setAlpha(getDimen(context, R.dimen.alpha_secondary));
    } else {
      imageView.setAlpha(index < 1000 ? 1.0f : getDimen(context, R.dimen.alpha_disabled));
    }
    DrawableCompat.setTint(
        imageView.getDrawable(),
        selected
            ? getData(context, R.attr.colorAccent)
            : ContextCompat.getColor(context, R.color.icon_tint));
  }

  private boolean isEnabled() {
    return index < 1000 || inventory.hasPro();
  }
}
