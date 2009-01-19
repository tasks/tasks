package com.timsu.astrid.utilities;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.timsu.astrid.R;

public class Preferences {

    // pref keys
    private static final String P_CURRENT_VERSION = "cv";
    private static final String P_SHOW_REPEAT_HELP = "repeathelp";
    private static final String P_TASK_LIST_SORT = "tlsort";
    private static final String P_SYNC_RTM_TOKEN = "rtmtoken";
    private static final String P_SYNC_RTM_LAST_SYNC = "rtmlastsync";
    private static final String P_SYNC_LAST_SYNC = "lastsync";

    // default values
    private static final boolean DEFAULT_PERSISTENCE_MODE = true;
    private static final boolean DEFAULT_COLORIZE = false;

    /** Set preference defaults, if unset. called at startup */
    public static void setPreferenceDefaults(Context context) {
        SharedPreferences prefs = getPrefs(context);
        Resources r = context.getResources();
        Editor editor = prefs.edit();

        if(!prefs.contains(r.getString(R.string.p_notif_annoy))) {
            editor.putBoolean(r.getString(R.string.p_notif_annoy),
                    DEFAULT_PERSISTENCE_MODE);
        }
        if(!prefs.contains(r.getString(R.string.p_fontSize))) {
            editor.putString(r.getString(R.string.p_fontSize), "20");
        }
        if(!prefs.contains(r.getString(R.string.p_deadlineTime))) {
            editor.putString(r.getString(R.string.p_deadlineTime), "7");
        }
        if(!prefs.contains(r.getString(R.string.p_colorize))) {
            editor.putBoolean(r.getString(R.string.p_colorize), DEFAULT_COLORIZE);
        }

        setVisibilityPreferences(prefs, editor, r);

        editor.commit();
    }

    private static void setVisibilityPreferences(SharedPreferences p, Editor e, Resources r) {
    	if(!p.contains(r.getString(R.string.prefs_titleVisible))) {
            e.putBoolean(r.getString(R.string.prefs_titleVisible),
            		Boolean.parseBoolean(r.getString(R.string.prefs_titleVisible_default)));
        }
    	if(!p.contains(r.getString(R.string.prefs_timeVisible))) {
            e.putBoolean(r.getString(R.string.prefs_timeVisible),
            		Boolean.parseBoolean(r.getString(R.string.prefs_timeVisible_default)));
        }
    	if(!p.contains(r.getString(R.string.prefs_deadlineVisible))) {
            e.putBoolean(r.getString(R.string.prefs_deadlineVisible),
            		Boolean.parseBoolean(r.getString(R.string.prefs_deadlineVisible_default)));
        }
    	if(!p.contains(r.getString(R.string.prefs_importanceVisible))) {
            e.putBoolean(r.getString(R.string.prefs_importanceVisible),
                    Boolean.parseBoolean(r.getString(R.string.prefs_importanceVisible_default)));
        }
    	if(!p.contains(r.getString(R.string.prefs_reminderVisible))) {
            e.putBoolean(r.getString(R.string.prefs_reminderVisible),
            		Boolean.parseBoolean(r.getString(R.string.prefs_reminderVisible_default)));
        }
    	if(!p.contains(r.getString(R.string.prefs_repeatVisible))) {
            e.putBoolean(r.getString(R.string.prefs_repeatVisible),
            		Boolean.parseBoolean(r.getString(R.string.prefs_repeatVisible_default)));
        }
    	if(!p.contains(r.getString(R.string.prefs_tagsVisible))) {
            e.putBoolean(r.getString(R.string.prefs_tagsVisible),
            		Boolean.parseBoolean(r.getString(R.string.prefs_tagsVisible_default)));
        }
    	if(!p.contains(r.getString(R.string.prefs_notesVisible))) {
            e.putBoolean(r.getString(R.string.prefs_notesVisible),
            		Boolean.parseBoolean(r.getString(R.string.prefs_notesVisible_default)));
        }
	}

	/** CurrentVersion: the currently installed version of Astrid */
    public static int getCurrentVersion(Context context) {
        return getPrefs(context).getInt(P_CURRENT_VERSION, 0);
    }

    /** CurrentVersion: the currently installed version of Astrid */
    public static void setCurrentVersion(Context context, int version) {
        Editor editor = getPrefs(context).edit();
        editor.putInt(P_CURRENT_VERSION, version);
        editor.commit();
    }

    /** ShowRepeatHelp: whether help dialog should be shown about repeats */
    public static boolean shouldShowRepeatHelp(Context context) {
        return getPrefs(context).getBoolean(P_SHOW_REPEAT_HELP, true);
    }

    public static void setShowRepeatHelp(Context context, boolean setting) {
        Editor editor = getPrefs(context).edit();
        editor.putBoolean(P_SHOW_REPEAT_HELP, setting);
        editor.commit();
    }

    /** returns hour at which quiet hours start, or null if not set */
    public static Integer getQuietHourStart(Context context) {
        return getIntegerValue(context, R.string.p_notif_quietStart);
    }

    /** returns hour at which quiet hours start, or null if not set */
    public static Integer getQuietHourEnd(Context context) {
        return getIntegerValue(context, R.string.p_notif_quietEnd);
    }

    /** Get notification ringtone, or null if not set */
    public static Uri getNotificationRingtone(Context context) {
    	Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(
                R.string.p_notification_ringtone), "");

        try {
			return Uri.parse(value);
		} catch (RuntimeException e) {
			return null;
		}
    }

    /** Get perstence mode setting */
    public static boolean isPersistenceMode(Context context) {
        Resources r = context.getResources();
        return getPrefs(context).getBoolean(r.getString(
                R.string.p_notif_annoy), DEFAULT_PERSISTENCE_MODE);
    }

    /** returns the font size user wants on the front page */
    public static Integer getTaskListFontSize(Context context) {
        return getIntegerValue(context, R.string.p_fontSize);
    }

    /** Return # of days from now to set deadlines by default */
    public static Integer getDefaultDeadlineDays(Context context) {
        return getIntegerValue(context, R.string.p_deadlineTime);
    }

    /** Get perstence mode setting */
    public static boolean isColorize(Context context) {
        Resources r = context.getResources();
        return getPrefs(context).getBoolean(r.getString(
                R.string.p_colorize), DEFAULT_COLORIZE);
    }

    /** Return # of days to remind by default */
    public static Integer getDefaultReminder(Context context) {
        return getIntegerValue(context, R.string.p_notif_defaultRemind);
    }

    /** TaskListSort: the sorting method for the task list */
    public static int getTaskListSort(Context context) {
        return getPrefs(context).getInt(P_TASK_LIST_SORT, 0);
    }

    /** TaskListSort: the sorting method for the task list */
    public static void setTaskListSort(Context context, int value) {
        Editor editor = getPrefs(context).edit();
        editor.putInt(P_TASK_LIST_SORT, value);
        editor.commit();
    }

    // --- synchronization preferences

    /** RTM authentication token, or null if doesn't exist */
    public static String getSyncRTMToken(Context context) {
        return getPrefs(context).getString(P_SYNC_RTM_TOKEN, null);
    }

    /** Sets the RTM authentication token. Set to null to clear. */
    public static void setSyncRTMToken(Context context, String setting) {
        Editor editor = getPrefs(context).edit();
        editor.putString(P_SYNC_RTM_TOKEN, setting);
        editor.commit();
    }

    /** RTM Last Successful Sync Date, or null */
    public static Date getSyncRTMLastSync(Context context) {
        Long value = getPrefs(context).getLong(P_SYNC_RTM_LAST_SYNC, 0);
        if(value == 0)
            return null;
        return new Date(value);
    }

    /** Set RTM Last Successful Sync Date */
    public static void setSyncRTMLastSync(Context context, Date date) {
        if(date == null) {
            clearPref(context, P_SYNC_RTM_LAST_SYNC);
            return;
        }

        Editor editor = getPrefs(context).edit();
        editor.putLong(P_SYNC_RTM_LAST_SYNC, date.getTime());
        editor.commit();
    }

    /** Should sync with RTM? */
    public static boolean shouldSyncRTM(Context context) {
        Resources r = context.getResources();
        return getPrefs(context).getBoolean(r.getString(
                R.string.p_sync_rtm), false);
    }

    /** returns the font size user wants on the front page */
    public static Integer autoSyncFrequency(Context context) {
        return getIntegerValue(context, R.string.p_sync_every);
    }

    /** Last Auto-Sync Date, or null */
    public static Date getSyncLastSync(Context context) {
        Long value = getPrefs(context).getLong(P_SYNC_LAST_SYNC, 0);
        if(value == 0)
            return null;
        return new Date(value);
    }

    /** Set Last Auto-Sync Date */
    public static void setSyncLastSync(Context context, Date date) {
        if(date == null) {
            clearPref(context, P_SYNC_LAST_SYNC);
            return;
        }

        Editor editor = getPrefs(context).edit();
        editor.putLong(P_SYNC_LAST_SYNC, date.getTime());
        editor.commit();
    }

    // --- helper methods

    private static void clearPref(Context context, String key) {
        Editor editor = getPrefs(context).edit();
        editor.remove(key);
        editor.commit();
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static Integer getIntegerValue(Context context, int keyResource) {
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(keyResource), "");

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static TaskFieldsVisibility getTaskFieldsVisibility(Context context) {
    	return TaskFieldsVisibility.getFromPreferences(context, getPrefs(context));
    }
}
