package com.todoroo.astrid.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.model.Task;

public class Preferences {

    private static final String P_CURRENT_VERSION = "cv"; //$NON-NLS-1$
    private static final String P_READ_INTRODUCTION = "ri"; //$NON-NLS-1$

    /** Set preference defaults, if unset. called at startup */
    public static void setPreferenceDefaults() {
        Context context = ContextManager.getContext();
        SharedPreferences prefs = getPrefs(context);
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        if(getIntegerFromString(R.string.EPr_default_urgency_key) == null) {
            editor.putString(r.getString(R.string.EPr_default_urgency_key),
                    Integer.toString(4));
        }
        if(getIntegerFromString(R.string.EPr_default_importance_key) == null) {
            editor.putString(r.getString(R.string.EPr_default_importance_key),
                    Integer.toString(Task.IMPORTANCE_SHOULD_DO));
        }

        editor.commit();
    }

    /* ======================================================================
     * ========================================================= system prefs
     * ====================================================================== */

	/** CurrentVersion: the currently installed version of Astrid */
    public static int getCurrentVersion() {
        Context context = ContextManager.getContext();
        return getPrefs(context).getInt(P_CURRENT_VERSION, 0);
    }

    /** CurrentVersion: the currently installed version of Astrid */
    public static void setCurrentVersion(int version) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putInt(P_CURRENT_VERSION, version);
        editor.commit();
    }

    /** ReadIntroduction: whether the user has read the introductory notes */
    public static boolean hasReadIntroduction() {
        Context context = ContextManager.getContext();
        return getPrefs(context).getBoolean(P_READ_INTRODUCTION, false);
    }

    /** ReadIntroduction: whether the user has read the introductory notes */
    public static void setReadIntroduction(boolean value) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putBoolean(P_READ_INTRODUCTION, value);
        editor.commit();
    }

    /* ======================================================================
     * ======================================================= helper methods
     * ====================================================================== */

    /** Get preferences object from the context */
    public static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    // --- preference fetching

    /** Gets an string value from a string preference. Returns null
     * if the value is not set
     *
     * @param context
     * @param key
     * @return integer value, or null on error
     */
    public static String getStringValue(String key) {
        Context context = ContextManager.getContext();
        return getPrefs(context).getString(key, null);
    }

    /** Gets an string value from a string preference. Returns null
     * if the value is not set
     *
     * @param context
     * @param key
     * @return integer value, or null on error
     */
    public static String getStringValue(int keyResource) {
        Context context = ContextManager.getContext();
        return getPrefs(context).getString(context.getResources().getString(keyResource), null);
    }

    /** Gets an integer value from a string key. Returns null
     * if the value is not set or not an integer.
     *
     * @param context
     * @param key
     * @return integer value, or null on error
     */
    public static Integer getIntegerFromString(String key) {
        Context context = ContextManager.getContext();
        String value = getPrefs(context).getString(key, null);
        if(value == null)
            return null;

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    /** Gets an integer value from a string preference. Returns null
     * if the value is not set or not an integer.
     *
     * @param context
     * @param keyResource resource from string.xml
     * @return integer value, or null on error
     */
    public static Integer getIntegerFromString(int keyResource) {
        Context context = ContextManager.getContext();
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(keyResource), null);
        if(value == null)
            return null;

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    /** Gets an float value from a string preference. Returns null
     * if the value is not set or not an flat.
     *
     * @param context
     * @param keyResource resource from string.xml
     * @return
     */
    public static Float getFloatFromString(int keyResource) {
        Context context = ContextManager.getContext();
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(keyResource), ""); //$NON-NLS-1$

        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return null;
        }
    }

    /** Gets a boolean preference (e.g. a CheckBoxPreference setting)
     *
     * @param context
     * @param key
     * @param defValue
     * @return default if value is unset otherwise the value
     */
    public static boolean getBoolean(String key, boolean defValue) {
        Context context = ContextManager.getContext();
        return getPrefs(context).getBoolean(key, defValue);
    }
}
