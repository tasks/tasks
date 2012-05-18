package com.todoroo.astrid.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.utility.Preferences;

public class ReengagementReceiver extends BroadcastReceiver {



    @Override
    public void onReceive(Context context, Intent intent) {

        int reengagementReminders = Preferences.getInt(ReengagementService.PREF_REENGAGEMENT_COUNT, 1);
        Preferences.setInt(ReengagementService.PREF_REENGAGEMENT_COUNT, reengagementReminders + 1);

        // TODO: show dialog

        ReengagementService.scheduleReengagementAlarm(context);
    }

}
