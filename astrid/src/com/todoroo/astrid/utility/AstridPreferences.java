package com.todoroo.astrid.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.ThemeService;

public class AstridPreferences {

    private static final String P_CURRENT_VERSION = "cv"; //$NON-NLS-1$

    private static final String P_CURRENT_VERSION_NAME = "cvname"; //$NON-NLS-1$

    public static final String P_FIRST_TASK = "ft"; //$NON-NLS-1$

    public static final String P_FIRST_LIST = "fl"; //$NON-NLS-1$

    public static final String P_UPGRADE_FROM = "uf"; //$NON-NLS-1$

    public static final String P_FIRST_LAUNCH = "fltime";  //$NON-NLS-1$

    public static final String P_LAST_POPOVER = "lpopover";  //$NON-NLS-1$

    private static final long MIN_POPOVER_TIME = 3 * 1000L;

    public static final String P_SUBTASKS_HELP = "sthelp"; //$NON-NLS-1$

    /** Set preference defaults, if unset. called at startup */
    public static void setPreferenceDefaults() {
        Context context = ContextManager.getContext();
        SharedPreferences prefs = Preferences.getPrefs(context);
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        Preferences.setIfUnset(prefs, editor, r, R.string.p_default_urgency_key, 0);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_default_importance_key, 2);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_default_hideUntil_key, 0);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_default_random_hours, 0);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_fontSize, 18);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_showNotes, false);

        if ("white-blue".equals(Preferences.getStringValue(R.string.p_theme))) { //$NON-NLS-1$ migrate from when white-blue wasn't the default
            Preferences.setString(R.string.p_theme, ThemeService.THEME_WHITE);
        }

        editor.commit();
    }

    /* ======================================================================
     * ========================================================= public prefs
     * ====================================================================== */

    /** Get publicly readable preferences */
    public static SharedPreferences getPublicPrefs(Context context) {
        context = context.getApplicationContext();
        return context.getSharedPreferences(AstridApiConstants.PUBLIC_PREFS,
                Context.MODE_WORLD_READABLE);
    }

    /* ======================================================================
     * ========================================================= system prefs
     * ====================================================================== */

	/** CurrentVersion: the currently installed version of Astrid */
    public static int getCurrentVersion() {
        return Preferences.getInt(P_CURRENT_VERSION, 0);
    }

    /** CurrentVersion: the currently installed version of Astrid */
    public static void setCurrentVersion(int version) {
        Preferences.setInt(P_CURRENT_VERSION, version);
    }

    /** The name (e.g. 4.0.1) of the currently installed version of astrid*/
    public static String getCurrentVersionName() {
        String versionName = Preferences.getStringValue(P_CURRENT_VERSION_NAME);
        if (versionName == null)
            versionName = "0"; //$NON-NLS-1$
        return versionName;
    }

    public static void setCurrentVersionName(String versionName) {
        Preferences.setString(P_CURRENT_VERSION_NAME, versionName);
    }

    /** If true, can show a help popover. If false, another one was recently shown */
    public static boolean canShowPopover() {
        long last = Preferences.getLong(P_LAST_POPOVER, 0);
        if(System.currentTimeMillis() - last < MIN_POPOVER_TIME)
            return false;
        Preferences.setLong(P_LAST_POPOVER, System.currentTimeMillis());
        return true;
    }

}
