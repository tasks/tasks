/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
      convertView = inflater.inflate(viewType.layout, parent, false);
      FilterViewHolder viewHolder;
      switch (viewType) {
        case ITEM:
          viewHolder = new FilterViewHolder(
              convertView, theme.getThemeAccent(), themeCache, navigationDrawer, locale, activity);
          break;
        case SEPARATOR:
          viewHolder = new FilterViewHolder();
          break;
        case SUBHEADER:
          viewHolder = new FilterViewHolder(convertView, activity);
          break;
        default:
          throw new RuntimeException();
      }
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
    FilterViewHolder viewHolder = (FilterViewHolder) convertView.getTag();
    switch (item.getItemType()) {
      case ITEM:
        viewHolder.bind(item, item.equals(selected), counts.get(item));
        break;
      case SUBHEADER:
        viewHolder.bind((NavigationDrawerSubheader) item);
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
}
