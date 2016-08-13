package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.files.FileExplore;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;

import java.io.File;

import javax.inject.Inject;

import static org.tasks.dialogs.ExportTasksDialog.newExportTasksDialog;
import static org.tasks.dialogs.ImportTasksDialog.newImportTasksDialog;

public class BackupPreferences extends InjectingPreferenceActivity {

    private static final String FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks";
    private static final String FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks";
    private static final int REQUEST_CODE_BACKUP_DIR = 2;
    private static final int REQUEST_PICKER = 1000;

    @Inject Preferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_backup);

        getPref(R.string.backup_BAc_import).setOnPreferenceClickListener(preference -> {
            startActivityForResult(new Intent(BackupPreferences.this, FileExplore.class) {{
                putExtra(FileExplore.EXTRA_START_PATH, preferences.getBackupDirectory().getAbsolutePath());
            }}, REQUEST_PICKER);
            return false;
        });

        getPref(R.string.backup_BAc_export).setOnPreferenceClickListener(preference -> {
            newExportTasksDialog().show(getFragmentManager(), FRAG_TAG_EXPORT_TASKS);
            return false;
        });

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
        } else if (requestCode == REQUEST_PICKER) {
            if (resultCode == RESULT_OK) {
                newImportTasksDialog(data.getStringExtra(FileExplore.EXTRA_FILE))
                        .show(getFragmentManager(), FRAG_TAG_IMPORT_TASKS);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initializeBackupDirectory() {
        findPreference(getString(R.string.p_backup_dir)).setOnPreferenceClickListener(p -> {
            Intent filesDir = new Intent(BackupPreferences.this, FileExplore.class);
            filesDir.putExtra(FileExplore.EXTRA_DIRECTORY_MODE, true);
            filesDir.putExtra(FileExplore.EXTRA_START_PATH, getBackupDirectory());
            startActivityForResult(filesDir, REQUEST_CODE_BACKUP_DIR);
            return true;
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
