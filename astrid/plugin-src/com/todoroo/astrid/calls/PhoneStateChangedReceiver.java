package com.todoroo.astrid.calls;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
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
            String number = digitsOnly(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
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
                    Calls.TYPE + " = ? AND " + Calls.NEW + " = ?",
                    new String[] { Integer.toString(Calls.MISSED_TYPE), "1" },
                    Calls.DATE + " DESC"
                    );

            try {
                if (calls.getCount() > 0) {
                    calls.moveToFirst();

                    int numberIndex = calls.getColumnIndex(Calls.NUMBER);
                    String number = calls.getString(numberIndex);

                    // Check for phone number match
                    if (!lastNumber.equals(digitsOnly(number)))
                        return;

                    // If a lot of time has passed since the most recent missed call, ignore
                    // It could be the same person calling you back before you call them back,
                    // but if you answer this time, the missed call will still be in the database
                    // and will be processed again.
                    int dateIndex = calls.getColumnIndex(Calls.DATE);
                    long date = calls.getLong(dateIndex);
                    if (DateUtilities.now() - date > 2 * DateUtilities.ONE_MINUTE)
                        return;

                    int nameIndex = calls.getColumnIndex(Calls.CACHED_NAME);
                    String name = calls.getString(nameIndex);

                    int timeIndex = calls.getColumnIndex(Calls.DATE);
                    long time = calls.getLong(timeIndex);
                    String timeString = DateUtilities.getTimeString(context, new Date(time));


                    Intent missedCallIntent = new Intent(context, MissedCallActivity.class);
                    missedCallIntent.putExtra(MissedCallActivity.EXTRA_NUMBER, number);
                    missedCallIntent.putExtra(MissedCallActivity.EXTRA_NAME, name);
                    missedCallIntent.putExtra(MissedCallActivity.EXTRA_TIME, timeString);
                    missedCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    context.startActivity(missedCallIntent);
                }
            } finally {
                calls.close();
            }
        } else {
            System.err.println("ASTRID Other state: " + state);
        }
    }

    private String digitsOnly(String number) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (Character.isDigit(c))
                builder.append(c);
        }
        return builder.toString();
    }

}
