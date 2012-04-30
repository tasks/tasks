package com.todoroo.astrid.calls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

@SuppressWarnings("nls")
public class PhoneStateChangedReceiver extends BroadcastReceiver {

    private static final String PREF_LAST_INCOMING_NUMBER = "last_incoming_number";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Preferences.getBoolean(R.string.p_field_missed_calls, true)) {
            Preferences.clear(PREF_LAST_INCOMING_NUMBER);
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            System.err.println("Ringing: " + number);
            if (TextUtils.isEmpty(number))
                return;

            Preferences.setString(PREF_LAST_INCOMING_NUMBER, number);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            String lastNumber = Preferences.getStringValue(PREF_LAST_INCOMING_NUMBER);
            if (TextUtils.isEmpty(lastNumber))
                return;

            Preferences.clear(PREF_LAST_INCOMING_NUMBER);
            Cursor calls = context.getContentResolver().query(
                    Calls.CONTENT_URI,
                    null,
                    Calls.TYPE + " = ? AND " + Calls.NEW + " = ? AND " + Calls.NUMBER + " = ?" ,
                    new String[] { Integer.toString(Calls.MISSED_TYPE), "1", lastNumber },
                    Calls.DATE + " DESC"
                    );

            try {
                if (calls.getCount() > 0) {
                    calls.moveToFirst();
                    int nameIndex = calls.getColumnIndex(Calls.CACHED_NAME);
                    String name = "";
                    if (nameIndex > -1)
                        name = calls.getString(nameIndex);
                    Intent missedCallIntent = new Intent(context, MissedCallActivity.class);
                    missedCallIntent.putExtra(MissedCallActivity.EXTRA_NUMBER, lastNumber);
                    missedCallIntent.putExtra(MissedCallActivity.EXTRA_NAME, name);
                    missedCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(missedCallIntent);
                }
            } finally {
                calls.close();
            }
        } else {
            System.err.println("Other state: " + state);
        }
    }

}
