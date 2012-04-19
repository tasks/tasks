package com.todoroo.astrid.core;

import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;

public class PerformancePreferences extends TodorooPreferenceActivity {

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_performance;
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        // TODO Auto-generated method stub

    }

}
