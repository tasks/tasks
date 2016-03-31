package org.tasks.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.backup.TasksXmlImporter;

import org.tasks.files.FileExplore;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class ImportTaskActivity extends InjectingAppCompatActivity {

    private static final int REQUEST_PICKER = 1000;

    @Inject TasksXmlImporter xmlImporter;
    @Inject Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivityForResult(new Intent(this, FileExplore.class) {{
            putExtra(FileExplore.EXTRA_START_PATH, preferences.getBackupDirectory().getAbsolutePath());
        }}, REQUEST_PICKER);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                String filePath = data.getStringExtra(FileExplore.EXTRA_FILE);
                xmlImporter.importTasks(ImportTaskActivity.this, filePath, new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            } else {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
