/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import org.tasks.preferences.Preferences;

public class GtasksTestPreferenceService extends GtasksPreferenceService {

    private long syncDate = 0;

    public GtasksTestPreferenceService(Preferences preferences) {
        super(preferences);
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    public void setSyncDate(long date) {
        syncDate = date;
    }

    @Override
    public long getLastSyncDate() {
        return syncDate;
    }
}
