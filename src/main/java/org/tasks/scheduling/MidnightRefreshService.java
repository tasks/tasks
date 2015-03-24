package org.tasks.scheduling;

import org.tasks.Broadcaster;

import javax.inject.Inject;

public class MidnightRefreshService extends MidnightIntentService {

    @Inject Broadcaster broadcaster;

    public MidnightRefreshService() {
        super(MidnightRefreshService.class.getSimpleName());
    }

    @Override
    void run() {
        broadcaster.refresh();
    }

    @Override
    String getLastRunPreference() {
        return "midnightRefreshDate";
    }
}
