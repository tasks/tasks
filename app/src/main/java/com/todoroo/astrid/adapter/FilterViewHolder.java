package com.todoroo.astrid.adapter;

import static androidx.core.content.ContextCompat.getColor;
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
import butterknife.BindView;
import butterknife.ButterKnife;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import org.tasks.R;
import org.tasks.filters.NavigationDrawerAction;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.intents.TaskIntents;
import org.tasks.locale.Locale;
import org.tasks.sync.SynchronizationPreferences;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeCache;

public class FilterViewHolder {

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

  private ThemeCache themeCache;
  private boolean navigationDrawer;
  private Locale locale;
  private Activity activity;
  private View itemView;

  FilterViewHolder(
      @NonNull View itemView,
      ThemeAccent accent,
      ThemeCache themeCache,
      boolean navigationDrawer,
      Locale locale,
      Activity activity) {
    ButterKnife.bind(this, itemView);

    this.itemView = itemView;
    this.themeCache = themeCache;
    this.navigationDrawer = navigationDrawer;
    this.locale = locale;
    this.activity = activity;

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
    ButterKnife.bind(this, itemView);

    icon.setOnClickListener(
        v -> activity.startActivity(new Intent(activity, SynchronizationPreferences.class)));
  }

  FilterViewHolder() {
  }

  public void bind(FilterListItem filter, boolean selected, Integer count) {
    if (selected) {
      if (navigationDrawer) {
        itemView.setBackgroundColor(getColor(activity, R.color.drawer_color_selected));
      } else {
        text.setChecked(true);
      }
    } else {
      itemView.setBackgroundResource(0);
      text.setChecked(false);
    }

    icon.setImageResource(filter.icon);
    icon.setColorFilter(
        filter.tint >= 0
            ? themeCache.getThemeColor(filter.tint).getPrimaryColor()
            : getColor(activity, R.color.text_primary));

    text.setText(filter.listingTitle);

    if (count == null || count == 0) {
      size.setVisibility(View.GONE);
    } else {
      size.setText(locale.formatNumber(count));
      size.setVisibility(View.VISIBLE);
    }

    row.setOnClickListener(
        v -> {
          if (filter instanceof Filter) {
            if (!selected) {
              activity.startActivity(TaskIntents.getTaskListIntent(activity, (Filter) filter));
            }
          } else if (filter instanceof NavigationDrawerAction) {
            NavigationDrawerAction action = (NavigationDrawerAction) filter;
            if (action.requestCode > 0) {
              activity.startActivityForResult(action.intent, action.requestCode);
            } else {
              activity.startActivity(action.intent);
            }
          }
        });
  }

  public void bind(NavigationDrawerSubheader filter) {
    text.setText(filter.listingTitle);
    icon.setVisibility(filter.error ? View.VISIBLE : View.GONE);
  }
}
