/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.adapter;

import static androidx.core.content.ContextCompat.getColor;
import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.locale.Locale;
import org.tasks.sync.SynchronizationPreferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;

public class FilterAdapter extends BaseAdapter {

  public static final int REQUEST_SETTINGS = 10123;
  public static final int REQUEST_PURCHASE = 10124;

  private static final String TOKEN_FILTERS = "token_filters";
  private static final String TOKEN_SELECTED = "token_selected";
  private static final int VIEW_TYPE_COUNT = FilterListItem.Type.values().length;
  private final Activity activity;
  private final Theme theme;
  private final Locale locale;
  private final LayoutInflater inflater;
  private final ThemeCache themeCache;
  private boolean navigationDrawer;
  private Filter selected = null;
  private List<FilterListItem> items = new ArrayList<>();
  private Map<Filter, Integer> counts = new HashMap<>();

  @Inject
  public FilterAdapter(Activity activity, Theme theme, ThemeCache themeCache, Locale locale) {
    this.activity = activity;
    this.theme = theme;
    this.locale = locale;
    this.inflater = theme.getLayoutInflater(activity);
    this.themeCache = themeCache;
  }

  public void setNavigationDrawer() {
    navigationDrawer = true;
  }

  public void save(Bundle outState) {
    outState.putParcelableArrayList(TOKEN_FILTERS, getItems());
    outState.putParcelable(TOKEN_SELECTED, selected);
  }

  public void restore(Bundle savedInstanceState) {
    items = savedInstanceState.getParcelableArrayList(TOKEN_FILTERS);
    selected = savedInstanceState.getParcelable(TOKEN_SELECTED);
  }

  public void setData(List<FilterListItem> items) {
    setData(items, selected);
  }

  public void setData(List<FilterListItem> items, @Nullable Filter selected) {
    setData(items, selected, -1);
  }

  public void setData(List<FilterListItem> items, @Nullable Filter selected, int defaultIndex) {
    assertMainThread();
    this.items = items;
    this.selected = defaultIndex >= 0 ? getFilter(indexOf(selected, defaultIndex)) : selected;
    notifyDataSetChanged();
  }

  public void setCounts(Map<Filter, Integer> counts) {
    assertMainThread();
    this.counts = counts;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    assertMainThread();
    return items.size();
  }

  @Override
  public FilterListItem getItem(int position) {
    assertMainThread();
    return items.get(position);
  }

  private Filter getFilter(int position) {
    FilterListItem item = getItem(position);
    return item instanceof Filter ? (Filter) item : null;
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  /** Create or reuse a view */
  private View newView(View convertView, ViewGroup parent, FilterListItem.Type viewType) {
    if (convertView == null) {
      ViewHolder viewHolder = new ViewHolder();
      switch (viewType) {
        case ITEM:
          convertView = inflater.inflate(R.layout.filter_adapter_row, parent, false);
          viewHolder.name = convertView.findViewById(R.id.name);
          if (navigationDrawer) {
            viewHolder.name.setCheckMarkDrawable(null);
          } else if (preLollipop()) {
            ColorStateList tintList =
                new ColorStateList(
                    new int[][] {
                      new int[] {-android.R.attr.state_checked},
                      new int[] {android.R.attr.state_checked}
                    },
                    new int[] {
                      ResourcesCompat.getColor(
                          activity.getResources(), android.R.color.transparent, null),
                      theme.getThemeAccent().getAccentColor()
                    });
            Drawable original =
                ContextCompat.getDrawable(activity, R.drawable.ic_outline_done_24px);
            Drawable wrapped = DrawableCompat.wrap(original.mutate());
            DrawableCompat.setTintList(wrapped, tintList);
            viewHolder.name.setCheckMarkDrawable(wrapped);
          }
          viewHolder.icon = convertView.findViewById(R.id.icon);
          viewHolder.size = convertView.findViewById(R.id.size);
          break;
        case SEPARATOR:
          convertView = inflater.inflate(R.layout.filter_adapter_separator, parent, false);
          break;
        case SUBHEADER:
          convertView = inflater.inflate(R.layout.filter_adapter_subheader, parent, false);
          viewHolder.name = convertView.findViewById(R.id.subheader_text);
          viewHolder.icon = convertView.findViewById(R.id.subheader_icon);
          viewHolder.icon.setOnClickListener(
              v -> activity.startActivity(new Intent(activity, SynchronizationPreferences.class)));
          break;
      }
      viewHolder.view = convertView;
      convertView.setTag(viewHolder);
    }
    return convertView;
  }

  public Filter getSelected() {
    return selected;
  }

  public void setSelected(Filter selected) {
    this.selected = selected;
    notifyDataSetChanged();
  }

  public ArrayList<FilterListItem> getItems() {
    assertMainThread();
    return newArrayList(items);
  }

  public int indexOf(FilterListItem item, int defaultValue) {
    assertMainThread();
    int index = items.indexOf(item);
    return index == -1 ? defaultValue : index;
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    FilterListItem item = getItem(position);

    convertView = newView(convertView, parent, item.getItemType());
    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
    viewHolder.item = getItem(position);
    switch (item.getItemType()) {
      case ITEM:
        populateItem(viewHolder);
        break;
      case SUBHEADER:
        populateHeader(viewHolder);
        break;
      case SEPARATOR:
        break;
    }

    return convertView;
  }

  @Override
  public int getViewTypeCount() {
    return VIEW_TYPE_COUNT;
  }

  @Override
  public boolean isEnabled(int position) {
    return getItem(position).getItemType() == FilterListItem.Type.ITEM;
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getItemType().ordinal();
  }

  @Override
  public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
    return getView(position, convertView, parent);
  }

  private void populateItem(ViewHolder viewHolder) {
    FilterListItem filter = viewHolder.item;
    if (filter == null) {
      return;
    }

    if (selected != null && selected.equals(filter)) {
      if (navigationDrawer) {
        viewHolder.view.setBackgroundColor(getColor(activity, R.color.drawer_color_selected));
      } else {
        viewHolder.name.setChecked(true);
      }
    } else {
      viewHolder.view.setBackgroundResource(0);
      viewHolder.name.setChecked(false);
    }

    viewHolder.icon.setImageResource(filter.icon);
    viewHolder.icon.setColorFilter(
        filter.tint >= 0
            ? themeCache.getThemeColor(filter.tint).getPrimaryColor()
            : getColor(activity, R.color.text_primary));

    viewHolder.name.setText(filter.listingTitle);

    Integer count = counts.get(filter);
    if (count == null || count == 0) {
      viewHolder.size.setVisibility(View.GONE);
    } else {
      viewHolder.size.setText(locale.formatNumber(count));
      viewHolder.size.setVisibility(View.VISIBLE);
    }
  }

  private void populateHeader(ViewHolder viewHolder) {
    NavigationDrawerSubheader filter = (NavigationDrawerSubheader) viewHolder.item;
    if (filter == null) {
      return;
    }

    viewHolder.name.setText(filter.listingTitle);
    viewHolder.icon.setVisibility(filter.error ? View.VISIBLE : View.GONE);
  }

  static class ViewHolder {
    FilterListItem item;
    CheckedTextView name;
    ImageView icon;
    TextView size;
    View view;
  }
}
