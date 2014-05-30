package com.todoroo.astrid.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;

import org.joda.time.DateTime;
import org.tasks.R;

import static com.todoroo.andlib.utility.Preferences.setBoolean;

public class AstridDefaultPreferenceSpec {

    private Context context;

    public AstridDefaultPreferenceSpec(Context context) {
        this.context = context;
    }

    public void setIfUnset() {
        SharedPreferences prefs = Preferences.getPrefs(context);
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        setPreference(prefs, editor, r, R.string.p_default_urgency_key, 0);
        setPreference(prefs, editor, r, R.string.p_default_importance_key, 2);
        setPreference(prefs, editor, r, R.string.p_default_hideUntil_key, 0);
        setPreference(prefs, editor, r, R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE);
        setPreference(prefs, editor, r, R.string.p_rmd_default_random_hours, 0);
        setPreference(prefs, editor, r, R.string.p_fontSize, 16);
        setPreference(prefs, editor, r, R.string.p_showNotes, false);

        setPreference(prefs, editor, r, R.string.p_field_missed_calls, true);

        setPreference(prefs, editor, r, R.string.p_end_at_deadline, true);

        setPreference(prefs, editor, r, R.string.p_rmd_persistent, true);

        setPreference(prefs, editor, r, R.string.p_show_today_filter, true);
        setPreference(prefs, editor, r, R.string.p_show_recently_modified_filter, true);
        setPreference(prefs, editor, r, R.string.p_show_not_in_list_filter, true);

        setPreference(prefs, editor, r, R.string.p_calendar_reminders, true);

        setPreference(prefs, editor, r, R.string.p_use_dark_theme, false);

        setPreference(prefs, editor, r, R.string.p_force_phone_layout, false);

        setPreference(prefs, editor, r, R.string.p_show_quickadd_controls, true);

        setPreference(prefs, editor, r, R.string.p_show_task_edit_comments, true);

        setPreference(prefs, editor, r, R.string.p_taskRowStyle_v2, "1"); //$NON-NLS-1$

        setPreference(prefs, editor, r, R.string.p_use_date_shortcuts, false);

        setPreference(prefs, editor, r, R.string.p_save_and_cancel, false);

        setPreference(prefs, editor, r, R.string.p_hide_plus_button, true);

        setPreference(prefs, editor, r, R.string.p_rmd_quietStart_old, 22); // enable quiet hours by default

        setIntIfUnset(prefs, editor, r, R.string.p_rmd_quietStart, r.getInteger(R.integer.default_quiet_hours_start));
        setIntIfUnset(prefs, editor, r, R.string.p_rmd_quietEnd, r.getInteger(R.integer.default_quiet_hours_end));
        setIntIfUnset(prefs, editor, r, R.string.p_rmd_time, r.getInteger(R.integer.default_remind_time));

        String dragDropTestInitialized = "android_drag_drop_initialized"; //$NON-NLS-1$
        if (!Preferences.getBoolean(dragDropTestInitialized, false)) {
            SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(context);
            if (publicPrefs != null) {
                Editor edit = publicPrefs.edit();
                if (edit != null) {
                    edit.putInt(SortHelper.PREF_SORT_SORT, SortHelper.SORT_AUTO);
                    edit.commit();
                    Preferences.setInt(AstridPreferences.P_SUBTASKS_HELP, 1);
                }
            }
            setBoolean(dragDropTestInitialized, true);
        }
        BeastModePreferences.setDefaultOrder(context);

        editor.commit();

        migrateToNewQuietHours();
    }

    static void migrateToNewQuietHours() {
        if(!Preferences.getBoolean(R.string.p_rmd_hasMigrated, false)) {
            setBoolean(R.string.p_rmd_enable_quiet, Preferences.getIntegerFromString(R.string.p_rmd_quietStart_old, -1) >= 0);
            setTime(R.string.p_rmd_quietStart_old, R.string.p_rmd_quietStart, 22);
            setTime(R.string.p_rmd_quietEnd_old, R.string.p_rmd_quietEnd, 10);
            setTime(R.string.p_rmd_time_old, R.string.p_rmd_time, 18);
            setBoolean(R.string.p_rmd_hasMigrated, true);
        }
    }

    private static void setTime(int oldResourceId, int newResourceId, int defValue) {
        int hour = Preferences.getIntegerFromString(oldResourceId, defValue);
        int millisOfDay = new DateTime().withMillisOfDay(0).withHourOfDay(hour).getMillisOfDay();
        Preferences.setInt(newResourceId, millisOfDay);
    }

    private static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, int value) {
        Preferences.setIfUnset(prefs, editor, r, key, value);
    }

    private static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, boolean value) {
        Preferences.setIfUnset(prefs, editor, r, key, value);
    }

    private static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, String value) {
        setIfUnset(prefs, editor, r, key, value);
    }

    private static void setIfUnset(SharedPreferences prefs, Editor editor, Resources r, int keyResource, String value) {
        String key = r.getString(keyResource);
        if(!prefs.contains(key) || !(prefs.getAll().get(key) instanceof String)) {
            editor.putString(key, value);
        }
    }

    private static void setIntIfUnset(SharedPreferences prefs, Editor editor, Resources r, int keyResource, int value) {
        String key = r.getString(keyResource);
        if(!prefs.contains(key)) {
            editor.putInt(key, value);
        }
    }
}
