package org.tasks.injection;

import org.tasks.caldav.CalDAVSyncAdapter;
import org.tasks.gtasks.GoogleTaskSyncAdapter;

import dagger.Subcomponent;

@Subcomponent(modules = SyncAdapterModule.class)
public interface SyncAdapterComponent {
    void inject(GoogleTaskSyncAdapter googleTaskSyncAdapter);

    void inject(CalDAVSyncAdapter calDAVSyncAdapter);
}
