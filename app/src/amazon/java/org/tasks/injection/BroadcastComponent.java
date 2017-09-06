package org.tasks.injection;

import com.todoroo.astrid.alarms.AlarmTaskRepeatListener;
import com.todoroo.astrid.calls.PhoneStateChangedReceiver;
import com.todoroo.astrid.gcal.CalendarAlarmReceiver;
import com.todoroo.astrid.gcal.GCalTaskCompleteListener;
import com.todoroo.astrid.repeats.RepeatTaskCompleteListener;
import com.todoroo.astrid.timers.TimerTaskCompleteListener;

import org.tasks.locale.receiver.FireReceiver;
import org.tasks.notifications.NotificationClearedReceiver;
import org.tasks.receivers.BootCompletedReceiver;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.receivers.MyPackageReplacedReceiver;
import org.tasks.receivers.PushReceiver;
import org.tasks.receivers.TeslaUnreadReceiver;
import org.tasks.widget.TasksWidget;

import dagger.Subcomponent;

@Subcomponent(modules = BroadcastModule.class)
public interface BroadcastComponent {
    void inject(FireReceiver fireReceiver);

    void inject(TimerTaskCompleteListener timerTaskCompleteListener);

    void inject(PhoneStateChangedReceiver phoneStateChangedReceiver);

    void inject(AlarmTaskRepeatListener alarmTaskRepeatListener);

    void inject(GCalTaskCompleteListener gCalTaskCompleteListener);

    void inject(CalendarAlarmReceiver calendarAlarmReceiver);

    void inject(RepeatTaskCompleteListener repeatTaskCompleteListener);

    void inject(MyPackageReplacedReceiver myPackageReplacedReceiver);

    void inject(CompleteTaskReceiver completeTaskReceiver);

    void inject(BootCompletedReceiver bootCompletedReceiver);

    void inject(TasksWidget tasksWidget);

    void inject(TeslaUnreadReceiver teslaUnreadReceiver);

    void inject(PushReceiver pushReceiver);

    void inject(NotificationClearedReceiver notificationClearedReceiver);
}
