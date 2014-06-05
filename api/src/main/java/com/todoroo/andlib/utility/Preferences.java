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
@Deprecated
public class Preferences {

    private static SharedPreferences preferences = null;

    /** Get preferences object from the context */
    public static SharedPreferences getPrefs(Context context) {
        if(preferences != null) {
            return preferences;
        }

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

    // --- preference fetching (string)

    /** Gets an string value from a string preference. Returns null
     * if the value is not set
     * @return integer value, or null on error
     */
    public static String getStringValue(String key) {
        Context context = ContextManager.getContext();
        return getPrefs(context).getString(key, null);
    }

    /** Gets an string value from a string preference. Returns null
     * if the value is not set
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
        if(value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void setString(String key, String newValue) {
        Context context = ContextManager.getContext();
        Editor editor = getPrefs(context).edit();
        editor.putString(key, newValue);
        editor.commit();
    }

    // --- preference fetching (boolean)

    public static boolean getBoolean(int keyResources, boolean defValue) {
        Context context = ContextManager.getContext();
        try {
            return getPrefs(context).getBoolean(context.getString(keyResources), defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }
}
