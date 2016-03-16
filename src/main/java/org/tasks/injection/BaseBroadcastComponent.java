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
import org.tasks.receivers.TaskNotificationReceiver;

public interface BaseBroadcastComponent {

    void inject(TimerTaskCompleteListener timerTaskCompleteListener);

    void inject(PhoneStateChangedReceiver phoneStateChangedReceiver);

    void inject(AlarmTaskRepeatListener alarmTaskRepeatListener);

    void inject(GCalTaskCompleteListener gCalTaskCompleteListener);

    void inject(CalendarAlarmReceiver calendarAlarmReceiver);

    void inject(RepeatTaskCompleteListener repeatTaskCompleteListener);

    void inject(MyPackageReplacedReceiver myPackageReplacedReceiver);

    void inject(RefreshReceiver refreshReceiver);

    void inject(TaskNotificationReceiver taskNotificationReceiver);

    void inject(CompleteTaskReceiver completeTaskReceiver);

    void inject(FirstLaunchReceiver firstLaunchReceiver);

    void inject(ListNotificationReceiver listNotificationReceiver);

    void inject(BootCompletedReceiver bootCompletedReceiver);

    void inject(TasksWidget tasksWidget);
}
