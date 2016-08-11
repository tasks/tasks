package org.tasks.injection;

import org.tasks.locale.receiver.FireReceiver;
import org.tasks.receivers.GoogleTaskPushReceiver;

import dagger.Subcomponent;

@Subcomponent(modules = BroadcastModule.class)
public interface BroadcastComponent extends BaseBroadcastComponent {
    void inject(FireReceiver fireReceiver);

    void inject(GoogleTaskPushReceiver forceSyncReceiver);
}
