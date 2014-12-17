package org.tasks.scheduling;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.sync.RecordSyncStatusCallback;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class GtasksBackgroundService extends RecurringIntervalIntentService {

    private static final Logger log = LoggerFactory.getLogger(GtasksBackgroundService.class);

    @Inject Preferences preferences;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksSyncV2Provider gtasksSyncV2Provider;
    @Inject Broadcaster broadcaster;

    public GtasksBackgroundService() {
        super(GtasksBackgroundService.class.getSimpleName());
    }

    @Override
    void run() {
        if (gtasksPreferenceService.isOngoing()) {
            log.debug("aborting: sync ongoing");
            return;
        }
        if(gtasksPreferenceService.isLoggedIn() && gtasksSyncV2Provider.isActive()) {
            gtasksSyncV2Provider.synchronizeActiveTasks(new RecordSyncStatusCallback(gtasksPreferenceService, broadcaster));
        }
    }

    @Override
    long intervalMillis() {
        try {
            return SECONDS.toMillis(preferences.getIntegerFromString(R.string.gtasks_GPr_interval_key, 0));
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            preferences.setString(R.string.gtasks_GPr_interval_key, "0");
            return 0;
        }
    }

    @Override
    String getLastRunPreference() {
        return "gtasks_last_sync";
    }
}
