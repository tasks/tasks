/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import android.content.Context;
import android.content.SharedPreferences;
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
    private static SharedPreferences getPrefs(Context context) {
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

    public static boolean getBoolean(int keyResources, boolean defValue) {
        Context context = ContextManager.getContext();
        try {
            return getPrefs(context).getBoolean(context.getString(keyResources), defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }
}
