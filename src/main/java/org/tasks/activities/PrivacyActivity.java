package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class PrivacyActivity extends InjectingAppCompatActivity {

    @Inject DialogBuilder dialogBuilder;
    @Inject Tracker tracker;
    @Inject Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialogBuilder.newDialog()
                .setCancelable(false)
                .setTitle(R.string.send_anonymous_statistics)
                .setMessage(String.format("%s\n\n%s",
                        getString(R.string.send_anonymous_statistics_summary),
                        getString(R.string.change_setting_anytime)))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.opt_out, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.setTrackingEnabled(false);
                        tracker.setTrackingEnabled(false);
                        finish();
                    }
                })
                .show();
    }
}
