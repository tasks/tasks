package org.tasks.notifications;

import android.content.Context;

import com.todoroo.andlib.utility.AndroidUtilities;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class TelephonyManager {

  private final android.telephony.TelephonyManager telephonyManager;

  @Inject
  public TelephonyManager(@ApplicationContext Context context) {
    telephonyManager =
        (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
  }

  public boolean callStateIdle() {
    return AndroidUtilities.atLeastS() || telephonyManager.getCallState() == android.telephony.TelephonyManager.CALL_STATE_IDLE;
  }
}
