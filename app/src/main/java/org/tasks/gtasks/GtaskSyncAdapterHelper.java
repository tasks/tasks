package org.tasks.gtasks;

import android.app.Activity;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.preferences.Preferences;

public class GtaskSyncAdapterHelper {

  private final GoogleAccountManager accountManager;
  private final Preferences preferences;
  private final PlayServices playServices;
  private final GoogleTaskListDao googleTaskListDao;
  private final Tracker tracker;

  @Inject
  public GtaskSyncAdapterHelper(
      GoogleAccountManager accountManager,
      Preferences preferences,
      PlayServices playServices,
      GoogleTaskListDao googleTaskListDao,
      Tracker tracker) {
    this.accountManager = accountManager;
    this.preferences = preferences;
    this.playServices = playServices;
    this.googleTaskListDao = googleTaskListDao;
    this.tracker = tracker;
  }

  public boolean isEnabled() {
    return hasAccounts() && playServices.isPlayServicesAvailable();
  }

  private boolean hasAccounts() {
    return !googleTaskListDao.getAccounts().isEmpty();
  }

  public void checkPlayServices(Activity activity) {
    if (hasAccounts()
        && !playServices.refreshAndCheck()
        && !preferences.getBoolean(R.string.warned_play_services, false)) {
      preferences.setBoolean(R.string.warned_play_services, true);
      playServices.resolve(activity);
      tracker.reportEvent(Tracking.Events.PLAY_SERVICES_WARNING, playServices.getStatus());
    }
  }
}
