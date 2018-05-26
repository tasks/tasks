package org.tasks.dashclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.billing.BillingClient;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.DefaultFilterProvider;

public class DashClockSettings extends InjectingPreferenceActivity {

  private static final int REQUEST_SELECT_FILTER = 1005;
  private static final int REQUEST_SUBSCRIPTION = 1006;

  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject BillingClient billingClient;
  @Inject Inventory inventory;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_dashclock);

    findPreference(getString(R.string.p_dashclock_filter))
        .setOnPreferenceClickListener(
            preference -> {
              Intent intent = new Intent(DashClockSettings.this, FilterSelectionActivity.class);
              intent.putExtra(
                  FilterSelectionActivity.EXTRA_FILTER, defaultFilterProvider.getDashclockFilter());
              intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
              startActivityForResult(intent, REQUEST_SELECT_FILTER);
              return false;
            });

    refreshPreferences();

    if (!inventory.purchasedDashclock()) {
      startActivityForResult(new Intent(this, PurchaseActivity.class), REQUEST_SUBSCRIPTION);
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SELECT_FILTER) {
      if (resultCode == Activity.RESULT_OK) {
        Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
        defaultFilterProvider.setDashclockFilter(filter);
        refreshPreferences();
        localBroadcastManager.broadcastRefresh();
      }
    } else if (requestCode == REQUEST_SUBSCRIPTION) {
      if (!inventory.purchasedDashclock()) {
        finish();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void refreshPreferences() {
    Filter filter = defaultFilterProvider.getFilterFromPreference(R.string.p_dashclock_filter);
    findPreference(getString(R.string.p_dashclock_filter)).setSummary(filter.listingTitle);
  }
}
