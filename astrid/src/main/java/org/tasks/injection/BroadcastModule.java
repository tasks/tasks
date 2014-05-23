package org.tasks.injection;

import org.tasks.scheduling.RefreshBroadcastReceiver;

import dagger.Module;

@Module(library = true,
        injects = {
                RefreshBroadcastReceiver.class
        })
public class BroadcastModule {
}
