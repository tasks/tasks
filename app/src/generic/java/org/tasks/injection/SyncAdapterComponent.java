package org.tasks.injection;

import org.tasks.caldav.CalDAVSyncAdapter;

import dagger.Subcomponent;

@Subcomponent(modules = SyncAdapterModule.class)
public interface SyncAdapterComponent {
    void inject(CalDAVSyncAdapter calDAVSyncAdapter);
}
