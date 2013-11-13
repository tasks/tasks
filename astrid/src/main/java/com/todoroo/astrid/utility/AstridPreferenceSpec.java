package com.todoroo.astrid.utility;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.todoroo.andlib.utility.Preferences;

public abstract class AstridPreferenceSpec {
    public abstract void setIfUnset();

    protected static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, int value) {
        Preferences.setIfUnset(prefs, editor, r, key, value);
    }

    protected static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, boolean value) {
        Preferences.setIfUnset(prefs, editor, r, key, value);
    }

    protected static void setPreference(SharedPreferences prefs, Editor editor, Resources r, int key, String value) {
        Preferences.setIfUnset(prefs, editor, r, key, value);
    }
}
