package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import org.tasks.files.FileExplore;

import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;

import java.io.File;

import javax.inject.Inject;

public class BackupPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_CODE_BACKUP_DIR = 2;

    @Inject Preferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_backup);

        initializeBackupDirectory();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BACKUP_DIR && resultCode == RESULT_OK) {
            if (data != null) {
                String dir = data.getStringExtra(FileExplore.EXTRA_DIRECTORY);
                preferences.setString(R.string.p_backup_dir, dir);
                updateBackupDirectory();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initializeBackupDirectory() {
        findPreference(getString(R.string.p_backup_dir)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                Intent filesDir = new Intent(BackupPreferences.this, FileExplore.class);
                filesDir.putExtra(FileExplore.EXTRA_DIRECTORY_MODE, true);
                filesDir.putExtra(FileExplore.EXTRA_START_PATH, getBackupDirectory());
                startActivityForResult(filesDir, REQUEST_CODE_BACKUP_DIR);
                return true;
            }
        });
        updateBackupDirectory();
    }

    private void updateBackupDirectory() {
        findPreference(getString(R.string.p_backup_dir)).setSummary(getBackupDirectory());
    }

    private String getBackupDirectory() {
        File dir = preferences.getBackupDirectory();
        return dir == null ? "" : dir.getAbsolutePath();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
