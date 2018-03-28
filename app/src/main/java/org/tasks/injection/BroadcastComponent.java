package org.tasks.injection;

import com.todoroo.astrid.calls.PhoneStateChangedReceiver;
import com.todoroo.astrid.gcal.CalendarAlarmReceiver;
import dagger.Subcomponent;
import org.tasks.notifications.NotificationClearedReceiver;
import org.tasks.receivers.BootCompletedReceiver;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.receivers.MyPackageReplacedReceiver;
import org.tasks.widget.TasksWidget;

@Subcomponent(modules = BroadcastModule.class)
public interface BroadcastComponent {

  void inject(PhoneStateChangedReceiver phoneStateChangedReceiver);

  void inject(CalendarAlarmReceiver calendarAlarmReceiver);

  void inject(MyPackageReplacedReceiver myPackageReplacedReceiver);

  void inject(CompleteTaskReceiver completeTaskReceiver);

  void inject(BootCompletedReceiver bootCompletedReceiver);

  void inject(TasksWidget tasksWidget);

  void inject(NotificationClearedReceiver notificationClearedReceiver);
}
