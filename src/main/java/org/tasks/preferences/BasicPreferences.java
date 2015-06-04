package org.tasks.preferences;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

public class BasicPreferences extends InjectingPreferenceActivity {

    private static final int RC_PREFS = 10001;

    @Inject DeviceInfo deviceInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        if (!getResources().getBoolean(R.bool.sync_enabled)) {
            getPreferenceScreen().removePreference(findPreference(getString(R.string.synchronization)));
        }
        findPreference(getString(R.string.EPr_appearance_header)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(BasicPreferences.this, AppearancePreferences.class), RC_PREFS);
                return true;
            }
        });

        findPreference(getString(R.string.contact_developer)).setIntent(
                new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "Tasks Support <support@tasks.org>", null)) {{
                    putExtra(Intent.EXTRA_SUBJECT, "Tasks Feedback");
                    putExtra(Intent.EXTRA_TEXT, deviceInfo.getDebugInfo());
                }});
        if (!deviceInfo.supportsBilling()) {
            remove(R.string.TLA_menu_donate);
        }
        if (!deviceInfo.isPlayStoreAvailable()) {
            remove(R.string.rate_tasks);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PREFS) {
            setResult(resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void remove(int resId) {
        getPreferenceScreen().removePreference(findPreference(getString(resId)));
    }
}
