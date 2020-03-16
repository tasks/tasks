package com.todoroo.astrid.adapter;

import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.TagFilter;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.locale.Locale;
import org.tasks.preferences.SyncPreferences;
import org.tasks.themes.ColorProvider;
import org.tasks.themes.CustomIcons;
import org.tasks.themes.DrawableUtil;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeColor;

public class FilterViewHolder extends RecyclerView.ViewHolder {

  @Nullable
  @BindView(R.id.row)
  View row;

  @BindView(R.id.text)
  CheckedTextView text;

  @BindView(R.id.icon)
  ImageView icon;

  @Nullable
  @BindView(R.id.size)
  TextView size;

  private OnClick onClick;
  private boolean navigationDrawer;
  private Locale locale;
  private Activity activity;
  private View itemView;
  private Inventory inventory;
  private ColorProvider colorProvider;

  FilterViewHolder(
      @NonNull View itemView,
      ThemeAccent accent,
      boolean navigationDrawer,
      Locale locale,
      Activity activity,
      Inventory inventory,
      ColorProvider colorProvider,
      OnClick onClick) {
    super(itemView);
    this.inventory = inventory;
    this.colorProvider = colorProvider;

    ButterKnife.bind(this, itemView);

    this.itemView = itemView;
    this.navigationDrawer = navigationDrawer;
    this.locale = locale;
    this.activity = activity;
    this.onClick = onClick;

    if (navigationDrawer) {
      text.setCheckMarkDrawable(null);
    } else if (preLollipop()) {
      ColorStateList tintList =
          new ColorStateList(
              new int[][] {
                new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
              },
              new int[] {
                ResourcesCompat.getColor(
                    activity.getResources(), android.R.color.transparent, null),
                accent.getAccentColor()
              });
      Drawable original = ContextCompat.getDrawable(activity, R.drawable.ic_outline_done_24px);
      Drawable wrapped = DrawableCompat.wrap(original.mutate());
      DrawableCompat.setTintList(wrapped, tintList);
      text.setCheckMarkDrawable(wrapped);
    }
  }

  FilterViewHolder(@NonNull View itemView, Activity activity) {
    super(itemView);

    ButterKnife.bind(this, itemView);

    icon.setOnClickListener(
        v -> activity.startActivity(new Intent(activity, SyncPreferences.class)));
  }

  FilterViewHolder(@NonNull View itemView) {
    super(itemView);
  }

  public void bind(FilterListItem filter, boolean selected, Integer count) {
    if (navigationDrawer) {
      itemView.setSelected(selected);
    } else {
      text.setChecked(selected);
    }

    int icon = getIcon(filter);
    this.icon.setImageDrawable(DrawableUtil.getWrapped(activity, icon));
    DrawableCompat.setTint(this.icon.getDrawable(), getColor(filter));
    text.setText(filter.listingTitle);

    if (count == null || count == 0) {
      size.setVisibility(View.GONE);
    } else {
      size.setText(locale.formatNumber(count));
      size.setVisibility(View.VISIBLE);
    }

    if (onClick != null) {
      row.setOnClickListener(v -> onClick.onClick(filter));
    }
  }

  private int getColor(FilterListItem filter) {
    if (filter.tint != 0) {
      ThemeColor color = colorProvider.getThemeColor(filter.tint, true);
      if (color.isFree() || inventory.purchasedThemes()) {
        return color.getPrimaryColor();
      }
    }
    return ContextCompat.getColor(activity, R.color.text_primary);
  }

  private int getIcon(FilterListItem filter) {
    if (filter.icon < 1000 || inventory.hasPro()) {
      Integer icon = CustomIcons.getIconResId(filter.icon);
      if (icon != null) {
        return icon;
      }
    }
    if (filter instanceof TagFilter) {
      return R.drawable.ic_outline_label_24px;
    } else if (filter instanceof GtasksFilter || filter instanceof CaldavFilter) {
      return R.drawable.ic_outline_cloud_24px;
    } else if (filter instanceof CustomFilter) {
      return R.drawable.ic_outline_filter_list_24px;
    } else {
      return filter.icon;
    }
  }

  public interface OnClick {
    void onClick(@Nullable FilterListItem item);
  }
}
