package com.todoroo.astrid.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class UpdateMessagePreference extends PreferenceActivity {

    public static final String TOKEN_PREFS_ARRAY = "prefs_array"; //$NON-NLS-1$

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_blank);
        String prefsArray = getIntent().getStringExtra(TOKEN_PREFS_ARRAY);
        try {
            JSONArray array = new JSONArray(prefsArray);
            if (array.length() == 0)
                finish();

            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject pref = array.getJSONObject(i);
                    addPreferenceFromJSON(pref);
                } catch (JSONException e) {
                    continue;
                }
            }
        } catch (JSONException e) {
            finish();
        }
    }

    @SuppressWarnings("nls")
    private void addPreferenceFromJSON(JSONObject obj) {
        String type = obj.optString("type", null);
        String key = obj.optString("key", null);
        String title = obj.optString("title", null);
        if (type == null || key == null || title == null)
            return;

        Preference pref = null;
        if ("bool".equals(type)) { // We can add other types we want to support and handle the preference construction here
            pref = new CheckBoxPreference(this);
            pref.setKey(key);
            pref.setTitle(title);
            pref.setDefaultValue(Preferences.getBoolean(key, false));
        }

        if (pref == null)
            return;

        if (obj.optBoolean("restart")) {
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    setResult(RESULT_OK);
                    return true;
                }
            });
        }

        getPreferenceScreen().addPreference(pref);
    }

}
