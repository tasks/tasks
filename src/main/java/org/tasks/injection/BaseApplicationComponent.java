package org.tasks.injection;

import org.tasks.Tasks;

public interface BaseApplicationComponent {
    Tasks inject(Tasks tasks);

    ActivityComponent plus(ActivityModule module);

    BroadcastComponent plus(BroadcastModule module);

    IntentServiceComponent plus(IntentServiceModule module);

    ServiceComponent plus(ServiceModule serviceModule);
}
