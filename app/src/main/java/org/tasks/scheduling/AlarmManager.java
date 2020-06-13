package org.tasks.scheduling;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import javax.inject.Inject;
import org.tasks.injection.ApplicationContext;

public class AlarmManager {

  private final android.app.AlarmManager alarmManager;

  @Inject
  public AlarmManager(@ApplicationContext Context context) {
    alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  public void cancel(PendingIntent pendingIntent) {
    alarmManager.cancel(pendingIntent);
  }

  @SuppressLint("NewApi")
  public void wakeup(long time, PendingIntent pendingIntent) {
    alarmManager.setExactAndAllowWhileIdle(
        android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
  }
}
