package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.files.FileExplore;

import org.tasks.R;
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
                String dir = data.getStringExtra(FileExplore.RESULT_DIR_SELECTED);
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
                filesDir.putExtra(FileExplore.EXTRA_DIRECTORIES_SELECTABLE, true);
                startActivityForResult(filesDir, REQUEST_CODE_BACKUP_DIR);
                return true;
            }
        });
        updateBackupDirectory();
    }

    private void updateBackupDirectory() {
        File dir = preferences.getBackupDirectory();
        String summary = dir == null ? "" : dir.getAbsolutePath();
        findPreference(getString(R.string.p_backup_dir)).setSummary(summary);
    }
}
