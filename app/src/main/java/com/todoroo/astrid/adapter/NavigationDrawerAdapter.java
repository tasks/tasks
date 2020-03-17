/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.adapter;

import static com.google.common.base.Objects.equal;
import static com.todoroo.astrid.api.FilterListItem.Type.ITEM;
import static com.todoroo.astrid.api.FilterListItem.Type.SUBHEADER;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.todoroo.astrid.adapter.FilterViewHolder.OnClick;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterListItem.Type;
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
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeAccent;

public class NavigationDrawerAdapter extends RecyclerView.Adapter<ViewHolder> {

  private static final String TOKEN_SELECTED = "token_selected";
  private final Activity activity;
  private final ThemeAccent accent;
  private final Locale locale;
  private final Inventory inventory;
  private final ColorProvider colorProvider;
  private final Preferences preferences;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final LayoutInflater inflater;
  private OnClick onClick;
  private Filter selected = null;

  private AsyncListDiffer<FilterListItem> differ;

  @Inject
  public NavigationDrawerAdapter(
      Activity activity,
      Theme theme,
      Locale locale,
      Inventory inventory,
      ColorProvider colorProvider,
      Preferences preferences,
      GoogleTaskDao googleTaskDao,
      CaldavDao caldavDao,
      LocalBroadcastManager localBroadcastManager) {
    this.activity = activity;
    this.accent = theme.getThemeAccent();
    this.locale = locale;
    this.inventory = inventory;
    this.colorProvider = colorProvider;
    this.preferences = preferences;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
    this.localBroadcastManager = localBroadcastManager;
    this.inflater = theme.getLayoutInflater(activity);

    differ = new AsyncListDiffer<>(this, new DiffCallback());
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

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemCount() {
    return differ.getCurrentList().size();
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
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    Type type = Type.values()[viewType];
    View view = inflater.inflate(type.layout, parent, false);
    if (type == ITEM) {
      return new FilterViewHolder(
          view, accent, true, locale, activity, inventory, colorProvider, this::onClickFilter);
    } else if (type == SUBHEADER) {
      return new SubheaderViewHolder(
          view, activity, preferences, googleTaskDao, caldavDao, localBroadcastManager);
    } else {
      return new FilterViewHolder(view);
    }
  }

  private void onClickFilter(@Nullable FilterListItem filter) {
    onClick.onClick(equal(filter, selected) ? null : filter);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    FilterListItem item = getItem(position);
    Type type = item.getItemType();
    if (type == ITEM) {
      ((FilterViewHolder) holder).bind(item, item.equals(selected), Math.max(item.count, 0));
    } else if (type == SUBHEADER) {
      ((SubheaderViewHolder) holder).bind((NavigationDrawerSubheader) item);
    }
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getItemType().ordinal();
  }

  private FilterListItem getItem(int position) {
    return differ.getCurrentList().get(position);
  }

  public void submitList(List<FilterListItem> filterListItems) {
    differ.submitList(filterListItems);
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
