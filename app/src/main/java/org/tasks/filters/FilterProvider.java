package org.tasks.filters;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static java.util.Collections.emptyList;
import static org.tasks.caldav.CaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT;
import static org.tasks.ui.NavigationDrawerFragment.REQUEST_DONATE;
import static org.tasks.ui.NavigationDrawerFragment.REQUEST_PURCHASE;
import static org.tasks.ui.NavigationDrawerFragment.REQUEST_SETTINGS;

import android.content.Context;
import android.content.Intent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.Function;
import org.tasks.R;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.billing.Inventory;
import org.tasks.caldav.CaldavCalendarSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.LocationDao;
import org.tasks.data.TagDataDao;
import org.tasks.etesync.EteSyncCalendarSettingsActivity;
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType;
import org.tasks.injection.ForApplication;
import org.tasks.location.LocationPickerActivity;
import org.tasks.preferences.HelpAndFeedback;
import org.tasks.preferences.MainPreferences;
import org.tasks.preferences.Preferences;
import org.tasks.ui.NavigationDrawerFragment;

public class FilterProvider {

  private final Context context;
  private final Inventory inventory;
  private final BuiltInFilterExposer builtInFilterExposer;
  private final TimerFilterExposer timerFilterExposer;
  private final CustomFilterExposer customFilterExposer;
  private final TagDataDao tagDataDao;
  private final GoogleTaskListDao googleTaskListDao;
  private final CaldavDao caldavDao;
  private final Preferences preferences;
  private final LocationDao locationDao;

  @Inject
  public FilterProvider(
      @ForApplication Context context,
      Inventory inventory,
      BuiltInFilterExposer builtInFilterExposer,
      TimerFilterExposer timerFilterExposer,
      CustomFilterExposer customFilterExposer,
      TagDataDao tagDataDao,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao,
      Preferences preferences,
      LocationDao locationDao) {
    this.context = context;
    this.inventory = inventory;
    this.builtInFilterExposer = builtInFilterExposer;
    this.timerFilterExposer = timerFilterExposer;
    this.customFilterExposer = customFilterExposer;
    this.tagDataDao = tagDataDao;
    this.googleTaskListDao = googleTaskListDao;
    this.caldavDao = caldavDao;
    this.preferences = preferences;
    this.locationDao = locationDao;
  }

  public List<FilterListItem> getRemoteListPickerItems() {
    assertNotMainThread();

    List<FilterListItem> items = new ArrayList<>();

    Filter item = new Filter(context.getString(R.string.dont_sync), null);
    item.icon = R.drawable.ic_outline_cloud_off_24px;
    items.add(item);

    for (Map.Entry<GoogleTaskAccount, List<Filter>> filters : getGoogleTaskFilters()) {
      GoogleTaskAccount account = filters.getKey();
      items.addAll(
          getSubmenu(
              account.getAccount(),
              !isNullOrEmpty(account.getError()),
              filters.getValue(),
              true,
              account.isCollapsed(),
              SubheaderType.GOOGLE_TASKS,
              account.getId()));
    }

    for (Map.Entry<CaldavAccount, List<Filter>> filters : getCaldavFilters()) {
      CaldavAccount account = filters.getKey();
      items.addAll(
          getSubmenu(
              account.getName(),
              !isNullOrEmpty(account.getError()),
              filters.getValue(),
              true,
              account.isCollapsed(),
              SubheaderType.CALDAV,
              account.getId()));
    }

    return items;
  }

  private void addFilters(List<FilterListItem> items, boolean navigationDrawer) {
    if (!preferences.getBoolean(R.string.p_filters_enabled, true)) {
      return;
    }

    items.addAll(getSubmenu(R.string.filters, R.string.p_collapse_filters, this::getFilters));

    if (navigationDrawer && !preferences.getBoolean(R.string.p_collapse_filters, false)) {
      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.add_filter),
              R.drawable.ic_outline_add_24px,
              NavigationDrawerFragment.REQUEST_NEW_FILTER));
    }
  }

  private void addTags(List<FilterListItem> items, boolean navigationDrawer) {
    if (!preferences.getBoolean(R.string.p_tags_enabled, true)) {
      return;
    }

    boolean collapsed = preferences.getBoolean(R.string.p_collapse_tags, false);
    Iterable<TagFilters> filters = collapsed ? emptyList() : tagDataDao.getTagFilters(now());
    if (preferences.getBoolean(R.string.p_tags_hide_unused, false)) {
      filters = filter(filters, f -> f.count > 0);
    }
    List<Filter> tags = newArrayList(Iterables.transform(filters, TagFilters::toTagFilter));
    Collections.sort(tags, new AlphanumComparator<>(AlphanumComparator.FILTER));

    items.addAll(
        getSubmenu(
            context.getString(R.string.tags),
            false,
            tags,
            false,
            collapsed,
            SubheaderType.PREFERENCE,
            R.string.p_collapse_tags));

    if (navigationDrawer && !collapsed) {
      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.new_tag),
              R.drawable.ic_outline_add_24px,
              new Intent(context, TagSettingsActivity.class),
              NavigationDrawerFragment.REQUEST_NEW_LIST));
    }
  }

  private void addPlaces(List<FilterListItem> items, boolean navigationDrawer) {
    if (!preferences.getBoolean(R.string.p_places_enabled, true)) {
      return;
    }

    boolean collapsed = preferences.getBoolean(R.string.p_collapse_locations, false);
    Iterable<LocationFilters> filters = collapsed ? emptyList() : locationDao.getPlaceFilters(now());
    if (preferences.getBoolean(R.string.p_places_hide_unused, false)) {
      filters = filter(filters, f -> f.count > 0);
    }

    items.addAll(
        getSubmenu(
            context.getString(R.string.places),
            false,
            newArrayList(Iterables.transform(filters, LocationFilters::toLocationFilter)),
            false,
            collapsed,
            SubheaderType.PREFERENCE,
            R.string.p_collapse_locations));

    if (navigationDrawer && !collapsed) {
      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.add_place),
              R.drawable.ic_outline_add_24px,
              new Intent(context, LocationPickerActivity.class),
              NavigationDrawerFragment.REQUEST_NEW_PLACE));
    }
  }

  public List<FilterListItem> getItems(boolean navigationDrawer) {
    assertNotMainThread();

    List<FilterListItem> items = new ArrayList<>();

    items.add(builtInFilterExposer.getMyTasksFilter());

    addFilters(items, navigationDrawer);

    addTags(items, navigationDrawer);

    addPlaces(items, navigationDrawer);

    for (Map.Entry<GoogleTaskAccount, List<Filter>> filters : getGoogleTaskFilters()) {
      GoogleTaskAccount account = filters.getKey();
      items.addAll(
          getSubmenu(
              account.getAccount(),
              !isNullOrEmpty(account.getError()),
              filters.getValue(),
              !navigationDrawer,
              account.isCollapsed(),
              SubheaderType.GOOGLE_TASKS,
              account.getId()));

      if (navigationDrawer && !account.isCollapsed()) {
        items.add(
            new NavigationDrawerAction(
                context.getString(R.string.new_list),
                R.drawable.ic_outline_add_24px,
                new Intent(context, GoogleTaskListSettingsActivity.class)
                    .putExtra(GoogleTaskListSettingsActivity.EXTRA_ACCOUNT, account),
                NavigationDrawerFragment.REQUEST_NEW_LIST));
      }
    }

    for (Map.Entry<CaldavAccount, List<Filter>> filters : getCaldavFilters()) {
      CaldavAccount account = filters.getKey();
      items.addAll(
          getSubmenu(
              account.getName(),
              !isNullOrEmpty(account.getError()),
              filters.getValue(),
              !navigationDrawer,
              account.isCollapsed(),
              SubheaderType.CALDAV,
              account.getId()));

      if (navigationDrawer && !account.isCollapsed()) {
        items.add(
            new NavigationDrawerAction(
                context.getString(R.string.new_list),
                R.drawable.ic_outline_add_24px,
                new Intent(
                        context,
                        account.isCaldavAccount()
                            ? CaldavCalendarSettingsActivity.class
                            : EteSyncCalendarSettingsActivity.class)
                    .putExtra(EXTRA_CALDAV_ACCOUNT, account),
                NavigationDrawerFragment.REQUEST_NEW_LIST));
      }
    }

    if (navigationDrawer) {
      items.add(new NavigationDrawerSeparator());

      //noinspection ConstantConditions
      if (BuildConfig.FLAVOR.equals("generic")) {
        items.add(
            new NavigationDrawerAction(
                context.getString(R.string.TLA_menu_donate),
                R.drawable.ic_outline_attach_money_24px,
                REQUEST_DONATE));
      } else if (!inventory.hasPro()) {
        items.add(
            new NavigationDrawerAction(
                context.getString(R.string.name_your_price),
                R.drawable.ic_outline_attach_money_24px,
                REQUEST_PURCHASE));
      }

      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.TLA_menu_settings),
              R.drawable.ic_outline_settings_24px,
              new Intent(context, MainPreferences.class),
              REQUEST_SETTINGS));

      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.help_and_feedback),
              R.drawable.ic_outline_help_outline_24px,
              new Intent(context, HelpAndFeedback.class),
              0));
    }

    return items;
  }

  private List<Filter> getFilters() {
    ArrayList<Filter> filters = new ArrayList<>();
    filters.addAll(builtInFilterExposer.getFilters());
    filters.addAll(timerFilterExposer.getFilters());
    filters.addAll(customFilterExposer.getFilters());
    return filters;
  }

  private Set<Entry<GoogleTaskAccount, List<Filter>>> getGoogleTaskFilters() {
    List<GoogleTaskAccount> accounts = googleTaskListDao.getAccounts();
    LinkedHashMap<GoogleTaskAccount, List<Filter>> filters = new LinkedHashMap<>();
    for (GoogleTaskAccount account : accounts) {
      filters.put(
          account,
          account.isCollapsed()
              ? emptyList()
              : newArrayList(
                  transform(
                      googleTaskListDao.getGoogleTaskFilters(account.getAccount(), now()),
                      GoogleTaskFilters::toGtasksFilter)));
    }
    for (Map.Entry<GoogleTaskAccount, List<Filter>> entry : filters.entrySet()) {
      Collections.sort(entry.getValue(), new AlphanumComparator<>(AlphanumComparator.FILTER));
    }
    return filters.entrySet();
  }

  private Set<Entry<CaldavAccount, List<Filter>>> getCaldavFilters() {
    List<CaldavAccount> accounts = caldavDao.getAccounts();
    LinkedHashMap<CaldavAccount, List<Filter>> filters = new LinkedHashMap<>();
    for (CaldavAccount account : accounts) {
      filters.put(
          account,
          account.isCollapsed()
              ? emptyList()
              : newArrayList(
                  transform(
                      caldavDao.getCaldavFilters(account.getUuid(), now()),
                      CaldavFilters::toCaldavFilter)));
    }
    for (Map.Entry<CaldavAccount, List<Filter>> entry : filters.entrySet()) {
      Collections.sort(entry.getValue(), new AlphanumComparator<>(AlphanumComparator.FILTER));
    }
    return filters.entrySet();
  }

  private List<FilterListItem> getSubmenu(int title, int prefId, Function<List<Filter>> getFilters) {
    boolean collapsed = preferences.getBoolean(prefId, false);
    return newArrayList(
        concat(
            ImmutableList.of(
                new NavigationDrawerSubheader(
                    context.getString(title), false, collapsed, SubheaderType.PREFERENCE, prefId)),
            collapsed ? emptyList() : getFilters.call()));
  }

  private List<FilterListItem> getSubmenu(
      String title,
      boolean error,
      List<Filter> filters,
      boolean hideIfEmpty,
      boolean collapsed,
      SubheaderType type,
      long id) {
    return hideIfEmpty && filters.isEmpty() && !collapsed
        ? ImmutableList.of()
        : newArrayList(
            concat(
                ImmutableList.of(new NavigationDrawerSubheader(title, error, collapsed, type, id)),
                filters));
  }
}
