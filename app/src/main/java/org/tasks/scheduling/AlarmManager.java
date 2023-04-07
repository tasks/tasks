package org.tasks.scheduling;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastS;

import android.app.PendingIntent;
import android.content.Context;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class AlarmManager {

  private final android.app.AlarmManager alarmManager;

  @Inject
  public AlarmManager(@ApplicationContext Context context) {
    alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  public void cancel(PendingIntent pendingIntent) {
    alarmManager.cancel(pendingIntent);
  }

  public void wakeup(long time, PendingIntent pendingIntent) {
    if (!atLeastS() || alarmManager.canScheduleExactAlarms()) {
      alarmManager.setExactAndAllowWhileIdle(
              android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }
  }
}
