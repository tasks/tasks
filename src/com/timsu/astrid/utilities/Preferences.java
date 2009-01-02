package com.timsu.astrid.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.timsu.astrid.R;

public class Preferences {

    private static String CURRENT_VERSION = "cv";

    private static SharedPreferences getPrefs(Context context) {
        return  PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getCurrentVersion(Context context) {
        return getPrefs(context).getInt(CURRENT_VERSION, 0);
    }

    public static void setCurrentVersion(Context context, int version) {
        Editor editor = getPrefs(context).edit();
        editor.putInt(CURRENT_VERSION, version);
        editor.commit();
    }

    /** returns hour at which quiet hours start, or null if not set */
    public static Integer getQuietHourStart(Context context) {
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(
                R.string.p_notif_quietStart), "");

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    /** returns hour at which quiet hours start, or null if not set */
    public static Integer getQuietHourEnd(Context context) {
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(
                R.string.p_notif_quietEnd), "");

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Uri getNotificationRingtone(Context context) {
    	Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(
                R.string.key_notification_ringtone), "");

        try {
			return Uri.parse(value);
		} catch (RuntimeException e) {
			return null;
		}
    }
}
