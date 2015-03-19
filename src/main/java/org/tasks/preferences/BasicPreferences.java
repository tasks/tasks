package org.tasks.preferences;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

public class BasicPreferences extends InjectingPreferenceActivity {

    @Inject DeviceInfo deviceInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action == null) {
            addPreferencesFromResource(R.xml.preferences);
            findPreference(getString(R.string.contact_developer)).setIntent(
                    new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "Alex Baker<baker.alex+tasks@gmail.com>", null)) {{
                        putExtra(Intent.EXTRA_SUBJECT, "Tasks Feedback");
                        putExtra(Intent.EXTRA_TEXT, deviceInfo.getDebugInfo());
                    }});
            if (!deviceInfo.isPlayStoreAvailable()) {
                getPreferenceScreen().removePreference(findPreference(getString(R.string.rate_tasks)));
            }
        } else if (action.equals(getString(R.string.EPr_appearance_header))) {
            toolbar.setTitle(getString(R.string.EPr_appearance_header));
            addPreferencesFromResource(R.xml.preferences_appearance);
        } else if (action.equals(getString(R.string.backup_BPr_header))) {
            toolbar.setTitle(getString(R.string.backup_BPr_header));
            addPreferencesFromResource(R.xml.preferences_backup);
        }
    }
}
