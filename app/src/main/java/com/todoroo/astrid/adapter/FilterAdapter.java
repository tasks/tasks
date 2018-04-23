/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;
import static org.tasks.caldav.CaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CustomFilterActivity;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.caldav.CaldavCalendarSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.filters.NavigationDrawerAction;
import org.tasks.filters.NavigationDrawerSeparator;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.locale.Locale;
import org.tasks.preferences.BasicPreferences;
import org.tasks.sync.SynchronizationPreferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.ui.NavigationDrawerFragment;

public class FilterAdapter extends ArrayAdapter<FilterListItem> {

  public static final int REQUEST_SETTINGS = 10123;
  public static final int REQUEST_PURCHASE = 10124;

  // --- instance variables
  private static final int VIEW_TYPE_COUNT = FilterListItem.Type.values().length;
  private final FilterProvider filterProvider;
  private final FilterCounter filterCounter;
  private final Activity activity;
  private final Theme theme;
  private final Locale locale;
  private final Inventory inventory;
  private final FilterListUpdateReceiver filterListUpdateReceiver = new FilterListUpdateReceiver();
  private final List<FilterListItem> items = new ArrayList<>();
  private final LayoutInflater inflater;
  private final ThemeCache themeCache;
  private boolean navigationDrawer;
  private Filter selected;

  @Inject
  public FilterAdapter(
      FilterProvider filterProvider,
      FilterCounter filterCounter,
      Activity activity,
      Theme theme,
      ThemeCache themeCache,
      Locale locale,
      Inventory inventory) {
    super(activity, 0);
    this.filterProvider = filterProvider;
    this.filterCounter = filterCounter;
    this.activity = activity;
    this.theme = theme;
    this.locale = locale;
    this.inventory = inventory;
    this.inflater = theme.getLayoutInflater(activity);
    this.themeCache = themeCache;
  }

  public void setNavigationDrawer() {
    navigationDrawer = true;
  }

  public FilterListUpdateReceiver getFilterListUpdateReceiver() {
    return filterListUpdateReceiver;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public void add(FilterListItem item) {
    super.add(item);

    items.add(item);

    if (navigationDrawer && item instanceof Filter) {
      filterCounter.registerFilter((Filter) item);
    }
  }

  @Override
  public void notifyDataSetChanged() {
    activity.runOnUiThread(FilterAdapter.super::notifyDataSetChanged);
  }

  public void refreshFilterCount() {
    filterCounter.refreshFilterCounts(this::notifyDataSetChanged);
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
            Drawable original = ContextCompat.getDrawable(activity, R.drawable.ic_check_black_24dp);
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
  }

  public int indexOf(FilterListItem item, int defaultValue) {
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

  private void addSubMenu(
      final int titleResource, boolean error, List<Filter> filters, boolean hideIfEmpty) {
    addSubMenu(activity.getResources().getString(titleResource), error, filters, hideIfEmpty);
  }

  /* ======================================================================
   * ============================================================= receiver
   * ====================================================================== */

  private void addSubMenu(String title, boolean error, List<Filter> filters, boolean hideIfEmpty) {
    if (hideIfEmpty && filters.isEmpty()) {
      return;
    }

    add(new NavigationDrawerSubheader(title, error));

    for (FilterListItem filterListItem : filters) {
      add(filterListItem);
    }
  }

  @Override
  public void clear() {
    super.clear();
    items.clear();
  }

  public void populateRemoteListPicker() {
    clear();

    Filter item = new Filter(activity.getString(R.string.dont_sync), null);
    item.icon = R.drawable.ic_cloud_off_black_24dp;
    add(item);

    for (Pair<GoogleTaskAccount, List<Filter>> filters : filterProvider.getGoogleTaskFilters()) {
      GoogleTaskAccount account = filters.first;
      addSubMenu(account.getAccount(), !isNullOrEmpty(account.getError()), filters.second, true);
    }

    for (Pair<CaldavAccount, List<Filter>> filters : filterProvider.getCaldavFilters()) {
      CaldavAccount account = filters.first;
      addSubMenu(account.getName(), !isNullOrEmpty(account.getError()), filters.second, true);
    }

    notifyDataSetChanged();
  }

  public void populateList() {
    clear();

    add(filterProvider.getMyTasksFilter());

    addSubMenu(R.string.filters, false, filterProvider.getFilters(), false);

    if (navigationDrawer) {
      add(
          new NavigationDrawerAction(
              activity.getResources().getString(R.string.FLA_new_filter),
              R.drawable.ic_add_24dp,
              new Intent(activity, CustomFilterActivity.class),
              NavigationDrawerFragment.ACTIVITY_REQUEST_NEW_FILTER));
    }

    addSubMenu(R.string.tags, false, filterProvider.getTags(), false);

    if (navigationDrawer) {
      add(
          new NavigationDrawerAction(
              activity.getResources().getString(R.string.new_tag),
              R.drawable.ic_add_24dp,
              new Intent(activity, TagSettingsActivity.class),
              NavigationDrawerFragment.REQUEST_NEW_LIST));
    }

    for (Pair<GoogleTaskAccount, List<Filter>> filters : filterProvider.getGoogleTaskFilters()) {
      GoogleTaskAccount account = filters.first;
      addSubMenu(
          account.getAccount(),
          !isNullOrEmpty(account.getError()),
          filters.second,
          !navigationDrawer);

      if (navigationDrawer) {
        add(
            new NavigationDrawerAction(
                activity.getResources().getString(R.string.new_list),
                R.drawable.ic_add_24dp,
                new Intent(activity, GoogleTaskListSettingsActivity.class)
                    .putExtra(GoogleTaskListSettingsActivity.EXTRA_ACCOUNT, account),
                NavigationDrawerFragment.REQUEST_NEW_GTASK_LIST));
      }
    }

    for (Pair<CaldavAccount, List<Filter>> filters : filterProvider.getCaldavFilters()) {
      CaldavAccount account = filters.first;
      addSubMenu(
          account.getName(), !isNullOrEmpty(account.getError()), filters.second, !navigationDrawer);

      if (navigationDrawer) {
        add(
            new NavigationDrawerAction(
                activity.getString(R.string.new_list),
                R.drawable.ic_add_24dp,
                new Intent(activity, CaldavCalendarSettingsActivity.class)
                    .putExtra(EXTRA_CALDAV_ACCOUNT, account),
                NavigationDrawerFragment.REQUEST_NEW_CALDAV_COLLECTION));
      }
    }

    if (navigationDrawer) {
      add(new NavigationDrawerSeparator());

      //noinspection ConstantConditions
      if (BuildConfig.FLAVOR.equals("generic")) {
        add(
            new NavigationDrawerAction(
                activity.getResources().getString(R.string.TLA_menu_donate),
                R.drawable.ic_attach_money_black_24dp,
                new Intent(Intent.ACTION_VIEW, Uri.parse("http://tasks.org/donate")),
                REQUEST_PURCHASE));
      } else if (!inventory.hasPro()) {
        add(
            new NavigationDrawerAction(
                activity.getResources().getString(R.string.upgrade_to_pro),
                R.drawable.ic_attach_money_black_24dp,
                new Intent(activity, PurchaseActivity.class),
                REQUEST_PURCHASE));
      }

      add(
          new NavigationDrawerAction(
              activity.getResources().getString(R.string.TLA_menu_settings),
              R.drawable.ic_settings_24dp,
              new Intent(activity, BasicPreferences.class),
              REQUEST_SETTINGS));
    }

    notifyDataSetChanged();

    filterCounter.refreshFilterCounts(this::notifyDataSetChanged);
  }

  private void populateItem(ViewHolder viewHolder) {
    FilterListItem filter = viewHolder.item;
    if (filter == null) {
      return;
    }

    if (selected != null && selected.equals(filter)) {
      viewHolder.view.setBackgroundColor(getColor(activity, R.color.drawer_color_selected));
    } else {
      viewHolder.view.setBackgroundResource(0);
    }

    viewHolder.icon.setImageResource(filter.icon);
    viewHolder.icon.setColorFilter(
        filter.tint >= 0
            ? themeCache.getThemeColor(filter.tint).getPrimaryColor()
            : getColor(activity, R.color.text_primary));

    String title = filter.listingTitle;
    if (!title.equals(viewHolder.name.getText())) {
      viewHolder.name.setText(title);
    }

    int countInt = 0;
    if (filterCounter.containsKey(filter)) {
      countInt = filterCounter.get(filter);
      viewHolder.size.setText(locale.formatNumber(countInt));
    }
    viewHolder.size.setVisibility(countInt > 0 ? View.VISIBLE : View.GONE);
  }

  private void populateHeader(ViewHolder viewHolder) {
    NavigationDrawerSubheader filter = (NavigationDrawerSubheader) viewHolder.item;
    if (filter == null) {
      return;
    }

    viewHolder.name.setText(filter.listingTitle);
    viewHolder.icon.setVisibility(filter.error ? View.VISIBLE : View.GONE);
  }

  /* ======================================================================
   * ================================================================ views
   * ====================================================================== */

  static class ViewHolder {

    FilterListItem item;
    CheckedTextView name;
    ImageView icon;
    TextView size;
    View view;
  }

  public class FilterListUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      notifyDataSetChanged();
    }
  }
}
