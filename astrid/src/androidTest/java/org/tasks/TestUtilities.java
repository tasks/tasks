package org.tasks;

import android.content.Context;

import org.tasks.preferences.Preferences;

public class TestUtilities {
    public static void clearPreferences(Context context) {
        new Preferences(context).clear();
    }
}
