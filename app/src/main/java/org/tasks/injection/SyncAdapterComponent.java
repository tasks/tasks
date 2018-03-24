package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.caldav.CalDAVSyncAdapter;
import org.tasks.gtasks.GoogleTaskSyncAdapter;

@Subcomponent(modules = SyncAdapterModule.class)
public interface SyncAdapterComponent {

  void inject(GoogleTaskSyncAdapter googleTaskSyncAdapter);

  void inject(CalDAVSyncAdapter calDAVSyncAdapter);
}
