package org.tasks.gtasks;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.AccountManager;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

public class GtaskSyncAdapterHelper {

    private static final String AUTHORITY = "org.tasks";

    private final AccountManager accountManager;
    private final Preferences preferences;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final PlayServicesAvailability playServicesAvailability;
    private final Tracker tracker;

    @Inject
    public GtaskSyncAdapterHelper(AccountManager accountManager, Preferences preferences,
                                  GtasksPreferenceService gtasksPreferenceService,
                                  PlayServicesAvailability playServicesAvailability, Tracker tracker) {
        this.accountManager = accountManager;
        this.preferences = preferences;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.playServicesAvailability = playServicesAvailability;
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
                playServicesAvailability.isPlayServicesAvailable() &&
                getAccount() != null;
    }

    public void enableSynchronization(boolean enabled) {
        Account account = getAccount();
        if (account != null) {
            Timber.d("enableSynchronization=%s", enabled);
            ContentResolver.setSyncAutomatically(account, AUTHORITY, enabled);
            if (enabled) {
                ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, TimeUnit.HOURS.toSeconds(1));
            } else {
                ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY);
            }
        }
    }

    public boolean isSyncEnabled() {
        return isEnabled() && ContentResolver.getSyncAutomatically(getAccount(), AUTHORITY);
    }

    private Account getAccount() {
        return accountManager.getAccount(gtasksPreferenceService.getUserName());
    }

    public void checkPlayServices(TaskListFragment taskListFragment) {
        if (taskListFragment != null &&
                preferences.getBoolean(R.string.sync_gtasks, false) &&
                !playServicesAvailability.refreshAndCheck() &&
                !preferences.getBoolean(R.string.warned_play_services, false)) {
            preferences.setBoolean(R.string.warned_play_services, true);
            playServicesAvailability.resolve(taskListFragment.getActivity());
            tracker.reportEvent(Tracking.Events.PLAY_SERVICES_WARNING, playServicesAvailability.getStatus());
        }
    }
}
