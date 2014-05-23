package org.tasks.injection;

import com.todoroo.astrid.widget.TasksWidget;

import org.tasks.scheduling.RefreshBroadcastReceiver;

import dagger.Module;

@Module(library = true,
        injects = {
                RefreshBroadcastReceiver.class,
                TasksWidget.class
        })
public class BroadcastModule {
}
