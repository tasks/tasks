package org.tasks.injection;

import org.tasks.gtasks.GoogleTaskSyncAdapter;

import dagger.Subcomponent;

@Subcomponent(modules = SyncAdapterModule.class)
public interface SyncAdapterComponent {
    void inject(GoogleTaskSyncAdapter googleTaskSyncAdapter);
}
