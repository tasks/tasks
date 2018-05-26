package org.tasks.scheduling;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;

public class AlarmManager {

  private final android.app.AlarmManager alarmManager;

  @Inject
  public AlarmManager(@ForApplication Context context) {
    alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  public void cancel(PendingIntent pendingIntent) {
    alarmManager.cancel(pendingIntent);
  }

  @SuppressLint("NewApi")
  public void wakeup(long time, PendingIntent pendingIntent) {
    if (atLeastMarshmallow()) {
      alarmManager.setExactAndAllowWhileIdle(
          android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
    } else if (atLeastKitKat()) {
      alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
    } else {
      alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }
  }
}
