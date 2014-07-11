package org.tasks.injection;

import com.todoroo.astrid.alarms.AlarmTaskRepeatListener;
import com.todoroo.astrid.backup.BackupStartupReceiver;
import com.todoroo.astrid.calls.PhoneStateChangedReceiver;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gcal.CalendarAlarmReceiver;
import com.todoroo.astrid.gcal.CalendarStartupReceiver;
import com.todoroo.astrid.gcal.GCalTaskCompleteListener;
import com.todoroo.astrid.gtasks.GtasksCustomFilterCriteriaExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.gtasks.GtasksStartupReceiver;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ShowNotificationReceiver;
import com.todoroo.astrid.repeats.RepeatTaskCompleteListener;
import com.todoroo.astrid.tags.TagCustomFilterCriteriaExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;
import com.todoroo.astrid.timers.TimerTaskCompleteListener;
import com.todoroo.astrid.widget.TasksWidget;

import org.tasks.scheduling.RefreshBroadcastReceiver;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
        RefreshBroadcastReceiver.class,
        TasksWidget.class,
        Notifications.class,
        GtasksCustomFilterCriteriaExposer.class,
        TagCustomFilterCriteriaExposer.class,
        GCalTaskCompleteListener.class,
        TimerTaskCompleteListener.class,
        RepeatTaskCompleteListener.class,
        AlarmTaskRepeatListener.class,
        GtasksStartupReceiver.class,
        PhoneStateChangedReceiver.class,
        ShowNotificationReceiver.class,
        CoreFilterExposer.class,
        TimerFilterExposer.class,
        CustomFilterExposer.class,
        GtasksFilterExposer.class,
        TagFilterExposer.class,
        BackupStartupReceiver.class,
        CalendarAlarmReceiver.class,
        CalendarStartupReceiver.class
})
public class BroadcastModule {
}
