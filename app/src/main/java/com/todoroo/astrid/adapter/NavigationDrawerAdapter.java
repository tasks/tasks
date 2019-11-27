/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.adapter;

import static com.google.common.base.Objects.equal;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.astrid.api.FilterListItem.Type.ITEM;
import static com.todoroo.astrid.api.FilterListItem.Type.SUBHEADER;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import com.todoroo.astrid.adapter.FilterViewHolder.OnClick;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterListItem.Type;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.billing.Inventory;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeCache;

public class NavigationDrawerAdapter extends ListAdapter<FilterListItem, FilterViewHolder> {

  private static final String TOKEN_SELECTED = "token_selected";
  private final Activity activity;
  private final ThemeAccent accent;
  private final Locale locale;
  private final Inventory inventory;
  private final LayoutInflater inflater;
  private final ThemeCache themeCache;
  private OnClick onClick;
  private Filter selected = null;
  private Map<Filter, Integer> counts = new HashMap<>();

  @Inject
  public NavigationDrawerAdapter(
      Activity activity, Theme theme, ThemeCache themeCache, Locale locale, Inventory inventory) {
    super(new DiffCallback());
    this.activity = activity;
    this.accent = theme.getThemeAccent();
    this.locale = locale;
    this.inventory = inventory;
    this.inflater = theme.getLayoutInflater(activity);
    this.themeCache = themeCache;
  }

  public void setOnClick(OnClick onClick) {
    this.onClick = onClick;
  }

  public void save(Bundle outState) {
    outState.putParcelable(TOKEN_SELECTED, selected);
  }

  public void restore(Bundle savedInstanceState) {
    selected = savedInstanceState.getParcelable(TOKEN_SELECTED);
  }

  public void setCounts(Map<Filter, Integer> counts) {
    assertMainThread();
    this.counts = counts;
    notifyDataSetChanged();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  public Filter getSelected() {
    return selected;
  }

  public void setSelected(Filter selected) {
    this.selected = selected;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    Type type = Type.values()[viewType];
    View view = inflater.inflate(type.layout, parent, false);
    if (type == ITEM) {
      return new FilterViewHolder(
          view, accent, themeCache, true, locale, activity, inventory, this::onClickFilter);
    } else if (type == SUBHEADER) {
      return new FilterViewHolder(view, activity);
    } else {
      return new FilterViewHolder(view);
    }
  }

  private void onClickFilter(@Nullable FilterListItem filter) {
    onClick.onClick(equal(filter, selected) ? null : filter);
  }

  @Override
  public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
    FilterListItem item = getItem(position);
    Type type = item.getItemType();
    if (type == ITEM) {
      holder.bind(item, item.equals(selected), item.count >= 0 ? item.count : counts.get(item));
    } else if (type == SUBHEADER) {
      holder.bind((NavigationDrawerSubheader) item);
    }
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getItemType().ordinal();
  }

  @Override
  public FilterListItem getItem(int position) {
    return super.getItem(position);
  }

  private static class DiffCallback extends ItemCallback<FilterListItem> {
    @Override
    public boolean areItemsTheSame(
        @NonNull FilterListItem oldItem, @NonNull FilterListItem newItem) {
      return oldItem.areItemsTheSame(newItem);
    }

    @Override
    public boolean areContentsTheSame(
        @NonNull FilterListItem oldItem, @NonNull FilterListItem newItem) {
      return oldItem.areContentsTheSame(newItem);
    }
  }
}
