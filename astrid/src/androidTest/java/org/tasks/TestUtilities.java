package org.tasks;

import android.content.Context;

import com.todoroo.astrid.utility.AstridPreferences;

import static com.todoroo.andlib.utility.Preferences.getPrefs;

public class TestUtilities {
    public static void clearPreferences(Context context) {
        getPrefs(context)
                .edit()
                .clear()
                .commit();
    }

    public static void resetPreferences(Context context) {
        clearPreferences(context);
        AstridPreferences.setPreferenceDefaults();
    }
}
