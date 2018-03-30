package org.tasks.gtasks;

import android.accounts.Account;
import android.app.Activity;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.preferences.Preferences;

public class GtaskSyncAdapterHelper {

  private static final String AUTHORITY = "org.tasks";

  private final GoogleAccountManager accountManager;
  private final Preferences preferences;
  private final PlayServices playServices;
  private final Tracker tracker;

  @Inject
  public GtaskSyncAdapterHelper(
      GoogleAccountManager accountManager,
      Preferences preferences,
      PlayServices playServices,
      Tracker tracker) {
    this.accountManager = accountManager;
    this.preferences = preferences;
    this.playServices = playServices;
    this.tracker = tracker;
  }

  public boolean isEnabled() {
    return preferences.getBoolean(R.string.sync_gtasks, false)
        && playServices.isPlayServicesAvailable()
        && getAccount() != null;
  }

  private Account getAccount() {
    return accountManager.getSelectedAccount();
  }

  public void checkPlayServices(Activity activity) {
    if (preferences.getBoolean(R.string.sync_gtasks, false)
        && !playServices.refreshAndCheck()
        && !preferences.getBoolean(R.string.warned_play_services, false)) {
      preferences.setBoolean(R.string.warned_play_services, true);
      playServices.resolve(activity);
      tracker.reportEvent(Tracking.Events.PLAY_SERVICES_WARNING, playServices.getStatus());
    }
  }
}
