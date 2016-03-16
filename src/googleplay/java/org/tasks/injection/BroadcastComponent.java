package org.tasks.injection;

import org.tasks.locale.receiver.FireReceiver;
import org.tasks.receivers.TeslaUnreadReceiver;

import dagger.Subcomponent;

@Subcomponent(modules = BroadcastModule.class)
public interface BroadcastComponent extends BaseBroadcastComponent {
    void inject(TeslaUnreadReceiver teslaUnreadReceiver);

    void inject(FireReceiver fireReceiver);
}
