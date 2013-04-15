package com.todoroo.astrid.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.utility.AstridDefaultPreferenceSpec.PreferenceExtras;

public class AstridLitePreferenceSpec extends AstridPreferenceSpec {

    @Override
    public void setIfUnset() {
        PreferenceExtras extras = new PreferenceExtras() {
            @Override
            public void setExtras(Context context, SharedPreferences prefs, Editor editor, Resources r, boolean ifUnset) {
                String dragDropTestInitialized = "android_drag_drop_initialized"; //$NON-NLS-1$
                if (!Preferences.getBoolean(dragDropTestInitialized, false)) {
                    SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(context);
                    if (publicPrefs != null) {
                        Editor edit = publicPrefs.edit();
                        if (edit != null) {
                            edit.putInt(SortHelper.PREF_SORT_FLAGS, SortHelper.FLAG_DRAG_DROP);
                            edit.putInt(SortHelper.PREF_SORT_SORT, SortHelper.SORT_AUTO);
                            edit.commit();
                            Preferences.setInt(AstridPreferences.P_SUBTASKS_HELP, 1);
                        }
                    }
                    Preferences.setBoolean(dragDropTestInitialized, true);
                }

                BeastModePreferences.setDefaultLiteModeOrder(context, false);
            }
        };

        setPrefs(extras, true);
    }

    @Override
    public void resetDefaults() {
        PreferenceExtras extras = new PreferenceExtras() {
            @Override
            public void setExtras(Context context, SharedPreferences prefs, Editor editor, Resources r, boolean ifUnset) {
                SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(context);
                if (publicPrefs != null) {
                    Editor edit = publicPrefs.edit();
                    if (edit != null) {
                        edit.putInt(SortHelper.PREF_SORT_FLAGS, SortHelper.FLAG_DRAG_DROP);
                        edit.putInt(SortHelper.PREF_SORT_SORT, SortHelper.SORT_AUTO);
                        edit.commit();
                        Preferences.setInt(AstridPreferences.P_SUBTASKS_HELP, 1);
                    }
                }
                BeastModePreferences.setDefaultLiteModeOrder(context, true);
            }
        };

        setPrefs(extras, false);
    }

    private static void setPrefs(PreferenceExtras extras, boolean ifUnset) {
        Context context = ContextManager.getContext();
        SharedPreferences prefs = Preferences.getPrefs(context);
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        setPreference(prefs, editor, r, R.string.p_default_urgency_key, 4, ifUnset);
        setPreference(prefs, editor, r, R.string.p_default_importance_key, 2, ifUnset);
        setPreference(prefs, editor, r, R.string.p_default_hideUntil_key, 0, ifUnset);
        setPreference(prefs, editor, r, R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE, ifUnset);
        setPreference(prefs, editor, r, R.string.p_default_reminders_mode_key, 16, ifUnset);
        setPreference(prefs, editor, r, R.string.p_rmd_default_random_hours, 0, ifUnset);
        setPreference(prefs, editor, r, R.string.p_fontSize, 20, ifUnset);
        setPreference(prefs, editor, r, R.string.p_showNotes, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_use_contact_picker, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_field_missed_calls, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_end_at_deadline, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_rmd_persistent, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_calendar_reminders, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_use_filters, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_list_members, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_rmd_social, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_theme, ThemeService.THEME_WHITE_ALT, ifUnset);

        setPreference(prefs, editor, r, R.string.p_force_phone_layout, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_today_filter, true, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_waiting_on_me_filter, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_recently_modified_filter, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_ive_assigned_filter, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_not_in_list_filter, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_menu_search, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_menu_friends, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_menu_sync, false, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_menu_sort, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_quickadd_controls, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_task_edit_comments, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_taskRowStyle_v2, "2", ifUnset); //$NON-NLS-1$

        setPreference(prefs, editor, r, R.string.p_use_date_shortcuts, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_save_and_cancel, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_hide_plus_button, true, ifUnset);

        extras.setExtras(context, prefs, editor, r, ifUnset);

        editor.commit();
    }

}
