package org.tasks.dashclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.injection.InjectingApplication;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class DashClockExtension extends com.google.android.apps.dashclock.api.DashClockExtension {

  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject TaskDao taskDao;
  @Inject Preferences preferences;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Inventory inventory;
  private final BroadcastReceiver refreshReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          refresh();
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();

    ((InjectingApplication) getApplication()).getComponent().inject(this);

    localBroadcastManager.registerRefreshReceiver(refreshReceiver);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    localBroadcastManager.unregisterReceiver(refreshReceiver);
  }

  @Override
  protected void onUpdateData(int i) {
    refresh();
  }

  private void refresh() {
    if (inventory.purchasedDashclock()) {
      final String filterPreference = preferences.getStringValue(R.string.p_dashclock_filter);
      Filter filter = defaultFilterProvider.getFilterFromPreference(filterPreference);

      int count = taskDao.count(filter);

      if (count == 0) {
        publish(null);
      } else {
        Intent clickIntent = new Intent(this, MainActivity.class);
        clickIntent.putExtra(MainActivity.LOAD_FILTER, filterPreference);
        ExtensionData extensionData =
            new ExtensionData()
                .visible(true)
                .icon(R.drawable.ic_check_white_24dp)
                .status(Integer.toString(count))
                .expandedTitle(getResources().getQuantityString(R.plurals.task_count, count, count))
                .expandedBody(filter.listingTitle)
                .clickIntent(clickIntent);
        if (count == 1) {
          List<Task> tasks = taskDao.fetchFiltered(filter);
          if (!tasks.isEmpty()) {
            extensionData.expandedTitle(tasks.get(0).getTitle());
          }
        }
        publish(extensionData);
      }
    } else {
      publish(
          new ExtensionData()
              .visible(true)
              .icon(R.drawable.ic_check_white_24dp)
              .status(getString(R.string.upgrade_to_pro))
              .expandedTitle(getString(R.string.upgrade_to_pro))
              .clickIntent(new Intent(this, DashClockSettings.class)));
    }
  }

  private void publish(ExtensionData data) {
    try {
      publishUpdate(data);
    } catch (Exception e) {
      Timber.e(e);
    }
  }
}
