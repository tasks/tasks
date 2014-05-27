package org.tasks.injection;

import com.todoroo.astrid.gtasks.GtasksCustomFilterCriteriaExposer;
import com.todoroo.astrid.gtasks.GtasksDetailExposer;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.GlobalEventReceiver;
import com.todoroo.astrid.tags.TagCustomFilterCriteriaExposer;
import com.todoroo.astrid.tags.TagDetailExposer;
import com.todoroo.astrid.widget.TasksWidget;

import org.tasks.scheduling.RefreshBroadcastReceiver;

import dagger.Module;

@Module(library = true,
        injects = {
                RefreshBroadcastReceiver.class,
                TasksWidget.class,
                Notifications.class,
                GtasksCustomFilterCriteriaExposer.class,
                GtasksDetailExposer.class,
                GlobalEventReceiver.class,
                TagDetailExposer.class,
                TagCustomFilterCriteriaExposer.class
        })
public class BroadcastModule {
}
