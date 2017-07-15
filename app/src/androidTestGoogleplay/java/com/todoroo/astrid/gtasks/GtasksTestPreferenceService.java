/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import org.tasks.preferences.Preferences;

public class GtasksTestPreferenceService extends GtasksPreferenceService {

    public GtasksTestPreferenceService(Preferences preferences) {
        super(preferences);
    }

    @Override
    public long getLastSyncDate() {
        return 0L;
    }
}
