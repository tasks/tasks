package org.tasks.injection;

import com.todoroo.astrid.alarms.AlarmTaskRepeatListener;
import com.todoroo.astrid.calls.PhoneStateChangedReceiver;
import com.todoroo.astrid.gcal.CalendarAlarmReceiver;
import com.todoroo.astrid.gcal.GCalTaskCompleteListener;
import com.todoroo.astrid.repeats.RepeatTaskCompleteListener;
import com.todoroo.astrid.timers.TimerTaskCompleteListener;
import com.todoroo.astrid.widget.TasksWidget;

import org.tasks.receivers.BootCompletedReceiver;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.receivers.FirstLaunchReceiver;
import org.tasks.receivers.ListNotificationReceiver;
import org.tasks.receivers.MyPackageReplacedReceiver;
import org.tasks.receivers.RefreshReceiver;
import org.tasks.receivers.RepeatConfirmationReceiver;
import org.tasks.receivers.TaskNotificationReceiver;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
                TasksWidget.class,
                TaskNotificationReceiver.class,
                ListNotificationReceiver.class,
                GCalTaskCompleteListener.class,
                TimerTaskCompleteListener.class,
                RepeatTaskCompleteListener.class,
                AlarmTaskRepeatListener.class,
                PhoneStateChangedReceiver.class,
                CalendarAlarmReceiver.class,
                BootCompletedReceiver.class,
                FirstLaunchReceiver.class,
                MyPackageReplacedReceiver.class,
                RefreshReceiver.class,
                CompleteTaskReceiver.class,
                RepeatConfirmationReceiver.class
        })
public class BroadcastModule {
}
