package org.tasks.preferences;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

public class HelpAndFeedbackActivity extends InjectingPreferenceActivity {

    @Inject DeviceInfo deviceInfo;
    @Inject Preferences preferences;
    @Inject PermissionChecker permissionChecker;
    @Inject PermissionRequestor permissionRequestor;

    private CheckBoxPreference debugLogging;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_help);

        findPreference(getString(R.string.contact_developer)).setIntent(
                new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "Tasks Support <support@tasks.org>", null)) {{
                    putExtra(Intent.EXTRA_SUBJECT, "Tasks Feedback");
                    putExtra(Intent.EXTRA_TEXT, deviceInfo.getDebugInfo());
                }});
        if (!deviceInfo.isPlayStoreAvailable()) {
            remove(R.string.rate_tasks);
        }

        debugLogging = (CheckBoxPreference) findPreference(getString(R.string.p_debug_logging));
        debugLogging.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && (boolean) newValue) {
                    if (permissionRequestor.requestFileWritePermission()) {
                        enableDebugLogging(true);
                    }
                } else {
                    enableDebugLogging(false);
                }
                return true;
            }
        });
        enableDebugLogging(
                preferences.getBoolean(R.string.p_debug_logging, false) &&
                permissionChecker.canWriteToExternalStorage());
    }

    private void enableDebugLogging(boolean enabled) {
        debugLogging.setChecked(enabled);
        preferences.setupLogger(enabled);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_FILE_WRITE) {
            enableDebugLogging(grantResults[0] == PackageManager.PERMISSION_GRANTED);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void remove(int resId) {
        getPreferenceScreen().removePreference(findPreference(getString(resId)));
    }
}