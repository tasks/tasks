/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.adapter;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.billing.Inventory;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ColorProvider;

public class FilterAdapter extends BaseAdapter {

  private static final String TOKEN_FILTERS = "token_filters";
  private static final String TOKEN_SELECTED = "token_selected";
  private static final int VIEW_TYPE_COUNT = FilterListItem.Type.values().length;
  private final Activity activity;
  private final Locale locale;
  private final Inventory inventory;
  private final ColorProvider colorProvider;
  private final Preferences preferences;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;
  private final LocalBroadcastManager localBroadcastManager;
  private Filter selected = null;
  private List<FilterListItem> items = new ArrayList<>();

  @Inject
  public FilterAdapter(
      Activity activity,
      Locale locale,
      Inventory inventory,
      ColorProvider colorProvider,
      Preferences preferences,
      GoogleTaskDao googleTaskDao,
      CaldavDao caldavDao,
      LocalBroadcastManager localBroadcastManager) {
    this.activity = activity;
    this.locale = locale;
    this.inventory = inventory;
    this.colorProvider = colorProvider;
    this.preferences = preferences;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
    this.localBroadcastManager = localBroadcastManager;
  }

  public void save(Bundle outState) {
    outState.putParcelableArrayList(TOKEN_FILTERS, getItems());
    outState.putParcelable(TOKEN_SELECTED, selected);
  }

  public void restore(Bundle savedInstanceState) {
    items = savedInstanceState.getParcelableArrayList(TOKEN_FILTERS);
    selected = savedInstanceState.getParcelable(TOKEN_SELECTED);
  }

  public void setData(List<FilterListItem> items, @Nullable Filter selected) {
    assertMainThread();
    this.items = items;
    this.selected = selected;
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

  @Override
  public long getItemId(int position) {
    return position;
  }

  /** Create or reuse a view */
  private View newView(View convertView, ViewGroup parent, FilterListItem.Type viewType) {
    if (convertView == null) {
      convertView =
          LayoutInflater.from(parent.getContext()).inflate(viewType.layout, parent, false);
      ViewHolder viewHolder;
      switch (viewType) {
        case ITEM:
          viewHolder =
              new FilterViewHolder(
                  convertView, false, locale, activity, inventory, colorProvider, null);
          break;
        case SEPARATOR:
          viewHolder = new FilterViewHolder(convertView);
          break;
        case SUBHEADER:
          viewHolder =
              new SubheaderViewHolder(
                  convertView,
                  activity,
                  preferences,
                  googleTaskDao,
                  caldavDao,
                  localBroadcastManager);
          break;
        default:
          throw new RuntimeException();
      }
      convertView.setTag(viewHolder);
    }
    return convertView;
  }

  private ArrayList<FilterListItem> getItems() {
    assertMainThread();
    return new ArrayList<>(items);
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    FilterListItem item = getItem(position);
    convertView = newView(convertView, parent, item.getItemType());
    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
    switch (item.getItemType()) {
      case ITEM:
        ((FilterViewHolder) viewHolder).bind(item, item.equals(selected), 0);
        break;
      case SUBHEADER:
        ((SubheaderViewHolder) viewHolder).bind((NavigationDrawerSubheader) item);
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
}
