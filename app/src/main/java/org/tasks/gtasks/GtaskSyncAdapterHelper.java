package org.tasks.gtasks;

import android.app.Activity;
import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.preferences.Preferences;
import org.tasks.data.GoogleTaskListDao;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

public class GtaskSyncAdapterHelper {

    private static final String AUTHORITY = "org.tasks";
    public static final String SYNC_FULL = "full";

    private final GoogleAccountManager accountManager;
    private final Preferences preferences;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final PlayServices playServices;
    private final GoogleTaskListDao googleTaskListDao;
    private final Tracker tracker;

    @Inject
    public GtaskSyncAdapterHelper(
            GoogleAccountManager accountManager,
            Preferences preferences,
            GtasksPreferenceService gtasksPreferenceService,
            PlayServices playServices,
            GoogleTaskListDao googleTaskListDao,
            Tracker tracker) {
      this.accountManager = accountManager;
      this.preferences = preferences;
      this.gtasksPreferenceService = gtasksPreferenceService;
      this.playServices = playServices;
      this.googleTaskListDao = googleTaskListDao;
      this.tracker = tracker;
  }
    public boolean initiateManualFullSync() {
        if (!hasAccounts()) {
            return false;
        }
        Bundle extras = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        extras.putBoolean(SYNC_FULL, true);
        for(GoogleTaskAccount account: googleTaskListDao.getAccounts()) {
            // TODO ContentResolver.requestSync(account, AUTHORITY, extras);
        }
        return true;
    }

    public boolean isEnabled() {
        return preferences.getBoolean(R.string.sync_gtasks, false) &&
                playServices.isPlayServicesAvailable() &&
                hasAccounts();
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
