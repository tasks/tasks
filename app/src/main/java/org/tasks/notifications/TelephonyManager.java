package org.tasks.notifications;

import android.app.Application;
import android.content.Context;
import javax.inject.Inject;

public class TelephonyManager {

  private final android.telephony.TelephonyManager telephonyManager;

  @Inject
  public TelephonyManager(Application context) {
    telephonyManager =
        (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
  }

  public boolean callStateIdle() {
    return telephonyManager.getCallState() == android.telephony.TelephonyManager.CALL_STATE_IDLE;
  }
}
