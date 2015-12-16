/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.calls;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.Notifier;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import timber.log.Timber;

public class PhoneStateChangedReceiver extends InjectingBroadcastReceiver {

    private static final String PREF_LAST_INCOMING_NUMBER = "last_incoming_number";

    private static final long WAIT_BEFORE_READ_LOG = 3000L;

    @Inject Preferences preferences;
    @Inject Notifier notifier;

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!preferences.fieldMissedPhoneCalls()) {
            preferences.clear(PREF_LAST_INCOMING_NUMBER);
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String number = digitsOnly(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
            if (TextUtils.isEmpty(number)) {
                return;
            }

            preferences.setString(PREF_LAST_INCOMING_NUMBER, number);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            final String lastNumber = preferences.getStringValue(PREF_LAST_INCOMING_NUMBER);
            if (TextUtils.isEmpty(lastNumber)) {
                return;
            }

            preferences.clear(PREF_LAST_INCOMING_NUMBER);

            new Thread() {
                @Override
                public void run() {
                    AndroidUtilities.sleepDeep(WAIT_BEFORE_READ_LOG);
                    Cursor calls;
                    try {
                        calls = context.getContentResolver().query(
                            Calls.CONTENT_URI,
                            new String[] { Calls.NUMBER, Calls.DATE, Calls.CACHED_NAME },
                            Calls.TYPE + " = ? AND " + Calls.NEW + " = ?",
                            new String[] { Integer.toString(Calls.MISSED_TYPE), "1" },
                            Calls.DATE + " DESC"
                            );
                    } catch (Exception e) { // Sometimes database is locked, retry once
                        Timber.e(e, e.getMessage());
                        AndroidUtilities.sleepDeep(300L);
                        try {
                            calls = context.getContentResolver().query(
                                    Calls.CONTENT_URI,
                                    new String[] { Calls.NUMBER, Calls.DATE, Calls.CACHED_NAME },
                                    Calls.TYPE + " = ? AND " + Calls.NEW + " = ?",
                                    new String[] { Integer.toString(Calls.MISSED_TYPE), "1" },
                                    Calls.DATE + " DESC"
                                    );
                        } catch (Exception e2) {
                            Timber.e(e2, e2.getMessage());
                            calls = null;
                        }
                    }
                    try {
                        if (calls == null) {
                            return;
                        }
                        if (calls.moveToFirst()) {
                            int numberIndex = calls.getColumnIndex(Calls.NUMBER);
                            String number = calls.getString(numberIndex);

                            // Sanity check for phone number match
                            // in case the phone logs haven't updated for some reaosn
                            if (!lastNumber.equals(digitsOnly(number))) {
                                return;
                            }

                            // If a lot of time has passed since the most recent missed call, ignore
                            // It could be the same person calling you back before you call them back,
                            // but if you answer this time, the missed call will still be in the database
                            // and will be processed again.
                            int dateIndex = calls.getColumnIndex(Calls.DATE);
                            long date = calls.getLong(dateIndex);
                            if (DateUtilities.now() - date > 2 * DateUtilities.ONE_MINUTE) {
                                return;
                            }

                            int nameIndex = calls.getColumnIndex(Calls.CACHED_NAME);
                            String name = calls.getString(nameIndex);

                            long contactId = getContactIdFromNumber(context, number);

                            notifier.triggerMissedCallNotification(name, number, contactId);
                        }
                    } catch (Exception e) {
                        Timber.e(e, e.getMessage());
                    } finally {
                        if (calls != null) {
                            calls.close();
                        }
                    }
                }
            }.start();
        }
    }

    private String digitsOnly(String number) {
        if (number == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (Character.isDigit(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private long getContactIdFromNumber(Context context, String number) {
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor c = context.getContentResolver().query(contactUri, new String[] { ContactsContract.PhoneLookup._ID }, null, null, null);

        try {
            if (c.moveToFirst()) {
                return c.getLong(c.getColumnIndex(ContactsContract.PhoneLookup._ID));
            }
        } finally {
            c.close();
        }
        return -1;
    }

}
