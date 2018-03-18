package org.tasks;

import org.tasks.caldav.CaldavAccountManager;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class FlavorSetup {
    private final CaldavAccountManager caldavAccountManager;
    private final Preferences preferences;

    @Inject
    public FlavorSetup(CaldavAccountManager caldavAccountManager, Preferences preferences) {
        this.caldavAccountManager = caldavAccountManager;
        this.preferences = preferences;
    }

    public void setup() {
        caldavAccountManager.setBackgroundSynchronization(preferences.getBoolean(R.string.p_background_sync, true));
    }
}
