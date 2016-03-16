package org.tasks.preferences;

import android.os.Bundle;
import android.preference.PreferenceScreen;

import org.tasks.R;
import org.tasks.injection.ActivityComponent;

public class BasicPreferences extends BaseBasicPreferences {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removePreference(findPreference(getString(R.string.synchronization)));
        preferenceScreen.removePreference(findPreference(getString(R.string.p_tesla_unread_enabled)));
        preferenceScreen.removePreference(findPreference(getString(R.string.p_tasker_enabled)));
        preferenceScreen.removePreference(findPreference(getString(R.string.p_collect_statistics)));
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
