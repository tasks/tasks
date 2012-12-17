package com.todoroo.astrid.utility;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.todoroo.andlib.utility.Preferences;

public abstract class AstridPreferenceSpec {
    public abstract void setIfUnset();
    public abstract void resetDefaults();

    protected static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, int value, boolean ifUnset) {
        if (ifUnset)
            Preferences.setIfUnset(prefs, editor, r, key, value);
        else
            Preferences.setString(r.getString(key), Integer.toString(value));
    }

    protected static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, boolean value, boolean ifUnset) {
        if (ifUnset)
            Preferences.setIfUnset(prefs, editor, r, key, value);
        else
            Preferences.setBoolean(r.getString(key), value);
    }

    protected static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, String value, boolean ifUnset) {
        if (ifUnset)
            Preferences.setIfUnset(prefs, editor, r, key, value);
        else
            Preferences.setString(r.getString(key), value);
    }
}
