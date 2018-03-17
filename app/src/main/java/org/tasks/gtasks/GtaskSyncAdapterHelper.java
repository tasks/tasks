package org.tasks.gtasks;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

public class GtaskSyncAdapterHelper {

    private static final String AUTHORITY = "org.tasks";

    private final GoogleAccountManager accountManager;
    private final Preferences preferences;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final PlayServices playServices;
    private final Tracker tracker;

    @Inject
    public GtaskSyncAdapterHelper(GoogleAccountManager accountManager, Preferences preferences,
                                  GtasksPreferenceService gtasksPreferenceService,
                                  PlayServices playServices, Tracker tracker) {
        this.accountManager = accountManager;
        this.preferences = preferences;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.playServices = playServices;
        this.tracker = tracker;
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public boolean initiateManualSync() {
        Account account = getAccount();
        if (account == null) {
            return false;
        }
        Bundle extras = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, AUTHORITY, extras);
        return true;
    }

    public void requestSynchronization() {
        Account account = getAccount();
        if (account == null) {
            return;
        }
        ContentResolver.requestSync(account, AUTHORITY, new Bundle());
    }

    public boolean isEnabled() {
        return preferences.getBoolean(R.string.sync_gtasks, false) &&
                playServices.isPlayServicesAvailable() &&
                getAccount() != null;
    }

    public void enableBackgroundSynchronization(boolean enabled) {
        Account account = getAccount();
        if (account != null) {
            Timber.d("enableBackgroundSynchronization=%s", enabled);
            ContentResolver.setSyncAutomatically(account, AUTHORITY, enabled);
            if (enabled) {
                ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, TimeUnit.HOURS.toSeconds(1));
            } else {
                ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY);
            }
        }
    }

    public boolean isBackgroundSyncEnabled() {
        return isEnabled() && ContentResolver.getSyncAutomatically(getAccount(), AUTHORITY);
    }

    private Account getAccount() {
        return accountManager.getAccount(gtasksPreferenceService.getUserName());
    }

    public void checkPlayServices(Activity activity) {
        if (preferences.getBoolean(R.string.sync_gtasks, false) &&
                !playServices.refreshAndCheck() &&
                !preferences.getBoolean(R.string.warned_play_services, false)) {
            preferences.setBoolean(R.string.warned_play_services, true);
            playServices.resolve(activity);
            tracker.reportEvent(Tracking.Events.PLAY_SERVICES_WARNING, playServices.getStatus());
        }
    }
}
