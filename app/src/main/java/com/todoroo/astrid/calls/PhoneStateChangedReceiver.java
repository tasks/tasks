/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.calls;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import java.io.InputStream;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.reminders.MissedCallActivity;
import timber.log.Timber;

public class PhoneStateChangedReceiver extends InjectingBroadcastReceiver {

  private static final String PREF_LAST_INCOMING_NUMBER = "last_incoming_number";

  private static final long WAIT_BEFORE_READ_LOG = 3000L;

  @Inject Preferences preferences;
  @Inject NotificationManager notificationManager;
  @Inject PermissionChecker permissionChecker;
  @Inject @ForApplication Context context;

  @Override
  public void onReceive(final Context context, Intent intent) {
    super.onReceive(context, intent);

    if (!intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
      return;
    }

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
            calls = getMissedCalls();
          } catch (Exception e) { // Sometimes database is locked, retry once
            Timber.e(e);
            AndroidUtilities.sleepDeep(300L);
            try {
              calls = getMissedCalls();
            } catch (Exception e2) {
              Timber.e(e2);
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

              triggerMissedCallNotification(name, number, contactId);
            }
          } catch (Exception e) {
            Timber.e(e);
          } finally {
            if (calls != null) {
              calls.close();
            }
          }
        }
      }.start();
    }
  }

  @SuppressLint("MissingPermission")
  private Cursor getMissedCalls() {
    if (permissionChecker.canAccessMissedCallPermissions()) {
      //noinspection MissingPermission
      return context
          .getContentResolver()
          .query(
              Calls.CONTENT_URI,
              new String[] {Calls.NUMBER, Calls.DATE, Calls.CACHED_NAME},
              Calls.TYPE + " = ? AND " + Calls.NEW + " = ?",
              new String[] {Integer.toString(Calls.MISSED_TYPE), "1"},
              Calls.DATE + " DESC");
    }
    return null;
  }

  @Override
  protected void inject(BroadcastComponent component) {
    component.inject(this);
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
    Uri contactUri =
        Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    Cursor c =
        context
            .getContentResolver()
            .query(contactUri, new String[] {ContactsContract.PhoneLookup._ID}, null, null, null);

    try {
      if (c.moveToFirst()) {
        return c.getLong(c.getColumnIndex(ContactsContract.PhoneLookup._ID));
      }
    } finally {
      c.close();
    }
    return -1;
  }

  private void triggerMissedCallNotification(
      final String name, final String number, long contactId) {
    final String title =
        context.getString(R.string.missed_call, TextUtils.isEmpty(name) ? number : name);

    Intent missedCallDialog = new Intent(context, MissedCallActivity.class);
    missedCallDialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    missedCallDialog.putExtra(MissedCallActivity.EXTRA_NUMBER, number);
    missedCallDialog.putExtra(MissedCallActivity.EXTRA_NAME, name);
    missedCallDialog.putExtra(MissedCallActivity.EXTRA_TITLE, title);

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_CALLS)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.app_name))
            .setWhen(currentTimeMillis())
            .setShowWhen(true)
            .setSmallIcon(R.drawable.ic_check_white_24dp)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    missedCallDialog.hashCode(),
                    missedCallDialog,
                    PendingIntent.FLAG_UPDATE_CURRENT));

    Bitmap contactImage = getContactImage(contactId);
    if (contactImage != null) {
      builder.setLargeIcon(contactImage);
    }

    Intent callNow = new Intent(context, MissedCallActivity.class);
    callNow.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    callNow.putExtra(MissedCallActivity.EXTRA_NUMBER, number);
    callNow.putExtra(MissedCallActivity.EXTRA_NAME, name);
    callNow.putExtra(MissedCallActivity.EXTRA_TITLE, title);
    callNow.putExtra(MissedCallActivity.EXTRA_CALL_NOW, true);

    Intent callLater = new Intent(context, MissedCallActivity.class);
    callLater.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    callLater.putExtra(MissedCallActivity.EXTRA_NUMBER, number);
    callLater.putExtra(MissedCallActivity.EXTRA_NAME, name);
    callLater.putExtra(MissedCallActivity.EXTRA_TITLE, title);
    callLater.putExtra(MissedCallActivity.EXTRA_CALL_LATER, true);
    builder
        .addAction(
            R.drawable.ic_phone_white_24dp,
            context.getString(R.string.MCA_return_call),
            PendingIntent.getActivity(
                context, callNow.hashCode(), callNow, PendingIntent.FLAG_UPDATE_CURRENT))
        .addAction(
            R.drawable.ic_add_white_24dp,
            context.getString(R.string.MCA_add_task),
            PendingIntent.getActivity(
                context, callLater.hashCode(), callLater, PendingIntent.FLAG_UPDATE_CURRENT));

    notificationManager.notify(number.hashCode(), builder, true, false, false);
  }

  private Bitmap getContactImage(long contactId) {
    Bitmap b = null;
    if (contactId >= 0) {
      Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
      InputStream input =
          ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
      try {
        b = BitmapFactory.decodeStream(input);
      } catch (OutOfMemoryError e) {
        Timber.e(e);
      }
    }
    return b;
  }
}
