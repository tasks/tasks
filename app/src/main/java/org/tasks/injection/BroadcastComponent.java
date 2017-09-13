package org.tasks.injection;

import com.todoroo.astrid.calls.PhoneStateChangedReceiver;
import com.todoroo.astrid.gcal.CalendarAlarmReceiver;

import org.tasks.locale.receiver.FireReceiver;
import org.tasks.notifications.NotificationClearedReceiver;
import org.tasks.receivers.BootCompletedReceiver;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.receivers.MyPackageReplacedReceiver;
import org.tasks.receivers.PushReceiver;
import org.tasks.widget.TasksWidget;

import dagger.Subcomponent;

@Subcomponent(modules = BroadcastModule.class)
public interface BroadcastComponent {
    void inject(FireReceiver fireReceiver);

    void inject(PhoneStateChangedReceiver phoneStateChangedReceiver);

    void inject(CalendarAlarmReceiver calendarAlarmReceiver);

    void inject(MyPackageReplacedReceiver myPackageReplacedReceiver);

    void inject(CompleteTaskReceiver completeTaskReceiver);

    void inject(BootCompletedReceiver bootCompletedReceiver);

    void inject(TasksWidget tasksWidget);

    void inject(PushReceiver pushReceiver);

    void inject(NotificationClearedReceiver notificationClearedReceiver);
}
