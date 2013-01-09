/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.todoroo.andlib.service.ContextManager;

/**
 * Helper class for reading and writing SharedPreferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Preferences {

    /**
     * Helper to write to editor if key specified is null. Writes a String
     * property with the given integer
     *
     * @param prefs
     * @param editor
     * @param r
     * @param keyResource
     * @param value
     */
    public static void setIfUnset(SharedPreferences prefs, Editor editor, Resources r, int keyResource, int value) {
        String key = r.getString(keyResource);
        if(!prefs.contains(key))
            editor.putString(key, Integer.toString(value));
    }

    /**
     * Helper to write to editor if key specified is null
     * @param prefs
     * @param editor
     * @param r
     * @param keyResource
     * @param value
     */
    public static void setIfUnset(SharedPreferences prefs, Editor editor, Resources r, int keyResource, boolean value) {
        String key = r.getString(keyResource);
        if(!prefs.contains(key) || !(prefs.getAll().get(key) instanceof Boolean))
            editor.putBoolean(key, value);
    }

    /**
     * Helper to write to editor if key specified is null
     * @param prefs
     * @param editor
     * @param r
     * @param keyResource
     * @param value
     */
    public static void setIfUnset(SharedPreferences prefs, Editor editor, Resources r, int keyResource, String value) {
        String key = r.getString(keyResource);
        if(!prefs.contains(key) || !(prefs.getAll().get(key) instanceof String))
            editor.putString(key, value);
    }

    /* ======================================================================
     * ======================================================= helper methods
     * ====================================================================== */

    private static SharedPreferences preferences = null;

    /** Get preferences object from the context */
    public static SharedPreferences getPrefs(Context context) {
        if(preferences != null)
            return preferences;

        context = context.getApplicationContext();
        try {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);

            // try writing
            preferences.edit().commit();

        } catch (Exception e) {
            String alternate = "preferences" + android.os.Process.myUid(); //$NON-NLS-1$
            preferences = context.getSharedPreferences(alternate, Context.MODE_PRIVATE);
        }

        return preferences;
    }

    /** @return true if given preference is set */
    public static boolean isSet(String key) {
        Context context = ContextManager.getContext();
        return getPrefs(context).contains(key);
    }

    // --- preference fetching (string)

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

    /** Gets an integer value from a string preference. Returns null
     * if the value is not set or not an integer.
     *
     * @param keyResource resource from string.xml
     * @return integer value, or null on error
     */
    public static int getIntegerFromString(int keyResource, int defaultValue) {
        Context context = ContextManager.getContext();
        Resources r = context.getResources();
        String value = getPrefs(context).getString(r.getString(keyResource), null);
        if(value == null)
            return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Gets an float value from a string preference. Returns null
     * if the value is not set or not an flat.
     *
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

    /**
     * Sets string preference
     */
    public static void setString(int keyResource, String newValue) {
        Context context = ContextManager.getContext();
        setString(context.getString(keyResource), newValue);
    }

    /**
     * Sets string preference
     */
    public static void setString(String key, String newValue) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putString(key, newValue);
        editor.commit();
    }

    /**
     * Sets string preference from integer value
     */
    public static void setStringFromInteger(int keyResource, int newValue) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putString(context.getString(keyResource), Integer.toString(newValue));
        editor.commit();
    }

    // --- preference fetching (boolean)

    /** Gets a boolean preference (e.g. a CheckBoxPreference setting)
     *
     * @param key
     * @param defValue
     * @return default if value is unset otherwise the value
     */
    public static boolean getBoolean(String key, boolean defValue) {
        Context context = ContextManager.getContext();
        try {
            return getPrefs(context).getBoolean(key, defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }

    /** Gets a boolean preference (e.g. a CheckBoxPreference setting)
     *
     * @param keyResource
     * @param defValue
     * @return default if value is unset otherwise the value
     */
    public static boolean getBoolean(int keyResources, boolean defValue) {
        return getBoolean(ContextManager.getString(keyResources), defValue);
    }

    /**
     * Sets boolean preference
     * @param key
     * @param value
     */
    public static void setBoolean(int keyResource, boolean value) {
        setBoolean(ContextManager.getString(keyResource), value);
    }

    /**
     * Sets boolean preference
     * @param key
     * @param value
     */
    public static void setBoolean(String key, boolean value) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    // --- preference fetching (int)

    /** Gets a int preference
     *
     * @param key
     * @param defValue
     * @return default if value is unset otherwise the value
     */
    public static int getInt(String key, int defValue) {
        Context context = ContextManager.getContext();
        return getPrefs(context).getInt(key, defValue);
    }

    /**
     * Sets int preference
     * @param key
     * @param value
     */
    public static void setInt(String key, int value) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    // --- preference fetching (long)

    /** Gets a long preference
     *
     * @param key
     * @param defValue
     * @return default if value is unset otherwise the value
     */
    public static long getLong(String key, long defValue) {
        Context context = ContextManager.getContext();
        return getPrefs(context).getLong(key, defValue);
    }

    /**
     * Sets long preference
     * @param key
     * @param value
     */
    public static void setLong(String key, long value) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    /**
     * Clears a preference
     * @param key
     */
    public static void clear(String key) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.remove(key);
        editor.commit();
    }

}
