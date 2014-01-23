package org.tasks;

import com.todoroo.astrid.utility.AstridPreferences;

import static com.todoroo.andlib.utility.Preferences.getPrefs;
import static org.robolectric.Robolectric.getShadowApplication;

public class TestUtilities {
    public static void clearPreferences() {
        getPrefs(getShadowApplication().getApplicationContext())
                .edit()
                .clear()
                .commit();
    }

    public static void resetPreferences() {
        clearPreferences();
        AstridPreferences.setPreferenceDefaults();
    }
}
