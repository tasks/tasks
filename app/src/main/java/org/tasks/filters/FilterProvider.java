package org.tasks.filters;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static com.todoroo.astrid.adapter.FilterAdapter.REQUEST_PURCHASE;
import static com.todoroo.astrid.adapter.FilterAdapter.REQUEST_SETTINGS;
import static org.tasks.caldav.CaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.util.Pair;
import com.google.common.collect.ImmutableList;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;
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
import org.tasks.caldav.CaldavFilterExposer;
import org.tasks.data.CaldavAccount;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.BasicPreferences;
import org.tasks.ui.NavigationDrawerFragment;

public class FilterProvider {

  private final Context context;
  private final Inventory inventory;
  private final BuiltInFilterExposer builtInFilterExposer;
  private final TimerFilterExposer timerFilterExposer;
  private final CustomFilterExposer customFilterExposer;
  private final TagFilterExposer tagFilterExposer;
  private final GtasksFilterExposer gtasksFilterExposer;
  private final CaldavFilterExposer caldavFilterExposer;

  @Inject
  public FilterProvider(
      @ForApplication Context context,
      Inventory inventory,
      BuiltInFilterExposer builtInFilterExposer,
      TimerFilterExposer timerFilterExposer,
      CustomFilterExposer customFilterExposer,
      TagFilterExposer tagFilterExposer,
      GtasksFilterExposer gtasksFilterExposer,
      CaldavFilterExposer caldavFilterExposer) {
    this.context = context;
    this.inventory = inventory;
    this.builtInFilterExposer = builtInFilterExposer;
    this.timerFilterExposer = timerFilterExposer;
    this.customFilterExposer = customFilterExposer;
    this.tagFilterExposer = tagFilterExposer;
    this.gtasksFilterExposer = gtasksFilterExposer;
    this.caldavFilterExposer = caldavFilterExposer;
  }

  public List<FilterListItem> getRemoteListPickerItems() {
    assertNotMainThread();

    List<FilterListItem> items = new ArrayList<>();

    Filter item = new Filter(context.getString(R.string.dont_sync), null);
    item.icon = R.drawable.ic_outline_cloud_off_24px;
    items.add(item);

    for (Pair<GoogleTaskAccount, List<Filter>> filters : getGoogleTaskFilters()) {
      GoogleTaskAccount account = filters.first;
      items.addAll(
          getSubmenu(
              account.getAccount(), !isNullOrEmpty(account.getError()), filters.second, true));
    }

    for (Pair<CaldavAccount, List<Filter>> filters : getCaldavFilters()) {
      CaldavAccount account = filters.first;
      items.addAll(
          getSubmenu(account.getName(), !isNullOrEmpty(account.getError()), filters.second, true));
    }

    return items;
  }

  public List<FilterListItem> getItems(boolean navigationDrawer) {
    assertNotMainThread();

    List<FilterListItem> items = new ArrayList<>();

    items.add(builtInFilterExposer.getMyTasksFilter());

    items.addAll(getSubmenu(R.string.filters, getFilters()));

    if (navigationDrawer) {
      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.FLA_new_filter),
              R.drawable.ic_outline_add_24px,
              new Intent(context, CustomFilterActivity.class),
              NavigationDrawerFragment.ACTIVITY_REQUEST_NEW_FILTER));
    }

    items.addAll(getSubmenu(R.string.tags, tagFilterExposer.getFilters()));

    if (navigationDrawer) {
      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.new_tag),
              R.drawable.ic_outline_add_24px,
              new Intent(context, TagSettingsActivity.class),
              NavigationDrawerFragment.REQUEST_NEW_LIST));
    }

    for (Pair<GoogleTaskAccount, List<Filter>> filters : getGoogleTaskFilters()) {
      GoogleTaskAccount account = filters.first;
      items.addAll(
          getSubmenu(
              account.getAccount(),
              !isNullOrEmpty(account.getError()),
              filters.second,
              !navigationDrawer));

      if (navigationDrawer) {
        items.add(
            new NavigationDrawerAction(
                context.getString(R.string.new_list),
                R.drawable.ic_outline_add_24px,
                new Intent(context, GoogleTaskListSettingsActivity.class)
                    .putExtra(GoogleTaskListSettingsActivity.EXTRA_ACCOUNT, account),
                NavigationDrawerFragment.REQUEST_NEW_GTASK_LIST));
      }
    }

    for (Pair<CaldavAccount, List<Filter>> filters : getCaldavFilters()) {
      CaldavAccount account = filters.first;
      items.addAll(
          getSubmenu(
              account.getName(),
              !isNullOrEmpty(account.getError()),
              filters.second,
              !navigationDrawer));

      if (navigationDrawer) {
        items.add(
            new NavigationDrawerAction(
                context.getString(R.string.new_list),
                R.drawable.ic_outline_add_24px,
                new Intent(context, CaldavCalendarSettingsActivity.class)
                    .putExtra(EXTRA_CALDAV_ACCOUNT, account),
                NavigationDrawerFragment.REQUEST_NEW_CALDAV_COLLECTION));
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
                new Intent(Intent.ACTION_VIEW, Uri.parse("http://tasks.org/donate")),
                REQUEST_PURCHASE));
      } else if (!inventory.hasPro()) {
        items.add(
            new NavigationDrawerAction(
                context.getString(R.string.upgrade_to_pro),
                R.drawable.ic_outline_attach_money_24px,
                new Intent(context, PurchaseActivity.class),
                REQUEST_PURCHASE));
      }

      items.add(
          new NavigationDrawerAction(
              context.getString(R.string.TLA_menu_settings),
              R.drawable.ic_outline_settings_24px,
              new Intent(context, BasicPreferences.class),
              REQUEST_SETTINGS));
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

  private List<Pair<GoogleTaskAccount, List<Filter>>> getGoogleTaskFilters() {
    return gtasksFilterExposer.getFilters();
  }

  private List<Pair<CaldavAccount, List<Filter>>> getCaldavFilters() {
    return caldavFilterExposer.getFilters();
  }

  private List<FilterListItem> getSubmenu(int title, List<Filter> filters) {
    return getSubmenu(context.getString(title), false, filters, false);
  }

  private List<FilterListItem> getSubmenu(
      String title, boolean error, List<Filter> filters, boolean hideIfEmpty) {
    return hideIfEmpty && filters.isEmpty()
        ? ImmutableList.of()
        : newArrayList(
            concat(ImmutableList.of(new NavigationDrawerSubheader(title, error)), filters));
  }
}
