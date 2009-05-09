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
    private static final String P_SYNC_LAST_SYNC_ATTEMPT = "lastsyncattempt";

    // pref values
    public static final int ICON_SET_PINK = 0;
    public static final int ICON_SET_BORING = 1;
    public static final int ICON_SET_ASTRID = 2;

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
            editor.putString(r.getString(R.string.p_deadlineTime), "1");
        }
        if(!prefs.contains(r.getString(R.string.p_notif_defaultRemind))) {
            editor.putString(r.getString(R.string.p_notif_defaultRemind), "0");
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

    // --- system preferences

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

    // --- notification settings

    /** returns hour at which quiet hours start, or null if not set */
    public static Integer getQuietHourStart(Context context) {
        return getIntegerValue(context, R.string.p_notif_quietStart);
    }

    /** returns hour at which quiet hours start, or null if not set */
    public static Integer getQuietHourEnd(Context context) {
        return getIntegerValue(context, R.string.p_notif_quietEnd);
    }

    /** returns hour at which quiet hours start, or null if not set */
    public static int getNotificationIconTheme(Context context) {
        Integer index = getIntegerValue(context, R.string.p_notif_icon);
        if(index == null)
            index = 0;
        return index;
    }

    /** Get notification ring tone, or null if not set */
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

    /** Return # of days to remind by default */
    public static Integer getDefaultReminder(Context context) {
        return getIntegerValue(context, R.string.p_notif_defaultRemind);
    }

    // --- postpone count & settings

    /** whether nags for postponing and other things should be shown */
    public static boolean shouldShowNags(Context context) {
        return getPrefs(context).getBoolean(context.getResources().
                getString(R.string.p_nagging), true);
    }

    // --- appearance settings

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

    /** Should display sync shortcut? */
    public static boolean shouldDisplaySyncButton(Context context) {
        Resources r = context.getResources();
        return getPrefs(context).getBoolean(r.getString(
                R.string.p_sync_button), false);
    }

    /** Should hide sync dialog boxes? */
    public static boolean shouldSuppressSyncDialogs(Context context) {
        Resources r = context.getResources();
        return getPrefs(context).getBoolean(r.getString(
                R.string.p_sync_quiet), false);
    }

    /** Reads the frequency, in seconds, auto-sync should occur.
     * @return seconds duration, or null if not desired */
    public static Integer getSyncAutoSyncFrequency(Context context) {
    	Integer time = getIntegerValue(context, R.string.p_sync_interval);
        if(time != null && time == 0)
            time = null;
        return time;
    }

    /** Reads the old auto */
    public static Float getSyncOldAutoSyncFrequency(Context context) {
    	return getFloatValue(context, R.string.p_sync_every_old);
    }

    /** Sets the auto-sync frequency to the desired value */
    public static void setSyncAutoSyncFrequency(Context context, int value) {
    	Editor editor = getPrefs(context).edit();
        editor.putString(context.getResources().getString(R.string.p_sync_interval),
        		Integer.toString(value));
        editor.commit();
    }

    /** Last Auto-Sync Date, or null */
    public static Date getSyncLastSync(Context context) {
        Long value = getPrefs(context).getLong(P_SYNC_LAST_SYNC, 0);
        if(value == 0)
            return null;
        return new Date(value);
    }

    /** Last Successful Auto-Sync Date, or null */
    public static Date getSyncLastSyncAttempt(Context context) {
        Long value = getPrefs(context).getLong(P_SYNC_LAST_SYNC_ATTEMPT, 0);
        if(value == 0)
            return null;
        return new Date(value);
    }

    /** Set Last Sync Date */
    public static void setSyncLastSync(Context context, Date date) {
        if(date == null) {
            clearPref(context, P_SYNC_LAST_SYNC);
            return;
        }

        Editor editor = getPrefs(context).edit();
        editor.putLong(P_SYNC_LAST_SYNC, date.getTime());
        editor.commit();
    }

    /** Set Last Auto-Sync Attempt Date */
    public static void setSyncLastSyncAttempt(Context context, Date date) {
        Editor editor = getPrefs(context).edit();
        editor.putLong(P_SYNC_LAST_SYNC_ATTEMPT, date.getTime());
        editor.commit();
    }

    // --- helper methods

    /** Clear the given preference */
    private static void clearPref(Context context, String key) {
        Editor editor = getPrefs(context).edit();
        editor.remove(key);
        editor.commit();
    }

    /** Get preferences object from the context */
    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /** Gets an integer value from a string resource id. Returns null
     * if the value is not set or not an integer.
     *
     * @param context
     * @param keyResource resource from string.xml
     * @return integer value, or null on error
     */
    private static Integer getIntegerValue(Context context, int keyResource) {
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(keyResource), "");

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    /** Gets an float value from a string resource id. Returns null
     * if the value is not set or not an flat.
     *
     * @param context
     * @param keyResource resource from string.xml
     * @return
     */
    private static Float getFloatValue(Context context, int keyResource) {
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(keyResource), "");

        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static TaskFieldsVisibility getTaskFieldsVisibility(Context context) {
    	return TaskFieldsVisibility.getFromPreferences(context, getPrefs(context));
    }
}
