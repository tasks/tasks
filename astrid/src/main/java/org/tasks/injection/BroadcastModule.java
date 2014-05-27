package org.tasks.injection;

import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.widget.TasksWidget;

import org.tasks.scheduling.RefreshBroadcastReceiver;

import dagger.Module;

@Module(library = true,
        injects = {
                RefreshBroadcastReceiver.class,
                TasksWidget.class,
                Notifications.class
        })
public class BroadcastModule {
}
