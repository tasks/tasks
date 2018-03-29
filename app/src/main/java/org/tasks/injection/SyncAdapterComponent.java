package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.gtasks.GoogleTaskSyncAdapter;

@Subcomponent(modules = SyncAdapterModule.class)
public interface SyncAdapterComponent {

  void inject(GoogleTaskSyncAdapter googleTaskSyncAdapter);
}
